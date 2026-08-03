package com.vaiinilla.app.data.discovery

import com.vaiinilla.app.core.network.HttpVaiinillaApiClient
import com.vaiinilla.app.data.fixture.ContractFixtureParser
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.domain.model.SpaceResolveResult
import com.vaiinilla.app.domain.repository.DiscoveryRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RemoteDiscoveryRepository(
    private val apiClient: HttpVaiinillaApiClient,
    private val parser: ContractFixtureParser,
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
    ): Result<Pair<List<PublicEstablishment>, String?>> =
        runCatching {
            val params =
                buildMap {
                    if (query.isNotBlank()) put("query", query)
                    put("limit", limit.coerceIn(1, 50).toString())
                    if (!cursor.isNullOrBlank()) put("cursor", cursor)
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
            val body = json.encodeToString(SpaceResolveRequestDto(token = token.trim()))
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
