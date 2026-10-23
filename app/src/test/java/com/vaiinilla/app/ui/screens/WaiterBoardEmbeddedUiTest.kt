package com.vaiinilla.app.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** El tablero del mesero dentro de Pedidos de Administración: pide en la mesa o para llevar, sin cobrar ni entregar. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class WaiterBoardEmbeddedUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `dentro de Administración trae Para llevar en vez del encabezado y no deja entregar`() {
        var takeAway = 0
        composeTestRule.setContent {
            ScreenshotTheme {
                WaiterOperationalScreen(
                    state = ScreenshotFixtures.waiterBoardState(),
                    onBack = {},
                    onOpenCounterOrder = { takeAway += 1 },
                    embedded = true,
                    canChargeAndDeliver = false,
                )
            }
        }

        composeTestRule.onNodeWithText("Pedido de mostrador").assertDoesNotExist()
        composeTestRule.onNodeWithText("Cuenta de mesero").assertDoesNotExist()
        composeTestRule.onNodeWithText("Para llevar").performClick()
        assertEquals(1, takeAway)

        // La mesa 12 (el mosaico dice "12") tiene un pedido listo: el admin lo ve, pero entregarlo es de Mesero y Caja.
        composeTestRule.onNodeWithText("12").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Tomar pedido").assertIsDisplayed()
        composeTestRule.onNodeWithText("Entregar").assertDoesNotExist()
    }

    @Test
    fun `el mesero sigue con su encabezado y su botón de entregar`() {
        composeTestRule.setContent {
            ScreenshotTheme {
                WaiterOperationalScreen(state = ScreenshotFixtures.waiterBoardState(), onBack = {})
            }
        }

        composeTestRule.onNodeWithText("Pedido de mostrador").assertIsDisplayed()
        composeTestRule.onNodeWithText("12").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Entregar").assertIsDisplayed()
    }
}
