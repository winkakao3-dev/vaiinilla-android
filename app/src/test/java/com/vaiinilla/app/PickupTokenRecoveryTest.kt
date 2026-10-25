package com.vaiinilla.app

import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.core.security.InMemoryPickupTokenStore
import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.data.order.RemoteOrderRepository
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.usecase.ListOrdersUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PickupTokenRecoveryTest {
    @Test
    fun `recovered pickup token comes from the qr endpoint and stays cached`() {
        val store = InMemoryPickupTokenStore()
        val client =
            RecordingApiClient(
                mapOf(
                    "pedidos" to orderListEnvelope(order("order-listo", "listo")),
                    "pedidos/order-listo/qr" to qrEnvelope("v1.recuperado"),
                ),
            )
        val repository = RemoteOrderRepository(client, OrderContractJson(), store)
        val order = repository.listOrders(OperationalRole.CLIENT).getOrThrow().single()
        assertNull(order.pickupToken)

        val recovered = repository.recoverPickupToken(order).getOrThrow()

        assertEquals("v1.recuperado", recovered.pickupToken)
        assertEquals("pedidos/order-listo/qr", client.requestedPaths.last())
        assertEquals("v1.recuperado", store.read("order-listo"))
        val relisted = repository.listOrders(OperationalRole.CLIENT).getOrThrow().single()
        assertEquals("v1.recuperado", relisted.pickupToken)
    }

    @Test
    fun `only ready orders without a pickup token ask for it`() {
        val store = InMemoryPickupTokenStore()
        store.save("order-listo-guardado", "v1.guardado")
        val client =
            RecordingApiClient(
                mapOf(
                    "pedidos" to
                        orderListEnvelope(
                            order("order-listo", "listo"),
                            order("order-listo-guardado", "listo"),
                            order("order-listo-con-qr", "listo", qrToken = "v1.del-backend"),
                            order("order-preparando", "preparando"),
                            order("order-cobrado", "cobrado"),
                            order("order-entregado", "entregado"),
                        ),
                    "pedidos/order-listo/qr" to qrEnvelope("v1.recuperado"),
                ),
            )
        val repository = RemoteOrderRepository(client, OrderContractJson(), store)
        val useCase = ListOrdersUseCase(repository)

        val orders = useCase.withRecoveredPickupTokens(useCase(OperationalRole.CLIENT).getOrThrow())

        assertEquals(listOf("pedidos", "pedidos/order-listo/qr"), client.requestedPaths)
        val tokens = orders.associate { it.summary.id to it.pickupToken }
        assertEquals("v1.recuperado", tokens["order-listo"])
        assertEquals("v1.guardado", tokens["order-listo-guardado"])
        assertEquals("v1.del-backend", tokens["order-listo-con-qr"])
        assertNull(tokens["order-preparando"])
    }

    @Test
    fun `a failed recovery keeps the order without a token and is retried after a short wait`() {
        val client =
            RecordingApiClient(
                mapOf("pedidos" to orderListEnvelope(order("order-listo", "listo"))),
            )
        val useCase = ListOrdersUseCase(RemoteOrderRepository(client, OrderContractJson(), InMemoryPickupTokenStore()))
        var clock = 0L
        useCase.now = { clock }

        val first = useCase.withRecoveredPickupTokens(useCase(OperationalRole.CLIENT).getOrThrow())
        assertNull(first.single().pickupToken)

        client.responses["pedidos/order-listo/qr"] = qrEnvelope("v1.recuperado")
        // El sondeo siguiente (5 s después) todavía no lo pide otra vez: espera 30 s (PEDIDO-20).
        clock = 5_000L
        assertNull(useCase.withRecoveredPickupTokens(useCase(OperationalRole.CLIENT).getOrThrow()).single().pickupToken)
        clock = 31_000L
        val second = useCase.withRecoveredPickupTokens(useCase(OperationalRole.CLIENT).getOrThrow())
        assertEquals("v1.recuperado", second.single().pickupToken)

        useCase.withRecoveredPickupTokens(useCase(OperationalRole.CLIENT).getOrThrow())
        assertEquals(2, client.requestedPaths.count { it == "pedidos/order-listo/qr" })
    }

    private fun order(
        id: String,
        state: String,
        qrToken: String? = null,
    ): String =
        """
        {
          "id": "$id",
          "folio": 42,
          "fecha_operativa": "2026-10-17",
          "estado": "$state",
          "metodo_pago": "efectivo",
          "destino": "para_llevar",
          "espacio": null,
          "subtotal": "26.00",
          "ahorro_combinado": "0.00",
          "cashback_otorgado": "0.00",
          "total": "26.00",
          "version": 3,
          "creado_en": "2026-10-17T12:00:00.000Z",
          "actualizado_en": "2026-10-17T12:05:00.000Z",
          "notas_cocina": "",
          "items": []${qrToken?.let { ",\n  \"qr_token\": \"$it\"" }.orEmpty()}
        }
        """.trimIndent()

    private fun orderListEnvelope(vararg orders: String): String =
        """{"data":[${orders.joinToString(",")}],"meta":{},"error":null}"""

    private fun qrEnvelope(token: String): String = """{"data":{"qr_token":"$token"},"meta":{},"error":null}"""

    private class RecordingApiClient(
        initial: Map<String, String>,
    ) : VaiinillaApiClient {
        override val baseUrl: String = "https://example.invalid/api/v1/"
        val responses = initial.toMutableMap()
        val requestedPaths = mutableListOf<String>()

        override fun get(
            path: String,
            query: Map<String, String>,
        ): Result<String> {
            requestedPaths += path
            return responses[path]?.let { Result.success(it) }
                ?: Result.failure(IllegalStateException("GET $path no configurado"))
        }

        override fun post(
            path: String,
            body: String,
            headers: Map<String, String>,
        ): Result<String> = Result.failure(UnsupportedOperationException())
    }
}
