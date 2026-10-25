package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.ui.components.RentalStep
import com.vaiinilla.app.ui.components.needsPickupQr
import com.vaiinilla.app.ui.components.rentalCourtName
import com.vaiinilla.app.ui.components.rentalStep
import com.vaiinilla.app.ui.components.rentalStepDescription
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.math.sqrt

/** Los tres pasos de la barra de progreso de la recogida. */
internal enum class PickupStep(
    val label: String,
) {
    ORDERED("Pedido"),
    MAKING("Preparando"),
    READY("Listo"),
}

internal fun pickupStep(state: OrderState): PickupStep? =
    when (state) {
        OrderState.PENDING_PAYMENT, OrderState.PAID -> PickupStep.ORDERED
        OrderState.PREPARING -> PickupStep.MAKING
        OrderState.READY, OrderState.DELIVERED -> PickupStep.READY
        OrderState.CANCELED, OrderState.NOT_PICKED_UP, OrderState.EXPIRED -> null
    }

private fun OrderDetail.cashPending(): Boolean =
    summary.state == OrderState.PENDING_PAYMENT && summary.paymentMethod == PaymentMethod.CASH

private fun OrderDetail.spaceName(): String = summary.space?.name?.takeIf { it.isNotBlank() } ?: "tu espacio"

internal fun pickupTitle(
    order: OrderDetail,
    now: Instant = Instant.now(),
): String {
    courtMoment(order, now)?.let { return it.title }
    return when (order.summary.state) {
        OrderState.PENDING_PAYMENT -> if (order.cashPending()) "Por pagar" else "Pedido recibido"
        OrderState.PAID -> "Pedido recibido"
        OrderState.PREPARING -> "Preparando"
        OrderState.READY -> "¡Listo!"
        else -> order.summary.state.label
    }
}

/** Mismo copy que la PWA (`order-labels.ts`). Una renta se cuenta por la cancha, no por el estado del pedido. */
internal fun pickupHint(
    order: OrderDetail,
    now: Instant = Instant.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val moment = courtMoment(order, now)
    val reservation = order.reservation
    if (moment != null && reservation != null) {
        val minutes = courtMinutesLeft(reservation.end, now)
        return when (moment) {
            CourtMoment.TO_PAY, CourtMoment.BOOKED -> {
                val step = if (moment == CourtMoment.TO_PAY) RentalStep.TO_PAY else RentalStep.BOOKED
                rentalStepDescription(
                    step,
                    reservation,
                    order.summary.paymentMethod,
                    null,
                    zone,
                    now.atZone(zone).toLocalDate(),
                )
            }
            CourtMoment.PLAYING -> "Te quedan $minutes min en ${order.rentalCourtName ?: "tu cancha"}."
            CourtMoment.LAST_MINUTES -> "Quedan $minutes min: los reflectores ya bajaron."
            CourtMoment.DONE -> "Gracias por jugar."
        }
    }
    val state = order.summary.state
    val toSpace = order.summary.destination == OrderDestination.IN_SPACE
    return when {
        // El código de efectivo solo vive en el dispositivo donde se hizo el pedido: en otro, el folio basta.
        order.cashPending() ->
            if (order.pickupToken.isNullOrBlank()) {
                "Di en Caja el folio ${order.summary.folio} para pagar."
            } else {
                "Muestra tu código en Caja para pagar."
            }
        toSpace && state == OrderState.READY -> "Ya va en camino a ${order.spaceName()}."
        toSpace && (state == OrderState.PAID || state == OrderState.PREPARING) -> "Te lo llevan a ${order.spaceName()}."
        state == OrderState.PENDING_PAYMENT -> "Estamos confirmando tu pago."
        state == OrderState.PAID -> "Cocina recibió la comanda."
        state == OrderState.PREPARING -> "Tu comida se está preparando."
        state == OrderState.READY -> "Recógelo en la barra."
        state == OrderState.DELIVERED -> "Pedido completado."
        // Quien lo canceló y su motivo, como en el seguimiento: el cliente no tiene que ir a buscarlo.
        state == OrderState.CANCELED ->
            orderCancellationNotice(order)
                ?.let { notice -> notice.reason?.let { "${notice.title}: $it" } }
                ?: "Este pedido se canceló."
        state == OrderState.NOT_PICKED_UP -> "No se recogió el pedido."
        else -> "Este pedido expiró."
    }
}

internal fun pickupCodeHint(order: OrderDetail): String =
    when {
        order.summary.destination == OrderDestination.IN_SPACE ->
            "Tu mesero te lo lleva. Si te pide el código, muéstrale este."
        order.cashPending() -> "Muéstralo en Caja para pagar."
        else -> "Muéstralo en la barra para recoger tu pedido."
    }

/**
 * "Mostrar código" solo sirve mientras el pedido sigue abierto. En mesa o cancha, además, el negocio
 * debe pedir el QR al entregar, salvo que haya que pagar en Caja.
 */
internal fun pickupCodeUseful(
    order: OrderDetail,
    deliveryRequiresQr: Boolean,
): Boolean =
    order.summary.state in setOf(OrderState.PENDING_PAYMENT, OrderState.PAID, OrderState.PREPARING, OrderState.READY) &&
        (order.cashPending() || order.needsPickupQr(deliveryRequiresQr))

/**
 * El código se abre solo cuando hay que enseñarlo: listo para recoger en barra, o pago en efectivo
 * pendiente. En mesa o cancha lo lleva el mesero, así que no se abre solo.
 */
internal fun shouldAutoRevealPickupCode(
    order: OrderDetail,
    hasCode: Boolean,
): Boolean =
    hasCode &&
        order.summary.destination != OrderDestination.IN_SPACE &&
        (order.summary.state == OrderState.READY || order.cashPending())

