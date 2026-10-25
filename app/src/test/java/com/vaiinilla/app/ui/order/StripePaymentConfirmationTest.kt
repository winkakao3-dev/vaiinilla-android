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
        assertEquals(15_000L, STRIPE_ABANDON_VISIBLE_AFTER_MS)
    }

    @Test
    fun `cancel appears only after Stripe stays stuck`() {
        assertFalse(canShowStripeAbandon(elapsedMs = 0L, isStuck = true))
        assertFalse(canShowStripeAbandon(elapsedMs = 14_999L, isStuck = true))
        assertTrue(canShowStripeAbandon(elapsedMs = 15_000L, isStuck = true))
        assertFalse(canShowStripeAbandon(elapsedMs = 15_000L, isStuck = false))
        assertTrue(canShowStripeAbandon(elapsedMs = 0L, isStuck = true, visibleAfterMs = 0L))
    }

    @Test
    fun `a failed read right after the sheet completed does not close the poll`() {
        runBlocking {
            val failed = stripeOrder(StripePaymentStatus.FAILED)
            val confirmed = stripeOrder(StripePaymentStatus.CONFIRMED, OrderState.PAID)
            var calls = 0

            val result =
                pollStripePaymentConfirmation(
                    orderId = failed.summary.id,
                    fetch = {
                        calls += 1
                        Result.success(if (calls <= 2) failed else confirmed)
                    },
                    maxPolls = 10,
                    failedGracePolls = STRIPE_FAILED_GRACE_POLLS_AFTER_SHEET,
                    wait = {},
                )

            assertEquals(StripePaymentPhase.CONFIRMED, result.phase)
            assertEquals(3, calls)
        }
    }

    @Test
    fun `a failure that outlasts the grace window is still reported as failed`() {
        runBlocking {
            val failed = stripeOrder(StripePaymentStatus.FAILED)
            var calls = 0

            val result =
                pollStripePaymentConfirmation(
                    orderId = failed.summary.id,
                    fetch = {
                        calls += 1
                        Result.success(failed)
                    },
                    maxPolls = 10,
                    failedGracePolls = STRIPE_FAILED_GRACE_POLLS_AFTER_SHEET,
                    wait = {},
                )

            assertEquals(StripePaymentPhase.FAILED, result.phase)
            assertEquals(STRIPE_FAILED_GRACE_POLLS_AFTER_SHEET + 1, calls)
        }
    }

    @Test
    fun `returning to the cart works after the sheet was closed locally while the server still says pending`() {
        val pending = stripeOrder(StripePaymentStatus.PENDING)

        assertTrue(canReturnStripeOrderToCart(pending, StripePaymentPhase.CANCELED))
        assertFalse(canReturnStripeOrderToCart(pending, StripePaymentPhase.PENDING))
        assertTrue(canReturnStripeOrderToCart(stripeOrder(StripePaymentStatus.FAILED), StripePaymentPhase.FAILED))
    }

    @Test
    fun `retry is allowed once confirmation timed out but not while it is still waiting`() {
        assertTrue(canRetryStripePayment(StripePaymentStatus.PENDING, StripePaymentPhase.TIMED_OUT))
        assertFalse(canRetryStripePayment(StripePaymentStatus.PENDING, StripePaymentPhase.PENDING))
        assertFalse(canRetryStripePayment(StripePaymentStatus.PROCESSING, StripePaymentPhase.PROCESSING_CONFIRMATION))
        assertTrue(canRetryStripePayment(StripePaymentStatus.FAILED, StripePaymentPhase.FAILED))
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
