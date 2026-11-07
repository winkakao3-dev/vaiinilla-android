package com.vaiinilla.app.core.error

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportingPolicyTest {
    @Test
    fun `auto solo reporta la app de produccion en release`() {
        assertTrue(CrashReportingPolicy.shouldCollect(CrashReportingMode.AUTO, isProduction = true, isDebug = false))
        assertFalse(CrashReportingPolicy.shouldCollect(CrashReportingMode.AUTO, isProduction = true, isDebug = true))
        assertFalse(CrashReportingPolicy.shouldCollect(CrashReportingMode.AUTO, isProduction = false, isDebug = false))
    }

    @Test
    fun `off apaga siempre y on enciende siempre`() {
        assertFalse(CrashReportingPolicy.shouldCollect(CrashReportingMode.OFF, isProduction = true, isDebug = false))
        assertTrue(CrashReportingPolicy.shouldCollect(CrashReportingMode.ON, isProduction = false, isDebug = true))
    }

    @Test
    fun `un valor raro cae en auto`() {
        assertEquals(CrashReportingMode.AUTO, CrashReportingMode.parse(null))
        assertEquals(CrashReportingMode.AUTO, CrashReportingMode.parse("quizas"))
        assertEquals(CrashReportingMode.OFF, CrashReportingMode.parse(" OFF "))
        assertEquals(CrashReportingMode.ON, CrashReportingMode.parse("true"))
    }
}
