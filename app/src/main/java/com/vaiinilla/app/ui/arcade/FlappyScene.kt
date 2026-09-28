package com.vaiinilla.app.ui.arcade

import com.vaiinilla.app.domain.arcade.FlappyGame
import com.vaiinilla.app.domain.arcade.clamp01
import com.vaiinilla.app.domain.arcade.jsRound
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Vuela: a whole day over the cafe valley (dawn, day, sunset, night). Vaini hangs from a coral balloon and the
 * pillars are giant pencils.
 */
internal object FlappyScene {
    /** Points needed to reach full night. */
    const val SPAN = 46.0

    private val K = FlappyGame.K

    private fun off(
        g: FlappyGame,
        par: Double,
        p: Double,
    ): Double = ((g.dist * par) % p + p) % p

    private fun dim(
        c: Int,
        d: Double,
    ): Int = mixColor(c, 0x0A1030, d * 0.62)

    private fun tint(
        c: Int,
        tod: Double,
    ): Int = mixColor(dim(c, SkyPalette.darkness(tod)), 0xFF8A5A, SkyPalette.warmth(tod) * 0.28)

    private fun mountains(
        cv: PixelCanvas,
        g: FlappyGame,
        tod: Double,
    ) {
        val p = 640.0
        val o = off(g, 0.05, p)
        val snow = tint(0xF4F6FF, tod)
        val rock = tint(0x8D9CC4, tod)
        for (x in 0 until cv.width) {
            val u = (x + o) / p * PI * 2
            val h = 150 + sin(u * 2 + 0.6) * 26 + sin(u * 5) * 9 + abs(sin(u * 3.3)) * 18
            var y = floor(h).toInt()
            while (y < 200) {
                cv.plot(x, y, if (y < h + 5 && h < 150) snow else rock, 1.0)
                y += 1
            }
            if (h < 140) {
                y = floor(h).toInt()
                while (y < h + 6) {
                    cv.plot(x, y, snow, 1.0)
                    y += 1
                }
            }
        }
    }

    private fun clouds(
        cv: PixelCanvas,
        g: FlappyGame,
        tod: Double,
    ) {
        val d = SkyPalette.darkness(tod)
        val warm = SkyPalette.warmth(tod)
        val col = mixColor(mixColor(0xFFFFFF, 0xFFC0A0, warm * 0.85), 0x3A4478, d * 0.85)
        val under = mixColor(col, 0x7A6A9A, 0.35)
        val layers =
            listOf(
                Triple(
                    0.07,
                    560.0,
                    listOf(
                        Triple(40.0, 44.0, 1.0),
                        Triple(210.0, 70.0, 0.8),
                        Triple(340.0, 30.0, 1.15),
                        Triple(500.0, 60.0, 0.9),
                    ),
                ),
                Triple(
                    0.13,
                    480.0,
                    listOf(Triple(100.0, 88.0, 0.7), Triple(300.0, 56.0, 0.6), Triple(440.0, 100.0, 0.75)),
                ),
            )
        for ((par, p, list) in layers) {
            val o = off(g, par, p)
            for ((cx, cy, k) in list) {
                for (rep in doubleArrayOf(-1.0, 0.0, 1.0)) {
                    val x = cx - o + rep * p
                    if (x < -50 || x > cv.width + 50) continue
                    cv.ellipse(x, cy, 26 * k, 8 * k, col, 0.92)
                    cv.ellipse(x - 12 * k, cy - 4 * k, 14 * k, 8 * k, col, 0.92)
                    cv.ellipse(x + 10 * k, cy - 6 * k, 12 * k, 8 * k, col, 0.92)
                    cv.ellipse(x, cy + 4 * k, 26 * k, 3 * k, under, 0.8)
                }
            }
        }
    }

