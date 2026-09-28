package com.vaiinilla.app

import com.vaiinilla.app.arcade.ArcadeGame
import com.vaiinilla.app.arcade.ArcadeKind
import com.vaiinilla.app.arcade.ArcadeSession
import com.vaiinilla.app.arcade.ArcadeStatus
import com.vaiinilla.app.arcade.FlappyGame
import com.vaiinilla.app.arcade.GalaxyGame
import com.vaiinilla.app.arcade.GalaxyWave
import com.vaiinilla.app.arcade.GravityGame
import com.vaiinilla.app.arcade.MemoryArcadeScoreStore
import com.vaiinilla.app.arcade.MemorySkateProgressStore
import com.vaiinilla.app.arcade.SkateGame
import com.vaiinilla.app.arcade.SkateMissions
import com.vaiinilla.app.arcade.StackGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class WaitingArcadeGameTest {
    private val dt = ArcadeSession.FIXED_STEP

    private fun run(
        game: ArcadeGame,
        seconds: Double,
        bot: (() -> Unit)? = null,
    ) {
        var t = 0.0
        while (t < seconds && !game.over) {
            bot?.invoke()
            game.step(dt)
            game.drainEvents()
            t += dt
        }
    }

    // Session

    @Test
    fun `tap starts a run and a game over screen appears after the hero falls`() {
        val session = ArcadeSession(seedSource = { 3 })
        assertEquals(ArcadeStatus.IDLE, session.status)
        session.primary()
        assertEquals(ArcadeStatus.PLAY, session.status)
        repeat(120 * 4) { session.step(dt) } // no more flaps: Vuela ends by itself
        assertEquals(ArcadeStatus.DEAD, session.status)
    }

    @Test
    fun `game over screen ignores taps for a moment and then retries`() {
        val session = ArcadeSession(seedSource = { 3 })
        session.primary()
        while (session.status == ArcadeStatus.PLAY) session.step(dt)
        session.primary()
        assertEquals(ArcadeStatus.DEAD, session.status) // too soon
        repeat((ArcadeSession.RETRY_DELAY / dt).toInt() + 2) { session.step(dt) }
        session.primary()
        assertEquals(ArcadeStatus.PLAY, session.status)
        assertEquals(0, session.score)
    }

    @Test
    fun `finished run is recorded and only a better one is flagged as new best`() {
        val store = MemoryArcadeScoreStore()
        val session = ArcadeSession(store, seedSource = { 3 })
        session.primary()
        while (session.status == ArcadeStatus.PLAY) session.step(dt)
        assertEquals(1, store.top(ArcadeKind.FLAPPY).size)
        assertFalse(session.newBest) // a run that scored nothing is not a record

        session.select(ArcadeKind.GALAXIA)
        session.primary()
        while (session.status == ArcadeStatus.PLAY) session.step(dt) // the idle jet still shoots a few enemies
        assertTrue(session.score > 0)
        assertTrue(session.newBest)
        assertEquals(session.score, store.top(ArcadeKind.GALAXIA).first())
    }

    @Test
    fun `selecting another game resets the run and closes the board`() {
        val session = ArcadeSession(seedSource = { 3 })
        session.primary()
        session.boardOpen = true
        session.select(ArcadeKind.GALAXIA)
        assertEquals(ArcadeKind.GALAXIA, session.kind)
        assertEquals(ArcadeStatus.IDLE, session.status)
        assertFalse(session.boardOpen)
        assertTrue(session.game is GalaxyGame)
    }

    @Test
    fun `leaderboard mixes seeds and player scores in order`() {
        val store = MemoryArcadeScoreStore()
        store.record(ArcadeKind.APILA, 40)
        val session = ArcadeSession(store)
        session.select(ArcadeKind.APILA)
        val rows = session.leaderboard()
        assertEquals(40, rows.first().points)
        assertTrue(rows.first().me)
        assertTrue(rows.size <= 7)
    }

    // Vuela

    @Test
    fun `vuela falls without flaps and levels up every eight pencils`() {
        val g = FlappyGame(3)
        g.tap()
        run(g, 3.0)
        assertTrue(g.over)
        assertEquals(1, FlappyGame.levelFor(0.0))
        assertEquals(2, FlappyGame.levelFor(8.0))
        val hard = FlappyGame.tuning(6)
        val easy = FlappyGame.tuning(1)
        assertTrue(hard.speed > easy.speed)
        assertTrue(hard.gap < easy.gap)
        assertEquals(0.0, FlappyGame.tuning(2).amp, 0.0)
        assertTrue(FlappyGame.tuning(3).amp > 0)
    }

    @Test
    fun `vuela can be played by a simple bot`() {
        val g = FlappyGame(3)
        g.tap()
        run(g, 40.0) {
            var target = 130.0 // no pillar yet: hover around mid height
            val p = g.pillars.firstOrNull { it.x + FlappyGame.K.PILLAR_W > FlappyGame.K.X - FlappyGame.K.RADIUS }
            if (p != null) {
                val eta = maxOf(0.0, (p.x - FlappyGame.K.X) / g.speed)
                target = FlappyGame.gapCenter(p, g.t + eta) + 32
            }
            if (g.y + g.vy * 0.07 > target) g.tap()
        }
        assertTrue(g.score >= 20)
        assertTrue(g.level >= 3)
    }

    // Apila

    @Test
    fun `apila perfect drops keep width and speed grows with the score`() {
        val g = StackGame(3)
        g.tap()
        run(g, 40.0) {
            val cur = g.cur
            if (cur != null && g.drop == null && abs(cur.x - g.stack.last().x) < 3) g.tap()
        }
        assertTrue(g.score > 10)
        assertEquals(StackGame.K.BASE_W, g.stack.last().w, 0.001)
        assertTrue(StackGame.speedFor(30.0) > StackGame.speedFor(0.0))
    }

    @Test
    fun `apila missing the tower ends the run`() {
        val g = StackGame(3)
        g.tap()
        run(g, 30.0) {
            val cur = g.cur
            if (cur != null && g.drop == null && cur.x >= 320) g.tap() // past the right edge of the base: no overlap
        }
        assertTrue(g.over)
    }

    // Gravedad

    @Test
    fun `gravedad flips and hits obstacles`() {
        val g = GravityGame(3)
        g.tap()
        assertEquals(-1, g.direction)
        run(g, 20.0)
        assertTrue(g.over)
    }

    // Galaxia

    @Test
    fun `galaxia waves get harder and the fourth brings the boss`() {
        assertTrue(GalaxyWave(4).boss)
        assertFalse(GalaxyWave(3).boss)
        assertTrue(GalaxyWave(8).bulletSpeed > GalaxyWave(1).bulletSpeed)
        assertTrue(GalaxyWave(8).diveEvery < GalaxyWave(1).diveEvery)
        assertTrue(GalaxyWave(8).hpBonus > GalaxyWave(1).hpBonus)
    }

    @Test
    fun `galaxia bot clears waves and an idle ship loses`() {
        val bot = GalaxyGame(3)
        bot.tap()
        run(bot, 110.0) {
            val shipY = GalaxyGame.K.SHIP_Y
            val threat = bot.bullets.firstOrNull { it.y > shipY - 70 && abs(it.x - bot.x) < 22 }
            if (threat != null) {
                bot.aim(bot.x + if (threat.x >= bot.x) -40 else 40)
            } else {
                val target =
                    bot.boss?.x
                        ?: bot.enemies
                            .filter { it.state == com.vaiinilla.app.arcade.GalaxyEnemy.State.FORMATION }
                            .minByOrNull { abs(it.x - bot.x) }
                            ?.x
                if (target != null) bot.aim(target)
            }
        }
        assertTrue(bot.wave >= 5)
        assertTrue(bot.score > 1000)

        val idle = GalaxyGame(3)
        idle.tap()
        run(idle, 150.0)
        assertTrue(idle.over)
    }

    // Skate

    @Test
    fun `skate challenges are deterministic per level and scale`() {
        assertEquals(SkateMissions.missionsFor(0), SkateMissions.missionsFor(0))
        assertEquals(3, SkateMissions.missionsFor(0).size)
        assertEquals(
            3,
            SkateMissions
                .missionsFor(2)
                .map { it.id }
                .toSet()
                .size,
        )
        val a = SkateMissions.missionsFor(0).first()
        val b = SkateMissions.missionsFor(3).firstOrNull { it.id == a.id }
        if (b != null) assertTrue(b.target > a.target)
    }

    @Test
    fun `skate game saves best and plays the demo before starting`() {
        val store = MemorySkateProgressStore()
        val g = SkateGame(4, store)
        run(g, 3.0) // title demo plays itself, never ends the game
        assertFalse(g.over)
        g.tap()
        var t = 0.0
        while (!g.over && t < 60) { // a run nobody steers loses eventually
            g.step(dt)
            g.drainEvents()
            t += dt
        }
        assertTrue(g.over)
        assertEquals(g.score.toInt(), store.load().best)
    }
}
