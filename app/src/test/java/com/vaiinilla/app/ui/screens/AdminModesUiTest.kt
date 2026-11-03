package com.vaiinilla.app.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.mode.AuthorizedModeContext
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.ui.mode.AuthorizedAccessUiState
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** M02: la administración ve en Modos cada rol que le manda `GET /sesiones/accesos`, Mesero incluido. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class AdminModesUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `la administración ve Caja, Mesero y Cocina de su negocio`() {
        val modes =
            listOf(
                OperationalRole.CASHIER,
                OperationalRole.KITCHEN,
                OperationalRole.WAITER,
                OperationalRole.ADMIN,
            ).mapIndexed { index, role ->
                AuthorizedMode(role, establishmentId = "usagi", establishmentName = "USAGI", membershipId = "m$index")
            }
        composeTestRule.setContent {
            ScreenshotTheme {
                AuthorizedModeScreen(
                    state = AuthorizedAccessUiState(modes = modes, loading = false),
                    onBack = null,
                    onSelectMode = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Entrar como Mesero en USAGI").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Entrar como Caja en USAGI").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Entrar como Cocina en USAGI").assertIsDisplayed()
        // Un solo renglón por negocio, con sus roles adentro.
        assertEquals(1, composeTestRule.onAllNodesWithText("USAGI").fetchSemanticsNodes().size)
    }

    @Test
    fun `los modos se juntan por negocio y el negocio actual va primero`() {
        val modes =
            listOf(
                AuthorizedMode(OperationalRole.CASHIER, "a", "Autoservicio", "m1"),
                AuthorizedMode(OperationalRole.WAITER, "b", "Cafetería", "m2"),
                AuthorizedMode(OperationalRole.KITCHEN, "a", "Autoservicio", "m3"),
                AuthorizedMode(OperationalRole.CASHIER, "b", "Cafetería", "m4"),
            )
        val context = AuthorizedModeContext(OperationalRole.WAITER, "b", "Cafetería", "m2", accessToken = "t")

        val businesses = groupModesByBusiness(modes, context)

        assertEquals(listOf("b", "a"), businesses.map { it.establishmentId })
        assertEquals(listOf(OperationalRole.CASHIER, OperationalRole.WAITER), businesses.first().modes.map { it.role })
        assertEquals(listOf(OperationalRole.CASHIER, OperationalRole.KITCHEN), businesses.last().modes.map { it.role })
    }
}
