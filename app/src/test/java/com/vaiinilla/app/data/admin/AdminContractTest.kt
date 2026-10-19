package com.vaiinilla.app.data.admin

import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.data.contract.ContractResponseParser
import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.domain.admin.AdminCategoryInput
import com.vaiinilla.app.domain.admin.AdminOptionGroupInput
import com.vaiinilla.app.domain.admin.AdminOptionInput
import com.vaiinilla.app.domain.admin.AdminPeriod
import com.vaiinilla.app.domain.admin.AdminProductInput
import com.vaiinilla.app.domain.admin.AdminProductRules
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PreparationStation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AdminContractTest {
    private val json = AdminContractJson()

    private val product =
        AdminProductInput(
            categoryId = 3,
            preparationStation = PreparationStation.CASHIER,
            name = " Latte ",
            description = "",
            ingredients = "Leche y espresso",
            allergens = "",
            estimatedTimeMinutes = 4,
            counterPrice = "55",
            available = true,
            optionGroups =
                listOf(
                    AdminOptionGroupInput(
                        id = 9,
                        name = "Tamaño",
                        minimumSelections = 1,
                        maximumSelections = 1,
                        options = listOf(AdminOptionInput(21, "Chico", "0"), AdminOptionInput(null, "Grande", "12.5")),
                    ),
                ),
        )

    @Test
    fun `money pads to two decimals and rejects malformed amounts`() {
        assertEquals("55.00", AdminProductRules.money("55"))
        assertEquals("12.50", AdminProductRules.money("12.5"))
        assertEquals("1200.00", AdminProductRules.money("$1,200"))
        assertNull(AdminProductRules.money("12.345"))
        assertNull(AdminProductRules.money("abc"))
        assertNull(AdminProductRules.money("123456789"))
    }

    @Test
    fun `rules accept a valid product and flag what the backend would reject`() {
        assertNull(AdminProductRules.problem(product))
        assertNotNull(AdminProductRules.problem(product.copy(counterPrice = "0")))
        assertNotNull(AdminProductRules.problem(product.copy(estimatedTimeMinutes = 241)))
        val group = product.optionGroups.first()
        assertNotNull(AdminProductRules.problem(product.copy(optionGroups = listOf(group.copy(maximumSelections = 3)))))
        assertNotNull(AdminProductRules.problem(product.copy(optionGroups = listOf(group.copy(minimumSelections = 2)))))
        assertNotNull(
            AdminProductRules.problem(
                product.copy(optionGroups = listOf(group, group.copy(id = null, name = "tamaño "))),
            ),
        )
        val repeated =
            group.copy(
                options = listOf(AdminOptionInput(null, "Chico", "0"), AdminOptionInput(null, "CHICO", "0")),
            )
        assertNotNull(AdminProductRules.problem(product.copy(optionGroups = listOf(repeated))))
    }

    @Test
    fun `product body uses the admin wire contract`() {
        val body = json.encodeProduct(AdminProductRules.normalized(product))
        assertTrue(body.contains("\"categoria_id\":3"))
        assertTrue(body.contains("\"estacion_preparacion\":\"caja\""))
        assertTrue(body.contains("\"nombre\":\"Latte\""))
        // El backend exige los textos opcionales aunque vayan en null, y rechaza un `id: null` en grupos u opciones.
        assertTrue(body.contains("\"descripcion\":null"))
        assertTrue(!body.contains("\"id\":null"))
        assertTrue(body.contains("\"precio_mostrador\":\"55.00\""))
        assertTrue(body.contains("\"tiempo_estimado_min\":4"))
        assertTrue(body.contains("\"min_selecciones\":1"))
        assertTrue(body.contains("{\"id\":21,\"nombre\":\"Chico\",\"precio_extra\":\"0.00\"}"))
        assertTrue(body.contains("{\"nombre\":\"Grande\",\"precio_extra\":\"12.50\"}"))
    }

    @Test
    fun `summary maps totals, payment labels and order states`() {
        val raw =
            """
            {"data":{"periodo":{"desde":"2026-10-10","hasta":"2026-10-16"},
              "resumen":{"ventas_totales":"1530.00","pedidos":12,"ticket_promedio":"127.50","productos_vendidos":30,
                "propinas":"40.00","recargas":"200.00","pedidos_cancelados":1,"comisiones":"3.00"},
              "ventas_por_dia":[{"fecha":"2026-10-15","ventas":"500.00","pedidos":4}],
              "metodos_pago":[{"metodo":"stripe","ventas":"900.00","pedidos":7}],
              "pedidos_por_estado":[{"estado":"no_recogido","pedidos":1}],
              "productos":[{"producto_id":4,"nombre":"Latte","cantidad":9}],
              "wallet":{}},
             "error":null,"meta":{}}
            """.trimIndent()
        val period = AdminPeriod.lastDays(7, LocalDate.parse("2026-10-16"))
        val summary = json.parseSummary(raw, period)
        assertEquals(LocalDate.parse("2026-10-10"), period.from)
        assertEquals("1530.00", summary.sales)
        assertEquals(30, summary.productsSold)
        assertEquals("Tarjeta en la app", summary.paymentMethods.single().label)
        assertEquals("No recogido", summary.ordersByStatus.single().label)
        assertEquals("Latte", summary.topProducts.single().label)
        assertEquals(1, summary.canceledOrders)
    }

    @Test
    fun `repository sends history filters and reads the next cursor`() {
        val client = RecordingClient("""{"data":[],"error":null,"meta":{"cursor":"abc"}}""")
        val repository = RemoteAdminRepository(client, ContractResponseParser(), OrderContractJson())
        val page = repository.orders(setOf(OrderState.DELIVERED), cursor = "prev").getOrThrow()
        assertEquals("pedidos", client.path)
        assertEquals(mapOf("estado" to "entregado", "cursor" to "prev", "limit" to "30"), client.query)
        assertEquals("abc", page.cursor)
        assertTrue(page.orders.isEmpty())
    }

    @Test
    fun `category edits are patched with an idempotency key`() {
        val client = RecordingClient("""{"data":{"id":5,"nombre":"Bebidas","orden":2},"error":null,"meta":{}}""")
        val repository = RemoteAdminRepository(client, ContractResponseParser(), OrderContractJson())
        val category = repository.updateCategory(5, AdminCategoryInput(" Bebidas ", 2), "idem-cat").getOrThrow()
        assertEquals("PATCH catalogo/categorias/5", "${client.method} ${client.path}")
        assertEquals("{\"nombre\":\"Bebidas\",\"orden\":2}", client.body)
        assertEquals("idem-cat", client.headers["Idempotency-Key"])
        assertEquals(5, category.id)
    }

    private class RecordingClient(
        private val response: String,
    ) : VaiinillaApiClient {
        override val baseUrl: String = "https://example.invalid/api/v1/"
        var method: String? = null
        var path: String? = null
        var query: Map<String, String> = emptyMap()
        var body: String = ""
        var headers: Map<String, String> = emptyMap()

        override fun get(
            path: String,
            query: Map<String, String>,
        ): Result<String> {
            record("GET", path, "", emptyMap())
            this.query = query
            return Result.success(response)
        }

        override fun post(
            path: String,
            body: String,
            headers: Map<String, String>,
        ): Result<String> = record("POST", path, body, headers)

        override fun patch(
            path: String,
            body: String,
            headers: Map<String, String>,
        ): Result<String> = record("PATCH", path, body, headers)

        override fun put(
            path: String,
            body: String,
            headers: Map<String, String>,
        ): Result<String> = record("PUT", path, body, headers)

        private fun record(
            method: String,
            path: String,
            body: String,
            headers: Map<String, String>,
        ): Result<String> {
            this.method = method
            this.path = path
            this.body = body
            this.headers = headers
            return Result.success(response)
        }
    }
}