    private fun balloonFar(
        cv: PixelCanvas,
        g: FlappyGame,
        tod: Double,
    ) {
        val p = 900.0
        val x = 700 - off(g, 0.09, p)
        val y = 92 + sin(g.t * 0.7) * 5
        val xx = if (x > -60) x else x + p
        if (xx < -40 || xx > cv.width + 40) return
        val d = SkyPalette.darkness(tod)
        for (i in -3..3) {
            cv.ellipse(
                xx + i * 4,
                y,
                6.0,
                14.0,
                if (i % 2 !=
                    0
                ) {
                    tint(ArcadePalette.CREAM, tod)
                } else {
                    tint(ArcadePalette.LIME, tod)
                },
            )
        }
        cv.ellipse(xx, y, 13.0, 14.0, tint(ArcadePalette.TERRACOTTA, tod), 0.35)
        cv.rect(xx - 4, y + 15, 8.0, 5.0, tint(0x8A5A38, tod))
        cv.stroke(xx - 4, y + 12, xx - 3, y + 15, 0.4, 0x5A4A3A)
        cv.stroke(xx + 4, y + 12, xx + 3, y + 15, 0.4, 0x5A4A3A)
        if (d > 0.3) cv.glow(xx, y + 16, 12.0, 0xFFCF7A, 0.5 * d, 1.0)
    }

    private fun birds(
        cv: PixelCanvas,
        g: FlappyGame,
        tod: Double,
    ) {
        if (SkyPalette.darkness(tod) > 0.55) return
        val a = 1 - SkyPalette.darkness(tod) * 1.6
        for (i in 0 until 6) {
            val fi = i.toDouble()
            val raw = (fi * 47 + 500 - g.t * (26 + fi * 2) - g.dist * 0.12) % 620
            val x = (raw + 620) % 620 - 60
            val y = 60 + (i % 3) * 14 + sin(g.t * 1.4 + fi) * 5
            val f = if (sin(g.t * 9 + fi * 1.7) > 0) 2.0 else -1.0
            cv.px(x, y, 0x3A3A50, a)
            cv.px(x - 1, y + f, 0x3A3A50, a)
            cv.px(x - 2, y + f * 1.5, 0x3A3A50, a)
            cv.px(x + 1, y + f, 0x3A3A50, a)
            cv.px(x + 2, y + f * 1.5, 0x3A3A50, a)
        }
    }

    private fun hills(
        cv: PixelCanvas,
        g: FlappyGame,
        tod: Double,
    ) {
        val layers =
            listOf(
                doubleArrayOf(0.18, 178.0, 12.0, 1.0),
                doubleArrayOf(0.32, 202.0, 9.0, 2.0),
            )
        val colors = listOf(intArrayOf(0xB9DC9A, 0xA3D08A), intArrayOf(0x8CC476, 0x7AB866))
        for ((n, layer) in layers.withIndex()) {
            val par = layer[0]
            val base = layer[1]
            val amp = layer[2]
            val seed = layer[3]
            val p = 480.0
            val o = off(g, par, p)
            val top = tint(colors[n][0], tod)
            val body = tint(colors[n][1], tod)
            for (x in 0 until cv.width) {
                val u = (x + o) / p * PI * 2
                val h = base + sin(u * 2 + seed) * amp + sin(u * 5 + seed * 2) * 4
                var y = floor(h).toInt()
                while (y < 246) {
                    cv.plot(x, y, if (y < h + 2) top else body, 1.0)
                    y += 1
                }
            }
        }
    }

    private fun windmill(
        cv: PixelCanvas,
        g: FlappyGame,
        tod: Double,
    ) {
        val p = 700.0
        val x0 = 260 - off(g, 0.28, p)
        val x = if (x0 < -30) x0 + p else x0
        if (x < -30 || x > cv.width + 30) return
        val by = 200.0
        cv.poly(doubleArrayOf(x - 6, by, x + 6, by, x + 3, by - 30, x - 3, by - 30), tint(0xEFE2C8, tod))
        cv.poly(doubleArrayOf(x - 6, by - 30, x + 6, by - 30, x, by - 38), tint(0xC9614A, tod))
        for (k in 0 until 4) {
            val a = g.t * 1.1 + k * PI / 2
            cv.stroke(x, by - 30, x + cos(a) * 20, by - 30 + sin(a) * 20, 0.9, tint(0xF4F0E4, tod))
            cv.stroke(
                x + cos(a) * 10,
                by - 30 + sin(a) * 10,
                x + cos(a) * 20,
                by - 30 + sin(a) * 20,
                1.8,
                tint(0xD8D0C0, tod),
                0.85,
            )
        }
        cv.disc(x, by - 30, 1.6, 0x5A4A3A)
    }

    private class House(
        val x: Double,
        val w: Double,
        val h: Double,
        val wall: Int,
        val roof: Int,
    )

