package com.vaiinilla.app.domain.arcade

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * One player's session at the arcade card: which game is selected, whether it is on the title screen, running or
 * over, and the scores. It drives the rules at a fixed step and leaves all drawing to the presentation layer.
 */
internal class ArcadeSession(
    val store: ArcadeScoreStore = MemoryArcadeScoreStore(),
    private val skateStore: SkateProgressStore = MemorySkateProgressStore(),
    private val seedSource: () -> Int = { Random.nextInt(1, 10_000) },
) {
    var kind = ArcadeKind.FLAPPY
        private set
    var status = ArcadeStatus.IDLE
        private set
    var score = 0
        private set
    var boardOpen = false

    var game: ArcadeGame = makeGame(ArcadeKind.FLAPPY)
        private set

    /** Seconds spent on the current screen (title, running or game over). */
    var screenTime = 0.0
        private set
    var newBest = false
        private set
    private var accumulator = 0.0
    private val events = mutableListOf<ArcadeEvent>()

    private fun makeGame(kind: ArcadeKind): ArcadeGame {
        val seed = seedSource()
        return when (kind) {
            ArcadeKind.FLAPPY -> FlappyGame(seed)
            ArcadeKind.SKATE -> SkateGame(seed, skateStore)
            ArcadeKind.APILA -> StackGame(seed)
            ArcadeKind.GRAVEDAD -> GravityGame(seed)
            ArcadeKind.GALAXIA -> GalaxyGame(seed)
        }
    }

    private fun load(
        next: ArcadeKind,
        nextStatus: ArcadeStatus,
    ) {
        kind = next
        game = makeGame(next)
        status = nextStatus
        score = 0
        screenTime = 0.0
        newBest = false
        boardOpen = false
        accumulator = 0.0
        events.clear()
    }

    fun select(next: ArcadeKind) {
        if (next == kind && status != ArcadeStatus.DEAD) return
        load(next, ArcadeStatus.IDLE)
    }

    /** A tap on the stage: starts a run, plays, or (on the game-over screen, after a moment) plays again. */
    fun primary() {
        if (boardOpen) return
        when (status) {
            ArcadeStatus.IDLE -> {
                status = ArcadeStatus.PLAY
                screenTime = 0.0
                game.tap()
            }
            ArcadeStatus.PLAY -> game.tap()
            ArcadeStatus.DEAD -> if (screenTime > RETRY_DELAY) retry()
        }
    }

    fun retry() {
        load(kind, ArcadeStatus.PLAY)
        game.tap()
    }

    fun release() {
        if (status == ArcadeStatus.PLAY) game.release()
    }

    fun aim(x: Double) {
        if (status != ArcadeStatus.DEAD) game.aim(x)
    }

    fun fall(on: Boolean) {
        if (status == ArcadeStatus.PLAY) game.fall(on)
    }

    /** Runs the simulation for [dt] seconds of wall time, in fixed steps. */
    fun advance(dt: Double) {
        if (boardOpen) return
        accumulator += min(max(dt, 0.0), MAX_ADVANCE)
        while (accumulator >= FIXED_STEP) {
            step(FIXED_STEP)
            accumulator -= FIXED_STEP
        }
    }

    /** One fixed step. The title and game-over screens keep running so the scenery stays alive. */
    fun step(dt: Double) {
        game.step(dt)
        events += game.drainEvents()
        screenTime += dt
        val s = Math.floor(game.score).toInt()
        if (s != score) score = s
        if (status == ArcadeStatus.PLAY && game.over && game.deadTime > GAME_OVER_DELAY) {
            status = ArcadeStatus.DEAD
            screenTime = 0.0
            newBest = s > (store.top(kind).firstOrNull() ?: 0)
            store.record(kind, s)
        }
    }

    /** Events raised since the last call, for particles and haptics. */
    fun drainEvents(): List<ArcadeEvent> = events.toList().also { events.clear() }

    /** Best score to show: the saved best, or the current run if it is higher. */
    fun bestScore(): Int = max(store.top(kind).firstOrNull() ?: 0, score)

    fun leaderboard(): List<ArcadeBoardRow> {
        val rows =
            ARCADE_SEEDS[kind].orEmpty().map { ArcadeBoardRow(it.first, it.second) } +
                store.top(kind).map { ArcadeBoardRow("TÚ", it, me = true) }
        return rows.sortedByDescending { it.points }.take(7)
    }

    companion object {
        /** Fixed simulation step, the same one the web prototype uses. */
        const val FIXED_STEP = 1.0 / 120.0

        /** Longest wall-clock gap one call may simulate, so a stall never makes the game jump ahead. */
        const val MAX_ADVANCE = 0.1

        /** Seconds after the run ends before the game-over screen appears (the hero gets to fall). */
        const val GAME_OVER_DELAY = 0.85

        /** Seconds the game-over screen ignores taps, so a frantic tap does not skip it. */
        const val RETRY_DELAY = 0.7
    }
}
