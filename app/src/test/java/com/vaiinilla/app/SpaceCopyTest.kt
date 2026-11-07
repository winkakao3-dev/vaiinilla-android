package com.vaiinilla.app

import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderSpace
import com.vaiinilla.app.domain.model.SpaceCopy
import com.vaiinilla.app.domain.model.SpaceDelivery
import com.vaiinilla.app.domain.model.VenueContext
import org.junit.Assert.assertEquals
import org.junit.Test

class SpaceCopyTest {
    private val venue = VenueContext.STANDARD

    @Test
    fun `sin tipo o con mesa conserva el texto de siempre`() {
        assertEquals("tu mesa", SpaceCopy.yourPlace(venue, null))
        assertEquals("tu mesa", SpaceCopy.yourPlace(venue, "mesa"))
        assertEquals("Comer aquí", SpaceCopy.destinationTitle(venue, null))
        assertEquals("Comer aquí", SpaceCopy.destinationTitle(venue, "mesa"))
        assertEquals("Te lo llevamos a tu espacio", SpaceCopy.destinationSubtitle(venue, null))
        assertEquals("Selecciona tu mesa", SpaceCopy.pickPrompt(venue, null))
    }

    @Test
    fun `una cancha se nombra como cancha`() {
        assertEquals("tu cancha", SpaceCopy.yourPlace(venue, "cancha"))
        assertEquals("En la cancha", SpaceCopy.destinationTitle(venue, "cancha"))
        assertEquals("Te lo llevamos a tu cancha", SpaceCopy.destinationSubtitle(venue, "cancha"))
        assertEquals("Selecciona tu cancha", SpaceCopy.pickPrompt(venue, "cancha"))
    }

    @Test
    fun `barra y drive-thru tienen su propio vocabulario`() {
        assertEquals("la barra", SpaceCopy.yourPlace(venue, "barra"))
        assertEquals("tu auto", SpaceCopy.yourPlace(venue, "drive_thru"))
        assertEquals("tu espacio", SpaceCopy.yourPlace(venue, "otro"))
    }

    @Test
    fun `el asiento de un cine o estadio tiene su vocabulario`() {
        assertEquals("Asiento", SpaceCopy.kindLabel(venue, "asiento"))
        assertEquals("tu asiento", SpaceCopy.yourPlace(venue, "asiento"))
        assertEquals("Selecciona tu asiento", SpaceCopy.pickPrompt(venue, "asiento"))
        assertEquals(
            "En tu asiento · Asiento 14",
            SpaceCopy.staffDestination(venue, OrderDestination.IN_SPACE, OrderSpace(30, "Asiento 14", "asiento")),
        )
    }

    @Test
    fun `cocina y caja ven el nombre del espacio`() {
        assertEquals("Para llevar", SpaceCopy.staffDestination(venue, OrderDestination.TAKE_AWAY, null))
        assertEquals(
            "Comer aquí · Mesa 1",
            SpaceCopy.staffDestination(venue, OrderDestination.IN_SPACE, OrderSpace(3, "Mesa 1", "mesa")),
        )
        assertEquals(
            "En la cancha · Cancha 2",
            SpaceCopy.staffDestination(venue, OrderDestination.IN_SPACE, OrderSpace(9, "Cancha 2", "cancha")),
        )
        assertEquals("Comer aquí", SpaceCopy.staffDestination(venue, OrderDestination.IN_SPACE, null))
    }

    @Test
    fun `cocina y caja distinguen domicilio y pasa por el`() {
        assertEquals("A domicilio", SpaceCopy.staffDestination(venue, OrderDestination.HOME_DELIVERY, null))
        assertEquals(
            "Pasa por él a la barra · Pista 1",
            SpaceCopy.staffDestination(
                venue,
                OrderDestination.IN_SPACE,
                OrderSpace(9, "Pista 1", "cancha"),
                pickUpAtBar = true,
            ),
        )
        assertEquals("Pasa por él a la barra", SpaceCopy.staffDestination(venue, OrderDestination.IN_SPACE, null, true))
    }

    @Test
    fun `en la barra el carrito y el aviso de listo no dicen mesa ni llevar`() {
        assertEquals("Pagas en caja y te lo entregamos en la barra", SpaceCopy.payAtCashierHint(venue, "barra"))
        assertEquals("Pagas en caja y te lo llevamos a tu mesa", SpaceCopy.payAtCashierHint(venue, "mesa"))
        assertEquals("Pagas en caja y te lo llevamos a tu cancha", SpaceCopy.payAtCashierHint(venue, "cancha"))
        assertEquals("Tu pedido está listo. Te lo entregamos en la barra.", SpaceCopy.readyInSpace(venue, "barra"))
        assertEquals("Tu pedido está listo. En un momento te lo llevan a tu mesa.", SpaceCopy.readyInSpace(venue, null))
        assertEquals("Ya está en tu mesa. Buen provecho.", SpaceCopy.deliveredInSpace(venue, "mesa"))
        assertEquals("Pedido entregado. Buen provecho.", SpaceCopy.deliveredInSpace(venue, "barra"))
    }

    @Test
    fun `si el negocio solo entrega en barra el carrito no promete llevarlo`() {
        assertEquals(
            "Pides desde tu lugar y pasas por él a la barra",
            SpaceCopy.destinationSubtitle(venue, "cancha", SpaceDelivery.BAR),
        )
        assertEquals("Te lo llevamos a tu cancha", SpaceCopy.destinationSubtitle(venue, "cancha", SpaceDelivery.BOTH))
        assertEquals("Te lo llevamos a tu cancha", SpaceCopy.destinationSubtitle(venue, "cancha", SpaceDelivery.TABLE))
    }
}
