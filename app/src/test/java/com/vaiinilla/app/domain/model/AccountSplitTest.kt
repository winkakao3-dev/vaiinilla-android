package com.vaiinilla.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class AccountSplitTest {
    private fun money(value: String) = BigDecimal(value)

    @Test
    fun `parts round down and the last one pays the leftover cent`() {
        // $100.00 entre 3: 33.33, 33.33 y 33.34, como el backend.
        var remaining = money("100.00")
        val first = AccountSplit.partAmount(remaining, 3)
        assertEquals(money("33.33"), first)
        remaining -= first
        val second = AccountSplit.partAmount(remaining, 2)
        assertEquals(money("33.33"), second)
        remaining -= second
        val last = AccountSplit.partAmount(remaining, 1)
        assertEquals(money("33.34"), last)
        assertEquals(money("100.00"), first + second + last)
    }

    @Test
    fun `parts always add up to the cent`() {
        listOf("0.01", "0.05", "10.00", "99.99", "250.10", "1234.57").forEach { total ->
            (1..7).forEach { people ->
                var remaining = money(total)
                var paid = BigDecimal.ZERO.setScale(2)
                for (left in people downTo 1) {
                    val part = AccountSplit.partAmount(remaining, left)
                    paid += part
                    remaining -= part
                }
                assertEquals("$total entre $people", money(total), paid)
            }
        }
    }

    @Test
    fun `one part pays everything that remains`() {
        assertEquals(money("87.65"), AccountSplit.partAmount(money("87.65"), 1))
    }

    @Test
    fun `an amount must be positive and not exceed what remains`() {
        assertTrue(AccountSplit.isValidAmount(money("50.00"), money("200.00")))
        assertTrue(AccountSplit.isValidAmount(money("200.00"), money("200.00")))
        assertFalse(AccountSplit.isValidAmount(money("200.01"), money("200.00")))
        assertFalse(AccountSplit.isValidAmount(BigDecimal.ZERO, money("200.00")))
        assertFalse(AccountSplit.isValidAmount(null, money("200.00")))
    }
}
