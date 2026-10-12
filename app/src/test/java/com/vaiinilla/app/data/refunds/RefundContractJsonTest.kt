package com.vaiinilla.app.data.refunds

import com.vaiinilla.app.domain.model.RefundMethod
import com.vaiinilla.app.domain.model.RefundOrigin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RefundContractJsonTest {
    private val contract = RefundContractJson()

    @Test
    fun `parses the pending list with the backend fields`() {
        val raw =
            """
            {"data":[
              {"id":"6f1c2b9e-1111-4a2b-9c3d-000000000001","origen":"sobrante_cuenta","espacio_id":7,
               "pedido_id":null,"metodo_original":"efectivo","monto":"45.00","estado":"pendiente",
               "creado_en":"2026-10-11T18:00:00.000Z","metodo_devolucion":null,"nota":null,"devuelta_en":null},
              {"id":"6f1c2b9e-1111-4a2b-9c3d-000000000002","origen":"rechazo_articulo","espacio_id":null,
               "pedido_id":"6f1c2b9e-2222-4a2b-9c3d-000000000003","metodo_original":"efectivo+terminal",
               "monto":"120.50","estado":"pendiente","creado_en":"2026-10-11T18:05:00.000Z",
               "metodo_devolucion":null,"nota":null,"devuelta_en":null}
            ],"error":null}
            """.trimIndent()

        val refunds = contract.parseList(raw)

        assertEquals(2, refunds.size)
        assertEquals(RefundOrigin.OVERPAID_ACCOUNT, refunds[0].origin)
        assertEquals(7, refunds[0].spaceId)
        assertEquals("efectivo", refunds[0].originalMethod)
        assertEquals("45.00", refunds[0].amount)
        assertNull(refunds[1].spaceId)
        assertEquals(RefundOrigin.REJECTED_ITEM, refunds[1].origin)
        assertEquals("efectivo+terminal", refunds[1].originalMethod)
    }

    @Test
    fun `parses a confirmed refund response`() {
        val raw =
            """
            {"data":{"id":"6f1c2b9e-1111-4a2b-9c3d-000000000001","origen":"cancelacion","espacio_id":null,
             "pedido_id":null,"metodo_original":"terminal","monto":"30.00","estado":"devuelta",
             "metodo_devolucion":"terminal","nota":null,"devuelta_en":"2026-10-11T19:00:00.000Z"},"error":null}
            """.trimIndent()

        val refund = contract.parseRefund(raw)

        assertEquals(RefundOrigin.CANCELLED_ORDER, refund.origin)
        assertEquals("30.00", refund.amount)
    }

    @Test
    fun `confirm body sends only metodo and a note that has text`() {
        assertEquals("{\"metodo\":\"terminal\"}", contract.encodeConfirm(RefundMethod.CARD, null))
        assertEquals("{\"metodo\":\"efectivo\"}", contract.encodeConfirm(RefundMethod.CASH, "   "))
        assertEquals(
            "{\"metodo\":\"otro\",\"nota\":\"Transferencia a su cuenta\"}",
            contract.encodeConfirm(RefundMethod.OTHER, "Transferencia a su cuenta"),
        )
    }
}
