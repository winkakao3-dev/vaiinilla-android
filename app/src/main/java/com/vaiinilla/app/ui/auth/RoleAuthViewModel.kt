package com.vaiinilla.app.ui.auth

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.config.AppEnvironment
import com.vaiinilla.app.core.config.DataSourceMode
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.usecase.AuthenticateSeedRoleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

data class RoleAuthUiState(
    val loading: Boolean = false,
    val authenticatingRole: OperationalRole? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class RoleAuthViewModel @Inject constructor(
    private val authenticateSeedRole: AuthenticateSeedRoleUseCase,
    private val environment: AppEnvironment,
) : ViewModel() {
    private val _state = mutableStateOf(RoleAuthUiState())
    val state: State<RoleAuthUiState> = _state

    fun authenticate(role: OperationalRole, onSuccess: () -> Unit) {
        if (environment.dataSourceMode == DataSourceMode.MOCK) {
            onSuccess()
            return
        }

        _state.value = RoleAuthUiState(loading = true, authenticatingRole = role)
        viewModelScope.launch {
            authenticateSeedRole(role).fold(
                onSuccess = {
                    _state.value = RoleAuthUiState()
                    onSuccess()
                },
                onFailure = { error ->
                    _state.value = RoleAuthUiState(
                        errorMessage = error.message ?: "No se pudo iniciar sesión.",
                        authenticatingRole = role,
                    )
                },
            )
        }
    }

    fun clearError() {
        _state.value = RoleAuthUiState()
    }
}
