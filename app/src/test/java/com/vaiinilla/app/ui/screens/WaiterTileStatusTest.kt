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

    @Test
    fun `al cobrar solo una parte el boton dice cuanto falta y de quien`() {
        val payable =
            listOf(
                AccountOrder("a", 12, "listo", "214.00", "Ana", "Cheesecake", true, true),
                AccountOrder("b", 17, "listo", "95.00", "Ana", "Pastel", true, true),
                AccountOrder("c", 13, "listo", "98.00", "Luis", "Crepa", true, true),
                AccountOrder("d", 11, "listo", "60.00", "Luis", "Cafés", true, true),
            )
        assertEquals("faltan $158 de Luis", pendingAfterPartial(payable, setOf("a", "b")))
        assertEquals("Ana", partialPayer(payable, setOf("a", "b")))
        assertEquals(null, pendingAfterPartial(payable, setOf("a", "b", "c", "d")))
        assertEquals(null, partialPayer(payable, setOf("a", "c")))
    }

    @Test
    fun `lo que sigue pone primero quien llama, luego lo listo, el turno por acabar y lo que falta cobrar`() {
        val call =
            TableCall(
                "c1", mesa, null, CallReason.UTENSILIOS, CallStatus.PENDIENTE, "Ana", null,
                "2026-11-02T19:59:00Z", null, null, 1,
            )
        val ending =
            SpaceAvailability(
                space = cancha,
                state = SpaceAvailabilityState.OCUPADA,
                settled = true,
                endsAt = "2026-11-02T20:10:00Z",
                releasesAt = null,
                remainingSeconds = null,
                graceMinutes = 5,
                startedAt = "2026-11-02T19:00:00Z",
            )
        val mesa1 = TableSpace(1, "Mesa 1", "mesa")
        val mesa2 = TableSpace(2, "Mesa 2", "mesa")
        val toCollect = ending.copy(space = mesa2, state = SpaceAvailabilityState.POR_COBRAR, endsAt = null)
        val items =
            waiterNextUp(
                listOf(
                    BoardTable(mesa2, null, emptyList(), toCollect),
                    BoardTable(cancha, null, emptyList(), ending),
                    BoardTable(mesa1, null, listOf(order("a", OrderState.READY))),
                    BoardTable(mesa, call, emptyList()),
                ),
                now,
            )
        assertEquals(
            listOf("Pide cubiertos o servilletas", "Llevar #1 a Mesa 1", "A Cancha Fútbol 7 le quedan 10 min", "Mesa 2 ya terminó"),
            items.map { it.title },
        )
        assertEquals(listOf("Voy", "Entregar", "Ver", "Cobrar"), items.map { it.action })
        assertEquals("Mesa 3 · hace 1 min · Ana", items.first().detail)
    }

    @Test
    fun `la llamada mas vieja va arriba y dice para que llaman`() {
        val mesa1 = TableSpace(1, "Mesa 1", "mesa")
        val nueva =
            TableCall("c2", mesa1, null, CallReason.ATENCION, CallStatus.PENDIENTE, null, null, "2026-11-02T19:59:30Z", null, null, 1)
        val vieja =
            TableCall("c1", mesa, null, CallReason.CUENTA, CallStatus.PENDIENTE, null, null, "2026-11-02T19:58:00Z", null, null, 1)
        val items = waiterNextUp(listOf(BoardTable(mesa1, nueva, emptyList()), BoardTable(mesa, vieja, emptyList())), now)
        assertEquals(listOf("Pide la cuenta", "Necesita atención"), items.map { it.title })
        assertEquals(listOf(CallReason.CUENTA, CallReason.ATENCION), items.map { it.reason })
    }
}
