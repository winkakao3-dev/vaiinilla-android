package com.vaiinilla.app.data.discovery

import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.data.contract.ContractResponseParser
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.GeoCoordinates
import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.domain.model.SpaceResolveResult
import com.vaiinilla.app.domain.repository.DiscoveryRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale

// Punto decimal fijo: con un locale como es-ES el separador sería coma y el backend no lo lee.
private fun Double.toCoordinateParam(): String = String.format(Locale.ROOT, "%.6f", this)

class RemoteDiscoveryRepository(
    private val apiClient: VaiinillaApiClient,
    private val parser: ContractResponseParser,
) : DiscoveryRepository {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    override fun searchEstablishments(
        query: String,
        limit: Int,
        cursor: String?,
        nearTo: GeoCoordinates?,
    ): Result<Pair<List<PublicEstablishment>, String?>> =
        runCatching {
            val params =
                buildMap {
                    if (query.isNotBlank()) put("query", query)
                    put("limit", limit.coerceIn(1, 50).toString())
                    if (!cursor.isNullOrBlank()) put("cursor", cursor)
                    nearTo?.let {
                        put("latitud", it.latitude.toCoordinateParam())
                        put("longitud", it.longitude.toCoordinateParam())
                    }
                }
            val raw =
                apiClient
                    .getPublic("publico/establecimientos", params)
                    .getOrThrow()
            val envelope = json.decodeFromString<EstablishmentListEnvelope>(raw)
            envelope.data.map { it.toDomain() } to envelope.meta?.cursor
        }

    override fun getEstablishment(slug: String): Result<PublicEstablishment> =
        runCatching {
            val raw =
                apiClient
                    .getPublic("publico/establecimientos/$slug")
                    .getOrThrow()
            json.decodeFromString<EstablishmentDetailEnvelope>(raw).data.toDomain()
        }

    override fun getGuestCatalog(slug: String): Result<Catalog> =
        runCatching {
            val raw =
                apiClient
                    .getPublic("publico/establecimientos/$slug/catalogo")
                    .getOrThrow()
            parser.parseCatalog(raw)
        }

    override fun resolveSpaceToken(token: String): Result<SpaceResolveResult> =
        runCatching {
            val normalizedToken = token.trim()
            require(normalizedToken.isNotEmpty()) { "Token de espacio vacío." }
            val body = json.encodeToString(SpaceResolveRequestDto(token = normalizedToken))
            val raw =
                apiClient
                    .postPublic("publico/espacios/resolver", body)
                    .getOrThrow()
            val data = json.decodeFromString<SpaceResolveEnvelope>(raw).data
            SpaceResolveResult(
                establishment = data.establecimiento.toDomain(),
                space = data.espacio?.toDomain(),
            )
        }
}
