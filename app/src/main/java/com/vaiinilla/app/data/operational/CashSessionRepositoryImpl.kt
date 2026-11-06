package com.vaiinilla.app.data.operational

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.domain.repository.CashDrawerMovementType
import com.vaiinilla.app.domain.repository.CashDrawerReport
import com.vaiinilla.app.domain.repository.CashLastClose
import com.vaiinilla.app.domain.repository.CashSessionInfo
import com.vaiinilla.app.domain.repository.CashSessionRepository
import com.vaiinilla.app.domain.repository.OrderRepositoryException

class RemoteCashSessionRepository(
    private val apiClient: VaiinillaApiClient,
    private val contractJson: OrderContractJson,
) : CashSessionRepository {
    override fun openSession(
        initialAmount: String,
        idempotencyKey: String,
    ): Result<Unit> =
        apiClient
            .post(
                path = "sesiones-caja",
                body = contractJson.encodeOpenCashSession(initialAmount),
                headers = mapOf("Idempotency-Key" to idempotencyKey),
            ).mapCatching {
                contractJson.parseCashSession(it)
                Unit
            }.mapApiErrors()

    @Volatile
    override var suggestedFloat: String? = null
        private set

    @Volatile
    override var expectedCash: String? = null
        private set

    @Volatile
    override var openedSession: CashSessionInfo? = null
        private set

    @Volatile
    override var lastClose: CashLastClose? = null
        private set

    override fun forgetLastClose() {
        lastClose = null
    }

    private fun readActive(): Result<String> =
        apiClient.get("sesiones-caja/activa").onSuccess { body ->
            suggestedFloat = runCatching { contractJson.parseSuggestedFloat(body) }.getOrNull()
            expectedCash = runCatching { contractJson.parseCashAmount(body, "efectivo_esperado") }.getOrNull()
            lastClose = runCatching { contractJson.parseLastCashClose(body) }.getOrNull() ?: lastClose
            openedSession =
                runCatching {
                    contractJson.parseCashSession(body)?.let { CashSessionInfo(it.id, it.initialAmount, it.openedAt) }
                }.getOrNull()
        }

    override fun hasActiveSession(): Result<Boolean> =
        readActive()
            .mapCatching { contractJson.parseCashSession(it) != null }
            .mapApiErrors()

    override fun activeSessionId(): Result<String?> =
        readActive()
            .mapCatching { contractJson.parseCashSession(it)?.id }
            .mapApiErrors()

    override fun activeSession(): Result<CashSessionInfo?> =
        readActive()
            .mapCatching { body ->
                contractJson.parseCashSession(body)?.let { CashSessionInfo(it.id, it.initialAmount, it.openedAt) }
            }.onSuccess { session ->
                // Con la caja cerrada `activa` no trae el último cierre: se pide aparte.
                // Si falla, queda el que había.
                if (session == null) readLastClose()?.let { lastClose = it }
            }.mapApiErrors()

    private fun readLastClose(): CashLastClose? =
        apiClient
            .get("sesiones-caja/ultimo-cierre")
            .mapCatching { contractJson.parseLastClosedSession(it) }
            .getOrNull()

    override fun closeSession(
        sessionId: String,
        finalAmount: String,
        idempotencyKey: String,
    ): Result<Unit> =
        apiClient
            .post(
                path = "sesiones-caja/$sessionId/cerrar",
                body = contractJson.encodeCloseCashSession(finalAmount),
                headers = mapOf("Idempotency-Key" to idempotencyKey),
            ).map { }
            .mapApiErrors()

    override fun drawerReport(sessionId: String): Result<CashDrawerReport> =
        apiClient
            .get("sesiones-caja/$sessionId/movimientos")
            .mapCatching { CashDrawerJson.parseReport(it) }
            .mapApiErrors()

    override fun registerDrawerMovement(
        sessionId: String,
        type: CashDrawerMovementType,
        amount: String,
        reason: String,
        idempotencyKey: String,
    ): Result<String?> =
        apiClient
            .post(
                path = "sesiones-caja/$sessionId/movimientos",
                body = CashDrawerJson.encodeMovement(type, amount, reason),
                headers = mapOf("Idempotency-Key" to idempotencyKey),
            ).mapCatching { raw ->
                // Ya respondió 2xx: el movimiento quedó aunque el cuerpo no se entienda.
                runCatching { CashDrawerJson.parseExpectedAfterMovement(raw) }.getOrNull()
                    ?.also { expectedCash = it }
            }.mapApiErrors()

    private fun <T> Result<T>.mapApiErrors(): Result<T> =
        fold(
            onSuccess = { Result.success(it) },
            onFailure = { error ->
                Result.failure(
                    when (error) {
                        is ApiClientException ->
                            OrderRepositoryException(error.code, error.message ?: error.code, error)
                        else -> error
                    },
                )
            },
        )
}

class NoOpCashSessionRepository : CashSessionRepository {
    private var open = true

    override fun openSession(
        initialAmount: String,
        idempotencyKey: String,
    ): Result<Unit> {
        open = true
        return Result.success(Unit)
    }

    override fun hasActiveSession(): Result<Boolean> = Result.success(open)

    override fun activeSessionId(): Result<String?> = Result.success(if (open) "local" else null)

    override fun closeSession(
        sessionId: String,
        finalAmount: String,
        idempotencyKey: String,
    ): Result<Unit> {
        open = false
        return Result.success(Unit)
    }
}
