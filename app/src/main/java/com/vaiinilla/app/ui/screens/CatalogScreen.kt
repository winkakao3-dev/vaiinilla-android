package com.vaiinilla.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.Category
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.repository.SpaceAvailability
import com.vaiinilla.app.ui.components.ActiveOrderBanner
import com.vaiinilla.app.ui.components.ChoiceChip
import com.vaiinilla.app.ui.components.CourtsCarousel
import com.vaiinilla.app.ui.components.EditorialAccentButton
import com.vaiinilla.app.ui.components.EditorialSearchField
import com.vaiinilla.app.ui.components.EmptyState
import com.vaiinilla.app.ui.components.MenuCartShortcut
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ProductCardSkeleton
import com.vaiinilla.app.ui.components.ProductDetailSheet
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.ProfileInitialsButton
import com.vaiinilla.app.ui.components.SlidingSegments
import com.vaiinilla.app.ui.components.SwipeToDeleteOrder
import com.vaiinilla.app.ui.components.VaiinillaBottomNavClearance
import com.vaiinilla.app.ui.components.VaiinillaBottomNavSideMargin
import com.vaiinilla.app.ui.components.VaiinillaBottomNavTop
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.order.OrderFlowUiState
import com.vaiinilla.app.ui.order.cartItemCount
import com.vaiinilla.app.ui.order.cartPreviewTotal
import com.vaiinilla.app.ui.order.filteredProducts
import com.vaiinilla.app.ui.order.isSelectedProductValid
import com.vaiinilla.app.ui.order.selectedProduct
import com.vaiinilla.app.ui.order.selectedProductPreviewPrice
import com.vaiinilla.app.ui.order.selectedProductPreviewTotal
import com.vaiinilla.app.ui.order.trayItemKind
import com.vaiinilla.app.ui.parentmode.ChildRestrictionTag
import com.vaiinilla.app.ui.parentmode.LocalChildControls
import com.vaiinilla.app.ui.theme.AmoledLimeWash
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.LocalVaiinillaThemeMode
import com.vaiinilla.app.ui.theme.LocalVaiinillaThemeModeChanger
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    state: OrderFlowUiState,
    activeOrder: OrderDetail? = null,
    onRetry: () -> Unit,
    onSearchChange: (String) -> Unit,
    onCategorySelected: (Int?) -> Unit,
    onProductSelected: (Int) -> Unit,
    onDismissProduct: () -> Unit,
    onToggleOption: (Int, Int) -> Unit,
    onClearOptionalGroup: (Int) -> Unit,
    onSelectedNotesChange: (String) -> Unit = {},
    onQuantityChange: (Int) -> Unit,
    onAddProduct: () -> Unit,
    onOpenTracking: () -> Unit = {},
    onDeleteOrder: (String) -> Unit = {},
    onOpenWallet: () -> Unit = {},
    onChangeVenue: () -> Unit = {},
    onOpenModes: (() -> Unit)? = null,
    profileInitials: String = "?",
    onOpenAccount: () -> Unit = {},
    courts: List<SpaceAvailability> = emptyList(),
    onOpenCourts: (() -> Unit)? = null,
    /** Abre el carrito desde el atajo flotante; sin él, el atajo no aparece. */
    onOpenCart: (() -> Unit)? = null,
    /** Abre "Desliza", el menú como feed de video; sin él no aparecen las pestañas. */
    onOpenFeed: (() -> Unit)? = null,
    /** "Llamar al mesero" sobre la barra (MenuWaiterLayer); sin mesa no llega. Recibe (compacta, visible). */
    waiterLayer: (@Composable (Boolean, Boolean) -> Unit)? = null,
) {
    val haptics = rememberVaiinillaHaptics()
    when {
        state.loading && state.catalog == null -> LoadingCatalog()
        state.guestVenueSuspended && state.errorMessage != null ->
            CatalogSuspendedError(
                message = state.errorMessage,
                onChangeVenue = onChangeVenue,
            )
        state.errorMessage != null && state.catalog == null -> CatalogError(state.errorMessage, onRetry)
        state.catalog != null ->
            CatalogContent(
                state = state,
                activeOrder = activeOrder,
                onRefresh = {
                    haptics.impact()
                    onRetry()
                },
                onSearchChange = onSearchChange,
                onCategorySelected = { catId ->
                    haptics.selection()
                    onCategorySelected(catId)
                },
                onProductSelected = { prodId ->
                    haptics.click()
                    onProductSelected(prodId)
                },
                onDismissProduct = onDismissProduct,
                onToggleOption = onToggleOption,
                onClearOptionalGroup = onClearOptionalGroup,
                onSelectedNotesChange = onSelectedNotesChange,
                onQuantityChange = onQuantityChange,
                onAddProduct = onAddProduct,
                onOpenTracking = onOpenTracking,
                onDeleteOrder = onDeleteOrder,
                onOpenWallet = onOpenWallet,
                onChangeVenue = onChangeVenue,
                onOpenModes = onOpenModes,
                profileInitials = profileInitials,
                onOpenAccount = onOpenAccount,
                courts = courts,
                onOpenCourts = onOpenCourts,
                onOpenCart =
                    onOpenCart?.let { open ->
                        {
                            haptics.click()
                            open()
                        }
                    },
                onOpenFeed = onOpenFeed,
                waiterLayer = waiterLayer,
            )
        else ->
            CatalogError(
                message = "No pudimos cargar el catálogo.",
                onRetry = onRetry,
            )
    }
}

