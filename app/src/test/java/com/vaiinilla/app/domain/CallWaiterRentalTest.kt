package com.vaiinilla.app.domain

import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.repository.callWaiterSpaceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class CallWaiterRentalTest {
    // Renta de Cancha 2 (espacio 8) de 18:00 a 19:00 (UTC-6): 2026-10-01T00:00Z a 01:00Z.
    private fun rental(
        orderState: String = "entregado",
        reservationState: String = "confirmada",
        withReservation: Boolean = true,
    ): OrderDetail {
        val reserva =
            if (withReservation) {
                """"reserva":{"id":"r-1","espacio":{"id":8,"nombre":"Cancha 2","tipo":"cancha"},
                "inicio":"2026-10-01T00:00:00.000Z","fin":"2026-10-01T01:00:00.000Z","duracion_min":60,
                "estado":"$reservationState"},"""
            } else {
                """"reserva":null,"""
            }
        return OrderContractJson().parseOrderDetail(
            """
            {"data":{"id":"9f023852-b234-4350-813d-7af67e9192ea","folio":2,"fecha_operativa":"2026-09-30",
            "estado":"$orderState","metodo_pago":"efectivo","destino":"para_llevar","espacio":null,"subtotal":"275.00",
            "ahorro_combinado":"0.00","cashback_otorgado":"0.00","total":"275.00","version":1,
            "creado_en":"2026-09-30T23:50:00.000Z","actualizado_en":"2026-09-30T23:50:00.000Z","notas_cocina":null,
            $reserva
            "items":[{"id":1,"producto_id":58,"nombre_producto":"Renta Cancha 2 · 60 min","estacion_preparacion":"caja",
            "cantidad":1,"precio_digital_unitario":"275.00","subtotal":"275.00","opciones":[]}]},
            "meta":{"page":null,"total_pages":null,"total_items":null,"cursor":null},"error":null}
            """.trimIndent(),
        )
    }

    private val beforeStart = Instant.parse("2026-09-30T23:55:00Z")
    private val during = Instant.parse("2026-10-01T00:30:00Z")
    private val after = Instant.parse("2026-10-01T01:00:00Z")

    @Test
    fun `se puede llamar al mesero solo mientras la renta esta en juego`() {
        val paid = rental()
        assertNull(callWaiterSpaceId(paid, beforeStart))
        assertEquals(8, callWaiterSpaceId(paid, during))
        assertNull(callWaiterSpaceId(paid, after))
    }

    @Test
    fun `una renta sin pagar o sin reserva no habilita la llamada`() {
        assertNull(callWaiterSpaceId(rental(orderState = "por_cobrar", reservationState = "pendiente_pago"), during))
        assertNull(callWaiterSpaceId(rental(withReservation = false), during))
    }
}
