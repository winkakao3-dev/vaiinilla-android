package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.Category
import com.vaiinilla.app.domain.model.OptionGroup
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.model.ProductOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuFeedTest {
    private fun product(
        id: Int,
        categoryId: Int = 1,
        available: Boolean = true,
        groups: List<OptionGroup> = emptyList(),
    ) = Product(
        id = id,
        categoryId = categoryId,
        preparationStation = PreparationStation.KITCHEN,
        name = "Platillo $id",
        description = "",
        ingredients = "",
        allergens = "",
        estimatedTimeMinutes = 10,
        counterPrice = "50.00",
        digitalPrice = "50.00",
        available = available,
        imageUrl = "",
        optionGroups = groups,
    )

    private val size =
        OptionGroup(
            id = 9,
            name = "Tamaño",
            minimumSelections = 1,
            maximumSelections = 1,
            options = listOf(ProductOption(id = 1, name = "Chico", extraPrice = "0.00")),
        )

    @Test
    fun `el feed sigue el orden de las categorías y deja lo agotado al final de la suya`() {
        val catalog =
            Catalog(
                categories = listOf(Category(1, "Postres", order = 2), Category(2, "Antojitos", order = 1)),
                products =
                    listOf(
                        product(10, 1),
                        product(11, 2),
                        product(12, 1, available = false),
                        product(13, 99),
                        product(14, 2),
                    ),
                cursor = null,
            )
        assertEquals(listOf(11, 14, 10, 12, 13), MenuFeed.products(catalog).map { it.id })
        // La cuadrícula tampoco lo esconde (B10): sale al final, como Agotado.
        assertEquals(listOf(10, 11, 13, 14, 12), OrderFlowUiState(catalog = catalog).filteredProducts.map { it.id })
    }

    @Test
    fun `solo un grupo obligatorio pide abrir la ficha`() {
        assertFalse(MenuFeed.needsChoice(product(1)))
        assertFalse(MenuFeed.needsChoice(product(1, groups = listOf(size.copy(minimumSelections = 0)))))
        assertTrue(MenuFeed.needsChoice(product(1, groups = listOf(size))))
    }

    @Test
    fun `el más suma a la misma línea y respeta el máximo`() {
        val p = product(1)
        var lines = emptyList<CartLine>()
        repeat(3) { lines = (MenuFeed.addOne(lines, p) as MenuFeed.Change.Updated).lines }
        assertEquals(1, lines.size)
        assertEquals(3, MenuFeed.quantityOf(lines, 1))

        val full = listOf(CartLine(p, MenuFeed.MAX_LINE_QUANTITY, emptySet()))
        assertTrue(MenuFeed.addOne(full, p) is MenuFeed.Change.Rejected)
    }

    @Test
    fun `una línea con nota no se mezcla con la del más`() {
        val p = product(1)
        val withNote = listOf(CartLine(p, 2, emptySet(), notes = "sin cebolla"))
        val lines = (MenuFeed.addOne(withNote, p) as MenuFeed.Change.Updated).lines
        assertEquals(2, lines.size)
        assertEquals(3, MenuFeed.quantityOf(lines, 1))
    }

    @Test
    fun `el menos baja la línea más reciente y la quita en cero`() {
        val p = product(1)
        val lines = listOf(CartLine(p, 2, emptySet(), notes = "sin cebolla"), CartLine(p, 1, emptySet()))
        val once = MenuFeed.removeOne(lines, 1)
        assertEquals(listOf(2), once.map { it.quantity })
        assertEquals("sin cebolla", once.single().notes)
        assertEquals(lines, MenuFeed.removeOne(lines, 404))
    }

    @Test
    fun `deslizar de lado cambia a la categoría de al lado`() {
        val categories = listOf(2, 1, 99)
        assertEquals(1, MenuFeed.neighborCategory(categories, 2, 1))
        assertEquals(null, MenuFeed.neighborCategory(categories, 2, -1))
        assertEquals(null, MenuFeed.neighborCategory(categories, 99, 1))
        assertEquals(null, MenuFeed.neighborCategory(categories, 404, 1))

        // A la izquierda la siguiente, a la derecha la anterior, si pasa el umbral.
        assertEquals(1, MenuFeed.categoryStep(dx = -120f, dy = 10f, projectedDx = -200f, threshold = 90f))
        assertEquals(-1, MenuFeed.categoryStep(dx = 120f, dy = 10f, projectedDx = 200f, threshold = 90f))
        assertEquals(0, MenuFeed.categoryStep(dx = -40f, dy = 5f, projectedDx = -60f, threshold = 90f))
        // Diagonal: no cuenta como de lado.
        assertEquals(0, MenuFeed.categoryStep(dx = -120f, dy = 100f, projectedDx = -300f, threshold = 90f))
    }

    @Test
    fun `la página sigue al dedo y rebota en las orillas`() {
        assertEquals(-40f, MenuFeed.followOffset(-100f, hasNeighbor = true, density = 2f), 0.001f)
        // Sin categoría de ese lado: raíz del desplazamiento en dp por 4 (100 px a 2x son 50 dp → √50·4·2).
        assertEquals(-56.5685f, MenuFeed.followOffset(-100f, hasNeighbor = false, density = 2f), 0.001f)
    }
}
