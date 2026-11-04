package com.vaiinilla.app.ui.navigation

import android.content.Context
import android.content.SharedPreferences
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.model.OperationalRole

/** Si este teléfono ya recibió a la persona en esa tienda; el aviso sale una sola vez. */
internal class StoreAccessMemory(
    private val prefs: SharedPreferences,
) {
    fun seen(establishmentId: String): Boolean = prefs.getBoolean(establishmentId, false)

    fun markSeen(establishmentId: String) {
        prefs.edit().putBoolean(establishmentId, true).apply()
    }

    companion object {
        fun from(context: Context) =
            StoreAccessMemory(context.applicationContext.getSharedPreferences("vaiinilla_acceso_tienda", Context.MODE_PRIVATE))
    }
}

internal fun pendingStoreAccess(
    modes: List<AuthorizedMode>,
    memory: StoreAccessMemory,
): AuthorizedMode? =
    modes.firstOrNull {
        it.role == OperationalRole.ADMIN && it.recentlyAssigned && it.establishmentActive && !memory.seen(it.establishmentId)
    }
