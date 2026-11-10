package com.vaiinilla.app.data.admin

import com.vaiinilla.app.data.tape.ServerTape
import com.vaiinilla.app.data.tape.TapeFloor
import com.vaiinilla.app.domain.admin.AdminCategoryInput
import com.vaiinilla.app.domain.admin.AdminPeriod
import com.vaiinilla.app.domain.admin.AdminProductInput
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PreparationStation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Base64
import java.util.UUID

/** El repositorio real del menú y los reportes del dueño contra lo que el servidor de pruebas contestó de verdad. */
class RemoteAdminRepositoryTapeTest {
    @get:Rule
    val tape = ServerTape()

    private val floor by lazy { TapeFloor(tape) }

    @Test
    fun `una categoria y su producto se crean, se les cambia el precio y la foto, y se archivan`() {
        val admin = floor.admin()
        val suffix = tape.value("sufijo") { UUID.randomUUID().toString().take(6) }
        val category = admin.createCategory(AdminCategoryInput("Postres $suffix", 7), floor.key()).getOrThrow()
        assertEquals("Postres $suffix", category.name)
        assertEquals(7, category.order)
        val renamed =
            admin
                .updateCategory(
                    category.id,
                    AdminCategoryInput("Dulces $suffix", 8),
                    floor.key(),
                ).getOrThrow()
        assertEquals("Dulces $suffix", renamed.name)

        val input =
            AdminProductInput(
                categoryId = category.id,
                preparationStation = PreparationStation.KITCHEN,
                name = "Flan $suffix",
                description = "De la casa",
                ingredients = "Leche, huevo y caramelo",
                allergens = "",
                estimatedTimeMinutes = 4,
                counterPrice = "38.00",
                available = true,
                optionGroups = emptyList(),
                allergenCodes = setOf("lacteos", "huevo"),
            )
        val product = admin.createProduct(input, floor.key()).getOrThrow()
        assertEquals("Flan $suffix", product.name)
        assertEquals(BigDecimal("38.00"), BigDecimal(product.counterPrice))
        assertEquals(setOf("lacteos", "huevo"), product.allergenCodes.toSet())
        assertEquals(PreparationStation.KITCHEN, product.preparationStation)

        val pricier =
            admin
                .updateProduct(
                    product.id,
                    input.copy(counterPrice = "42.50", available = false),
                    floor.key(),
                ).getOrThrow()
        assertEquals(BigDecimal("42.50"), BigDecimal(pricier.counterPrice))
        assertFalse(pricier.available)

        val withPhoto =
            floor
                .catalog(
                    "admin",
                ).uploadProductImage(product.id, PIXEL, "flan.png", "image/png", floor.key())
                .getOrThrow()
        assertTrue(withPhoto.imageUrl.isNotBlank())
        assertTrue(
            admin
                .deleteProductImage(product.id, floor.key())
                .getOrThrow()
                .imageUrl
                .isBlank(),
        )

        admin.setProductArchived(product.id, true, floor.key()).getOrThrow()
        assertTrue(
            floor
                .catalog("admin")
                .getCatalog()
                .getOrThrow()
                .products
                .none { it.id == product.id },
        )
    }

    @Test
    fun `el resumen del dueño cuenta la venta del dia y la lista de pedidos la trae`() {
        floor.open()
        val admin = floor.admin()
        val today = LocalDate.parse(tape.value("hoy") { LocalDate.now().toString() })
        val period = AdminPeriod(today.minusDays(1), today.plusDays(1))
        val before = admin.summary(period).getOrThrow()

        val order = floor.order(floor.product(), 1)
        floor
            .orders(
                "cajero",
            ).collectCash(order.summary.id, order.summary.total, order.summary.version, floor.key(), null)
            .getOrThrow()
        val after = admin.summary(period).getOrThrow()
        assertEquals(BigDecimal(before.sales) + BigDecimal(order.summary.total), BigDecimal(after.sales))
        assertEquals(before.orders + 1, after.orders)
        assertTrue(
            admin.orders(setOf(OrderState.PAID), null).getOrThrow().orders.any {
                it.summary.id ==
                    order.summary.id
            },
        )
        floor.orders("cajero").cancelOrder(order.summary.id, order.summary.version + 1, "Grabación", floor.key())
    }

    private companion object {
        val PIXEL: ByteArray =
            Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==",
            )
    }
}
