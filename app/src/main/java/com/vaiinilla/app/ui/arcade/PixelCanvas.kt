package com.vaiinilla.app.ui.arcade

import com.vaiinilla.app.domain.arcade.clamp01
import com.vaiinilla.app.domain.arcade.jsRound
import com.vaiinilla.app.domain.arcade.lerp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

private const val OPAQUE = 0xFF000000.toInt()

/**
 * A small deterministic pixel rasterizer (480x270 by default) that the arcade scenes draw into. Colours are packed
 * as 0xRRGGBB ints.
 *
 * Rounding follows the web prototype exactly (`Math.round`, clamped byte writes rounding half to even) so the scenes
 * can be checked frame by frame against the reference images.
 */
internal class PixelCanvas(
    val width: Int = WIDTH,
    val height: Int = HEIGHT,
) {
    /** 0xAARRGGBB; alpha stays 0 until a pixel is painted. */
    val pixels = IntArray(width * height)

    fun clear() = pixels.fill(0)

    // primitives

    fun px(
        x: Double,
        y: Double,
        color: Int,
        alpha: Double = 1.0,
    ) = plot(jsRound(x), jsRound(y), color, alpha)

    /** Paints one pixel at integer coordinates (outside the canvas or with no alpha it does nothing). */
    fun plot(
        ix: Int,
        iy: Int,
        color: Int,
        alpha: Double,
    ) {
        if (ix < 0 || iy < 0 || ix >= width || iy >= height || alpha <= 0) return
        val i = iy * width + ix
        if (alpha >= 1) {
            pixels[i] = OPAQUE or color
            return
        }
        val d = pixels[i]
        val r = blend((d shr 16) and 255, (color shr 16) and 255, alpha)
        val g = blend((d shr 8) and 255, (color shr 8) and 255, alpha)
        val b = blend(d and 255, color and 255, alpha)
        pixels[i] = OPAQUE or (r shl 16) or (g shl 8) or b
    }

    fun rect(
        x: Double,
        y: Double,
        w: Double,
        h: Double,
        color: Int,
        alpha: Double = 1.0,
    ) {
        val x0 = jsRound(x)
        val y0 = jsRound(y)
        val cols = if (w > 0) min(ceil(w), 100_000.0).toInt() else 0
        val rows = if (h > 0) min(ceil(h), 100_000.0).toInt() else 0
        if (cols == 0 || rows == 0) return
        // only the part inside the canvas can change any pixel
        val iMin = max(0, -x0)
        val iMax = min(cols, width - x0)
        val jMin = max(0, -y0)
        val jMax = min(rows, height - y0)
        if (iMin >= iMax || jMin >= jMax) return
        if (alpha >= 1) {
            val opaque = OPAQUE or color
            for (j in jMin until jMax) {
                val start = (y0 + j) * width + x0 + iMin
                pixels.fill(opaque, start, start + (iMax - iMin))
            }
            return
        }
        for (j in jMin until jMax) {
            for (i in iMin until iMax) plot(x0 + i, y0 + j, color, alpha)
        }
    }

    fun disc(
        cx: Double,
        cy: Double,
        r: Double,
        color: Int,
        alpha: Double = 1.0,
    ) {
        val r2 = r * r
        val yMin = floor(cy - r).toInt()
        val yMax = ceil(cy + r).toInt()
        val xMin = floor(cx - r).toInt()
        val xMax = ceil(cx + r).toInt()
        for (y in yMin..yMax) {
            for (x in xMin..xMax) {
                val dx = x - cx
                val dy = y - cy
                if (dx * dx + dy * dy <= r2) plot(x, y, color, alpha)
            }
        }
    }

    fun ellipse(
        cx: Double,
        cy: Double,
        rx: Double,
        ry: Double,
        color: Int,
        alpha: Double = 1.0,
    ) {
        val yMin = floor(cy - ry).toInt()
        val yMax = ceil(cy + ry).toInt()
        val xMin = floor(cx - rx).toInt()
        val xMax = ceil(cx + rx).toInt()
        for (y in yMin..yMax) {
            for (x in xMin..xMax) {
                val dx = (x - cx) / rx
                val dy = (y - cy) / ry
                if (dx * dx + dy * dy <= 1) plot(x, y, color, alpha)
            }
        }
    }

    /** Thick line with round caps (a capsule). */
    fun stroke(
        x0: Double,
        y0: Double,
        x1: Double,
        y1: Double,
        r: Double,
        color: Int,
        alpha: Double = 1.0,
    ) {
        val minX = floor(min(x0, x1) - r).toInt()
        val maxX = ceil(max(x0, x1) + r).toInt()
        val minY = floor(min(y0, y1) - r).toInt()
        val maxY = ceil(max(y0, y1) + r).toInt()
        val vx = x1 - x0
        val vy = y1 - y0
        val len2 = if (vx * vx + vy * vy == 0.0) 1.0 else vx * vx + vy * vy
        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val t = max(0.0, min(1.0, ((x - x0) * vx + (y - y0) * vy) / len2))
                val dx = x - (x0 + vx * t)
                val dy = y - (y0 + vy * t)
                if (dx * dx + dy * dy <= r * r) plot(x, y, color, alpha)
            }
        }
    }

    /** Even-odd polygon fill; [pts] holds x0, y0, x1, y1, ... */
    fun poly(
        pts: DoubleArray,
        color: Int,
        alpha: Double = 1.0,
    ) {
        val n = pts.size / 2
        if (n == 0) return
        var minX = pts[0]
        var maxX = pts[0]
        var minY = pts[1]
        var maxY = pts[1]
        for (i in 1 until n) {
            minX = min(minX, pts[i * 2])
            maxX = max(maxX, pts[i * 2])
            minY = min(minY, pts[i * 2 + 1])
            maxY = max(maxY, pts[i * 2 + 1])
        }
        for (y in floor(minY).toInt()..ceil(maxY).toInt()) {
            val fy = y + 0.5
            for (x in floor(minX).toInt()..ceil(maxX).toInt()) {
                var inside = false
                var j = n - 1
                for (i in 0 until n) {
                    val xi = pts[i * 2]
                    val yi = pts[i * 2 + 1]
                    val xj = pts[j * 2]
                    val yj = pts[j * 2 + 1]
                    if ((yi > fy) != (yj > fy) && x + 0.5 < ((xj - xi) * (fy - yi)) / (yj - yi) + xi) inside = !inside
                    j = i
                }
                if (inside) plot(x, y, color, alpha)
            }
        }
    }

    // effects shared by the scenes

    /** Vertical gradient with Bayer dithering between colour stops (y, colour). */
    fun gradient(
        stops: List<Pair<Int, Int>>,
        from: Int = 0,
        to: Int = width,
    ) {
        val xStart = max(0, from)
        val xEnd = min(width, to)
        if (xStart >= xEnd) return
        for (k in 0 until stops.size - 1) {
            val (y0, c0) = stops[k]
            val (y1, c1) = stops[k + 1]
            if (y0 >= y1) continue
            var y = max(0, y0)
            val yEnd = min(height, y1)
            while (y < yEnd) {
                val f = clamp01((y - y0).toDouble() / (y1 - y0))
                // the dither pattern repeats every 4 pixels, so each row has only four possible outcomes
                val row = (y and 3) * 4
                val pick = IntArray(4) { OPAQUE or (if (f > BAYER[row + it] / 16) c1 else c0) }
                val base = y * width
                for (x in xStart until xEnd) pixels[base + x] = pick[x and 3]
                y += 1
            }
        }
    }

    /** Soft light pool: blends toward [color] with a radial falloff, quantised to keep the pixel-art banding. */
    fun glow(
        cx: Double,
        cy: Double,
        r: Double,
        color: Int,
        strength: Double = 0.5,
        squashY: Double = 1.0,
    ) {
        val lr = ((color shr 16) and 255).toDouble()
        val lg = ((color shr 8) and 255).toDouble()
        val lb = (color and 255).toDouble()
        val yMin = max(0, floor(cy - r * squashY).toInt())
        val yMax = min(height - 1, ceil(cy + r * squashY).toInt())
        val xMin = max(0, floor(cx - r).toInt())
        val xMax = min(width - 1, ceil(cx + r).toInt())
        for (y in yMin..yMax) {
            for (x in xMin..xMax) {
                val dist = hypot(x - cx, (y - cy) / squashY) / r
                if (dist >= 1) continue
                val a = jsRound((1 - dist) * (1 - dist) * 14).toDouble() / 14 * strength
                val i = y * width + x
                val d = pixels[i]
                val dr = ((d shr 16) and 255).toDouble()
                val dg = ((d shr 8) and 255).toDouble()
                val db = (d and 255).toDouble()
                val nr = clampByte(min(255.0, dr + (lr - dr * 0.4) * a))
                val ng = clampByte(min(255.0, dg + (lg - dg * 0.4) * a))
                val nb = clampByte(min(255.0, db + (lb - db * 0.4) * a))
                pixels[i] = (d and OPAQUE) or (nr shl 16) or (ng shl 8) or nb
            }
        }
    }

    /** Copies the opaque pixels of [src] into this canvas, optionally mirrored vertically. */
    fun blit(
        src: PixelCanvas,
        x: Int,
        y: Int,
        flipY: Boolean = false,
    ) {
        for (j in 0 until src.height) {
            for (i in 0 until src.width) {
                val s = src.pixels[j * src.width + i]
                if ((s ushr 24) == 0) continue
                val dx = x + i
                val dy = y + (if (flipY) src.height - 1 - j else j)
                if (dx < 0 || dy < 0 || dx >= width || dy >= height) continue
                pixels[dy * width + dx] = OPAQUE or (s and 0x00FFFFFF)
            }
        }
    }

    private fun blend(
        dst: Int,
        src: Int,
        alpha: Double,
    ): Int = clampByte(dst + (src - dst) * alpha)

    companion object {
        const val WIDTH = 480
        const val HEIGHT = 270
        private val BAYER =
            doubleArrayOf(0.0, 8.0, 2.0, 10.0, 12.0, 4.0, 14.0, 6.0, 3.0, 11.0, 1.0, 9.0, 15.0, 7.0, 13.0, 5.0)
    }
}

/** `Uint8ClampedArray` write: clamp to 0..255, then round half to even. */
internal fun clampByte(v: Double): Int =
    when {
        !(v > 0) -> 0 // also catches NaN
        v >= 255 -> 255
        else -> Math.rint(v).toInt()
    }

internal fun mixColor(
    a: Int,
    b: Int,
    t: Double,
): Int {
    fun ch(shift: Int): Int = jsRound(lerp(((a shr shift) and 255).toDouble(), ((b shr shift) and 255).toDouble(), t))
    return (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
}

/** Integer hash in [0, 1), identical to the web prototype's. */
internal fun arcadeHash(
    x: Double,
    y: Double,
): Double {
    fun toInt32(v: Double): Int = if (v.isNaN()) 0 else v.toLong().toInt()
    var h = (toInt32(x) * 374_761_393) xor (toInt32(y) * 668_265_263)
    h = (h xor (h ushr 13)) * 1_274_126_177
    h = h xor (h ushr 16)
    return (h.toLong() and 0xFFFFFFFFL).toDouble() / 4_294_967_296.0
}
