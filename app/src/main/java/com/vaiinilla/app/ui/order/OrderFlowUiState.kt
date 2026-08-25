package com.vaiinilla.app.ui.order

import com.vaiinilla.app.core.text.normalizeForSearch
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.ContractRules
import com.vaiinilla.app.domain.model.GuestVenueContext
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.model.StripePaymentSession
import com.vaiinilla.app.ui.assistant.AssistantChatMessage

data class OrderFlowUiState(
    val loading: Boolean = true,
    val catalog: Catalog? = null,
    val operationalStatus: OperationalStatus? = null,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val selectedCategoryId: Int? = null,
    val selectedProductId: Int? = null,
    val selectedOptionIds: Set<Int> = emptySet(),
    val selectedQuantity: Int = 1,
    val cartLines: List<CartLine> = emptyList(),
    val kitchenNotes: String = "",
    val checkoutDestination: OrderDestination = OrderDestination.TAKE_AWAY,
    val selectedSpaceId: Int = 0,
    val checkoutPayment: PaymentMethod = PaymentMethod.CASH,
    val creatingOrder: Boolean = false,
    val createOrderError: String? = null,
    val createdOrder: OrderDetail? = null,
    val stripeObservedOrder: OrderDetail? = null,
    val stripePendingOrderId: String? = null,
    val stripePaymentSession: StripePaymentSession? = null,
    val stripePresentationKey: String? = null,
    val stripePaymentPhase: StripePaymentPhase = StripePaymentPhase.IDLE,
    val stripePaymentMessage: String? = null,
    val retryingStripePayment: Boolean = false,
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
        val query = searchQuery.normalizeForSearch()
        return if (query.isEmpty()) {
            categoryFiltered
        } else {
            categoryFiltered.filter { product ->
                product.name.normalizeForSearch().contains(query) ||
                    product.description.normalizeForSearch().contains(query)
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
    get() = cartLines.isNotEmpty() && !creatingOrder && !hasUnresolvedStripePayment

val OrderFlowUiState.hasUnresolvedStripePayment: Boolean
    get() = !stripePendingOrderId.isNullOrBlank()

val OrderFlowUiState.requiresOperationalReady: Boolean
    get() =
        checkoutPayment == PaymentMethod.CASH ||
            checkoutPayment == PaymentMethod.BALANCE ||
            checkoutPayment == PaymentMethod.STRIPE

val OrderFlowUiState.canCreateOrder: Boolean
    get() = canSubmitCart && (!requiresOperationalReady || isOperationallyReady)

val OrderFlowUiState.checkoutSpaceId: Int?
    get() =
        if (checkoutDestination == OrderDestination.IN_SPACE) {
            guestVenue?.space?.id
        } else {
            null
        }

val OrderFlowUiState.selectedSpaceName: String
    get() = guestVenue?.space?.name ?: "Escanea el QR de tu mesa"

fun OrderFlowUiState.hasSufficientBalance(walletBalance: Int): Boolean {
    if (checkoutPayment != PaymentMethod.BALANCE) return true
    val total = Money.parse(cartPreviewTotal).toInt()
    return walletBalance >= total
}

fun OperationalStatus.checkoutStaffBlocker(): String? {
    if (acceptingOrders && cashSessionOpen) return null
    if (!cashSessionOpen) {
        return "Caja no tiene sesión abierta. Entra a Caja y ábrela antes de confirmar."
    }
    return when {
        !cashierOnline && !kitchenOnline ->
            "Caja y Cocina no están en línea. Tienen que quedar abiertas en otros dispositivos (o en la web) mientras pides como alumno."
        !cashierOnline ->
            "Caja no está en línea. Déjala abierta en otro dispositivo o en la web."
        !kitchenOnline ->
            "Cocina no está en línea. Déjala abierta en otro dispositivo o en la web."
        else -> "El establecimiento no está recibiendo pedidos en este momento."
    }
}

val OrderFlowUiState.operationalBlockerMessage: String?
    get() {
        if (cartLines.isEmpty() || isOperationallyReady) return null
        val status = operationalStatus ?: return "No pudimos verificar si el establecimiento está recibiendo pedidos."
        return status.checkoutStaffBlocker()
    }

enum class StripePaymentPhase {
    IDLE,
    READY,
    PRESENTING,
    PROCESSING_CONFIRMATION,
    PENDING,
    TIMED_OUT,
    CONFIRMED,
    FAILED,
    CANCELED,
    REFUNDING,
    REFUNDED,
}

internal fun isEstablishmentSwitch(
    current: GuestVenueContext?,
    next: GuestVenueContext,
): Boolean = current?.establishment?.id?.let { it != next.establishment.id } == true
