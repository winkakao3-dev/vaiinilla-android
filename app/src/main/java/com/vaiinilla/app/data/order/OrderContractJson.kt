package com.vaiinilla.app.data.order

import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.CreatedOrder
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

        fun encodeCashCollection(
            amountReceived: String,
            expectedVersion: Int,
        ): String =
            json.encodeToString(
                CashCollectionRequestDto(
                    amountReceived = amountReceived,
                    expectedVersion = expectedVersion,
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

        fun parseCreatedOrder(raw: String): CreatedOrder {
            val envelope = json.decodeFromString<OrderDetailEnvelopeDto>(raw)
            requireEnvelopeSuccess(envelope.error)
            val order = envelope.data.toDomain()
            val session =
                if (order.summary.paymentMethod == PaymentMethod.STRIPE) {
                    requireNotNull(envelope.data.payment) { "Stripe order missing pago" }.toStripeSession()
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

        fun parseCashCollection(raw: String): OrderDetail {
            val envelope = json.decodeFromString<CashCollectionEnvelopeDto>(raw)
            requireEnvelopeSuccess(envelope.error)
            return envelope.data.order.toDomain()
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
