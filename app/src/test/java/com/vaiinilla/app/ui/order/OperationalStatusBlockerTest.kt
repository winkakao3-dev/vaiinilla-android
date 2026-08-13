package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.OperationalStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun `names which station is offline`() {
        assertTrue(status(cashier = false, kitchen = true).checkoutStaffBlocker()!!.contains("Caja"))
        assertTrue(status(cashier = true, kitchen = false).checkoutStaffBlocker()!!.contains("Cocina"))
        assertEquals(
            "Caja y Cocina no están en línea. Tienen que quedar abiertas en otros dispositivos (o en la web) mientras pides como alumno.",
            status(cashier = false, kitchen = false).checkoutStaffBlocker(),
        )
    }
}
