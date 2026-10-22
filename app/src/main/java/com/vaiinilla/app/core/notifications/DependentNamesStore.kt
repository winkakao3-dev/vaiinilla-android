package com.vaiinilla.app.core.notifications

import android.content.Context

/**
 * El nombre de pila de cada hijo del tutor, para que el aviso de un pedido o de saldo bajo diga
 * "Tilin" aunque el push solo traiga `dependienteUsuarioId`. Se reescribe al cargar Mis hijos.
 */
object DependentNamesStore {
    private const val PREFS = "parent_mode_children"

    fun replace(
        context: Context,
        names: Map<String, String>,
    ) {
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply { names.forEach { (id, name) -> putString(id, name) } }
            .apply()
    }

    fun firstName(
        context: Context,
        dependentId: String,
    ): String? = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(dependentId, null)

    fun clear(context: Context) {
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
