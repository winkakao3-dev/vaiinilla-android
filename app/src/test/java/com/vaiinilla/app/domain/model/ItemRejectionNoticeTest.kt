package com.vaiinilla.app.domain.model

import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ItemRejectionNoticeTest {
    private val order = ScreenshotFixtures.sampleOrder()
    private val base = "Se quitó Torta del pedido ${order.summary.folio}. El cliente verá el motivo."

    private fun outcome(
        amount: String,
        method: ItemRefundMethod,
        original: String? = null,
    ): ItemRejectionOutcome =
        ItemRejectionOutcome(
            order = order,
            itemId = 1,
            amount = amount,
            refundMethod = method,
            manualRefundOriginalMethod = original,
        )

    @Test
    fun `sin pago o sin monto solo dice que el cliente vera el motivo`() {
        assertEquals(base, itemRejectionNotice(order, "Torta", outcome("0.00", ItemRefundMethod.CASH)))
        assertEquals(base, itemRejectionNotice(order, "Torta", outcome("80.00", ItemRefundMethod.NONE)))
    }

    @Test
    fun `efectivo sale del cajon`() {
        assertEquals(
            "$base Entrega \$80.00 MXN del cajón al cliente.",
            itemRejectionNotice(order, "Torta", outcome("80.00", ItemRefundMethod.CASH)),
        )
    }

    @Test
    fun `tarjeta se reembolsa a la tarjeta`() {
        assertEquals(
            "$base Se reembolsan \$80.00 MXN a su tarjeta.",
            itemRejectionNotice(order, "Torta", outcome("80.00", ItemRefundMethod.STRIPE)),
        )
    }

    @Test
    fun `manual nombra el metodo original y solo reemplaza el primer mas`() {
        assertEquals(
            "$base Devolución pendiente: \$80.00 MXN (se pagó con efectivo y terminal). Caja debe entregarla y confirmarla.",
            itemRejectionNotice(order, "Torta", outcome("80.00", ItemRefundMethod.MANUAL, "efectivo+terminal")),
        )
        assertEquals(
            "$base Devolución pendiente: \$80.00 MXN (se pagó con efectivo y terminal+saldo). Caja debe entregarla y confirmarla.",
            itemRejectionNotice(order, "Torta", outcome("80.00", ItemRefundMethod.MANUAL, "efectivo+terminal+saldo")),
        )
        assertEquals(
            "$base Devolución pendiente: \$80.00 MXN (se pagó con otro medio). Caja debe entregarla y confirmarla.",
            itemRejectionNotice(order, "Torta", outcome("80.00", ItemRefundMethod.MANUAL)),
        )
    }
}
