package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.CreateOrderItem
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class OrderIdempotencyTest {
    @Test
    fun `identical cart content produces the same fingerprint`() {
        val a = cartRequest(quantity = 2, optionIds = listOf(310, 305))
        val b = cartRequest(quantity = 2, optionIds = listOf(305, 310)) // different insertion order

        assertEquals(createOrderFingerprint(a), createOrderFingerprint(b))
    }

    @Test
    fun `changing the quantity changes the fingerprint`() {
        val a = cartRequest(quantity = 1)
        val b = cartRequest(quantity = 2)

        assertNotEquals(createOrderFingerprint(a), createOrderFingerprint(b))
    }

    @Test
    fun `changing the selected options changes the fingerprint`() {
        val a = cartRequest(optionIds = listOf(310))
        val b = cartRequest(optionIds = listOf(311))

        assertNotEquals(createOrderFingerprint(a), createOrderFingerprint(b))
    }

    @Test
    fun `changing the payment method changes the fingerprint`() {
        val a = cartRequest(paymentMethod = PaymentMethod.CASH)
        val b = cartRequest(paymentMethod = PaymentMethod.STRIPE)

        assertNotEquals(createOrderFingerprint(a), createOrderFingerprint(b))
    }

    @Test
    fun `resolveIdempotencyKey reuses the in-memory key over everything else`() {
        val key =
            resolveIdempotencyKey(
                inMemoryKey = "in-memory",
                persistedKey = "persisted",
                generate = { error("must not generate when an in-memory key exists") },
            )

        assertEquals("in-memory", key)
    }

    @Test
    fun `resolveIdempotencyKey falls back to the persisted key when memory is empty`() {
        val key =
            resolveIdempotencyKey(
                inMemoryKey = null,
                persistedKey = "persisted",
                generate = { error("must not generate when a persisted key exists") },
            )

        assertEquals("persisted", key)
    }

    @Test
    fun `resolveIdempotencyKey generates a fresh key only when both are absent`() {
        val key =
            resolveIdempotencyKey(
                inMemoryKey = null,
                persistedKey = null,
                generate = { "fresh" },
            )

        assertEquals("fresh", key)
    }

    private fun cartRequest(
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        quantity: Int = 1,
        optionIds: List<Int> = listOf(310, 314, 317),
    ) = CreateOrderRequest(
        paymentMethod = paymentMethod,
        destination = OrderDestination.TAKE_AWAY,
        spaceId = null,
        kitchenNotes = "Salsa aparte",
        items = listOf(CreateOrderItem(productId = 103, quantity = quantity, optionIds = optionIds)),
    )
}
