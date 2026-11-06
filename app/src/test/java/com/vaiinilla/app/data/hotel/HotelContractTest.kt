package com.vaiinilla.app.data.hotel

import com.vaiinilla.app.data.admin.AdminBusinessJson
import com.vaiinilla.app.domain.admin.BusinessType
import com.vaiinilla.app.domain.admin.SpaceType
import com.vaiinilla.app.domain.admin.StoreTemplate
import com.vaiinilla.app.domain.hotel.HotelConfig
import com.vaiinilla.app.domain.hotel.HotelErrors
import com.vaiinilla.app.domain.hotel.HotelException
import com.vaiinilla.app.domain.hotel.RequestAction
import com.vaiinilla.app.domain.hotel.RequestStatus
import com.vaiinilla.app.domain.hotel.RequestType
import com.vaiinilla.app.domain.hotel.RoomCleaning
import com.vaiinilla.app.domain.hotel.StayStatus
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
              "pedidos":[{"pedido_id":"p1","folio":41,"total":"320.00","estado":"entregado"}],"total":"4200.00",
              "campo_nuevo":true}}
            """.trimIndent()
        val stay = json.parseStay(raw)
        assertEquals("482916", stay.code)
        assertEquals(StayStatus.IN_HOUSE, stay.status)
        assertEquals(304, stay.roomId)
        assertEquals("180", stay.charges.single().amount)
        assertEquals("p1", stay.orders.single().orderId)
        assertEquals("4200.00", stay.total)
    }

    @Test
    fun `a missing stay is null, not an error`() {
        assertNull(json.parseStayOrNull("""{"data":null}"""))
    }

    @Test
    fun `requests and rooms read their wire values`() {
        val requests =
            json.parseRequests(
                """{"data":[{"id":"r1","tipo":"toallas","cantidad":2,"estado":"tomada","espacio_nombre":"Habitación 304"}]}""",
            )
        assertEquals(RequestType.TOWELS, requests.single().type)
        assertEquals(RequestStatus.TAKEN, requests.single().status)
        assertTrue(requests.single().status.isOpen)

        val room =
            json
                .parseRooms(
                    """{"data":[{"id":304,"nombre":"Habitación 304","piso":3,"categoria":"suite","limpieza":"por_limpiar","ocupada":true}]}""",
                ).single()
        assertEquals(RoomCleaning.DIRTY, room.cleaning)
        assertTrue(room.occupied)
    }

    @Test
    fun `requests are sent with the wire names of the contract`() {
        val body = Json.parseToJsonElement(json.encodeRequest(RequestType.LATE_CHECKOUT, 1, "  a las 2  ")).jsonObject
        assertEquals("salida_tardia", body["tipo"]!!.jsonPrimitive.content)
        assertEquals("a las 2", body["nota"]!!.jsonPrimitive.content)
        assertEquals("aprobar", Json.parseToJsonElement(json.encodeTransition(RequestAction.APPROVE)).jsonObject["accion"]!!.jsonPrimitive.content)
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
        val hotel = Json.parseToJsonElement(admin.encodeBatch("mesa", "Habitación", 301, 310, null, 3, "doble")).jsonObject
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
        val config = json.parseConfig("""{"data":{"solicitudes_activas":false,"salida_tardia_hora":"14:00:00","salida_tardia_precio":350}}""")
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
    fun `rooms are created in a batch and edited through habitaciones`() {
        val body = Json.parseToJsonElement(json.encodeRoomBatch(" Habitación ", 301, 310, 3, "doble")).jsonObject
        assertEquals("Habitación", body["prefijo"]!!.jsonPrimitive.content)
        assertEquals("3", body["piso"]!!.jsonPrimitive.content)
        val result = json.parseRoomBatch("""{"data":{"creadas":9,"omitidas":["Habitación 305"]}}""")
        assertEquals(9, result.created)
        assertEquals(listOf("Habitación 305"), result.omitted)
        val room = json.parseRoom("""{"data":{"id":304,"nombre":"Habitación 304","qr_token":"abc","qr_url":"https://x.mx/q/abc"}}""")
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
        val saved = hotel.saveHotelConfig(HotelConfig(cardAtOrderEnabled = true, kitchenFrom = "07:00", kitchenTo = "23:00"), "k").getOrThrow()
        assertTrue(hotel.config().getOrThrow().cardAtOrderEnabled)
        assertEquals("23:00", saved.kitchenTo)
    }

    @Test
    fun `the fake hotel walks the guest from code to checkout`() {
        val hotel = FakeHotelRepository()
        hotel.reset(linkedGuest = false)
        assertNull(hotel.myStay().getOrThrow())
        assertEquals("CODIGO_INVALIDO", (hotel.linkStay("000000").exceptionOrNull() as HotelException).code)
        val stay = hotel.linkStay("482916", spaceId = 304).getOrThrow()
        assertEquals("Habitación 304", stay.roomName)
        assertNotNull(hotel.myStay().getOrThrow())

        val request = hotel.createRequest(RequestType.TOWELS, 2, null, "k1").getOrThrow()
        assertEquals(RequestStatus.TAKEN, hotel.transition(request.id, RequestAction.TAKE, "k2").getOrThrow().status)
        assertTrue(hotel.openRequests().getOrThrow().any { it.id == request.id })

        val closed = hotel.checkout(stay.id, com.vaiinilla.app.domain.hotel.CheckoutMethod.CARD, "k3").getOrThrow()
        assertEquals(StayStatus.CHECKED_OUT, closed.status)
        assertEquals(RoomCleaning.DIRTY, hotel.rooms().getOrThrow().first { it.id == 304 }.cleaning)
        val error = hotel.createRequest(RequestType.CLEANING, 1, null, "k4").exceptionOrNull() as HotelException
        assertEquals(HotelErrors.ROOM_REQUIRES_STAY, error.code)
    }
}