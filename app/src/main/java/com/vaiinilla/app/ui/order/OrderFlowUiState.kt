package com.vaiinilla.app.ui.order

import com.vaiinilla.app.core.config.DataSourceMode
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.ContractRules
import com.vaiinilla.app.domain.model.DemoCheckoutFixtures
import com.vaiinilla.app.domain.model.GuestVenueContext
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.ui.assistant.AssistantChatMessage
import com.vaiinilla.app.ui.assistant.AssistantLocalReplies

data class OrderFlowUiState(
    val loading: Boolean = true,
    val catalog: Catalog? = null,
    val operationalStatus: OperationalStatus? = null,
    val dataSourceMode: DataSourceMode = DataSourceMode.MOCK,
    val testOnlyMode: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val selectedCategoryId: Int? = null,
    val selectedProductId: Int? = null,
    val selectedOptionIds: Set<Int> = emptySet(),
    val selectedQuantity: Int = 1,
    val cartLines: List<CartLine> = emptyList(),
    val kitchenNotes: String = "",
    val checkoutDestination: OrderDestination = OrderDestination.TAKE_AWAY,
    val selectedSpaceId: Int = DemoCheckoutFixtures.DEFAULT_SPACE.id,
    val checkoutPayment: PaymentMethod = PaymentMethod.CASH,
    val creatingOrder: Boolean = false,
    val createOrderError: String? = null,
    val createdOrder: OrderDetail? = null,
    val guestVenue: GuestVenueContext? = null,
    val guestVenueSuspended: Boolean = false,
    val assistantChatMessages: List<AssistantChatMessage> = emptyList(),
)

val OrderFlowUiState.selectedProduct: Product?
    get() = catalog?.products?.firstOrNull { it.id == selectedProductId }

val OrderFlowUiState.filteredProducts: List<Product>
    get() {
        val products = catalog?.products.orEmpty().filter(Product::available)
        val categoryFiltered =
            selectedCategoryId?.let { categoryId ->
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
    get() =
        selectedProduct?.let { product ->
            runCatching { ContractRules.validateSelections(product, selectedOptionIds) }.isSuccess
        } ?: false

val OrderFlowUiState.isOperationallyReady: Boolean
    get() =
        operationalStatus?.let { status ->
            status.acceptingOrders && status.cashSessionOpen
        } == true

val OrderFlowUiState.canSubmitCart: Boolean
    get() = cartLines.isNotEmpty() && !creatingOrder

val OrderFlowUiState.requiresOperationalReady: Boolean
    get() = checkoutPayment == PaymentMethod.CASH

val OrderFlowUiState.canCreateOrder: Boolean
    get() = canSubmitCart && (!requiresOperationalReady || isOperationallyReady)

val OrderFlowUiState.checkoutSpaceId: Int?
    get() =
        if (checkoutDestination == OrderDestination.IN_SPACE) {
            selectedSpaceId
        } else {
            null
        }

val OrderFlowUiState.selectedSpaceName: String
    get() = DemoCheckoutFixtures.spaceForId(selectedSpaceId)?.name ?: DemoCheckoutFixtures.SPACE_NAME

fun OrderFlowUiState.hasSufficientBalance(walletBalance: Int): Boolean {
    if (checkoutPayment != PaymentMethod.BALANCE) return true
    val total = Money.parse(cartPreviewTotal).toInt()
    return walletBalance >= total
}

val OrderFlowUiState.usesStudentCheckout: Boolean
    get() =
        checkoutPayment != PaymentMethod.CASH ||
            checkoutDestination != OrderDestination.TAKE_AWAY

val OrderFlowUiState.operationalBlockerMessage: String?
    get() {
        if (cartLines.isEmpty() || isOperationallyReady) return null
        val status = operationalStatus ?: return "No pudimos verificar si el establecimiento está recibiendo pedidos."
        if (!status.cashSessionOpen) {
            return "Caja no tiene sesión abierta. Entra a Caja y ábrela antes de confirmar."
        }
        if (!status.acceptingOrders) {
            return "No hay Caja o Cocina disponibles. Si usas un solo teléfono, " +
                "vuelve a intentar; la app avisará a ambos roles automáticamente."
        }
        return "El establecimiento no está recibiendo pedidos en este momento."
    }
