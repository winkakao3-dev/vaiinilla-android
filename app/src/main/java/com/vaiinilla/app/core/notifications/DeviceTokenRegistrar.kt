package com.vaiinilla.app.core.notifications

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.vaiinilla.app.core.network.HttpVaiinillaApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registers this device's FCM token with the backend (`POST /devices`).
 *
 * Called when a session context becomes active (any role — routing decides who
 * gets which alerts) and whenever FCM rotates the token. Registration is
 * idempotent server-side, so a repeated call is a cheap no-op; we still skip it
 * when the same token was already posted in a previous session.
 */
@Singleton
class DeviceTokenRegistrar
    @Inject
    constructor(
        private val apiClient: HttpVaiinillaApiClient,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** Fetches the current FCM token and registers it unless already done. */
        fun register(context: Context) {
            scope.launch {
                try {
                    val token = FirebaseMessaging.getInstance().token.await()
                    registerToken(context, token)
                } catch (e: Exception) {
                    Log.w(TAG, "No se pudo obtener el token FCM", e)
                }
            }
        }

        /** Registers [token] if it differs from the last one we posted. */
        fun registerToken(
            context: Context,
            token: String,
        ) {
            val prefs = prefs(context)
            if (prefs.getString(KEY_TOKEN, null) == token) return
            scope.launch {
                val body =
                    JSONObject()
                        .put("fcmToken", token)
                        .put("plataforma", "android")
                        .toString()
                apiClient
                    .post("devices", body)
                    .onSuccess {
                        prefs.edit().putString(KEY_TOKEN, token).apply()
                        Log.d(TAG, "Token FCM registrado")
                    }.onFailure { e ->
                        Log.w(TAG, "Registro de token FCM falló (se reintentará): ${e.message}")
                    }
            }
        }

        private fun prefs(context: Context): SharedPreferences =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        private companion object {
            const val TAG = "DeviceTokenRegistrar"
            const val PREFS = "device_tokens"
            const val KEY_TOKEN = "fcm_token_registered"
        }
    }
