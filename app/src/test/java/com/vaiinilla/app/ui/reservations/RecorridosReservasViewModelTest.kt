package com.vaiinilla.app.ui.reservations

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.venue.VenueContextStore
import com.vaiinilla.app.data.guest.GuestReservationSession
import com.vaiinilla.app.domain.model.CourtDay
import com.vaiinilla.app.domain.model.CourtSchedule
import com.vaiinilla.app.domain.model.Reservation
import com.vaiinilla.app.domain.model.ReservationAccess
import com.vaiinilla.app.domain.model.ReservationPayment
import com.vaiinilla.app.domain.model.ReservationPaymentMethod
import com.vaiinilla.app.domain.model.ReservationRefund
import com.vaiinilla.app.domain.model.ReservationState
import com.vaiinilla.app.domain.repository.ReservationRepository
import com.vaiinilla.app.ui.operational.StaffHarness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

private class FakeCourts : ReservationRepository {
    // Horario de 08:00 a 22:00 local (UTC-6) el 1 de octubre, a las 14:10 locales.
    private val opens = Instant.parse("2026-10-01T14:00:00Z")
    private val closes = Instant.parse("2026-10-02T04:00:00Z")
    val day =
        CourtDay(
            date = "2026-10-01",
            today = "2026-10-01",
            timeZone = "America/Chihuahua",
            opensAt = opens,
            closesAt = closes,
            blockMinutes = 30,
            durations = listOf(60, 90, 120),
            daysAhead = 14,
            holdMinutes = 10,
            now = Instant.parse("2026-10-01T20:10:00Z"),
            courts =
                listOf(
                    CourtSchedule(
                        id = 7,
                        name = "Cancha 1",
                        pricePerHour = "300.00",
                        customerPricePerHour = null,
                        rentable = true,
                        busy = emptyList(),
                    ),
                ),
        )

    @Volatile var mine: List<Reservation> = emptyList()

    @Volatile var payFails: Throwable? = null
    val created = CopyOnWriteArrayList<Triple<Int, Instant?, Int>>()
    val paid = CopyOnWriteArrayList<Pair<String, ReservationPaymentMethod>>()
    val cancelled = CopyOnWriteArrayList<String>()

    fun reservation(
        id: String = "r1",
        start: Instant = Instant.parse("2026-10-01T20:10:00Z"),
        minutes: Int = 60,
        state: ReservationState = ReservationState.PENDING_PAYMENT,
    ) = Reservation(
        id = id,
        courtId = 7,
        courtName = "Cancha 1",
        start = start,
        end = start.plusSeconds(minutes * 60L),
        durationMinutes = minutes,
        amount = "300.00",
        customerPrice = null,
        state = state,
        holdExpiresAt = start.plusSeconds(600),
        orderId = null,
        channel = "app",
        customerName = null,
        version = 1,
    )

    override fun day(
        date: String?,
        access: ReservationAccess,
    ) = Result.success(day)

    override fun list(
        access: ReservationAccess,
        date: String?,
    ) = Result.success(mine)

    override fun create(
        access: ReservationAccess,
        courtId: Int,
        start: Instant?,
        durationMinutes: Int,
        customerName: String?,
        idempotencyKey: String,
        players: Int?,
    ): Result<Reservation> {
        created += Triple(courtId, start, durationMinutes)
        val r = reservation(start = start ?: day.now, minutes = durationMinutes)
        mine = mine + r
        return Result.success(r)
    }

    override fun pay(
        access: ReservationAccess,
        reservationId: String,
        method: ReservationPaymentMethod,
        cashReceived: String?,
        idempotencyKey: String,
    ): Result<ReservationPayment> {
        paid += reservationId to method
        payFails?.let { return Result.failure(it) }
        val r = mine.first { it.id == reservationId }.copy(state = ReservationState.CONFIRMED)
        mine = mine.map { if (it.id == reservationId) r else it }
        return Result.success(ReservationPayment(r, null, null, null))
    }

    override fun cancel(
        access: ReservationAccess,
        reservationId: String,
        idempotencyKey: String,
        refund: ReservationRefund?,
    ): Result<Reservation> {
        cancelled += reservationId
        val r = mine.first { it.id == reservationId }.copy(state = ReservationState.CANCELLED)
        mine = mine.filterNot { it.id == reservationId }
        return Result.success(r)
    }
}

