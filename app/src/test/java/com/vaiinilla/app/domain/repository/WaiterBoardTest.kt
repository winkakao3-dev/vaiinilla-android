package com.vaiinilla.app.domain.repository

import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderSpace
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WaiterBoardTest {
    private fun space(id: Int) = TableSpace(id = id, name = "Mesa $id", type = "mesa")

    private fun call(
        id: String,
        spaceId: Int,
        status: CallStatus = CallStatus.PENDIENTE,
        createdAt: String = "2026-09-26T08:00:00Z",
    ) = TableCall(
        id = id,
        space = space(spaceId),
        orderId = null,
        reason = CallReason.ATENCION,
        status = status,
        clientName = "Ana",
        takenBy = null,
        createdAt = createdAt,
        takenAt = null,
        closedAt = null,
        version = 1,
    )

    private fun order(
        id: String,
        state: OrderState,
    ) = BoardOrder(
        id = id,
        folio = 1,
        state = state,
        version = 1,
        clientName = "Ana",
        itemsSummary = "1× Taco",
        updatedAt = "2026-09-26T08:00:00Z",
    )

    private fun table(
        spaceId: Int,
        call: TableCall? = null,
        orders: List<BoardOrder> = emptyList(),
    ) = BoardTable(space = space(spaceId), call = call, orders = orders)

    private fun orderDetail(
        destination: OrderDestination = OrderDestination.IN_SPACE,
        space: OrderSpace? = OrderSpace(id = 4, name = "Mesa 4", type = "mesa"),
        state: OrderState = OrderState.READY,
    ) = OrderDetail(
        summary =
            OrderSummary(
                id = "order-1",
                folio = 1,
                operationalDate = "2026-09-26",
                state = state,
                paymentMethod = PaymentMethod.CASH,
                destination = destination,
                space = space,
                subtotal = "0.00",
                combinedSavings = "0.00",
                cashbackAwarded = "0.00",
                total = "0.00",
                version = 1,
                createdAt = "2026-09-26T08:00:00Z",
                updatedAt = "2026-09-26T08:00:00Z",
            ),
        user = null,
        kitchenNotes = "",
        items = emptyList(),
    )

    @Test
    fun `table state follows call over ready over active over free`() {
        assertEquals(TableState.CALL, table(1, call = call("c1", 1)).tableState())
        assertEquals(TableState.READY, table(1, orders = listOf(order("o1", OrderState.READY))).tableState())
        assertEquals(TableState.ACTIVE, table(1, orders = listOf(order("o1", OrderState.PREPARING))).tableState())
        assertEquals(TableState.FREE, table(1).tableState())
    }

    @Test
    fun `calls come first, oldest call first, then ready, active and free`() {
        val sorted =
            sortWaiterTables(
                listOf(
                    table(9),
                    table(3, orders = listOf(order("o3", OrderState.PREPARING))),
                    table(7, call = call("c7", 7, createdAt = "2026-09-26T08:02:00Z")),
                    table(12, orders = listOf(order("o12", OrderState.READY))),
                    table(4, call = call("c4", 4, createdAt = "2026-09-26T08:01:00Z")),
                ),
            )
        assertEquals(listOf(4, 7, 12, 3, 9), sorted.map { it.space.id })
    }

    @Test
    fun `a call on the way still ranks the table as calling`() {
        val sorted =
            sortWaiterTables(
                listOf(
                    table(12, orders = listOf(order("o12", OrderState.READY))),
                    table(4, call = call("c4", 4, status = CallStatus.EN_CAMINO)),
                ),
            )
        assertEquals(4, sorted.first().space.id)
        assertEquals(TableState.CALL, sorted.first().tableState())
    }

    @Test
    fun `canCallWaiter requires an in-space order with a table and not canceled`() {
        assertTrue(canCallWaiter(orderDetail()))
        assertFalse(canCallWaiter(orderDetail(destination = OrderDestination.TAKE_AWAY, space = null)))
        assertFalse(canCallWaiter(orderDetail(space = null)))
        assertFalse(canCallWaiter(orderDetail(state = OrderState.CANCELED)))
    }

    @Test
    fun `unknown call wire values fail instead of pretending a state`() {
        val reason =
            runCatching { CallReason.fromWireValue("desconocido") }.exceptionOrNull()
        val status =
            runCatching { CallStatus.fromWireValue("desconocido") }.exceptionOrNull()
        assertTrue(reason is IllegalArgumentException)
        assertTrue(status is IllegalArgumentException)
    }
}
