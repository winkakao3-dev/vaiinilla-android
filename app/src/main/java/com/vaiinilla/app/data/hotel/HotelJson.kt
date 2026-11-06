package com.vaiinilla.app.data.hotel

import com.vaiinilla.app.domain.hotel.ChargeCategory
import com.vaiinilla.app.domain.hotel.CheckoutMethod
import com.vaiinilla.app.domain.hotel.HotelConfig
import com.vaiinilla.app.domain.hotel.HotelRequest
import com.vaiinilla.app.domain.hotel.HotelRoom
import com.vaiinilla.app.domain.hotel.HotelSummary
import com.vaiinilla.app.domain.hotel.HotelTopItem
import com.vaiinilla.app.domain.hotel.NewStay
import com.vaiinilla.app.domain.hotel.RequestAction
import com.vaiinilla.app.domain.hotel.RequestStatus
import com.vaiinilla.app.domain.hotel.RequestType
import com.vaiinilla.app.domain.hotel.RoomBatchResult
import com.vaiinilla.app.domain.hotel.RoomCleaning
import com.vaiinilla.app.domain.hotel.Stay
import com.vaiinilla.app.domain.hotel.StayCharge
import com.vaiinilla.app.domain.hotel.StayOrder
import com.vaiinilla.app.domain.hotel.StayStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

/**
 * Contratos del modo Hotel (`/estancias`, `/solicitudes-hotel`, `/habitaciones`, `/hotel/resumen`).
 * Los nombres de campo siguen la sección 7.2 del plan; todo lo que el servidor puede omitir es opcional
 * para que un campo que cambie no tumbe la pantalla.
 */
