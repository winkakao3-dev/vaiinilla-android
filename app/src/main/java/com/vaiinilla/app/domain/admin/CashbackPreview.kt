package com.vaiinilla.app.domain.admin

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Lo que el cliente ganaría de cashback con un pedido, con la misma regla que aplica el servidor al entregarlo
 * (`wallet_acreditar_cashback_entrega`): regla activa, vigencia, día de la semana (0 = domingo), horario (que puede
 * cruzar la medianoche) y el porcentaje sobre el total, redondeado a centavos. El carrito y la hoja de pago lo dicen
 * (24); con el cashback apagado no dicen nada.
 */
object CashbackPreview {
    /** El porcentaje que aplica en [now]; `null` si no hay regla o hoy no aplica. */
    fun percentAt(
        rule: CashbackRule?,
        now: ZonedDateTime,
    ): BigDecimal? {
        if (rule == null || !rule.active) return null
        val percent = rule.percent.toBigDecimalOrNull()?.takeIf { it.signum() > 0 } ?: return null
        val today = now.toLocalDate()
        rule.validFrom?.let { from -> parseDate(from)?.let { if (today.isBefore(it)) return null } }
        rule.validTo?.let { to -> parseDate(to)?.let { if (today.isAfter(it)) return null } }
        val day = now.dayOfWeek.value % 7
        if (!rule.activeDays.isNullOrEmpty() && day !in rule.activeDays) return null
        val start = rule.startTime?.let(StoreRules::time)
        val end = rule.endTime?.let(StoreRules::time)
        if (start != null && end != null) {
            val time = now.toLocalTime()
            val inside =
                when {
                    start < end -> time >= start && time < end
                    start > end -> time >= start || time < end
                    else -> false
                }
            if (!inside) return null
        }
        return percent
    }

    /** Lo que se ganaría con [total]; `null` si no se gana nada. */
    fun earned(
        rule: CashbackRule?,
        total: String,
        now: ZonedDateTime,
    ): BigDecimal? {
        val percent = percentAt(rule, now) ?: return null
        val amount = total.toBigDecimalOrNull() ?: return null
        return amount
            .multiply(percent)
            .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
            .takeIf { it.signum() > 0 }
    }

    private fun parseDate(raw: String): LocalDate? = runCatching { LocalDate.parse(raw.take(10)) }.getOrNull()
}
