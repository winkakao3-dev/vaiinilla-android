package com.vaiinilla.app.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPreferencesPickupTokenStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : PickupTokenStore {
        private val preferences = context.getSharedPreferences(SECURE_PREFERENCES_NAME, Context.MODE_PRIVATE)
        private val legacyPreferences = context.getSharedPreferences(LEGACY_PREFERENCES_NAME, Context.MODE_PRIVATE)

        override fun save(
            orderId: String,
            pickupToken: String,
        ) {
            if (orderId.isBlank() || pickupToken.isBlank()) return
            runCatching {
                val cipher =
                    Cipher.getInstance(TRANSFORMATION).apply {
                        init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
                    }
                val storageKey = storageKey(orderId)
                preferences
                    .edit()
                    .putString("$storageKey.iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                    .putString(
                        "$storageKey.ciphertext",
                        Base64.encodeToString(cipher.doFinal(pickupToken.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP),
                    ).apply()
                legacyPreferences.edit().remove(orderId).apply()
            }
        }

        override fun read(orderId: String): String? {
            if (orderId.isBlank()) return null
            val storageKey = storageKey(orderId)
            val encrypted =
                runCatching {
                    val iv = preferences.getString("$storageKey.iv", null) ?: return@runCatching null
                    val ciphertext = preferences.getString("$storageKey.ciphertext", null) ?: return@runCatching null
                    Cipher
                        .getInstance(TRANSFORMATION)
                        .apply {
                            init(
                                Cipher.DECRYPT_MODE,
                                getOrCreateSecretKey(),
                                GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)),
                            )
                        }.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP))
                        .toString(Charsets.UTF_8)
                }.getOrNull()
            if (!encrypted.isNullOrBlank()) return encrypted

            // One-time migration for tokens written by the previous plaintext store.
            val legacy = legacyPreferences.getString(orderId, null).orEmpty()
            if (legacy.isBlank()) return null
            save(orderId, legacy)
            return legacy
        }

        override fun clear() {
            preferences.edit().clear().apply()
            legacyPreferences.edit().clear().apply()
        }

        private fun storageKey(orderId: String): String = "order_${orderId.replace(ORDER_KEY_PATTERN, "_")}"

        private fun getOrCreateSecretKey(): SecretKey {
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
            val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            if (existing != null) return existing

            return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).run {
                init(
                    KeyGenParameterSpec
                        .Builder(
                            KEY_ALIAS,
                            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build(),
                )
                generateKey()
            }
        }

        private companion object {
            const val ANDROID_KEY_STORE = "AndroidKeyStore"
            const val KEY_ALIAS = "vaiinilla_pickup_token_v1"
            const val TRANSFORMATION = "AES/GCM/NoPadding"
            const val SECURE_PREFERENCES_NAME = "secure_pickup_tokens"
            const val LEGACY_PREFERENCES_NAME = "pickup_tokens"
            val ORDER_KEY_PATTERN = Regex("[^A-Za-z0-9._-]")
        }
    }
