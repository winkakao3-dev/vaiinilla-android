package com.vaiinilla.app.data.operational

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.data.order.OrderContractJson
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

    override fun hasActiveSession(): Result<Boolean> =
        apiClient
            .get("sesiones-caja/activa")
            .mapCatching { contractJson.parseCashSession(it) != null }
            .mapApiErrors()

    override fun activeSessionId(): Result<String?> =
        apiClient
            .get("sesiones-caja/activa")
            .mapCatching { contractJson.parseCashSession(it)?.id }
            .mapApiErrors()

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
