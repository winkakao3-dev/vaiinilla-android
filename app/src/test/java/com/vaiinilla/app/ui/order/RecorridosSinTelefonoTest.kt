package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderSpace
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.VenueContext
import com.vaiinilla.app.ui.components.moneyLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Recorridos del cliente (P01 a P09) en la JVM: estado del pedido -> texto que lee el cliente.
 * Prueba la lógica de la pantalla de seguimiento; no cuenta como recorrido en teléfono.
 */
class RecorridosSinTelefonoTest {
    private val venue = VenueContext.STANDARD

    @Test
    fun p01_paraLlevarPreparandoYListo() {
        val making = order(OrderState.PREPARING)
        assertEquals(OrderJourney.COUNTER, orderJourney(making))
        assertEquals("Tu pedido se está preparando.", journeyHeadline(making, OrderJourney.COUNTER))
        assertEquals(1, journeyStepIndex(making, OrderJourney.COUNTER))
        val ready = order(OrderState.READY)
        assertEquals("Ya está. Pasa a la barra por él.", journeyHeadline(ready, OrderJourney.COUNTER))
        assertEquals(3, journeyStepIndex(ready, OrderJourney.COUNTER))
        assertEquals(CaptionTone.DONE, journeyCaption(venue, ready, OrderJourney.COUNTER).tone)
    }

    @Test
    fun p02_enEfectivoNoEmpiezaHastaPagarEnCaja() {
        val cash = order(OrderState.PENDING_PAYMENT, PaymentMethod.CASH)
        assertEquals("Paga en Caja y empiezan tu pedido.", journeyHeadline(cash, OrderJourney.COUNTER))
        assertEquals("Por pagar", journeySteps(venue, cash, OrderJourney.COUNTER).first())
        assertNull(journeyHint(cash, OrderJourney.COUNTER))
        assertEquals(
            "Hasta que pagues no se prepara nada y no se te cobra nada.",
            journeyNote(cash, OrderJourney.COUNTER),
        )
        val paid = order(OrderState.PAID, PaymentMethod.CASH)
        assertTrue(journeyHint(paid, OrderJourney.COUNTER)!!.startsWith("Te avisamos cuando esté en la barra"))
    }

    @Test
    fun p03_driveThruYaLlegueMueveElPedido() {
        val dt = JourneyVenue(driveThru = true)
        val making = order(OrderState.PREPARING)
        assertEquals(OrderJourney.DRIVE_THRU, orderJourney(making, dt))
        assertEquals("Avísanos cuando entres al carril.", journeyHeadline(making, OrderJourney.DRIVE_THRU))
        val arrived = making.copy(arrivedAt = Instant.parse("2026-11-02T18:00:00Z"))
        assertEquals("Ya saben que llegaste.", journeyHeadline(arrived, OrderJourney.DRIVE_THRU))
        val ready = order(OrderState.READY)
        assertEquals("Está listo. Di tu folio 42 en la ventanilla.", journeyHint(ready, OrderJourney.DRIVE_THRU))
    }

    @Test
    fun p04_enLaMesaTeLoLlevan() {
        val making = order(OrderState.PREPARING, space = "mesa")
        assertEquals(OrderJourney.TABLE, orderJourney(making))
        assertEquals("Te lo llevan a la Mesa 4.", journeyHeadline(making, OrderJourney.TABLE))
        val ready = order(OrderState.READY, space = "mesa")
        assertEquals("Está listo. Te lo llevan a la Mesa 4.", journeyHeadline(ready, OrderJourney.TABLE))
        // Listo no es entregado: «En tu mesa» se marca cuando el mesero lo entrega.
        assertEquals(2, journeyStepIndex(ready, OrderJourney.TABLE))
        assertEquals(4, journeyStepIndex(order(OrderState.DELIVERED, space = "mesa"), OrderJourney.TABLE))
        assertEquals(
            "Ya te lo entregaron.",
            journeyHeadline(order(OrderState.DELIVERED, space = "mesa"), OrderJourney.TABLE),
        )
    }

    @Test
    fun p05_enLaBarraTeLoEntreganAhi() {
        val making = order(OrderState.PREPARING, space = "barra")
        assertEquals(OrderJourney.BAR, orderJourney(making))
        assertEquals("Te lo entregan en la barra.", journeyHeadline(making, OrderJourney.BAR))
        assertEquals("Te lo entregan en la barra. Te llaman por tu folio 42.", journeyHint(making, OrderJourney.BAR))
        val ready = order(OrderState.READY, space = "barra")
        assertEquals("Ya está. Te lo dan en la barra.", journeyHeadline(ready, OrderJourney.BAR))
    }

    @Test
    fun p07_laComidaVaALaCanchaRentada() {
        val making = order(OrderState.PREPARING, space = "cancha")
        assertEquals(OrderJourney.COURT_FOOD, orderJourney(making))
        assertEquals("Te lo llevan a tu Cancha 2.", journeyHeadline(making, OrderJourney.COURT_FOOD))
        assertEquals(
            "Te lo llevan a Cancha 2 sin interrumpir el juego.",
            journeyHint(making, OrderJourney.COURT_FOOD),
        )
        val ready = order(OrderState.READY, space = "cancha")
        assertEquals("Está listo. Te lo llevan a tu Cancha 2.", journeyHeadline(ready, OrderJourney.COURT_FOOD))
    }

    @Test
    fun p09_cancelado_dicePorDondeRegresaElDinero() {
        val bySaldo = order(OrderState.CANCELED, PaymentMethod.BALANCE).withCancel("cajero")
        assertEquals("${moneyLabel("10.00")} a tu saldo Vaiinilla", journeyMoneyBack(bySaldo))
        assertEquals("Cancelaron tu pedido. Te regresan todo.", journeyHeadline(bySaldo, OrderJourney.COUNTER))
        val payAtEnd =
            order(OrderState.CANCELED, PaymentMethod.CASH).let {
                it.copy(summary = it.summary.copy(payAtEnd = true))
            }
        assertEquals("No se te cobra nada", journeyMoneyBack(payAtEnd))
        val byClient = order(OrderState.CANCELED, PaymentMethod.BALANCE)
        assertEquals("Tu pedido se canceló.", journeyHeadline(byClient, OrderJourney.COUNTER))
        assertEquals(CaptionTone.PROBLEM, journeyCaption(venue, bySaldo, OrderJourney.COUNTER).tone)
        assertTrue(journeySteps(venue, bySaldo, OrderJourney.COUNTER).isEmpty())
    }

    private fun OrderDetail.withCancel(role: String) = copy(summary = summary.copy(canceledByRole = role))

    private fun order(
        state: OrderState,
        payment: PaymentMethod = PaymentMethod.BALANCE,
        space: String? = null,
    ) = OrderDetail(
        summary =
            OrderSummary(
                id = "1",
                folio = 42,
                operationalDate = "2026-11-02",
                state = state,
                paymentMethod = payment,
                destination = if (space != null) OrderDestination.IN_SPACE else OrderDestination.TAKE_AWAY,
                space = space?.let { OrderSpace(id = 2, name = if (it == "mesa") "Mesa 4" else "Cancha 2", type = it) },
                subtotal = "10.00",
                combinedSavings = "0.00",
                cashbackAwarded = "0.00",
                total = "10.00",
                version = 1,
                createdAt = "2026-11-02T00:00:00Z",
                updatedAt = "2026-11-02T00:00:00Z",
            ),
        user = null,
        kitchenNotes = "",
        items = emptyList(),
        pickupToken = "VN-42",
    )
}
