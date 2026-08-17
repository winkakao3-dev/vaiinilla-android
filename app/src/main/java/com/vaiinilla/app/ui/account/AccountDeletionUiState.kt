package com.vaiinilla.app.ui.account

sealed interface AccountDeletionStatus {
    data object Idle : AccountDeletionStatus

    data object Confirmation : AccountDeletionStatus

    data class Reauthentication(
        val busy: Boolean = false,
    ) : AccountDeletionStatus

    data object Deleting : AccountDeletionStatus

    data class RecoverableError(
        val retryable: Boolean = true,
    ) : AccountDeletionStatus

    data object Success : AccountDeletionStatus
}

data class AccountDeletionUiState(
    val status: AccountDeletionStatus = AccountDeletionStatus.Idle,
    val errorMessage: String? = null,
)
