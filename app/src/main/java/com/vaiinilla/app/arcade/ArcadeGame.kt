package com.vaiinilla.app.arcade

import kotlin.math.floor

/**
 * Things that happened during a step. The presentation layer turns them into particles, popups and haptics;
 * the rules never depend on how they are shown.
 */
internal sealed interface ArcadeEvent {
    data object Flap : ArcadeEvent

    data class Score(
        val n: Int,
    ) : ArcadeEvent

    data class Level(
        val n: Int,
    ) : ArcadeEvent

    data object Hit : ArcadeEvent

    data object Release : ArcadeEvent

    data class Place(
        val x: Double,
        val y: Double,
    ) : ArcadeEvent

    data class Perfect(
        val combo: Int,
        val x: Double,
        val y: Double,
    ) : ArcadeEvent

    data object Grow : ArcadeEvent

    data class Flip(
        val direction: Int,
    ) : ArcadeEvent

    data class Land(
        val direction: Int,
    ) : ArcadeEvent

    data object Shoot : ArcadeEvent

    data object HitEnemy : ArcadeEvent

    data class Kill(
        val kind: GalaxyEnemyKind,
        val x: Double,
        val y: Double,
        val points: Int,
    ) : ArcadeEvent

    data object Hurt : ArcadeEvent

    data class Power(
        val n: Int,
    ) : ArcadeEvent

    data object Life : ArcadeEvent

    data class Wave(
        val n: Int,
        val boss: Boolean,
    ) : ArcadeEvent

    data object BossFire : ArcadeEvent

    data class BossDown(
        val x: Double,
        val y: Double,
    ) : ArcadeEvent

    data object Bail : ArcadeEvent
}

/** One mini game's rules. No drawing and no framework types, so every game runs in plain unit tests. */
internal interface ArcadeGame {
    val score: Double
    val started: Boolean
    val over: Boolean

    /** Seconds since the run ended (0 while it is still going). */
    val deadTime: Double

    /** Game clock in seconds. */
    val time: Double

    /** A tap: starts the run, then does the game's one action. */
    fun tap()

    /** The finger lifted (only Skate cares: a short hop). */
    fun release() {}

    /** The finger is over horizontal position [x] (only Galaxia cares). */
    fun aim(x: Double) {}

    /** A downward swipe is held (only Skate cares). */
    fun fall(on: Boolean) {}

    fun step(dt: Double)

    /** Returns and clears the events raised since the last call. */
    fun drainEvents(): List<ArcadeEvent>
}

/** Seeded PRNG (mulberry32): identical sequences on every platform, so runs are reproducible in tests. */
internal class ArcadeRandom(
    seed: Int,
) {
    private var state = seed

    fun next(): Double {
        state += 0x6D2B79F5
        var t = (state xor (state ushr 15)) * (1 or state)
        t = (t + ((t xor (t ushr 7)) * (61 or t))) xor t
        return (t xor (t ushr 14)).toLong().and(0xFFFFFFFFL).toDouble() / 4294967296.0
    }
}

/** `Math.round`: halves round toward +infinity. The arcade rules and scenes use it so results match the reference frames. */
internal fun jsRound(v: Double): Int {
    val f = floor(v + 0.5)
    return if (f > -1e9 && f < 1e9) f.toInt() else 0
}

internal fun clamp01(v: Double): Double =
    if (v < 0) {
        0.0
    } else if (v > 1) {
        1.0
    } else {
        v
    }

internal fun lerp(
    a: Double,
    b: Double,
    t: Double,
): Double = a + (b - a) * t
