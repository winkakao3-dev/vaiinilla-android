package com.vaiinilla.app.domain.usecase

import com.vaiinilla.app.domain.model.ContractRules
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.repository.OrderRepository
import javax.inject.Inject

class CreateStudentCheckoutUseCase
    @Inject
    constructor(
        private val repository: OrderRepository,
    ) {
        operator fun invoke(
            request: CreateOrderRequest,
            idempotencyKey: String,
        ): Result<OrderDetail> =
            runCatching {
                ContractRules.validateStudentCheckoutRequest(request)
                repository.createStudentCheckout(request, idempotencyKey).getOrThrow()
            }
    }
