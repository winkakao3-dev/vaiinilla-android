package com.vaiinilla.app.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.domain.model.WalletData
import com.vaiinilla.app.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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

        fun refresh() {
            if (_state.value.loading) return
            _state.value = _state.value.copy(loading = true, error = null)
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { repository.getMyWallet() }
                _state.value =
                    result.fold(
                        onSuccess = { WalletRemoteUiState(data = it) },
                        onFailure = { WalletRemoteUiState(error = it.message ?: "No se pudo consultar la wallet.") },
                    )
            }
        }
    }