@Preview(
    name = "Menú · claro",
    showBackground = true,
    widthDp = 411,
    heightDp = 891,
)
@Composable
private fun CatalogScreenLightPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        CatalogScreenPreviewContent()
    }
}

@Preview(
    name = "Menú · oscuro",
    showBackground = true,
    widthDp = 411,
    heightDp = 891,
)
@Composable
private fun CatalogScreenDarkPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Dark) {
        CatalogScreenPreviewContent()
    }
}

@Composable
private fun CatalogScreenPreviewContent() {
    val waffle =
        Product(
            id = 1,
            categoryId = 1,
            preparationStation = PreparationStation.KITCHEN,
            name = "Waffle de la casa",
            description = "Waffle recién hecho con fruta y miel.",
            ingredients = "Waffle, fruta, miel",
            allergens = "Gluten, lácteos",
            estimatedTimeMinutes = 12,
            counterPrice = "55.00",
            digitalPrice = "55.00",
            available = true,
            imageUrl = "waffle",
            optionGroups = emptyList(),
        )
    val burrito =
        waffle.copy(
            id = 2,
            categoryId = 2,
            name = "Burrito norteño",
            description = "Burrito de asada con queso y salsa verde.",
            ingredients = "Tortilla, asada, queso, salsa",
            allergens = "Gluten, lácteos",
            counterPrice = "64.00",
            digitalPrice = "64.00",
            imageUrl = "burrito_norteno",
        )
    val jamaica =
        waffle.copy(
            id = 3,
            categoryId = 3,
            name = "Agua de jamaica",
            description = "Agua fresca de jamaica.",
            ingredients = "Jamaica, agua, azúcar",
            allergens = "",
            estimatedTimeMinutes = 2,
            counterPrice = "18.00",
            digitalPrice = "18.00",
            imageUrl = "jamaica",
        )
    val products = listOf(waffle, burrito, jamaica)
    val previewState =
        OrderFlowUiState(
            loading = false,
            catalog =
                Catalog(
                    categories =
                        listOf(
                            Category(id = 1, name = "Desayunos", order = 1),
                            Category(id = 2, name = "Comida", order = 2),
                            Category(id = 3, name = "Bebidas", order = 3),
                        ),
                    products = products,
                    cursor = null,
                ),
            operationalStatus =
                OperationalStatus(
                    acceptingOrders = true,
                    cashSessionOpen = true,
                    cashierOnline = true,
                    kitchenOnline = true,
                    estimatedTimeMinutes = 15,
                    consultedAt = "preview",
                ),
            cartLines = listOf(CartLine(product = burrito, quantity = 1, selectedOptionIds = emptySet())),
        )

    CatalogScreen(
        state = previewState,
        onRetry = {},
        onSearchChange = {},
        onCategorySelected = {},
        onProductSelected = {},
        onDismissProduct = {},
        onToggleOption = { _, _ -> },
        onClearOptionalGroup = {},
        onQuantityChange = {},
        onAddProduct = {},
        onOpenTracking = {},
        onOpenWallet = {},
        onChangeVenue = {},
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CatalogContent(
    state: OrderFlowUiState,
    activeOrder: OrderDetail?,
    onRefresh: () -> Unit,
    onSearchChange: (String) -> Unit,
    onCategorySelected: (Int?) -> Unit,
    onProductSelected: (Int) -> Unit,
    onDismissProduct: () -> Unit,
    onToggleOption: (Int, Int) -> Unit,
    onClearOptionalGroup: (Int) -> Unit,
    onSelectedNotesChange: (String) -> Unit = {},
    onQuantityChange: (Int) -> Unit,
    onAddProduct: () -> Unit,
    onOpenTracking: () -> Unit,
    onDeleteOrder: (String) -> Unit,
    onOpenWallet: () -> Unit,
    onChangeVenue: () -> Unit,
    onOpenModes: (() -> Unit)?,
    profileInitials: String,
    onOpenAccount: () -> Unit,
    courts: List<SpaceAvailability> = emptyList(),
    onOpenCourts: (() -> Unit)? = null,
    /** Abre el carrito desde el atajo flotante; sin él, el atajo no aparece. */
    onOpenCart: (() -> Unit)? = null,
    onOpenFeed: (() -> Unit)? = null,
    waiterLayer: (@Composable (Boolean, Boolean) -> Unit)? = null,
) {
    val catalog = requireNotNull(state.catalog)
    val colors = LocalVaiinillaColors.current
    val themeMode = LocalVaiinillaThemeMode.current
    val themeChanger = LocalVaiinillaThemeModeChanger.current
    val focusManager = LocalFocusManager.current
    val sortedCategories = remember(catalog.categories) { catalog.categories.sortedBy(Category::order) }
    val filteredProducts =
        remember(state.catalog, state.searchQuery, state.selectedCategoryId) {
            state.filteredProducts
        }

    val cartShortcutVisible = onOpenCart != null && state.cartItemCount > 0 && state.selectedProduct == null

    val scene = scenePaletteForTheme()
    val shelfColors =
        colors.copy(
            paper = Color.Transparent,
            solidPaper = colors.onInk,
            paper2 = scene.glassCard,
            line = scene.glassLine,
            ink = scene.ink,
            muted = scene.muted,
        )
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.paper)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                },
    ) {
        if (themeMode == VaiinillaThemeMode.Amoled) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(AmoledLimeWash, colors.paper),
                                radius = 900f,
                            ),
                        ),
            )
        }
        PullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = onRefresh,
            // Con la ficha abierta el menú queda debajo de su velo: TalkBack tampoco debe recorrerlo.
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(if (state.selectedProduct != null) Modifier.clearAndSetSemantics {} else Modifier),
            indicator = {},
        ) {
            CompositionLocalProvider(LocalScene provides scene, LocalVaiinillaColors provides shelfColors) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .statusBarsPadding(),
                    contentPadding =
                        PaddingValues(
                            start = CatalogSideMargin,
                            end = CatalogSideMargin,
                            top = 16.dp,
                            // Con el atajo al carrito encima del dock, el último renglón sube lo que mide.
                            bottom =
                                VaiinillaBottomNavClearance + 48.dp +
                                    if (cartShortcutVisible || waiterLayer != null) 62.dp else 0.dp,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        CatalogHeader(
                            state = state,
                            onSearchChange = onSearchChange,
                            onChangeVenue = onChangeVenue,
                            onOpenModes = onOpenModes,
                            onCycleTheme = { themeChanger?.invoke(themeMode.next()) },
                            profileInitials = profileInitials,
                            onOpenAccount = onOpenAccount,
                            onOpenFeed = onOpenFeed,
                        )
                    }
                    // Las categorías se quedan arriba al bajar por el menú: cambiar de sección no obliga a regresar.
                    stickyHeader(key = "categories") {
                        CategoryChips(
                            categories = sortedCategories,
                            selectedCategoryId = state.selectedCategoryId,
                            onCategorySelected = onCategorySelected,
                            modifier =
                                Modifier
                                    .bleedHorizontally(CatalogSideMargin)
                                    .fadeBelow(colors.paper, 16.dp)
                                    .background(colors.paper),
                            sideMargin = CatalogSideMargin,
                        )
                    }

                    activeOrder?.let { order ->
                        item(
                            key = "active-order-${order.summary.id}",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            SwipeToDeleteOrder(
                                orderFolio = order.summary.folio.toString(),
                                onDelete = { onDeleteOrder(order.summary.id) },
                                modifier = Modifier.padding(bottom = 4.dp),
                            ) {
                                ActiveOrderBanner(
                                    folio = order.summary.folio.toString(),
                                    statusLabel = order.summary.state.label,
                                    itemCount = order.items.sumOf { it.quantity },
                                    destination = order.summary.destination.label,
                                    onClick = onOpenTracking,
                                )
                            }
                        }
                    }

                    // Al buscar solo quedan los resultados: las canchas no se filtran por texto.
                    if (courts.isNotEmpty() && state.searchQuery.isBlank()) {
                        item(key = "courts-banner", span = { GridItemSpan(maxLineSpan) }) {
                            CourtsCarousel(
                                courts = courts,
                                bleed = CatalogSideMargin,
                                onOpen = onOpenCourts,
                                // Con el espacio de la cuadrícula (12) queda el mismo hueco de 14 que entre columnas.
                                modifier = Modifier.animateItem().padding(bottom = 2.dp),
                            )
                        }
                    }

                    if (filteredProducts.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            EmptySearchState(onClearSearch = { onSearchChange("") })
                        }
                    } else {
                        items(filteredProducts, key = Product::id) { product ->
                            Box(modifier = Modifier.animateItem().fillMaxWidth()) {
                                ProductCard(product = product, onClick = { onProductSelected(product.id) })
                            }
                        }
                    }
                }
            }
        }

        MenuCartShortcut(
            count = state.cartItemCount,
            total = moneyLabel(state.cartPreviewTotal),
            visible = cartShortcutVisible,
            onClick = { onOpenCart?.invoke() },
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = VaiinillaBottomNavSideMargin, bottom = VaiinillaBottomNavTop + 10.dp),
        )

        // Se esconde con la ficha de un platillo abierta, como "Pedir ya"; en Pedidos queda el botón de la tarjeta.
        waiterLayer?.invoke(cartShortcutVisible, state.selectedProduct == null)

        state.selectedProduct?.let { product ->
            val category = catalog.categories.firstOrNull { it.id == product.categoryId }
            val defaultOptionIds =
                product.optionGroups
                    .flatMap { group ->
                        group.options.take(group.minimumSelections).map { it.id }
                    }.toSet()
            ProductDetailSheet(
                product = product,
                categoryName = category?.name.orEmpty(),
                selectedOptionIds = state.selectedOptionIds,
                defaultOptionIds = defaultOptionIds,
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
                onAdd = onAddProduct,
            )
        }
    }
}

