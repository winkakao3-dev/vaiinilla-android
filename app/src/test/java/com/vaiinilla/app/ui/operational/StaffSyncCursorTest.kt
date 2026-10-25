package com.vaiinilla.app.ui.operational

import org.junit.Assert.assertEquals
import org.junit.Test

class StaffSyncCursorTest {
    @Test
    fun `el cursor del delta se pide con margen para no saltar commits tardios`() {
        assertEquals("2026-10-25T10:00:00Z", deltaCursor("2026-10-25T10:00:30Z"))
        assertEquals("2026-10-25T09:59:45.500Z", deltaCursor("2026-10-25T10:00:15.500Z"))
    }

    @Test
    fun `un sello que no se entiende se deja como esta`() {
        assertEquals("ayer", deltaCursor("ayer"))
    }

    @Test
    fun `el pedido de Caja con la caja cerrada no dice enviado a Caja`() {
        assertEquals("Pedido #5 enviado a Caja", counterOrderToast(5, null, false, null))
        assertEquals(
            "Pedido #5 creado. Abre la caja para poder cobrarlo.",
            counterOrderToast(5, null, false, CashierContext(cashOpen = false)),
        )
        assertEquals(
            "Pedido #5 de Mesa 2 creado. Cóbralo desde la lista de Caja.",
            counterOrderToast(5, "Mesa 2", false, CashierContext(cashOpen = true)),
        )
        assertEquals("Pedido #5 de Mesa 2 enviado a cocina", counterOrderToast(5, "Mesa 2", true, null))
    }
}
