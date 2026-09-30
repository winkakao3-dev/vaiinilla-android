package com.vaiinilla.app.data.reservations

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.domain.model.CourtDay
import com.vaiinilla.app.domain.model.Reservation
import com.vaiinilla.app.domain.model.ReservationPayment
import com.vaiinilla.app.domain.model.ReservationPaymentMethod
import com.vaiinilla.app.domain.repository.ReservationException
import com.vaiinilla.app.domain.repository.ReservationRepository
import java.time.Instant

class RemoteReservationRepository(
    private val apiClient: VaiinillaApiClient,
    private val contractJson: ReservationContractJson,
) : ReservationRepository {
    override fun day(date: String?): Result<CourtDay> =
        apiClient
            .get("reservas/disponibilidad", date?.let { mapOf("fecha" to it) } ?: emptyMap())
            .mapCatching(contractJson::parseDay)
            .mapApiErrors()

    override fun list(date: String?): Result<List<Reservation>> =
        apiClient
            .get("reservas", date?.let { mapOf("fecha" to it) } ?: emptyMap())
            .mapCatching(contractJson::parseReservations)
            .mapApiErrors()

    override fun create(
        courtId: Int,
        start: Instant?,
        durationMinutes: Int,
        customerName: String?,
        idempotencyKey: String,
    ): Result<Reservation> =
        apiClient
            .post(
                path = "reservas",
                body = contractJson.encodeCreate(courtId, start, durationMinutes, customerName),
                headers = mapOf("Idempotency-Key" to idempotencyKey),
            ).mapCatching(contractJson::parseReservation)
            .mapApiErrors()

    override fun pay(
        reservationId: String,
        method: ReservationPaymentMethod,
        cashReceived: String?,
        idempotencyKey: String,
    ): Result<ReservationPayment> =
        apiClient
            .post(
                path = "reservas/$reservationId/pago",
                body = contractJson.encodePay(method.wire, cashReceived),
                headers = mapOf("Idempotency-Key" to idempotencyKey),
            ).mapCatching(contractJson::parsePayment)
            .mapApiErrors()

    override fun cancel(
        reservationId: String,
        idempotencyKey: String,
    ): Result<Reservation> =
        apiClient
            .postWithoutBody(
                path = "reservas/$reservationId/cancelacion",
                headers = mapOf("Idempotency-Key" to idempotencyKey),
            ).mapCatching(contractJson::parseReservation)
            .mapApiErrors()

    private fun <T> Result<T>.mapApiErrors(): Result<T> =
        fold(
            onSuccess = { Result.success(it) },
            onFailure = { error ->
                Result.failure(
                    if (error is ApiClientException) {
                        ReservationException(error.code, error.message ?: error.code, error)
                    } else {
                        error
                    },
                )
            },
        )
}
