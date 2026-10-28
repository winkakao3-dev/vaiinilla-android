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

        composeTestRule.onNodeWithText("Devolver $20").assertIsDisplayed()
        composeTestRule.onNodeWithText("Se quitó un artículo ya pagado · se pagó con saldo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Devuelto en efectivo").performClick()
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

        composeTestRule.onNodeWithText("Devuelto en efectivo").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Otro").assertIsEnabled().performClick()
        assertEquals(null, confirmed)
        composeTestRule.onNodeWithText("Confirmar devolución").assertIsNotEnabled()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Transferencia")
        composeTestRule.onNodeWithText("Confirmar devolución").assertIsEnabled().performClick()
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

        composeTestRule.onNodeWithText("Devuelto en terminal").assertIsEnabled()
        composeTestRule.onNodeWithText("En efectivo").assertIsNotEnabled()
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
