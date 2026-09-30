package com.vaiinilla.app.domain.repository

interface CashSessionRepository {
    fun openSession(
        initialAmount: String,
        idempotencyKey: String,
    ): Result<Unit>

    fun hasActiveSession(): Result<Boolean>

    /** Id de la sesión de caja abierta, o null si no hay. */
    fun activeSessionId(): Result<String?>

    /** Cierra la sesión; el servidor responde CASH_SESSION_HAS_OPEN_ACCOUNTS si hay cuentas por cobrar. */
    fun closeSession(
        sessionId: String,
        finalAmount: String,
        idempotencyKey: String,
    ): Result<Unit>
}
