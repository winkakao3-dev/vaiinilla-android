package com.vaiinilla.app.ui.hotel.staff

import com.vaiinilla.app.domain.hotel.ChargeCategory
import com.vaiinilla.app.domain.hotel.HotelRequest
import com.vaiinilla.app.domain.hotel.HotelRoom
import com.vaiinilla.app.domain.hotel.RequestStatus
import com.vaiinilla.app.domain.hotel.RequestType
import com.vaiinilla.app.domain.hotel.RoomCleaning
import com.vaiinilla.app.domain.hotel.Stay
import com.vaiinilla.app.domain.hotel.StayCharge
import com.vaiinilla.app.domain.hotel.StayOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class HotelStaffLogicTest {
    private fun room(
        name: String,
        floor: Int? = 3,
        cleaning: RoomCleaning = RoomCleaning.READY,
        occupied: Boolean = false,
        dnd: Boolean = false,
    ) = HotelRoom(id = name.filter { it.isDigit() }.toInt(), name = name, floor = floor, cleaning = cleaning, occupied = occupied, doNotDisturb = dnd)

    @Test
    fun `el tono de una habitacion sale de su estado`() {
        assertEquals(RoomTone.DO_NOT_DISTURB, roomTone(room("Habitación 307", occupied = true, dnd = true)))
        assertEquals(RoomTone.OCCUPIED, roomTone(room("Habitación 301", occupied = true)))
        assertEquals(RoomTone.DIRTY, roomTone(room("Habitación 302", cleaning = RoomCleaning.DIRTY)))
        assertEquals(RoomTone.READY, roomTone(room("Habitación 303")))
    }

    @Test
    fun `los filtros cuentan sucias, limpias y ocupadas`() {
        val state =
            HotelRoomsUiState(
                rooms =
                    listOf(
                        room("Habitación 301", occupied = true),
                        room("Habitación 302", cleaning = RoomCleaning.DIRTY),
                        room("Habitación 303"),
                        room("Habitación 304", occupied = true),
                    ),
            )
        assertEquals(4, state.count(RoomsFilter.ALL))
        assertEquals(1, state.count(RoomsFilter.DIRTY))
        assertEquals(1, state.count(RoomsFilter.CLEAN))
        assertEquals(2, state.count(RoomsFilter.OCCUPIED))
    }

    @Test
    fun `el tablero abre en el primer piso con trabajo pendiente`() {
        val state =
            HotelRoomsUiState(
                rooms =
                    listOf(
                        room("Habitación 101", floor = 1),
                        room("Habitación 302", floor = 3, cleaning = RoomCleaning.DIRTY),
                    ),
            )
        assertEquals(3, state.floorShown)
        assertEquals(listOf("Habitación 302"), state.visibleRooms.map { it.name })
        assertEquals(1, state.copy(floor = 1).floorShown)
    }

    @Test
    fun `el titulo de una solicitud lleva la habitacion y la cantidad`() {
        val towels = HotelRequest(id = "1", roomName = "Habitación 304", type = RequestType.TOWELS, quantity = 2)
        assertEquals("304 · 2 toallas", requestTitle(towels))
        val pillow = HotelRequest(id = "2", roomName = "Habitación 312", type = RequestType.PILLOWS)
        assertEquals("312 · Almohadas", requestTitle(pillow))
    }

    @Test
    fun `el subtitulo dice cuanto hace, la nota o quien la tomo`() {
        val now = Instant.parse("2026-11-06T20:06:00Z")
        val fresh = HotelRequest(id = "1", type = RequestType.TOWELS, createdAt = "2026-11-06T20:00:00Z")
        assertEquals("Pedido hace 6 min", requestSubtitle(fresh, now))
        assertEquals("Pedido hace 6 min · Después de las 11:00", requestSubtitle(fresh.copy(note = "Después de las 11:00"), now))
        assertEquals("Tomada por Rosa", requestSubtitle(fresh.copy(status = RequestStatus.TAKEN, takenBy = "Rosa"), now))
    }

    @Test
    fun `la cuenta suma noches, room service y cargos agrupados`() {
        val stay =
            Stay(
                id = "S1",
                guestName = "Laura Ramos",
                checkIn = "2026-11-06",
                checkOut = "2026-11-09",
                nightlyRate = "1650.00",
                nights = 3,
                orders = listOf(StayOrder("a", "A-001", "236.50", "entregado"), StayOrder("b", "A-002", "548.00", "entregado")),
                charges =
                    listOf(
                        StayCharge("c1", "Minibar", "40.00", ChargeCategory.MINIBAR),
                        StayCharge("c2", "Minibar", "50.00", ChargeCategory.MINIBAR),
                        StayCharge("c3", "Spa", "310.00", ChargeCategory.SPA),
                    ),
            )
        val bill = checkoutBreakdown(stay)
        assertEquals(BigDecimal("4950.00"), bill.stayAmount)
        assertEquals(2, bill.roomServiceCount)
        assertEquals(BigDecimal("784.50"), bill.roomServiceTotal)
        assertEquals(listOf("Minibar" to BigDecimal("90.00"), "Spa" to BigDecimal("310.00")), bill.charges)
        assertEquals(BigDecimal("6134.50"), bill.total)
    }

    @Test
    fun `el total del servidor manda sobre el calculado`() {
        val stay = Stay(id = "S1", guestName = "A", checkIn = "2026-11-06", checkOut = "2026-11-07", nightlyRate = "100.00", total = "999.00")
        assertEquals(BigDecimal("999.00"), checkoutBreakdown(stay).total)
    }

    @Test
    fun `el importe del cargo manual se normaliza o se rechaza`() {
        assertEquals("90.00", parseAmount("90"))
        assertEquals("90.50", parseAmount(" $90.5 "))
        assertEquals("1250.75", parseAmount("1,250.75"))
        assertNull(parseAmount(""))
        assertNull(parseAmount("0"))
        assertNull(parseAmount("-5"))
        assertNull(parseAmount("12.345"))
        assertNull(parseAmount("abc"))
    }

    @Test
    fun `dinero, fechas y numero de habitacion se dicen como en Doop`() {
        assertEquals("$4,950.00", hotelMoney("4950.00"))
        assertEquals("$1,650", hotelMoneyShort("1650.00"))
        assertEquals("6 al 9 de nov", hotelDateRange("2026-11-06", "2026-11-09"))
        assertEquals("30 de oct al 2 de nov", hotelDateRange("2026-10-30", "2026-11-02"))
        assertEquals("304", hotelRoomNumber("Habitación 304"))
        assertEquals("304", hotelRoomNumber("Hab. 304"))
        assertEquals(listOf("1 × Chilaquiles verdes", "2 × Café americano"), deliveryLines("1× Chilaquiles verdes, 2× Café americano"))
    }
}
