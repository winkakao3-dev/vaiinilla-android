package com.vaiinilla.app.ui.order

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
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
    fun `discovery shows recommended card and every other venue`() {
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

        composeTestRule.onNodeWithText("¿Dónde comes hoy?").assertIsDisplayed()
        composeTestRule.onNodeWithText("RECOMENDADA PARA TI").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("saulP1").assertCountEquals(2)
        composeTestRule.onAllNodesWithText("OTRAS SEDES DISPONIBLES").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("America").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Cafetería Sur").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Escanear QR").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Usar código").assertCountEquals(1)
        composeTestRule.onNodeWithText("CAFETERÍA ACTIVA").assertIsDisplayed()
    }

    @Test
    fun `quick access cards remain readable with larger text`() {
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 1.35f),
            ) {
                ScreenshotTheme {
                    DiscoveryScreen(
                        state =
                            DiscoveryUiState(
                                establishments = listOf(active, america),
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
        }
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText("ACCESO RÁPIDO EN MESA").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Escanear QR").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Usar código").assertCountEquals(1)
    }
}
