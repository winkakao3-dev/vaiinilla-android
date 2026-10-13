package com.vaiinilla.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class KitchenAgeTest {
    private fun minutes(value: Int) = value * 60_000L

    @Test
    fun `el color cambia a los 10 y a los 20 minutos`() {
        assertEquals(KitchenAge.FRESH, kitchenAge(minutes(9) + 59_000))
        assertEquals(KitchenAge.WAITING, kitchenAge(minutes(10)))
        assertEquals(KitchenAge.WAITING, kitchenAge(minutes(19) + 59_000))
        assertEquals(KitchenAge.LATE, kitchenAge(minutes(20)))
    }

    @Test
    fun `se lee en minutos y pasada una hora dice mas de una hora`() {
        assertEquals("0 min", kitchenAgeLabel(42_000))
        assertEquals("7 min", kitchenAgeLabel(minutes(7) + 30_000))
        assertEquals("59 min", kitchenAgeLabel(minutes(59) + 59_000))
        assertEquals("+1 h", kitchenAgeLabel(minutes(60)))
        assertEquals("+1 h", kitchenAgeLabel(minutes(1127)))
        assertEquals("0 min", kitchenAgeLabel(-5_000))
    }
}
