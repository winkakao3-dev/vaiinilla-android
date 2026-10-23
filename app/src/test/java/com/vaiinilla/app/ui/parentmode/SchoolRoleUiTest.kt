package com.vaiinilla.app.ui.parentmode

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Cartera de una escuela: cada quien ve solo lo suyo, y nada dice "papá". */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class SchoolRoleUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `quien es sale de sus hijos, sus tutores o de elegir`() {
        assertEquals(
            WalletSchoolEntry.PARENT,
            schoolEntryFor(hasChildren = true, hasTutors = true, childrenLoaded = true),
        )
        assertEquals(
            WalletSchoolEntry.STUDENT,
            schoolEntryFor(hasChildren = false, hasTutors = true, childrenLoaded = false),
        )
        assertEquals(
            WalletSchoolEntry.UNKNOWN,
            schoolEntryFor(hasChildren = false, hasTutors = false, childrenLoaded = true),
        )
        // Sin la lista de hijos todavía, las tarjetas no salen para luego irse.
        assertEquals(
            WalletSchoolEntry.LOADING,
            schoolEntryFor(hasChildren = false, hasTutors = false, childrenLoaded = false),
        )
    }

    @Test
    fun `quien no tiene hijos ni tutor elige entre Soy tutor y Soy alumno`() {
        val roles = mutableListOf<SchoolRole>()
        composeTestRule.setContent {
            ScreenshotTheme {
                WalletFamilySection(
                    children = ChildrenState(ParentFixtures.services(FakeGuardianship())),
                    onOpen = {},
                    onAdd = {},
                    onConsent = {},
                    entry = WalletSchoolEntry.UNKNOWN,
                    onRole = { roles += it },
                    onLinkCode = {},
                )
            }
        }

        composeTestRule.onNodeWithText("¿Eres tutor o alumno?").assertIsDisplayed()
        composeTestRule.onNodeWithTag("wallet-school-parent").performClick()
        composeTestRule.onNodeWithTag("wallet-school-student").performClick()
        assertEquals(listOf(SchoolRole.PARENT, SchoolRole.STUDENT), roles)
    }

    @Test
    fun `el alumno con tutor ve su codigo para otro tutor y no las tarjetas`() {
        var linkCode = 0
        composeTestRule.setContent {
            ScreenshotTheme {
                WalletFamilySection(
                    children = ChildrenState(ParentFixtures.services(FakeGuardianship())),
                    onOpen = {},
                    onAdd = {},
                    onConsent = {},
                    entry = WalletSchoolEntry.STUDENT,
                    onRole = {},
                    onLinkCode = { linkCode++ },
                )
            }
        }

        composeTestRule.onNodeWithText("Cómo funciona").assertIsDisplayed()
        composeTestRule.onNodeWithText("¿Eres tutor o alumno?").assertDoesNotExist()
        composeTestRule.onNodeWithTag("wallet-school-link-code").performClick()
        assertEquals(1, linkCode)
    }

    @Test
    fun `Alumnos trae solo lo del alumno y su guia`() {
        val children = ChildrenState(ParentFixtures.services(FakeGuardianship()))
        runBlocking { children.load() }
        composeTestRule.setContent {
            ScreenshotTheme {
                SchoolOptionsPage(
                    children = children,
                    role = SchoolRole.STUDENT,
                    hasTutors = false,
                    onAccountClaimed = {},
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Alumnos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Que mi tutor me agregue").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mi tutor ya me dio de alta").assertIsDisplayed()
        composeTestRule.onNodeWithTag("school-options-add").assertDoesNotExist()
        composeTestRule.onNodeWithTag("school-options-unlock").assertDoesNotExist()
    }

    @Test
    fun `el alumno que ya tiene tutor no reclama otra cuenta y su guia habla de otro tutor`() {
        val children = ChildrenState(ParentFixtures.services(FakeGuardianship()))
        runBlocking { children.load() }
        composeTestRule.setContent {
            ScreenshotTheme {
                SchoolOptionsPage(
                    children = children,
                    role = SchoolRole.STUDENT,
                    hasTutors = true,
                    onAccountClaimed = {},
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Que otro tutor me agregue").assertIsDisplayed()
        composeTestRule.onNodeWithTag("school-options-claim").assertDoesNotExist()
        assertTrue(schoolGuideSteps(SchoolRole.STUDENT, hasTutors = true).first().contains("Que otro tutor me agregue"))
    }

    @Test
    fun `ninguna guia dice papa`() {
        val all =
            schoolGuideSteps(SchoolRole.PARENT, false) +
                schoolGuideSteps(SchoolRole.STUDENT, false) +
                schoolGuideSteps(SchoolRole.STUDENT, true)
        assertTrue(all.none { it.contains("papá", ignoreCase = true) })
    }
}
