package com.vaiinilla.app.ui.arcade

import com.vaiinilla.app.domain.arcade.GravityGame
import com.vaiinilla.app.domain.arcade.jsRound
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

/** Gravedad: night bus 17 in the rain. Vaini sprints on the floor or the ceiling; seats and bags below, umbrellas and straps above. */
internal object GravityScene {
    private val K = GravityGame.K

    private fun off(
        g: GravityGame,
        par: Double,
        p: Double,
    ): Double = ((g.dist * par) % p + p) % p

    private fun city(
        cv: PixelCanvas,
        g: GravityGame,
    ) {
        cv.gradient(listOf(K.CEIL.toInt() to 0x141A3A, 130 to 0x2A2A5A, K.FLOOR.toInt() to 0x4A3A6A))
        val layers =
            listOf(
                doubleArrayOf(0.12, 70.0, 100.0, 6.0) to 0x241F4A,
                doubleArrayOf(0.25, 96.0, 140.0, 8.0) to 0x1A1A3C,
            )
        for ((li, layer) in layers.withIndex()) {
            val (v, col) = layer
            val par = v[0]
            val minH = v[1]
            val maxH = v[2]
            val seed = v[3]
            val o = off(g, par, 480.0)
            val spread = if (li == 1) 0.7 else 0.5
            for (x in 0 until cv.width) {
                val u = floor((x + o) / 40)
                val h = minH + arcadeHash(u, seed) * (maxH - minH) * spread
                var y = floor(K.FLOOR - 24 - h).toInt()
                while (y < K.FLOOR - 24) {
                    cv.plot(x, y, col, 1.0)
                    y += 1
                }
            }
            for (x in 0 until cv.width step 4) {
                val u = floor((x + o) / 40)
                val h = minH + arcadeHash(u, seed) * (maxH - minH) * spread
                var wy = floor(K.FLOOR - 24 - h) + 6
                while (wy < K.FLOOR - 30) {
                    if (arcadeHash(x + o, wy) > 0.72) cv.rect(x.toDouble(), wy, 2.0, 3.0, 0xFFD98A, 0.75)
                    wy += 9
                }
            }
        }
        // bokeh street lights (blurry discs)
        val p = 520.0
        val o = off(g, 0.6, p)
        val colors = intArrayOf(0xFFD98A, 0xFF8A7A, 0x8AD0FF, 0xFFE6A0)
        for (i in 0 until 9) {
            val fi = i.toDouble()
            val x = ((arcadeHash(fi, 40.0) * p - o) % p + p) % p - 20
            val y = 90 + arcadeHash(fi, 41.0) * 100
            val r = 6 + arcadeHash(fi, 42.0) * 10
            val col = colors[i % 4]
            cv.glow(x, y, r * 2.2, col, 0.32, 1.0)
            cv.disc(x, y, r * 0.5, col, 0.5)
        }
    }

    private fun rain(
        cv: PixelCanvas,
        g: GravityGame,
    ) {
        val w = cv.width.toDouble()
        for (i in 0 until 90) {
            val fi = i.toDouble()
            val sp = 180 + arcadeHash(fi, 1.0) * 160
            val x0 = arcadeHash(fi, 2.0) * (w + 80)
            val y = (arcadeHash(fi, 3.0) * 300 + g.t * sp) % 300
            val x = (x0 - y * 0.35 - g.dist * 0.5 * (0.4 + arcadeHash(fi, 4.0))) % (w + 80)
            val xx = (x + w + 80) % (w + 80) - 40
            if (y < K.CEIL || y > K.FLOOR) continue
            cv.stroke(xx, y, xx - 2, y + 7, 0.4, 0xB8C8FF, 0.4)
        }
        // droplets sliding down the glass
        for (i in 0 until 14) {
            val fi = i.toDouble()
            val x = arcadeHash(fi, 9.0) * w
            val y = K.CEIL + 8 + (arcadeHash(fi, 10.0) * 200 + g.t * (10 + arcadeHash(fi, 11.0) * 14)) % 170
            cv.px(x, y, 0xD8E4FF, 0.55)
            cv.px(x, y - 1, 0xD8E4FF, 0.3)
        }
    }

