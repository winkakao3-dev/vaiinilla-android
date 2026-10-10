package com.vaiinilla.app.domain

import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.PastOrderEntry
import com.vaiinilla.app.domain.model.groupOrdersBySession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderSessionsTest {
    private fun order(
        folio: Int,
        session: String?,
    ): OrderDetail {
        val destino = if (session == null) "para_llevar" else "en_espacio"
        val sesion = session?.let { "\"$it\"" } ?: "null"
        val espacio = if (session == null) "null" else """{"id":67,"nombre":"Mesa 67","tipo":"mesa"}"""
        return OrderContractJson().parseOrderDetail(
            """
            {"data":{"id":"p-$folio","folio":$folio,"fecha_operativa":"2026-10-08","estado":"entregado",
            "metodo_pago":"efectivo","destino":"$destino","espacio":$espacio,"subtotal":"20.00",
            "ahorro_combinado":"0.00","cashback_otorgado":"0.00","total":"20.00","version":1,
            "creado_en":"2026-10-08T18:00:00.000Z","actualizado_en":"2026-10-08T18:00:00.000Z",
            "notas_cocina":null,"sesion_espacio_id":$sesion,"items":[],"reserva":null},
            "meta":{"page":null,"total_pages":null,"total_items":null,"cursor":null},"error":null}
            """.trimIndent(),
        )
    }

    @Test
    fun `los pedidos de una sesion forman una tarjeta y el para llevar sale suelto`() {
        val entries =
            groupOrdersBySession(
                listOf(order(33, "ses-a"), order(34, "ses-a"), order(35, "ses-a"), order(36, null)),
            )

        assertEquals(2, entries.size)
        val session = entries[0] as PastOrderEntry.TableSession
        assertEquals("ses-a", session.sessionId)
        assertEquals(listOf(33, 34, 35), session.orders.map { it.summary.folio })
        assertEquals(36, (entries[1] as PastOrderEntry.Single).order.summary.folio)
    }

    @Test
    fun `sesiones intercaladas se agrupan cada una en el lugar de su primer pedido`() {
        val entries =
            groupOrdersBySession(listOf(order(1, "ses-a"), order(2, "ses-b"), order(3, "ses-a")))

        assertEquals(listOf("session-ses-a", "session-ses-b"), entries.map { it.key })
        assertEquals(listOf(1, 3), (entries[0] as PastOrderEntry.TableSession).orders.map { it.summary.folio })
        assertEquals(listOf(2), (entries[1] as PastOrderEntry.TableSession).orders.map { it.summary.folio })
    }

    @Test
    fun `un solo pedido con sesion tambien forma grupo y sin sesion no`() {
        val withSession = groupOrdersBySession(listOf(order(7, "ses-c")))
        val withoutSession = groupOrdersBySession(listOf(order(8, null)))

        assertTrue(withSession.single() is PastOrderEntry.TableSession)
        assertTrue(withoutSession.single() is PastOrderEntry.Single)
    }
}
