package com.vaiinilla.app.ui.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.vaiinilla.app.ui.account.AccountDeletionStatus
import com.vaiinilla.app.ui.account.AccountDeletionUiState
import com.vaiinilla.app.ui.screens.AccountDeletionWarnings
import com.vaiinilla.app.ui.screens.WalletAccountScreen
import com.vaiinilla.app.ui.screens.accountDeletionBalance
import com.vaiinilla.app.ui.screens.accountDeletionPoints
import com.vaiinilla.app.ui.screens.accountTitle
import com.vaiinilla.app.ui.screens.staffModesLine
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Cuenta como el lienzo (Doop E12) y borrar la cuenta dice antes qué se pierde (E13). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class AccountDeletionScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val warnings =
        AccountDeletionWarnings(
            balance = accountDeletionBalance("245.50"),
            venueName = "Pastelería Vaini",
            principalChildren = listOf("Sofía", "Mateo"),
            staffRoles = listOf("Caja", "Cocina"),
        )

    @Test
    fun `solo avisa del saldo si queda algo`() {
        assertNull(accountDeletionBalance("0.00"))
        assertNull(accountDeletionBalance(null))
        assertEquals(true, accountDeletionBalance("245.50")?.contains("245"))
    }

    @Test
    fun `nombra a los hijos y los modos que se pierden`() {
        val points = accountDeletionPoints(warnings)
        assertEquals(3, points.size)
        assertEquals(true, points.first().startsWith("Sofía y Mateo te tienen como tutor principal"))
        assertEquals("Pierdes tus modos de Caja y Cocina.", points.last())
        assertEquals(1, accountDeletionPoints(AccountDeletionWarnings()).size)
    }

    @Test
    fun `la cuenta sin nombre propio no repite el correo`() {
        assertEquals("Tu cuenta", accountTitle("robot@kakaowin.garden", "robot@kakaowin.garden"))
        assertEquals("Kao Arreola", accountTitle("Kao Arreola", "kao@correo.mx"))
        assertEquals("Caja y Cocina en Pastelería Vaini", staffModesLine(listOf("Caja", "Cocina"), "Pastelería Vaini"))
        assertEquals("Caja, cocina o mesero", staffModesLine(emptyList(), "Pastelería Vaini"))
    }

    @Test
    fun cuenta_como_doop() {
        composeTestRule.setContent {
            VaiinillaTheme(themeMode = VaiinillaThemeMode.Dark) {
                WalletAccountScreen(
                    onBack = {},
                    displayName = "Kao Arreola",
                    email = "kao@correo.mx",
                    userId = "u-preview",
                    signedIn = true,
                    venueName = "Pastelería Vaini",
                    hasStaffModes = true,
                    onOpenNotices = {},
                    deletionWarnings = warnings,
                    termsUrl = "https://app.vaiinilla.app/legal/terminos/2026-10",
                    privacyUrl = "https://app.vaiinilla.app/legal/privacidad/2026-10",
                )
            }
        }
        composeTestRule.onNodeWithText("Tu cafetería · cambiar").assertExists()
        composeTestRule.onNodeWithText("Caja y Cocina en Pastelería Vaini").assertExists()
        composeTestRule.onNodeWithText("APARIENCIA").assertExists()
        composeTestRule.onNodeWithText("Auto").assertExists()
        composeTestRule.onNodeWithText("Eliminar mi cuenta").assertExists()
        captureScreenRoboImage("wallet/e12_cuenta_oscura.png")

        composeTestRule.onNodeWithText("Términos y privacidad").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Aviso de privacidad").assertExists()
    }

    @Test
    fun eliminar_cuenta_confirmacion() {        composeTestRule.setContent {
            VaiinillaTheme(themeMode = VaiinillaThemeMode.Dark) {
                WalletAccountScreen(
                    onBack = {},
                    displayName = "Kao Arreola",
                    signedIn = true,
                    accountDeletionState = AccountDeletionUiState(status = AccountDeletionStatus.Confirmation),
                    deletionWarnings = warnings,
                )
            }
        }
        composeTestRule.onNodeWithText("Eliminar para siempre").assertExists()
        captureScreenRoboImage("eliminar_cuenta_confirmacion.png")
    }
}