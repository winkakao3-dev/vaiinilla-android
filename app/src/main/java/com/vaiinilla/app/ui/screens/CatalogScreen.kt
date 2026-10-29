package com.vaiinilla.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.SwipeUp
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.guardianship.restrictionFor
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
import com.vaiinilla.app.ui.components.HeroGlassPill
import com.vaiinilla.app.ui.components.MenuCartShortcut
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ProductCardSkeleton
import com.vaiinilla.app.ui.components.ProductDetailSheet
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.ProfileInitialsButton
import com.vaiinilla.app.ui.components.VaiinillaBottomNavClearance
import com.vaiinilla.app.ui.components.VaiinillaBottomNavSideMargin
import com.vaiinilla.app.ui.components.VaiinillaBottomNavTop
import com.vaiinilla.app.ui.components.VenueLogo
import com.vaiinilla.app.ui.components.WholeWordTitle
import com.vaiinilla.app.ui.components.cremeHeroBrush
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.order.OrderFlowUiState
import com.vaiinilla.app.ui.order.cartItemCount
import com.vaiinilla.app.ui.order.cartPreviewTotal
import com.vaiinilla.app.ui.order.filteredProducts
import com.vaiinilla.app.ui.order.isSelectedProductValid
import com.vaiinilla.app.ui.order.menuNotice
import com.vaiinilla.app.ui.order.pickupTitle
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
    onOpenWallet: () -> Unit = {},
    onChangeVenue: () -> Unit = {},
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
                onOpenWallet = onOpenWallet,
                onChangeVenue = onChangeVenue,
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

