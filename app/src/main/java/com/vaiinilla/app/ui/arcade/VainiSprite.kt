package com.vaiinilla.app.ui.arcade

import com.vaiinilla.app.arcade.jsRound
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** How Vaini is posed for one frame. Angles are in radians from straight down, positive = outwards. */
internal class VainiPose(
    var armL: Double = 0.0,
    var armR: Double = 0.0,
    var blink: Boolean = false,
    var blush: Double = 0.0,
    var lookX: Double = 0.0,
    var bob: Double = 0.0,
    var legH: Double = 6.0,
    /** Walk phase; null keeps both legs planted. */
    var walk: Double? = null,
    /** Ground shadow colour; null draws none. */
    var shadow: Int? = null,
)

/**
 * Vaini as a pixel sprite: a cream sheet of paper, torn top with soft teeth, lime corner folded forward,
 * two lime text lines, tall black eyes low on the face, chunky arms and two stubby legs.
 */
internal object VainiSprite {
    private const val CREAM = 0xF5EEDE
    private const val CREAM_HI = 0xFFFAF0
    private const val CREAM_SHADE = 0xE2D6BB
    private const val OUTLINE = 0xB8A784
    private const val LIME = 0xA6D62A
    private const val LIME_HI = 0xC6EC52
    private const val LIME_SHADE = 0x6E9E1C
    private const val EYE = 0x141414
    private const val EYE_HI = 0xFFFFFF

    private const val BODY_W = 30
    private const val BODY_H = 29
    private val teeth = intArrayOf(2, 1, 0, 1, 2)

    private fun topAt(c: Int): Int = if (c >= 20) c - 20 else teeth[c % 5]

    private fun insideBody(
        c: Int,
        r: Int,
    ): Boolean {
        if (c < 0 || c >= BODY_W || r < 0 || r >= BODY_H) return false
        if (r < topAt(c)) return false
        if (c >= 20 && r < 9 && r < c - 20) return false
        if (r == BODY_H - 1 && (c == 0 || c == BODY_W - 1)) return false // rounded bottom corners
        return true
    }

