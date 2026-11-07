package com.vaiinilla.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class RelativeAgeTest {
    private val now = Instant.parse("2026-11-06T18:00:00Z")

    @Test
    fun `lo corto dice ahora, minutos u horas`() {
        assertEquals("ahora", RelativeAge.short(59_999))
        assertEquals("1 min", RelativeAge.short(60_000))
        assertEquals("59 min", RelativeAge.short(3_599_999))
        assertEquals("1 h", RelativeAge.short(3_600_000))
        assertEquals("30 h", RelativeAge.short(30 * 3_600_000L))
    }

    @Test
    fun `desde un texto de fecha`() {
        assertEquals("5 min", RelativeAge.short("2026-11-06T17:55:00Z", now))
        assertEquals("ahora", RelativeAge.short("no es fecha", now))
        assertNull(RelativeAge.elapsedMs("no es fecha", now))
        assertEquals(300_000L, RelativeAge.elapsedMs("2026-11-06T17:55:00Z", now))
    }

    @Test
    fun `shortAgo agrega hace salvo en ahora`() {
        assertEquals("hace 5 min", RelativeAge.shortAgo("2026-11-06T17:55:00Z", now))
        assertEquals("ahora", RelativeAge.shortAgo("2026-11-06T17:59:30Z", now))
    }

    @Test
    fun `ago sube de minutos a horas a dias`() {
        assertEquals("ahora", RelativeAge.ago(59_000))
        assertEquals("hace 5 min", RelativeAge.ago(5 * 60_000L))
        assertEquals("hace 2 h", RelativeAge.ago(2 * 3_600_000L))
        assertEquals("hace 1 d", RelativeAge.ago(86_400_000L))
        assertEquals("hace 6 d", RelativeAge.ago(6 * 86_400_000L + 5_000))
    }
}
