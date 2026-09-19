package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import kotlinx.coroutines.isActive

private val RunnerCardBg = Color(0xFF1C1D1B)
private val RunnerCardText = Color(0xFFF5F2E8)

private const val MASCOT_BOX_DP = 52f
private const val MASCOT_FEET_FRACTION = 0.777f
private const val MASCOT_HITBOX_LEFT = 8f
private const val MASCOT_HITBOX_RIGHT = 36f
private const val MASCOT_HITBOX_HEIGHT = 38f
private const val GRAVITY = 2100f
private const val JUMP_VELOCITY = 505f
private const val SPEED_START = 165f
private const val SPEED_STEP = 5f
private const val SPEED_CAP = 300f

internal enum class RunnerStatus { Idle, Running, Over }

internal class RunnerObstacle(
    var x: Float,
    val side: Float,
    val kind: VaiinillaGlyphKind,
    var passed: Boolean = false,
)

internal class RunnerGame {
    var status by mutableStateOf(RunnerStatus.Idle)
    var score by mutableIntStateOf(0)
    var best by mutableIntStateOf(0)
    var frame by mutableLongStateOf(0L)

    var widthDp = 360f
    var groundYdp = 120f
    var mascotOffsetY = 0f
    var mascotVy = 0f
    var runPhase = 0f
    var speed = SPEED_START
    var spawnIn = 220f
    val obstacles = mutableListOf<RunnerObstacle>()
    private val rng = kotlin.random.Random.Default

    val airborne get() = mascotOffsetY > 0.5f
    val mascotX get() = widthDp * 0.16f

    fun tap() {
        when (status) {
            RunnerStatus.Running -> jump()
            RunnerStatus.Idle -> {
                status = RunnerStatus.Running
                jump()
            }
            RunnerStatus.Over -> {
                reset()
                status = RunnerStatus.Running
                jump()
            }
        }
    }

    private fun jump() {
        if (!airborne) mascotVy = JUMP_VELOCITY
    }

    private fun reset() {
        score = 0
        obstacles.clear()
        speed = SPEED_START
        mascotOffsetY = 0f
        mascotVy = 0f
        spawnIn = 200f
    }

    fun step(dt: Float) {
        if (status != RunnerStatus.Running) return
        frame++
        runPhase += dt * speed / 9f
        mascotVy -= GRAVITY * dt
        mascotOffsetY += mascotVy * dt
        if (mascotOffsetY <= 0f) {
            mascotOffsetY = 0f
            mascotVy = 0f
        }
        val it = obstacles.iterator()
        while (it.hasNext()) {
            val ob = it.next()
            ob.x -= speed * dt
            when {
                ob.x + ob.side < 0f -> it.remove()
                !ob.passed && ob.x + ob.side < mascotX -> {
                    ob.passed = true
                    score++
                    speed = kotlin.math.min(SPEED_CAP, speed + SPEED_STEP)
                }
            }
        }
        spawnIn -= speed * dt
        if (spawnIn <= 0f) spawn()
        if (collides()) {
            status = RunnerStatus.Over
            if (score > best) best = score
        }
    }

    private fun spawn() {
        val kind =
            when (rng.nextInt(3)) {
                0 -> VaiinillaGlyphKind.Cup
                1 -> VaiinillaGlyphKind.Cube
                else -> VaiinillaGlyphKind.Note
            }
        val side =
            when (kind) {
                VaiinillaGlyphKind.Cup -> 34f
                VaiinillaGlyphKind.Cube -> 30f
                else -> 32f
            }
        obstacles += RunnerObstacle(x = widthDp + 24f, side = side, kind = kind)
        val airtime = 2f * JUMP_VELOCITY / GRAVITY
        spawnIn = speed * airtime + 110f + rng.nextFloat() * 150f
    }

