package com.vaiinilla.app.ui.discovery

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Assert.assertEquals
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
class QrScannerOverlayTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val submitted = mutableListOf<String>()
    private var withoutTable = 0

    private fun overlay(venueName: String?) {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            ScreenshotTheme(mode = VaiinillaThemeMode.Light) {
                QrScannerOverlay(
                    title = "Apunta al QR de tu mesa",
                    subtitle = "También sirve acercar el teléfono a la etiqueta",
                    systemBars = WindowInsets(top = 40, bottom = 24),
                    onClose = {},
                    torchAvailable = true,
                    torchOn = false,
                    onToggleTorch = {},
                    tableCode =
                        QrScannerTableCode(
                            onSubmit = { submitted += it },
                            venueName = venueName,
                            onOrderWithoutTable = { withoutTable++ },
                        ),
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(1_200)
    }

    @Test
    fun `escanea tu mesa como el dibujo de Doop`() {
        overlay(venueName = "Pastelería Vaini")
        composeTestRule.onNodeWithText("¿No ves un QR?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Para llevar o a domicilio en Pastelería Vaini").assertIsDisplayed()
        composeTestRule.onNodeWithText("Linterna").assertIsDisplayed()
        captureScreenRoboImage("lugar/l01_escanea_mesa.png")
    }

    @Test
    fun `los cuatro numeros entran solos y pedir sin mesa avisa`() {
        overlay(venueName = null)
        composeTestRule.onNodeWithText("Busca el lugar para pedir para llevar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("scanner-table-code").performTextInput("40a21")
        composeTestRule.mainClock.advanceTimeBy(400)
        assertEquals(listOf("4021"), submitted)
        composeTestRule.onNodeWithTag("scanner-without-table").performClick()
        assertEquals(1, withoutTable)
    }
}
