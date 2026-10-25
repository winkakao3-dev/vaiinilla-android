package com.vaiinilla.app.domain.model

import java.math.BigDecimal
import java.time.Instant

/** Largo máximo de la nota de una línea; la nota general de cocina usa el mismo límite en el carrito. */
const val CART_LINE_NOTE_MAX = 90

data class CartLine(
    val product: Product,
    val quantity: Int,
    val selectedOptionIds: Set<Int>,
    /** Igual nota consolida la línea; una nota distinta la separa. Viene recortada. */
    val notes: String = "",
) {
    // La nota va al final y solo si existe: una clave sin nota queda igual que antes, y las opciones
    // no llevan ":", así que la clave no es ambigua.
    val key: String =
        buildString {
            append(product.id)
            append(':')
            append(selectedOptionIds.sorted().joinToString(","))
            if (notes.isNotEmpty()) {
                append(':')
                append(notes)
            }
        }
}

data class CreateOrderRequest(
    val paymentMethod: PaymentMethod,
    val destination: OrderDestination,
    val spaceId: Int?,
    /** Lo que llega a cocina a nivel pedido: la nota general más el resumen por línea del caso de uso. */
    val kitchenNotes: String,
    val items: List<CreateOrderItem>,
    /** Pagar al final: el pedido va a la cuenta del espacio (efectivo, en un espacio). */
    val payAtEnd: Boolean = false,
    /** `MOSTRADOR` solo lo envía el staff al tomar un pedido en mostrador. */
    val origin: OrderOrigin = OrderOrigin.APP,
    /** Nombre del cliente de mostrador; opcional. Vacío o blanco no se envía. */
    val customerName: String? = null,
)

enum class OrderOrigin(
    val wireValue: String,
) {
    APP("app"),
    MOSTRADOR("mostrador"),
}

data class CreateOrderItem(
    val productId: Int,
    val quantity: Int,
    val optionIds: List<Int>,
    val notes: String = "",
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
    /** Solo en un pedido de renta de cancha: no se prepara ni se entrega, se juega. */
    val reservation: OrderReservation? = null,
    /** Cuándo el cliente avisó que ya llegó por su pedido (drive-thru); `null` si no ha avisado. */
    val arrivedAt: Instant? = null,
    /** Nombre a cuyo nombre lo tomó el staff en mostrador; `null` en pedidos de la app. */
    val customerName: String? = null,
    /** Qué hizo el servidor con el dinero; solo viene en la respuesta de cancelar o rechazar (CAJA-06). */
    val refund: RefundOutcome? = null,
)

/** Cómo quedó el dinero de un pedido o artículo pagado que se canceló: lo que Caja debe hacer o ya no. */
data class RefundOutcome(
    /** Salió del cajón: Caja lo entrega en mano. */
    val cashHandedBack: BigDecimal? = null,
    /** Quedó como devolución pendiente en Caja. */
    val pendingForCashier: BigDecimal? = null,
    val returnedToBalance: BigDecimal? = null,
    val cardRefund: Boolean = false,
)

data class OrderReservation(
    val id: String,
    val space: OrderSpace?,
    val start: Instant,
    val end: Instant,
    val durationMinutes: Int,
    val state: ReservationState,
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
    /** El pedido va a la cuenta del espacio y se paga al final, en efectivo. */
    val payAtEnd: Boolean = false,
    /** Pedido a la cuenta que aún no se cobra (y sigue vigente). */
    val paymentPending: Boolean = false,
    /** Motivo que indicó quien canceló o rechazó el pedido; solo si está cancelado. */
    val cancelReason: String? = null,
    val canceledByRole: String? = null,
    /** Sesión de mesa a la que pertenece el pedido; `null` si va para llevar o no tiene sesión. */
    val tableSessionId: String? = null,
    /** Se entrega sin escanear el QR de recogida (`entrega_sin_qr`, pedidos a una cuenta abierta). */
    val deliveryWithoutQr: Boolean = false,
    /** Lo que el servidor ya registró como pagado; `null` en un servidor que no lo manda. */
    val amountPaid: String? = null,
    /** Lo que aún falta cobrar (`saldo_pendiente`); `null` en un servidor que no lo manda. */
    val balanceDue: String? = null,
    /** Día (AAAA-MM-DD) y franja (HH:MM-HH:MM) de un pedido apartado para otro día; `null` si es para ahora. */
    val deliveryDate: String? = null,
    val deliverySlot: String? = null,
)

