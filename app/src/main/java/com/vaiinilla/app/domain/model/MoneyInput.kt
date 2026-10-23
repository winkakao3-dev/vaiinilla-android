package com.vaiinilla.app.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Lo que la gente escribe en un campo de dinero ("200", "12.5", "12,5") y lo que el backend espera
 * ("200.00", "12.50"). Muchos teclados en español ponen coma en el teclado decimal: se toma como punto.
 */
object MoneyInput {
    /**
     * Deja dígitos y un solo separador decimal con hasta dos decimales, mientras se escribe. Con punto y coma
     * a la vez ("1,250.50") las comas son de miles y se quitan.
     */
    fun sanitize(
        raw: String,
        maxIntegerDigits: Int = 8,
    ): String {
        val unified = if (raw.contains('.')) raw.replace(",", "") else raw.replace(',', '.')
        val clean = unified.filter { it.isDigit() || it == '.' }
        val dot = clean.indexOf('.')
        if (dot < 0) return clean.take(maxIntegerDigits)
        val decimals = clean.substring(dot + 1).replace(".", "").take(2)
        return clean.substring(0, dot).take(maxIntegerDigits) + "." + decimals
    }

    /** El monto escrito como número; null si está vacío o no es un monto. */
    fun parse(text: String): BigDecimal? {
        val clean = sanitize(text.trim(), maxIntegerDigits = Int.MAX_VALUE)
        if (clean.none { it.isDigit() }) return null
        val normalized = if (clean.startsWith(".")) "0$clean" else clean
        return normalized.removeSuffix(".").toBigDecimalOrNull()?.takeIf { it.signum() >= 0 }
    }

    /** "200" → "200.00", "12,5" → "12.50": el formato con dos decimales del contrato; null si no hay monto. */
    fun toWire(text: String): String? = parse(text)?.setScale(2, RoundingMode.DOWN)?.toPlainString()
}
