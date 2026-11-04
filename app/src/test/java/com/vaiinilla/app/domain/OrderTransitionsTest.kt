package com.vaiinilla.app.domain

import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderTransitions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderTransitionsTest {
    @Test
    fun `the requestable edges match the server`() {
        assertTrue(OrderTransitions.canRequest(OrderState.PENDING_PAYMENT, OrderState.PREPARING))
        assertTrue(OrderTransitions.canRequest(OrderState.PAID, OrderState.PREPARING))
        assertTrue(OrderTransitions.canRequest(OrderState.PREPARING, OrderState.READY))
        assertTrue(OrderTransitions.canRequest(OrderState.READY, OrderState.DELIVERED))
    }

    @Test
    fun `stale or skipped steps are not requested`() {
        assertFalse(OrderTransitions.canRequest(OrderState.READY, OrderState.PREPARING))
        assertFalse(OrderTransitions.canRequest(OrderState.PAID, OrderState.READY))
        assertFalse(OrderTransitions.canRequest(OrderState.PREPARING, OrderState.DELIVERED))
        assertFalse(OrderTransitions.canRequest(OrderState.CANCELED, OrderState.DELIVERED))
    }

    @Test
    fun `states the staff never requests have no sources`() {
        assertEquals(emptySet<OrderState>(), OrderTransitions.sourcesOf(OrderState.PAID))
        assertEquals(emptySet<OrderState>(), OrderTransitions.sourcesOf(OrderState.ON_THE_WAY))
    }
}