class HotelJson {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            isLenient = true
        }

    // region Lectura

    fun parseStayOrNull(raw: String): Stay? = json.decodeFromString<Envelope<StayDto?>>(raw).data?.toDomain()

    fun parseStay(raw: String): Stay = json.decodeFromString<Envelope<StayDto>>(raw).data.toDomain()

    fun parseStays(raw: String): List<Stay> = json.decodeFromString<Envelope<List<StayDto>>>(raw).data.map { it.toDomain() }

    fun parseCharge(raw: String): StayCharge = json.decodeFromString<Envelope<ChargeDto>>(raw).data.toDomain()

    fun parseRequest(raw: String): HotelRequest = json.decodeFromString<Envelope<RequestDto>>(raw).data.toDomain()

    fun parseRequests(raw: String): List<HotelRequest> =
        json.decodeFromString<Envelope<List<RequestDto>>>(raw).data.map { it.toDomain() }

    fun parseRoom(raw: String): HotelRoom = json.decodeFromString<Envelope<RoomDto>>(raw).data.toDomain()

    fun parseRooms(raw: String): List<HotelRoom> = json.decodeFromString<Envelope<List<RoomDto>>>(raw).data.map { it.toDomain() }

    fun parseRoomBatch(raw: String): RoomBatchResult =
        json.decodeFromString<Envelope<RoomBatchDto>>(raw).data.let { RoomBatchResult(it.creadas, it.omitidas) }

    fun parseConfig(raw: String): HotelConfig = json.decodeFromString<Envelope<HotelConfigDto>>(raw).data.toDomain()

    fun parseSummary(raw: String): HotelSummary = json.decodeFromString<Envelope<SummaryDto>>(raw).data.toDomain()

    // endregion

    // region Escritura

    fun encodeLink(
        code: String,
        spaceId: Int?,
        qrToken: String?,
    ): String =
        buildJsonObject {
            put("codigo", code.trim())
            spaceId?.let { put("espacio_id", it) }
            qrToken?.let { put("qr_token", it) }
        }.toString()

    fun encodeDoNotDisturb(enabled: Boolean): String = buildJsonObject { put("no_molestar", enabled) }.toString()

    fun encodeRequest(
        type: RequestType,
        quantity: Int,
        note: String?,
    ): String =
        buildJsonObject {
            put("tipo", type.wire)
            put("cantidad", quantity)
            note?.trim()?.takeIf { it.isNotEmpty() }?.let { put("nota", it) }
        }.toString()

    fun encodeCheckIn(draft: NewStay): String =
        buildJsonObject {
            put("huesped", draft.guestName.trim())
            draft.contact?.trim()?.takeIf { it.isNotEmpty() }?.let { put("contacto", it) }
            put("espacio_id", draft.roomId)
            put("entrada", draft.checkIn)
            put("salida", draft.checkOut)
            draft.nightlyRate?.let { put("tarifa_noche", it) }
        }.toString()

    fun encodeCharge(
        concept: String,
        amount: String,
        category: ChargeCategory,
    ): String =
        buildJsonObject {
            put("concepto", concept.trim())
            put("monto", amount)
            put("categoria", category.wire)
        }.toString()

    fun encodeCheckout(method: CheckoutMethod): String = buildJsonObject { put("metodo", method.wire) }.toString()

    fun encodeTransition(action: RequestAction): String = buildJsonObject { put("accion", action.wire) }.toString()

    fun encodeCleaning(cleaning: RoomCleaning): String = buildJsonObject { put("limpieza", cleaning.wire) }.toString()

    fun encodeRoomBatch(
        prefix: String,
        from: Int,
        to: Int,
        floor: Int?,
        category: String?,
    ): String =
        buildJsonObject {
            put("prefijo", prefix.trim())
            put("desde", from)
            put("hasta", to)
            floor?.let { put("piso", it) }
            category?.trim()?.takeIf { it.isNotEmpty() }?.let { put("categoria", it) }
        }.toString()

    fun encodeRoomUpdate(
        floor: Int?,
        category: String?,
    ): String =
        buildJsonObject {
            put("piso", floor?.let { JsonPrimitive(it) } ?: JsonNull)
            put("categoria", category?.trim()?.takeIf { it.isNotEmpty() }?.let { JsonPrimitive(it) } ?: JsonNull)
        }.toString()

    fun encodeHotelConfig(config: HotelConfig): String =
        buildJsonObject {
            put("cargo_habitacion_activo", config.chargeToRoom)
            put("tarjeta_al_pedir", config.cardAtOrderEnabled)
            put("codigo_entrega_activo", config.deliveryCode)
            put("cocina_desde", config.kitchenFrom?.let { JsonPrimitive(it) } ?: JsonNull)
            put("cocina_hasta", config.kitchenTo?.let { JsonPrimitive(it) } ?: JsonNull)
            put("solicitudes_activas", config.requestsEnabled)
            put("no_molestar_activo", config.doNotDisturbEnabled)
            put("salida_tardia_hora", config.lateCheckoutTime?.let { JsonPrimitive(it) } ?: JsonNull)
            put("salida_tardia_precio", config.lateCheckoutPrice?.let { JsonPrimitive(it) } ?: JsonNull)
            put("hora_checkout", config.checkoutTime?.let { JsonPrimitive(it) } ?: JsonNull)
        }.toString()

    // endregion
}

@Serializable
private data class Envelope<T>(
    val data: T,
)

private fun JsonPrimitive?.text(): String? = if (this == null || this is JsonNull) null else contentOrNull?.trim()?.ifEmpty { null }

@Serializable
private data class ChargeDto(
    val id: JsonPrimitive,
    val concepto: String = "",
    val monto: JsonPrimitive? = null,
    val categoria: String? = null,
    @SerialName("creado_en") val creadoEn: String? = null,
) {
    fun toDomain() =
        StayCharge(
            id = id.text().orEmpty(),
            concept = concepto,
            amount = monto.text() ?: "0",
            category = ChargeCategory.fromWire(categoria),
            createdAt = creadoEn,
        )
}

