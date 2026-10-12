package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.repository.BoardOrder
import com.vaiinilla.app.domain.repository.BoardTable
import com.vaiinilla.app.domain.repository.CallReason
import com.vaiinilla.app.domain.repository.CallStatus
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.domain.repository.TableSpace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WaiterAlertsTest {
    private fun table(
        id: Int,
        name: String,
        orders: List<BoardOrder> = emptyList(),
        call: TableCall? = null,
    ) = BoardTable(space = TableSpace(id = id, name = name, type = "mesa"), call = call, orders = orders)

    private fun order(
        id: String,
        folio: Int,
        state: OrderState,
    ) = BoardOrder(
        id = id,
        folio = folio,
        state = state,
        version = 1,
        clientName = "Ana",
        itemsSummary = "2× Tacos",
        updatedAt = "2026-10-11T10:00:00Z",
    )

    private fun call(
        id: String,
        status: CallStatus,
    ) = TableCall(
        id = id,
        space = TableSpace(id = 3, name = "Cancha 1", type = "cancha"),
        orderId = null,
        reason = CallReason.ATENCION,
        status = status,
        clientName = "Luis",
        takenBy = null,
        createdAt = "2026-10-11T10:00:00Z",
        takenAt = null,
        closedAt = null,
        version = 1,
    )

    @Test
    fun `avisa del pedido que cocina acaba de marcar listo y a que mesa llevarlo`() {
        val before = listOf(table(2, "Mesa 2", orders = listOf(order("a", 9, OrderState.PREPARING))))
        val after = listOf(table(2, "Mesa 2", orders = listOf(order("a", 9, OrderState.READY))))

        assertTrue(newWaiterAlerts(after, emptySet()).isNotEmpty())
        val alert = newWaiterAlerts(after, waiterAlertKeys(before)).single()

        assertEquals(WaiterAlertKind.READY, alert.kind)
        assertEquals(2, alert.spaceId)
        assertEquals("Pedido #9 listo · llévalo a Mesa 2", alert.title)
        assertEquals("Ana · 2× Tacos", alert.body)
    }

    @Test
    fun `no repite un aviso ya visto`() {
        val tables = listOf(table(2, "Mesa 2", orders = listOf(order("a", 9, OrderState.READY))))

        assertTrue(newWaiterAlerts(tables, waiterAlertKeys(tables)).isEmpty())
    }

    @Test
    fun `avisa de una llamada nueva pendiente y no de una que ya tomaron`() {
        val pending = listOf(table(3, "Cancha 1", call = call("c1", CallStatus.PENDIENTE)))
        val taken = listOf(table(3, "Cancha 1", call = call("c1", CallStatus.EN_CAMINO)))

        val alert = newWaiterAlerts(pending, emptySet()).single()
        assertEquals(WaiterAlertKind.CALL, alert.kind)
        assertEquals("Cancha 1 está llamando", alert.title)
        assertEquals("Necesita atención · Luis", alert.body)
        assertTrue(newWaiterAlerts(taken, emptySet()).isEmpty())
    }

    @Test
    fun `los pedidos listos van antes que las llamadas`() {
        val tables =
            listOf(
                table(3, "Cancha 1", call = call("c1", CallStatus.PENDIENTE)),
                table(2, "Mesa 2", orders = listOf(order("a", 9, OrderState.READY))),
            )

        val kinds = newWaiterAlerts(tables, emptySet()).map { it.kind }

        assertEquals(listOf(WaiterAlertKind.READY, WaiterAlertKind.CALL), kinds)
    }

    @Test
    fun `las claves solo incluyen pedidos listos y llamadas pendientes`() {
        val tables =
            listOf(
                table(
                    2,
                    "Mesa 2",
                    orders = listOf(order("a", 9, OrderState.READY), order("b", 10, OrderState.PREPARING)),
                    call = call("c1", CallStatus.ATENDIDA),
                ),
            )

        assertEquals(setOf("ready:a"), waiterAlertKeys(tables))
    }
}
