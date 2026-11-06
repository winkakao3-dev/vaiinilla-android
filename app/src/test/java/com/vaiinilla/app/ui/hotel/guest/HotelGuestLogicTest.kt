package com.vaiinilla.app.ui.hotel.guest

import com.vaiinilla.app.data.hotel.FakeHotelRepository
import com.vaiinilla.app.domain.hotel.ChargeCategory
import com.vaiinilla.app.domain.hotel.HotelErrors
import com.vaiinilla.app.domain.hotel.HotelException
import com.vaiinilla.app.domain.hotel.RequestType
import com.vaiinilla.app.domain.hotel.Stay
import com.vaiinilla.app.domain.hotel.StayAccount
import com.vaiinilla.app.domain.hotel.StayCharge
import com.vaiinilla.app.domain.hotel.StayOrder
import com.vaiinilla.app.domain.hotel.StayStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class HotelGuestLogicTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun waitUntil(
        timeoutMs: Long = 3_000,
        condition: () -> Boolean,
    ) {
        val end = System.currentTimeMillis() + timeoutMs
        while (!condition() && System.currentTimeMillis() < end) Thread.sleep(10)
        assertTrue("la condición no se cumplió a tiempo", condition())
    }

    @Test
    fun `una estancia cerrada muestra lo cobrado en el check-out y todos sus pedidos`() {
        val closed =
            Stay(
                id = "S9",
                guestName = "Elena",
                checkIn = "2026-11-03",
                checkOut = "2026-11-06",
                nightlyRate = "300.00",
                nights = 3,
                status = StayStatus.CHECKED_OUT,
                totalCharged = "3439.00",
                account =
                    StayAccount(
                        nightsTotal = "900.00",
                        chargesTotal = "0.00",
                        ordersTotal = "0.00",
                        total = "2805.00",
                    ),
                orders = listOf(StayOrder("a", "A-001", "55.00", "entregado", pendingPayment = false)),
            )
        assertEquals("TOTAL COBRADO" to BigDecimal("3439.00"), accountHeadline(closed))
        assertEquals(1, accountLines(closed).size)

        val open = closed.copy(status = StayStatus.IN_HOUSE, totalCharged = null)
        assertEquals("CARGOS A LA HABITACIÓN", accountHeadline(open).first)
        assertEquals(0, accountLines(open).size)
    }

    @Test
    fun `el rango de la estancia se dice con el mes una sola vez`() {
        assertEquals("del 6 al 9 de nov", stayRange("2026-11-06", "2026-11-09"))
        assertEquals("del 30 oct al 2 de nov", stayRange("2026-10-30", "2026-11-02"))
        assertEquals("del 6 al 9 de nov", stayRange("2026-11-06T15:00:00Z", "2026-11-09T12:00:00Z"))
        assertNull(stayRange(null, "2026-11-09"))
        assertEquals("9 de nov", shortDate("2026-11-09"))
    }

    @Test
    fun `la cuenta suma room service y cargos y deja la estancia aparte`() {
        val stay =
            Stay(
                id = "S1",
                guestName = "Laura Méndez",
                checkIn = "2026-11-06",
                checkOut = "2026-11-09",
                nightlyRate = "1650.00",
                nights = 3,
                orders =
                    listOf(
                        StayOrder("P1", "A-412", "236.50", "entregado", null),
                        StayOrder("P0", "A-398", "548.00", "entregado", null),
                    ),
                charges =
                    listOf(
                        StayCharge("C1", "Minibar", "90.00", ChargeCategory.MINIBAR),
                        StayCharge("C2", "Spa", "310.00", ChargeCategory.SPA),
                    ),
            )
        assertEquals(BigDecimal("1184.50"), roomCharges(stay))
        assertEquals(BigDecimal("4950.00"), stayCost(stay))
        assertEquals("Room service · pedido A-412", accountLines(stay).first().title)
    }

    @Test
    fun `la cuenta del servidor solo lista los pedidos que suma`() {
        val stay =
            Stay(
                id = "S1",
                guestName = "Elena Marín",
                checkIn = "2026-11-06",
                checkOut = "2026-11-07",
                nightlyRate = "900.00",
                orders =
                    listOf(
                        StayOrder("P4", "4", "55.00", "listo", null, "1× Jugo de naranja", pendingPayment = true),
                        StayOrder("P5", "5", "55.00", "por_cobrar", null, "1× Jugo de naranja", pendingPayment = false),
                    ),
                account =
                    StayAccount(
                        nightsTotal = "900.00",
                        chargesTotal = "0.00",
                        ordersTotal = "55.00",
                        total = "955.00",
                        pendingOrders = 1,
                    ),
            )
        assertEquals(BigDecimal("55.00"), roomCharges(stay))
        assertEquals(listOf(BigDecimal("55.00")), accountLines(stay).map { it.amount })
    }

    @Test
    fun `una estancia sin tarifa no inventa el costo de las noches`() {
        val stay = Stay(id = "S1", guestName = "Ana", checkIn = "2026-11-06", checkOut = "2026-11-07")
        assertNull(stayCost(stay))
        assertEquals(BigDecimal.ZERO, roomCharges(stay))
    }

    @Test
    fun `lo que se pide por cantidad da la vuelta y lo demas se prende y se apaga`() {
        val vm = HotelRequestsViewModel(FakeHotelRepository())
        repeat(3) { vm.tap(RequestType.TOWELS) }
        assertEquals(3, vm.state.value.picked[RequestType.TOWELS])
        repeat(2) { vm.tap(RequestType.TOWELS) }
        assertNull(vm.state.value.picked[RequestType.TOWELS])

        vm.tap(RequestType.CLEANING)
        assertEquals(1, vm.state.value.picked[RequestType.CLEANING])
        vm.tap(RequestType.CLEANING)
        assertTrue(
            vm.state.value.picked
                .isEmpty(),
        )
    }

    @Test
    fun `solo cuenta Sin molestar si cambio respecto a como estaba`() {
        val vm = HotelRequestsViewModel(FakeHotelRepository())
        assertEquals(0, vm.state.value.pendingCount)
        vm.toggleDoNotDisturb()
        assertEquals(1, vm.state.value.pendingCount)
        vm.toggleDoNotDisturb()
        assertEquals(0, vm.state.value.pendingCount)
    }

    @Test
    fun `enviar crea una solicitud por cosa y deja Sin molestar prendido`() {
        val repository = FakeHotelRepository()
        val vm = HotelRequestsViewModel(repository)
        repeat(2) { vm.tap(RequestType.TOWELS) }
        vm.tap(RequestType.CLEANING)
        vm.toggleDoNotDisturb()
        assertEquals(3, vm.state.value.pendingCount)

        vm.submit()
        waitUntil { !vm.state.value.submitting && vm.state.value.sentCount > 0 }

        assertEquals(3, vm.state.value.sentCount)
        assertNull(vm.state.value.error)
        val mine = repository.myRequests().getOrThrow()
        assertTrue(mine.any { it.type == RequestType.TOWELS && it.quantity == 2 })
        assertTrue(mine.any { it.type == RequestType.CLEANING })
        assertTrue(repository.myStay().getOrThrow()!!.doNotDisturb)
    }

    @Test
    fun `sin estancia ligada el huesped ve que falta el codigo`() {
        val repository = FakeHotelRepository().apply { reset(linkedGuest = false) }
        val vm = HotelRequestsViewModel(repository)
        vm.tap(RequestType.TOWELS)
        vm.submit()
        waitUntil { !vm.state.value.submitting && vm.state.value.error != null }
        assertTrue(
            vm.state.value.error!!
                .contains("código de estancia"),
        )
        assertEquals(1, vm.state.value.picked.size)
    }

    @Test
    fun `un codigo equivocado no liga y uno bueno si`() {
        val repository = FakeHotelRepository().apply { reset(linkedGuest = false) }
        val vm = HotelStayViewModel(repository)
        var linked = false
        vm.link("000000", 304, null) { linked = true }
        waitUntil { !vm.state.value.linking && vm.state.value.linkError != null }
        assertEquals(false, linked)

        vm.link("482916", 304, null) { linked = true }
        waitUntil { linked }
        assertNotNull(vm.state.value.stay)
        assertNull(vm.state.value.linkError)
    }

    @Test
    fun `ROOM_REQUIRES_STAY se dice en palabras del huesped`() {
        val message = hotelErrorMessage(HotelException(HotelErrors.ROOM_REQUIRES_STAY, "403"))
        assertTrue(message.contains("código de estancia"))
    }
}
