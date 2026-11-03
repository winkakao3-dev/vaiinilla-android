package com.vaiinilla.app.ui.screens

import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class CashierStatusTest {
    @Test
    fun `los billetes ofrecidos alcanzan para el total`() {
        assertEquals(listOf(100, 200, 500).map(::BigDecimal), cashBillOptions(BigDecimal("96")))
        assertEquals(listOf(500, 1000).map(::BigDecimal), cashBillOptions(BigDecimal("480")))
        assertEquals(emptyList<BigDecimal>(), cashBillOptions(BigDecimal("1000")))
    }

    @Test
    fun `el cambio dice con que billetes y monedas se da`() {
        assertEquals("1 billete de $20", cashChangePieces(BigDecimal("20.00")))
        assertEquals("2 billetes de $20", cashChangePieces(BigDecimal("40")))
        assertEquals("1 billete de $20 y 1 moneda de $5", cashChangePieces(BigDecimal("25")))
        assertEquals(
            "1 billete de $100, 1 billete de $20, 1 moneda de $10, 1 moneda de $5 y 1 moneda de $2",
            cashChangePieces(BigDecimal("137")),
        )
        assertEquals(null, cashChangePieces(BigDecimal.ZERO))
    }

    @Test
    fun `el estado dice como va y si ya pago`() {
        val pending = ScreenshotFixtures.sampleOrder(state = OrderState.PENDING_PAYMENT).summary
        assertEquals("Sin empezar", cashierOrderStatus(pending).progress)
        assertEquals(false, cashierOrderStatus(pending).paid)
        val ready = ScreenshotFixtures.sampleOrder(state = OrderState.READY).summary
        assertEquals(true, cashierOrderStatus(ready).ready)
    }
}