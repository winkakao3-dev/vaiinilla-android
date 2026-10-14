package com.vaiinilla.app.ui.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.core.qr.QrCodeEncoder
import com.vaiinilla.app.ui.discovery.QrPayloadParser
import com.vaiinilla.app.ui.screens.LocalScene
import com.vaiinilla.app.ui.screens.WalletCodeOverlay
import com.vaiinilla.app.ui.screens.jarFillLevel
import com.vaiinilla.app.ui.screens.scenePaletteForTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Las escenas del Vainiverso que llegaron de iOS, quietas en sus momentos clave, a cada hora del día. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class VainiversoParityScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun capture(
        name: String,
        mode: VaiinillaThemeMode = VaiinillaThemeMode.Light,
        content: @Composable () -> Unit,
    ) {
        composeTestRule.setContent {
            ScreenshotTheme(mode) {
                CompositionLocalProvider(LocalScene provides scenePaletteForTheme(), content = content)
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("vainiverso/$name.png")
    }

    private val walletMatrix = QrCodeEncoder.encode(QrPayloadParser.encodeUser("sandbox-agente-cliente"))

    @Composable
    private fun WalletCode(at: Float) {
        WalletCodeOverlay(
            matrix = walletMatrix,
            shown = true,
            fill = jarFillLevel("120.00"),
            label = "$120.00",
            onClose = {},
            frozenAt = at,
        )
    }

    @Test
    fun `wallet_code_flight_day`() = capture("wallet_code_flight_day") { WalletCode(0.42f) }

    @Test
    fun `wallet_code_day`() = capture("wallet_code_day") { WalletCode(1f) }

    @Test
    fun `wallet_code_night`() = capture("wallet_code_night", VaiinillaThemeMode.Dark) { WalletCode(1f) }
}
