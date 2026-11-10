package com.vaiinilla.app.ui.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.vaiinilla.app.domain.auth.student.StudentAuthMfaFactor
import com.vaiinilla.app.domain.auth.student.TotpEnrollmentData
import com.vaiinilla.app.ui.components.TotpMfaChallengeForm
import com.vaiinilla.app.ui.screens.platform.TotpEnrollStage
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Capturas de Super Admin / Plataforma en tema oscuro para Doop.
 * Muestra el enrolamiento directo del autenticador y la confirmación de seguridad.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class PlatformTotpDoopScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun dark(content: @Composable () -> Unit) =
        composeTestRule.setContent {
            ScreenshotTheme(mode = VaiinillaThemeMode.Dark) {
                val colors = LocalVaiinillaColors.current
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(colors.paper)
                        .padding(20.dp),
                ) {
                    content()
                }
            }
        }

    private fun settle(ms: Long = 1_500) {
        composeTestRule.mainClock.advanceTimeBy(ms)
        composeTestRule.waitForIdle()
    }

    private val sampleEnrollData =
        TotpEnrollmentData(
            sharedSecretKey = "T6MSWXXFJ6LL553DE2BCTXWUTDCLB3I3",
            qrCodeUrl =
                "otpauth://totp/Vaiinilla%20dev:cesar.kao%40kakaowin.garden" +
                    "?secret=T6MSWXXFJ6LL553DE2BCTXWUTDCLB3I3&issuer=Vaiinilla",
        )

    @Test
    fun doop_superadmin_totp_enrolar() {
        dark {
            TotpEnrollStage(
                enrollData = sampleEnrollData,
                code = "582",
                loading = false,
                errorMessage = null,
                onCodeChange = {},
                onSubmit = {},
                onCancel = {},
            )
        }
        settle()
        captureScreenRoboImage("galeria/doop_superadmin_totp_enrolar.png")
    }

    @Test
    fun doop_superadmin_totp_enrolar_error() {
        dark {
            TotpEnrollStage(
                enrollData = sampleEnrollData,
                code = "123456",
                loading = false,
                errorMessage = "El código no es válido. Revisa tu aplicación autenticadora e inténtalo de nuevo.",
                onCodeChange = {},
                onSubmit = {},
                onCancel = {},
            )
        }
        settle()
        captureScreenRoboImage("galeria/doop_superadmin_totp_enrolar_error.png")
    }

    @Test
    fun doop_superadmin_totp_desafio() {
        dark {
            TotpMfaChallengeForm(
                factors = listOf(StudentAuthMfaFactor("f1", "Google Authenticator")),
                selectedFactorUid = "f1",
                code = "481920",
                loading = false,
                errorMessage = null,
                onFactorSelected = {},
                onCodeChange = {},
                onSubmit = {},
                onCancel = {},
            )
        }
        settle()
        captureScreenRoboImage("galeria/doop_superadmin_totp_desafio.png")
    }
}
