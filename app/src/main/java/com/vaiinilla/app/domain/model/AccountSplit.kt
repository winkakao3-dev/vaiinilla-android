package com.vaiinilla.app.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Partes iguales de una cuenta (docs/dividir-cuenta.md): cada parte es lo que falta
 * entre las personas que faltan, redondeado hacia abajo al centavo. Con una sola parte
 * se paga todo lo que queda, así que el último centavo lo paga quien cierra.
 * Igual que el backend y el panel.
 */
object AccountSplit {
    fun partAmount(
        remaining: BigDecimal,
        parts: Int,
    ): BigDecimal {
        val total = remaining.setScale(2, RoundingMode.DOWN)
        if (parts <= 1) return total
        return total.divide(BigDecimal(parts), 2, RoundingMode.DOWN)
    }

    /** Un abono por monto válido: mayor que cero y sin pasar de lo que falta. */
    fun isValidAmount(
        amount: BigDecimal?,
        remaining: BigDecimal,
    ): Boolean = amount != null && amount.signum() == 1 && amount <= remaining
}