@Serializable
private data class StayOrderDto(
    @SerialName("pedido_id") val pedidoId: JsonPrimitive? = null,
    val id: JsonPrimitive? = null,
    val folio: Int? = null,
    val total: JsonPrimitive? = null,
    val estado: String = "",
    @SerialName("creado_en") val creadoEn: String? = null,
) {
    fun toDomain() =
        StayOrder(
            orderId = (pedidoId ?: id).text().orEmpty(),
            folio = folio,
            total = total.text() ?: "0",
            state = estado,
            createdAt = creadoEn,
        )
}

@Serializable
private data class StayDto(
    val id: JsonPrimitive,
    val huesped: String = "",
    val contacto: String? = null,
    @SerialName("usuario_id") val usuarioId: JsonPrimitive? = null,
    val codigo: JsonPrimitive? = null,
    @SerialName("espacio_id") val espacioId: Int? = null,
    @SerialName("espacio_nombre") val espacioNombre: String? = null,
    val piso: Int? = null,
    val entrada: String = "",
    val salida: String = "",
    @SerialName("tarifa_noche") val tarifaNoche: JsonPrimitive? = null,
    val estado: String? = null,
    val noches: Int? = null,
    val cargos: List<ChargeDto> = emptyList(),
    val pedidos: List<StayOrderDto> = emptyList(),
    val total: JsonPrimitive? = null,
    @SerialName("metodo_cobro") val metodoCobro: String? = null,
    @SerialName("cobrado_en") val cobradoEn: String? = null,
    @SerialName("no_molestar") val noMolestar: Boolean? = null,
) {
    fun toDomain() =
        Stay(
            id = id.text().orEmpty(),
            guestName = huesped,
            contact = contacto,
            userId = usuarioId.text(),
            code = codigo.text(),
            roomId = espacioId,
            roomName = espacioNombre,
            floor = piso,
            checkIn = entrada,
            checkOut = salida,
            nightlyRate = tarifaNoche.text(),
            status = StayStatus.fromWire(estado),
            nights = noches ?: 1,
            charges = cargos.map { it.toDomain() },
            orders = pedidos.map { it.toDomain() },
            total = total.text(),
            paymentMethod = metodoCobro,
            paidAt = cobradoEn,
            doNotDisturb = noMolestar ?: false,
        )
}

@Serializable
private data class RequestDto(
    val id: JsonPrimitive,
    @SerialName("estancia_id") val estanciaId: JsonPrimitive? = null,
    @SerialName("espacio_id") val espacioId: Int? = null,
    @SerialName("espacio_nombre") val espacioNombre: String? = null,
    val piso: Int? = null,
    val tipo: String? = null,
    val cantidad: Int? = null,
    val nota: String? = null,
    val estado: String? = null,
    @SerialName("tomada_por") val tomadaPor: String? = null,
    @SerialName("creado_en") val creadoEn: String? = null,
) {
    fun toDomain() =
        HotelRequest(
            id = id.text().orEmpty(),
            stayId = estanciaId.text(),
            roomId = espacioId,
            roomName = espacioNombre,
            floor = piso,
            type = RequestType.fromWire(tipo),
            quantity = cantidad ?: 1,
            note = nota,
            status = RequestStatus.fromWire(estado),
            takenBy = tomadaPor,
            createdAt = creadoEn,
        )
}

@Serializable
private data class RoomDto(
    val id: Int,
    val nombre: String = "",
    val piso: Int? = null,
    val categoria: String? = null,
    val limpieza: String? = null,
    val ocupada: Boolean? = null,
    @SerialName("no_molestar") val noMolestar: Boolean? = null,
    @SerialName("solicitudes_abiertas") val solicitudesAbiertas: Int? = null,
    @SerialName("estancia_id") val estanciaId: JsonPrimitive? = null,
    val huesped: String? = null,
    @SerialName("qr_token") val qrToken: String? = null,
    @SerialName("qr_url") val qrUrl: String? = null,
) {
    fun toDomain() =
        HotelRoom(
            id = id,
            name = nombre,
            floor = piso,
            category = categoria,
            cleaning = RoomCleaning.fromWire(limpieza),
            occupied = ocupada ?: false,
            doNotDisturb = noMolestar ?: false,
            openRequests = solicitudesAbiertas ?: 0,
            stayId = estanciaId.text(),
            guestName = huesped,
            qrToken = qrToken,
            qrUrl = qrUrl,
        )
}

