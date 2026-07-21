package com.vaiinilla.app.data.order

import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.repository.OrderRepository

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
    ).fold(
        onSuccess = {
            Result.failure(IllegalStateException("Falta el adaptador generado desde OpenAPI."))
        },
        onFailure = { Result.failure(it) },
    )
}
