package com.vaiinilla.app.ui.order

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import com.vaiinilla.app.domain.model.GuestVenueContext
import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.ui.discovery.DiscoveryUiState
import com.vaiinilla.app.ui.discovery.SEARCH_FAILED_MESSAGE
import com.vaiinilla.app.ui.screens.DiscoveryScreen
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertEquals
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
    fun `discovery lists every venue as a row and keeps QR and code reachable`() {
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
                    onContinueSelected = {},
                    profileInitials = "DR",
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Dónde pedir").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("saulP1").assertCountEquals(1)
        composeTestRule.onNodeWithText("Aquí estás · Acceso libre").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("America").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Cafetería Sur").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Escanear QR").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Usar código").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Abiertas ahora").assertCountEquals(0)
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
                        onContinueSelected = {},
                        profileInitials = "DR",
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText("¿Ya estás en tu mesa?").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Escanear QR").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Usar código").assertCountEquals(1)
    }

    @Test
    fun `sin negocios en la lista salen las tarjetas de QR, codigo y NFC sin el panel viejo`() {
        setEmptyDiscovery(nfcAvailable = true)

        composeTestRule.onAllNodesWithText("Escanea el QR de tu mesa o de la tienda").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Sin coincidencias").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("¿Ya estás en tu mesa?").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Escanear QR").assertCountEquals(1)
        composeTestRule.onNodeWithText("De tu mesa o tu escuela").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Los 4 dígitos de tu mesa").assertCountEquals(0)
        composeTestRule.onNodeWithText("Acercar al NFC").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Si tu mesa tiene etiqueta").assertIsDisplayed()
        composeTestRule.onNodeWithText("Acercar al NFC").performClick()
        composeTestRule
            .onNodeWithText("Acerca la parte de atrás de tu teléfono a la etiqueta: Vaiinilla abre tu mesa sola.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Usar código").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Escribe los 4 dígitos que aparecen junto al QR de tu mesa.").assertIsDisplayed()
    }

    @Test
    fun `sin NFC en el telefono no sale la tarjeta Acercar al NFC`() {
        setEmptyDiscovery(nfcAvailable = false)

        composeTestRule.onNodeWithText("Usar código").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Acercar al NFC").assertCountEquals(0)
    }

    @Test
    fun `una busqueda sin resultados avisa y deja limpiarla`() {
        var query = "zzz"
        composeTestRule.setContent {
            ScreenshotTheme {
                DiscoveryScreen(
                    state = DiscoveryUiState(establishments = emptyList(), query = query),
                    onQueryChange = { query = it },
                    onSpaceTokenChange = {},
                    onSelectEstablishment = {},
                    onResolveSpace = {},
                    onContinueSelected = {},
                    profileInitials = "DR",
                    nfcAvailable = true,
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Sin coincidencias").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Escanear QR").assertCountEquals(1)
        composeTestRule.onNodeWithText("Limpiar búsqueda").performClick()
        assertEquals("", query)
    }

    private fun setEmptyDiscovery(nfcAvailable: Boolean) {
        composeTestRule.setContent {
            ScreenshotTheme {
                DiscoveryScreen(
                    state = DiscoveryUiState(establishments = emptyList()),
                    onQueryChange = {},
                    onSpaceTokenChange = {},
                    onSelectEstablishment = {},
                    onResolveSpace = {},
                    onContinueSelected = {},
                    profileInitials = "DR",
                    nfcAvailable = nfcAvailable,
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `search failure shows only the error with retry`() {
        var retries = 0
        composeTestRule.setContent {
            ScreenshotTheme {
                DiscoveryScreen(
                    state =
                        DiscoveryUiState(
                            query = "PADEL",
                            searchFailed = true,
                            errorMessage = SEARCH_FAILED_MESSAGE,
                        ),
                    onQueryChange = {},
                    onSpaceTokenChange = {},
                    onSelectEstablishment = {},
                    onResolveSpace = {},
                    onContinueSelected = {},
                    onRetrySearch = { retries++ },
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(SEARCH_FAILED_MESSAGE).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Sin coincidencias").assertCountEquals(0)
        composeTestRule.onNodeWithText("Reintentar").performClick()
        assertEquals(1, retries)
    }
}
