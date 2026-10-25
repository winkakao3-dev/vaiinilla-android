package com.vaiinilla.app.data.order

import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.core.security.InMemoryPickupTokenStore
import com.vaiinilla.app.domain.model.OperationalRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteOrderListPaginationTest {
    @Test
    fun `client list follows the cursor so recent orders are not hidden behind the first page`() {
        val client =
            PagedApiClient(
                pages =
                    mapOf(
                        null to page(listOf("viejo-1", "viejo-2"), nextCursor = "c1"),
                        "c1" to page(listOf("medio"), nextCursor = "c2"),
                        "c2" to page(listOf("reciente"), nextCursor = null),
                    ),
            )
        val repository = RemoteOrderRepository(client, OrderContractJson(), InMemoryPickupTokenStore())

        val orders = repository.listOrders(OperationalRole.CLIENT).getOrThrow()

        assertEquals(listOf("viejo-1", "viejo-2", "medio", "reciente"), orders.map { it.summary.id })
        assertEquals(listOf(null, "c1", "c2"), client.requestedCursors)
        assertTrue(
            "la primera petición no lleva parámetros: es la que guarda la caché",
            client.queries.first().isEmpty(),
        )
    }

    @Test
    fun `incremental refresh keeps its filter on every page`() {
        val client =
            PagedApiClient(
                pages =
                    mapOf(
                        null to page(listOf("a"), nextCursor = "c1"),
                        "c1" to page(listOf("b"), nextCursor = null),
                    ),
            )
        val repository = RemoteOrderRepository(client, OrderContractJson(), InMemoryPickupTokenStore())

        repository.listOrders(OperationalRole.CLIENT, updatedSince = "2026-10-17T12:00:00Z").getOrThrow()

        assertTrue(client.queries.all { it["actualizado_desde"] == "2026-10-17T12:00:00Z" })
    }

    @Test
    fun `a cursor that never ends stops at the page limit`() {
        val client = PagedApiClient(pages = emptyMap(), endless = true)
        val repository = RemoteOrderRepository(client, OrderContractJson(), InMemoryPickupTokenStore())

        val orders = repository.listOrders(OperationalRole.CLIENT).getOrThrow()

        assertEquals(20, client.queries.size)
        assertEquals(20, orders.size)
    }

    @Test
    fun `a failing later page fails the whole list instead of returning a partial one`() {
        val client = PagedApiClient(pages = mapOf(null to page(listOf("a"), nextCursor = "c1")))
        val repository = RemoteOrderRepository(client, OrderContractJson(), InMemoryPickupTokenStore())

        assertTrue(repository.listOrders(OperationalRole.CLIENT).isFailure)
    }

    private fun page(
        ids: List<String>,
        nextCursor: String?,
    ): String =
        """{"data":[${ids.joinToString(
            ",",
        ) { order(it) }}],"meta":{"cursor":${nextCursor?.let { "\"$it\"" }}},"error":null}"""

    private fun order(id: String): String =
        """
        {
          "id": "$id", "folio": 42, "fecha_operativa": "2026-10-17", "estado": "entregado",
          "metodo_pago": "efectivo", "destino": "para_llevar", "espacio": null,
          "subtotal": "26.00", "ahorro_combinado": "0.00", "cashback_otorgado": "0.00", "total": "26.00",
          "version": 3, "creado_en": "2026-10-17T12:00:00.000Z", "actualizado_en": "2026-10-17T12:05:00.000Z",
          "notas_cocina": "", "items": []
        }
        """.trimIndent()

    private class PagedApiClient(
        private val pages: Map<String?, String>,
        private val endless: Boolean = false,
    ) : VaiinillaApiClient {
        override val baseUrl: String = "https://example.invalid/api/v1/"
        val queries = mutableListOf<Map<String, String>>()
        val requestedCursors: List<String?> get() = queries.map { it["cursor"] }

        override fun get(
            path: String,
            query: Map<String, String>,
        ): Result<String> {
            queries += query
            if (endless) {
                val index = queries.size
                return Result.success(
                    """{"data":[${endlessOrder("o$index")}],"meta":{"cursor":"c$index"},"error":null}""",
                )
            }
            return pages[query["cursor"]]?.let { Result.success(it) }
                ?: Result.failure(IllegalStateException("página ${query["cursor"]} no configurada"))
        }

        override fun post(
            path: String,
            body: String,
            headers: Map<String, String>,
        ): Result<String> = Result.failure(UnsupportedOperationException())

        private fun endlessOrder(id: String): String =
            """{"id":"$id","folio":1,"fecha_operativa":"2026-10-17","estado":"entregado","metodo_pago":"efectivo",""" +
                """"destino":"para_llevar","espacio":null,"subtotal":"1.00","ahorro_combinado":"0.00",""" +
                """"cashback_otorgado":"0.00","total":"1.00","version":1,"creado_en":"2026-10-17T12:00:00.000Z",""" +
                """"actualizado_en":"2026-10-17T12:05:00.000Z","notas_cocina":"","items":[]}"""
    }
}
