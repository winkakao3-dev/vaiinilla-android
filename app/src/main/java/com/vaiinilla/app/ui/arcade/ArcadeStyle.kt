package com.vaiinilla.app.ui.arcade

import com.vaiinilla.app.domain.arcade.clamp01
import com.vaiinilla.app.domain.arcade.jsRound
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Brand colours the arcade scenes share (0xRRGGBB). */
internal object ArcadePalette {
    const val CREAM = 0xF5EEDE
    const val CREAM_SHADE = 0xE2D6BB
    const val LIME = 0xA6D62A
    const val LIME_HI = 0xC6EC52
    const val LIME_SHADE = 0x6E9E1C
    const val INK = 0x2A2030
    const val COFFEE = 0x5A3A26
    const val TERRACOTTA = 0xD9714A
    const val MOTO = 0x2F6FE0
    const val MUSTARD = 0xE7B96A
    const val LEAF = 0x3F9A4A
    const val LEAF_HI = 0x6CC36A
    const val CORAL = 0xFF6A5A
    const val PINK = 0xFF9DB0
    const val GOLD = 0xFFE45C
    const val WHITE = 0xFFFFFF
    const val PANEL = 0x1A1424
}

/** What the HUD and overlays need to know about the current frame. */
internal enum class ArcadeScreen { TITLE, PLAYING, OVER }

internal class ArcadeFrameInfo(
    val screen: ArcadeScreen,
    var screenTime: Double,
    val best: Int,
    val newBest: Boolean,
    val dt: Double,
    val touch: Boolean = true,
    /** A frozen frame: show the blinking hint in its visible phase. */
    val holdHint: Boolean = false,
)

// particles, popups and shake

internal class ArcadeParticle(
    var x: Double,
    var y: Double,
    var vx: Double,
    var vy: Double,
    var life: Double,
    val maxLife: Double,
    val color: Int,
    val gravity: Double,
    val size: Double,
    val dash: Boolean = false,
)

internal class ArcadePopup(
    val text: String,
    val x: Double,
    var y: Double,
    var life: Double,
    val color: Int,
)

/** Short-lived visual effects fed by game events. Purely visual; the rules never read them. */
internal class ArcadeEffects {
    val particles = mutableListOf<ArcadeParticle>()
    val popups = mutableListOf<ArcadePopup>()
    var shake = 0.0

    fun burst(
        x: Double,
        y: Double,
        count: Int,
        colors: IntArray,
        speed: Double = 60.0,
        life: Double = 0.4,
        gravity: Double = 120.0,
        size: Double = 1.0,
        up: Double = 0.0,
    ) {
        for (i in 0 until count) {
            val a = i.toDouble() / count * 2 * PI + Random.nextDouble() * 0.5
            val s = speed * (0.5 + Random.nextDouble() * 0.7)
            particles +=
                ArcadeParticle(
                    x,
                    y,
                    cos(a) * s,
                    sin(a) * s - up,
                    life * (0.7 + Random.nextDouble() * 0.5),
                    life,
                    colors[i % colors.size],
                    gravity,
                    size,
                )
        }
    }

    /** The three lime "emotion" dashes from the character sheet. */
    fun dashes(
        x: Double,
        y: Double,
        direction: Double,
    ) {
        for (i in 0 until 3) {
            particles +=
                ArcadeParticle(
                    x + direction * 4,
                    y + (i - 1) * 6.0,
                    direction * (45 + i * 8.0),
                    (i - 1) * 22.0,
                    0.32,
                    0.32,
                    ArcadePalette.LIME,
                    0.0,
                    2.0,
                    dash = true,
                )
        }
    }

    fun popup(
        text: String,
        x: Double,
        y: Double,
        color: Int,
    ) {
        popups += ArcadePopup(text, x, y, 1.0, color)
    }

    fun step(dt: Double) {
        for (p in particles) {
            p.life -= dt
            p.vy += p.gravity * dt
            p.x += p.vx * dt
            p.y += p.vy * dt
        }
        particles.removeAll { it.life <= 0 }
        for (p in popups) {
            p.life -= dt
            p.y -= 18 * dt
        }
        popups.removeAll { it.life <= 0 }
        shake = max(0.0, shake - dt * 3)
    }

