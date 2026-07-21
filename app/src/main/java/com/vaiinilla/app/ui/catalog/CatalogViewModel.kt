package com.vaiinilla.app.ui.catalog

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.vaiinilla.app.core.config.AppEnvironment
import com.vaiinilla.app.domain.usecase.GetCatalogUseCase
import com.vaiinilla.app.domain.usecase.GetOperationalStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CatalogViewModel
    @Inject
    constructor(
        private val getCatalog: GetCatalogUseCase,
        private val getOperationalStatus: GetOperationalStatusUseCase,
        environment: AppEnvironment,
    ) : ViewModel() {
        private val _uiState =
            mutableStateOf(
                CatalogUiState(dataSourceMode = environment.dataSourceMode),
            )
        val uiState: State<CatalogUiState> = _uiState

        init {
            refresh()
        }

        fun refresh() {
            _uiState.value = _uiState.value.copy(loading = true, errorMessage = null)
            val catalog = getCatalog()
            val status = getOperationalStatus()
            val failure = catalog.exceptionOrNull() ?: status.exceptionOrNull()

            _uiState.value =
                CatalogUiState(
                    loading = false,
                    catalog = catalog.getOrNull(),
                    operationalStatus = status.getOrNull(),
                    errorMessage = failure?.message,
                    dataSourceMode = _uiState.value.dataSourceMode,
                )
        }
    }
