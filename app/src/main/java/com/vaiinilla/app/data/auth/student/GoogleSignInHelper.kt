package com.vaiinilla.app.data.auth.student

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import java.security.SecureRandom

class GoogleSignInUnavailableException(
    message: String = "No se pudo iniciar sesión con Google. Inténtalo de nuevo.",
) : IllegalStateException(message)

/**
 * Wraps Credential Manager's "Sign in with Google" flow. `default_web_client_id` is
 * generated automatically by the google-services Gradle plugin from the project's
 * OAuth web client (type 3) in google-services.json -- never hardcode a client id here.
 *
 * Uses `GetSignInWithGoogleOption`, not `GetGoogleIdOption`: the latter is meant for a
 * passive/automatic call as soon as a screen loads (no user interaction) and reliably
 * fails with "16: Cannot find a matching credential" when triggered from an explicit
 * button tap like ours -- this is the option Google's own docs point to for that case.
 */
object GoogleSignInHelper {
    suspend fun requestIdToken(
        context: Context,
        webClientId: String,
    ): String {
        val option =
            GetSignInWithGoogleOption
                .Builder(webClientId)
                .setNonce(generateNonce())
                .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val response =
            try {
                CredentialManager.create(context).getCredential(context, request)
            } catch (error: GetCredentialException) {
                throw GoogleSignInUnavailableException()
            }
        val credential = response.credential
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw GoogleSignInUnavailableException()
        }
        return try {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (error: GoogleIdTokenParsingException) {
            throw GoogleSignInUnavailableException()
        }
    }

    private fun generateNonce(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
