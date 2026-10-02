package com.vaiinilla.app.domain.model

import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SkipsKitchenTest {
    private fun order(
        state: OrderState,
        vararg stations: PreparationStation,
    ): OrderDetail {
        val base = ScreenshotFixtures.sampleOrder(state = state)
        val item = base.items.first()
        return base.copy(items = stations.mapIndexed { i, s -> item.copy(id = i + 1, preparationStation = s) })
    }

    @Test
    fun `a drink-only order in ready skips the kitchen`() {
        assertTrue(order(OrderState.READY, PreparationStation.CASHIER).skipsKitchen)
    }

    @Test
    fun `an order with a kitchen item does not skip it`() {
        assertFalse(order(OrderState.READY, PreparationStation.CASHIER, PreparationStation.KITCHEN).skipsKitchen)
    }

    @Test
    fun `an order already in preparing keeps the step`() {
        assertFalse(order(OrderState.PREPARING, PreparationStation.CASHIER).skipsKitchen)
    }

    @Test
    fun `a rejected kitchen item does not count`() {
        val base = order(OrderState.READY, PreparationStation.CASHIER, PreparationStation.KITCHEN)
        val rejected =
            base.copy(
                items =
                    base.items.map {
                        if (it.preparationStation == PreparationStation.KITCHEN) {
                            it.copy(rejection = OrderItemRejection("Se acabó", "10.00"))
                        } else {
                            it
                        }
                    },
            )
        assertTrue(rejected.skipsKitchen)
    }
}
