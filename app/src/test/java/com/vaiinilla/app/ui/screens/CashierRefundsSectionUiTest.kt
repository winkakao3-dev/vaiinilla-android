package com.vaiinilla.app.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.vaiinilla.app.domain.repository.PendingRefund
import com.vaiinilla.app.domain.repository.RefundMethod
import com.vaiinilla.app.ui.operational.PendingRefundsUiState
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi", sdk = [33])
class CashierRefundsSectionUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `el aviso de caja dice cuantas devoluciones hay y abre el detalle`() {
        val refund = PendingRefund("r-1", "rechazo_articulo", null, "p-1", "saldo", "20.00", "pendiente", "")
        composeTestRule.setContent {
            ScreenshotTheme {
                CashierRefundsEntry(
                    state = PendingRefundsUiState(refunds = listOf(refund)),
                    canConfirm = true,
                    cashOpen = true,
                    colors = rememberOperationalColors(),
                    onConfirm = { _, _, _ -> },
                )
            }
        }

        composeTestRule.onNodeWithText("Ya se lo di · $20").assertDoesNotExist()
        composeTestRule.onNodeWithText("1 devolución pendiente").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ver").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Ya se lo di · $20").assertIsDisplayed()
    }

    @Test
    fun `con varias devoluciones el aviso suma el total`() {
        val refunds =
            listOf(
                PendingRefund("r-1", "cancelacion", null, "p-1", "efectivo", "20.00", "pendiente", ""),
                PendingRefund("r-2", "cancelacion", null, "p-2", "efectivo", "15.50", "pendiente", ""),
            )
        assertEquals("2 devoluciones pendientes", refundsEntryTitle(refunds.size))
        assertEquals("$35.50 en total", refundsEntryDetail(refunds))
    }
    @Test
    fun `lo pagado con saldo se devuelve en efectivo`() {
        val refund = PendingRefund("r-1", "rechazo_articulo", null, "p-1", "saldo", "20.00", "pendiente", "")
        var confirmed: RefundMethod? = null
        composeTestRule.setContent {
            ScreenshotTheme {
                CashierRefundsSection(
                    state = PendingRefundsUiState(refunds = listOf(refund)),
                    canConfirm = true,
                    cashOpen = true,
                    colors = rememberOperationalColors(),
                    onConfirm = { _, method, _ -> confirmed = method },
                )
            }
        }

        composeTestRule.onNodeWithText("Ya se lo di · $20").assertIsDisplayed()
        composeTestRule.onNodeWithText("Se quitó un artículo ya pagado", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Ya se lo di · $20").performClick()
        assertEquals(RefundMethod.CASH, confirmed)
    }

    @Test
    fun `otro se ofrece con la caja cerrada y pide el motivo`() {
        val refund = PendingRefund("r-3", "cancelacion", null, "p-3", "efectivo", "15.00", "pendiente", "")
        var confirmed: RefundMethod? = null
        var note: String? = null
        composeTestRule.setContent {
            ScreenshotTheme {
                CashierRefundsSection(
                    state = PendingRefundsUiState(refunds = listOf(refund)),
                    canConfirm = true,
                    cashOpen = false,
                    colors = rememberOperationalColors(),
                    onConfirm = { _, method, reason ->
                        confirmed = method
                        note = reason
                    },
                )
            }
        }

        // Con la caja cerrada «Otro» ya viene elegido y el efectivo no se puede confirmar.
        composeTestRule.onNodeWithText("En efectivo").performClick()
        composeTestRule.onNodeWithText("Ya se lo di · $15").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Otro").performClick()
        assertEquals(null, confirmed)
        composeTestRule.onNodeWithText("Escribe el motivo para confirmar").assertIsNotEnabled()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Transferencia")
        composeTestRule.onNodeWithText("Ya se lo di · $15").assertIsEnabled().performClick()
        assertEquals(RefundMethod.OTHER, confirmed)
        assertEquals("Transferencia", note)
    }

    @Test
    fun `con la caja cerrada no se ofrece devolver en efectivo`() {
        val refund = PendingRefund("r-2", "cancelacion", null, "p-2", "terminal", "35.00", "pendiente", "")
        composeTestRule.setContent {
            ScreenshotTheme {
                CashierRefundsSection(
                    state = PendingRefundsUiState(refunds = listOf(refund)),
                    canConfirm = true,
                    cashOpen = false,
                    colors = rememberOperationalColors(),
                    onConfirm = { _, _, _ -> },
                )
            }
        }

        composeTestRule.onNodeWithText("En terminal").performClick()
        composeTestRule.onNodeWithText("Ya se lo di · $35").assertIsEnabled()
        composeTestRule.onNodeWithText("En efectivo").performClick()
        composeTestRule.onNodeWithText("Ya se lo di · $35").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Abre la caja para devolver en efectivo.").assertIsDisplayed()
    }

    @Test
    fun `el metodo sugerido sigue a la tarjeta y no al saldo`() {
        assertEquals(RefundMethod.CASH, suggestedRefundMethod("efectivo"))
        assertEquals(RefundMethod.CASH, suggestedRefundMethod("efectivo+saldo"))
        assertEquals(RefundMethod.TERMINAL, suggestedRefundMethod("saldo+terminal"))
        assertEquals(RefundMethod.TERMINAL, suggestedRefundMethod("stripe"))
    }
}
