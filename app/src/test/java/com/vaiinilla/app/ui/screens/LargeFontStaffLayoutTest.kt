package com.vaiinilla.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** L01: con letra grande los accesos de Caja y los botones de rol no parten ni cortan palabras. */
class LargeFontStaffLayoutTest {
    @Test
    fun `caja deja sus tres accesos a lo ancho con letra normal`() {
        assertFalse(cashierQuickActionsStack(screenWidthDp = 360, fontScale = 1f, count = 3))
        assertFalse(cashierQuickActionsStack(screenWidthDp = 411, fontScale = 1f, count = 3))
    }

    @Test
    fun `con letra 1_3 los accesos de caja van en renglones`() {
        assertTrue(cashierQuickActionsStack(screenWidthDp = 411, fontScale = 1.3f, count = 3))
        // Dos mosaicos sí caben a lo ancho con 1.3; con 2.0, ya no.
        assertFalse(cashierQuickActionsStack(screenWidthDp = 360, fontScale = 1.3f, count = 2))
        assertTrue(cashierQuickActionsStack(screenWidthDp = 360, fontScale = 2f, count = 2))
        assertFalse(cashierQuickActionsStack(screenWidthDp = 360, fontScale = 1.3f, count = 1))
    }

    @Test
    fun `con letra grande un boton de rol por renglon`() {
        assertEquals(2, modeButtonsPerRow(count = 4, fontScale = 1f))
        assertEquals(3, modeButtonsPerRow(count = 3, fontScale = 1f))
        assertEquals(1, modeButtonsPerRow(count = 4, fontScale = 1.3f))
    }
}
