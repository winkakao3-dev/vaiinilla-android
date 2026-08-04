package com.vaiinilla.app.domain.usecase

import com.vaiinilla.app.domain.model.ContractRules
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.repository.OrderRepository
import javax.inject.Inject

/** Creates the cash order covered by the live Entrega 01 backend contract. */
class CreateRemoteOrderUseCase
    @Inject
    constructor(
        private val repository: OrderRepository,
    ) {
        operator fun invoke(
            request: CreateOrderRequest,
            idempotencyKey: String,
        ): Result<OrderDetail> =
            runCatching {
                ContractRules.validateRemoteOrderRequest(request)
                repository.createOrder(request, idempotencyKey).getOrThrow()
            }
    }
