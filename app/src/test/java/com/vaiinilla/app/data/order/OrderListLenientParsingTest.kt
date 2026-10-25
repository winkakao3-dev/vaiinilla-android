package com.vaiinilla.app.data.order

import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.StripePaymentStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/** Un valor que esta versión no conoce no debe dejar al cliente sin su lista de pedidos ni sin menú. */
class OrderListLenientParsingTest {
    private val json = OrderContractJson()

    private fun order(
        id: String,
        state: String,
    ) = """
        {
          "id": "$id", "folio": 1, "fecha_operativa": "2026-10-17", "estado": "$state",
          "metodo_pago": "efectivo", "destino": "para_llevar", "espacio": null,
          "subtotal": "1.00", "ahorro_combinado": "0.00", "cashback_otorgado": "0.00", "total": "1.00",
          "version": 1, "creado_en": "2026-10-17T12:00:00.000Z", "actualizado_en": "2026-10-17T12:05:00.000Z",
          "notas_cocina": "", "items": []
        }
        """.trimIndent()

    @Test
    fun `an order in an unknown state is skipped and the rest of the list survives`() {
        val raw =
            """{"data":[${order("a", "entregado")},${order("b", "estado_nuevo")},${order("c", "listo")}],""" +
                """"meta":{"cursor":null},"error":null}"""

        assertEquals(listOf("a", "c"), json.parseOrderList(raw).map { it.summary.id })
        assertEquals(listOf("a", "c"), json.parseOrderListPage(raw).orders.map { it.summary.id })
        assertEquals(
            OrderState.READY,
            json
                .parseOrderList(raw)
                .last()
                .summary.state,
        )
    }

    @Test
    fun `unknown payment status reads as pending and unknown kitchen station as kitchen`() {
        assertEquals(StripePaymentStatus.PENDING, StripePaymentStatus.fromWireValue("algo_nuevo"))
        assertEquals(StripePaymentStatus.REFUNDED, StripePaymentStatus.fromWireValue("reembolsado"))
        assertEquals(PreparationStation.KITCHEN, PreparationStation.fromWireValue("barra"))
        assertEquals(PreparationStation.CASHIER, PreparationStation.fromWireValue("caja"))
    }
}
