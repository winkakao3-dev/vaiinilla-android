package com.vaiinilla.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CashBillCounterTest {
    @Test
    fun `suma billetes y monedas`() {
        assertEquals("2310.00", billCountTotal(mapOf(500 to 2, 200 to 4, 100 to 3, 50 to 3, 20 to 2), "20"))
        assertEquals("0.00", billCountTotal(emptyMap(), ""))
        assertEquals("100.50", billCountTotal(mapOf(100 to 1), "0.5"))
    }

    @Test
    fun `monedas mal escritas no dan total`() {
        assertNull(billCountTotal(mapOf(100 to 1), "abc"))
    }
}
