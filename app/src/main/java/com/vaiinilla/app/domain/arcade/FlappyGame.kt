package com.vaiinilla.app.domain.arcade

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Vuela: Vaini hangs from a balloon and flies between giant pencils. One tap = one flap.
 * Every 8 pencils the level rises: faster, a narrower gap and, from level 3, pencils that move.
 */
internal class FlappyGame(
    seed: Int = 7,
) : ArcadeGame {
    object K {
        const val X = 120.0
        const val RADIUS = 9.0
        const val GROUND = 246.0
        const val GRAVITY = 820.0
        const val FLAP = -236.0
        const val FALL_MAX = 330.0
        const val SPEED0 = 92.0
        const val SPEED_MAX = 165.0
        const val PILLAR_W = 30.0
        const val GAP0 = 100.0
        const val GAP_MIN = 80.0
        const val SPACING = 150.0
        const val TOP = 60.0
        const val BOTTOM = 196.0
        const val PER_LEVEL = 8.0
        const val MOVING_FROM = 3
    }

    class Pillar(
        var x: Double,
        val baseY: Double,
        var gapY: Double,
        val gap: Double,
        val amp: Double,
        val freq: Double,
        val phase: Double,
        var passed: Boolean = false,
    )

    class Tuning(
        val speed: Double,
        val gap: Double,
        val amp: Double,
        val freq: Double,
    )

    var t = 0.0
        private set
    var dist = 0.0
        private set
    override var started = false
        private set
    override var over = false
        private set
    var deadT = 0.0
        private set
    override var score = 0.0
        private set
    var level = 1
        private set
    var y = 118.0
        private set
    var vy = 0.0
        private set
    var speed = K.SPEED0
        private set
    val pillars = mutableListOf<Pillar>()
    var flapT = 9.0
        private set
    var levelT = 9.0
        private set
    private var nextAt = 210.0
    private var prevGap = 128.0
    private val rand = ArcadeRandom(seed)
    private val events = mutableListOf<ArcadeEvent>()

    override val time: Double get() = t
    override val deadTime: Double get() = deadT

    override fun tap() {
        if (over) return
        started = true
        vy = K.FLAP
        flapT = 0.0
        events += ArcadeEvent.Flap
    }

    override fun drainEvents(): List<ArcadeEvent> = events.toList().also { events.clear() }

    override fun step(dt: Double) {
        t += dt
        flapT += dt
        levelT += dt
        if (!started) { // idle: hover on the title screen
            y = 118 + sin(t * 3) * 6
            dist += K.SPEED0 * 0.5 * dt
            return
        }
        if (over) { // fall to the ground after a hit, world stops
            deadT += dt
            vy = min(K.FALL_MAX, vy + K.GRAVITY * dt)
            y = min(K.GROUND - 18, y + vy * dt)
            return
        }
        val tune = tuning(level)
        speed = tune.speed
        dist += speed * dt
        vy = min(K.FALL_MAX, vy + K.GRAVITY * dt)
        y += vy * dt
        if (y < -8) {
            y = -8.0
            vy = max(0.0, vy)
        }

        if (dist >= nextAt) {
            nextAt = dist + K.SPACING
            val lo = K.TOP + tune.gap / 2
            val hi = K.BOTTOM - tune.gap / 2 + 20
            val c = max(lo, min(hi, prevGap + (rand.next() - 0.5) * 120))
            prevGap = c
            pillars +=
                Pillar(
                    x = 480.0 + 20,
                    baseY = c,
                    gapY = c,
                    gap = tune.gap,
                    amp = tune.amp,
                    freq = tune.freq,
                    phase = rand.next() * 6.28,
                )
        }
        for (p in pillars) {
            p.x -= speed * dt
            p.gapY = gapCenter(p, t)
            if (!p.passed && p.x + K.PILLAR_W < K.X - K.RADIUS) {
                p.passed = true
                score += 1
                events += ArcadeEvent.Score(score.toInt())
                val lv = levelFor(score)
                if (lv > level) {
                    level = lv
                    levelT = 0.0
                    events += ArcadeEvent.Level(lv)
                }
            }
            val inX = K.X + K.RADIUS > p.x + 2 && K.X - K.RADIUS < p.x + K.PILLAR_W - 2
            if (inX && (y - K.RADIUS < p.gapY - p.gap / 2 || y + K.RADIUS > p.gapY + p.gap / 2)) {
                die()
                return
            }
        }
        pillars.removeAll { it.x <= -60 }
        if (y + K.RADIUS + 8 > K.GROUND) die()
    }

    private fun die() {
        over = true
        vy = -120.0
        events += ArcadeEvent.Hit
    }

    companion object {
        fun levelFor(score: Double): Int = kotlin.math.floor(score / K.PER_LEVEL).toInt() + 1

        /** Difficulty knobs for a level, in one place so drawing and tests read the same numbers. */
        fun tuning(level: Int): Tuning {
            val lv = level.toDouble()
            return Tuning(
                speed = min(K.SPEED_MAX, K.SPEED0 + (lv - 1) * 9),
                gap = max(K.GAP_MIN, K.GAP0 - (lv - 1) * 2.5),
                amp = if (level >= K.MOVING_FROM) min(26.0, (level - K.MOVING_FROM + 1) * 6.0) else 0.0,
                freq = 1.1 + lv * 0.07,
            )
        }

        /** Centre of a pillar's gap at time [t]; moving pencils sway around their base. */
        fun gapCenter(
            p: Pillar,
            t: Double,
        ): Double {
            val c = p.baseY + p.amp * sin(t * p.freq + p.phase)
            return max(p.gap / 2 + 26, min(K.GROUND - 16 - p.gap / 2, c))
        }
    }
}