    private fun frame(
        cv: PixelCanvas,
        g: GravityGame,
    ) {
        // window pillars pass by quickly
        val p = 240.0
        val o = off(g, 1.0, p)
        for (rep in -1..2) {
            val x = jsRound(rep * p + 150 - o).toDouble()
            cv.rect(x, K.CEIL, 8.0, K.FLOOR - K.CEIL, 0x2A2848)
            cv.rect(x, K.CEIL, 2.0, K.FLOOR - K.CEIL, 0x4A4878)
            cv.rect(x + 6, K.CEIL, 2.0, K.FLOOR - K.CEIL, 0x1A1830)
        }
        // glass reflection sheen
        for (i in 0 until 3) {
            val raw = (i * 180 + 40 - g.dist * 0.05) % 540
            val x = (raw + 540) % 540 - 40
            cv.poly(doubleArrayOf(x, K.CEIL, x + 22, K.CEIL, x + 6, K.FLOOR, x - 16, K.FLOOR), 0xFFFFFF, 0.05)
        }
    }

    private fun busCeiling(cv: PixelCanvas) {
        val w = cv.width.toDouble()
        cv.rect(0.0, 0.0, w, K.CEIL, 0xE9E2D2)
        cv.rect(0.0, K.CEIL - 4, w, 4.0, 0xC9C0AC)
        cv.rect(0.0, K.CEIL - 1, w, 1.0, 0x8A8270)
        // lamp strip
        cv.rect(0.0, 10.0, w, 8.0, 0xFFF6D0)
        cv.rect(0.0, 10.0, w, 2.0, 0xFFFFFF)
        cv.rect(0.0, 17.0, w, 1.0, 0xE8D9A0)
        cv.glow(240.0, 30.0, 260.0, 0xFFE9A8, 0.09, 0.28)
        // handrail
        cv.rect(0.0, 28.0, w, 3.0, 0xC8CCD4)
        cv.rect(0.0, 28.0, w, 1.0, 0xFFFFFF)
        cv.rect(0.0, 30.0, w, 1.0, 0x8A90A0)
        // route sign 17
        cv.rect(20.0, 3.0, 46.0, 20.0, 0x1A1A1A)
        cv.rect(22.0, 5.0, 42.0, 16.0, 0x2A2A2A)
        cv.text("17", 26.0, 7.0, 0xFFB43A, 1.0, 3.0)
        cv.text("VAINI", 46.0, 14.0, 0xFFB43A, 0.9)
    }

    private fun busFloor(
        cv: PixelCanvas,
        g: GravityGame,
    ) {
        val w = cv.width.toDouble()
        val h = cv.height.toDouble()
        cv.rect(0.0, K.FLOOR, w, h - K.FLOOR, 0x2C2844)
        cv.rect(0.0, K.FLOOR, w, 3.0, 0x4A4670)
        cv.rect(0.0, K.FLOOR + 3, w, 1.0, 0x1C1A30)
        cv.rect(0.0, K.FLOOR + 10, w, 4.0, 0xF2C74A, 0.85)
        cv.rect(0.0, K.FLOOR + 10, w, 1.0, 0xFFE388)
        val o = off(g, 1.0, 16.0)
        var x = -16.0
        while (x < w + 16) {
            cv.rect(x - o, K.FLOOR + 18, 2.0, h - K.FLOOR - 18, 0x231F38)
            x += 16
        }
        // seat shadows under the windows
        cv.rect(0.0, K.FLOOR - 4, w, 4.0, 0x1A1830, 0.55)
    }

