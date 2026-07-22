package com.vaiinilla.app.domain.repository

import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.OperationalStatus

interface CatalogRepository {
    fun getCatalog(): Result<Catalog>
    fun getOperationalStatus(): Result<OperationalStatus>
}

class CatalogRepositoryException(
    val code: String,
    message: String,
) : IllegalStateException(message)
