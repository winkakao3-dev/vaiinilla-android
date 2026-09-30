package com.vaiinilla.app.data.operational

import com.vaiinilla.app.data.contract.MetaDto
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.repository.AccountCollection
import com.vaiinilla.app.domain.repository.AccountOrder
import com.vaiinilla.app.domain.repository.BoardOrder
import com.vaiinilla.app.domain.repository.BoardTable
import com.vaiinilla.app.domain.repository.CallReason
import com.vaiinilla.app.domain.repository.CallStatus
import com.vaiinilla.app.domain.repository.SpaceAccount
import com.vaiinilla.app.domain.repository.SpaceAvailability
import com.vaiinilla.app.domain.repository.SpaceAvailabilityState
import com.vaiinilla.app.domain.repository.SpaceSessionDetail
import com.vaiinilla.app.domain.repository.SpaceSessionInfo
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.domain.repository.TableCallTaker
import com.vaiinilla.app.domain.repository.TableSpace
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.math.BigDecimal
import java.math.RoundingMode
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

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class DeliverTransitionRequestDto(
    @SerialName("estado_objetivo") val target: String,
    @SerialName("version_esperada") val expectedVersion: Int,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("qr_token") val qrToken: String? = null,
)

@Serializable
data class BuyerCallRequestDto(
    val motivo: String,
    @SerialName("pedido_id") val orderId: String? = null,
)

@Serializable
data class SpaceAvailabilityDto(
    val espacio: TableSpaceDto,
    val estado: String,
    val saldada: Boolean = true,
    @SerialName("fin_previsto") val endsAt: String? = null,
    @SerialName("libera_en") val releasesAt: String? = null,
    @SerialName("restante_seg") val remainingSeconds: Int? = null,
    @SerialName("gracia_min") val graceMinutes: Int = 5,
    @SerialName("inicio") val startedAt: String? = null,
)

@Serializable
data class AvailabilityEnvelopeDto(
    val data: List<SpaceAvailabilityDto>,
    val meta: MetaDto? = null,
    val error: JsonElement? = null,
)

@Serializable
data class SpaceSessionInfoDto(
    val id: String,
    val inicio: String,
    @SerialName("fin_previsto") val endsAt: String? = null,
    val version: Int,
)

@Serializable
data class AccountOrderDto(
    val id: String,
    val folio: Int,
    val estado: String,
    val total: Double,
    @SerialName("pago_diferido") val payAtEnd: Boolean = false,
    @SerialName("pendiente_cobro") val pending: Boolean = false,
    val cliente: TableCallClientDto? = null,
    @SerialName("items_resumen") val itemsSummary: String = "",
)

@Serializable
data class SpaceAccountDto(
    val pedidos: List<AccountOrderDto> = emptyList(),
    val total: Double = 0.0,
    val pendiente: Double = 0.0,
    val pagado: Double = 0.0,
    val saldada: Boolean = true,
)

@Serializable
data class SpaceSessionDetailDto(
    val espacio: TableSpaceDto,
    val estado: String,
    val saldada: Boolean = true,
    @SerialName("fin_previsto") val endsAt: String? = null,
    @SerialName("libera_en") val releasesAt: String? = null,
    @SerialName("restante_seg") val remainingSeconds: Int? = null,
    @SerialName("gracia_min") val graceMinutes: Int = 5,
    @SerialName("inicio") val startedAt: String? = null,
    val sesion: SpaceSessionInfoDto? = null,
    val cuenta: SpaceAccountDto? = null,
)

@Serializable
data class SpaceSessionDetailEnvelopeDto(
    val data: SpaceSessionDetailDto,
    val meta: MetaDto? = null,
    val error: JsonElement? = null,
)

@Serializable
data class AccountCollectionDto(
    @SerialName("pedidos_cobrados") val ordersCollected: Int,
    val total: String,
    @SerialName("monto_recibido") val received: String,
    val cambio: String,
    /** Lo que sigue sin cobrar de la cuenta; un servidor anterior no lo envía. */
    val restante: String = "0.00",
)

