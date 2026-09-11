package com.vaiinilla.app.data.cache

import android.content.Context
import android.util.Base64
import com.vaiinilla.app.core.security.SecureSessionStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Session-scoped cache of the last successful GET body for the screens that
 * benefit from instant paint (wallet, client orders). Entries are keyed by the
 * stable JWT claims (`uid` + `rol`), so the cache survives token refreshes while
 * never leaking data across accounts or staff/client contexts.
 */
@Singleton
class RemoteResponseCache
    @Inject
    constructor(
        @ApplicationContext context: Context,
        private val sessionStore: SecureSessionStore,
    ) {
        private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        private val uidPattern = Regex("\"uid\"\\s*:\\s*\"([^\"]+)\"")
        private val rolePattern = Regex("\"rol\"\\s*:\\s*\"([^\"]+)\"")

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
            val scope = stableScope(token) ?: digest(token)
            val queryKey = query.entries.sortedBy { it.key }.joinToString("&") { "${it.key}=${it.value}" }
            return "$scope$KEY_SEPARATOR$path$QUERY_SEPARATOR$queryKey"
        }

        /** JWT claims survive token refreshes; the raw token itself does not. */
        private fun stableScope(token: String): String? =
            runCatching {
                val payload = token.split('.').getOrNull(1) ?: return null
                val json =
                    String(
                        Base64.decode(
                            payload,
                            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
                        ),
                    )
                val uid = uidPattern.find(json)?.groupValues?.get(1) ?: return null
                val role = rolePattern.find(json)?.groupValues?.get(1) ?: "sin-rol"
                "$uid:$role"
            }.getOrNull()

        private fun digest(value: String): String =
            MessageDigest
                .getInstance("SHA-256")
                .digest(value.toByteArray())
                .take(12)
                .joinToString("") { "%02x".format(it) }

        private companion object {
            const val PREFS_NAME = "vaiinilla_remote_cache"
            const val KEY_SEPARATOR = "::"
            const val QUERY_SEPARATOR = "?"
        }
    }
