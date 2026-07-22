package com.vaiinilla.app.data.order

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.repository.OrderRepository
import com.vaiinilla.app.domain.repository.OrderRepositoryException

class RemoteOrderRepository(
    private val apiClient: VaiinillaApiClient,
    private val contractJson: OrderContractJson,
) : OrderRepository {
    override fun createOrder(
        request: CreateOrderRequest,
        idempotencyKey: String,
    ): Result<OrderDetail> = apiClient.post(
        path = "pedidos",
        body = contractJson.encodeCreateRequest(request),
        headers = mapOf("Idempotency-Key" to idempotencyKey),
    ).mapCatching { contractJson.parseOrderDetail(it) }
        .mapApiErrors()

    override fun getOrder(orderId: String): Result<OrderDetail> = apiClient.get("pedidos/$orderId")
        .mapCatching { contractJson.parseOrderDetail(it) }
        .mapApiErrors()

    override fun listOrders(role: OperationalRole, updatedSince: String?): Result<List<OrderDetail>> {
        val query = buildMap {
            updatedSince?.let { put("actualizado_desde", it) }
        }
        return apiClient.get("pedidos", query)
            .mapCatching { contractJson.parseOrderList(it) }
            .mapApiErrors()
    }

    override fun collectCash(
        orderId: String,
        amountReceived: String,
        idempotencyKey: String,
    ): Result<OrderDetail> = getOrder(orderId)
        .mapCatching { current ->
            apiClient.post(
                path = "pedidos/$orderId/cobros-efectivo",
                body = contractJson.encodeCashCollection(
                    amountReceived = amountReceived,
                    expectedVersion = current.summary.version,
                ),
                headers = mapOf("Idempotency-Key" to idempotencyKey),
            ).mapCatching { contractJson.parseCashCollection(it) }
                .getOrThrow()
        }
        .mapApiErrors()

    override fun transition(
        orderId: String,
        targetState: OrderState,
        expectedVersion: Int,
        idempotencyKey: String,
    ): Result<OrderDetail> = apiClient.post(
        path = "pedidos/$orderId/transiciones",
        body = contractJson.encodeTransition(targetState, expectedVersion),
        headers = mapOf("Idempotency-Key" to idempotencyKey),
    ).mapCatching { contractJson.parseOrderDetail(it) }
        .mapApiErrors()

    private fun <T> Result<T>.mapApiErrors(): Result<T> = this.mapError { error ->
        when (error) {
            is ApiClientException -> OrderRepositoryException(error.code, error.message ?: error.code)
            is OrderRepositoryException -> error
            else -> error
        }
    }

    private inline fun <T> Result<T>.mapError(transform: (Throwable) -> Throwable): Result<T> =
        fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(transform(it)) },
        )
}
