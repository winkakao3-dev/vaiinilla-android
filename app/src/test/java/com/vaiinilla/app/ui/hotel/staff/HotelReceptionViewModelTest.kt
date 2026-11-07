package com.vaiinilla.app.ui.hotel.staff

import com.vaiinilla.app.data.hotel.FakeHotelRepository
import com.vaiinilla.app.domain.hotel.ChargeCategory
import com.vaiinilla.app.domain.hotel.HotelRepository
import com.vaiinilla.app.domain.hotel.StayCharge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/** Un cargo que falla una vez, como una respuesta que se pierde: guarda las llaves de idempotencia que recibe. */
private class FlakyChargeHotel(
    private val inner: HotelRepository,
) : HotelRepository by inner {
    val chargeKeys = mutableListOf<String>()
    private var failNext = true

    override fun addCharge(
        stayId: String,
        concept: String,
        amount: String,
        category: ChargeCategory,
        idempotencyKey: String,
    ): Result<StayCharge> {
        chargeKeys += idempotencyKey
        if (failNext) {
            failNext = false
            return Result.failure(IllegalStateException("sin red"))
        }
        return inner.addCharge(stayId, concept, amount, category, idempotencyKey)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class HotelReceptionViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "la condición no se cumplió a tiempo" }
            Thread.sleep(10)
        }
    }

    private fun hotelWithStay(): Pair<FlakyChargeHotel, String> {
        val inner = FakeHotelRepository()
        inner.reset(linkedGuest = true)
        val stayId = inner.myStay().getOrThrow()!!.id
        return FlakyChargeHotel(inner) to stayId
    }

    @Test
    fun `retrying a manual charge after a lost response sends the same idempotency key`() {
        val (hotel, stayId) = hotelWithStay()
        val viewModel = HotelReceptionViewModel(hotel)

        viewModel.addCharge(stayId, "Minibar", "90.00", ChargeCategory.MINIBAR)
        waitUntil { !viewModel.state.value.busy && viewModel.state.value.error != null }
        viewModel.addCharge(stayId, "Minibar", "90.00", ChargeCategory.MINIBAR)
        waitUntil { hotel.chargeKeys.size == 2 && !viewModel.state.value.busy }

        assertEquals(hotel.chargeKeys[0], hotel.chargeKeys[1])
        // Con el cargo aceptado, el siguiente igual ya es otro cargo y estrena llave.
        viewModel.addCharge(stayId, "Minibar", "90.00", ChargeCategory.MINIBAR)
        waitUntil { hotel.chargeKeys.size == 3 && !viewModel.state.value.busy }
        assert(hotel.chargeKeys[2] != hotel.chargeKeys[1])
    }

    @Test
    fun `reloading the lists does not erase the error of a failed write`() {
        val (hotel, stayId) = hotelWithStay()
        val viewModel = HotelReceptionViewModel(hotel)

        viewModel.addCharge(stayId, "Spa", "300.00", ChargeCategory.SPA)
        waitUntil { !viewModel.state.value.busy && viewModel.state.value.error != null }
        val error = viewModel.state.value.error

        viewModel.refresh()
        waitUntil { viewModel.state.value.loaded }

        assertNotNull(error)
        assertEquals(error, viewModel.state.value.error)
    }
}
