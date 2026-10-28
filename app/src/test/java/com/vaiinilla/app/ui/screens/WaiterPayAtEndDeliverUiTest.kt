package com.vaiinilla.app.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.repository.BoardOrder
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** C01/D06: un pedido listo que va a la cuenta de la mesa se entrega sin cobrarlo antes. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi", sdk = [33])
class WaiterPayAtEndDeliverUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun ready(payAtEnd: Boolean) =
        BoardOrder(
            id = "p1",
            folio = 7,
            state = OrderState.READY,
            version = 2,
            clientName = "Ana",
            itemsSummary = "1× Café americano",
            updatedAt = "2026-10-27T15:28:13.527Z",
            deliveryWithoutQr = payAtEnd,
            paymentPending = true,
            payAtEnd = payAtEnd,
        )

    private fun show(
        order: BoardOrder,
        onDeliver: (BoardOrder) -> Unit = {},
    ) {
        composeTestRule.setContent {
            ScreenshotTheme {
                WaiterSheetOrderRow(
                    order = order,
                    readOnly = false,
                    canDeliver = true,
                    colors = rememberOperationalColors(),
                    onDeliver = onDeliver,
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `an order on the table tab shows Entregar`() {
        var delivered: BoardOrder? = null
        val order = ready(payAtEnd = true)
        show(order, onDeliver = { delivered = it })

        composeTestRule.onNodeWithText("Cobrar primero").assertDoesNotExist()
        composeTestRule.onNodeWithText("Entregar").performClick()
        composeTestRule.onNodeWithText("¿Ya lo entregaste?").performClick()

        assertEquals(order, delivered)
    }

    @Test
    fun `an unpaid order outside a tab still asks to collect first`() {
        show(ready(payAtEnd = false))

        composeTestRule.onNodeWithText("Cobrar primero").assertExists()
        composeTestRule.onNodeWithText("Entregar").assertDoesNotExist()
    }
}
