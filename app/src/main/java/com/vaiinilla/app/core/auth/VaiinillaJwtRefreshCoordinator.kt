package com.vaiinilla.app.core.auth

import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.data.auth.student.StudentAuthUserNotFoundException
import com.vaiinilla.app.domain.model.OperationalRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
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
        private val consecutiveRejections = AtomicInteger(0)
        private val sessionExpiredEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        /**
         * La cuenta fue deshabilitada, borrada o sus credenciales ya no valen: reintentar no la arregla y la
         * app debe pedir iniciar sesión. Se emite tras [EXPIRY_REJECTION_COUNT] rechazos seguidos, no por un
         * solo fallo de red.
         */
        val sessionExpired: SharedFlow<Unit> = sessionExpiredEvents.asSharedFlow()

        /**
         * [saveToken] guarda el token del modo nuevo dentro de refreshLock: si el refresh del modo anterior está en
         * vuelo, termina primero y su token queda debajo. Guardado antes y fuera del lock, ese refresh tardío de
         * Caja pisaba el token de Administración y todo lo de admin respondía 403 hasta el siguiente refresh.
         */
        fun startSession(
            role: OperationalRole,
            expiresInSeconds: Int,
            saveToken: () -> Unit = {},
            refresh: (() -> Result<Unit>)? = null,
        ) {
            val generation =
                refreshLock.withLock {
                    saveToken()
                    activeRole.set(role)
                    activeRefresh.set(refresh)
                    lastSuccessfulRefreshNanos = Long.MIN_VALUE
                    consecutiveRejections.set(0)
                    sessionGeneration.incrementAndGet()
                }
            scheduleRefresh(expiresInSeconds, generation)
        }

        fun clearSession() {
            // El callback de refresh corre dentro de refreshLock (incluido su
            // saveAccessToken). Esperar el lock aquí ordena el logout DESPUÉS de
            // cualquier refresh en vuelo: el token que acabe de escribirse queda
            // borrado por el sessionStore.clear() posterior del llamador, en vez
            // de resucitar una sesión ya cerrada.
            refreshLock.withLock {
                sessionGeneration.incrementAndGet()
                activeRole.set(null)
                activeRefresh.set(null)
                lastSuccessfulRefreshNanos = Long.MIN_VALUE
                consecutiveRejections.set(0)
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
                    consecutiveRejections.set(0)
                } else if (result.exceptionOrNull()?.isRejectedCredential() == true) {
                    if (consecutiveRejections.incrementAndGet() >= EXPIRY_REJECTION_COUNT) {
                        consecutiveRejections.set(0)
                        sessionExpiredEvents.tryEmit(Unit)
                    }
                } else {
                    consecutiveRejections.set(0)
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
            const val EXPIRY_REJECTION_COUNT = 3
        }
    }

/** Firebase o el servidor dicen que esta cuenta o sus credenciales ya no valen (no es un fallo de red). */
internal fun Throwable.isRejectedCredential(): Boolean =
    generateSequence(this) { it.cause }.take(MAX_CAUSE_DEPTH).any { error ->
        when (error) {
            is StudentAuthUserNotFoundException,
            is FirebaseAuthInvalidUserException,
            is FirebaseAuthInvalidCredentialsException,
            -> true
            is FirebaseAuthException -> error.errorCode in REJECTED_FIREBASE_CODES
            is ApiClientException -> error.httpStatus == 401
            else -> false
        }
    }

private const val MAX_CAUSE_DEPTH = 6
private val REJECTED_FIREBASE_CODES = setOf("ERROR_USER_DISABLED", "ERROR_USER_NOT_FOUND", "ERROR_USER_TOKEN_EXPIRED")
