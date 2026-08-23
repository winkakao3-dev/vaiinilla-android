package com.vaiinilla.app.ui.order

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaiinilla.app.domain.model.GuestVenueContext
import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.ui.discovery.DiscoveryUiState
import com.vaiinilla.app.ui.screens.DiscoveryScreen
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
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
class DiscoveryCompactLayoutTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val active =
        PublicEstablishment(
            id = "1",
            name = "saulP1",
            slug = "saulp1",
            clientIdLabel = "Matrícula",
            clientIdRequired = false,
        )
    private val america =
        PublicEstablishment(
            id = "2",
            name = "America",
            slug = "america",
            clientIdLabel = "Matrícula",
            clientIdRequired = true,
        )
    private val south =
        PublicEstablishment(
            id = "3",
            name = "Cafetería Sur",
            slug = "cafeteria-sur",
            clientIdLabel = "Matrícula",
            clientIdRequired = true,
        )

    @Test
    fun `discovery keeps full list collapsed until explicitly requested`() {
        composeTestRule.setContent {
            ScreenshotTheme {
                DiscoveryScreen(
                    state =
                        DiscoveryUiState(
                            establishments = listOf(active, america, south),
                            selected = GuestVenueContext(establishment = active, space = null),
                        ),
                    onQueryChange = {},
                    onSpaceTokenChange = {},
                    onSelectEstablishment = {},
                    onResolveSpace = {},
                    onConfirmSwitch = {},
                    onDismissSwitch = {},
                    onContinueSelected = {},
                    profileInitials = "DR",
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Seguir al menú").assertIsDisplayed()
        composeTestRule.onNodeWithText("Buscar otra cafetería").assertIsDisplayed()
        composeTestRule.onNodeWithText("Escanear QR").assertIsDisplayed()
        composeTestRule.onNodeWithText("Usar código").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ver todas las cafeterías").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Cafetería Sur").assertCountEquals(0)

        composeTestRule.onNodeWithContentDescription("Ver todas las cafeterías").performClick()
        composeTestRule.onNodeWithText("Ocultar cafeterías").assertIsDisplayed()
    }
}
