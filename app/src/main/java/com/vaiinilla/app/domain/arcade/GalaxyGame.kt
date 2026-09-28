package com.vaiinilla.app.domain.arcade

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

internal enum class GalaxyEnemyKind(
    val points: Int,
) {
    DONA(10),
    TAZA(20),
    CRUASAN(30),
}

/** How hard a wave is, in one place so drawing, tests and bots read the same numbers. */
internal class GalaxyWave(
    n: Int,
) {
    val boss = n % 4 == 0
    val rows = min(4, 2 + (n - 1) / 2)
    val cols = min(8, 5 + (n - 1) / 2)
    val diveEvery = max(0.65, 2.6 - n * 0.17)
    val diveSpeed = 120.0 + n * 10
    val bulletSpeed = 110.0 + n * 8
    val fireEvery = max(0.38, 1.5 - n * 0.1)
    val hpBonus =
        if (n >= 11) {
            2
        } else if (n >= 6) {
            1
        } else {
            0
        }
    val bossHp = 22 + n * 3
}

internal class GalaxyEnemy(
    val kind: GalaxyEnemyKind,
    val col: Int,
    val row: Int,
    startX: Double,
    delay: Double,
    var hp: Int,
    val wob: Double,
) {
    enum class State { ENTER, FORMATION, DIVE, RETURNING }

    var x = startX
    var y = -20.0
    var state = State.ENTER
    var t = delay
    var sx = startX
    var flash = 0.0
    var aimX = 0.0
    var fired = false
    var dead = false
}

internal class GalaxyBoss(
    val maxHp: Int,
) {
    var x = 240.0
    var y = -50.0
    var hp = maxHp
    var dir = 1.0
    var fireT = 1.6
    var hit = 0.0
}

internal class GalaxyBullet(
    var x: Double,
    var y: Double,
    val vx: Double,
    val vy: Double,
    var dead: Boolean = false,
)

internal class GalaxyShot(
    var x: Double,
    var y: Double,
    val vx: Double,
    var dead: Boolean = false,
)

internal class GalaxyDrop(
    val x: Double,
    var y: Double,
    val kind: Kind,
    val vy: Double,
    var dead: Boolean = false,
) {
    enum class Kind { LEAF, HEART }
}

/**
 * Galaxia: Vaini flies a jet and defends the cafe from donuts, mugs and croissants.
 * Drag to move; the jet fires by itself. Each wave brings more enemies, closer dives and faster shots;
 * every 4th wave a boss (the espresso machine) arrives. Lime leaves upgrade the shot, hearts give a life.
 */
