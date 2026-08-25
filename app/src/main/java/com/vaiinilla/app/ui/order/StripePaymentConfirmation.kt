package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.StripePaymentStatus
import kotlinx.coroutines.delay

internal const val STRIPE_CONFIRMATION_POLL_INTERVAL_MS = 3_000L
internal const val STRIPE_CONFIRMATION_TIMEOUT_MS = 90_000L
internal val STRIPE_CONFIRMATION_MAX_POLLS: Int =
    ((STRIPE_CONFIRMATION_TIMEOUT_MS / STRIPE_CONFIRMATION_POLL_INTERVAL_MS) + 1).toInt()

internal data class StripePaymentPollResult(
    val order: OrderDetail?,
    val phase: StripePaymentPhase,
    val lastError: Throwable?,
    val timedOut: Boolean,
)

/**
 * Polls the backend payment state. A local PaymentSheet result never changes the outcome by itself;
 * only the order returned by the backend can finish this loop.
 */
internal suspend fun pollStripePaymentConfirmation(
    orderId: String,
    fetch: suspend (String) -> Result<OrderDetail>,
    maxPolls: Int = STRIPE_CONFIRMATION_MAX_POLLS,
    intervalMillis: Long = STRIPE_CONFIRMATION_POLL_INTERVAL_MS,
    wait: suspend (Long) -> Unit = { delay(it) },
): StripePaymentPollResult {
    require(orderId.isNotBlank()) { "orderId must not be blank" }
    require(maxPolls > 0) { "maxPolls must be positive" }
    require(intervalMillis >= 0) { "intervalMillis must not be negative" }

    var latest: OrderDetail? = null
    var lastError: Throwable? = null

    repeat(maxPolls) { index ->
        fetch(orderId).fold(
            onSuccess = {
                latest = it
                lastError = null
            },
            onFailure = { lastError = it },
        )

        val phase = stripePhaseFromBackend(latest)
        if (phase.isTerminalStripeConfirmationPhase()) {
            return StripePaymentPollResult(
                order = latest,
                phase = phase,
                lastError = lastError,
                timedOut = false,
            )
        }

        if (index < maxPolls - 1) {
            wait(intervalMillis)
        }
    }

    return StripePaymentPollResult(
        order = latest,
        phase = StripePaymentPhase.TIMED_OUT,
        lastError = lastError,
        timedOut = true,
    )
}

internal fun StripePaymentPhase.isTerminalStripeConfirmationPhase(): Boolean =
    this == StripePaymentPhase.CONFIRMED ||
        this == StripePaymentPhase.FAILED ||
        this == StripePaymentPhase.CANCELED ||
        this == StripePaymentPhase.REFUNDING ||
        this == StripePaymentPhase.REFUNDED

internal fun StripePaymentStatus?.confirmationMessage(): String =
    when (this) {
        StripePaymentStatus.PROCESSING -> "Stripe está procesando tu pago."
        StripePaymentStatus.REQUIRES_ACTION -> "Completa la acción solicitada para continuar."
        StripePaymentStatus.PENDING -> "Estamos verificando el pago con Vaiinilla."
        else -> "Estamos verificando el pago con Vaiinilla."
    }
