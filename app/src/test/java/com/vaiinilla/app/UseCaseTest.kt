package com.vaiinilla.app

import com.vaiinilla.app.data.catalog.FixtureCatalogRepository
import com.vaiinilla.app.data.fixture.ContractFixtureParser
import com.vaiinilla.app.domain.usecase.GetCatalogUseCase
import com.vaiinilla.app.domain.usecase.GetOperationalStatusUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UseCaseTest {
    private val repository = FixtureCatalogRepository(TestFixtureSource(), ContractFixtureParser())

    @Test
    fun `catalog use case exposes fixture products`() {
        val result = GetCatalogUseCase(repository)()
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrThrow().products.size)
    }

    @Test
    fun `operational status comes from repository`() {
        val result = GetOperationalStatusUseCase(repository)()
        assertTrue(result.getOrThrow().acceptingOrders)
    }
}
