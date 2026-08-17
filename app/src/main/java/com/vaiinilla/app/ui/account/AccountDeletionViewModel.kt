package com.vaiinilla.app.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.auth.StudentSessionCleanup
import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.data.auth.student.StudentAuthUserNotFoundException
import com.vaiinilla.app.domain.account.AccountDeletionRepository
import com.vaiinilla.app.domain.auth.student.StudentAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AccountDeletionViewModel
    @Inject
    constructor(
        private val authRepository: StudentAuthRepository,
        private val accountDeletionRepository: AccountDeletionRepository,
        private val sessionCleanup: StudentSessionCleanup,
    ) : ViewModel() {
        private val _state = androidx.compose.runtime.mutableStateOf(AccountDeletionUiState())
        val state: androidx.compose.runtime.State<AccountDeletionUiState> = _state

        private var idempotencyKey: String? = null

        fun requestConfirmation() {
            if (_state.value.status != AccountDeletionStatus.Idle) return
            _state.value = AccountDeletionUiState(status = AccountDeletionStatus.Confirmation)
        }

        fun cancel() {
            idempotencyKey = null
            _state.value = AccountDeletionUiState()
        }

        fun confirm() {
            if (_state.value.status != AccountDeletionStatus.Confirmation) return
            idempotencyKey = UUID.randomUUID().toString()
            _state.value = AccountDeletionUiState(status = AccountDeletionStatus.Reauthentication())
        }

        fun submitPassword(
            password: String,
            onDeleted: () -> Unit = {},
            onSessionInvalidated: () -> Unit = {},
        ) {
            val status = _state.value.status
            if (status !is AccountDeletionStatus.Reauthentication || status.busy) return
            if (password.isBlank()) {
                _state.value = _state.value.copy(errorMessage = "Ingresa tu contraseña para continuar.")
                return
            }
            val key = idempotencyKey ?: return
            _state.value = _state.value.copy(status = status.copy(busy = true), errorMessage = null)
            viewModelScope.launch {
                authRepository.reauthenticateWithPassword(password).fold(
                    onSuccess = {
                        _state.value = AccountDeletionUiState(status = AccountDeletionStatus.Deleting)
                        deleteWithToken(
                            idempotencyKey = key,
                            onDeleted = onDeleted,
                            onSessionInvalidated = onSessionInvalidated,
                        )
                    },
                    onFailure = { error ->
                        if (error is StudentAuthUserNotFoundException) {
                            invalidateLocalSession(onSessionInvalidated)
                        } else {
                            _state.value =
                                AccountDeletionUiState(
                                    status = AccountDeletionStatus.Reauthentication(),
                                    errorMessage = error.toUserFacingMessage("No pudimos confirmar tu contraseña."),
                                )
                        }
                    },
                )
            }
        }

        fun retry(
            onDeleted: () -> Unit = {},
            onSessionInvalidated: () -> Unit = {},
        ) {
            val status = _state.value.status as? AccountDeletionStatus.RecoverableError ?: return
            if (status.retryable.not()) return
            val key = idempotencyKey ?: return
            _state.value = AccountDeletionUiState(status = AccountDeletionStatus.Deleting)
            viewModelScope.launch {
                deleteWithToken(
                    idempotencyKey = key,
                    onDeleted = onDeleted,
                    onSessionInvalidated = onSessionInvalidated,
                )
            }
        }

        private suspend fun deleteWithToken(
            idempotencyKey: String,
            onDeleted: () -> Unit,
            onSessionInvalidated: () -> Unit,
        ) {
            authRepository.getIdToken(forceRefresh = true).fold(
                onSuccess = { firebaseIdToken ->
                    accountDeletionRepository.deleteAccount(firebaseIdToken, idempotencyKey).fold(
                        onSuccess = {
                            sessionCleanup.clear()
                            this@AccountDeletionViewModel.idempotencyKey = null
                            _state.value =
                                AccountDeletionUiState(
                                    status = AccountDeletionStatus.Success,
                                    errorMessage = "Tu cuenta fue eliminada correctamente",
                                )
                            onDeleted()
                        },
                        onFailure = { error -> handleApiFailure(error) },
                    )
                },
                onFailure = { error ->
                    if (error is StudentAuthUserNotFoundException) {
                        invalidateLocalSession(onSessionInvalidated)
                    } else {
                        _state.value =
                            AccountDeletionUiState(
                                status = AccountDeletionStatus.RecoverableError(),
                                errorMessage = error.toUserFacingMessage(),
                            )
                    }
                },
            )
        }

        private fun handleApiFailure(error: Throwable) {
            val apiError = error as? ApiClientException
            when (apiError?.code?.uppercase()) {
                "REAUTHENTICATION_REQUIRED",
                "UNAUTHENTICATED",
                ->
                    _state.value =
                        AccountDeletionUiState(
                            status = AccountDeletionStatus.Reauthentication(),
                            errorMessage = "Confirma tu contraseña otra vez para continuar.",
                        )
                else ->
                    _state.value =
                        AccountDeletionUiState(
                            status = AccountDeletionStatus.RecoverableError(),
                            errorMessage = accountDeletionErrorMessage(apiError ?: error),
                        )
            }
        }

        private fun invalidateLocalSession(onSessionInvalidated: () -> Unit) {
            viewModelScope.launch {
                sessionCleanup.clear()
                idempotencyKey = null
                _state.value = AccountDeletionUiState()
                onSessionInvalidated()
            }
        }

        private fun accountDeletionErrorMessage(error: Throwable): String =
            when ((error as? ApiClientException)?.code?.uppercase()) {
                "ACCOUNT_DELETION_CONFIRMATION_REQUIRED" ->
                    "No se pudo confirmar la eliminación. Inténtalo de nuevo."
                "IDENTITY_NOT_FOUND" ->
                    "No encontramos tu identidad en Vaiinilla. Inténtalo de nuevo."
                "ACCOUNT_DELETION_ALREADY_REQUESTED" ->
                    "La eliminación ya fue solicitada. Inténtalo de nuevo para confirmar su estado."
                "ACCOUNT_DELETION_FAILED" ->
                    "El servidor no pudo completar la eliminación. Tu sesión sigue activa; inténtalo de nuevo."
                else -> error.toUserFacingMessage()
            }
    }
