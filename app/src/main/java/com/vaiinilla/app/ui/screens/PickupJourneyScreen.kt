package com.vaiinilla.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.common.BitMatrix
import com.vaiinilla.app.core.qr.QrCodeEncoder
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.order.MOSAIC_FLIP_LENGTH
import com.vaiinilla.app.ui.order.PickupStep
import com.vaiinilla.app.ui.order.mosaicCellStart
import com.vaiinilla.app.ui.order.pickupCodeHint
import com.vaiinilla.app.ui.order.pickupHint
import com.vaiinilla.app.ui.order.pickupStep
import com.vaiinilla.app.ui.order.pickupTitle
import com.vaiinilla.app.ui.order.shouldAutoRevealPickupCode
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private val SkyTop = Color(0xFFEAE3D0)
private val SkyBottom = Color(0xFFF4EFE2)
private val Facade = Color(0xFFE6D3AE)
private val FacadeShade = Color(0xFFD5BE92)
private val FacadeLight = Color(0xFFF3E9D0)
private val WindowFrame = Color(0xFFF8F2E2)
private val WindowGlass = Color(0xFF7D96A0)
private val WindowGlassLight = Color(0xFFA3B8BE)
private val Awning = Color(0xFFB7D464)
private val AwningDark = Color(0xFF9DBB4C)
private val SignInk = Color(0xFF1F2716)
private val SignCream = Color(0xFFF3EFE4)
private val ShopLight = Color(0xFFFFF2D3)
private val ShopLightDeep = Color(0xFFF2D9A2)
private val ShopShelf = Color(0xFFD9B98A)
private val DoorGreen = Color(0xFF2E3B1E)
private val Sidewalk = Color(0xFFDCD3C1)
private val Street = Color(0xFFC9C1B0)
private val Leaf = Color(0xFF8DAF48)
private val LeafDark = Color(0xFF6E9131)
private val Pot = Color(0xFFB98A5E)

private val StagePaper = Color(0xFFF4F1E8)
private val Ink = Color(0xFF151513)
private val Muted = Color(0xFF6F6B62)
private val Lime = Color(0xFFB7D464)
private val LimeInk = Color(0xFF172008)
private val GlassTint = Color(0xBDFBF8F1)
private val GlassEdge = Color(0xA6FFFFFF)
private val GlassLine = Color(0x1F151513)
private val QrDark = Color(0xFF151513)
private val QrLight = Color(0xFFFBF8F0)
private val VainiCream = Color(0xFFF6EFDD)
private val VainiLime = Color(0xFFA8D52C)
private val VainiInk = Color(0xFF1A1414)

// Proporciones de la fachada, en fracción del alto de la pantalla.
private const val CORNICE_Y = 0.285f
private const val SIGN_TOP_Y = 0.298f
private const val SIGN_BOTTOM_Y = 0.336f
private const val AWNING_BOTTOM_Y = 0.372f
private const val SHOP_BOTTOM_Y = 0.578f
private const val SIDEWALK_BOTTOM_Y = 0.625f

// La cámara vuela hacia la puerta de la tienda.
private const val FOCUS_Y = 0.47f
private const val ZOOM_MAX = 3.4f
private const val CAMERA_FROM = 0.04f
private const val CAMERA_TO = 0.5f
private const val HANDOFF = 0.4f
private const val SAMPLE_SCALE = 0.25f

private fun segment(
    p: Float,
    from: Float,
    to: Float,
): Float = ((p - from) / (to - from)).coerceIn(0f, 1f)

private fun eased(
    p: Float,
    from: Float,
    to: Float,
): Float = FastOutSlowInEasing.transform(segment(p, from, to))

private fun lerpF(
    a: Float,
    b: Float,
    t: Float,
): Float = a + (b - a) * t

/** Movimiento de Vaini en la escena; la escena en sí es fija. */
private data class SceneMotion(
    val blink: Float = 1f,
    val wave: Float = 0f,
    val happy: Boolean = false,
)

