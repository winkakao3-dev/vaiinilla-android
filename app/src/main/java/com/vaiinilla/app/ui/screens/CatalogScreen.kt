package com.vaiinilla.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.Category
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.ui.components.ActiveOrderBanner
import com.vaiinilla.app.ui.components.EmptyState
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ProductCardSkeleton
import com.vaiinilla.app.ui.components.ProductDetailSheet
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.SwipeToDeleteOrder
import com.vaiinilla.app.ui.components.VaiinillaBottomNavClearance
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.order.OrderFlowUiState
import com.vaiinilla.app.ui.order.cartItemCount
import com.vaiinilla.app.ui.order.filteredProducts
import com.vaiinilla.app.ui.order.isSelectedProductValid
import com.vaiinilla.app.ui.order.selectedProduct
import com.vaiinilla.app.ui.order.selectedProductPreviewPrice
import com.vaiinilla.app.ui.order.selectedProductPreviewTotal
import com.vaiinilla.app.ui.theme.AmoledLimeWash
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.LocalVaiinillaThemeMode
import com.vaiinilla.app.ui.theme.LocalVaiinillaThemeModeChanger
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode

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
    onQuantityChange: (Int) -> Unit,
    onAddProduct: () -> Unit,
    onOpenCart: () -> Unit,
    onOpenTracking: () -> Unit = {},
    onDeleteOrder: (String) -> Unit = {},
    onOpenWallet: () -> Unit = {},
    onChangeVenue: () -> Unit = {},
    onOpenModes: (() -> Unit)? = null,
    profileInitials: String = "?",
    onOpenAccount: () -> Unit = {},
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
                onQuantityChange = onQuantityChange,
                onAddProduct = onAddProduct,
                onOpenCart = {
                    haptics.click()
                    onOpenCart()
                },
                onOpenTracking = onOpenTracking,
                onDeleteOrder = onDeleteOrder,
                onOpenWallet = onOpenWallet,
                onChangeVenue = onChangeVenue,
                onOpenModes = onOpenModes,
                profileInitials = profileInitials,
                onOpenAccount = onOpenAccount,
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
        onOpenCart = {},
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
    onQuantityChange: (Int) -> Unit,
    onAddProduct: () -> Unit,
    onOpenCart: () -> Unit,
    onOpenTracking: () -> Unit,
    onDeleteOrder: (String) -> Unit,
    onOpenWallet: () -> Unit,
    onChangeVenue: () -> Unit,
    onOpenModes: (() -> Unit)?,
    profileInitials: String,
    onOpenAccount: () -> Unit,
) {
    val catalog = requireNotNull(state.catalog)
    val colors = LocalVaiinillaColors.current
    val themeMode = LocalVaiinillaThemeMode.current
    val themeChanger = LocalVaiinillaThemeModeChanger.current
    val focusManager = LocalFocusManager.current

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
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier =
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                contentPadding =
                    PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 16.dp,
                        bottom = VaiinillaBottomNavClearance + 48.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    CatalogHeader(
                        state = state,
                        categories = catalog.categories.sortedBy(Category::order),
                        onSearchChange = onSearchChange,
                        onCategorySelected = onCategorySelected,
                        onOpenCart = onOpenCart,
                        onChangeVenue = onChangeVenue,
                        onOpenModes = onOpenModes,
                        onCycleTheme = { themeChanger?.invoke(themeMode.next()) },
                        profileInitials = profileInitials,
                        onOpenAccount = onOpenAccount,
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

                item(span = { GridItemSpan(maxLineSpan) }) {
                    MenuSectionHead(state = state)
                }

                if (state.filteredProducts.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptySearchState(onClearSearch = { onSearchChange("") })
                    }
                } else {
                    items(state.filteredProducts, key = Product::id) { product ->
                        ProductCard(product = product, onClick = { onProductSelected(product.id) })
                    }
                }
            }
        }

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
                onDismiss = onDismissProduct,
                onToggleOption = onToggleOption,
                onClearOptionalGroup = onClearOptionalGroup,
                onQuantityChange = onQuantityChange,
                onAdd = onAddProduct,
            )
        }
    }
}

