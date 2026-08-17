package com.vaiinilla.app.domain.account

interface AccountDeletionRepository {
    suspend fun deleteAccount(
        firebaseIdToken: String,
        idempotencyKey: String,
    ): Result<Unit>
}
