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
    fun `hotel space and business type are offered only to a server that knows them`() {
        assertEquals("habitacion", SpaceType.ROOM.wire)
        assertFalse(BusinessType.offered(null).contains(BusinessType.HOTEL))
        assertTrue(BusinessType.offered(listOf("restaurante", "hotel")).contains(BusinessType.HOTEL))
        assertTrue(StoreTemplate.HOTEL.allowsPayAtEnd)
        assertFalse(StoreTemplate.HOTEL.deliveryRequiresQr)
    }

    @Test
    fun `rooms are creatable only in a hotel and come first there`() {
        assertFalse(SpaceType.creatable().contains(SpaceType.ROOM))
        assertFalse(SpaceType.creatable(batch = true).contains(SpaceType.ROOM))
        Venue.update(Venue.HOTEL)
        assertEquals(SpaceType.ROOM, SpaceType.creatable(batch = true).first())
        assertTrue(Venue.isHotel)
    }

    @Test
    fun `room batch sends floor and category and a plain batch does not`() {
        val admin = AdminBusinessJson()
        val hotel = Json.parseToJsonElement(admin.encodeBatch("habitacion", "Habitación", 301, 310, null, 3, "doble")).jsonObject
        assertEquals("3", hotel["piso"]!!.jsonPrimitive.content)
        assertEquals("doble", hotel["categoria"]!!.jsonPrimitive.content)
        val plain = Json.parseToJsonElement(admin.encodeBatch("mesa", "Mesa", 1, 5)).jsonObject
        assertFalse(plain.containsKey("piso"))
        assertFalse(plain.containsKey("categoria"))
    }

    @Test
    fun `managed rooms keep floor, category and cleaning`() {
        val space =
            AdminBusinessJson()
                .parseSpace(
                    """{"data":{"id":9,"nombre":"Habitación 304","tipo":"habitacion","piso":3,"categoria":"suite","limpieza":"lista"}}""",
                )
        assertTrue(space.isRoom)
        assertEquals(3, space.floor)
        assertEquals("suite", space.category)
    }

    @Test
    fun `hotel config round trips through the store config contract`() {
        val admin = AdminBusinessJson()
        val config = admin.parseStoreConfig("""{"data":{"tipo":"hotel","hotel_config":{"solicitudes_activas":false,"salida_tardia_hora":"14:00:00","salida_tardia_precio":350}}}""").hotelConfig
        assertNotNull(config)
        assertFalse(config!!.requestsEnabled)
        assertTrue(config.doNotDisturbEnabled)
        assertEquals("14:00", config.lateCheckoutTime)
        assertEquals("350", config.lateCheckoutPrice)
        val sent = Json.parseToJsonElement(admin.encodeHotelConfig(HotelConfig(lateCheckoutTime = "14:00"))).jsonObject["hotel_config"]!!.jsonObject
        assertEquals("14:00", sent["salida_tardia_hora"]!!.jsonPrimitive.content)
    }

    @Test
    fun `a room speaks as a room to guests and as Hab to staff`() {
        assertEquals("tu habitación", SpaceCopy.yourPlace("habitacion"))
        assertEquals("A tu habitación", SpaceCopy.destinationTitle("habitacion"))
        assertEquals("Selecciona tu habitación", SpaceCopy.pickPrompt("habitacion"))
        assertEquals("Tu pedido está listo. Ya va a tu habitación.", SpaceCopy.readyInSpace("habitacion"))
        assertEquals("Hab. 304", SpaceCopy.short("habitacion", "Habitación 304"))
        assertEquals("Mesa 4", SpaceCopy.short("mesa", "Mesa 4"))
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