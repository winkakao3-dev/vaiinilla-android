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
    fun `el estado dice como va y si ya pago`() {
        val pending = ScreenshotFixtures.sampleOrder(state = OrderState.PENDING_PAYMENT).summary
        assertEquals("Sin empezar", cashierOrderStatus(pending).progress)
        assertEquals(false, cashierOrderStatus(pending).paid)
        val ready = ScreenshotFixtures.sampleOrder(state = OrderState.READY).summary
        assertEquals(true, cashierOrderStatus(ready).ready)
    }
}