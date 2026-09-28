package com.vaiinilla.app.ui.arcade

import com.vaiinilla.app.arcade.GalaxyBoss
import com.vaiinilla.app.arcade.GalaxyDrop
import com.vaiinilla.app.arcade.GalaxyEnemy
import com.vaiinilla.app.arcade.GalaxyEnemyKind
import com.vaiinilla.app.arcade.GalaxyGame
import com.vaiinilla.app.arcade.jsRound
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Galaxia: Vaini sits in the cockpit of a blue-and-lime jet in a cozy night sky. Donuts, mugs and croissants
 * attack; the boss is a giant espresso machine.
 */
internal object GalaxyScene {
    private val K = GalaxyGame.K

    private fun space(
        cv: PixelCanvas,
        g: GalaxyGame,
    ) {
        val w = cv.width.toDouble()
        cv.gradient(listOf(0 to 0x070820, 110 to 0x14104A, 200 to 0x2A1A66, 270 to 0x4A2A80))
        // nebula
        cv.glow(380.0, 70.0, 110.0, 0xFF6AB8, 0.09, 0.6)
        cv.glow(90.0, 130.0, 120.0, 0x4AB8FF, 0.08, 0.6)
        cv.glow(240.0, 240.0, 200.0, 0xA86AFF, 0.1, 0.35)
        // stars in three depths, falling gently
        val depths =
            listOf(
                doubleArrayOf(46.0, 6.0, 0.0, 3.0),
                doubleArrayOf(30.0, 16.0, 0.0, 5.0),
                doubleArrayOf(16.0, 34.0, 1.0, 8.0),
            )
        for (d in depths) {
            val n = d[0].toInt()
            val par = d[1]
            val size = d[2] > 0
            val seed = d[3]
            for (i in 0 until n) {
                val fi = i.toDouble()
                val x = arcadeHash(fi, seed) * w
                val y = (arcadeHash(fi, seed + 1) * 300 + g.t * par) % 290 - 10
                val tw = 0.5 + 0.5 * sin(g.t * (1 + arcadeHash(fi, seed + 2) * 2) + fi)
                cv.px(x, y, 0xFFFFFF, 0.35 + tw * 0.65)
                if (size) {
                    cv.px(x + 1, y, 0xFFFFFF, 0.35)
                    cv.px(x, y + 1, 0xFFFFFF, 0.35)
                }
            }
        }
        // big cream moon with a lime flag
        val my = 60 + ((g.t * 1.6) % 400) * 0 + sin(g.t * 0.15) * 3
        cv.glow(86.0, my, 60.0, 0xFFF4D0, 0.18, 1.0)
        cv.disc(86.0, my, 30.0, 0xEFE6CC)
        cv.disc(92.0, my - 3, 24.0, 0xF8F2E0)
        for ((dx, dy, r) in listOf(
            Triple(-14.0, 6.0, 6.0),
            Triple(10.0, 14.0, 4.0),
            Triple(-4.0, -14.0, 4.0),
            Triple(16.0, -6.0, 3.0),
        )) {
            cv.disc(86 + dx, my + dy, r, 0xD8CDB0)
            cv.disc(86 + dx - 1, my + dy - 1, r * 0.6, 0xE8DFC4)
        }
        cv.rect(96.0, my - 34, 1.0, 14.0, 0x8A6A4A)
        cv.poly(doubleArrayOf(97.0, my - 34, 108.0, my - 30, 97.0, my - 26), ArcadePalette.LIME)
        // ringed planet
        cv.disc(410.0, 210.0, 17.0, 0xE8A060)
        cv.disc(414.0, 206.0, 12.0, 0xF4C088, 0.9)
        var i = -12.0
        while (i <= 12) {
            cv.rect(396.0, 210 + i * 0.7, 28.0, 1.0, 0xC07A48, 0.5)
            i += 6
        }
        for (a in 0 until 72) {
            val t = a / 72.0 * PI * 2
            val x = 410 + cos(t) * 32
            val y = 210 + sin(t) * 7
            if (sin(t) > 0 || abs(x - 410) > 17) {
                cv.px(x, y, 0xF4DCAA)
                cv.px(x, y + 1, 0xC9A878, 0.7)
            }
        }
        // Cafe Vaini space station passing by
        val sx = 620 - (g.t * 9) % 820
        val sy = 150.0
        if (sx > -60 && sx < w + 20) {
            cv.rect(sx, sy, 36.0, 14.0, 0x3A2A56)
            cv.rect(sx + 2, sy + 2, 32.0, 10.0, 0x4A3A70)
            cv.rect(sx + 8, sy - 6, 20.0, 6.0, 0xE8D8B0)
            for (k in 0 until 4) cv.rect(sx + 5 + k * 8, sy + 5, 5.0, 4.0, if (k % 2 != 0) 0xFFD98A else 0x7AD0FF)
            cv.text("CAFE", sx + 9, sy - 5, 0x6A3A1A)
            cv.rect(sx - 8, sy + 5, 8.0, 3.0, 0x6A8AE0)
            cv.rect(sx + 36, sy + 5, 8.0, 3.0, 0x6A8AE0)
        }
        // comet
        val u = (g.t % 11) / 11
        if (u < 0.12) {
            val k = u / 0.12
            val x = 470 - k * 400
            val y = 20 + k * 90
            for (n in 0 until 18) cv.px(x + n * 2.4, y - n * 1.1, 0xBFE8FF, (1 - n / 18.0) * 0.9)
            cv.disc(x, y, 2.0, 0xFFFFFF)
        }
    }

