package com.vaiinilla.app.data.order

import com.vaiinilla.app.domain.model.OrderItemRejection
import com.vaiinilla.app.domain.model.activeItems
import com.vaiinilla.app.domain.model.rejectedItemsHint
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ItemRejectionContractTest {
    @Test
    fun `lee el rechazo del articulo y arma el aviso para el cliente`() {
        val base = ScreenshotFixtures.sampleOrder()
        val first = base.items.first()
        val order =
            base.copy(
                items =
                    listOf(
                        first.copy(
                            id = 1,
                            productName = "Torta",
                            rejection = OrderItemRejection("Se terminó el pan", "80.00"),
                        ),
                        first.copy(id = 2, productName = "Tacos"),
                    ),
            )
        assertEquals(listOf("Tacos"), order.activeItems.map { it.productName })
        assertEquals("Se quitó Torta: Se terminó el pan.", order.rejectedItemsHint)
        assertNull(base.rejectedItemsHint)
    }

    @Test
    fun `el contrato trae rechazo por articulo`() {
        val raw =
            ScreenshotFixtures.rawCreatedOrder().replaceFirst(
                "\"opciones\"",
                "\"rechazo\":{\"motivo\":\"Sin pan\",\"monto\":\"80.00\"},\"opciones\"",
            )
        val parsed = OrderContractJson().parseOrderDetail(raw)
        assertEquals(OrderItemRejection("Sin pan", "80.00"), parsed.items.first().rejection)
    }
}
