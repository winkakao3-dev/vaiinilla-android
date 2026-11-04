package com.vaiinilla.app.ui.parentmode

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.vaiinilla.app.domain.guardianship.Dependent
import com.vaiinilla.app.domain.guardianship.GuardianshipState
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** El paso 3 de agregar a un hijo: recargarle si la escuela ya lo deja ver, o esperar a que apruebe. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi", sdk = [33])
class AddedChildStepUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private class AddingGuardianship(
        private val result: Dependent,
    ) : FakeGuardianship() {
        override fun addDependent(
            name: String,
            studentId: String,
            group: String?,
            consentVersion: String,
            idempotencyKey: String,
        ): Result<Dependent> = Result.success(result)
    }

    private fun addChild(
        result: Dependent,
        onOpenChild: (Dependent, Int?) -> Unit = { _, _ -> },
        onDismiss: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            ScreenshotTheme {
                AddChildFlow(
                    AddingGuardianship(result),
                    onAdded = {},
                    onDismiss = onDismiss,
                    onOpenChild = onOpenChild,
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Nombre completo").performTextInput(result.name)
        composeTestRule.onNodeWithText("Matrícula").performTextInput("A100")
        composeTestRule.onNodeWithTag("add-child-consent").performTouchInput { click(Offset(100f, centerY)) }
        composeTestRule.onNodeWithTag("add-child-submit").performClick()
    }

    private fun waitForTag(tag: String) =
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }

    @Test
    fun `aprobado ofrece la primera recarga con el monto elegido y abre su ficha en recargar`() {
        val opened = mutableListOf<Pair<String, Int?>>()
        val child = ParentFixtures.tilin.copy(schoolState = GuardianshipState.APPROVED, balance = java.math.BigDecimal.ZERO)
        addChild(child, onOpenChild = { c, amount -> opened += c.userId to amount })
        waitForTag("added-child-reload")

        composeTestRule.onNodeWithText("${child.firstName} ya está contigo").assertIsDisplayed()
        composeTestRule.onNodeWithText("¿Le recargas para que coma?").assertIsDisplayed()
        composeTestRule.onNodeWithTag("added-child-reload").performClick()
        composeTestRule.onNodeWithTag("added-child-amount-300").performClick()
        composeTestRule.onNodeWithTag("added-child-reload").performClick()
        composeTestRule.onNodeWithTag("added-child-open").performClick()

        assertEquals(listOf(child.userId to 200, child.userId to 300, child.userId to null), opened)
    }

    @Test
    fun `si la escuela revisa a los tutores dice que espera y solo deja cerrar`() {
        var dismissed = 0
        val child = ParentFixtures.tilin
        addChild(child, onDismiss = { dismissed++ })
        waitForTag("added-child-done")

        composeTestRule.onNodeWithText("Le avisamos a la escuela").assertIsDisplayed()
        composeTestRule.onNodeWithText("Esperando a que la escuela te apruebe").assertExists()
        composeTestRule.onNodeWithTag("added-child-reload").assertDoesNotExist()
        composeTestRule.onNodeWithTag("added-child-done").performClick()

        assertEquals(1, dismissed)
    }
}