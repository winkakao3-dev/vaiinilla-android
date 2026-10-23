package com.vaiinilla.app.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.ui.operational.OperationalUiState
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * La tarjeta del pedido en Caja se reiniciaba sola (G02): cada sondeo trae una copia nueva del pedido y, si
 * arriba aparecía un aviso, la tarjeta se rehacía con la propina en "Sin" y el efectivo en el total.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi", sdk = [33])
class CashierHeroDraftUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `la propina elegida sigue tras el sondeo aunque aparezca un aviso arriba`() {
        val pending = ScreenshotFixtures.sampleOrder(state = OrderState.PENDING_PAYMENT)
        var state by mutableStateOf(
            OperationalUiState(
                role = OperationalRole.CASHIER,
                orders = listOf(pending),
                cashSessionOpen = true,
                catalog = ScreenshotFixtures.catalogLoadedState().catalog,
            ),
        )
        var collected: Pair<String, String?>? = null
        composeTestRule.setContent {
            ScreenshotTheme {
                CashierOperationalScreen(
                    state = state,
                    onBack = {},
                    onOpenCashSession = {},
                    onCollect = { _, amount, _, tip -> collected = amount to tip },
                    onChangeMode = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("10%").performClick()
        composeTestRule.onNodeWithText("Cobrar $90.20").assertIsDisplayed()

        // El sondeo: el mismo pedido con otra versión, y un aviso que entra arriba de la tarjeta.
        state =
            state.copy(
                orders = listOf(pending.copy(summary = pending.summary.copy(version = 2))),
                errorMessage = "No se pudo actualizar la caja.",
            )
        composeTestRule.waitForIdle()
        state = state.copy(errorMessage = null)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Cobrar $90.20").assertIsDisplayed().performClick()
        assertEquals("90.20" to "8.20", collected)
    }

    @Test
    fun `si cambia el total lo escrito se vuelve a sugerir`() {
        val pending = ScreenshotFixtures.sampleOrder(state = OrderState.PENDING_PAYMENT)
        var state by mutableStateOf(
            OperationalUiState(
                role = OperationalRole.CASHIER,
                orders = listOf(pending),
                cashSessionOpen = true,
                catalog = ScreenshotFixtures.catalogLoadedState().catalog,
            ),
        )
        composeTestRule.setContent {
            ScreenshotTheme {
                CashierOperationalScreen(
                    state = state,
                    onBack = {},
                    onOpenCashSession = {},
                    onCollect = { _, _, _, _ -> },
                    onChangeMode = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("10%").performClick()
        state = state.copy(orders = listOf(pending.copy(summary = pending.summary.copy(version = 2, total = "60.00"))))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Cobrar $60.00").assertIsDisplayed()
    }
}
