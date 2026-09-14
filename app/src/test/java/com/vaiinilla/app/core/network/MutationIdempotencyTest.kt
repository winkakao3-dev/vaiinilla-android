package com.vaiinilla.app.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MutationIdempotencyTest {
    @Test
    fun `same logical transition produces the same key`() {
        val a = MutationIdempotency.orderTransition("ord-1", "listo", 7, null)
        val b = MutationIdempotency.orderTransition("ord-1", "listo", 7, null)

        assertEquals(a, b)
    }

    @Test
    fun `a different expected version produces a different key`() {
        val a = MutationIdempotency.orderTransition("ord-1", "listo", 7, null)
        val b = MutationIdempotency.orderTransition("ord-1", "listo", 8, null)

        assertNotEquals(a, b)
    }

    @Test
    fun `distinct operations never share a key`() {
        val transition = MutationIdempotency.orderTransition("ord-1", "listo", 7, null)
        val collection = MutationIdempotency.cashCollection("ord-1", "150.00", 7)

        assertNotEquals(transition, collection)
    }

    @Test
    fun `pickup token participates in the transition key`() {
        val without = MutationIdempotency.orderTransition("ord-1", "entregado", 3, null)
        val with = MutationIdempotency.orderTransition("ord-1", "entregado", 3, "tok-abc")

        assertNotEquals(without, with)
    }

    @Test
    fun `account deletion key is stable per uid and distinct across uids`() {
        val a = MutationIdempotency.accountDeletion("uid-1")
        val b = MutationIdempotency.accountDeletion("uid-1")
        val other = MutationIdempotency.accountDeletion("uid-2")

        assertEquals(a, b)
        assertNotEquals(a, other)
    }

    @Test
    fun `product image key follows image content`() {
        val image = "fake-image-bytes".toByteArray()
        val same = MutationIdempotency.productImage(9, image)
        val sameAgain = MutationIdempotency.productImage(9, image.copyOf())
        val different = MutationIdempotency.productImage(9, "other-bytes".toByteArray())
        val otherProduct = MutationIdempotency.productImage(10, image)

        assertEquals(same, sameAgain)
        assertNotEquals(same, different)
        assertNotEquals(same, otherProduct)
    }

    @Test
    fun `keys are valid uuids — the backend requires it`() {
        val uuidPattern = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

        assertTrue(uuidPattern.matches(MutationIdempotency.cashCollection("o", "1.00", 1)))
        assertTrue(uuidPattern.matches(MutationIdempotency.orderTransition("o", "listo", 1, null)))
        assertTrue(uuidPattern.matches(MutationIdempotency.accountDeletion("uid")))
    }
}
