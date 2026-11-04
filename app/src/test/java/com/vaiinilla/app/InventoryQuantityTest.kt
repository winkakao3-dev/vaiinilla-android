package com.vaiinilla.app

import com.vaiinilla.app.ui.admin.formatQuantity
import com.vaiinilla.app.ui.admin.parseQuantity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InventoryQuantityTest {
    @Test
    fun `la coma con tres cifras separa miles`() {
        assertEquals(1500.0, parseQuantity("1,500")!!, 0.0)
        assertEquals(1500.5, parseQuantity("1,500.5")!!, 0.0)
        assertEquals(1_234_567.0, parseQuantity("1,234,567")!!, 0.0)
    }

    @Test
    fun `la coma con uno o dos decimales es punto decimal`() {
        assertEquals(12.5, parseQuantity("12,5")!!, 0.0)
        assertEquals(0.5, parseQuantity("0,500")!!, 0.0)
        assertEquals(1.5, parseQuantity(" 1.5 ")!!, 0.0)
    }

    @Test
    fun `vacio, negativo o mezclado es null`() {
        assertNull(parseQuantity(""))
        assertNull(parseQuantity("-3"))
        assertNull(parseQuantity("1,5.3"))
        assertNull(parseQuantity("abc"))
    }

    @Test
    fun `costos con cuatro decimales`() {
        assertEquals("0.0123", formatQuantity(0.0123, decimals = 4))
        assertEquals("0.012", formatQuantity(0.0123))
        assertEquals("12", formatQuantity(12.0))
    }
}
