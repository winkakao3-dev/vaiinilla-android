package com.vaiinilla.app.ui.operational

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Cada cuánto consultan pedidos Caja, Cocina y Mesero (EQUIPO-21): a su ritmo de siempre con la app a la vista y
 * cada minuto en segundo plano. Minimizada no se apaga, solo baja el ritmo para no gastar batería.
 */
object StaffPollCadence {
    const val BACKGROUND_INTERVAL_MS = 60_000L

    fun intervalMs(
        foreground: Boolean,
        foregroundIntervalMs: Long,
    ): Long = if (foreground) foregroundIntervalMs else BACKGROUND_INTERVAL_MS
}

/** Si la app está a la vista según el ciclo de vida del proceso, no el de una sola pantalla. */
interface AppVisibility {
    fun isForeground(): Boolean

    suspend fun awaitForeground()
}

object ProcessAppVisibility : AppVisibility {
    private val lifecycle: Lifecycle? get() = runCatching { ProcessLifecycleOwner.get().lifecycle }.getOrNull()

    // Sin ciclo de vida del proceso (pruebas en la JVM) se trata como a la vista: el ritmo de siempre.
    override fun isForeground(): Boolean = lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) ?: true

    override suspend fun awaitForeground() {
        lifecycle?.currentStateFlow?.first { it.isAtLeast(Lifecycle.State.STARTED) }
    }
}

/**
 * Espera hasta el siguiente sondeo del personal. En segundo plano espera un minuto, pero si la app vuelve a la vista
 * antes, consulta en ese momento en vez de dejar al cajero hasta un minuto con la lista vieja.
 */
suspend fun awaitNextStaffPoll(
    foregroundIntervalMs: Long,
    app: AppVisibility = ProcessAppVisibility,
    backgroundIntervalMs: Long = StaffPollCadence.BACKGROUND_INTERVAL_MS,
) {
    if (app.isForeground()) {
        delay(StaffPollCadence.intervalMs(foreground = true, foregroundIntervalMs = foregroundIntervalMs))
        return
    }
    withTimeoutOrNull(backgroundIntervalMs) { app.awaitForeground() }
}
