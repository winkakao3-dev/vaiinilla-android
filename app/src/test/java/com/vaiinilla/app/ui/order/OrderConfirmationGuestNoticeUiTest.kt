package com.vaiinilla.app.ui.order

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.ui.screens.GUEST_ORDER_NOTICE
import com.vaiinilla.app.ui.screens.OrderConfirmationScreen
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A06: sin cuenta, la confirmación avisa que el pedido solo se ve en este teléfono. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi", sdk = [33])
class OrderConfirmationGuestNoticeUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun show(guest: Boolean) {
        composeTestRule.setContent {
            VaiinillaTheme {
                OrderConfirmationScreen(
                    order = ScreenshotFixtures.sampleOrder(state = OrderState.PREPARING),
                    onReturnToMenu = {},
                    showTrackingButton = false,
                    guest = guest,
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(2_000)
    }

    @Test
    fun `el invitado ve el aviso`() {
        show(guest = true)
        composeTestRule.onAllNodesWithText(GUEST_ORDER_NOTICE).assertCountEquals(1)
    }

    @Test
    fun `con cuenta no hay aviso`() {
        show(guest = false)
        composeTestRule.onAllNodesWithText(GUEST_ORDER_NOTICE).assertCountEquals(0)
    }
}
