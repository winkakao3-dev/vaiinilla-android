package com.vaiinilla.app

import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.data.discovery.RemoteDiscoveryRepository
import com.vaiinilla.app.data.fixture.ContractFixtureParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteDiscoveryRepositoryTest {
    private val source = TestFixtureSource()

    @Test
    fun `remote discovery uses public contract routes and maps responses`() {
        val client = RecordingPublicApiClient()
        client.getResponses["publico/establecimientos"] =
            source.read("fixtures/publico_establecimientos.json").replace(
                "\"cursor\": null",
                "\"cursor\": \"opaque-next\"",
            )
        client.getResponses["publico/establecimientos/cafeteria-centro"] =
            """
            {
              "data": {
                "id": "8246ff44-aad0-4e49-9268-b71c997893fe",
                "nombre": "Cafetería Centro",
                "slug": "cafeteria-centro",
                "identificador_cliente_etiqueta": "Matrícula",
                "identificador_cliente_obligatorio": true
              },
              "meta": { "page": null, "total_pages": null, "total_items": null, "cursor": null },
              "error": null
            }
            """.trimIndent()
        client.getResponses["publico/establecimientos/cafeteria-centro/catalogo"] =
            source.read("fixtures/catalog.json")
        client.postResponses["publico/espacios/resolver"] = source.read("fixtures/publico_espacio_mesa4.json")

        val repository = RemoteDiscoveryRepository(client, ContractFixtureParser())

        val establishments = repository.searchEstablishments("centro", limit = 20, cursor = "opaque").getOrThrow()
        assertEquals("cafeteria-centro", establishments.first.first().slug)
        assertEquals("opaque-next", establishments.second)
        assertEquals(
            PublicRequest.Get(
                path = "publico/establecimientos",
                query = mapOf("query" to "centro", "limit" to "20", "cursor" to "opaque"),
            ),
            client.getRequests.first(),
        )

        assertEquals(
            "cafeteria-centro",
            repository.getEstablishment("cafeteria-centro").getOrThrow().slug,
        )
        assertEquals(
            3,
            repository
                .getGuestCatalog("cafeteria-centro")
                .getOrThrow()
                .products
                .size,
        )

        val resolved = repository.resolveSpaceToken("  live-space-token  ").getOrThrow()
        assertEquals("cafeteria-centro", resolved.establishment.slug)
        assertEquals(12, resolved.space?.id)
        assertEquals(
            PublicRequest.Post(
                path = "publico/espacios/resolver",
                body = "{\"token\":\"live-space-token\"}",
                headers = emptyMap(),
            ),
            client.postRequests.single(),
        )
        assertTrue(client.usedPublicTransport)
        assertFalse(client.usedAuthenticatedTransport)
    }

    @Test
    fun `blank space token is rejected before a remote request`() {
        val client = RecordingPublicApiClient()
        val repository = RemoteDiscoveryRepository(client, ContractFixtureParser())

        val result = repository.resolveSpaceToken("  ")

        assertTrue(result.isFailure)
        assertTrue(client.postRequests.isEmpty())
    }

    @Test
    fun `catalog failure remains primary over authenticated status failure`() {
        val catalogFailure = IllegalStateException("catalog down")
        val statusFailure = IllegalStateException("status requires auth")

        assertEquals(
            catalogFailure,
            com.vaiinilla.app.ui.order.firstGuestVenueFailure(
                Result.failure<Unit>(catalogFailure),
                Result.failure<Unit>(statusFailure),
            ),
        )
        assertEquals(
            statusFailure,
            com.vaiinilla.app.ui.order.firstGuestVenueFailure(
                Result.success(Unit),
                Result.failure<Unit>(statusFailure),
            ),
        )
    }

    private sealed interface PublicRequest {
        data class Get(
            val path: String,
            val query: Map<String, String>,
        ) : PublicRequest

        data class Post(
            val path: String,
            val body: String,
            val headers: Map<String, String>,
        ) : PublicRequest
    }

    private class RecordingPublicApiClient : VaiinillaApiClient {
        override val baseUrl: String = "https://example.invalid/api/v1/"
        val getResponses = mutableMapOf<String, String>()
        val postResponses = mutableMapOf<String, String>()
        val getRequests = mutableListOf<PublicRequest.Get>()
        val postRequests = mutableListOf<PublicRequest.Post>()
        var usedPublicTransport = false
        var usedAuthenticatedTransport = false

        override fun get(
            path: String,
            query: Map<String, String>,
        ): Result<String> {
            usedAuthenticatedTransport = true
            return Result.failure(AssertionError("public discovery used authenticated GET: $path"))
        }

        override fun post(
            path: String,
            body: String,
            headers: Map<String, String>,
        ): Result<String> {
            usedAuthenticatedTransport = true
            return Result.failure(AssertionError("public discovery used authenticated POST: $path"))
        }

        override fun getPublic(
            path: String,
            query: Map<String, String>,
        ): Result<String> {
            usedPublicTransport = true
            getRequests += PublicRequest.Get(path, query)
            return getResponses[path]?.let { value -> Result.success(value) }
                ?: Result.failure(IllegalStateException("GET $path no configurado"))
        }

        override fun postPublic(
            path: String,
            body: String,
            headers: Map<String, String>,
        ): Result<String> {
            usedPublicTransport = true
            postRequests += PublicRequest.Post(path, body, headers)
            return postResponses[path]?.let { value -> Result.success(value) }
                ?: Result.failure(IllegalStateException("POST $path no configurado"))
        }
    }
}
