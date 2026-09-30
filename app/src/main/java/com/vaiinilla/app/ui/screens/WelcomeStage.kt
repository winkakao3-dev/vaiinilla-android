package com.vaiinilla.app.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.R
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * The welcome hero: Vaini on its stage, with the photos of its other lives packed around it like
 * stickers. They burst out from behind Vaini on arrival, then one at a time flips like a coin to
 * show another scene while Vaini glances at it. Positions are fractions of the square hero.
 */

private val WelcomeScenes =
    listOf(
        R.drawable.welcome_scene_karate,
        R.drawable.welcome_scene_moto,
        R.drawable.welcome_scene_bloques,
        R.drawable.welcome_scene_tacos,
        R.drawable.welcome_scene_puente,
        R.drawable.welcome_scene_laptop,
        R.drawable.welcome_scene_oxxo,
        R.drawable.welcome_scene_taller,
        R.drawable.welcome_scene_camion,
    )

private data class PhotoSlot(
    val x: Float,
    val y: Float,
    val size: Float,
    val tilt: Float,
    val round: Boolean,
    val phase: Float,
    val arriveOrder: Int,
)

private val PhotoSlots =
    listOf(
        PhotoSlot(x = 0.22f, y = 0.21f, size = 0.31f, tilt = -8f, round = false, phase = 0.00f, arriveOrder = 0),
        PhotoSlot(x = 0.78f, y = 0.20f, size = 0.27f, tilt = 9f, round = true, phase = 0.35f, arriveOrder = 1),
        PhotoSlot(x = 0.84f, y = 0.56f, size = 0.24f, tilt = -6f, round = false, phase = 0.60f, arriveOrder = 2),
        PhotoSlot(x = 0.16f, y = 0.65f, size = 0.25f, tilt = 7f, round = true, phase = 0.20f, arriveOrder = 5),
        PhotoSlot(x = 0.71f, y = 0.85f, size = 0.27f, tilt = -10f, round = false, phase = 0.80f, arriveOrder = 3),
        PhotoSlot(x = 0.38f, y = 0.87f, size = 0.21f, tilt = 6f, round = false, phase = 0.50f, arriveOrder = 4),
    )

/** Slots flip in this order, jumping around the stage so the eye keeps moving. */
private val FlipOrder = listOf(1, 3, 5, 0, 4, 2)

private enum class AccentKind { Food, Spark, Dashes, Flower }

private data class AccentSpec(
    val kind: AccentKind,
    val x: Float,
    val y: Float,
    val size: Float,
    val tilt: Float,
    val phase: Float,
    @DrawableRes val drawable: Int = 0,
)

private val AccentSpecs =
    listOf(
        AccentSpec(AccentKind.Food, 0.50f, 0.10f, 0.11f, 12f, 0.50f, R.drawable.welcome_bubble_jamaica),
        AccentSpec(AccentKind.Food, 0.12f, 0.87f, 0.11f, -10f, 0.10f, R.drawable.welcome_bubble_waffle),
        AccentSpec(AccentKind.Food, 0.07f, 0.43f, 0.10f, 8f, 0.70f, R.drawable.welcome_bubble_torta),
        AccentSpec(AccentKind.Spark, 0.70f, 0.33f, 0.07f, 0f, 0.25f),
        AccentSpec(AccentKind.Spark, 0.94f, 0.37f, 0.05f, 18f, 0.65f),
        AccentSpec(AccentKind.Dashes, 0.26f, 0.43f, 0.09f, 0f, 0.40f),
        AccentSpec(AccentKind.Flower, 0.94f, 0.76f, 0.07f, 14f, 0.90f),
    )

private const val DISC_CENTER_Y = 0.52f
private const val DISC_SIZE = 0.64f
private const val MASCOT_SIZE = 0.52f
private const val FIRST_FLIP_DELAY_MS = 2200L
private const val FLIP_GAP_MS = 1300L
private const val BOB_PERIOD_MS = 5600

