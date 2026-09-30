package com.vaiinilla.app.domain

import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.data.reservations.ReservationContractJson
import com.vaiinilla.app.domain.model.BusyInterval
import com.vaiinilla.app.domain.model.CourtDay
import com.vaiinilla.app.domain.model.CourtSchedule
import com.vaiinilla.app.domain.model.ReservationSlots
import com.vaiinilla.app.domain.model.ReservationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ReservationSlotsTest {
    // Horario de 08:00 a 22:00 local (UTC-6) el 1 de octubre.
    private val opens = Instant.parse("2026-10-01T14:00:00Z")
    private val closes = Instant.parse("2026-10-02T04:00:00Z")

    private fun court(vararg busy: Pair<String, String>) =
        CourtSchedule(
            id = 7,
            name = "Cancha 1",
            pricePerHour = "300.00",
            customerPricePerHour = null,
            rentable = true,
            busy = busy.map { BusyInterval(Instant.parse(it.first), Instant.parse(it.second), "reserva") },
        )

    private fun day(
        now: String = "2026-09-30T18:00:00Z",
        date: String = "2026-10-01",
        today: String = "2026-09-30",
    ) = CourtDay(
        date = date,
        today = today,
        timeZone = "America/Chihuahua",
        opensAt = opens,
        closesAt = closes,
        blockMinutes = 30,
        durations = listOf(60, 90, 120),
        daysAhead = 14,
        holdMinutes = 10,
        now = Instant.parse(now),
        courts = emptyList(),
    )

    @Test
    fun `los inicios van cada 30 minutos y dejan caber la duracion mas corta`() {
        val starts = ReservationSlots.startTimes(day())
        assertEquals(opens, starts.first())
        // Último inicio: 21:00, para que quepa una hora antes de las 22:00.
        assertEquals(Instant.parse("2026-10-02T03:00:00Z"), starts.last())
        assertEquals(27, starts.size)
    }

    @Test
    fun `hoy solo se ofrecen los bloques que no han pasado`() {
        val hoy = day(now = "2026-10-01T20:10:00Z", date = "2026-10-01", today = "2026-10-01")
        assertEquals(Instant.parse("2026-10-01T20:30:00Z"), ReservationSlots.startTimes(hoy).first())
    }

    @Test
    fun `una reserva ocupada bloquea lo que la empalma pero no lo que empieza donde termina`() {
        val ocupada = court("2026-10-02T00:00:00Z" to "2026-10-02T01:30:00Z") // 18:00–19:30 local
        val d = day()
        assertFalse(ReservationSlots.isFree(d, ocupada, Instant.parse("2026-10-01T23:30:00Z"), 60))
        assertTrue(ReservationSlots.isFree(d, ocupada, Instant.parse("2026-10-01T23:00:00Z"), 60))
        assertTrue(ReservationSlots.isFree(d, ocupada, Instant.parse("2026-10-02T01:30:00Z"), 90))
        // A las 17:30 caben 30 minutos, no una hora: ese inicio no se ofrece para 60, 90 ni 120.
        assertEquals(
            emptyList<Int>(),
            ReservationSlots.availableDurations(d, ocupada, Instant.parse("2026-10-01T23:30:00Z")),
        )
        assertEquals(
            listOf(60),
            ReservationSlots.availableDurations(d, ocupada, Instant.parse("2026-10-01T23:00:00Z")),
        )
    }

    @Test
    fun `no se reserva pasando el cierre`() {
        assertEquals(
            listOf(60),
            ReservationSlots.availableDurations(day(), court(), Instant.parse("2026-10-02T03:00:00Z")),
        )
    }

    @Test
    fun `rentar ahora solo hoy y con la cancha libre`() {
        val hoy = day(now = "2026-10-01T20:10:00Z", date = "2026-10-01", today = "2026-10-01")
        assertTrue(ReservationSlots.canRentNow(hoy, court()))
        assertFalse(ReservationSlots.canRentNow(hoy, court("2026-10-01T20:00:00Z" to "2026-10-01T21:00:00Z")))
        assertFalse(ReservationSlots.canRentNow(day(), court()))
    }

    @Test
    fun `el monto es proporcional al precio por hora`() {
        assertEquals("450.00", ReservationSlots.amountFor("300.00", 90))
        assertEquals("375.00", ReservationSlots.amountFor("250.00", 90))
        assertEquals("100.00", ReservationSlots.amountFor("50.00", 120))
    }
}

