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

/** Cómo se paga un pedido: un pedido a la cuenta se paga al final, con toda la cuenta del espacio. */
fun orderPaymentLabel(summary: OrderSummary): String =
    when {
        summary.payAtEnd && summary.paymentPending -> "Pagar al final"
        summary.payAtEnd -> "Cuenta pagada"
        else -> paymentMethodLabel(summary.paymentMethod)
    }

/** El estado del pedido dicho al cliente: un pedido a la cuenta está "recibido", no "cobrado". */
fun orderStateLabel(summary: OrderSummary): String =
    if (summary.payAtEnd && summary.state == OrderState.PAID) "Recibido" else summary.state.label

/**
 * ¿El cliente tiene que mostrar su QR para recibir el pedido? Para llevar, siempre; en un espacio
 * solo si el establecimiento no dispensa el QR al entregar.
 */
fun OrderDetail.needsPickupQr(deliveryRequiresQr: Boolean): Boolean =
    summary.destination != OrderDestination.IN_SPACE || deliveryRequiresQr
