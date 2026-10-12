package com.vaiinilla.app.domain.model

/**
 * El aviso tras quitar un artículo: qué pasa con el dinero. Una devolución manual
 * (terminal, saldo o mezcla) es una tarea de Caja con método y monto, no un detalle.
 * Mismos textos que el panel web (`itemRejectionNotice`).
 */
fun itemRejectionNotice(
    order: OrderDetail,
    name: String,
    outcome: ItemRejectionOutcome,
): String {
    val base = "Se quitó $name del pedido ${order.summary.folio}. El cliente verá el motivo."
    if (outcome.amount == ZERO_AMOUNT || outcome.refundMethod == ItemRefundMethod.NONE) return base
    val monto = "\$${outcome.amount} MXN"
    return when (outcome.refundMethod) {
        ItemRefundMethod.CASH -> "$base Entrega $monto del cajón al cliente."
        ItemRefundMethod.STRIPE -> "$base Se reembolsan $monto a su tarjeta."
        ItemRefundMethod.MANUAL -> {
            val metodo = (outcome.manualRefundOriginalMethod ?: "otro medio").replaceFirst("+", " y ")
            "$base Devolución pendiente: $monto (se pagó con $metodo). Caja debe entregarla y confirmarla."
        }
        ItemRefundMethod.NONE -> base
    }
}

private const val ZERO_AMOUNT = "0.00"
