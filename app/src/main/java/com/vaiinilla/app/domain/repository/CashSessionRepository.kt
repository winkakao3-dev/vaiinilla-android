package com.vaiinilla.app.domain.repository

interface CashSessionRepository {
    fun openSession(
        initialAmount: String,
        idempotencyKey: String,
    ): Result<Unit>

    fun hasActiveSession(): Result<Boolean>
}
