package com.vaiinilla.app.core.error

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalCrashReporterInstallerTest {
    private val originalHandler = Thread.getDefaultUncaughtExceptionHandler()

    @After
    fun restoreHandler() {
        Thread.setDefaultUncaughtExceptionHandler(originalHandler)
    }

    @Test
    fun `reports the exception then delegates to the previous handler`() {
        val reported = mutableListOf<Throwable>()
        val delegated = mutableListOf<Throwable>()
        val previousHandler =
            Thread.UncaughtExceptionHandler { _, throwable -> delegated += throwable }

        GlobalCrashReporterInstaller(reporter = { reported += it })
            .install(previousHandler)

        val boom = RuntimeException("boom")
        Thread.getDefaultUncaughtExceptionHandler()!!.uncaughtException(Thread.currentThread(), boom)

        assertEquals(listOf(boom), reported)
        assertEquals(listOf(boom), delegated)
    }

    @Test
    fun `a reporter failure does not stop delegation to the previous handler`() {
        val delegated = mutableListOf<Throwable>()
        val previousHandler =
            Thread.UncaughtExceptionHandler { _, throwable -> delegated += throwable }

        GlobalCrashReporterInstaller(reporter = { throw IllegalStateException("reporter down") })
            .install(previousHandler)

        val boom = RuntimeException("boom")
        Thread.getDefaultUncaughtExceptionHandler()!!.uncaughtException(Thread.currentThread(), boom)

        assertEquals(listOf(boom), delegated)
    }

    @Test
    fun `replaces the default handler instead of appending to it`() {
        val previousHandler = Thread.UncaughtExceptionHandler { _, _ -> }

        GlobalCrashReporterInstaller(reporter = {})
            .install(previousHandler)

        assertTrue(
            "install() must swap in its own handler, not leave the previous one active",
            Thread.getDefaultUncaughtExceptionHandler() !== previousHandler,
        )
    }
}
