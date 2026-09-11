package com.vaiinilla.app.ui.account

import com.vaiinilla.app.domain.auth.student.StudentAuthMfaChallenge

sealed interface AccountDeletionStatus {
    data object Idle : AccountDeletionStatus

    data object Confirmation : AccountDeletionStatus

    data class Reauthentication(
        val busy: Boolean = false,
    ) : AccountDeletionStatus

    data class MfaChallenge(
        val challenge: StudentAuthMfaChallenge,
        val code: String = "",
        val factorUid: String? = null,
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
