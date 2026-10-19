package com.vaiinilla.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.ui.components.NumberTicker
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ProductDetailSheet
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.order.MenuFeed
import com.vaiinilla.app.ui.order.OrderFlowUiState
import com.vaiinilla.app.ui.order.cartItemCount
import com.vaiinilla.app.ui.order.cartPreviewTotal
import com.vaiinilla.app.ui.order.isSelectedProductValid
import com.vaiinilla.app.ui.order.selectedProduct
import com.vaiinilla.app.ui.order.selectedProductPreviewPrice
import com.vaiinilla.app.ui.order.selectedProductPreviewTotal
import com.vaiinilla.app.ui.theme.Lime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val FeedInk = Color(0xFF151515)
private val LimeInk = Color(0xFF1D2A0A)
private val OnMedia = Color.White
private val MediaShadow = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 1f), 8f)

/** Lo que vuela de un platillo a "Pedir" cuando se agrega. */
private data class Flyer(
    val id: Long,
    val imageUrl: String,
    val start: Offset,
)

private data class Pop(
    val id: Long,
    val at: Offset,
)

/**
 * "Desliza": el menú como un feed de video. Un platillo por pantalla con su foto a sangre, el texto abajo a la
 * izquierda y la columna de acciones a la derecha. "+" agrega (y se vuelve contador), doble toque también agrega,
 * y "Pedir" lleva al carrito de siempre, donde se elige cómo pagar.
 */
