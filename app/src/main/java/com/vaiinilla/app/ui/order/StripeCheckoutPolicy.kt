package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.StripePaymentSession
import com.vaiinilla.app.domain.model.StripePaymentStatus
import com.vaiinilla.app.domain.model.awaitsAcceptanceBeforePaying
import com.vaiinilla.app.domain.model.awaitsPaymentAfterAcceptance
import com.vaiinilla.app.domain.model.isStripePaymentConfirmedByBackend
import com.vaiinilla.app.domain.repository.OrderRepositoryException

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
        order?.awaitsAcceptanceBeforePaying() == true -> StripePaymentPhase.AWAITING_ACCEPTANCE
        order?.awaitsPaymentAfterAcceptance() == true -> StripePaymentPhase.AWAITING_PAYMENT
        order?.isStripePaymentConfirmedByBackend() == true -> StripePaymentPhase.CONFIRMED
        order?.payment?.status == StripePaymentStatus.FAILED -> StripePaymentPhase.FAILED
        order?.payment?.status == StripePaymentStatus.CANCELED -> StripePaymentPhase.CANCELED
        order?.payment?.status == StripePaymentStatus.REFUND_PENDING ||
            order?.payment?.status == StripePaymentStatus.REFUNDING -> StripePaymentPhase.REFUNDING
        order?.payment?.status == StripePaymentStatus.REFUNDED -> StripePaymentPhase.REFUNDED
        // Cancelado, expirado o no recogido sin reembolso en curso: ya no hay cobro que esperar. Antes caía en
        // PENDING, el sondeo duraba 90 s, terminaba en TIMED_OUT y el pedido muerto seguía bloqueando el carrito.
        order?.summary?.state?.isTerminalWithoutDelivery == true -> StripePaymentPhase.CANCELED
        else -> StripePaymentPhase.PENDING
    }

/**
 * El servidor dice que el pedido no existe (o no es de esta cuenta): no hay cobro que cuidar y el carrito no
 * debe quedarse bloqueado por él. Sin red o con otro error el pago sigue pendiente, porque puede haber un cobro
 * en camino.
 */
internal fun Throwable.isMissingOrder(): Boolean =
    (this as? OrderRepositoryException)?.code in setOf("ORDER_NOT_FOUND", "NOT_FOUND")

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
    orderClosed: Boolean = false,
): Boolean =
    // Un pedido cancelado, expirado o entregado ya no se paga: reintentar solo daría un 409 (PAGO-05).
    !orderClosed &&
        canRetryOpenOrder(paymentStatus, phase)

private fun canRetryOpenOrder(
    paymentStatus: StripePaymentStatus?,
    phase: StripePaymentPhase,
): Boolean =
    paymentStatus in setOf(StripePaymentStatus.FAILED, StripePaymentStatus.CANCELED) ||
        phase == StripePaymentPhase.AWAITING_PAYMENT ||
        // La pantalla pudo ver la aceptación antes que esta fase; si aún no, el servidor responde 409.
        phase == StripePaymentPhase.AWAITING_ACCEPTANCE ||
        phase == StripePaymentPhase.CANCELED ||
        phase == StripePaymentPhase.TIMED_OUT
