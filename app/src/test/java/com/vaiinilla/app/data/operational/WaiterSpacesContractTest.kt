package com.vaiinilla.app.data.operational

import com.vaiinilla.app.domain.repository.CallReason
import com.vaiinilla.app.domain.repository.SpaceAvailabilityState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WaiterSpacesContractTest {
    private val json = WaiterContractJson()

    @Test
    fun `parses the availability map with the turn start and end`() {
        val raw =
            """
            {"data":[
              {"espacio":{"id":7,"nombre":"Cancha 1","tipo":"cancha"},"estado":"ocupada","saldada":false,
               "inicio":"2026-09-29T20:00:00.000Z","fin_previsto":"2026-09-29T21:00:00.000Z",
               "libera_en":"2026-09-29T21:05:00.000Z","restante_seg":1800,"gracia_min":5},
              {"espacio":{"id":8,"nombre":"Cancha 2","tipo":"cancha"},"estado":"libre","saldada":true,
               "inicio":null,"fin_previsto":null,"libera_en":null,"restante_seg":null,"gracia_min":5}
            ],"meta":{},"error":null}
            """.trimIndent()

        val courts = json.parseAvailability(raw)

        assertEquals(2, courts.size)
        assertEquals(SpaceAvailabilityState.OCUPADA, courts[0].state)
        assertFalse(courts[0].settled)
        assertEquals("2026-09-29T20:00:00.000Z", courts[0].startedAt)
        assertEquals("2026-09-29T21:00:00.000Z", courts[0].endsAt)
        assertEquals(1800, courts[0].remainingSeconds)
        assertEquals(SpaceAvailabilityState.LIBRE, courts[1].state)
        assertNull(courts[1].startedAt)
        assertNull(courts[1].endsAt)
    }

    @Test
    fun `an unknown state from a newer server is rejected instead of guessed`() {
        val raw =
            """{"data":[{"espacio":{"id":1,"nombre":"C","tipo":"cancha"},"estado":"mantenimiento"}],"meta":{},"error":null}"""
        val error = runCatching { json.parseAvailability(raw) }.exceptionOrNull()
        assertNotNull(error)
    }

    @Test
    fun `parses a session with its account and money as two decimals`() {
        val raw =
            """
            {"data":{"espacio":{"id":7,"nombre":"Cancha 1","tipo":"cancha"},"estado":"ocupada","saldada":false,
              "inicio":"2026-09-29T20:00:00.000Z","fin_previsto":"2026-09-29T21:00:00.000Z",
              "libera_en":"2026-09-29T21:05:00.000Z","restante_seg":900,"gracia_min":5,
              "sesion":{"id":"s-1","estado":"abierta","inicio":"2026-09-29T20:00:00.000Z",
                        "fin_previsto":"2026-09-29T21:00:00.000Z","version":4},
              "cuenta":{"pedidos":[
                 {"id":"o-1","folio":41,"estado":"entregado","total":110,"pago_diferido":true,
                  "pendiente_cobro":true,"cliente":{"nombre":"Ana"},"items_resumen":"2× Agua"},
                 {"id":"o-2","folio":42,"estado":"cobrado","total":89.5,"pago_diferido":false,
                  "pendiente_cobro":false,"cliente":null,"items_resumen":"1× Café"}],
                "total":199.5,"pendiente":110,"pagado":89.5,"saldada":false}},
             "meta":{},"error":null}
            """.trimIndent()

        val detail = json.parseSessionDetail(raw)

        assertEquals(4, detail.session?.version)
        assertEquals("s-1", detail.session?.id)
        val account = detail.account
        assertNotNull(account)
        assertEquals("199.50", account?.total)
        assertEquals("110.00", account?.pending)
        assertEquals("89.50", account?.paid)
        assertFalse(account?.settled ?: true)
        assertEquals("110.00", account?.orders?.get(0)?.total)
        assertTrue(account?.orders?.get(0)?.payAtEnd ?: false)
        assertTrue(account?.orders?.get(0)?.pending ?: false)
        assertEquals("89.50", account?.orders?.get(1)?.total)
        assertNull(account?.orders?.get(1)?.clientName)
    }

    @Test
    fun `a free space has neither session nor account`() {
        val raw =
            """{"data":{"espacio":{"id":8,"nombre":"Cancha 2","tipo":"cancha"},"estado":"libre","saldada":true,
               "sesion":null,"cuenta":null},"meta":{},"error":null}"""
        val detail = json.parseSessionDetail(raw)
        assertNull(detail.session)
        assertNull(detail.account)
        assertEquals(SpaceAvailabilityState.LIBRE, detail.availability.state)
    }

    @Test
    fun `parses the account collection with the change`() {
        val raw =
            """{"data":{"espacio":{"id":7,"nombre":"Cancha 1","tipo":"cancha"},"pedidos_cobrados":2,
               "total":"220.00","monto_recibido":"300.00","cambio":"80.00"},"meta":{},"error":null}"""
        val collection = json.parseAccountCollection(raw)
        assertEquals(2, collection.ordersCollected)
        assertEquals("220.00", collection.total)
        assertEquals("300.00", collection.received)
        assertEquals("80.00", collection.change)
        // Un servidor anterior no manda `restante`: la cuenta se toma como saldada.
        assertEquals("0.00", collection.remaining)
        assertTrue(collection.settled)
    }

    @Test
    fun `a split collection reports what is left and is not settled`() {
        val raw =
            """{"data":{"espacio":{"id":7,"nombre":"Cancha 1","tipo":"cancha"},"pedidos_cobrados":1,
               "total":"100.00","monto_recibido":"100.00","cambio":"0.00","restante":"120.00"},"meta":{},"error":null}"""
        val collection = json.parseAccountCollection(raw)
        assertEquals("120.00", collection.remaining)
        assertFalse(collection.settled)
    }

    @Test
    fun `collect sends only the chosen orders and omits the list when collecting everything`() {
        assertEquals(
            """{"monto_recibido":"100.00","total_esperado":"100.00","pedido_ids":["a","b"]}""",
            json.encodeCollectAccount("100.00", "100.00", listOf("a", "b")),
        )
        assertFalse(json.encodeCollectAccount("100.00", "100.00", null).contains("pedido_ids"))
    }

    @Test
    fun `reads the delivery policy and keeps the QR by default`() {
        assertFalse(json.parseDeliveryRequiresQr("""{"data":{"entrega_requiere_qr":false},"meta":{},"error":null}"""))
        assertTrue(json.parseDeliveryRequiresQr("""{"data":{"entrega_requiere_qr":true},"meta":{},"error":null}"""))
        assertTrue(json.parseDeliveryRequiresQr("""{"data":{},"meta":{},"error":null}"""))
    }

    @Test
    fun `open session omits the duration for a table and sends it for a court`() {
        assertEquals("{}", json.encodeOpenSession(null))
        assertEquals("""{"duracion_min":90}""", json.encodeOpenSession(90))
    }

    @Test
    fun `extend and release send the version only when known`() {
        assertEquals("""{"minutos":30}""", json.encodeExtendSession(30, null))
        assertEquals("""{"minutos":60,"version":4}""", json.encodeExtendSession(60, 4))
        assertEquals("{}", json.encodeReleaseSpace(null))
        assertEquals("""{"version":2}""", json.encodeReleaseSpace(2))
    }

    @Test
    fun `collect sends the received cash and the total the waiter saw`() {
        assertEquals(
            """{"monto_recibido":"300.00","total_esperado":"220.00"}""",
            json.encodeCollectAccount("300.00", "220.00"),
        )
        assertEquals("""{"monto_recibido":"220.00"}""", json.encodeCollectAccount("220.00", null))
    }

    @Test
    fun `delivery without a QR does not send the field at all`() {
        val without = json.encodeDeliver(3, null)
        assertFalse(without.contains("qr_token"))
        assertTrue(without.contains("\"estado_objetivo\":\"entregado\""))
        assertFalse(json.encodeDeliver(3, "   ").contains("qr_token"))
        assertTrue(json.encodeDeliver(3, " VN-1 ").contains("\"qr_token\":\"VN-1\""))
    }

    @Test
    fun `the bill call reason is understood`() {
        assertEquals(CallReason.CUENTA, CallReason.fromWireValue("cuenta"))
        assertEquals("Pide la cuenta", CallReason.CUENTA.staffLabel)
    }
}
