package com.vaiinilla.app.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.vaiinilla.app.BuildConfig
import com.vaiinilla.app.core.auth.VaiinillaJwtRefreshCoordinator
import com.vaiinilla.app.core.config.AppEnvironment
import com.vaiinilla.app.core.config.DataSourceMode
import com.vaiinilla.app.core.security.SecureSessionStore
import com.vaiinilla.app.core.security.SeedJwtCache
import com.vaiinilla.app.domain.auth.SeedAccounts
import com.vaiinilla.app.domain.model.OperationalRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSeedAuthRepository
    @Inject
    constructor(
        private val environment: AppEnvironment,
        private val contextoExchange: SesionesContextoExchange,
        private val sessionStore: SecureSessionStore,
        private val seedJwtCache: SeedJwtCache,
        private val refreshCoordinator: VaiinillaJwtRefreshCoordinator,
    ) {
        private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

        suspend fun authenticateRole(role: OperationalRole): Result<Unit> =
            withContext(Dispatchers.IO) {
                if (environment.dataSourceMode != DataSourceMode.REMOTE) {
                    return@withContext Result.success(Unit)
                }
                runCatching {
                    requireSeedAuthAllowed()
                    signInAndExchange(role, forceRefresh = false)
                    Unit
                }
            }

        suspend fun ensureRoleJwt(
            role: OperationalRole,
            forceRefresh: Boolean = false,
        ): Result<String> =
            withContext(Dispatchers.IO) {
                if (environment.dataSourceMode != DataSourceMode.REMOTE) {
                    return@withContext Result.failure(IllegalStateException("Seed auth solo aplica en REMOTE."))
                }
                if (!forceRefresh) {
                    seedJwtCache.get(role)?.let { return@withContext Result.success(it) }
                }
                runCatching {
                    requireSeedAuthAllowed()
                    signInAndExchange(role, forceRefresh).accessToken
                }
            }

        suspend fun restoreActiveRole(role: OperationalRole): Result<Unit> =
            withContext(Dispatchers.IO) {
                if (environment.dataSourceMode != DataSourceMode.REMOTE) {
                    return@withContext Result.success(Unit)
                }
                runCatching {
                    requireSeedAuthAllowed()
                    val cached = seedJwtCache.get(role)
                    if (cached != null) {
                        signInSeedAccount(role)
                        sessionStore.saveAccessToken(cached)
                        refreshCoordinator.startSession(role, DEFAULT_EXPIRES_IN_SECONDS)
                    } else {
                        signInAndExchange(role, forceRefresh = false)
                    }
                    Unit
                }
            }

        private fun requireSeedAuthAllowed() {
            if (!BuildConfig.SEED_AUTH_ENABLED) {
                throw IllegalStateException(
                    "Seed auth solo está disponible en builds debug con local.properties.",
                )
            }
            if (!SeedAccounts.isConfigured()) {
                throw IllegalStateException(
                    "Faltan passwords seed en local.properties (vaiinillaSeedPassword*).",
                )
            }
        }

        fun refreshRoleSession(role: OperationalRole): Result<Unit> =
            runBlocking {
                runCatching {
                    signInAndExchange(role, forceRefresh = true)
                }.map { }
            }

        private suspend fun signInAndExchange(
            role: OperationalRole,
            forceRefresh: Boolean,
        ): SesionesContextoDataDto {
            val account =
                SeedAccounts.forRole(role)
                    ?: throw IllegalStateException("No hay cuenta seed para el rol ${role.name}.")

            signInSeedAccount(role)

            val firebaseToken =
                auth.currentUser
                    ?.getIdToken(forceRefresh)
                    ?.await()
                    ?.token
                    ?.takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("No se pudo obtener el ID token de Firebase.")

            val session = contextoExchange.exchange(firebaseToken, account.membresiaId)
            seedJwtCache.put(role, session.accessToken, session.expiresIn)
            sessionStore.saveAccessToken(session.accessToken)
            refreshCoordinator.startSession(role, session.expiresIn)
            return session
        }

        private suspend fun signInSeedAccount(role: OperationalRole) {
            val account =
                SeedAccounts.forRole(role)
                    ?: throw IllegalStateException("No hay cuenta seed para el rol ${role.name}.")
            auth.signInWithEmailAndPassword(account.email, account.password).await()
        }

        private companion object {
            const val DEFAULT_EXPIRES_IN_SECONDS = 900
        }
    }
