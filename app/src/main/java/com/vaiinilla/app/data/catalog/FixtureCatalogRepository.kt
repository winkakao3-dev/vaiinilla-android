package com.vaiinilla.app.data.catalog

import com.vaiinilla.app.data.fixture.ContractFixtureParser
import com.vaiinilla.app.data.fixture.FixtureSource
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.ContractRules
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.repository.CatalogRepository

class FixtureCatalogRepository(
    private val fixtureSource: FixtureSource,
    private val parser: ContractFixtureParser,
) : CatalogRepository {
    override fun getCatalog(): Result<Catalog> = runCatching {
        parser.parseCatalog(fixtureSource.read(CATALOG_PATH)).also(ContractRules::validateCatalog)
    }

    override fun getOperationalStatus(): Result<OperationalStatus> = runCatching {
        parser.parseOperationalStatus(fixtureSource.read(OPERATIONAL_STATUS_PATH)).also(
            ContractRules::validateOperationalStatus,
        )
    }

    private companion object {
        const val CATALOG_PATH = "fixtures/catalog.json"
        const val OPERATIONAL_STATUS_PATH = "fixtures/operational_status.json"
    }
}
