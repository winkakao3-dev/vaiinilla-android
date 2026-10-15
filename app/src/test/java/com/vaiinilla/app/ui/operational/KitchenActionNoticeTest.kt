package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KitchenActionNoticeTest {
    private fun orderIn(state: OrderState): OrderDetail {
        val order = ScreenshotFixtures.sampleOrder(state = state)
        return order.copy(summary = order.summary.copy(folio = 42))
    }

    @Test
    fun `confirms the state the server returned`() {
        assertEquals("Comanda #42 en preparación", kitchenActionNotice(orderIn(OrderState.PREPARING)))
        assertEquals("Comanda #42 lista", kitchenActionNotice(orderIn(OrderState.READY)))
        assertEquals("Comanda #42 rechazada", kitchenActionNotice(orderIn(OrderState.CANCELED)))
    }

    @Test
    fun `no confirmation when the server kept or moved the order elsewhere`() {
        assertNull(kitchenActionNotice(orderIn(OrderState.PAID)))
        assertNull(kitchenActionNotice(orderIn(OrderState.DELIVERED)))
        assertNull(kitchenActionNotice(orderIn(OrderState.EXPIRED)))
    }
}
