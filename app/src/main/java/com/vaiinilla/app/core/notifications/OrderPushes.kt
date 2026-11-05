package com.vaiinilla.app.core.notifications

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge

/**
 * Un push de pedido (cambio de estado al cliente, aviso de pedido nuevo al personal) adelanta la siguiente
 * consulta: con la app abierta el cambio se ve al llegar el aviso y no hasta 5 s (o 1 min en segundo plano) después.
 */
object OrderPushes {
    private val events = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val updates: SharedFlow<Unit> = events.asSharedFlow()

    fun notifyChanged() {
        events.tryEmit(Unit)
    }

    /** Espera lo que pase primero: la pausa normal entre consultas ([wait]) o un push de pedido. */
    suspend fun awaitNextPoll(wait: suspend () -> Unit) {
        merge(flow { wait().also { emit(Unit) } }, updates).first()
    }
}