@Composable
private fun CatalogHeader(
    state: OrderFlowUiState,
    onSearchChange: (String) -> Unit,
    onChangeVenue: () -> Unit,
    onOpenModes: (() -> Unit)?,
    onCycleTheme: () -> Unit,
    profileInitials: String,
    onOpenAccount: () -> Unit,
    onOpenFeed: (() -> Unit)? = null,
) {
    val colors = LocalVaiinillaColors.current
    val venue = state.guestVenue
    Column {
        // Un solo título: el lugar donde estás pidiendo, que también sirve para cambiarlo (como Wonder).
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(
                            enabled = venue != null,
                            onClickLabel = "Cambiar de cafetería",
                            onClick = onChangeVenue,
                        ),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = venue?.establishment?.name ?: "Menú",
                        color = colors.ink,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        lineHeight = 28.sp,
                        letterSpacing = (-0.6).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (venue != null) {
                        Icon(
                            Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = colors.ink,
                            modifier = Modifier.padding(start = 2.dp).size(22.dp),
                        )
                    }
                }
                venue?.space?.let { space ->
                    Text(
                        text = "${space.name} · ${space.type}",
                        color = colors.muted,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.size(10.dp))
            ProfileInitialsButton(profileInitials, onClick = onOpenAccount, onLongClick = onCycleTheme)
        }

        if (onOpenFeed != null) {
            Spacer(Modifier.height(14.dp))
            MenuModeTabs(onOpenFeed = onOpenFeed)
        }

        Spacer(Modifier.height(16.dp))
        EditorialSearchField(
            value = state.searchQuery,
            onValueChange = onSearchChange,
            placeholder = "Buscar productos…",
            seaGlass = true,
        )

        if (onOpenModes != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = onOpenModes,
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    Text("Cambiar modo", color = colors.muted, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private val CatalogSideMargin = 20.dp

/** Ocupa también el margen lateral de la cuadrícula, para que la barra fija llegue de borde a borde. */
private fun Modifier.bleedHorizontally(margin: Dp): Modifier =
    layout { measurable, constraints ->
        val extra = (margin * 2).roundToPx()
        val width = constraints.maxWidth + extra
        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
        layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
    }

/** Un degradado del color de fondo bajo la barra: los productos se desvanecen al pasar, sin un corte. */
private fun Modifier.fadeBelow(
    color: Color,
    height: Dp,
): Modifier =
    drawWithContent {
        drawContent()
        drawRect(
            brush =
                Brush.verticalGradient(
                    listOf(color, color.copy(alpha = 0f)),
                    startY = size.height,
                    endY =
                        size.height + height.toPx(),
                ),
            topLeft = Offset(0f, size.height),
            size = Size(size.width, height.toPx()),
        )
    }

@Composable
private fun CategoryChips(
    categories: List<Category>,
    selectedCategoryId: Int?,
    onCategorySelected: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    sideMargin: Dp = 0.dp,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 10.dp, horizontal = sideMargin + 2.dp),
    ) {
        item {
            ChoiceChip(
                label = "Todo",
                selected = selectedCategoryId == null,
                onClick = { onCategorySelected(null) },
                seaGlass = true,
            )
        }
        items(categories, key = Category::id) { category ->
            ChoiceChip(
                label = category.name,
                selected = selectedCategoryId == category.id,
                onClick = { onCategorySelected(category.id) },
                seaGlass = true,
            )
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val kind = remember(product.name) { trayItemKind(product.name) }
    // Como antes del vidrio (y como iOS): la foto redondeada sobre el papel y, debajo, el nombre y el precio.
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
                .physicalPress(scale = PhysicalPressScale.ProductCard, onClick = onClick),
    ) {
        ProductImage(
            imageUrl = product.imageUrl,
            contentDescription = product.name,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.paper2),
            placeholder = { m -> ProductIllustration(kind, m) },
        )
        Text(
            text = product.name,
            color = colors.ink,
            fontWeight = FontWeight.Black,
            fontSize = 17.sp,
            lineHeight = 21.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 2.dp, top = 12.dp, end = 2.dp),
        )
        Text(
            text = moneyLabel(product.digitalPrice),
            color = colors.muted,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(start = 2.dp, top = 4.dp, end = 2.dp),
        )
        // El alumno con tutor ve con candado lo que no se le vende, antes de abrirlo.
        LocalChildControls.current?.restriction(product.id)?.let {
            ChildRestrictionTag(it, colors.muted, Modifier.padding(start = 2.dp, top = 4.dp, end = 2.dp))
        }
    }
}

