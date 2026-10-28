package com.vaiinilla.app.ui.reservations

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** D08: si el lugar pide cuenta, al invitado no se le ofrece la renta sin cuenta. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi", sdk = [33])
class ReservationsAccountRequiredUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val taps = mutableListOf<String>()

    private fun show(state: ReservationsUiState) {
        composeTestRule.setContent {
            ScreenshotTheme {
                ReservationsScreen(
                    state = state,
                    venueName = "Pádel",
                    onBack = {},
                    onSelectDate = {},
                    onSelectCourt = {},
                    onRentNow = {},
                    onSelectStart = {},
                    onSelectDuration = {},
                    onReserve = {},
                    onPay = {},
                    onDismissPayment = {},
                    onResumePayment = {},
                    onCancel = {},
                    onDismissMessage = {},
                    onSignIn = { taps += "login" },
                    onCreateAccount = { taps += "registro" },
                )
            }
        }
    }

    @Test
    fun `con requiere_cuenta se pide cuenta y no sale la renta sin cuenta`() {
        show(ReservationsUiState(guestMode = true, guestEntry = true, accountRequired = true))

        composeTestRule.onNodeWithText("Necesitas una cuenta").assertExists()
        composeTestRule.onNodeWithText("Renta sin cuenta").assertDoesNotExist()
        composeTestRule.onNodeWithText("Crear cuenta").performClick()
        composeTestRule.onNodeWithText("Ya tengo cuenta, iniciar sesión").performClick()
        assertEquals(listOf("registro", "login"), taps)
    }

    @Test
    fun `sin requiere_cuenta el invitado sigue viendo la renta sin cuenta`() {
        show(ReservationsUiState(guestMode = true, guestEntry = true))

        composeTestRule.onNodeWithText("Renta sin cuenta").assertExists()
        composeTestRule.onNodeWithText("Necesitas una cuenta").assertDoesNotExist()
    }
}
