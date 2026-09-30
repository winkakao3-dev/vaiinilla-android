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
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.model.PublicSpace
import com.vaiinilla.app.domain.model.StripePaymentSession
import com.vaiinilla.app.domain.model.isStripePaymentConfirmedByBackend
import com.vaiinilla.app.ui.assistant.AssistantChatMessage

data class OrderFlowUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
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
    /** Cancha rentada y en curso del cliente en este negocio: puede recibir ahí su comida. */
    val rentedCourt: PublicSpace? = null,
    val checkoutPayment: PaymentMethod = PaymentMethod.CASH,
    /** Pagar al final: el pedido va a la cuenta del espacio y se paga al irse (en efectivo). */
    val checkoutPayAtEnd: Boolean = false,
    val creatingOrder: Boolean = false,
    val createOrderError: String? = null,
    val createdOrder: OrderDetail? = null,
    val stripeObservedOrder: OrderDetail? = null,
    val stripePendingOrderId: String? = null,
    val resolvingPendingStripePayment: Boolean = false,
    val stripePaymentSession: StripePaymentSession? = null,
    val stripePresentationKey: String? = null,
    val stripePaymentPhase: StripePaymentPhase = StripePaymentPhase.IDLE,
    val stripePaymentMessage: String? = null,
    val retryingStripePayment: Boolean = false,
    val purchaseCelebration: PurchaseCelebration? = null,
    val guestVenue: GuestVenueContext? = null,
    val guestVenueSuspended: Boolean = false,
    val assistantChatMessages: List<AssistantChatMessage> = emptyList(),
)

/** A one-shot visual confirmation for the order currently being created. */
data class PurchaseCelebration(
    val orderId: String,
    val kind: PurchaseCelebrationKind,
)

enum class PurchaseCelebrationKind {
    PAYMENT_CONFIRMED,
    ORDER_RECEIVED,
}

internal fun purchaseCelebrationFor(order: OrderDetail): PurchaseCelebration? =
    when (order.summary.paymentMethod) {
        PaymentMethod.CASH ->
            PurchaseCelebration(
                orderId = order.summary.id,
                kind = PurchaseCelebrationKind.ORDER_RECEIVED,
            )
        PaymentMethod.BALANCE ->
            order
                .takeIf {
                    it.summary.state in
                        setOf(
                            OrderState.PAID,
                            OrderState.PREPARING,
                            OrderState.READY,
                            OrderState.DELIVERED,
                        )
                }?.let {
                    PurchaseCelebration(
                        orderId = it.summary.id,
                        kind = PurchaseCelebrationKind.PAYMENT_CONFIRMED,
                    )
                }
        PaymentMethod.STRIPE ->
            order.takeIf { it.isStripePaymentConfirmedByBackend() }?.let {
                PurchaseCelebration(
                    orderId = it.summary.id,
                    kind = PurchaseCelebrationKind.PAYMENT_CONFIRMED,
                )
            }
    }

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
        // La app sigue la regla del backend: basta con que el negocio reciba pedidos. Si Caja
        // o Cocina están fuera de línea, el backend acepta el pedido y lo deja en su cola.
        operationalStatus?.acceptingOrders == true

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

/** Espacios a los que se puede mandar el pedido: el del QR y, si hay, la cancha rentada. */
val OrderFlowUiState.checkoutSpaces: List<PublicSpace>
    get() = listOfNotNull(guestVenue?.space, rentedCourt).distinctBy { it.id }

/** El espacio elegido, si sigue entre los permitidos; si no, el primero (la cancha rentada va primero). */
val OrderFlowUiState.checkoutSpace: PublicSpace?
    get() = checkoutSpaces.firstOrNull { it.id == selectedSpaceId } ?: rentedCourt ?: guestVenue?.space

val OrderFlowUiState.checkoutSpaceId: Int?
    get() =
        if (checkoutDestination == OrderDestination.IN_SPACE) {
            checkoutSpace?.id
        } else {
            null
        }

val OrderFlowUiState.selectedSpaceName: String
    get() = checkoutSpace?.name ?: "Escanea el QR de tu mesa"

/**
 * Whether the known wallet balance can cover the cart. Unknown or unparsable
 * values stay affordable: the backend remains the authoritative check.
 */
fun OrderFlowUiState.isBalancePaymentAffordable(walletBalance: String?): Boolean {
    val balance = walletBalance?.let { runCatching { Money.parse(it) }.getOrNull() } ?: return true
    val total = runCatching { Money.parse(cartPreviewTotal) }.getOrNull() ?: return true
    return balance >= total
}

/** Client-facing copy: never expose which staff devices are online. */
const val ESTABLISHMENT_CLOSED_MESSAGE =
    "El establecimiento no está abierto en este momento. Verifica que esté abierto y desliza hacia abajo para actualizar."

fun OperationalStatus.checkoutStaffBlocker(): String? = if (acceptingOrders) null else ESTABLISHMENT_CLOSED_MESSAGE

val OrderFlowUiState.operationalBlockerMessage: String?
    get() {
        if (cartLines.isEmpty() || isOperationallyReady) return null
        val status = operationalStatus ?: return "No pudimos verificar si el establecimiento está recibiendo pedidos."
        return status.checkoutStaffBlocker()
    }

/** ¿Se ofrece "Pagar al final"? Solo en un espacio y si el establecimiento lo permite. */
val OrderFlowUiState.canPayAtEnd: Boolean
    get() = checkoutDestination == OrderDestination.IN_SPACE && operationalStatus?.allowsPayAtEnd == true

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
