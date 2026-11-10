package com.vaiinilla.app.data.hotel

import com.vaiinilla.app.data.tape.ServerTape
import com.vaiinilla.app.domain.hotel.ChargeCategory
import com.vaiinilla.app.domain.hotel.CheckoutMethod
import com.vaiinilla.app.domain.hotel.HotelErrors
import com.vaiinilla.app.domain.hotel.HotelException
import com.vaiinilla.app.domain.hotel.HotelRoom
import com.vaiinilla.app.domain.hotel.NewStay
import com.vaiinilla.app.domain.hotel.RequestAction
import com.vaiinilla.app.domain.hotel.RequestStatus
import com.vaiinilla.app.domain.hotel.RequestType
import com.vaiinilla.app.domain.hotel.RoomCleaning
import com.vaiinilla.app.domain.hotel.StayStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/** El repositorio real del hotel contra lo que el servidor de pruebas contestó de verdad. */
class RemoteHotelRepositoryTapeTest {
    @get:Rule
    val tape = ServerTape()

    private fun hotel(account: String) = RemoteHotelRepository(tape.client("hotel.$account"))

    private fun key() = UUID.randomUUID().toString()

    private fun day(
        name: String,
        fromToday: Long,
    ) = tape.value(name) { LocalDate.now().plusDays(fromToday).toString() }

    /** Una grabación que se cortó a medias deja estancias abiertas: se cierran antes de empezar. */
    private fun closeLeftovers() {
        val reception = hotel("cajero")
        (reception.stays(StayStatus.IN_HOUSE).getOrThrow() + reception.stays(StayStatus.RESERVED).getOrThrow())
            .filter { it.guestName.endsWith(GUEST_MARK) }
            .forEach { listed ->
                val stay = reception.stay(listed.id).getOrThrow()
                if (stay.status == StayStatus.RESERVED) {
                    reception.cancelStay(stay.id, stay.version, key()).getOrThrow()
                } else {
                    reception
                        .checkout(stay.id, CheckoutMethod.CARD, key(), null, stay.account?.total, stay.version)
                        .getOrThrow()
                }
            }
    }

    /** Cada grabación usa su propia habitación para no chocar con lo que dejó una anterior. */
    private fun newRoom(): HotelRoom {
        closeLeftovers()
        val prefix = tape.value("habitacion") { "Cinta ${UUID.randomUUID().toString().take(6)} " }
        val batch = hotel("admin").createRooms(prefix, 1, 1, floor = 2, category = "doble", key()).getOrThrow()
        assertEquals(1, batch.created)
        val room = hotel("cajero").rooms().getOrThrow().single { it.name.startsWith(prefix.trim()) }
        assertEquals(2, room.floor)
        assertEquals("doble", room.category)
        assertFalse(room.occupied)
        return room
    }

    private fun money(amount: String?) = BigDecimal(amount)

    private companion object {
        const val GUEST_MARK = " de cinta"
    }

