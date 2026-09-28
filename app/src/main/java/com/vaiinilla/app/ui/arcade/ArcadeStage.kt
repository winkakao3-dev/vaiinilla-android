package com.vaiinilla.app.ui.arcade

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.vaiinilla.app.domain.arcade.ArcadeSession
import com.vaiinilla.app.domain.arcade.ArcadeStatus
import kotlinx.coroutines.channels.Channel
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Runs one arcade session on screen: advances the rules each frame, turns events into effects and haptics, and
 * paints the frame into a bitmap the card shows.
 *
 * While a run is on it repaints every frame. On the title and game-over screens it animates for a few seconds and
 * then freezes on a still frame and stops asking for frames: a customer can leave the card open for minutes while
 * the order cooks without it spending battery.
 */
internal class ArcadeStage(
    val session: ArcadeSession,
    private val haptic: (ArcadeHaptic) -> Unit = {},
) {
    val effects = ArcadeEffects()

    /** Bumps on every repaint and every user action, so the composables that read it redraw. */
    var version by mutableLongStateOf(0L)
        private set

    private val renderer = ArcadeRenderer()
    private val bitmap: Bitmap = Bitmap.createBitmap(PixelCanvas.WIDTH, PixelCanvas.HEIGHT, Bitmap.Config.ARGB_8888)
    val image: ImageBitmap = bitmap.asImageBitmap()

    /** Screen shake in canvas pixels; the card offsets the image by it. */
    var shakeX = 0f
        private set
    var shakeY = 0f
        private set

    private val wakeSignal = Channel<Unit>(Channel.CONFLATED)
    private var frozen = false
    private var idleClock = IDLE_ANIMATION
    private var lastStatus = session.status
    private var lastWork = 0.0

    init {
        paint(1.0 / 60.0, hold = false)
    }

    /** Something changed (a tap, another game, the board): animate again for a moment. */
    fun wake() {
        idleClock = IDLE_ANIMATION
        frozen = false
        version += 1
        wakeSignal.trySend(Unit)
    }

    /** Suspends until [wake] is called. Used by the frame loop while the stage is frozen. */
    suspend fun awaitWake() {
        wakeSignal.receive()
    }

    val isFrozen: Boolean get() = frozen

    /** Called once per display frame with the frame time in seconds. */
    fun tick(now: Double) {
        if (session.status != lastStatus) {
            lastStatus = session.status
            idleClock = IDLE_ANIMATION
        }
        val playing = session.status == ArcadeStatus.PLAY
        val interval = if (playing) 0.0 else IDLE_INTERVAL
        if (lastWork == 0.0) {
            lastWork = now
            return
        }
        if (now - lastWork < interval) return
        val dt = min(max(now - lastWork, 0.0), ArcadeSession.MAX_ADVANCE)
        lastWork = now
        if (session.boardOpen) {
            frozen = true
            return
        }
        session.advance(dt)
        for (h in ArcadeEffectsDirector.absorb(session.drainEvents(), session.game, effects)) haptic(h)
        effects.step(dt)
        if (playing) {
            idleClock = IDLE_ANIMATION
            paint(dt, hold = false)
            return
        }
        idleClock -= dt
        if (idleClock <= 0) {
            paint(dt, hold = true) // one last still frame, with the hint visible
            frozen = true
        } else {
            paint(dt, hold = false)
        }
    }

    /** Forget the clock so the first frame after a pause does not jump ahead. */
    fun resetClock() {
        lastWork = 0.0
    }

    private fun screenFor(status: ArcadeStatus) =
        when (status) {
            ArcadeStatus.IDLE -> ArcadeScreen.TITLE
            ArcadeStatus.PLAY -> ArcadeScreen.PLAYING
            ArcadeStatus.DEAD -> ArcadeScreen.OVER
        }

    private fun paint(
        dt: Double,
        hold: Boolean,
    ) {
        val info =
            ArcadeFrameInfo(
                screenFor(session.status),
                session.screenTime,
                session.bestScore(),
                session.newBest,
                dt,
                true,
                hold,
            )
        renderer.render(session.kind, session.game, effects, info)
        val s = effects.shake
        shakeX = if (s > 0) ((Random.nextDouble() - 0.5) * 6 * s).toFloat() else 0f
        shakeY = if (s > 0) ((Random.nextDouble() - 0.5) * 6 * s).toFloat() else 0f
        val c = renderer.canvas
        bitmap.setPixels(c.pixels, 0, c.width, 0, 0, c.width, c.height)
        version += 1
    }

    companion object {
        private const val IDLE_ANIMATION = 3.0
        private const val IDLE_INTERVAL = 0.1
    }
}
