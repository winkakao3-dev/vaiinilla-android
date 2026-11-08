package com.vaiinilla.app.data.reservations

import com.vaiinilla.app.core.network.ApiJson
import com.vaiinilla.app.core.venue.VenueContextStore
import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.domain.model.BusyInterval
import com.vaiinilla.app.domain.model.CourtDay
import com.vaiinilla.app.domain.model.CourtProfile
import com.vaiinilla.app.domain.model.CourtSchedule
import com.vaiinilla.app.domain.model.CustomerPrice
import com.vaiinilla.app.domain.model.Reservation
import com.vaiinilla.app.domain.model.ReservationPayment
import com.vaiinilla.app.domain.model.ReservationState
import com.vaiinilla.app.domain.model.VenueContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import javax.inject.Inject

@Serializable
data class CustomerPriceDto(
    val tarjeta: String,
    @SerialName("efectivo_saldo") val efectivoSaldo: String,
)

@Serializable
data class BusyIntervalDto(
    val inicio: String,
    val fin: String,
    val motivo: String = "reserva",
)

@Serializable
data class CourtRefDto(
    val id: Int,
    val nombre: String? = null,
    val tipo: String? = null,
)

/** Ficha que escribe el dueño en el panel (migración 50). Todo opcional. */
@Serializable
data class CourtProfileDto(
    val descripcion: String? = null,
    @SerialName("imagen_url") val imagenUrl: String? = null,
    val caracteristicas: List<String> = emptyList(),
    val deporte: String? = null,
    val superficie: String? = null,
)

@Serializable
data class CourtScheduleDto(
    val espacio: CourtRefDto,
    val ficha: CourtProfileDto? = null,
    @SerialName("precio_hora") val precioHora: String? = null,
    @SerialName("precio_hora_cliente") val precioHoraCliente: CustomerPriceDto? = null,
    val rentable: Boolean = false,
    val ocupado: List<BusyIntervalDto> = emptyList(),
    /** Las del establecimiento desde el mínimo de este espacio; un servidor anterior no las manda. */
    @SerialName("duraciones_min") val duracionesMin: List<Int> = emptyList(),
    /** R25: cuántos caben (1-99); null = sin tope propio. Un servidor anterior no la manda. */
    val capacidad: Int? = null,
)

@Serializable
data class CourtDayDto(
    val fecha: String,
    val hoy: String,
    @SerialName("zona_horaria") val zonaHoraria: String,
    val abre: String,
    val cierra: String,
    @SerialName("bloque_min") val bloqueMin: Int = 30,
    @SerialName("duraciones_min") val duracionesMin: List<Int> = listOf(60, 90, 120),
    @SerialName("dias_adelanto") val diasAdelanto: Int = 14,
    @SerialName("apartado_min") val apartadoMin: Int = 10,
    val ahora: String,
    val canchas: List<CourtScheduleDto> = emptyList(),
    // Un servidor anterior no lo manda: se sigue ofreciendo la renta sin cuenta.
    @SerialName("requiere_cuenta") val requiereCuenta: Boolean = false,
    /** «boliche» nombra pistas; un servidor anterior no lo manda. */
    @SerialName("tipo_establecimiento") val tipoEstablecimiento: String? = null,
    /** Día sin servicio; un servidor anterior no lo manda. */
    val cerrado: Boolean = false,
)

@Serializable
data class ReservationDto(
    val id: String,
    val espacio: CourtRefDto,
    val inicio: String,
    val fin: String,
    @SerialName("duracion_min") val duracionMin: Int,
    val monto: String,
    @SerialName("precio_cliente") val precioCliente: CustomerPriceDto? = null,
    val estado: String,
    @SerialName("expira_en") val expiraEn: String? = null,
    @SerialName("pedido_id") val pedidoId: String? = null,
    val canal: String = "cliente",
    @SerialName("nombre_cliente") val nombreCliente: String? = null,
    val jugadores: Int? = null,
    val version: Int = 1,
)

@Serializable
private data class CourtDayEnvelopeDto(
    val data: CourtDayDto,
    val error: JsonElement? = null,
)

@Serializable
private data class ReservationEnvelopeDto(
    val data: ReservationDto,
    val error: JsonElement? = null,
)

@Serializable
private data class ReservationListEnvelopeDto(
    val data: List<ReservationDto>,
    val error: JsonElement? = null,
)

@Serializable
private data class CashDto(
    @SerialName("monto_recibido") val montoRecibido: String? = null,
    val cambio: String? = null,
)

