package com.vaiinilla.app.domain.usecase

import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.CreateOrderItem
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.PaymentMethod
import javax.inject.Inject

class BuildCreateOrderRequestUseCase
    @Inject
    constructor() {
        operator fun invoke(
            lines: List<CartLine>,
            kitchenNotes: String,
            paymentMethod: PaymentMethod = PaymentMethod.CASH,
            destination: OrderDestination = OrderDestination.TAKE_AWAY,
            spaceId: Int? = null,
        ): CreateOrderRequest =
            CreateOrderRequest(
                paymentMethod = paymentMethod,
                destination = destination,
                spaceId = spaceId,
                kitchenNotes = kitchenNotes,
                items =
                    lines.map { line ->
                        CreateOrderItem(
                            productId = line.product.id,
                            quantity = line.quantity,
                            optionIds = line.selectedOptionIds.sorted(),
                        )
                    },
            )
    }
