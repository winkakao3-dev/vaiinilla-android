package com.vaiinilla.app.ui.order

import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.usecase.BuildCreateOrderRequestUseCase
import com.vaiinilla.app.domain.usecase.combineKitchenNotes
import com.vaiinilla.app.domain.usecase.generalKitchenNotes
import com.vaiinilla.app.domain.usecase.kitchenLineNotesSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CartLineNotesTest {
    private val burrito = product(id = 103, name = "Burrito")
    private val agua = product(id = 201, name = "Agua")

    @Test
    fun `same product options and note consolidate into one line`() {
        val a = CartLine(burrito, quantity = 1, selectedOptionIds = setOf(310), notes = "sin cebolla")
        val b = CartLine(burrito, quantity = 2, selectedOptionIds = setOf(310), notes = "sin cebolla")

        assertEquals(a.key, b.key)
    }

    @Test
    fun `a line without note keeps the key it had before notes existed`() {
        assertEquals("103:310", CartLine(burrito, quantity = 1, selectedOptionIds = setOf(310)).key)
    }

    @Test
    fun `same product with a different note stays as a separate line`() {
        val withNote = CartLine(burrito, quantity = 1, selectedOptionIds = emptySet(), notes = "sin cebolla")
        val plain = CartLine(burrito, quantity = 1, selectedOptionIds = emptySet())

        assertNotEquals(withNote.key, plain.key)
    }

    @Test
    fun `a note containing key separators does not collide with another line`() {
        val tricky = CartLine(burrito, quantity = 1, selectedOptionIds = setOf(310), notes = "310::x")
        val other = CartLine(burrito, quantity = 1, selectedOptionIds = setOf(310), notes = "x")

        assertNotEquals(tricky.key, other.key)
    }

    @Test
    fun `use case carries each line note and combines it with the general note`() {
        val request =
            BuildCreateOrderRequestUseCase().invoke(
                lines =
                    listOf(
                        CartLine(burrito, quantity = 1, selectedOptionIds = emptySet(), notes = "sin cebolla"),
                        CartLine(agua, quantity = 2, selectedOptionIds = emptySet()),
                    ),
                kitchenNotes = "Para la mesa 4",
            )

        assertEquals("Para la mesa 4 | Burrito: sin cebolla", request.kitchenNotes)
        assertEquals(listOf("sin cebolla", ""), request.items.map { it.notes })
    }

    @Test
    fun `order without line notes sends the general note untouched`() {
        val request =
            BuildCreateOrderRequestUseCase().invoke(
                lines = listOf(CartLine(burrito, quantity = 1, selectedOptionIds = emptySet())),
                kitchenNotes = "  Salsa aparte  ",
                paymentMethod = PaymentMethod.CASH,
                destination = OrderDestination.TAKE_AWAY,
            )

        assertEquals("  Salsa aparte  ", request.kitchenNotes)
        assertEquals(listOf(""), request.items.map { it.notes })
    }

    @Test
    fun `line notes are summarised for the kitchen with dot separators`() {
        val summary =
            kitchenLineNotesSummary(
                listOf("Burrito" to "sin cebolla", "Agua" to "", "Torta" to "sin jitomate"),
            )

        assertEquals("Burrito: sin cebolla · Torta: sin jitomate", summary)
    }

    @Test
    fun `payload sends notas only on lines that have one`() {
        val request =
            BuildCreateOrderRequestUseCase().invoke(
                lines =
                    listOf(
                        CartLine(burrito, quantity = 1, selectedOptionIds = emptySet(), notes = "sin cebolla"),
                        CartLine(agua, quantity = 1, selectedOptionIds = emptySet()),
                    ),
                kitchenNotes = "",
            )

        val json = OrderContractJson().encodeCreateRequest(request)

        assertEquals(1, Regex("\"notas\":").findAll(json).count())
        assertTrue(json.contains("\"notas\":\"sin cebolla\""))
        assertTrue(json.contains("\"notas_cocina\":\"Burrito: sin cebolla\""))
    }

    @Test
    fun `payload without any notes has no notas key at all`() {
        val request =
            BuildCreateOrderRequestUseCase().invoke(
                lines = listOf(CartLine(agua, quantity = 1, selectedOptionIds = emptySet())),
                kitchenNotes = "",
            )

        val json = OrderContractJson().encodeCreateRequest(request)

        assertFalse(json.contains("\"notas\""))
    }

    @Test
    fun `general note is recovered when the line summary is stripped`() {
        val summary = kitchenLineNotesSummary(listOf("Burrito" to "sin cebolla"))
        val combined = combineKitchenNotes("Para la mesa 4", summary)

        assertEquals("Para la mesa 4", generalKitchenNotes(combined, summary))
    }

    @Test
    fun `summary-only notes restore to an empty general note`() {
        val summary = kitchenLineNotesSummary(listOf("Burrito" to "sin cebolla"))

        assertEquals("", generalKitchenNotes(combineKitchenNotes("", summary), summary))
    }

    @Test
    fun `notes without a matching summary are kept as they are`() {
        assertEquals("Solo general", generalKitchenNotes("Solo general", ""))
        assertEquals("Otra cosa", generalKitchenNotes("Otra cosa", "Burrito: sin cebolla"))
    }

    private fun product(
        id: Int,
        name: String,
    ) = Product(
        id = id,
        categoryId = 1,
        preparationStation = PreparationStation.KITCHEN,
        name = name,
        description = "",
        ingredients = "",
        allergens = "",
        estimatedTimeMinutes = 5,
        counterPrice = "10.00",
        digitalPrice = "10.00",
        available = true,
        imageUrl = "",
        optionGroups = emptyList(),
    )
}
