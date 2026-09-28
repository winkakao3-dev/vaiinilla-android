package com.vaiinilla.app.ui.arcade

import com.vaiinilla.app.arcade.StackGame
import com.vaiinilla.app.arcade.clamp01
import com.vaiinilla.app.arcade.jsRound
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

/**
 * Apila: a tower of cafe crates that climbs from the table through clouds and dusk into space. Vaini rides the
 * swinging crate.
 */
internal object StackScene {
    /** Blocks needed to reach full night. */
    const val SPAN = 42.0

    private val K = StackGame.K

    private class Palette(
        val tod: Double,
        val alt: Double,
        val colors: IntArray,
    )

    /** (face, highlight, shade) */
    private val crates =
        listOf(
            intArrayOf(0xF0C98A, 0xF8DBA8, 0xC9A066),
            intArrayOf(0xE8805A, 0xF29A7A, 0xB85A38),
            intArrayOf(0x5B8DE0, 0x86AEF0, 0x3A68B8),
            intArrayOf(0x8FCF5A, 0xB3E77E, 0x66A03A),
            intArrayOf(0xC8A8E8, 0xDCC4F4, 0x9A78C0),
            intArrayOf(0xF0D060, 0xF8E488, 0xC8A83A),
        )
    private val space = intArrayOf(0x04061A, 0x0A1040, 0x161A5A, 0x2A2470)

    private fun easeOut(t: Double): Double = 1 - (1 - t).pow(3)

    /** Sky palette: time of day first, then blended toward deep space with altitude. */
    private fun palette(g: StackGame): Palette {
        val tod = SkyPalette.timeOfDay(g.score, SPAN)
        val base = SkyPalette.colors(tod)
        val u = clamp01((g.cam - 260) / 760)
        return Palette(tod, u, IntArray(4) { mixColor(base[it], space[it], u * 0.92) })
    }

    private fun screenY(
        g: StackGame,
        wy: Double,
        par: Double,
    ): Double = wy + g.cam * par

    private fun cloudLayer(
        cv: PixelCanvas,
        g: StackGame,
        pal: Palette,
        par: Double,
        seed: Double,
        every: Double,
        alpha: Double,
    ) {
        val d = max(SkyPalette.darkness(pal.tod), pal.alt * 0.8)
        val warm = SkyPalette.warmth(pal.tod) * (1 - pal.alt)
        val col = mixColor(mixColor(0xFFFFFF, 0xFFC0A0, warm * 0.85), 0x3A4478, d * 0.85)
        val under = mixColor(col, 0x7A6A9A, 0.35)
        for (i in 0 until 30) {
            val fi = i.toDouble()
            val y = screenY(g, 190 - fi * every, par)
            if (y < -20 || y > 290) continue
            val k = 0.7 + arcadeHash(fi, seed) * 0.8
            val raw = (arcadeHash(fi, seed + 1) * 620 - g.t * (3 + arcadeHash(fi, seed + 2) * 5)) % 620
            val x = (raw + 620) % 620 - 70
            val a = alpha * (1 - pal.alt * 0.9)
            if (a < 0.05) continue
            cv.ellipse(x, y, 26 * k, 7 * k, col, 0.9 * a)
            cv.ellipse(x - 11 * k, y - 4 * k, 13 * k, 7 * k, col, 0.9 * a)
            cv.ellipse(x + 10 * k, y - 5 * k, 12 * k, 7 * k, col, 0.9 * a)
            cv.ellipse(x, y + 4 * k, 26 * k, 2.5 * k, under, 0.7 * a)
        }
    }

    private class Tower(
        val x: Double,
        val w: Double,
        val h: Double,
        val color: Int,
    )

