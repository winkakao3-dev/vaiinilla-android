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
        preferences.edit(commit = true) {
            putString(key(scope), json.encodeSnapshot(snapshot))
            val index = preferences.getStringSet(INDEX_KEY, emptySet()).orEmpty().toMutableSet()
            if (snapshot.pending.isEmpty()) index.remove(indexEntry(scope)) else index.add(indexEntry(scope))
            putStringSet(INDEX_KEY, index)
        }
    }

    override fun scopesWithPurchases(): List<CounterScope> {
        val indexed = preferences.getStringSet(INDEX_KEY, emptySet()).orEmpty().mapNotNull(::parseIndexEntry)
        // Lo guardado antes del índice: las llaves traen dos ids de 36 caracteres.
        val legacy =
            preferences.all.keys
                .filter { it.startsWith(KEY_PREFIX) }
                .mapNotNull { parseLegacyKey(it.removePrefix(KEY_PREFIX)) }
        return (indexed + legacy)
            .distinct()
            .filter { scope -> runCatching { load(scope, "").pending.isNotEmpty() }.getOrDefault(false) }
    }

    private fun indexEntry(scope: CounterScope) =
        "${scope.membershipId.lowercase()}|${scope.establishmentId.lowercase()}"

    private fun parseIndexEntry(entry: String): CounterScope? =
        entry.split('|').takeIf { it.size == 2 }?.let { CounterScope(it[0], it[1]) }

    private fun parseLegacyKey(rest: String): CounterScope? =
        rest
            .takeIf { it.length == 2 * ID_LENGTH + 1 && it[ID_LENGTH] == '-' }
            ?.let { CounterScope(it.substring(0, ID_LENGTH), it.substring(ID_LENGTH + 1)) }

    private fun key(scope: CounterScope): String =
        "$KEY_PREFIX${scope.membershipId.lowercase()}-${scope.establishmentId.lowercase()}"

    private companion object {
        const val PREFERENCES_NAME = "vaiinilla_offline_counter"
        const val KEY_PREFIX = "offline-counter-"
        const val INDEX_KEY = "scopes-with-purchases"
        const val ID_LENGTH = 36
    }
}