private val StickerEdgeLight = Color(0xFFFFFCF4)
private val StickerEdgeDark = Color(0xFFF6EFDD)
private val MascotPaper = Color(0xFFF5ECDA)
private val MascotInk = Color(0xFF1D1C18)

@Composable
internal fun WelcomeHero(
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight)
        Box(modifier = Modifier.size(side)) {
            WelcomeHeroStage(side = side, reduceMotion = reduceMotion)
        }
    }
}

@Composable
private fun WelcomeHeroStage(
    side: Dp,
    reduceMotion: Boolean,
) {
    val colors = LocalVaiinillaColors.current
    val sidePx = with(LocalDensity.current) { side.toPx() }
    val bobTravelPx = with(LocalDensity.current) { 3.dp.toPx() }

    val disc = remember(reduceMotion) { Animatable(if (reduceMotion) 1f else 0f) }
    val photoArrivals = remember(reduceMotion) { PhotoSlots.map { Animatable(if (reduceMotion) 1f else 0f) } }
    val accentArrivals = remember(reduceMotion) { AccentSpecs.map { Animatable(if (reduceMotion) 1f else 0f) } }
    val flips = remember { PhotoSlots.map { Animatable(0f) } }
    val look = remember { Animatable(0f) }
    val frontScenes = remember { mutableStateListOf(*PhotoSlots.indices.map { WelcomeScenes[it] }.toTypedArray()) }
    val backScenes = remember { mutableStateListOf(*PhotoSlots.indices.map { WelcomeScenes[it] }.toTypedArray()) }

    val bobTransition = rememberInfiniteTransition(label = "welcome_bob")
    val bobClock by
        bobTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(BOB_PERIOD_MS, easing = LinearEasing), RepeatMode.Restart),
            label = "welcome_bob_clock",
        )
    val bob = if (reduceMotion) 0f else bobClock

    LaunchedEffect(reduceMotion) {
        if (reduceMotion) return@LaunchedEffect
        launch { disc.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 260f)) }
        photoArrivals.forEachIndexed { index, arrival ->
            launch {
                delay(300L + PhotoSlots[index].arriveOrder * 60L)
                arrival.animateTo(1f, spring(dampingRatio = 0.58f, stiffness = 190f))
            }
        }
        accentArrivals.forEachIndexed { index, arrival ->
            launch {
                delay(620L + index * 45L)
                arrival.animateTo(1f, spring(dampingRatio = 0.48f, stiffness = 380f))
            }
        }
    }

    LaunchedEffect(reduceMotion) {
        if (reduceMotion) return@LaunchedEffect
        val hidden = ArrayDeque(WelcomeScenes.drop(PhotoSlots.size))
        delay(FIRST_FLIP_DELAY_MS)
        var turn = 0
        while (true) {
            val slot = FlipOrder[turn % FlipOrder.size]
            val incoming = hidden.removeFirst()
            hidden.addLast(frontScenes[slot])
            backScenes[slot] = incoming
            launch {
                look.animateTo(
                    if (PhotoSlots[slot].x < 0.5f) -1f else 1f,
                    spring(dampingRatio = 0.72f, stiffness = 240f),
                )
            }
            flips[slot].animateTo(
                180f,
                spring(dampingRatio = 0.55f, stiffness = 150f, visibilityThreshold = 0.5f),
            )
            frontScenes[slot] = incoming
            flips[slot].snapTo(0f)
            launch {
                delay(420)
                look.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 180f))
            }
            delay(FLIP_GAP_MS)
            turn++
        }
    }

    val discColor = if (colors.isDark) colors.accent.copy(alpha = 0.17f) else colors.accentInk
    Box(
        modifier =
            Modifier
                .offset(x = side * (0.5f - DISC_SIZE / 2), y = side * (DISC_CENTER_Y - DISC_SIZE / 2))
                .size(side * DISC_SIZE)
                .graphicsLayer {
                    val p = disc.value
                    val scale = 0.55f + 0.45f * p
                    scaleX = scale
                    scaleY = scale
                    alpha = (p * 2.5f).coerceIn(0f, 1f)
                }.clip(CircleShape)
                .background(discColor),
    )

    val stickerEdge = if (colors.isDark) StickerEdgeDark else StickerEdgeLight
    PhotoSlots.forEachIndexed { index, slot ->
        val stickerSize = side * slot.size
        val arrival = photoArrivals[index]
        Box(
            modifier =
                Modifier
                    .offset(x = side * slot.x - stickerSize / 2, y = side * slot.y - stickerSize / 2)
                    .size(stickerSize)
                    .graphicsLayer {
                        val p = arrival.value
                        val wave = 2f * PI.toFloat() * (bob + slot.phase)
                        translationX = (0.5f - slot.x) * sidePx * (1f - p)
                        translationY = (DISC_CENTER_Y - slot.y) * sidePx * (1f - p) + sin(wave) * bobTravelPx
                        val scale = 0.2f + 0.8f * p
                        scaleX = scale
                        scaleY = scale
                        rotationZ = slot.tilt - slot.tilt * 3f * (1f - p) + sin(wave * 0.7f) * 1.4f
                        alpha = (p * 3f).coerceIn(0f, 1f)
                    },
        ) {
            PhotoSticker(
                front = frontScenes[index],
                back = backScenes[index],
                flipDegrees = flips[index].value,
                round = slot.round,
                edge = stickerEdge,
            )
        }
    }

    AccentSpecs.forEachIndexed { index, spec ->
        if (spec.kind != AccentKind.Food) return@forEachIndexed
        AccentItem(spec, side, sidePx, bob, bobTravelPx, accentArrivals[index].value) {
            Image(
                painter = painterResource(spec.drawable),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .shadow(6.dp, CircleShape, ambientColor = MascotInk, spotColor = MascotInk)
                        .clip(CircleShape)
                        .background(stickerEdge)
                        .padding(2.dp)
                        .clip(CircleShape),
            )
        }
    }

    val mascotSize = side * MASCOT_SIZE
    // The figure's visual centre sits 2.5 % above its box centre, so nudge the box down.
    val mascotTop = side * DISC_CENTER_Y - mascotSize / 2 + mascotSize * 0.025f
    VaiinillaMascot(
        modifier =
            Modifier
                .offset(x = side * 0.5f - mascotSize / 2, y = mascotTop)
                .size(mascotSize),
        delayMs = 140,
        paperColor = MascotPaper,
        accentColor = colors.accent,
        inkColor = MascotInk,
        reduceMotion = reduceMotion,
        wave = true,
        look = look.value,
    )

    AccentSpecs.forEachIndexed { index, spec ->
        if (spec.kind == AccentKind.Food) return@forEachIndexed
        AccentItem(spec, side, sidePx, bob, bobTravelPx, accentArrivals[index].value) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                when (spec.kind) {
                    AccentKind.Spark ->
                        drawVaiinillaGlyph(
                            VaiinillaGlyphKind.Spark,
                            color = colors.accent,
                            accent = if (spec.tilt == 0f) colors.accent else colors.yolk,
                        )
                    AccentKind.Dashes -> drawEmotionDashes(colors.accent)
                    AccentKind.Flower -> drawVanillaFlower(colors.yolk, MascotPaper)
                    AccentKind.Food -> Unit
                }
            }
        }
    }
}

