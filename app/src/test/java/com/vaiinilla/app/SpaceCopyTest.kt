package com.vaiinilla.app

import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderSpace
import com.vaiinilla.app.domain.model.SpaceCopy
import org.junit.Assert.assertEquals
import org.junit.Test

class SpaceCopyTest {
    @Test
    fun `sin tipo o con mesa conserva el texto de siempre`() {
        assertEquals("tu mesa", SpaceCopy.yourPlace(null))
        assertEquals("tu mesa", SpaceCopy.yourPlace("mesa"))
        assertEquals("Comer aquí", SpaceCopy.destinationTitle(null))
        assertEquals("Comer aquí", SpaceCopy.destinationTitle("mesa"))
        assertEquals("Te lo llevamos a tu espacio", SpaceCopy.destinationSubtitle(null))
        assertEquals("Selecciona tu mesa", SpaceCopy.pickPrompt(null))
    }

    @Test
    fun `una cancha se nombra como cancha`() {
        assertEquals("tu cancha", SpaceCopy.yourPlace("cancha"))
        assertEquals("En la cancha", SpaceCopy.destinationTitle("cancha"))
        assertEquals("Te lo llevamos a tu cancha", SpaceCopy.destinationSubtitle("cancha"))
        assertEquals("Selecciona tu cancha", SpaceCopy.pickPrompt("cancha"))
    }

    @Test
    fun `barra y drive-thru tienen su propio vocabulario`() {
        assertEquals("la barra", SpaceCopy.yourPlace("barra"))
        assertEquals("tu auto", SpaceCopy.yourPlace("drive_thru"))
        assertEquals("tu espacio", SpaceCopy.yourPlace("otro"))
    }

    @Test
    fun `cocina y caja ven el nombre del espacio`() {
        assertEquals("Para llevar", SpaceCopy.staffDestination(OrderDestination.TAKE_AWAY, null))
        assertEquals(
            "Comer aquí · Mesa 1",
            SpaceCopy.staffDestination(OrderDestination.IN_SPACE, OrderSpace(3, "Mesa 1", "mesa")),
        )
        assertEquals(
            "En la cancha · Cancha 2",
            SpaceCopy.staffDestination(OrderDestination.IN_SPACE, OrderSpace(9, "Cancha 2", "cancha")),
        )
        assertEquals("Comer aquí", SpaceCopy.staffDestination(OrderDestination.IN_SPACE, null))
    }
}
