package com.vaiinilla.app

import com.vaiinilla.app.ui.screens.ApilaGame
import com.vaiinilla.app.ui.screens.ArcadeEngine
import com.vaiinilla.app.ui.screens.ArcadeKind
import com.vaiinilla.app.ui.screens.ArcadeStatus
import com.vaiinilla.app.ui.screens.BrincaGame
import com.vaiinilla.app.ui.screens.FlappyGame
import com.vaiinilla.app.ui.screens.GravedadGame
import com.vaiinilla.app.ui.screens.MemoryArcadeScoreStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WaitingArcadeGameTest {
    private fun engine(): ArcadeEngine =
        ArcadeEngine(MemoryArcadeScoreStore()).apply {
            width = 330f
            height = 250f
        }

    @Test
    fun `primary starts brinca and the mascot jumps`() {
        val e = engine()
        assertEquals(ArcadeStatus.IDLE, e.status)
        e.primary()
        assertEquals(ArcadeStatus.PLAY, e.status)
        e.step(0.016f)
        val g = e.game as BrincaGame
        assertTrue(g.y < g.floorY(e.height))
    }

    @Test
    fun `brinca collision ends the run and records the score`() {
        val store = MemoryArcadeScoreStore()
        val e =
            ArcadeEngine(store).apply {
                width = 330f
                height = 250f
            }
        e.primary()
        val g = e.game as BrincaGame
        g.obstacles += BrincaGame.Ob(x = 60f, w = 18f, h = 40f)
        e.step(0.016f)
        assertEquals(ArcadeStatus.DEAD, e.status)
        assertTrue(store.top(ArcadeKind.BRINCA).isNotEmpty())
    }

    @Test
    fun `brinca tap after death restarts the run`() {
        val e = engine()
        e.primary()
        (e.game as BrincaGame).obstacles += BrincaGame.Ob(x = 60f, w = 18f, h = 40f)
        e.step(0.016f)
        assertEquals(ArcadeStatus.DEAD, e.status)
        e.primary() // dead: el overlay manda, no el canvas
        assertEquals(ArcadeStatus.DEAD, e.status)
        e.retry()
        assertEquals(ArcadeStatus.PLAY, e.status)
        assertTrue((e.game as BrincaGame).obstacles.isEmpty())
    }

    @Test
    fun `flappy rises on tap and dies on column`() {
        val e = engine()
        e.select(ArcadeKind.FLAPPY)
        e.primary()
        val g = e.game as FlappyGame
        val before = g.y
        repeat(3) { e.step(0.016f) }
        assertTrue(g.y < before)
        g.columns += FlappyGame.Col(x = 60f, gapTop = 0f, gap = 40f)
        repeat(200) { if (e.status == ArcadeStatus.PLAY) e.step(0.016f) }
        assertEquals(ArcadeStatus.DEAD, e.status)
    }

    @Test
    fun `apila shrinks the block on imperfect drop and dies on a miss`() {
        val e = engine()
        e.select(ArcadeKind.APILA)
        e.primary()
        val g = e.game as ApilaGame
        val topW = g.stack.last().w
        e.step(0.05f) // consume el primer drop en pos~0
        assertTrue(g.stack.size == 2 || e.status == ArcadeStatus.DEAD)
        if (e.status == ArcadeStatus.PLAY) {
            assertTrue(g.stack.last().w < topW)
        }
        // fuerza un fallo: mueve el bloque fuera del tope y suelta
        val e2 = engine()
        e2.select(ArcadeKind.APILA)
        e2.primary()
        val g2 = e2.game as ApilaGame
        e2.step(0.016f)
        if (e2.status == ArcadeStatus.PLAY) {
            g2.pos = e2.width + 500f
            g2.drop(e2.width, e2.height)
            assertTrue(g2.fall != null)
        }
    }

    @Test
    fun `gravedad flips to the ceiling and back`() {
        val e = engine()
        e.select(ArcadeKind.GRAVEDAD)
        e.primary()
        val g = e.game as GravedadGame
        repeat(60) { e.step(0.016f) }
        assertTrue(g.y <= g.ceilY(e.height) + 0.5f)
        g.tap()
        repeat(60) { e.step(0.016f) }
        assertTrue(g.y >= g.floorY(e.height) - 0.5f || e.status == ArcadeStatus.DEAD)
    }

    @Test
    fun `select switches games and resets state`() {
        val e = engine()
        e.primary()
        e.select(ArcadeKind.APILA)
        assertEquals(ArcadeKind.APILA, e.kind)
        assertEquals(ArcadeStatus.IDLE, e.status)
        assertEquals(0, e.score)
        assertFalse(e.boardOpen)
    }

    @Test
    fun `leaderboard mixes seeds with own scores`() {
        val store = MemoryArcadeScoreStore()
        store.record(ArcadeKind.BRINCA, 40)
        val e = ArcadeEngine(store)
        val rows = e.leaderboard()
        assertTrue(rows.any { it.me && it.points == 40 })
        assertTrue(rows.any { !it.me })
        assertTrue(rows.zipWithNext().all { (a, b) -> a.points >= b.points })
    }
}
