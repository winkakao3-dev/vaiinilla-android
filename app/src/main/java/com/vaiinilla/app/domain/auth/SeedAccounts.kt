package com.vaiinilla.app.domain.auth

import com.vaiinilla.app.BuildConfig
import com.vaiinilla.app.domain.model.OperationalRole

/**
 * Seed role metadata for local REMOTE debug.
 *
 * Passwords are **never** shipped in source or release APKs. They come from
 * `local.properties` → BuildConfig on debug builds only. See `docs/FIREBASE_SEED_AUTH.md`.
 */
object SeedAccounts {
    private data class SeedIdentity(
        val role: OperationalRole,
        val email: String,
        val membresiaId: String,
    )

    private val identities =
        listOf(
            SeedIdentity(
                role = OperationalRole.CLIENT,
                email = "cliente@vaiinilla.test",
                membresiaId = "9c3334d4-1ed6-4cc2-a4a8-785a6afd03f3",
            ),
            SeedIdentity(
                role = OperationalRole.CASHIER,
                email = "cajero@vaiinilla.test",
                membresiaId = "a1111111-0000-4000-8000-0000000000a1",
            ),
            SeedIdentity(
                role = OperationalRole.KITCHEN,
                email = "cocina@vaiinilla.test",
                membresiaId = "a1111111-0000-4000-8000-0000000000a2",
            ),
            SeedIdentity(
                role = OperationalRole.WAITER,
                email = "mesero@vaiinilla.test",
                membresiaId = "a1111111-0000-4000-8000-0000000000a3",
            ),
        )

    private val byRole = identities.associateBy { it.role }

    fun forRole(role: OperationalRole): SeedAccount? {
        if (!BuildConfig.SEED_AUTH_ENABLED) return null
        val identity = byRole[role] ?: return null
        val password = passwordFor(role)
        if (password.isBlank()) return null
        return SeedAccount(
            role = identity.role,
            email = identity.email,
            membresiaId = identity.membresiaId,
            password = password,
        )
    }

    fun all(): List<SeedAccount> = OperationalRole.entries.mapNotNull(::forRole)

    fun isConfigured(): Boolean = all().size == OperationalRole.entries.size

    private fun passwordFor(role: OperationalRole): String =
        when (role) {
            OperationalRole.CLIENT -> BuildConfig.SEED_PASSWORD_CLIENTE
            OperationalRole.CASHIER -> BuildConfig.SEED_PASSWORD_CAJERO
            OperationalRole.KITCHEN -> BuildConfig.SEED_PASSWORD_COCINA
            OperationalRole.WAITER -> BuildConfig.SEED_PASSWORD_MESERO
        }
}
