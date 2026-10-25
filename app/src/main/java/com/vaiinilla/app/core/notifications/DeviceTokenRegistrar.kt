package com.vaiinilla.app.core.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.vaiinilla.app.core.network.HttpVaiinillaApiClient
import com.vaiinilla.app.core.security.JwtContextScope
import com.vaiinilla.app.core.security.SecureSessionStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registers this device's FCM token in the backend (idempotent per JWT context +
 * FCM token marker) so staff and client alerts can target the right device.
 */
@Singleton
class DeviceTokenRegistrar
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val apiClient: HttpVaiinillaApiClient,
        private val sessionStore: SecureSessionStore,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        private val registrationMutex = Mutex()

        init {
            prefs.edit().remove(LEGACY_KEY_TOKEN).apply()
        }

        private var retryJob: Job? = null

        /** Registra el token; si la red falla reintenta con espera creciente en vez de esperar al próximo turno. */
        fun register() {
            startRegistration {
                try {
                    registerTokenIfNeeded(FirebaseMessaging.getInstance().token.await())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "No se pudo obtener el token FCM", e)
                    false
                }
            }
        }

        fun registerToken(token: String) {
            startRegistration { registerTokenIfNeeded(token) }
        }

        @Synchronized
        private fun startRegistration(attempt: suspend () -> Boolean) {
            retryJob?.cancel()
            retryJob =
                scope.launch {
                    for (waitMs in RETRY_DELAYS_MS) {
                        if (attempt()) return@launch
                        delay(waitMs)
                    }
                    attempt()
                }
        }

        /**
         * Al cerrar sesión: DELETE /devices/{token} con la sesión que sale (todavía guardada) para que sus
         * avisos dejen de llegar a este teléfono, y luego se invalida el token local.
         */
        suspend fun clearRegistration() {
            // Un reintento pendiente registraría de nuevo el token con la sesión que acaba de salir.
            synchronized(this) { retryJob?.cancel() }
            registrationMutex.withLock {
                // Instalaciones de antes no guardaban el token registrado: se usa el vigente.
                val registered = prefs.getString(KEY_REGISTERED_TOKEN, null) ?: currentFcmTokenOrNull()
                if (registered != null && !sessionStore.readAccessToken().isNullOrBlank()) {
                    apiClient
                        .delete(unregisterPath(registered))
                        .onFailure { e -> Log.w(TAG, "No se pudo borrar el token FCM del servidor: ${e.message}") }
                }
                prefs
                    .edit()
                    .remove(KEY_MARKER)
                    .remove(KEY_REGISTERED_TOKEN)
                    .remove(LEGACY_KEY_TOKEN)
                    .apply()
            }
            try {
                FirebaseMessaging.getInstance().deleteToken().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo invalidar el token FCM local", e)
            }
        }

        private suspend fun currentFcmTokenOrNull(): String? =
            try {
                FirebaseMessaging.getInstance().token.await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }

        /** `true` si ya quedó registrado o no hay nada que registrar; `false` si falló y conviene reintentar. */
        private suspend fun registerTokenIfNeeded(fcmToken: String): Boolean {
            if (fcmToken.isBlank()) return true
            val sessionToken =
                sessionStore.readAccessToken()?.takeIf { it.isNotBlank() } ?: return true
            val marker = deviceRegistrationMarker(sessionToken, fcmToken)
            return registrationMutex.withLock {
                if (prefs.getString(KEY_MARKER, null) == marker) return@withLock true
                val body =
                    JSONObject()
                        .put("fcmToken", fcmToken)
                        .put("plataforma", "android")
                        .toString()
                val result = apiClient.post("devices", body)
                result
                    .onSuccess {
                        val currentToken = sessionStore.readAccessToken()
                        if (currentToken != null &&
                            deviceRegistrationMarker(currentToken, fcmToken) == marker
                        ) {
                            prefs
                                .edit()
                                .putString(KEY_MARKER, marker)
                                .putString(KEY_REGISTERED_TOKEN, fcmToken)
                                .apply()
                        }
                        Log.d(TAG, "Token FCM registrado")
                    }.onFailure { e ->
                        Log.w(TAG, "Registro de token FCM falló (se reintentará): ${e.message}")
                    }
                result.isSuccess
            }
        }

        private companion object {
            const val TAG = "DeviceTokenRegistrar"
            const val PREFS = "device_tokens"
            const val KEY_MARKER = "fcm_registration_marker"
            const val KEY_REGISTERED_TOKEN = "fcm_registered_token"
            const val LEGACY_KEY_TOKEN = "fcm_token_registered"
            val RETRY_DELAYS_MS = longArrayOf(5_000, 15_000, 45_000, 120_000, 300_000)
        }
    }

internal fun deviceRegistrationMarker(
    sessionToken: String,
    fcmToken: String,
): String = JwtContextScope.sha256("${JwtContextScope.storageKey(sessionToken)}\u0000$fcmToken")

internal fun unregisterPath(fcmToken: String): String =
    "devices/" + URLEncoder.encode(fcmToken, "UTF-8").replace("+", "%20")
