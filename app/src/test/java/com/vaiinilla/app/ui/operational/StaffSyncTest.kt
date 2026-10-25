package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.cashAmountDue
import com.vaiinilla.app.domain.model.needsCashCollection
import com.vaiinilla.app.domain.repository.BoardTable
import com.vaiinilla.app.domain.repository.TableSpace
import com.vaiinilla.app.domain.repository.sortWaiterTables
import com.vaiinilla.app.ui.screens.kitchenActiveOrder
import com.vaiinilla.app.ui.screens.kitchenQueue
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StaffSyncTest {
    private fun order(
        id: String,
        state: OrderState = OrderState.PENDING_PAYMENT,
        method: PaymentMethod = PaymentMethod.CASH,
        version: Int = 1,
        createdAt: String = "2026-10-24T10:00:00Z",
        balanceDue: String? = null,
    ): OrderDetail {
        val base = ScreenshotFixtures.sampleOrder(state = state, paymentMethod = method)
        return base.copy(
            summary =
                base.summary.copy(
                    id = id,
                    version = version,
                    createdAt = createdAt,
                    balanceDue = balanceDue,
                ),
        )
    }

    @Test
    fun `reconcile drops what the server no longer lists`() {
        val current = listOf(order("a"), order("b"))

        val result = reconcileOrders(current, listOf(order("b")))

        assertEquals(listOf("b"), result.map { it.summary.id })
    }

    @Test
    fun `reconcile keeps the local copy when it is newer than the list`() {
        val current = listOf(order("a", version = 5))

        val result = reconcileOrders(current, listOf(order("a", version = 4)))

        assertEquals(5, result.single().summary.version)
    }

    @Test
    fun `a full page is never used to retire orders`() {
        val current = listOf(order("gone"))
        val page = (1..FULL_LIST_LIMIT).map { order("p$it") }

        val result = reconcileOrders(current, page)

        assertTrue(result.any { it.summary.id == "gone" })
        assertEquals(FULL_LIST_LIMIT + 1, result.size)
    }

    @Test
    fun `merge adds and updates without removing`() {
        val result = mergeOrders(listOf(order("a", version = 1)), listOf(order("a", version = 2), order("b")))

        assertEquals(setOf("a", "b"), result.map { it.summary.id }.toSet())
        assertEquals(2, result.first { it.summary.id == "a" }.summary.version)
    }

    @Test
    fun `cash order started by the kitchen is still collectable while it has a balance`() {
        val preparing = order("a", OrderState.PREPARING, balanceDue = "85.00").summary

        assertTrue(preparing.needsCashCollection)
        assertEquals("85.00", preparing.cashAmountDue)
    }

    @Test
    fun `paid or card orders do not ask for cash`() {
        assertFalse(order("a", OrderState.PREPARING, balanceDue = "0.00").summary.needsCashCollection)
        assertFalse(order("b", OrderState.PREPARING).summary.needsCashCollection)
        assertFalse(order("c", method = PaymentMethod.STRIPE).summary.needsCashCollection)
        assertTrue(order("d").summary.needsCashCollection)
    }

    @Test
    fun `kitchen sees cash orders before payment but not unpaid card orders`() {
        val queue =
            kitchenQueue(
                listOf(
                    order("card", method = PaymentMethod.STRIPE, createdAt = "2026-10-24T09:00:00Z"),
                    order("late", OrderState.PAID, createdAt = "2026-10-24T11:00:00Z"),
                    order("early", createdAt = "2026-10-24T10:00:00Z"),
                ),
            )

        assertEquals(listOf("early", "late"), queue.map { it.summary.id })
    }

    @Test
    fun `kitchen keeps working the oldest pending order when the chosen one is ready`() {
        val queue = kitchenQueue(listOf(order("ready", OrderState.READY), order("next", OrderState.PAID)))

        assertEquals("next", kitchenActiveOrder(queue, selectedOrderId = "ready")?.summary?.id)
    }

    @Test
    fun `alerts name each order that needs the role once`() {
        val orders = listOf(order("cash"), order("paid", OrderState.PAID), order("prep", OrderState.PREPARING))

        assertEquals(
            listOf(StaffAlertKind.CASH_DUE),
            staffAlertsFor(OperationalRole.CASHIER, orders).map { it.kind },
        )
        assertEquals(
            setOf("cash", "paid"),
            staffAlertsFor(OperationalRole.KITCHEN, orders).map { it.orderId }.toSet(),
        )
    }

    @Test
    fun `tables sort by number inside the name`() {
        fun table(name: String) =
            BoardTable(
                space = TableSpace(id = name.hashCode(), name = name, type = "mesa"),
                call = null,
                orders = emptyList(),
            )

        val sorted = sortWaiterTables(listOf("Mesa 10", "Mesa 2", "Mesa 1").map(::table))

        assertEquals(listOf("Mesa 1", "Mesa 2", "Mesa 10"), sorted.map { it.space.name })
    }
}
