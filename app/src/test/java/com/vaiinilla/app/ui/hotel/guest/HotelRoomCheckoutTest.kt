package com.vaiinilla.app.ui.hotel.guest

import com.vaiinilla.app.domain.hotel.HotelConfig
import com.vaiinilla.app.domain.model.GuestVenueContext
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.domain.model.PublicSpace
import com.vaiinilla.app.domain.model.Venue
import com.vaiinilla.app.ui.order.OrderFlowUiState
import com.vaiinilla.app.ui.order.canPayAtEnd
import com.vaiinilla.app.ui.order.isHotelRoomOrder
import com.vaiinilla.app.ui.screens.paymentActionLabel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** El pedido a una habitación: «pagar al final» es el cargo a la habitación y lo enciende el hotel, no la mesa. */
class HotelRoomCheckoutTest {
    @After
    fun tearDown() = Venue.update(null)

    private fun state(
        type: String,
        config: HotelConfig?,
        isRoom: Boolean? = null,
    ) = OrderFlowUiState(
        checkoutDestination = OrderDestination.IN_SPACE,
        guestVenue =
            GuestVenueContext(
                establishment =
                    PublicEstablishment(
                        id = "e1",
                        name = "Hotel Mirador del Valle",
                        slug = "mirador",
                        clientIdLabel = "",
                        clientIdRequired = false,
                        allowsPayAtEnd = false,
                    ),
                space = PublicSpace(id = 304, name = "Habitación 304", type = type, isRoom = isRoom),
            ),
        hotelConfig = config,
    )

    @Test
    fun `en un hotel el cargo a la habitacion depende de la config y no de la mesa`() {
        Venue.update(Venue.HOTEL)
        assertTrue(state("mesa", HotelConfig(chargeToRoom = true)).isHotelRoomOrder)
        assertTrue(state("mesa", HotelConfig(chargeToRoom = true)).canPayAtEnd)
        assertFalse(state("mesa", HotelConfig(chargeToRoom = false)).canPayAtEnd)
    }

    @Test
    fun `una mesa del restaurante del hotel no es una habitacion aunque el negocio sea hotel`() {
        Venue.update(Venue.HOTEL)
        assertTrue(state("mesa", HotelConfig(chargeToRoom = true), isRoom = true).isHotelRoomOrder)
        val restaurant = state("mesa", HotelConfig(chargeToRoom = true), isRoom = false)
        assertFalse(restaurant.isHotelRoomOrder)
        assertFalse(restaurant.canPayAtEnd)
    }

    @Test
    fun `fuera de un hotel nada cambia`() {
        Venue.update("cafeteria")
        val state = state("mesa", HotelConfig(chargeToRoom = true))
        assertFalse(state.isHotelRoomOrder)
        assertFalse(state.canPayAtEnd)
    }

    @Test
    fun `el boton dice que se carga a la habitacion`() {
        assertEquals("Pedir · $236.50 a mi habitación", paymentActionLabel(null, true, "$236.50", toRoom = true))
        assertEquals("Pedir · pagas al final", paymentActionLabel(null, true, "$236.50"))
        assertEquals("Pagar $236.50", paymentActionLabel(PaymentMethod.STRIPE, false, "$236.50", toRoom = true))
    }
}
