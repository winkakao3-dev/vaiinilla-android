package com.vaiinilla.app.ui.reservations

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.vaiinilla.app.GuestReservationTest
import com.vaiinilla.app.InMemoryGuestKeyVault
import com.vaiinilla.app.data.guest.GuestReservationSession
import com.vaiinilla.app.data.guest.GuestSessionStore
import com.vaiinilla.app.domain.model.CourtDay
import com.vaiinilla.app.domain.model.CourtSchedule
import com.vaiinilla.app.domain.model.CustomerPrice
import com.vaiinilla.app.domain.model.Reservation
import com.vaiinilla.app.domain.model.ReservationAccess
import com.vaiinilla.app.domain.model.ReservationPayment
import com.vaiinilla.app.domain.model.ReservationPaymentMethod
import com.vaiinilla.app.domain.model.ReservationRefund
import com.vaiinilla.app.domain.model.ReservationState
import com.vaiinilla.app.domain.model.Venue
import com.vaiinilla.app.domain.repository.ReservationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** L08: "Rentar ahora" aparta de una vez y otro tiempo en la hoja de pago suelta el apartado y aparta otro. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ReservationDurationChangeTest {
    private val now = Instant.parse("2026-11-03T22:10:00Z")
    private val calls = mutableListOf<String>()
    private var failNextCreate = false

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        Venue.update(null)
        Dispatchers.resetMain()
    }

    @Test
    fun `rent now holds at once and another time swaps the hold`() {
        val viewModel = loadedViewModel()

        viewModel.rentNow()
        val first = waitFor { viewModel.uiState.value.pending }
        assertEquals(60, first.durationMinutes)
        assertTrue(viewModel.uiState.value.pendingStartsNow)

        viewModel.changePendingDuration(90)
        val second = waitFor { viewModel.uiState.value.pending?.takeIf { it.durationMinutes == 90 } }
        viewModel.onHidden()

        assertEquals(listOf("create ahora 60", "cancel r-1", "create ahora 90"), calls)
        assertEquals("r-2", second.id)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `when the new hold fails the court is left free and the sheet closes`() {
        val viewModel = loadedViewModel()
        viewModel.rentNow()
        waitFor { viewModel.uiState.value.pending }

        failNextCreate = true
        viewModel.changePendingDuration(120)
        val error = waitFor { viewModel.uiState.value.error }
        viewModel.onHidden()

        assertNull(viewModel.uiState.value.pending)
        assertTrue(error, error.endsWith("La cancha quedó libre: vuelve a elegirla."))
        assertEquals(listOf("create ahora 60", "cancel r-1", "create ahora 120"), calls)
    }

    @Test
    fun `players from the payment sheet swap the hold and keep the time (U03)`() {
        val viewModel = loadedViewModel()
        viewModel.rentNow()
        waitFor { viewModel.uiState.value.pending }

        viewModel.changePendingPlayers(4)
        val second = waitFor { viewModel.uiState.value.pending?.takeIf { it.players == 4 } }
        viewModel.changePendingDuration(90)
        val third = waitFor { viewModel.uiState.value.pending?.takeIf { it.durationMinutes == 90 } }
        viewModel.onHidden()

        assertEquals(60, second.durationMinutes)
        // Cambiar el tiempo conserva los jugadores.
        assertEquals(4, third.players)
        assertEquals(
            listOf("create ahora 60", "cancel r-1", "create ahora 60 j4", "cancel r-2", "create ahora 90 j4"),
            calls,
        )
    }

    @Test
    fun `a new hold does not inherit the players of the previous one`() {
        val viewModel = loadedViewModel()
        viewModel.selectPlayers(3)

        viewModel.rentNow()
        waitFor { viewModel.uiState.value.pending }
        viewModel.onHidden()

        assertEquals(0, viewModel.uiState.value.players)
    }

    @Test
    fun `a resumed rent-now hold is held again from now (R10)`() {
        val viewModel = loadedViewModel()
        // «Mis reservas» › «Retomar pago»: su inicio (el minuto en que se apartó) ya pasó y no cae en bloque.
        val held = reservation("r-9", 60, ReservationState.PENDING_PAYMENT).copy(start = now.plusSeconds(420))
        viewModel.resumePayment(held)
        assertTrue(viewModel.uiState.value.pendingStartsNow)

        viewModel.changePendingDuration(90)
        waitFor { viewModel.uiState.value.pending?.takeIf { it.durationMinutes == 90 } }
        viewModel.onHidden()

        assertEquals(listOf("cancel r-9", "create ahora 90"), calls)
    }

    @Test
    fun `bowling asks for players before renting now and caps them at the lane capacity (R11, R25)`() {
        Venue.update(Venue.BOWLING)
        val viewModel = loadedViewModel()

        viewModel.rentNow()
        // Sin jugadores no se aparta: el resumen los pide primero.
        assertTrue(viewModel.uiState.value.rentNow)
        assertFalse(viewModel.uiState.value.canReserve)
        assertTrue(calls.isEmpty())

        viewModel.selectPlayers(10)
        assertEquals(6, viewModel.uiState.value.players)
        viewModel.reserve()
        waitFor { viewModel.uiState.value.pending }
        viewModel.onHidden()

        assertEquals(listOf("create ahora 60 j6"), calls)
    }

    @Test
    fun `invitado sin qr va directo a entrar con su cuenta y con qr escribe su nombre`() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val api = GuestReservationTest.FakeApi { _, _, _, _ -> Result.failure(IllegalStateException("sin red")) }
        val session = GuestReservationSession(api, GuestSessionStore(context, InMemoryGuestKeyVault()))
        val viewModel = ReservationsViewModel(FakeRepository(), session)

        viewModel.onVisible(guestSlug = "club", hasAccount = false)
        assertTrue(viewModel.uiState.value.accountRequired)
        assertFalse(viewModel.uiState.value.guestEntry)

        viewModel.onVisible(guestSlug = "club", hasAccount = false, cameThroughQr = true)
        assertFalse(viewModel.uiState.value.accountRequired)
        assertTrue(viewModel.uiState.value.guestEntry)
        viewModel.onHidden()
    }

    private fun loadedViewModel(): ReservationsViewModel {
        val context: Context = ApplicationProvider.getApplicationContext()
        val api = GuestReservationTest.FakeApi { _, _, _, _ -> Result.failure(IllegalStateException("sin red")) }
        val session = GuestReservationSession(api, GuestSessionStore(context, InMemoryGuestKeyVault()))
        val viewModel = ReservationsViewModel(FakeRepository(), session)
        viewModel.onVisible(guestSlug = null, hasAccount = true)
        waitFor { viewModel.uiState.value.day }
        viewModel.selectCourt(7)
        return viewModel
    }

    private fun <T : Any> waitFor(read: () -> T?): T {
        repeat(150) {
            read()?.let { return it }
            Thread.sleep(20)
        }
        error("no llegó a tiempo")
    }

    private fun reservation(
        id: String,
        minutes: Int,
        state: ReservationState,
    ) = Reservation(
        id = id,
        courtId = 7,
        courtName = "Cancha 1",
        start = now,
        end = now.plusSeconds(minutes * 60L),
        durationMinutes = minutes,
        amount = "350.00",
        customerPrice = null,
        state = state,
        holdExpiresAt = now.plusSeconds(600),
        orderId = null,
        channel = "cliente",
        customerName = null,
        version = 1,
    )

    private inner class FakeRepository : ReservationRepository {
        private var created = 0

        override fun day(
            date: String?,
            access: ReservationAccess,
        ): Result<CourtDay> =
            Result.success(
                CourtDay(
                    date = "2026-11-03",
                    today = "2026-11-03",
                    timeZone = "America/Chihuahua",
                    opensAt = now.minusSeconds(7_200),
                    closesAt = now.plusSeconds(21_600),
                    blockMinutes = 30,
                    durations = listOf(60, 90, 120),
                    daysAhead = 7,
                    holdMinutes = 10,
                    now = now,
                    courts =
                        listOf(
                            CourtSchedule(
                                id = 7,
                                name = "Cancha 1",
                                pricePerHour = "350.00",
                                customerPricePerHour = CustomerPrice(card = "350.00", cashOrBalance = "350.00"),
                                rentable = true,
                                busy = emptyList(),
                                capacity = 6,
                            ),
                        ),
                ),
            )

        override fun list(
            access: ReservationAccess,
            date: String?,
        ): Result<List<Reservation>> = Result.success(emptyList())

        override fun create(
            access: ReservationAccess,
            courtId: Int,
            start: Instant?,
            durationMinutes: Int,
            customerName: String?,
            idempotencyKey: String,
            players: Int?,
        ): Result<Reservation> {
            calls += "create ${start ?: "ahora"} $durationMinutes" + (players?.let { " j$it" } ?: "")
            if (failNextCreate) return Result.failure(IllegalStateException("Ese horario ya se tomó."))
            created++
            return Result.success(
                reservation("r-$created", durationMinutes, ReservationState.PENDING_PAYMENT).copy(players = players),
            )
        }

        override fun pay(
            access: ReservationAccess,
            reservationId: String,
            method: ReservationPaymentMethod,
            cashReceived: String?,
            idempotencyKey: String,
        ): Result<ReservationPayment> = Result.failure(UnsupportedOperationException())

        override fun cancel(
            access: ReservationAccess,
            reservationId: String,
            idempotencyKey: String,
            refund: ReservationRefund?,
        ): Result<Reservation> {
            calls += "cancel $reservationId"
            return Result.success(reservation(reservationId, 60, ReservationState.CANCELLED))
        }
    }
}
