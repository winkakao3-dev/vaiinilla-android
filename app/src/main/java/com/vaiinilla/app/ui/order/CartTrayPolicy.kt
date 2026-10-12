package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.CartLine
import java.text.Normalizer

/** Cómo se dibuja un producto sobre la bandeja del carrito. */
internal enum class TrayItemKind { DRINK, PASTRY, PLATE }

/** Una pieza sobre la bandeja: cada unidad del carrito es una pieza, con id estable para animarla. */
internal data class TraySlot(
    val id: String,
    val kind: TrayItemKind,
)

/** Más piezas no caben en la bandeja; la pizarra lleva la cuenta completa. */
internal const val TRAY_MAX_ITEMS = 6

private val drinkWords =
    listOf(
        "cafe",
        "americano",
        "latte",
        "capuchino",
        "cappuccino",
        "espresso",
        "chocolate",
        "te",
        "tea",
        "matcha",
        "chai",
        "agua",
        "jugo",
        "refresco",
        "soda",
        "frappe",
        "smoothie",
        "malteada",
        "limonada",
        "naranjada",
        "bebida",
    )
private val pastryWords =
    listOf(
        "galletas?",
        "pan",
        "croissant",
        "cuernito",
        "donas?",
        "muffin",
        "pastel",
        "brownie",
        "concha",
        "panque",
        "rol",
        "cookies?",
    )

private fun String.plain(): String =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

internal fun trayItemKind(productName: String): TrayItemKind {
    val name = productName.plain()

    fun List<String>.matches() = any { Regex("\\b$it\\b").containsMatchIn(name) }
    // El pan va antes: "galleta de chocolate" es galleta, no bebida.
    return when {
        pastryWords.matches() -> TrayItemKind.PASTRY
        drinkWords.matches() -> TrayItemKind.DRINK
        else -> TrayItemKind.PLATE
    }
}

internal fun traySlots(lines: List<CartLine>): List<TraySlot> =
    lines
        .flatMap { line ->
            List(line.quantity.coerceAtLeast(0)) { i -> TraySlot("${line.key}#$i", trayItemKind(line.product.name)) }
        }.take(TRAY_MAX_ITEMS)
