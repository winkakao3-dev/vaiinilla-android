package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.repository.BoardTable
import com.vaiinilla.app.domain.repository.CallStatus

enum class WaiterAlertKind { READY, CALL }

data class WaiterAlert(
    val key: String,
    val kind: WaiterAlertKind,
    val spaceId: Int,
    val title: String,
    val body: String,
)

/** Claves de todo lo que hoy merece aviso: pedidos listos y llamadas pendientes. */
fun waiterAlertKeys(tables: List<BoardTable>): Set<String> =
    buildSet {
        for (table in tables) {
            table.call?.takeIf { it.status == CallStatus.PENDIENTE }?.let { add("call:${it.id}") }
            for (order in table.orders) if (order.state == OrderState.READY) add("ready:${order.id}")
        }
    }

/** Avisos nuevos respecto a [seen]: pedidos recién listos primero, luego llamadas. */
fun newWaiterAlerts(
    tables: List<BoardTable>,
    seen: Set<String>,
): List<WaiterAlert> {
    val ready = mutableListOf<WaiterAlert>()
    val calls = mutableListOf<WaiterAlert>()
    for (table in tables) {
        val place = table.space.name
        for (order in table.orders) {
            val key = "ready:${order.id}"
            if (order.state != OrderState.READY || key in seen) continue
            ready +=
                WaiterAlert(
                    key = key,
                    kind = WaiterAlertKind.READY,
                    spaceId = table.space.id,
                    title = "Pedido #${order.folio} listo · llévalo a $place",
                    body = "${order.clientName ?: "Cliente"} · ${order.itemsSummary}",
                )
        }
        val call = table.call ?: continue
        val key = "call:${call.id}"
        if (call.status != CallStatus.PENDIENTE || key in seen) continue
        calls +=
            WaiterAlert(
                key = key,
                kind = WaiterAlertKind.CALL,
                spaceId = table.space.id,
                title = "$place está llamando",
                body = "${call.reason.staffLabel} · ${call.clientName ?: "Cliente"}",
            )
    }
    return ready + calls
}
