package com.vaiinilla.app

import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.core.security.InMemoryPickupTokenStore
import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.data.order.RemoteOrderRepository
import com.vaiinilla.app.domain.model.CreateOrderItem
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.StripePaymentStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StripeOrderContractTest {
    private val json = OrderContractJson()

    @Test
    fun `stripe request uses exact wire value and never sends trusted money fields`() {
        val raw = json.encodeCreateRequest(stripeRequest())

        assertTrue(raw.contains("\"metodo_pago\":\"stripe\""))
        assertTrue(raw.contains("\"notas_cocina\":null"))
        assertFalse(raw.contains("\"total\""))
        assertFalse(raw.contains("precio_unitario"))
        assertFalse(raw.contains("stripe_account_id"))
        assertFalse(raw.contains("payment_intent"))
        assertFalse(raw.contains("client_secret"))
    }

    @Test
    fun `create response separates ephemeral Stripe credentials from normal order payment`() {
        val created = json.parseCreatedOrder(stripeCreatedEnvelope())

        assertEquals(PaymentMethod.STRIPE, created.order.summary.paymentMethod)
        assertEquals(OrderState.PENDING_PAYMENT, created.order.summary.state)
        assertEquals("62.00", created.order.summary.total)
        assertEquals("pickup-only-at-create", created.order.pickupToken)
        assertEquals(StripePaymentStatus.PENDING, created.order.payment?.status)
        assertEquals("acct_test_establecimiento_001", created.order.payment?.stripeAccountId)
        assertNotNull(created.stripeSession)
        assertEquals("pi_test_001_secret_test", created.stripeSession?.clientSecret)
        assertEquals("pk_test_51Vaiinilla", created.stripeSession?.publishableKey)
    }

    @Test
    fun `normal order query accepts pago without client secret or publishable key`() {
        val order = json.parseOrderDetail(stripeConfirmedEnvelope())

        assertEquals(OrderState.PAID, order.summary.state)
        assertEquals(StripePaymentStatus.CONFIRMED, order.payment?.status)
        assertNull(order.pickupToken)
    }

    @Test
    fun `retry uses same order path no body and caller idempotency key`() {
        val client = StripeRecordingApiClient(stripeRetryEnvelope())
        val repository = RemoteOrderRepository(client, json, InMemoryPickupTokenStore())

        val result = repository.retryStripePayment("order-1", "retry-key-1")

        assertTrue(result.isSuccess)
        assertEquals("pedidos/order-1/pago/stripe", client.lastPath)
        assertNull(client.lastBody)
        assertEquals("retry-key-1", client.lastHeaders["Idempotency-Key"])
        assertEquals("pi_test_001", result.getOrThrow().paymentIntentId)
    }

    @Test
    fun `live publishable key is rejected before PaymentSheet`() {
        val liveEnvelope = stripeCreatedEnvelope().replace("pk_test_51Vaiinilla", "pk_live_51Vaiinilla")

        val result = runCatching { json.parseCreatedOrder(liveEnvelope) }

        assertTrue(result.isFailure)
        assertTrue(
            result
                .exceptionOrNull()
                ?.message
                .orEmpty()
                .contains("Test Mode"),
        )
    }

    @Test
    fun `all terminal order states from new contract are represented`() {
        assertEquals(OrderState.CANCELED, OrderState.fromWireValue("cancelado"))
        assertEquals(OrderState.NOT_PICKED_UP, OrderState.fromWireValue("no_recogido"))
        assertEquals(OrderState.EXPIRED, OrderState.fromWireValue("expirado"))
    }

    private fun stripeRequest() =
        CreateOrderRequest(
            paymentMethod = PaymentMethod.STRIPE,
            destination = OrderDestination.TAKE_AWAY,
            spaceId = null,
            kitchenNotes = "",
            items = listOf(CreateOrderItem(productId = 101, quantity = 2, optionIds = listOf(301))),
        )

    private fun stripeCreatedEnvelope(): String =
        orderEnvelope(
            state = "por_cobrar",
            paymentStatus = "pendiente_pago",
            includeSecrets = true,
            includePickup = true,
        )

    private fun stripeConfirmedEnvelope(): String =
        orderEnvelope(
            state = "cobrado",
            paymentStatus = "confirmado",
            includeSecrets = false,
            includePickup = false,
        )

    private fun orderEnvelope(
        state: String,
        paymentStatus: String,
        includeSecrets: Boolean,
        includePickup: Boolean,
    ): String {
        val secretFields =
            if (includeSecrets) {
                """
                ,
                "client_secret": "pi_test_001_secret_test",
                "publishable_key": "pk_test_51Vaiinilla"
                """.trimIndent()
            } else {
                ""
            }
        val pickup =
            if (includePickup) {
                """
                ,
                "qr_token": "pickup-only-at-create"
                """.trimIndent()
            } else {
                ""
            }
        return """
            {
              "data": {
                "id": "order-1",
                "folio": 42,
                "fecha_operativa": "2026-08-20",
                "estado": "$state",
                "metodo_pago": "stripe",
                "destino": "para_llevar",
                "espacio": null,
                "subtotal": "54.00",
                "ahorro_combinado": "4.00",
                "cashback_otorgado": "0.00",
                "total": "62.00",
                "version": 1,
                "creado_en": "2026-08-20T22:00:00.000Z",
                "actualizado_en": "2026-08-20T22:00:00.000Z",
                "notas_cocina": null,
                "pago": {
                  "payment_attempt_id": "attempt-1",
                  "payment_intent_id": "pi_test_001",
                  "stripe_account_id": "acct_test_establecimiento_001",
                  "payment_status": "$paymentStatus"$secretFields
                },
                "items": [
                  {
                    "id": 501,
                    "producto_id": 101,
                    "nombre_producto": "Chocolate frío",
                    "estacion_preparacion": "cocina",
                    "cantidad": 2,
                    "precio_digital_unitario": "26.00",
                    "precio_cobro_unitario": "22.00",
                    "subtotal": "54.00",
                    "opciones": [
                      {"opcion_id": 301, "nombre": "Leche entera", "precio_extra": "5.00"}
                    ]
                  }
                ]$pickup
              },
              "meta": {},
              "error": null
            }
            """.trimIndent()
    }

    private fun stripeRetryEnvelope(): String =
        """
        {
          "data": {
            "pago": {
              "payment_attempt_id": "attempt-1",
              "payment_intent_id": "pi_test_001",
              "client_secret": "pi_test_001_secret_test",
              "stripe_account_id": "acct_test_establecimiento_001",
              "publishable_key": "pk_test_51Vaiinilla",
              "payment_status": "fallido"
            }
          },
          "meta": {},
          "error": null
        }
        """.trimIndent()

    private class StripeRecordingApiClient(
        private val response: String,
    ) : VaiinillaApiClient {
        override val baseUrl = "https://example.invalid/api/v1/"
        var lastPath = ""
        var lastBody: String? = null
        var lastHeaders: Map<String, String> = emptyMap()

        override fun get(
            path: String,
            query: Map<String, String>,
        ): Result<String> = Result.failure(UnsupportedOperationException())

        override fun post(
            path: String,
            body: String,
            headers: Map<String, String>,
        ): Result<String> {
            lastPath = path
            lastBody = body
            lastHeaders = headers
            return Result.success(response)
        }

        override fun postWithoutBody(
            path: String,
            headers: Map<String, String>,
        ): Result<String> {
            lastPath = path
            lastBody = null
            lastHeaders = headers
            return Result.success(response)
        }
    }
}
