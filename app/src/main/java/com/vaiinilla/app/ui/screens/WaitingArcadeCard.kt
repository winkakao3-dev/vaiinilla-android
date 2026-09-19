package com.vaiinilla.app.ui.screens

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.VaiinillaHaptics
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

private val ArcadePanel = Color(0xFF1C1D1B)
private val ArcadeWell = Color(0xFF0A0A0C)
private val ArcadeControl = Color(0xFF232427)
private val ArcadePill = Color(0xFF4A4A4E)
private val ArcadeInk = Color(0xFFF5F2E8)
private val ArcadeDim = Color(0xFF8E8E93)
private val ArcadeLine = Color.White.copy(alpha = 0.09f)

internal enum class ArcadeKind(
    val label: String,
    val hint: String,
) {
    FLAPPY("Flappy", "Toca para volar"),
    BRINCA("Brinca", "Toca para brincar"),
    APILA("Apila", "Toca para soltar"),
    GRAVEDAD("Gravedad", "Toca para cambiar la gravedad"),
}

internal enum class ArcadeStatus { IDLE, PLAY, DEAD }

// ===================== juegos =====================

internal sealed interface ArcadeGame {
    val kind: ArcadeKind
    var score: Float

    fun reset(
        width: Float,
        height: Float,
    )

    fun tap()

    /** Avanza la física; devuelve true si la partida terminó en este paso. */
    fun step(
        dt: Float,
        width: Float,
        height: Float,
    ): Boolean
}

internal class BrincaGame : ArcadeGame {
    override val kind = ArcadeKind.BRINCA
    override var score = 0f
    var y = 0f
    var vy = 0f
    var speed = 175f
    var nextIn = 0.6f
    val obstacles = mutableListOf<Ob>()

    data class Ob(
        var x: Float,
        val w: Float,
        val h: Float,
    )

    fun floorY(height: Float) = height - 52f

    override fun reset(
        width: Float,
        height: Float,
    ) {
        score = 0f
        y = floorY(height)
        vy = 0f
        speed = 175f
        nextIn = 0.6f
        obstacles.clear()
    }

    override fun tap() {
        // se decide fuera del suelo con y; el motor aplica solo si está en el piso
        pendingJump = true
    }

    private var pendingJump = false

    fun jumpIfGrounded() {
        if (pendingJump && y >= floorYcached - 0.5f) vy = -575f
        pendingJump = false
    }

    private var floorYcached = 0f

    override fun step(
        dt: Float,
        width: Float,
        height: Float,
    ): Boolean {
        floorYcached = floorY(height)
        jumpIfGrounded()
        speed = min(310f, speed + 2.5f * dt)
        vy += 1750f * dt
        y += vy * dt
        if (y > floorYcached) {
            y = floorYcached
            vy = 0f
        }
        nextIn -= dt
        if (nextIn <= 0f) {
            nextIn = 0.85f + Random.nextFloat() * 0.65f
            obstacles.add(Ob(width + 20f, 14f + Random.nextFloat() * 6f, 22f + Random.nextFloat() * 22f))
        }
        val mx = 64f
        val r = 14f
        for (o in obstacles) {
            o.x -= speed * dt
            if (mx + r > o.x && mx - r < o.x + o.w && y + r * 0.4f > floorYcached - o.h) return true
        }
        obstacles.removeAll { it.x < -30f }
        score += dt * 10f
        return false
    }
}

internal class FlappyGame : ArcadeGame {
    override val kind = ArcadeKind.FLAPPY
    override var score = 0f
    var y = 0f
    var vy = 0f
    var speed = 150f
    var nextIn = 0f
    val columns = mutableListOf<Col>()

    data class Col(
        var x: Float,
        val gapTop: Float,
        val gap: Float,
        var passed: Boolean = false,
    )

    override fun reset(
        width: Float,
        height: Float,
    ) {
        score = 0f
        y = height * 0.45f
        vy = 0f
        speed = 150f
        nextIn = 0f
        columns.clear()
    }

