package com.vaiinilla.app.ui.assistant

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Abrir el asistente como invitado (Y04): avisa que una IA de otra empresa procesa los mensajes, pide el nombre
 * para que el mesero lo ubique y después deja platicar. Corre en la JVM con Robolectric y Compose; no cuenta como
 * recorrido en teléfono.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class RecorridosAsistenteInvitadoTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val noActions =
        CustomerAssistantActions(
            addProduct = { false },
            addOrder = { _, _ -> emptyList() },
            openCart = {},
            openProduct = {},
            openCourt = {},
            callWaiter = null,
        )

    private fun show(
        state: CustomerAssistantState,
        onSend: (String) -> Unit = {},
        onSubmitName: (String) -> Unit = {},
    ) {
        composeTestRule.setContent {
            ScreenshotTheme {
                CustomerAssistantScreen(
                    state = state,
                    venueName = "USAGI",
                    place = "Mesa 2",
                    customerName = null,
                    actions = noActions,
                    onSend = onSend,
                    onRetry = {},
                    onRestart = {},
                    onTurnDone = {},
                    onOrderInCart = { _, _ -> },
                    onSubmitName = onSubmitName,
                    onBack = {},
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun y04_alAbrirAvisaQueUnaIADeOtraEmpresaProcesaLosMensajes() {
        show(CustomerAssistantState())
        composeTestRule.onNodeWithText("¿Qué se te antoja?").assertIsDisplayed()
        composeTestRule
            .onNode(hasText("Tus mensajes los procesa una IA de otra empresa", substring = true))
            .assertIsDisplayed()
    }

    @Test
    fun y04_elInvitadoSinNombreVeLaTarjetaParaDecirComoSeLlama() {
        show(CustomerAssistantState(needsName = true))
        composeTestRule.onNodeWithText("¿Cómo te llamas?").assertExists()
        composeTestRule
            .onNode(hasText("No se lo mandamos al asistente", substring = true))
            .assertExists()
    }

    @Test
    fun y04_sinNombreNoSeOfrecenLasRespuestasRapidas() {
        show(CustomerAssistantState(needsName = true, options = listOf("Quiero algo dulce")))
        composeTestRule.onNodeWithText("Quiero algo dulce").assertDoesNotExist()
    }

    @Test
    fun y04_conNombreYaSePuedeElegirUnaSugerenciaYSeEnvia() {
        val sent = mutableListOf<String>()
        show(CustomerAssistantState(), onSend = { sent += it })
        composeTestRule.onNodeWithText("¿Qué se te antoja?").assertIsDisplayed()
        assertTrue(sent.isEmpty())
        composeTestRule.onNodeWithText("Sin alcohol").performClick()
        composeTestRule.waitForIdle()
        assertEquals(listOf("Sin alcohol"), sent)
    }
}
