package com.vaiinilla.app

import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderItem
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.ui.screens.confirmationCashPending
import com.vaiinilla.app.ui.screens.confirmationTicketQrPayload
import com.vaiinilla.app.ui.screens.confirmationTicketTitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfirmationTicketCopyTest {
    @Test
    fun `title uses first product or count`() {
        assertEquals("Burrito norteño", confirmationTicketTitle(order(items = listOf("Burrito norteño"))))
        assertEquals("2 productos", confirmationTicketTitle(order(items = listOf("Burrito norteño", "Agua"))))
        assertEquals("Pedido", confirmationTicketTitle(order(items = emptyList())))
    }

    @Test
    fun `qr never invents a pickup token when creation token is missing`() {
        assertEquals("VN-1", confirmationTicketQrPayload(order(token = "VN-1")))
        assertEquals(null, confirmationTicketQrPayload(order(token = null)))
        assertEquals(null, confirmationTicketQrPayload(order(token = "  ")))
    }

    @Test
    fun `cash pending only when cash and por cobrar`() {
        assertTrue(confirmationCashPending(order(state = OrderState.PENDING_PAYMENT, payment = PaymentMethod.CASH)))
        assertFalse(confirmationCashPending(order(state = OrderState.PAID, payment = PaymentMethod.CASH)))
        assertFalse(confirmationCashPending(order(state = OrderState.PENDING_PAYMENT, payment = PaymentMethod.BALANCE)))
    }

    private fun order(
        items: List<String> = listOf("Burrito norteño"),
        token: String? = "VN-1",
        state: OrderState = OrderState.PENDING_PAYMENT,
        payment: PaymentMethod = PaymentMethod.CASH,
    ) = OrderDetail(
        summary =
            OrderSummary(
                id = "1",
                folio = 1042,
                operationalDate = "2026-08-13",
                state = state,
                paymentMethod = payment,
                destination = OrderDestination.TAKE_AWAY,
                space = null,
                subtotal = "10.00",
                combinedSavings = "0.00",
                cashbackAwarded = "0.00",
                total = "10.00",
                version = 1,
                createdAt = "2026-08-13T00:00:00Z",
                updatedAt = "2026-08-13T00:00:00Z",
            ),
        user = null,
        kitchenNotes = "",
        items =
            items.mapIndexed { index, name ->
                OrderItem(
                    id = index,
                    productId = index,
                    productName = name,
                    preparationStation = PreparationStation.KITCHEN,
                    quantity = 1,
                    unitDigitalPrice = "10.00",
                    subtotal = "10.00",
                    options = emptyList(),
                )
            },
        pickupToken = token,
    )
}
