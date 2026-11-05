package com.vaiinilla.app.ui.screens

import com.vaiinilla.app.domain.model.SpaceDelivery
import org.junit.Assert.assertEquals
import org.junit.Test

class CourtMenuDeliveryLineTest {
    @Test
    fun `a la cancha promete llevarlo`() {
        assertEquals("Te lo llevan a la Pista 1", courtMenuDeliveryLine(SpaceDelivery.TABLE, "la Pista 1"))
    }

    @Test
    fun `solo barra no promete llevarlo`() {
        assertEquals("Pides aquí y pasas por él a la barra", courtMenuDeliveryLine(SpaceDelivery.BAR, "la Pista 1"))
    }

    @Test
    fun `ambas ofrece las dos`() {
        assertEquals("Te lo llevan a la Pista 1 o pasas por él", courtMenuDeliveryLine(SpaceDelivery.BOTH, "la Pista 1"))
    }
}
