package com.vaiinilla.app.ui.screens

import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import org.junit.Assert.assertEquals
import org.junit.Test

class KitchenSoldOutTest {
    private fun order(
        folio: Int,
        names: List<String>,
    ): OrderDetail {
        val base = ScreenshotFixtures.sampleOrder(state = OrderState.PAID)
        val template = base.items.first()
        return base.copy(
            summary = base.summary.copy(id = "o$folio", folio = folio),
            items = names.mapIndexed { i, name -> template.copy(id = folio * 10 + i, productName = name, rejection = null) },
        )
    }

    @Test
    fun `cuenta las comandas en fila que llevan lo que se acabo`() {
        val queue = listOf(order(14, listOf("Latte frío", "Crepa")), order(15, listOf("latte frío")), order(16, listOf("Crepa")))
        assertEquals(2, kitchenOrdersWithProduct(queue, "Latte frío"))
        assertEquals(0, kitchenOrdersWithProduct(queue, "Pastel"))
    }

    @Test
    fun `lo de cocina va primero y lo archivado no sale`() {
        val products = ScreenshotFixtures.catalog().products
        val template = products.first()
        val list =
            listOf(
                template.copy(id = 1, name = "Coca-Cola", preparationStation = PreparationStation.CASHIER),
                template.copy(id = 2, name = "Crepa", preparationStation = PreparationStation.KITCHEN),
                template.copy(id = 3, name = "Archivada", preparationStation = PreparationStation.KITCHEN, archived = true),
                template.copy(id = 4, name = "Bagel", preparationStation = PreparationStation.KITCHEN, available = false),
            )
        assertEquals(listOf("Bagel", "Crepa", "Coca-Cola"), kitchenSoldOutProducts(list).map { it.name })
    }
}
