package com.vaiinilla.app

import com.vaiinilla.app.core.config.DataSourceMode
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.data.catalog.FixtureCatalogRepository
import com.vaiinilla.app.data.catalog.RemoteCatalogRepository
import com.vaiinilla.app.data.fixture.ContractFixtureParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositorySelectionTest {
    @Test
    fun `data source mode parses case-insensitively`() {
        assertEquals(DataSourceMode.MOCK, DataSourceMode.from("mock"))
        assertEquals(DataSourceMode.REMOTE, DataSourceMode.from("REMOTE"))
    }

    @Test
    fun `fixture repository reads canonical JSON assets`() {
        val repository = FixtureCatalogRepository(TestFixtureSource(), ContractFixtureParser())
        assertTrue(repository.getCatalog().isSuccess)
        assertEquals(
            3,
            repository
                .getCatalog()
                .getOrThrow()
                .products.size,
        )
    }

    @Test
    fun `remote catalog repository parses api envelope`() {
        val source = TestFixtureSource()
        val catalogJson = source.read("fixtures/catalog.json")
        val repository =
            RemoteCatalogRepository(
                RecordingApiClient(
                    responses =
                        mapOf(
                            "catalogo" to catalogJson,
                        ),
                ),
                ContractFixtureParser(),
            )
        val result = repository.getCatalog()
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrThrow().products.size)
    }

    private class RecordingApiClient(
        private val responses: Map<String, String>,
    ) : VaiinillaApiClient {
        override val baseUrl: String = "https://example.invalid/api/v1/"

        override fun get(
            path: String,
            query: Map<String, String>,
        ): Result<String> =
            responses[path]?.let { Result.success(it) }
                ?: Result.failure(IllegalStateException("GET $path no configurado"))

        override fun post(
            path: String,
            body: String,
            headers: Map<String, String>,
        ): Result<String> = Result.failure(IllegalStateException("POST no configurado"))
    }
}