    override fun tap() {
        vy = -430f
    }

    override fun step(
        dt: Float,
        width: Float,
        height: Float,
    ): Boolean {
        vy += 1650f * dt
        y += vy * dt
        speed = min(235f, speed + 3f * dt)
        nextIn -= dt
        if (nextIn <= 0f) {
            nextIn = 1.5f
            columns.add(
                Col(
                    width + 30f,
                    80f + Random.nextFloat() * max(1f, height - 220f),
                    122f,
                ),
            )
        }
        val mx = 70f
        val r = 13f
        for (c in columns) {
            c.x -= speed * dt
            if (!c.passed && c.x + 46f < mx - r) {
                c.passed = true
                score += 1f
            }
            if (mx + r > c.x && mx - r < c.x + 46f && (y - r < c.gapTop || y + r > c.gapTop + c.gap)) {
                return true
            }
        }
        columns.removeAll { it.x < -60f }
        return y > height - 30f || y < -20f
    }
}

internal class ApilaGame : ArcadeGame {
    override val kind = ArcadeKind.APILA
    override var score = 0f
    val stack = mutableListOf<Block>()
    var pos = 0f
    var dir = 1f
    val blockH = 20f
    var cam = 0f
    var fall: Fall? = null

    data class Block(
        val x: Float,
        val w: Float,
    )

    data class Fall(
        val x: Float,
        val w: Float,
        var y: Float,
        var vy: Float,
    )

    fun topY(height: Float) = height - 46f - (stack.size - 1) * blockH + cam

    override fun reset(
        width: Float,
        height: Float,
    ) {
        score = 0f
        stack.clear()
        stack.add(Block(width / 2f - 75f, 150f))
        pos = 0f
        dir = 1f
        cam = 0f
        fall = null
    }

    /** Devuelve true si el bloque cayó sin apoyarse (la partida acabó). */
    fun drop(
        width: Float,
        height: Float,
    ): Boolean {
        val top = stack.last()
        val l = max(pos, top.x)
        val r = min(pos + top.w, top.x + top.w)
        val w = r - l
        if (w <= 0f) {
            fall = Fall(pos, top.w, topY(height) - blockH, 0f)
            return true
        }
        val diff = abs(pos - top.x)
        stack.add(Block(if (diff < 4f) top.x else l, if (diff < 4f) top.w else w))
        score += 1f
        pos = if (score.toInt() % 2 == 1) width - stack.last().w else 0f
        dir = if (score.toInt() % 2 == 1) -1f else 1f
        return false
    }

    override fun tap() {
        pendingDrop = true
    }

    private var pendingDrop = false
    private var died = false

    fun consumeDrop(
        width: Float,
        height: Float,
    ): Boolean {
        if (!pendingDrop) return false
        pendingDrop = false
        return drop(width, height)
    }

    override fun step(
        dt: Float,
        width: Float,
        height: Float,
    ): Boolean {
        if (consumeDrop(width, height)) died = true
        val top = stack.last()
        val spd = min(300f, 130f + score * 9f)
        pos += dir * spd * dt
        if (pos < 0f) {
            pos = 0f
            dir = 1f
        }
        if (pos + top.w > width) {
            pos = width - top.w
            dir = -1f
        }
        val want = max(0f, stack.size * blockH - (height - 150f))
        cam += (want - cam) * min(1f, dt * 6f)
        fall?.let {
            it.vy += 1600f * dt
            it.y += it.vy * dt
        }
        return died
    }
}

internal class GravedadGame : ArcadeGame {
    override val kind = ArcadeKind.GRAVEDAD
    override var score = 0f
    var gdir = 1f
    var y = 0f
    var vy = 0f
    var speed = 180f
    var nextIn = 0.8f
    val obstacles = mutableListOf<Ob>()

    data class Ob(
        var x: Float,
        val w: Float,
        val side: Float,
        var passed: Boolean = false,
    )

    fun ceilY(height: Float) = 58f

