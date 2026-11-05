package com.vaiinilla.app.ui.screens

import com.vaiinilla.app.domain.model.CART_LINE_NOTE_MAX
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.Product

/**
 * R24: las líneas del pedido que toma el personal (mostrador, mesa, Admin › Pedir) van por id de línea, no por
 * producto. Así «2 hamburguesas, una sin cebolla» son dos líneas del mismo producto, cada una con su nota y sus
 * opciones. Los ids son enteros crecientes locales a la hoja; al servidor solo viajan las líneas.
 */
internal object StaffOrderLines {
    /** Piezas de un producto entre todas sus líneas: lo que muestra la ficha del menú. */
    fun quantityOf(
        lines: Map<Int, CartLine>,
        productId: Int,
    ): Int = lines.values.filter { it.product.id == productId }.sumOf { it.quantity }

    /** El id que tendría una línea nueva. */
    fun nextId(lines: Map<Int, CartLine>): Int = (lines.keys.maxOrNull() ?: 0) + 1

    /**
     * «+» desde la ficha: suma a la primera línea del producto sin nota (la «normal»); si no hay, abre una con las
     * opciones iniciales. Una línea llena (20) no crece.
     */
    fun addUnit(
        lines: Map<Int, CartLine>,
        product: Product,
        initialOptions: Set<Int>,
    ): Map<Int, CartLine> {
        val plain = lines.entries.firstOrNull { (_, line) -> line.product.id == product.id && line.notes.isEmpty() }
        return if (plain == null) {
            addLine(lines, CartLine(product, 1, initialOptions))
        } else {
            val quantity = (plain.value.quantity + 1).coerceAtMost(COUNTER_LINE_QUANTITY_MAX)
            lines + (plain.key to plain.value.copy(quantity = quantity))
        }
    }

    fun addLine(
        lines: Map<Int, CartLine>,
        line: CartLine,
    ): Map<Int, CartLine> = lines + (nextId(lines) to line)

    /** «−» desde la ficha: quita una pieza de la última línea de ese producto. */
    fun removeUnit(
        lines: Map<Int, CartLine>,
        productId: Int,
    ): Map<Int, CartLine> {
        val last = lines.entries.lastOrNull { (_, line) -> line.product.id == productId } ?: return lines
        return step(lines, last.key, -1)
    }

    /** Sube o baja una línea; en cero se quita. */
    fun step(
        lines: Map<Int, CartLine>,
        lineId: Int,
        delta: Int,
    ): Map<Int, CartLine> {
        val line = lines[lineId] ?: return lines
        val quantity = (line.quantity + delta).coerceAtMost(COUNTER_LINE_QUANTITY_MAX)
        return if (quantity <= 0) lines - lineId else lines + (lineId to line.copy(quantity = quantity))
    }

    /**
     * «Separar una»: saca una pieza a su propia línea, con las mismas opciones y sin nota, para anotarle algo
     * distinto. Una línea de una sola pieza no se separa.
     */
    fun splitOne(
        lines: Map<Int, CartLine>,
        lineId: Int,
    ): Map<Int, CartLine> {
        val line = lines[lineId] ?: return lines
        if (line.quantity <= 1) return lines
        val shrunk = lines + (lineId to line.copy(quantity = line.quantity - 1))
        return addLine(shrunk, line.copy(quantity = 1, notes = ""))
    }

    /** La nota de una línea, con el mismo largo que en el carrito del cliente. Se guarda sin recortar mientras se escribe. */
    fun setNote(
        lines: Map<Int, CartLine>,
        lineId: Int,
        note: String,
    ): Map<Int, CartLine> {
        val line = lines[lineId] ?: return lines
        return lines + (lineId to line.copy(notes = note.take(CART_LINE_NOTE_MAX)))
    }

    fun setOptions(
        lines: Map<Int, CartLine>,
        lineId: Int,
        optionIds: Set<Int>,
    ): Map<Int, CartLine> {
        val line = lines[lineId] ?: return lines
        return lines + (lineId to line.copy(selectedOptionIds = optionIds))
    }

    /** Lo que se manda: notas recortadas y en el orden en que se armaron. */
    fun toSubmit(lines: Map<Int, CartLine>): List<CartLine> =
        lines.entries.sortedBy { it.key }.map { (_, line) -> line.copy(notes = line.notes.trim()) }
}
