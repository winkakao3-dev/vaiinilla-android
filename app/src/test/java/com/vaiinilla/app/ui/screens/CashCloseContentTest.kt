package com.vaiinilla.app.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
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

    // C07 (Doop) cambió el deslizador por el botón «Cerrar caja con $…»: se confirma tocándolo.
    @Test
    fun `tocar cierra una sola vez con el efectivo normalizado`() {
        showSheet()
        composeTestRule.onNodeWithText("Escribe el efectivo contado").assertIsNotEnabled()

        composeTestRule.onNode(hasSetTextAction()).performTextInput("725.5")
        composeTestRule.onNodeWithText("Cerrar caja con $725.50").assertIsEnabled().performClick()
        composeTestRule.waitForIdle()
        assertEquals(listOf("725.50"), closes)
    }

    @Test
    fun `con lector de pantalla el toque confirma`() {
        showSheet()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("1200")
        composeTestRule
            .onNodeWithText("Cerrar caja con $1,200")
            .performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(listOf("1200.00"), closes)
    }

    @Test
    fun `sin un monto valido no se puede cerrar`() {
        showSheet()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("12.345")
        composeTestRule
            .onNodeWithText("Escribe el efectivo contado")
            .assertIsNotEnabled()
            .performClick()
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
        composeTestRule.onNodeWithText("Cerrar caja con $725.50").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Cerrando…").assertIsNotEnabled()

        composeTestRule.runOnIdle { state = CloseCashUi(errorMessage = "Sin conexión") }
        composeTestRule
            .onNodeWithText("Cerrar caja con $725.50")
            .assertIsEnabled()
            .performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(listOf("725.50", "725.50"), closes)
    }

    @Test
    fun `el conteo sobrevive a cerrar la hoja y volver a abrirla`() {
        val count = mutableStateOf(CashCountDraft())
        var shown by mutableStateOf(true)
        composeTestRule.setContent {
            ScreenshotTheme {
                if (shown) {
                    CashCloseContent(
                        state = CloseCashUi(),
                        colors = rememberOperationalColors(),
                        onDismiss = {},
                        onConfirm = { closes += it },
                        onOpenAccounts = null,
                        countState = count,
                    )
                }
            }
        }
        composeTestRule.onNode(hasSetTextAction()).performTextInput("980")
        shown = false
        composeTestRule.waitForIdle()
        shown = true
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("Cerrar caja con $980")
            .performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(listOf("980.00"), closes)
    }

    @Test
    fun `el conteo billete por billete se guarda y se restaura igual`() {
        val draft =
            CashCountDraft(
                amount = "1250.00",
                counting = true,
                billCounts = mapOf(500 to 2, 200 to 1),
                coins = "50",
            )
        val scope = SaverScope { true }
        val saved = with(CashCountDraft.Saver) { scope.save(draft) }!!
        assertEquals(draft, CashCountDraft.Saver.restore(saved))
    }
}