@Composable
fun MenuFeedScreen(
    state: OrderFlowUiState,
    onBack: () -> Unit,
    onQuickAdd: (Int) -> Unit,
    onQuickRemove: (Int) -> Unit,
    onOpenProduct: (Int) -> Unit,
    onDismissProduct: () -> Unit,
    onToggleOption: (Int, Int) -> Unit,
    onClearOptionalGroup: (Int) -> Unit,
    onSelectedNotesChange: (String) -> Unit,
    onQuantityChange: (Int) -> Unit,
    onAddSelected: () -> Unit,
    onOpenCart: () -> Unit,
) {
    val catalog = state.catalog
    val products = remember(catalog) { catalog?.let(MenuFeed::products).orEmpty() }
    val categoryNames = remember(catalog) { catalog?.categories?.associate { it.id to it.name }.orEmpty() }
    val feedCategories = remember(products) { products.map { it.categoryId }.distinct() }
    val venue = state.guestVenue?.establishment
    val haptics = rememberVaiinillaHaptics()
    val reduced = reducedMotion()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val pagerState = rememberPagerState(pageCount = { products.size })
    val flyers = remember { mutableStateListOf<Flyer>() }
    val pops = remember { mutableStateListOf<Pop>() }
    var orderCenter by remember { mutableStateOf<Offset?>(null) }
    val orderBump = remember { Animatable(1f) }
    var hintVisible by rememberSaveable { mutableStateOf(true) }
    var toast by remember { mutableStateOf<String?>(null) }
    // Cuánto sigue la página al dedo mientras se desliza de lado para cambiar de categoría.
    val swipeX = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(2600)
        hintVisible = false
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { if (it > 0) hintVisible = false }
    }
    LaunchedEffect(state.createOrderError) {
        if (state.selectedProduct == null) toast = state.createOrderError
    }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2400)
            toast = null
        }
    }

    fun jumpTo(categoryId: Int) {
        val target = products.indexOfFirst { it.categoryId == categoryId }
        if (target >= 0) {
            scope.launch {
                if (reduced) pagerState.scrollToPage(target) else pagerState.animateScrollToPage(target)
            }
        }
    }

    fun add(
        product: Product,
        from: Offset?,
    ) {
        haptics.click()
        onQuickAdd(product.id)
        if (from != null && !MenuFeed.needsChoice(product) && !reduced) {
            flyers += Flyer(System.nanoTime(), product.imageUrl, from)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val fallbackTarget = with(density) { Offset(maxWidth.toPx() / 2f, maxHeight.toPx() - 60.dp.toPx()) }

        if (products.isEmpty()) {
            Text(
                "No hay platillos disponibles por ahora.",
                color = OnMedia,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            VerticalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                key = { products[it].id },
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationX = swipeX.value }
                        .categorySwipe(
                            neighbor = { step ->
                                products.getOrNull(pagerState.currentPage)?.let {
                                    MenuFeed.neighborCategory(feedCategories, it.categoryId, step)
                                }
                            },
                            onFollow = { dx, hasNeighbor ->
                                scope.launch { swipeX.snapTo(MenuFeed.followOffset(dx, hasNeighbor, density.density)) }
                            },
                            onRelease = { categoryId ->
                                if (categoryId != null) {
                                    haptics.selection()
                                    jumpTo(categoryId)
                                }
                                scope.launch {
                                    if (reduced) {
                                        swipeX.snapTo(0f)
                                    } else {
                                        swipeX.animateTo(
                                            0f,
                                            spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
                                        )
                                    }
                                }
                            },
                        ),
            ) { page ->
                val product = products[page]
                val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                FeedPage(
                    product = product,
                    categoryName = categoryNames[product.categoryId].orEmpty(),
                    storeName = venue?.name,
                    storeLogo = venue?.logoUrl,
                    quantity = MenuFeed.quantityOf(state.cartLines, product.id),
                    active = pagerState.currentPage == page,
                    pageOffset = offset,
                    reduced = reduced,
                    onAdd = { from -> add(product, from) },
                    onRemove = {
                        haptics.selection()
                        onQuickRemove(product.id)
                    },
                    onDetails = {
                        haptics.click()
                        onOpenProduct(product.id)
                    },
                    onDoubleTap = { at ->
                        pops += Pop(System.nanoTime(), at)
                        add(product, at)
                    },
                )
            }
        }

        // Arriba: regresar al menú y las categorías, con una sola línea que se desliza a la del platillo.
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassRoundButton(onClick = onBack, size = 40.dp, label = "Volver al menú") {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null,
                    tint = OnMedia,
                    modifier = Modifier.size(20.dp),
                )
            }
            if (products.isNotEmpty()) {
                FeedCategoryTabs(
                    categories = feedCategories.map { it to categoryNames[it].orEmpty() },
                    activeCategoryId = products[pagerState.currentPage].categoryId,
                    reduced = reduced,
                    onSelect = { categoryId ->
                        haptics.selection()
                        jumpTo(categoryId)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // "Pedir": aparece con lo primero que agregas y lleva al carrito.
        AnimatedVisibility(
            visible = state.cartItemCount > 0,
            enter = slideInVertically(spring(dampingRatio = 0.8f, stiffness = 520f)) { it * 2 } + fadeIn(),
            exit = slideOutVertically(spring(stiffness = 520f)) { it * 2 } + fadeOut(),
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
        ) {
            OrderPill(
                state = state,
                bump = orderBump.value,
                onPlaced = { orderCenter = it },
                onClick = {
                    haptics.click()
                    onOpenCart()
                },
            )
        }

        flyers.forEach { flyer ->
            FlyingThumb(
                flyer = flyer,
                target = orderCenter ?: fallbackTarget,
                onLanded = {
                    flyers.remove(flyer)
                    scope.launch {
                        orderBump.snapTo(1.08f)
                        orderBump.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 420f))
                    }
                },
            )
        }
        pops.forEach { pop -> PlusOnePop(pop) { pops.remove(pop) } }

        AnimatedVisibility(visible = hintVisible && products.size > 1, enter = fadeIn(), exit = fadeOut(tween(400))) {
            SwipeHint(reduced)
        }

        toast?.let { text ->
            Text(
                text,
                color = OnMedia,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 64.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }

        state.selectedProduct?.let { product ->
            val defaults =
                product.optionGroups
                    .flatMap { group ->
                        group.options.take(group.minimumSelections).map { it.id }
                    }.toSet()
            ProductDetailSheet(
                product = product,
                categoryName = categoryNames[product.categoryId].orEmpty(),
                selectedOptionIds = state.selectedOptionIds,
                defaultOptionIds = defaults,
                quantity = state.selectedQuantity,
                previewPrice = state.selectedProductPreviewPrice,
                previewTotal = state.selectedProductPreviewTotal,
                canAdd = state.isSelectedProductValid,
                errorMessage = state.createOrderError,
                notes = state.selectedNotes,
                onDismiss = onDismissProduct,
                onToggleOption = onToggleOption,
                onClearOptionalGroup = onClearOptionalGroup,
                onNotesChange = onSelectedNotesChange,
                onQuantityChange = onQuantityChange,
                onAdd = {
                    haptics.click()
                    onAddSelected()
                },
            )
        }
    }
}

