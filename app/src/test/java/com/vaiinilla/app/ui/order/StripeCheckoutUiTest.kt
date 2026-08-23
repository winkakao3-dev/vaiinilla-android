package com.vaiinilla.app.ui.order

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.vaiinilla.app.domain.model.OrderPayment
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.StripePaymentStatus
import com.vaiinilla.app.ui.components.CheckoutPaymentPicker
import com.vaiinilla.app.ui.screens.OrderConfirmationScreen
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class StripeCheckoutUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `checkout exposes Stripe as tarjeta without changing cash or balance labels`() {
        composeTestRule.setContent {
            VaiinillaTheme {
                CheckoutPaymentPicker(
                    selected = PaymentMethod.STRIPE,
                    onSelect = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Efectivo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Saldo").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Tarjeta").assertCountEquals(1)
    }

    @Test
    fun `network uncertainty stays pending instead of showing false failure`() {
        val order =
            ScreenshotFixtures
                .sampleOrder(
                    state = OrderState.PENDING_PAYMENT,
                    paymentMethod = PaymentMethod.STRIPE,
                ).copy(
                    payment =
                        OrderPayment(
                            paymentAttemptId = "attempt-1",
                            paymentIntentId = "pi_test_001",
                            stripeAccountId = "acct_test_001",
                            status = StripePaymentStatus.PENDING,
                        ),
                )
        val message = "No pudimos confirmar el estado del pago todavía."

        composeTestRule.setContent {
            VaiinillaTheme {
                OrderConfirmationScreen(
                    order = order,
                    onReturnToMenu = {},
                    stripePaymentPhase = StripePaymentPhase.PENDING,
                    stripePaymentMessage = message,
                )
            }
        }

        composeTestRule.onNodeWithText("Pago pendiente").assertIsDisplayed()
        composeTestRule.onAllNodesWithText(message).assertCountEquals(2)
    }
}
