package com.vaiinilla.app.ui.parentmode

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaiinilla.app.domain.guardianship.Dependent
import com.vaiinilla.app.domain.guardianship.PrivacyNotice
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private class RecordingGuardianship(
    children: List<Dependent>,
    private val notice: PrivacyNotice,
) : FakeGuardianship(children) {
    val accepted = mutableListOf<String>()

    override fun privacyNotice() = Result.success(notice)

    override fun acceptPrivacyNotice(
        version: String,
        idempotencyKey: String,
    ): Result<List<Dependent>> {
        accepted += version
        return super.acceptPrivacyNotice(version, idempotencyKey)
    }
}

/**
 * Aviso de privacidad del tutor (F16): se lee entero, solo se acepta con la casilla marcada y se guarda con la
 * versión que mostró. Corre en la JVM con Robolectric y Compose; no cuenta como recorrido en teléfono.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class RecorridosAvisoPrivacidadTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun open(guardianship: RecordingGuardianship): ChildrenState {
        val children = ChildrenState(ParentFixtures.services(guardianship))
        runBlocking { children.load() }
        composeTestRule.setContent {
            ScreenshotTheme { PrivacyConsentSheet(children = children, onDismiss = {}) }
        }
        composeTestRule.waitForIdle()
        return children
    }

    private val notice = PrivacyNotice("2026-10-21", false, "Aviso de privacidad de Vaiinilla", listOf("Usamos los datos de tu hijo solo para vender."))

    @Test
    fun f16_elAvisoMuestraSuTextoYNombraALosHijosQueFaltan() {
        open(RecordingGuardianship(listOf(ParentFixtures.ana), notice))
        composeTestRule.onNodeWithText("Falta tu permiso para Ana").assertIsDisplayed()
        composeTestRule.onNodeWithText("Aviso de privacidad de Vaiinilla").assertIsDisplayed()
        composeTestRule.onNodeWithText("Usamos los datos de tu hijo solo para vender.").assertIsDisplayed()
    }

    @Test
    fun f16_sinMarcarLaCasillaNoSePuedeAceptar() {
        val repo = RecordingGuardianship(listOf(ParentFixtures.ana), notice)
        open(repo)
        composeTestRule.onNodeWithTag("pending-consent-accept").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("pending-consent-check").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("pending-consent-accept").assertIsEnabled()
        assertEquals(emptyList<String>(), repo.accepted)
    }

    @Test
    fun f16_aceptarGuardaElPermisoConLaVersionQueSeLeyoYQuitaLaTarjeta() {
        val repo = RecordingGuardianship(listOf(ParentFixtures.ana), notice)
        val children = open(repo)
        composeTestRule.onNodeWithTag("pending-consent-check").performClick()
        composeTestRule.onNodeWithTag("pending-consent-accept").performClick()
        composeTestRule.waitUntil(5_000) { repo.accepted.isNotEmpty() }
        assertEquals(listOf("2026-10-21"), repo.accepted)
        composeTestRule.waitUntil(5_000) { children.childrenNeedingConsent.isEmpty() }
    }

    @Test
    fun f16_unBorradorDiceSuVersion() {
        open(RecordingGuardianship(listOf(ParentFixtures.ana), notice.copy(isDraft = true)))
        composeTestRule.onNodeWithText("Versión 2026-10-21").assertIsDisplayed()
    }
}