/** Cámara: `pantalla = anchor + (puntoEscena - focus) * zoom`. */
private data class Camera(
    val zoom: Float,
    val anchor: Offset,
    val focus: Offset,
) {
    fun toScreen(scene: Offset): Offset = anchor + (scene - focus) * zoom

    fun toScene(screen: Offset): Offset = focus + (screen - anchor) / zoom
}

private fun cameraAt(
    p: Float,
    width: Float,
    height: Float,
): Camera {
    val t = eased(p, CAMERA_FROM, CAMERA_TO)
    val focus = Offset(width * 0.5f, height * FOCUS_Y)
    val target = Offset(width * 0.5f, height * 0.42f)
    return Camera(
        zoom = lerpF(1f, ZOOM_MAX, t),
        anchor = focus + (target - focus) * t,
        focus = focus,
    )
}

/** Colores de la escena bajo cada bloque del mosaico, tal como se ve al pasar la cámara por [HANDOFF]. */
private class Mosaic(
    val matrix: BitMatrix,
    val sceneCenter: Offset,
    val sceneSide: Float,
    val sampled: Array<Color>,
)

private fun buildMosaic(
    matrix: BitMatrix,
    width: Float,
    height: Float,
    density: Density,
    textMeasurer: TextMeasurer,
): Mosaic {
    val camera = cameraAt(HANDOFF, width, height)
    val screenSide = width * 1.18f
    val sceneCenter = camera.toScene(Offset(width * 0.5f, height * 0.42f))
    val sceneSide = screenSide / camera.zoom
    val bitmap = ImageBitmap((width * SAMPLE_SCALE).roundToInt().coerceAtLeast(1), (height * SAMPLE_SCALE).roundToInt().coerceAtLeast(1))
    CanvasDrawScope().draw(
        density,
        LayoutDirection.Ltr,
        androidx.compose.ui.graphics.Canvas(bitmap),
        Size(bitmap.width.toFloat(), bitmap.height.toFloat()),
    ) {
        withTransform({ scale(SAMPLE_SCALE, SAMPLE_SCALE, pivot = Offset.Zero) }) {
            drawStorefront(Size(width, height), textMeasurer, SceneMotion())
        }
    }
    val pixels: PixelMap = bitmap.toPixelMap()
    val n = matrix.width
    val sampled =
        Array(n * n) { index ->
            val col = index % n
            val row = index / n
            val point =
                Offset(
                    sceneCenter.x + ((col + 0.5f) / n - 0.5f) * sceneSide,
                    sceneCenter.y + ((row + 0.5f) / n - 0.5f) * sceneSide,
                )
            val x = (point.x * SAMPLE_SCALE).roundToInt()
            val y = (point.y * SAMPLE_SCALE).roundToInt()
            if (x in 0 until pixels.width && y in 0 until pixels.height) pixels[x, y] else SkyBottom
        }
    return Mosaic(matrix, sceneCenter, sceneSide, sampled)
}

