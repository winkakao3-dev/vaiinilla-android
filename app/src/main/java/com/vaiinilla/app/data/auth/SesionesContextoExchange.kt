package com.vaiinilla.app.data.auth

import com.vaiinilla.app.core.network.HttpVaiinillaApiClient
import com.vaiinilla.app.data.auth.SesionesContextoDataDto
import com.vaiinilla.app.data.auth.SesionesContextoEnvelopeDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SesionesContextoExchange
    @Inject
    constructor(
        private val apiClient: HttpVaiinillaApiClient,
    ) : ContextoExchanger {
        private val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }

        override fun exchange(
            firebaseIdToken: String,
            membresiaId: String,
        ): SesionesContextoDataDto {
            val body = json.encodeToString(SesionesContextoRequestDto(membresiaId = membresiaId))
            val raw =
                apiClient
                    .postWithBearer(
                        bearer = firebaseIdToken,
                        path = "sesiones/contexto",
                        body = body,
                    ).getOrElse { throw it }
            return json.decodeFromString<SesionesContextoEnvelopeDto>(raw).data
        }
    }

@kotlinx.serialization.Serializable
private data class SesionesContextoRequestDto(
    @kotlinx.serialization.SerialName("membresia_id") val membresiaId: String,
)
