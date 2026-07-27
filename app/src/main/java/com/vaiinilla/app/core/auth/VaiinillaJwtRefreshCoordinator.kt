package com.vaiinilla.app.core.auth

import com.vaiinilla.app.domain.model.OperationalRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class VaiinillaJwtRefreshCoordinator
    @Inject
    constructor(
        private val authRepositoryProvider: Provider<com.vaiinilla.app.data.auth.FirebaseSeedAuthRepository>,
    ) : ActiveSessionRefresher {
        private val activeRole = AtomicReference<OperationalRole?>(null)
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private var refreshJob: Job? = null

        fun startSession(
            role: OperationalRole,
            expiresInSeconds: Int,
        ) {
            activeRole.set(role)
            scheduleRefresh(expiresInSeconds)
        }

        fun clearSession() {
            activeRole.set(null)
            refreshJob?.cancel()
            refreshJob = null
        }

        override fun refreshActiveSession(): Result<Unit> {
            val role =
                activeRole.get()
                    ?: return Result.failure(IllegalStateException("No hay sesión activa para refrescar."))
            return authRepositoryProvider.get().refreshRoleSession(role)
        }

        private fun scheduleRefresh(expiresInSeconds: Int) {
            refreshJob?.cancel()
            val delayMs = (expiresInSeconds * 1_000L - REFRESH_LEAD_MS).coerceAtLeast(MIN_REFRESH_DELAY_MS)
            refreshJob =
                scope.launch {
                    delay(delayMs)
                    refreshActiveSession()
                }
        }

        private companion object {
            const val REFRESH_LEAD_MS = 180_000L
            const val MIN_REFRESH_DELAY_MS = 60_000L
        }
    }
