package com.vaiinilla.app.ui.support

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Ayuda (Doop L12): el último pedido con «Pedir ayuda», los temas que se abren y el correo a soporte. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class SupportScreenUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val opened = mutableListOf<String>()

    private fun screen(mode: VaiinillaThemeMode = VaiinillaThemeMode.Dark) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalUriHandler provides
                    object : UriHandler {
                        override fun openUri(uri: String) {
                            opened += uri
                        }
                    },
            ) {
                ScreenshotTheme(mode = mode) {
                    SupportScreen(
                        onBack = {},
                        termsUrl = "https://app.vaiinilla.app/legal/terminos/2026-10",
                        privacyUrl = "https://app.vaiinilla.app/legal/privacidad/2026-10",
                        lastOrder = SupportLastOrder(folio = 221, state = "En cocina", total = "$137"),
                    )
                }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(1_500)
    }

    @Test
    fun `ayuda como el dibujo de Doop`() {
        screen()
        composeTestRule.onRoot().captureRoboImage("lugar/l12_ayuda.png")
    }

    @Test
    fun `pedir ayuda escribe a soporte con el folio en el asunto`() {
        screen()
        composeTestRule.onNodeWithText("Pedir ayuda").performClick()
        composeTestRule.onNodeWithText("Escribir a soporte").performClick()
        assertEquals(
            listOf("mailto:equipo@vaiinilla.app?subject=Ayuda%20con%20el%20pedido%20%23221", "mailto:equipo@vaiinilla.app"),
            opened,
        )
    }

    @Test
    fun `un tema se abre en su lugar`() {
        screen()
        composeTestRule.onNodeWithText("Pagos").performClick()
        composeTestRule.mainClock.advanceTimeBy(1_000)
        composeTestRule.onNodeWithText(SupportCopy.cards[1].body).assertIsDisplayed()
    }
}