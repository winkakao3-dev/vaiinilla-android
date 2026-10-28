package com.vaiinilla.app.domain

import com.vaiinilla.app.domain.time.BusinessTime
import com.vaiinilla.app.ui.screens.receiptLocalTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.TimeZone

/** EQUIPO-23: la misma fecha UTC sale con la hora del negocio aunque el teléfono esté en otra zona. */
class BusinessTimeTest {
    private lateinit var phoneZone: TimeZone

    @Before
    fun phoneInTokyo() {
        phoneZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
    }

    @After
    fun restorePhoneZone() {
        TimeZone.setDefault(phoneZone)
    }

    @Test
    fun defaultZoneIsChihuahua() {
        assertEquals(ZoneId.of("America/Chihuahua"), BusinessTime.zone)
    }

    @Test
    fun sameUtcInstantShowsBusinessHourNotPhoneHour() {
        val instant = Instant.parse("2026-10-27T19:05:00Z")
        val hour = DateTimeFormatter.ofPattern("HH:mm")

        assertEquals("13:05", BusinessTime.format(instant, hour))
        assertEquals("13:05", receiptLocalTime("2026-10-27T19:05:00Z"))
    }

    @Test
    fun serverZoneWinsAndBadZoneFallsBackToBusiness() {
        assertEquals(ZoneId.of("America/Mexico_City"), BusinessTime.zoneOr("America/Mexico_City"))
        assertEquals(BusinessTime.zone, BusinessTime.zoneOr("no/zona"))
        assertEquals(BusinessTime.zone, BusinessTime.zoneOr(null))
    }
}