@Composable
private fun FeedPage(
    product: Product,
    categoryName: String,
    storeName: String?,
    storeLogo: String?,
    quantity: Int,
    active: Boolean,
    pageOffset: Float,
    reduced: Boolean,
    onAdd: (Offset?) -> Unit,
    onRemove: () -> Unit,
    onDetails: () -> Unit,
    onDoubleTap: (Offset) -> Unit,
) {
    // Mientras el platillo está al frente, la foto se acerca despacio, como si fuera video.
    val zoom =
        if (active && !reduced) {
            val transition = rememberInfiniteTransition(label = "ken-burns-${product.id}")
            transition
                .animateFloat(
                    initialValue = 1.02f,
                    targetValue = 1.12f,
                    animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Reverse),
                    label = "zoom",
                ).value
        } else {
            1.02f
        }
    var origin by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                // El paralaje mueve la foto: sin recortar, la del siguiente platillo se asoma en este.
                .clipToBounds()
                .onGloballyPositioned { origin = it.boundsInRoot().topLeft }
                .pointerInput(product.id) {
                    detectTapGestures(onDoubleTap = { at -> onDoubleTap(origin + at) })
                }.semantics { contentDescription = "${product.name}, ${moneyLabel(product.digitalPrice)}" },
    ) {
        ProductImage(
            imageUrl = product.imageUrl,
            contentDescription = product.name,
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoom
                        scaleY = zoom
                        if (!reduced) translationY = pageOffset * size.height * 0.1f
                    },
            placeholder = { m -> InitialPlane(product.name, m) },
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.42f), 0.18f to Color.Transparent)),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.45f to Color.Transparent,
                        0.7f to Color.Black.copy(alpha = 0.42f),
                        1f to Color.Black.copy(alpha = 0.8f),
                    ),
                ),
        )

        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 92.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (storeName != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StoreAvatar(storeName, storeLogo, 22.dp)
                    Text(
                        storeName,
                        style = mediaText(13.sp, FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                product.name,
                style = mediaText(30.sp, FontWeight.Black, lineHeight = 32.sp, letterSpacing = (-0.6).sp),
                maxLines = 3,
            )
            Text(moneyLabel(product.digitalPrice), style = mediaText(20.sp, FontWeight.Bold))
            if (product.description.isNotBlank()) {
                Text(
                    product.description,
                    style = mediaText(14.sp, FontWeight.Normal, lineHeight = 19.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (product.estimatedTimeMinutes > 0) FeedChip("${product.estimatedTimeMinutes} min")
                if (categoryName.isNotBlank()) FeedChip(categoryName)
                if (MenuFeed.needsChoice(product)) FeedChip("Con opciones")
            }
        }

        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 10.dp, bottom = 104.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (storeName != null) StoreAvatar(storeName, storeLogo, 48.dp, border = 2.dp)
            Adder(product = product, quantity = quantity, reduced = reduced, onAdd = onAdd, onRemove = onRemove)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                GlassRoundButton(onClick = onDetails, label = "Detalles de ${product.name}") {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = OnMedia, modifier = Modifier.size(24.dp))
                }
                Text("Detalles", style = mediaText(11.5.sp, FontWeight.SemiBold))
            }
        }
    }
}

