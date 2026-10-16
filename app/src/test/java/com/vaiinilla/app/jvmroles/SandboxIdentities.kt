package com.vaiinilla.app.jvmroles

import android.content.Context
import com.vaiinilla.app.BuildConfig
import com.vaiinilla.app.R
import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Personas del sandbox de dev con sesión real de Firebase, sin el SDK de Firebase: test-setup da un
 * custom token por rol y el REST de Firebase lo cambia por un ID token, igual que vaiinilla-roles/src/login.js.
 * El secreto se lee de ~/.vaiinilla-test-accounts.json en cada uso (en la nube lo pone el proxy, ver
 * [testSetupHeaders]). Ni el secreto ni los tokens se imprimen.
 */
@Singleton
class SandboxIdentities
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private class Identity(
            val session: StudentAuthSession,
            val idToken: String,
            val refreshToken: String,
            val expiresAtMillis: Long,
        )

        private val identities = ConcurrentHashMap<HarnessRole, Identity>()
        private val apiKey by lazy { context.getString(R.string.google_api_key) }

        fun session(role: HarnessRole): StudentAuthSession? = identities[role]?.session

        @Synchronized
        fun signIn(role: HarnessRole): StudentAuthSession {
            identities[role]?.let { return it.session }
            val customToken = customTokenFor(role)
            val body = JSONObject().put("token", customToken).put("returnSecureToken", true).toString()
            val json = post("$IDENTITY_URL$apiKey", body, "application/json")
            val identity =
                identityFrom(json.getString("idToken"), json.getString("refreshToken"), json.getString("expiresIn"))
            identities[role] = identity
            return identity.session
        }

        @Synchronized
        fun idToken(
            role: HarnessRole,
            forceRefresh: Boolean,
        ): String {
            val current = identities[role] ?: throw IllegalStateException("${role.cliName} no ha iniciado sesión.")
            if (!forceRefresh && current.expiresAtMillis - System.currentTimeMillis() > REFRESH_MARGIN_MILLIS) {
                return current.idToken
            }
            val body = "grant_type=refresh_token&refresh_token=${URLEncoder.encode(current.refreshToken, "UTF-8")}"
            val json = post("$SECURE_TOKEN_URL$apiKey", body, "application/x-www-form-urlencoded")
            val refreshed =
                identityFrom(json.getString("id_token"), json.getString("refresh_token"), json.getString("expires_in"))
            identities[role] = refreshed
            return refreshed.idToken
        }

        fun forget(role: HarnessRole) {
            identities.remove(role)
        }

        private fun customTokenFor(role: HarnessRole): String {
            check(!BuildConfig.IS_PRODUCTION) { "El arnés solo corre contra development." }
            // Tráfico del arnés, no de la app: ni el sabotaje ni el solo lectura del vigía lo tocan.
            val json =
                VigiaNetwork.accounted {
                    post(
                        url = "${BuildConfig.API_BASE_URL}dev/test-setup",
                        body = "",
                        contentType = "application/json",
                        headers = testSetupHeaders(),
                        showErrorBody = false,
                    )
                }
            return json
                .getJSONObject("data")
                .getJSONObject("roles")
                .getJSONObject(role.sandboxKey)
                .getString("firebase_custom_token")
        }

        private fun identityFrom(
            idToken: String,
            refreshToken: String,
            expiresInSeconds: String,
        ): Identity {
            val claims = JSONObject(String(Base64.getUrlDecoder().decode(idToken.split('.')[1])))
            val provider = claims.optJSONObject("firebase")?.optString("sign_in_provider")
            val session =
                StudentAuthSession(
                    uid = claims.getString("user_id"),
                    email = claims.optString("email"),
                    displayName = claims.optString("name"),
                    // Las personas del sandbox entran por custom token y no tienen correo. La app pide correo
                    // verificado solo del lado del cliente; el backend valida el ID token, igual que en la web del staff.
                    emailVerified = claims.optBoolean("email_verified", false) || provider == "custom",
                )
            return Identity(
                session,
                idToken,
                refreshToken,
                System.currentTimeMillis() + expiresInSeconds.toLong() * 1000,
            )
        }

        private fun post(
            url: String,
            body: String,
            contentType: String,
            headers: Map<String, String> = emptyMap(),
            showErrorBody: Boolean = true,
        ): JSONObject {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", contentType)
            headers.forEach(connection::setRequestProperty)
            connection.outputStream.use { it.write(body.toByteArray()) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                // Firebase responde {"error":{"message":"..."}} sin datos sensibles; test-setup podría traer tokens.
                val detail =
                    if (showErrorBody) {
                        runCatching {
                            JSONObject(text).getJSONObject("error").getString("message")
                        }.getOrNull()
                    } else {
                        null
                    }
                throw IllegalStateException(
                    "POST ${URL(url).host}${URL(url).path} respondió $status${detail?.let { ": $it" }.orEmpty()}",
                )
            }
            return JSONObject(text)
        }

        private companion object {
            const val IDENTITY_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithCustomToken?key="
            const val SECURE_TOKEN_URL = "https://securetoken.googleapis.com/v1/token?key="
            const val REFRESH_MARGIN_MILLIS = 60_000L
        }
    }

/**
 * Header de test-setup. En una sesión de Claude Code en la nube no hay archivo: el secreto de red del
 * entorno agrega X-Test-Setup-Secret después de que la petición sale de la VM, así que va sin header.
 */
internal fun testSetupHeaders(
    home: File = File(System.getProperty("user.home")),
    env: Map<String, String> = System.getenv(),
): Map<String, String> {
    val accounts = File(home, ".vaiinilla-test-accounts.json")
    if (env["CLAUDE_CODE_REMOTE"] == "true" && !accounts.exists()) return emptyMap()
    return mapOf("X-Test-Setup-Secret" to JSONObject(accounts.readText()).getString("test_setup_secret"))
}
