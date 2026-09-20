package com.vaiinilla.app.data.order

import com.vaiinilla.app.BuildConfig
import com.vaiinilla.app.domain.model.CreateOrderItem
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderItem
import com.vaiinilla.app.domain.model.OrderItemOption
import com.vaiinilla.app.domain.model.OrderPayment
import com.vaiinilla.app.domain.model.OrderSpace
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.OrderUser
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.StripePaymentSession
import com.vaiinilla.app.domain.model.StripePaymentStatus

fun CreateOrderRequest.toDto(): CreateOrderRequestDto =
    CreateOrderRequestDto(
        paymentMethod = paymentMethod.wireValue,
        destination = destination.wireValue,
        spaceId = spaceId,
        kitchenNotes = kitchenNotes.trim().takeIf { it.isNotEmpty() },
        items = items.map { it.toDto() },
    )

private fun CreateOrderItem.toDto(): CreateOrderItemDto =
    CreateOrderItemDto(
        productId = productId,
        quantity = quantity,
        optionIds = optionIds,
    )

fun OrderDetailDto.toDomain(): OrderDetail =
    OrderDetail(
        summary =
            OrderSummary(
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
        user = user?.let { OrderUser(name = it.name, enrollment = it.enrollment.orEmpty()) },
        kitchenNotes = kitchenNotes.orEmpty(),
        items =
            items.map { item ->
                OrderItem(
                    id = item.id,
                    productId = item.productId,
                    productName = item.productName,
                    preparationStation = PreparationStation.fromWireValue(item.preparationStation),
                    quantity = item.quantity,
                    unitDigitalPrice = item.unitDigitalPrice,
                    subtotal = item.subtotal,
                    options =
                        item.options.map { option ->
                            OrderItemOption(
                                optionId = option.optionId,
                                name = option.name,
                                extraPrice = option.extraPrice,
                            )
                        },
                    unitCollectionPrice = item.unitCollectionPrice,
                )
            },
        pickupToken = pickupToken,
        payment = payment?.toDomain(),
    )

fun OrderPaymentDto.toDomain(): OrderPayment =
    OrderPayment(
        paymentAttemptId = paymentAttemptId,
        paymentIntentId = paymentIntentId,
        stripeAccountId = stripeAccountId,
        status = StripePaymentStatus.fromWireValue(paymentStatus),
    )

fun OrderPaymentDto.toStripeSession(expectedTotalCents: Long? = null): StripePaymentSession {
    val secret = requireNotNull(clientSecret) { "Stripe response missing client_secret" }
    val key = requireNotNull(publishableKey) { "Stripe response missing publishable_key" }
    val expectedPrefix = if (BuildConfig.IS_PRODUCTION) "pk_live_" else "pk_test_"
    val expectedMode = if (BuildConfig.IS_PRODUCTION) "Live" else "Test"
    require(key.startsWith(expectedPrefix)) { "Stripe publishable_key must be $expectedMode Mode for this build." }
    currency?.let {
        require(it.equals("mxn", ignoreCase = true)) { "Stripe currency must be mxn, got $it." }
    }
    if (amountCents != null && expectedTotalCents != null) {
        require(amountCents == expectedTotalCents) {
            "Stripe amount_cents ($amountCents) does not match the order total ($expectedTotalCents)."
        }
    }
    return StripePaymentSession(
        paymentAttemptId = paymentAttemptId,
        paymentIntentId = paymentIntentId,
        clientSecret = secret,
        stripeAccountId = stripeAccountId,
        publishableKey = key,
        status = StripePaymentStatus.fromWireValue(paymentStatus),
        amountCents = amountCents,
        currency = currency,
        pricingPolicyVersion = pricingPolicyVersion,
        applicationFeeCents = applicationFeeCents,
    )
}

internal fun orderTotalToCents(total: String): Long? =
    total
        .trim()
        .toBigDecimalOrNull()
        ?.movePointRight(2)
        ?.setScale(0)
        ?.toLong()