    fun floorY(height: Float) = height - 58f

    override fun reset(
        width: Float,
        height: Float,
    ) {
        score = 0f
        gdir = 1f
        y = floorY(height)
        vy = 0f
        speed = 180f
        nextIn = 0.8f
        obstacles.clear()
    }

    override fun tap() {
        gdir *= -1f
    }

    override fun step(
        dt: Float,
        width: Float,
        height: Float,
    ): Boolean {
        speed = min(300f, speed + 2.5f * dt)
        vy += 2300f * gdir * dt
        y += vy * dt
        val target = if (gdir > 0f) floorY(height) else ceilY(height)
        if (gdir > 0f && y > target) {
            y = target
            vy = 0f
        }
        if (gdir < 0f && y < target) {
            y = target
            vy = 0f
        }
        nextIn -= dt
        if (nextIn <= 0f) {
            nextIn = 1.0f + Random.nextFloat() * 0.6f
            obstacles.add(
                Ob(
                    width + 20f,
                    38f + Random.nextFloat() * 42f,
                    if (Random.nextBoolean()) 1f else -1f,
                ),
            )
        }
        val mx = 64f
        val r = 13f
        for (o in obstacles) {
            o.x -= speed * dt
            if (!o.passed && o.x + o.w < mx - r) {
                o.passed = true
                score += 1f
            }
            if (mx + r > o.x && mx - r < o.x + o.w) {
                val surf = if (o.side > 0f) floorY(height) else ceilY(height)
                if (abs(y - surf) < 22f) return true
            }
        }
        obstacles.removeAll { it.x < -100f }
        return false
    }
}

// ===================== marcador =====================

internal interface ArcadeScoreStore {
    fun top(kind: ArcadeKind): List<Int>

    fun record(
        kind: ArcadeKind,
        score: Int,
    )
}

internal class MemoryArcadeScoreStore : ArcadeScoreStore {
    private val data = mutableMapOf<ArcadeKind, MutableList<Int>>()

    override fun top(kind: ArcadeKind) = data[kind]?.toList() ?: emptyList()

    override fun record(
        kind: ArcadeKind,
        score: Int,
    ) {
        val list = data.getOrPut(kind) { mutableListOf() }
        list.add(score)
        list.sortDescending()
        while (list.size > 8) list.removeAt(list.lastIndex)
    }
}

internal class PrefsArcadeScoreStore(
    context: Context,
) : ArcadeScoreStore {
    private val prefs = context.getSharedPreferences("vaiinilla.arcade", Context.MODE_PRIVATE)

    override fun top(kind: ArcadeKind): List<Int> =
        prefs
            .getString(kind.name, "")
            .orEmpty()
            .split(",")
            .mapNotNull { it.toIntOrNull() }

    override fun record(
        kind: ArcadeKind,
        score: Int,
    ) {
        val list = (top(kind) + score).sortedDescending().take(8)
        prefs.edit().putString(kind.name, list.joinToString(",")).apply()
    }
}

internal data class ArcadeBoardRow(
    val name: String,
    val points: Int,
    val me: Boolean = false,
)

private val ARCADE_SEEDS: Map<ArcadeKind, List<Pair<String, Int>>> =
    mapOf(
        ArcadeKind.FLAPPY to listOf("LA COCINA" to 9, "DOÑA V" to 6, "CAJA" to 4),
        ArcadeKind.BRINCA to listOf("LA COCINA" to 32, "DOÑA V" to 24, "MESERO" to 18),
        ArcadeKind.APILA to listOf("DOÑA V" to 14, "LA COCINA" to 11, "MESERO" to 8),
        ArcadeKind.GRAVEDAD to listOf("MESERO" to 15, "DOÑA V" to 12, "LA COCINA" to 9),
    )

// ===================== motor =====================

