package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.domain.model.Category
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.ui.components.ComingSoonSheet
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ProductDetailSheet
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.QuickActionCards
import com.vaiinilla.app.ui.components.StudentTab
import com.vaiinilla.app.ui.components.VaiinillaBottomNav
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.order.OrderFlowUiState
import com.vaiinilla.app.ui.order.cartItemCount
import com.vaiinilla.app.ui.order.filteredProducts
import com.vaiinilla.app.ui.order.isSelectedProductValid
import com.vaiinilla.app.ui.order.selectedProduct
import com.vaiinilla.app.ui.order.selectedProductPreviewPrice
import com.vaiinilla.app.ui.order.selectedProductPreviewTotal
import com.vaiinilla.app.ui.theme.Cream
import com.vaiinilla.app.ui.theme.CreamDeep
import com.vaiinilla.app.ui.theme.Ink
import com.vaiinilla.app.ui.theme.Lime
import com.vaiinilla.app.ui.theme.MutedInk

@Composable
fun CatalogScreen(
    state: OrderFlowUiState,
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
) {
    when {
        state.loading -> LoadingCatalog()
        state.errorMessage != null -> CatalogError(state.errorMessage, onRetry)
        state.catalog != null -> CatalogContent(
            state = state,
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
        )
        else -> CatalogError(
            message = "No pudimos cargar el catálogo.",
            onRetry = onRetry,
        )
    }
}

@Composable
private fun CatalogContent(
    state: OrderFlowUiState,
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
) {
    val catalog = requireNotNull(state.catalog)
    var comingSoonTitle by remember { mutableStateOf<String?>(null) }
    var comingSoonDescription by remember { mutableStateOf<String?>(null) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
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
                    onOpenAssistant = {
                        comingSoonTitle = "Asistente Vaiinilla"
                        comingSoonDescription = "Recomendaciones y chat guiado llegarán en la siguiente fase."
                    },
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                QuickActionCards(
                    modifier = Modifier.padding(top = 18.dp, bottom = 4.dp),
                    onActionClick = { action ->
                        if (action.title == "Asistente") {
                            comingSoonTitle = "Asistente Vaiinilla"
                            comingSoonDescription = "Pregúntale qué pedir cuando el asistente esté disponible."
                        }
                    },
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                MenuSectionHead(state = state)
            }

            if (state.filteredProducts.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptySearchState()
                }
            } else {
                items(state.filteredProducts, key = Product::id) { product ->
                    ProductCard(product = product, onClick = { onProductSelected(product.id) })
                }
            }
        }

        VaiinillaBottomNav(
            activeTab = StudentTab.MENU,
            cartCount = state.cartItemCount,
            onMenu = {},
            onAssistant = {
                comingSoonTitle = "Asistente Vaiinilla"
                comingSoonDescription = "Recomendaciones y chat guiado llegarán en la siguiente fase."
                onOpenAssistant()
            },
            onOrders = onOpenTracking,
            onWallet = {
                comingSoonTitle = "Cartera"
                comingSoonDescription = "Saldo, recargas y stickers digitales llegarán en la siguiente fase."
                onOpenWallet()
            },
            onCart = onOpenCart,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        val title = comingSoonTitle
        val description = comingSoonDescription
        if (title != null && description != null) {
            ComingSoonSheet(
                title = title,
                description = description,
                onDismiss = {
                    comingSoonTitle = null
                    comingSoonDescription = null
                },
            )
        }

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
    onOpenAssistant: () -> Unit = {},
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Hola, Dani", color = MutedInk, fontWeight = FontWeight.ExtraBold)
                Text("¿Qué se te antoja?", color = Ink, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.weight(1f))
            Box {
                IconButton(
                    onClick = onOpenCart,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CreamDeep),
                ) {
                    Icon(Icons.Outlined.ShoppingCart, contentDescription = "Abrir carrito", tint = Ink)
                }
                if (state.cartItemCount > 0) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd),
                        color = Lime,
                        shape = RoundedCornerShape(99.dp),
                    ) {
                        Text(
                            text = state.cartItemCount.toString(),
                            color = Ink,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.size(10.dp))
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Ink),
                contentAlignment = Alignment.Center,
            ) {
                Text("DA", color = Cream, fontWeight = FontWeight.Black)
            }
        }

        Spacer(Modifier.height(16.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CreamDeep,
            shape = RoundedCornerShape(19.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = MutedInk)
                Spacer(Modifier.size(10.dp))
                BasicTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Ink),
                    decorationBox = { input ->
                        Box {
                            if (state.searchQuery.isBlank()) {
                                Text("Buscar burritos, bebidas…", color = MutedInk)
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
private fun MenuSectionHead(state: OrderFlowUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text("Menú de hoy", color = Ink, fontWeight = FontWeight.Black)
        state.operationalStatus?.let { status ->
            Surface(
                color = if (status.acceptingOrders && status.cashSessionOpen) Lime else CreamDeep,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = if (status.acceptingOrders && status.cashSessionOpen) {
                        "${status.estimatedTimeMinutes} min"
                    } else {
                        "No disponible"
                    },
                    color = Ink,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(13.dp))
            .background(if (selected) Ink else CreamDeep)
            .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            color = if (selected) Cream else Ink,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun ProductCard(product: Product, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .physicalPress(scale = PhysicalPressScale.ProductCard, onClick = onClick),
        color = CreamDeep,
        shape = RoundedCornerShape(28.dp),
    ) {
        Column {
            ProductImage(
                imageUrl = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
            )
            Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 14.dp)) {
                Text(
                    text = product.name,
                    color = Ink,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = moneyLabel(product.digitalPrice),
                    color = Ink,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 9.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptySearchState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CreamDeep,
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = MutedInk, modifier = Modifier.size(36.dp))
            Text("No encontramos productos", color = Ink, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 12.dp))
            Text("Prueba con otra búsqueda o categoría.", color = MutedInk, modifier = Modifier.padding(top = 5.dp))
        }
    }
}

@Composable
private fun LoadingCatalog() {
    Box(
        modifier = Modifier.fillMaxSize().background(Cream),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = Lime)
    }
}

@Composable
private fun CatalogError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("No pudimos abrir el menú", color = Ink, fontWeight = FontWeight.Black)
        Text(message, color = MutedInk, modifier = Modifier.padding(top = 8.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink),
        ) {
            Text("Reintentar", fontWeight = FontWeight.Black)
        }
    }
}
