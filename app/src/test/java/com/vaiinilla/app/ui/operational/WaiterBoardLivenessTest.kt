package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.domain.repository.WaiterBoard
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class WaiterBoardLivenessTest {
    private val loaded = ScreenshotFixtures.waiterBoardState()

    @Test
    fun `one failed refresh keeps the board live`() {
        val afterOne = loaded.withBoardFailure("La conexión tardó demasiado. Inténtalo de nuevo.")

        assertEquals(WaiterBoardLink.Live, afterOne.boardLink)
        assertEquals(1, afterOne.consecutiveBoardFailures)
    }

    @Test
    fun `two failed refreshes in a row drop the live pill but keep the tables`() {
        val afterTwo =
            loaded
                .withBoardFailure("La conexión tardó demasiado. Inténtalo de nuevo.")
                .withBoardFailure("La conexión tardó demasiado. Inténtalo de nuevo.")

        assertEquals(WaiterBoardLink.Offline, afterTwo.boardLink)
        assertEquals(loaded.tables, afterTwo.tables)
        assertEquals("La conexión tardó demasiado. Inténtalo de nuevo.", afterTwo.errorMessage)
    }

    @Test
    fun `a good board resets failures and records when it arrived`() {
        val receivedAt = Instant.parse("2026-10-15T18:00:00Z")
        val board = WaiterBoard(tables = loaded.tables, callsEnabled = true)

        val recovered =
            loaded
                .withBoardFailure("Sin red")
                .withBoardFailure("Sin red")
                .withBoard(board, receivedAt)

        assertEquals(WaiterBoardLink.Live, recovered.boardLink)
        assertEquals(0, recovered.consecutiveBoardFailures)
        assertEquals(receivedAt, recovered.lastBoardRefreshAt)
        assertEquals(null, recovered.errorMessage)
    }

    @Test
    fun `before the first good board the pill is connecting, never live`() {
        val starting = WaiterUiState(loading = true)

        assertEquals(WaiterBoardLink.Connecting, starting.boardLink)
        assertFalse(starting.boardLoaded)
    }

    @Test
    fun `a failure before any good board is offline right away and shows no board`() {
        val failed = WaiterUiState(loading = true).withBoardFailure("No pudimos contactar al servidor.")

        assertEquals(WaiterBoardLink.Offline, failed.boardLink)
        assertFalse(failed.boardLoaded)
        assertTrue(failed.tables.isEmpty())
    }

    @Test
    fun `a good board stays shown after it starts failing`() {
        val receivedAt = Instant.parse("2026-10-15T18:00:00Z")
        val stale =
            WaiterUiState()
                .withBoard(WaiterBoard(tables = emptyList(), callsEnabled = true), receivedAt)
                .withBoardFailure("Sin red")

        assertTrue(stale.boardLoaded)
        assertEquals(WaiterBoardLink.Live, stale.boardLink)
    }
}
