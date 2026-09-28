package com.vaiinilla.app.domain.arcade

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Kinds of obstacle on the street: size, whether Vaini can grind on top, points for clearing it. */
internal enum class SkateObstacleKind(
    val width: Double,
    val height: Double,
    val grindable: Boolean,
    val clearPoints: Int,
) {
    CONE(12.0, 16.0, false, 100),
    BIN(16.0, 28.0, false, 150),
    BENCH(44.0, 18.0, true, 100),
    RAIL(96.0, 30.0, true, 120),
}

internal class SkateObstacle(
    val kind: SkateObstacleKind,
    val x: Double,
) {
    var cleared = false
    var grindCounted = false
    var hitThis = false
}

internal class SkateStar(
    val x: Double,
    val y: Double,
    var taken: Boolean = false,
)

internal class SkateStats {
    var cones = 0
    var jumps = 0
    var kickflips = 0
    var grinds = 0
    var stars = 0
    var hits = 0
    var cleanDistance = 0.0
}

internal class SkatePlayer {
    var y = SkateRun.GROUND
    var vy = 0.0
    var grounded = true
    var grinding: SkateObstacle? = null
    var flip = 0.0
    var flipping = false
    var hurt = 0.0
    var stumble = 0.0
    var holdJump = false
    var fastFall = false
    var airTime = 0.0
}

/**
 * What the street scene needs to react to. Carries the player height at the moment it happened, because the scene
 * may see the events a few steps later.
 */
internal sealed interface SkateEvent {
    data object Jump : SkateEvent

    data object Flip : SkateEvent

    data class Land(
        val playerY: Double,
    ) : SkateEvent

    data class Star(
        val x: Double,
        val y: Double,
    ) : SkateEvent

    data class Points(
        val text: String,
    ) : SkateEvent

    data class GrindSpark(
        val playerY: Double,
    ) : SkateEvent

    data class Hit(
        val playerY: Double,
    ) : SkateEvent

    data class Bail(
        val text: String,
    ) : SkateEvent

    data object GameOver : SkateEvent
}

