package com.vaiinilla.app.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import com.vaiinilla.app.ui.operational.CloseCashUi
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class CashCloseContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val closes = mutableListOf<String>()

    private fun showSheet() {
        composeTestRule.setContent {
            ScreenshotTheme {
                CashCloseContent(
                    state = CloseCashUi(),
                    colors = rememberOperationalColors(),
                    onDismiss = {},
                    onConfirm = { closes += it },
                    onOpenAccounts = null,
                )
            }
        }
    }

    @Test
    fun `deslizar cierra una sola vez con el efectivo normalizado`() {
        showSheet()
        composeTestRule.onNodeWithContentDescription("Escribe el efectivo contado").assertIsNotEnabled()

        composeTestRule.onNode(hasSetTextAction()).performTextInput("725.5")
        composeTestRule
            .onNodeWithContentDescription(
                "Desliza para cerrar con $725.50",
            ).performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        assertEquals(listOf("725.50"), closes)

        composeTestRule.onNodeWithContentDescription("Desliza para cerrar con $725.50").assertIsNotEnabled()
    }

    @Test
    fun `con lector de pantalla el toque confirma`() {
        showSheet()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("1200")
        composeTestRule
            .onNodeWithContentDescription("Desliza para cerrar con $1,200")
            .performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(listOf("1200.00"), closes)
    }

    @Test
    fun `sin un monto valido no se puede cerrar`() {
        showSheet()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("12.345")
        composeTestRule
            .onNodeWithContentDescription("Escribe el efectivo contado")
            .assertIsNotEnabled()
            .performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        assertEquals(emptyList<String>(), closes)
    }

    @Test
    fun `si el servidor rechaza el cierre se puede volver a deslizar`() {
        var state by mutableStateOf(CloseCashUi())
        composeTestRule.setContent {
            ScreenshotTheme {
                CashCloseContent(
                    state = state,
                    colors = rememberOperationalColors(),
                    onDismiss = {},
                    onConfirm = {
                        closes += it
                        state = CloseCashUi(closing = true)
                    },
                    onOpenAccounts = null,
                )
            }
        }
        composeTestRule.onNode(hasSetTextAction()).performTextInput("725.5")
        composeTestRule
            .onNodeWithContentDescription(
                "Desliza para cerrar con $725.50",
            ).performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Cerrando…").assertIsNotEnabled()

        composeTestRule.runOnIdle { state = CloseCashUi(errorMessage = "Sin conexión") }
        composeTestRule
            .onNodeWithContentDescription("Desliza para cerrar con $725.50")
            .assertIsEnabled()
            .performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(listOf("725.50", "725.50"), closes)
    }
}