internal class ArcadeEngine(
    internal val store: ArcadeScoreStore = MemoryArcadeScoreStore(),
) {
    var kind by mutableStateOf(ArcadeKind.BRINCA)
        private set
    var status by mutableStateOf(ArcadeStatus.IDLE)
        private set
    var score by mutableIntStateOf(0)
        private set
    var frame by mutableLongStateOf(0L)
        private set
    var boardOpen by mutableStateOf(false)
    var width by mutableFloatStateOf(360f)
    var height by mutableFloatStateOf(250f)
    var runPhase by mutableFloatStateOf(0f)

    var game: ArcadeGame = BrincaGame()
        private set

    init {
        game.reset(width, height)
    }

    fun select(next: ArcadeKind) {
        if (next == kind && status != ArcadeStatus.DEAD) return
        kind = next
        game = gameFor(next)
        game.reset(width, height)
        status = ArcadeStatus.IDLE
        score = 0
        boardOpen = false
    }

    /** Toque sobre el escenario: arranca, salta o deja el dead overlay actuar. */
    fun primary() {
        if (status == ArcadeStatus.DEAD || boardOpen) return
        if (status == ArcadeStatus.IDLE) {
            start()
        } else {
            game.tap()
        }
    }

    fun retry() {
        start()
    }

    private fun start() {
        game.reset(width, height)
        status = ArcadeStatus.PLAY
        score = 0
        boardOpen = false
        game.tap()
    }

    fun step(dt: Float) {
        if (status != ArcadeStatus.PLAY || boardOpen) return
        runPhase += dt * gameSpeedForPhase() / 9f
        if (game.step(dt, width, height)) {
            status = ArcadeStatus.DEAD
            store.record(kind, score)
        }
        score = game.score.toInt()
        frame++
    }

    private fun gameSpeedForPhase(): Float =
        when (val g = game) {
            is BrincaGame -> g.speed
            is GravedadGame -> g.speed
            else -> 175f
        }

    fun bestScore(): Int = (store.top(kind) + score).maxOrNull() ?: 0

    fun leaderboard(): List<ArcadeBoardRow> {
        val rows = mutableListOf<ArcadeBoardRow>()
        ARCADE_SEEDS[kind].orEmpty().forEach { (n, s) -> rows += ArcadeBoardRow(n, s) }
        store.top(kind).forEach { s -> rows += ArcadeBoardRow("TÚ", s, me = true) }
        return rows.sortedByDescending { it.points }.take(7)
    }

    private fun gameFor(k: ArcadeKind): ArcadeGame =
        when (k) {
            ArcadeKind.FLAPPY -> FlappyGame()
            ArcadeKind.BRINCA -> BrincaGame()
            ArcadeKind.APILA -> ApilaGame()
            ArcadeKind.GRAVEDAD -> GravedadGame()
        }
}

// ===================== dibujo =====================

private const val ARCADE_MASCOT_BOX_DP = 44f
private const val ARCADE_FEET_FRACTION = 0.777f

private fun DrawScope.dashedGround(
    y: Float,
    ink: Color,
) {
    val dash = 14f
    val gap = 16f
    var x = 10f
    while (x < size.width - 10f) {
        drawLine(
            ink.copy(alpha = 0.28f),
            Offset(x, y),
            Offset(min(x + dash, size.width - 10f), y),
            strokeWidth = 2f,
            cap = StrokeCap.Round,
        )
        x += dash + gap
    }
}

private fun DrawScope.arcadeMascot(
    feetX: Float,
    feetY: Float,
    boxPx: Float,
    runPhase: Float,
    airborne: Boolean,
    tiltDeg: Float,
    upsideDown: Boolean,
    paper: Color,
    accent: Color,
) {
    withTransform({
        translate(feetX - boxPx * 0.5f, feetY - boxPx * ARCADE_FEET_FRACTION)
        if (upsideDown) scale(1f, -1f, pivot = Offset(boxPx * 0.5f, boxPx * 0.41f))
    }) {
        drawVaiinillaMascotRunner(
            boxPx = boxPx,
            paperColor = paper,
            accentColor = accent,
            inkColor = ArcadeWell,
            runPhase = runPhase,
            airborne = airborne,
            leanDegrees = tiltDeg,
        )
    }
}