    @Test
    fun `una estancia completa suma noches y cargos, cobra la salida y deja el cuarto por limpiar`() {
        val room = newRoom()
        val guestName = "Huésped$GUEST_MARK"
        val stay =
            hotel("cajero")
                .checkIn(
                    NewStay(guestName, contact = null, room.id, checkIn = null, day("salida", 2), "1000.00"),
                    key(),
                ).getOrThrow()
        assertEquals(StayStatus.IN_HOUSE, stay.status)
        assertEquals(guestName, stay.guestName)
        assertEquals(room.id, stay.roomId)
        assertTrue("el código es de 6 dígitos", Regex("\\d{6}").matches(stay.code.orEmpty()))
        val nights = BigDecimal(stay.nights)

        val occupied =
            hotel(
                "cajero",
            ).checkIn(NewStay("Otro$GUEST_MARK", null, room.id, null, day("salida", 2), null), key())
        assertEquals(HotelErrors.ROOM_OCCUPIED, (occupied.exceptionOrNull() as HotelException).code)

        val guest = hotel("cliente")
        assertNull("sin ligar todavía no hay estancia", guest.myStay().getOrThrow())
        val linked = guest.linkStay(stay.code!!, spaceId = room.id).getOrThrow()
        assertEquals(stay.id, linked.id)
        val mine = guest.myStay().getOrThrow()!!
        assertEquals(money("1000.00") * nights, money(mine.account?.total))
        assertTrue(guest.setDoNotDisturb(true).getOrThrow().doNotDisturb)
        assertTrue(
            hotel("cajero")
                .rooms()
                .getOrThrow()
                .single { it.id == room.id }
                .doNotDisturb,
        )
        assertFalse(guest.setDoNotDisturb(false).getOrThrow().doNotDisturb)

        val charge =
            hotel(
                "cajero",
            ).addCharge(stay.id, "Agua del minibar", "85.00", ChargeCategory.MINIBAR, key()).getOrThrow()
        assertEquals("Agua del minibar", charge.concept)
        assertEquals(money("85.00"), money(charge.amount))
        assertEquals(ChargeCategory.MINIBAR, charge.category)

        val open = hotel("cajero").stay(stay.id).getOrThrow()
        assertEquals(listOf("Agua del minibar"), open.charges.map { it.concept })
        val total = money("1000.00") * nights + money("85.00")
        assertEquals(total, money(open.account?.total))
        assertTrue(hotel("cajero").stays(StayStatus.IN_HOUSE).getOrThrow().any { it.id == stay.id })

        val changed =
            hotel("cajero").checkout(stay.id, CheckoutMethod.CASH, key(), "5000.00", "1.00", open.version)
        assertEquals(HotelErrors.ACCOUNT_TOTAL_CHANGED, (changed.exceptionOrNull() as HotelException).code)

        val closed =
            hotel("cajero")
                .checkout(stay.id, CheckoutMethod.CASH, key(), "5000.00", total.toPlainString(), open.version)
                .getOrThrow()
        assertEquals(StayStatus.CHECKED_OUT, closed.status)
        assertEquals(total, money(closed.totalCharged))
        assertEquals("efectivo", closed.paymentMethod)
        assertNull("al salir el huésped ya no tiene estancia", guest.myStay().getOrThrow())

        val afterwards = hotel("mesero").rooms().getOrThrow().single { it.id == room.id }
        assertFalse(afterwards.occupied)
        assertEquals(RoomCleaning.DIRTY, afterwards.cleaning)
        assertEquals(
            RoomCleaning.READY,
            hotel("mesero").setCleaning(room.id, RoomCleaning.READY, key()).getOrThrow().cleaning,
        )
    }

