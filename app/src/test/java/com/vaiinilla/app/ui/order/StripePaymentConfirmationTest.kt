package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.OrderPayment
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.StripePaymentStatus
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StripePaymentConfirmationTest {
    @Test
    fun `PaymentSheet completion remains pending until backend confirms`() {
        runBlocking {
            val pending = stripeOrder(StripePaymentStatus.PENDING)
            val confirmed = stripeOrder(StripePaymentStatus.CONFIRMED, OrderState.PAID)
            val waits = mutableListOf<Long>()
            var calls = 0

            val result =
                pollStripePaymentConfirmation(
                    orderId = pending.summary.id,
                    fetch = {
                        calls += 1
                        Result.success(if (calls == 1) pending else confirmed)
                    },
                    maxPolls = 3,
                    wait = { waits += it },
                )

            assertEquals(StripePaymentPhase.CONFIRMED, result.phase)
            assertEquals(confirmed, result.order)
            assertEquals(listOf(STRIPE_CONFIRMATION_POLL_INTERVAL_MS), waits)
            assertFalse(result.timedOut)
        }
    }

    @Test
    fun `processing and requires action never become local success or retry`() {
        runBlocking {
            listOf(StripePaymentStatus.PROCESSING, StripePaymentStatus.REQUIRES_ACTION).forEach { status ->
                val order = stripeOrder(status)
                val result =
                    pollStripePaymentConfirmation(
                        orderId = order.summary.id,
                        fetch = { Result.success(order) },
                        maxPolls = 2,
                        wait = {},
                    )

                assertEquals(StripePaymentPhase.TIMED_OUT, result.phase)
                assertTrue(result.timedOut)
                assertFalse(status.canRetry)
            }
        }
    }

    @Test
    fun `backend failure and cancellation stop polling and allow retry`() {
        runBlocking {
            listOf(StripePaymentStatus.FAILED, StripePaymentStatus.CANCELED).forEach { status ->
                val order = stripeOrder(status)
                var calls = 0
                val result =
                    pollStripePaymentConfirmation(
                        orderId = order.summary.id,
                        fetch = {
                            calls += 1
                            Result.success(order)
                        },
                        maxPolls = 31,
                        wait = { error("terminal status must not wait") },
                    )

                assertEquals(
                    if (status == StripePaymentStatus.FAILED) {
                        StripePaymentPhase.FAILED
                    } else {
                        StripePaymentPhase.CANCELED
                    },
                    result.phase,
                )
                assertEquals(1, calls)
                assertTrue(status.canRetry)
            }
        }
    }

    @Test
    fun `timeout does not convert pending payment into failure`() {
        runBlocking {
            val order = stripeOrder(StripePaymentStatus.PENDING)
            val waits = mutableListOf<Long>()

            val result =
                pollStripePaymentConfirmation(
                    orderId = order.summary.id,
                    fetch = { Result.success(order) },
                    maxPolls = 3,
                    intervalMillis = STRIPE_CONFIRMATION_POLL_INTERVAL_MS,
                    wait = { waits += it },
                )

            assertEquals(StripePaymentPhase.TIMED_OUT, result.phase)
            assertEquals(3, waits.size + 1)
            assertEquals(listOf(3_000L, 3_000L), waits)
            assertEquals(StripePaymentStatus.PENDING, result.order?.payment?.status)
        }
    }

    @Test
    fun `production polling cadence is three seconds for at most ninety seconds`() {
        assertEquals(3_000L, STRIPE_CONFIRMATION_POLL_INTERVAL_MS)
        assertEquals(90_000L, STRIPE_CONFIRMATION_TIMEOUT_MS)
        assertEquals(31, STRIPE_CONFIRMATION_MAX_POLLS)
    }

    private fun stripeOrder(
        status: StripePaymentStatus,
        state: OrderState = OrderState.PENDING_PAYMENT,
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
                    status = status,
                ),
        )
}