    private val houses =
        listOf(
            House(0.0, 46.0, 34.0, 0xE9C79A, 0xC9614A),
            House(58.0, 40.0, 46.0, 0xF2DCC0, 0x8A5A4A),
            House(112.0, 56.0, 30.0, 0xE8B98A, 0xB04A3A),
            House(178.0, 44.0, 40.0, 0xF0D4A8, 0x6A8A5A),
            House(232.0, 52.0, 36.0, 0xECC9A0, 0xC9614A),
        )

    private fun town(
        cv: PixelCanvas,
        g: FlappyGame,
        tod: Double,
    ) {
        val p = 300.0
        val o = off(g, 0.55, p)
        val d = SkyPalette.darkness(tod)
        for (rep in -1..2) {
            for (house in houses) {
                val hw = house.w
                val hh = house.h
                val x0 = jsRound(house.x + rep * p - o).toDouble()
                if (x0 > cv.width || x0 + hw < 0) continue
                val y0 = 246 - hh
                val seed = (house.x + rep * 3).toInt().toDouble()
                cv.rect(x0, y0, hw, hh, tint(house.wall, tod))
                cv.rect(x0 + hw - 4, y0, 4.0, hh, tint(mixColor(house.wall, 0x000000, 0.12), tod))
                cv.poly(doubleArrayOf(x0 - 3, y0, x0 + hw / 2, y0 - 12, x0 + hw + 3, y0), tint(house.roof, tod))
                val lit = d > 0.35 && arcadeHash(seed, 3.0) > 0.25
                cv.rect(x0 + 6, y0 + 10, 7.0, 8.0, if (lit) 0xFFD98A else tint(0x7AB8D8, tod))
                if (lit) cv.glow(x0 + 9, y0 + 14, 12.0, 0xFFD98A, 0.35 * d, 1.0)
                if (hw > 44) {
                    val lit2 = d > 0.35 && arcadeHash(seed, 4.0) > 0.4
                    cv.rect(x0 + hw - 18, y0 + 10, 7.0, 8.0, if (lit2) 0xFFD98A else tint(0xFFD98A, tod))
                    if (lit2) cv.glow(x0 + hw - 14, y0 + 14, 12.0, 0xFFD98A, 0.35 * d, 1.0)
                }
                cv.rect(x0 + hw / 2 - 3, 246.0 - 14, 6.0, 14.0, tint(0x7A5232, tod))
                if (arcadeHash(seed, 6.0) > 0.55) { // chimney with smoke
                    cv.rect(x0 + hw - 12, y0 - 10, 5.0, 9.0, tint(0x8A5A4A, tod))
                    for (k in 0 until 4) {
                        val u = (g.t * 0.5 + k / 4.0 + arcadeHash(seed, 9.0)) % 1
                        cv.disc(
                            x0 + hw - 9 + sin(u * 6 + k) * 3,
                            y0 - 12 - u * 22,
                            1.5 + u * 2,
                            tint(0xFFFFFF, tod),
                            0.35 * (1 - u),
                        )
                    }
                }
            }
        }
    }

