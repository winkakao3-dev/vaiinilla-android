package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderReservation
import com.vaiinilla.app.domain.model.OrderSpace
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.ReservationState
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

    private fun order(
        state: OrderState,
        payment: PaymentMethod = PaymentMethod.BALANCE,
        toSpace: Boolean = false,
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
            ),
        user = null,
        kitchenNotes = "",
        items = emptyList(),
        pickupToken = "VN-42",
    )
}
