package com.vaiinilla.app.data.guest

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingCreateKeyTtlTest {
    private val hour = 60L * 60 * 1000

    @Test
    fun `a key saved moments ago can still replay a lost response`() {
        assertTrue(isPendingCreateKeyFresh(savedAtMs = 1_000_000, nowMs = 1_000_000 + 2 * hour))
    }

    @Test
    fun `the same cart a day later is a new order`() {
        assertFalse(isPendingCreateKeyFresh(savedAtMs = 1_000_000, nowMs = 1_000_000 + 25 * hour))
    }

    @Test
    fun `a key from an older app version without a date is treated as expired`() {
        assertFalse(isPendingCreateKeyFresh(savedAtMs = 0, nowMs = 5 * hour))
    }

    @Test
    fun `a clock that went backwards expires the key instead of keeping it forever`() {
        assertFalse(isPendingCreateKeyFresh(savedAtMs = 10 * hour, nowMs = 2 * hour))
    }
}
