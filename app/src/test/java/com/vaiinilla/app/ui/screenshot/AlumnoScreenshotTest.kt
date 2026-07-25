package com.vaiinilla.app.ui.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.ui.screens.AssistantScreen
import com.vaiinilla.app.ui.screens.CartScreen
import com.vaiinilla.app.ui.screens.CatalogScreen
import com.vaiinilla.app.ui.screens.RoleSelectorScreen
import com.vaiinilla.app.ui.screens.SplashScreen
import com.vaiinilla.app.ui.screens.WalletScreen
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
class AlumnoScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `01_splash`() {
        composeTestRule.setContent {
            ScreenshotTheme {
                SplashScreen(onFinished = {})
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("01_splash.png")
    }

    @Test
    fun `02_role_selector`() {
        composeTestRule.setContent {
            ScreenshotTheme {
                RoleSelectorScreen(
                    testOnlyMode = true,
                    onTestOnlyModeChange = {},
                    onRoleSelected = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("02_role_selector.png")
    }

    @Test
    fun `03_catalog`() {
        val state = ScreenshotFixtures.catalogLoadedState()
        composeTestRule.setContent {
            ScreenshotTheme {
                CatalogScreen(
                    state = state,
                    onRetry = {},
                    onSearchChange = {},
                    onCategorySelected = {},
                    onProductSelected = {},
                    onDismissProduct = {},
                    onToggleOption = { _, _ -> },
                    onClearOptionalGroup = {},
                    onQuantityChange = {},
                    onAddProduct = {},
                    onOpenCart = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("03_catalog.png")
    }

    @Test
    fun `04_cart`() {
        val state = ScreenshotFixtures.cartState()
        composeTestRule.setContent {
            ScreenshotTheme {
                CartScreen(
                    state = state,
                    onMenu = {},
                    onQuantityChange = { _, _ -> },
                    onNotesChange = {},
                    onDestinationChange = {},
                    onPaymentChange = {},
                    onConfirm = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("04_cart.png")
    }

    @Test
    fun `05_assistant_hub`() {
        val state = ScreenshotFixtures.catalogLoadedState()
        composeTestRule.setContent {
            ScreenshotTheme {
                AssistantScreen(
                    state = state,
                    onOpenChat = {},
                    onOpenProduct = {},
                    onMenu = {},
                    onOrders = {},
                    onWallet = {},
                    onCart = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("05_assistant_hub.png")
    }

    @Test
    fun `06_wallet`() {
        val state = ScreenshotFixtures.catalogLoadedState()
        composeTestRule.setContent {
            ScreenshotTheme {
                WalletScreen(
                    state = state,
                    balance = 200,
                    onAddMoney = {},
                    onPaymentMethods = {},
                    onAccount = {},
                    onMenu = {},
                    onAssistant = {},
                    onOrders = {},
                    onCart = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("06_wallet.png")
    }
}
