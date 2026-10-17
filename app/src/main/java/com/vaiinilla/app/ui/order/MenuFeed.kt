package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.Product

/**
 * El menú deslizable ("Desliza"): un platillo por pantalla, como un feed de video. Aquí van las reglas que no
 * dependen de la pantalla: el orden del feed y cómo el "+" y el "−" cambian el carrito.
 */
object MenuFeed {
    const val MAX_LINE_QUANTITY = 20
    const val MAX_LINES = 50

    /** Lo disponible, en el orden de las categorías del menú; lo que no tiene categoría conocida va al final. */
    fun products(catalog: Catalog): List<Product> {
        val order =
            catalog.categories.sortedBy { it.order }.withIndex().associate { (index, category) ->
                category.id to
                    index
            }
        return catalog.products
            .filter(Product::available)
            .withIndex()
            .sortedWith(compareBy({ order[it.value.categoryId] ?: Int.MAX_VALUE }, { it.index }))
            .map { it.value }
    }

    /** Con un grupo obligatorio, el "+" abre la ficha para elegir; sin él, agrega directo. */
    fun needsChoice(product: Product): Boolean = product.optionGroups.any { it.minimumSelections > 0 }

    fun quantityOf(
        lines: List<CartLine>,
        productId: Int,
    ): Int = lines.filter { it.product.id == productId }.sumOf { it.quantity }

    sealed interface Change {
        data class Updated(
            val lines: List<CartLine>,
        ) : Change

        data class Rejected(
            val message: String,
        ) : Change
    }

    /** Uno más, sin opciones ni nota: se suma a la línea igual que ya exista. */
    fun addOne(
        lines: List<CartLine>,
        product: Product,
    ): Change {
        val candidate = CartLine(product = product, quantity = 1, selectedOptionIds = emptySet())
        val existing = lines.firstOrNull { it.key == candidate.key }
        return when {
            existing != null && existing.quantity >= MAX_LINE_QUANTITY ->
                Change.Rejected("La cantidad máxima por línea es $MAX_LINE_QUANTITY.")
            existing != null ->
                Change.Updated(lines.map { if (it.key == candidate.key) it.copy(quantity = it.quantity + 1) else it })
            lines.size >= MAX_LINES -> Change.Rejected("El carrito admite hasta $MAX_LINES líneas.")
            else -> Change.Updated(lines + candidate)
        }
    }

    /** Uno menos del platillo: baja la última línea que lo tenga (la más reciente) y la quita en cero. */
    fun removeOne(
        lines: List<CartLine>,
        productId: Int,
    ): List<CartLine> {
        val index = lines.indexOfLast { it.product.id == productId }
        if (index < 0) return lines
        val line = lines[index]
        return if (line.quantity <= 1) {
            lines.filterIndexed { i, _ -> i != index }
        } else {
            lines.toMutableList().also { it[index] = line.copy(quantity = line.quantity - 1) }
        }
    }
}
