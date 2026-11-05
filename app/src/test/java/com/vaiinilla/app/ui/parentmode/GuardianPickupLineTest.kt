package com.vaiinilla.app.ui.parentmode

import org.junit.Assert.assertEquals
import org.junit.Test

class GuardianPickupLineTest {
    @Test
    fun `the guardian sees when a reserved order is picked up`() {
        // P08: sin franja no decía cuándo se recoge.
        assertEquals("Se recoge el viernes 6", guardianPickupLine("Viernes 6", null))
        assertEquals("Se recoge mañana a las 12:00", guardianPickupLine("Mañana", "12:00-12:30"))
        assertEquals("Se recoge hoy", guardianPickupLine("Hoy", ""))
    }
}
