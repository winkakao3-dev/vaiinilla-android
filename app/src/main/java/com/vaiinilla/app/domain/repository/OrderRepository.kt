package com.vaiinilla.app.domain.repository

import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.OrderDetail

interface OrderRepository {
    fun createOrder(request: CreateOrderRequest, idempotencyKey: String): Result<OrderDetail>
}

class OrderRepositoryException(
    val code: String,
    message: String,
) : IllegalStateException(message)
