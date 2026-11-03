package com.vaiinilla.app.ui.screens

import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import org.junit.Assert.assertEquals
import org.junit.Test

class CashierOrderSearchTest {
    private fun order(
        folio: Int,
        name: String?,
        state: OrderState,
        createdAt: String,
    ): OrderDetail {
        val base = ScreenshotFixtures.sampleOrder(state = state)
        return base.copy(
            summary = base.summary.copy(id = "o$folio", folio = folio, createdAt = createdAt),
            customerName = name,
            user = null,
        )
    }

    private val orders =
        listOf(
            order(4, "Diego", OrderState.DELIVERED, "2026-11-02T14:15:00Z"),
            order(11, "Diego Rámirez", OrderState.READY, "2026-11-02T15:38:00Z"),
            order(12, "Ana", OrderState.READY, "2026-11-02T15:40:00Z"),
        )

    @Test
    fun `busca por nombre sin acentos y lo mas nuevo primero`() {
        assertEquals(listOf(11, 4), cashierOrderSearch(orders, "die", CashierSearchFilter.TODAY).map { it.summary.folio })
        assertEquals(listOf(11), cashierOrderSearch(orders, "ramirez", CashierSearchFilter.TODAY).map { it.summary.folio })
    }

    @Test
    fun `busca por folio y respeta el filtro`() {
        assertEquals(listOf(11), cashierOrderSearch(orders, "#11", CashierSearchFilter.TODAY).map { it.summary.folio })
        assertEquals(listOf(4), cashierOrderSearch(orders, "diego", CashierSearchFilter.DELIVERED).map { it.summary.folio })
        assertEquals(listOf(12, 11), cashierOrderSearch(orders, "", CashierSearchFilter.READY).map { it.summary.folio })
    }
}