internal class GalaxyGame(
    seed: Int = 9,
) : ArcadeGame {
    object K {
        const val SHIP_Y = 232.0
        const val SHIP_HALF_W = 10.0
        const val SHIP_HALF_H = 6.0
        const val SHIP_SPEED = 340.0
        const val BULLET_SPEED = 400.0
        const val FIRE = 0.27
        const val MAX_LIVES = 5
        const val START_LIVES = 3
        const val DX = 34.0
        const val DY = 25.0
        const val TOP = 46.0
        const val INVULN = 1.7
        const val ENEMY_R = 8.0
    }

    var t = 0.0
        private set
    override var started = false
        private set
    override var over = false
        private set
    var deadT = 0.0
        private set
    override var score = 0.0
        private set
    var x = 240.0
        private set
    var targetX = 240.0
        private set
    var lives = K.START_LIVES
        private set
    var invuln = 0.0
        private set
    var power = 1
        private set
    var fireT = 0.0
        private set
    var lean = 0.0
        private set
    var wave = 0
        private set
    var waveT = 9.0
        private set
    var cfg: GalaxyWave? = null
        private set
    val enemies = mutableListOf<GalaxyEnemy>()
    val bullets = mutableListOf<GalaxyBullet>()
    val shots = mutableListOf<GalaxyShot>()
    val drops = mutableListOf<GalaxyDrop>()
    var boss: GalaxyBoss? = null
        private set
    var kills = 0
        private set

    /** Held arrow key: -1, 0 or 1 (keyboard steering for tests). */
    var key = 0
    private var waveDelay = 0.8
    private var diveT = 2.0
    private var fireEnemyT = 1.5
    private var ox = 240.0
    private val rand = ArcadeRandom(seed)
    private val events = mutableListOf<ArcadeEvent>()

    override val time: Double get() = t
    override val deadTime: Double get() = deadT

    override fun aim(x: Double) {
        targetX = max(K.SHIP_HALF_W + 4, min(480 - K.SHIP_HALF_W - 4, x))
    }

    override fun tap() {
        if (!over) started = true
    }

    override fun drainEvents(): List<ArcadeEvent> = events.toList().also { events.clear() }

    private fun slotX(e: GalaxyEnemy): Double {
        val w = cfg!!
        val breathe = 1 + sin(t * 1.4) * 0.05
        return ox + (e.col - (w.cols - 1) / 2.0) * K.DX * breathe
    }

    private fun slotY(e: GalaxyEnemy): Double = K.TOP + e.row * K.DY + sin(t * 2 + e.col) * 1.5

    private fun spawnWave() {
        wave += 1
        val c = GalaxyWave(wave)
        cfg = c
        waveT = 0.0
        events += ArcadeEvent.Wave(wave, c.boss)
        bullets.clear()
        enemies.clear()
        if (c.boss) {
            boss = GalaxyBoss(c.bossHp)
            return
        }
        boss = null
        val kinds = listOf(GalaxyEnemyKind.CRUASAN, GalaxyEnemyKind.TAZA, GalaxyEnemyKind.TAZA, GalaxyEnemyKind.DONA)
        var i = 0
        for (row in 0 until c.rows) {
            for (col in 0 until c.cols) {
                val kind = kinds[min(row, 3)]
                val side = if ((col + row) % 2 == 1) -1.0 else 1.0
                val startX = if (side > 0) 500.0 else -20.0
                enemies +=
                    GalaxyEnemy(
                        kind = kind,
                        col = col,
                        row = row,
                        startX = startX,
                        delay = -i * 0.09,
                        hp = 1 + c.hpBonus,
                        wob = rand.next() * 6,
                    )
                i += 1
            }
        }
    }

    override fun step(dt: Double) {
        t += dt
        waveT += dt
        invuln = max(0.0, invuln - dt)
        if (!started) {
            x += (240 + sin(t * 1.3) * 90 - x) * min(1.0, dt * 3)
            ox = 240 + sin(t * 0.6) * 40
            if (cfg == null) cfg = GalaxyWave(1)
            return
        }
        if (over) {
            deadT += dt
            return
        }

        // ship: follows the finger, or the arrow keys
        if (key != 0) aim(targetX + key * K.SHIP_SPEED * dt)
        val dx = targetX - x
        x += max(-K.SHIP_SPEED * dt, min(K.SHIP_SPEED * dt, dx * 14 * dt))
        lean += ((dx / 60) - lean) * min(1.0, dt * 10)

        // auto fire
        fireT -= dt
        if (fireT <= 0) {
            fireT = K.FIRE - (power - 1) * 0.02
            val offsets =
                if (power == 1) {
                    listOf(0.0)
                } else if (power == 2) {
                    listOf(-6.0, 6.0)
                } else {
                    listOf(-8.0, 0.0, 8.0)
                }
            offsets.forEachIndexed { i, o ->
                shots += GalaxyShot(x + o, K.SHIP_Y - 14, if (power == 3) (i - 1) * 40.0 else 0.0)
            }
            events += ArcadeEvent.Shoot
        }

        // waves
        if (cfg == null || (enemies.isEmpty() && boss == null)) {
            waveDelay -= dt
            if (waveDelay <= 0) {
                spawnWave()
                waveDelay = 1.5
            }
        }
        cfg?.let {
            ox = 240 + sin(t * 0.6) * (if (it.cols >= 7) 26 else 50)
            stepEnemies(dt, it)
        }
        stepBoss(dt)
        stepShots(dt)
    }

    private fun bez(
        a: Double,
        b: Double,
        c: Double,
        t: Double,
    ): Double = (1 - t) * (1 - t) * a + 2 * (1 - t) * t * b + t * t * c

    private fun stepEnemies(
        dt: Double,
        cfg: GalaxyWave,
    ) {
        diveT -= dt
        fireEnemyT -= dt
        val formed = enemies.filter { it.state == GalaxyEnemy.State.FORMATION }
        if (diveT <= 0 && formed.size > 1) {
            diveT = cfg.diveEvery * (0.75 + rand.next() * 0.5)
            val e = formed[floor(rand.next() * formed.size).toInt()]
            e.state = GalaxyEnemy.State.DIVE
            e.t = 0.0
            e.sx = e.x
            e.aimX = x + (rand.next() - 0.5) * 60
            e.fired = false
        }
        if (fireEnemyT <= 0 && formed.isNotEmpty()) {
            fireEnemyT = cfg.fireEvery * (0.7 + rand.next() * 0.6)
            val shooters = formed.filter { e -> formed.none { it.col == e.col && it.row > e.row } }
            val index = floor(rand.next() * shooters.size).toInt()
            val e = if (index < shooters.size) shooters[index] else formed[0]
            bullets += GalaxyBullet(e.x, e.y + 8, 0.0, cfg.bulletSpeed)
        }
        for (e in enemies) {
            e.flash = max(0.0, e.flash - dt)
            e.t += dt
            if (e.t < 0) continue
            val sx = slotX(e)
            val sy = slotY(e)
            when (e.state) {
                GalaxyEnemy.State.ENTER -> { // swoop in from the side along a curve
                    val k = min(1.0, e.t / 1.3)
                    e.x = bez(e.sx, ox, sx, k) + sin(k * 6 + e.wob) * 6 * (1 - k)
                    e.y = bez(-20.0, 150.0, sy, k)
                    if (k >= 1) e.state = GalaxyEnemy.State.FORMATION
                }
                GalaxyEnemy.State.FORMATION -> {
                    e.x = sx
                    e.y = sy
                }
                GalaxyEnemy.State.DIVE -> { // sine dive toward where the player was
                    val k = e.t * cfg.diveSpeed / 230
                    e.y += cfg.diveSpeed * dt
                    e.x = e.sx + (e.aimX - e.sx) * min(1.0, k * 0.9) + sin(e.t * 4 + e.wob) * 38 * min(1.0, k)
                    if (!e.fired && e.y > 100) {
                        e.fired = true
                        bullets += GalaxyBullet(e.x, e.y + 6, (x - e.x) * 0.4, cfg.bulletSpeed * 0.9)
                    }
                    if (e.y > 290) {
                        e.state = GalaxyEnemy.State.RETURNING
                        e.t = 0.0
                        e.y = -20.0
                        e.sx = e.x
                    }
                }
                GalaxyEnemy.State.RETURNING -> {
                    val k = min(1.0, e.t / 1.0)
                    e.x = bez(e.sx, (e.sx + sx) / 2, sx, k)
                    e.y = bez(-20.0, 40.0, sy, k)
                    if (k >= 1) e.state = GalaxyEnemy.State.FORMATION
                }
            }
            // collision with the ship
            if (invuln <= 0 &&
                abs(e.x - x) < K.ENEMY_R + K.SHIP_HALF_W - 2 &&
                abs(e.y - (K.SHIP_Y - 4)) < K.ENEMY_R + K.SHIP_HALF_H
            ) {
                hurt()
                killEnemy(e, diving = true)
            }
        }
        enemies.removeAll { it.dead }
    }

    private fun stepBoss(dt: Double) {
        val b = boss ?: return
        b.hit = max(0.0, b.hit - dt)
        if (b.y < 62) {
            b.y += 60 * dt
            return
        }
        b.x += b.dir * (50 + wave * 4) * dt
        if (b.x > 420) b.dir = -1.0
        if (b.x < 60) b.dir = 1.0
        b.fireT -= dt
        if (b.fireT <= 0) {
            b.fireT = max(0.7, 1.5 - wave * 0.05)
            for (vx in listOf(-50.0, 0.0, 50.0)) bullets += GalaxyBullet(b.x, b.y + 20, vx, cfg!!.bulletSpeed)
            events += ArcadeEvent.BossFire
        }
    }

    private fun stepShots(dt: Double) {
        for (s in shots) {
            s.y -= K.BULLET_SPEED * dt
            s.x += s.vx * dt
        }
        for (s in shots) {
            for (e in enemies) {
                if (e.dead || e.t < 0 || e.state == GalaxyEnemy.State.ENTER) continue
                if (abs(s.x - e.x) < K.ENEMY_R + 2 && abs(s.y - e.y) < K.ENEMY_R + 3) {
                    s.dead = true
                    e.hp -= 1
                    e.flash = 0.1
                    if (e.hp <=
                        0
                    ) {
                        killEnemy(e, diving = e.state == GalaxyEnemy.State.DIVE)
                    } else {
                        events += ArcadeEvent.HitEnemy
                    }
                    break
                }
            }
            val b = boss
            if (!s.dead && b != null && b.y >= 60 && abs(s.x - b.x) < 34 && abs(s.y - b.y) < 24) {
                s.dead = true
                b.hp -= 1
                b.hit = 0.08
                events += ArcadeEvent.HitEnemy
                if (b.hp <= 0) {
                    score += 500
                    events += ArcadeEvent.BossDown(b.x, b.y)
                    drops += GalaxyDrop(b.x, b.y, GalaxyDrop.Kind.HEART, 40.0)
                    drops += GalaxyDrop(b.x - 20, b.y, GalaxyDrop.Kind.LEAF, 40.0)
                    boss = null
                }
            }
        }
        shots.removeAll { it.dead || it.y <= -10 }
        for (b in bullets) {
            b.x += b.vx * dt
            b.y += b.vy * dt
            if (invuln <= 0 && abs(b.x - x) < K.SHIP_HALF_W && abs(b.y - (K.SHIP_Y - 2)) < K.SHIP_HALF_H + 3) {
                b.dead = true
                hurt()
            }
        }
        bullets.removeAll { it.dead || it.y >= 285 || it.x <= -10 || it.x >= 490 }
        for (d in drops) {
            d.y += d.vy * dt
            if (abs(d.x - x) < 16 && abs(d.y - (K.SHIP_Y - 8)) < 14) {
                d.dead = true
                if (d.kind == GalaxyDrop.Kind.LEAF) {
                    power = min(3, power + 1)
                    score += 50
                    events += ArcadeEvent.Power(power)
                } else {
                    lives = min(K.MAX_LIVES, lives + 1)
                    events += ArcadeEvent.Life
                }
            }
        }
        drops.removeAll { it.dead || it.y >= 285 }
    }

    private fun killEnemy(
        e: GalaxyEnemy,
        diving: Boolean,
    ) {
        e.dead = true
        kills += 1
        val pts = e.kind.points * (if (diving) 2 else 1)
        score += pts
        events += ArcadeEvent.Kill(e.kind, e.x, e.y, pts)
        val r = rand.next()
        if (r < 0.1) {
            drops += GalaxyDrop(e.x, e.y, GalaxyDrop.Kind.LEAF, 50.0)
        } else if (r < 0.135) {
            drops += GalaxyDrop(e.x, e.y, GalaxyDrop.Kind.HEART, 50.0)
        }
    }

    private fun hurt() {
        lives -= 1
        invuln = K.INVULN
        power = max(1, power - 1)
        events += ArcadeEvent.Hurt
        if (lives <= 0) {
            over = true
            events += ArcadeEvent.Hit
        }
    }
}
