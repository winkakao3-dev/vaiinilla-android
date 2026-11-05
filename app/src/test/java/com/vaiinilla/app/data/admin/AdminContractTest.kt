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
    fun `unpaid accounts read orders, paid and remaining, and the old count still parses`() {
        val parsed =
            json.parseUnpaidAccounts(
                """
                {"data":{"cuentas":[
                  {"espacio":{"id":4,"nombre":"Pista 4","tipo":"pista"},"sesion_id":null,"abandonada":true,
                   "pedidos":[{"id":"p1","folio":12,"total":"120.00","restante":"80.00"}],"num_pedidos":1,
                   "total":"120.00","abonado":"40.00","restante":"80.00","horas":3},
                  {"espacio":{"id":5,"nombre":"Mesa 5"},"pedidos":2,"total":"90.00","horas":0}
                ],"total":"210.00","restante":"170.00","abandonadas":1}}
                """.trimIndent(),
            )
        val first = parsed.accounts[0]
        assertEquals(1, first.orders)
        assertEquals("40.00", first.paid)
        assertEquals("80.00", first.remaining)
        assertNull(first.sessionId)
        assertEquals("12", first.orderLines.single().folio)
        assertEquals("80.00", first.orderLines.single().remaining)
        val old = parsed.accounts[1]
        assertEquals(2, old.orders)
        assertEquals("90.00", old.remaining)
        assertTrue(old.orderLines.isEmpty())
        assertEquals("170.00", parsed.remaining)
    }

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
              "productos":[{"producto_id":4,"nombre":"Latte","cantidad":9,"ventas":"405.00"}],
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
        assertEquals("405.00", summary.topProducts.single().amount)
        assertEquals("no_recogido", summary.ordersByStatus.single().key)
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

    @Test
    fun `lo archivado se recuerda tras reiniciar y desarchivar lo olvida`() {
        val raw =
            """{"data":{"id":7,"categoria_id":3,"estacion_preparacion":"caja","nombre":"Té viejo",
            "tiempo_estimado_min":1,"precio_mostrador":"20.00","precio_digital":"20.00","disponible":true,
            "archivado":true},"error":null,"meta":{}}"""
        val store = MemoryArchivedProductsStore()
        val repository =
            RemoteAdminRepository(
                RecordingClient(raw),
                ContractResponseParser(),
                OrderContractJson(),
                archivedStore = store,
            )
        repository.setProductArchived(7, archived = true, idempotencyKey = "idem-arch").getOrThrow()

        // Otra instancia con el mismo almacén: como la app tras un reinicio en frío.
        val restarted =
            RemoteAdminRepository(
                RecordingClient(raw),
                ContractResponseParser(),
                OrderContractJson(),
                archivedStore = store,
            )
        val remembered = restarted.archivedProducts().single()
        assertEquals(7, remembered.id)
        assertEquals("Té viejo", remembered.name)
        assertTrue(remembered.archived)

        restarted.setProductArchived(7, archived = false, idempotencyKey = "idem-desarch").getOrThrow()
        assertTrue(restarted.archivedProducts().isEmpty())
    }

    private class MemoryArchivedProductsStore : ArchivedProductsStore {
        private val products = linkedMapOf<Int, String>()

        override fun readAll(): List<String> = products.values.toList()

        override fun put(
            productId: Int,
            rawProduct: String,
        ) {
            products[productId] = rawProduct
        }

        override fun remove(productId: Int) {
            products.remove(productId)
        }
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

    @Test
    fun `unpaid accounts show what is left after abonos (R33)`() {
        val unpaid =
            json.parseUnpaidAccounts(
                """
                {"data":{"cuentas":[
                  {"espacio":{"id":4,"nombre":"Mesa 4"},"pedidos":2,"total":"300.00","horas":1,"abandonada":true,
                   "abonado":"100.00","restante":"200.00"},
                  {"espacio":{"id":9,"nombre":"Mesa 9"},"pedidos":1,"total":"80.00"}
                 ],"total":"380.00","restante":"280.00","abandonadas":1}}
                """.trimIndent(),
            )

        assertEquals("300.00", unpaid.accounts[0].total)
        assertEquals("200.00", unpaid.accounts[0].remaining)
        assertEquals("100.00", unpaid.accounts[0].paid)
        // Un servidor anterior no manda restante: se queda el total.
        assertEquals("80.00", unpaid.accounts[1].remaining)
        assertEquals("0.00", unpaid.accounts[1].paid)
        assertEquals("380.00", unpaid.total)
        assertEquals("280.00", unpaid.remaining)
    }

    @Test
    fun `unpaid accounts read the count in pedidos and the list in pedidos_detalle`() {
        val unpaid =
            json.parseUnpaidAccounts(
                """
                {"data":{"cuentas":[
                  {"espacio":{"id":4,"nombre":"Mesa 4"},"pedidos":2,"total":"300.00",
                   "pedidos_detalle":[{"id":"a","folio":"12","total":"200.00","restante":"150.00"},
                                      {"id":"b","folio":"13","total":"100.00"}]}
                 ],"total":"300.00"}}
                """.trimIndent(),
            )

        val account = unpaid.accounts.single()
        assertEquals(2, account.orders)
        assertEquals(listOf("a", "b"), account.orderLines.map { it.id })
        assertEquals("12", account.orderLines[0].folio)
        assertEquals("150.00", account.orderLines[0].remaining)
        assertEquals("100.00", account.orderLines[1].remaining)
    }
}