private fun DrawScope.drawStorefront(
    stage: Size,
    textMeasurer: TextMeasurer,
    motion: SceneMotion,
) {
    val w = stage.width
    val h = stage.height
    val hair = 1.dp.toPx()
    drawRect(Brush.verticalGradient(listOf(SkyTop, SkyBottom), endY = h * 0.6f), size = stage)

    // Fachada de los pisos de arriba, con hiladas finas.
    drawRect(Facade, size = Size(w, h * SHOP_BOTTOM_Y))
    var course = h * 0.014f
    while (course < h * CORNICE_Y) {
        drawLine(FacadeShade.copy(alpha = 0.32f), Offset(0f, course), Offset(w, course), strokeWidth = hair)
        course += h * 0.0185f
    }
    for (x in listOf(0.035f, 0.965f)) {
        drawRect(FacadeLight, topLeft = Offset(w * x - w * 0.018f, 0f), size = Size(w * 0.036f, h * CORNICE_Y))
    }

    // Ventanas: dos pisos de tres.
    for (row in 0..1) {
        val top = h * (0.042f + row * 0.122f)
        val wh = h * 0.094f
        for (col in 0..2) {
            val cx = w * (0.2f + col * 0.3f)
            val ww = w * 0.17f
            val left = cx - ww / 2f
            drawRect(FacadeShade, topLeft = Offset(left - hair * 3, top - hair * 3), size = Size(ww + hair * 6, wh + hair * 6))
            drawRect(WindowFrame, topLeft = Offset(left, top), size = Size(ww, wh))
            val inset = hair * 3.5f
            val glassTop = top + inset
            val glassSize = Size(ww - inset * 2, wh - inset * 2)
            drawRect(
                Brush.linearGradient(
                    listOf(WindowGlass, WindowGlassLight, WindowGlass),
                    start = Offset(left, glassTop),
                    end = Offset(left + ww, glassTop + wh),
                ),
                topLeft = Offset(left + inset, glassTop),
                size = glassSize,
            )
            drawLine(WindowFrame, Offset(cx, glassTop), Offset(cx, glassTop + glassSize.height), strokeWidth = hair * 3)
            val mid = glassTop + glassSize.height * 0.42f
            drawLine(WindowFrame, Offset(left + inset, mid), Offset(left + ww - inset, mid), strokeWidth = hair * 3)
            drawRect(FacadeLight, topLeft = Offset(left - hair * 6, top + wh + hair * 2), size = Size(ww + hair * 12, hair * 6))
        }
    }

    // Cornisa, letrero y toldo.
    drawRect(FacadeLight, topLeft = Offset(0f, h * (CORNICE_Y - 0.012f)), size = Size(w, h * 0.016f))
    drawLine(FacadeShade, Offset(0f, h * (CORNICE_Y + 0.004f)), Offset(w, h * (CORNICE_Y + 0.004f)), strokeWidth = hair * 2)
    val signLeft = w * 0.07f
    val signTop = h * SIGN_TOP_Y
    val signHeight = h * (SIGN_BOTTOM_Y - SIGN_TOP_Y)
    drawRoundRect(SignInk, topLeft = Offset(signLeft, signTop), size = Size(w * 0.86f, signHeight), cornerRadius = CornerRadius(hair * 3))
    val signStyle =
        TextStyle(
            color = SignCream,
            fontSize = (signHeight * 0.46f / density / fontScale).sp,
            fontFamily = VaiinillaSerif,
            fontWeight = FontWeight.Bold,
            letterSpacing = (signHeight * 0.08f / density / fontScale).sp,
        )
    val sign = textMeasurer.measure("VAIINILLA", signStyle)
    drawText(sign, topLeft = Offset((w - sign.size.width) / 2f, signTop + (signHeight - sign.size.height) / 2f))
    for (dot in listOf(0.2f, 0.8f)) {
        drawCircle(Lime, radius = hair * 3, center = Offset(w * dot, signTop + signHeight / 2f))
    }
    val awningTop = h * SIGN_BOTTOM_Y
    val awningBottom = h * AWNING_BOTTOM_Y
    val awningLeft = w * 0.05f
    val awningWidth = w * 0.9f
    val stripes = 11
    val stripe = awningWidth / stripes
    for (i in 0 until stripes) {
        drawRect(
            if (i % 2 == 0) Awning else AwningDark,
            topLeft = Offset(awningLeft + i * stripe, awningTop),
            size = Size(stripe + 0.5f, awningBottom - awningTop),
        )
        drawCircle(
            if (i % 2 == 0) Awning else AwningDark,
            radius = stripe / 2f,
            center = Offset(awningLeft + i * stripe + stripe / 2f, awningBottom),
        )
    }
    drawRect(Color(0x1A000000), topLeft = Offset(awningLeft, awningTop), size = Size(awningWidth, hair * 2))

    // Planta baja: dos vitrinas con luz cálida y la puerta abierta.
    val shopTop = awningBottom + h * 0.016f
    val shopBottom = h * SHOP_BOTTOM_Y
    val plinth = h * 0.03f
    for ((from, to) in listOf(0.08f to 0.39f, 0.61f to 0.92f)) {
        val left = w * from
        val width = w * (to - from)
        val glassBottom = shopBottom - plinth
        drawRect(DoorGreen, topLeft = Offset(left - hair * 3, shopTop - hair * 3), size = Size(width + hair * 6, glassBottom - shopTop + hair * 6))
        drawRect(
            Brush.verticalGradient(listOf(ShopLight, ShopLightDeep), startY = shopTop, endY = glassBottom),
            topLeft = Offset(left, shopTop),
            size = Size(width, glassBottom - shopTop),
        )
        for (lamp in listOf(0.3f, 0.7f)) {
            val lx = left + width * lamp
            drawLine(SignInk, Offset(lx, shopTop), Offset(lx, shopTop + h * 0.03f), strokeWidth = hair)
            drawArc(SignInk, 180f, 180f, true, topLeft = Offset(lx - hair * 9, shopTop + h * 0.026f), size = Size(hair * 18, hair * 12))
            drawCircle(Color(0x66FFE7A8), radius = hair * 16, center = Offset(lx, shopTop + h * 0.04f))
        }
        val shelfY = shopTop + (glassBottom - shopTop) * 0.62f
        drawRect(ShopShelf, topLeft = Offset(left, shelfY), size = Size(width, hair * 4))
        for (cup in 0..3) {
            val cx = left + width * (0.18f + cup * 0.21f)
            drawRoundRect(
                if (cup % 2 == 0) VainiCream else Lime,
                topLeft = Offset(cx - hair * 5, shelfY - hair * 12),
                size = Size(hair * 10, hair * 12),
                cornerRadius = CornerRadius(hair * 2),
            )
        }
        drawRect(DoorGreen.copy(alpha = 0.18f), topLeft = Offset(left, glassBottom - hair * 2), size = Size(width, hair * 2))
        drawRect(FacadeShade, topLeft = Offset(left - hair * 3, glassBottom + hair * 3), size = Size(width + hair * 6, plinth - hair * 3))
    }
    val doorLeft = w * 0.415f
    val doorWidth = w * 0.17f
    drawRect(DoorGreen, topLeft = Offset(doorLeft - hair * 4, shopTop - hair * 4), size = Size(doorWidth + hair * 8, shopBottom - shopTop + hair * 4))
    drawRect(
        Brush.verticalGradient(listOf(ShopLightDeep, ShopLight), startY = shopTop, endY = shopBottom),
        topLeft = Offset(doorLeft, shopTop),
        size = Size(doorWidth, shopBottom - shopTop),
    )
    val vainiBox = doorWidth * 0.92f
    withTransform({ translate(doorLeft + (doorWidth - vainiBox) / 2f, shopBottom - vainiBox * 0.8f) }) {
        drawVainiStanding(
            boxPx = vainiBox,
            paperColor = VainiCream,
            accentColor = VainiLime,
            inkColor = VainiInk,
            blink = motion.blink,
            waveDegrees = motion.wave,
            happy = motion.happy,
        )
    }

    // Banqueta, macetas y calle.
    drawRect(Sidewalk, topLeft = Offset(0f, shopBottom), size = Size(w, h * (SIDEWALK_BOTTOM_Y - SHOP_BOTTOM_Y)))
    var slab = 0f
    while (slab < w) {
        drawLine(Street.copy(alpha = 0.5f), Offset(slab, shopBottom), Offset(slab, h * SIDEWALK_BOTTOM_Y), strokeWidth = hair)
        slab += w * 0.125f
    }
    drawRect(Street, topLeft = Offset(0f, h * SIDEWALK_BOTTOM_Y), size = Size(w, h * (1f - SIDEWALK_BOTTOM_Y)))
    drawRect(Color(0x14000000), topLeft = Offset(0f, h * SIDEWALK_BOTTOM_Y), size = Size(w, hair * 3))
    for (x in listOf(0.045f, 0.955f)) {
        val cx = w * x
        val potTop = shopBottom - h * 0.012f
        drawCircle(LeafDark, radius = w * 0.045f, center = Offset(cx, potTop - w * 0.035f))
        drawCircle(Leaf, radius = w * 0.038f, center = Offset(cx - w * 0.012f, potTop - w * 0.05f))
        drawRoundRect(Pot, topLeft = Offset(cx - w * 0.03f, potTop), size = Size(w * 0.06f, h * 0.03f), cornerRadius = CornerRadius(hair * 3))
    }
}

