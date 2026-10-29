package com.vaiinilla.app.core.notifications

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * El push de una llamada al mesero («va en camino») despierta a las pantallas que la siguen, para que con la app
 * abierta el cambio se vea al instante y no hasta la siguiente consulta.
 */
object TableCallPushes {
    private val events = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val updates: SharedFlow<Unit> = events.asSharedFlow()

    fun notifyChanged() {
        events.tryEmit(Unit)
    }

    /** Espera la siguiente consulta: [pollMs] o antes, si llega un push de llamada. */
    suspend fun awaitNextPoll(pollMs: Long) {
        withTimeoutOrNull(pollMs) { updates.first() }
    }
}
