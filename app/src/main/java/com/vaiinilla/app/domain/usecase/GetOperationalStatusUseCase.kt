package com.vaiinilla.app.domain.usecase

import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.repository.CatalogRepository
import javax.inject.Inject

class GetOperationalStatusUseCase
    @Inject
    constructor(
        private val repository: CatalogRepository,
    ) {
        operator fun invoke(): Result<OperationalStatus> = repository.getOperationalStatus()
    }
