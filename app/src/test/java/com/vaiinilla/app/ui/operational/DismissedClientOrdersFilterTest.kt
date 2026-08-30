package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DismissedClientOrdersFilterTest {
    private val first = ScreenshotFixtures.sampleOrder()
    private val second = first.copy(summary = first.summary.copy(id = "second-order", folio = 9999))

    @Test
    fun `client list hides locally dismissed orders`() {
        val visible =
            filterDismissedClientOrders(
                role = OperationalRole.CLIENT,
                orders = listOf(first, second),
                dismissedOrderIds = setOf(first.summary.id),
            )

        assertEquals(listOf(second), visible)
    }

    @Test
    fun `menu never falls back to an older order after latest is dismissed`() {
        val older =
            first.copy(
                summary =
                    first.summary.copy(
                        id = "older-order",
                        createdAt = "2026-08-29T12:00:00Z",
                    ),
            )
        val latest =
            second.copy(
                summary =
                    second.summary.copy(
                        id = "latest-order",
                        createdAt = "2026-08-29T13:00:00Z",
                    ),
            )

        val resolvedLatest = resolveLatestClientOrder(previous = latest, incoming = listOf(older))
        val menuOrder = visibleLatestMenuOrder(resolvedLatest, setOf(latest.summary.id))

        assertEquals(latest, resolvedLatest)
        assertEquals(null, menuOrder)
    }

    @Test
    fun `staff lists never inherit client dismissals`() {
        val visible =
            filterDismissedClientOrders(
                role = OperationalRole.CASHIER,
                orders = listOf(first, second),
                dismissedOrderIds = setOf(first.summary.id, second.summary.id),
            )

        assertEquals(2, visible.size)
        assertTrue(first in visible)
        assertTrue(second in visible)
    }
}
