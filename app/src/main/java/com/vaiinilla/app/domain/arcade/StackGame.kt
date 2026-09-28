package com.vaiinilla.app.domain.arcade

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Apila: Vaini rides a crate that slides side to side. One tap drops it; whatever overhangs falls away.
 * Every 10 crates the level rises (more speed). Three perfect drops in a row give a little width back.
 */
internal class StackGame(
    seed: Int = 5,
) : ArcadeGame {
    object K {
        const val HOVER = 26.0
        const val BLOCK_H = 16.0
        const val BASE_W = 150.0
        const val GROUND = 236.0
        const val PERFECT = 4.0
        const val SPEED0 = 96.0
        const val SPEED_MAX = 270.0
        const val DROP_TIME = 0.11
        const val MIN_W = 3.0
        const val PER_LEVEL = 10.0
        const val GROW = 6.0
        const val GROW_AT = 3
    }

    class Block(
        val x: Double,
        val w: Double,
        val hue: Int,
    )

    class Moving(
        val w: Double,
        var x: Double,
        var dir: Double,
        val hue: Int,
    )

    class Drop(
        var t: Double,
        val x: Double,
        val w: Double,
        val hue: Int,
    )

    class Piece(
        var x: Double,
        val w: Double,
        var y: Double,
        var vy: Double,
        val vx: Double,
        val hue: Int,
    )

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
    var level = 1
        private set
    var levelT = 9.0
        private set
    val stack = mutableListOf(Block(240 - K.BASE_W / 2, K.BASE_W, 0))
    var cur: Moving? = null
        private set
    var drop: Drop? = null
        private set
    var lastDrop: Drop? = null
        private set
    var cam = 0.0
        private set
    private var camTarget = 0.0
    val pieces = mutableListOf<Piece>()
    var combo = 0
        private set
    var hop = 9.0
        private set
    private val rand = ArcadeRandom(seed)
    private val events = mutableListOf<ArcadeEvent>()

    override val time: Double get() = t
    override val deadTime: Double get() = deadT

    /** Y of the top of the tower, in world coordinates. */
    val topY: Double get() = K.GROUND - stack.size * K.BLOCK_H

    init {
        spawn()
    }

    private fun spawn() {
        val top = stack.last()
        val fromLeft = stack.size % 2 == 1
        cur = Moving(top.w, if (fromLeft) 0.0 else 480 - top.w, if (fromLeft) 1.0 else -1.0, stack.size)
    }

    override fun tap() {
        if (over) return
        if (!started) {
            started = true
            return
        }
        val c = cur
        if (drop != null || c == null) return
        drop = Drop(0.0, c.x, c.w, c.hue)
        cur = null
        events += ArcadeEvent.Release
    }

    override fun drainEvents(): List<ArcadeEvent> = events.toList().also { events.clear() }

    override fun step(dt: Double) {
        t += dt
        hop += dt
        levelT += dt
        cam += (camTarget - cam) * min(1.0, dt * 5)
        for (p in pieces) {
            p.vy += 1500 * dt
            p.y += p.vy * dt
            p.x += p.vx * dt
        }
        pieces.removeAll { it.y - cam >= 340 }
        if (!started) {
            cur?.let {
                it.x += it.dir * K.SPEED0 * 0.7 * dt
                bounce(it)
            }
            return
        }
        if (over) {
            deadT += dt
            return
        }
        cur?.let {
            it.x += it.dir * speedFor(score) * dt
            bounce(it)
        }
        drop?.let {
            it.t += dt
            if (it.t >= K.DROP_TIME) land()
        }
    }

    private fun bounce(b: Moving) {
        if (b.x < 0) {
            b.x = 0.0
            b.dir = 1.0
        }
        if (b.x + b.w > 480) {
            b.x = 480 - b.w
            b.dir = -1.0
        }
    }

    private fun land() {
        val d = drop ?: return
        drop = null
        lastDrop = d
        val top = stack.last()
        val l = max(d.x, top.x)
        val r = min(d.x + d.w, top.x + top.w)
        val w = r - l
        val y = topY - K.BLOCK_H
        if (w < K.MIN_W) { // full miss: the block falls with Vaini
            rand.next()
            pieces += Piece(d.x, d.w, y, 0.0, 0.0, d.hue)
            over = true
            events += ArcadeEvent.Hit
            return
        }
        val perfect = abs(d.x - top.x) < K.PERFECT
        val block: Block
        if (perfect) {
            combo += 1
            // a streak of perfects earns width back
            val grow = if (combo >= K.GROW_AT) min(K.GROW, K.BASE_W - top.w) else 0.0
            block = Block(max(0.0, min(480 - top.w - grow, top.x - grow / 2)), top.w + grow, d.hue)
            if (grow > 0) events += ArcadeEvent.Grow
        } else {
            combo = 0
            block = Block(l, w, d.hue)
            val overL = l - d.x
            val overR = d.x + d.w - r
            if (overL > 0) pieces += Piece(d.x, overL, y, 0.0, -30.0, d.hue)
            if (overR > 0) pieces += Piece(r, overR, y, 0.0, 30.0, d.hue)
        }
        stack += block
        score += 1
        hop = 0.0
        val lv = levelFor(score)
        if (lv > level) {
            level = lv
            levelT = 0.0
            events += ArcadeEvent.Level(lv)
        }
        camTarget = max(0.0, stack.size * K.BLOCK_H - 120)
        val cx = block.x + block.w / 2
        events += if (perfect) ArcadeEvent.Perfect(combo, cx, y) else ArcadeEvent.Place(cx, y)
        spawn()
    }

    companion object {
        fun levelFor(score: Double): Int = floor(score / K.PER_LEVEL).toInt() + 1

        fun speedFor(score: Double): Double = min(K.SPEED_MAX, K.SPEED0 + (levelFor(score) - 1) * 20 + score * 1.4)
    }
}
