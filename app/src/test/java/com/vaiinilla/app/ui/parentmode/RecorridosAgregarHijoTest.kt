package com.vaiinilla.app.ui.parentmode

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.vaiinilla.app.domain.guardianship.Dependent
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private class AddingGuardianship : FakeGuardianship() {
    data class Call(
        val name: String,
        val studentId: String,
        val group: String?,
        val consentVersion: String,
    )

    val added = mutableListOf<Call>()

    override fun addDependent(
        name: String,
        studentId: String,
        group: String?,
        consentVersion: String,
        idempotencyKey: String,
    ): Result<Dependent> {
        added += Call(name, studentId, group, consentVersion)
        return Result.success(ParentFixtures.tilin)
    }
}

/**
 * Agregar a mi hijo en 3 pasos (W06): pide quién es, exige aceptar el aviso antes de enviar y manda sus datos con
 * la versión del aviso. Corre en la JVM con Robolectric y Compose; no cuenta como recorrido en teléfono.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class RecorridosAgregarHijoTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun show(repo: AddingGuardianship) {
        composeTestRule.setContent {
            ScreenshotTheme {
                AddChildFlow(repo, onAdded = {}, onDismiss = {}, startsWithSchoolCode = false, schoolName = "Colegio Vaini")
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun fill() {
        composeTestRule.onNodeWithText("Nombre completo").performTextInput("Sofía Ramírez")
        composeTestRule.onNodeWithText("Matrícula").performTextInput("A100")
        composeTestRule.onNodeWithText("Grupo").performTextInput("3°B")
    }

    @Test
    fun w06_sinCodigoDeEscuelaPideDeUnaVezQuienEsTuHijo() {
        show(AddingGuardianship())
        composeTestRule.onNodeWithText("¿Quién es tu hijo?").assertIsDisplayed()
        composeTestRule.onNodeWithTag("add-child-submit").assertIsNotEnabled()
    }

    @Test
    fun w06_sinAceptarElAvisoNoSePuedeEnviarAunqueEsteTodoLleno() {
        val repo = AddingGuardianship()
        show(repo)
        fill()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("add-child-submit").assertIsNotEnabled()
        assertEquals(emptyList<AddingGuardianship.Call>(), repo.added)
    }

    @Test
    fun w06_conElAvisoAceptadoSeEnviaConLaVersionDelAviso() {
        val repo = AddingGuardianship()
        show(repo)
        fill()
        composeTestRule.onNodeWithTag("add-child-consent").performTouchInput { click(Offset(100f, centerY)) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("add-child-submit").assertIsEnabled().performClick()
        composeTestRule.waitUntil(5_000) { repo.added.isNotEmpty() }
        val call = repo.added.single()
        assertEquals("Sofía Ramírez", call.name)
        assertEquals("A100", call.studentId)
        assertEquals("3°B", call.group)
        assertEquals("v1", call.consentVersion)
    }
}
