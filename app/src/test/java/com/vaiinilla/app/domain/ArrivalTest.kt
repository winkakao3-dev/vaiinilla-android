package com.vaiinilla.app.domain

import com.vaiinilla.app.data.contract.ContractResponseParser
import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.ui.components.canAnnounceArrival
import com.vaiinilla.app.ui.screens.arrivalSuffix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ArrivalTest {
    private fun order(
        state: String = "cobrado",
        destination: String = "para_llevar",
        arrivedAt: String? = null,
    ): OrderDetail {
        val llegada = if (arrivedAt == null) """"llegada_en":null,""" else """"llegada_en":"$arrivedAt","""
        return OrderContractJson().parseOrderDetail(
            """
            {"data":{"id":"9f023852-b234-4350-813d-7af67e9192ea","folio":7,"fecha_operativa":"2026-09-30",
            "estado":"$state","metodo_pago":"saldo","destino":"$destination","espacio":null,"subtotal":"90.00",
            "ahorro_combinado":"0.00","cashback_otorgado":"0.00","total":"90.00","version":2,
            "creado_en":"2026-09-30T23:50:00.000Z","actualizado_en":"2026-09-30T23:50:00.000Z","notas_cocina":null,
            $llegada
            "items":[{"id":1,"producto_id":45,"nombre_producto":"Hamburguesa","estacion_preparacion":"cocina",
            "cantidad":1,"precio_digital_unitario":"90.00","subtotal":"90.00","opciones":[]}]},
            "meta":{"page":null,"total_pages":null,"total_items":null,"cursor":null},"error":null}
            """.trimIndent(),
        )
    }

    @Test
    fun `lee cuando el cliente avisó que llegó`() {
        assertNull(order().arrivedAt)
        assertEquals(Instant.parse("2026-10-01T01:30:35.102Z"), order(arrivedAt = "2026-10-01T01:30:35.102Z").arrivedAt)
    }

    @Test
    fun `un servidor anterior sin llegada_en no revienta`() {
        val raw =
            """{"data":{"id":"9f023852-b234-4350-813d-7af67e9192ea","folio":7,"fecha_operativa":"2026-09-30","estado":"cobrado",
            "metodo_pago":"saldo","destino":"para_llevar","espacio":null,"subtotal":"90.00","ahorro_combinado":"0.00",
            "cashback_otorgado":"0.00","total":"90.00","version":2,"creado_en":"2026-09-30T23:50:00.000Z",
            "actualizado_en":"2026-09-30T23:50:00.000Z","notas_cocina":null,"items":[]},
            "meta":{"page":null,"total_pages":null,"total_items":null,"cursor":null},"error":null}"""
        assertNull(OrderContractJson().parseOrderDetail(raw).arrivedAt)
    }

    @Test
    fun `el aviso solo se ofrece en un drive-thru, para llevar y con el pedido vivo`() {
        listOf("por_cobrar", "cobrado", "preparando", "listo").forEach {
            assertTrue(it, order(state = it).canAnnounceArrival(driveThru = true))
        }
        listOf("entregado", "cancelado", "expirado", "no_recogido").forEach {
            assertFalse(it, order(state = it).canAnnounceArrival(driveThru = true))
        }
        assertFalse(order().canAnnounceArrival(driveThru = false))
        assertFalse(order(destination = "en_espacio").canAnnounceArrival(driveThru = true))
        assertEquals(OrderDestination.TAKE_AWAY, order().summary.destination)
        assertEquals(OrderState.PAID, order().summary.state)
    }

    @Test
    fun `cocina y caja ven YA LLEGO solo si el cliente avisó`() {
        assertEquals("", arrivalSuffix(order()))
        assertEquals(" · YA LLEGÓ", arrivalSuffix(order(arrivedAt = "2026-10-01T01:30:35.102Z")))
    }

    @Test
    fun `el estado operativo trae el tipo del negocio y un servidor anterior es cafeteria`() {
        val parser = ContractResponseParser()
        val base =
            """"recibiendo_pedidos":true,"sesion_caja_abierta":true,"caja_en_linea":true,"cocina_en_linea":true,
            "tiempo_estimado_min":5,"consultado_en":"2026-09-30T23:00:00Z""""
        val drive =
            parser.parseOperationalStatus(
                """{"data":{$base,"tipo":"drive_thru"},"meta":{"page":null,"total_pages":null,"total_items":null,"cursor":null},"error":null}""",
            )
        assertEquals("drive_thru", drive.businessType)
        assertTrue(drive.isDriveThru)
        val old =
            parser.parseOperationalStatus(
                """{"data":{$base},"meta":{"page":null,"total_pages":null,"total_items":null,"cursor":null},"error":null}""",
            )
        assertEquals("cafeteria", old.businessType)
        assertFalse(old.isDriveThru)
        assertNotNull(old.consultedAt)
    }
}
