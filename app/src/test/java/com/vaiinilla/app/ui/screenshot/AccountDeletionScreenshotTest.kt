package com.vaiinilla.app.ui.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.vaiinilla.app.ui.account.AccountDeletionStatus
import com.vaiinilla.app.ui.account.AccountDeletionUiState
import com.vaiinilla.app.ui.screens.AccountDeletionWarnings
import com.vaiinilla.app.ui.screens.WalletAccountScreen
import com.vaiinilla.app.ui.screens.accountDeletionBalance
import com.vaiinilla.app.ui.screens.accountDeletionPoints
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

/** Borrar la cuenta dice antes qué se pierde (Doop E13). */
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
    fun eliminar_cuenta_confirmacion() {
        composeTestRule.setContent {
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