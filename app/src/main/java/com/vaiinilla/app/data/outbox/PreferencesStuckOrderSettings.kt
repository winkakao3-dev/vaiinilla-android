package com.vaiinilla.app.data.outbox

import android.content.Context
import androidx.core.content.edit
import com.vaiinilla.app.domain.outbox.StuckOrderPolicy
import com.vaiinilla.app.domain.outbox.StuckOrderSettings

/** El ajuste de comandas paradas de cada negocio, guardado en este teléfono. */
class PreferencesStuckOrderSettings(
    context: Context,
) : StuckOrderSettings {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun policyFor(establishmentId: String): StuckOrderPolicy {
        val key = establishmentId.lowercase()
        return StuckOrderPolicy(
            enabled = preferences.getBoolean("$key-enabled", true),
            minutes = preferences.getInt("$key-minutes", StuckOrderPolicy.DEFAULT_MINUTES),
        )
    }

    override fun save(
        establishmentId: String,
        policy: StuckOrderPolicy,
    ) {
        val key = establishmentId.lowercase()
        preferences.edit {
            putBoolean("$key-enabled", policy.enabled)
            putInt("$key-minutes", policy.minutes)
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "vaiinilla_stuck_orders"
    }
}
