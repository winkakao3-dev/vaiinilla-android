package com.vaiinilla.app.ui.components

import com.vaiinilla.app.domain.model.PaymentMethod

fun paymentMethodLabel(method: PaymentMethod): String = when (method) {
    PaymentMethod.CASH -> "Efectivo"
    PaymentMethod.BALANCE -> "Saldo"
    PaymentMethod.CARD -> "Tarjeta"
}
