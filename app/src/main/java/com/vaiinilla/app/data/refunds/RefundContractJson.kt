package com.vaiinilla.app.data.refunds

import com.vaiinilla.app.domain.model.PendingRefund
import com.vaiinilla.app.domain.model.RefundMethod
import com.vaiinilla.app.domain.model.RefundOrigin
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

/** Campos de `mapDevolucion` en el backend (src/services/devoluciones.service.js). */
@Serializable
data class PendingRefundDto(
    val id: String,
    val origen: String,
    @SerialName("espacio_id") val espacioId: Int? = null,
    @SerialName("metodo_original") val metodoOriginal: String,
    val monto: String,
)

@Serializable
private data class PendingRefundListEnvelopeDto(
    val data: List<PendingRefundDto>,
    val error: JsonElement? = null,
)

@Serializable
private data class PendingRefundEnvelopeDto(
    val data: PendingRefundDto,
    val error: JsonElement? = null,
)

/** Contrato JSON de /api/v1/devoluciones. */
class RefundContractJson
    @Inject
    constructor() {
        private val json =
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }

        fun parseList(raw: String): List<PendingRefund> {
            val envelope = json.decodeFromString<PendingRefundListEnvelopeDto>(raw)
            require(envelope.error == null || envelope.error is JsonNull) { "La API devolvió un error." }
            return envelope.data.map { it.toDomain() }
        }

        fun parseRefund(raw: String): PendingRefund {
            val envelope = json.decodeFromString<PendingRefundEnvelopeDto>(raw)
            require(envelope.error == null || envelope.error is JsonNull) { "La API devolvió un error." }
            return envelope.data.toDomain()
        }

        /** Cuerpo de `POST /devoluciones/{id}/confirmaciones`: `nota` solo si hay texto. */
        fun encodeConfirm(
            method: RefundMethod,
            note: String?,
        ): String =
            buildJsonObject {
                put("metodo", method.wire)
                note?.takeIf { it.isNotBlank() }?.let { put("nota", it) }
            }.toString()

        private fun PendingRefundDto.toDomain() =
            PendingRefund(
                id = id,
                origin = RefundOrigin.fromWire(origen),
                spaceId = espacioId,
                originalMethod = metodoOriginal,
                amount = monto,
            )
    }
