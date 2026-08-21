package com.vaiinilla.app

import com.vaiinilla.app.domain.model.OrderPayment
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.StripePaymentSession
import com.vaiinilla.app.domain.model.StripePaymentStatus
import com.vaiinilla.app.domain.model.isStripePaymentConfirmedByBackend
import com.vaiinilla.app.ui.order.StripePaymentPhase
import com.vaiinilla.app.ui.order.stripePhaseFromBackend
import com.vaiinilla.app.ui.order.toRuntimeConfiguration
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StripeCheckoutPolicyTest {
    @Test
    fun `PaymentSheet runtime configuration uses connected account from current response`() {
        val session =
            StripePaymentSession(
                paymentAttemptId = "attempt-1",
                paymentIntentId = "pi_test_001",
                clientSecret = "pi_test_001_secret_test",
                stripeAccountId = "acct_test_current_venue",
                publishableKey = "pk_test_current_venue",
                status = StripePaymentStatus.PENDING,
            )

        val config = session.toRuntimeConfiguration()

        assertEquals("pk_test_current_venue", config.publishableKey)
        assertEquals("acct_test_current_venue", config.stripeAccountId)
        assertEquals("pi_test_001_secret_test", config.clientSecret)
    }

    @Test
    fun `Completed remains pending while webhook has not advanced backend`() {
        val order = stripeOrder(OrderState.PENDING_PAYMENT, StripePaymentStatus.PENDING)

        assertEquals(StripePaymentPhase.PENDING, stripePhaseFromBackend(order))
        assertFalse(order.isStripePaymentConfirmedByBackend())
    }

    @Test
    fun `processing or requires action remain backend pending and never false-confirm`() {
        val order = stripeOrder(OrderState.PENDING_PAYMENT, StripePaymentStatus.PENDING)

        assertEquals(StripePaymentPhase.PENDING, stripePhaseFromBackend(order))
        assertFalse(order.isStripePaymentConfirmedByBackend())
    }

    @Test
    fun `payment confirmado without cobrado state is still pending`() {
        val order = stripeOrder(OrderState.PENDING_PAYMENT, StripePaymentStatus.CONFIRMED)

        assertEquals(StripePaymentPhase.PENDING, stripePhaseFromBackend(order))
        assertFalse(order.isStripePaymentConfirmedByBackend())
    }

    @Test
    fun `backend confirmation requires confirmed payment and advanced order state`() {
        listOf(
            OrderState.PAID,
            OrderState.PREPARING,
            OrderState.READY,
            OrderState.DELIVERED,
        ).forEach { state ->
            val order = stripeOrder(state, StripePaymentStatus.CONFIRMED)
            assertTrue(order.isStripePaymentConfirmedByBackend())
            assertEquals(StripePaymentPhase.CONFIRMED, stripePhaseFromBackend(order))
        }
    }

    @Test
    fun `local failure or cancellation cannot create backend failure state`() {
        val stillPending = stripeOrder(OrderState.PENDING_PAYMENT, StripePaymentStatus.PENDING)
        val failed = stripeOrder(OrderState.PENDING_PAYMENT, StripePaymentStatus.FAILED)
        val canceled = stripeOrder(OrderState.PENDING_PAYMENT, StripePaymentStatus.CANCELED)

        assertEquals(StripePaymentPhase.PENDING, stripePhaseFromBackend(stillPending))
        assertEquals(StripePaymentPhase.FAILED, stripePhaseFromBackend(failed))
        assertEquals(StripePaymentPhase.CANCELED, stripePhaseFromBackend(canceled))
    }

    private fun stripeOrder(
        state: OrderState,
        paymentStatus: StripePaymentStatus,
    ) = ScreenshotFixtures
        .sampleOrder(
            state = state,
            paymentMethod = PaymentMethod.STRIPE,
        ).copy(
            payment =
                OrderPayment(
                    paymentAttemptId = "attempt-1",
                    paymentIntentId = "pi_test_001",
                    stripeAccountId = "acct_test_001",
                    status = paymentStatus,
                ),
        )
}
