package com.vaiinilla.app.domain.arcade

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Gravedad: night bus 17. Vaini runs on the floor or the ceiling; one tap flips gravity. */
internal class GravityGame(
    seed: Int = 3,
) : ArcadeGame {
    object K {
        const val X = 100.0
        const val FLOOR = 228.0
        const val CEIL = 44.0
        const val HALF_H = 17.0
        const val HALF_W = 9.0
        const val GRAVITY = 2400.0
        const val V_MAX = 780.0
        const val SPEED0 = 140.0
        const val SPEED_MAX = 250.0
        const val OBSTACLE_H = 34.0
    }

    enum class Kind { SEAT, BACKPACK, UMBRELLA, STRAP }

    class Obstacle(
        var x: Double,
        val w: Double,
        val onFloor: Boolean,
        val kind: Kind,
        var passed: Boolean = false,
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
    var direction = 1
        private set
    var y = K.FLOOR - K.HALF_H
        private set
    var vy = 0.0
        private set
    var speed = K.SPEED0
        private set
    val obstacles = mutableListOf<Obstacle>()
    private var nextAt = 250.0
    private val rand = ArcadeRandom(seed)
    private val events = mutableListOf<ArcadeEvent>()

    override val time: Double get() = t
    override val deadTime: Double get() = deadT

    override fun tap() {
        if (over) return
        started = true
        direction *= -1
        events += ArcadeEvent.Flip(direction)
    }

    override fun drainEvents(): List<ArcadeEvent> = events.toList().also { events.clear() }

    override fun step(dt: Double) {
        t += dt
        if (!started) {
            dist += 60 * dt
            return
        }
        if (over) {
            deadT += dt
            return
        }
        speed = min(K.SPEED_MAX, K.SPEED0 + score * 2.2)
        dist += speed * dt
        vy = max(-K.V_MAX, min(K.V_MAX, vy + K.GRAVITY * direction * dt))
        y += vy * dt
        if (y >= FLOOR_CENTER) {
            if (vy > 200) events += ArcadeEvent.Land(1)
            y = FLOOR_CENTER
            vy = 0.0
        }
        if (y <= CEIL_CENTER) {
            if (vy < -200) events += ArcadeEvent.Land(-1)
            y = CEIL_CENTER
            vy = 0.0
        }

        if (dist >= nextAt) {
            val onFloor = rand.next() < 0.5
            val w = 28 + floor(rand.next() * 26)
            val kinds = if (onFloor) listOf(Kind.SEAT, Kind.BACKPACK) else listOf(Kind.UMBRELLA, Kind.STRAP)
            val kind = kinds[floor(rand.next() * kinds.size).toInt()]
            obstacles += Obstacle(480.0 + 30, w, onFloor, kind)
            nextAt = dist + speed * 0.95 + 60 + rand.next() * 90
        }
        for (o in obstacles) {
            o.x -= speed * dt
            if (!o.passed && o.x + o.w < K.X - K.HALF_W) {
                o.passed = true
                score += 1
                events += ArcadeEvent.Score(score.toInt())
            }
            val oy0 = if (o.onFloor) K.FLOOR - K.OBSTACLE_H else K.CEIL
            val oy1 = if (o.onFloor) K.FLOOR else K.CEIL + K.OBSTACLE_H
            val hit =
                K.X + K.HALF_W - 3 > o.x &&
                    K.X - K.HALF_W + 3 < o.x + o.w &&
                    y + K.HALF_H - 3 > oy0 &&
                    y - K.HALF_H + 3 < oy1
            if (hit) {
                over = true
                events += ArcadeEvent.Hit
                return
            }
        }
        obstacles.removeAll { it.x <= -80 }
    }

    companion object {
        const val FLOOR_CENTER = K.FLOOR - K.HALF_H
        const val CEIL_CENTER = K.CEIL + K.HALF_H
    }
}