private fun DrawScope.roundedRect(
    left: Float,
    top: Float,
    w: Float,
    h: Float,
    r: Float,
    color: Color,
) {
    drawRoundRect(
        color,
        Offset(left, top),
        Size(w, h),
        CornerRadius(r, r),
    )
}

private fun DrawScope.drawArcadeFrame(
    engine: ArcadeEngine,
    accent: Color,
) {
    val g = engine.game
    val u = density
    val wDp = size.width / u
    val hDp = size.height / u
    val box = ARCADE_MASCOT_BOX_DP * u

    when (g) {
        is FlappyGame -> {
            dashedGround((hDp - 24f) * u, ArcadeInk)
            for (c in g.columns) {
                roundedRect(c.x * u, -6f * u, 46f * u, (c.gapTop + 6f) * u, 10f * u, ArcadeControl)
                roundedRect(
                    c.x * u,
                    (c.gapTop + c.gap) * u,
                    46f * u,
                    (hDp - c.gapTop - c.gap - 18f) * u,
                    10f * u,
                    ArcadeControl,
                )
                drawRect(
                    accent,
                    Offset((c.x + 5f) * u, (c.gapTop - 6f) * u),
                    Size(36f * u, 5f * u),
                )
                drawRect(
                    accent,
                    Offset((c.x + 5f) * u, (c.gapTop + c.gap + 1f) * u),
                    Size(36f * u, 5f * u),
                )
            }
            val tilt =
                (
                    if (engine.status == ArcadeStatus.IDLE) {
                        0f
                    } else {
                        (g.vy / 600f) * 57.2958f
                    }
                ).coerceIn(-29f, 52f)
            val idleBob = if (engine.status == ArcadeStatus.IDLE) kotlin.math.sin(engine.frame / 18f) * 6f else 0f
            arcadeMascot(
                feetX = 70f * u,
                feetY = (g.y + idleBob) * u + box * ARCADE_FEET_FRACTION * 0.5f,
                boxPx = box,
                runPhase = engine.runPhase,
                airborne = true,
                tiltDeg = tilt,
                upsideDown = false,
                paper = ArcadeInk,
                accent = accent,
            )
        }

        is BrincaGame -> {
            val gy = (g.floorY(hDp) + 15f) * u
            dashedGround(gy, ArcadeInk)
            for (o in g.obstacles) {
                roundedRect(o.x * u, gy - o.h * u, o.w * u, o.h * u, 4f * u, ArcadeInk)
                drawRect(
                    accent,
                    Offset((o.x + 2f) * u, gy - (o.h - 3f) * u),
                    Size((o.w - 4f) * u, 3f * u),
                )
            }
            arcadeMascot(
                feetX = 70f * u,
                feetY = g.y * u + 15f * u,
                boxPx = box,
                runPhase = engine.runPhase,
                airborne = g.y < g.floorY(hDp) - 0.5f,
                tiltDeg = (g.vy / 1400f * 57.2958f).coerceIn(-30f, 30f),
                upsideDown = false,
                paper = ArcadeInk,
                accent = accent,
            )
        }

        is ApilaGame -> {
            dashedGround((hDp - 22f) * u, ArcadeInk)
            g.stack.forEachIndexed { i, b ->
                val by = (hDp - 46f - i * g.blockH + g.cam) * u
                if (by <= size.height + 20f * u) {
                    roundedRect(
                        b.x * u,
                        by,
                        b.w * u,
                        (g.blockH - 3f) * u,
                        4f * u,
                        if (i % 5 == 4) accent else ArcadeInk,
                    )
                }
            }
            g.fall?.let { f ->
                roundedRect(f.x * u, f.y * u, f.w * u, (g.blockH - 3f) * u, 4f * u, ArcadeInk)
            }
            if (engine.status != ArcadeStatus.IDLE) {
                val moving = g.pos
                val top = g.stack.last()
                val my = (g.topY(hDp) - g.blockH - 24f) * u
                roundedRect(
                    moving * u,
                    my,
                    top.w * u,
                    (g.blockH - 3f) * u,
                    4f * u,
                    if (g.stack.size % 5 == 4) accent else ArcadeInk,
                )
                arcadeMascot(
                    feetX = (moving + top.w / 2f) * u,
                    feetY = my,
                    boxPx = box * 0.8f,
                    runPhase = engine.runPhase,
                    airborne = true,
                    tiltDeg = 0f,
                    upsideDown = false,
                    paper = ArcadeInk,
                    accent = accent,
                )
            } else {
                arcadeMascot(
                    feetX = size.width / 2f,
                    feetY = (g.topY(hDp) - g.blockH + 4f) * u,
                    boxPx = box * 0.85f,
                    runPhase = engine.runPhase,
                    airborne = true,
                    tiltDeg = 0f,
                    upsideDown = false,
                    paper = ArcadeInk,
                    accent = accent,
                )
            }
        }

        is GravedadGame -> {
            drawLine(
                ArcadeControl,
                Offset(10f * u, (g.ceilY(hDp) - 16f) * u),
                Offset(size.width - 10f * u, (g.ceilY(hDp) - 16f) * u),
                strokeWidth = 2f * u,
            )
            dashedGround((g.floorY(hDp) + 15f) * u, ArcadeInk)
            for (o in g.obstacles) {
                val baseY = if (o.side > 0f) (g.floorY(hDp) + 15f) * u else (g.ceilY(hDp) - 16f) * u
                val n = max(2, (o.w / 14f).toInt())
                val path = Path()
                for (i in 0 until n) {
                    val x0 = (o.x + o.w * i / n) * u
                    val tip = if (o.side > 0f) baseY - 20f * u else baseY + 20f * u
                    path.moveTo(x0, baseY)
                    path.lineTo(x0 + (o.w / n / 2f) * u, tip)
                    path.lineTo(x0 + (o.w / n) * u, baseY)
                }
                path.close()
                drawPath(path, ArcadeInk.copy(alpha = 0.86f))
            }
            val onCeil = g.gdir < 0f
            arcadeMascot(
                feetX = 70f * u,
                feetY = (g.y + if (onCeil) -14f else 15f) * u,
                boxPx = box,
                runPhase = engine.runPhase,
                airborne = false,
                tiltDeg = 0f,
                upsideDown = onCeil,
                paper = ArcadeInk,
                accent = accent,
            )
        }
    }
}

