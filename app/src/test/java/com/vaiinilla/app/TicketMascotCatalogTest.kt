package com.vaiinilla.app

import com.vaiinilla.app.ui.screens.TicketMascotCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TicketMascotCatalogTest {
    @Test
    fun testSameOrderAlwaysGetsSameMascot() {
        val orderId = "9f4c1e2a-0000-4000-8000-000000000001"
        assertEquals(TicketMascotCatalog.drawableFor(orderId), TicketMascotCatalog.drawableFor(orderId))
    }

    @Test
    fun testEveryMascotCanAppear() {
        val seen = (1..500).map { TicketMascotCatalog.drawableFor("order-$it") }.toSet()
        assertEquals(TicketMascotCatalog.mascots.size, seen.size)
    }

    @Test
    fun testDistributionIsApproximatelyUniform() {
        val random = Random(42)
        val counts = mutableMapOf<Int, Int>()
        repeat(20_000) {
            val orderId = "${random.nextInt()}-${random.nextInt()}"
            counts.merge(TicketMascotCatalog.drawableFor(orderId), 1, Int::plus)
        }
        assertEquals(TicketMascotCatalog.mascots.size, counts.size)
        counts.values.forEach { count ->
            assertTrue("Unexpected mascot count: $count", count in 3_600..4_400)
        }
    }
}
