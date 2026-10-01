package com.vaiinilla.app.domain

import com.vaiinilla.app.data.contract.ContractResponseParser
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.ui.order.ESTABLISHMENT_CLOSED_MESSAGE
import com.vaiinilla.app.ui.order.checkoutStaffBlocker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OrderWindowTest {
    private fun status(extra: String): OperationalStatus {
        val accepting = !extra.contains("\"dentro_de_franja\":false")
        return ContractResponseParser().parseOperationalStatus(
            """
            {"data":{"recibiendo_pedidos":$accepting,"sesion_caja_abierta":true,
            "caja_en_linea":true,"cocina_en_linea":true,"tiempo_estimado_min":5,"consultado_en":"2026-09-30T23:50:00.000Z"
            $extra},"meta":{"page":null,"total_pages":null,"total_items":null,"cursor":null},"error":null}
            """.trimIndent(),
        )
    }

    @Test
    fun `fuera de la franja el cliente ve el horario de pedidos`() {
        val outside =
            status(
                ""","franjas_pedido":[{"desde":"12:00","hasta":"15:00"},{"desde":"18:00","hasta":"20:00"}],"dentro_de_franja":false""",
            )
        assertEquals("12:00 a 15:00 y 18:00 a 20:00", outside.orderHoursText)
        assertEquals(
            "Por ahora no se reciben pedidos. Horario de pedidos: 12:00 a 15:00 y 18:00 a 20:00.",
            outside.checkoutStaffBlocker(),
        )
    }

    @Test
    fun `dentro de la franja no hay aviso`() {
        val inside = status(""","franjas_pedido":[{"desde":"12:00","hasta":"15:00"}],"dentro_de_franja":true""")
        assertNull(inside.outsideOrderHoursMessage)
        assertNull(inside.checkoutStaffBlocker())
    }

    @Test
    fun `un servidor anterior o un negocio sin franjas no tiene limite`() {
        assertNull(status("").outsideOrderHoursMessage)
        assertNull(status(""","franjas_pedido":[],"dentro_de_franja":true""").outsideOrderHoursMessage)
    }

    @Test
    fun `cerrado por otra razon sigue diciendo el aviso generico`() {
        val closed = status("").copy(acceptingOrders = false)
        assertEquals(ESTABLISHMENT_CLOSED_MESSAGE, closed.checkoutStaffBlocker())
    }
}
