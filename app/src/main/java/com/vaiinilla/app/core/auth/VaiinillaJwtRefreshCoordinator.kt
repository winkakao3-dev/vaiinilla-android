package com.vaiinilla.app.core.auth

import com.vaiinilla.app.domain.model.OperationalRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.concurrent.withLock

@Singleton
class VaiinillaJwtRefreshCoordinator
    @Inject
    constructor(
        private val authRepositoryProvider: Provider<com.vaiinilla.app.data.auth.FirebaseSeedAuthRepository>,
    ) : ActiveSessionRefresher {
        private val activeRole = AtomicReference<OperationalRole?>(null)
        private val activeRefresh = AtomicReference<(() -> Result<Unit>)?>(null)
        private val sessionGeneration = AtomicLong(0)
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private var refreshJob: Job? = null
        private val refreshLock = ReentrantLock()
        private var lastSuccessfulRefreshNanos: Long = Long.MIN_VALUE

        fun startSession(
            role: OperationalRole,
            expiresInSeconds: Int,
            refresh: (() -> Result<Unit>)? = null,
        ) {
            val generation = sessionGeneration.incrementAndGet()
            activeRole.set(role)
            activeRefresh.set(refresh)
            refreshLock.withLock {
                lastSuccessfulRefreshNanos = Long.MIN_VALUE
            }
            scheduleRefresh(expiresInSeconds, generation)
        }

        fun clearSession() {
            sessionGeneration.incrementAndGet()
            activeRole.set(null)
            activeRefresh.set(null)
            refreshLock.withLock {
                lastSuccessfulRefreshNanos = Long.MIN_VALUE
            }
            refreshJob?.cancel()
            refreshJob = null
        }

        override fun refreshActiveSession(): Result<Unit> =
            refreshLock.withLock {
                val now = System.nanoTime()
                if (
                    lastSuccessfulRefreshNanos != Long.MIN_VALUE &&
                    now - lastSuccessfulRefreshNanos <= REFRESH_COALESCE_WINDOW_NANOS
                ) {
                    return@withLock Result.success(Unit)
                }

                val result =
                    activeRefresh.get()?.invoke()
                        ?: activeRole.get()?.let { role ->
                            authRepositoryProvider.get().refreshRoleSession(role)
                        }
                        ?: Result.failure(IllegalStateException("No hay sesión activa para refrescar."))
                if (result.isSuccess) {
                    lastSuccessfulRefreshNanos = System.nanoTime()
                }
                result
            }

        private fun scheduleRefresh(
            expiresInSeconds: Int,
            generation: Long,
        ) {
            refreshJob?.cancel()
            val normalDelayMs =
                (expiresInSeconds * 1_000L - REFRESH_LEAD_MS).coerceAtLeast(MIN_REFRESH_DELAY_MS)
            refreshJob =
                scope.launch {
                    var nextDelayMs = normalDelayMs
                    while (isActive && sessionGeneration.get() == generation) {
                        delay(nextDelayMs)
                        if (!isActive || sessionGeneration.get() != generation) break
                        val result = refreshActiveSession()
                        nextDelayMs =
                            if (result.isSuccess) {
                                normalDelayMs
                            } else {
                                REFRESH_FAILURE_RETRY_MS
                            }
                    }
                }
        }

        private companion object {
            const val REFRESH_LEAD_MS = 180_000L
            const val MIN_REFRESH_DELAY_MS = 60_000L
            const val REFRESH_COALESCE_WINDOW_NANOS = 5_000_000_000L
            const val REFRESH_FAILURE_RETRY_MS = 30_000L
        }
    }
