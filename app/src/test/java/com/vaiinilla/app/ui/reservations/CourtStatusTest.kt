package com.vaiinilla.app.ui.reservations

import com.vaiinilla.app.domain.model.BusyInterval
import com.vaiinilla.app.domain.model.CourtDay
import com.vaiinilla.app.domain.model.CourtSchedule
import com.vaiinilla.app.domain.model.ReservationSlots
import com.vaiinilla.app.domain.model.VenueContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** En vivo, #16: una pista ocupada decía «Fuera de horario» y un día cerrado lo decía en cada pista. */
class CourtStatusTest {
    private val venue = VenueContext.STANDARD

    // Hoy de 08:00 a 22:00 local (UTC-6).
    private val opens = Instant.parse("2026-11-05T14:00:00Z")
    private val closes = Instant.parse("2026-11-06T04:00:00Z")

    private fun court(vararg busy: Pair<String, String>) =
        CourtSchedule(
            id = 3,
            name = "Pista 3",
            pricePerHour = "300.00",
            customerPricePerHour = null,
            rentable = true,
            busy = busy.map { BusyInterval(Instant.parse(it.first), Instant.parse(it.second), "turno") },
        )

    private fun day(
        now: String,
        date: String = "2026-11-05",
        closed: Boolean = false,
    ) = CourtDay(
        date = date,
        today = "2026-11-05",
        timeZone = "America/Mexico_City",
        opensAt = opens,
        closesAt = closes,
        blockMinutes = 30,
        durations = listOf(60),
        daysAhead = 14,
        holdMinutes = 10,
        now = Instant.parse(now),
        courts = emptyList(),
        closed = closed,
    )

    private fun status(
        day: CourtDay,
        court: CourtSchedule,
    ) = courtStatus(day, court, freeNow = ReservationSlots.canRentNow(day, court))

    @Test
    fun `a lane in use says busy`() {
        val busy = court("2026-11-05T17:00:00Z" to "2026-11-05T19:00:00Z")
        assertEquals(CourtStatus.BUSY, status(day("2026-11-05T18:00:00Z"), busy))
    }

    @Test
    fun `a lane reserved in a few minutes says it gets busy soon, not out of hours`() {
        val soon = court("2026-11-05T18:20:00Z" to "2026-11-05T19:20:00Z")
        assertEquals(CourtStatus.BUSY_SOON, status(day("2026-11-05T18:00:00Z"), soon))
    }

    @Test
    fun `free now, before opening and after closing`() {
        assertEquals(CourtStatus.FREE_NOW, status(day("2026-11-05T18:00:00Z"), court()))
        assertEquals(CourtStatus.OPENS_LATER, status(day("2026-11-05T12:00:00Z"), court()))
        assertEquals(CourtStatus.CLOSED_FOR_TODAY, status(day("2026-11-06T04:30:00Z"), court()))
    }

    @Test
    fun `in hours without room before closing says it closes soon`() {
        assertEquals(CourtStatus.CLOSING_SOON, status(day("2026-11-06T03:30:00Z"), court()))
    }

    @Test
    fun `another day keeps its labels`() {
        val other = day("2026-11-05T18:00:00Z", date = "2026-11-06")
        assertEquals(CourtStatus.FREE_ALL_DAY, status(other, court()))
    }

    @Test
    fun `a closed day is said once for the place`() {
        assertTrue(closedDayMessage(venue, isToday = true).startsWith("Hoy el lugar no abre"))
        assertTrue(closedDayMessage(venue, isToday = false).startsWith("Ese día el lugar no abre"))
    }
}
