package com.vaiinilla.app.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.vaiinilla.app.ui.account.AccountDeletionStatus
import com.vaiinilla.app.ui.account.AccountDeletionUiState
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tu cuenta (W03): lo que se ofrece, el cierre de sesión con confirmación y el aviso de lo que se pierde al
 * eliminarla. Corre en la JVM con Robolectric y Compose; no cuenta como recorrido en teléfono.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class RecorridosCuentaPantallaTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun show(
        hasStaffModes: Boolean = false,
        warnings: AccountDeletionWarnings = AccountDeletionWarnings(),
        deletion: AccountDeletionUiState = AccountDeletionUiState(),
        onSignOut: () -> Unit = {},
        onRequestDeletion: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            VaiinillaTheme(themeMode = VaiinillaThemeMode.Dark) {
                WalletAccountScreen(
                    onBack = {},
                    displayName = "Kao Papá",
                    email = "kao.papa@correo.mx",
                    userId = "u-1",
                    signedIn = true,
                    venueName = "Boliche Strike",
                    hasStaffModes = hasStaffModes,
                    onOpenNotices = {},
                    onSignOut = onSignOut,
                    accountDeletionState = deletion,
                    onRequestAccountDeletion = onRequestDeletion,
                    deletionWarnings = warnings,
                    termsUrl = "https://app.vaiinilla.app/legal/terminos/2026-10",
                    privacyUrl = "https://app.vaiinilla.app/legal/privacidad/2026-10",
                )
            }
        }
    }

    @Test
    fun w03_laCuentaOfreceCodigoAparienciaAvisosAyudaYTerminos() {
        show()
        composeTestRule.onNodeWithText("Mi código").assertIsDisplayed()
        composeTestRule.onNodeWithText("APARIENCIA").assertExists()
        composeTestRule.onNodeWithText("Auto").assertExists()
        composeTestRule.onNodeWithText("Avisos recibidos").assertExists()
        composeTestRule.onNodeWithText("Ayuda y soporte").assertExists()
        composeTestRule.onNodeWithText("Términos y privacidad").assertExists()
        composeTestRule.onNodeWithText("Cerrar sesión").assertExists()
        composeTestRule.onNodeWithText("Eliminar mi cuenta").assertExists()
    }

    @Test
    fun w03_sinSerPersonalNoSaleModosDePersonalNiSeHablaDeHijos() {
        show(hasStaffModes = false)
        composeTestRule.onNodeWithText("Modos de personal").assertDoesNotExist()
        assertTrue(composeTestRule.onAllNodesWithText("hijos", substring = true, ignoreCase = true).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun w03_siEsPersonalSaleModosDePersonal() {
        show(hasStaffModes = true)
        composeTestRule.onNodeWithText("Modos de personal").assertExists()
    }

    @Test
    fun w03_cerrarSesionPideConfirmacionAntesDeSalir() {
        var signedOut = 0
        show(onSignOut = { signedOut++ })
        composeTestRule.onNodeWithText("Cerrar sesión").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("¿Cerrar sesión?").assertIsDisplayed()
        assertEquals(0, signedOut)
        composeTestRule.onAllNodesWithText("Cerrar sesión")[composeTestRule.onAllNodesWithText("Cerrar sesión").fetchSemanticsNodes().size - 1].performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, signedOut)
    }

    @Test
    fun w03_eliminarLaCuentaAvisaDelSaldoYSeCancelaConMejorNo() {
        show(
            warnings = AccountDeletionWarnings(balance = accountDeletionBalance("155.00")),
            deletion = AccountDeletionUiState(status = AccountDeletionStatus.Confirmation),
        )
        composeTestRule.onNodeWithText("Eliminar tu cuenta").assertIsDisplayed()
        assertTrue(composeTestRule.onAllNodesWithText("155", substring = true).fetchSemanticsNodes().isNotEmpty())
        composeTestRule.onNode(hasText("Mejor no")).assertIsDisplayed()
    }

    @Test
    fun w03_eliminarMiCuentaPideEmpezarElFlujo() {
        var requested = 0
        show(onRequestDeletion = { requested++ })
        composeTestRule.onNodeWithText("Eliminar mi cuenta").performScrollTo().performClick()
        assertEquals(1, requested)
    }
}