    private fun collides(): Boolean {
        val mLeft = mascotX + MASCOT_HITBOX_LEFT
        val mRight = mascotX + MASCOT_HITBOX_RIGHT
        val feetY = groundYdp - mascotOffsetY
        val mTop = feetY - MASCOT_HITBOX_HEIGHT
        return obstacles.any { ob ->
            val oLeft = ob.x + ob.side * 0.2f
            val oRight = ob.x + ob.side * 0.8f
            val oTop = groundYdp - ob.side * 0.78f
            mRight > oLeft && mLeft < oRight && feetY > oTop && mTop < groundYdp
        }
    }
}

@Composable
internal fun WaitingRunnerCard(game: RunnerGame = remember { RunnerGame() }) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()

    LaunchedEffect(game.status) {
        if (game.status != RunnerStatus.Running) return@LaunchedEffect
        var last = 0L
        while (isActive) {
            withFrameNanos { t ->
                if (last == 0L) last = t
                val dt = ((t - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
                last = t
                game.step(dt)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = RunnerCardBg,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .testTag("waiting-runner")
                        .pointerInput(Unit) {
                            detectTapGestures {
                                haptics.click()
                                game.tap()
                            }
                        },
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val d = density
                    game.widthDp = size.width / d
                    game.groundYdp = size.height / d - 12f
                    @Suppress("UNUSED_EXPRESSION")
                    game.frame

                    val groundPx = game.groundYdp * d
                    drawLine(
                        RunnerCardText.copy(alpha = 0.32f),
                        Offset(0f, groundPx),
                        Offset(size.width, groundPx),
                        strokeWidth = 1.6f.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    val tickSpacing = 34f * d
                    val tickOffset = -((game.runPhase * 9f * d) % tickSpacing)
                    var tx = tickOffset
                    while (tx < size.width) {
                        drawLine(
                            RunnerCardText.copy(alpha = 0.14f),
                            Offset(tx, groundPx + 7f * d),
                            Offset(tx + 9f * d, groundPx + 7f * d),
                            strokeWidth = 1.4f.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                        tx += tickSpacing
                    }

                    for (ob in game.obstacles) {
                        val sidePx = ob.side * d
                        withTransform({
                            translate(ob.x * d, groundPx - sidePx)
                            scale(sidePx / 100f, sidePx / 100f, pivot = Offset.Zero)
                        }) {
                            drawVaiinillaGlyphUnits(
                                ob.kind,
                                RunnerCardText,
                                colors.accent,
                            )
                        }
                    }

                    val boxPx = MASCOT_BOX_DP * d
                    val mascotTop = groundPx - game.mascotOffsetY * d - boxPx * MASCOT_FEET_FRACTION
                    val lean =
                        when {
                            game.status == RunnerStatus.Over -> -86f
                            game.airborne -> (-game.mascotVy * 0.024f).coerceIn(-16f, 12f)
                            else -> 0f
                        }
                    withTransform({ translate(game.mascotX * d, mascotTop) }) {
                        drawVaiinillaMascotRunner(
                            boxPx = boxPx,
                            paperColor = RunnerCardText,
                            accentColor = colors.accent,
                            inkColor = RunnerCardBg,
                            runPhase = game.runPhase,
                            airborne = game.airborne,
                            leanDegrees = lean,
                        )
                    }
                }
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(start = 18.dp, top = 12.dp, end = 18.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (game.best > 0) {
                        Text(
                            "mejor ${game.best}",
                            color = RunnerCardText.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        "${game.score}",
                        color = RunnerCardText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                val hint =
                    when (game.status) {
                        RunnerStatus.Idle -> "Toca para brincar"
                        RunnerStatus.Over -> "¡Ups! Toca para otra"
                        RunnerStatus.Running -> null
                    }
                if (hint != null) {
                    Text(
                        hint,
                        modifier = Modifier.align(Alignment.Center),
                        color = RunnerCardText.copy(alpha = 0.72f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Preview(name = "Recreo run", showBackground = true, widthDp = 360)
@Composable
private fun WaitingRunnerCardPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        Box(modifier = Modifier.padding(16.dp)) {
            WaitingRunnerCard()
        }
    }
}
