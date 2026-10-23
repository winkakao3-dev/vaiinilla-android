package com.vaiinilla.app.ui.order

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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

/**
 * M05: el servidor rechaza el pedido (p. ej. el límite del tutor) después de que se cierra la hoja de pago;
 * el motivo iba al final de la lista, fuera de la pantalla, y parecía que no pasaba nada.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class CartOrderErrorVisibleTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `el error del servidor al pedir se ve sin hacer scroll`() {
        val base = ScreenshotFixtures.cartState()
        val products = requireNotNull(base.catalog).products
        val lines =
            (0 until 6).map { i ->
                val product = products[i % products.size].copy(id = 900 + i, name = "Producto ${i + 1}")
                CartLine(product = product, quantity = 1, selectedOptionIds = emptySet())
            }
        var state by mutableStateOf(base.copy(cartLines = lines))
        composeTestRule.setContent {
            ScreenshotTheme {
                ScreenshotWithStudentNav(activeTab = StudentTab.CART, cartCount = lines.size) {
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

        state = state.copy(createOrderError = "Con esta compra pasarías tu límite de hoy.")
        composeTestRule.mainClock.advanceTimeBy(2_000)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Con esta compra pasarías tu límite de hoy.").assertIsDisplayed()
    }
}