    @Test
    fun `aprobar la salida tardia la cobra y resolver una solicitud normal no cobra nada`() {
        val admin = hotel("admin")
        val config = admin.hotelConfig().getOrThrow()
        val saved =
            admin
                .saveHotelConfig(
                    config.copy(lateCheckoutTime = "15:00", lateCheckoutPrice = "300.00"),
                    key(),
                ).getOrThrow()
        assertEquals("15:00", saved.lateCheckoutTime)
        assertEquals(money("300.00"), money(saved.lateCheckoutPrice))

        val room = newRoom()
        val stay =
            hotel("cajero")
                .checkIn(NewStay("Huésped$GUEST_MARK", null, room.id, null, day("salida", 2), "1000.00"), key())
                .getOrThrow()
        val guest = hotel("cliente")
        guest.linkStay(stay.code!!, spaceId = room.id).getOrThrow()

        val towels = guest.createRequest(RequestType.TOWELS, 2, "Para la alberca", key()).getOrThrow()
        assertEquals(RequestType.TOWELS, towels.type)
        assertEquals(2, towels.quantity)
        assertEquals("Para la alberca", towels.note)
        assertEquals(RequestStatus.PENDING, towels.status)
        val late = guest.createRequest(RequestType.LATE_CHECKOUT, 1, null, key()).getOrThrow()
        val pillows = guest.createRequest(RequestType.PILLOWS, 1, null, key()).getOrThrow()
        // El servidor también devuelve las de estancias anteriores de la misma cuenta.
        val mine = guest.myRequests().getOrThrow().filter { it.stayId == stay.id }
        assertEquals(setOf(towels.id, late.id, pillows.id), mine.map { it.id }.toSet())
        assertEquals(RequestStatus.CANCELED, guest.cancelRequest(pillows.id, key()).getOrThrow().status)

        val staff = hotel("mesero")
        val board = staff.openRequests().getOrThrow().filter { it.stayId == stay.id }
        assertEquals(setOf(towels.id, late.id), board.map { it.id }.toSet())
        assertEquals(room.name, board.first().roomName)

        val taken = staff.transition(towels.id, RequestAction.TAKE, key(), towels.version).getOrThrow()
        assertEquals(RequestStatus.TAKEN, taken.status)
        val resolved = staff.transition(towels.id, RequestAction.RESOLVE, key(), taken.version).getOrThrow()
        assertEquals(RequestStatus.RESOLVED, resolved.status)
        val before = hotel("cajero").stay(stay.id).getOrThrow()
        assertTrue("resolver toallas no carga nada a la cuenta", before.charges.isEmpty())

        val approved = hotel("cajero").transition(late.id, RequestAction.APPROVE, key(), late.version).getOrThrow()
        assertEquals(RequestStatus.RESOLVED, approved.status)
        val after = hotel("cajero").stay(stay.id).getOrThrow()
        val lateCharge = after.charges.single()
        assertEquals(ChargeCategory.LATE_CHECKOUT, lateCharge.category)
        assertEquals(money("300.00"), money(lateCharge.amount))
        assertNotEquals("la salida prevista se recorre", before.expectedDeparture, after.expectedDeparture)
        assertEquals(money(before.account?.total) + money("300.00"), money(after.account?.total))

        hotel("cajero")
            .checkout(stay.id, CheckoutMethod.CARD, key(), null, after.account?.total, after.version)
            .getOrThrow()
        hotel("mesero").setCleaning(room.id, RoomCleaning.READY, key()).getOrThrow()
        admin.saveHotelConfig(config, key()).getOrThrow()
    }

    @Test
    fun `una reserva espera su llegada, se puede cancelar y el dueño ve el resumen y cambia la habitacion`() {
        val room = newRoom()
        val reception = hotel("cajero")
        val draft =
            NewStay("Reserva$GUEST_MARK", "614 000 0000", room.id, day("llegada", 3), day("salida", 5), "800.00")
        val reserved = reception.checkIn(draft, key()).getOrThrow()
        assertEquals(StayStatus.RESERVED, reserved.status)
        assertEquals(2, reserved.nights)
        assertEquals("614 000 0000", reserved.contact)
        assertTrue(reception.stays(StayStatus.RESERVED).getOrThrow().any { it.id == reserved.id })
        assertFalse(
            "una reserva no ocupa la habitación",
            reception
                .rooms()
                .getOrThrow()
                .single { it.id == room.id }
                .occupied,
        )

        val canceled = reception.cancelStay(reserved.id, reserved.version, key()).getOrThrow()
        assertEquals(StayStatus.CANCELED, canceled.status)
        val again = reception.cancelStay(reserved.id, canceled.version, key())
        assertEquals(HotelErrors.STAY_INVALID_STATE, (again.exceptionOrNull() as HotelException).code)

        val today =
            reception
                .checkIn(
                    draft.copy(checkIn = day("hoy", 0), checkOut = day("mañana", 1)),
                    key(),
                ).getOrThrow()
        val arrived =
            if (today.status == StayStatus.RESERVED) {
                reception.confirmArrival(today.id, today.version, key()).getOrThrow()
            } else {
                today
            }
        assertEquals(StayStatus.IN_HOUSE, arrived.status)

        val admin = hotel("admin")
        val summary = admin.summary().getOrThrow()
        assertTrue(summary.totalRooms >= 1)
        assertTrue(summary.occupied >= 1)
        val moved = admin.updateRoom(room.id, floor = 5, category = "suite", key()).getOrThrow()
        assertEquals(5, moved.floor)
        assertEquals("suite", moved.category)

        val open = reception.stay(arrived.id).getOrThrow()
        reception.checkout(arrived.id, CheckoutMethod.CARD, key(), null, open.account?.total, open.version).getOrThrow()
        hotel("mesero").setCleaning(room.id, RoomCleaning.READY, key()).getOrThrow()
    }
}
