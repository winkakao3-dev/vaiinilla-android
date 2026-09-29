package com.vaiinilla.app.data.order

import com.vaiinilla.app.domain.model.ContractRules
import com.vaiinilla.app.domain.model.CreateOrderItem
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.ui.order.createOrderFingerprint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PayAtEndContractTest {
    private val json = OrderContractJson()

    private fun request(
        payment: PaymentMethod = PaymentMethod.CASH,
        destination: OrderDestination = OrderDestination.IN_SPACE,
        spaceId: Int? = 7,
        payAtEnd: Boolean = false,
    ) = CreateOrderRequest(
        paymentMethod = payment,
        destination = destination,
        spaceId = spaceId,
        kitchenNotes = "",
        items = listOf(CreateOrderItem(productId = 44, quantity = 1, optionIds = emptyList())),
        payAtEnd = payAtEnd,
    )

    private fun orderData(extra: String = "") =
        """
        {"id":"o-1","folio":42,"fecha_operativa":"2026-09-29","estado":"cobrado",
         "metodo_pago":"efectivo","destino":"en_espacio","espacio":{"id":7,"nombre":"Cancha 1","tipo":"cancha"},
         "subtotal":"110.00","ahorro_combinado":"0.00","cashback_otorgado":"0.00","total":"110.00","version":1,
         "creado_en":"2026-09-29T20:00:00.000Z","actualizado_en":"2026-09-29T20:00:00.000Z"$extra}
        """.trimIndent()

    private fun orderJson(extra: String = "") = """{"data":${orderData(extra)},"meta":{},"error":null}"""

    @Test
    fun `a normal order is sent exactly as before, without the pay at end field`() {
        assertFalse(json.encodeCreateRequest(request()).contains("pago_diferido"))
    }

    @Test
    fun `an order to the account sends pago_diferido true`() {
        assertTrue(json.encodeCreateRequest(request(payAtEnd = true)).contains("\"pago_diferido\":true"))
    }

    @Test
    fun `parses an order to the account with its pending payment`() {
        val order = json.parseOrderDetail(orderJson(""","pago_diferido":true,"pago_pendiente":true"""))
        assertTrue(order.summary.payAtEnd)
        assertTrue(order.summary.paymentPending)
        assertEquals(OrderState.PAID, order.summary.state)
    }

    @Test
    fun `an older server without the new fields still parses`() {
        val order = json.parseOrderDetail(orderJson())
        assertFalse(order.summary.payAtEnd)
        assertFalse(order.summary.paymentPending)
        assertNull(order.summary.cancelReason)
        assertNull(order.summary.canceledByRole)
    }

    @Test
    fun `parses who rejected an order and why`() {
        val raw =
            orderJson(""","motivo_cancelacion":"Se acabó el producto","cancelado_por_rol":"cocina"""")
                .replace("\"estado\":\"cobrado\"", "\"estado\":\"cancelado\"")
        val order = json.parseOrderDetail(raw)
        assertEquals(OrderState.CANCELED, order.summary.state)
        assertEquals("Se acabó el producto", order.summary.cancelReason)
        assertEquals("cocina", order.summary.canceledByRole)
    }

    @Test
    fun `parses the cancelled order from the cancellation envelope`() {
        val raw = """{"data":{"pedido":${orderData()},"devolucion":null,"reembolso":null},"meta":{},"error":null}"""
        assertEquals("o-1", json.parseCancelledOrder(raw).summary.id)
    }

    @Test
    fun `cancel sends the version and the trimmed reason`() {
        assertEquals(
            """{"version_esperada":3,"motivo":"Se acabó"}""",
            json.encodeCancelOrder(3, "  Se acabó "),
        )
    }

    @Test
    fun `pay at end needs cash and a space`() {
        ContractRules.validateRemoteOrderRequest(request(payAtEnd = true))
        assertThrows(IllegalArgumentException::class.java) {
            ContractRules.validateRemoteOrderRequest(request(payment = PaymentMethod.STRIPE, payAtEnd = true))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ContractRules.validateRemoteOrderRequest(request(payment = PaymentMethod.BALANCE, payAtEnd = true))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ContractRules.validateRemoteOrderRequest(
                request(destination = OrderDestination.TAKE_AWAY, spaceId = null, payAtEnd = true),
            )
        }
    }

    @Test
    fun `the idempotency fingerprint separates pay at end from a normal cash order`() {
        assertNotEquals(createOrderFingerprint(request()), createOrderFingerprint(request(payAtEnd = true)))
        // Y no cambia para los pedidos de siempre: una llave guardada sigue sirviendo.
        assertEquals(createOrderFingerprint(request()), createOrderFingerprint(request(payAtEnd = false)))
    }
}