@Composable
private fun CatalogHeader(
    state: OrderFlowUiState,
    categories: List<Category>,
    onSearchChange: (String) -> Unit,
    onCategorySelected: (Int?) -> Unit,
    onOpenCart: () -> Unit,
    onChangeVenue: () -> Unit,
    onOpenModes: (() -> Unit)?,
    onCycleTheme: () -> Unit,
    profileInitials: String,
    onOpenAccount: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val venue = state.guestVenue
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Menú de Vaiinilla",
                    color = colors.muted,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                )
                Text(
                    "¿Qué se te antoja?",
                    color = colors.ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                )
                if (venue != null) {
                    val spaceLabel = venue.space?.let { "${it.name} · ${it.type}" }
                    Text(
                        text = spaceLabel ?: venue.establishment.name,
                        color = colors.muted,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            if (venue != null) {
                TextButton(onClick = onChangeVenue) {
                    Text("Cambiar", color = colors.muted, fontWeight = FontWeight.Bold)
                }
            }
            val cartScale by animateFloatAsState(
                targetValue = if (state.cartItemCount > 0) 1.08f else 1.0f,
                animationSpec = spring(dampingRatio = 0.45f, stiffness = 400f),
                label = "cart_bounce",
            )
            Box(modifier = Modifier.scale(cartScale)) {
                IconButton(
                    onClick = onOpenCart,
                    modifier =
                        Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.paper2),
                ) {
                    Icon(Icons.Outlined.ShoppingCart, contentDescription = "Abrir carrito", tint = colors.ink)
                }
                if (state.cartItemCount > 0) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd),
                        color = colors.accent,
                        shape = RoundedCornerShape(99.dp),
                    ) {
                        Text(
                            text = state.cartItemCount.toString(),
                            color = colors.accentInk,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.size(10.dp))
            Box(
                modifier =
                    Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(colors.ink)
                        .combinedClickable(
                            onClick = onOpenAccount,
                            onLongClick = onCycleTheme,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(profileInitials, color = colors.paper, fontWeight = FontWeight.Black)
            }
        }

        Spacer(Modifier.height(16.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.paper2,
            shape = RoundedCornerShape(19.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.muted)
                Spacer(Modifier.size(10.dp))
                BasicTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle =
                        androidx.compose.ui.text
                            .TextStyle(color = colors.ink),
                    decorationBox = { input ->
                        Box {
                            if (state.searchQuery.isBlank()) {
                                Text("Buscar productos…", color = colors.muted)
                            }
                            input()
                        }
                    },
                )
            }
        }

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

        Spacer(Modifier.height(14.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                CategoryChip(
                    label = "Todo",
                    selected = state.selectedCategoryId == null,
                    onClick = { onCategorySelected(null) },
                )
            }
            items(categories, key = Category::id) { category ->
                CategoryChip(
                    label = category.name,
                    selected = state.selectedCategoryId == category.id,
                    onClick = { onCategorySelected(category.id) },
                )
            }
        }
    }
}

@Composable
private fun MenuSectionHead(state: OrderFlowUiState) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text("Menú de hoy", color = colors.ink, fontWeight = FontWeight.Black, fontSize = 19.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            state.operationalStatus?.let { status ->
                Surface(
                    color = if (status.acceptingOrders && status.cashSessionOpen) colors.accent else colors.paper2,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text =
                            if (status.acceptingOrders && status.cashSessionOpen) {
                                "${status.estimatedTimeMinutes} min"
                            } else {
                                "No disponible"
                            },
                        color =
                            if (status.acceptingOrders && status.cashSessionOpen) {
                                colors.accentInk
                            } else {
                                colors.muted
                            },
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(13.dp))
                .background(if (selected) colors.ink else colors.paper2)
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            color = if (selected) colors.paper else colors.ink,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun ProductCard(
    product: Product,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .physicalPress(scale = PhysicalPressScale.ProductCard, onClick = onClick),
        color = colors.paper2,
        shape = RoundedCornerShape(28.dp),
    ) {
        Column {
            ProductImage(
                imageUrl = product.imageUrl,
                contentDescription = product.name,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(170.dp),
            )
            Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 14.dp)) {
                Text(
                    text = product.name,
                    color = colors.ink,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = moneyLabel(product.digitalPrice),
                    color = colors.ink,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 9.dp),
                )
            }
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
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.accentInk),
        ) {
            Text("Reintentar", fontWeight = FontWeight.Black)
        }
    }
}
