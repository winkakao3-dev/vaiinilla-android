package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.data.cashcut.CashCutJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** «Cobrado hoy» en Caja: lo del día con todos los métodos, no solo el efectivo. */
class CollectedTodayTest {
    @Test
    fun `suma todos los metodos y desglosa de mayor a menor sin los que van en cero`() {
        val raw =
            """
            {"data": {"fecha": "2026-11-09", "tipo": "parcial", "turnos": [],
              "resumen": {"ventas": "1325.00", "pedidos": 14},
              "formas_pago": [
                {"metodo": "efectivo", "pedidos": 8, "monto": "410.00"},
                {"metodo": "stripe", "pedidos": 5, "monto": "820.00"},
                {"metodo": "saldo", "pedidos": 1, "monto": "95.00"},
                {"metodo": "terminal", "pedidos": 0, "monto": "0.00"}
              ]}}
            """.trimIndent()
        val today = CollectedToday.of(CashCutJson.dayCut(raw))
        assertEquals("1325.00", today.total)
        assertEquals(listOf("efectivo", "stripe", "saldo"), today.payments.map { it.method })
        assertEquals(
            "Tarjeta en línea $820 · Efectivo $410 · Saldo $95",
            today.breakdownLine { "$" + it.removeSuffix(".00") },
        )
    }

    @Test
    fun `sin formas de pago no hay desglose`() {
        val raw = """{"data": {"fecha": "2026-11-09", "tipo": "parcial", "turnos": []}}"""
        val today = CollectedToday.of(CashCutJson.dayCut(raw))
        assertEquals("0.00", today.total)
        assertNull(today.breakdownLine { it })
    }
}