    private fun obstacle(
        cv: PixelCanvas,
        g: GravityGame,
        o: GravityGame.Obstacle,
    ) {
        val x = jsRound(o.x).toDouble()
        val w = o.w
        if (o.onFloor) {
            val y = K.FLOOR - K.OBSTACLE_H
            if (o.kind == GravityGame.Kind.SEAT) {
                cv.rect(x, y + 14, w, 20.0, 0x3A6AC8)
                cv.rect(x, y + 14, w, 2.0, 0x6A94EE)
                cv.rect(x + w - 4, y + 14, 4.0, 20.0, 0x2A4A98)
                cv.rect(x + 2, y, w - 4, 16.0, 0x4A7AD8)
                cv.rect(x + 2, y, w - 4, 2.0, 0x7AA4F4)
                cv.rect(x + w - 6, y, 4.0, 16.0, 0x2A4A98)
                var i = 4.0
                while (i < w - 4) {
                    cv.rect(x + i, y + 4, 1.0, 10.0, 0x3A62B8)
                    i += 8
                }
                cv.rect(x + w / 2 - 2, y - 3, 4.0, 4.0, 0xC8CCD4)
            } else {
                val bw = min(w, 34.0)
                cv.rect(x, y + 6, bw, 28.0, ArcadePalette.CORAL)
                cv.rect(x, y + 6, bw, 2.0, 0xFF9A8A)
                cv.rect(x + bw - 4, y + 6, 4.0, 28.0, 0xC9443A)
                cv.rect(x + 3, y + 14, bw - 10, 8.0, 0xE0503F)
                cv.rect(x + 3, y + 14, bw - 10, 1.0, 0xFF9A8A)
                cv.rect(x + 5, y + 18, 4.0, 2.0, ArcadePalette.LIME)
                cv.rect(x + 6, y + 2, bw - 12, 5.0, 0x3A2A2A)
                cv.rect(x + 8, y + 4, bw - 16, 2.0, 0x5A4A4A)
            }
        } else {
            val y0 = K.CEIL
            if (o.kind == GravityGame.Kind.UMBRELLA) {
                val cx = x + w / 2
                cv.stroke(cx, y0, cx, y0 + 12, 0.8, 0xC8CCD4)
                cv.poly(
                    doubleArrayOf(cx - 7, y0 + 12, cx + 7, y0 + 12, cx + 5, y0 + 30, cx - 5, y0 + 30),
                    ArcadePalette.MUSTARD,
                )
                cv.rect(cx - 7, y0 + 12, 14.0, 2.0, 0xF8D488)
                cv.rect(cx + 2, y0 + 14, 4.0, 16.0, 0xC99A3A)
                cv.stroke(cx, y0 + 30, cx, y0 + 35, 0.7, 0xC8CCD4)
                if (arcadeHash(x, 3.0) > 0.3) cv.px(cx + 1, y0 + 37 + (g.t * 20) % 6, 0x9EC8FF, 0.8)
                // a small bag hanging beside it to fill the width
                if (w > 40) {
                    cv.rect(x + 4, y0 + 20, 12.0, 14.0, ArcadePalette.MOTO)
                    cv.rect(x + 4, y0 + 20, 12.0, 2.0, 0x6A9AF0)
                    cv.stroke(x + 10, y0, x + 10, y0 + 20, 0.6, 0xC8CCD4)
                }
            } else {
                // pair of straps with lime grips
                for (sx in doubleArrayOf(x + 6, x + w - 8)) {
                    cv.stroke(sx + 1, y0, sx + 1, y0 + 20, 1.2, 0x2A2A3A)
                    cv.rect(sx - 4, y0 + 20, 10.0, 12.0, 0x2A2A3A)
                    cv.rect(sx - 3, y0 + 23, 8.0, 6.0, ArcadePalette.LIME)
                    cv.rect(sx - 3, y0 + 23, 8.0, 1.0, ArcadePalette.LIME_HI)
                }
            }
        }
    }

    private fun player(
        cv: PixelCanvas,
        g: GravityGame,
    ) {
        val y = g.y
        val feetDown = g.direction > 0
        val grounded = abs(g.vy) < 1
        val run = g.started && grounded && !g.over
        val anchor = if (feetDown) y + K.HALF_H else y - K.HALF_H // feet line
        val shadowY = if (feetDown) K.FLOOR else K.CEIL
        cv.ellipse(K.X, shadowY + (if (feetDown) 1 else -1), if (grounded) 15.0 else 8.0, 2.4, 0x000000, 0.4)
        val pose = VainiPose()
        pose.armL = if (!grounded) 2.4 else 0.8 + sin(g.t * 14) * 0.7
        pose.armR = if (!grounded) 2.2 else 0.8 - sin(g.t * 14) * 0.7
        pose.blink = g.over
        pose.blush = if (grounded) 0.3 else 1.0
        pose.lookX = 1.0
        pose.walk = if (run) g.t * 14 else null
        pose.bob = if (run) jsRound(sin(g.t * 14)).toDouble() else 0.0
        pose.legH = 6.0
        if (feetDown) {
            VainiSprite.draw(cv, K.X, jsRound(anchor).toDouble(), pose)
        } else {
            VainiSprite.drawUpsideDown(cv, K.X, jsRound(anchor).toDouble(), pose)
        }
    }

    fun draw(
        cv: PixelCanvas,
        g: GravityGame,
    ) {
        city(cv, g)
        rain(cv, g)
        frame(cv, g)
        busCeiling(cv)
        busFloor(cv, g)
        for (o in g.obstacles) obstacle(cv, g, o)
        player(cv, g)
    }
}
