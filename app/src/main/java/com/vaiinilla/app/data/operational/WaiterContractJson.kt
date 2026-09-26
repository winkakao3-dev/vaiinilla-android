package com.vaiinilla.app.data.operational

import com.vaiinilla.app.data.contract.MetaDto
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.repository.BoardOrder
import com.vaiinilla.app.domain.repository.BoardTable
import com.vaiinilla.app.domain.repository.CallReason
import com.vaiinilla.app.domain.repository.CallStatus
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.domain.repository.TableCallTaker
import com.vaiinilla.app.domain.repository.TableSpace
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class TableSpaceDto(
    val id: Int,
    val nombre: String,
    val tipo: String,
)

@Serializable
data class TableCallTakerDto(
    @SerialName("usuario_id") val userId: String,
    val nombre: String,
)

@Serializable
data class TableCallClientDto(
    val nombre: String,
)

@Serializable
data class TableCallDto(
    val id: String,
    val espacio: TableSpaceDto,
    @SerialName("pedido_id") val orderId: String? = null,
    val motivo: String,
    val estado: String,
    val cliente: TableCallClientDto? = null,
    @SerialName("tomada_por") val takenBy: TableCallTakerDto? = null,
    @SerialName("creado_en") val createdAt: String,
    @SerialName("tomada_en") val takenAt: String? = null,
    @SerialName("cerrada_en") val closedAt: String? = null,
    val version: Int,
)

@Serializable
data class BoardOrderDto(
    val id: String,
    val folio: Int,
    val estado: String,
    val version: Int,
    val cliente: TableCallClientDto? = null,
    @SerialName("items_resumen") val itemsSummary: String,
    @SerialName("actualizado_en") val updatedAt: String,
)

@Serializable
data class BoardTableDto(
    val espacio: TableSpaceDto,
    val llamada: TableCallDto? = null,
    val pedidos: List<BoardOrderDto> = emptyList(),
)

@Serializable
data class BoardEnvelopeDto(
    val data: List<BoardTableDto>,
    val meta: MetaDto? = null,
    val error: JsonElement? = null,
)

@Serializable
data class SpacesEnvelopeDto(
    val data: List<TableSpaceDto>,
    val meta: MetaDto? = null,
    val error: JsonElement? = null,
)

@Serializable
data class CallsEnvelopeDto(
    val data: List<TableCallDto>,
    val meta: MetaDto? = null,
    val error: JsonElement? = null,
)

@Serializable
data class CallEnvelopeDto(
    val data: TableCallDto,
    val meta: MetaDto? = null,
    val error: JsonElement? = null,
)

@Serializable
data class CallTransitionRequestDto(
    @SerialName("estado_objetivo") val target: String,
    @SerialName("version_esperada") val expectedVersion: Int,
)

@Serializable
data class DeliverTransitionRequestDto(
    @SerialName("estado_objetivo") val target: String,
    @SerialName("version_esperada") val expectedVersion: Int,
    @SerialName("qr_token") val qrToken: String,
)

@Serializable
data class BuyerCallRequestDto(
    val motivo: String,
    @SerialName("pedido_id") val orderId: String? = null,
)

fun TableSpaceDto.toDomain(): TableSpace = TableSpace(id = id, name = nombre, type = tipo)

fun TableCallDto.toDomain(): TableCall =
    TableCall(
        id = id,
        space = espacio.toDomain(),
        orderId = orderId,
        reason = CallReason.fromWireValue(motivo),
        status = CallStatus.fromWireValue(estado),
        clientName = cliente?.nombre,
        takenBy = takenBy?.let { TableCallTaker(userId = it.userId, name = it.nombre) },
        createdAt = createdAt,
        takenAt = takenAt,
        closedAt = closedAt,
        version = version,
    )

fun BoardOrderDto.toDomain(): BoardOrder =
    BoardOrder(
        id = id,
        folio = folio,
        state = OrderState.fromWireValue(estado),
        version = version,
        clientName = cliente?.nombre,
        itemsSummary = itemsSummary,
        updatedAt = updatedAt,
    )

fun BoardTableDto.toDomain(): BoardTable =
    BoardTable(
        space = espacio.toDomain(),
        call = llamada?.toDomain(),
        orders = pedidos.map { it.toDomain() },
    )

@Singleton
class WaiterContractJson
    @Inject
    constructor() {
        private val json =
            Json {
                ignoreUnknownKeys = true
                explicitNulls = true
                isLenient = false
                encodeDefaults = true
            }

        fun parseBoard(raw: String): List<BoardTable> {
            val envelope = json.decodeFromString<BoardEnvelopeDto>(raw)
            require(envelope.error == null) { "La API devolvió un error en el envelope." }
            return envelope.data.map { it.toDomain() }
        }

        fun parseSpaces(raw: String): List<TableSpace> {
            val envelope = json.decodeFromString<SpacesEnvelopeDto>(raw)
            require(envelope.error == null) { "La API devolvió un error en el envelope." }
            return envelope.data.map { it.toDomain() }
        }

        fun parseCalls(raw: String): List<TableCall> {
            val envelope = json.decodeFromString<CallsEnvelopeDto>(raw)
            require(envelope.error == null) { "La API devolvió un error en el envelope." }
            return envelope.data.map { it.toDomain() }
        }

        fun parseCall(raw: String): TableCall {
            val envelope = json.decodeFromString<CallEnvelopeDto>(raw)
            require(envelope.error == null) { "La API devolvió un error en el envelope." }
            return envelope.data.toDomain()
        }

        fun encodeCallTransition(
            target: CallStatus,
            expectedVersion: Int,
        ): String =
            json.encodeToString(
                CallTransitionRequestDto(
                    target = target.wireValue,
                    expectedVersion = expectedVersion,
                ),
            )

        fun encodeDeliver(
            expectedVersion: Int,
            qrToken: String,
        ): String =
            json.encodeToString(
                DeliverTransitionRequestDto(
                    target = "entregado",
                    expectedVersion = expectedVersion,
                    qrToken = qrToken.trim(),
                ),
            )

        fun encodeBuyerCall(
            reason: CallReason,
            orderId: String?,
        ): String =
            json.encodeToString(
                BuyerCallRequestDto(
                    motivo = reason.wireValue,
                    orderId = orderId,
                ),
            )
    }
