package com.vaiinilla.app.data.operational

import com.vaiinilla.app.domain.repository.RefundMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** El JSON de `/devoluciones` tal como lo manda api-dev (23 oct). */
class PendingRefundContractTest {
    private val list =
        """
        {"data":[{"id":"5e0c7fb8-383b-4963-b793-7886af99bfc7","origen":"cancelacion","espacio_id":null,
        "pedido_id":"347cf031-9f91-499d-b1e2-80a5cd3a56df","metodo_original":"efectivo","monto":"50.00",
        "estado":"pendiente","creado_en":"2026-10-23T06:15:19.359Z","metodo_devolucion":null,"nota":null,
        "devuelta_en":null},{"id":"029c638a-6f93-4ed5-a934-2ec4fb4ad36a","origen":"rechazo_articulo",
        "espacio_id":4,"pedido_id":"0065f0e4-3c49-4001-bf34-afbe984047cc","metodo_original":"saldo",
        "monto":"20.00","estado":"pendiente","creado_en":"2026-10-23T06:18:59.715Z","metodo_devolucion":null,
        "nota":null,"devuelta_en":null}],"meta":{"page":null,"total_pages":null,"total_items":null,"cursor":null},
        "error":null}
        """.trimIndent()

    @Test
    fun `lee las devoluciones pendientes`() {
        val refunds = PendingRefundJson.parseList(list)

        assertEquals(2, refunds.size)
        assertEquals("cancelacion", refunds[0].origin)
        assertEquals("50.00", refunds[0].amount)
        assertNull(refunds[0].spaceId)
        assertEquals(4, refunds[1].spaceId)
        assertEquals("saldo", refunds[1].originalMethod)
    }

    @Test
    fun `la confirmación manda el método y la nota solo si hay`() {
        assertEquals("""{"metodo":"efectivo"}""", PendingRefundJson.encodeConfirmation(RefundMethod.CASH, null))
        assertEquals(
            """{"metodo":"otro","nota":"Transferencia"}""",
            PendingRefundJson.encodeConfirmation(RefundMethod.OTHER, " Transferencia "),
        )
    }
}
