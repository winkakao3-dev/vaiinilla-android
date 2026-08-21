package com.vaiinilla.app.domain.usecase

import com.vaiinilla.app.domain.model.ContractRules
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.CreatedOrder
import com.vaiinilla.app.domain.repository.OrderRepository
import javax.inject.Inject

class CreateOrderUseCase
    @Inject
    constructor(
        private val repository: OrderRepository,
    ) {
        operator fun invoke(
            request: CreateOrderRequest,
            idempotencyKey: String,
        ): Result<CreatedOrder> =
            runCatching {
                ContractRules.validateCreateOrderRequest(request)
                repository.createOrder(request, idempotencyKey).getOrThrow()
            }
    }