@Serializable
private data class RoomBatchDto(
    val creadas: Int = 0,
    val omitidas: List<String> = emptyList(),
)

@Serializable
private data class TopItemDto(
    val nombre: String = "",
    val cantidad: Int = 0,
    val total: JsonPrimitive? = null,
)

@Serializable
private data class SummaryDto(
    val ocupadas: Int = 0,
    val total: Int = 0,
    @SerialName("room_service_hoy") val roomServiceHoy: JsonPrimitive? = null,
    @SerialName("pedidos_cuartos_hoy") val pedidosCuartosHoy: Int? = null,
    @SerialName("en_cocina") val enCocina: Int? = null,
    @SerialName("solicitudes_abiertas") val solicitudesAbiertas: Int? = null,
    @SerialName("solicitud_mas_vieja_min") val solicitudMasViejaMin: Int? = null,
    @SerialName("lo_mas_pedido") val loMasPedido: List<TopItemDto> = emptyList(),
    @SerialName("salidas_hoy") val salidasHoy: Int? = null,
) {
    fun toDomain() =
        HotelSummary(
            occupied = ocupadas,
            totalRooms = total,
            roomServiceToday = roomServiceHoy.text(),
            roomOrdersToday = pedidosCuartosHoy ?: 0,
            inKitchenNow = enCocina ?: 0,
            openRequests = solicitudesAbiertas ?: 0,
            oldestRequestMinutes = solicitudMasViejaMin,
            topItems = loMasPedido.map { HotelTopItem(it.nombre, it.cantidad, it.total.text()) },
            departuresToday = salidasHoy ?: 0,
        )
}

/** `GET hotel/configuracion`; la configuración del hotel no viaja en ninguna respuesta que ya existía. */
@Serializable
data class HotelConfigDto(
    @SerialName("cargo_habitacion_activo") val cargoHabitacionActivo: Boolean = true,
    @SerialName("tarjeta_al_pedir") val tarjetaAlPedir: Boolean = false,
    @SerialName("codigo_entrega_activo") val codigoEntregaActivo: Boolean = true,
    @SerialName("cocina_desde") val cocinaDesde: String? = null,
    @SerialName("cocina_hasta") val cocinaHasta: String? = null,
    @SerialName("solicitudes_activas") val solicitudesActivas: Boolean = true,
    @SerialName("no_molestar_activo") val noMolestarActivo: Boolean = true,
    @SerialName("salida_tardia_hora") val salidaTardiaHora: String? = null,
    @SerialName("salida_tardia_precio") val salidaTardiaPrecio: JsonPrimitive? = null,
    @SerialName("hora_checkout") val horaCheckout: String? = null,
) {
    fun toDomain() =
        HotelConfig(
            chargeToRoom = cargoHabitacionActivo,
            cardAtOrderEnabled = tarjetaAlPedir,
            deliveryCode = codigoEntregaActivo,
            kitchenFrom = cocinaDesde?.take(5)?.ifBlank { null },
            kitchenTo = cocinaHasta?.take(5)?.ifBlank { null },
            requestsEnabled = solicitudesActivas,
            doNotDisturbEnabled = noMolestarActivo,
            lateCheckoutTime = salidaTardiaHora?.take(5)?.ifBlank { null },
            lateCheckoutPrice = salidaTardiaPrecio.text(),
            checkoutTime = horaCheckout?.take(5)?.ifBlank { null },
        )
}