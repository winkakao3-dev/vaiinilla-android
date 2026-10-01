package com.vaiinilla.app.domain

import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.data.reservations.ReservationContractJson
import com.vaiinilla.app.domain.model.BusyInterval
import com.vaiinilla.app.domain.model.CourtSchedule
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.ReservationSlots
import com.vaiinilla.app.domain.model.ReservationState
import com.vaiinilla.app.domain.repository.SpaceAvailability
import com.vaiinilla.app.domain.repository.SpaceAvailabilityState
import com.vaiinilla.app.domain.repository.TableSpace
import com.vaiinilla.app.ui.components.RentalStep
import com.vaiinilla.app.ui.components.destinationDisplayLabel
import com.vaiinilla.app.ui.components.isLiveRental
import com.vaiinilla.app.ui.components.isRental
import com.vaiinilla.app.ui.components.rentalScheduleLabel
import com.vaiinilla.app.ui.components.rentalStateLabel
import com.vaiinilla.app.ui.components.rentalStep
import com.vaiinilla.app.ui.components.rentalStepDescription
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class RentalTrackingTest {
    private val zone = ZoneId.of("America/Chihuahua")
    private val today = LocalDate.parse("2026-09-30")

    // Renta de Cancha 2 de 18:00 a 19:00 (UTC-6) del 30 de septiembre.
    private fun rental(
        orderState: String = "por_cobrar",
        reservationState: String = "pendiente_pago",
        method: String = "efectivo",
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
            "estado":"$orderState","metodo_pago":"$method","destino":"para_llevar","espacio":null,"subtotal":"275.00",
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
    fun `el pedido de renta trae su reserva y muestra la cancha, no para llevar`() {
        val order = rental()
        assertTrue(order.isRental)
        assertEquals("Cancha 2", destinationDisplayLabel(order))
        assertEquals(ReservationState.PENDING_PAYMENT, order.reservation?.state)
        assertEquals(60, order.reservation?.durationMinutes)
    }

    @Test
    fun `un pedido normal no es renta`() {
        val order = rental(withReservation = false)
        assertFalse(order.isRental)
        assertNull(rentalStep(order, during))
        assertEquals("Para llevar", destinationDisplayLabel(order))
    }

    @Test
    fun `sin pagar va en por pagar y pide pagar en caja, sin hablar de cocina`() {
        val order = rental()
        assertEquals(RentalStep.TO_PAY, rentalStep(order, beforeStart))
        assertEquals("Por pagar", rentalStateLabel(order, beforeStart))
        val text = rentalStepDescription(RentalStep.TO_PAY, order.reservation!!, PaymentMethod.CASH, null, zone, today)
        assertEquals("Paga en caja para confirmar tu horario.", text)
    }

    @Test
    fun `pagada sigue reservada, en juego y terminada segun la hora aunque el pedido diga entregado`() {
        val order = rental(orderState = "entregado", reservationState = "confirmada")
        assertEquals(RentalStep.BOOKED, rentalStep(order, beforeStart))
        assertEquals(RentalStep.PLAYING, rentalStep(order, during))
        assertEquals(RentalStep.DONE, rentalStep(order, after))
        assertTrue(order.isLiveRental(beforeStart))
        assertTrue(order.isLiveRental(during))
        assertFalse(order.isLiveRental(after))
        assertEquals("En juego", rentalStateLabel(order, during))
    }

    @Test
    fun `una renta cancelada o vencida se cae y dice por que`() {
        val expired = rental(orderState = "expirado", reservationState = "expirada")
        assertNull(rentalStep(expired, during))
        assertFalse(expired.isLiveRental(during))
        val canceled = rental(reservationState = "cancelada")
        assertEquals("Cancelada", rentalStateLabel(canceled, during))
    }

    @Test
    fun `el horario se lee hoy, manana o con fecha`() {
        val reservation = rental().reservation!!
        assertEquals("Hoy · 18:00–19:00", rentalScheduleLabel(reservation, zone, today))
        assertEquals("Mañana · 18:00–19:00", rentalScheduleLabel(reservation, zone, today.minusDays(1)))
        val booked = rentalStepDescription(RentalStep.BOOKED, reservation, PaymentMethod.CASH, null, zone, today)
        assertEquals("Te esperamos hoy a las 18:00.", booked)
    }
}

class CourtBusyTest {
    private fun court(vararg busy: Pair<String, String>) =
        CourtSchedule(
            id = 7,
            name = "Cancha 1",
            pricePerHour = "300.00",
            customerPricePerHour = null,
            rentable = true,
            busy = busy.map { BusyInterval(Instant.parse(it.first), Instant.parse(it.second), "reserva") },
        )

    @Test
    fun `ocupada hasta une un turno con la reserva pegada`() {
        val court =
            court(
                "2026-09-30T18:00:00Z" to "2026-09-30T19:00:00Z",
                "2026-09-30T19:00:00Z" to "2026-09-30T20:30:00Z",
                "2026-09-30T21:00:00Z" to "2026-09-30T22:00:00Z",
            )
        assertEquals(
            Instant.parse("2026-09-30T20:30:00Z"),
            ReservationSlots.busyUntil(court, Instant.parse("2026-09-30T18:30:00Z")),
        )
        assertNull(ReservationSlots.busyUntil(court, Instant.parse("2026-09-30T20:45:00Z")))
    }

    private fun space(
        state: SpaceAvailabilityState,
        start: String?,
        end: String?,
        reservationState: String? = "pendiente_pago",
    ) = SpaceAvailability(
        space = TableSpace(id = 7, name = "Cancha 1", type = "cancha"),
        state = state,
        settled = true,
        endsAt = null,
        releasesAt = null,
        remainingSeconds = null,
        graceMinutes = 5,
        pricePerHour = "300.00",
        nextReservationStart = start,
        nextReservationEnd = end,
        nextReservationState = reservationState,
    )

    @Test
    fun `una cancha apartada ahora no se cuenta libre en el mapa`() {
        val now = Instant.parse("2026-09-30T18:05:00Z")
        val held = space(SpaceAvailabilityState.LIBRE, "2026-09-30T18:00:00Z", "2026-09-30T19:00:00Z")
        assertTrue(held.isHeldAt(now))
        assertTrue(held.isWaitingPayment)
        val later = space(SpaceAvailabilityState.LIBRE, "2026-09-30T20:00:00Z", "2026-09-30T21:00:00Z")
        assertFalse(later.isHeldAt(now))
        val noReservation = space(SpaceAvailabilityState.LIBRE, null, null, null)
        assertFalse(noReservation.isHeldAt(now))
        val occupied = space(SpaceAvailabilityState.OCUPADA, "2026-09-30T18:00:00Z", "2026-09-30T19:00:00Z")
        assertFalse(occupied.isHeldAt(now))
    }

    @Test
    fun `la disponibilidad trae la ficha de la cancha`() {
        val day =
            ReservationContractJson(OrderContractJson()).parseDay(
                """
                {"data":{"fecha":"2026-10-01","hoy":"2026-09-30","zona_horaria":"America/Chihuahua",
                "apertura":"08:00","cierre":"22:00","abre":"2026-10-01T14:00:00.000Z","cierra":"2026-10-02T04:00:00.000Z",
                "bloque_min":30,"duraciones_min":[60,90],"dias_adelanto":14,"apartado_min":10,
                "ahora":"2026-09-30T19:00:00.000Z","canchas":[
                {"espacio":{"id":7,"nombre":"Cancha 1","tipo":"cancha"},"ficha":{"descripcion":"Techada de vidrio",
                "imagen_url":"https://x/cancha.jpg","caracteristicas":["Techada","Con luz"]},
                "precio_hora":"300.00","rentable":true,"ocupado":[]},
                {"espacio":{"id":8,"nombre":"Cancha 2","tipo":"cancha"},"precio_hora":"250.00","rentable":true,"ocupado":[]}]},
                "meta":null,"error":null}
                """.trimIndent(),
            )
        val withProfile = day.courts.first().profile
        assertEquals("Techada de vidrio", withProfile.description)
        assertEquals("https://x/cancha.jpg", withProfile.imageUrl)
        assertEquals(listOf("Techada", "Con luz"), withProfile.features)
        assertTrue(day.courts[1].profile.isEmpty)
    }
}
