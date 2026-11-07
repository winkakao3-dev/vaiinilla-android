package com.vaiinilla.app.ui.operational

/** Una comanda marcada «lista» que espera su ventana de deshacer antes de mandarse al servidor. */
data class PendingReady(
    val orderId: String,
    val version: Int,
    val deadlineMs: Long,
)

/**
 * Las marcas de «lista» de Cocina que aún se pueden deshacer. Viven en el ViewModel y no en la pantalla: girar el
 * teléfono, cambiar de pestaña o salir de la pantalla no las pierde ni las manda de golpe sin orden, y varias a la
 * vez se mandan todas (una por una), no solo la primera. Sin relojes ni corrutinas: quien la usa pasa la hora.
 */
class PendingReadyQueue {
    private val items = LinkedHashMap<String, PendingReady>()

    /** Marca una comanda; marcarla otra vez reinicia su espera. */
    fun add(
        orderId: String,
        version: Int,
        nowMs: Long,
    ): PendingReady = PendingReady(orderId, version, nowMs + UNDO_WINDOW_MS).also { items[orderId] = it }

    /** Deshace la marca; devuelve si había una. */
    fun undo(orderId: String): Boolean = items.remove(orderId) != null

    /** Saca las que ya cumplieron su espera, en el orden en que se marcaron. */
    fun takeDue(nowMs: Long): List<PendingReady> {
        val due = items.values.filter { it.deadlineMs <= nowMs }
        due.forEach { items.remove(it.orderId) }
        return due
    }

    /** Saca todas, sin esperar (salir de la pantalla o del rol). */
    fun drain(): List<PendingReady> = items.values.toList().also { items.clear() }

    fun snapshot(): Map<String, PendingReady> = LinkedHashMap(items)

    companion object {
        /** Cuánto se puede deshacer «lista» antes de que salga al servidor. */
        const val UNDO_WINDOW_MS = 10_000L
    }
}
