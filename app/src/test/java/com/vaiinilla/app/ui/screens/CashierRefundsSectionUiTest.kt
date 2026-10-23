package com.vaiinilla.app.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
    fun `lo pagado con saldo se devuelve con la terminal o en efectivo`() {
        val refund = PendingRefund("r-1", "rechazo_articulo", null, "p-1", "saldo", "20.00", "pendiente", "")
        var confirmed: RefundMethod? = null
        composeTestRule.setContent {
            ScreenshotTheme {
                CashierRefundsSection(
                    state = PendingRefundsUiState(refunds = listOf(refund)),
                    canConfirm = true,
                    colors = rememberOperationalColors(),
                    onConfirm = { _, method -> confirmed = method },
                )
            }
        }

        composeTestRule.onNodeWithText("Devolver $20").assertIsDisplayed()
        composeTestRule.onNodeWithText("Se quitó un artículo ya pagado · se pagó con saldo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Devuelto en terminal").assertIsDisplayed()
        composeTestRule.onNodeWithText("En efectivo").performClick()
        assertEquals(RefundMethod.CASH, confirmed)
    }
}
