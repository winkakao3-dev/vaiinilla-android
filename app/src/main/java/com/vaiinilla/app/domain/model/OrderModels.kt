package com.vaiinilla.app.domain.model

data class CartLine(
    val product: Product,
    val quantity: Int,
    val selectedOptionIds: Set<Int>,
) {
    val key: String =
        buildString {
            append(product.id)
            append(':')
            append(selectedOptionIds.sorted().joinToString(","))
        }
}

data class CreateOrderRequest(
    val paymentMethod: PaymentMethod,
    val destination: OrderDestination,
    val spaceId: Int?,
    val kitchenNotes: String,
    val items: List<CreateOrderItem>,
)

data class CreateOrderItem(
    val productId: Int,
    val quantity: Int,
    val optionIds: List<Int>,
)

data class CreatedOrder(
    val order: OrderDetail,
    val stripeSession: StripePaymentSession? = null,
) {
    val summary: OrderSummary
        get() = order.summary
    val pickupToken: String?
        get() = order.pickupToken
}

data class OrderDetail(
    val summary: OrderSummary,
    val user: OrderUser?,
    val kitchenNotes: String,
    val items: List<OrderItem>,
    val pickupToken: String? = null,
    val payment: OrderPayment? = null,
)

fun OrderDetail.isStripePaymentConfirmedByBackend(): Boolean =
    summary.paymentMethod == PaymentMethod.STRIPE &&
        payment?.status == StripePaymentStatus.CONFIRMED &&
        summary.state in
        setOf(
            OrderState.PAID,
            OrderState.PREPARING,
            OrderState.READY,
            OrderState.DELIVERED,
        )

data class OrderSummary(
    val id: String,
    val folio: Int,
    val operationalDate: String,
    val state: OrderState,
    val paymentMethod: PaymentMethod,
    val destination: OrderDestination,
    val space: OrderSpace?,
    val subtotal: String,
    val combinedSavings: String,
    val cashbackAwarded: String,
    val total: String,
    val version: Int,
    val createdAt: String,
    val updatedAt: String,
)

data class OrderPayment(
    val paymentAttemptId: String,
    val paymentIntentId: String,
    val stripeAccountId: String,
    val status: StripePaymentStatus,
)

/** Ephemeral Stripe credentials returned only by create/retry endpoints. Never persist or log this object. */
data class StripePaymentSession(
    val paymentAttemptId: String,
    val paymentIntentId: String,
    val clientSecret: String,
    val stripeAccountId: String,
    val publishableKey: String,
    val status: StripePaymentStatus,
)

data class OrderUser(
    val name: String,
    val enrollment: String,
)

data class OrderSpace(
    val id: Int,
    val name: String,
    val type: String,
)

data class OrderItem(
    val id: Int,
    val productId: Int,
    val productName: String,
    val preparationStation: PreparationStation,
    val quantity: Int,
    val unitDigitalPrice: String,
    val subtotal: String,
    val options: List<OrderItemOption>,
    val unitCollectionPrice: String? = null,
)

data class OrderItemOption(
    val optionId: Int,
    val name: String,
    val extraPrice: String,
)

enum class PaymentMethod(
    val wireValue: String,
    val label: String,
) {
    CASH("efectivo", "Efectivo"),
    BALANCE("saldo", "Saldo"),
    STRIPE("stripe", "Tarjeta"),
    ;

    companion object {
        fun fromWireValue(value: String): PaymentMethod =
            entries.firstOrNull {
                it.wireValue == value
            } ?: throw IllegalArgumentException("metodo_pago no soportado: $value")
    }
}

enum class StripePaymentStatus(
    val wireValue: String,
    val label: String,
) {
    PENDING("pendiente_pago", "Pendiente"),
    PROCESSING("processing", "Pago en proceso"),
    REQUIRES_ACTION("requires_action", "Acción requerida"),
    CONFIRMED("confirmado", "Confirmado"),
    FAILED("fallido", "Fallido"),
    CANCELED("cancelado", "Cancelado"),
    REFUND_PENDING("pendiente_reembolso", "Reembolso pendiente"),
    REFUNDING("reembolsando", "Reembolsando"),
    REFUNDED("reembolsado", "Reembolsado"),
    ;

    val canRetry: Boolean
        get() = this == FAILED || this == CANCELED

    val isAwaitingConfirmation: Boolean
        get() = this == PENDING || this == PROCESSING || this == REQUIRES_ACTION

    companion object {
        fun fromWireValue(value: String): StripePaymentStatus =
            entries.firstOrNull { it.wireValue == value }
                ?: throw IllegalArgumentException("payment_status no soportado: $value")
    }
}

enum class OrderDestination(
    val wireValue: String,
    val label: String,
) {
    TAKE_AWAY("para_llevar", "Para llevar"),
    IN_SPACE("en_espacio", "En espacio"),
    ;

    companion object {
        fun fromWireValue(value: String): OrderDestination =
            entries.firstOrNull {
                it.wireValue == value
            } ?: throw IllegalArgumentException("destino no soportado: $value")
    }
}

enum class OrderState(
    val wireValue: String,
    val label: String,
) {
    PENDING_PAYMENT("por_cobrar", "Por cobrar"),
    PAID("cobrado", "Cobrado"),
    PREPARING("preparando", "Preparando"),
    READY("listo", "Listo"),
    DELIVERED("entregado", "Entregado"),
    CANCELED("cancelado", "Cancelado"),
    NOT_PICKED_UP("no_recogido", "No recogido"),
    EXPIRED("expirado", "Expirado"),
    ;

    val trackingIndex: Int
        get() = trackingFlow.indexOf(this)

    val isTerminalWithoutDelivery: Boolean
        get() = this == CANCELED || this == NOT_PICKED_UP || this == EXPIRED

    companion object {
        val trackingFlow =
            listOf(
                PENDING_PAYMENT,
                PAID,
                PREPARING,
                READY,
                DELIVERED,
            )

        fun fromWireValue(value: String): OrderState =
            entries.firstOrNull {
                it.wireValue == value
            } ?: throw IllegalArgumentException("estado no soportado: $value")
    }
}
