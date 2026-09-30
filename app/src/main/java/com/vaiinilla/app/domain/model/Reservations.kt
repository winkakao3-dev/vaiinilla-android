package com.vaiinilla.app.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant

/** Estado de una reserva de cancha tal como la manda el backend (docs/reservas-cancha.md). */
enum class ReservationState(
    val wire: String,
    val label: String,
) {
    PENDING_PAYMENT("pendiente_pago", "Esperando pago"),
    CONFIRMED("confirmada", "Pagada"),
    IN_PROGRESS("en_curso", "En juego"),
    FINISHED("terminada", "Terminada"),
    CANCELLED("cancelada", "Cancelada"),
    EXPIRED("expirada", "Venció el apartado"),
    CONFLICT("conflicto", "Revisar con el personal"),
    ;

    /** Sigue apartando la cancha (se ve en el mapa y en "mis reservas" como activa). */
    val isLive: Boolean
        get() = this == PENDING_PAYMENT || this == CONFIRMED || this == IN_PROGRESS

    companion object {
        fun fromWire(value: String): ReservationState = entries.firstOrNull { it.wire == value } ?: CONFLICT
    }
}

/** Lo que paga el cliente en la app: con tarjeta (precio digital) o con efectivo/saldo. */
data class CustomerPrice(
    val card: String,
    val cashOrBalance: String,
)

data class BusyInterval(
    val start: Instant,
    val end: Instant,
    /** `reserva` o `turno`. */
    val reason: String,
)

data class CourtSchedule(
    val id: Int,
    val name: String,
    val pricePerHour: String?,
    val customerPricePerHour: CustomerPrice?,
    val rentable: Boolean,
    val busy: List<BusyInterval>,
)

/** Horario reservable de un día y lo ocupado de cada cancha. */
data class CourtDay(
    val date: String,
    val today: String,
    val timeZone: String,
    val opensAt: Instant,
    val closesAt: Instant,
    val blockMinutes: Int,
    val durations: List<Int>,
    val daysAhead: Int,
    val holdMinutes: Int,
    val now: Instant,
    val courts: List<CourtSchedule>,
)

data class Reservation(
    val id: String,
    val courtId: Int,
    val courtName: String?,
    val start: Instant,
    val end: Instant,
    val durationMinutes: Int,
    val amount: String,
    val customerPrice: CustomerPrice?,
    val state: ReservationState,
    val holdExpiresAt: Instant?,
    val orderId: String?,
    val channel: String,
    val customerName: String?,
    val version: Int,
)

enum class ReservationPaymentMethod(
    val wire: String,
    val label: String,
) {
    BALANCE("saldo", "Saldo"),
    CARD("stripe", "Tarjeta"),
    CASH("efectivo", "Efectivo en caja"),
}

/** Resultado de pagar: la reserva, su pedido de renta (con hoja de Stripe si aplica) y el cambio. */
data class ReservationPayment(
    val reservation: Reservation,
    val order: CreatedOrder?,
    val cashReceived: String?,
    val cashChange: String?,
)

/**
 * Cuentas puras de horarios: qué inicios ofrecer, qué duraciones caben sin chocar y cuánto cuesta.
 * Sin Android para poder probarlas.
 */
object ReservationSlots {
    /** Inicios de `blockMinutes` entre la apertura y el cierre; hoy, solo desde el siguiente bloque. */
    fun startTimes(day: CourtDay): List<Instant> {
        val block = Duration.ofMinutes(day.blockMinutes.toLong())
        val shortest = Duration.ofMinutes((day.durations.minOrNull() ?: day.blockMinutes).toLong())
        val result = mutableListOf<Instant>()
        var slot = day.opensAt
        while (!slot.plus(shortest).isAfter(day.closesAt)) {
            if (!slot.isBefore(day.now)) result += slot
            slot = slot.plus(block)
        }
        return result
    }

    /** La cancha está libre de `start` a `start + minutes` y dentro del horario. */
    fun isFree(
        day: CourtDay,
        court: CourtSchedule,
        start: Instant,
        minutes: Int,
    ): Boolean {
        val end = start.plus(Duration.ofMinutes(minutes.toLong()))
        if (start.isBefore(day.opensAt) || end.isAfter(day.closesAt)) return false
        return court.busy.none { it.start.isBefore(end) && start.isBefore(it.end) }
    }

    /** Un inicio se ofrece si cabe al menos la duración más corta. */
    fun isStartAvailable(
        day: CourtDay,
        court: CourtSchedule,
        start: Instant,
    ): Boolean = availableDurations(day, court, start).isNotEmpty()

    fun availableDurations(
        day: CourtDay,
        court: CourtSchedule,
        start: Instant,
    ): List<Int> = day.durations.filter { isFree(day, court, start, it) }

    /** ¿La cancha está libre ahora mismo por al menos la duración más corta? ("Rentar ahora"). */
    fun canRentNow(
        day: CourtDay,
        court: CourtSchedule,
    ): Boolean = day.date == day.today && court.rentable && isStartAvailable(day, court, day.now)

    /** `precio × minutos / 60`, redondeado al centavo, en texto con dos decimales. */
    fun amountFor(
        pricePerHour: String,
        minutes: Int,
    ): String =
        Money.format(
            Money
                .parse(pricePerHour)
                .multiply(BigDecimal(minutes))
                .divide(BigDecimal(60), 2, RoundingMode.HALF_UP),
        )
}