    private val towers =
        listOf(
            Tower(0.0, 60.0, 46.0, 0x9AB8A0),
            Tower(58.0, 40.0, 70.0, 0x8AA88F),
            Tower(98.0, 64.0, 38.0, 0xA4C0A8),
            Tower(160.0, 44.0, 60.0, 0x8AA88F),
            Tower(204.0, 70.0, 44.0, 0x9AB8A0),
            Tower(274.0, 46.0, 80.0, 0x8AA88F),
            Tower(320.0, 60.0, 40.0, 0xA4C0A8),
            Tower(380.0, 44.0, 62.0, 0x9AB8A0),
            Tower(424.0, 60.0, 48.0, 0x8AA88F),
        )

    private fun skyline(
        cv: PixelCanvas,
        g: StackGame,
        pal: Palette,
    ) {
        val y0 = 236 + g.cam * 0.5
        if (y0 > 330) return
        val d = SkyPalette.darkness(pal.tod)
        val dark = mixColor(0x6A8A80, 0x10182C, d * 0.6)
        for (t in towers) {
            val col = mixColor(mixColor(t.color, 0xFF9A70, SkyPalette.warmth(pal.tod) * 0.25), 0x10182C, d * 0.7)
            cv.rect(t.x, y0 - t.h, t.w, t.h + 60, col)
            var wy = y0 - t.h + 6
            while (wy < y0 - 4) {
                var wx = t.x + 5
                while (wx < t.x + t.w - 6) {
                    val on = if (d > 0.3) arcadeHash(wx, wy) > 0.45 else (arcadeHash(wx, wy) > 0.5 && d > 0)
                    cv.rect(wx, wy, 4.0, 5.0, if (on) 0xFFE0A0 else dark, if (on) 0.95 else 0.85)
                    wx += 9
                }
                wy += 9
            }
        }
        // lit signs on two towers at night
        if (d > 0.4) {
            cv.glow(80.0, y0 - 60, 24.0, 0xFF8AD0, 0.3 * d, 1.0)
            cv.glow(296.0, y0 - 70, 24.0, 0x7AD0FF, 0.3 * d, 1.0)
        }
    }

    private fun plane(
        cv: PixelCanvas,
        g: StackGame,
        pal: Palette,
    ) {
        val y = screenY(g, -110.0, 0.55)
        if (y < -10 || y > 280 || pal.alt > 0.7) return
        val x = (g.t * 32) % 700 - 100
        cv.rect(x - 8, y, 16.0, 3.0, 0xF4F4F8)
        cv.rect(x + 6, y - 1, 4.0, 2.0, 0xF4F4F8)
        cv.poly(doubleArrayOf(x - 1, y, x - 6, y - 5, x - 3, y), 0xC8CCD8)
        cv.poly(doubleArrayOf(x - 8, y, x - 12, y - 4, x - 9, y), 0xC8CCD8)
        cv.rect(x - 2, y + 1, 5.0, 1.0, 0x8FB8F0)
        for (i in 1 until 30) cv.px(x - 10 - i * 2, y + 1, 0xFFFFFF, 0.5 * (1 - i / 30.0))
    }

    private fun balloons(
        cv: PixelCanvas,
        g: StackGame,
        pal: Palette,
    ) {
        for ((wx, wy, k) in listOf(
            Triple(110.0, -210.0, 1.0),
            Triple(370.0, -380.0, 0.8),
            Triple(230.0, -560.0, 0.9),
        )) {
            val y = screenY(g, wy, 0.5) + sin(g.t * 0.8 + wx) * 4
            val x = wx + sin(g.t * 0.3 + wy) * 6
            if (y < -30 || y > 300 || pal.alt > 0.85) continue
            val a = 1 - pal.alt
            for (i in -3..3) {
                cv.ellipse(
                    x + i * 4 * k,
                    y,
                    6 * k,
                    14 * k,
                    if (i % 2 !=
                        0
                    ) {
                        ArcadePalette.CREAM
                    } else {
                        ArcadePalette.LIME
                    },
                    a,
                )
            }
            cv.ellipse(x, y, 13 * k, 14 * k, ArcadePalette.CORAL, 0.25 * a)
            cv.rect(x - 4 * k, y + 15 * k, 8 * k, 5 * k, 0x8A5A38, a)
            if (SkyPalette.darkness(pal.tod) >
                0.3
            ) {
                cv.glow(x, y + 17 * k, 14.0, 0xFFCF7A, 0.5 * SkyPalette.darkness(pal.tod), 1.0)
            }
        }
    }

