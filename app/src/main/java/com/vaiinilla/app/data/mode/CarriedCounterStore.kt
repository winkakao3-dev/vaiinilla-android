package com.vaiinilla.app.data.mode

import android.content.Context
import androidx.core.content.edit

/**
 * La marca «Ya lo llevé» de lo de Caja, por pedido. Es solo de este teléfono: no se comparte con el equipo ni
 * llama al servidor. Antes vivía en un `rememberSaveable` y se perdía al cerrar la hoja de la cuenta.
 * Guarda los últimos [MAX_ORDERS] pedidos marcados para que la lista no crezca sin límite.
 */
class CarriedCounterStore(
    context: Context,
) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun isCarried(orderId: String): Boolean = read().contains(orderId)

    fun setCarried(
        orderId: String,
        carried: Boolean,
    ) {
        val ids = read().filterNot { it == orderId }.toMutableList()
        if (carried) ids.add(orderId)
        preferences.edit { putString(KEY_IDS, ids.takeLast(MAX_ORDERS).joinToString(SEPARATOR)) }
    }

    private fun read(): List<String> =
        preferences
            .getString(KEY_IDS, null)
            .orEmpty()
            .split(SEPARATOR)
            .filter { it.isNotEmpty() }

    companion object {
        const val MAX_ORDERS = 200
        private const val PREFERENCES_NAME = "vaiinilla_carried_counter"
        private const val KEY_IDS = "ids"
        private const val SEPARATOR = "\n"
    }
}
