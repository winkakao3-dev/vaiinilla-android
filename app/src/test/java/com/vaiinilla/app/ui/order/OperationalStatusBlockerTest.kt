package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.OperationalStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OperationalStatusBlockerTest {
    private fun status(
        accepting: Boolean = false,
        cashOpen: Boolean = true,
        cashier: Boolean = true,
        kitchen: Boolean = true,
    ) = OperationalStatus(
        acceptingOrders = accepting,
        cashSessionOpen = cashOpen,
        cashierOnline = cashier,
        kitchenOnline = kitchen,
        estimatedTimeMinutes = 12,
        consultedAt = "2026-08-12T00:00:00.000Z",
    )

    @Test
    fun `ready shop has no blocker`() {
        assertNull(status(accepting = true).checkoutStaffBlocker())
    }

    @Test
    fun `uses neutral establishment copy when operations are unavailable`() {
        assertEquals(
            ESTABLISHMENT_CLOSED_MESSAGE,
            status(cashier = false, kitchen = true).checkoutStaffBlocker(),
        )
        assertEquals(
            ESTABLISHMENT_CLOSED_MESSAGE,
            status(cashier = true, kitchen = false).checkoutStaffBlocker(),
        )
        assertEquals(
            ESTABLISHMENT_CLOSED_MESSAGE,
            status(cashier = false, kitchen = false).checkoutStaffBlocker(),
        )
    }
}
