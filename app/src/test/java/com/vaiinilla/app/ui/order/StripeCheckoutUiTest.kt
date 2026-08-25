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

        composeTestRule.onNodeWithText("Confirmando tu pago").assertIsDisplayed()
        composeTestRule.onNodeWithText(message).assertIsDisplayed()
        composeTestRule.onNodeWithText("Total a pagar").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Reintentar pago").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Pasa a Caja").assertCountEquals(0)
    }

    @Test
    fun `processing Stripe payment shows a waiting state without retry`() {
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
                            status = StripePaymentStatus.PROCESSING,
                        ),
                )

        composeTestRule.setContent {
            VaiinillaTheme {
                OrderConfirmationScreen(
                    order = order,
                    onReturnToMenu = {},
                    stripePaymentPhase = StripePaymentPhase.PENDING,
                )
            }
        }

        composeTestRule.onNodeWithText("Pago en proceso").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Reintentar pago").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Pasa a Caja").assertCountEquals(0)
    }

    @Test
    fun `backend failure enables retry but never shows cash instructions`() {
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
                            status = StripePaymentStatus.FAILED,
                        ),
                )

        composeTestRule.setContent {
            VaiinillaTheme {
                OrderConfirmationScreen(
                    order = order,
                    onReturnToMenu = {},
                    stripePaymentPhase = StripePaymentPhase.FAILED,
                    onRetryStripePayment = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Pago no completado").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reintentar pago").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Pasa a Caja").assertCountEquals(0)
    }
}
