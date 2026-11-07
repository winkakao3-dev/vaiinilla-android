package com.vaiinilla.app.data.hotel

import com.vaiinilla.app.domain.hotel.ChargeCategory
import com.vaiinilla.app.domain.hotel.CheckoutMethod
import com.vaiinilla.app.domain.hotel.HotelConfig
import com.vaiinilla.app.domain.hotel.HotelDeparture
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
import com.vaiinilla.app.domain.hotel.StayAccount
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
import java.time.Duration
import java.time.Instant

/**
 * Contratos del modo Hotel según `docs/hotel.md` del backend (`feature/hotel-backend`): `/estancias`,
 * `/solicitudes-hotel`, `/habitaciones`, `/hotel/configuracion` y `/hotel/resumen`. Todo lo que el servidor
 * puede omitir es opcional para que un campo que cambie no tumbe la pantalla.
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

    /** `GET estancias` responde `{ hoy, estancias: [...] }`. */
    fun parseStays(raw: String): List<Stay> =
        json
            .decodeFromString<Envelope<StayListDto>>(raw)
            .data.estancias
            .map { it.toDomain() }

    /** `POST estancias/:id/cargos` responde la estancia con el cargo: el recién agregado es el último. */
    fun parseAddedCharge(raw: String): StayCharge =
        parseStay(raw).charges.lastOrNull() ?: throw IllegalStateException("La estancia no trae el cargo.")

    fun parseRequest(raw: String): HotelRequest = json.decodeFromString<Envelope<RequestDto>>(raw).data.toDomain()

    fun parseRequests(raw: String): List<HotelRequest> =
        json.decodeFromString<Envelope<List<RequestDto>>>(raw).data.map { it.toDomain() }

    /** Sirve también para `POST habitaciones/:id/limpieza`, que solo trae `id`, `nombre` y `limpieza`. */
    fun parseRoom(raw: String): HotelRoom = json.decodeFromString<Envelope<RoomDto>>(raw).data.toDomain()

    fun parseRooms(raw: String): List<HotelRoom> =
        json.decodeFromString<Envelope<List<RoomDto>>>(raw).data.map {
            it.toDomain()
        }

    fun parseRoomBatch(raw: String): RoomBatchResult =
        json.decodeFromString<Envelope<RoomBatchDto>>(raw).data.let {
            RoomBatchResult(it.creadas, it.omitidas, it.convertidas)
        }

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
            // El servidor exige uno solo de los dos; el QR manda cuando el lugar trae los dos.
            if (qrToken != null) put("qr_token", qrToken) else spaceId?.let { put("espacio_id", it) }
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

    /** `entrada` y `salida` (se manda `salida` o `noches`, no los dos). */
    fun encodeCheckIn(draft: NewStay): String =
        buildJsonObject {
            put("espacio_id", draft.roomId)
            put("huesped_nombre", draft.guestName.trim())
            draft.contact
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { put("contacto", it) }
            draft.checkIn?.let { put("entrada", it) }
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

    fun encodeCheckout(
        method: CheckoutMethod,
        receivedAmount: String?,
        expectedTotal: String?,
        expectedVersion: Int?,
    ): String =
        buildJsonObject {
            put("metodo_pago", method.wire)
            receivedAmount?.let { put("monto_recibido", it) }
            expectedTotal?.let { put("total_esperado", it) }
            expectedVersion?.let { put("version_esperada", it) }
        }.toString()

    fun encodeExpectedVersion(version: Int): String = buildJsonObject { put("version_esperada", version) }.toString()

    fun encodeTransition(
        action: RequestAction,
        expectedVersion: Int?,
    ): String =
        buildJsonObject {
            put("accion", action.wire)
            expectedVersion?.let { put("version_esperada", it) }
        }.toString()

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
            // El servidor rechaza `hora_checkout: null` (400): sin hora, la clave no se manda.
            config.checkoutTime?.let { put("hora_checkout", it) }
        }.toString()

    // endregion
}

@Serializable
private data class Envelope<T>(
    val data: T,
)

private fun JsonPrimitive?.text(): String? =
    if (this == null ||
        this is JsonNull
    ) {
        null
    } else {
        contentOrNull?.trim()?.ifEmpty { null }
    }

@Serializable
private data class StayListDto(
    val hoy: String? = null,
    val estancias: List<StayDto> = emptyList(),
)

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
    val id: JsonPrimitive? = null,
    val folio: JsonPrimitive? = null,
    val total: JsonPrimitive? = null,
    val estado: String = "",
    @SerialName("items_resumen") val itemsResumen: String? = null,
    @SerialName("por_cobrar") val porCobrar: Boolean? = null,
    @SerialName("creado_en") val creadoEn: String? = null,
) {
    fun toDomain() =
        StayOrder(
            orderId = id.text().orEmpty(),
            folio = folio.text(),
            total = total.text() ?: "0",
            state = estado,
            createdAt = creadoEn,
            itemsSummary = itemsResumen,
            pendingPayment = porCobrar ?: false,
        )
}

