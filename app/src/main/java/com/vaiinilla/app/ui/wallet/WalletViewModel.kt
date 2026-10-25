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

/** El saldo que se puede enseñar en [establishmentId]: nunca el cargado para otro negocio. */
fun WalletRemoteUiState.visibleFor(establishmentId: String?): WalletRemoteUiState =
    if (data == null || data.belongsTo(establishmentId)) this else copy(data = null)

private fun WalletData.belongsTo(establishmentId: String?): Boolean {
    val owner = wallet.establishmentId
    return establishmentId == null || owner == null || owner == establishmentId
}

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
                            // Un sondeo que falla no borra el saldo que ya se veía: queda el último y el aviso.
                            onFailure = {
                                WalletRemoteUiState(
                                    data = _state.value.data,
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
        fun refreshIfStale(
            establishmentId: String? = null,
            maxAgeMs: Long = STALE_AFTER_MS,
        ) {
            val data = _state.value.data
            // El saldo es por negocio: el de otro lugar no se reutiliza ni como caché.
            if (data != null && !data.belongsTo(establishmentId)) {
                refreshJob?.cancel()
                lastLoadedAtMs = 0L
                _state.value = WalletRemoteUiState()
            } else if (data != null && System.currentTimeMillis() - lastLoadedAtMs < maxAgeMs) {
                return
            }
            refresh()
        }

        /** Un pedido pagado, cancelado o devuelto mueve el saldo: la próxima consulta no espera a que envejezca. */
        fun invalidate() {
            lastLoadedAtMs = 0L
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
