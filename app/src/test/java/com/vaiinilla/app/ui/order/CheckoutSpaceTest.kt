package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.GuestVenueContext
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.domain.model.PublicSpace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CheckoutSpaceTest {
    private val mesa = PublicSpace(id = 2, name = "Mesa 1", type = "mesa")
    private val cancha = PublicSpace(id = 4, name = "Cancha 1", type = "cancha")

    private fun state(
        rented: PublicSpace? = null,
        selected: Int = 0,
        destination: OrderDestination = OrderDestination.IN_SPACE,
    ) = OrderFlowUiState(
        guestVenue = GuestVenueContext(establishment = establishment(), space = mesa),
        rentedCourt = rented,
        selectedSpaceId = selected,
        checkoutDestination = destination,
    )

    @Test
    fun `sin cancha rentada el pedido va al espacio del QR`() {
        assertEquals(listOf(mesa), state().checkoutSpaces)
        assertEquals(2, state().checkoutSpaceId)
    }

    @Test
    fun `con cancha rentada en curso se ofrece y va primero aunque entro por la mesa`() {
        val s = state(rented = cancha)
        assertEquals(listOf(mesa, cancha), s.checkoutSpaces)
        assertEquals(4, s.checkoutSpaceId)
        assertEquals("Cancha 1", s.selectedSpaceName)
    }

    @Test
    fun `el cliente puede volver a elegir la mesa del QR`() {
        assertEquals(2, state(rented = cancha, selected = 2).checkoutSpaceId)
    }

    @Test
    fun `una eleccion que ya no existe no se cuela en el pedido`() {
        assertEquals(2, state(selected = 99).checkoutSpaceId)
    }

    @Test
    fun `para llevar no manda espacio`() {
        assertNull(state(rented = cancha, destination = OrderDestination.TAKE_AWAY).checkoutSpaceId)
    }

    private fun establishment(): PublicEstablishment =
        PublicEstablishment(
            id = "est-1",
            name = "padel prueba",
            slug = "padel",
            clientIdLabel = "Identificador",
            clientIdRequired = false,
        )
}
