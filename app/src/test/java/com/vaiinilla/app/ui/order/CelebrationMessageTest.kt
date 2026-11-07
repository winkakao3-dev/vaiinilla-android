package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.VenueContext
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderSpace
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Test

class CelebrationMessageTest {
    private val venue = VenueContext.STANDARD

    @Test
    fun `un pedido a la cuenta no manda al cliente a Caja`() {
        assertEquals(
            "Va a tu cuenta: la pagas al final.",
            celebrationMessage(venue, order(payAtEnd = true), PurchaseCelebrationKind.ORDER_RECEIVED),
        )
        assertEquals(
            "Paga en Caja para continuar con tu pedido.",
            celebrationMessage(venue, order(payAtEnd = false), PurchaseCelebrationKind.ORDER_RECEIVED),
        )
        assertEquals(
            "Tu pago fue autorizado y tu pedido ya está en marcha.",
            celebrationMessage(venue, order(payAtEnd = false), PurchaseCelebrationKind.PAYMENT_CONFIRMED),
        )
    }

    @Test
    fun `en un hotel una mesa del restaurante no se carga a la habitacion`() {
        val hotel = VenueContext.of("hotel")
        assertEquals(
            "Se carga a tu habitación: la pagas al salir.",
            celebrationMessage(hotel, order(payAtEnd = true), PurchaseCelebrationKind.ORDER_RECEIVED),
        )
        val withRestaurantTable = hotel.copy(hotelTables = setOf(4))
        assertEquals(
            "Va a tu cuenta: la pagas al final.",
            celebrationMessage(withRestaurantTable, order(payAtEnd = true), PurchaseCelebrationKind.ORDER_RECEIVED),
        )
    }

    private fun order(payAtEnd: Boolean) =
        OrderDetail(
            summary =
                OrderSummary(
                    id = "1",
                    folio = 42,
                    operationalDate = "2026-10-17",
                    state = OrderState.PENDING_PAYMENT,
                    paymentMethod = PaymentMethod.CASH,
                    destination = OrderDestination.IN_SPACE,
                    space = OrderSpace(id = 4, name = "Mesa 4", type = "mesa"),
                    subtotal = "10.00",
                    combinedSavings = "0.00",
                    cashbackAwarded = "0.00",
                    total = "10.00",
                    version = 1,
                    createdAt = "2026-10-17T00:00:00Z",
                    updatedAt = "2026-10-17T00:00:00Z",
                    payAtEnd = payAtEnd,
                    paymentPending = payAtEnd,
                ),
            user = null,
            kitchenNotes = "",
            items = emptyList(),
        )
}