class ReservationContractJsonTest {
    private val json = ReservationContractJson(OrderContractJson())

    @Test
    fun `lee la disponibilidad del dia con precios al cliente y lo ocupado`() {
        val day =
            json.parseDay(
                """
                {"data":{"fecha":"2026-10-01","hoy":"2026-09-30","zona_horaria":"America/Chihuahua",
                "apertura":"08:00","cierre":"22:00","abre":"2026-10-01T14:00:00.000Z","cierra":"2026-10-02T04:00:00.000Z",
                "bloque_min":30,"duraciones_min":[60,90,120,150,180],"dias_adelanto":14,"apartado_min":10,
                "ahora":"2026-09-30T19:00:00.000Z","canchas":[{"espacio":{"id":7,"nombre":"Cancha 1","tipo":"cancha"},
                "precio_hora":"300.00","precio_hora_cliente":{"tarjeta":"326.95","efectivo_saldo":"330.00"},"rentable":true,
                "ocupado":[{"inicio":"2026-10-02T00:00:00.000Z","fin":"2026-10-02T01:30:00.000Z","motivo":"reserva"}]}]},
                "meta":null,"error":null}
                """.trimIndent(),
            )
        val court = day.courts.single()
        assertEquals("Cancha 1", court.name)
        assertEquals("330.00", court.customerPricePerHour?.cashOrBalance)
        assertEquals(1, court.busy.size)
        assertEquals(listOf(60, 90, 120, 150, 180), day.durations)
    }

    @Test
    fun `lee el pago en efectivo con su pedido de renta y el cambio`() {
        val payment =
            json.parsePayment(
                """
                {"data":{"reserva":{"id":"r-1","espacio":{"id":7,"nombre":"Cancha 1","tipo":"cancha"},
                "inicio":"2026-09-30T19:08:00.000Z","fin":"2026-09-30T20:08:00.000Z","duracion_min":60,"precio_hora":"300.00",
                "monto":"300.00","precio_cliente":{"tarjeta":"326.95","efectivo_saldo":"330.00"},"estado":"en_curso",
                "expira_en":null,"pedido_id":"9f023852-b234-4350-813d-7af67e9192ea","canal":"mostrador",
                "nombre_cliente":"Luis","version":4},
                "pedido":{"id":"9f023852-b234-4350-813d-7af67e9192ea","folio":12,"fecha_operativa":"2026-09-30",
                "estado":"entregado","metodo_pago":"efectivo","destino":"para_llevar","espacio":null,"subtotal":"330.00",
                "ahorro_combinado":"0.00","cashback_otorgado":"0.00","total":"330.00","version":4,
                "creado_en":"2026-09-30T19:08:00.000Z","actualizado_en":"2026-09-30T19:08:00.000Z","notas_cocina":null,
                "items":[{"id":1,"producto_id":58,"nombre_producto":"Renta Cancha 1 · 60 min","estacion_preparacion":"caja",
                "cantidad":1,"precio_digital_unitario":"330.00","subtotal":"330.00","opciones":[]}]},
                "cobro":{"monto_recibido":"500.00","cambio":"170.00"}},"meta":null,"error":null}
                """.trimIndent(),
            )
        assertEquals(ReservationState.IN_PROGRESS, payment.reservation.state)
        assertEquals("Luis", payment.reservation.customerName)
        assertEquals(
            "330.00",
            payment.order
                ?.order
                ?.summary
                ?.total,
        )
        assertNull(payment.order?.stripeSession)
        assertEquals("170.00", payment.cashChange)
    }

    @Test
    fun `un estado desconocido no revienta y se pide revisarlo con el personal`() {
        assertEquals(ReservationState.CONFLICT, ReservationState.fromWire("nuevo_estado"))
        assertTrue(ReservationState.CONFIRMED.isLive)
        assertFalse(ReservationState.CANCELLED.isLive)
    }
}
