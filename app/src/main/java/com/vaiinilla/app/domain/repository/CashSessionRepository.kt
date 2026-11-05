package com.vaiinilla.app.domain.repository

interface CashSessionRepository {
    fun openSession(
        initialAmount: String,
        idempotencyKey: String,
    ): Result<Unit>

    fun hasActiveSession(): Result<Boolean>

    /** El fondo que propuso el servidor en la última consulta de la caja (`fondo_sugerido`), si llegó. */
    val suggestedFloat: String? get() = null

    /** El efectivo que debería haber en la caja abierta (`efectivo_esperado`), si el servidor lo manda. */
    val expectedCash: String? get() = null

    /** La sesión abierta que trajo la última consulta de la caja, con su fondo y su hora; null si no hay o no llegó. */
    val openedSession: CashSessionInfo? get() = null

    /**
     * El último cierre: con la caja abierta llega en `ultimo_cierre`; con la caja cerrada,
     * [activeSession] lo pide a `sesiones-caja/ultimo-cierre`.
     */
    val lastClose: CashLastClose? get() = null

    /** Id de la sesión de caja abierta, o null si no hay. */
    fun activeSessionId(): Result<String?>

    /** La sesión abierta con su fondo y su hora de apertura, o null si no hay. */
    fun activeSession(): Result<CashSessionInfo?> =
        activeSessionId().map { id ->
            id?.let { CashSessionInfo(it, null, null) }
        }

    /** Renglones y totales del cajón de la sesión (`GET sesiones-caja/{id}/movimientos`). */
    fun drawerReport(sessionId: String): Result<CashDrawerReport> =
        Result.failure(UnsupportedOperationException("Este servidor no da los movimientos del cajón."))

    /**
     * Caja mete o saca efectivo del cajón con su motivo (`POST sesiones-caja/{id}/movimientos`). Devuelve el nuevo
     * `efectivo_esperado`, si llegó.
     */
    fun registerDrawerMovement(
        sessionId: String,
        type: CashDrawerMovementType,
        amount: String,
        reason: String,
        idempotencyKey: String,
    ): Result<String?> =
        Result.failure(UnsupportedOperationException("Este servidor no guarda movimientos del cajón."))

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

data class CashLastClose(
    val counted: String,
    /** ISO-8601 en UTC. */
    val closedAt: String?,
    val opening: String? = null,
    val expected: String? = null,
    val closedByMe: Boolean = false,
    /** Ventas en efectivo del turno (`ventas_efectivo`); solo lo trae `ultimo-cierre`. */
    val cashSales: String? = null,
)

/** Entrada o salida de efectivo que Caja anota a mano. */
enum class CashDrawerMovementType(
    val wireValue: String,
) {
    IN("entrada"),
    OUT("salida"),
}

/** `resumen` de los movimientos del cajón: lo mismo que el corte, renglón por renglón. */
data class CashDrawerSummary(
    val opening: String,
    val cashSales: String,
    val tips: String,
    val reloads: String,
    val cashIn: String,
    val cashOut: String,
    val refunds: String,
    val expected: String,
)

/** Un renglón del cajón (`movimientos[]`). */
data class CashDrawerMovement(
    val id: String,
    /** `apertura`, `cobro`, `propina`, `wallet_recharge`, `ajuste_entrada` o `ajuste_salida`. */
    val type: String,
    /** Sale del cajón (resta del esperado). */
    val outgoing: Boolean,
    val amount: String,
    val reason: String?,
    val registeredBy: String?,
    val orderFolio: Int?,
    /** Una salida con pedido o devolución la generó el sistema al devolver dinero. */
    val isRefund: Boolean,
    /** ISO-8601 en UTC. */
    val createdAt: String,
)

data class CashDrawerReport(
    val summary: CashDrawerSummary,
    val movements: List<CashDrawerMovement>,
)

/** Motivo de una entrada o salida a mano: lo mismo que acepta el servidor. */
object CashDrawerRules {
    const val REASON_MIN = 3
    const val REASON_MAX = 160

    fun validReason(reason: String): Boolean = reason.trim().length in REASON_MIN..REASON_MAX
}
