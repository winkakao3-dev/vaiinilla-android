package com.vaiinilla.app.ui.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import com.vaiinilla.app.domain.mode.AuthorizedInvitation
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.ui.mode.AuthorizedAccessUiState
import com.vaiinilla.app.ui.screens.InvitationAcceptanceScreen
import com.vaiinilla.app.ui.screens.formatInvitationExpiry
import com.vaiinilla.app.ui.screens.invitationHeadline
import com.vaiinilla.app.ui.screens.invitationMatchesSession
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
import java.time.Instant

/** La invitación de personal como el L14 de Doop. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class StaffInvitationScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val invitation =
        AuthorizedInvitation(
            token = "t",
            establishmentName = "Pastelería Vaini",
            invitedEmail = "lupita.r@correo.mx",
            role = OperationalRole.CASHIER,
            expiresAt = Instant.parse("2026-11-07T00:00:00Z"),
        )

    @Test
    fun `dice quién invita y a qué rol`() {
        assertEquals("Pastelería Vaini te invita" to "Trabaja en Caja", invitationHeadline(invitation))
        assertEquals("Administra el negocio", invitationHeadline(invitation.copy(role = OperationalRole.ADMIN)).second)
    }

    @Test
    fun `compara el correo invitado con el de la sesión sin importar mayúsculas`() {
        assertEquals(true, invitationMatchesSession(invitation, "Lupita.R@correo.mx"))
        assertEquals(false, invitationMatchesSession(invitation, "otra@correo.mx"))
        assertNull(invitationMatchesSession(invitation, null))
    }

    @Test
    fun `la vigencia dice el dia como el dibujo`() {
        assertEquals("viernes 6 de nov, 18:00", formatInvitationExpiry(invitation.expiresAt!!))
    }

    @Test
    fun invitacion_personal() {
        composeTestRule.setContent {
            VaiinillaTheme(themeMode = VaiinillaThemeMode.Dark) {
                InvitationAcceptanceScreen(
                    state =
                        AuthorizedAccessUiState(
                            invitation = invitation,
                            session = StudentAuthSession("u", "lupita.r@correo.mx", "Lupita", emailVerified = true),
                        ),
                    onBack = {},
                    onAccept = {},
                    onAuthenticate = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Trabaja en Caja").assertExists()
        composeTestRule.onNodeWithText("Ahora no").assertExists()
        composeTestRule.onRoot().captureRoboImage("invitacion_personal.png")
    }
}