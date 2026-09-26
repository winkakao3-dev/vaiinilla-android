package com.vaiinilla.app.data.operational

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.repository.BoardOrder
import com.vaiinilla.app.domain.repository.BoardTable
import com.vaiinilla.app.domain.repository.CallReason
import com.vaiinilla.app.domain.repository.CallStatus
import com.vaiinilla.app.domain.repository.CallsUnavailableException
import com.vaiinilla.app.domain.repository.OrderRepositoryException
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.domain.repository.WaiterBoard
import com.vaiinilla.app.domain.repository.WaiterRepository

class RemoteWaiterRepository(
    private val apiClient: VaiinillaApiClient,
    private val contractJson: WaiterContractJson,
    private val orderContractJson: OrderContractJson,
) : WaiterRepository {
    private var boardEndpoint = true
    private var callsEnabled = true

    override fun board(): Result<WaiterBoard> =
        runCatching {
            val tables =
                if (boardEndpoint) {
                    try {
                        apiClient.get("espacios/tablero").mapCatching(contractJson::parseBoard).getOrThrow()
                    } catch (error: Throwable) {
                        if (!isMissing(error)) throw error
                        boardEndpoint = false
                        fallbackBoard()
                    }
                } else {
                    fallbackBoard()
                }
            val withCalls =
                if (!boardEndpoint && callsEnabled) {
                    try {
                        val calls =
                            apiClient
                                .get("llamadas", mapOf("estado" to OPEN_CALL_QUERY))
                                .mapCatching(contractJson::parseCalls)
                                .getOrThrow()
                        tables.map { table ->
                            table.copy(call = calls.firstOrNull { it.space.id == table.space.id })
                        }
                    } catch (error: Throwable) {
                        if (!isMissing(error)) throw error
                        callsEnabled = false
                        tables
                    }
                } else {
                    tables
                }
            WaiterBoard(tables = withCalls, callsEnabled = boardEndpoint || callsEnabled)
        }.mapApiErrors()

    private fun fallbackBoard(): List<BoardTable> {
        val spaces =
            apiClient
                .get("espacios")
                .mapCatching(contractJson::parseSpaces)
                .getOrThrow()
        val ready =
            apiClient
                .get("pedidos", mapOf("estado" to OrderState.READY.wireValue))
                .mapCatching(orderContractJson::parseOrderList)
                .getOrThrow()
        return spaces.map { space ->
            val orders =
                ready
                    .filter { it.summary.destination == OrderDestination.IN_SPACE && it.summary.space?.id == space.id }
                    .map { order ->
                        BoardOrder(
                            id = order.summary.id,
                            folio = order.summary.folio,
                            state = order.summary.state,
                            version = order.summary.version,
                            clientName = order.user?.name,
                            itemsSummary = order.items.joinToString(", ") { "${it.quantity}× ${it.productName}" },
                            updatedAt = order.summary.updatedAt,
                        )
                    }
            BoardTable(space = space, call = null, orders = orders)
        }
    }

    override fun transitionCall(
        call: TableCall,
        target: CallStatus,
        idempotencyKey: String,
    ): Result<TableCall> =
        apiClient
            .post(
                path = "llamadas/${call.id}/transiciones",
                body = contractJson.encodeCallTransition(target, call.version),
                headers = mapOf("Idempotency-Key" to idempotencyKey),
            ).mapCatching(contractJson::parseCall)
            .mapApiErrors()

    override fun deliver(
        order: BoardOrder,
        qrToken: String,
        idempotencyKey: String,
    ): Result<Unit> =
        apiClient
            .post(
                path = "pedidos/${order.id}/transiciones",
                body = contractJson.encodeDeliver(order.version, qrToken),
                headers = mapOf("Idempotency-Key" to idempotencyKey),
            ).mapCatching {
                orderContractJson.parseOrderDetail(it)
                Unit
            }.mapApiErrors()

    override fun currentCall(espacioId: Int): Result<TableCall?> =
        runCatching {
            val calls =
                apiClient
                    .get("llamadas", mapOf("estado" to OPEN_CALL_QUERY))
                    .mapCatching(contractJson::parseCalls)
                    .getOrThrow()
            calls.firstOrNull { it.space.id == espacioId }
        }.mapGuardUnavailable()

    override fun call(
        espacioId: Int,
        reason: CallReason,
        orderId: String?,
        idempotencyKey: String,
    ): Result<TableCall> =
        apiClient
            .post(
                path = "espacios/$espacioId/llamadas",
                body = contractJson.encodeBuyerCall(reason, orderId),
                headers = mapOf("Idempotency-Key" to idempotencyKey),
            ).mapCatching(contractJson::parseCall)
            .mapGuardUnavailable()

    override fun cancelCall(
        call: TableCall,
        idempotencyKey: String,
    ): Result<TableCall> =
        apiClient
            .postWithoutBody(
                path = "llamadas/${call.id}/cancelaciones",
                headers = mapOf("Idempotency-Key" to idempotencyKey),
            ).mapCatching(contractJson::parseCall)
            .mapGuardUnavailable()

    private fun isMissing(error: Throwable): Boolean {
        val cause = generateSequence(error) { it.cause }.firstOrNull { it is ApiClientException } as? ApiClientException
        return cause?.httpStatus in listOf(404, 405, 501)
    }

    private fun <T> Result<T>.mapApiErrors(): Result<T> =
        fold(
            onSuccess = { Result.success(it) },
            onFailure = { error ->
                Result.failure(
                    when (error) {
                        is ApiClientException ->
                            OrderRepositoryException(error.code, error.message ?: error.code, error)
                        is OrderRepositoryException -> error
                        is CallsUnavailableException -> error
                        else -> error
                    },
                )
            },
        )

    private fun <T> Result<T>.mapGuardUnavailable(): Result<T> =
        fold(
            onSuccess = { Result.success(it) },
            onFailure = { error ->
                if (isMissing(error)) {
                    Result.failure(CallsUnavailableException())
                } else if (error is ApiClientException) {
                    Result.failure(OrderRepositoryException(error.code, error.message ?: error.code, error))
                } else {
                    Result.failure(error)
                }
            },
        )

    private companion object {
        const val OPEN_CALL_QUERY = "pendiente,en_camino"
    }
}