    private fun aurora(
        cv: PixelCanvas,
        g: StackGame,
        pal: Palette,
    ) {
        val u = clamp01((pal.alt - 0.25) / 0.5) * SkyPalette.darkness(pal.tod + 0.2)
        if (u < 0.05) return
        var x = 0.0
        while (x < cv.width) {
            val base = 96 + sin(x * 0.02 + g.t * 0.5) * 20 + sin(x * 0.05 - g.t * 0.9) * 8
            val hh = 46 + sin(x * 0.03 + g.t) * 14
            var y = base
            while (y < base + hh) {
                cv.px(x, y, mixColor(0x7AFFC0, 0xB58AFF, clamp01((y - base) / hh)), 0.16 * u * (1 - (y - base) / hh))
                y += 2
            }
            cv.px(x, base, 0xB0FFE0, 0.22 * u)
            x += 2
        }
    }

    private fun planets(
        cv: PixelCanvas,
        g: StackGame,
        pal: Palette,
    ) {
        val u = clamp01((pal.alt - 0.32) / 0.3)
        if (u < 0.05) return
        // ringed planet
        val px0 = 372.0
        val py0 = screenY(g, -150.0, 0.22)
        if (py0 > -40 && py0 < 320) {
            cv.disc(px0, py0, 16.0, 0xE8B070, u)
            cv.disc(px0 + 4, py0 - 3, 12.0, 0xF4C890, u * 0.9)
            var i = -14.0
            while (i <= 14) {
                cv.rect(px0 - 14, py0 + i * 0.6, 28.0, 1.0, 0xC98A58, u * 0.5)
                i += 7
            }
            for (a in 0 until 64) {
                val t = a / 64.0 * PI * 2
                val x = px0 + cos(t) * 30
                val y = py0 + sin(t) * 7
                if (sin(t) > 0 || abs(x - px0) > 16) {
                    cv.px(x, y, 0xF0D8A8, u * 0.85)
                    cv.px(x, y + 1, 0xC9A878, u * 0.6)
                }
            }
        }
        // small blue-green planet
        val qy = screenY(g, -120.0, 0.3)
        if (qy > -20 && qy < 300) {
            cv.disc(90.0, qy, 9.0, 0x3A78E0, u)
            cv.disc(87.0, qy - 2, 5.0, 0x5AD0A0, u * 0.8)
            cv.disc(93.0, qy + 3, 3.0, 0xF4F4FF, u * 0.7)
        }
        // satellite
        val sy = screenY(g, -280.0, 0.4)
        val sx = (g.t * 14) % 620 - 60
        if (sy > 0 && sy < 270) {
            cv.rect(sx, sy, 5.0, 3.0, 0xC8CCD8, u)
            cv.rect(sx - 6, sy, 5.0, 2.0, 0x5B8DE0, u)
            cv.rect(sx + 6, sy, 5.0, 2.0, 0x5B8DE0, u)
            cv.px(sx + 2, sy - 2, 0xFF6A5A, u * (if (sin(g.t * 6) > 0) 1.0 else 0.2))
        }
    }

