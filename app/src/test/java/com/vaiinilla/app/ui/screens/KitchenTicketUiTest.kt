package com.vaiinilla.app.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
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

/** La comanda de Cocina como en iOS: con sus notas, lo listo al final y lo rechazado fuera (B04, H01, H04, L05). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class KitchenTicketUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `la comanda trae la nota del pedido y la de cada producto`() {
        val order =
            order("con-nota", 101, OrderState.PAID)
                .copy(kitchenNotes = "Sin cebolla · Hamburguesa: sin pepinillos")
        setKitchen(listOf(order))

        composeTestRule.onNodeWithText("Sin cebolla · Hamburguesa: sin pepinillos").assertIsDisplayed()
    }

    @Test
    fun `al marcar lista sube la siguiente comanda`() {
        // La lista llega ordenada por última actualización: la recién marcada lista queda primero.
        val ready = order("lista", 201, OrderState.READY)
        val next = order("siguiente", 202, OrderState.PAID)
        setKitchen(listOf(ready, next))

        composeTestRule.onNodeWithText("#202").assertIsDisplayed()
        composeTestRule.onNodeWithText("Empezar a preparar").assertIsDisplayed()
    }

    @Test
    fun `la comanda rechazada sale de Cocina`() {
        val rejected = order("rechazada", 301, OrderState.CANCELED)
        val next = order("siguiente", 302, OrderState.PREPARING)
        setKitchen(listOf(rejected, next))

        composeTestRule.onNodeWithText("#302").assertIsDisplayed()
        composeTestRule.onNodeWithText("#301").assertDoesNotExist()
        composeTestRule.onNodeWithText("Nueva").assertDoesNotExist()
    }

    @Test
    fun `Cocina tiene el orbe del asistente`() {
        setKitchen(listOf(order("a", 401, OrderState.PAID)))
        composeTestRule.onNodeWithContentDescription("Abrir ayuda").assertIsDisplayed()
    }

    @Test
    fun `Caja tiene el orbe del asistente`() {
        composeTestRule.setContent {
            ScreenshotTheme {
                CashierOperationalScreen(
                    state = OperationalUiState(role = OperationalRole.CASHIER, cashSessionOpen = true),
                    onBack = {},
                    onOpenCashSession = {},
                    onCollect = { _, _, _, _ -> },
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Abrir ayuda").assertIsDisplayed()
    }

    @Test
    fun `la fila pone lo pendiente antes que lo listo y quita lo que ya no es de Cocina`() {
        val queue =
            kitchenQueue(
                listOf(
                    order("listo", 1, OrderState.READY),
                    order("entregado", 2, OrderState.DELIVERED),
                    order("preparando", 3, OrderState.PREPARING),
                    order("cancelado", 4, OrderState.CANCELED),
                    order("cobrado", 5, OrderState.PAID),
                ),
            )
        assertEquals(listOf("preparando", "cobrado", "listo"), queue.map { it.summary.id })
    }

    private fun setKitchen(orders: List<OrderDetail>) {
        composeTestRule.setContent {
            ScreenshotTheme {
                KitchenOperationalScreen(
                    state = OperationalUiState(role = OperationalRole.KITCHEN, orders = orders),
                    onBack = {},
                    onStart = { _, _ -> },
                    onReady = { _, _ -> },
                )
            }
        }
    }

    private fun order(
        id: String,
        folio: Int,
        state: OrderState,
    ): OrderDetail {
        val base = ScreenshotFixtures.sampleOrder(state = state)
        return base.copy(summary = base.summary.copy(id = id, folio = folio))
    }
}
