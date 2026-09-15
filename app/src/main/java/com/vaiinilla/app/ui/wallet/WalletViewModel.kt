package com.vaiinilla.app.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.model.WalletData
import com.vaiinilla.app.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class WalletRemoteUiState(
    val loading: Boolean = false,
    val data: WalletData? = null,
    val error: String? = null,
)

@HiltViewModel
class WalletViewModel
    @Inject
    constructor(
        private val repository: WalletRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(WalletRemoteUiState())
        val state: StateFlow<WalletRemoteUiState> = _state.asStateFlow()
        private var refreshJob: Job? = null
        private var lastLoadedAtMs: Long = 0L

        fun refresh() {
            if (_state.value.loading) return
            val cached = _state.value.data ?: repository.cachedMyWallet()
            _state.value = _state.value.copy(loading = true, data = cached, error = null)
            refreshJob?.cancel()
            refreshJob =
                viewModelScope.launch {
                    val result = withContext(Dispatchers.IO) { repository.getMyWallet() }
                    _state.value =
                        result.fold(
                            onSuccess = {
                                lastLoadedAtMs = System.currentTimeMillis()
                                WalletRemoteUiState(data = it)
                            },
                            onFailure = {
                                WalletRemoteUiState(
                                    error =
                                        it.toUserFacingMessage("No se pudo consultar la wallet."),
                                )
                            },
                        )
                }
        }

        /**
         * El saldo cambia por actores externos (recarga en Caja, pago en línea):
         * cargarlo una sola vez por sesión lo dejaba obsoleto hasta reiniciar la
         * app. Se reconsulta cuando el dato visible ya tiene edad.
         */
        fun refreshIfStale(maxAgeMs: Long = STALE_AFTER_MS) {
            val hasData = _state.value.data != null
            if (hasData && System.currentTimeMillis() - lastLoadedAtMs < maxAgeMs) return
            refresh()
        }

        fun clearForSessionTermination() {
            refreshJob?.cancel()
            refreshJob = null
            lastLoadedAtMs = 0L
            _state.value = WalletRemoteUiState()
        }

        private companion object {
            const val STALE_AFTER_MS = 20_000L
        }
    }
