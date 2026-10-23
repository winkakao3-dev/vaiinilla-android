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

    /**
     * Los productos en el orden de las categorías del menú, con lo agotado al final de su categoría (B10); lo
     * que no tiene categoría conocida va al final.
     */
    fun products(catalog: Catalog): List<Product> {
        val order =
            catalog.categories.sortedBy { it.order }.withIndex().associate { (index, category) ->
                category.id to
                    index
            }
        return catalog.products
            .withIndex()
            .sortedWith(
                compareBy({ order[it.value.categoryId] ?: Int.MAX_VALUE }, { !it.value.available }, { it.index }),
            ).map { it.value }
    }

    /** La categoría al lado de [current] en el orden del feed: +1 la siguiente, -1 la anterior; null en las orillas. */
    fun neighborCategory(
        categories: List<Int>,
        current: Int,
        step: Int,
    ): Int? = categories.indexOf(current).takeIf { it >= 0 }?.let { categories.getOrNull(it + step) }

    /** Un deslizamiento de lado cuenta si es 1.5 veces más horizontal que vertical. */
    fun isSideways(
        dx: Float,
        dy: Float,
    ): Boolean = kotlin.math.abs(dx) > kotlin.math.abs(dy) * 1.5f

    /**
     * A qué lado cambia de categoría un deslizamiento de lado: a la izquierda (+1) la siguiente, a la derecha (-1) la
     * anterior, 0 si no alcanza. [projectedDx] es a dónde llegaría el dedo con su velocidad; tiene que pasar [threshold].
     */
    fun categoryStep(
        dx: Float,
        dy: Float,
        projectedDx: Float,
        threshold: Float,
    ): Int =
        when {
            !isSideways(dx, dy) || kotlin.math.abs(projectedDx) <= threshold -> 0
            projectedDx < 0f -> 1
            else -> -1
        }

    /**
     * Cuánto se mueve la página con el dedo: 40 % si hay categoría de ese lado; en la primera y la última, un rebote
     * corto (raíz del desplazamiento por 4, en dp).
     */
    fun followOffset(
        dx: Float,
        hasNeighbor: Boolean,
        density: Float,
    ): Float =
        if (hasNeighbor) {
            dx * 0.4f
        } else {
            kotlin.math.sign(dx) * kotlin.math.sqrt(kotlin.math.abs(dx) / density) * 4f * density
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
