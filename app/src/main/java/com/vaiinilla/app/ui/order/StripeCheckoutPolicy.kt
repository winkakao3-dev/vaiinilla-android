package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.OrderDetail
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
