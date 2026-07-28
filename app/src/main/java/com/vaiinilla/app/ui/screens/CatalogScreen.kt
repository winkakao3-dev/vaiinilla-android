package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.domain.model.Category
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.ui.components.ActiveOrderBanner
import com.vaiinilla.app.ui.components.DemoEmptyState
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ProductDetailSheet
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.QuickActionCards
import com.vaiinilla.app.ui.components.StudentTab
import com.vaiinilla.app.ui.components.VaiinillaBottomNav
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
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
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode

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
    onOpenAssistant: () -> Unit = {},
    onOpenWallet: () -> Unit = {},
    onChangeVenue: () -> Unit = {},
    showDemoTabs: Boolean = false,
) {
    when {
        state.loading -> LoadingCatalog()
        state.errorMessage != null -> CatalogError(state.errorMessage, onRetry)
        state.catalog != null ->
            CatalogContent(
                state = state,
                activeOrder = activeOrder,
                onSearchChange = onSearchChange,
                onCategorySelected = onCategorySelected,
                onProductSelected = onProductSelected,
                onDismissProduct = onDismissProduct,
                onToggleOption = onToggleOption,
                onClearOptionalGroup = onClearOptionalGroup,
                onQuantityChange = onQuantityChange,
                onAddProduct = onAddProduct,
                onOpenCart = onOpenCart,
                onOpenTracking = onOpenTracking,
                onOpenAssistant = onOpenAssistant,
                onOpenWallet = onOpenWallet,
                onChangeVenue = onChangeVenue,
                showDemoTabs = showDemoTabs,
            )
        else ->
            CatalogError(
                message = "No pudimos cargar el catálogo.",
                onRetry = onRetry,
            )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CatalogContent(
    state: OrderFlowUiState,
    activeOrder: OrderDetail?,
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
    onOpenAssistant: () -> Unit,
    onOpenWallet: () -> Unit,
    onChangeVenue: () -> Unit,
    showDemoTabs: Boolean,
) {
    val catalog = requireNotNull(state.catalog)
    val colors = LocalVaiinillaColors.current
    val themeMode = LocalVaiinillaThemeMode.current
    val themeChanger = LocalVaiinillaThemeModeChanger.current
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.paper),
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
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 132.dp),
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
                    onCycleTheme = { themeChanger?.invoke(themeMode.next()) },
                )
            }

            activeOrder?.let { order ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ActiveOrderBanner(
                        folio = order.summary.folio.toString(),
                        statusLabel = order.summary.state.label,
                        itemCount = order.items.sumOf { it.quantity },
                        destination = order.summary.destination.label,
                        onClick = onOpenTracking,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                if (showDemoTabs) {
                    QuickActionCards(
                        modifier = Modifier.padding(top = 18.dp, bottom = 4.dp),
                        onActionClick = { action ->
                            if (action.title == "Asistente") {
                                onOpenAssistant()
                            }
                        },
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                MenuSectionHead(
                    state = state,
                    showAssistantShortcut = showDemoTabs,
                    onOpenAssistant = onOpenAssistant,
                )
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

        VaiinillaBottomNav(
            showDemoTabs = showDemoTabs,
            activeTab = StudentTab.MENU,
            cartCount = state.cartItemCount,
            onMenu = {},
            onAssistant = onOpenAssistant,
            onOrders = onOpenTracking,
            onWallet = onOpenWallet,
            onCart = onOpenCart,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

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
    onCycleTheme: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val venue = state.guestVenue
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (venue != null) {
                    Text(
                        venue.establishment.name,
                        color = colors.ink,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val spaceLabel = venue.space?.let { "${it.name} · ${it.type}" }
                    Text(
                        spaceLabel ?: "Pedido sin mesa asignada",
                        color = colors.muted,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text("Hola, Dani", color = colors.muted, fontWeight = FontWeight.ExtraBold)
                    Text("¿Qué se te antoja?", color = colors.ink, fontWeight = FontWeight.Black)
                }
            }
            if (venue != null) {
                TextButton(onClick = onChangeVenue) {
                    Text("Cambiar", color = colors.muted, fontWeight = FontWeight.Bold)
                }
            }
            Box {
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
                            onClick = {},
                            onLongClick = onCycleTheme,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text("DA", color = colors.paper, fontWeight = FontWeight.Black)
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
                                Text("Buscar burritos, bebidas…", color = colors.muted)
                            }
                            input()
                        }
                    },
                )
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
private fun MenuSectionHead(
    state: OrderFlowUiState,
    showAssistantShortcut: Boolean,
    onOpenAssistant: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text("Menú de hoy", color = colors.ink, fontWeight = FontWeight.Black)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showAssistantShortcut) {
                TextButton(onClick = onOpenAssistant) {
                    Text("No sé qué pedir", color = colors.muted, fontWeight = FontWeight.Bold)
                }
            }
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
                        color = colors.accentInk,
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
    DemoEmptyState(
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
    Box(
        modifier = Modifier.fillMaxSize().background(colors.paper),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = colors.accent)
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
