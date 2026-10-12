package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderSpace
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
            pickupCodeUseful(order(OrderState.PENDING_PAYMENT, PaymentMethod.CASH, toSpace = true), deliveryRequiresQr = false),
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
