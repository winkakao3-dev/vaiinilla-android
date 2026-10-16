package com.vaiinilla.app.domain.admin

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Las mismas reglas que el backend aplica al guardar un producto, para avisar antes de enviarlo en lugar de
 * esperar un 400.
 */
object AdminProductRules {
    const val MAX_GROUPS = 12
    const val MAX_OPTIONS = 20
    const val MAX_MINUTES = 240

    /** "55" → "55.00", "12.5" → "12.50"; null si no es un monto válido. */
    fun money(raw: String): String? {
        val text = raw.trim().removePrefix("$").replace(",", "")
        if (!MONEY.matches(text)) return null
        return BigDecimal(text).setScale(2, RoundingMode.UNNECESSARY).toPlainString()
    }

    // Hasta 8 enteros y 2 decimales, como `DINERO` en el backend.
    private val MONEY = Regex("""^\d{1,8}(\.\d{1,2})?$""")

    /** Lo que impide guardar, en palabras para quien administra; null si se puede. */
    fun problem(input: AdminProductInput): String? {
        if (input.name.isBlank()) return "Escribe el nombre del producto."
        if (input.name.trim().length > 120) return "El nombre admite hasta 120 letras."
        val price = money(input.counterPrice) ?: return "Escribe el precio en pesos, por ejemplo 55 o 55.50."
        if (BigDecimal(price).signum() == 0) return "El precio debe ser mayor a cero."
        if (input.estimatedTimeMinutes !in
            0..MAX_MINUTES
        ) {
            return "El tiempo de preparación va de 0 a $MAX_MINUTES minutos."
        }
        if (input.optionGroups.size > MAX_GROUPS) return "Un producto admite hasta $MAX_GROUPS grupos de opciones."
        if (duplicated(input.optionGroups.map { it.name })) return "Hay dos grupos de opciones con el mismo nombre."
        input.optionGroups.forEach { group ->
            val label = group.name.trim().ifEmpty { "sin nombre" }
            if (group.name.isBlank()) return "Ponle nombre a cada grupo de opciones."
            if (group.options.isEmpty() || group.options.size > MAX_OPTIONS) {
                return "El grupo \"$label\" necesita entre 1 y $MAX_OPTIONS opciones."
            }
            if (group.options.any { it.name.isBlank() }) return "Ponle nombre a cada opción de \"$label\"."
            if (duplicated(group.options.map { it.name })) return "\"$label\" tiene dos opciones con el mismo nombre."
            if (group.options.any { money(it.extraPrice) == null }) {
                return "Revisa el costo extra de las opciones de \"$label\"."
            }
            if (group.maximumSelections < 1 ||
                group.minimumSelections < 0 ||
                group.minimumSelections > group.maximumSelections ||
                group.maximumSelections > group.options.size
            ) {
                return "En \"$label\", el mínimo y el máximo deben quedar entre 0 y ${group.options.size}."
            }
        }
        return null
    }

    /** El producto listo para enviar: montos con dos decimales y textos recortados. */
    fun normalized(input: AdminProductInput): AdminProductInput =
        input.copy(
            counterPrice = money(input.counterPrice) ?: input.counterPrice,
            optionGroups =
                input.optionGroups.map { group ->
                    group.copy(
                        options = group.options.map { it.copy(extraPrice = money(it.extraPrice) ?: it.extraPrice) },
                    )
                },
        )

    private fun duplicated(names: List<String>): Boolean {
        val keys = names.map { it.trim().lowercase() }
        return keys.toSet().size != keys.size
    }
}
