package com.vaiinilla.app.data.localhub

import android.content.Context
import androidx.core.content.edit
import com.vaiinilla.app.domain.localhub.LocalKeyStore
import com.vaiinilla.app.domain.localhub.LocalTicketBoardSnapshot

/** Las llaves del negocio (hoy y mañana) que el teléfono bajó con internet. */
class PreferencesLocalKeyStore(
    context: Context,
) : LocalKeyStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun keys(establishmentId: String): List<String> =
        preferences
            .getString(key(establishmentId), null)
            ?.split(SEPARATOR)
            ?.filter { it.isNotBlank() }
            .orEmpty()

    override fun save(
        establishmentId: String,
        keys: List<String>,
    ) {
        preferences.edit(commit = true) { putString(key(establishmentId), keys.joinToString(SEPARATOR)) }
    }

    private fun key(establishmentId: String) = "keys-${establishmentId.lowercase()}"

    private companion object {
        const val PREFERENCES_NAME = "vaiinilla_local_hub_keys"
        const val SEPARATOR = ","
    }
}

/** Las comandas que el concentrador (la app de Caja) recibió: sobreviven a que se cierre la app. */
class PreferencesLocalBoardStore(
    context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(establishmentId: String): LocalTicketBoardSnapshot =
        preferences
            .getString(key(establishmentId), null)
            ?.let { raw -> runCatching { LocalHubJson.decodeBoard(raw) }.getOrNull() }
            ?: LocalTicketBoardSnapshot()

    fun save(
        establishmentId: String,
        board: LocalTicketBoardSnapshot,
    ) {
        preferences.edit(commit = true) { putString(key(establishmentId), LocalHubJson.encodeBoard(board)) }
    }

    private fun key(establishmentId: String) = "board-${establishmentId.lowercase()}"

    private companion object {
        const val PREFERENCES_NAME = "vaiinilla_local_hub_board"
    }
}