    private fun enemy(
        cv: PixelCanvas,
        g: GalaxyGame,
        e: GalaxyEnemy,
    ) {
        val x = jsRound(e.x).toDouble()
        val y = jsRound(e.y).toDouble()
        val f = if (sin(g.t * 6 + e.wob) > 0) 1.0 else 0.0
        val white = e.flash > 0

        fun c(col: Int): Int = if (white) 0xFFFFFF else col

        fun angry(
            dx: Double = 0.0,
            dy: Double = 0.0,
        ) {
            cv.rect(x - 4 + dx, y - 1 + dy, 2.0, 3.0, 0x1A1414)
            cv.rect(x + 2 + dx, y - 1 + dy, 2.0, 3.0, 0x1A1414)
            cv.px(x - 5 + dx, y - 2 + dy, 0x1A1414)
            cv.px(x + 4 + dx, y - 2 + dy, 0x1A1414)
        }
        when (e.kind) {
            GalaxyEnemyKind.DONA -> {
                cv.disc(x, y, 9.0, c(0xD99A55))
                cv.disc(x, y - 1, 8.0, c(0xFF9DB0))
                cv.disc(x, y - 1, 8.0, c(0xFFB8C8), 0.35)
                val sprinkles =
                    listOf(
                        Triple(-5.0, -5.0, 0xFFFFFF),
                        Triple(4.0, -6.0, 0x7AD0FF),
                        Triple(6.0, 1.0, 0xFFF2A0),
                        Triple(-6.0, 3.0, 0xA6D62A),
                        Triple(1.0, 5.0, 0xFFFFFF),
                    )
                for ((dx, dy, col) in sprinkles) cv.rect(x + dx, y + dy, 2.0, 1.0, c(col))
                cv.disc(x, y + 1, 2.4, c(0x14104A))
                angry(0.0, -2.0)
            }
            GalaxyEnemyKind.TAZA -> {
                cv.rect(x - 8, y - 6, 15.0, 13.0, c(0xD9714A))
                cv.rect(x - 8, y - 6, 15.0, 2.0, c(0xF29A7A))
                cv.rect(x + 4, y - 6, 3.0, 13.0, c(0xB85A38))
                cv.stroke(x + 8, y - 2, x + 11, y + 2, 1.4, c(0xD9714A))
                cv.rect(x - 7, y - 5, 13.0, 1.0, c(0x5A3A26))
                angry(-1.0, 1.0)
                cv.rect(x - 3, y + 4, 6.0, 1.0, 0x5A3A26)
                for (k in 0 until 2) {
                    val u = (g.t * 1.2 + k * 0.5) % 1
                    cv.disc(x - 2 + k * 4 + sin(u * 6) * 2, y - 9 - u * 8, 1 + u, 0xFFFFFF, 0.5 * (1 - u))
                }
            }
            GalaxyEnemyKind.CRUASAN -> {
                // a fat crescent made of five rounded segments, tips curling down
                val seg =
                    listOf(
                        doubleArrayOf(-8.0, 3 - f, 3.0) to 0xD89840,
                        doubleArrayOf(-5.0, -1.0, 4.0) to 0xE8A848,
                        doubleArrayOf(0.0, -3.0, 5.0) to 0xF4BC5A,
                        doubleArrayOf(5.0, -1.0, 4.0) to 0xE8A848,
                        doubleArrayOf(8.0, 3 - f, 3.0) to 0xD89840,
                    )
                for ((s, _) in seg) cv.disc(x + s[0], y + s[1], s[2] + 1, c(0xA86A20))
                for ((s, col) in seg) cv.disc(x + s[0], y + s[1], s[2], c(col))
                for (dx in doubleArrayOf(-3.0, 3.0)) cv.stroke(x + dx, y - 6, x + dx * 1.1, y + 1, 0.3, c(0xC98A30))
                cv.px(x - 1, y - 6, c(0xFFF2C0))
                cv.px(x, y - 6, c(0xFFF2C0))
                angry(0.0, 0.0)
            }
        }
    }

