package com.vaiinilla.app.data.discovery

import com.vaiinilla.app.data.fixture.ContractFixtureParser
import com.vaiinilla.app.data.fixture.FixtureSource
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.domain.model.SpaceResolveResult
import com.vaiinilla.app.domain.repository.DiscoveryRepository
import kotlinx.serialization.json.Json

class FixtureDiscoveryRepository(
    private val fixtureSource: FixtureSource,
    private val parser: ContractFixtureParser,
) : DiscoveryRepository {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    override fun searchEstablishments(
        query: String,
        limit: Int,
        cursor: String?,
    ): Result<Pair<List<PublicEstablishment>, String?>> =
        runCatching {
            val all = loadEstablishments()
            val filtered =
                if (query.isBlank()) {
                    all
                } else {
                    all.filter {
                        it.name.contains(query, ignoreCase = true) ||
                            it.slug.contains(query, ignoreCase = true)
                    }
                }
            filtered
                .sortedWith(compareBy({ it.name.lowercase() }, { it.id }))
                .take(limit.coerceIn(1, 50)) to null
        }

    override fun getEstablishment(slug: String): Result<PublicEstablishment> =
        runCatching {
            loadEstablishments().firstOrNull { it.slug == slug }
                ?: error("Establecimiento no encontrado: $slug")
        }

    override fun getGuestCatalog(slug: String): Result<Catalog> =
        runCatching {
            getEstablishment(slug).getOrThrow()
            // MOCK reuses Entrega 01 catalog fixture for any known slug.
            parser.parseCatalog(fixtureSource.read("fixtures/catalog.json"))
        }

    override fun resolveSpaceToken(token: String): Result<SpaceResolveResult> =
        runCatching {
            val normalized = token.trim()
            if (normalized.isEmpty()) error("Token de espacio vacío.")
            // Fixture tokens: mesa4 / space-mesa-4
            if (
                normalized.equals("mesa4", ignoreCase = true) ||
                normalized.equals("space-mesa-4", ignoreCase = true)
            ) {
                val envelope =
                    json.decodeFromString<SpaceResolveEnvelope>(
                        fixtureSource.read("fixtures/publico_espacio_mesa4.json"),
                    )
                val data = envelope.data
                return@runCatching SpaceResolveResult(
                    establishment = data.establecimiento.toDomain(),
                    space = data.espacio?.toDomain(),
                )
            }
            error("Token de espacio no reconocido en MOCK.")
        }

    private fun loadEstablishments(): List<PublicEstablishment> {
        val envelope =
            json.decodeFromString<EstablishmentListEnvelope>(
                fixtureSource.read("fixtures/publico_establecimientos.json"),
            )
        return envelope.data.map { it.toDomain() }
    }
}
