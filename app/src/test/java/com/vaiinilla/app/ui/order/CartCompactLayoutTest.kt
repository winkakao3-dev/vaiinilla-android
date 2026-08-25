package com.vaiinilla.app.ui.order

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.ui.components.StudentTab
import com.vaiinilla.app.ui.screens.CartScreen
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import com.vaiinilla.app.ui.screenshot.ScreenshotWithStudentNav
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
class CartCompactLayoutTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `two product checkout keeps key actions visible without scrolling`() {
        val base = ScreenshotFixtures.cartState()
        val products = requireNotNull(base.catalog).products
        val state =
            base.copy(
                cartLines =
                    listOf(
                        base.cartLines.first(),
                        CartLine(
                            product = products[1],
                            quantity = 1,
                            selectedOptionIds = emptySet(),
                        ),
                    ),
            )

        composeTestRule.setContent {
            ScreenshotTheme {
                ScreenshotWithStudentNav(activeTab = StudentTab.CART, cartCount = 2) {
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
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Pago").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Sin costo de entrega").assertCountEquals(0)
        composeTestRule.onNodeWithText("Añadir nota para cocina").assertIsDisplayed()
        composeTestRule.onNodeWithText("Subtotal").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirmar pedido").assertIsDisplayed()
    }
}