    private fun boss(
        cv: PixelCanvas,
        g: GalaxyGame,
        b: GalaxyBoss,
    ) {
        val x = jsRound(b.x).toDouble()
        val y = jsRound(b.y).toDouble()
        val white = b.hit > 0

        fun c(col: Int): Int = if (white) 0xFFFFFF else col
        cv.rect(x - 34, y - 22, 68.0, 40.0, c(0x3A3A4A))
        cv.rect(x - 34, y - 22, 68.0, 4.0, c(0x6A6A80))
        cv.rect(x + 26, y - 22, 8.0, 40.0, c(0x2A2A38))
        cv.rect(x - 28, y - 14, 56.0, 14.0, c(0x1A1A26))
        cv.rect(x - 26, y - 12, 52.0, 10.0, c(0x2A2A3A))
        val blink = sin(g.t * 8) > 0
        val lamp = if (blink) 0xFFD0C8 else 0xC03A30
        cv.rect(x - 20, y - 9, 6.0, 6.0, 0xFF5A4A)
        cv.rect(x + 14, y - 9, 6.0, 6.0, 0xFF5A4A)
        cv.rect(x - 19, y - 8, 2.0, 2.0, lamp)
        cv.rect(x + 15, y - 8, 2.0, 2.0, lamp)
        cv.rect(x - 12, y - 6, 24.0, 2.0, 0xFF5A4A)
        cv.rect(x - 10, y - 4, 20.0, 1.0, 0xC03A30)
        for (i in 0 until 5) {
            cv.rect(
                x - 24 + i * 12,
                y + 4,
                8.0,
                3.0,
                if (i % 2 !=
                    0
                ) {
                    ArcadePalette.LIME
                } else {
                    ArcadePalette.LIME_SHADE
                },
            )
        }
        cv.rect(x - 10, y + 18, 20.0, 8.0, c(0xC8CCD4))
        cv.rect(x - 4, y + 26, 8.0, 6.0, c(0x8A90A0))
        for (k in 0 until 3) {
            val u = (g.t * 0.9 + k / 3.0) % 1
            cv.disc(x - 24 + k * 24, y - 26 - u * 12, 2 + u * 2, 0xFFFFFF, 0.45 * (1 - u))
        }
        // hp bar
        cv.rect(x - 30, y - 34, 60.0, 4.0, ArcadePalette.PANEL, 0.8)
        cv.rect(x - 29, y - 33, jsRound(58 * (b.hp.toDouble() / b.maxHp)).toDouble(), 2.0, 0xFF6A5A)
    }

