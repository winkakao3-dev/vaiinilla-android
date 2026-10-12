package com.vaiinilla.app.ui.order

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.repository.OrderRepositoryException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderTrackingLinkPolicyTest {
    @Test
    fun `la espera tras errores crece de 5 s a 1 min y no pasa del tope`() {
        val middle = { 0.5 }
        assertEquals(5_000L, OrderTrackingLinkPolicy.retryDelayMs(1, middle))
        assertEquals(10_000L, OrderTrackingLinkPolicy.retryDelayMs(2, middle))
        assertEquals(20_000L, OrderTrackingLinkPolicy.retryDelayMs(3, middle))
        assertEquals(40_000L, OrderTrackingLinkPolicy.retryDelayMs(4, middle))
        assertEquals(60_000L, OrderTrackingLinkPolicy.retryDelayMs(5, middle))
        assertEquals(60_000L, OrderTrackingLinkPolicy.retryDelayMs(50, middle))
    }

    @Test
    fun `el azar solo mueve la espera entre 80 y 120 por ciento`() {
        assertEquals(4_000L, OrderTrackingLinkPolicy.retryDelayMs(1) { 0.0 })
        assertEquals(6_000L, OrderTrackingLinkPolicy.retryDelayMs(1) { 1.0 })
    }

    @Test
    fun `404 y 410 del enlace son pedido que ya no existe, otros errores no`() {
        assertTrue(OrderTrackingLinkPolicy.isGone(OrderRepositoryException("NOT_FOUND", "x", apiError(404))))
        assertTrue(OrderTrackingLinkPolicy.isGone(apiError(410)))
        assertFalse(OrderTrackingLinkPolicy.isGone(apiError(500)))
        assertFalse(OrderTrackingLinkPolicy.isGone(IllegalStateException("sin red")))
    }

    @Test
    fun `solo siguen en curso los pedidos activos, y el entregado que aún se cobra`() {
        assertTrue(OrderTrackingLinkPolicy.isLive(order(OrderState.PENDING_PAYMENT)))
        assertTrue(OrderTrackingLinkPolicy.isLive(order(OrderState.PAID)))
        assertTrue(OrderTrackingLinkPolicy.isLive(order(OrderState.PREPARING)))
        assertTrue(OrderTrackingLinkPolicy.isLive(order(OrderState.READY)))
        assertFalse(OrderTrackingLinkPolicy.isLive(order(OrderState.DELIVERED)))
        assertFalse(OrderTrackingLinkPolicy.isLive(order(OrderState.CANCELED)))
        assertTrue(
            OrderTrackingLinkPolicy.isLive(
                order(OrderState.DELIVERED, payAtEnd = true, paymentPending = true),
            ),
        )
    }

    private fun apiError(status: Int) = ApiClientException("ORDER_NOT_FOUND", "x", httpStatus = status)

    private fun order(
        state: OrderState,
        payAtEnd: Boolean = false,
        paymentPending: Boolean = false,
    ) = OrderDetail(
        summary =
            OrderSummary(
                id = "o",
                folio = 1,
                operationalDate = "2026-10-11",
                state = state,
                paymentMethod = PaymentMethod.CASH,
                destination = OrderDestination.IN_SPACE,
                space = null,
                subtotal = "0.00",
                combinedSavings = "0.00",
                cashbackAwarded = "0.00",
                total = "0.00",
                version = 1,
                createdAt = "2026-10-11T08:00:00Z",
                updatedAt = "2026-10-11T08:00:00Z",
                payAtEnd = payAtEnd,
                paymentPending = paymentPending,
            ),
        user = null,
        kitchenNotes = "",
        items = emptyList(),
    )
}