/** Pantalla de recogida: la tienda con el estado del pedido y, cuando toca, el QR que sale de la fachada. */
@Composable
internal fun PickupJourneyScreen(
    order: OrderDetail,
    qrPayload: String?,
    onBack: () -> Unit,
    onViewTracking: () -> Unit,
) {
    val reduceMotion = reducedMotion()
    val haptics = LocalHapticFeedback.current
    val textMeasurer = rememberTextMeasurer()
    val matrix = remember(qrPayload) { qrPayload?.let { runCatching { QrCodeEncoder.encode(it) }.getOrNull() } }
    val hasCode = matrix != null
    val state = order.summary.state
    val step = pickupStep(state)
    val ready = step == PickupStep.READY

    var revealed by rememberSaveable(order.summary.id) { mutableStateOf(false) }
    val reveal = remember(order.summary.id) { Animatable(if (revealed) 1f else 0f) }
    var autoRevealedState by rememberSaveable(order.summary.id) { mutableStateOf<String?>(null) }
    val autoReveal = shouldAutoRevealPickupCode(order, hasCode)
    LaunchedEffect(autoReveal, state) {
        if (!autoReveal || autoRevealedState == state.wireValue) return@LaunchedEffect
        // Deja ver a Vaini saludar antes de volar hacia el código.
        delay(if (reduceMotion) 300 else 1500)
        autoRevealedState = state.wireValue
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        revealed = true
    }
    LaunchedEffect(revealed) {
        val target = if (revealed) 1f else 0f
        if (reduceMotion) reveal.snapTo(target) else reveal.animateTo(target, spring(dampingRatio = 1f, stiffness = 14f))
    }
    BackHandler(enabled = revealed) { revealed = false }
    ScreenBrightnessBoost(active = revealed)

    val idle = rememberInfiniteTransition(label = "pickup_vaini")
    val blink by idle.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                keyframes {
                    durationMillis = 3800
                    1f at 3200
                    0.06f at 3280
                    1f at 3390
                },
                RepeatMode.Restart,
            ),
        label = "blink",
    )
    val wave by idle.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec =
            infiniteRepeatable(
                keyframes {
                    durationMillis = 2200
                    0f at 0
                    -112f at 380
                    -128f at 620
                    -98f at 860
                    -128f at 1100
                    -112f at 1320
                    0f at 1750
                },
                RepeatMode.Restart,
            ),
        label = "wave",
    )
    val motion =
        SceneMotion(
            blink = if (reduceMotion) 1f else blink,
            wave = if (ready && !reduceMotion) wave else 0f,
            happy = ready,
        )

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .background(StagePaper),
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val stage = Size(widthPx, heightPx)
        val mosaic =
            remember(matrix, widthPx, heightPx) {
                matrix?.let { buildMosaic(it, widthPx, heightPx, density, textMeasurer) }
            }
        val p = reveal.value
        val camera = cameraAt(p, widthPx, heightPx)
        val sceneAlpha = 1f - segment(p, 0.46f, 0.62f)
        val speedBlur = if (p in 0.03f..0.5f) min(10f, abs(reveal.velocity) * 9f) else 0f

        if (sceneAlpha > 0.001f) {
            Canvas(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0f, 0f)
                            scaleX = camera.zoom
                            scaleY = camera.zoom
                            translationX = camera.anchor.x - camera.focus.x * camera.zoom
                            translationY = camera.anchor.y - camera.focus.y * camera.zoom
                            alpha = sceneAlpha
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && speedBlur > 0.5f) {
                                renderEffect = BlurEffect(speedBlur, speedBlur, TileMode.Clamp)
                            }
                        },
            ) {
                drawStorefront(stage, textMeasurer, motion)
            }
        }

        if (mosaic != null && p > 0.3f) {
            Canvas(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = "Código QR del pedido ${order.summary.folio}" },
            ) {
                drawMosaic(mosaic, p, stage)
            }
        }

        StatusSheet(
            order = order,
            step = step,
            hasCode = hasCode,
            hidden = eased(p, 0f, 0.2f),
            backdrop = { drawStorefront(stage, textMeasurer, motion) },
            onShowCode = { revealed = true },
            onViewTracking = onViewTracking,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        val pickupIn = eased(p, 0.8f, 1f)
        if (pickupIn > 0.001f) {
            PickupCodeSheet(
                order = order,
                shown = pickupIn,
                onViewTracking = onViewTracking,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            GlassCircleButton(
                closing = segment(p, 0.4f, 0.7f),
                onClick = { if (revealed) revealed = false else onBack() },
            )
        }
    }
}

