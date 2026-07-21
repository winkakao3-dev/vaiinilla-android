package com.vaiinilla.app.ui.order

import com.vaiinilla.app.core.config.DataSourceMode
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.ContractRules
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.Product

data class OrderFlowUiState(
    val loading: Boolean = true,
    val catalog: Catalog? = null,
    val operationalStatus: OperationalStatus? = null,
    val dataSourceMode: DataSourceMode = DataSourceMode.MOCK,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val selectedCategoryId: Int? = null,
    val selectedProductId: Int? = null,
    val selectedOptionIds: Set<Int> = emptySet(),
    val selectedQuantity: Int = 1,
    val cartLines: List<CartLine> = emptyList(),
    val kitchenNotes: String = "",
    val creatingOrder: Boolean = false,
    val createOrderError: String? = null,
    val createdOrder: OrderDetail? = null,
)

val OrderFlowUiState.selectedProduct: Product?
    get() = catalog?.products?.firstOrNull { it.id == selectedProductId }

val OrderFlowUiState.filteredProducts: List<Product>
    get() {
        val products = catalog?.products.orEmpty().filter(Product::available)
        val categoryFiltered = selectedCategoryId?.let { categoryId ->
            products.filter { it.categoryId == categoryId }
        } ?: products
        val query = searchQuery.trim()
        return if (query.isEmpty()) {
            categoryFiltered
        } else {
            categoryFiltered.filter { product ->
                product.name.contains(query, ignoreCase = true) ||
                    product.description.contains(query, ignoreCase = true)
            }
        }
    }

val OrderFlowUiState.cartItemCount: Int
    get() = cartLines.sumOf(CartLine::quantity)

val OrderFlowUiState.cartPreviewTotal: String
    get() = Money.cartPreview(cartLines)

val OrderFlowUiState.selectedProductPreviewPrice: String
    get() = selectedProduct?.let { Money.productUnitPreview(it, selectedOptionIds) } ?: "0.00"

val OrderFlowUiState.selectedProductPreviewTotal: String
    get() = Money.format(Money.parse(selectedProductPreviewPrice) * selectedQuantity.toBigDecimal())

val OrderFlowUiState.isSelectedProductValid: Boolean
    get() = selectedProduct?.let { product ->
        runCatching { ContractRules.validateSelections(product, selectedOptionIds) }.isSuccess
    } ?: false

val OrderFlowUiState.canCreateOrder: Boolean
    get() = cartLines.isNotEmpty() && operationalStatus?.let { status ->
        status.acceptingOrders && status.cashSessionOpen
    } == true && !creatingOrder
