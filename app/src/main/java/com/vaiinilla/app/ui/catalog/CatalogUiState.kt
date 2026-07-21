package com.vaiinilla.app.ui.catalog

import com.vaiinilla.app.core.config.DataSourceMode
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.OperationalStatus

data class CatalogUiState(
    val loading: Boolean = true,
    val catalog: Catalog? = null,
    val operationalStatus: OperationalStatus? = null,
    val errorMessage: String? = null,
    val dataSourceMode: DataSourceMode = DataSourceMode.MOCK,
)