    fun draw(cv: PixelCanvas) {
        for (p in particles) {
            val a = clamp01(p.life / p.maxLife * 1.4)
            when {
                p.dash -> cv.rect(p.x, p.y, 5.0, 2.0, p.color, a)
                p.size > 1 -> cv.disc(p.x, p.y, p.size, p.color, a)
                else -> cv.px(p.x, p.y, p.color, a)
            }
        }
        for (p in popups) {
            val w = PixelFont.width(p.text)
            val x = jsRound(p.x - w / 2).toDouble()
            val y = jsRound(p.y).toDouble()
            cv.text(p.text, x + 1, y + 1, ArcadePalette.INK, 0.9)
            cv.text(p.text, x, y, p.color)
        }
    }
}

// HUD and overlays

internal fun PixelCanvas.bigText(
    s: String,
    x: Double,
    y: Double,
    color: Int,
    scale: Double = 2.0,
    shadow: Int = ArcadePalette.INK,
) {
    text(s, x + scale, y + scale, shadow, 1.0, scale)
    text(s, x, y, color, 1.0, scale)
}

internal fun PixelCanvas.centeredText(
    s: String,
    y: Double,
    color: Int,
    scale: Double = 1.0,
    shadow: Int = ArcadePalette.INK,
) = bigText(s, jsRound((width - PixelFont.width(s, scale)) / 2).toDouble(), y, color, scale, shadow)

/** Score at the top centre and the best score at the top right. [onLight] draws dark text on pale backgrounds. */
internal fun PixelCanvas.hudScore(
    score: Double,
    best: Int,
    onLight: Boolean = false,
) {
    val s =
        kotlin.math
            .floor(score)
            .toInt()
            .toString()
    val face = if (onLight) ArcadePalette.INK else ArcadePalette.WHITE
    val edge = if (onLight) ArcadePalette.WHITE else ArcadePalette.INK
    bigText(s, jsRound(width / 2.0 - PixelFont.width(s, 3.0) / 2).toDouble(), 14.0, face, 3.0, edge)
    val b = "MEJOR $best"
    text(b, width - PixelFont.width(b) - 8 + 1, 9.0, edge, 0.7)
    text(b, width - PixelFont.width(b) - 8, 8.0, face)
}

/** Small notice in the top-right corner ("NIVEL 3", "OLA 2"): slides in, holds, fades out. */
internal fun PixelCanvas.cornerToast(
    label: String,
    age: Double,
    color: Int = ArcadePalette.LIME_HI,
    y: Double = 20.0,
    life: Double = 2.2,
) {
    if (age < 0 || age > life) return
    val inK = clamp01(age / 0.22)
    val outK = clamp01((life - age) / 0.3)
    val a = min(inK, outK)
    val w = PixelFont.width(label) + 12
    val slide = (1 - (1 - (1 - inK) * (1 - inK) * (1 - inK))) * 26
    val x = jsRound(width - w - 8 + slide).toDouble()
    rect(x, y, w, 13.0, ArcadePalette.PANEL, 0.78 * a)
    rect(x, y, 2.0, 13.0, color, a)
    text(label, x + 7, y + 4, color, a)
}

/** Rounded translucent panel. */
internal fun PixelCanvas.panel(
    x: Double,
    y: Double,
    w: Double,
    h: Double,
    color: Int = ArcadePalette.PANEL,
    alpha: Double = 0.82,
) {
    rect(x + 2, y, w - 4, h, color, alpha)
    rect(x, y + 2, w, h - 4, color, alpha)
    rect(x + 1, y + 1, w - 2, h - 2, color, alpha)
}

internal fun PixelCanvas.drawTitle(
    time: Double,
    title: String,
    hint: String,
) {
    // the title sits at the top so the hero and the scene stay visible
    centeredText(title, 16.0, ArcadePalette.LIME_HI, 4.0)
    val w = PixelFont.width(hint) + 22
    panel(jsRound((width - w) / 2).toDouble(), 58.0, w, 18.0)
    if (sin(time * 5) > -0.3) centeredText(hint, 63.0, ArcadePalette.WHITE)
}

internal fun PixelCanvas.drawGameOver(
    time: Double,
    score: Double,
    best: Int,
    newBest: Boolean,
) {
    panel(130.0, 78.0, 220.0, 118.0)
    centeredText("¡OUPS!", 88.0, ArcadePalette.CORAL, 2.0)
    centeredText(
        kotlin.math
            .floor(score)
            .toInt()
            .toString(),
        112.0,
        ArcadePalette.WHITE,
        4.0,
    )
    centeredText(if (newBest) "¡NUEVO RECORD!" else "MEJOR $best", 142.0, if (newBest) ArcadePalette.GOLD else 0xC8BFD8)
    if (time > 0.7 && sin(time * 5) > -0.3) centeredText("TOCA PARA OTRA", 168.0, ArcadePalette.WHITE)
}
