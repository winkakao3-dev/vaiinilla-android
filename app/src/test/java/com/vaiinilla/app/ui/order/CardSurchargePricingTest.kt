package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Con la comisión pasada al cliente, solo la tarjeta cobra el precio digital; el resto, el de mostrador. */
class CardSurchargePricingTest {
    private fun product(
        counter: String,
        digital: String,
    ) = Product(
        id = 1,
        categoryId = 1,
        preparationStation = PreparationStation.KITCHEN,
        name = "Producto",
        description = "",
        ingredients = "",
        allergens = "",
        estimatedTimeMinutes = 5,
        counterPrice = counter,
        digitalPrice = digital,
        available = true,
        imageUrl = "",
        optionGroups = emptyList(),
    )

    private fun stateWith(
        counter: String,
        digital: String,
        payment: PaymentMethod = PaymentMethod.CASH,
    ) = OrderFlowUiState(
        cartLines = listOf(CartLine(product(counter, digital), 1, emptySet())),
        checkoutPayment = payment,
    )

    @Test
    fun `cash, balance and pay at end charge the counter price`() {
        val state = stateWith(counter = "100.00", digital = "105.00")
        assertEquals("100.00", state.cartPreviewTotal)
        assertEquals("100.00", Money.cartPreview(state.cartLines, PaymentMethod.BALANCE))
    }

    @Test
    fun `card charges the digital price and the total follows the chosen method`() {
        val state = stateWith(counter = "100.00", digital = "105.00", payment = PaymentMethod.STRIPE)
        assertEquals("105.00", state.cartPreviewTotal)
    }

    @Test
    fun `a balance equal to the counter total covers the cart even when card costs more`() {
        val state = stateWith(counter = "100.00", digital = "105.00")
        assertTrue(state.isBalancePaymentAffordable("100.00"))
        assertFalse(state.isBalancePaymentAffordable("99.99"))
    }

    @Test
    fun `the card fee is the difference, and absent when prices match`() {
        assertEquals("5.00", stateWith(counter = "100.00", digital = "105.00").cardSurchargeLabel)
        assertNull(stateWith(counter = "100.00", digital = "100.00").cardSurchargeLabel)
    }
}
