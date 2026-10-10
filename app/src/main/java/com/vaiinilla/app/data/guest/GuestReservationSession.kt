package com.vaiinilla.app.data.guest

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.VaiinillaApiClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Sin identidad guardada: hay que entrar como invitado antes de ver o apartar canchas. */
class GuestIdentityRequiredException :
    IllegalStateException("Entra como invitado para ver horarios y apartar una cancha.")

/**
 * Identidad de invitado para rentar sin cuenta (docs/compra-sin-cuenta.md del backend). La llave
 * vive en el dispositivo y renueva la sesión de cliente de 15 min. Nunca da de alta un invitado
 * por su cuenta: eso solo ocurre en [enter], tras el aviso de términos en pantalla.
 */
@Singleton
class GuestReservationSession
    @Inject
    constructor(
        private val apiClient: VaiinillaApiClient,
        private val store: GuestSessionStore,
    ) {
        private var cached: CachedBearer? = null

        fun hasIdentity(): Boolean = store.readGuestKey() != null

        fun storedName(): String? = store.readGuestName()

        /** Alta del invitado con su nombre y la aceptación de los términos vigentes. */
        @Synchronized
        fun enter(
            establishmentSlug: String,
            name: String,
        ): Result<Unit> =
            runCatching {
                val legal = fetchLegalVersions()
                val body =
                    buildJsonObject {
                        put("establecimiento_slug", establishmentSlug)
                        put("nombre", name)
                        put("terminos_version", legal.terminosVersion)
                        put("privacidad_version", legal.privacidadVersion)
                    }.toString()
                val raw =
                    apiClient
                        .postPublic(
                            path = "publico/invitados",
                            body = body,
                            headers = mapOf("Idempotency-Key" to UUID.randomUUID().toString()),
                        ).getOrThrow()
                val data = json.decodeFromString<GuestEnvelopeDto>(raw).data
                val key =
                    data.invitado?.llave?.takeIf { it.isNotBlank() }
                        ?: error("El backend no devolvió la llave de invitado.")
                store.saveGuest(name = data.invitado?.nombre?.takeIf { it.isNotBlank() } ?: name, key = key)
                cache(establishmentSlug, data.accessToken)
            }

        /**
         * Sesión de cliente del invitado para [establishmentSlug]: la reutiliza mientras sea reciente
         * y, si no, la renueva con la llave guardada. Si el backend rechaza la llave, la olvida y
         * pide entrar de nuevo.
         */
        @Synchronized
        fun bearer(establishmentSlug: String): Result<String> {
            cached
                ?.takeIf { it.slug == establishmentSlug && System.currentTimeMillis() - it.issuedAtMs < RENEW_AFTER_MS }
                ?.let { return Result.success(it.token) }
            val key = store.readGuestKey() ?: return Result.failure(GuestIdentityRequiredException())
            val body =
                buildJsonObject {
                    put("establecimiento_slug", establishmentSlug)
                    put("llave", key)
                }.toString()
            return runCatching {
                val raw = apiClient.postPublic(path = "publico/invitados/sesiones", body = body).getOrThrow()
                json.decodeFromString<GuestEnvelopeDto>(raw).data.accessToken
            }.onSuccess { token -> cache(establishmentSlug, token) }
                .recoverCatching { error ->
                    // Solo la llave rechazada (401/404) borra la identidad. Un 429 o un 5xx no: el
                    // apartado sigue vivo y la llave sirve para volver a intentarlo.
                    if (error is ApiClientException && error.httpStatus in KEY_REJECTED_STATUSES) {
                        forgetIdentity()
                        throw GuestIdentityRequiredException()
                    }
                    throw error
                }
        }

        /** Descarta la sesión en memoria para que la siguiente llamada renueve (p. ej. tras un 401). */
        @Synchronized
        fun invalidate() {
            cached = null
        }

        /**
         * Ejecuta [request] con el bearer del invitado. Un 401 (sesión vencida) reintenta una vez con
         * una sesión nueva; la clave de idempotencia la pone quien llama, así que no duplica nada.
         */
        fun <T> withBearer(
            establishmentSlug: String,
            request: (bearer: String) -> Result<T>,
        ): Result<T> {
            val token = bearer(establishmentSlug).getOrElse { return Result.failure(it) }
            val result = request(token)
            if ((result.exceptionOrNull() as? ApiClientException)?.httpStatus != HTTP_UNAUTHORIZED) return result
            invalidate()
            val renewed = bearer(establishmentSlug).getOrElse { return Result.failure(it) }
            return request(renewed)
        }

        /** Olvida la identidad de invitado en este dispositivo (p. ej. tras reclamar sus pedidos a una cuenta). */
        @Synchronized
        fun forgetIdentity() {
            cached = null
            store.clearGuest()
        }

        private fun cache(
            slug: String,
            token: String,
        ) {
            cached = CachedBearer(slug = slug, token = token, issuedAtMs = System.currentTimeMillis())
        }

        private fun fetchLegalVersions(): LegalVersionsDto {
            val raw = apiClient.getPublic("publico/legal/vigente").getOrThrow()
            return json.decodeFromString<LegalEnvelopeDto>(raw).data.also { versions ->
                require(versions.terminosVersion.isNotBlank()) {
                    "El backend no devolvió la versión vigente de términos."
                }
                require(versions.privacidadVersion.isNotBlank()) {
                    "El backend no devolvió la versión vigente de privacidad."
                }
            }
        }

        private class CachedBearer(
            val slug: String,
            val token: String,
            val issuedAtMs: Long,
        )

        private companion object {
            val json = Json { ignoreUnknownKeys = true }

            /** La sesión de invitado dura 15 min; se renueva antes para no llegar con un 401. */
            const val RENEW_AFTER_MS = 10 * 60 * 1000L

            const val HTTP_UNAUTHORIZED = 401

            val KEY_REJECTED_STATUSES = setOf(HTTP_UNAUTHORIZED, 404)
        }
    }

@Serializable
private data class GuestEnvelopeDto(
    val data: GuestSessionDataDto,
)

@Serializable
private data class GuestSessionDataDto(
    @SerialName("access_token") val accessToken: String,
    val invitado: GuestDto? = null,
)

@Serializable
private data class GuestDto(
    val nombre: String? = null,
    val llave: String? = null,
)

@Serializable
private data class LegalEnvelopeDto(
    val data: LegalVersionsDto,
)

@Serializable
private data class LegalVersionsDto(
    @SerialName("terminos_version") val terminosVersion: String,
    @SerialName("privacidad_version") val privacidadVersion: String,
)
