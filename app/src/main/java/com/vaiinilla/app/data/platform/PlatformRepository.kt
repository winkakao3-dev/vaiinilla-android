package com.vaiinilla.app.data.platform

import com.vaiinilla.app.core.network.HttpVaiinillaApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class PlatformAuthority(
    @SerialName("es_autoridad") val isAuthority: Boolean = false,
    @SerialName("totp_enrolado") val totpEnrolled: Boolean = false,
)

@Serializable
data class PlatformContext(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Int,
)

@Serializable
data class PlatformStripe(
    @SerialName("stripe_enabled") val enabled: Boolean = false,
    @SerialName("charges_enabled") val chargesEnabled: Boolean = false,
    @SerialName("payouts_enabled") val payoutsEnabled: Boolean = false,
    @SerialName("details_submitted") val detailsSubmitted: Boolean = false,
    @SerialName("estado_onboarding") val onboardingState: String? = null,
)

@Serializable
data class PlatformStore(
    val id: String,
    val nombre: String,
    val slug: String,
    val tipo: String = "cafeteria",
    @SerialName("zona_horaria") val timeZone: String = "America/Chihuahua",
    @SerialName("hora_cierre_forzado") val forcedClose: String = "03:00:00",
    val estado: String = "activo",
    @SerialName("motivo_suspension") val suspensionReason: String? = null,
    val descripcion: String? = null,
    @SerialName("imagen_url") val imageUrl: String? = null,
    val direccion: String? = null,
    val horario: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    @SerialName("instagram_url") val instagramUrl: String? = null,
    @SerialName("facebook_url") val facebookUrl: String? = null,
    @SerialName("tiktok_url") val tiktokUrl: String? = null,
    @SerialName("whatsapp_url") val whatsappUrl: String? = null,
    @SerialName("sitio_web_url") val websiteUrl: String? = null,
    /** `ambos`, `personal` o `ninguno`; null si el servidor todavía no lo conoce. */
    @SerialName("asistente_modo") val assistantMode: String? = null,
    /** Solo viene en la lista: `asignado`, `invitado` o `sin_asignar`. */
    val administrador: String? = null,
    val stripe: PlatformStripe? = null,
)

@Serializable
data class PlatformAssignment(
    val email: String,
    val asignado: Boolean,
    @SerialName("correo_enviado") val mailSent: Boolean = false,
)

@Serializable
data class PlatformMetricsSummary(
    @SerialName("ventas_totales") val sales: String = "0",
    val pedidos: Int = 0,
    @SerialName("ticket_promedio") val averageTicket: String = "0",
    val comisiones: String = "0",
)

@Serializable
data class PlatformOperation(
    @SerialName("establecimientos_activos") val active: Int = 0,
    @SerialName("establecimientos_suspendidos") val suspended: Int = 0,
    @SerialName("invitaciones_pendientes") val invitations: Int = 0,
    @SerialName("sesiones_caja_abiertas") val openRegisters: Int = 0,
    @SerialName("establecimientos_recibiendo") val receiving: Int = 0,
    @SerialName("establecimientos_sin_operacion") val idle: Int = 0,
)

@Serializable
data class PlatformMetrics(
    val resumen: PlatformMetricsSummary = PlatformMetricsSummary(),
    val operacion: PlatformOperation = PlatformOperation(),
)

@Serializable
private data class Envelope<T>(
    val data: T,
    val meta: PageMeta? = null,
)

@Serializable
private data class PageMeta(
    val cursor: String? = null,
)

@Serializable
private data class StripeLink(
    @SerialName("account_link_url") val url: String,
)

@Serializable
private data class MapsPoint(
    val latitud: Double,
    val longitud: Double,
)

data class PlatformStoresPage(
    val stores: List<PlatformStore>,
    val cursor: String?,
)

internal val platformJson =
    Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

/** El dinero llega como texto o número según la consulta; `isLenient` acepta los dos en un `String`. */
internal fun parsePlatformMetrics(raw: String): PlatformMetrics =
    platformJson.decodeFromString<Envelope<PlatformMetrics>>(raw).data

internal fun parsePlatformStores(raw: String): PlatformStoresPage {
    val envelope = platformJson.decodeFromString<Envelope<List<PlatformStore>>>(raw)
    return PlatformStoresPage(envelope.data, envelope.meta?.cursor)
}

internal fun parsePlatformStore(raw: String): PlatformStore =
    platformJson.decodeFromString<Envelope<PlatformStore>>(raw).data

internal fun parsePlatformAssignment(raw: String): PlatformAssignment =
    platformJson.decodeFromString<Envelope<PlatformAssignment>>(raw).data

internal fun encodeStoreFields(fields: Map<String, Any?>): String =
    JsonObject(
        fields.mapValues { (_, value) ->
            when (value) {
                null -> JsonNull
                is Boolean -> JsonPrimitive(value)
                is Number -> JsonPrimitive(value)
                else -> JsonPrimitive(value.toString())
            }
        },
    ).toString()

