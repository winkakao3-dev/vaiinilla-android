package com.vaiinilla.app.core.security

import com.vaiinilla.app.BuildConfig
import com.vaiinilla.app.domain.model.OperationalRole
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoleAccessTokenStore
    @Inject
    constructor(
        private val sessionStore: SecureSessionStore,
        private val seedJwtCache: SeedJwtCache,
    ) {
        fun applyRole(role: OperationalRole) {
            val token = tokenFor(role)?.trim().orEmpty()
            if (token.isNotEmpty()) {
                sessionStore.saveAccessToken(token)
            }
        }

        fun tokenFor(role: OperationalRole): String? = seedJwtCache.get(role) ?: buildConfigTokenFor(role)

        private fun buildConfigTokenFor(role: OperationalRole): String? =
            when (role) {
                OperationalRole.CLIENT ->
                    BuildConfig.ACCESS_TOKEN_CLIENTE.ifBlank {
                        BuildConfig.BOOTSTRAP_ACCESS_TOKEN
                    }
                OperationalRole.CASHIER ->
                    BuildConfig.ACCESS_TOKEN_CAJERO.ifBlank {
                        BuildConfig.BOOTSTRAP_ACCESS_TOKEN
                    }
                OperationalRole.KITCHEN ->
                    BuildConfig.ACCESS_TOKEN_COCINA.ifBlank {
                        BuildConfig.BOOTSTRAP_ACCESS_TOKEN
                    }
                OperationalRole.WAITER -> BuildConfig.ACCESS_TOKEN_MESERO.ifBlank { BuildConfig.BOOTSTRAP_ACCESS_TOKEN }
            }.takeIf { it.isNotBlank() }
    }
