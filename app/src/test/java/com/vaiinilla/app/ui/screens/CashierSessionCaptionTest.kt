package com.vaiinilla.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

/** El indicador de Caja (G01, C01): con la sesión abierta dice en qué negocio se cobra, con qué fondo y desde qué hora. */
class CashierSessionCaptionTest {
    @Test
    fun `con la caja abierta dice el negocio`() {
        assertEquals("USAGI", cashierSessionCaption(true, " USAGI "))
        assertEquals("Sesión de caja abierta", cashierSessionCaption(true, ""))
    }

    @Test
    fun `con el fondo y la hora de apertura los suma en la hora del negocio`() {
        assertEquals(
            "Pastelería Vaini · fondo $500 · desde 9:02",
            cashierSessionCaption(true, "Pastelería Vaini", openingAmount = "500.00", openedAt = "2026-11-03T15:02:00Z"),
        )
        assertEquals("Pastelería Vaini", cashierSessionCaption(true, "Pastelería Vaini", openedAt = "no es fecha"))
    }

    @Test
    fun `cerrada o sin saber sigue como cuenta de caja`() {
        assertEquals("Cuenta de caja", cashierSessionCaption(false, "USAGI"))
        assertEquals("Cuenta de caja", cashierSessionCaption(null, "USAGI"))
    }
}
