package com.vaiinilla.app.data.order

import com.vaiinilla.app.BuildConfig
import com.vaiinilla.app.domain.model.CreateOrderItem
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.DeliveryAddress
import com.vaiinilla.app.domain.model.HomeDeliveryQuote
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderHomeDelivery
import com.vaiinilla.app.domain.model.OrderItem
import com.vaiinilla.app.domain.model.OrderItemOption
import com.vaiinilla.app.domain.model.OrderItemRejection
import com.vaiinilla.app.domain.model.OrderPayment
import com.vaiinilla.app.domain.model.OrderReservation
import com.vaiinilla.app.domain.model.OrderSpace
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.OrderUser
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.PaymentPart
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.ReservationState
import com.vaiinilla.app.domain.model.StripePaymentSession
import com.vaiinilla.app.domain.model.StripePaymentStatus
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant

fun CreateOrderRequest.toDto(): CreateOrderRequestDto =
    CreateOrderRequestDto(
        paymentMethod = paymentMethod.wireValue,
        destination = destination.wireValue,
        spaceId = spaceId,
        kitchenNotes = kitchenNotes.trim().takeIf { it.isNotEmpty() },
        items = items.map { it.toDto() },
        payAtEnd = payAtEnd.takeIf { it },
        customerName = customerName?.trim()?.takeIf { it.isNotEmpty() },
        delivery = delivery?.toDto(),
        deliveryDate = deliveryDate?.takeIf { it.isNotBlank() },
        deliverySlot = deliverySlot?.takeIf { it.isNotBlank() },
        paysOnArrival = paysOnArrival.takeIf { it },
        withDeposit = withDeposit.takeIf { it },
    )

fun DeliveryAddress.toDto(): DeliveryDto =
    DeliveryDto(
        address = address.trim(),
        reference = reference.trim().takeIf { it.isNotEmpty() },
        phone = phone.filter(Char::isDigit),
        latitude = latitude?.let(::JsonPrimitive),
        longitude = longitude?.let(::JsonPrimitive),
    )

/** Un monto o un número que el servidor manda como texto o como número; `null` si viene vacío. */
internal fun JsonPrimitive.flexibleText(): String? =
    if (this is JsonNull) {
        null
    } else {
        content.trim().takeIf {
            it.isNotEmpty()
        }
    }

fun HomeDeliveryQuoteDto.toDomain(): HomeDeliveryQuote =
    HomeDeliveryQuote(
        reaches = llega,
        distanceKm = distanceKm?.flexibleText(),
        fee = costo?.flexibleText(),
        minimum = minimo?.flexibleText(),
    )

private fun CreateOrderItem.toDto(): CreateOrderItemDto =
    CreateOrderItemDto(
        productId = productId,
        quantity = quantity,
        optionIds = optionIds,
        notes = notes.takeIf { it.isNotEmpty() },
    )

fun OrderDetailDto.toDomain(): OrderDetail =
    OrderDetail(
        summary =
            OrderSummary(
                id = id,
                folio = folio,
                operationalDate = operationalDate,
                deliveryDate = deliveryDate?.takeIf { it.isNotBlank() },
                deliverySlot = deliverySlot?.takeIf { it.isNotBlank() },
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
                payAtEnd = payAtEnd,
                paymentPending = paymentPending,
                cancelReason = cancelReason,
                canceledByRole = canceledByRole,
                tableSessionId = tableSessionId,
                deliveryWithoutQr = deliveryWithoutQr,
                amountPaid = amountPaid,
                balanceDue = balanceDue,
                chargedMethod = chargedMethod?.takeIf { it.isNotBlank() },
                paymentsByMethod = paymentsByMethod.map { PaymentPart(method = it.method, amount = it.amount) },
            ),
        user =
            user?.let {
                OrderUser(name = it.name, enrollment = it.enrollment.orEmpty(), allergies = it.allergies.orEmpty())
            },
        kitchenNotes = kitchenNotes.orEmpty(),
        items =
            items.map { item ->
                OrderItem(
                    id = item.id,
                    productId = item.productId,
                    productName = item.productName,
                    imageUrl = item.imageUrl?.takeIf { it.isNotBlank() },
                    preparationStation = PreparationStation.fromWireValue(item.preparationStation),
                    quantity = item.quantity,
                    unitDigitalPrice = item.unitDigitalPrice,
                    subtotal = item.subtotal,
                    rejection = item.rejection?.let { OrderItemRejection(reason = it.reason, amount = it.amount) },
                    notes = item.notes.orEmpty(),
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
        reservation = reservation?.toDomain(),
        arrivedAt = arrivedAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
        customerName = customerName?.trim()?.takeIf { it.isNotEmpty() },
        homeDelivery =
            delivery?.let { entrega ->
                OrderHomeDelivery(
                    address =
                        DeliveryAddress(
                            address = entrega.address,
                            reference = entrega.reference.orEmpty(),
                            phone = entrega.phone.orEmpty(),
                            latitude = entrega.latitude?.flexibleText()?.toDoubleOrNull(),
                            longitude = entrega.longitude?.flexibleText()?.toDoubleOrNull(),
                        ),
                    fee = deliveryFee?.flexibleText(),
                    deposit = deposit?.flexibleText(),
                    dueOnArrival = dueOnArrival?.flexibleText(),
                    code = deliveryCode?.flexibleText(),
                )
            },
        answerBy = answerBy?.let(::serverInstant),
        payBy = payBy?.let(::serverInstant),
    )

// El servidor lee estas fechas de to_jsonb: llegan con desfase ("…+00:00"), no con Z.
private fun serverInstant(raw: String): Instant? =
    runCatching {
        java.time.OffsetDateTime
            .parse(raw)
            .toInstant()
    }.getOrNull()
        ?: runCatching { Instant.parse(raw) }.getOrNull()

private fun OrderReservationDto.toDomain(): OrderReservation? =
    runCatching {
        OrderReservation(
            id = id,
            space = space?.let { OrderSpace(id = it.id, name = it.name, type = it.type) },
            start = Instant.parse(start),
            end = Instant.parse(end),
            durationMinutes = durationMinutes,
            state = ReservationState.fromWire(state),
        )
    }.getOrNull()

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
