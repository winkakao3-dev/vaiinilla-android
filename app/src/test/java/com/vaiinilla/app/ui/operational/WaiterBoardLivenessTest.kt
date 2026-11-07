package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.core.network.ApiFailure
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
        val afterOne = loaded.withBoardFailure(ApiFailure.Timeout)

        assertEquals(WaiterBoardLink.Live, afterOne.boardLink)
        assertEquals(1, afterOne.consecutiveBoardFailures)
    }

    @Test
    fun `two failed refreshes in a row drop the live pill but keep the tables`() {
        val afterTwo =
            loaded
                .withBoardFailure(ApiFailure.Timeout)
                .withBoardFailure(ApiFailure.Timeout)

        assertEquals(WaiterBoardLink.Offline, afterTwo.boardLink)
        assertEquals(loaded.tables, afterTwo.tables)
        assertEquals(ApiFailure.Timeout.text, afterTwo.errorMessage)
    }

    @Test
    fun `a good board resets failures and records when it arrived`() {
        val receivedAt = Instant.parse("2026-10-15T18:00:00Z")
        val board = WaiterBoard(tables = loaded.tables, callsEnabled = true)

        val recovered =
            loaded
                .withBoardFailure(ApiFailure.NoConnection)
                .withBoardFailure(ApiFailure.NoConnection)
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
        val failed = WaiterUiState(loading = true).withBoardFailure(ApiFailure.ServerUnreachable)

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
                .withBoardFailure(ApiFailure.NoConnection)

        assertTrue(stale.boardLoaded)
        assertEquals(WaiterBoardLink.Live, stale.boardLink)
    }

    @Test
    fun `the board view never flips between error and loading while retrying`() {
        val failed = WaiterUiState().withBoardFailure(ApiFailure.NoConnection)
        assertEquals(WaiterBoardView.Error(ApiFailure.NoConnection), failed.boardView)
        // Un sondeo marca «cargando» pero conserva el fallo: sigue siendo error hasta que llegue un tablero.
        assertEquals(WaiterBoardView.Error(ApiFailure.NoConnection), failed.copy(loading = true).boardView)
        assertEquals(WaiterBoardView.Loading, WaiterUiState(loading = true).boardView)
    }

    @Test
    fun `an empty business that already loaded is ready, not loading, during a poll`() {
        val receivedAt = Instant.parse("2026-10-15T18:00:00Z")
        val empty =
            WaiterUiState(loading = true)
                .withBoard(WaiterBoard(tables = emptyList(), callsEnabled = true), receivedAt)
                .copy(loading = true)

        assertEquals(WaiterBoardView.Ready, empty.boardView)
    }
}