@Composable
private fun EmptySearchState(onClearSearch: () -> Unit) {
    EmptyState(
        icon = Icons.Outlined.Search,
        title = "No encontramos eso",
        message = "Prueba otra palabra o categoría.",
        actionLabel = "Limpiar búsqueda",
        onAction = onClearSearch,
        quietAction = true,
    )
}

@Composable
private fun LoadingCatalog() {
    val colors = LocalVaiinillaColors.current
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.paper)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(0.35f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.paper2),
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(0.6f)
                            .height(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.paper2),
                )
            }
            Box(
                modifier =
                    Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.paper2),
            )
        }
        Spacer(Modifier.height(20.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(6) {
                ProductCardSkeleton()
            }
        }
    }
}

@Composable
private fun CatalogSuspendedError(
    message: String,
    onChangeVenue: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.paper)
                .statusBarsPadding()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Cafetería suspendida", color = colors.ink, fontWeight = FontWeight.Black)
        Text(message, color = colors.muted, modifier = Modifier.padding(top = 8.dp))
        Button(
            onClick = onChangeVenue,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.accentInk),
        ) {
            Text("Elegir otra cafetería", fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun CatalogError(
    message: String,
    onRetry: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.paper)
                .statusBarsPadding()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("No pudimos abrir el menú", color = colors.ink, fontWeight = FontWeight.Black)
        Text(message, color = colors.muted, modifier = Modifier.padding(top = 8.dp))
        EditorialAccentButton(text = "Reintentar", onClick = onRetry, modifier = Modifier.padding(top = 20.dp))
    }
}