    private fun table(
        cv: PixelCanvas,
        g: StackGame,
    ) {
        val y = K.GROUND + g.cam
        if (y > 300) return
        val w = cv.width.toDouble()
        cv.rect(0.0, y, w, 10.0, 0xA8703F)
        cv.rect(0.0, y, w, 2.0, 0xD19A62)
        cv.rect(0.0, y + 8, w, 2.0, 0x7A4A28)
        var x = 0.0
        while (x < w) {
            cv.rect(x + 12, y + 3, 24.0, 1.0, 0x8A5A32)
            x += 40
        }
        cv.rect(0.0, y + 10, w, 60.0, 0x7A4A28)
        cv.rect(70.0, y + 10, 12.0, 80.0, 0x5A3418)
        cv.rect(400.0, y + 10, 12.0, 80.0, 0x5A3418)
        cv.rect(24.0, y - 12, 14.0, 12.0, 0xE86B4A)
        cv.rect(25.0, y - 12, 3.0, 11.0, 0xFF9A7A)
        cv.stroke(39.0, y - 8, 44.0, y - 5, 1.4, 0xE86B4A)
        for (k in 0 until 3) {
            val u = (g.t * 0.7 + k / 3.0) % 1
            cv.disc(31 + sin(u * 6 + k) * 2, y - 14 - u * 16, 1.2 + u * 1.6, 0xFFFFFF, 0.4 * (1 - u))
        }
        cv.rect(440.0, y - 10, 16.0, 10.0, ArcadePalette.TERRACOTTA)
        for ((dx, len, c) in listOf(
            Triple(-4.0, 14.0, ArcadePalette.LEAF),
            Triple(0.0, 18.0, ArcadePalette.LEAF_HI),
            Triple(4.0, 13.0, ArcadePalette.LEAF),
        )) {
            cv.stroke(448.0, y - 10, 448 + dx, y - 10 - len, 1.6, c)
        }
    }

    private fun crate(
        cv: PixelCanvas,
        xIn: Double,
        yIn: Double,
        wIn: Double,
        hue: Int,
        alpha: Double = 1.0,
    ) {
        val x = jsRound(xIn).toDouble()
        val y = jsRound(yIn).toDouble()
        val w = jsRound(wIn).toDouble()
        val (c, hi, sh) = crates[hue % crates.size].let { Triple(it[0], it[1], it[2]) }
        cv.rect(x, y, w, K.BLOCK_H, 0x3A2A20, 0.55 * alpha)
        cv.rect(x + 1, y + 1, w - 2, K.BLOCK_H - 2, c, alpha)
        cv.rect(x + 1, y + 1, w - 2, 2.0, hi, alpha)
        cv.rect(x + 1, y + K.BLOCK_H - 3, w - 2, 2.0, sh, alpha)
        cv.rect(x + w - 3, y + 1, 2.0, K.BLOCK_H - 2, sh, alpha)
        if (w > 26) {
            cv.rect(x + 6, y + 4, 3.0, K.BLOCK_H - 6, ArcadePalette.LIME, alpha)
            cv.rect(x + 6, y + 4, 1.0, K.BLOCK_H - 6, ArcadePalette.LIME_HI, alpha)
        }
        if (w > 46) {
            cv.rect(x + w - 16, y + 5, 8.0, 6.0, ArcadePalette.CREAM, alpha)
            cv.rect(x + w - 15, y + 7, 6.0, 1.0, sh, alpha)
        }
    }

    private fun player(
        cv: PixelCanvas,
        g: StackGame,
    ) {
        val x: Double
        var y: Double
        val pose = VainiPose()
        if (g.over) {
            val d = g.lastDrop
            x = (d?.x ?: 200.0) + (d?.w ?: 60.0) / 2
            y = g.topY - K.BLOCK_H + 520 * g.deadT * g.deadT
            pose.armL = 2.6
            pose.armR = 2.6
            pose.blink = true
        } else {
            val base = g.topY - K.BLOCK_H
            val dropping = g.drop
            val cur = g.cur
            if (dropping != null) {
                x = dropping.x + dropping.w / 2
                y = base - K.HOVER * (1 - clamp01(dropping.t / K.DROP_TIME).pow(2))
            } else {
                x = if (cur != null) cur.x + cur.w / 2 else 240.0
                y = base - K.HOVER
            }
            val from = clamp01(g.hop / 0.32)
            if (dropping == null && g.hop < 0.32) {
                y += (K.BLOCK_H + K.HOVER) * (1 - easeOut(from)) - sin(from * PI) * 14
            }
            pose.armL = 1.0 + sin(g.t * 4) * 0.2
            pose.armR = 1.0 - sin(g.t * 4) * 0.2
            pose.blush = 0.4 + clamp01(g.combo / 3.0)
            pose.lookX = if (cur != null && cur.dir != 0.0) cur.dir else 1.0
            pose.legH = if (g.hop < 0.1) 4.0 else 6.0
        }
        VainiSprite.draw(cv, x, y + g.cam, pose)
    }

