package com.vaiinilla.app.domain.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZonedDateTime

class CashbackPreviewTest {
    private val zone = ZoneId.of("America/Mexico_City")

    // Jueves 5 de noviembre de 2026, 17:30.
    private val thursdayAfternoon = ZonedDateTime.of(2026, 11, 5, 17, 30, 0, 0, zone)

    @Test
    fun `an active rule says what this order earns`() {
        assertEquals(BigDecimal("4.60"), CashbackPreview.earned(rule(), "460.00", thursdayAfternoon))
    }

    @Test
    fun `cashback turned off says nothing`() {
        assertNull(CashbackPreview.earned(rule(active = false), "460.00", thursdayAfternoon))
        assertNull(CashbackPreview.earned(null, "460.00", thursdayAfternoon))
        assertNull(CashbackPreview.earned(rule(percent = "0.00"), "460.00", thursdayAfternoon))
    }

    @Test
    fun `outside its days, hours or dates the rule does not apply`() {
        assertNull(CashbackPreview.percentAt(rule(days = listOf(1, 2)), thursdayAfternoon))
        assertEquals(BigDecimal("1.00"), CashbackPreview.percentAt(rule(days = listOf(4)), thursdayAfternoon))
        assertNull(CashbackPreview.percentAt(rule(start = "08:00", end = "12:00"), thursdayAfternoon))
        assertEquals(
            BigDecimal("1.00"),
            CashbackPreview.percentAt(rule(start = "16:00", end = "02:00"), thursdayAfternoon),
        )
        assertNull(CashbackPreview.percentAt(rule(validTo = "2026-11-04"), thursdayAfternoon))
    }

    private fun rule(
        active: Boolean = true,
        percent: String = "1.00",
        days: List<Int>? = null,
        start: String? = null,
        end: String? = null,
        validTo: String? = null,
    ) = CashbackRule(
        name = "Regla principal",
        percent = percent,
        startTime = start,
        endTime = end,
        activeDays = days,
        validFrom = null,
        validTo = validTo,
        active = active,
    )
}
