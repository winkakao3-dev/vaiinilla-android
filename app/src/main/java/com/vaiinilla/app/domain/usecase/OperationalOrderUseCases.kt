package com.vaiinilla.app.domain.usecase

import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.repository.CashSessionRepository
import com.vaiinilla.app.domain.repository.OrderRepository
import javax.inject.Inject

class ListOrdersUseCase
    @Inject
    constructor(
        private val repository: OrderRepository,
    ) {
        operator fun invoke(
            role: OperationalRole,
            updatedSince: String? = null,
        ): Result<List<OrderDetail>> = repository.listOrders(role, updatedSince)
    }

class GetOrderUseCase
    @Inject
    constructor(
        private val repository: OrderRepository,
    ) {
        operator fun invoke(orderId: String): Result<OrderDetail> = repository.getOrder(orderId)
    }

class RetryStripePaymentUseCase
    @Inject
    constructor(
        private val repository: OrderRepository,
    ) {
        operator fun invoke(
            orderId: String,
            idempotencyKey: String,
        ) = repository.retryStripePayment(orderId, idempotencyKey)
    }

class CollectCashUseCase
    @Inject
    constructor(
        private val repository: OrderRepository,
    ) {
        operator fun invoke(
            orderId: String,
            amountReceived: String,
            expectedVersion: Int,
            idempotencyKey: String,
        ): Result<OrderDetail> =
            repository.collectCash(
                orderId = orderId,
                amountReceived = amountReceived,
                expectedVersion = expectedVersion,
                idempotencyKey = idempotencyKey,
            )
    }

class TransitionOrderUseCase
    @Inject
    constructor(
        private val repository: OrderRepository,
    ) {
        operator fun invoke(
            orderId: String,
            targetState: OrderState,
            expectedVersion: Int,
            idempotencyKey: String,
            pickupToken: String? = null,
        ): Result<OrderDetail> =
            repository.transition(
                orderId = orderId,
                targetState = targetState,
                expectedVersion = expectedVersion,
                idempotencyKey = idempotencyKey,
                pickupToken = pickupToken,
            )
    }

class OpenCashSessionUseCase
    @Inject
    constructor(
        private val repository: CashSessionRepository,
    ) {
        operator fun invoke(
            initialAmount: String,
            idempotencyKey: String,
        ): Result<Unit> = repository.openSession(initialAmount, idempotencyKey)
    }
