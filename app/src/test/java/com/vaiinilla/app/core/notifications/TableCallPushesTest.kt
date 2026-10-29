package com.vaiinilla.app.core.notifications

import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TableCallPushesTest {
    @Test
    fun `el push de va en camino adelanta la consulta de la llamada`() =
        runTest {
            val woke = async { TableCallPushes.awaitNextPoll(5_000).let { currentTime } }
            runCurrent()
            advanceTimeBy(1_000)
            TableCallPushes.notifyChanged()
            assertEquals(1_000L, woke.await())
        }

    @Test
    fun `sin push la consulta espera su intervalo`() =
        runTest {
            TableCallPushes.awaitNextPoll(5_000)
            assertEquals(5_000L, currentTime)
        }
}
