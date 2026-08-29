package com.vaiinilla.app.ui.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.ui.components.StudentTab
import com.vaiinilla.app.ui.screens.CartScreen
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
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
class PaymentOverlayScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `60 payment overlay light selected`() {
        captureSelectedOverlay("60_payment_overlay_light_selected.png", VaiinillaThemeMode.Light)
    }

    @Test
    fun `61 payment overlay dark selected`() {
        captureSelectedOverlay("61_payment_overlay_dark_selected.png", VaiinillaThemeMode.Dark)
    }

    @Test
    fun `62 payment overlay amoled selected`() {
        captureSelectedOverlay("62_payment_overlay_amoled_selected.png", VaiinillaThemeMode.Amoled)
    }

    private fun captureSelectedOverlay(
        name: String,
        mode: VaiinillaThemeMode,
    ) {
        composeTestRule.setContent {
            ScreenshotTheme(mode = mode) {
                ScreenshotWithStudentNav(activeTab = StudentTab.CART, cartCount = 3) {
                    CartScreen(
                        state = ScreenshotFixtures.cartState(),
                        onMenu = {},
                        onQuantityChange = { _, _ -> },
                        onNotesChange = {},
                        onDestinationChange = {},
                        onPaymentChange = {},
                        onConfirm = {},
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Pagar").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Pago desde la app").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(name)
    }
}