private const val MENU_PREFS = "vaiinilla_menu"
private const val FEED_INVITE_SEEN = "feed_invite_seen"

/**
 * Las dos formas de ver el menú: la cuadrícula de siempre y "Desliza", el feed de un platillo por pantalla. La
 * primera vez una nota invita a probarlo; se va al abrirlo o al cerrarla.
 */
@Composable
private fun MenuModeTabs(onOpenFeed: () -> Unit) {
    val colors = LocalVaiinillaColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember(context) { context.getSharedPreferences(MENU_PREFS, android.content.Context.MODE_PRIVATE) }
    var inviteVisible by remember { mutableStateOf(!prefs.getBoolean(FEED_INVITE_SEEN, false)) }
    var selected by remember { mutableIntStateOf(0) }

    fun open() {
        prefs.edit().putBoolean(FEED_INVITE_SEEN, true).apply()
        inviteVisible = false
        selected = 1
        scope.launch {
            // Primero se desliza la pestaña; luego se abre el feed y, al volver, la pestaña regresa a "Menú".
            delay(200)
            onOpenFeed()
            delay(400)
            selected = 0
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SlidingSegments(
            labels = listOf("Menú", "Desliza"),
            selectedIndex = selected,
            onSelect = { if (it == 1) open() },
            trackColor = colors.paper2,
            indicatorColor = colors.ink,
            selectedContentColor = colors.onInk,
            idleContentColor = colors.muted,
            borderColor = colors.line,
            verticalPadding = 9.dp,
        )
        AnimatedVisibility(visible = inviteVisible) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.paper2)
                        .border(1.dp, colors.line, RoundedCornerShape(18.dp))
                        .padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Nuevo: mira el menú deslizando, un platillo a la vez, como en TikTok.",
                    color = colors.ink,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = ::open) { Text("Probar", color = colors.ink, fontWeight = FontWeight.Black) }
                IconButton(onClick = {
                    prefs.edit().putBoolean(FEED_INVITE_SEEN, true).apply()
                    inviteVisible = false
                }) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Cerrar aviso",
                        tint = colors.muted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
