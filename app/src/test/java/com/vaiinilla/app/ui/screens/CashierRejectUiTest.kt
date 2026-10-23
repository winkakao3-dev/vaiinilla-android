package com.vaiinilla.app.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.vaiinilla.app.domain.model.OperationalRole
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

/** Caja rechaza un pedido (B09) o quita un artículo (G07) con motivo, como Cocina. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi", sdk = [33])
class CashierRejectUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val pending =
        ScreenshotFixtures.sampleOrder(state = OrderState.PENDING_PAYMENT).let { order ->
            val first = order.items.first()
            order.copy(items = order.items + first.copy(id = first.id + 1, productName = "Agua de jamaica"))
        }

    private fun show(
        onRejectOrder: (String, Int, String) -> Unit = { _, _, _ -> },
        onRejectItem: (String, Int, Int, String) -> Unit = { _, _, _, _ -> },
    ) {
        composeTestRule.setContent {
            ScreenshotTheme {
                CashierOperationalScreen(
                    state =
                        OperationalUiState(
                            role = OperationalRole.CASHIER,
                            orders = listOf(pending),
                            cashSessionOpen = true,
                        ),
                    onBack = {},
                    onOpenCashSession = {},
                    onCollect = { _, _, _, _ -> },
                    onRejectOrder = onRejectOrder,
                    onRejectItem = onRejectItem,
                    onChangeMode = {},
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `rechazar el pedido pide el motivo y lo manda`() {
        var rejected: Triple<String, Int, String>? = null
        show(onRejectOrder = { id, version, reason -> rejected = Triple(id, version, reason) })

        composeTestRule.onNodeWithText("Rechazar pedido").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Se acabó el producto").performClick()
        composeTestRule.onAllNodesWithText("Rechazar pedido").onLast().performClick()

        assertEquals(Triple(pending.summary.id, pending.summary.version, "Se acabó el producto"), rejected)
    }

    @Test
    fun `quitar un artículo manda ese artículo`() {
        var removed: Pair<Int, String>? = null
        show(onRejectItem = { _, itemId, _, reason -> removed = itemId to reason })
        val water = pending.items.last()

        composeTestRule.onAllNodesWithText("Quitar")[1].performScrollTo().performClick()
        composeTestRule.onNodeWithText("Faltan ingredientes").performClick()
        composeTestRule.onNodeWithText("Quitar de la comanda").performClick()

        assertEquals(water.id to "Faltan ingredientes", removed)
    }
}
