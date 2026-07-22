package com.vaiinilla.app.data.catalog

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.data.fixture.ContractFixtureParser
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.repository.CatalogRepository
import com.vaiinilla.app.domain.repository.CatalogRepositoryException

class RemoteCatalogRepository(
    private val apiClient: VaiinillaApiClient,
    private val parser: ContractFixtureParser,
) : CatalogRepository {
    override fun getCatalog(): Result<Catalog> = apiClient.get("catalogo")
        .mapCatching { parser.parseCatalog(it) }
        .mapApiErrors()

    override fun getOperationalStatus(): Result<OperationalStatus> = apiClient.get("estado-operativo")
        .mapCatching { parser.parseOperationalStatus(it) }
        .mapApiErrors()

    private fun <T> Result<T>.mapApiErrors(): Result<T> = fold(
        onSuccess = { Result.success(it) },
        onFailure = { error ->
            Result.failure(
                when (error) {
                    is ApiClientException -> CatalogRepositoryException(error.code, error.message ?: error.code)
                    is CatalogRepositoryException -> error
                    else -> error
                },
            )
        },
    )
}