    /** Level chip, perfect-streak chip and the corner notice when the level rises. */
    fun drawHudExtras(
        cv: PixelCanvas,
        g: StackGame,
    ) {
        val label = "NIV ${g.level}"
        cv.rect(8.0, 8.0, PixelFont.width(label) + 10, 13.0, ArcadePalette.PANEL, 0.6)
        cv.text(label, 13.0, 12.0, ArcadePalette.GOLD)
        if (g.level > 1) cv.cornerToast("NIVEL ${g.level}", g.levelT, ArcadePalette.GOLD)
        if (g.combo >= 2) {
            val c = "PERFECTOS x${g.combo}"
            cv.rect(8.0, 24.0, PixelFont.width(c) + 10, 13.0, ArcadePalette.PANEL, 0.6)
            cv.text(c, 13.0, 28.0, ArcadePalette.WHITE)
        }
    }

    fun draw(
        cv: PixelCanvas,
        g: StackGame,
    ) {
        val pal = palette(g)
        cv.gradient(listOf(0 to pal.colors[0], 90 to pal.colors[1], 180 to pal.colors[2], 270 to pal.colors[3]))
        SkyPalette.drawStars(cv, max(pal.tod, 0.5 + pal.alt * 0.5), g.t, 200.0, 5.0)
        SkyPalette.drawShootingStar(cv, max(pal.tod, pal.alt), g.t)
        // sun / moon (the moon takes over as it darkens)
        val dk = SkyPalette.darkness(pal.tod)
        val hy = 64 + g.cam * 0.05
        if (dk < 0.9 && pal.alt < 0.8) {
            val warm = SkyPalette.warmth(pal.tod)
            cv.glow(400.0, hy, 40.0, mixColor(0xFFF4C8, 0xFF8A6A, warm), 0.3 * (1 - dk), 1.0)
            cv.disc(400.0, hy, 14.0, mixColor(0xFFFBE6, 0xFF9A5A, warm), 1 - dk)
        }
        val mo = max(dk, pal.alt)
        if (mo > 0.1) {
            cv.glow(400.0, hy, 40.0, 0xC8D4FF, 0.25 * mo, 1.0)
            cv.disc(400.0, hy, 15.0, 0xE8ECF8, mo)
            cv.disc(404.0, hy - 2, 12.0, 0xF8F9FF, mo)
            for ((dx, dy, r) in listOf(Triple(-5.0, 3.0, 3.0), Triple(3.0, 6.0, 2.0), Triple(-2.0, -5.0, 2.0))) {
                cv.disc(400 + dx, hy + dy, r, 0xC8CFE6, mo * 0.9)
            }
        }
        aurora(cv, g, pal)
        planets(cv, g, pal)
        cloudLayer(cv, g, pal, 0.4, 3.0, 46.0, 0.55)
        balloons(cv, g, pal)
        plane(cv, g, pal)
        skyline(cv, g, pal)
        cloudLayer(cv, g, pal, 0.62, 9.0, 58.0, 0.9)
        table(cv, g)
        g.stack.forEachIndexed { i, b -> crate(cv, b.x, K.GROUND - (i + 1) * K.BLOCK_H + g.cam, b.w, b.hue) }
        for (p in g.pieces) crate(cv, p.x, p.y + g.cam, p.w, p.hue, 0.95)
        g.cur?.let { crate(cv, it.x, g.topY - K.BLOCK_H - K.HOVER + g.cam, it.w, it.hue) }
        g.drop?.let {
            crate(
                cv,
                it.x,
                g.topY - K.BLOCK_H - K.HOVER * (1 - clamp01(it.t / K.DROP_TIME).pow(2)) + g.cam,
                it.w,
                it.hue,
            )
        }
        player(cv, g)
    }
}
