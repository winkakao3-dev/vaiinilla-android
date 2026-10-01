package com.vaiinilla.app.ui.components

import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderReservation
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.ReservationState
import com.vaiinilla.app.domain.model.StripePaymentStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Pasos de una renta de cancha. No pasa por cocina: se paga, queda reservada, se juega y termina.
 * El backend da el pedido de renta por entregado al cobrarse (docs/reservas-cancha.md), así que el
 * seguimiento sale de la reserva y de la hora, no del estado del pedido.
 */
enum class RentalStep(
    val title: String,
) {
    TO_PAY("POR PAGAR"),
    BOOKED("RESERVADA"),
    PLAYING("EN JUEGO"),
    DONE("TERMINADA"),
}

private val MX = Locale.forLanguageTag("es-MX")
private val HOUR = DateTimeFormatter.ofPattern("HH:mm", MX)
private val DAY = DateTimeFormatter.ofPattern("EEE d MMM", MX)

val OrderDetail.isRental: Boolean
    get() = reservation != null

/** Paso actual de la renta; `null` si no es renta o si se cayó (cancelada, vencida o en revisión). */
fun rentalStep(
    order: OrderDetail,
    now: Instant = Instant.now(),
): RentalStep? {
    val reservation = order.reservation ?: return null
    if (order.summary.state.isTerminalWithoutDelivery) return null
    return when (reservation.state) {
        ReservationState.PENDING_PAYMENT -> RentalStep.TO_PAY
        ReservationState.CONFIRMED,
        ReservationState.IN_PROGRESS,
        ReservationState.FINISHED,
        ->
            when {
                !now.isBefore(reservation.end) -> RentalStep.DONE
                !now.isBefore(reservation.start) -> RentalStep.PLAYING
                else -> RentalStep.BOOKED
            }
        ReservationState.CANCELLED, ReservationState.EXPIRED, ReservationState.CONFLICT -> null
    }
}

/** Una renta sigue "en curso" en Mis pedidos hasta que termina su horario o se cae. */
fun OrderDetail.isLiveRental(now: Instant = Instant.now()): Boolean =
    rentalStep(this, now).let { it != null && it != RentalStep.DONE }

fun rentalStateLabel(
    order: OrderDetail,
    now: Instant = Instant.now(),
): String =
    when (rentalStep(order, now)) {
        RentalStep.TO_PAY -> "Por pagar"
        RentalStep.BOOKED -> "Reservada"
        RentalStep.PLAYING -> "En juego"
        RentalStep.DONE -> "Terminada"
        null -> order.reservation?.state?.label ?: order.summary.state.label
    }

fun rentalStepDescription(
    step: RentalStep,
    reservation: OrderReservation,
    paymentMethod: PaymentMethod,
    paymentStatus: StripePaymentStatus?,
    zone: ZoneId = ZoneId.systemDefault(),
    today: LocalDate = LocalDate.now(zone),
): String =
    when (step) {
        RentalStep.TO_PAY ->
            when (paymentMethod) {
                PaymentMethod.CASH -> "Paga en caja para confirmar tu horario."
                PaymentMethod.STRIPE ->
                    if (paymentStatus == StripePaymentStatus.FAILED || paymentStatus == StripePaymentStatus.CANCELED) {
                        "El pago no se completó. Reintenta para no perder el horario."
                    } else {
                        "Termina el pago con tarjeta para confirmar tu horario."
                    }
                PaymentMethod.BALANCE -> "Estamos confirmando tu pago."
            }
        RentalStep.BOOKED -> {
            val day = rentalDay(reservation.start, zone, today)
            val onDay = if (day == "hoy" || day == "mañana") day else "el $day"
            "Te esperamos $onDay a las ${reservation.start.atZone(zone).format(HOUR)}."
        }
        RentalStep.PLAYING -> "Tu horario termina a las ${reservation.end.atZone(zone).format(HOUR)}."
        RentalStep.DONE -> "Gracias por jugar."
    }

/** "Hoy · 18:00–19:00", "Mañana · …" o "vie 3 oct · …". */
fun rentalScheduleLabel(
    reservation: OrderReservation,
    zone: ZoneId = ZoneId.systemDefault(),
    today: LocalDate = LocalDate.now(zone),
): String {
    val start = reservation.start.atZone(zone)
    val end = reservation.end.atZone(zone)
    return "${rentalDay(reservation.start, zone, today).replaceFirstChar { it.uppercase(MX) }} · " +
        "${start.format(HOUR)}–${end.format(HOUR)}"
}

/** "hoy", "mañana" o "vie 3 oct". */
private fun rentalDay(
    instant: Instant,
    zone: ZoneId,
    today: LocalDate,
): String =
    when (val date = instant.atZone(zone).toLocalDate()) {
        today -> "hoy"
        today.plusDays(1) -> "mañana"
        else -> date.format(DAY).replace(".", "")
    }

/** Nombre de la cancha rentada, o `null` si el pedido no es renta. */
val OrderDetail.rentalCourtName: String?
    get() = reservation?.space?.name
