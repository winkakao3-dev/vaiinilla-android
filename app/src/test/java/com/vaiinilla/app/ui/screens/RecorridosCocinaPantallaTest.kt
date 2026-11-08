package com.vaiinilla.app.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderItem
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.ui.operational.OperationalUiState
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
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
 * Cocina en pantalla (N01 y N04): ver por comanda o por producto con las cantidades sumadas, y el estado de
 * «Todo al día». Corre en la JVM con Robolectric y Compose; no cuenta como recorrido en teléfono.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class RecorridosCocinaPantallaTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val template = ScreenshotFixtures.sampleOrder(state = OrderState.PAID).items.first()

    private fun item(
        id: Int,
        name: String,
        quantity: Int,
    ): OrderItem =
        template.copy(id = id, productName = name, quantity = quantity, options = emptyList(), rejection = null)

    private fun order(
        folio: Int,
        vararg items: OrderItem,
    ): OrderDetail {
        val base = ScreenshotFixtures.sampleOrder(state = OrderState.PAID)
        return base.copy(summary = base.summary.copy(id = "o$folio", folio = folio), items = items.toList())
    }

    private fun show(
        orders: List<OrderDetail>,
        reserved: (@androidx.compose.runtime.Composable () -> Unit)? = null,
        queue: com.vaiinilla.app.ui.operational.QueueSync = com.vaiinilla.app.ui.operational.QueueSync.Live,
    ) {
        composeTestRule.setContent {
            ScreenshotTheme {
                KitchenOperationalScreen(
                    state = OperationalUiState(role = OperationalRole.KITCHEN, orders = orders, queue = queue),
                    onBack = {},
                    onStart = { _, _ -> },
                    onMarkReady = { _, _ -> },
                    reservedEntry = reserved,
                )
            }
        }
    }

    @Test
    fun n01_porProductoSumaLoMismoDeVariasComandasConSusFolios() {
        val a = order(16, item(1, "Papas", 1), item(2, "Nachos", 1))
        val b = order(17, item(3, "Papas", 1), item(4, "Pizza", 1))
        val totals = kitchenProductTotals(listOf(a, b))
        assertEquals(listOf("Papas"), totals.take(1).map { it.name })
        assertEquals(2, totals.first().quantity)
        assertEquals(listOf(16 to 1, 17 to 1), totals.first().folios)
        assertEquals(setOf("Papas", "Nachos", "Pizza"), totals.map { it.name }.toSet())

        show(listOf(a, b))
        composeTestRule.onNodeWithText("Por comanda").assertIsDisplayed()
        composeTestRule.onNodeWithText("Por producto").performClick()
        composeTestRule.waitForIdle()
        assertTrue(composeTestRule.onAllNodesWithText("Papas", substring = true).fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeTestRule.onAllNodesWithText("Pizza", substring = true).fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun n01_volverAPorComandaMuestraLasComandasOtraVez() {
        show(listOf(order(16, item(1, "Papas", 1))))
        composeTestRule.onNodeWithText("Por producto").performClick()
        composeTestRule.onNodeWithText("Por comanda").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("#16").assertIsDisplayed()
    }

    @Test
    fun n04_sinComandasCocinaDiceTodoAlDia() {
        show(emptyList())
        composeTestRule.onNodeWithText("Todo al día").assertIsDisplayed()
    }

    @Test
    fun n04_enUnaEscuelaCocinaOfreceParaManana() {
        show(emptyList(), reserved = { androidx.compose.material3.Text("Para mañana") })
        composeTestRule.waitForIdle()
        assertTrue(composeTestRule.onAllNodesWithText("Para mañana").fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun n11_sinRedSaleLaFranjaAmarillaYLaListaSeQueda() {
        show(
            listOf(order(16, item(1, "Papas", 1))),
            queue =
                com.vaiinilla.app.ui.operational.QueueSync.Failed(
                    com.vaiinilla.app.core.network.ApiFailure.NoConnection,
                ),
        )
        composeTestRule.onNodeWithText("Sin internet").assertIsDisplayed()
        composeTestRule.onNodeWithText("#16").assertIsDisplayed()
    }

    @Test
    fun n11_conLaRedBienNoHayFranja() {
        show(listOf(order(16, item(1, "Papas", 1))))
        composeTestRule.onNodeWithText("Sin internet").assertDoesNotExist()
    }

    @Test
    fun n11_unFalloDelServidorNoDiceSinInternet() {
        show(
            listOf(order(16, item(1, "Papas", 1))),
            queue =
                com.vaiinilla.app.ui.operational.QueueSync.Failed(
                    com.vaiinilla.app.core.network.ApiFailure
                        .ServerError(500, "X", "Tuvimos un problema"),
                ),
        )
        composeTestRule.onNodeWithText("Sin internet").assertDoesNotExist()
    }
}
