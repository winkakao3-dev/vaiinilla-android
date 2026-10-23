package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState

/** Quién canceló el pedido y por qué, para decírselo al cliente (igual que `cancellationNotice` de iOS). */
data class OrderCancellationNotice(
    val title: String,
    val reason: String?,
    val note: String?,
)

/** `null` si el pedido no está cancelado. */
fun orderCancellationNotice(order: OrderDetail): OrderCancellationNotice? {
    if (order.summary.state != OrderState.CANCELED) return null
    val by =
        when (order.summary.canceledByRole) {
            "cocina" -> "Cocina"
            "cajero" -> "Caja"
            "admin" -> "El establecimiento"
            else -> null
        }
    return OrderCancellationNotice(
        title = by?.let { "$it canceló tu pedido" } ?: "Pedido cancelado",
        reason =
            order.summary.cancelReason
                ?.trim()
                ?.takeIf { it.isNotEmpty() },
        note = if (order.summary.payAtEnd) "No se te cobrará: este pedido iba a tu cuenta." else null,
    )
}
