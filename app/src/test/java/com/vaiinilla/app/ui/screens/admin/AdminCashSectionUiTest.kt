package com.vaiinilla.app.ui.screens.admin

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.vaiinilla.app.ui.admin.AdminUiState
import com.vaiinilla.app.ui.screens.receiptLocalTime
import com.vaiinilla.app.ui.screens.rememberOperationalColors
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** J09: el admin ve con cuánto y a qué hora se abrió la caja. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class AdminCashSectionUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `la caja abierta muestra el fondo inicial y la hora`() {
        val openedAt = "2026-10-27T15:05:00Z"
        composeTestRule.setContent {
            ScreenshotTheme {
                AdminCashSection(
                    state =
                        AdminUiState(
                            cashLoaded = true,
                            cashSessionId = "s-1",
                            cashOpeningAmount = "500.00",
                            cashOpenedAt = openedAt,
                        ),
                    colors = rememberOperationalColors(),
                    readOnly = true,
                    actions = AdminCashActions(onOpenCash = { _, _ -> }, onCloseCash = { _, _ -> }),
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Fondo inicial").assertIsDisplayed()
        composeTestRule.onNodeWithText("$500").assertIsDisplayed()
        composeTestRule.onNodeWithText(receiptLocalTime(openedAt)).assertIsDisplayed()
    }
}
