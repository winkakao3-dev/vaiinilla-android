package com.vaiinilla.app.domain.repository

import com.vaiinilla.app.domain.model.OrderState
import org.junit.Assert.assertEquals
import org.junit.Test

class WaiterBoardAvailabilityTest {
    private val court = TableSpace(id = 7, name = "Cancha 1", type = "cancha")

    private fun availability(state: SpaceAvailabilityState) =
        SpaceAvailability(
            space = court,
            state = state,
            settled = true,
            endsAt = null,
            releasesAt = null,
            remainingSeconds = null,
            graceMinutes = 5,
        )

    private fun table(
        state: SpaceAvailabilityState?,
        orders: List<BoardOrder> = emptyList(),
    ) = BoardTable(
        space = court,
        call = null,
        orders = orders,
        availability = state?.let(::availability),
    )

    @Test
    fun `a busy court is not shown as free even without orders`() {
        assertEquals(TableState.ACTIVE, table(SpaceAvailabilityState.OCUPADA).tableState())
        assertEquals(TableState.ACTIVE, table(SpaceAvailabilityState.EN_GRACIA).tableState())
        assertEquals(TableState.ACTIVE, table(SpaceAvailabilityState.POR_COBRAR).tableState())
    }

    @Test
    fun `a free court, or a server without availability, stays free`() {
        assertEquals(TableState.FREE, table(SpaceAvailabilityState.LIBRE).tableState())
        assertEquals(TableState.FREE, table(null).tableState())
    }

    @Test
    fun `an order ready to deliver still outranks the court being busy`() {
        val ready =
            BoardOrder("o-1", 41, OrderState.READY, 1, "Ana", "1× Agua", "2026-09-29T20:00:00Z")
        assertEquals(TableState.READY, table(SpaceAvailabilityState.OCUPADA, listOf(ready)).tableState())
    }
}
