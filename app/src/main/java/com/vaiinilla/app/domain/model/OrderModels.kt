package com.vaiinilla.app.domain.model

import com.vaiinilla.app.domain.time.BusinessTime
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    /** A domicilio: a dónde se lleva (CASA-05). */
    val delivery: DeliveryAddress? = null,
    /** Encargo para otro día: `AAAA-MM-DD` y la franja "10:00-12:00". */
    val deliveryDate: String? = null,
    val deliverySlot: String? = null,
    /** A domicilio y en efectivo: se paga en la puerta. */
    val paysOnArrival: Boolean = false,
    /** Con tarjeta: se cobra el anticipo y el resto en la puerta. */
    val withDeposit: Boolean = false,
    /** «Pasa por él»: en un espacio, el cliente lo recoge en la barra en lugar de que se lo lleven. */
    val pickUpAtBar: Boolean = false,
    /** «Para llevar» confirmado aunque el cliente tenga una silla activa en una mesa o pista (R15). */
    val confirmTakeAway: Boolean = false,
)

/** A dónde se lleva un pedido, como lo escribe el cliente. */
data class DeliveryAddress(
    val address: String,
    val reference: String = "",
    val phone: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    /** Lo mínimo para que el negocio llegue y avise: calle y número, y un teléfono de 10 dígitos. */
    val problem: String?
        get() =
            when {
                address.trim().length < 8 -> "Escribe la calle, el número y la colonia."
                phone.count(Char::isDigit) != 10 -> "Escribe un teléfono de 10 dígitos para que te avisen."
                else -> null
            }
}

/** Lo de un pedido a domicilio que el cliente y el negocio necesitan ver. */
data class OrderHomeDelivery(
    val address: DeliveryAddress,
    val fee: String? = null,
    val deposit: String? = null,
    /** Lo que se cobra en la puerta; "0.00" si ya está pagado. */
    val dueOnArrival: String? = null,
    /** Con qué se cobró en la puerta (`efectivo`, `terminal`, `transferencia`…); null si aún no. */
    val collectedAtDoorMethod: String? = null,
    /** Los 4 dígitos que el cliente dicta al recibir: solo le llegan a él y desde `en_camino`. */
    val code: String? = null,
    /** Quien lo lleva. El servidor aún no lo manda: sin nombre se dice «quien te lo lleve». */
    val courierName: String? = null,
) {
    val hasAmountDue: Boolean
        get() = (dueOnArrival?.toBigDecimalOrNull()?.signum() ?: 0) > 0
}

/** Lo que responde el negocio al preguntar si llega a una ubicación. */
data class HomeDeliveryQuote(
    val reaches: Boolean,
    val distanceKm: String?,
    val fee: String?,
    val minimum: String?,
)

/** Un día de la agenda del negocio con sus pedidos a domicilio y encargos. */
data class AgendaDay(
    val date: String,
    val orders: List<OrderDetail>,
)

/** Lo que el negocio hace con un pedido a domicilio o un encargo (CASA-09). */
sealed class HomeOrderAction(
    val path: String,
) {
    data object Accept : HomeOrderAction("aceptar")

    data class Reject(
        val reason: String,
    ) : HomeOrderAction("rechazar")

    data object SendOut : HomeOrderAction("en-camino")

    data class Deliver(
        val code: String?,
    ) : HomeOrderAction("entregar-domicilio")

    /** `metodo`: "efectivo" o "transferencia". */
    data class Collect(
        val method: String,
    ) : HomeOrderAction("cobrar-entrega")

    /** El negocio cancela un pedido aceptado que aún no se entrega; va por `cancelaciones`, no por la ruta de domicilio. */
    data class Cancel(
        val reason: String,
        val expectedVersion: Int,
    ) : HomeOrderAction("cancelaciones")
}

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
    /** Solo a domicilio; `null` con un servidor anterior. */
    val homeDelivery: OrderHomeDelivery? = null,
    /** Encargo por aceptar: hasta cuándo contesta el negocio; `null` si no puso plazo. */
    val answerBy: Instant? = null,
    /** Encargo aceptado con tarjeta: hasta cuándo paga el cliente antes de que se cancele. */
    val payBy: Instant? = null,
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

