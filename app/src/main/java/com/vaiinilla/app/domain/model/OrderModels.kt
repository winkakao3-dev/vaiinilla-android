package com.vaiinilla.app.domain.model

data class CartLine(
    val product: Product,
    val quantity: Int,
    val selectedOptionIds: Set<Int>,
) {
    val key: String = buildString {
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

data class OrderDetail(
    val summary: OrderSummary,
    val user: OrderUser?,
    val kitchenNotes: String,
    val items: List<OrderItem>,
    val pickupToken: String? = null,
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
)

data class OrderItemOption(
    val optionId: Int,
    val name: String,
    val extraPrice: String,
)

enum class PaymentMethod(val wireValue: String) {
    CASH("efectivo");

    companion object {
        fun fromWireValue(value: String): PaymentMethod = entries.firstOrNull {
            it.wireValue == value
        } ?: throw IllegalArgumentException("metodo_pago no soportado: $value")
    }
}

enum class OrderDestination(val wireValue: String, val label: String) {
    TAKE_AWAY("para_llevar", "Para llevar"),
    IN_SPACE("en_espacio", "En espacio"),
    ;

    companion object {
        fun fromWireValue(value: String): OrderDestination = entries.firstOrNull {
            it.wireValue == value
        } ?: throw IllegalArgumentException("destino no soportado: $value")
    }
}

enum class OrderState(val wireValue: String, val label: String) {
    PENDING_PAYMENT("por_cobrar", "Por cobrar"),
    PAID("cobrado", "Cobrado"),
    PREPARING("preparando", "Preparando"),
    READY("listo", "Listo"),
    DELIVERED("entregado", "Entregado"),
    ;

    val trackingIndex: Int
        get() = when (this) {
            PENDING_PAYMENT -> 0
            PAID -> 1
            PREPARING -> 2
            READY -> 3
            DELIVERED -> 4
        }

    companion object {
        val trackingFlow = listOf(
            PENDING_PAYMENT,
            PAID,
            PREPARING,
            READY,
            DELIVERED,
        )

        fun fromWireValue(value: String): OrderState = entries.firstOrNull {
            it.wireValue == value
        } ?: throw IllegalArgumentException("estado no soportado: $value")
    }
}
