package com.vaiinilla.app.core.security

import com.vaiinilla.app.domain.model.OperationalRole
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedJwtCache
    @Inject
    constructor() {
        private val cache = ConcurrentHashMap<OperationalRole, Entry>()

        fun get(role: OperationalRole): String? = cache[role]?.takeUnless { isExpired(it) }?.token

        fun isValid(role: OperationalRole): Boolean = get(role) != null

        fun put(
            role: OperationalRole,
            token: String,
            expiresInSeconds: Int,
        ) {
            val expiresAtEpochMs = System.currentTimeMillis() + expiresInSeconds * 1_000L - EXPIRY_BUFFER_MS
            cache[role] = Entry(token = token, expiresAtEpochMs = expiresAtEpochMs)
        }

        fun clear() {
            cache.clear()
        }

        private fun isExpired(entry: Entry): Boolean = System.currentTimeMillis() >= entry.expiresAtEpochMs

        private data class Entry(
            val token: String,
            val expiresAtEpochMs: Long,
        )

        private companion object {
            const val EXPIRY_BUFFER_MS = 60_000L
        }
    }
