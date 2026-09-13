package com.vaiinilla.app.core

import kotlinx.coroutines.CancellationException

/**
 * `runCatching` que no se traga [CancellationException]: la relanza para respetar
 * la cancelación estructurada (viewModelScope, cambios de rol, navegación).
 * Usar en lugar de `runCatching` cuando el bloque puede suspender.
 */
internal suspend inline fun <T> runCatchingCancellable(crossinline block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (expected: CancellationException) {
        throw expected
    } catch (error: Throwable) {
        Result.failure(error)
    }
