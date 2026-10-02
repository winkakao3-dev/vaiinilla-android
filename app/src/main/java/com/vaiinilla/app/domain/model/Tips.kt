package com.vaiinilla.app.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/** Porcentajes sugeridos de propina (docs/propinas.md del backend). */
val TipPercents = listOf(10, 15, 20)

/** Propina al cobrar: sin propina, un porcentaje del monto que se cobra o un monto libre. */
sealed interface TipChoice {
    data object None : TipChoice

    data class Percent(
        val percent: Int,
    ) : TipChoice

    data class Custom(
        val amount: String,
    ) : TipChoice
}

/** El monto de la propina, al centavo (medio centavo hacia arriba). Un monto libre inválido es 0. */
fun TipChoice.amountFor(base: BigDecimal): BigDecimal =
    when (this) {
        TipChoice.None -> BigDecimal.ZERO.setScale(2)
        is TipChoice.Percent -> base.multiply(BigDecimal(percent)).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        is TipChoice.Custom ->
            amount
                .trim()
                .toBigDecimalOrNull()
                ?.takeIf { it.signum() >= 0 && it.scale() <= 2 }
                ?.setScale(2)
                ?: BigDecimal.ZERO.setScale(2)
    }

/** Lo que se manda como `propina`: null si no hay propina. */
fun BigDecimal.asTipWire(): String? = takeIf { it.signum() > 0 }?.setScale(2)?.toPlainString()
