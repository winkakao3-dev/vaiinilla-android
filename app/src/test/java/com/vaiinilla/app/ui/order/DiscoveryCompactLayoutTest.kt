package com.vaiinilla.app.ui.order

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import com.vaiinilla.app.domain.model.GuestVenueContext
import com.vaiinilla.app.domain.model.LastOrderedPlace
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
import java.time.Instant

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
    fun `al abrir no se listan los lugares y el QR y el codigo van arriba`() {
        setDiscovery(
            DiscoveryUiState(
                establishments = listOf(active, america, south),
                selected = GuestVenueContext(establishment = active, space = null),
            ),
        )

        composeTestRule.onNodeWithText("Dónde pedir").assertIsDisplayed()
        composeTestRule.onNodeWithText("¿Ya estás en el lugar?").assertIsDisplayed()
        composeTestRule.onNodeWithTag("guest-scan-qr").assertIsDisplayed()
        composeTestRule.onNodeWithTag("guest-table-code").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("America").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Cafetería Sur").assertCountEquals(0)
        // El lugar activo es el camino de vuelta cuando no hay «Pediste aquí».
        composeTestRule.onNodeWithTag("guest-active-place").performScrollTo().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Acercar al NFC").assertCountEquals(0)
    }

    @Test
    fun `pediste aqui ofrece pedir otra vez en el ultimo lugar`() {
        var selected: PublicEstablishment? = null
        setDiscovery(
            DiscoveryUiState(
                lastPlace =
                    LastOrderedPlace(
                        establishment = america,
                        firstItemName = "Pastel Michi",
                        otherItemsCount = 2,
                        orderedAt = Instant.now(),
                    ),
            ),
            onSelect = { selected = it },
        )

        composeTestRule.onNodeWithText("Pediste aquí").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hoy · Pastel Michi y 2 más", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("guest-last-place").performClick()
        assertEquals(america, selected)
        // Con un lugar conocido ya no es la primera visita.
        composeTestRule.onAllNodesWithText("¿No ves un QR?", substring = true).assertCountEquals(0)
    }

    @Test
    fun `la primera vez explica que hacer sin QR y el codigo abre la hoja`() {
        setDiscovery(DiscoveryUiState(establishments = emptyList()))

        composeTestRule.onNodeWithText("¿No ves un QR?", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("guest-table-code").performClick()
        composeTestRule.onNodeWithText("Escribe los 4 dígitos que aparecen junto al QR de tu mesa.").assertIsDisplayed()
    }

    @Test
    fun `las tarjetas de entrada se leen con texto grande`() {
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 1.35f),
            ) {
                ScreenshotTheme {
                    DiscoveryScreen(
                        state = DiscoveryUiState(establishments = listOf(active, america)),
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

        composeTestRule.onNodeWithTag("guest-scan-qr").assertIsDisplayed()
        composeTestRule.onNodeWithTag("guest-table-code").assertIsDisplayed()
    }

    @Test
    fun `al buscar salen los lugares con su conteo`() {
        setDiscovery(DiscoveryUiState(query = "caf", establishments = listOf(south)))

        composeTestRule.onNodeWithText("1 lugar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("establishment-cafeteria-sur").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("¿Ya estás en el lugar?").assertCountEquals(0)
    }

    @Test
    fun `una busqueda sin resultados lo dice y deja borrarla`() {
        var query = "zzz"
        setDiscovery(DiscoveryUiState(establishments = emptyList(), query = query), onQuery = { query = it })

        composeTestRule.onNodeWithText("No encontramos «zzz».").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Borrar búsqueda").performClick()
        assertEquals("", query)
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
        composeTestRule.onAllNodesWithText(SEARCH_FAILED_MESSAGE).assertCountEquals(1)
        composeTestRule.onAllNodesWithText("No encontramos «PADEL».").assertCountEquals(0)
        composeTestRule.onNodeWithText("Reintentar").performClick()
        assertEquals(1, retries)
    }

    private fun setDiscovery(
        state: DiscoveryUiState,
        onQuery: (String) -> Unit = {},
        onSelect: (PublicEstablishment) -> Unit = {},
    ) {
        composeTestRule.setContent {
            ScreenshotTheme {
                DiscoveryScreen(
                    state = state,
                    onQueryChange = onQuery,
                    onSpaceTokenChange = {},
                    onSelectEstablishment = onSelect,
                    onResolveSpace = {},
                    onContinueSelected = {},
                    profileInitials = "DR",
                )
            }
        }
        composeTestRule.waitForIdle()
    }
}