@Serializable
private data class AccountDto(
    @SerialName("noches_total") val nochesTotal: JsonPrimitive? = null,
    @SerialName("cargos_total") val cargosTotal: JsonPrimitive? = null,
    @SerialName("pedidos_total") val pedidosTotal: JsonPrimitive? = null,
    val total: JsonPrimitive? = null,
    @SerialName("pedidos_pendientes") val pedidosPendientes: Int? = null,
) {
    fun toDomain() =
        StayAccount(
            nightsTotal = nochesTotal.text(),
            chargesTotal = cargosTotal.text(),
            ordersTotal = pedidosTotal.text(),
            total = total.text(),
            pendingOrders = pedidosPendientes ?: 0,
        )
}

@Serializable
private data class StayDto(
    val id: JsonPrimitive,
    val huesped: String = "",
    val contacto: String? = null,
    @SerialName("usuario_id") val usuarioId: JsonPrimitive? = null,
    @SerialName("usuarios_vinculados") val usuariosVinculados: Int? = null,
    val codigo: JsonPrimitive? = null,
    @SerialName("espacio_id") val espacioId: Int? = null,
    @SerialName("espacio_nombre") val espacioNombre: String? = null,
    val piso: Int? = null,
    val categoria: String? = null,
    val entrada: String = "",
    val salida: String = "",
    val noches: Int? = null,
    @SerialName("salida_prevista") val salidaPrevista: String? = null,
    @SerialName("tarifa_noche") val tarifaNoche: JsonPrimitive? = null,
    val estado: String? = null,
    val cargos: List<ChargeDto> = emptyList(),
    val pedidos: List<StayOrderDto> = emptyList(),
    val total: JsonPrimitive? = null,
    val cuenta: AccountDto? = null,
    @SerialName("metodo_cobro") val metodoCobro: String? = null,
    @SerialName("total_cobrado") val totalCobrado: JsonPrimitive? = null,
    @SerialName("cobrado_en") val cobradoEn: String? = null,
    @SerialName("no_molestar") val noMolestar: Boolean? = null,
    val version: Int? = null,
    val ajustes: HotelConfigDto? = null,
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
            total = total.text() ?: cuenta?.total.text(),
            paymentMethod = metodoCobro,
            paidAt = cobradoEn,
            doNotDisturb = noMolestar ?: false,
            category = categoria,
            version = version ?: 1,
            expectedDeparture = salidaPrevista,
            linkedUsers = usuariosVinculados ?: 0,
            account = cuenta?.toDomain(),
            totalCharged = totalCobrado.text(),
            settings = ajustes?.toDomain(),
        )
}

@Serializable
private data class RequestRoomDto(
    val id: Int? = null,
    val nombre: String? = null,
    val piso: Int? = null,
)

@Serializable
private data class NamedDto(
    val nombre: String? = null,
)

@Serializable
private data class RequestDto(
    val id: JsonPrimitive,
    @SerialName("estancia_id") val estanciaId: JsonPrimitive? = null,
    val habitacion: RequestRoomDto? = null,
    val tipo: String? = null,
    val cantidad: Int? = null,
    val nota: String? = null,
    val estado: String? = null,
    val huesped: NamedDto? = null,
    @SerialName("tomada_por") val tomadaPor: NamedDto? = null,
    @SerialName("creado_en") val creadoEn: String? = null,
    val version: Int? = null,
) {
    fun toDomain() =
        HotelRequest(
            id = id.text().orEmpty(),
            stayId = estanciaId.text(),
            roomId = habitacion?.id,
            roomName = habitacion?.nombre,
            floor = habitacion?.piso,
            type = RequestType.fromWire(tipo),
            quantity = cantidad ?: 1,
            note = nota,
            status = RequestStatus.fromWire(estado),
            takenBy = tomadaPor?.nombre,
            createdAt = creadoEn,
            version = version ?: 1,
            guestName = huesped?.nombre,
        )
}

@Serializable
private data class RoomStayDto(
    val id: JsonPrimitive? = null,
    @SerialName("huesped_nombre") val huespedNombre: String? = null,
    @SerialName("salida_prevista") val salidaPrevista: String? = null,
)

