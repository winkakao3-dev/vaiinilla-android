package com.vaiinilla.app.ui.parentmode

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.vaiinilla.app.domain.guardianship.Dependent
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.math.BigDecimal

/** Los 3 pasos de agregar a un hijo en oscuro, como L13, E06, L15 y L16 de Doop. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class AddChildFlowScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private class SchoolGuardianship(
        private val result: Dependent = ParentFixtures.tilin,
    ) : FakeGuardianship() {
        override fun unlockParent(
            schoolCode: String,
            idempotencyKey: String,
        ): Result<Unit> = Result.success(Unit)

        override fun addDependent(
            name: String,
            studentId: String,
            group: String?,
            consentVersion: String,
            idempotencyKey: String,
        ): Result<Dependent> = Result.success(result)
    }

    private fun show(
        guardianship: FakeGuardianship,
        startsWithSchoolCode: Boolean,
    ) {
        composeTestRule.setContent {
            ScreenshotTheme(mode = VaiinillaThemeMode.Dark) {
                AddChildFlow(
                    guardianship,
                    onAdded = {},
                    onDismiss = {},
                    startsWithSchoolCode = startsWithSchoolCode,
                    schoolName = SCHOOL,
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun settle(ms: Long = 1_500) {
        composeTestRule.mainClock.advanceTimeBy(ms)
        composeTestRule.waitForIdle()
    }

    private fun fillAndSubmit(name: String) {
        composeTestRule.onNodeWithText("Nombre completo").performTextInput(name)
        composeTestRule.onNodeWithText("Matrícula").performTextInput("A100")
        composeTestRule.onNodeWithText("Grupo").performTextInput("3°B")
        composeTestRule.onNodeWithTag("add-child-consent").performTouchInput { click(Offset(100f, centerY)) }
        composeTestRule.onNodeWithTag("add-child-submit").performClick()
    }

    private fun waitForTag(tag: String) =
        composeTestRule.waitUntil(5_000) { composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun l13_codigo_de_la_escuela() {
        show(SchoolGuardianship(), startsWithSchoolCode = true)
        composeTestRule.onNodeWithTag("parent-unlock-field").performTextInput("SAND")
        settle()
        captureScreenRoboImage("lugar/l13_codigo_escuela.png")
    }

    @Test
    fun e06_quien_es_tu_hijo() {
        show(SchoolGuardianship(), startsWithSchoolCode = true)
        composeTestRule.onNodeWithTag("parent-unlock-field").performTextInput("SANDBX")
        composeTestRule.onNodeWithTag("parent-unlock-submit").performClick()
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("¿Quién es tu hijo?").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Nombre completo").performTextInput("Sofía Ramírez Ortega")
        composeTestRule.onNodeWithText("Matrícula").performTextInput("A100")
        composeTestRule.onNodeWithText("Grupo").performTextInput("3°B")
        composeTestRule.onNodeWithTag("add-child-consent").performTouchInput { click(Offset(100f, centerY)) }
        settle()
        captureScreenRoboImage("lugar/e06_quien_es_tu_hijo.png")
    }

    @Test
    fun l15_primera_recarga() {
        val child = ParentFixtures.tilin.copy(isAwaitingSchool = false, balance = BigDecimal.ZERO, group = "3°B")
        show(SchoolGuardianship(child), startsWithSchoolCode = false)
        fillAndSubmit(child.name)
        waitForTag("added-child-reload")
        settle()
        captureScreenRoboImage("lugar/l15_primera_recarga.png")
    }

    @Test
    fun l16_esperando_a_la_escuela() {
        val child = ParentFixtures.tilin.copy(isAwaitingSchool = true, group = "3°B", studentId = "A100")
        show(SchoolGuardianship(child), startsWithSchoolCode = false)
        fillAndSubmit(child.name)
        waitForTag("added-child-done")
        settle()
        captureScreenRoboImage("lugar/l16_esperando_escuela.png")
    }

    private companion object {
        const val SCHOOL = "Colegio Vaini"
    }
}