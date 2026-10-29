package com.vaiinilla.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WalletMovementLabelsTest {
    @Test
    fun `una compra con saldo dice su folio, el lugar y que se compro`() {
        assertEquals("Compra #12", movementTitle("compra", "Compra con saldo", 12))
        assertEquals(
            "Mesa 2 · 2× Molletes, 1× Café americano y 2 más",
            movementDetail("Mesa 2", "2× Molletes, 1× Café americano y 2 más"),
        )
        assertEquals("Para llevar", movementDetail("Para llevar", null))
    }

    @Test
    fun `una recarga o un servidor anterior se quedan como antes`() {
        assertEquals("Recarga en Caja", movementTitle("recarga_efectivo", "Recarga", null))
        assertEquals("Compra", movementTitle("compra", "Compra con saldo", null))
        assertNull(movementDetail(null, null))
    }
}
