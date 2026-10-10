package com.vaiinilla.app.domain.repository

import com.vaiinilla.app.domain.model.CourtDay
import com.vaiinilla.app.domain.model.Reservation
import com.vaiinilla.app.domain.model.ReservationAccess
import com.vaiinilla.app.domain.model.ReservationPayment
import com.vaiinilla.app.domain.model.ReservationPaymentMethod
import java.time.Instant

/** Error del backend al reservar, con su código (`SLOT_TAKEN`, `RESERVATION_NOT_PAYABLE`…). */
class ReservationException(
    val code: String,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/** Reservas de cancha (docs/reservas-cancha.md del backend). */
interface ReservationRepository {
    /** Horario del día y lo ocupado por cancha; `date` = "YYYY-MM-DD" o null para hoy. */
    fun day(
        date: String?,
        access: ReservationAccess,
    ): Result<CourtDay>

    /** Cliente: las suyas. Personal: las del día (`date` o hoy). */
    fun list(
        access: ReservationAccess,
        date: String? = null,
    ): Result<List<Reservation>>

    /** Aparta la cancha 10 minutos. `start` null = para ahora. */
    fun create(
        access: ReservationAccess,
        courtId: Int,
        start: Instant?,
        durationMinutes: Int,
        customerName: String?,
        idempotencyKey: String,
    ): Result<Reservation>

    /** Cobra la renta. El personal manda `cashReceived` para cobrarla en la misma llamada. */
    fun pay(
        access: ReservationAccess,
        reservationId: String,
        method: ReservationPaymentMethod,
        cashReceived: String?,
        idempotencyKey: String,
    ): Result<ReservationPayment>

    /** Cancela sin reembolso. */
    fun cancel(
        access: ReservationAccess,
        reservationId: String,
        idempotencyKey: String,
    ): Result<Reservation>
}