    private fun ship(
        cv: PixelCanvas,
        g: GalaxyGame,
    ) {
        if (g.over && g.deadT > 0.05) return
        if (g.invuln > 0 && floor(g.t * 14).toInt() % 2 == 0) return
        val x = jsRound(g.x).toDouble()
        val y = K.SHIP_Y
        val lean = max(-3.0, min(3.0, g.lean * 4))
        // engine flames
        val fl = 4 + sin(g.t * 40) * 1.6
        for (dx in doubleArrayOf(-6.0, 6.0)) {
            cv.poly(doubleArrayOf(x + dx - 3, y + 16, x + dx + 3, y + 16, x + dx, y + 16 + fl + 5), 0xFF9A3A)
            cv.poly(doubleArrayOf(x + dx - 1.5, y + 16, x + dx + 1.5, y + 16, x + dx, y + 16 + fl), 0xFFF2A0)
        }
        cv.glow(x, y + 20, 26.0, 0xFF9A5A, 0.28, 0.6)
        // swept wings and tail fins
        for (m in doubleArrayOf(-1.0, 1.0)) {
            cv.poly(
                doubleArrayOf(
                    x + m * 7,
                    y - 3,
                    x + m * 35,
                    y + 13,
                    x + m * 35,
                    y + 17,
                    x + m * 30,
                    y + 17,
                    x + m * 7,
                    y + 11,
                ),
                ArcadePalette.LIME,
            )
            cv.poly(
                doubleArrayOf(x + m * 7, y - 3, x + m * 35, y + 13, x + m * 33, y + 13, x + m * 7, y - 1),
                ArcadePalette.LIME_HI,
            )
            cv.poly(doubleArrayOf(x + m * 7, y + 9, x + m * 30, y + 17, x + m * 7, y + 11), ArcadePalette.LIME_SHADE)
            cv.rect(x + m * 33 - (if (m < 0) 0 else 2), y + 13, 2.0, 5.0, 0xFF6A5A) // wing-tip light
            cv.poly(doubleArrayOf(x + m * 8, y + 8, x + m * 19, y + 22, x + m * 8, y + 20), 0x2455B0)
        }
        // fuselage
        cv.poly(doubleArrayOf(x - 11, y - 12, x + 11, y - 12, x + 10, y + 19, x - 10, y + 19), 0x2F6FE0)
        cv.rect(x - 11, y - 12, 4.0, 31.0, 0x5B8DE0)
        cv.rect(x + 6, y - 12, 5.0, 31.0, 0x2455B0)
        cv.rect(x - 10, y + 9, 20.0, 2.0, ArcadePalette.LIME)
        cv.rect(x - 10, y + 11, 20.0, 1.0, ArcadePalette.LIME_SHADE)
        for (dx in doubleArrayOf(-8.0, 3.0)) cv.rect(x + dx, y + 14, 5.0, 5.0, 0x5A5A70) // engines
        // Vaini seated: his feet sit below the dashboard, so only his upper body shows
        val firing = g.fireT > 0.15
        val pose = VainiPose()
        pose.armL = 0.25
        pose.armR = 0.25
        pose.legH = 3.0
        pose.blink = g.over
        pose.blush = if (firing) 0.9 else 0.4
        pose.lookX = 0.0
        pose.bob = jsRound(sin(g.t * 12) * 0.5).toDouble()
        VainiSprite.draw(cv, x + lean * 0.3, y - 4, pose)
        // dashboard rim over his lower body
        cv.rect(x - 14, y - 12, 28.0, 10.0, 0x2F6FE0)
        cv.rect(x - 14, y - 12, 28.0, 2.0, 0x86AEF0)
        cv.rect(x - 14, y - 4, 28.0, 2.0, 0x1A3F8A)
        cv.rect(x - 14, y - 6, 28.0, 2.0, ArcadePalette.LIME)
        cv.rect(x - 14, y - 12, 2.0, 10.0, 0x5B8DE0)
        cv.rect(x + 12, y - 12, 2.0, 10.0, 0x2455B0)
        cv.rect(x - 6, y - 10, 3.0, 2.0, 0xFFE45C)
        cv.rect(x + 3, y - 10, 3.0, 2.0, 0xFF6A5A) // little dashboard lights
        // glass canopy: pale tint, frame and a highlight
        for (yy in -42..-12) {
            val k = (yy + 12) / 30.0
            val half = jsRound(17 * sqrt(max(0.0, 1 - k * k))).toDouble()
            if (half > 0) cv.rect(x - half, y + yy, half * 2, 1.0, 0xBFE8FF, 0.2)
        }
        var a = PI
        while (a <= PI * 2) {
            val ex = x + cos(a) * 17
            val ey = y - 12 + sin(a) * 30
            cv.px(ex, ey, 0x7AB0D8)
            cv.px(ex, ey + 1, 0x4A7AA8, 0.7)
            a += 0.05
        }
        a = PI * 1.15
        while (a <= PI * 1.45) {
            cv.px(x + cos(a) * 13, y - 12 + sin(a) * 26, 0xFFFFFF, 0.9)
            a += 0.06
        }
        cv.rect(x - 1, y - 43, 2.0, 4.0, 0xD8CDB0) // nose tip
    }

