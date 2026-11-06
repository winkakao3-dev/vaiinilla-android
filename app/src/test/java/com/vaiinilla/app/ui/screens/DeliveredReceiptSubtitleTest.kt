package com.vaiinilla.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class DeliveredReceiptSubtitleTest {
    @Test
    fun `con dinero por cobrar no dice que ya quedo pagado`() {
        assertEquals("Ya quedó entregado. Todavía falta pagar.", deliveredReceiptSubtitle(null, hasAmountDue = true))
        assertEquals("Rosa lo entregó. Todavía falta pagar.", deliveredReceiptSubtitle("Rosa", hasAmountDue = true))
    }

    @Test
    fun `pagado dice que ya quedo pagado`() {
        assertEquals("Ya quedó entregado y pagado.", deliveredReceiptSubtitle(null, hasAmountDue = false))
        assertEquals("Rosa lo entregó y ya quedó pagado.", deliveredReceiptSubtitle("Rosa", hasAmountDue = false))
    }
}
