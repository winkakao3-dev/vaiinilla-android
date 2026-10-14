package com.vaiinilla.app.ui.screens

import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.OptionGroup
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.model.ProductOption
import org.junit.Assert.assertEquals
import org.junit.Test

class CounterOrderSheetTest {
    private val radio = group(id = 1, minimum = 1, maximum = 1)
    private val opcional = group(id = 2, minimum = 0, maximum = 1)
    private val multiple = group(id = 3, minimum = 0, maximum = 2)

    @Test
    fun `single selection group replaces the previous option`() {
        assertEquals(setOf(12), alternarOpcionMostrador(setOf(11), radio, 12))
    }

    @Test
    fun `required option cannot be unselected`() {
        assertEquals(setOf(11), alternarOpcionMostrador(setOf(11), radio, 11))
    }

    @Test
    fun `optional option can be unselected`() {
        assertEquals(emptySet<Int>(), alternarOpcionMostrador(setOf(21), opcional, 21))
    }

    @Test
    fun `multiple selection group stops at the maximum`() {
        assertEquals(setOf(31, 32), alternarOpcionMostrador(setOf(31, 32), multiple, 33))
        assertEquals(setOf(31, 33), alternarOpcionMostrador(setOf(31), multiple, 33))
    }

    @Test
    fun `initial selection takes only the required minimum of each group`() {
        val producto = product(listOf(radio, multiple))

        assertEquals(setOf(11), opcionesIniciales(producto))
    }

    @Test
    fun `la barra cuenta articulos y suma precio de mostrador mas extras`() {
        val conExtra =
            OptionGroup(
                id = 9,
                name = "Leche",
                minimumSelections = 0,
                maximumSelections = 1,
                options = listOf(ProductOption(id = 91, name = "Avena", extraPrice = "7.50")),
            )
        val burrito = product(listOf(conExtra))
        val lineas =
            listOf(
                CartLine(burrito, 2, setOf(91)),
                CartLine(
                    burrito.copy(id = 101, name = "Agua", counterPrice = "15.00", optionGroups = emptyList()),
                    1,
                    emptySet(),
                ),
            )
        assertEquals("70.00", Money.counterPreview(lineas))
        assertEquals("3 artículos · $70 · Crear pedido", counterOrderBarLabel(lineas, "Crear pedido"))
        assertEquals("1 artículo · $15 · Enviar a Caja", counterOrderBarLabel(lineas.drop(1), "Enviar a Caja"))
    }

    @Test
    fun `el buscador ignora acentos y mayusculas`() {
        val menu =
            listOf(
                product(emptyList()).copy(id = 1, name = "Café americano"),
                product(emptyList()).copy(id = 2, name = "Limonada rosa"),
            )
        assertEquals(listOf(1), counterOrderMatches(menu, "cafe").map { it.id })
        assertEquals(listOf(2), counterOrderMatches(menu, "  LIMO ").map { it.id })
        assertEquals(listOf(1, 2), counterOrderMatches(menu, "").map { it.id })
    }

    private fun group(
        id: Int,
        minimum: Int,
        maximum: Int,
    ) = OptionGroup(
        id = id,
        name = "grupo $id",
        minimumSelections = minimum,
        maximumSelections = maximum,
        options =
            listOf(
                ProductOption(id = id * 10 + 1, name = "a", extraPrice = "0.00"),
                ProductOption(id = id * 10 + 2, name = "b", extraPrice = "0.00"),
                ProductOption(id = id * 10 + 3, name = "c", extraPrice = "0.00"),
            ),
    )

    private fun product(groups: List<OptionGroup>) =
        Product(
            id = 100,
            categoryId = 1,
            preparationStation = PreparationStation.KITCHEN,
            name = "Burrito",
            description = "",
            ingredients = "",
            allergens = "",
            estimatedTimeMinutes = 5,
            counterPrice = "20.00",
            digitalPrice = "20.00",
            available = true,
            imageUrl = "",
            optionGroups = groups,
        )
}
