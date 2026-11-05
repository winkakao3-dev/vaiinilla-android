package com.vaiinilla.app.ui.admin

import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Test

class AwaitingAcceptanceTest {
    @Test
    fun `an order for another day shows even when the active page did not bring it`() {
        // T02: «Activos» llega de 30 en 30 del más viejo al más nuevo; el encargo nuevo llega por su consulta propia.
        val fetched = listOf(order("encargo-manana", OrderState.AWAITING_ACCEPTANCE))
        val listed = listOf(order("viejo", OrderState.PENDING_PAYMENT))
        assertEquals(listOf("encargo-manana"), awaitingAcceptance(fetched, listed).map { it.summary.id })
    }

    @Test
    fun `an order in both lists shows once and other states stay out`() {
        val fetched = listOf(order("a", OrderState.AWAITING_ACCEPTANCE))
        val listed = listOf(order("a", OrderState.AWAITING_ACCEPTANCE), order("b", OrderState.PAID))
        assertEquals(listOf("a"), awaitingAcceptance(fetched, listed).map { it.summary.id })
    }

    private fun order(
        id: String,
        state: OrderState,
    ) = OrderDetail(
        summary =
            OrderSummary(
                id = id,
                folio = 1,
                operationalDate = "2026-11-06",
                state = state,
                paymentMethod = PaymentMethod.CASH,
                destination = OrderDestination.TAKE_AWAY,
                space = null,
                subtotal = "10.00",
                combinedSavings = "0.00",
                cashbackAwarded = "0.00",
                total = "10.00",
                version = 1,
                createdAt = "2026-11-05T20:00:00Z",
                updatedAt = "2026-11-05T20:00:00Z",
            ),
        user = null,
        kitchenNotes = "",
        items = emptyList(),
    )
}
