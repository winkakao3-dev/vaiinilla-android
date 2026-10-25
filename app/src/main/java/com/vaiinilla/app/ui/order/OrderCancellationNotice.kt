package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.StripePaymentStatus

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
        note = cancellationMoneyNote(order),
    )
}

/** Qué pasó con el dinero de un pedido cancelado, con lo que el servidor deja ver (PEDIDO-06). */
internal fun cancellationMoneyNote(order: OrderDetail): String? =
    when {
        order.summary.payAtEnd -> "No se te cobrará: este pedido iba a tu cuenta."
        order.summary.paymentMethod == PaymentMethod.BALANCE -> "El importe regresó a tu saldo Vaiinilla."
        order.summary.paymentMethod == PaymentMethod.STRIPE ->
            when (order.payment?.status) {
                StripePaymentStatus.REFUND_PENDING, StripePaymentStatus.REFUNDING ->
                    "Tu reembolso a la tarjeta está en proceso. El banco puede tardar unos días en reflejarlo."
                StripePaymentStatus.REFUNDED ->
                    "Te reembolsamos a la tarjeta. El banco puede tardar unos días en reflejarlo."
                else -> null
            }
        else -> null
    }