/**
 * En qué punto (0–1) del recorrido empieza a girar cada bloque del mosaico hasta su color del QR:
 * del centro hacia afuera, con un poco de desorden estable para que no se vea mecánico.
 * Todos terminan antes de 0.88, así el QR queda completo y escaneable al final.
 */
internal fun mosaicCellStart(
    col: Int,
    row: Int,
    size: Int,
): Float {
    val center = (size - 1) / 2f
    val dx = col - center
    val dy = row - center
    val distance = sqrt(dx * dx + dy * dy) / (center * sqrt(2f)).coerceAtLeast(1f)
    val jitter = (((col * 73_856_093) xor (row * 19_349_663)).toUInt() % 1000u).toFloat() / 1000f
    return MOSAIC_FLIP_FROM + (distance.coerceIn(0f, 1f) * 0.82f + jitter * 0.18f) * MOSAIC_FLIP_SPREAD
}

internal const val MOSAIC_FLIP_FROM = 0.42f
internal const val MOSAIC_FLIP_SPREAD = 0.3f
internal const val MOSAIC_FLIP_LENGTH = 0.14f

/** El lugar del mundo donde se sigue un pedido, como `PickupPlace` en iOS. */
internal enum class PickupPlace {
    /** La fachada de la tienda; el QR sale de ella. */
    STOREFRONT,

    /** Asomados por la vitrina mientras se prepara un pedido para llevar. */
    KITCHEN,

    /** El lugar visto desde arriba, con la mesa (o el espacio) a donde llega el pedido. */
    TABLE,

    /** La cancha rentada vista desde arriba. */
    COURT,
}

/** Los espacios que se ven en el plano con mesas; una cancha no tiene mesas. */
internal fun spaceHasTables(type: String): Boolean = type != "cancha"

internal fun pickupPlace(order: OrderDetail): PickupPlace =
    when {
        order.reservation != null -> PickupPlace.COURT
        order.summary.destination == OrderDestination.IN_SPACE &&
            spaceHasTables(order.summary.space?.type ?: "mesa") -> PickupPlace.TABLE
        order.summary.state == OrderState.PREPARING -> PickupPlace.KITCHEN
        else -> PickupPlace.STOREFRONT
    }

/** Mesas en el plano del Vainiverso (vista desde arriba del lugar). */
internal const val FLOOR_TABLE_COUNT = 6

/**
 * Cuál mesa del plano es la tuya: la del número en el nombre ("Mesa 4" → la cuarta) o, si no tiene, una fija según
 * el id, para que el mismo espacio caiga siempre en el mismo lugar.
 */
internal fun floorTableIndex(
    name: String,
    id: Int,
): Int {
    val number =
        Regex("\\d+")
            .findAll(name)
            .lastOrNull()
            ?.value
            ?.toIntOrNull()
    if (number != null && number > 0) return (number - 1) % FLOOR_TABLE_COUNT
    return kotlin.math.abs(id) % FLOOR_TABLE_COUNT
}

// Cancha: las luces del turno, como en iOS.

/** Minutos antes del final en que bajan los reflectores: la gracia que avisa que el turno se acaba. */
internal const val COURT_GRACE_MINUTES = 5L

/** En qué va una renta, como se cuenta en la cancha: los últimos minutos son su propio momento. */
internal enum class CourtMoment(
    val title: String,
) {
    TO_PAY("Por pagar"),
    BOOKED("Reservada"),
    PLAYING("En juego"),
    LAST_MINUTES("Últimos minutos"),
    DONE("Terminada"),
}

/** `null` si no es renta o si se cayó (cancelada, vencida o en revisión). */
internal fun courtMoment(
    order: OrderDetail,
    now: Instant,
): CourtMoment? {
    val step = rentalStep(order, now) ?: return null
    val reservation = order.reservation ?: return null
    return when (step) {
        RentalStep.TO_PAY -> CourtMoment.TO_PAY
        RentalStep.BOOKED -> CourtMoment.BOOKED
        RentalStep.DONE -> CourtMoment.DONE
        RentalStep.PLAYING ->
            if (Duration.between(now, reservation.end) <= Duration.ofMinutes(COURT_GRACE_MINUTES)) {
                CourtMoment.LAST_MINUTES
            } else {
                CourtMoment.PLAYING
            }
    }
}

/** Minutos que quedan de juego, redondeados hacia arriba (con 30 s quedan "1 min"). */
internal fun courtMinutesLeft(
    end: Instant,
    now: Instant,
): Int = ceil(Duration.between(now, end).toMillis() / 60_000.0).toInt().coerceAtLeast(0)

/** Cómo se ve la cancha rentada: cuánto de las líneas ya se dibujó (el tiempo jugado) y cuánta luz dan los reflectores. */
internal data class CourtLights(
    val lines: Double,
    val lights: Double,
)

/**
 * Antes del turno las luces esperan bajas y sin líneas; jugando se dibujan con el tiempo y en la gracia bajan; al
 * terminar se apagan.
 */
internal fun courtLights(
    start: Instant,
    end: Instant,
    now: Instant,
): CourtLights {
    val total = Duration.between(start, end).toMillis()
    if (total <= 0 || !now.isBefore(end)) return CourtLights(lines = 1.0, lights = 0.0)
    if (now.isBefore(start)) return CourtLights(lines = 0.0, lights = 0.3)
    val inGrace = Duration.between(now, end) <= Duration.ofMinutes(COURT_GRACE_MINUTES)
    return CourtLights(
        lines = Duration.between(start, now).toMillis().toDouble() / total,
        lights = if (inGrace) 0.4 else 1.0,
    )
}
