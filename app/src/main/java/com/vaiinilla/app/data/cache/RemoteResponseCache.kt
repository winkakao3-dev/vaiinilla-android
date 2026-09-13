package com.vaiinilla.app.data.cache

import android.content.Context
import com.vaiinilla.app.core.security.JwtContextScope
import com.vaiinilla.app.core.security.SecureSessionStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Session-scoped cache of the last successful GET body for the screens that
 * benefit from instant paint (wallet, client orders). Entries are keyed by the
 * stable JWT claims (`uid` + `rol` + `establecimiento_id` + `membresia_id`),
 * so the cache survives token refreshes while never leaking data across
 * accounts or staff/client contexts.
 */
@Singleton
class RemoteResponseCache
    @Inject
    constructor(
        @ApplicationContext context: Context,
        private val sessionStore: SecureSessionStore,
    ) {
        private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun read(
            path: String,
            query: Map<String, String> = emptyMap(),
        ): String? {
            val key = scopedKey(path, query) ?: return null
            return prefs.getString(key, null)
        }

        fun write(
            path: String,
            query: Map<String, String>,
            body: String,
        ) {
            val key = scopedKey(path, query) ?: return
            prefs.edit().putString(key, body).apply()
        }

        private fun scopedKey(
            path: String,
            query: Map<String, String>,
        ): String? {
            val token = sessionStore.readAccessToken()?.takeIf { it.isNotBlank() } ?: return null
            val scope = JwtContextScope.storageKey(token)
            val queryKey = query.entries.sortedBy { it.key }.joinToString("&") { "${it.key}=${it.value}" }
            return "$scope$KEY_SEPARATOR$path$QUERY_SEPARATOR$queryKey"
        }

        private companion object {
            const val PREFS_NAME = "vaiinilla_remote_cache"
            const val KEY_SEPARATOR = "::"
            const val QUERY_SEPARATOR = "?"
        }
    }