    /** ([x], [y]) is the centre of the feet line. */
    fun draw(
        cv: PixelCanvas,
        x: Double,
        y: Double,
        pose: VainiPose,
    ) {
        val bx = jsRound(x - BODY_W / 2.0)
        val by = jsRound(y - BODY_H - pose.legH + pose.bob)
        val fbx = bx.toDouble()
        val fby = by.toDouble()

        pose.shadow?.let { cv.ellipse(x, y + 1, 17.0, 3.0, it, 0.35) }

        // legs: stubby and shaded; walking lifts them alternately
        val walk = pose.walk
        val lift =
            if (walk == null) {
                intArrayOf(0, 0)
            } else {
                intArrayOf(max(0, jsRound(sin(walk) * 2)), max(0, jsRound(-sin(walk) * 2)))
            }
        for ((k, lx) in intArrayOf(6, 18).withIndex()) {
            val ly = fby + (BODY_H - 1 - lift[k])
            cv.rect(fbx + lx, ly, 7.0, pose.legH + 1, OUTLINE)
            cv.rect(fbx + lx + 1, ly, 5.0, pose.legH, CREAM)
            cv.rect(fbx + lx + 4, ly, 1.0, pose.legH, CREAM_SHADE)
        }

        // arms: capsules from the side notches
        val armY = fby + 17

        fun arm(
            side: Double,
            ang: Double,
        ) {
            val sx = if (side < 0) fbx + 1 else fbx + (BODY_W - 2)
            val ex = sx + side * sin(ang) * 9
            val ey = armY + cos(ang) * 9
            cv.stroke(sx, armY, ex, ey, 3.9, OUTLINE)
            cv.stroke(sx, armY, ex, ey, 2.9, CREAM)
            cv.stroke(sx + side * 0.6, armY + 1, ex + side * 0.6, ey + 0.6, 1.2, CREAM_SHADE)
        }
        arm(-1.0, pose.armL)
        arm(1.0, pose.armR)

        // body with outline and light from the top-left
        for (r in -1..BODY_H) {
            for (c in -1..BODY_W) {
                if (insideBody(c, r)) continue
                if (insideBody(c - 1, r) || insideBody(c + 1, r) || insideBody(c, r - 1) || insideBody(c, r + 1)) {
                    cv.px(fbx + c, fby + r, OUTLINE)
                }
            }
        }
        for (r in 0 until BODY_H) {
            for (c in 0 until BODY_W) {
                if (!insideBody(c, r)) continue
                var col = CREAM
                if (c >= BODY_W - 3 || r >= BODY_H - 3) {
                    col = CREAM_SHADE
                } else if (r <= topAt(c) + 1 && c < 20) {
                    col = CREAM_HI
                }
                cv.px(fbx + c, fby + r, col)
            }
        }

        // folded corner: lit lime flap over the cut, darker curl underneath
        for (r in 0 until 12) {
            for (c in 19 until BODY_W) {
                val onFlap = c >= 20 && r >= c - 20 && r <= 8 + (c - 20) / 9.0
                if (onFlap) cv.px(fbx + c, fby + r, if (r == c - 20) LIME_HI else LIME)
                val onShade = c >= 24 && r >= 9 && r <= 9 + (c - 24) && c <= BODY_W - 1
                if (onShade) cv.px(fbx + c, fby + r, LIME_SHADE)
            }
        }
        for (r in 0..8) cv.px(fbx + 20, fby + r, LIME_SHADE)

        // two text lines with rounded ends
        fun line(
            c0: Double,
            len: Double,
            r0: Double,
        ) {
            cv.rect(fbx + c0 + 1, fby + r0, len - 2, 3.0, LIME)
            cv.rect(fbx + c0, fby + r0 + 1, 1.0, 1.0, LIME)
            cv.rect(fbx + c0 + len - 1, fby + r0 + 1, 1.0, 1.0, LIME)
            cv.rect(fbx + c0 + 1, fby + r0 + 2, len - 2, 1.0, LIME_SHADE)
        }
        line(4.0, 13.0, 6.0)
        line(4.0, 9.0, 11.0)

        // eyes: tall pills that look sideways and blink
        val lx = jsRound(pose.lookX).toDouble()
        for (ex in doubleArrayOf(7.0, 18.0)) {
            val cx = fbx + ex + lx
            if (pose.blink) {
                cv.rect(cx, fby + 19, 4.0, 1.0, EYE)
            } else {
                cv.rect(cx, fby + 16, 4.0, 7.0, EYE)
                cv.px(cx, fby + 16, CREAM)
                cv.px(cx + 3, fby + 16, CREAM)
                cv.px(cx, fby + 22, CREAM)
                cv.px(cx + 3, fby + 22, CREAM)
                cv.px(cx + 1, fby + 17, EYE_HI, 0.95)
            }
        }

        if (pose.blush != 0.0) {
            cv.rect(fbx + 5, fby + 23, 3.0, 1.0, 0xFF9D8A, pose.blush * 0.8)
            cv.rect(fbx + 23, fby + 23, 3.0, 1.0, 0xFF9D8A, pose.blush * 0.8)
        }
    }

    private val mirrorSprite = PixelCanvas(64, 64)

    /** Vaini upside down (feet on the ceiling). ([x], [y]) is the same feet-line anchor [draw] uses. */
    fun drawUpsideDown(
        cv: PixelCanvas,
        x: Double,
        y: Double,
        pose: VainiPose,
    ) {
        mirrorSprite.clear()
        val p =
            VainiPose(
                pose.armL,
                pose.armR,
                pose.blink,
                pose.blush,
                pose.lookX,
                pose.bob,
                pose.legH,
                pose.walk,
                null,
            )
        draw(mirrorSprite, 32.0, 60.0, p)
        // sprite rows j map to y + (60 - j): the feet row lands on y and the body hangs below it
        cv.blit(mirrorSprite, jsRound(x - 32), jsRound(y - 3), flipY = true)
    }
}
