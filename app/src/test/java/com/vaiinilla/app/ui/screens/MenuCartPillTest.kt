package com.vaiinilla.app.ui.screens

import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.ui.order.OrderFlowUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** En vivo, #20: con recibe_pedidos apagado salía el aviso y también la píldora «Ver carrito». */
class MenuCartPillTest {
    private val cake =
        Product(
            id = 1,
            categoryId = 1,
            preparationStation = PreparationStation.KITCHEN,
            name = "Pastel",
            description = "",
            ingredients = "",
            allergens = "",
            estimatedTimeMinutes = 30,
            counterPrice = "100.00",
            digitalPrice = "100.00",
            available = true,
            imageUrl = "",
            optionGroups = emptyList(),
        )

    private fun status(accepting: Boolean) =
        OperationalStatus(
            acceptingOrders = accepting,
            cashSessionOpen = true,
            cashierOnline = true,
            kitchenOnline = true,
            estimatedTimeMinutes = 5,
            consultedAt = "2026-11-05T12:00:00Z",
        )

    private fun state(status: OperationalStatus?) =
        OrderFlowUiState(cartLines = listOf(CartLine(cake, 1, emptySet())), operationalStatus = status)

    @Test
    fun `the pill hides while the store does not take orders`() {
        assertFalse(menuCartPillVisible(state(status(accepting = false))))
    }

    @Test
    fun `the pill shows when the store takes orders or its status is still unknown`() {
        assertTrue(menuCartPillVisible(state(status(accepting = true))))
        assertTrue(menuCartPillVisible(state(null)))
    }

    @Test
    fun `no pill with an empty cart`() {
        assertFalse(menuCartPillVisible(OrderFlowUiState(operationalStatus = status(accepting = true))))
    }
}
