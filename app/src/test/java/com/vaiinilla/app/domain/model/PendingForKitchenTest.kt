package com.vaiinilla.app.domain.model

import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PendingForKitchenTest {
    @Test
    fun `solo las comandas por cobrar o cobradas son pendientes de cocina`() {
        val orders =
            listOf(
                ScreenshotFixtures.sampleOrder(state = OrderState.PENDING_PAYMENT),
                ScreenshotFixtures.sampleOrder(state = OrderState.PAID),
                ScreenshotFixtures.sampleOrder(state = OrderState.PREPARING),
                ScreenshotFixtures.sampleOrder(state = OrderState.READY),
            )

        assertEquals(
            listOf(OrderState.PENDING_PAYMENT, OrderState.PAID),
            orders.pendingForKitchen.map { it.summary.state },
        )
    }
}
