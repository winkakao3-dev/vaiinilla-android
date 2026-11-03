package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import org.junit.Assert.assertEquals
import org.junit.Test

/** Con la comisión de la tarjeta pasada al cliente, la línea del carrito cuesta lo mismo que suma el total. */
class CartLinePriceTest {
    private val cake =
        Product(
            id = 2291,
            categoryId = 1,
            preparationStation = PreparationStation.KITCHEN,
            name = "Pastel Osito",
            description = "",
            ingredients = "",
            allergens = "",
            estimatedTimeMinutes = 30,
            counterPrice = "420.00",
            digitalPrice = "456.23",
            available = true,
            imageUrl = "",
            optionGroups = emptyList(),
        )
    private val line = CartLine(cake, 1, emptySet())

    private fun state(method: PaymentMethod) = OrderFlowUiState(cartLines = listOf(line), checkoutPayment = method)

    @Test
    fun `paying in cash the line shows the counter price, like the total`() {
        val cash = state(PaymentMethod.CASH)
        assertEquals("420.00", cash.cartLineTotal(line))
        assertEquals(cash.checkoutTotal, cash.cartLineTotal(line))
    }

    @Test
    fun `paying by card the line shows the card price, like the total`() {
        val card = state(PaymentMethod.STRIPE)
        assertEquals("456.23", card.cartLineTotal(line))
        assertEquals(card.checkoutTotal, card.cartLineTotal(line))
    }

    @Test
    fun `the counter line multiplies by quantity`() {
        assertEquals("840.00", Money.counterLinePreview(line.copy(quantity = 2)))
    }
}
