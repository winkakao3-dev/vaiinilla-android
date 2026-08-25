package com.vaiinilla.app.ui.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.domain.model.OrderPayment
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.StripePaymentStatus
import com.vaiinilla.app.ui.order.StripePaymentPhase
import com.vaiinilla.app.ui.screens.OrderConfirmationScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class StripePaymentScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun capture(
        name: String,
        content: @Composable () -> Unit,
    ) {
        composeTestRule.setContent {
            ScreenshotTheme(content = content)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(name)
    }

    @Test
    fun `56 stripe payment pending`() {
        capture("56_stripe_payment_pending.png") {
            OrderConfirmationScreen(
                order = stripeOrder(StripePaymentStatus.PENDING),
                onReturnToMenu = {},
                stripePaymentPhase = StripePaymentPhase.PENDING,
                stripePaymentMessage = "Estamos verificando el pago con Vaiinilla.",
                onViewTracking = {},
            )
        }
    }

    @Test
    fun `57 stripe payment confirmed`() {
        capture("57_stripe_payment_confirmed.png") {
            OrderConfirmationScreen(
                order = stripeOrder(StripePaymentStatus.CONFIRMED, OrderState.PAID),
                onReturnToMenu = {},
                stripePaymentPhase = StripePaymentPhase.CONFIRMED,
                onViewTracking = {},
            )
        }
    }

    @Test
    fun `58 stripe payment failed`() {
        capture("58_stripe_payment_failed.png") {
            OrderConfirmationScreen(
                order = stripeOrder(StripePaymentStatus.FAILED),
                onReturnToMenu = {},
                stripePaymentPhase = StripePaymentPhase.FAILED,
                onRetryStripePayment = {},
                onViewTracking = {},
            )
        }
    }

    @Test
    fun `59 stripe payment canceled`() {
        capture("59_stripe_payment_canceled.png") {
            OrderConfirmationScreen(
                order = stripeOrder(StripePaymentStatus.CANCELED),
                onReturnToMenu = {},
                stripePaymentPhase = StripePaymentPhase.CANCELED,
                onRetryStripePayment = {},
                onViewTracking = {},
            )
        }
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
