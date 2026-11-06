package com.vaiinilla.app.data.hotel

import com.vaiinilla.app.data.admin.AdminBusinessJson
import com.vaiinilla.app.data.discovery.SpaceResolveEnvelope
import com.vaiinilla.app.data.discovery.toDomain
import com.vaiinilla.app.domain.admin.BusinessType
import com.vaiinilla.app.domain.admin.SpaceType
import com.vaiinilla.app.domain.admin.StoreTemplate
import com.vaiinilla.app.domain.hotel.CheckoutMethod
import com.vaiinilla.app.domain.hotel.HotelConfig
import com.vaiinilla.app.domain.hotel.HotelErrors
import com.vaiinilla.app.domain.hotel.HotelException
import com.vaiinilla.app.domain.hotel.NewStay
import com.vaiinilla.app.domain.hotel.RequestAction
import com.vaiinilla.app.domain.hotel.RequestStatus
import com.vaiinilla.app.domain.hotel.RequestType
import com.vaiinilla.app.domain.hotel.RoomCleaning
import com.vaiinilla.app.domain.hotel.StayStatus
import com.vaiinilla.app.domain.model.PublicSpace
import com.vaiinilla.app.domain.model.SpaceCopy
import com.vaiinilla.app.domain.model.Venue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HotelContractTest {
    private val json = HotelJson()

    @After
    fun resetVenue() = Venue.update(null)

    @Test
    fun `stay keeps the code, the nights and what is charged to the room`() {
        val raw =
            """
            {"data":{"id":"e1","huesped":"Laura Méndez","codigo":482916,"espacio_id":304,"espacio_nombre":"Habitación 304",
              "piso":3,"entrada":"2026-11-06","salida":"2026-11-08","tarifa_noche":"1850.00","estado":"en_casa","noches":2,
              "cargos":[{"id":7,"concepto":"Minibar","monto":180,"categoria":"minibar"}],
              "pedidos":[{"id":"p1","folio":"A-014","total":"320.00","estado":"pendiente","por_cobrar":true,"items_resumen":"2x Agua"}],
              "total":"4200.00","version":3,"categoria":"Doble","salida_prevista":"2026-11-08T18:00:00.000Z",
              "cuenta":{"noches_total":"3700.00","cargos_total":"180.00","pedidos_total":"320.00","total":"4200.00","pedidos_pendientes":1},
              "ajustes":{"activo":true,"tarjeta_al_pedir":true,"hora_checkout":"12:00"},
              "campo_nuevo":true}}
            """.trimIndent()
        val stay = json.parseStay(raw)
        assertEquals("482916", stay.code)
        assertEquals(StayStatus.IN_HOUSE, stay.status)
        assertEquals(304, stay.roomId)
        assertEquals("180", stay.charges.single().amount)
        assertEquals("p1", stay.orders.single().orderId)
        assertEquals("A-014", stay.orders.single().folio)
        assertTrue(stay.orders.single().pendingPayment)
        assertEquals("4200.00", stay.total)
        assertEquals(3, stay.version)
        assertEquals(1, stay.account!!.pendingOrders)
        assertTrue(stay.settings!!.cardAtOrderEnabled)
        assertEquals("12:00", stay.settings!!.checkoutTime)
    }

    @Test
    fun `a missing stay is null, not an error`() {
        assertNull(json.parseStayOrNull("""{"data":null}"""))
    }

    @Test
    fun `requests and rooms read their wire values`() {
        val requests =
            json.parseRequests(
                """{"data":[{"id":"r1","tipo":"toallas","cantidad":2,"estado":"tomada","version":2,
                    "habitacion":{"id":12,"nombre":"Habitación 101","piso":1},"huesped":{"nombre":"Ana"},
                    "tomada_por":{"nombre":"Personal del hotel"}}]}""",
            )
        assertEquals("Habitación 101", requests.single().roomName)
        assertEquals(2, requests.single().version)
        assertEquals("Personal del hotel", requests.single().takenBy)
        assertEquals(RequestType.TOWELS, requests.single().type)
        assertEquals(RequestStatus.TAKEN, requests.single().status)
        assertTrue(requests.single().status.isOpen)

        val room =
            json
                .parseRooms(
                    """{"data":[{"id":304,"nombre":"Habitación 304","piso":3,"categoria":"suite","limpieza":"por_limpiar","ocupada":true,
                    "estancia":{"id":"e1","huesped_nombre":"Laura","salida_prevista":"2026-11-08T18:00:00.000Z"},"pedidos_en_curso":2}]}""",
                ).single()
        assertEquals(RoomCleaning.DIRTY, room.cleaning)
        assertTrue(room.occupied)
        assertEquals("Laura", room.guestName)
        assertEquals(2, room.ordersInProgress)
    }

    @Test
    fun `requests are sent with the wire names of the contract`() {
        val body = Json.parseToJsonElement(json.encodeRequest(RequestType.LATE_CHECKOUT, 1, "  a las 2  ")).jsonObject
        assertEquals("salida_tardia", body["tipo"]!!.jsonPrimitive.content)
        assertEquals("a las 2", body["nota"]!!.jsonPrimitive.content)
        assertEquals(
            "aprobar",
            Json
                .parseToJsonElement(
                    json.encodeTransition(RequestAction.APPROVE, 4),
                ).jsonObject["accion"]!!
                .jsonPrimitive.content,
        )
    }

    @Test
    fun `hotel is a business type but is never offered as a template`() {
        assertEquals(BusinessType.HOTEL, BusinessType.fromWire("hotel"))
        assertFalse(BusinessType.offered(null).contains(BusinessType.HOTEL))
        assertTrue(StoreTemplate.entries.none { it.type == BusinessType.HOTEL })
    }

    @Test
    fun `a table is a room only in a hotel`() {
        assertEquals("Mesa", SpaceType.TABLE.label)
        assertFalse(SpaceCopy.isRoom("mesa"))
        Venue.update(Venue.HOTEL)
        assertEquals("Habitación", SpaceType.TABLE.label)
        assertTrue(SpaceCopy.isRoom("mesa"))
        assertFalse(SpaceCopy.isRoom("barra"))
    }

    @Test
    fun `room batch sends floor and category and a plain batch does not`() {
        val admin = AdminBusinessJson()
        val hotel =
            Json
                .parseToJsonElement(
                    admin.encodeBatch("mesa", "Habitación", 301, 310, null, 3, "doble"),
                ).jsonObject
        assertEquals("3", hotel["piso"]!!.jsonPrimitive.content)
        assertEquals("doble", hotel["categoria"]!!.jsonPrimitive.content)
        val plain = Json.parseToJsonElement(admin.encodeBatch("mesa", "Mesa", 1, 5)).jsonObject
        assertFalse(plain.containsKey("piso"))
        assertFalse(plain.containsKey("categoria"))
    }

    @Test
    fun `a managed table counts as a room only in a hotel`() {
        val space = AdminBusinessJson().parseSpace("""{"data":{"id":9,"nombre":"Habitación 304","tipo":"mesa"}}""")
        assertFalse(space.isRoom)
        Venue.update(Venue.HOTEL)
        assertTrue(space.isRoom)
    }

    @Test
    fun `hotel config is read and saved only through hotel configuracion`() {
        val config =
            json.parseConfig(
                """{"data":{"activo":true,"solicitudes_activas":false,"salida_tardia_hora":"14:00:00","salida_tardia_precio":350}}""",
            )
        assertFalse(config.requestsEnabled)
        assertTrue(config.doNotDisturbEnabled)
        assertEquals("14:00", config.lateCheckoutTime)
        assertEquals("350", config.lateCheckoutPrice)
        val sent = Json.parseToJsonElement(json.encodeHotelConfig(HotelConfig(lateCheckoutTime = "14:00"))).jsonObject
        assertEquals("14:00", sent["salida_tardia_hora"]!!.jsonPrimitive.content)
        assertFalse(sent.containsKey("hotel_config"))
    }

    @Test
    fun `a table speaks as a room only in a hotel`() {
        assertEquals("tu mesa", SpaceCopy.yourPlace("mesa"))
        assertEquals("Comer aquí", SpaceCopy.destinationTitle("mesa"))
        assertEquals("Mesa 4", SpaceCopy.short("mesa", "Mesa 4"))

        Venue.update(Venue.HOTEL)
        assertEquals("tu habitación", SpaceCopy.yourPlace("mesa"))
        assertEquals("A tu habitación", SpaceCopy.destinationTitle("mesa"))
        assertEquals("Selecciona tu habitación", SpaceCopy.pickPrompt("mesa"))
        assertEquals("Tu pedido está listo. Ya va a tu habitación.", SpaceCopy.readyInSpace("mesa"))
        assertEquals("Hab. 304", SpaceCopy.short("mesa", "Habitación 304"))
        assertEquals("tu cancha", SpaceCopy.yourPlace("cancha"))
    }

    @Test
    fun `checkin, checkout and the summary use the names of the backend contract`() {
        val checkIn =
            Json
                .parseToJsonElement(
                    json.encodeCheckIn(NewStay("Ana", " a@x.mx ", 12, "2026-11-09", "2026-11-11", "1200.00")),
                ).jsonObject
        assertEquals("Ana", checkIn["huesped_nombre"]!!.jsonPrimitive.content)
        assertEquals("2026-11-11", checkIn["salida"]!!.jsonPrimitive.content)
        assertFalse(checkIn.containsKey("huesped"))
        val checkout =
            Json
                .parseToJsonElement(
                    json.encodeCheckout(CheckoutMethod.CASH, "3000.00", "2650.00", 3),
                ).jsonObject
        assertEquals("efectivo", checkout["metodo_pago"]!!.jsonPrimitive.content)
        assertEquals("2650.00", checkout["total_esperado"]!!.jsonPrimitive.content)
        assertEquals("3", checkout["version_esperada"]!!.jsonPrimitive.content)

        val summary =
            json.parseSummary(
                """{"data":{"habitaciones":{"total":20,"ocupadas":12,"por_limpiar":3,"sin_molestar":1},
                    "room_service":{"pedidos_hoy":9,"ventas_hoy":"1840.00","pedidos_a_cuartos":2,"en_cocina_ahora":1},
                    "solicitudes":{"abiertas":4,"mas_vieja_desde":"2026-11-09T14:10:00.000Z"},
                    "lo_mas_pedido":[{"producto":"Hamburguesa","cantidad":7}],"llegadas_hoy":3,
                    "salidas_hoy":[{"estancia_id":"e1","huesped_nombre":"Ana","habitacion":{"id":12,"nombre":"Habitación 101"},"salida_prevista":"x"}],
                    "consultado_en":"2026-11-09T14:40:00.000Z"}}""",
            )
        assertEquals(12, summary.occupied)
        assertEquals("1840.00", summary.roomServiceToday)
        assertEquals(30, summary.oldestRequestMinutes)
        assertEquals("Hamburguesa", summary.topItems.single().name)
        assertEquals("Habitación 101", summary.departures.single().roomName)
        assertEquals(3, summary.arrivalsToday)
    }

    @Test
    fun `the stay list comes inside hoy and estancias and a charge comes back inside the stay`() {
        val stays =
            json.parseStays(
                """{"data":{"hoy":"2026-11-09","estancias":[{"id":"e1","huesped":"Ana","estado":"reservada"}]}}""",
            )
        assertEquals(StayStatus.RESERVED, stays.single().status)
        val charge =
            json.parseAddedCharge(
                """{"data":{"id":"e1","huesped":"Ana","cargos":[{"id":"c1","concepto":"Minibar","monto":"150.00","categoria":"minibar"}]}}""",
            )
        assertEquals("150.00", charge.amount)
    }

    @Test
    fun `rooms are created in a batch and edited through habitaciones`() {
        val body = Json.parseToJsonElement(json.encodeRoomBatch(" Habitación ", 301, 310, 3, "doble")).jsonObject
        assertEquals("Habitación", body["prefijo"]!!.jsonPrimitive.content)
        assertEquals("3", body["piso"]!!.jsonPrimitive.content)
        val result = json.parseRoomBatch("""{"data":{"creadas":9,"omitidas":["Habitación 305"]}}""")
        assertEquals(9, result.created)
        assertEquals(listOf("Habitación 305"), result.omitted)
        val room =
            json.parseRoom(
                """{"data":{"id":304,"nombre":"Habitación 304","qr_token":"abc","qr_url":"https://x.mx/q/abc"}}""",
            )
        assertEquals("abc", room.qrToken)
        assertEquals("https://x.mx/q/abc", room.qrUrl)
    }

    @Test
    fun `the fake creates rooms, skips repeated ones and keeps the saved config`() {
        val hotel = FakeHotelRepository()
        val batch = hotel.createRooms("Habitación", 304, 306, 3, "doble", "k").getOrThrow()
        assertEquals(1, batch.created)
        assertEquals(listOf("Habitación 304", "Habitación 305"), batch.omitted)
        assertEquals("suite", hotel.updateRoom(304, 4, " suite ", "k").getOrThrow().category)
        val saved =
            hotel
                .saveHotelConfig(
                    HotelConfig(cardAtOrderEnabled = true, kitchenFrom = "07:00", kitchenTo = "23:00"),
                    "k",
                ).getOrThrow()
        assertTrue(hotel.config().getOrThrow().cardAtOrderEnabled)
        assertEquals("23:00", saved.kitchenTo)
    }

    @Test
    fun `the fake hotel walks the guest from code to checkout`() {
        val hotel = FakeHotelRepository()
        hotel.reset(linkedGuest = false)
        assertNull(hotel.myStay().getOrThrow())
        assertEquals(HotelErrors.STAY_NOT_FOUND, (hotel.linkStay("000000").exceptionOrNull() as HotelException).code)
        val stay = hotel.linkStay("482916", spaceId = 304).getOrThrow()
        assertEquals("Habitación 304", stay.roomName)
        assertNotNull(hotel.myStay().getOrThrow())

        val request = hotel.createRequest(RequestType.TOWELS, 2, null, "k1").getOrThrow()
        assertEquals(RequestStatus.TAKEN, hotel.transition(request.id, RequestAction.TAKE, "k2").getOrThrow().status)
        assertTrue(hotel.openRequests().getOrThrow().any { it.id == request.id })

        val closed = hotel.checkout(stay.id, CheckoutMethod.CARD, "k3").getOrThrow()
        assertEquals(StayStatus.CHECKED_OUT, closed.status)
        assertEquals(
            RoomCleaning.DIRTY,
            hotel
                .rooms()
                .getOrThrow()
                .first { it.id == 304 }
                .cleaning,
        )
        val error = hotel.createRequest(RequestType.CLEANING, 1, null, "k4").exceptionOrNull() as HotelException
        assertEquals(HotelErrors.ROOM_REQUIRES_STAY, error.code)
    }

    @Test
    fun `a table of the hotel restaurant is not a room when the server says so`() {
        val room =
            Json { ignoreUnknownKeys = true }
                .decodeFromString<SpaceResolveEnvelope>(
                    """{"data":{"establecimiento":{"id":"e1","nombre":"Hotel","slug":"hotel","identificador_cliente_etiqueta":"Id","identificador_cliente_obligatorio":false},"espacio":{"id":7,"nombre":"Habitación 304","tipo":"mesa","es_habitacion":true}}}""",
                ).data.espacio!!
                .toDomain()
        val restaurant = room.copy(id = 9, name = "Mesa 2", isRoom = false)
        val other =
            Json { ignoreUnknownKeys = true }
                .decodeFromString<SpaceResolveEnvelope>(
                    """{"data":{"establecimiento":{"id":"e2","nombre":"Cafe","slug":"cafe","identificador_cliente_etiqueta":"Id","identificador_cliente_obligatorio":false},"espacio":{"id":3,"nombre":"Mesa 1","tipo":"mesa"}}}""",
                ).data.espacio!!
                .toDomain()
        assertEquals(true, room.isRoom)
        assertNull(other.isRoom)

        Venue.update(Venue.HOTEL)
        assertTrue(SpaceCopy.isRoom(room))
        assertFalse(SpaceCopy.isRoom(restaurant))
        // Sin el dato, en un hotel toda mesa se trata como habitación, como antes; sin espacio no hay habitación.
        assertTrue(SpaceCopy.isRoom(other))
        assertFalse(SpaceCopy.isRoom(null as PublicSpace?))
        Venue.update("cafeteria")
        assertFalse(SpaceCopy.isRoom(room))

        Venue.markSpace(restaurant.id, restaurant.isRoom)
        Venue.markSpace(room.id, room.isRoom)
        assertTrue(Venue.isHotelTable(restaurant.id))
        assertFalse(Venue.isHotelTable(room.id))
        Venue.markSpace(restaurant.id, null)
        assertFalse(Venue.isHotelTable(restaurant.id))
    }

    @Test
    fun `a check-in for today leaves the date out and a config without checkout time does not send null`() {
        val today =
            Json
                .parseToJsonElement(
                    json.encodeCheckIn(NewStay("Ana", null, 12, null, "2026-11-11", "1200.00")),
                ).jsonObject
        assertFalse(today.containsKey("entrada"))
        assertEquals("2026-11-11", today["salida"]!!.jsonPrimitive.content)

        val withoutTime = Json.parseToJsonElement(json.encodeHotelConfig(HotelConfig(checkoutTime = null))).jsonObject
        assertFalse(withoutTime.containsKey("hora_checkout"))
        val withTime = Json.parseToJsonElement(json.encodeHotelConfig(HotelConfig(checkoutTime = "12:00"))).jsonObject
        assertEquals("12:00", withTime["hora_checkout"]!!.jsonPrimitive.content)
    }
}
