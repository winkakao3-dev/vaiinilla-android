package com.vaiinilla.app.ui.order

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.ui.screens.OrderConfirmationScreen
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** B07: un pedido entregado abre su recibo (método de pago y compartir como sticker) desde la recogida. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi", sdk = [33])
class DeliveredReceiptUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `a delivered order offers its receipt`() {
        var opened = 0
        composeTestRule.setContent {
            VaiinillaTheme {
                OrderConfirmationScreen(
                    order = ScreenshotFixtures.sampleOrder(state = OrderState.DELIVERED),
                    onReturnToMenu = {},
                    showTrackingButton = false,
                    onViewReceipt = { opened++ },
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(2_000)
        // La hoja puede quedar más abajo que la pantalla de prueba: se toca por su acción, como TalkBack.
        composeTestRule.onNodeWithText("Ver recibo").performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(1, opened)
    }

    @Test
    fun `an order still in the kitchen has no receipt yet`() {
        composeTestRule.setContent {
            VaiinillaTheme {
                OrderConfirmationScreen(
                    order = ScreenshotFixtures.sampleOrder(state = OrderState.PREPARING),
                    onReturnToMenu = {},
                    showTrackingButton = false,
                    onViewReceipt = {},
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(2_000)
        composeTestRule.onAllNodesWithText("Ver recibo").assertCountEquals(0)
    }
}
