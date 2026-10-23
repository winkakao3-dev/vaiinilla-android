package com.vaiinilla.app.ui.discovery

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.mode.MySchool
import com.vaiinilla.app.domain.mode.MySchools
import com.vaiinilla.app.domain.model.GuestVenueContext
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.ui.screens.DiscoveryScreen
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** "Tus escuelas" arriba de Dónde pedir. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class MySchoolsRowTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun mode(
        role: OperationalRole,
        id: String,
        name: String,
        isSchool: Boolean,
        active: Boolean = true,
    ) = AuthorizedMode(
        role = role,
        establishmentId = id,
        establishmentName = name,
        membershipId = "m-$id-${role.name}",
        establishmentSlug = name.lowercase().replace(' ', '-'),
        isSchool = isSchool,
        establishmentActive = active,
    )

    @Test
    fun onlyClientSchoolsThatAreOpenOnceEach() {
        val schools =
            MySchools.from(
                listOf(
                    mode(OperationalRole.CLIENT, "e1", "Sandbox", isSchool = true),
                    mode(OperationalRole.CASHIER, "e1", "Sandbox", isSchool = true),
                    mode(OperationalRole.CLIENT, "e2", "Cafeteria", isSchool = false),
                    mode(OperationalRole.CASHIER, "e3", "Prepa Norte", isSchool = true),
                    mode(OperationalRole.CLIENT, "e4", "Cerrada", isSchool = true, active = false),
                    mode(OperationalRole.CLIENT, "e5", "Prepa Sur", isSchool = true),
                ),
            )
        assertEquals(listOf("e1", "e5"), schools.map { it.id })
        assertEquals(listOf("e5"), MySchools.others(schools, currentEstablishmentId = "e1").map { it.id })
    }

    @Test
    fun theRowShowsTheOtherSchoolsAndOpensOneWithATap() {
        val current =
            PublicEstablishment(
                id = "e1",
                name = "Sandbox",
                slug = "sandbox",
                clientIdLabel = "Matrícula",
                clientIdRequired = false,
            )
        val opened = mutableListOf<String>()
        composeTestRule.setContent {
            ScreenshotTheme {
                DiscoveryScreen(
                    state =
                        DiscoveryUiState(
                            establishments = listOf(current),
                            selected = GuestVenueContext(establishment = current, space = null),
                            mySchools =
                                listOf(
                                    MySchool("e1", "Sandbox", "sandbox"),
                                    MySchool("e5", "Prepa Sur", "prepa-sur"),
                                ),
                        ),
                    onQueryChange = {},
                    onSpaceTokenChange = {},
                    onSelectEstablishment = {},
                    onResolveSpace = {},
                    onContinueSelected = {},
                    onOpenSchool = { opened += it.slug },
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Tus escuelas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("guest-school-prepa-sur").assertIsDisplayed()
        // La escuela donde ya está no se repite en la fila.
        composeTestRule.onNodeWithTag("guest-school-sandbox").assertDoesNotExist()

        composeTestRule.onNodeWithTag("guest-school-prepa-sur").performClick()
        assertEquals(listOf("prepa-sur"), opened)
    }

    @Test
    fun withoutOtherSchoolsThereIsNoRow() {
        val current =
            PublicEstablishment(
                id = "e1",
                name = "Sandbox",
                slug = "sandbox",
                clientIdLabel = "Matrícula",
                clientIdRequired = false,
            )
        composeTestRule.setContent {
            ScreenshotTheme {
                DiscoveryScreen(
                    state =
                        DiscoveryUiState(
                            selected = GuestVenueContext(establishment = current, space = null),
                            mySchools = listOf(MySchool("e1", "Sandbox", "sandbox")),
                        ),
                    onQueryChange = {},
                    onSpaceTokenChange = {},
                    onSelectEstablishment = {},
                    onResolveSpace = {},
                    onContinueSelected = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("guest-my-schools").assertDoesNotExist()
    }
}
