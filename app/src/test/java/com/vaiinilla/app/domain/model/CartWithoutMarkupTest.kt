package com.vaiinilla.app.domain.model

import com.vaiinilla.app.TestFixtureSource
import com.vaiinilla.app.data.contract.ContractResponseParser
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regresión: sin pasar la comisión, el menú manda el precio de mostrador y el carrito suma
 * exactamente eso (más los extras). La app no aplica ningún recargo propio.
 */
class CartWithoutMarkupTest {
    private val burrito =
        ContractResponseParser()
            .parseCatalog(TestFixtureSource().read("fixtures/catalog.json"))
            .products
            .first { it.id == 103 }

    private fun exact(price: String) = burrito.copy(digitalPrice = price, optionGroups = emptyList())

    @Test
    fun `a product at counter price does not go up when added to the cart`() {
        val product = exact("120.00")
        assertEquals("120.00", Money.productUnitPreview(product, emptySet()))
        assertEquals("120.00", Money.cartLinePreview(CartLine(product, 1, emptySet())))
    }

    @Test
    fun `the cart total is the exact sum of the lines`() {
        val lines =
            listOf(
                CartLine(exact("120.00"), 2, emptySet()),
                CartLine(exact("25.00"), 1, emptySet()),
            )
        assertEquals("265.00", Money.cartPreview(lines))
    }
}
