package com.vaiinilla.app.counter

import com.vaiinilla.app.domain.counter.ReservedCalendar
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Los días de "Para mañana" en Cocina: hoy y los siguientes días de escuela. */
class ReservedCalendarTest {
    @Test
    fun `un viernes salta el fin de semana y nombra los dias`() {
        val friday = LocalDate.parse("2026-10-23")
        val dates = ReservedCalendar.dates(friday)
        assertEquals(listOf("2026-10-23", "2026-10-26", "2026-10-27", "2026-10-28", "2026-10-29"), dates)
        assertEquals("Hoy", ReservedCalendar.label("2026-10-23", friday))
        assertEquals("Lunes 26", ReservedCalendar.label("2026-10-26", friday))
        assertEquals("Mañana", ReservedCalendar.label("2026-10-24", friday))
    }
}
