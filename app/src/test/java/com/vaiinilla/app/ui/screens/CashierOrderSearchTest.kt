package com.vaiinilla.app.ui.screens

import com.vaiinilla.app.domain.model.DeliveryAddress
import com.vaiinilla.app.domain.model.OfflineCapture
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderHomeDelivery
import com.vaiinilla.app.domain.model.OrderSpace
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderUser
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

    @Test
    fun `N07 busca por los primeros digitos del folio`() {
        assertEquals(listOf(12, 11), cashierOrderSearch(orders, "1", CashierSearchFilter.TODAY).map { it.summary.folio })
    }

    @Test
    fun `N07 busca por telefono del domicilio, matricula, lugar y folio provisional`() {
        val base = ScreenshotFixtures.sampleOrder(state = OrderState.READY)
        val phone = DeliveryAddress(address = "Calle 1", phone = "614 123 4567")
        val home =
            order(20, "Lupita", OrderState.READY, "2026-11-08T18:00:00Z").copy(homeDelivery = OrderHomeDelivery(phone))
        val student =
            order(21, null, OrderState.READY, "2026-11-08T18:01:00Z").copy(
                user = OrderUser(name = "Sofía", enrollment = "A0042"),
            )
        val lane =
            order(22, null, OrderState.READY, "2026-11-08T18:02:00Z").copy(
                summary =
                    base.summary.copy(
                        id = "o22",
                        folio = 22,
                        createdAt = "2026-11-08T18:02:00Z",
                        space = OrderSpace(4, "Pista 4", "pista"),
                    ),
            )
        val offline =
            order(23, null, OrderState.READY, "2026-11-08T18:03:00Z").copy(
                offlineCapture = OfflineCapture(capturedAt = null, localFolio = "M7-014"),
            )
        val all = listOf(home, student, lane, offline)
        val folios = { query: String ->
            cashierOrderSearch(all, query, CashierSearchFilter.TODAY).map { it.summary.folio }
        }
        assertEquals(listOf(20), folios("6141234567"))
        assertEquals(listOf(20), folios("4567"))
        assertEquals(listOf(21), folios("a0042"))
        assertEquals(listOf(21), folios("sofia"))
        assertEquals(listOf(22), folios("pista 4"))
        assertEquals(listOf(23), folios("m7-014"))
    }}
