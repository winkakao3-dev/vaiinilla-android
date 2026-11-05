package com.vaiinilla.app.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

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

/** Foto, descripción, características, deporte y superficie de la cancha que escribe el dueño. */
data class CourtProfile(
    val description: String? = null,
    val imageUrl: String? = null,
    val features: List<String> = emptyList(),
    val sport: String? = null,
    val surface: String? = null,
) {
    val isEmpty: Boolean
        get() = description == null && imageUrl == null && features.isEmpty() && sport == null && surface == null
}

data class CourtSchedule(
    val id: Int,
    val name: String,
    val pricePerHour: String?,
    val customerPricePerHour: CustomerPrice?,
    val rentable: Boolean,
    val busy: List<BusyInterval>,
    val profile: CourtProfile = CourtProfile(),
    /** Duraciones que se pueden rentar en este espacio (respeta su mínimo); vacía con un servidor anterior. */
    val durations: List<Int> = emptyList(),
    /** R25: cuántos caben en el espacio; null = sin tope propio (el servidor usa [ReservationSlots.MAX_PLAYERS]). */
    val capacity: Int? = null,
) {
    /** Tope de jugadores que acepta el servidor para este espacio. */
    val maxPlayers: Int
        get() = capacity ?: ReservationSlots.MAX_PLAYERS
}

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
    /** El lugar no renta a invitados: hace falta una cuenta (`requiere_cuenta`). */
    val requiresAccount: Boolean = false,
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
    /** Boliche: cuántos juegan; null sin dato. */
    val players: Int? = null,
)

/**
 * #28: reembolso al cancelar una renta ya pagada desde el personal. El servidor lo exige para cancelar una
 * reserva `confirmada` y lo devuelve por la misma vía del pago. `amount` va con dos decimales («150.00»),
 * de 0 a lo pagado; `reason` de 3 a 240 caracteres.
 */
data class ReservationRefund(
    val amount: String,
    val reason: String,
)

/** Quién aparta y paga: la cuenta con sesión, o un invitado (sin cuenta) del negocio. */
sealed interface ReservationAccess {
    data object Account : ReservationAccess

    data class Guest(
        val establishmentSlug: String,
    ) : ReservationAccess
}

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

/** Turno del día en el que empieza un horario, en la hora local del establecimiento. */
enum class ReservationShift(
    val label: String,
) {
    MORNING("Mañana"),
    AFTERNOON("Tarde"),
    NIGHT("Noche"),
}

/**
 * Cuentas puras de horarios: qué inicios ofrecer, qué duraciones caben sin chocar y cuánto cuesta.
 * Sin Android para poder probarlas.
 */
object ReservationSlots {
    /** Jugadores de una renta cuando el espacio no tiene capacidad propia (contrato C del backend). */
    const val MAX_PLAYERS = 30

    /** Mismos cortes que la PWA: 06–11 mañana, 12–18 tarde, el resto noche. */
    fun shiftOf(
        start: Instant,
        zone: ZoneId,
    ): ReservationShift =
        when (start.atZone(zone).hour) {
            in 6..11 -> ReservationShift.MORNING
            in 12..18 -> ReservationShift.AFTERNOON
            else -> ReservationShift.NIGHT
        }

    /** Agrupa los inicios por turno, en el orden en que aparecen. */
    fun groupByShift(
        starts: List<Instant>,
        zone: ZoneId,
    ): Map<ReservationShift, List<Instant>> = starts.groupBy { shiftOf(it, zone) }

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

    /**
     * Si la cancha está ocupada en `at`, hasta cuándo, uniendo intervalos pegados (un turno seguido
     * de una reserva no deja un "libre" de cero minutos). `null` si está libre en ese momento.
     */
    fun busyUntil(
        court: CourtSchedule,
        at: Instant,
    ): Instant? {
        var until: Instant? = null
        var cursor = at
        while (true) {
            val covering =
                court.busy
                    .filter {
                        !cursor.isBefore(
                            it.start,
                        ) &&
                            cursor.isBefore(it.end)
                    }.maxOfOrNull { it.end }
            if (covering == null) return until
            until = covering
            cursor = covering
        }
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
    ): List<Int> = durationsFor(day, court).filter { isFree(day, court, start, it) }

    /** Las del espacio (respetan su mínimo); con un servidor anterior, las del establecimiento. */
    fun durationsFor(
        day: CourtDay,
        court: CourtSchedule,
    ): List<Int> = court.durations.ifEmpty { day.durations }

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