/** Rutas de `/plataforma`: llevan su propio token de 10 minutos, nunca el contexto del negocio. */
@Singleton
class PlatformRepository
    @Inject
    constructor(
        private val api: HttpVaiinillaApiClient,
    ) {
        suspend fun authority(firebaseToken: String): Result<PlatformAuthority> =
            io {
                api.getWithBearer(firebaseToken, "plataforma/sesiones/autoridad").map {
                    platformJson.decodeFromString<Envelope<PlatformAuthority>>(it).data
                }
            }

        suspend fun context(firebaseToken: String): Result<PlatformContext> =
            io {
                api.postWithBearer(firebaseToken, "plataforma/sesiones/contexto", null).map {
                    platformJson.decodeFromString<Envelope<PlatformContext>>(it).data
                }
            }

        suspend fun metrics(
            token: String,
            from: String,
            to: String,
        ): Result<PlatformMetrics> =
            io {
                api
                    .getWithBearer(token, "plataforma/metricas", mapOf("desde" to from, "hasta" to to))
                    .map(::parsePlatformMetrics)
            }

        suspend fun stores(
            token: String,
            state: String?,
            query: String?,
            cursor: String?,
        ): Result<PlatformStoresPage> =
            io {
                val params =
                    buildMap {
                        put("limit", "50")
                        state?.let { put("estado", it) }
                        query?.takeIf { it.isNotBlank() }?.let { put("query", it.trim()) }
                        cursor?.let { put("cursor", it) }
                    }
                api.getWithBearer(token, "plataforma/establecimientos", params).map(::parsePlatformStores)
            }

        suspend fun create(
            token: String,
            fields: Map<String, Any?>,
            key: String,
        ): Result<PlatformStore> =
            io {
                api
                    .postWithBearer(token, "plataforma/establecimientos", encodeStoreFields(fields), idem(key))
                    .map(::parsePlatformStore)
            }

        suspend fun configure(
            token: String,
            id: String,
            fields: Map<String, Any?>,
            key: String,
        ): Result<PlatformStore> =
            io {
                api
                    .patchWithBearer(token, "plataforma/establecimientos/$id", encodeStoreFields(fields), idem(key))
                    .map(::parsePlatformStore)
            }

        suspend fun uploadImage(
            token: String,
            id: String,
            bytes: ByteArray,
            mime: String,
            key: String,
        ): Result<PlatformStore> =
            io {
                api
                    .putMultipartWithBearer(
                        bearer = token,
                        path = "plataforma/establecimientos/$id/imagen",
                        fieldName = "imagen",
                        filename = "tienda.jpg",
                        mimeType = mime,
                        bytes = bytes,
                        headers = idem(key),
                    ).map(::parsePlatformStore)
            }

        suspend fun setSuspended(
            token: String,
            id: String,
            suspend: Boolean,
            reason: String,
            key: String,
        ): Result<PlatformStore> =
            io {
                val action = if (suspend) "suspender" else "reactivar"
                api
                    .postWithBearer(
                        token,
                        "plataforma/establecimientos/$id/$action",
                        encodeStoreFields(mapOf("motivo" to reason.trim())),
                        idem(key),
                    ).map(::parsePlatformStore)
            }

        suspend fun assignAdmin(
            token: String,
            id: String,
            email: String,
            key: String,
        ): Result<PlatformAssignment> =
            io {
                api
                    .postWithBearer(
                        token,
                        "plataforma/establecimientos/$id/primer-administrador",
                        encodeStoreFields(mapOf("email" to email.trim().lowercase())),
                        idem(key),
                    ).map(::parsePlatformAssignment)
            }

        suspend fun resolveMaps(
            token: String,
            url: String,
        ): Result<Pair<Double, Double>> =
            io {
                api
                    .postWithBearer(
                        token,
                        "plataforma/establecimientos/resolver-maps",
                        encodeStoreFields(mapOf("url" to url.trim())),
                    ).map {
                        // El backend lo manda en el sobre `data`, como todo lo demás de plataforma.
                        val point = platformJson.decodeFromString<Envelope<MapsPoint>>(it).data
                        point.latitud to point.longitud
                    }
            }

        suspend fun stripe(
            token: String,
            id: String,
        ): Result<PlatformStripe?> =
            io {
                api.getWithBearer(token, "plataforma/establecimientos/$id/stripe").map {
                    platformJson.decodeFromString<Envelope<PlatformStripe?>>(it).data
                }
            }

        suspend fun stripeOnboarding(
            token: String,
            id: String,
            key: String,
        ): Result<String> =
            io {
                api
                    .postWithBearer(token, "plataforma/establecimientos/$id/stripe/onboarding", null, idem(key))
                    .map { platformJson.decodeFromString<Envelope<StripeLink>>(it).data.url }
            }

        suspend fun stripeLinkExisting(
            token: String,
            id: String,
            accountId: String?,
            key: String,
        ): Result<Unit> =
            io {
                val body =
                    accountId?.takeIf { it.isNotBlank() }?.let {
                        encodeStoreFields(mapOf("stripe_account_id" to it.trim()))
                    } ?: "{}"
                api
                    .postWithBearer(token, "plataforma/establecimientos/$id/stripe/vincular-existente", body, idem(key))
                    .map { }
            }

        suspend fun stripeSetEnabled(
            token: String,
            id: String,
            enabled: Boolean,
            key: String,
        ): Result<Unit> =
            io {
                api
                    .patchWithBearer(
                        token,
                        "plataforma/establecimientos/$id/stripe/configuracion",
                        encodeStoreFields(mapOf("stripe_enabled" to enabled)),
                        idem(key),
                    ).map { }
            }

        private fun idem(key: String) = mapOf("Idempotency-Key" to key)

        private suspend fun <T> io(block: () -> Result<T>): Result<T> = withContext(Dispatchers.IO) { block() }
    }