/** Rules of Vaini Skate: physics, obstacles, tricks and scoring. No drawing, no framework types. */
internal class SkateRun(
    seed: Int,
) {
    private val rng = ArcadeRandom(seed)
    var time = 0.0
        private set
    var distance = 0.0
        private set
    var speed = START_SPEED
        private set
    var score = 0
        private set
    var lives = 3
        private set
    var over = false
        private set
    var combo = 0
        private set
    var bestCombo = 0
        private set
    val player = SkatePlayer()
    val obstacles = mutableListOf<SkateObstacle>()
    val stars = mutableListOf<SkateStar>()
    private var nextSpawn = 420.0
    val stats = SkateStats()
    private val events = mutableListOf<SkateEvent>()

    fun screenX(worldX: Double): Double = worldX - distance

    val meters: Int get() = floor(distance / 20).toInt()

    fun drainEvents(): List<SkateEvent> = events.toList().also { events.clear() }

    private fun addPoints(
        base: Int,
        label: String,
    ) {
        combo += 1
        bestCombo = max(bestCombo, combo)
        val mult = 1 + combo / 3
        val pts = base * mult
        score += pts
        events += SkateEvent.Points("$label +$pts${if (mult > 1) " x$mult" else ""}")
    }

    // input

    fun pressJump() {
        if (over || player.stumble > 0) return
        if (player.grounded || player.grinding != null) {
            player.vy = JUMP_V
            player.grounded = false
            player.grinding = null
            player.holdJump = true
            player.airTime = 0.0
            stats.jumps += 1
            events += SkateEvent.Jump
        } else if (!player.flipping && player.flip == 0.0) {
            player.flipping = true // kickflip: must finish before landing
            events += SkateEvent.Flip
        }
    }

    fun releaseJump() {
        player.holdJump = false
    }

    fun setFastFall(on: Boolean) {
        player.fastFall = on
    }

    // spawning

    private fun spawn() {
        val level = min(1.0, distance / 12000)
        val roll = rng.next()
        val at = 520 + distance
        when {
            roll < 0.34 -> obstacles += SkateObstacle(SkateObstacleKind.CONE, at)
            roll < 0.52 -> obstacles += SkateObstacle(SkateObstacleKind.BIN, at)
            roll < 0.74 -> obstacles += SkateObstacle(SkateObstacleKind.BENCH, at)
            else -> obstacles += SkateObstacle(SkateObstacleKind.RAIL, at)
        }
        // double cone pattern gets likelier with distance
        if (rng.next() < 0.25 * level) obstacles += SkateObstacle(SkateObstacleKind.CONE, at + 70)
        // an arc of stars over the obstacle
        if (rng.next() < 0.7) {
            val n = 5
            val peak = 50 + rng.next() * 30
            for (i in 0 until n) {
                val y = GROUND - 20 - sin(i.toDouble() / (n - 1) * PI) * peak
                stars += SkateStar(at - 40 + i * 22, y)
            }
        }
        val gap = 150 + rng.next() * 140 - level * 60 + speed * 0.25
        nextSpawn = distance + gap
    }

    // step

    fun step(dt: Double) {
        if (over) return
        val p = player
        time += dt
        speed = min(MAX_SPEED, speed + ACCEL * dt)
        if (p.stumble > 0) {
            p.stumble -= dt
            speed = max(START_SPEED * 0.6, speed - 200 * dt)
        }
        distance += speed * dt
        if (p.hurt > 0) p.hurt -= dt
        if (p.hurt <= 0) stats.cleanDistance += speed * dt
        if (distance >= nextSpawn) spawn()

        // vertical motion
        val grind = p.grinding
        if (grind != null) {
            p.y = GROUND - grind.kind.height
            if (!grind.grindCounted) {
                grind.grindCounted = true
                stats.grinds += 1
                addPoints(150, "GRIND")
            }
            score += jsRound(60 * dt)
            events += SkateEvent.GrindSpark(p.y)
            if (screenX(grind.x) + grind.kind.width / 2 < PLAYER_X - HALF_W) {
                p.grinding = null
                p.grounded = false
                p.vy = -60.0
            }
        } else if (!p.grounded) {
            val g = GRAVITY * (if (p.holdJump && p.vy < 0) HOLD_GRAVITY else 1.0) * (if (p.fastFall) FAST_FALL else 1.0)
            p.vy += g * dt
            p.y += p.vy * dt
            p.airTime += dt
            if (p.flipping) {
                p.flip += dt
                if (p.flip >= FLIP_TIME) {
                    p.flipping = false
                    p.flip = 0.0
                    stats.kickflips += 1
                    addPoints(250, "KICKFLIP")
                }
            }
            // land on a grindable top
            if (p.vy > 0) {
                for (o in obstacles) {
                    val k = o.kind
                    val sx = screenX(o.x)
                    if (!k.grindable || abs(sx - PLAYER_X) > k.width / 2 + HALF_W - 4) continue
                    val top = GROUND - k.height
                    if (p.y >= top && p.y - p.vy * dt <= top + 2) {
                        land(top)
                        if (!over && p.stumble <= 0) p.grinding = o
                        break
                    }
                }
            }
            if (p.grinding == null && p.y >= GROUND) land(GROUND)
        }

        // obstacles: clearing and collisions
        for (o in obstacles) {
            val k = o.kind
            val sx = screenX(o.x)
            val overlapX = abs(sx - PLAYER_X) < k.width / 2 + HALF_W - 3
            if (overlapX && p.grinding !== o && p.y > GROUND - k.height + 3 && p.hurt <= 0) hit()
            if (!o.cleared && sx + k.width / 2 < PLAYER_X - HALF_W) {
                o.cleared = true
                if (!o.hitThis && !o.grindCounted) {
                    if (k == SkateObstacleKind.CONE) stats.cones += 1
                    addPoints(k.clearPoints, if (k == SkateObstacleKind.CONE) "OLLIE" else "SALTO")
                }
            }
            if (overlapX && p.hurt > 0) o.hitThis = true
        }
        // stars
        for (s in stars) {
            if (s.taken) continue
            val sx = screenX(s.x)
            if (abs(sx - PLAYER_X) < 12 && abs(s.y - (p.y - BODY_H / 2)) < 22) {
                s.taken = true
                stats.stars += 1
                score += 25
                events += SkateEvent.Star(sx, s.y)
            }
        }
        obstacles.removeAll { screenX(it.x) <= -80 }
        stars.removeAll { screenX(it.x) <= -20 || it.taken }
    }

    private fun land(y: Double) {
        val p = player
        p.y = y
        p.vy = 0.0
        p.grounded = y == GROUND
        p.holdJump = false
        events += SkateEvent.Land(y)
        if (p.flipping) {
            p.flipping = false
            p.flip = 0.0
            bail()
        }
    }

    private fun bail() {
        combo = 0
        player.stumble = 0.6
        events += SkateEvent.Bail("¡CAÍDA!")
    }

    private fun hit() {
        lives -= 1
        combo = 0
        stats.hits += 1
        stats.cleanDistance = 0.0
        player.hurt = INVULNERABLE
        player.stumble = 0.4
        events += SkateEvent.Hit(player.y)
        if (lives <= 0) {
            over = true
            events += SkateEvent.GameOver
        }
    }

    companion object {
        const val GROUND = 226.0 // feet line on the street
        const val PLAYER_X = 120.0 // Vaini's fixed screen x
        const val FLIP_TIME = 0.42
        const val HALF_W = 11.0
        const val BODY_H = 36.0
        private const val GRAVITY = 980.0
        private const val JUMP_V = -310.0
        private const val HOLD_GRAVITY = 0.52
        private const val FAST_FALL = 2.4
        private const val START_SPEED = 150.0
        private const val MAX_SPEED = 330.0
        private const val ACCEL = 4.2
        private const val INVULNERABLE = 1.4
    }
}
