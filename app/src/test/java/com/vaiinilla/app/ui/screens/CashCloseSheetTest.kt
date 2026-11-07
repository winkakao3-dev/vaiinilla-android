package com.vaiinilla.app.ui.screens

import com.vaiinilla.app.domain.repository.CashDrawerMovement
import com.vaiinilla.app.domain.repository.CashDrawerSummary
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

    @Test
    fun `el aviso de cobrado sin internet dice cuantos faltan`() {
        assertEquals("Hay 1 pedido cobrado sin internet sin registrar", unsyncedCashTitle(1))
        assertEquals("Hay 3 pedidos cobrados sin internet sin registrar", unsyncedCashTitle(3))
    }

    @Test
    fun `el cierre desglosa como el corte y sin resumen dice el neto con su signo`() {
        val summary =
            CashDrawerSummary(
                opening = "500.00",
                cashSales = "320.00",
                tips = "0.00",
                reloads = "0.00",
                cashIn = "0.00",
                cashOut = "0.00",
                refunds = "620.00",
                expected = "200.00",
            )
        assertEquals(
            listOf("Fondo al abrir", "Cobros en efectivo", "Devoluciones en efectivo"),
            closeBreakdown(summary, "500.00", "200.00").map { it.first },
        )
        assertEquals("−$620.00", closeBreakdown(summary, "500.00", "200.00").last().second)
        val neto = closeBreakdown(null, "500.00", "380.00")
        assertEquals("Movimientos del turno (neto)" to "−$120.00", neto.last())
        assertEquals(emptyList<Pair<String, String>>(), closeBreakdown(null, null, "380.00"))
    }

    @Test
    fun `los renglones del cajon usan las palabras del corte`() {
        fun row(
            type: String,
            refund: Boolean = false,
        ) = CashDrawerMovement(
            id = "x",
            type = type,
            outgoing = type == "ajuste_salida",
            amount = "1.00",
            reason = null,
            registeredBy = null,
            orderFolio = 7,
            isRefund = refund,
            createdAt = "2026-11-05T15:00:00Z",
        )
        assertEquals("Salida del cajón", drawerMovementTitle(row("ajuste_salida")))
        assertEquals("Devolución en efectivo #7", drawerMovementTitle(row("ajuste_salida", refund = true)))
        assertEquals("Entrada al cajón", drawerMovementTitle(row("ajuste_entrada")))
        assertEquals("Cobro en efectivo #7", drawerMovementTitle(row("cobro")))
    }
}
