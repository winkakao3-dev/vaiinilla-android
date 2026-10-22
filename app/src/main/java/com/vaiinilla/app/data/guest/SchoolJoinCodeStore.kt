package com.vaiinilla.app.data.guest

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * El código de la escuela con el que se entró ("Usar código" › Escuela). Una escuela cerrada no deja unirse
 * por slug (403 JOIN_CODE_REQUIRED), así que el contexto de cliente lo manda como `codigo_union` mientras
 * siga guardado para ese establecimiento.
 */
@Singleton
class SchoolJoinCodeStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val prefs get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        fun remember(
            establishmentId: String,
            code: String,
        ) {
            prefs.edit().putString(establishmentId, code.trim().uppercase()).apply()
        }

        fun codeFor(establishmentId: String): String? = prefs.getString(establishmentId, null)

        private companion object {
            const val PREFS = "school_join_codes"
        }
    }
