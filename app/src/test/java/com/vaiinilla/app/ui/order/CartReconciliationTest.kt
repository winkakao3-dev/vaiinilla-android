package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CartReconciliationTest {
    private fun product(
        id: Int,
        price: String = "30.00",
        available: Boolean = true,
        name: String = "Producto $id",
    ) = Product(
        id = id,
        categoryId = 1,
        preparationStation = PreparationStation.KITCHEN,
        name = name,
        description = "",
        ingredients = "",
        allergens = "",
        estimatedTimeMinutes = 5,
        counterPrice = price,
        digitalPrice = price,
        available = available,
        imageUrl = "",
        optionGroups = emptyList(),
    )

    private fun line(product: Product) = CartLine(product = product, quantity = 1, selectedOptionIds = emptySet())

    @Test
    fun `a product that sold out stays in the cart flagged so it can be removed`() {
        val sold = line(product(1))

        val result = reconcileCartLines(listOf(sold), listOf(product(1, available = false)))

        assertEquals(setOf(sold.key), result.unavailableKeys)
        assertEquals(1, result.lines.size)
        assertTrue(
            !result.lines
                .single()
                .product.available,
        )
        assertNull(result.notice)
    }

    @Test
    fun `a price change is applied and announced`() {
        val old = line(product(2, price = "30.00", name = "Café"))

        val result = reconcileCartLines(listOf(old), listOf(product(2, price = "35.00", name = "Café")))

        assertEquals(
            "35.00",
            result.lines
                .single()
                .product.digitalPrice,
        )
        assertEquals("Cambió el precio de Café.", result.notice)
        assertTrue(result.unavailableKeys.isEmpty())
    }

    @Test
    fun `a product missing from the menu is dropped and announced`() {
        val gone = line(product(3, name = "Torta"))
        val kept = line(product(4))

        val result = reconcileCartLines(listOf(gone, kept), listOf(product(4)))

        assertEquals(listOf(kept.key), result.lines.map { it.key })
        assertEquals("Ya no está en el menú: Torta.", result.notice)
    }

    @Test
    fun `unchanged cart reports nothing`() {
        val result = reconcileCartLines(listOf(line(product(5))), listOf(product(5)))

        assertTrue(result.unavailableKeys.isEmpty())
        assertNull(result.notice)
    }

    @Test
    fun `restored cart notice counts the lines the menu no longer has`() {
        assertNull(droppedCartLinesNotice(0))
        assertEquals("Un producto de tu carrito ya no está en el menú y lo quitamos.", droppedCartLinesNotice(1))
        assertEquals("3 productos de tu carrito ya no están en el menú y los quitamos.", droppedCartLinesNotice(3))
    }

    @Test
    fun `flagged keys of removed lines are forgotten`() {
        val kept = line(product(6))
        val removed = line(product(7))

        assertEquals(setOf(kept.key), setOf(kept.key, removed.key).stillIn(listOf(kept)))
    }
}
