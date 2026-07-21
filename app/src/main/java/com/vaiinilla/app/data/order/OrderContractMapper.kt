package com.vaiinilla.app.data.order

import com.vaiinilla.app.domain.model.CreateOrderItem
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderItem
import com.vaiinilla.app.domain.model.OrderItemOption
import com.vaiinilla.app.domain.model.OrderSpace
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.OrderUser
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.PreparationStation

fun CreateOrderRequest.toDto(): CreateOrderRequestDto = CreateOrderRequestDto(
    paymentMethod = paymentMethod.wireValue,
    destination = destination.wireValue,
    spaceId = spaceId,
    kitchenNotes = kitchenNotes,
    items = items.map { it.toDto() },
)

private fun CreateOrderItem.toDto(): CreateOrderItemDto = CreateOrderItemDto(
    productId = productId,
    quantity = quantity,
    optionIds = optionIds,
)

fun OrderDetailDto.toDomain(): OrderDetail = OrderDetail(
    summary = OrderSummary(
        id = id,
        folio = folio,
        operationalDate = operationalDate,
        state = OrderState.fromWireValue(state),
        paymentMethod = PaymentMethod.fromWireValue(paymentMethod),
        destination = OrderDestination.fromWireValue(destination),
        space = space?.let { OrderSpace(id = it.id, name = it.name, type = it.type) },
        subtotal = subtotal,
        combinedSavings = combinedSavings,
        cashbackAwarded = cashbackAwarded,
        total = total,
        version = version,
        createdAt = createdAt,
        updatedAt = updatedAt,
    ),
    user = user?.let { OrderUser(name = it.name, enrollment = it.enrollment) },
    kitchenNotes = kitchenNotes,
    items = items.map { item ->
        OrderItem(
            id = item.id,
            productId = item.productId,
            productName = item.productName,
            preparationStation = PreparationStation.fromWireValue(item.preparationStation),
            quantity = item.quantity,
            unitDigitalPrice = item.unitDigitalPrice,
            subtotal = item.subtotal,
            options = item.options.map { option ->
                OrderItemOption(
                    optionId = option.optionId,
                    name = option.name,
                    extraPrice = option.extraPrice,
                )
            },
        )
    },
)