/** Contrato JSON de /api/v1/reservas. El pedido de renta se lee con el mismo parser de pedidos. */
class ReservationContractJson
    @Inject
    constructor(
        private val orderContractJson: OrderContractJson,
        private val venueContext: VenueContextStore,
    ) {
        private val json =
            ApiJson.omitNullsWithDefaults

        fun parseDay(raw: String): CourtDay {
            val envelope = json.decodeFromString<CourtDayEnvelopeDto>(raw)
            require(envelope.error == null || envelope.error is JsonNull) { "La API devolvió un error." }
            envelope.data.tipoEstablecimiento?.let(venueContext::update)
            return envelope.data.toDomain(venueContext.current)
        }

        fun parseReservation(raw: String): Reservation {
            val envelope = json.decodeFromString<ReservationEnvelopeDto>(raw)
            require(envelope.error == null || envelope.error is JsonNull) { "La API devolvió un error." }
            return envelope.data.toDomain()
        }

        fun parseReservations(raw: String): List<Reservation> {
            val envelope = json.decodeFromString<ReservationListEnvelopeDto>(raw)
            require(envelope.error == null || envelope.error is JsonNull) { "La API devolvió un error." }
            return envelope.data.map { it.toDomain() }
        }

        /** `{ data: { reserva, pedido, cobro } }`: el pedido de renta es un pedido normal. */
        fun parsePayment(raw: String): ReservationPayment {
            val root = json.parseToJsonElement(raw).jsonObject
            val data = requireNotNull(root["data"]?.jsonObject) { "Respuesta sin data." }
            val reservation =
                json
                    .decodeFromJsonElement(
                        ReservationDto.serializer(),
                        data.getValue("reserva"),
                    ).toDomain()
            val order =
                data["pedido"]?.takeIf { it !is JsonNull }?.let { pedido ->
                    orderContractJson.parseCreatedOrder(
                        buildJsonObject {
                            put("data", pedido)
                            put(
                                "meta",
                                root["meta"] as? JsonObject
                                    ?: buildJsonObject {
                                        put("page", JsonNull)
                                        put("total_pages", JsonNull)
                                        put("total_items", JsonNull)
                                        put("cursor", JsonNull)
                                    },
                            )
                            put("error", JsonNull)
                        }.toString(),
                    )
                }
            val cash =
                data["cobro"]?.takeIf { it is JsonObject }?.let {
                    json.decodeFromJsonElement(CashDto.serializer(), it)
                }
            return ReservationPayment(
                reservation = reservation,
                order = order,
                cashReceived = cash?.montoRecibido,
                cashChange = cash?.cambio,
            )
        }

        fun encodeCreate(
            courtId: Int,
            start: Instant?,
            durationMinutes: Int,
            customerName: String?,
            players: Int? = null,
        ): String =
            buildJsonObject {
                put("espacio_id", courtId)
                start?.let { put("inicio", it.toString()) }
                put("duracion_min", durationMinutes)
                customerName?.takeIf { it.isNotBlank() }?.let { put("nombre_cliente", it.trim()) }
                players?.let { put("jugadores", it) }
            }.toString()

        fun encodePay(
            method: String,
            cashReceived: String?,
        ): String =
            buildJsonObject {
                put("metodo_pago", method)
                cashReceived?.let { put("monto_recibido", it) }
            }.toString()

        /** #28: cancelar una renta pagada desde el personal exige `{monto_reembolso, motivo}`. */
        fun encodeRefund(
            amount: String,
            reason: String,
        ): String =
            buildJsonObject {
                put("monto_reembolso", amount)
                put("motivo", reason.trim())
            }.toString()
    }

private fun CustomerPriceDto.toDomain() = CustomerPrice(card = tarjeta, cashOrBalance = efectivoSaldo)

private fun CourtDayDto.toDomain(venue: VenueContext): CourtDay =
    CourtDay(
        date = fecha,
        today = hoy,
        timeZone = zonaHoraria,
        opensAt = Instant.parse(abre),
        closesAt = Instant.parse(cierra),
        blockMinutes = bloqueMin,
        durations = duracionesMin,
        daysAhead = diasAdelanto,
        holdMinutes = apartadoMin,
        now = Instant.parse(ahora),
        requiresAccount = requiereCuenta,
        closed = cerrado,
        courts =
            canchas.map { court ->
                CourtSchedule(
                    id = court.espacio.id,
                    name = court.espacio.nombre ?: "${venue.vocabulary.courtTitle} ${court.espacio.id}",
                    pricePerHour = court.precioHora,
                    customerPricePerHour = court.precioHoraCliente?.toDomain(),
                    rentable = court.rentable,
                    profile =
                        CourtProfile(
                            description = court.ficha?.descripcion?.takeIf { it.isNotBlank() },
                            imageUrl = court.ficha?.imagenUrl?.takeIf { it.isNotBlank() },
                            features = court.ficha?.caracteristicas.orEmpty(),
                            sport = court.ficha?.deporte?.takeIf { it.isNotBlank() },
                            surface = court.ficha?.superficie?.takeIf { it.isNotBlank() },
                        ),
                    busy =
                        court.ocupado.map {
                            BusyInterval(
                                start = Instant.parse(it.inicio),
                                end = Instant.parse(it.fin),
                                reason = it.motivo,
                            )
                        },
                    durations = court.duracionesMin.sorted(),
                    capacity = court.capacidad?.takeIf { it > 0 },
                )
            },
    )

fun ReservationDto.toDomain() =
    Reservation(
        id = id,
        courtId = espacio.id,
        courtName = espacio.nombre,
        start = Instant.parse(inicio),
        end = Instant.parse(fin),
        durationMinutes = duracionMin,
        amount = monto,
        customerPrice = precioCliente?.toDomain(),
        state = ReservationState.fromWire(estado),
        holdExpiresAt = expiraEn?.let(Instant::parse),
        orderId = pedidoId,
        channel = canal,
        customerName = nombreCliente,
        players = jugadores,
        version = version,
    )
