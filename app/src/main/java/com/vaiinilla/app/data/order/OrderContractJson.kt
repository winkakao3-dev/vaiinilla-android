package com.vaiinilla.app.data.order

import com.vaiinilla.app.domain.model.CashCollectionResult
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.CreatedOrder
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.RefundOutcome
import com.vaiinilla.app.domain.model.StripePaymentSession
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
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
            val data = envelope.data
            return data.order.toDomain().copy(
                refund =
                    refundOutcome(
                        cash = data.cashReturned,
                        pending = data.pendingRefund,
                        balance = data.balanceRefund,
                        card = data.cardRefund != null && data.cardRefund !is JsonNull,
                    ),
            )
        }

        fun parseRejectedItem(raw: String): OrderDetail {
            val envelope = json.decodeFromString<RejectItemEnvelopeDto>(raw)
            requireEnvelopeSuccess(envelope.error)
            val data = envelope.data
            return data.order.toDomain().copy(
                refund = refundOutcome(cash = null, pending = data.pendingRefund, balance = data.balanceRefund),
            )
        }

        private fun refundOutcome(
            cash: RefundAmountDto?,
            pending: RefundAmountDto?,
            balance: RefundAmountDto?,
            card: Boolean = false,
        ): RefundOutcome? {
            fun RefundAmountDto?.positive() =
                (this?.amount?.takeUnless { it is JsonNull }?.content)?.toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
            val outcome =
                RefundOutcome(
                    cashHandedBack = cash.positive(),
                    pendingForCashier = pending.positive(),
                    returnedToBalance = balance.positive(),
                    cardRefund = card,
                )
            return outcome.takeIf {
                it.cashHandedBack != null ||
                    it.pendingForCashier != null ||
                    it.returnedToBalance != null ||
                    it.cardRefund
            }
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

        fun parseOrderList(raw: String): List<OrderDetail> {
            val envelope = json.decodeFromString<OrderListEnvelopeDto>(raw)
            requireEnvelopeSuccess(envelope.error)
            return envelope.data.toDomainTolerant()
        }

        /** Una página del listado de pedidos con el cursor de la siguiente (null en la última). */
        fun parseOrderListPage(raw: String): OrderListPage {
            val envelope = json.decodeFromString<OrderListEnvelopeDto>(raw)
            requireEnvelopeSuccess(envelope.error)
            return OrderListPage(
                orders = envelope.data.toDomainTolerant(),
                nextCursor = envelope.meta.cursor?.takeIf { it.isNotBlank() },
            )
        }

        /**
         * Un pedido con un valor que esta versión no conoce (un método de pago nuevo) no tira la lista entera: se
         * omite ese y se muestran los demás. Si ninguno se entiende, el contrato cambió y el error sale (STAFF-21).
         */
        private fun List<OrderDetailDto>.toDomainTolerant(): List<OrderDetail> {
            val parsed = map { runCatching { it.toDomain() } }
            val understood = parsed.mapNotNull { it.getOrNull() }
            if (understood.isEmpty() && parsed.isNotEmpty()) parsed.first().getOrThrow()
            return understood
        }

        fun parsePickupToken(raw: String): String {
            val envelope = json.decodeFromString<PickupTokenEnvelopeDto>(raw)
            requireEnvelopeSuccess(envelope.error)
            val token = envelope.data.pickupToken.trim()
            require(token.isNotEmpty()) { "La API devolvió un qr_token vacío." }
            return token
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

class OrderListPage(
    val orders: List<OrderDetail>,
    val nextCursor: String?,
)
