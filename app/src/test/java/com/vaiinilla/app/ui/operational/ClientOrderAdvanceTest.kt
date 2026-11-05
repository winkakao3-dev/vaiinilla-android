package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.domain.model.OrderState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClientOrderAdvanceTest {
    @Test
    fun `forward steps and endings without delivery are announced`() {
        assertTrue(announcesClientAdvance(OrderState.PAID, OrderState.PREPARING))
        assertTrue(announcesClientAdvance(OrderState.READY, OrderState.DELIVERED))
        assertTrue(announcesClientAdvance(OrderState.PENDING_PAYMENT, OrderState.CANCELED))
        assertTrue(announcesClientAdvance(OrderState.READY, OrderState.NOT_PICKED_UP))
    }

    @Test
    fun `accepting a card preorder asks the client to pay even though the timeline does not move`() {
        assertTrue(announcesClientAdvance(OrderState.AWAITING_ACCEPTANCE, OrderState.PENDING_PAYMENT))
        assertTrue(acceptedAwaitingPayment(OrderState.AWAITING_ACCEPTANCE, OrderState.PENDING_PAYMENT))
        assertFalse(acceptedAwaitingPayment(OrderState.PENDING_PAYMENT, OrderState.PAID))
    }

    @Test
    fun `a home delivery leaving is announced`() {
        assertTrue(announcesClientAdvance(OrderState.READY, OrderState.ON_THE_WAY))
    }

    @Test
    fun `no change or a step back is not announced`() {
        assertFalse(announcesClientAdvance(OrderState.PREPARING, OrderState.PREPARING))
        assertFalse(announcesClientAdvance(OrderState.READY, OrderState.PREPARING))
    }
}
