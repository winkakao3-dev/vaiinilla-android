package com.vaiinilla.app.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.ui.operational.OperationalUiState
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** C01/D09/I03: un pedido listo de una cuenta abierta (`entrega_sin_qr`) se entrega en Caja sin escanear. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi", sdk = [33])
class CashierDeliverWithoutQrUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun ready(withoutQr: Boolean): OrderDetail =
        ScreenshotFixtures.sampleOrder(state = OrderState.READY).let {
            it.copy(summary = it.summary.copy(deliveryWithoutQr = withoutQr))
        }

    private fun show(
        order: OrderDetail,
        onScanDeliver: (String, Int) -> Unit = { _, _ -> },
        onDeliver: (String, Int) -> Unit = { _, _ -> },
    ) {
        composeTestRule.setContent {
            ScreenshotTheme {
                CashierOperationalScreen(
                    state =
                        OperationalUiState(
                            role = OperationalRole.CASHIER,
                            orders = listOf(order),
                            cashSessionOpen = true,
                        ),
                    onBack = {},
                    onOpenCashSession = {},
                    onCollect = { _, _, _, _ -> },
                    onScanDeliver = onScanDeliver,
                    onDeliver = onDeliver,
                    onChangeMode = {},
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `an open account order is delivered without scanning`() {
        val order = ready(withoutQr = true)
        var delivered: Pair<String, Int>? = null
        var scanned: Pair<String, Int>? = null
        show(order, onScanDeliver = { id, v -> scanned = id to v }, onDeliver = { id, v -> delivered = id to v })

        composeTestRule.onNodeWithText("Escanear QR para entregar").assertDoesNotExist()
        composeTestRule.onNodeWithText("Entregar").performScrollTo().performClick()

        assertEquals(order.summary.id to order.summary.version, delivered)
        assertNull(scanned)
    }

    @Test
    fun `any other ready order still asks for the QR`() {
        val order = ready(withoutQr = false)
        var scanned: Pair<String, Int>? = null
        show(order, onScanDeliver = { id, v -> scanned = id to v })

        composeTestRule.onNodeWithText("Escanear QR para entregar").performScrollTo().performClick()

        assertEquals(order.summary.id to order.summary.version, scanned)
    }
}
