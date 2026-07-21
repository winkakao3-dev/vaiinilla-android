package com.vaiinilla.app

import com.vaiinilla.app.core.network.EmptyVaiinillaApiClient
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.data.order.RemoteOrderRepository
import com.vaiinilla.app.domain.model.CreateOrderItem
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.PaymentMethod
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderRepositorySelectionTest {
    @Test
    fun `remote order repository stops without inventing response adapter`() {
        val repository = RemoteOrderRepository(
            EmptyVaiinillaApiClient("https://example.invalid/api/v1/"),
            OrderContractJson(),
        )
        val result = repository.createOrder(
            CreateOrderRequest(
                paymentMethod = PaymentMethod.CASH,
                destination = OrderDestination.TAKE_AWAY,
                spaceId = null,
                kitchenNotes = "",
                items = listOf(CreateOrderItem(103, 1, listOf(310, 314))),
            ),
            UUID.randomUUID().toString(),
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("OpenAPI aprobado") == true)
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

    private fun validRequest() = CreateOrderRequest(
        paymentMethod = PaymentMethod.CASH,
        destination = OrderDestination.TAKE_AWAY,
        spaceId = null,
        kitchenNotes = "",
        items = listOf(CreateOrderItem(103, 1, listOf(310, 314))),
    )

    private class RecordingApiClient : VaiinillaApiClient {
        override val baseUrl: String = "https://example.invalid/api/v1/"
        var lastPath: String = ""
        var lastBody: String = ""
        var lastHeaders: Map<String, String> = emptyMap()

        override fun get(path: String): Result<String> = Result.failure(IllegalStateException("not used"))

        override fun post(path: String, body: String, headers: Map<String, String>): Result<String> {
            lastPath = path
            lastBody = body
            lastHeaders = headers
            return Result.failure(IllegalStateException("transport unavailable"))
        }
    }

}
