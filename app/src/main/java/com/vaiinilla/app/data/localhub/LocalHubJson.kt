package com.vaiinilla.app.data.localhub

import com.vaiinilla.app.domain.localhub.LocalTicket
import com.vaiinilla.app.domain.localhub.LocalTicketBoardSnapshot
import com.vaiinilla.app.domain.localhub.LocalTicketItem
import com.vaiinilla.app.domain.localhub.LocalTicketState
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.Instant

/** Lo que viaja por el Wi‑Fi del local y lo que el concentrador guarda en disco. Una línea JSON por mensaje. */
object LocalHubJson {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        }

    @Serializable
    data class Request(
        val op: String,
        val ts: Long,
        val firma: String,
        /** JSON de la operación como texto: la firma va sobre estos bytes exactos. */
        val payload: String,
    )

    @Serializable
    data class Response(
        val ok: Boolean,
        val error: String? = null,
        val payload: String = "",
    )

    @Serializable
    data class MarkTicket(
        val id: String,
        val estado: String,
    )

    @Serializable
    private data class TicketDto(
        val id: String,
        val folioLocal: String,
        val lugar: String,
        val articulos: List<ItemDto>,
        val capturadoEn: String,
        val tomadoPor: String,
        val estado: String = LocalTicketState.PENDING.wireValue,
    ) {
        fun toDomain(): LocalTicket =
            LocalTicket(
                id = id,
                localFolio = folioLocal,
                placeLabel = lugar,
                items = articulos.map { LocalTicketItem(it.nombre, it.cantidad, it.notas, it.estacion) },
                capturedAt = Instant.parse(capturadoEn),
                takenBy = tomadoPor,
                state = LocalTicketState.fromWire(estado),
            )

        companion object {
            fun of(ticket: LocalTicket): TicketDto =
                TicketDto(
                    id = ticket.id,
                    folioLocal = ticket.localFolio,
                    lugar = ticket.placeLabel,
                    articulos = ticket.items.map { ItemDto(it.name, it.quantity, it.notes, it.station) },
                    capturadoEn = ticket.capturedAt.toString(),
                    tomadoPor = ticket.takenBy,
                    estado = ticket.state.wireValue,
                )
        }
    }

    @Serializable
    private data class ItemDto(
        val nombre: String,
        val cantidad: Int,
        val notas: String = "",
        val estacion: String,
    )

    fun encodeRequest(request: Request): String = json.encodeToString(Request.serializer(), request)

    fun decodeRequest(raw: String): Request = json.decodeFromString(Request.serializer(), raw)

    fun encodeResponse(response: Response): String = json.encodeToString(Response.serializer(), response)

    fun decodeResponse(raw: String): Response = json.decodeFromString(Response.serializer(), raw)

    fun encodeTicket(ticket: LocalTicket): String = json.encodeToString(TicketDto.serializer(), TicketDto.of(ticket))

    fun decodeTicket(raw: String): LocalTicket = json.decodeFromString(TicketDto.serializer(), raw).toDomain()

    fun encodeTickets(tickets: List<LocalTicket>): String =
        json.encodeToString(ListSerializer(TicketDto.serializer()), tickets.map { TicketDto.of(it) })

    fun decodeTickets(raw: String): List<LocalTicket> =
        json.decodeFromString(ListSerializer(TicketDto.serializer()), raw).map { it.toDomain() }

    fun encodeBoard(board: LocalTicketBoardSnapshot): String = encodeTickets(board.tickets)

    fun decodeBoard(raw: String): LocalTicketBoardSnapshot = LocalTicketBoardSnapshot(decodeTickets(raw))

    fun encodeMark(mark: MarkTicket): String = json.encodeToString(MarkTicket.serializer(), mark)

    fun decodeMark(raw: String): MarkTicket = json.decodeFromString(MarkTicket.serializer(), raw)

    /** `GET /llave-local`: `{ data: { llaves: [{ dia, llave }] } }`. */
    fun decodeKeys(raw: String): List<String> =
        json.decodeFromString(KeysEnvelope.serializer(), raw).data.llaves.map { it.llave }

    @Serializable
    private data class KeysEnvelope(
        val data: KeysData,
    )

    @Serializable
    private data class KeysData(
        val llaves: List<KeyDto> = emptyList(),
    )

    @Serializable
    private data class KeyDto(
        val dia: String,
        val llave: String,
    )
}
