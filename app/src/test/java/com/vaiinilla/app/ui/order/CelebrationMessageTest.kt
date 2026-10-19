package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderSpace
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Test

class CelebrationMessageTest {
    @Test
    fun `un pedido a la cuenta no manda al cliente a Caja`() {
        assertEquals(
            "Va a tu cuenta: la pagas al final.",
            celebrationMessage(order(payAtEnd = true), PurchaseCelebrationKind.ORDER_RECEIVED),
        )
        assertEquals(
            "Paga en Caja para continuar con tu pedido.",
            celebrationMessage(order(payAtEnd = false), PurchaseCelebrationKind.ORDER_RECEIVED),
        )
        assertEquals(
            "Tu pago fue autorizado y tu pedido ya está en marcha.",
            celebrationMessage(order(payAtEnd = false), PurchaseCelebrationKind.PAYMENT_CONFIRMED),
        )
    }

    private fun order(payAtEnd: Boolean) =
        OrderDetail(
            summary =
                OrderSummary(
                    id = "1",
                    folio = 42,
                    operationalDate = "2026-10-17",
                    state = OrderState.PENDING_PAYMENT,
                    paymentMethod = PaymentMethod.CASH,
                    destination = OrderDestination.IN_SPACE,
                    space = OrderSpace(id = 4, name = "Mesa 4", type = "mesa"),
                    subtotal = "10.00",
                    combinedSavings = "0.00",
                    cashbackAwarded = "0.00",
                    total = "10.00",
                    version = 1,
                    createdAt = "2026-10-17T00:00:00Z",
                    updatedAt = "2026-10-17T00:00:00Z",
                    payAtEnd = payAtEnd,
                    paymentPending = payAtEnd,
                ),
            user = null,
            kitchenNotes = "",
            items = emptyList(),
        )
}
