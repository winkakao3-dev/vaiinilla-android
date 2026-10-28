package com.vaiinilla.app.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Abrir caja con el fondo que propone el servidor ya escrito, y vacío si no llega. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi", sdk = [33])
class CashOpenSuggestedFloatTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val json = OrderContractJson()

    @Test
    fun `lee fondo_sugerido con o sin sesion y nada si no llega`() {
        assertEquals(
            "500.00",
            json.parseSuggestedFloat("""{"data":null,"meta":{"fondo_sugerido":"500"},"error":null}"""),
        )
        assertEquals(
            "350.50",
            json.parseSuggestedFloat(
                """{"data":{"id":"s","monto_inicial":"0","fondo_sugerido":350.5},"error":null}""",
            ),
        )
        assertEquals("500.00", json.parseSuggestedFloat("""{"data":null,"fondo_sugerido":"500.00","error":null}"""))
        assertNull(json.parseSuggestedFloat("""{"data":null,"meta":{},"error":null}"""))
        assertNull(json.parseSuggestedFloat("""{"data":null,"meta":{"fondo_sugerido":null},"error":null}"""))
    }

    @Test
    fun `con fondo sugerido se abre en un toque con ese monto`() {
        val confirmed = mutableListOf<String>()
        composeTestRule.setContent {
            ScreenshotTheme {
                CashOpenContent(
                    opening = false,
                    errorMessage = null,
                    colors = rememberOperationalColors(),
                    onConfirm = { confirmed += it },
                    suggestedAmount = "500.00",
                )
            }
        }
        composeTestRule.onNodeWithText("Abrir con", substring = true).performClick()
        assertEquals(1, confirmed.size)
        assertEquals(0, confirmed.single().toBigDecimal().compareTo("500".toBigDecimal()))
    }

    @Test
    fun `sin fondo sugerido el campo queda vacio`() {
        composeTestRule.setContent {
            ScreenshotTheme {
                CashOpenContent(
                    opening = false,
                    errorMessage = null,
                    colors = rememberOperationalColors(),
                    onConfirm = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Escribe el efectivo del cajón").assertExists()
    }
}
