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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
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

        fun register() {
            scope.launch {
                try {
                    val token = FirebaseMessaging.getInstance().token.await()
                    registerTokenIfNeeded(token)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "No se pudo obtener el token FCM", e)
                }
            }
        }

        fun registerToken(token: String) {
            scope.launch { registerTokenIfNeeded(token) }
        }

        suspend fun clearRegistration() {
            registrationMutex.withLock {
                prefs
                    .edit()
                    .remove(KEY_MARKER)
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

        private suspend fun registerTokenIfNeeded(fcmToken: String) {
            if (fcmToken.isBlank()) return
            val sessionToken =
                sessionStore.readAccessToken()?.takeIf { it.isNotBlank() } ?: return
            val marker = deviceRegistrationMarker(sessionToken, fcmToken)
            registrationMutex.withLock {
                if (prefs.getString(KEY_MARKER, null) == marker) return@withLock
                val body =
                    JSONObject()
                        .put("fcmToken", fcmToken)
                        .put("plataforma", "android")
                        .toString()
                apiClient
                    .post("devices", body)
                    .onSuccess {
                        val currentToken = sessionStore.readAccessToken()
                        if (currentToken != null &&
                            deviceRegistrationMarker(currentToken, fcmToken) == marker
                        ) {
                            prefs.edit().putString(KEY_MARKER, marker).apply()
                        }
                        Log.d(TAG, "Token FCM registrado")
                    }.onFailure { e ->
                        Log.w(TAG, "Registro de token FCM falló (se reintentará): ${e.message}")
                    }
            }
        }

        private companion object {
            const val TAG = "DeviceTokenRegistrar"
            const val PREFS = "device_tokens"
            const val KEY_MARKER = "fcm_registration_marker"
            const val LEGACY_KEY_TOKEN = "fcm_token_registered"
        }
    }

internal fun deviceRegistrationMarker(
    sessionToken: String,
    fcmToken: String,
): String = JwtContextScope.sha256("${JwtContextScope.storageKey(sessionToken)}\u0000$fcmToken")