    private fun ground(
        cv: PixelCanvas,
        g: FlappyGame,
        tod: Double,
    ) {
        val d = SkyPalette.darkness(tod)
        val w = cv.width.toDouble()
        cv.rect(0.0, 246.0, w, 24.0, tint(0x6BB04F, tod))
        cv.rect(0.0, 246.0, w, 3.0, tint(ArcadePalette.LIME_HI, tod))
        cv.rect(0.0, 249.0, w, 1.0, tint(ArcadePalette.LIME_SHADE, tod))
        cv.rect(0.0, 252.0, w, 18.0, tint(0x5A9A44, tod))
        val o = off(g, 1.0, 24.0)
        var x = -24.0
        while (x < w + 24) {
            cv.poly(doubleArrayOf(x - o, 270.0, x - o + 12, 254.0, x - o + 24, 270.0), tint(0x4E8A3C, tod))
            cv.rect(x - o + 3, 247.0, 2.0, 3.0, tint(ArcadePalette.LIME_SHADE, tod))
            cv.rect(x - o + 15, 246.0, 2.0, 4.0, tint(ArcadePalette.LIME_SHADE, tod))
            x += 24
        }
        // little flowers
        val of = off(g, 1.0, 96.0)
        x = -96.0
        while (x < w + 96) {
            for ((dx, c) in listOf(20.0 to 0xFF8A9A, 58.0 to 0xFFE45C, 84.0 to 0xFFFFFF)) {
                cv.px(x + dx - of, 245.0, tint(c, tod))
                cv.px(x + dx - of, 246.0, tint(ArcadePalette.LEAF, tod))
            }
            x += 96
        }
        // street lamps that light up at dusk
        val p = 320.0
        val ol = off(g, 1.0, p)
        for (rep in -1..2) {
            val lx = jsRound(70 + rep * p - ol).toDouble()
            if (lx < -20 || lx > w + 20) continue
            cv.rect(lx, 214.0, 2.0, 32.0, 0x2A2030)
            cv.rect(lx - 3, 212.0, 8.0, 3.0, 0x2A2030)
            cv.rect(lx - 2, 214.0, 6.0, 2.0, if (d > 0.25) 0xFFF0B8 else 0xC8C0A0)
            if (d > 0.25) cv.glow(lx + 1, 226.0, 34.0, 0xFFD98A, 0.5 * d, 1.2)
        }
        // fireflies
        if (d > 0.4) {
            for (i in 0 until 14) {
                val fi = i.toDouble()
                val raw = (arcadeHash(fi, 21.0) * 560 - g.dist * (0.5 + arcadeHash(fi, 22.0) * 0.4)) % 560
                val bx = (raw + 560) % 560 - 40
                val by = 190 + arcadeHash(fi, 23.0) * 50 + sin(g.t * 1.5 + fi) * 6
                val a = (0.4 + 0.6 * sin(g.t * 3 + fi * 2)) * d
                cv.glow(bx, by, 5.0, 0xE8FF8A, 0.6 * clamp01(a), 1.0)
                cv.px(bx, by, 0xF4FFB0, clamp01(a))
            }
        }
    }

    private fun pencil(
        cv: PixelCanvas,
        x: Double,
        edge: Double,
        hangsDown: Boolean,
        tod: Double,
    ) {
        val w = K.PILLAR_W

        fun c(col: Int): Int = tint(col, tod)

        fun yAt(d: Double): Double = edge + (if (hangsDown) -d else d)

        fun band(
            d0: Double,
            d1: Double,
            fn: (Double, Double) -> Unit,
        ) {
            val a = minOf(yAt(d0), yAt(d1))
            val b = maxOf(yAt(d0), yAt(d1))
            if (b < -5 || a > 275) return
            fn(floor(a), Math.ceil(b) - floor(a))
        }
        for (dd in 0 until 24) {
            val fd = dd.toDouble()
            val half = (w / 2) * (0.22 + 0.78 * (fd / 24))
            val y = yAt(fd) - (if (hangsDown) 1 else 0)
            cv.rect(
                x + w / 2 - half,
                y,
                half * 2,
                1.0,
                if (dd <
                    7
                ) {
                    c(0x3A3A44)
                } else if (dd % 6 < 3) {
                    c(0xF0C98A)
                } else {
                    c(0xE6B878)
                },
            )
            if (dd >= 7) {
                cv.px(x + w / 2 - half, y, c(0xC9925A))
                cv.px(x + w / 2 + half - 1, y, c(0xC9925A))
            }
        }
        band(24.0, 2000.0) { y, h ->
            cv.rect(x, y, w, h, c(ArcadePalette.LIME))
            cv.rect(x, y, 5.0, h, c(ArcadePalette.LIME_HI))
            cv.rect(x + 5, y, 3.0, h, c(0xB9E13C))
            cv.rect(x + w - 8, y, 8.0, h, c(0x8DBB20))
            cv.rect(x + w - 4, y, 4.0, h, c(ArcadePalette.LIME_SHADE))
            cv.rect(x + w / 2 - 1, y, 2.0, h, c(0x8DBB20))
        }
        band(24.0, 30.0) { y, h -> cv.rect(x, y, w, h, c(0xE9F7A6)) }
    }

