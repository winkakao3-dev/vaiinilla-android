package com.vaiinilla.app.ui.components

import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.PaymentMethod

fun paymentMethodLabel(method: PaymentMethod): String =
    when (method) {
        PaymentMethod.CASH -> "Efectivo"
        PaymentMethod.BALANCE -> "Saldo"
        PaymentMethod.STRIPE -> "Tarjeta"
    }

/** Un método como lo manda el servidor en los pagos (`efectivo`, `terminal`, `saldo`, `stripe`, `mixto`). */
fun paymentWireLabel(method: String): String =
    when (method) {
        "efectivo" -> "Efectivo"
        "terminal" -> "Terminal"
        "saldo" -> "Saldo"
        "stripe" -> "Tarjeta"
        "mixto" -> "Mixto"
        else -> method.replaceFirstChar { it.uppercase() }
    }

/**
 * Con qué se cobró de verdad: «Terminal», o «Mixto · Efectivo $66.67 + Terminal $33.33» si hubo partes con
 * métodos distintos. `null` si el servidor no lo manda o aún no hay pagos.
 */
fun chargedPaymentLabel(summary: OrderSummary): String? {
    val method = summary.chargedMethod ?: return null
    if (method != "mixto") return paymentWireLabel(method)
    val parts =
        summary.paymentsByMethod.joinToString(" + ") { "${paymentWireLabel(it.method)} ${moneyLabel(it.amount)}" }
    return if (parts.isEmpty()) "Mixto" else "Mixto · $parts"
}

/** Cómo se paga un pedido: un pedido a la cuenta se paga al final, con toda la cuenta del espacio. */
fun orderPaymentLabel(summary: OrderSummary): String =
    when {
        summary.payAtEnd && summary.paymentPending -> "Pagar al final"
        summary.payAtEnd -> "Cuenta pagada"
        else -> chargedPaymentLabel(summary) ?: paymentMethodLabel(summary.paymentMethod)
    }

/** El estado del pedido dicho al cliente: un pedido a la cuenta está "recibido", no "cobrado". */
fun orderStateLabel(summary: OrderSummary): String =
    if (summary.payAtEnd && summary.state == OrderState.PAID) "Recibido" else summary.state.label

/**
 * ¿El cliente tiene que mostrar su QR para recibir el pedido? No, si el backend lo dispensa para este
 * pedido (`entrega_sin_qr`); para llevar, siempre; en un espacio solo si el establecimiento pide el QR.
 */
fun OrderDetail.needsPickupQr(deliveryRequiresQr: Boolean): Boolean =
    !summary.deliveryWithoutQr && (summary.destination != OrderDestination.IN_SPACE || deliveryRequiresQr)