private fun DrawScope.drawMosaic(
    mosaic: Mosaic,
    p: Float,
    stage: Size,
) {
    val n = mosaic.matrix.width
    val camera = cameraAt(p, stage.width, stage.height)
    val cameraCenter = camera.toScreen(mosaic.sceneCenter)
    val cameraSide = mosaic.sceneSide * camera.zoom
    val finalSide = min(stage.width * 0.74f, stage.height * 0.4f)
    val finalCenter = Offset(stage.width * 0.5f, stage.height * 0.38f)
    val settle = eased(p, 0.5f, 0.88f)
    val side = lerpF(cameraSide, finalSide, settle)
    val center = cameraCenter + (finalCenter - cameraCenter) * settle
    val rect = Rect(center.x - side / 2f, center.y - side / 2f, center.x + side / 2f, center.y + side / 2f)
    val cell = side / n
    val gridAlpha = eased(p, 0.3f, 0.44f)

    val cardAlpha = eased(p, 0.6f, 0.86f)
    if (cardAlpha > 0f) {
        val pad = cell * 1.6f
        val radius = CornerRadius(24.dp.toPx())
        drawRoundRect(
            Color.Black.copy(alpha = 0.06f * cardAlpha),
            topLeft = Offset(rect.left - pad, rect.top - pad + 10.dp.toPx()),
            size = Size(side + pad * 2, side + pad * 2),
            cornerRadius = radius,
        )
        drawRoundRect(
            QrLight.copy(alpha = cardAlpha),
            topLeft = Offset(rect.left - pad, rect.top - pad),
            size = Size(side + pad * 2, side + pad * 2),
            cornerRadius = radius,
        )
    }
    // Las juntas entre bloques se ven mientras es mosaico y se cierran antes del final para que escanee.
    val gap = cell * 0.08f * (1f - segment(p, 0.78f, 0.9f))
    for (row in 0 until n) {
        for (col in 0 until n) {
            val start = mosaicCellStart(col, row, n)
            val flip = eased(p, start, start + MOSAIC_FLIP_LENGTH)
            val target = if (mosaic.matrix[col, row]) QrDark else QrLight
            val color = lerp(mosaic.sampled[row * n + col], target, flip)
            val bump = 1f - 0.22f * sin(PI.toFloat() * flip)
            val size = (cell - gap) * bump
            val cx = rect.left + (col + 0.5f) * cell
            val cy = rect.top + (row + 0.5f) * cell
            drawRect(
                color.copy(alpha = color.alpha * gridAlpha),
                topLeft = Offset(cx - size / 2f, cy - size / 2f),
                size = Size(size + 0.5f, size + 0.5f),
            )
        }
    }
}

