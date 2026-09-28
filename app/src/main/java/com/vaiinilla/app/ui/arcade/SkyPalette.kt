package com.vaiinilla.app.ui.arcade

import com.vaiinilla.app.domain.arcade.clamp01
import com.vaiinilla.app.domain.arcade.jsRound
import com.vaiinilla.app.domain.arcade.lerp
import kotlin.math.PI
import kotlin.math.sin

/** The sky for games that run through a whole day: dawn, day, sunset, night, with sun, moon and stars. */
internal object SkyPalette {
    private class Key(
        val at: Double,
        val colors: IntArray,
    )

    private val keys =
        listOf(
            Key(0.0, intArrayOf(0x78B0D8, 0xF2C4C0, 0xFFD8A0, 0xFFB488)), // dawn
            Key(0.28, intArrayOf(0x5CB6E2, 0xA4DCEC, 0xE4F2D0, 0xFFF0C0)), // day
            Key(0.6, intArrayOf(0x54468E, 0xCC6688, 0xF6A070, 0xFFD28C)), // sunset
            Key(0.88, intArrayOf(0x080C2A, 0x18205A, 0x2C3878, 0x5A4A8C)), // night
        )

    private fun smooth(t: Double): Double = t * t * (3 - 2 * t)

    /** Time of day in 0..1 from progress; [span] is how many points it takes to reach full night. */
    fun timeOfDay(
        score: Double,
        span: Double,
    ): Double = clamp01(score / span)

    fun colors(tod: Double): IntArray {
        var a = keys[0]
        var b = keys.last()
        for (i in 0 until keys.size - 1) {
            if (tod >= keys[i].at && tod <= keys[i + 1].at) {
                a = keys[i]
                b = keys[i + 1]
                break
            }
        }
        if (tod >= keys.last().at) return keys.last().colors
        val f = smooth((tod - a.at) / (b.at - a.at))
        return IntArray(4) { mixColor(a.colors[it], b.colors[it], f) }
    }

    /** 0 in daylight, 1 at full night: dims scenery and lights up windows. */
    fun darkness(tod: Double): Double = smooth(clamp01((tod - 0.5) / 0.38))

    /** Peaks at sunset. */
    fun warmth(tod: Double): Double = sin(clamp01((tod - 0.42) / 0.3) * PI)

    fun drawSky(
        cv: PixelCanvas,
        tod: Double,
        height: Int = 270,
    ) {
        val c = colors(tod)
        cv.gradient(
            listOf(
                0 to c[0],
                jsRound(height * 0.34) to c[1],
                jsRound(height * 0.66) to c[2],
                height to c[3],
            ),
        )
    }

    fun drawStars(
        cv: PixelCanvas,
        tod: Double,
        time: Double,
        ySpan: Double = 200.0,
        seed: Double = 5.0,
    ) {
        val d = darkness(tod)
        if (d < 0.08) return
        for (i in 0 until 90) {
            val fi = i.toDouble()
            val x = arcadeHash(fi, seed) * cv.width
            val y = arcadeHash(fi, seed + 1) * ySpan
            val big = arcadeHash(fi, seed + 2) > 0.9
            val tw = 0.55 + 0.45 * sin(time * (1.5 + arcadeHash(fi, seed + 3) * 2) + fi)
            cv.px(x, y, 0xFFFFFF, d * tw)
            if (big && d * tw > 0.5) {
                cv.px(x - 1, y, 0xFFFFFF, d * 0.4)
                cv.px(x + 1, y, 0xFFFFFF, d * 0.4)
                cv.px(x, y - 1, 0xFFFFFF, d * 0.4)
                cv.px(x, y + 1, 0xFFFFFF, d * 0.4)
            }
        }
    }

    /** Sun rises on the left, the moon takes over at night on the right. */
    fun drawCelestial(
        cv: PixelCanvas,
        tod: Double,
        yShift: Double = 0.0,
    ) {
        val d = darkness(tod)
        val su = clamp01(tod / 0.66)
        val sx = lerp(70.0, 410.0, su)
        val sy = 200 - sin(su * PI) * 150 + yShift
        val warm = warmth(tod)
        val sunColor = mixColor(0xFFFBE0, 0xFF9A5A, warm)
        val halo = mixColor(0xFFF4C8, 0xFF8A6A, warm)
        if (d < 0.95) {
            cv.glow(sx, sy, 46.0, halo, 0.35 * (1 - d), 1.0)
            cv.disc(sx, sy, 20.0, halo, 0.5 * (1 - d))
            cv.disc(sx, sy, 15.0, sunColor, 1 - d * 0.9)
        }
        if (d > 0.1) {
            val mx = 392.0
            val my = 62 + yShift * 0.6
            cv.glow(mx, my, 40.0, 0xC8D4FF, 0.28 * d, 1.0)
            cv.disc(mx, my, 15.0, 0xE8ECF8, d)
            cv.disc(mx + 4, my - 2, 12.0, 0xF8F9FF, d)
            for ((dx, dy, r) in listOf(Triple(-5.0, 3.0, 3.0), Triple(3.0, 6.0, 2.0), Triple(-2.0, -5.0, 2.0))) {
                cv.disc(mx + dx, my + dy, r, 0xC8CFE6, d * 0.9)
            }
        }
    }

    /** A shooting star every few seconds at night. */
    fun drawShootingStar(
        cv: PixelCanvas,
        tod: Double,
        time: Double,
    ) {
        val d = darkness(tod)
        if (d < 0.6) return
        val period = 6.5
        val u = (time % period) / period
        if (u > 0.12) return
        val k = u / 0.12
        val x = 380 - k * 260
        val y = 30 + k * 70
        for (i in 0 until 14) cv.px(x + i * 2.4, y - i * 1.2, 0xFFFFFF, (1 - i / 14.0) * (1 - k * 0.6))
    }
}
