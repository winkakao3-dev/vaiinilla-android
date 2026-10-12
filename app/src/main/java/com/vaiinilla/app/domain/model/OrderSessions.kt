@file:Suppress("ktlint:standard:filename")

package com.vaiinilla.app.domain.model

/** Una fila de "Anteriores": un pedido suelto o todos los pedidos de una sesión de mesa. */
sealed interface PastOrderEntry {
    val key: String

    data class Single(
        val order: OrderDetail,
    ) : PastOrderEntry {
        override val key: String get() = order.summary.id
    }

    data class TableSession(
        val sessionId: String,
        val orders: List<OrderDetail>,
    ) : PastOrderEntry {
        override val key: String get() = "session-$sessionId"
    }
}

/**
 * Agrupa los pedidos por sesión de mesa (`sesion_espacio_id`) en el orden en que aparece cada sesión.
 * Los pedidos sin sesión (para llevar) salen sueltos. Un solo pedido con sesión también forma grupo.
 */
fun groupOrdersBySession(orders: List<OrderDetail>): List<PastOrderEntry> {
    val bySession = orders.groupBy { it.summary.tableSessionId }
    val emitted = mutableSetOf<String>()
    return orders.mapNotNull { order ->
        val sessionId = order.summary.tableSessionId
        when {
            sessionId == null -> PastOrderEntry.Single(order)
            emitted.add(sessionId) -> PastOrderEntry.TableSession(sessionId, bySession.getValue(sessionId))
            else -> null
        }
    }
}
