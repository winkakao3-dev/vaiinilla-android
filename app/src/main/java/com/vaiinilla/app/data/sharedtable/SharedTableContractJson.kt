package com.vaiinilla.app.data.sharedtable

import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.SharedTable
import com.vaiinilla.app.domain.model.SharedTableAmounts
import com.vaiinilla.app.domain.model.SharedTableGroup
import com.vaiinilla.app.domain.model.SharedTableOrder
import com.vaiinilla.app.domain.model.SharedTableParticipant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.math.BigDecimal
import javax.inject.Inject

@Serializable
private data class SpaceRefDto(
    val id: Int,
    val nombre: String,
    val tipo: String,
)

@Serializable
private data class ParticipantDto(
    val alias: String,
    @SerialName("soy_yo") val soyYo: Boolean,
)

@Serializable
private data class TableOrderDto(
    val id: String? = null,
    val folio: Int,
    val estado: String,
    @SerialName("items_resumen") val itemsResumen: String = "",
    val total: String,
    @SerialName("pendiente_cobro") val pendienteCobro: Boolean,
    val pagara: String? = null,
    @SerialName("lo_pago_yo") val loPagoYo: Boolean = false,
)

@Serializable
private data class GroupDto(
    val alias: String? = null,
    @SerialName("soy_yo") val soyYo: Boolean,
    val pedidos: List<TableOrderDto>,
    val total: String,
    val pagado: String,
    val pendiente: String,
)

@Serializable
private data class AmountsDto(
    val total: String,
    val pagado: String,
    val pendiente: String,
)

@Serializable
private data class TableDto(
    val espacio: SpaceRefDto,
    @SerialName("mi_alias") val miAlias: String,
    @SerialName("cuenta_abierta") val cuentaAbierta: Boolean,
    val participantes: List<ParticipantDto>,
    val grupos: List<GroupDto>,
    val totales: AmountsDto,
    @SerialName("mi_parte") val miParte: AmountsDto,
)

@Serializable
private data class TableEnvelopeDto(
    val data: TableDto? = null,
    val error: JsonElement? = null,
)

/** Contrato JSON de /api/v1/mesas (docs/mesa-compartida.md del backend). */
class SharedTableContractJson
    @Inject
    constructor() {
        private val json =
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }

        /** La mesa, o null si el cliente no está en ninguna (`data: null`). */
        fun parseTable(raw: String): SharedTable? {
            val envelope = json.decodeFromString<TableEnvelopeDto>(raw)
            require(envelope.error == null || envelope.error is JsonNull) { "La API devolvió un error." }
            return envelope.data?.toDomain()
        }

        fun encodeJoin(
            qrToken: String,
            alias: String,
        ): String =
            buildJsonObject {
                put("token", qrToken)
                put("alias", alias)
            }.toString()

        fun encodeClaim(
            folio: Int,
            payIt: Boolean,
        ): String =
            buildJsonObject {
                put("folio", folio)
                put("pago_yo", payIt)
            }.toString()
    }

private fun AmountsDto.toDomain() = SharedTableAmounts(BigDecimal(total), BigDecimal(pagado), BigDecimal(pendiente))

private fun TableDto.toDomain() =
    SharedTable(
        spaceId = espacio.id,
        spaceName = espacio.nombre,
        spaceType = espacio.tipo,
        myAlias = miAlias,
        accountOpen = cuentaAbierta,
        participants = participantes.map { SharedTableParticipant(it.alias, it.soyYo) },
        groups =
            grupos.map { group ->
                SharedTableGroup(
                    alias = group.alias,
                    isMe = group.soyYo,
                    orders =
                        group.pedidos.map {
                            SharedTableOrder(
                                id = it.id,
                                folio = it.folio,
                                status = OrderState.fromWireValue(it.estado),
                                itemsSummary = it.itemsResumen,
                                total = BigDecimal(it.total),
                                pendingPayment = it.pendienteCobro,
                                payer = it.pagara,
                                iPayIt = it.loPagoYo,
                            )
                        },
                    amounts =
                        SharedTableAmounts(
                            BigDecimal(group.total),
                            BigDecimal(group.pagado),
                            BigDecimal(group.pendiente),
                        ),
                )
            },
        totals = totales.toDomain(),
        myShare = miParte.toDomain(),
    )
