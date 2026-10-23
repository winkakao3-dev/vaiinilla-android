package com.vaiinilla.app.ui.components

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.guardianship.restrictionFor
import com.vaiinilla.app.domain.model.CART_LINE_NOTE_MAX
import com.vaiinilla.app.domain.model.OptionGroup
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.model.ProductOption
import com.vaiinilla.app.ui.parentmode.LocalChildControls
import com.vaiinilla.app.ui.screens.LocalScene
import com.vaiinilla.app.ui.screens.ProductMonogram
import com.vaiinilla.app.ui.screens.ProductPhotoShadow
import com.vaiinilla.app.ui.screens.glassEdge
import com.vaiinilla.app.ui.screens.productTone
import com.vaiinilla.app.ui.screens.scenePaletteForTheme
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.SeaGlassButton
import com.vaiinilla.app.ui.theme.SeaGlassTone
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

@Composable
fun ProductDetailSheet(
    product: Product,
    categoryName: String,
    selectedOptionIds: Set<Int>,
    defaultOptionIds: Set<Int> = emptySet(),
    quantity: Int,
    previewPrice: String,
    previewTotal: String,
    canAdd: Boolean,
    errorMessage: String?,
    notes: String = "",
    onDismiss: () -> Unit,
    onToggleOption: (groupId: Int, optionId: Int) -> Unit,
    onClearOptionalGroup: (groupId: Int) -> Unit,
    onNotesChange: (String) -> Unit = {},
    onQuantityChange: (delta: Int) -> Unit,
    onAdd: () -> Unit,
) {
    val scene = scenePaletteForTheme()
    val baseColors = LocalVaiinillaColors.current
    val colors =
        baseColors.copy(
            paper = scene.sheetSolid,
            paper2 = scene.glassCard,
            line = scene.glassLine,
            ink = scene.ink,
            ink2 = scene.ink.copy(alpha = 0.82f),
            muted = scene.muted,
        )
    val tone = productTone(product)
    val reduceMotion = reducedMotion()
    // Lo agotado, y lo que el tutor del alumno bloqueó o tiene un alérgeno suyo, se ve pero no se agrega.
    val childRestriction = LocalChildControls.current.restrictionFor(product)
    val canAdd = canAdd && childRestriction == null
    val errorMessage = childRestriction?.label ?: errorMessage
    val isCustomized = selectedOptionIds != defaultOptionIds
    val interactionSource = remember { MutableInteractionSource() }
    val visibility = remember { MutableTransitionState(false) }
    var dismissing by remember { mutableStateOf(false) }
    val haptics = rememberVaiinillaHaptics()
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 96.dp.toPx() }
    var sheetPullDistancePx by remember(product.id) { mutableFloatStateOf(0f) }

    fun requestDismiss() {
        if (!dismissing) {
            dismissing = true
            visibility.targetState = false
        }
    }

    // Preview del back predictivo: mientras se arrastra desde el borde la ficha
    // sigue al dedo y el scrim se abre para asomar el catalogo detras. Al soltar
    // se cierra con la animacion de salida normal; al cancelar vuelve con spring.
    val scope = rememberCoroutineScope()
    var backProgress by remember { mutableFloatStateOf(0f) }
    var backSettleJob by remember { mutableStateOf<Job?>(null) }
    val backShiftPx = with(density) { 220.dp.toPx() }
    PredictiveBackHandler(enabled = true) { events ->
        backSettleJob?.cancel()
        try {
            events.collect { event ->
                backProgress = if (reduceMotion) 0f else event.progress
            }
            backProgress = 0f
            requestDismiss()
        } catch (e: CancellationException) {
            backSettleJob =
                scope.launch {
                    Animatable(backProgress).animateTo(
                        targetValue = 0f,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 600f),
                    ) { backProgress = value }
                }
            throw e
        }
    }

    val sheetNestedScroll =
        remember(product.id, listState, dismissThresholdPx) {
            object : NestedScrollConnection {
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (available.y < 0f && sheetPullDistancePx > 0f) {
                        sheetPullDistancePx = (sheetPullDistancePx + available.y).coerceAtLeast(0f)
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    val atTop =
                        listState.firstVisibleItemIndex == 0 &&
                            listState.firstVisibleItemScrollOffset == 0
                    if (!atTop || available.y <= 0f) return Offset.Zero
                    sheetPullDistancePx += available.y
                    // Consume the overscroll so the list and dismiss gesture never animate the sheet independently.
                    return Offset(0f, available.y)
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (sheetPullDistancePx <= 0f) return Velocity.Zero
                    val shouldDismiss =
                        sheetPullDistancePx >= dismissThresholdPx || available.y >= 1_100f
                    sheetPullDistancePx = 0f
                    if (shouldDismiss) requestDismiss()
                    return if (shouldDismiss) Velocity(0f, available.y) else Velocity.Zero
                }

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity,
                ): Velocity {
                    sheetPullDistancePx = 0f
                    return Velocity.Zero
                }
            }
        }

    val scrimAlpha by
        animateFloatAsState(
            targetValue = if (visibility.targetState) 0.58f else 0f,
            animationSpec = tween(durationMillis = if (reduceMotion) 0 else 180),
            label = "product-detail-scrim",
        )
    val enter =
        if (reduceMotion) {
            fadeIn(animationSpec = tween(0))
        } else {
            slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = spring(dampingRatio = 0.88f, stiffness = 480f),
            ) +
                scaleIn(
                    initialScale = 0.97f,
                    transformOrigin = TransformOrigin(0.5f, 1f),
                    animationSpec = spring(dampingRatio = 0.88f, stiffness = 480f),
                ) +
                fadeIn(animationSpec = tween(150))
        }
    val exit =
        if (reduceMotion) {
            fadeOut(animationSpec = tween(0))
        } else {
            slideOutVertically(
                targetOffsetY = { it / 3 },
                animationSpec = tween(210),
            ) +
                scaleOut(
                    targetScale = 0.98f,
                    transformOrigin = TransformOrigin(0.5f, 1f),
                    animationSpec = tween(210),
                ) +
                fadeOut(animationSpec = tween(140))
        }

    LaunchedEffect(Unit) { visibility.targetState = true }
    LaunchedEffect(visibility.currentState, dismissing) {
        if (dismissing && !visibility.currentState) onDismiss()
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = (scrimAlpha * (1f - backProgress * 0.9f)).coerceIn(0f, 1f)))
                .clickable(onClick = ::requestDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visibleState = visibility,
            modifier =
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                        ),
                    ).padding(top = 8.dp)
                    .offset { IntOffset(0, (backShiftPx * backProgress).roundToInt()) }
                    .align(Alignment.BottomCenter),
            enter = enter,
            exit = exit,
        ) {
            Surface(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .nestedScroll(sheetNestedScroll)
                        .testTag("product-detail-surface")
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {},
                        ),
                color = scene.sheetSolid,
                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                border = BorderStroke(1.dp, scene.glassEdge),
                shadowElevation = 24.dp,
            ) {
                // La luz de la foto se queda pegada a la foto: un halo corto bajo su borde, no un fondo de color
                // para toda la hoja. Sin foto no hay halo.
                CompositionLocalProvider(LocalScene provides scene, LocalVaiinillaColors provides colors) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(HeroHeight + HaloFade)
                            .graphicsLayer {
                                compositingStrategy = CompositingStrategy.Offscreen
                                // Sube con la foto al hacer scroll; si no, quedaría un fantasma detrás del texto.
                                val photoOnScreen = listState.firstVisibleItemIndex == 0
                                translationY =
                                    if (photoOnScreen) -listState.firstVisibleItemScrollOffset.toFloat() else 0f
                                alpha = if (photoOnScreen) 1f else 0f
                            }.drawWithContent {
                                drawContent()
                                drawRect(
                                    Brush.verticalGradient(
                                        0.7f to Color.Black,
                                        1f to Color.Transparent,
                                    ),
                                    blendMode = BlendMode.DstIn,
                                )
                            },
                    ) {
                        ProductPhotoShadow(
                            product = product,
                            tone = tone,
                            shape = RoundedCornerShape(28.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(HeroHeight),
                            blur = 28.dp,
                            drop = 14.dp,
                            inset = 34.dp,
                            strength = if (colors.isDark) 0.36f else 0.28f,
                        )
                    }
                    Column(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.weight(1f).testTag("product-detail-scroll"),
                            contentPadding =
                                androidx.compose.foundation.layout
                                    .PaddingValues(bottom = 24.dp),
                        ) {
                            item {
                                ProductHero(
                                    product = product,
                                    previewPrice = previewPrice,
                                    customized = isCustomized,
                                    onDismiss = ::requestDismiss,
                                )
                            }
                            item {
                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 18.dp),
                                ) {
                                    Text(
                                        text = product.name,
                                        color = colors.ink,
                                        fontSize = 32.sp,
                                        lineHeight = 36.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-1.1).sp,
                                    )
                                    ProductFactsLine(
                                        previewPrice = previewPrice,
                                        estimatedMinutes = product.estimatedTimeMinutes,
                                        categoryName = categoryName,
                                        modifier = Modifier.padding(top = 6.dp),
                                    )
                                    if (product.description.isNotBlank()) {
                                        Text(
                                            text = product.description,
                                            color = colors.muted,
                                            fontSize = 16.sp,
                                            lineHeight = 22.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(top = 6.dp),
                                        )
                                    }
                                }
                            }

                            product.optionGroups.forEach { group ->
                                item(key = "option-group-${group.id}") {
                                    ProductOptionGroup(
                                        group = group,
                                        selectedOptionIds = selectedOptionIds,
                                        onToggleOption = onToggleOption,
                                        onClearOptionalGroup = onClearOptionalGroup,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                    )
                                }
                            }

                            item(key = "product-notes") {
                                EditorialNotesField(
                                    value = notes,
                                    onValueChange = { onNotesChange(it.take(CART_LINE_NOTE_MAX)) },
                                    label = "Nota para cocina (opcional)",
                                    placeholder = "Ej. sin cebolla, salsa aparte",
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                )
                            }

                            item {
                                ProductDetails(
                                    ingredients = product.ingredients,
                                    allergens = product.allergens,
                                    modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp),
                                )
                            }
                        }

                        ProductDetailDock(
                            product = product,
                            selectedOptionIds = selectedOptionIds,
                            quantity = quantity,
                            previewTotal = previewTotal,
                            canAdd = canAdd,
                            errorMessage = errorMessage,
                            customized = isCustomized,
                            onQuantityChange = { delta ->
                                haptics.click()
                                onQuantityChange(delta)
                            },
                            onAdd = {
                                haptics.impact()
                                onAdd()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductHero(
    product: Product,
    previewPrice: String,
    customized: Boolean,
    onDismiss: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val scene = LocalScene.current
    val heroShape = RoundedCornerShape(28.dp)
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 14.dp)
                .height(HeroHeight)
                .clip(heroShape)
                .background(colors.paper2)
                .glassEdge(scene.glassEdge, heroShape),
    ) {
        ProductImage(
            imageUrl = product.imageUrl,
            contentDescription = product.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            placeholder = { m -> ProductMonogram(product.name, m) },
        )

        // Una sola salida: atrás es el gesto del sistema.
        HeroActionButton(
            icon = Icons.Rounded.Close,
            description = "Cerrar",
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd).padding(14.dp),
        )

        AnimatedVisibility(
            visible = customized,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f),
            modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
        ) {
            Surface(color = colors.ink, shape = RoundedCornerShape(14.dp)) {
                Text(
                    "Personalizado",
                    color = colors.onInk,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/** Precio, tiempo y categoría en una sola línea, como Chick-fil-A: los datos se leen sin tarjetas. */
@Composable
private fun ProductFactsLine(
    previewPrice: String,
    estimatedMinutes: Int,
    categoryName: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        AnimatedContent(
            targetState = previewPrice,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInVertically { height -> height } + fadeIn()) togetherWith
                        (slideOutVertically { height -> -height } + fadeOut())
                } else {
                    (slideInVertically { height -> -height } + fadeIn()) togetherWith
                        (slideOutVertically { height -> height } + fadeOut())
                }.using(SizeTransform(clip = false))
            },
            label = "facts_price_ticker",
        ) { price ->
            Text(
                moneyLabel(price),
                color = colors.ink,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Black,
            )
        }
        val rest =
            listOfNotNull(
                estimatedMinutes.takeIf { it > 0 }?.let { "listo en ~${estimatedTimeLabel(it)}" },
                categoryName.takeIf { it.isNotBlank() },
            ).joinToString(" · ")
        if (rest.isNotEmpty()) {
            Text(
                " · $rest",
                color = colors.muted,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HeroActionButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCircleButton(icon, description, onClick, modifier, size = 46.dp, iconSize = 21.dp)
}

@Composable
private fun ProductOptionGroup(
    group: OptionGroup,
    selectedOptionIds: Set<Int>,
    onToggleOption: (Int, Int) -> Unit,
    onClearOptionalGroup: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val options =
        buildList<ProductOptionChoice> {
            if (group.minimumSelections == 0) {
                add(
                    ProductOptionChoice(
                        option = null,
                        label = "Sin extra",
                        selected = group.options.none { it.id in selectedOptionIds },
                    ),
                )
            }
            group.options.forEach { option ->
                add(
                    ProductOptionChoice(
                        option = option,
                        label =
                            option.name + if (option.extraPrice == "0.00") "" else " +${moneyLabel(option.extraPrice)}",
                        selected = option.id in selectedOptionIds,
                    ),
                )
            }
        }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                group.name,
                color = colors.ink,
                fontSize = 18.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f),
            )
            Surface(
                color = colors.paper2,
                shape = RoundedCornerShape(99.dp),
            ) {
                Text(
                    if (group.minimumSelections > 0) "Obligatorio" else "Opcional",
                    color = colors.muted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                )
            }
        }
        Column(
            modifier = Modifier.padding(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.chunked(2).forEach { rowOptions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowOptions.forEach { choice ->
                        ProductOptionTile(
                            label = choice.label,
                            selected = choice.selected,
                            onClick = {
                                haptics.selection()
                                val option = choice.option
                                if (option == null) {
                                    onClearOptionalGroup(group.id)
                                } else {
                                    onToggleOption(group.id, option.id)
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowOptions.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private data class ProductOptionChoice(
    val option: ProductOption?,
    val label: String,
    val selected: Boolean,
)

@Composable
private fun ProductOptionTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // La elegida se rellena de tinta: el lima queda solo para "Agregar".
    ChoiceChip(
        label = label,
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        height = 56.dp,
        shape = RoundedCornerShape(20.dp),
    )
}

/** Ingredientes y alérgenos como texto. Si el negocio no capturó nada, no se muestra nada. */
@Composable
private fun ProductDetails(
    ingredients: String,
    allergens: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val hasRelevantAllergens =
        allergens.isNotBlank() &&
            !allergens.startsWith("Sin ", ignoreCase = true)
    if (ingredients.isBlank() && !hasRelevantAllergens) return
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (ingredients.isNotBlank()) {
            Text(
                "Lleva: $ingredients",
                color = colors.ink2,
                fontSize = 15.sp,
                lineHeight = 21.sp,
            )
        }
        if (hasRelevantAllergens) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = colors.ink2,
                    modifier = Modifier.padding(top = 1.dp).size(18.dp),
                )
                Text(
                    "Alérgenos: $allergens",
                    color = colors.ink2,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ProductDetailDock(
    product: Product,
    selectedOptionIds: Set<Int>,
    quantity: Int,
    previewTotal: String,
    canAdd: Boolean,
    errorMessage: String?,
    customized: Boolean,
    onQuantityChange: (Int) -> Unit,
    onAdd: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val selection =
        product.optionGroups
            .flatMap { it.options }
            .filter { it.id in selectedOptionIds }
            .joinToString(" · ") { it.name }
            .ifBlank { "Sin extras" }

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        color = colors.paper.copy(alpha = 0.94f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        border = BorderStroke(1.dp, LocalScene.current.glassEdge),
        shadowElevation = 18.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    if (product.optionGroups.isEmpty()) {
                        Text(
                            "Cantidad",
                            color = colors.ink,
                            fontSize = 17.sp,
                            lineHeight = 21.sp,
                            fontWeight = FontWeight.Black,
                        )
                        return@Column
                    }
                    Text(
                        if (customized) "Tu personalización" else "Tu selección",
                        color = colors.muted,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        selection,
                        color = colors.ink,
                        fontSize = 17.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                QuantityControl(
                    quantity = quantity,
                    onMinus = { onQuantityChange(-1) },
                    onPlus = { onQuantityChange(1) },
                )
            }
            Spacer(Modifier.height(10.dp))
            SeaGlassButton(
                tone = SeaGlassTone.lima,
                onClick = onAdd,
                enabled = canAdd,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier =
                        Modifier.clearAndSetSemantics {
                            text = AnnotatedString("Agregar · ${moneyLabel(previewTotal)}")
                        },
                ) {
                    Text(
                        "Agregar · ",
                        fontSize = 17.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Black,
                    )
                    AnimatedContent(
                        targetState = previewTotal,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInVertically { height -> height } + fadeIn()) togetherWith
                                    (slideOutVertically { height -> -height } + fadeOut())
                            } else {
                                (slideInVertically { height -> -height } + fadeIn()) togetherWith
                                    (slideOutVertically { height -> height } + fadeOut())
                            }.using(SizeTransform(clip = false))
                        },
                        label = "add_price_ticker",
                    ) { total ->
                        Text(
                            moneyLabel(total),
                            fontSize = 17.sp,
                            lineHeight = 21.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
            errorMessage?.let { message ->
                Text(
                    message,
                    color = colors.coral,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun QuantityControl(
    quantity: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        QuantityButton(
            icon = Icons.Rounded.Remove,
            description = "Quitar uno",
            onClick = onMinus,
        )
        AnimatedContent(
            targetState = quantity,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInVertically { height -> height } + fadeIn()) togetherWith
                        (slideOutVertically { height -> -height } + fadeOut())
                } else {
                    (slideInVertically { height -> -height } + fadeIn()) togetherWith
                        (slideOutVertically { height -> height } + fadeOut())
                }.using(SizeTransform(clip = false))
            },
            label = "sheet_quantity_ticker",
        ) { count ->
            Text(
                count.toString(),
                color = colors.ink,
                fontSize = 22.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp).widthIn(min = 28.dp),
            )
        }
        QuantityButton(
            icon = Icons.Rounded.Add,
            description = "Agregar uno",
            onClick = onPlus,
        )
    }
}

@Composable
private fun QuantityButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    // Plano y neutro: el único volumen de la hoja es "Agregar".
    val colors = LocalVaiinillaColors.current
    Box(
        modifier =
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.ink.copy(alpha = 0.06f))
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = colors.ink, modifier = Modifier.size(19.dp))
    }
}

private val HeroHeight = 300.dp
private val HaloFade = 96.dp

private fun estimatedTimeLabel(minutes: Int): String =
    when (minutes) {
        in 8..10 -> "8–10 min"
        in 11..15 -> "10–15 min"
        else -> "$minutes min"
    }
