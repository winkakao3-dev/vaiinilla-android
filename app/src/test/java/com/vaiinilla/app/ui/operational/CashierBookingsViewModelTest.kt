package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.MutationIdempotency
import com.vaiinilla.app.domain.model.CourtDay
import com.vaiinilla.app.domain.model.CourtSchedule
import com.vaiinilla.app.domain.model.Reservation
import com.vaiinilla.app.domain.model.ReservationAccess
import com.vaiinilla.app.domain.model.ReservationPayment
import com.vaiinilla.app.domain.model.ReservationPaymentMethod
import com.vaiinilla.app.domain.model.ReservationState
import com.vaiinilla.app.domain.repository.ReservationException
import com.vaiinilla.app.domain.repository.ReservationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class CashierBookingsViewModelTest {
    private val start: Instant = Instant.parse("2026-10-11T18:00:00Z")

    private fun booking(
        id: String,
        state: ReservationState,
    ) = Reservation(
        id = id,
        courtId = 1,
        courtName = "Cancha 1",
        start = start,
        end = start.plusSeconds(3600),
        durationMinutes = 60,
        amount = "150.00",
        customerPrice = null,
        state = state,
        holdExpiresAt = null,
        orderId = null,
        channel = "mostrador",
        customerName = "Ana",
        version = 1,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads today's courts and hides cancelled reservations`() {
        val repository =
            FakeReservationRepository(
                rows = listOf(booking("a", ReservationState.PENDING_PAYMENT), booking("b", ReservationState.CANCELLED)),
            )
        val viewModel = CashierBookingsViewModel(repository)

        viewModel.onVisible()
        awaitUntil { viewModel.uiState.value.loaded && !viewModel.uiState.value.loading }

        val state = viewModel.uiState.value
        assertTrue(state.hasCourts)
        assertEquals(listOf("a"), state.bookings.map { it.id })
        assertNull(state.loadError)
    }

    @Test
    fun `a first-load 404 is retried once before showing an error`() {
        val repository =
            FakeReservationRepository(
                rows = listOf(booking("a", ReservationState.PENDING_PAYMENT)),
                dayFailuresBeforeSuccess = 1,
            )
        val viewModel = CashierBookingsViewModel(repository)

        viewModel.onVisible()
        awaitUntil { viewModel.uiState.value.loaded && !viewModel.uiState.value.loading }

        assertNull(viewModel.uiState.value.loadError)
        assertEquals(2, repository.dayCalls)
    }

    @Test
    fun `cash payment sends the same key for the same amount and shows the web notice`() {
        val repository = FakeReservationRepository(rows = listOf(booking("a", ReservationState.PENDING_PAYMENT)))
        val viewModel = CashierBookingsViewModel(repository)
        viewModel.onVisible()
        awaitUntil { viewModel.uiState.value.loaded && !viewModel.uiState.value.loading }

        viewModel.startPayment(booking("a", ReservationState.PENDING_PAYMENT))
        viewModel.onReceivedChange("200")
        viewModel.submitPayment()
        awaitUntil { viewModel.uiState.value.notice != null && !viewModel.uiState.value.paymentBusy }

        assertEquals("Reserva cobrada.", viewModel.uiState.value.notice)
        assertNull(viewModel.uiState.value.paying)
        assertEquals(listOf(MutationIdempotency.reservationPayment("a", "200.00")), repository.payKeys)
    }

    @Test
    fun `cancelling a pending reservation confirms with the web message`() {
        val repository = FakeReservationRepository(rows = listOf(booking("a", ReservationState.PENDING_PAYMENT)))
        val viewModel = CashierBookingsViewModel(repository)
        viewModel.onVisible()
        awaitUntil { viewModel.uiState.value.loaded && !viewModel.uiState.value.loading }

        viewModel.askCancel(booking("a", ReservationState.PENDING_PAYMENT))
        viewModel.confirmCancel()
        awaitUntil { viewModel.uiState.value.notice != null && !viewModel.uiState.value.cancelBusy }

        assertEquals("Cancha liberada. La reserva quedó cancelada.", viewModel.uiState.value.notice)
        assertNull(viewModel.uiState.value.canceling)
        assertEquals(listOf(MutationIdempotency.reservationCancellation("a")), repository.cancelKeys)
    }

    private fun awaitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 3_000
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "La condición no se cumplió a tiempo." }
            Thread.sleep(5)
        }
    }

    private class FakeReservationRepository(
        private val rows: List<Reservation>,
        private var dayFailuresBeforeSuccess: Int = 0,
    ) : ReservationRepository {
        var dayCalls = 0
        val payKeys = mutableListOf<String>()
        val cancelKeys = mutableListOf<String>()
        private val day =
            CourtDay(
                date = "2026-10-11",
                today = "2026-10-11",
                timeZone = "America/Mexico_City",
                opensAt = Instant.parse("2026-10-11T10:00:00Z"),
                closesAt = Instant.parse("2026-10-11T23:00:00Z"),
                blockMinutes = 30,
                durations = listOf(60),
                daysAhead = 14,
                holdMinutes = 10,
                now = Instant.parse("2026-10-11T18:00:00Z"),
                courts = listOf(CourtSchedule(1, "Cancha 1", "150.00", null, true, emptyList())),
            )

        override fun day(
            date: String?,
            access: ReservationAccess,
        ): Result<CourtDay> {
            dayCalls += 1
            if (dayFailuresBeforeSuccess > 0) {
                dayFailuresBeforeSuccess -= 1
                return Result.failure(
                    ReservationException("NOT_FOUND", "no", ApiClientException("NOT_FOUND", "no", 404)),
                )
            }
            return Result.success(day)
        }

        override fun list(
            access: ReservationAccess,
            date: String?,
        ): Result<List<Reservation>> = Result.success(rows)

        override fun create(
            access: ReservationAccess,
            courtId: Int,
            start: Instant?,
            durationMinutes: Int,
            customerName: String?,
            idempotencyKey: String,
        ): Result<Reservation> = Result.failure(UnsupportedOperationException())

        override fun pay(
            access: ReservationAccess,
            reservationId: String,
            method: ReservationPaymentMethod,
            cashReceived: String?,
            idempotencyKey: String,
        ): Result<ReservationPayment> {
            payKeys += idempotencyKey
            return Result.success(
                ReservationPayment(
                    reservation = rows.first { it.id == reservationId },
                    order = null,
                    cashReceived = cashReceived,
                    cashChange = null,
                ),
            )
        }

        override fun cancel(
            access: ReservationAccess,
            reservationId: String,
            idempotencyKey: String,
        ): Result<Reservation> {
            cancelKeys += idempotencyKey
            return Result.success(rows.first { it.id == reservationId })
        }
    }
}