/**
 * Efectivo que Caja todavía tiene que cobrar. Cocina puede arrancar un pedido en efectivo antes del cobro, así
 * que el saldo pendiente sigue existiendo en preparando y en listo: el estado solo no lo dice.
 * Un servidor que no manda `saldo_pendiente` solo deja pendiente al pedido por cobrar.
 */
val OrderSummary.needsCashCollection: Boolean
    get() {
        if (paymentMethod != PaymentMethod.CASH || payAtEnd) return false
        if (state != OrderState.PENDING_PAYMENT && state != OrderState.PREPARING && state != OrderState.READY) {
            return false
        }
        val due = balanceDue?.let { runCatching { BigDecimal(it) }.getOrNull() }
        return if (due != null) due.signum() > 0 else state == OrderState.PENDING_PAYMENT
    }

/** Lo que Caja cobra hoy de este pedido: el saldo pendiente o, si el servidor no lo manda, el total. */
val OrderSummary.cashAmountDue: String
    get() = balanceDue?.takeIf { runCatching { BigDecimal(it).signum() > 0 }.getOrDefault(false) } ?: total

data class CashCollectionResult(
    val order: OrderDetail,
    val amountReceived: String,
    val change: String,
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
    val amountCents: Long? = null,
    val currency: String? = null,
    val pricingPolicyVersion: String? = null,
    val applicationFeeCents: Long? = null,
)

data class OrderUser(
    val name: String,
    val enrollment: String,
    /** Códigos del catálogo de alérgenos; van en una banda coral arriba del ticket de Cocina y de Caja. */
    val allergies: List<String> = emptyList(),
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
    /** Rechazo por artículo: Cocina o Caja lo quitaron y ya no cuenta en el total. */
    val rejection: OrderItemRejection? = null,
    /** Foto actual del producto, como la manda el pedido. */
    val imageUrl: String? = null,
    /** Nota de esta línea; vacía si el cliente no escribió ninguna. */
    val notes: String = "",
)

data class OrderItemRejection(
    val reason: String,
    val amount: String,
)

/**
 * Sin artículos de cocina el pedido no se prepara: el backend lo pasa solo a listo. El seguimiento
 * marca "Preparando" como omitido (no aplica) en vez de hecho. Si el estado ya es preparando, sí aplica.
 */
val OrderDetail.skipsKitchen: Boolean
    get() {
        val live = items.filter { it.rejection == null }
        return summary.state != OrderState.PREPARING &&
            live.isNotEmpty() &&
            live.none { it.preparationStation == PreparationStation.KITCHEN }
    }

const val PREPARING_SKIPPED_NOTE = "No aplica: este pedido no pasa por cocina."

/** Lo que sí se prepara: un artículo quitado no encabeza la tarjeta. */
val OrderDetail.activeItems: List<OrderItem>
    get() = items.filter { it.rejection == null }

/** "Se quitó Torta: Se terminó el pan." para el cliente, o null si no se quitó nada. */
val OrderDetail.rejectedItemsHint: String?
    get() =
        items
            .filter { it.rejection != null }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" ") { "Se quitó ${it.productName}: ${it.rejection?.reason}." }

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
        /** Un estado de pago que la app no conoce se trata como pendiente: el servidor sigue siendo quien decide. */
        fun fromWireValue(value: String): StripePaymentStatus = entries.firstOrNull { it.wireValue == value } ?: PENDING
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

    /** Ya no hay nada que seguir ni recoger: entregado o terminado sin entrega. */
    val isClosed: Boolean
        get() = this == DELIVERED || isTerminalWithoutDelivery

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
