package com.vaiinilla.app.data.order

import com.vaiinilla.app.domain.model.CashCollectionResult
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.CreatedOrder
import com.vaiinilla.app.domain.model.ItemRefundMethod
import com.vaiinilla.app.domain.model.ItemRejectionOutcome
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.StripePaymentSession
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderContractJson
    @Inject
    constructor() {
        private val json =
            Json {
                ignoreUnknownKeys = true
                explicitNulls = true
                isLenient = false
                encodeDefaults = true
            }

        fun encodeCreateRequest(request: CreateOrderRequest): String = json.encodeToString(request.toDto())

        fun encodeCancelOrder(
            expectedVersion: Int,
            reason: String,
        ): String =
            json.encodeToString(CancelOrderRequestDto(expectedVersion = expectedVersion, reason = reason.trim()))

        fun parseCancelledOrder(raw: String): OrderDetail {
            val envelope = json.decodeFromString<CancelOrderEnvelopeDto>(raw)
            requireEnvelopeSuccess(envelope.error)
            return envelope.data.order.toDomain()
        }

        fun encodeItemRejection(
            expectedVersion: Int,
            reason: String,
        ): String =
            json.encodeToString(ItemRejectionRequestDto(expectedVersion = expectedVersion, reason = reason.trim()))

        fun parseItemRejection(raw: String): ItemRejectionOutcome {
            val envelope = json.decodeFromString<ItemRejectionEnvelopeDto>(raw)
            requireEnvelopeSuccess(envelope.error)
            val rejection = envelope.data.rejection
            return ItemRejectionOutcome(
                order = envelope.data.order.toDomain(),
                itemId = rejection.itemId,
                amount = rejection.amount,
                refundMethod =
                    requireNotNull(ItemRefundMethod.fromWire(rejection.refundMethod)) {
                        "Método de reembolso desconocido: ${rejection.refundMethod}"
                    },
                manualRefundOriginalMethod = envelope.data.refund?.originalMethod,
            )
        }

        fun encodeCashCollection(
            amountReceived: String,
            expectedVersion: Int,
            tip: String? = null,
        ): String =
            json.encodeToString(
                CashCollectionRequestDto(
                    amountReceived = amountReceived,
                    expectedVersion = expectedVersion,
                    tip = tip,
                ),
            )

        fun encodeTransition(
            targetState: OrderState,
            expectedVersion: Int,
            pickupToken: String? = null,
        ): String =
            json.encodeToString(
                TransitionRequestDto(
                    targetState = targetState.wireValue,
                    expectedVersion = expectedVersion,
                    pickupToken = pickupToken,
                ),
            )

        fun encodeOpenCashSession(initialAmount: String): String =
            json.encodeToString(
                OpenCashSessionRequestDto(initialAmount = initialAmount),
            )

        fun encodeCloseCashSession(finalAmount: String): String =
            json.encodeToString(CloseCashSessionRequestDto(finalAmount = finalAmount))

        fun parseCreatedOrder(raw: String): CreatedOrder {
            val envelope = json.decodeFromString<OrderDetailEnvelopeDto>(raw)
            requireEnvelopeSuccess(envelope.error)
            val order = envelope.data.toDomain()
            val session =
                if (order.summary.paymentMethod == PaymentMethod.STRIPE) {
                    requireNotNull(envelope.data.payment) { "Stripe order missing pago" }
                        .toStripeSession(expectedTotalCents = orderTotalToCents(envelope.data.total))
                } else {
                    null
                }
            return CreatedOrder(order = order, stripeSession = session)
        }

        fun parseOrderDetail(raw: String): OrderDetail {
            val envelope = json.decodeFromString<OrderDetailEnvelopeDto>(raw)
            requireEnvelopeSuccess(envelope.error)
            return envelope.data.toDomain()
        }

        /** El token de recogida, o null si la respuesta no trae uno utilizable. */
        fun parsePickupQr(raw: String): String? {
            val envelope = json.decodeFromString<PickupQrEnvelopeDto>(raw)
            requireEnvelopeSuccess(envelope.error)
            val raw = envelope.data?.qrToken ?: return null
            return raw.trim().takeIf { it.isNotEmpty() }
        }

        fun parseOrderList(raw: String): List<OrderDetail> {
            val envelope = json.decodeFromString<OrderListEnvelopeDto>(raw)
            requireEnvelopeSuccess(envelope.error)
            return envelope.data.map { it.toDomain() }
        }

        fun parseStripeRetry(raw: String): StripePaymentSession {
            val envelope = json.decodeFromString<StripeRetryEnvelopeDto>(raw)
            requireEnvelopeSuccess(envelope.error)
            return envelope.data.payment.toStripeSession()
        }

        fun parseCashCollection(raw: String): CashCollectionResult {
            val envelope = json.decodeFromString<CashCollectionEnvelopeDto>(raw)
            requireEnvelopeSuccess(envelope.error)
            return CashCollectionResult(
                order = envelope.data.order.toDomain(),
                amountReceived = envelope.data.amountReceived,
                change = envelope.data.change,
            )
        }

        fun parseCashSession(raw: String): CashSessionDto? {
            val envelope = json.decodeFromString<CashSessionEnvelopeDto>(raw)
            requireEnvelopeSuccess(envelope.error)
            return envelope.data
        }

        private fun requireEnvelopeSuccess(error: JsonElement?) {
            require(error == null) { "La API devolvió un error en el envelope." }
        }
    }
