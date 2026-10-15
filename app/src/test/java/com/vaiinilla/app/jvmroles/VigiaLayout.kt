package com.vaiinilla.app.jvmroles

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.TextLayoutResult

/**
 * Revisa una pantalla quieta y anota lo que se ve mal sin tocar nada:
 *
 * - texto-cortado: un texto que no cabe y se corta sin puntos suspensivos (los "…" son a propósito).
 * - fuera-de-pantalla: texto o algo tocable que se sale por los lados sin estar en una fila que se desliza.
 * - encimados: dos cosas tocables que se pisan a medias; el dedo no sabe cuál va a tocar.
 *
 * Usa el árbol sin fusionar para los textos (cada Text tiene su propio layout) y el fusionado para lo tocable.
 */
object VigiaLayout {
    data class Issue(
        val kind: String,
        val severity: String,
        val detail: String,
    )

    fun audit(
        unmerged: List<SemanticsNode>,
        clickable: List<SemanticsNode>,
        window: Rect,
        labelOf: (SemanticsNode) -> String,
    ): List<Issue> =
        clippedTexts(unmerged, window) + offscreen(unmerged, clickable, window, labelOf) + overlaps(clickable, labelOf)

    private fun clippedTexts(
        nodes: List<SemanticsNode>,
        window: Rect,
    ): List<Issue> =
        nodes.mapNotNull { node ->
            val text =
                node.config
                    .getOrNull(SemanticsProperties.Text)
                    ?.joinToString(" ") { it.text }
                    ?.trim()
            if (text.isNullOrEmpty() || !node.boundsInWindow.overlaps(window)) return@mapNotNull null
            val layout = layoutOf(node) ?: return@mapNotNull null
            // hasVisualOverflow no sirve: compara contra el ancho máximo que le ofrecieron al párrafo, no contra
            // lo que ocupan sus líneas, y marca como cortado un "Uno" que cabe de sobra.
            val tooWide = widestLine(layout) > layout.size.width + SLACK
            val tooTall =
                layout.multiParagraph.height > layout.size.height + SLACK || layout.multiParagraph.didExceedMaxLines
            if (!tooWide && !tooTall) return@mapNotNull null
            val ellipsized = (0 until layout.lineCount).any(layout::isLineEllipsized)
            if (ellipsized) return@mapNotNull null
            val shown = shownText(layout, text)
            Issue(
                kind = "texto-cortado",
                severity = "error",
                detail =
                    "\"${text.take(TEXT_LIMIT)}\" no cabe y se corta sin \"…\"" +
                        (shown?.let { ": se lee \"$it\"" } ?: "") + " " + measures(layout),
            )
        }

    private fun offscreen(
        unmerged: List<SemanticsNode>,
        clickable: List<SemanticsNode>,
        window: Rect,
        labelOf: (SemanticsNode) -> String,
    ): List<Issue> {
        val texts = unmerged.filter { SemanticsProperties.Text in it.config }
        return (texts + clickable)
            .distinctBy { it.id }
            .filter { node ->
                val bounds = node.boundsInWindow
                val sticksOut = bounds.left < window.left - SLACK || bounds.right > window.right + SLACK
                sticksOut && bounds.top < window.bottom && bounds.bottom > window.top && !inHorizontalScroller(node)
            }.mapNotNull { node ->
                val label = labelOf(node).ifBlank { return@mapNotNull null }
                val bounds = node.boundsInWindow
                Issue(
                    kind = "fuera-de-pantalla",
                    severity = "sospecha",
                    detail =
                        "\"${label.take(TEXT_LIMIT)}\" se sale de la pantalla " +
                            "(${bounds.left.toInt()}–${bounds.right.toInt()} de ${window.width.toInt()} px) y no se puede deslizar.",
                )
            }
    }

