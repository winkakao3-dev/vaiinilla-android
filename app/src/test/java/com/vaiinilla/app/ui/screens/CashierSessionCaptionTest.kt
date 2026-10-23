package com.vaiinilla.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

/** El indicador de Caja (G01): con la sesión abierta dice en qué negocio se cobra. */
class CashierSessionCaptionTest {
    @Test
    fun `con la caja abierta dice la sesion y el negocio`() {
        assertEquals("Sesión de caja abierta · USAGI", cashierSessionCaption(true, " USAGI "))
        assertEquals("Sesión de caja abierta", cashierSessionCaption(true, ""))
    }

    @Test
    fun `cerrada o sin saber sigue como cuenta de caja`() {
        assertEquals("Cuenta de caja", cashierSessionCaption(false, "USAGI"))
        assertEquals("Cuenta de caja", cashierSessionCaption(null, "USAGI"))
    }
}
