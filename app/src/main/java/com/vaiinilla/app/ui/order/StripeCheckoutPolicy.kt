package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.StripePaymentSession
import com.vaiinilla.app.domain.model.StripePaymentStatus
import com.vaiinilla.app.domain.model.isStripePaymentConfirmedByBackend

data class StripeRuntimeConfiguration(
    val publishableKey: String,
    val stripeAccountId: String,
    val clientSecret: String,
)

fun StripePaymentSession.toRuntimeConfiguration(): StripeRuntimeConfiguration =
    StripeRuntimeConfiguration(
        publishableKey = publishableKey,
        stripeAccountId = stripeAccountId,
        clientSecret = clientSecret,
    )

fun stripePhaseFromBackend(order: OrderDetail?): StripePaymentPhase =
    when {
        order?.isStripePaymentConfirmedByBackend() == true -> StripePaymentPhase.CONFIRMED
        order?.payment?.status == StripePaymentStatus.FAILED -> StripePaymentPhase.FAILED
        order?.payment?.status == StripePaymentStatus.CANCELED -> StripePaymentPhase.CANCELED
        order?.payment?.status == StripePaymentStatus.REFUND_PENDING ||
            order?.payment?.status == StripePaymentStatus.REFUNDING -> StripePaymentPhase.REFUNDING
        order?.payment?.status == StripePaymentStatus.REFUNDED -> StripePaymentPhase.REFUNDED
        else -> StripePaymentPhase.PENDING
    }

/**
 * Volver al carrito desde el pago: vale cuando el servidor ya dijo fallido/cancelado o cuando la persona cerró
 * la hoja de Stripe (fase local CANCELED), aunque el servidor siga viendo el intento como pendiente (PAGO-01).
 */
internal fun canReturnStripeOrderToCart(
    order: OrderDetail,
    phase: StripePaymentPhase,
): Boolean =
    order.summary.paymentMethod == PaymentMethod.STRIPE &&
        (
            order.payment?.status in setOf(StripePaymentStatus.FAILED, StripePaymentStatus.CANCELED) ||
                phase == StripePaymentPhase.CANCELED
        )

/**
 * Reintentar abre otra vez la hoja con el mismo intento de pago. Además de fallido/cancelado, se permite cuando la
 * confirmación venció (TIMED_OUT): era la única salida que dejaba al pedido sin forma de pagarse (PAGO-08).
 */
internal fun canRetryStripePayment(
    paymentStatus: StripePaymentStatus?,
    phase: StripePaymentPhase,
): Boolean =
    paymentStatus in setOf(StripePaymentStatus.FAILED, StripePaymentStatus.CANCELED) ||
        phase == StripePaymentPhase.CANCELED ||
        phase == StripePaymentPhase.TIMED_OUT
