package com.vaiinilla.app.ui.hotel.guest

import com.vaiinilla.app.data.hotel.FakeHotelRepository
import com.vaiinilla.app.domain.hotel.HotelRepository
import com.vaiinilla.app.domain.hotel.HotelRequest
import com.vaiinilla.app.domain.hotel.RequestType
import com.vaiinilla.app.ui.operational.StaffHarness
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

/** Una solicitud que falla una vez, como una respuesta que se pierde: guarda la llave y la cantidad de cada intento. */
private class FlakyRequestHotel(
    private val inner: HotelRepository,
) : HotelRepository by inner {
    val attempts = mutableListOf<Pair<String, Int>>()
    private var failNext = true

    override fun createRequest(
        type: RequestType,
        quantity: Int,
        note: String?,
        idempotencyKey: String,
    ): Result<HotelRequest> {
        attempts += idempotencyKey to quantity
        if (failNext) {
            failNext = false
            return Result.failure(IllegalStateException("sin red"))
        }
        return inner.createRequest(type, quantity, note, idempotencyKey)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class HotelRequestsViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
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

    @Test
    fun `a retry of the same request keeps the key and a new quantity gets another`() {
        val inner = FakeHotelRepository()
        inner.reset(linkedGuest = true)
        val hotel = FlakyRequestHotel(inner)
        val viewModel = HotelRequestsViewModel(hotel)

        viewModel.tap(RequestType.TOWELS)
        viewModel.submit()
        waitUntil { !viewModel.state.value.submitting && viewModel.state.value.error != null }
        viewModel.submit()
        waitUntil { hotel.attempts.size == 2 && !viewModel.state.value.submitting }
        assertEquals(hotel.attempts[0].first, hotel.attempts[1].first)

        // Otra cantidad después de un fallo es otra solicitud: no reusa la llave del cuerpo anterior.
        val second = FlakyRequestHotel(inner)
        val other = HotelRequestsViewModel(second)
        other.tap(RequestType.PILLOWS)
        other.submit()
        waitUntil { !other.state.value.submitting && other.state.value.error != null }
        other.tap(RequestType.PILLOWS)
        other.submit()
        waitUntil { second.attempts.size == 2 && !other.state.value.submitting }
        assertNotEquals(second.attempts[0].first, second.attempts[1].first)
        assertEquals(listOf(1, 2), second.attempts.map { it.second })
    }
}