@Composable
private fun GlassCircleButton(
    closing: Float,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(GlassTint)
                .border(1.dp, GlassEdge, CircleShape)
                .physicalPress(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = if (closing < 0.5f) "Regresar" else null,
            tint = Ink,
            modifier =
                Modifier.graphicsLayer {
                    alpha = 1f - closing
                    rotationZ = closing * 90f
                    scaleX = 1f - closing * 0.4f
                    scaleY = 1f - closing * 0.4f
                },
        )
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = if (closing >= 0.5f) "Cerrar código" else null,
            tint = Ink,
            modifier =
                Modifier.graphicsLayer {
                    alpha = closing
                    rotationZ = (1f - closing) * -90f
                    scaleX = 0.6f + closing * 0.4f
                    scaleY = 0.6f + closing * 0.4f
                },
        )
    }
}

/** Vidrio esmerilado: la misma escena, desenfocada, detrás de un velo claro. Sin blur (Android < 12) queda un velo opaco. */
@Composable
private fun GlassPanel(
    modifier: Modifier = Modifier,
    backdrop: (DrawScope.() -> Unit)?,
    content: @Composable () -> Unit,
) {
    var top by remember { mutableFloatStateOf(0f) }
    val shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    val frosted = backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .onGloballyPositioned { top = it.positionInRoot().y }
                .clip(shape),
    ) {
        if (frosted && backdrop != null) {
            Canvas(
                modifier =
                    Modifier
                        .matchParentSize()
                        .blur(26.dp),
            ) {
                withTransform({ translate(0f, -top) }) { backdrop() }
            }
        }
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .background(if (frosted) GlassTint else QrLight)
                    .border(1.dp, GlassEdge, shape),
        )
        content()
    }
}

