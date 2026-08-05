package com.vaiinilla.app.ui.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.domain.model.GuestVenueContext
import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.domain.model.PublicSpace
import com.vaiinilla.app.ui.discovery.DiscoveryUiState
import com.vaiinilla.app.ui.screens.CatalogScreen
import com.vaiinilla.app.ui.screens.DiscoveryScreen
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
class DiscoveryScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val centro =
        PublicEstablishment(
            id = "8246ff44-aad0-4e49-9268-b71c997893fe",
            name = "Cafetería Centro",
            slug = "cafeteria-centro",
            clientIdLabel = "Matrícula",
            clientIdRequired = true,
        )

    private val norte =
        PublicEstablishment(
            id = "a1111111-0000-4000-8000-0000000000b1",
            name = "Cafetería Norte",
            slug = "cafeteria-norte",
            clientIdLabel = "Número de empleado",
            clientIdRequired = false,
        )

    @Test
    fun `vai25_discovery_list`() {
        composeTestRule.setContent {
            ScreenshotTheme {
                DiscoveryScreen(
                    state =
                        DiscoveryUiState(
                            establishments = listOf(centro, norte),
                        ),
                    onQueryChange = {},
                    onSpaceTokenChange = {},
                    onSelectEstablishment = {},
                    onResolveSpace = {},
                    onConfirmSwitch = {},
                    onDismissSwitch = {},
                    onContinueSelected = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("vai25_discovery_list.png")
    }

    @Test
    fun `vai25_discovery_active_mesa4`() {
        composeTestRule.setContent {
            ScreenshotTheme {
                DiscoveryScreen(
                    state =
                        DiscoveryUiState(
                            establishments = listOf(centro, norte),
                            selected =
                                GuestVenueContext(
                                    establishment = centro,
                                    space = PublicSpace(id = 12, name = "Mesa 4", type = "mesa"),
                                ),
                            spaceTokenInput = "mesa4",
                        ),
                    onQueryChange = {},
                    onSpaceTokenChange = {},
                    onSelectEstablishment = {},
                    onResolveSpace = {},
                    onConfirmSwitch = {},
                    onDismissSwitch = {},
                    onContinueSelected = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("vai25_discovery_active_mesa4.png")
    }

    @Test
    fun `vai25_catalog_guest_venue`() {
        val state =
            ScreenshotFixtures.catalogLoadedState().copy(
                guestVenue =
                    GuestVenueContext(
                        establishment = centro,
                        space = PublicSpace(id = 12, name = "Mesa 4", type = "mesa"),
                    ),
            )
        composeTestRule.setContent {
            ScreenshotTheme {
                CatalogScreen(
                    state = state,
                    onRetry = {},
                    onSearchChange = {},
                    onCategorySelected = {},
                    onProductSelected = {},
                    onDismissProduct = {},
                    onToggleOption = { _, _ -> },
                    onClearOptionalGroup = {},
                    onQuantityChange = {},
                    onAddProduct = {},
                    onOpenCart = {},
                    onChangeVenue = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("vai25_catalog_guest_venue.png")
    }
}
