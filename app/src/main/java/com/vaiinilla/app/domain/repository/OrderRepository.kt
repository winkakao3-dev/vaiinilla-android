package com.vaiinilla.app.domain.repository

import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.CreatedOrder
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.StripePaymentSession

interface OrderRepository {
    fun createOrder(
        request: CreateOrderRequest,
        idempotencyKey: String,
    ): Result<CreatedOrder>

    fun getOrder(orderId: String): Result<OrderDetail>

    fun listOrders(
        role: OperationalRole,
        updatedSince: String? = null,
    ): Result<List<OrderDetail>>

    fun retryStripePayment(
        orderId: String,
        idempotencyKey: String,
    ): Result<StripePaymentSession>

    fun collectCash(
        orderId: String,
        amountReceived: String,
        expectedVersion: Int,
        idempotencyKey: String,
    ): Result<OrderDetail>

    fun transition(
        orderId: String,
        targetState: OrderState,
        expectedVersion: Int,
        idempotencyKey: String,
        pickupToken: String? = null,
    ): Result<OrderDetail>
}

class OrderRepositoryException(
    val code: String,
    message: String,
) : IllegalStateException(message)
