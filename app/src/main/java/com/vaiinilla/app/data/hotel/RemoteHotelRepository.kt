package com.vaiinilla.app.data.hotel

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.domain.hotel.ChargeCategory
import com.vaiinilla.app.domain.hotel.CheckoutMethod
import com.vaiinilla.app.domain.hotel.HotelConfig
import com.vaiinilla.app.domain.hotel.HotelException
import com.vaiinilla.app.domain.hotel.HotelRepository
import com.vaiinilla.app.domain.hotel.HotelRequest
import com.vaiinilla.app.domain.hotel.HotelRoom
import com.vaiinilla.app.domain.hotel.HotelSummary
import com.vaiinilla.app.domain.hotel.NewStay
import com.vaiinilla.app.domain.hotel.RequestAction
import com.vaiinilla.app.domain.hotel.RequestType
import com.vaiinilla.app.domain.hotel.RoomBatchResult
import com.vaiinilla.app.domain.hotel.RoomCleaning
import com.vaiinilla.app.domain.hotel.Stay
import com.vaiinilla.app.domain.hotel.StayCharge
import com.vaiinilla.app.domain.hotel.StayStatus

/** El modo Hotel contra el servidor real. Los paths son los de la sección 7.2 del plan. */
class RemoteHotelRepository(
    private val apiClient: VaiinillaApiClient,
    private val json: HotelJson = HotelJson(),
) : HotelRepository {
    override fun myStay(): Result<Stay?> =
        apiClient
            .get("estancias/mia")
            .mapCatching(json::parseStayOrNull)
            .recoverCatching { error ->
                // Sin estancia ligada el servidor contesta 404: no es un fallo, es «todavía no hay».
                if (error is ApiClientException && error.httpStatus == NOT_FOUND) null else throw error
            }.mapApiErrors()

    override fun linkStay(
        code: String,
        spaceId: Int?,
        qrToken: String?,
    ): Result<Stay> =
        apiClient
            .post("estancias/vincular", json.encodeLink(code, spaceId, qrToken))
            .mapCatching(json::parseStay)
            .mapApiErrors()

    override fun setDoNotDisturb(enabled: Boolean): Result<Stay> =
        apiClient
            .patch("estancias/mia", json.encodeDoNotDisturb(enabled))
            .mapCatching(json::parseStay)
            .mapApiErrors()

    override fun createRequest(
        type: RequestType,
        quantity: Int,
        note: String?,
        idempotencyKey: String,
    ): Result<HotelRequest> =
        apiClient
            .post("solicitudes-hotel", json.encodeRequest(type, quantity, note), key(idempotencyKey))
            .mapCatching(json::parseRequest)
            .mapApiErrors()

    /** El huésped ve sus solicitudes abiertas y las cerradas. */
    override fun myRequests(): Result<List<HotelRequest>> =
        apiClient
            .get("solicitudes-hotel", mapOf("estado" to "pendiente,tomada,resuelta,cancelada"))
            .mapCatching(json::parseRequests)
            .mapApiErrors()

    override fun cancelRequest(
        requestId: String,
        idempotencyKey: String,
    ): Result<HotelRequest> =
        apiClient
            .post("solicitudes-hotel/$requestId/cancelaciones", EMPTY_BODY, key(idempotencyKey))
            .mapCatching(json::parseRequest)
            .mapApiErrors()

    override fun stays(status: StayStatus?): Result<List<Stay>> =
        apiClient
            .get("estancias", status?.let { mapOf("estado" to it.wire) } ?: emptyMap())
            .mapCatching(json::parseStays)
            .mapApiErrors()

    override fun stay(stayId: String): Result<Stay> =
        apiClient.get("estancias/$stayId").mapCatching(json::parseStay).mapApiErrors()

    override fun checkIn(
        draft: NewStay,
        idempotencyKey: String,
    ): Result<Stay> =
        apiClient
            .post("estancias", json.encodeCheckIn(draft), key(idempotencyKey))
            .mapCatching(json::parseStay)
            .mapApiErrors()

    override fun addCharge(
        stayId: String,
        concept: String,
        amount: String,
        category: ChargeCategory,
        idempotencyKey: String,
    ): Result<StayCharge> =
        apiClient
            .post("estancias/$stayId/cargos", json.encodeCharge(concept, amount, category), key(idempotencyKey))
            .mapCatching(json::parseAddedCharge)
            .mapApiErrors()

    override fun checkout(
        stayId: String,
        method: CheckoutMethod,
        idempotencyKey: String,
        receivedAmount: String?,
        expectedTotal: String?,
        expectedVersion: Int?,
    ): Result<Stay> =
        apiClient
            .post(
                "estancias/$stayId/checkout",
                json.encodeCheckout(method, receivedAmount, expectedTotal, expectedVersion),
                key(idempotencyKey),
            ).mapCatching(json::parseStay)
            .mapApiErrors()

    override fun confirmArrival(
        stayId: String,
        expectedVersion: Int,
        idempotencyKey: String,
    ): Result<Stay> =
        apiClient
            .post("estancias/$stayId/check-in", json.encodeExpectedVersion(expectedVersion), key(idempotencyKey))
            .mapCatching(json::parseStay)
            .mapApiErrors()

    override fun cancelStay(
        stayId: String,
        expectedVersion: Int,
        idempotencyKey: String,
    ): Result<Stay> =
        apiClient
            .post("estancias/$stayId/cancelaciones", json.encodeExpectedVersion(expectedVersion), key(idempotencyKey))
            .mapCatching(json::parseStay)
            .mapApiErrors()

    /** El mismo `GET` sirve a los dos lados: el personal recibe las abiertas del hotel y el huésped, las suyas. */
    override fun openRequests(): Result<List<HotelRequest>> =
        apiClient.get("solicitudes-hotel").mapCatching(json::parseRequests).mapApiErrors()

    override fun transition(
        requestId: String,
        action: RequestAction,
        idempotencyKey: String,
        expectedVersion: Int?,
    ): Result<HotelRequest> =
        apiClient
            .post(
                "solicitudes-hotel/$requestId/transiciones",
                json.encodeTransition(action, expectedVersion),
                key(idempotencyKey),
            )
            .mapCatching(json::parseRequest)
            .mapApiErrors()

    override fun rooms(): Result<List<HotelRoom>> =
        apiClient.get("habitaciones").mapCatching(json::parseRooms).mapApiErrors()

    override fun setCleaning(
        roomId: Int,
        cleaning: RoomCleaning,
        idempotencyKey: String,
    ): Result<HotelRoom> =
        apiClient
            .post("habitaciones/$roomId/limpieza", json.encodeCleaning(cleaning), key(idempotencyKey))
            .mapCatching(json::parseRoom)
            .mapApiErrors()

    override fun summary(): Result<HotelSummary> =
        apiClient.get("hotel/resumen").mapCatching(json::parseSummary).mapApiErrors()

    override fun hotelConfig(): Result<HotelConfig> =
        apiClient.get("hotel/configuracion").mapCatching(json::parseConfig).mapApiErrors()

    override fun saveHotelConfig(
        config: HotelConfig,
        idempotencyKey: String,
    ): Result<HotelConfig> =
        apiClient
            .put("hotel/configuracion", json.encodeHotelConfig(config), key(idempotencyKey))
            .mapCatching(json::parseConfig)
            .mapApiErrors()

    override fun createRooms(
        prefix: String,
        from: Int,
        to: Int,
        floor: Int?,
        category: String?,
        idempotencyKey: String,
    ): Result<RoomBatchResult> =
        apiClient
            .post("habitaciones/lote", json.encodeRoomBatch(prefix, from, to, floor, category), key(idempotencyKey))
            .mapCatching(json::parseRoomBatch)
            .mapApiErrors()

    override fun updateRoom(
        roomId: Int,
        floor: Int?,
        category: String?,
        idempotencyKey: String,
    ): Result<HotelRoom> =
        apiClient
            .patch("habitaciones/$roomId", json.encodeRoomUpdate(floor, category), key(idempotencyKey))
            .mapCatching(json::parseRoom)
            .mapApiErrors()

    private fun key(idempotencyKey: String) = mapOf("Idempotency-Key" to idempotencyKey)

    private fun <T> Result<T>.mapApiErrors(): Result<T> =
        fold(
            onSuccess = { Result.success(it) },
            onFailure = { error ->
                Result.failure(
                    if (error is ApiClientException) {
                        HotelException(error.code, error.message ?: error.code, error)
                    } else {
                        error
                    },
                )
            },
        )

    private companion object {
        const val EMPTY_BODY = "{}"
        const val NOT_FOUND = 404
    }
}