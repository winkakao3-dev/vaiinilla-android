package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.ContractRules
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.Product

/**
 * Lo que cambió en el menú mientras el carrito esperaba: las líneas guardan una foto del producto, así que un
 * platillo agotado o con otro precio se enteraba la persona hasta que el servidor rechazaba el pago (COMPRA-02).
 */
internal data class CartReconciliation(
    /** Líneas con el producto del menú vigente; las que ya no existen se quitaron. */
    val lines: List<CartLine>,
    /** Claves de las líneas que siguen en el carrito pero ya no se pueden pedir. */
    val unavailableKeys: Set<String>,
    val removedNames: List<String>,
    val repricedNames: List<String>,
) {
    val notice: String?
        get() =
            listOfNotNull(
                removedNames.takeIf { it.isNotEmpty() }?.let { "Ya no está en el menú: ${it.joinToString()}." },
                repricedNames.takeIf { it.isNotEmpty() }?.let { "Cambió el precio de ${it.joinToString()}." },
            ).joinToString(" ").takeIf { it.isNotEmpty() }
}

internal fun reconcileCartLines(
    lines: List<CartLine>,
    products: List<Product>,
): CartReconciliation {
    val byId = products.associateBy { it.id }
    val kept = mutableListOf<CartLine>()
    val unavailable = mutableSetOf<String>()
    val removed = mutableListOf<String>()
    val repriced = mutableListOf<String>()
    lines.forEach { line ->
        val fresh = byId[line.product.id]
        if (fresh == null) {
            removed += line.product.name
            return@forEach
        }
        val updated = line.copy(product = fresh)
        val stillValid = runCatching { ContractRules.validateSelections(fresh, line.selectedOptionIds) }.isSuccess
        if (!fresh.available || !stillValid) unavailable += updated.key
        if (Money.cartLinePreview(updated) != Money.cartLinePreview(line)) repriced += fresh.name
        kept += updated
    }
    return CartReconciliation(kept, unavailable, removed.distinct(), repriced.distinct())
}

internal fun Set<String>.stillIn(lines: List<CartLine>): Set<String> = intersect(lines.mapTo(mutableSetOf()) { it.key })

/** Aviso al restaurar un carrito guardado cuyo producto ya no está en el menú. */
internal fun droppedCartLinesNotice(count: Int): String? =
    when {
        count <= 0 -> null
        count == 1 -> "Un producto de tu carrito ya no está en el menú y lo quitamos."
        else -> "$count productos de tu carrito ya no están en el menú y los quitamos."
    }
