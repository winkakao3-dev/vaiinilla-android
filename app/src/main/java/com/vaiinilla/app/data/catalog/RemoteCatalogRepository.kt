package com.vaiinilla.app.data.catalog

import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.repository.CatalogRepository

class RemoteCatalogRepository(
    private val apiClient: VaiinillaApiClient,
) : CatalogRepository {
    override fun getCatalog(): Result<Catalog> = apiClient.get("catalogo").fold(
        onSuccess = {
            Result.failure(IllegalStateException("Falta el adaptador generado desde OpenAPI."))
        },
        onFailure = { Result.failure(it) },
    )

    override fun getOperationalStatus(): Result<OperationalStatus> = apiClient.get("estado-operativo").fold(
        onSuccess = {
            Result.failure(IllegalStateException("Falta el adaptador generado desde OpenAPI."))
        },
        onFailure = { Result.failure(it) },
    )
}