    private fun bullets(
        cv: PixelCanvas,
        g: GalaxyGame,
    ) {
        // player shots: thick lime bolts with a white core and a dark edge so they read on any background
        for (p in g.shots) {
            cv.glow(p.x, p.y, 10.0, 0xD8FF70, 0.55, 1.8)
            cv.rect(p.x - 2, p.y - 6, 4.0, 12.0, 0x1A2A08)
            cv.rect(p.x - 1, p.y - 5, 2.0, 10.0, ArcadePalette.LIME_HI)
            cv.rect(p.x - 1, p.y - 5, 1.0, 10.0, 0xFFFFFF)
        }
        // enemy fire: hot coral orbs, outlined and pulsing, with a short trail, sized for a tiny screen
        for (b in g.bullets) {
            val pulse = 0.5 + 0.5 * sin(g.t * 22 + b.x * 0.3)
            for (i in 1..3) cv.disc(b.x - b.vx * 0.012 * i, b.y - i * 3.2, 3.6 - i * 0.7, 0xFF6A4A, 0.32 - i * 0.07)
            cv.glow(b.x, b.y, 12 + pulse * 2, 0xFF6A4A, 0.6, 1.0)
            cv.disc(b.x, b.y, 5.2, 0x1A0F2A)
            cv.disc(b.x, b.y, 4.4, 0xFF4A3A)
            cv.disc(b.x, b.y, 3.0, 0xFF9A70)
            cv.disc(b.x, b.y, 1.7, 0xFFF4E0)
            cv.px(b.x - 2, b.y - 2, 0xFFFFFF, 0.9)
        }
        for (d in g.drops) drop(cv, g, d)
    }

    private fun drop(
        cv: PixelCanvas,
        g: GalaxyGame,
        d: GalaxyDrop,
    ) {
        val bob = sin(g.t * 6 + d.x) * 1.5
        cv.glow(d.x, d.y + bob, 12.0, if (d.kind == GalaxyDrop.Kind.LEAF) 0xC6EC52 else 0xFF8AA0, 0.45, 1.0)
        if (d.kind == GalaxyDrop.Kind.LEAF) {
            cv.ellipse(d.x, d.y + bob, 6.0, 4.0, ArcadePalette.LIME)
            cv.ellipse(d.x - 1, d.y + bob - 1, 4.0, 2.0, ArcadePalette.LIME_HI)
            cv.stroke(d.x - 5, d.y + bob + 3, d.x + 5, d.y + bob - 3, 0.4, ArcadePalette.LIME_SHADE)
        } else {
            cv.rect(d.x - 4, d.y + bob - 3, 8.0, 4.0, 0xFF5A7A)
            cv.rect(d.x - 3, d.y + bob - 4, 2.0, 1.0, 0xFF5A7A)
            cv.rect(d.x + 1, d.y + bob - 4, 2.0, 1.0, 0xFF5A7A)
            cv.rect(d.x - 2, d.y + bob + 1, 5.0, 1.0, 0xFF5A7A)
            cv.px(d.x, d.y + bob + 2, 0xFF5A7A)
        }
    }

    /** Wave chip, hearts, shot power and the corner notice when a wave starts. */
    fun drawHudExtras(
        cv: PixelCanvas,
        g: GalaxyGame,
    ) {
        val label = "OLA ${max(1, g.wave)}"
        cv.rect(8.0, 8.0, PixelFont.width(label) + 10, 13.0, ArcadePalette.PANEL, 0.6)
        cv.text(label, 13.0, 12.0, ArcadePalette.LIME_HI)
        for (i in 0 until g.lives) {
            val x = 8.0 + i * 10
            val y = 25.0
            val c = 0xFF5A7A
            cv.rect(x, y + 1, 7.0, 3.0, c)
            cv.rect(x + 1, y, 2.0, 1.0, c)
            cv.rect(x + 4, y, 2.0, 1.0, c)
            cv.rect(x + 1, y + 4, 5.0, 1.0, c)
            cv.rect(x + 2, y + 5, 3.0, 1.0, c)
            cv.px(x + 3, y + 6, c)
        }
        for (i in 0 until 3) cv.rect(8.0 + i * 6, 37.0, 4.0, 3.0, if (i < g.power) ArcadePalette.LIME_HI else 0x4A4470)
        if (g.wave >= 1) {
            val boss = g.cfg?.boss ?: false
            cv.cornerToast(
                if (boss) "¡JEFE: CAFETERA!" else "OLA ${g.wave}",
                g.waveT,
                if (boss) 0xFF6A5A else ArcadePalette.LIME_HI,
                20.0,
                2.4,
            )
        }
    }

    fun draw(
        cv: PixelCanvas,
        g: GalaxyGame,
    ) {
        space(cv, g)
        for (e in g.enemies) if (e.t >= 0) enemy(cv, g, e)
        g.boss?.let { boss(cv, g, it) }
        bullets(cv, g)
        ship(cv, g)
    }
}