/**
 * Rentar una cancha como cliente con cuenta (D02, D03 y D04): rentar ahora y pagar con saldo, apartar una hora y
 * pagar en caja, y cancelar. Corre en la JVM; no cuenta como recorrido en teléfono.
 */
class RecorridosReservasViewModelTest {
    private val repo = FakeCourts()
    private lateinit var vm: ReservationsViewModel

    private val ui get() = vm.uiState.value

    @Before
    fun setUp() {
        StaffHarness.installMain()
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val field = unsafeClass.getDeclaredField("theUnsafe")
        field.isAccessible = true
        val session =
            unsafeClass
                .getMethod(
                    "allocateInstance",
                    Class::class.java,
                ).invoke(field.get(null), GuestReservationSession::class.java)
                as GuestReservationSession
        vm = ReservationsViewModel(repo, session, VenueContextStore())
        vm.onVisible(guestSlug = null, hasAccount = true)
        StaffHarness.awaitUntil("el horario cargado") { ui.day != null && ui.selectedCourtId == 7 }
    }

    @Test
    fun d02_rentarAhoraApartaDesdeYaConLaDuracionMasCortaYSePagaConSaldo() {
        vm.pickRentNow()
        assertEquals(60, ui.selectedDuration)
        vm.reserve()
        StaffHarness.awaitUntil("el apartado") { ui.pending != null && !ui.working }
        assertEquals(Triple(7, null, 60), repo.created.single())
        assertTrue(ui.pendingStartsNow)
        vm.pay(ReservationPaymentMethod.BALANCE)
        StaffHarness.awaitUntil("el pago") { ui.pending == null && !ui.working && ui.notice != null }
        assertEquals(listOf("r1" to ReservationPaymentMethod.BALANCE), repo.paid.toList())
        assertEquals(true, ui.notice!!.startsWith("¡Listo!"))
    }

    @Test
    fun d02_sinSaldoSuficienteDiceQueRecargueOPagueEnCajaYLaReservaSigueApartada() {
        repo.payFails = ApiClientException("INSUFFICIENT_WALLET_BALANCE", "x", 409)
        vm.pickRentNow()
        vm.reserve()
        StaffHarness.awaitUntil("el apartado") { ui.pending != null && !ui.working }
        vm.pay(ReservationPaymentMethod.BALANCE)
        StaffHarness.awaitUntil("el error") {
            ui.error ==
                "No te alcanza el saldo para esta renta. Recarga o paga en caja." &&
                !ui.working
        }
        assertEquals("r1", ui.pending?.id)
    }

    @Test
    fun d03_apartarUnaHoraConInicioYPagarEnCajaDiceQuePagueAntesDeQueVenza() {
        val start = Instant.parse("2026-10-01T21:00:00Z")
        vm.selectStart(start)
        vm.selectDuration(90)
        vm.reserve()
        StaffHarness.awaitUntil("el apartado") { ui.pending != null && !ui.working }
        assertEquals(Triple(7, start, 90), repo.created.single())
        vm.pay(ReservationPaymentMethod.CASH)
        StaffHarness.awaitUntil("el aviso") { ui.notice == "Paga en caja antes de que venza el apartado." }
        assertEquals(listOf("r1" to ReservationPaymentMethod.CASH), repo.paid.toList())
    }

    @Test
    fun d03_sinElegirInicioNiAhoraNoSeAparta() {
        // Elegir la cancha borra el inicio y la duración: hasta elegirlos de nuevo no hay nada que apartar.
        vm.selectCourt(7)
        vm.reserve()
        Thread.sleep(150)
        assertTrue(repo.created.isEmpty())
        assertNull(ui.pending)
    }

    @Test
    fun d04_cancelarUnaReservaAvisaYLaQuitaDeMisReservas() {
        repo.mine = listOf(repo.reservation(state = ReservationState.CONFIRMED))
        vm.dismissMessages()
        vm.onVisible(guestSlug = null, hasAccount = true)
        StaffHarness.awaitUntil("mis reservas") { ui.mine.size == 1 }
        vm.cancel(ui.mine.single())
        StaffHarness.awaitUntil("cancelada") { ui.notice == "Reserva cancelada." && !ui.working }
        assertEquals(listOf("r1"), repo.cancelled.toList())
        StaffHarness.awaitUntil("mis reservas vacías") { ui.mine.isEmpty() }
        assertNull(ui.pending)
    }
}
