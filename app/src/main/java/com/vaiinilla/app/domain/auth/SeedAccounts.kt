package com.vaiinilla.app.domain.auth

import com.vaiinilla.app.domain.model.OperationalRole

/**
 * Demo-only seed accounts (Saúl). Passwords are published for local REMOTE testing only.
 * Do not use in production builds.
 */
object SeedAccounts {
    private val accounts = listOf(
        SeedAccount(
            role = OperationalRole.CLIENT,
            email = "cliente@vaiinilla.test",
            membresiaId = "9c3334d4-1ed6-4cc2-a4a8-785a6afd03f3",
            password = "saul1234",
        ),
        SeedAccount(
            role = OperationalRole.CASHIER,
            email = "cajero@vaiinilla.test",
            membresiaId = "a1111111-0000-4000-8000-0000000000a1",
            password = "saul1234",
        ),
        SeedAccount(
            role = OperationalRole.KITCHEN,
            email = "cocina@vaiinilla.test",
            membresiaId = "a1111111-0000-4000-8000-0000000000a2",
            password = "saul1234",
        ),
        SeedAccount(
            role = OperationalRole.WAITER,
            email = "mesero@vaiinilla.test",
            membresiaId = "a1111111-0000-4000-8000-0000000000a3",
            password = "saul1234",
        ),
    )

    private val byRole = accounts.associateBy { it.role }

    fun forRole(role: OperationalRole): SeedAccount? = byRole[role]

    fun all(): List<SeedAccount> = accounts
}
