package com.vaiinilla.app

import com.vaiinilla.app.data.inventory.InventoryProduct
import com.vaiinilla.app.data.inventory.decodeSupplyProducts
import com.vaiinilla.app.data.inventory.encodeSupplyProducts
import com.vaiinilla.app.domain.model.Category
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.ui.screens.admin.matchingMenuProducts
import com.vaiinilla.app.ui.screens.admin.usesPicker
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** «Lo usan» de un insumo: contrato 5 y el selector de productos y categorías del menú. */
class SupplyProductsTest {
    @Test
    fun `lee los productos que usan el insumo`() {
        val links =
            decodeSupplyProducts(
                """
                {"data":[{"producto_id":3,"nombre":"Michelada","categoria_id":2,"categoria_nombre":"Bebidas",
                  "cantidad":0.25,"extra":true}]}
                """.trimIndent(),
            )
        val link = links.single()
        assertEquals(3, link.productId)
        assertEquals("Michelada", link.name)
        assertEquals(2, link.categoryId)
        assertEquals("Bebidas", link.categoryName)
        assertEquals(0.25, link.quantity, 0.0)
    }

    @Test
    fun `manda productos y categorias completas con su cantidad`() {
        val body = Json.parseToJsonElement(encodeSupplyProducts(mapOf(3 to 1.5), mapOf(2 to 1.0))).jsonObject
        val product = body["productos"]!!.jsonArray.single().jsonObject
        assertEquals(3, product["producto_id"]!!.jsonPrimitive.int)
        assertEquals(1.5, product["cantidad"]!!.jsonPrimitive.double, 0.0)
        val category = body["categorias"]!!.jsonArray.single().jsonObject
        assertEquals(2, category["categoria_id"]!!.jsonPrimitive.int)
        assertEquals(1.0, category["cantidad"]!!.jsonPrimitive.double, 0.0)
    }

    @Test
    fun `quitarlo de todos manda listas vacias`() {
        val body = Json.parseToJsonElement(encodeSupplyProducts(emptyMap(), emptyMap())).jsonObject
        assertEquals(0, body["productos"]!!.jsonArray.size)
        assertEquals(0, body["categorias"]!!.jsonArray.size)
    }

    @Test
    fun `con el catalogo agrupa por categoria del menu con su id y su orden`() {
        val products = listOf(inventory(1, "Corona", "Cervezas"), inventory(2, "Nachos", "Botanas"))
        val catalog = listOf(catalog(1, categoryId = 10), catalog(2, categoryId = 20))
        val categories = listOf(Category(10, "Cervezas", order = 2), Category(20, "Botanas", order = 1))

        val picker = usesPicker(products, catalog, categories)

        assertEquals(listOf(20, 10), picker.map { it.id })
        assertEquals(listOf(1 to "Corona"), picker[1].products)
    }

    @Test
    fun `sin catalogo agrupa por nombre y no ofrece id de categoria`() {
        val picker = usesPicker(listOf(inventory(1, "Corona", "Cervezas")), null, null)
        assertNull(picker.single().id)
        assertEquals("Cervezas", picker.single().name)
    }

    @Test
    fun `busca productos sin importar acentos y primero los que empiezan igual`() {
        val products =
            listOf(
                inventory(1, "Agua de papá", "Bebidas"),
                inventory(2, "Papas fritas", "Botanas"),
                inventory(3, "Nachos", "Botanas"),
            )
        assertEquals(listOf(2, 1), matchingMenuProducts("PAPA", products).map { it.id })
    }

    @Test
    fun `con menos de dos letras o sin coincidencias no sugiere productos`() {
        val products = listOf(inventory(1, "Papas", "Botanas"))
        assertEquals(emptyList<InventoryProduct>(), matchingMenuProducts("p", products))
        assertEquals(emptyList<InventoryProduct>(), matchingMenuProducts("cerveza", products))
        assertEquals(emptyList<InventoryProduct>(), matchingMenuProducts("papas", null))
    }

    @Test
    fun `sugiere como maximo cinco productos`() {
        val products = (1..8).map { inventory(it, "Papas $it", "Botanas") }
        assertEquals(5, matchingMenuProducts("pa", products).size)
    }
    private fun inventory(
        id: Int,
        name: String,
        category: String,
    ) = InventoryProduct(id, name, category, 50.0, true, false, false, null, 0.0, null)

    private fun catalog(
        id: Int,
        categoryId: Int,
    ) = Product(
        id = id,
        categoryId = categoryId,
        preparationStation = PreparationStation.KITCHEN,
        name = "p$id",
        description = "",
        ingredients = "",
        allergens = "",
        estimatedTimeMinutes = 5,
        counterPrice = "50.00",
        digitalPrice = "50.00",
        available = true,
        imageUrl = "",
        optionGroups = emptyList(),
    )
}
