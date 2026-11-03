package com.vaiinilla.app.ui.screens

import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.repository.AccountOrder
import com.vaiinilla.app.domain.repository.BoardOrder
import com.vaiinilla.app.domain.repository.BoardTable
import com.vaiinilla.app.domain.repository.CallReason
import com.vaiinilla.app.domain.repository.CallStatus
import com.vaiinilla.app.domain.repository.SpaceAccount
import com.vaiinilla.app.domain.repository.SpaceAvailability
import com.vaiinilla.app.domain.repository.SpaceAvailabilityState
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.domain.repository.TableSpace
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class WaiterTileStatusTest {
    private val now = Instant.parse("2026-11-02T20:00:00Z")
    private val mesa = TableSpace(3, "Mesa 3", "mesa")
    private val cancha = TableSpace(7, "Cancha Fútbol 7", "cancha")

    private fun order(
        id: String,
        state: OrderState,
    ) = BoardOrder(id, id.length, state, 1, "Ana", "Cheesecake", "2026-11-02T19:50:00Z")

    @Test
    fun `una mesa que llama dice desde cuando`() {
        val call =
            TableCall(
                "c1", mesa, null, CallReason.UTENSILIOS, CallStatus.PENDIENTE, "Ana", null,
                "2026-11-02T19:59:00Z", null, null, 1,
            )
        val status = waiterTileStatus(BoardTable(mesa, call, emptyList()), now)
        assertEquals("LLAMANDO · 1 min", status.label)
        assertEquals(TileTone.Call, status.tone)
    }

    @Test
    fun `lo listo gana a lo que va en cocina`() {
        val table = BoardTable(mesa, null, listOf(order("a", OrderState.READY), order("bb", OrderState.READY)))
        assertEquals("2 LISTOS", waiterTileStatus(table, now).label)
        val kitchen = BoardTable(mesa, null, listOf(order("a", OrderState.PREPARING)))
        assertEquals("EN COCINA", waiterTileStatus(kitchen, now).label)
    }

    @Test
    fun `una cancha con turno dice cuanto le queda`() {
        val availability =
            SpaceAvailability(
                space = cancha,
                state = SpaceAvailabilityState.OCUPADA,
                settled = true,
                endsAt = "2026-11-02T20:32:00Z",
                releasesAt = null,
                remainingSeconds = null,
                graceMinutes = 5,
                startedAt = "2026-11-02T19:00:00Z",
            )
        val status = waiterTileStatus(BoardTable(cancha, null, emptyList(), availability), now)
        assertEquals("QUEDAN 32 MIN", status.label)
        assertEquals(TileTone.Turn, status.tone)
    }

    @Test
    fun `sin nada es libre`() {
        assertEquals("LIBRE", waiterTileStatus(BoardTable(mesa, null, emptyList()), now).label)
    }

    @Test
    fun `la hoja junta lo del tablero con lo ya entregado y pone lo listo primero`() {
        val account =
            SpaceAccount(
                orders =
                    listOf(
                        AccountOrder("a", 12, "preparando", "214.00", "Ana", "Cheesecake", true, true),
                        AccountOrder("b", 13, "listo", "98.00", "Luis", "Crepa", true, true),
                        AccountOrder("c", 11, "entregado", "60.00", "Mesero", "2 cafés", true, true),
                    ),
                total = "372.00",
                pending = "372.00",
                paid = "0.00",
                settled = false,
            )
        val rows =
            waiterSheetOrders(
                listOf(order("a", OrderState.PREPARING), order("b", OrderState.READY)),
                account,
            )
        assertEquals(listOf("b", "a", "c"), rows.map { it.order.id })
        assertEquals(listOf(true, true, false), rows.map { it.live })
        assertEquals("214.00", rows[1].amount)
        assertEquals(OrderState.DELIVERED, rows[2].order.state)
    }
}
