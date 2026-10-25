package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderPayment
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.StripePaymentStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** PEDIDO-06: el cliente lee qué pasó con su dinero cuando se cancela el pedido. */
class CancellationMoneyNoteTest {
    @Test
    fun `balance orders say the money went back to the wallet`() {
        assertEquals(
            "El importe regresó a tu saldo Vaiinilla.",
            cancellationMoneyNote(order(PaymentMethod.BALANCE)),
        )
    }

    @Test
    fun `card orders follow the refund status the server reports`() {
        assertEquals(
            "Tu reembolso a la tarjeta está en proceso. El banco puede tardar unos días en reflejarlo.",
            cancellationMoneyNote(order(PaymentMethod.STRIPE, StripePaymentStatus.REFUNDING)),
        )
        assertEquals(
            "Te reembolsamos a la tarjeta. El banco puede tardar unos días en reflejarlo.",
            cancellationMoneyNote(order(PaymentMethod.STRIPE, StripePaymentStatus.REFUNDED)),
        )
    }

    @Test
    fun `nothing is promised when the server does not show a refund`() {
        assertNull(cancellationMoneyNote(order(PaymentMethod.STRIPE, StripePaymentStatus.PENDING)))
        assertNull(cancellationMoneyNote(order(PaymentMethod.CASH)))
    }

    @Test
    fun `a pay at the end order says it will not be charged`() {
        assertEquals(
            "No se te cobrará: este pedido iba a tu cuenta.",
            cancellationMoneyNote(order(PaymentMethod.CASH, payAtEnd = true)),
        )
    }

    private fun order(
        method: PaymentMethod,
        paymentStatus: StripePaymentStatus? = null,
        payAtEnd: Boolean = false,
    ) = OrderDetail(
        summary =
            OrderSummary(
                id = "1",
                folio = 7,
                operationalDate = "2026-10-11",
                state = OrderState.CANCELED,
                paymentMethod = method,
                destination = OrderDestination.TAKE_AWAY,
                space = null,
                subtotal = "10.00",
                combinedSavings = "0.00",
                cashbackAwarded = "0.00",
                total = "10.00",
                version = 1,
                createdAt = "2026-10-11T00:00:00Z",
                updatedAt = "2026-10-11T00:00:00Z",
                payAtEnd = payAtEnd,
            ),
        user = null,
        kitchenNotes = "",
        items = emptyList(),
        pickupToken = null,
        payment = paymentStatus?.let { OrderPayment("a", "pi", "acct", it) },
    )
}
