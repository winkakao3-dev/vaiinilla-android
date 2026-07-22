package com.vaiinilla.app

import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.data.order.RemoteOrderRepository
import com.vaiinilla.app.domain.model.CreateOrderItem
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderRepositorySelectionTest {
    @Test
    fun `remote order repository parses create response from api envelope`() {
        val repository = RemoteOrderRepository(
            RecordingApiClient(
                postResponse = sampleOrderEnvelope(OrderState.PENDING_PAYMENT.wireValue),
            ),
            OrderContractJson(),
        )
        val result = repository.createOrder(validRequest(), UUID.randomUUID().toString())
        assertTrue(result.isSuccess)
        assertEquals(OrderState.PENDING_PAYMENT, result.getOrThrow().summary.state)
    }

    @Test
    fun `remote repository emits approved path body and idempotency header`() {
        val client = RecordingApiClient()
        val repository = RemoteOrderRepository(client, OrderContractJson())
        val key = UUID.randomUUID().toString()

        repository.createOrder(validRequest(), key)

        assertEquals("pedidos", client.lastPath)
        assertEquals(key, client.lastHeaders["Idempotency-Key"])
        assertTrue(client.lastBody.contains("\"metodo_pago\":\"efectivo\""))
        assertTrue(client.lastBody.contains("\"destino\":\"para_llevar\""))
        assertTrue(client.lastBody.contains("\"espacio_id\":null"))
        assertFalse(client.lastBody.contains("precio"))
        assertFalse(client.lastBody.contains("total"))
        assertFalse(client.lastBody.contains("estado"))
    }

    @Test
    fun `remote collect cash uses cobros-efectivo path`() {
        val client = RecordingApiClient(
            getResponses = mapOf(
                "pedidos/order-1" to sampleOrderEnvelope("por_cobrar", version = 1),
            ),
            postResponse = sampleCashEnvelope("cobrado", version = 2),
        )
        val repository = RemoteOrderRepository(client, OrderContractJson())

        val result = repository.collectCash("order-1", "26.00", UUID.randomUUID().toString())

        assertTrue(result.isSuccess)
        assertEquals("pedidos/order-1/cobros-efectivo", client.lastPath)
        assertTrue(client.lastBody.contains("\"monto_recibido\":\"26.00\""))
        assertTrue(client.lastBody.contains("\"version_esperada\":1"))
    }

    private fun validRequest() = CreateOrderRequest(
        paymentMethod = PaymentMethod.CASH,
        destination = OrderDestination.TAKE_AWAY,
        spaceId = null,
        kitchenNotes = "",
        items = listOf(CreateOrderItem(103, 1, listOf(310, 314))),
    )

    private fun sampleOrderEnvelope(state: String, version: Int = 1): String = """
        {
          "data": {
            "id": "order-1",
            "folio": 42,
            "fecha_operativa": "2026-07-21",
            "estado": "$state",
            "metodo_pago": "efectivo",
            "destino": "para_llevar",
            "espacio": null,
            "subtotal": "26.00",
            "ahorro_combinado": "0.00",
            "cashback_otorgado": "0.00",
            "total": "26.00",
            "version": $version,
            "creado_en": "2026-07-21T12:00:00.000Z",
            "actualizado_en": "2026-07-21T12:00:00.000Z",
            "notas_cocina": "",
            "items": []
          },
          "meta": { "page": null, "total_pages": null, "total_items": null, "cursor": null },
          "error": null
        }
    """.trimIndent()

    private fun sampleCashEnvelope(state: String, version: Int): String = """
        {
          "data": {
            "pedido": {
              "id": "order-1",
              "folio": 42,
              "fecha_operativa": "2026-07-21",
              "estado": "$state",
              "metodo_pago": "efectivo",
              "destino": "para_llevar",
              "espacio": null,
              "subtotal": "26.00",
              "ahorro_combinado": "0.00",
              "cashback_otorgado": "0.00",
              "total": "26.00",
              "version": $version,
              "creado_en": "2026-07-21T12:00:00.000Z",
              "actualizado_en": "2026-07-21T12:00:00.000Z",
              "notas_cocina": "",
              "items": []
            },
            "monto_recibido": "26.00",
            "cambio": "0.00"
          },
          "meta": { "page": null, "total_pages": null, "total_items": null, "cursor": null },
          "error": null
        }
    """.trimIndent()

    private class RecordingApiClient(
        private val getResponses: Map<String, String> = emptyMap(),
        private val postResponse: String? = null,
    ) : VaiinillaApiClient {
        override val baseUrl: String = "https://example.invalid/api/v1/"
        var lastPath: String = ""
        var lastBody: String = ""
        var lastHeaders: Map<String, String> = emptyMap()

        override fun get(path: String, query: Map<String, String>): Result<String> {
            lastPath = path
            return getResponses[path]?.let { Result.success(it) }
                ?: Result.failure(IllegalStateException("GET $path no configurado"))
        }

        override fun post(path: String, body: String, headers: Map<String, String>): Result<String> {
            lastPath = path
            lastBody = body
            lastHeaders = headers
            return postResponse?.let { Result.success(it) }
                ?: Result.failure(IllegalStateException("POST $path sin respuesta"))
        }
    }
}
