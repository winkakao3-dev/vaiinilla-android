package com.vaiinilla.app.domain.repository

interface CashSessionRepository {
    fun openSession(
        initialAmount: String,
        idempotencyKey: String,
    ): Result<Unit>

    fun hasActiveSession(): Result<Boolean>

    /** Id de la sesión de caja abierta, o null si no hay. */
    fun activeSessionId(): Result<String?>

    /** La sesión abierta con su fondo y su hora de apertura, o null si no hay. */
    fun activeSession(): Result<CashSessionInfo?> =
        activeSessionId().map { id ->
            id?.let { CashSessionInfo(it, null, null) }
        }

    /** Cierra la sesión; el servidor responde CASH_SESSION_HAS_OPEN_ACCOUNTS si hay cuentas por cobrar. */
    fun closeSession(
        sessionId: String,
        finalAmount: String,
        idempotencyKey: String,
    ): Result<Unit>
}

data class CashSessionInfo(
    val id: String,
    /** Fondo con el que se abrió (`monto_inicial`). */
    val initialAmount: String?,
    /** ISO-8601 en UTC (`abierta_en`). */
    val openedAt: String?,
)
