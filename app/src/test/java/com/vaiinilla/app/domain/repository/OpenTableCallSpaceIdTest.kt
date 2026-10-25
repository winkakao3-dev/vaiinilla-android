package com.vaiinilla.app.domain.repository

import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderSpace
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class OpenTableCallSpaceIdTest {
    private val now = Instant.parse("2026-10-12T12:00:00.000Z")

    @Test
    fun deliveredInSpaceCanCallWithinTwoHours() {
        val order = order(OrderState.DELIVERED, updatedAt = "2026-10-12T10:30:00.000Z")
        assertEquals(4, openTableCallSpaceId(order, now))
    }

    @Test
    fun deliveredInSpaceStopsCallingAfterTwoHours() {
        val order = order(OrderState.DELIVERED, updatedAt = "2026-10-12T09:30:00.000Z")
        assertNull(openTableCallSpaceId(order, now))
    }

    @Test
    fun unreadableDeliveryTimeHidesTheButton() {
        val order = order(OrderState.DELIVERED, updatedAt = "12/10/2026 10:30")
        assertNull(openTableCallSpaceId(order, now))
    }

    @Test
    fun windowOnlyAppliesOnceDelivered() {
        val order = order(OrderState.READY, updatedAt = "2026-10-11T00:00:00.000Z")
        assertEquals(4, openTableCallSpaceId(order, now))
    }

    @Test
    fun canceledOrdersNeverCall() {
        val order = order(OrderState.CANCELED, updatedAt = "2026-10-12T11:30:00.000Z")
        assertNull(openTableCallSpaceId(order, now))
    }

    @Test
    fun expiredAndNotPickedUpOrdersNeverCall() {
        listOf(OrderState.EXPIRED, OrderState.NOT_PICKED_UP).forEach { state ->
            assertNull(openTableCallSpaceId(order(state, updatedAt = "2026-10-12T11:30:00.000Z"), now))
        }
    }

    @Test
    fun takeAwayHasNoTable() {
        val order = order(OrderState.DELIVERED, updatedAt = "2026-10-12T11:30:00.000Z", toSpace = false)
        assertNull(openTableCallSpaceId(order, now))
    }

    private fun order(
        state: OrderState,
        updatedAt: String,
        toSpace: Boolean = true,
    ) = OrderDetail(
        summary =
            OrderSummary(
                id = "1",
                folio = 42,
                operationalDate = "2026-10-12",
                state = state,
                paymentMethod = PaymentMethod.CASH,
                destination = if (toSpace) OrderDestination.IN_SPACE else OrderDestination.TAKE_AWAY,
                space = if (toSpace) OrderSpace(id = 4, name = "Mesa 4", type = "mesa") else null,
                subtotal = "10.00",
                combinedSavings = "0.00",
                cashbackAwarded = "0.00",
                total = "10.00",
                version = 1,
                createdAt = "2026-10-12T09:00:00.000Z",
                updatedAt = updatedAt,
            ),
        user = null,
        kitchenNotes = "",
        items = emptyList(),
    )
}
