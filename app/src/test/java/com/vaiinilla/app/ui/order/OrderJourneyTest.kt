package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderSpace
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.ui.components.moneyLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class OrderJourneyTest {
    @Test
    fun eachOrderFindsItsJourney() {
        assertEquals(OrderJourney.COUNTER, orderJourney(order(OrderState.PAID)))
        assertEquals(OrderJourney.DRIVE_THRU, orderJourney(order(OrderState.PAID), JourneyVenue(driveThru = true)))
        assertEquals(OrderJourney.TABLE, orderJourney(order(OrderState.PAID, space = "mesa")))
        assertEquals(OrderJourney.BAR, orderJourney(order(OrderState.PAID, space = "barra")))
        assertEquals(OrderJourney.SEAT, orderJourney(order(OrderState.PAID, space = "asiento")))
        assertEquals(OrderJourney.COURT_FOOD, orderJourney(order(OrderState.PAID, space = "cancha")))
        assertEquals(OrderJourney.PREORDER_PICKUP, orderJourney(order(OrderState.PAID, date = "2026-11-09")))
        assertEquals(
            OrderJourney.SCHOOL,
            orderJourney(order(OrderState.PAID, date = "2026-11-09"), JourneyVenue(school = true)),
        )
        assertEquals(OrderJourney.HOME_DELIVERY, orderJourney(order(OrderState.PAID, home = true)))
    }

    @Test
    fun stepsNameWhatHappensForEachType() {
        val bar = order(OrderState.PREPARING, space = "barra")
        assertEquals(listOf("Pagado", "Preparando", "Listo", "Entregado"), journeySteps(bar, OrderJourney.BAR))
        assertEquals(1, journeyStepIndex(bar, OrderJourney.BAR))
        // Listo cierra «Listo» y espera el último paso, como Doop P2 y P7; en asiento, listo ya es «En camino».
        assertEquals(3, journeyStepIndex(order(OrderState.READY, space = "cancha"), OrderJourney.COURT_FOOD))
        assertEquals(3, journeyStepIndex(order(OrderState.READY), OrderJourney.COUNTER))
        assertEquals(2, journeyStepIndex(order(OrderState.READY, space = "asiento"), OrderJourney.SEAT))

        val cash = order(OrderState.PENDING_PAYMENT, PaymentMethod.CASH)
        assertEquals("Por pagar", journeySteps(cash, OrderJourney.COUNTER).first())
        assertEquals(0, journeyStepIndex(cash, OrderJourney.COUNTER))

        val home = order(OrderState.ON_THE_WAY, home = true)
        assertEquals(3, journeyStepIndex(home, OrderJourney.HOME_DELIVERY))
        assertEquals(5, journeyStepIndex(order(OrderState.DELIVERED, home = true), OrderJourney.HOME_DELIVERY))

        assertTrue(journeySteps(order(OrderState.CANCELED), OrderJourney.COUNTER).isEmpty())
        assertEquals(-1, journeyStepIndex(order(OrderState.CANCELED), OrderJourney.COUNTER))
    }

    @Test
    fun driveThruMovesWhenTheClientArrives() {
        val waiting = order(OrderState.PREPARING)
        val arrived = waiting.copy(arrivedAt = Instant.parse("2026-11-02T18:00:00Z"))
        assertEquals(1, journeyStepIndex(waiting, OrderJourney.DRIVE_THRU))
        assertEquals(2, journeyStepIndex(arrived, OrderJourney.DRIVE_THRU))
        assertEquals(
            "Ya saben que llegaste. Lo terminan y te lo dan en la ventanilla.",
            journeyHint(arrived, OrderJourney.DRIVE_THRU),
        )
    }

    @Test
    fun hintsFallBackWhenCashIsDueOrTheOrderClosed() {
        assertNull(journeyHint(order(OrderState.PENDING_PAYMENT, PaymentMethod.CASH), OrderJourney.BAR))
        assertNull(journeyHint(order(OrderState.DELIVERED), OrderJourney.COUNTER))
        assertEquals(
            "Te lo llevan a Cancha 2 sin interrumpir el juego.",
            journeyHint(order(OrderState.PREPARING, space = "cancha"), OrderJourney.COURT_FOOD),
        )
    }

    @Test
    fun factsShowWhereAndWhatWasPaid() {
        val facts = journeyFacts(order(OrderState.PREPARING, space = "mesa"), OrderJourney.TABLE)
        assertEquals(JourneyFact("Te lo llevan a", "Cancha 2"), facts[0])
        assertEquals("Pagado con saldo", facts[1].label)

        val cash = journeyFacts(order(OrderState.PENDING_PAYMENT, PaymentMethod.CASH), OrderJourney.COUNTER)
        assertTrue(cash.any { it.label == "Total a pagar en Caja" && it.tone == FactTone.DUE })
    }

    @Test
    fun notesWarnWhatMattersNow() {
        assertEquals(
            "Hasta que pagues no se prepara nada y no se te cobra nada.",
            journeyNote(order(OrderState.PENDING_PAYMENT, PaymentMethod.CASH), OrderJourney.COUNTER),
        )
        assertEquals(
            "Si te mueves a una mesa, dile al mesero tu folio.",
            journeyNote(order(OrderState.PREPARING, space = "barra"), OrderJourney.BAR),
        )
        assertNull(journeyNote(order(OrderState.DELIVERED), OrderJourney.COUNTER))
    }

    @Test
    fun captionTitleAndLeadTellWhereTheOrderIs() {
        val bar = order(OrderState.PREPARING, space = "barra")
        assertEquals(
            JourneyCaption("Preparando · en la barra", CaptionTone.WAITING),
            journeyCaption(bar, OrderJourney.BAR),
        )
        assertEquals("Te lo entregan en la barra.", journeyHeadline(bar, OrderJourney.BAR))
        assertEquals("Pedido", journeyNoun(OrderJourney.BAR))
        assertEquals("en la barra", journeyKindLabel(OrderJourney.BAR))

        val ready = order(OrderState.READY)
        assertEquals(CaptionTone.DONE, journeyCaption(ready, OrderJourney.COUNTER).tone)
        assertEquals(
            "Di tu folio o muestra este código. No tienes que formarte.",
            journeyLead(ready, OrderJourney.COUNTER),
        )
        // Sin el código en este teléfono no se promete un código: basta el folio.
        assertEquals("Di tu folio 42 en la barra.", journeyLead(ready.copy(pickupToken = null), OrderJourney.COUNTER))

        val cash = order(OrderState.PENDING_PAYMENT, PaymentMethod.CASH)
        assertEquals("Por pagar · en Caja", journeyCaption(cash, OrderJourney.COUNTER).text)
        assertEquals("Paga en Caja y empiezan tu pedido.", journeyHeadline(cash, OrderJourney.COUNTER))

        val court = order(OrderState.READY, space = "cancha")
        assertEquals("Ya va a tu Cancha 2.", journeyHeadline(court, OrderJourney.COURT_FOOD))

        val canceled = order(OrderState.CANCELED)
        val byCashier = canceled.copy(summary = canceled.summary.copy(canceledByRole = "cajero"))
        assertEquals(CaptionTone.PROBLEM, journeyCaption(byCashier, OrderJourney.COUNTER).tone)
        assertEquals("Cancelaron tu pedido. Te regresan todo.", journeyHeadline(byCashier, OrderJourney.COUNTER))
        assertEquals("${moneyLabel("10.00")} a tu saldo Vaiinilla", journeyMoneyBack(byCashier))
        assertNull(journeyLead(byCashier, OrderJourney.COUNTER))
    }

    @Test
    fun anOrderCanceledBeforePayingSaysNothingWasCharged() {
        val canceled = order(OrderState.CANCELED, PaymentMethod.STRIPE, home = true)
        // Sin pago con tarjeta todavía no hay nada que prometer.
        assertNull(journeyMoneyBack(canceled))
        val unpaidPreorder = canceled.copy(answerBy = Instant.parse("2026-11-03T23:30:00Z"))
        assertEquals("No te cobramos nada", journeyMoneyBack(unpaidPreorder))
        assertTrue(journeyFacts(unpaidPreorder, OrderJourney.HOME_DELIVERY).none { it.label == "Pagaste el anticipo" })
    }

    private fun order(
        state: OrderState,
        payment: PaymentMethod = PaymentMethod.BALANCE,
        space: String? = null,
        date: String? = null,
        home: Boolean = false,
    ) = OrderDetail(
        summary =
            OrderSummary(
                id = "1",
                folio = 42,
                operationalDate = "2026-11-02",
                state = state,
                paymentMethod = payment,
                destination =
                    when {
                        home -> OrderDestination.HOME_DELIVERY
                        space != null -> OrderDestination.IN_SPACE
                        else -> OrderDestination.TAKE_AWAY
                    },
                space = space?.let { OrderSpace(id = 2, name = "Cancha 2", type = it) },
                subtotal = "10.00",
                combinedSavings = "0.00",
                cashbackAwarded = "0.00",
                total = "10.00",
                version = 1,
                createdAt = "2026-11-02T00:00:00Z",
                updatedAt = "2026-11-02T00:00:00Z",
                deliveryDate = date,
            ),
        user = null,
        kitchenNotes = "",
        items = emptyList(),
        pickupToken = "VN-42",
    )
}
