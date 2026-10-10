package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.CreateOrderItem
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderOrigin
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
    fun `changing a line note changes the fingerprint`() {
        val a = cartRequest(notes = "sin cebolla")
        val b = cartRequest(notes = "sin cebolla, salsa aparte")

        assertNotEquals(createOrderFingerprint(a), createOrderFingerprint(b))
    }

    @Test
    fun `identical line notes produce the same fingerprint`() {
        val a = cartRequest(notes = "sin cebolla")
        val b = cartRequest(notes = "sin cebolla")

        assertEquals(createOrderFingerprint(a), createOrderFingerprint(b))
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

    @Test
    fun `counter orders with a different customer name get a different fingerprint`() {
        val ana = counterRequest(customerName = "Ana")
        val luis = counterRequest(customerName = "Luis")

        assertNotEquals(createOrderFingerprint(ana), createOrderFingerprint(luis))
    }

    @Test
    fun `counter order fingerprint differs from the same app order`() {
        val app = cartRequest()
        val counter = counterRequest(customerName = null)

        assertNotEquals(createOrderFingerprint(app), createOrderFingerprint(counter))
    }

    @Test
    fun `counterOrderKeyFor reuses the key when the order is unchanged`() {
        val first = counterOrderKeyFor(previous = null, fingerprint = "fp", generate = { "k1" })
        val retry = counterOrderKeyFor(previous = first, fingerprint = "fp", generate = { error("no reuse") })

        assertEquals("k1", retry.key)
    }

    @Test
    fun `counterOrderKeyFor generates a new key when the order changed`() {
        val first = CounterOrderKey(fingerprint = "fp", key = "k1")
        val changed = counterOrderKeyFor(previous = first, fingerprint = "fp-2", generate = { "k2" })

        assertEquals("k2", changed.key)
    }

    private fun counterRequest(customerName: String?) =
        CreateOrderRequest(
            paymentMethod = PaymentMethod.CASH,
            destination = OrderDestination.TAKE_AWAY,
            spaceId = null,
            kitchenNotes = "",
            items = listOf(CreateOrderItem(productId = 103, quantity = 1, optionIds = listOf(310))),
            origin = OrderOrigin.MOSTRADOR,
            customerName = customerName,
        )

    private fun cartRequest(
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        quantity: Int = 1,
        optionIds: List<Int> = listOf(310, 314, 317),
        notes: String = "",
    ) = CreateOrderRequest(
        paymentMethod = paymentMethod,
        destination = OrderDestination.TAKE_AWAY,
        spaceId = null,
        kitchenNotes = "Salsa aparte",
        items = listOf(CreateOrderItem(productId = 103, quantity = quantity, optionIds = optionIds, notes = notes)),
    )
}