    private fun overlaps(
        clickable: List<SemanticsNode>,
        labelOf: (SemanticsNode) -> String,
    ): List<Issue> {
        val issues = mutableListOf<Issue>()
        for (i in clickable.indices) {
            for (j in i + 1 until clickable.size) {
                val a = clickable[i].boundsInWindow
                val b = clickable[j].boundsInWindow
                if (!a.overlaps(b) || a.containsRect(b) || b.containsRect(a)) continue
                val shared = a.intersect(b)
                val smaller = minOf(a.width * a.height, b.width * b.height)
                if (smaller <= 0f || shared.width * shared.height / smaller < OVERLAP_RATIO) continue
                if (!bothVisible(under = clickable[i], top = clickable[j])) continue
                issues +=
                    Issue(
                        kind = "encimados",
                        severity = "sospecha",
                        detail =
                            "\"${labelOf(
                                clickable[i],
                            ).take(TEXT_LIMIT)}\" y \"${labelOf(clickable[j]).take(TEXT_LIMIT)}\" " +
                                "se pisan: un toque en la parte común puede ir a cualquiera.",
                    )
            }
        }
        return issues
    }

    /**
     * Dos cosas que se pisan solo confunden al dedo si las dos se ven. [clickable] viene en el orden en que se
     * dibuja, así que [top] queda encima de [under]. No cuenta cuando la capa de [top] tapa por completo a [under]
     * (un botón del encabezado bajo una hoja) ni cuando [under] es contenido que se desliza por debajo de una
     * barra fija (una tarjeta del menú bajo el dock). La capa sale del árbol de layout, no del semántico: una hoja
     * con solo fondo no tiene nodo semántico propio.
     */
    private fun bothVisible(
        under: SemanticsNode,
        top: SemanticsNode,
    ): Boolean {
        val underChain = layoutChain(under)
        val topChain = layoutChain(top)
        val common = underChain.lastOrNull { node -> topChain.any { it === node } } ?: return true
        val topLayer = topChain.getOrNull(topChain.indexOfFirst { it === common } + 1) ?: return true
        if (topLayer.coordinates.boundsInWindow().containsRect(under.boundsInWindow)) return false
        val topAncestors = generateSequence(top) { it.parent }.map { it.id }.toSet()
        return generateSequence(under.parent) { it.parent }
            .takeWhile { it.id !in topAncestors }
            .none { SemanticsProperties.VerticalScrollAxisRange in it.config }
    }

    /** De la raíz al nodo, incluido. */
    private fun layoutChain(node: SemanticsNode): List<LayoutInfo> =
        generateSequence(node.layoutInfo) { it.parentInfo }.toList().asReversed()

    private fun layoutOf(node: SemanticsNode): TextLayoutResult? {
        val results = mutableListOf<TextLayoutResult>()
        val ok =
            runCatching {
                node.config
                    .getOrNull(SemanticsActions.GetTextLayoutResult)
                    ?.action
                    ?.invoke(results)
            }.getOrNull()
        return if (ok == true) results.firstOrNull() else null
    }

    /** Cuánto mide el texto contra el lugar que le dieron, para saber de un vistazo cuánto se corta. */
    private fun measures(layout: TextLayoutResult): String {
        val paragraph = layout.multiParagraph
        return "(texto ${widestLine(layout).toInt()}×${paragraph.height.toInt()} px en " +
            "${layout.size.width}×${layout.size.height} px, ${paragraph.lineCount} línea(s)" +
            (if (paragraph.didExceedMaxLines) ", pasa del máximo de líneas" else "") + ")"
    }

    private fun widestLine(layout: TextLayoutResult): Float =
        (0 until layout.lineCount).maxOfOrNull { layout.getLineRight(it) - layout.getLineLeft(it) } ?: 0f

    /** Lo que alcanza a verse del texto cuando el corte es a lo alto (líneas que no caben). */
    private fun shownText(
        layout: TextLayoutResult,
        text: String,
    ): String? {
        if (!layout.didOverflowHeight || layout.lineCount == 0) return null
        val visibleLines = (0 until layout.lineCount).count { layout.getLineBottom(it) <= layout.size.height + 1f }
        if (visibleLines == 0) return ""
        val end = layout.getLineEnd(visibleLines - 1, visibleEnd = true).coerceAtMost(text.length)
        return text.take(end).take(TEXT_LIMIT)
    }

    private fun inHorizontalScroller(node: SemanticsNode): Boolean {
        var current: SemanticsNode? = node
        while (current != null) {
            if (SemanticsProperties.HorizontalScrollAxisRange in current.config) return true
            current = current.parent
        }
        return false
    }

    private fun Rect.containsRect(other: Rect): Boolean =
        other.left >= left - SLACK &&
            other.top >= top - SLACK &&
            other.right <= right + SLACK &&
            other.bottom <= bottom + SLACK

    private const val TEXT_LIMIT = 60
    private const val SLACK = 2f
    private const val OVERLAP_RATIO = 0.25f
}
