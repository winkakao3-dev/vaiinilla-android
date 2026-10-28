package com.vaiinilla.app.ui.operational

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** EQUIPO-21: a la vista el personal consulta a su ritmo de siempre; minimizada, cada minuto. */
class StaffPollCadenceTest {
    @Test
    fun foregroundKeepsItsIntervalAndBackgroundWaitsAMinute() {
        assertEquals(5_000L, StaffPollCadence.intervalMs(foreground = true, foregroundIntervalMs = 5_000L))
        assertEquals(15_000L, StaffPollCadence.intervalMs(foreground = true, foregroundIntervalMs = 15_000L))
        assertEquals(60_000L, StaffPollCadence.intervalMs(foreground = false, foregroundIntervalMs = 5_000L))
        assertEquals(60_000L, StaffPollCadence.intervalMs(foreground = false, foregroundIntervalMs = 15_000L))
    }

    @Test
    fun backgroundWaitsTheWholeBackgroundInterval() =
        runBlocking {
            val app = FakeApp(foreground = false)
            val started = System.nanoTime()
            awaitNextStaffPoll(foregroundIntervalMs = 5L, app = app, backgroundIntervalMs = 200L)
            val waitedMs = (System.nanoTime() - started) / 1_000_000
            assertTrue("esperó $waitedMs ms", waitedMs >= 190)
        }

    @Test
    fun comingBackToForegroundPollsRightAway() =
        runBlocking {
            val app = FakeApp(foreground = false)
            app.back.complete(Unit)
            val started = System.nanoTime()
            awaitNextStaffPoll(foregroundIntervalMs = 5L, app = app, backgroundIntervalMs = 10_000L)
            val waitedMs = (System.nanoTime() - started) / 1_000_000
            assertTrue("esperó $waitedMs ms", waitedMs < 1_000)
        }

    private class FakeApp(
        private val foreground: Boolean,
    ) : AppVisibility {
        val back = CompletableDeferred<Unit>()

        override fun isForeground(): Boolean = foreground

        override suspend fun awaitForeground() = back.await()
    }
}
