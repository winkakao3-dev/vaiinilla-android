package com.vaiinilla.app.data.operational

import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.domain.repository.PendingRefund
import com.vaiinilla.app.domain.repository.PendingRefundRepository
import com.vaiinilla.app.domain.repository.RefundMethod
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/** `/devoluciones`: las pendientes y su confirmación por Caja. */
class RemotePendingRefundRepository(
    private val apiClient: VaiinillaApiClient,
) : PendingRefundRepository {
    override fun pending(spaceId: Int?): Result<List<PendingRefund>> =
        apiClient
            .get("devoluciones", spaceId?.let { mapOf("espacio_id" to it.toString()) }.orEmpty())
            .mapCatching(PendingRefundJson::parseList)

    override fun confirm(
        refundId: String,
        method: RefundMethod,
        note: String?,
        idempotencyKey: String,
    ): Result<PendingRefund> =
        apiClient
            .post(
                path = "devoluciones/$refundId/confirmaciones",
                body = PendingRefundJson.encodeConfirmation(method, note),
                headers = mapOf("Idempotency-Key" to idempotencyKey),
            ).mapCatching(PendingRefundJson::parseOne)
}

internal object PendingRefundJson {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    fun parseList(raw: String): List<PendingRefund> {
        val envelope = json.decodeFromString<RefundListEnvelopeDto>(raw)
        require(envelope.error == null) { "La API devolvió un error en el envelope." }
        return envelope.data.map(PendingRefundDto::toDomain)
    }

    fun parseOne(raw: String): PendingRefund {
        val envelope = json.decodeFromString<RefundEnvelopeDto>(raw)
        require(envelope.error == null) { "La API devolvió un error en el envelope." }
        return envelope.data.toDomain()
    }

    fun encodeConfirmation(
        method: RefundMethod,
        note: String?,
    ): String = json.encodeToString(RefundConfirmationDto(method.wireValue, note?.trim()?.ifEmpty { null }))
}

@Serializable
internal data class PendingRefundDto(
    val id: String,
    val origen: String,
    @SerialName("espacio_id") val spaceId: Int? = null,
    @SerialName("pedido_id") val orderId: String? = null,
    @SerialName("metodo_original") val originalMethod: String,
    val monto: String,
    val estado: String,
    @SerialName("creado_en") val createdAt: String,
) {
    fun toDomain() =
        PendingRefund(
            id = id,
            origin = origen,
            spaceId = spaceId,
            orderId = orderId,
            originalMethod = originalMethod,
            amount = monto,
            state = estado,
            createdAt = createdAt,
        )
}

@Serializable
internal data class RefundListEnvelopeDto(
    val data: List<PendingRefundDto>,
    val error: JsonElement? = null,
)

@Serializable
internal data class RefundEnvelopeDto(
    val data: PendingRefundDto,
    val error: JsonElement? = null,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class RefundConfirmationDto(
    val metodo: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val nota: String? = null,
)