@Composable
private fun AccentItem(
    spec: AccentSpec,
    side: Dp,
    sidePx: Float,
    bob: Float,
    bobTravelPx: Float,
    arrival: Float,
    content: @Composable () -> Unit,
) {
    val itemSize = side * spec.size
    Box(
        modifier =
            Modifier
                .offset(x = side * spec.x - itemSize / 2, y = side * spec.y - itemSize / 2)
                .size(itemSize)
                .graphicsLayer {
                    val wave = 2f * PI.toFloat() * (bob + spec.phase)
                    translationY = cos(wave) * bobTravelPx * 1.3f + (1f - arrival) * sidePx * 0.04f
                    val scale = arrival.coerceAtLeast(0f)
                    scaleX = scale
                    scaleY = scale
                    rotationZ = spec.tilt + sin(wave) * 6f
                    alpha = (arrival * 3f).coerceIn(0f, 1f)
                },
    ) {
        content()
    }
}

/** One photo sticker; flipping past 90° shows the next scene, un-mirrored. */
@Composable
private fun PhotoSticker(
    @DrawableRes front: Int,
    @DrawableRes back: Int,
    flipDegrees: Float,
    round: Boolean,
    edge: Color,
) {
    val showBack = flipDegrees > 90f
    val outer: Shape = if (round) CircleShape else RoundedCornerShape(percent = 24)
    val inner: Shape = if (round) CircleShape else RoundedCornerShape(percent = 21)
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = flipDegrees
                    cameraDistance = 14f * density
                    scaleX = if (showBack) -1f else 1f
                }.shadow(10.dp, outer, ambientColor = MascotInk, spotColor = MascotInk)
                .clip(outer)
                .background(edge)
                .padding(3.dp)
                .clip(inner),
    ) {
        Image(
            painter = painterResource(if (showBack) back else front),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Vaini's "emotion mark": three short lime dashes fanning out to the left. */
private fun DrawScope.drawEmotionDashes(color: Color) {
    val w = size.minDimension
    val stroke = w * 0.13f
    val anchor = Offset(w * 1.05f, w * 0.5f)
    listOf(-40f, 0f, 40f).forEach { degrees ->
        val radians = Math.toRadians(180.0 + degrees).toFloat()
        val start = Offset(anchor.x + cos(radians) * w * 0.52f, anchor.y + sin(radians) * w * 0.52f)
        val end = Offset(anchor.x + cos(radians) * w * 0.92f, anchor.y + sin(radians) * w * 0.92f)
        drawLine(color, start, end, strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

/** The little yellow vanilla flower from Café Vaini. */
private fun DrawScope.drawVanillaFlower(
    petal: Color,
    heart: Color,
) {
    val w = size.minDimension
    val center = Offset(w / 2f, w / 2f)
    repeat(5) { i ->
        val radians = (2.0 * PI * i / 5 - PI / 2).toFloat()
        drawCircle(
            petal,
            radius = w * 0.2f,
            center = Offset(center.x + cos(radians) * w * 0.24f, center.y + sin(radians) * w * 0.24f),
        )
    }
    drawCircle(heart, radius = w * 0.13f, center = center)
}

/** Shown on the launch route only if the session check takes a moment: Vaini waits, dots breathe. */
@Composable
internal fun WelcomeBootStage(
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(350)
        reveal.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 220f))
    }
    val dots = rememberInfiniteTransition(label = "boot_dots")
    val dotClock by
        dots.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
            label = "boot_dots_clock",
        )
    Box(
        modifier = modifier.fillMaxSize().background(colors.paper),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier.graphicsLayer {
                    alpha = reveal.value
                    val scale = 0.94f + 0.06f * reveal.value
                    scaleX = scale
                    scaleY = scale
                },
        ) {
            Box(
                modifier =
                    Modifier
                        .size(132.dp)
                        .clip(CircleShape)
                        .background(if (colors.isDark) colors.accent.copy(alpha = 0.10f) else colors.accentInk),
                contentAlignment = Alignment.Center,
            ) {
                VaiinillaMascot(
                    modifier = Modifier.size(104.dp),
                    paperColor = MascotPaper,
                    accentColor = colors.accent,
                    inkColor = MascotInk,
                    reduceMotion = reduceMotion,
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                repeat(3) { i ->
                    val wave = if (reduceMotion) 0.5f else (sin(2f * PI.toFloat() * (dotClock - i * 0.16f)) + 1f) / 2f
                    Box(
                        modifier =
                            Modifier
                                .size(7.dp)
                                .graphicsLayer {
                                    translationY = -wave * 5.dp.toPx()
                                    alpha = 0.45f + 0.55f * wave
                                }.clip(CircleShape)
                                .background(colors.accent),
                    )
                }
            }
        }
    }
}
