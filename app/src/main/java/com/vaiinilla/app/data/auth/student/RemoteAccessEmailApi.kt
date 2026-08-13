package com.vaiinilla.app.data.auth.student

import com.vaiinilla.app.core.network.HttpVaiinillaApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Email delivery routes approved by Entrega 02; Firebase remains the identity provider. */
interface AccessEmailApi {
    suspend fun sendVerification(firebaseIdToken: String): Result<Unit>

    suspend fun sendRecovery(email: String): Result<Unit>

    suspend fun currentLegal(): Result<LegalDocuments>
}

@Singleton
class RemoteAccessEmailApi
    @Inject
    constructor(
        private val apiClient: HttpVaiinillaApiClient,
    ) : AccessEmailApi {
        private val json =
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }

        override suspend fun sendVerification(firebaseIdToken: String): Result<Unit> =
            withContext(Dispatchers.IO) {
                apiClient
                    .postWithBearer(
                        bearer = firebaseIdToken,
                        path = "publico/correos/verificacion",
                    ).mapCatching { raw ->
                        requireAccepted(raw)
                        Unit
                    }
            }

        override suspend fun sendRecovery(email: String): Result<Unit> =
            withContext(Dispatchers.IO) {
                apiClient
                    .postPublic(
                        path = "publico/correos/recuperacion",
                        body = json.encodeToString(RecoveryRequestDto(email.trim().lowercase())),
                    ).mapCatching { raw ->
                        requireAccepted(raw)
                        Unit
                    }
            }

        override suspend fun currentLegal(): Result<LegalDocuments> =
            withContext(Dispatchers.IO) {
                runCatching {
                    val raw = apiClient.getPublic("publico/legal/vigente").getOrThrow()
                    val versions = json.decodeFromString<EmailLegalEnvelopeDto>(raw).data
                    LegalDocuments(
                        termsVersion = versions.terminosVersion,
                        termsUrl = versions.terminosUrl,
                        privacyVersion = versions.privacidadVersion,
                        privacyUrl = versions.privacidadUrl,
                    )
                }
            }

        private fun requireAccepted(raw: String) {
            val response = json.decodeFromString<EmailDispatchEnvelopeDto>(raw)
            require(response.data.accepted) {
                "El backend no aceptó el envío del correo."
            }
        }
    }

@Serializable
private data class RecoveryRequestDto(
    val email: String,
)

@Serializable
private data class EmailDispatchEnvelopeDto(
    val data: EmailDispatchDataDto,
)

@Serializable
private data class EmailDispatchDataDto(
    @SerialName("aceptado") val accepted: Boolean,
)

@Serializable
private data class EmailLegalEnvelopeDto(
    val data: EmailLegalVersionsDto,
)

@Serializable
private data class EmailLegalVersionsDto(
    @SerialName("terminos_version") val terminosVersion: String,
    @SerialName("terminos_url") val terminosUrl: String = "",
    @SerialName("privacidad_version") val privacidadVersion: String,
    @SerialName("privacidad_url") val privacidadUrl: String = "",
)

data class LegalDocuments(
    val termsVersion: String,
    val termsUrl: String,
    val privacyVersion: String,
    val privacyUrl: String,
)
