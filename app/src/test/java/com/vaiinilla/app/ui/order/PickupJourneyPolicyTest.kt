package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderReservation
import com.vaiinilla.app.domain.model.OrderSpace
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.ReservationState
import com.vaiinilla.app.ui.components.needsPickupQr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class PickupJourneyPolicyTest {
    @Test
    fun stepsFollowTheKitchen() {
        assertEquals(PickupStep.ORDERED, pickupStep(OrderState.PAID))
        assertEquals(PickupStep.MAKING, pickupStep(OrderState.PREPARING))
        assertEquals(PickupStep.READY, pickupStep(OrderState.READY))
        assertNull(pickupStep(OrderState.CANCELED))
    }

    @Test
    fun codeOpensByItselfOnlyWhenItMustBeShown() {
        assertTrue(shouldAutoRevealPickupCode(order(OrderState.READY), hasCode = true))
        assertTrue(shouldAutoRevealPickupCode(order(OrderState.PENDING_PAYMENT, PaymentMethod.CASH), hasCode = true))
        assertFalse(shouldAutoRevealPickupCode(order(OrderState.PREPARING), hasCode = true))
        assertFalse(shouldAutoRevealPickupCode(order(OrderState.READY), hasCode = false))
        assertFalse(shouldAutoRevealPickupCode(order(OrderState.READY, toSpace = true), hasCode = true))
    }

    @Test
    fun codeButtonOnlyWhileTheOrderIsOpen() {
        assertTrue(pickupCodeUseful(order(OrderState.PREPARING), deliveryRequiresQr = false))
        assertFalse(pickupCodeUseful(order(OrderState.DELIVERED), deliveryRequiresQr = true))
        assertFalse(pickupCodeUseful(order(OrderState.CANCELED), deliveryRequiresQr = true))
        assertFalse(pickupCodeUseful(order(OrderState.READY, toSpace = true), deliveryRequiresQr = false))
        assertTrue(pickupCodeUseful(order(OrderState.READY, toSpace = true), deliveryRequiresQr = true))
        assertTrue(
            pickupCodeUseful(
                order(OrderState.PENDING_PAYMENT, PaymentMethod.CASH, toSpace = true),
                deliveryRequiresQr = false,
            ),
        )
    }

    @Test
    fun anOpenAccountOrderNeedsNoCode() {
        // `entrega_sin_qr`: el backend dispensa el QR de un pedido a una cuenta abierta, aunque el negocio lo pida.
        val open = order(OrderState.READY, toSpace = true, withoutQr = true)
        assertFalse(pickupCodeUseful(open, deliveryRequiresQr = true))
        assertFalse(open.needsPickupQr(deliveryRequiresQr = true))
        assertTrue(order(OrderState.READY, toSpace = true).needsPickupQr(deliveryRequiresQr = true))
        // Si falta pagar en Caja, el código sigue sirviendo para pagar.
        assertTrue(
            pickupCodeUseful(
                order(OrderState.PENDING_PAYMENT, PaymentMethod.CASH, toSpace = true, withoutQr = true),
                deliveryRequiresQr = true,
            ),
        )
    }

    @Test
    fun copyMatchesThePwa() {
        assertEquals("¡Listo!", pickupTitle(order(OrderState.READY)))
        assertEquals("Recógelo en la barra.", pickupHint(order(OrderState.READY)))
        assertEquals("Por pagar", pickupTitle(order(OrderState.PENDING_PAYMENT, PaymentMethod.CASH)))
        assertEquals("¡Listo! Te lo llevan a Mesa 4.", pickupHint(order(OrderState.READY, toSpace = true)))
        assertEquals(
            "Tu mesero te lo lleva. Si te pide el código, muéstrale este.",
            pickupCodeHint(order(OrderState.READY, toSpace = true)),
        )
    }

    @Test
    fun everyMosaicBlockSettlesBeforeTheEnd() {
        val size = 29
        for (row in 0 until size) {
            for (col in 0 until size) {
                val start = mosaicCellStart(col, row, size)
                assertTrue(start >= MOSAIC_FLIP_FROM)
                assertTrue(start + MOSAIC_FLIP_LENGTH <= 0.88f)
            }
        }
        val center = mosaicCellStart(14, 14, size)
        val corner = mosaicCellStart(0, 0, size)
        assertTrue(center < corner)
    }

    @Test
    fun eachOrderIsFollowedInItsPlace() {
        assertEquals(PickupPlace.STOREFRONT, pickupPlace(order(OrderState.PAID)))
        assertEquals(PickupPlace.KITCHEN, pickupPlace(order(OrderState.PREPARING)))
        assertEquals(PickupPlace.STOREFRONT, pickupPlace(order(OrderState.READY)))
        assertEquals(PickupPlace.TABLE, pickupPlace(order(OrderState.PREPARING, toSpace = true)))
        val toCourt = order(OrderState.PREPARING, toSpace = true)
        val courtSpace = toCourt.summary.copy(space = OrderSpace(id = 9, name = "Cancha 1", type = "cancha"))
        assertEquals(PickupPlace.KITCHEN, pickupPlace(toCourt.copy(summary = courtSpace)))
        val start = Instant.parse("2026-10-14T18:00:00Z")
        val rental =
            order(OrderState.DELIVERED).copy(
                reservation =
                    OrderReservation(
                        id = "r",
                        space = null,
                        start = start,
                        end = start.plusSeconds(3600),
                        durationMinutes = 60,
                        state = ReservationState.CONFIRMED,
                    ),
            )
        assertEquals(PickupPlace.COURT, pickupPlace(rental))
    }

    @Test
    fun yourTableIsTheOneNamedInTheSpace() {
        assertEquals(3, floorTableIndex("Mesa 4", 99))
        assertEquals(0, floorTableIndex("Mesa 1", 99))
        assertEquals(1, floorTableIndex("Mesa 8", 99))
        assertEquals(2, floorTableIndex("Terraza", 14))
        assertEquals(5, floorTableIndex("Mesa 0", 5))
    }

    @Test
    fun courtLinesFollowTheTimePlayedAndTheLightsDimBeforeTheEnd() {
        val start = Instant.ofEpochSecond(0)
        val end = start.plusSeconds(60 * 60)
        assertEquals(CourtLights(0.0, 0.3), courtLights(start, end, start.minusSeconds(60)))
        assertEquals(CourtLights(0.5, 1.0), courtLights(start, end, start.plusSeconds(30 * 60)))
        val grace = courtLights(start, end, end.minusSeconds(4 * 60))
        assertEquals(0.4, grace.lights, 0.0)
        assertEquals(56.0 / 60, grace.lines, 1e-9)
        assertEquals(CourtLights(1.0, 0.0), courtLights(start, end, end))
        assertEquals(CourtLights(1.0, 0.0), courtLights(end, start, start))
    }

    @Test
    fun aRentalIsToldByTheCourtNotByTheOrderStatus() {
        val start = Instant.ofEpochSecond(1_000_000)
        val end = start.plusSeconds(3600)
        val rental =
            order(OrderState.DELIVERED).copy(
                reservation =
                    OrderReservation(
                        id = "r",
                        space = OrderSpace(id = 9, name = "Cancha 2", type = "cancha"),
                        start = start,
                        end = end,
                        durationMinutes = 60,
                        state = ReservationState.CONFIRMED,
                    ),
            )
        assertEquals(CourtMoment.BOOKED, courtMoment(rental, start.minusSeconds(60)))
        assertEquals("En juego", pickupTitle(rental, start.plusSeconds(20 * 60)))
        assertEquals("Te quedan 40 min en Cancha 2.", pickupHint(rental, start.plusSeconds(20 * 60)))
        assertEquals("Últimos minutos", pickupTitle(rental, end.minusSeconds(4 * 60 + 30)))
        assertEquals("Quedan 5 min: los reflectores ya bajaron.", pickupHint(rental, end.minusSeconds(4 * 60 + 30)))
        assertEquals("Terminada", pickupTitle(rental, end))
        assertNull(courtMoment(order(OrderState.PREPARING), start))
        assertEquals(1, courtMinutesLeft(end, end.minusSeconds(30)))
    }

    private fun order(
        state: OrderState,
        payment: PaymentMethod = PaymentMethod.BALANCE,
        toSpace: Boolean = false,
        withoutQr: Boolean = false,
    ) = OrderDetail(
        summary =
            OrderSummary(
                id = "1",
                folio = 42,
                operationalDate = "2026-10-11",
                state = state,
                paymentMethod = payment,
                destination = if (toSpace) OrderDestination.IN_SPACE else OrderDestination.TAKE_AWAY,
                space = if (toSpace) OrderSpace(id = 4, name = "Mesa 4", type = "mesa") else null,
                subtotal = "10.00",
                combinedSavings = "0.00",
                cashbackAwarded = "0.00",
                total = "10.00",
                version = 1,
                createdAt = "2026-10-11T00:00:00Z",
                updatedAt = "2026-10-11T00:00:00Z",
                deliveryWithoutQr = withoutQr,
            ),
        user = null,
        kitchenNotes = "",
        items = emptyList(),
        pickupToken = "VN-42",
    )
}
