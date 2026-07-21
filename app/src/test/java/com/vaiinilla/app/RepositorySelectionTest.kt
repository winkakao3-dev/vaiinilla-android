package com.vaiinilla.app

import com.vaiinilla.app.core.config.DataSourceMode
import com.vaiinilla.app.core.network.EmptyVaiinillaApiClient
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
    fun `remote repository stops instead of inventing endpoints`() {
        val repository =
            RemoteCatalogRepository(
                EmptyVaiinillaApiClient("https://example.invalid/api/v1/"),
            )
        val result = repository.getCatalog()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("OpenAPI aprobado") == true)
    }
}
