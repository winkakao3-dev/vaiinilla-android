package com.vaiinilla.app.ui.hotel.staff

import com.vaiinilla.app.data.hotel.FakeHotelRepository
import com.vaiinilla.app.domain.hotel.HotelRepository
import com.vaiinilla.app.domain.hotel.HotelRequest
import com.vaiinilla.app.domain.hotel.RequestAction
import com.vaiinilla.app.domain.hotel.RequestStatus
import com.vaiinilla.app.domain.hotel.RequestType
import com.vaiinilla.app.ui.operational.StaffHarness
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** Guarda la acción que el tablero manda por cada solicitud. */
private class RecordingHotel(
    private val inner: HotelRepository,
) : HotelRepository by inner {
    val sent = mutableListOf<Pair<String, RequestAction>>()

    override fun transition(
        requestId: String,
        action: RequestAction,
        idempotencyKey: String,
        expectedVersion: Int?,
    ): Result<HotelRequest> {
        sent += requestId to action
        return inner.transition(requestId, action, idempotencyKey, expectedVersion)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class HotelRoomsViewModelTest {
    private val fake = FakeHotelRepository()
    private val hotel = RecordingHotel(fake)

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        fake.reset(linkedGuest = true)
    }

    @After
    fun tearDown() {
        StaffHarness.restoreMain()
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "la condición no se cumplió a tiempo" }
            Thread.sleep(10)
        }
    }

    private fun lateCheckout(): HotelRequest =
        fake.createRequest(RequestType.LATE_CHECKOUT, 1, null, "k-late").getOrThrow()

    private fun advanceOnBoard(requestId: String) {
        val board = HotelRoomsViewModel(hotel)
        board.refresh()
        waitUntil { board.state.value.loaded }
        board.advance(
            board.state.value.requests
                .first { it.id == requestId },
        )
        waitUntil { hotel.sent.isNotEmpty() && board.state.value.busyId == null }
    }

    @Test
    fun `una salida tardia pendiente se aprueba`() {
        val request = lateCheckout()

        advanceOnBoard(request.id)

        assertEquals(listOf(request.id to RequestAction.APPROVE), hotel.sent)
    }

    @Test
    fun `una salida tardia ya tomada tambien se aprueba, no se cierra sin cargo`() {
        val request = lateCheckout()
        fake.transition(request.id, RequestAction.TAKE, "k-take", request.version).getOrThrow()

        advanceOnBoard(request.id)

        assertEquals(listOf(request.id to RequestAction.APPROVE), hotel.sent)
    }

    @Test
    fun `lo demas se toma y despues se da por listo`() {
        val pendingTowels = fake.openRequests().getOrThrow().first { it.status == RequestStatus.PENDING }
        val takenCleaning = fake.openRequests().getOrThrow().first { it.status == RequestStatus.TAKEN }

        advanceOnBoard(pendingTowels.id)
        advanceOnBoard(takenCleaning.id)

        assertEquals(
            listOf(pendingTowels.id to RequestAction.TAKE, takenCleaning.id to RequestAction.RESOLVE),
            hotel.sent,
        )
    }
}