@OptIn(ExperimentalMaterial3Api::class)
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
    onOpenWallet: () -> Unit,
    onChangeVenue: () -> Unit,
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
        val gridState = rememberLazyGridState()
        val scope = rememberCoroutineScope()
        val compactBarHeight =
            with(LocalDensity.current) {
                (
                    WindowInsets.statusBars.getTop(this) +
                        CompactBarHeight.roundToPx()
                )
            }
        // La cabecera compacta aparece cuando las categorías de la lista ya pasaron debajo de ella.
        val compactVisible by remember(gridState) {
            derivedStateOf {
                val chips = gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == CATEGORIES_KEY }
                if (chips !=
                    null
                ) {
                    chips.offset.y + chips.size.height < compactBarHeight
                } else {
                    gridState.firstVisibleItemIndex > 0
                }
            }
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
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            start = CatalogSideMargin,
                            end = CatalogSideMargin,
                            top = 0.dp,
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
                            onCycleTheme = { themeChanger?.invoke(themeMode.next()) },
                            profileInitials = profileInitials,
                            onOpenAccount = onOpenAccount,
                            pageColor = colors.paper,
                            onOpenFeed = onOpenFeed,
                        )
                    }
                    state.menuNotice?.let { notice ->
                        item(key = "closed-notice", span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                notice,
                                color = colors.ink,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(colors.paper2)
                                        .border(1.dp, colors.coral.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                        .padding(14.dp),
                            )
                        }
                    }
                    // Al bajar, estas categorías pasan a la cabecera compacta: cambiar de sección no obliga a regresar.
                    item(key = CATEGORIES_KEY, span = { GridItemSpan(maxLineSpan) }) {
                        CategoryChips(
                            categories = sortedCategories,
                            selectedCategoryId = state.selectedCategoryId,
                            onCategorySelected = onCategorySelected,
                            modifier = Modifier.bleedHorizontally(CatalogSideMargin),
                            sideMargin = CatalogSideMargin,
                        )
                    }

                    activeOrder?.let { order ->
                        item(
                            key = "active-order-${order.summary.id}",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            ActiveOrderBanner(
                                folio = order.summary.folio.toString(),
                                statusLabel = pickupTitle(order),
                                itemCount = order.items.sumOf { it.quantity },
                                destination = order.summary.destination.label,
                                modifier = Modifier.padding(bottom = 4.dp),
                                onClick = onOpenTracking,
                            )
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

                    if (catalog.products.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            EmptyMenuState()
                        }
                    } else if (filteredProducts.isEmpty()) {
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

        CompositionLocalProvider(LocalScene provides scene, LocalVaiinillaColors provides shelfColors) {
            CompactMenuBar(
                visible = compactVisible && state.selectedProduct == null,
                state = state,
                categories = sortedCategories,
                onCategorySelected = onCategorySelected,
                onChangeVenue = onChangeVenue,
                onSearch = { scope.launch { gridState.animateScrollToItem(0) } },
                profileInitials = profileInitials,
                onOpenAccount = onOpenAccount,
                background = colors.paper,
            )
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
            ProductDetailSheet(
                product = product,
                categoryName = category?.name.orEmpty(),
                selectedOptionIds = state.selectedOptionIds,
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
    onCycleTheme: () -> Unit,
    profileInitials: String,
    onOpenAccount: () -> Unit,
    pageColor: Color,
    onOpenFeed: (() -> Unit)? = null,
) {
    Column {
        MenuHero(
            state = state,
            onChangeVenue = onChangeVenue,
            onCycleTheme = onCycleTheme,
            profileInitials = profileInitials,
            onOpenAccount = onOpenAccount,
            pageColor = pageColor,
            modifier = Modifier.bleedHorizontally(CatalogSideMargin),
        )
        Spacer(Modifier.height(18.dp))
        EditorialSearchField(
            value = state.searchQuery,
            onValueChange = onSearchChange,
            placeholder = "Buscar productos…",
            seaGlass = true,
        )
        if (onOpenFeed != null) {
            Spacer(Modifier.height(10.dp))
            MenuModeTabs(onOpenFeed = onOpenFeed)
        }
    }
}

/**
 * La foto del negocio a lo ancho, con su logo y su nombre en grande encima; sin foto, su logo sobre el papel,
 * y sin foto ni logo, solo el nombre en un encabezado más bajo. Abajo se funde con [pageColor], el fondo de la
 * pantalla: dentro de la lista el papel es transparente y no sirve para fundir.
 */
@Composable
private fun MenuHero(
    state: OrderFlowUiState,
    onChangeVenue: () -> Unit,
    onCycleTheme: () -> Unit,
    profileInitials: String,
    onOpenAccount: () -> Unit,
    pageColor: Color,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val venue = state.guestVenue
    val establishment = venue?.establishment
    val name = establishment?.name ?: "Menú"
    val photoUrl = establishment?.photoUrl
    val logoUrl = establishment?.logoUrl
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(if (photoUrl != null || logoUrl != null) HeroHeight else HeroHeightNameOnly),
    ) {
        if (photoUrl != null) {
            ProductImage(
                imageUrl = photoUrl,
                contentDescription = "Foto de $name",
                modifier = Modifier.fillMaxSize(),
                placeholder = { m -> HeroLogoFallback(establishment.logoUrl, name, m) },
            )
        } else if (logoUrl != null) {
            HeroLogoFallback(logoUrl, name, Modifier.fillMaxSize())
        }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(cremeHeroBrush(pageColor)),
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = CatalogSideMargin, end = CatalogSideMargin, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (venue != null) {
                HeroGlassPill(label = "Cambiar lugar", icon = Icons.Rounded.Storefront, onClick = onChangeVenue)
            } else {
                Spacer(Modifier.size(1.dp))
            }
            ProfileInitialsButton(profileInitials, onClick = onOpenAccount, onLongClick = onCycleTheme)
        }
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, end = 24.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                VenueLogo(establishment?.logoUrl, name, size = 28.dp)
                venue?.space?.let { space ->
                    Text(
                        text = "${space.name} · ${space.type}",
                        color = colors.ink,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }
            WholeWordTitle(
                text = name.uppercase(),
                color = colors.ink,
                fontSize = 46.sp,
                lineHeight = 44.sp,
                letterSpacing = (-1.2).sp,
                maxLines = 3,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            enabled = venue != null,
                            onClickLabel = "Cambiar de cafetería",
                            onClick = onChangeVenue,
                        ),
            )
        }
    }
}

@Composable
private fun HeroLogoFallback(
    logoUrl: String?,
    name: String,
    modifier: Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Box(modifier = modifier.background(colors.paper2), contentAlignment = Alignment.Center) {
        VenueLogo(logoUrl, name, size = 120.dp, modifier = Modifier.padding(bottom = 90.dp))
    }
}

