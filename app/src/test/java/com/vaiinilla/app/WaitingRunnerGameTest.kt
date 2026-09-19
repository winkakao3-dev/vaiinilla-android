package com.vaiinilla.app

import com.vaiinilla.app.ui.screens.RunnerGame
import com.vaiinilla.app.ui.screens.RunnerStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WaitingRunnerGameTest {
    private fun run(game: RunnerGame, seconds: Float) {
        var t = 0f
        while (t < seconds) {
            game.step(0.016f)
            t += 0.016f
        }
    }

    @Test
    fun `tap starts the run and jumps`() {
        val game = RunnerGame()
        assertEquals(RunnerStatus.Idle, game.status)
        game.tap()
        assertEquals(RunnerStatus.Running, game.status)
        game.step(0.016f)
        assertTrue(game.airborne)
    }

    @Test
    fun `mascot lands back on the ground`() {
        val game = RunnerGame()
        game.tap()
        run(game, 1f)
        assertTrue(!game.airborne)
        assertEquals(RunnerStatus.Running, game.status)
    }

    @Test
    fun `obstacles spawn and scroll toward the mascot`() {
        val game = RunnerGame()
        game.tap()
        run(game, 2f)
        assertTrue(game.obstacles.isNotEmpty())
        val x0 = game.obstacles.first().x
        run(game, 0.5f)
        assertTrue(game.obstacles.first().x < x0 || game.obstacles.size > 1)
    }

    @Test
    fun `colliding with an obstacle ends the run`() {
        val game = RunnerGame()
        game.tap()
        var steps = 0
        while (game.status == RunnerStatus.Running && steps < 60_000) {
            game.step(0.016f)
            steps++
        }
        assertEquals(RunnerStatus.Over, game.status)
    }

    @Test
    fun `tap after game over restarts`() {
        val game = RunnerGame()
        game.tap()
        var steps = 0
        while (game.status == RunnerStatus.Running && steps < 60_000) {
            game.step(0.016f)
            steps++
        }
        game.tap()
        assertEquals(RunnerStatus.Running, game.status)
        assertEquals(0, game.score)
        assertTrue(game.obstacles.isEmpty())
    }
}