// ===================== tarjeta =====================

@Composable
internal fun WaitingArcadeCard(
    engine: ArcadeEngine? = null,
    haptics: VaiinillaHaptics = rememberVaiinillaHaptics(),
) {
    val colors = LocalVaiinillaColors.current
    val context = LocalContext.current
    val e = engine ?: remember { ArcadeEngine(PrefsArcadeScoreStore(context)) }

    LaunchedEffect(e.status, e.boardOpen) {
        if (e.status != ArcadeStatus.PLAY || e.boardOpen) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last == 0L) {
                    last = now
                } else {
                    val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.033f)
                    last = now
                    e.step(dt)
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ArcadePanel,
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier =
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ArcadeControl)
                            .padding(3.dp),
                ) {
                    ArcadeKind.entries.forEach { k ->
                        val selected = k == e.kind
                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (selected) ArcadePill else Color.Transparent)
                                    .pointerInput(k) {
                                        detectTapGestures {
                                            haptics.click()
                                            e.select(k)
                                        }
                                    },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                k.label,
                                color = if (selected) ArcadeInk else ArcadeDim,
                                fontSize = 12.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                            )
                        }
                    }
                }
                Box(
                    modifier =
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ArcadeControl)
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    haptics.click()
                                    e.boardOpen = !e.boardOpen
                                }
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.EmojiEvents,
                        contentDescription = "Leaderboard",
                        tint = if (e.boardOpen) colors.accent else ArcadeDim,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .height(250.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ArcadeWell),
            ) {
                Canvas(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .testTag("waiting-arcade")
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    haptics.click()
                                    e.primary()
                                }
                            },
                ) {
                    e.width = size.width / density
                    e.height = size.height / density
                    @Suppress("UNUSED_EXPRESSION")
                    e.frame
                    drawArcadeFrame(e, colors.accent)
                }

                val hint =
                    when {
                        e.boardOpen -> null
                        e.status == ArcadeStatus.IDLE -> e.kind.hint
                        else -> null
                    }
                if (hint != null) {
                    Text(
                        hint,
                        modifier = Modifier.align(Alignment.Center),
                        color = ArcadeDim,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    "${e.score}",
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 12.dp, end = 14.dp),
                    color = ArcadeInk,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                )

                if (e.status == ArcadeStatus.DEAD && !e.boardOpen) {
                    ArcadeDeadOverlay(
                        score = e.score,
                        best = e.bestScore(),
                        accent = colors.accent,
                        accentInk = colors.accentInk,
                        onRetry = {
                            haptics.click()
                            e.retry()
                        },
                        onBoard = {
                            haptics.click()
                            e.boardOpen = true
                        },
                    )
                }
                if (e.boardOpen) {
                    ArcadeBoardOverlay(
                        kind = e.kind,
                        rows = e.leaderboard(),
                        accent = colors.accent,
                        onClose = {
                            haptics.click()
                            e.boardOpen = false
                        },
                    )
                }
            }

            Text(
                "Mini juegos mientras va tu pedido",
                modifier = Modifier.fillMaxWidth().padding(top = 9.dp, start = 4.dp, end = 4.dp),
                color = ArcadeDim,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ArcadeDeadOverlay(
    score: Int,
    best: Int,
    accent: Color,
    accentInk: Color,
    onRetry: () -> Unit,
    onBoard: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color(0xDB0A0A0C)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Se acabó", color = ArcadeInk, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "$score ${if (score == 1) "punto" else "puntos"} · tu mejor: $best",
            color = ArcadeDim,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(14.dp))
        Surface(
            modifier =
                Modifier
                    .clip(CircleShape)
                    .pointerInput(Unit) { detectTapGestures { onRetry() } },
            color = accent,
            shape = CircleShape,
        ) {
            Text(
                "Otra vez",
                modifier = Modifier.padding(horizontal = 30.dp, vertical = 12.dp),
                color = accentInk,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Ver leaderboard",
            modifier = Modifier.pointerInput(Unit) { detectTapGestures { onBoard() } }.padding(6.dp),
            color = ArcadeDim,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ArcadeBoardOverlay(
    kind: ArcadeKind,
    rows: List<ArcadeBoardRow>,
    accent: Color,
    onClose: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(ArcadeWell)
                .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text("Leaderboard", color = ArcadeInk, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "${kind.label.lowercase().replaceFirstChar { it.uppercase() }} · top ${min(7, rows.size)}",
            color = ArcadeDim,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
        )
        rows.forEachIndexed { i, row ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .border(width = 0.dp, color = Color.Transparent)
                        .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${i + 1}",
                    modifier = Modifier.width(18.dp),
                    color = ArcadeDim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    row.name,
                    modifier = Modifier.weight(1f),
                    color = if (row.me) accent else ArcadeInk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "${row.points}",
                    color = if (row.me) accent else ArcadeInk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            if (i < rows.lastIndex) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(ArcadeLine))
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            "Seguir jugando",
            modifier =
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .pointerInput(Unit) { detectTapGestures { onClose() } }
                    .padding(6.dp),
            color = ArcadeDim,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Preview(name = "Arcade", showBackground = true, widthDp = 360)
@Composable
private fun WaitingArcadeCardPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        Box(modifier = Modifier.padding(16.dp)) {
            WaitingArcadeCard()
        }
    }
}