/** Arriba al bajar: el negocio en chico, la lupa, la cuenta y las categorías, para cambiar de sección sin regresar. */
@Composable
private fun BoxScope.CompactMenuBar(
    visible: Boolean,
    state: OrderFlowUiState,
    categories: List<Category>,
    onCategorySelected: (Int?) -> Unit,
    onChangeVenue: () -> Unit,
    onSearch: () -> Unit,
    profileInitials: String,
    onOpenAccount: () -> Unit,
    background: Color,
) {
    val colors = LocalVaiinillaColors.current
    val establishment = state.guestVenue?.establishment
    val name = establishment?.name ?: "Menú"
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.TopCenter),
        enter =
            fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                slideInVertically(spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)) { -it / 3 },
        exit =
            fadeOut(spring(stiffness = Spring.StiffnessMedium)) +
                slideOutVertically(spring(stiffness = Spring.StiffnessMedium)) { -it / 3 },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(background.copy(alpha = 0.97f))
                    .statusBarsPadding(),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(CompactBarHeight)
                        .padding(horizontal = CatalogSideMargin),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier =
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(
                                enabled = state.guestVenue != null,
                                onClickLabel = "Cambiar de cafetería",
                                onClick = onChangeVenue,
                            ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    VenueLogo(establishment?.logoUrl, name, size = 32.dp)
                    Text(
                        text = name.uppercase(),
                        color = colors.ink,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        letterSpacing = (-0.4).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 10.dp).weight(1f, fill = false),
                    )
                    if (state.guestVenue != null) {
                        Icon(
                            Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = colors.ink,
                            modifier = Modifier.padding(start = 2.dp).size(22.dp),
                        )
                    }
                }
                IconButton(onClick = onSearch) {
                    Icon(Icons.Rounded.Search, contentDescription = "Buscar productos", tint = colors.ink)
                }
                ProfileInitialsButton(profileInitials, onClick = onOpenAccount)
            }
            CategoryChips(
                categories = categories,
                selectedCategoryId = state.selectedCategoryId,
                onCategorySelected = onCategorySelected,
                sideMargin = CatalogSideMargin,
            )
        }
    }
}

private val HeroHeight = 380.dp
private val HeroHeightNameOnly = 250.dp
private val CompactBarHeight = 60.dp
private const val CATEGORIES_KEY = "categories"

private val CatalogSideMargin = 20.dp

/** Ocupa también el margen lateral de la cuadrícula, para que la barra fija llegue de borde a borde. */
private fun Modifier.bleedHorizontally(margin: Dp): Modifier =
    layout { measurable, constraints ->
        val extra = (margin * 2).roundToPx()
        val width = constraints.maxWidth + extra
        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
        layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
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
                height = 48.dp,
                seaGlass = true,
            )
        }
        items(categories, key = Category::id) { category ->
            ChoiceChip(
                label = category.name,
                selected = selectedCategoryId == category.id,
                onClick = { onCategorySelected(category.id) },
                height = 48.dp,
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
    // La foto llena la tarjeta y el nombre y el precio van encima, sobre una sombra que los deja leer.
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.84f)
                .physicalPress(scale = PhysicalPressScale.ProductCard, onClick = onClick)
                .clip(RoundedCornerShape(26.dp))
                .background(colors.paper2),
    ) {
        ProductImage(
            imageUrl = product.imageUrl,
            contentDescription = product.name,
            modifier = Modifier.fillMaxSize(),
            placeholder = { m -> ProductIllustration(kind, m) },
            card = true,
        )
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.45f to Color.Black.copy(alpha = 0.55f),
                            1f to Color.Black.copy(alpha = 0.78f),
                        ),
                    ).padding(start = 14.dp, end = 14.dp, top = 34.dp, bottom = 12.dp),
        ) {
            // Con la letra del sistema al máximo, «Quesabirrias» se partía a media palabra (EQUIPO-30).
            WholeWordTitle(
                text = product.name,
                color = Color.White,
                fontSize = 17.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.sp,
                maxLines = 2,
                minFontSize = 8.sp,
            )
            Text(
                text = moneyLabel(product.digitalPrice),
                color = Color.White.copy(alpha = 0.88f),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
            // Lo agotado sale para todos; el alumno con tutor ve además con candado lo que no se le vende.
            LocalChildControls.current.restrictionFor(product)?.let {
                ChildRestrictionTag(it, Color.White.copy(alpha = 0.88f), Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun EmptyMenuState() {
    EmptyState(
        icon = Icons.Outlined.Restaurant,
        title = "Aún no hay productos",
        message = "Esta cafetería todavía no sube su menú.",
    )
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
        MenuModeIcons(selectedIndex = selected, onSelect = { if (it == 1) open() })
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

/** Menú (cuadrícula) y Desliza (la mano), como pestañas de ícono; una raya se desliza bajo la elegida. */
@Composable
private fun MenuModeIcons(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val tabs = listOf("Menú" to Icons.Rounded.GridView, "Desliza" to Icons.Rounded.SwipeUp)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val tabWidth = maxWidth / tabs.size
        val indicatorX by animateDpAsState(
            targetValue = tabWidth * selectedIndex,
            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
            label = "menu-mode-indicator",
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, (label, icon) ->
                val selected = index == selectedIndex
                val tint by animateColorAsState(
                    targetValue = if (selected) colors.ink else colors.muted,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "menu-mode-tint",
                )
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(52.dp)
                            .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) })
                            .semantics { contentDescription = label },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
                }
            }
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.line),
        )
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = indicatorX)
                    .width(tabWidth)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(colors.ink),
        )
    }
}
