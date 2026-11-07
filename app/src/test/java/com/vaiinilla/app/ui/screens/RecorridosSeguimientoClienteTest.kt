package com.vaiinilla.app.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.repository.CallReason
import com.vaiinilla.app.domain.repository.CallStatus
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.domain.repository.TableSpace
import com.vaiinilla.app.ui.operational.OperationalUiState
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pedidos del cliente (B07): lo que sigue en curso arriba y lo ya entregado en «Anteriores». Y llamar al mesero
 * (Z04): se elige el motivo y se avisa. Corre en la JVM con Robolectric y Compose; no cuenta como teléfono.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class RecorridosSeguimientoClienteTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun order(
        id: String,
        folio: Int,
        state: OrderState,
        destination: OrderDestination = OrderDestination.TAKE_AWAY,
    ): OrderDetail {
        val base = ScreenshotFixtures.sampleOrder(state = state, destination = destination, spaceId = 4)
        return base.copy(summary = base.summary.copy(id = id, folio = folio))
    }

    private fun show(
        orders: List<OrderDetail>,
        onSelect: (String) -> Unit = {},
        canCall: Boolean = false,
        onCall: (suspend (Int, CallReason, String?) -> Result<TableCall>)? = null,
    ) {
        composeTestRule.setContent {
            ScreenshotTheme {
                StudentTrackingScreen(
                    state = OperationalUiState(role = OperationalRole.CLIENT, orders = orders),
                    orderState = ScreenshotFixtures.catalogLoadedState(),
                    onMenu = {},
                    onAssistant = {},
                    onWallet = {},
                    onCart = {},
                    onOpenCatalog = {},
                    onSelectOrder = onSelect,
                    canCallWaiter = { canCall },
                    onCurrentWaiterCall = if (canCall) ({ Result.success(null) }) else null,
                    onCallWaiter = onCall,
                    onCancelWaiter = if (canCall) ({ Result.success(it) }) else null,
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun scrollTo(text: String) {
        composeTestRule.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText(text, substring = true))
        composeTestRule.onNodeWithText(text, substring = true).assertExists()
    }

    @Test
    fun b07_loQueSigueEnCursoVaArribaYLoEntregadoEnAnteriores() {
        show(listOf(order("a", 21, OrderState.PREPARING), order("b", 20, OrderState.DELIVERED)))
        scrollTo("ANTERIORES")
        composeTestRule.onNodeWithText("#21").assertExists()
        scrollTo("#20")
    }

    @Test
    fun b07_sinPedidosLoDiceYOfreceAPedirAlgo() {
        show(emptyList())
        composeTestRule.onNodeWithText("Sin pedidos activos").assertExists()
        composeTestRule.onNodeWithText("Pedir algo").assertExists()
    }

    @Test
    fun b07_unPedidoAnteriorSeLlamaPorSuFolioYElNegocio() {
        assertEquals("#20 · Boliche Strike", pastOrderTitle(order("b", 20, OrderState.DELIVERED), "Boliche Strike"))
    }

    @Test
    fun b07_alTocarUnPedidoSeAbreSuSeguimiento() {
        val selected = mutableListOf<String>()
        show(listOf(order("a", 21, OrderState.PREPARING)), onSelect = { selected += it })
        composeTestRule.onNodeWithText("#21").performClick()
        composeTestRule.waitForIdle()
        assertEquals(listOf("a"), selected)
    }

    @Test
    fun z04_llamarAlMeseroOfreceLosMotivosYAvisaConElElegido() {
        val calls = mutableListOf<CallReason>()
        val call =
            TableCall(
                id = "c-1",
                space = TableSpace(4, "Mesa 4", "mesa"),
                orderId = null,
                reason = CallReason.UTENSILIOS,
                status = CallStatus.PENDIENTE,
                clientName = "Robot",
                takenBy = null,
                createdAt = java.time.Instant.now().toString(),
                takenAt = null,
                closedAt = null,
                version = 1,
            )
        show(
            listOf(order("a", 21, OrderState.READY, OrderDestination.IN_SPACE)),
            canCall = true,
            onCall = { _, reason, _ ->
                calls += reason
                Result.success(call)
            },
        )
        composeTestRule.onNodeWithText("Llamar al mesero").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Cubiertos o servilletas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pedir la cuenta").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cubiertos o servilletas").performClick()
        composeTestRule.waitUntil(5_000) { calls.isNotEmpty() }
        assertEquals(listOf(CallReason.UTENSILIOS), calls)
    }
}