/** El "+" y el contador son el mismo objeto: crece hacia arriba y cambia lo de adentro. */
@Composable
private fun Adder(
    product: Product,
    quantity: Int,
    reduced: Boolean,
    onAdd: (Offset?) -> Unit,
    onRemove: () -> Unit,
) {
    val inCart = quantity > 0
    val height by animateDpAsState(
        targetValue = if (inCart) 132.dp else 52.dp,
        animationSpec = if (reduced) snapSpec() else spring(dampingRatio = 0.55f, stiffness = 420f),
        label = "adder-height",
    )
    var center by remember { mutableStateOf<Offset?>(null) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier =
                Modifier
                    .width(52.dp)
                    .height(height)
                    .onGloballyPositioned { center = it.boundsInRoot().center }
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.White),
        ) {
            Crossfade(
                targetState = inCart,
                animationSpec = tween(if (reduced) 0 else 180),
                label = "adder",
            ) { showStepper ->
                if (!showStepper) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .physicalPress(scale = PhysicalPressScale.Small) { onAdd(center) }
                                .semantics { contentDescription = "Agregar ${product.name}" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = null,
                            tint = FeedInk,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Box(
                            Modifier
                                .size(44.dp, 40.dp)
                                .physicalPress(scale = PhysicalPressScale.Small) { onAdd(center) }
                                .semantics { contentDescription = "Uno más de ${product.name}" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = null,
                                tint = FeedInk,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        NumberTicker(value = quantity, label = "qty-${product.id}") {
                            Text("$it", color = FeedInk, fontWeight = FontWeight.Black, fontSize = 17.sp)
                        }
                        Box(
                            Modifier
                                .size(44.dp, 40.dp)
                                .physicalPress(scale = PhysicalPressScale.Small, onClick = onRemove)
                                .semantics { contentDescription = "Uno menos de ${product.name}" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Remove,
                                contentDescription = null,
                                tint = FeedInk,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
        Text(if (inCart) "En tu pedido" else "Agregar", style = mediaText(11.5.sp, FontWeight.SemiBold))
    }
}

@Composable
private fun OrderPill(
    state: OrderFlowUiState,
    bump: Float,
    onPlaced: (Offset) -> Unit,
    onClick: () -> Unit,
) {
    val recent = state.cartLines.takeLast(3).reversed()
    Row(
        modifier =
            Modifier
                .onGloballyPositioned { onPlaced(it.boundsInRoot().center) }
                .graphicsLayer {
                    scaleX = bump
                    scaleY = bump
                }.height(58.dp)
                .clip(RoundedCornerShape(29.dp))
                .background(Lime)
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
                .semantics {
                    contentDescription =
                        "Pedir: ${state.cartItemCount} artículos, ${moneyLabel(state.cartPreviewTotal)}"
                }.padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box {
            recent.forEachIndexed { index, line ->
                ProductImage(
                    imageUrl = line.product.imageUrl,
                    contentDescription = "",
                    modifier =
                        Modifier
                            .offset(x = (index * 28).dp)
                            .size(42.dp)
                            .clip(CircleShape)
                            .border(2.dp, Lime, CircleShape)
                            .background(Color.White)
                            .clearAndSetSemantics {},
                    placeholder = { m -> InitialPlane(line.product.name, m) },
                )
            }
            Box(Modifier.width((42 + (recent.size - 1).coerceAtLeast(0) * 28).dp).height(42.dp))
        }
        Text("Pedir", color = LimeInk, fontWeight = FontWeight.Black, fontSize = 17.sp)
        Row(
            modifier =
                Modifier
                    .height(42.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .background(LimeInk.copy(alpha = 0.12f))
                    .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            NumberTicker(value = state.cartItemCount, label = "feed-count") {
                Text("$it", color = LimeInk, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Text("·", color = LimeInk, fontWeight = FontWeight.Bold)
            NumberTicker(
                value = state.cartPreviewTotal.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                label = "feed-total",
            ) {
                Text(
                    moneyLabel(state.cartPreviewTotal),
                    color = LimeInk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
        }
    }
}

/** La foto del platillo vuela a "Pedir": un resorte lleva la posición y otro la escala; se desenfoca de prisa. */
@Composable
private fun FlyingThumb(
    flyer: Flyer,
    target: Offset,
    onLanded: () -> Unit,
) {
    val density = LocalDensity.current
    val half = with(density) { 23.dp.toPx() }
    val position = remember(flyer.id) { Animatable(flyer.start, Offset.VectorConverter) }
    val scale = remember(flyer.id) { Animatable(1.25f) }
    LaunchedEffect(flyer.id) {
        launch { scale.animateTo(0.7f, spring(dampingRatio = 0.55f, stiffness = 420f)) }
        position.animateTo(target, spring(dampingRatio = 0.85f, stiffness = 190f))
        onLanded()
    }
    ProductImage(
        imageUrl = flyer.imageUrl,
        contentDescription = "",
        modifier =
            Modifier
                .offset { IntOffset((position.value.x - half).roundToInt(), (position.value.y - half).roundToInt()) }
                .size(46.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }.clip(CircleShape)
                .border(2.dp, Color.White, CircleShape)
                .background(Color.White),
    )
}

@Composable
private fun PlusOnePop(
    pop: Pop,
    onDone: () -> Unit,
) {
    val progress = remember(pop.id) { Animatable(0f) }
    LaunchedEffect(pop.id) {
        progress.animateTo(1f, spring(dampingRatio = 1f, stiffness = 180f))
        onDone()
    }
    val density = LocalDensity.current
    Text(
        "+1",
        style = mediaText(22.sp, FontWeight.Black),
        modifier =
            Modifier
                .offset {
                    with(density) {
                        IntOffset(
                            (pop.at.x - 14.dp.toPx()).roundToInt(),
                            (pop.at.y - 40.dp.toPx() - progress.value * 40.dp.toPx()).roundToInt(),
                        )
                    }
                }.graphicsLayer {
                    val s = 0.8f + progress.value * 0.4f
                    scaleX = s
                    scaleY = s
                    alpha = 1f - ((progress.value - 0.6f).coerceAtLeast(0f) * 2.5f)
                },
    )
}

@Composable
private fun FeedCategoryTabs(
    categories: List<Pair<Int, String>>,
    activeCategoryId: Int,
    reduced: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    var viewport by remember { mutableStateOf(0) }
    val bounds = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }
    val density = LocalDensity.current
    val active = bounds[activeCategoryId]
    val pad = with(density) { 10.dp.toPx() }
    val indicatorX by animateFloatAsState(
        targetValue = (active?.first ?: 0f) + pad,
        animationSpec = if (reduced) snapSpec() else spring(dampingRatio = 0.8f, stiffness = 520f),
        label = "indicator-x",
    )
    val indicatorW by animateFloatAsState(
        targetValue = ((active?.second ?: 0f) - pad * 2).coerceAtLeast(0f),
        animationSpec = if (reduced) snapSpec() else spring(dampingRatio = 0.8f, stiffness = 520f),
        label = "indicator-w",
    )
    LaunchedEffect(activeCategoryId, active) {
        val (x, w) = active ?: return@LaunchedEffect
        val target = (x + w / 2 - viewport / 2f).roundToInt().coerceIn(0, scroll.maxValue)
        if (abs(target - scroll.value) > 4) scroll.animateScrollTo(target)
    }
    Box(modifier = modifier.onGloballyPositioned { viewport = it.size.width }.horizontalScroll(scroll)) {
        Row(modifier = Modifier.padding(horizontal = 6.dp)) {
            categories.forEach { (id, name) ->
                val selected = id == activeCategoryId
                Text(
                    name,
                    style = mediaText(15.sp, if (selected) FontWeight.Bold else FontWeight.SemiBold),
                    color = if (selected) OnMedia else OnMedia.copy(alpha = 0.62f),
                    maxLines = 1,
                    modifier =
                        Modifier
                            .onGloballyPositioned { bounds[id] = it.positionInParent().x to it.size.width.toFloat() }
                            .physicalPress(scale = PhysicalPressScale.Small) { onSelect(id) }
                            .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 10.dp),
                )
            }
        }
        if (active != null) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 2.dp)
                    .offset { IntOffset((indicatorX + with(density) { 6.dp.toPx() }).roundToInt(), 0) }
                    .width(with(density) { indicatorW.toDp() })
                    .height(3.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(OnMedia),
            )
        }
    }
}

@Composable
private fun SwipeHint(reduced: Boolean) {
    val lift =
        if (reduced) {
            0f
        } else {
            rememberInfiniteTransition(label = "swipe-hint")
                .animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Restart),
                    label = "lift",
                ).value
        }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                Icons.Rounded.KeyboardArrowUp,
                contentDescription = null,
                tint = OnMedia,
                modifier = Modifier.size(56.dp).offset(y = (-24 * lift).dp).graphicsLayer { alpha = 1f - lift * 0.6f },
            )
            Text("Desliza para ver más", style = mediaText(22.sp, FontWeight.Black))
        }
    }
}

@Composable
private fun GlassRoundButton(
    onClick: () -> Unit,
    label: String,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f))
                .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
                .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun StoreAvatar(
    name: String,
    logoUrl: String?,
    size: androidx.compose.ui.unit.Dp,
    border: androidx.compose.ui.unit.Dp = 1.5.dp,
) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .border(border, Color.White, CircleShape)
            .background(Color.White),
    ) {
        if (logoUrl.isNullOrBlank()) {
            InitialPlane(name, Modifier.fillMaxSize())
        } else {
            ProductImage(imageUrl = logoUrl, contentDescription = "", modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun FeedChip(text: String) {
    Text(
        text,
        style = mediaText(12.sp, FontWeight.SemiBold),
        modifier =
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color.Black.copy(alpha = 0.32f))
                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
                .padding(horizontal = 9.dp, vertical = 5.dp),
    )
}

/** Sin foto: un plano neutro con la inicial, nunca una ilustración genérica. La letra crece con el plano. */
@Composable
private fun InitialPlane(
    name: String,
    modifier: Modifier,
) {
    BoxWithConstraints(modifier.background(Color(0xFF2A2927)), contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight)
        Text(
            name.trim().take(1).uppercase(),
            color = Color.White.copy(alpha = 0.75f),
            fontWeight = FontWeight.Black,
            fontSize = (side.value * 0.45f).coerceIn(12f, 96f).sp,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

private fun mediaText(
    size: androidx.compose.ui.unit.TextUnit,
    weight: FontWeight,
    lineHeight: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    letterSpacing: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
) = TextStyle(
    color = OnMedia,
    fontSize = size,
    fontWeight = weight,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
    shadow = MediaShadow,
)

private fun <T> snapSpec() =
    androidx.compose.animation.core
        .snap<T>()

/**
 * De lado, no hacia arriba: a la izquierda la siguiente categoría, a la derecha la anterior. Solo toma el gesto si es
 * 1.5 veces más horizontal que vertical; lo vertical sigue siendo del pager. [neighbor] da la categoría de ese lado
 * (o null en la primera y la última), [onFollow] mueve la página con el dedo y [onRelease] recibe a qué categoría
 * cambiar, si alcanzó.
 */
private fun Modifier.categorySwipe(
    neighbor: (step: Int) -> Int?,
    onFollow: (dx: Float, hasNeighbor: Boolean) -> Unit,
    onRelease: (categoryId: Int?) -> Unit,
): Modifier =
    pointerInput(Unit) {
        val threshold = SWIPE_THRESHOLD_DP * density
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val tracker = VelocityTracker()
            tracker.addPosition(down.uptimeMillis, down.position)
            var dx = 0f
            var dy = 0f
            var sideways = false
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                // Si el pager ya se llevó el gesto hacia arriba o abajo, este no lo toca.
                if (!sideways && change.isConsumed) return@awaitEachGesture
                val delta = change.positionChange()
                dx += delta.x
                dy += delta.y
                tracker.addPosition(change.uptimeMillis, change.position)
                if (!sideways && kotlin.math.abs(dx) > viewConfiguration.touchSlop && MenuFeed.isSideways(dx, dy)) {
                    sideways = true
                }
                if (sideways) {
                    change.consume()
                    onFollow(dx, neighbor(if (dx < 0f) 1 else -1) != null)
                }
            }
            if (!sideways) return@awaitEachGesture
            val projected = dx + tracker.calculateVelocity().x * SWIPE_PROJECTION_SECONDS
            val step = MenuFeed.categoryStep(dx, dy, projected, threshold)
            onRelease(if (step != 0) neighbor(step) else null)
        }
    }

private const val SWIPE_THRESHOLD_DP = 90f

/** Cuánto se proyecta la velocidad al soltar, como el desplazamiento previsto de iOS. */
private const val SWIPE_PROJECTION_SECONDS = 0.2f
