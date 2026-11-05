package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.SpaceDelivery
import com.vaiinilla.app.ui.components.trackingStepDescription
import com.vaiinilla.app.ui.components.trackingStepTitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpaceDeliveryTest {
    private fun state(
        delivery: SpaceDelivery,
        chosen: Boolean = false,
        destination: OrderDestination = OrderDestination.IN_SPACE,
    ) = OrderFlowUiState(
        operationalStatus =
            OperationalStatus(
                acceptingOrders = true,
                cashSessionOpen = true,
                cashierOnline = true,
                kitchenOnline = true,
                estimatedTimeMinutes = 10,
                consultedAt = "2026-11-04T20:00:00.000Z",
                spaceDelivery = delivery,
            ),
        checkoutDestination = destination,
        checkoutPickUpAtBar = chosen,
    )

    @Test
    fun `an unknown or missing value keeps the waiter flow`() {
        assertEquals(SpaceDelivery.TABLE, SpaceDelivery.fromWire(null))
        assertEquals(SpaceDelivery.TABLE, SpaceDelivery.fromWire("otra"))
        assertEquals(SpaceDelivery.BOTH, SpaceDelivery.fromWire("ambas"))
    }

    @Test
    fun `only a business with both options asks the customer, and only at a table`() {
        assertTrue(state(SpaceDelivery.BOTH).asksSpaceDelivery)
        assertFalse(state(SpaceDelivery.BAR).asksSpaceDelivery)
        assertFalse(state(SpaceDelivery.BOTH, destination = OrderDestination.TAKE_AWAY).asksSpaceDelivery)
    }

    @Test
    fun `what travels with the order follows the business and then the customer`() {
        assertTrue(state(SpaceDelivery.BAR).pickUpAtBarForOrder)
        assertFalse(state(SpaceDelivery.TABLE, chosen = true).pickUpAtBarForOrder)
        assertTrue(state(SpaceDelivery.BOTH, chosen = true).pickUpAtBarForOrder)
        assertFalse(state(SpaceDelivery.BOTH, chosen = false).pickUpAtBarForOrder)
        assertFalse(
            state(SpaceDelivery.BAR, destination = OrderDestination.TAKE_AWAY).pickUpAtBarForOrder,
        )
    }

    @Test
    fun `ready at a table says on the way or come to the bar`() {
        val inSpace = OrderDestination.IN_SPACE
        assertEquals(
            "EN CAMINO",
            trackingStepTitle(OrderState.READY, PaymentMethod.CASH, null, false, inSpace, pickUpAtBar = false),
        )
        assertEquals(
            "LISTO, PASA A LA BARRA",
            trackingStepTitle(OrderState.READY, PaymentMethod.CASH, null, false, inSpace, pickUpAtBar = true),
        )
        assertEquals(
            "LISTO",
            trackingStepTitle(OrderState.READY, PaymentMethod.CASH, null),
        )
        assertEquals(
            "Tu pedido está listo, pasa a la barra.",
            trackingStepDescription(OrderState.READY, inSpace, PaymentMethod.CASH, null, pickUpAtBar = true),
        )
    }
}
