package com.vaiinilla.app.domain.usecase

import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.repository.CatalogRepository
import javax.inject.Inject

class GetCatalogUseCase
    @Inject
    constructor(
        private val repository: CatalogRepository,
    ) {
        operator fun invoke(): Result<Catalog> = repository.getCatalog()
    }
