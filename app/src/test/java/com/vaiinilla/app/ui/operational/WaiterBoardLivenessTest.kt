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

        assertTrue(afterOne.boardLive)
        assertEquals(1, afterOne.consecutiveBoardFailures)
    }

    @Test
    fun `two failed refreshes in a row drop the live pill but keep the tables`() {
        val afterTwo =
            loaded
                .withBoardFailure("La conexión tardó demasiado. Inténtalo de nuevo.")
                .withBoardFailure("La conexión tardó demasiado. Inténtalo de nuevo.")

        assertFalse(afterTwo.boardLive)
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

        assertTrue(recovered.boardLive)
        assertEquals(0, recovered.consecutiveBoardFailures)
        assertEquals(receivedAt, recovered.lastBoardRefreshAt)
        assertEquals(null, recovered.errorMessage)
    }
}
