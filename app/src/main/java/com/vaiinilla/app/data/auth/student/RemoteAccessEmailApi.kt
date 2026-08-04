package com.vaiinilla.app.data.auth.student

import com.vaiinilla.app.core.network.HttpVaiinillaApiClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Email delivery routes approved by Entrega 02; Firebase remains the identity provider. */
interface AccessEmailApi {
    fun sendVerification(firebaseIdToken: String): Result<Unit>

    fun sendRecovery(email: String): Result<Unit>
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

        override fun sendVerification(firebaseIdToken: String): Result<Unit> =
            apiClient
                .postWithBearer(
                    bearer = firebaseIdToken,
                    path = "publico/correos/verificacion",
                    body = "{}",
                ).mapCatching { raw ->
                    requireAccepted(raw)
                    Unit
                }

        override fun sendRecovery(email: String): Result<Unit> =
            apiClient
                .postPublic(
                    path = "publico/correos/recuperacion",
                    body = json.encodeToString(RecoveryRequestDto(email.trim())),
                ).mapCatching { raw ->
                    requireAccepted(raw)
                    Unit
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