/** "hoy a las 15:30", "mañana a las 9:00" o "el 5 nov a las 9:00", con la hora del negocio. */
fun deadlineLabel(
    at: Instant,
    zone: ZoneId = BusinessTime.zone,
    today: LocalDate = LocalDate.now(zone),
): String {
    val local = at.atZone(zone)
    val hour = DateTimeFormatter.ofPattern("H:mm").format(local)
    val day =
        when (local.toLocalDate()) {
            today -> "hoy"
            today.plusDays(1) -> "mañana"
            else ->
                "el " +
                    DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-MX")).format(local).trimEnd('.')
        }
    return "$day a las $hour"
}

/** Encargo con tarjeta que el negocio aún no acepta: no se cobra nada hasta que lo acepte (EQUIPO-53). */
fun OrderDetail.awaitsAcceptanceBeforePaying(): Boolean =
    summary.paymentMethod == PaymentMethod.STRIPE &&
        summary.state == OrderState.AWAITING_ACCEPTANCE &&
        payment == null

/** Encargo con tarjeta que el negocio ya aceptó y el cliente todavía no paga: aún no tiene intento de pago. */
fun OrderDetail.awaitsPaymentAfterAcceptance(): Boolean =
    summary.paymentMethod == PaymentMethod.STRIPE &&
        summary.state == OrderState.PENDING_PAYMENT &&
        payment == null

/** Lo que se paga con tarjeta al aceptarse: el anticipo si lo hay, si no el total. */
val OrderDetail.amountDueOnAcceptance: String
    get() = homeDelivery?.deposit ?: summary.total

fun OrderDetail.isStripePaymentConfirmedByBackend(): Boolean =
    summary.paymentMethod == PaymentMethod.STRIPE &&
        payment?.status == StripePaymentStatus.CONFIRMED &&
        summary.state.isPaidOrLater

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
    /** Código del motivo de cancelación (`anticipo_sin_pagar`…); distingue quién dejó pasar el plazo (R29). */
    val cancelReasonCode: String? = null,
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
    /**
     * Con qué se cobró de verdad (`metodo_pago_cobrado`): `efectivo`, `terminal`, `saldo`, `stripe` o `mixto`.
     * `paymentMethod` es con qué se pidió; en una cuenta de mesa siempre dice efectivo. `null` sin pagos o en un
     * servidor anterior.
     */
    val chargedMethod: String? = null,
    /** Lo cobrado con cada método (`pagos_por_metodo`). */
    val paymentsByMethod: List<PaymentPart> = emptyList(),
    /** «Pasa por él» (`recoge_en_barra`): el cliente lo recoge en la barra y Caja lo entrega. */
    val pickUpAtBar: Boolean = false,
)

/** Una parte del cobro: `metodo` como lo manda el servidor y su monto. */
data class PaymentPart(
    val method: String,
    val amount: String,
)

/**
 * Efectivo que Caja todavía tiene que cobrar. Cocina puede arrancar un pedido en efectivo antes del cobro, así
 * que el saldo pendiente sigue existiendo en preparando y en listo: el estado solo no lo dice.
 * Un servidor que no manda `saldo_pendiente` solo deja pendiente al pedido por cobrar.
 */
val OrderSummary.needsCashCollection: Boolean
    get() {
        // A domicilio se cobra en la puerta (`cobrar-entrega`), no en Caja.
        if (paymentMethod != PaymentMethod.CASH ||
            payAtEnd ||
            destination == OrderDestination.HOME_DELIVERY
        ) {
            return false
        }
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
    HOME_DELIVERY("a_domicilio", "A domicilio"),
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

    /** Encargo que el negocio aún no acepta. */
    AWAITING_ACCEPTANCE("por_aceptar", "Por aceptar"),

    /** Pedido a domicilio que ya salió. */
    ON_THE_WAY("en_camino", "En camino"),
    ;

    val trackingIndex: Int
        get() =
            when (this) {
                AWAITING_ACCEPTANCE -> 0
                ON_THE_WAY -> trackingFlow.indexOf(READY)
                else -> trackingFlow.indexOf(this)
            }

    val isTerminalWithoutDelivery: Boolean
        get() = this == CANCELED || this == NOT_PICKED_UP || this == EXPIRED

    /** Ya se cobró y el pedido sigue su camino: cobrado, preparando, listo o entregado. */
    val isPaidOrLater: Boolean
        get() = this == PAID || this == PREPARING || this == READY || this == DELIVERED

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
