package com.vaiinilla.app.core.contract

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet

/**
 * Pedidos que el servidor mandó y esta versión de la app no entiende (un estado o un método de pago nuevos).
 *
 * Las listas los omiten para que los demás se sigan viendo, pero omitirlos en silencio dejaba a Caja, Cocina o
 * Mesero sin un pedido real y sin saberlo. Aquí se cuentan por id para que la pantalla pida actualizar la app.
 * Un pedido sale de la cuenta cuando vuelve a leerse bien, o al cambiar de rol o de sesión.
 */
object UnreadableOrders {
    private val ids = MutableStateFlow<Set<String>>(emptySet())
    private val countState = MutableStateFlow(0)

    /** Cuántos pedidos distintos no se pudieron leer. */
    val count: StateFlow<Int> = countState.asStateFlow()

    /** El pedido [id] no se pudo leer. */
    fun report(id: String) {
        if (id.isBlank()) return
        countState.value = ids.updateAndGet { it + id }.size
    }

    /** El pedido [id] ya se leyó bien. */
    fun resolve(id: String) {
        if (id !in ids.value) return
        countState.value = ids.updateAndGet { it - id }.size
    }

    fun clear() {
        ids.value = emptySet()
        countState.value = 0
    }
}

/**
 * Convierte cada pedido y omite los que esta versión no entiende, avisando su id a [UnreadableOrders].
 *
 * Con [failWhenNoneUnderstood], si la lista traía pedidos y ninguno se entiende, el contrato cambió de verdad y el
 * primer error sale (STAFF-21). Dentro de una mesa o un grupo no se usa: ahí un solo pedido raro no debe tirar
 * el tablero entero.
 */
internal fun <T, R> List<T>.mapOrdersTolerant(
    id: (T) -> String,
    failWhenNoneUnderstood: Boolean = true,
    transform: (T) -> R,
): List<R> {
    if (isEmpty()) return emptyList()
    val results = map { item -> runCatching { transform(item) } }
    val understood = mutableListOf<R>()
    results.forEachIndexed { index, result ->
        val orderId = id(get(index))
        result
            .onSuccess {
                understood += it
                UnreadableOrders.resolve(orderId)
            }.onFailure { UnreadableOrders.report(orderId) }
    }
    if (failWhenNoneUnderstood && understood.isEmpty()) results.first().getOrThrow()
    return understood
}

/**
 * Lo mismo para algo que no es un pedido (una mesa, una llamada, un espacio): se omite sin contarlo como pedido.
 * Si la lista traía elementos y ninguno se entiende, el primer error sale igual que en [mapOrdersTolerant].
 */
internal fun <T, R> List<T>.mapSkippingUnknown(transform: (T) -> R): List<R> {
    if (isEmpty()) return emptyList()
    val results = map { runCatching { transform(it) } }
    val understood = results.mapNotNull { it.getOrNull() }
    if (understood.isEmpty()) results.first().getOrThrow()
    return understood
}
