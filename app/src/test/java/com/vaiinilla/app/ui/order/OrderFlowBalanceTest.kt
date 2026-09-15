package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderFlowBalanceTest {
    private fun productWithDigitalPrice(price: String): Product =
        Product(
            id = 1,
            categoryId = 1,
            preparationStation = PreparationStation.KITCHEN,
            name = "Producto",
            description = "",
            ingredients = "",
            allergens = "",
            estimatedTimeMinutes = 5,
            counterPrice = price,
            digitalPrice = price,
            available = true,
            imageUrl = "",
            optionGroups = emptyList(),
        )

    private fun stateWithTotal(
        digitalPrice: String,
        quantity: Int = 1,
    ): OrderFlowUiState =
        OrderFlowUiState(
            cartLines =
                listOf(
                    CartLine(
                        product = productWithDigitalPrice(digitalPrice),
                        quantity = quantity,
                        selectedOptionIds = emptySet(),
                    ),
                ),
        )

    @Test
    fun `balance covering the cart total is affordable`() {
        assertTrue(stateWithTotal("18.00").isBalancePaymentAffordable("18.00"))
        assertTrue(stateWithTotal("18.00").isBalancePaymentAffordable("18.01"))
    }

    @Test
    fun `balance a cent below the cart total is not affordable`() {
        // Regresión: la comparación previa truncaba el total a Int, así que
        // un carrito de "18.50" pasaba como cubierto por un saldo de "18.00".
        assertFalse(stateWithTotal("18.00").isBalancePaymentAffordable("17.99"))
        assertFalse(stateWithTotal("9.25", quantity = 2).isBalancePaymentAffordable("18.00"))
    }

    @Test
    fun `unknown or malformed balance stays affordable`() {
        val state = stateWithTotal("18.00")

        assertTrue(state.isBalancePaymentAffordable(null))
        assertTrue(state.isBalancePaymentAffordable(""))
        assertTrue(state.isBalancePaymentAffordable("no-es-numero"))
    }
}
