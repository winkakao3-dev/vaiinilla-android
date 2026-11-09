package com.vaiinilla.app.data.order

import android.content.Context
import androidx.core.content.edit

/**
 * N08: pedidos mixtos cuya parte de Caja (bebidas) ya se llevó a la mesa. El servidor no guarda esa marca, así que
 * vive en las preferencias del teléfono: sobrevive a cerrar la hoja, cambiar de modo (Caja o Mesero) y volver a
 * entrar a la cuenta. Las marcas de más de un día se tiran solas.
 */
class CarriedCounterPartsStore(
    context: Context,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun isCarried(orderId: String): Boolean = orderId in fresh()

    fun setCarried(
        orderId: String,
        carried: Boolean,
    ) {
        if (orderId.isBlank()) return
        val updated = fresh().toMutableMap()
        if (carried) updated[orderId] = now() else updated.remove(orderId)
        preferences.edit { putStringSet(KEY_CARRIED, updated.map { (id, at) -> "$id|$at" }.toSet()) }
    }

    private fun fresh(): Map<String, Long> {
        val limit = now() - MAX_AGE_MILLIS
        return preferences
            .getStringSet(KEY_CARRIED, emptySet())
            .orEmpty()
            .mapNotNull { entry ->
                val id = entry.substringBeforeLast('|')
                val at = entry.substringAfterLast('|').toLongOrNull() ?: return@mapNotNull null
                (id to at).takeIf { id.isNotBlank() && at >= limit }
            }.toMap()
    }

    internal companion object {
        const val PREFERENCES_NAME = "vaiinilla_carried_counter_parts"
        const val KEY_CARRIED = "carried_order_ids"
        const val MAX_AGE_MILLIS = 24L * 60 * 60 * 1000
    }
}
