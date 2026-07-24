package com.vaiinilla.app.data.order

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.core.security.PickupTokenStore
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.repository.OrderRepository
import com.vaiinilla.app.domain.repository.OrderRepositoryException

class RemoteOrderRepository(
    private val apiClient: VaiinillaApiClient,
    private val contractJson: OrderContractJson,
    private val pickupTokenStore: PickupTokenStore,
) : OrderRepository {
    override fun createOrder(
        request: CreateOrderRequest,
        idempotencyKey: String,
    ): Result<OrderDetail> = apiClient.post(
        path = "pedidos",
        body = contractJson.encodeCreateRequest(request),
        headers = mapOf("Idempotency-Key" to idempotencyKey),
    ).mapCatching { contractJson.parseOrderDetail(it) }
        .mapCatching { pickupTokenStore.attach(it) }
        .mapApiErrors()

    override fun createStudentCheckout(
        request: CreateOrderRequest,
        idempotencyKey: String,
    ): Result<OrderDetail> = Result.failure(
        OrderRepositoryException(
            code = "STUDENT_CHECKOUT_UNSUPPORTED",
            message = "Saldo, tarjeta y mesa están disponibles en modo demo local.",
        ),
    )

    override fun getOrder(orderId: String): Result<OrderDetail> = apiClient.get("pedidos/$orderId")
        .mapCatching { contractJson.parseOrderDetail(it) }
        .mapCatching { pickupTokenStore.attach(it) }
        .mapApiErrors()

    override fun listOrders(role: OperationalRole, updatedSince: String?): Result<List<OrderDetail>> {
        val query = buildMap {
            updatedSince?.let { put("actualizado_desde", it) }
        }
        return apiClient.get("pedidos", query)
            .mapCatching { contractJson.parseOrderList(it) }
            .mapCatching { orders -> orders.map(pickupTokenStore::attach) }
            .mapApiErrors()
    }

    override fun collectCash(
        orderId: String,
        amountReceived: String,
        expectedVersion: Int,
        idempotencyKey: String,
    ): Result<OrderDetail> {
        val cachedToken = pickupTokenStore.read(orderId)
        return apiClient.post(
            path = "pedidos/$orderId/cobros-efectivo",
            body = contractJson.encodeCashCollection(
                amountReceived = amountReceived,
                expectedVersion = expectedVersion,
            ),
            headers = mapOf("Idempotency-Key" to idempotencyKey),
        ).mapCatching { contractJson.parseCashCollection(it) }
            .mapCatching { pickupTokenStore.attach(it.copy(pickupToken = it.pickupToken ?: cachedToken)) }
            .mapApiErrors()
    }

    override fun transition(
        orderId: String,
        targetState: OrderState,
        expectedVersion: Int,
        idempotencyKey: String,
        pickupToken: String?,
    ): Result<OrderDetail> {
        val token = pickupToken ?: pickupTokenStore.read(orderId)
        return apiClient.post(
            path = "pedidos/$orderId/transiciones",
            body = contractJson.encodeTransition(targetState, expectedVersion, token),
            headers = mapOf("Idempotency-Key" to idempotencyKey),
        ).mapCatching { contractJson.parseOrderDetail(it) }
            .mapCatching { pickupTokenStore.attach(it.copy(pickupToken = it.pickupToken ?: token)) }
            .mapApiErrors()
    }

    private fun <T> Result<T>.mapApiErrors(): Result<T> = fold(
        onSuccess = { Result.success(it) },
        onFailure = { error ->
            Result.failure(
                when (error) {
                    is ApiClientException -> OrderRepositoryException(error.code, error.message ?: error.code)
                    is OrderRepositoryException -> error
                    else -> error
                },
            )
        },
    )
}
