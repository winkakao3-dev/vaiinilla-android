package com.vaiinilla.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StaffShiftTest {
    @Test
    fun `la semana empieza en lunes`() {
        assertEquals(0, weekdayIndex(LocalDate.parse("2026-11-02")))
        assertEquals(1, weekdayIndex(LocalDate.parse("2026-11-03")))
        assertEquals(6, weekdayIndex(LocalDate.parse("2026-11-08")))
    }
}