    private fun pillars(
        cv: PixelCanvas,
        g: FlappyGame,
        tod: Double,
    ) {
        for (p in g.pillars) {
            val x = jsRound(p.x).toDouble()
            if (x < -40 || x > cv.width + 10) continue
            val top = p.gapY - p.gap / 2
            val bot = p.gapY + p.gap / 2
            pencil(cv, x, top, true, tod)
            pencil(cv, x, bot, false, tod)

            fun c(col: Int): Int = tint(col, tod)
            cv.rect(x, 0.0, K.PILLAR_W, 8.0, c(0xFF9DB0))
            cv.rect(x, 8.0, K.PILLAR_W, 5.0, c(0xC8CCD4))
            cv.rect(x, 10.0, K.PILLAR_W, 1.0, c(0x8A90A0))
            cv.rect(x, 13.0, K.PILLAR_W, 1.0, c(0x7A8090))
            cv.rect(x, 244.0, K.PILLAR_W, 2.0, c(ArcadePalette.LIME_SHADE), 0.6)
            if (p.amp > 0) { // moving pencils carry a tiny arrow
                val dir = if (cos(g.t * p.freq + p.phase) > 0) 1.0 else -1.0
                val ay = p.gapY
                cv.poly(doubleArrayOf(x + 15, ay - dir * 4, x + 11, ay + dir * 1, x + 19, ay + dir * 1), 0xFFFFFF, 0.55)
            }
        }
    }

    private fun player(
        cv: PixelCanvas,
        g: FlappyGame,
        tod: Double,
    ) {
        val x = K.X
        val y = jsRound(g.y).toDouble()
        val d = SkyPalette.darkness(tod)
        val flap = if (g.flapT < 0.18) 1 - g.flapT / 0.18 else 0.0
        val sway = sin(g.t * 4) * 2
        val bx = x + 2 + sway - g.vy * 0.02
        val by = y - 46 - flap * 3
        cv.stroke(bx, by + 12, x + 6, y - 10, 0.5, 0x6A5A5A)
        if (d > 0.3) cv.glow(bx, by, 34.0, 0xFF9A7A, 0.35 * d, 1.0) // the balloon glows at night
        cv.ellipse(bx, by, 12.0, 14.0, ArcadePalette.CORAL)
        cv.ellipse(bx - 3, by - 4, 8.0, 9.0, 0xFF8A7A)
        cv.disc(bx - 5, by - 7, 2.0, 0xFFE0D8)
        cv.poly(doubleArrayOf(bx - 3, by + 13, bx + 3, by + 13, bx, by + 17), 0xD94A3A)
        cv.ellipse(bx + 4, by + 3, 7.0, 8.0, 0xD94A3A, 0.35)
        val pose = VainiPose()
        pose.armL = 0.5 + flap * 0.8
        pose.armR = 2.7
        pose.legH = 5.0
        pose.blink = g.over && g.deadT < 0.4
        pose.blush = if (g.over) 0.0 else 0.6 + flap
        pose.lookX = 1.0
        pose.walk = g.t * 10 + 1
        VainiSprite.draw(cv, x, y + 17, pose)
    }

    /** Level chip at the top left and the corner notice when the level rises. */
    fun drawHudExtras(
        cv: PixelCanvas,
        g: FlappyGame,
    ) {
        val label = "NIV ${g.level}"
        cv.rect(8.0, 8.0, PixelFont.width(label) + 10, 13.0, ArcadePalette.PANEL, 0.6)
        cv.text(label, 13.0, 12.0, ArcadePalette.LIME_HI)
        if (g.level > 1) cv.cornerToast(if (g.level == 3) "NIVEL 3: ¡SE MUEVEN!" else "NIVEL ${g.level}", g.levelT)
    }

    fun draw(
        cv: PixelCanvas,
        g: FlappyGame,
    ) {
        val tod = SkyPalette.timeOfDay(g.score, SPAN)
        SkyPalette.drawSky(cv, tod, 246)
        SkyPalette.drawStars(cv, tod, g.t, 170.0, 5.0)
        SkyPalette.drawCelestial(cv, tod)
        SkyPalette.drawShootingStar(cv, tod, g.t)
        mountains(cv, g, tod)
        clouds(cv, g, tod)
        balloonFar(cv, g, tod)
        birds(cv, g, tod)
        hills(cv, g, tod)
        windmill(cv, g, tod)
        town(cv, g, tod)
        pillars(cv, g, tod)
        ground(cv, g, tod)
        player(cv, g, tod)
    }
}
