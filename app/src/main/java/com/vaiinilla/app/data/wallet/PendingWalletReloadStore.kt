package com.vaiinilla.app.data.wallet

import android.content.Context
import androidx.core.content.edit
import com.vaiinilla.app.core.security.JwtContextScope
import com.vaiinilla.app.core.security.SecureSessionStore
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Una recarga en efectivo confirmada por el cajero se persiste aquí ANTES del POST.
 * Si el proceso muere después de que el servidor abonó el saldo, la misma
 * Idempotency-Key sobrevive y el siguiente intento/replay confirma la operación
 * en lugar de abonar dos veces. El registro es por contexto JWT (operador +
 * establecimiento), igual que el marker de DeviceTokenRegistrar.
 */
@Singleton
class PendingWalletReloadStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
        private val sessionStore: SecureSessionStore,
    ) {
        private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        fun read(): PendingWalletReload? {
            val key = storageKey() ?: return null
            val raw = prefs.getString(key, null) ?: return null
            return runCatching {
                val obj = JSONObject(raw)
                PendingWalletReload(
                    userId = obj.getString("userId"),
                    amount = obj.getString("amount"),
                    idempotencyKey = obj.getString("idempotencyKey"),
                    storedAtEpochMs = obj.optLong("storedAtEpochMs", 0L),
                )
            }.getOrNull()
        }

        fun write(record: PendingWalletReload) {
            val key = storageKey() ?: return
            val json =
                JSONObject()
                    .put("userId", record.userId)
                    .put("amount", record.amount)
                    .put("idempotencyKey", record.idempotencyKey)
                    .put("storedAtEpochMs", record.storedAtEpochMs)
                    .toString()
            prefs.edit { putString(key, json) }
        }

        fun clear() {
            val key = storageKey() ?: return
            prefs.edit { remove(key) }
        }

        private fun storageKey(): String? {
            val token = sessionStore.readAccessToken()?.takeIf { it.isNotBlank() } ?: return null
            return "pending.${JwtContextScope.storageKey(token)}"
        }

        private companion object {
            const val PREFS = "pending_wallet_reload_v1"
        }
    }

data class PendingWalletReload(
    val userId: String,
    val amount: String,
    val idempotencyKey: String,
    val storedAtEpochMs: Long = 0L,
)
