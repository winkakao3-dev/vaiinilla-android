package com.vaiinilla.app.domain.usecase

import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.repository.OrderRepository
import javax.inject.Inject

class ListOrdersUseCase @Inject constructor(
    private val repository: OrderRepository,
) {
    operator fun invoke(role: OperationalRole, updatedSince: String? = null): Result<List<OrderDetail>> =
        repository.listOrders(role, updatedSince)
}

class GetOrderUseCase @Inject constructor(
    private val repository: OrderRepository,
) {
    operator fun invoke(orderId: String): Result<OrderDetail> = repository.getOrder(orderId)
}

class CollectCashUseCase @Inject constructor(
    private val repository: OrderRepository,
) {
    operator fun invoke(
        orderId: String,
        amountReceived: String,
        idempotencyKey: String,
    ): Result<OrderDetail> = repository.collectCash(orderId, amountReceived, idempotencyKey)
}

class TransitionOrderUseCase @Inject constructor(
    private val repository: OrderRepository,
) {
    operator fun invoke(
        orderId: String,
        targetState: OrderState,
        expectedVersion: Int,
        idempotencyKey: String,
    ): Result<OrderDetail> = repository.transition(orderId, targetState, expectedVersion, idempotencyKey)
}
