package com.vaiinilla.app.ui.screens

import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderItem
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import org.junit.Assert.assertEquals
import org.junit.Test

class KitchenProductTotalsTest {
    private val template = ScreenshotFixtures.sampleOrder(state = OrderState.PAID).items.first()

    private fun item(
        id: Int,
        name: String,
        quantity: Int,
        station: PreparationStation = PreparationStation.KITCHEN,
    ): OrderItem =
        template.copy(
            id = id,
            productName = name,
            quantity = quantity,
            preparationStation = station,
            options = emptyList(),
            rejection = null,
        )

    private fun order(
        folio: Int,
        items: List<OrderItem>,
    ): OrderDetail {
        val base = ScreenshotFixtures.sampleOrder(state = OrderState.PAID)
        return base.copy(summary = base.summary.copy(id = "o$folio", folio = folio), items = items)
    }

    @Test
    fun `suma cada producto de todas las comandas y deja fuera lo de Caja`() {
        val orders =
            listOf(
                order(14, listOf(item(1, "Crepa de cajeta", 2), item(2, "Coca-Cola", 1, PreparationStation.CASHIER))),
                order(16, listOf(item(3, "Crepa de cajeta", 1), item(4, "Latte", 1))),
            )
        val totals = kitchenProductTotals(orders)
        assertEquals(listOf("Crepa de cajeta", "Latte"), totals.map { it.name })
        assertEquals(3, totals.first().quantity)
        assertEquals(listOf(14 to 2, 16 to 1), totals.first().folios)
    }

    @Test
    fun `la comanda separa lo de cocina de lo de Caja`() {
        val items = listOf(item(1, "Crepa", 2), item(2, "Coca-Cola", 2, PreparationStation.CASHIER))
        val (kitchen, cashier) = kitchenSplitItems(items)
        assertEquals(listOf("Crepa"), kitchen.map { it.productName })
        assertEquals(listOf("Coca-Cola"), cashier.map { it.productName })
        val onlyCashier = listOf(item(2, "Coca-Cola", 2, PreparationStation.CASHIER))
        assertEquals(onlyCashier to emptyList<OrderItem>(), kitchenSplitItems(onlyCashier))
    }
}
