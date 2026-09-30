package com.vaiinilla.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CashCloseSheetTest {
    @Test
    fun `normaliza el efectivo contado a dos decimales`() {
        assertEquals("725.50", normalizeCountedCash("725.5"))
        assertEquals("725.50", normalizeCountedCash(" $725.50 "))
        assertEquals("1200.00", normalizeCountedCash("1200"))
        assertEquals("725.50", normalizeCountedCash("725,50"))
        assertEquals("0.00", normalizeCountedCash("0"))
        assertEquals("15.00", normalizeCountedCash("015"))
    }

    @Test
    fun `rechaza montos invalidos`() {
        assertNull(normalizeCountedCash(""))
        assertNull(normalizeCountedCash("abc"))
        assertNull(normalizeCountedCash("-5"))
        assertNull(normalizeCountedCash("12.345"))
        assertNull(normalizeCountedCash("1.2.3"))
    }
}
