package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.model.OrderDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PayAtEndGatingTest {
    private fun status(
        accepting: Boolean = true,
        cashOpen: Boolean = false,
        cashier: Boolean = false,
        kitchen: Boolean = false,
        allowsPayAtEnd: Boolean = true,
    ) = OperationalStatus(
        acceptingOrders = accepting,
        cashSessionOpen = cashOpen,
        cashierOnline = cashier,
        kitchenOnline = kitchen,
        estimatedTimeMinutes = 10,
        consultedAt = "2026-09-29T20:00:00.000Z",
        allowsPayAtEnd = allowsPayAtEnd,
    )

    private fun state(
        status: OperationalStatus?,
        payAtEnd: Boolean,
        destination: OrderDestination = OrderDestination.IN_SPACE,
    ) = OrderFlowUiState(
        operationalStatus = status,
        checkoutPayAtEnd = payAtEnd,
        checkoutDestination = destination,
    )

    @Test
    fun `an order to the account only needs the establishment to accept orders`() {
        assertTrue(state(status(), payAtEnd = true).isOperationallyReady)
        assertNull(status().checkoutStaffBlocker(payAtEnd = true))
    }

    @Test
    fun `any other payment still needs cash, cashier and kitchen`() {
        assertFalse(state(status(), payAtEnd = false).isOperationallyReady)
        assertEquals(ESTABLISHMENT_CLOSED_MESSAGE, status().checkoutStaffBlocker())
        assertTrue(
            state(status(cashOpen = true, cashier = true, kitchen = true), payAtEnd = false).isOperationallyReady,
        )
    }

    @Test
    fun `an establishment that does not accept orders blocks even pay at end`() {
        assertFalse(state(status(accepting = false), payAtEnd = true).isOperationallyReady)
        assertEquals(ESTABLISHMENT_CLOSED_MESSAGE, status(accepting = false).checkoutStaffBlocker(payAtEnd = true))
    }

    @Test
    fun `pay at end is offered only in a space of an establishment that allows it`() {
        assertTrue(state(status(), payAtEnd = false).canPayAtEnd)
        assertFalse(state(status(allowsPayAtEnd = false), payAtEnd = false).canPayAtEnd)
        assertFalse(state(status(), payAtEnd = false, destination = OrderDestination.TAKE_AWAY).canPayAtEnd)
        assertFalse(state(null, payAtEnd = false).canPayAtEnd)
    }
}
