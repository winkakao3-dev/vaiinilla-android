package com.vaiinilla.app.data.reservations

import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.core.network.idempotencyHeaders
import com.vaiinilla.app.core.network.mapApiFailure
import com.vaiinilla.app.data.guest.GuestReservationSession
import com.vaiinilla.app.domain.model.CourtDay
import com.vaiinilla.app.domain.model.Reservation
import com.vaiinilla.app.domain.model.ReservationAccess
import com.vaiinilla.app.domain.model.ReservationPayment
import com.vaiinilla.app.domain.model.ReservationPaymentMethod
import com.vaiinilla.app.domain.model.ReservationRefund
import com.vaiinilla.app.domain.repository.ReservationException
import com.vaiinilla.app.domain.repository.ReservationRepository
import java.time.Instant

class RemoteReservationRepository(
    private val apiClient: VaiinillaApiClient,
    private val contractJson: ReservationContractJson,
    private val guestSession: GuestReservationSession,
) : ReservationRepository {
    override fun day(
        date: String?,
        access: ReservationAccess,
    ): Result<CourtDay> =
        access
            .perform { bearer ->
                read("reservas/disponibilidad", bearer, date?.let { mapOf("fecha" to it) } ?: emptyMap())
            }.mapCatching(contractJson::parseDay)
            .mapApiErrors()

    override fun list(
        access: ReservationAccess,
        date: String?,
    ): Result<List<Reservation>> =
        access
            .perform { bearer ->
                read("reservas", bearer, date?.let { mapOf("fecha" to it) } ?: emptyMap())
            }.mapCatching(contractJson::parseReservations)
            .mapApiErrors()

    override fun create(
        access: ReservationAccess,
        courtId: Int,
        start: Instant?,
        durationMinutes: Int,
        customerName: String?,
        idempotencyKey: String,
        players: Int?,
    ): Result<Reservation> {
        val body = contractJson.encodeCreate(courtId, start, durationMinutes, customerName, players)
        val headers = idempotencyHeaders(idempotencyKey)
        return access
            .perform { bearer -> send("reservas", bearer, body, headers) }
            .mapCatching(contractJson::parseReservation)
            .mapApiErrors()
    }

    override fun pay(
        access: ReservationAccess,
        reservationId: String,
        method: ReservationPaymentMethod,
        cashReceived: String?,
        idempotencyKey: String,
    ): Result<ReservationPayment> {
        val body = contractJson.encodePay(method.wire, cashReceived)
        val headers = idempotencyHeaders(idempotencyKey)
        return access
            .perform { bearer -> send("reservas/$reservationId/pago", bearer, body, headers) }
            .mapCatching(contractJson::parsePayment)
            .mapApiErrors()
    }

    override fun cancel(
        access: ReservationAccess,
        reservationId: String,
        idempotencyKey: String,
        refund: ReservationRefund?,
    ): Result<Reservation> {
        val headers = idempotencyHeaders(idempotencyKey)
        val body = refund?.let { contractJson.encodeRefund(it.amount, it.reason) }
        return access
            .perform { bearer -> send("reservas/$reservationId/cancelacion", bearer, body, headers) }
            .mapCatching(contractJson::parseReservation)
            .mapApiErrors()
    }

    /** `null` = sesión de la cuenta; si no, el bearer del invitado. */
    private fun read(
        path: String,
        bearer: String?,
        query: Map<String, String>,
    ): Result<String> =
        if (bearer == null) {
            apiClient.get(path, query)
        } else {
            apiClient.getWithBearer(bearer, path, query)
        }

    private fun send(
        path: String,
        bearer: String?,
        body: String?,
        headers: Map<String, String>,
    ): Result<String> =
        when {
            bearer != null -> apiClient.postWithBearer(bearer, path, body, headers)
            body == null -> apiClient.postWithoutBody(path, headers)
            else -> apiClient.post(path, body, headers)
        }

    private fun <T> ReservationAccess.perform(request: (bearer: String?) -> Result<T>): Result<T> =
        when (this) {
            ReservationAccess.Account -> request(null)
            is ReservationAccess.Guest -> guestSession.withBearer(establishmentSlug) { bearer -> request(bearer) }
        }

    private fun <T> Result<T>.mapApiErrors(): Result<T> =
        mapApiFailure {
            ReservationException(
                it.code,
                it.message ?: it.code,
                it,
            )
        }
}
