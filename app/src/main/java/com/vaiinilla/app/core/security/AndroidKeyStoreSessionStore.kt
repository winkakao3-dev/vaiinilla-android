package com.vaiinilla.app.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AndroidKeyStoreSessionStore(
    context: Context,
) : SecureSessionStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun saveAccessToken(token: String) {
        require(token.isNotBlank()) { "El access token no puede estar vacío." }
        val cipher =
            Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            }
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        preferences
            .edit()
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString(KEY_CIPHERTEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    override fun readAccessToken(): String? =
        runCatching {
            val iv = preferences.getString(KEY_IV, null) ?: return null
            val ciphertext = preferences.getString(KEY_CIPHERTEXT, null) ?: return null
            val cipher =
                Cipher.getInstance(TRANSFORMATION).apply {
                    init(
                        Cipher.DECRYPT_MODE,
                        getOrCreateSecretKey(),
                        GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)),
                    )
                }
            cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)).toString(Charsets.UTF_8)
        }.getOrElse {
            clear()
            null
        }

    override fun clear() {
        preferences.edit().clear().apply()
    }

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
        const val KEY_ALIAS = "vaiinilla_access_token_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val PREFERENCES_NAME = "secure_session"
        const val KEY_IV = "iv"
        const val KEY_CIPHERTEXT = "ciphertext"
    }
}
