package com.vaiinilla.app.data.counter

import android.content.Context
import androidx.core.content.edit
import com.vaiinilla.app.domain.counter.CounterScope
import com.vaiinilla.app.domain.counter.OfflineCounterSnapshot
import com.vaiinilla.app.domain.counter.OfflineCounterStore

/**
 * Las ventas sin conexión y las fichas del día de un cajero en un negocio. Viven solo en este teléfono
 * (fuera del respaldo de Android, como las demás preferencias de la app) y sobreviven a matar la app.
 */
class PreferencesOfflineCounterStore(
    context: Context,
    private val json: StudentCounterJson = StudentCounterJson(),
) : OfflineCounterStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun load(
        scope: CounterScope,
        today: String,
    ): OfflineCounterSnapshot {
        // Un archivo que no se entiende no debe tumbar Caja; se empieza de cero.
        val stored =
            preferences
                .getString(key(scope), null)
                ?.let { raw -> runCatching { json.decodeSnapshot(raw) }.getOrNull() }
        return (stored ?: OfflineCounterSnapshot(day = today)).startingDay(today)
    }

    override fun save(
        scope: CounterScope,
        snapshot: OfflineCounterSnapshot,
    ) {
        // commit: la venta tiene que estar en disco antes de intentar mandarla.
        preferences.edit(commit = true) { putString(key(scope), json.encodeSnapshot(snapshot)) }
    }

    private fun key(scope: CounterScope): String =
        "offline-counter-${scope.membershipId.lowercase()}-${scope.establishmentId.lowercase()}"

    private companion object {
        const val PREFERENCES_NAME = "vaiinilla_offline_counter"
    }
}