@Composable
private fun StatusSheet(
    order: OrderDetail,
    step: PickupStep?,
    hasCode: Boolean,
    hidden: Float,
    backdrop: DrawScope.() -> Unit,
    onShowCode: () -> Unit,
    onViewTracking: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var height by remember { mutableFloatStateOf(0f) }
    val entrance = remember { Animatable(0f) }
    val reduceMotion = reducedMotion()
    LaunchedEffect(Unit) {
        if (reduceMotion) entrance.snapTo(1f) else entrance.animateTo(1f, spring(dampingRatio = 0.86f, stiffness = 180f))
    }
    val progress = remember { Animatable((step?.ordinal ?: -1).toFloat() + 1f) }
    LaunchedEffect(step) {
        val target = (step?.ordinal ?: -1).toFloat() + 1f
        if (reduceMotion) progress.snapTo(target) else progress.animateTo(target, spring(dampingRatio = 0.9f, stiffness = 120f))
    }
    GlassPanel(
        backdrop = backdrop,
        modifier =
            modifier
                .onSizeChanged { height = it.height.toFloat() }
                .graphicsLayer {
                    val away = (1f - entrance.value) * 0.35f + hidden
                    translationY = height * away.coerceIn(0f, 1.1f)
                    alpha = (1f - hidden * 1.4f).coerceIn(0f, 1f)
                },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 16.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 36.dp, height = 4.dp)
                    .background(GlassLine, CircleShape),
            )
            Spacer(Modifier.height(12.dp))
            Text("Pedido #${order.summary.folio}", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            AnimatedContent(
                targetState = pickupTitle(order),
                transitionSpec = {
                    (fadeIn(spring(stiffness = 180f)) + scaleIn(spring(stiffness = 180f), initialScale = 0.92f) + slideInVertically { it / 4 }) togetherWith
                        (fadeOut(spring(stiffness = 300f)) + scaleOut(targetScale = 1.04f) + slideOutVertically { -it / 4 })
                },
                label = "pickup_title",
            ) { title ->
                Text(
                    title,
                    color = Ink,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.2).sp,
                )
            }
            AnimatedContent(
                targetState = pickupHint(order),
                transitionSpec = { fadeIn(spring(stiffness = 180f)) togetherWith fadeOut(spring(stiffness = 300f)) },
                label = "pickup_hint",
            ) { hint ->
                Text(hint, color = Muted, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (step != null) {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    PickupStep.entries.forEach { item ->
                        val fill = (progress.value - item.ordinal).coerceIn(0f, 1f)
                        Column(Modifier.weight(1f)) {
                            Canvas(
                                Modifier
                                    .fillMaxWidth()
                                    .height(5.dp),
                            ) {
                                val r = CornerRadius(size.height / 2f)
                                drawRoundRect(GlassLine, cornerRadius = r)
                                if (fill > 0f) drawRoundRect(Ink, size = Size(size.width * fill, size.height), cornerRadius = r)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                item.label,
                                color = lerp(Muted, Ink, fill),
                                fontSize = 11.sp,
                                fontWeight = if (item == step) FontWeight.Black else FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            OrderSummaryCard(order)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (hasCode) {
                    PillButton("Mostrar código", primary = true, onClick = onShowCode, modifier = Modifier.weight(1f))
                }
                PillButton("Seguir pedido", primary = !hasCode, onClick = onViewTracking, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun OrderSummaryCard(order: OrderDetail) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Color(0x99FFFFFF))
                .border(1.dp, GlassEdge, shape)
                .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        val visible = order.items.take(2)
        visible.forEach { item ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${item.quantity} × ${item.productName}",
                    color = Ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(moneyLabel(item.subtotal), color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
        }
        val hidden = order.items.size - visible.size
        if (hidden > 0) Text("+$hidden más", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Box(
            Modifier
                .padding(vertical = 6.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(GlassLine),
        )
        Text(
            when (order.summary.destination) {
                OrderDestination.IN_SPACE -> "A ${order.summary.space?.name ?: "tu espacio"}"
                OrderDestination.TAKE_AWAY -> "Para llevar · recoge en barra"
            } + " · " + moneyLabel(order.summary.total),
            color = Muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PickupCodeSheet(
    order: OrderDetail,
    shown: Float,
    onViewTracking: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var height by remember { mutableFloatStateOf(0f) }
    GlassPanel(
        backdrop = null,
        modifier =
            modifier
                .onSizeChanged { height = it.height.toFloat() }
                .graphicsLayer {
                    translationY = height * (1f - shown)
                    alpha = shown
                },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "#${order.summary.folio}",
                    color = Ink,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.6).sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    order.summary.state.label,
                    color = LimeInk,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    modifier =
                        Modifier
                            .clip(CircleShape)
                            .background(Lime)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
            Text("Código de recogida", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(pickupCodeHint(order), color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text(
                order.items.joinToString(" · ") { it.productName },
                color = Muted,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LightMode, contentDescription = null, tint = Muted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Subimos el brillo mientras está abierto.", color = Muted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(14.dp))
            PillButton("Seguir pedido", primary = true, onClick = onViewTracking, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PillButton(
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier =
            modifier
                .height(50.dp)
                .clip(shape)
                .background(if (primary) Lime else Color(0x8CFFFFFF))
                .border(1.dp, if (primary) Color.Transparent else GlassLine, shape)
                .physicalPress(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (primary) LimeInk else Ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.3).sp,
        )
    }
}

/** Sube el brillo al máximo mientras el código está a la vista y lo regresa al cerrar. */
@Composable
private fun ScreenBrightnessBoost(active: Boolean) {
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(active, activity) {
        if (!active) return@DisposableEffect onDispose {}
        val window = activity.window
        val previous = window.attributes.screenBrightness
        window.attributes = window.attributes.apply { screenBrightness = 1f }
        onDispose { window.attributes = window.attributes.apply { screenBrightness = previous } }
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