@Serializable
data class AccountCollectionEnvelopeDto(
    val data: AccountCollectionDto,
    val meta: MetaDto? = null,
    val error: JsonElement? = null,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class OpenSessionRequestDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("duracion_min") val durationMinutes: Int? = null,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ExtendSessionRequestDto(
    @SerialName("minutos") val minutes: Int,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("version") val expectedVersion: Int? = null,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ReleaseSpaceRequestDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("version") val expectedVersion: Int? = null,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class CollectAccountRequestDto(
    @SerialName("monto_recibido") val received: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("total_esperado") val expectedTotal: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("pedido_ids") val orderIds: List<String>? = null,
)

/** Solo la política de entrega del estado operativo: el resto no le hace falta al mesero. */
@Serializable
data class DeliveryPolicyDto(
    @SerialName("entrega_requiere_qr") val deliveryRequiresQr: Boolean = true,
)

@Serializable
data class DeliveryPolicyEnvelopeDto(
    val data: DeliveryPolicyDto,
    val meta: MetaDto? = null,
    val error: JsonElement? = null,
)

private fun Double.toMoney(): String = BigDecimal.valueOf(this).setScale(2, RoundingMode.HALF_UP).toPlainString()

fun TableSpaceDto.toDomain(): TableSpace = TableSpace(id = id, name = nombre, type = tipo)

fun SpaceAvailabilityDto.toDomain(): SpaceAvailability =
    SpaceAvailability(
        space = espacio.toDomain(),
        state = SpaceAvailabilityState.fromWireValue(estado),
        settled = saldada,
        endsAt = endsAt,
        releasesAt = releasesAt,
        remainingSeconds = remainingSeconds,
        graceMinutes = graceMinutes,
        startedAt = startedAt,
    )

fun AccountOrderDto.toDomain(): AccountOrder =
    AccountOrder(
        id = id,
        folio = folio,
        state = estado,
        total = total.toMoney(),
        clientName = cliente?.nombre,
        itemsSummary = itemsSummary,
        payAtEnd = payAtEnd,
        pending = pending,
    )

fun SpaceAccountDto.toDomain(): SpaceAccount =
    SpaceAccount(
        orders = pedidos.map { it.toDomain() },
        total = total.toMoney(),
        pending = pendiente.toMoney(),
        paid = pagado.toMoney(),
        settled = saldada,
    )

fun SpaceSessionDetailDto.toDomain(): SpaceSessionDetail =
    SpaceSessionDetail(
        availability =
            SpaceAvailability(
                space = espacio.toDomain(),
                state = SpaceAvailabilityState.fromWireValue(estado),
                settled = saldada,
                endsAt = endsAt,
                releasesAt = releasesAt,
                remainingSeconds = remainingSeconds,
                graceMinutes = graceMinutes,
                startedAt = startedAt,
            ),
        session =
            sesion?.let {
                SpaceSessionInfo(
                    id = it.id,
                    startedAt = it.inicio,
                    endsAt = it.endsAt,
                    version = it.version,
                )
            },
        account = cuenta?.toDomain(),
    )

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

        fun parseAvailability(raw: String): List<SpaceAvailability> {
            val envelope = json.decodeFromString<AvailabilityEnvelopeDto>(raw)
            require(envelope.error == null) { "La API devolvió un error en el envelope." }
            return envelope.data.map { it.toDomain() }
        }

        fun parseSessionDetail(raw: String): SpaceSessionDetail {
            val envelope = json.decodeFromString<SpaceSessionDetailEnvelopeDto>(raw)
            require(envelope.error == null) { "La API devolvió un error en el envelope." }
            return envelope.data.toDomain()
        }

        fun parseAccountCollection(raw: String): AccountCollection {
            val envelope = json.decodeFromString<AccountCollectionEnvelopeDto>(raw)
            require(envelope.error == null) { "La API devolvió un error en el envelope." }
            return AccountCollection(
                ordersCollected = envelope.data.ordersCollected,
                total = envelope.data.total,
                received = envelope.data.received,
                change = envelope.data.cambio,
                remaining = envelope.data.restante,
            )
        }

        fun parseDeliveryRequiresQr(raw: String): Boolean {
            val envelope = json.decodeFromString<DeliveryPolicyEnvelopeDto>(raw)
            require(envelope.error == null) { "La API devolvió un error en el envelope." }
            return envelope.data.deliveryRequiresQr
        }

        fun encodeOpenSession(durationMinutes: Int?): String =
            json.encodeToString(OpenSessionRequestDto(durationMinutes))

        fun encodeExtendSession(
            minutes: Int,
            expectedVersion: Int?,
        ): String = json.encodeToString(ExtendSessionRequestDto(minutes = minutes, expectedVersion = expectedVersion))

        fun encodeReleaseSpace(expectedVersion: Int?): String =
            json.encodeToString(ReleaseSpaceRequestDto(expectedVersion))

        fun encodeCollectAccount(
            received: String,
            expectedTotal: String?,
            orderIds: List<String>? = null,
        ): String =
            json.encodeToString(
                CollectAccountRequestDto(received = received, expectedTotal = expectedTotal, orderIds = orderIds),
            )

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
            qrToken: String?,
        ): String =
            json.encodeToString(
                DeliverTransitionRequestDto(
                    target = "entregado",
                    expectedVersion = expectedVersion,
                    qrToken = qrToken?.trim()?.takeIf { it.isNotEmpty() },
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