@Serializable
private data class RoomDto(
    val id: Int,
    val nombre: String = "",
    val piso: Int? = null,
    val categoria: String? = null,
    val limpieza: String? = null,
    val ocupada: Boolean? = null,
    @SerialName("no_molestar") val noMolestar: Boolean? = null,
    val estancia: RoomStayDto? = null,
    @SerialName("solicitudes_abiertas") val solicitudesAbiertas: Int? = null,
    @SerialName("pedidos_en_curso") val pedidosEnCurso: Int? = null,
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
            occupied = ocupada ?: (estancia != null),
            doNotDisturb = noMolestar ?: false,
            openRequests = solicitudesAbiertas ?: 0,
            stayId = estancia?.id.text(),
            guestName = estancia?.huespedNombre,
            qrToken = qrToken,
            qrUrl = qrUrl,
            stayDeparture = estancia?.salidaPrevista,
            ordersInProgress = pedidosEnCurso ?: 0,
        )
}

@Serializable
private data class RoomBatchDto(
    val solicitadas: Int = 0,
    val creadas: Int = 0,
    val convertidas: Int = 0,
    val omitidas: List<String> = emptyList(),
)

@Serializable
private data class SummaryRoomsDto(
    val total: Int = 0,
    val ocupadas: Int = 0,
    @SerialName("por_limpiar") val porLimpiar: Int = 0,
    @SerialName("sin_molestar") val sinMolestar: Int = 0,
)

@Serializable
private data class SummaryRoomServiceDto(
    @SerialName("pedidos_hoy") val pedidosHoy: Int = 0,
    @SerialName("ventas_hoy") val ventasHoy: JsonPrimitive? = null,
    @SerialName("pedidos_a_cuartos") val pedidosACuartos: Int = 0,
    @SerialName("en_cocina_ahora") val enCocinaAhora: Int = 0,
)

@Serializable
private data class SummaryRequestsDto(
    val abiertas: Int = 0,
    @SerialName("mas_vieja_desde") val masViejaDesde: String? = null,
)

@Serializable
private data class TopItemDto(
    val producto: String = "",
    val cantidad: Int = 0,
)

@Serializable
private data class DepartureDto(
    @SerialName("estancia_id") val estanciaId: JsonPrimitive? = null,
    @SerialName("huesped_nombre") val huespedNombre: String = "",
    val habitacion: RequestRoomDto? = null,
    @SerialName("salida_prevista") val salidaPrevista: String? = null,
)

@Serializable
private data class SummaryDto(
    val habitaciones: SummaryRoomsDto = SummaryRoomsDto(),
    @SerialName("room_service") val roomService: SummaryRoomServiceDto = SummaryRoomServiceDto(),
    val solicitudes: SummaryRequestsDto = SummaryRequestsDto(),
    @SerialName("lo_mas_pedido") val loMasPedido: List<TopItemDto> = emptyList(),
    @SerialName("llegadas_hoy") val llegadasHoy: Int = 0,
    @SerialName("salidas_hoy") val salidasHoy: List<DepartureDto> = emptyList(),
    @SerialName("consultado_en") val consultadoEn: String? = null,
) {
    fun toDomain() =
        HotelSummary(
            occupied = habitaciones.ocupadas,
            totalRooms = habitaciones.total,
            roomServiceToday = roomService.ventasHoy.text(),
            roomOrdersToday = roomService.pedidosHoy,
            inKitchenNow = roomService.enCocinaAhora,
            openRequests = solicitudes.abiertas,
            oldestRequestMinutes = minutesSince(solicitudes.masViejaDesde, consultadoEn),
            topItems = loMasPedido.map { HotelTopItem(it.producto, it.cantidad) },
            departuresToday = salidasHoy.size,
            dirtyRooms = habitaciones.porLimpiar,
            doNotDisturbRooms = habitaciones.sinMolestar,
            ordersToRooms = roomService.pedidosACuartos,
            arrivalsToday = llegadasHoy,
            departures =
                salidasHoy.map {
                    HotelDeparture(
                        it.estanciaId.text().orEmpty(),
                        it.huespedNombre,
                        it.habitacion?.nombre.orEmpty(),
                        it.salidaPrevista,
                    )
                },
        )
}

/** Cuánto lleva abierta la solicitud más vieja, medido contra la hora del servidor (`consultado_en`). */
private fun minutesSince(
    since: String?,
    now: String?,
): Int? =
    runCatching {
        val start = Instant.parse(since ?: return null)
        val end = now?.let(Instant::parse) ?: Instant.now()
        Duration
            .between(start, end)
            .toMinutes()
            .toInt()
            .coerceAtLeast(0)
    }.getOrNull()

/** `GET hotel/configuracion` y los `ajustes` del huésped; ninguna respuesta que ya existía lleva estos campos. */
@Serializable
data class HotelConfigDto(
    val activo: Boolean = true,
    @SerialName("cargo_habitacion_activo") val cargoHabitacionActivo: Boolean = true,
    @SerialName("tarjeta_al_pedir") val tarjetaAlPedir: Boolean = false,
    @SerialName("codigo_entrega_activo") val codigoEntregaActivo: Boolean = false,
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
            active = activo,
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
