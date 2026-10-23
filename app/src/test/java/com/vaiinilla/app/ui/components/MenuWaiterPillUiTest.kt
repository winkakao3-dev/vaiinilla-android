package com.vaiinilla.app.ui.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaiinilla.app.domain.repository.CallReason
import com.vaiinilla.app.domain.repository.CallStatus
import com.vaiinilla.app.domain.repository.CallsUnavailableException
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.domain.repository.TableCallTaker
import com.vaiinilla.app.domain.repository.TableSpace
import com.vaiinilla.app.ui.screens.CatalogScreen
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** La píldora "Llamar al mesero" del menú y su ventana: motivos, estado en vivo, cancelar y cuándo no sale. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class MenuWaiterPillUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `abre los cuatro motivos y queda llamando`() {
        val sent = mutableListOf<Pair<CallReason, String?>>()
        setLayer(
            onCall = { _, reason, orderId ->
                sent += reason to orderId
                Result.success(call(CallStatus.PENDIENTE, reason = reason))
            },
        )
        // TalkBack la activa por su acción de toque, no por coordenadas.
        pill().assertContentDescriptionEquals("Llamar al mesero").assertHasClickAction().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("¿Qué necesitas?").assertIsDisplayed()
        CallReason.entries.forEach { composeTestRule.onNodeWithText(it.label).assertIsDisplayed() }
        composeTestRule.onNodeWithText("Pedir la cuenta").performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(CallReason.CUENTA to null), sent)
        pill().assertContentDescriptionEquals("Llamando al mesero…")
        composeTestRule.onNodeWithText("¿Qué necesitas?").assertDoesNotExist()
    }

    @Test
    fun `con el mesero en camino lo dice y todavia deja cancelar`() {
        setLayer(onCurrent = { Result.success(call(CallStatus.EN_CAMINO, taker = "Luis Pérez")) })
        pill().assertContentDescriptionEquals("Luis va en camino").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Luis Pérez va en camino").assertIsDisplayed()
        // K02: el servidor la cancela mientras siga abierta.
        composeTestRule.onNodeWithText("Cancelar llamada").assertIsDisplayed()
    }

    @Test
    fun `si el servidor falla al llamar se ve el error y la pildora sigue`() {
        setLayer(onCall = { _, _, _ -> Result.failure(CallsUnavailableException()) })
        pill().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Pedir la cuenta").performClick()
        composeTestRule.waitForIdle()

        // C04: antes la píldora se iba sin decir nada.
        composeTestRule
            .onNodeWithText("Llamar al mesero todavía no está disponible en esta cafetería.")
            .assertIsDisplayed()
        pill().assertExists()
    }

    @Test
    fun `cancelar desde la pildora cierra la llamada y arranca la espera`() {
        val cancelled = mutableListOf<String>()
        var current: TableCall? = call(CallStatus.PENDIENTE)
        setLayer(
            onCurrent = { Result.success(current) },
            onCancel = {
                cancelled += it.id
                current = null
                Result.success(it.copy(status = CallStatus.CANCELADA))
            },
        )
        pill().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Ya le avisamos a tu mesero").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mesa 1 · Cubiertos o servilletas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancelar llamada").performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf("call-1"), cancelled)
        pill().assertContentDescriptionEquals("Otra vez en 60 s")
    }

    @Test
    fun `junto a Pedir ya se encoge`() {
        setLayer(compact = true)
        composeTestRule.onNodeWithText("Mesero", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `el menu la pone sin ficha abierta y la esconde con un platillo abierto`() {
        composeTestRule.setContent {
            ScreenshotTheme {
                CatalogScreen(
                    state = ScreenshotFixtures.catalogLoadedState(),
                    onRetry = {},
                    onSearchChange = {},
                    onCategorySelected = {},
                    onProductSelected = {},
                    onDismissProduct = {},
                    onToggleOption = { _, _ -> },
                    onClearOptionalGroup = {},
                    onQuantityChange = {},
                    onAddProduct = {},
                    waiterLayer = { compact, visible -> Text("capa compacta=$compact visible=$visible") },
                )
            }
        }
        composeTestRule.onNodeWithText("capa compacta=false visible=true").assertExists()
    }

    @Test
    fun `con la ficha de un platillo abierta la capa recibe que no se ve`() {
        val state = ScreenshotFixtures.catalogLoadedState()
        val product = state.catalog!!.products.first()
        composeTestRule.setContent {
            ScreenshotTheme {
                CatalogScreen(
                    state = state.copy(selectedProductId = product.id),
                    onRetry = {},
                    onSearchChange = {},
                    onCategorySelected = {},
                    onProductSelected = {},
                    onDismissProduct = {},
                    onToggleOption = { _, _ -> },
                    onClearOptionalGroup = {},
                    onQuantityChange = {},
                    onAddProduct = {},
                    waiterLayer = { compact, visible -> Text("capa compacta=$compact visible=$visible") },
                )
            }
        }
        composeTestRule.onNodeWithText("capa compacta=false visible=false").assertExists()
    }

    @Test
    fun `sin mesa el menu no trae la pildora`() {
        composeTestRule.setContent {
            ScreenshotTheme {
                CatalogScreen(
                    state = ScreenshotFixtures.catalogLoadedState(),
                    onRetry = {},
                    onSearchChange = {},
                    onCategorySelected = {},
                    onProductSelected = {},
                    onDismissProduct = {},
                    onToggleOption = { _, _ -> },
                    onClearOptionalGroup = {},
                    onQuantityChange = {},
                    onAddProduct = {},
                )
            }
        }
        composeTestRule.onNodeWithTag(MENU_WAITER_PILL_TAG).assertDoesNotExist()
    }

    @Test
    fun `oculta no se ve aunque haya mesa`() {
        setLayer(visible = false)
        pill().assertDoesNotExist()
    }

    private fun pill() = composeTestRule.onNodeWithTag(MENU_WAITER_PILL_TAG)

    private fun setLayer(
        visible: Boolean = true,
        compact: Boolean = false,
        onCurrent: suspend (Int) -> Result<TableCall?> = { Result.success(null) },
        onCall: suspend (
            Int,
            CallReason,
            String?,
        ) -> Result<TableCall> = { _, _, _ -> Result.success(call(CallStatus.PENDIENTE)) },
        onCancel: suspend (TableCall) -> Result<TableCall> = { Result.success(it) },
    ) {
        composeTestRule.setContent {
            ScreenshotTheme {
                MenuWaiterLayer(
                    spaceId = 1,
                    visible = visible,
                    compact = compact,
                    onCurrent = onCurrent,
                    onCall = onCall,
                    onCancel = onCancel,
                    onJoinTable = null,
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun call(
        status: CallStatus,
        taker: String? = null,
        reason: CallReason = CallReason.UTENSILIOS,
    ) = TableCall(
        id = "call-1",
        space = TableSpace(id = 1, name = "Mesa 1", type = "mesa"),
        orderId = null,
        reason = reason,
        status = status,
        clientName = "Ana",
        takenBy = taker?.let { TableCallTaker(userId = "u-2", name = it) },
        createdAt = "2026-10-22T16:54:08.777Z",
        takenAt = null,
        closedAt = null,
        version = 1,
    )
}
