package com.vaiinilla.app.data.auth

import com.vaiinilla.app.core.network.HttpVaiinillaApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
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
                explicitNulls = false
            }

        override suspend fun exchange(
            firebaseIdToken: String,
            establecimientoSlug: String,
            establecimientoId: String,
            identificadorCliente: String?,
        ): SesionesContextoDataDto =
            withContext(Dispatchers.IO) {
                val body =
                    json.encodeToString(
                        SesionesContextoClienteRequestDto(
                            establecimientoSlug = establecimientoSlug,
                            identificadorCliente = identificadorCliente,
                        ),
                    )
                val raw =
                    apiClient
                        .postWithBearer(
                            bearer = firebaseIdToken,
                            path = "sesiones/contexto-cliente",
                            body = body,
                            headers = mapOf("Idempotency-Key" to UUID.randomUUID().toString()),
                        ).getOrElse { throw it }
                json.decodeFromString<SesionesContextoEnvelopeDto>(raw).data
            }

        /** Existing seed-role exchange; VAI-26 uses the client-context overload above. */
        suspend fun exchange(
            firebaseIdToken: String,
            membresiaId: String,
        ): SesionesContextoDataDto =
            withContext(Dispatchers.IO) {
                val body = json.encodeToString(SesionesContextoRequestDto(membresiaId = membresiaId))
                val raw =
                    apiClient
                        .postWithBearer(
                            bearer = firebaseIdToken,
                            path = "sesiones/contexto",
                            body = body,
                            headers = mapOf("Idempotency-Key" to UUID.randomUUID().toString()),
                        ).getOrElse { throw it }
                json.decodeFromString<SesionesContextoEnvelopeDto>(raw).data
            }
    }

@kotlinx.serialization.Serializable
private data class SesionesContextoClienteRequestDto(
    @kotlinx.serialization.SerialName("establecimiento_slug") val establecimientoSlug: String,
    @kotlinx.serialization.SerialName("identificador_cliente") val identificadorCliente: String?,
)

@kotlinx.serialization.Serializable
private data class SesionesContextoRequestDto(
    @kotlinx.serialization.SerialName("membresia_id") val membresiaId: String,
)
