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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.common.BitMatrix
import com.vaiinilla.app.core.qr.QrCodeEncoder
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.repository.CallReason
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.domain.repository.openTableCallSpaceId
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.order.MOSAIC_FLIP_LENGTH
import com.vaiinilla.app.ui.order.PickupStep
import com.vaiinilla.app.ui.order.PurchaseCelebrationKind
import com.vaiinilla.app.ui.order.mosaicCellStart
import com.vaiinilla.app.ui.order.pickupCodeHint
import com.vaiinilla.app.ui.order.pickupCodeUseful
import com.vaiinilla.app.ui.order.pickupHint
import com.vaiinilla.app.ui.order.pickupStep
import com.vaiinilla.app.ui.order.pickupTitle
import com.vaiinilla.app.ui.order.shouldAutoRevealPickupCode
import com.vaiinilla.app.ui.theme.SeaGlassButton
import com.vaiinilla.app.ui.theme.SeaGlassTone
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private val Lime = Color(0xFFB7D464)
private val LimeInk = Color(0xFF172008)

// La cámara vuela hacia la puerta de la tienda.
private const val FOCUS_Y = 0.395f
private const val ZOOM_MAX = 3.4f
private const val CAMERA_FROM = 0.04f
private const val CAMERA_TO = 0.5f
private const val HANDOFF = 0.4f

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

private fun cameraAt(
    p: Float,
    width: Float,
    height: Float,
): SceneCamera =
    SceneCamera.flight(
        focus = Offset(width * 0.5f, height * FOCUS_Y),
        target = Offset(width * 0.5f, height * 0.42f),
        zoom = ZOOM_MAX,
        t = eased(p, CAMERA_FROM, CAMERA_TO),
    )

/** Colores de la escena bajo cada bloque del mosaico, tal como se ve al pasar la cámara por [HANDOFF]. */
private class Mosaic(
    val cells: SceneMosaic,
    val sceneCenter: Offset,
    val sceneSide: Float,
)

private fun buildMosaic(
    matrix: BitMatrix,
    width: Float,
    height: Float,
    density: Density,
    textMeasurer: TextMeasurer,
    palette: ScenePalette,
): Mosaic {
    val camera = cameraAt(HANDOFF, width, height)
    val sceneCenter = camera.toScene(Offset(width * 0.5f, height * 0.42f))
    val sceneSide = width * 1.18f / camera.zoom
    val cells =
        SceneMosaic.build(matrix, Size(width, height), sceneCenter, sceneSide, density, palette.skyBottom) { stage ->
            drawStorefront(stage, textMeasurer, palette, SceneMotion())
        }
    return Mosaic(cells, sceneCenter, sceneSide)
}

/**
 * Pantalla de recogida: la tienda con el estado del pedido y, cuando toca, el QR que sale de la fachada.
 * Si hay [celebration], abre con la tarjeta de "¡Pedido recibido!" sobre la tienda desenfocada; la tarjeta
 * cae y se vuelve la hoja de estado, sin cambiar de pantalla.
 */
@Composable
internal fun PickupJourneyScreen(
    order: OrderDetail,
    qrPayload: String?,
    onBack: () -> Unit,
    onViewTracking: () -> Unit,
    celebration: PurchaseCelebrationKind? = null,
    onCelebrationFinished: () -> Unit = {},
    deliveryRequiresQr: Boolean = true,
    showTrackingButton: Boolean = true,
    onCurrentWaiterCall: (suspend (Int) -> Result<TableCall?>)? = null,
    onCallWaiter: (suspend (Int, CallReason, String?) -> Result<TableCall>)? = null,
    onCancelWaiter: (suspend (TableCall) -> Result<TableCall>)? = null,
) {
    val palette = scenePaletteForTheme()
    CompositionLocalProvider(LocalScene provides palette) {
        PickupJourney(
            order = order,
            qrPayload = qrPayload,
            onBack = onBack,
            onViewTracking = onViewTracking.takeIf { showTrackingButton },
            celebration = celebration,
            onCelebrationFinished = onCelebrationFinished,
            deliveryRequiresQr = deliveryRequiresQr,
            onCurrentWaiterCall = onCurrentWaiterCall,
            onCallWaiter = onCallWaiter,
            onCancelWaiter = onCancelWaiter,
        )
    }
}

@Composable
private fun PickupJourney(
    order: OrderDetail,
    qrPayload: String?,
    onBack: () -> Unit,
    onViewTracking: (() -> Unit)?,
    celebration: PurchaseCelebrationKind?,
    onCelebrationFinished: () -> Unit,
    deliveryRequiresQr: Boolean,
    onCurrentWaiterCall: (suspend (Int) -> Result<TableCall?>)? = null,
    onCallWaiter: (suspend (Int, CallReason, String?) -> Result<TableCall>)? = null,
    onCancelWaiter: (suspend (TableCall) -> Result<TableCall>)? = null,
) {
    val palette = LocalScene.current
    val reduceMotion = reducedMotion()
    val haptics = LocalHapticFeedback.current
    val textMeasurer = rememberTextMeasurer()
    val matrix = remember(qrPayload) { qrPayload?.let { runCatching { QrCodeEncoder.encode(it) }.getOrNull() } }
    val hasCode = matrix != null && pickupCodeUseful(order, deliveryRequiresQr)
    val state = order.summary.state
    val step = pickupStep(state)
    val ready = step == PickupStep.READY

    // 1 = tarjeta de celebración al frente; 0 = ya es la hoja de estado.
    val intro = remember(order.summary.id) { Animatable(if (celebration != null) 1f else 0f) }
    val badge = remember(order.summary.id) { Animatable(if (celebration != null && !reduceMotion) 0f else 1f) }
    val check = remember(order.summary.id) { Animatable(if (celebration != null && !reduceMotion) 0f else 1f) }
    val latestOnCelebrationFinished by rememberUpdatedState(onCelebrationFinished)
    LaunchedEffect(order.summary.id, celebration) {
        if (celebration == null) return@LaunchedEffect
        intro.snapTo(1f)
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (!reduceMotion) {
            badge.snapTo(0f)
            check.snapTo(0f)
            launch { badge.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 380f)) }
            launch {
                delay(140)
                check.animateTo(1f, spring(dampingRatio = 1f, stiffness = 90f))
            }
        }
        delay(if (reduceMotion) 900 else 1700)
        if (reduceMotion) intro.snapTo(0f) else intro.animateTo(0f, spring(dampingRatio = 0.92f, stiffness = 55f))
        latestOnCelebrationFinished()
    }
    val celebrating = celebration != null

    var revealed by rememberSaveable(order.summary.id) { mutableStateOf(false) }
    val reveal = remember(order.summary.id) { Animatable(if (revealed) 1f else 0f) }
    var autoRevealedState by rememberSaveable(order.summary.id) { mutableStateOf<String?>(null) }
    val autoReveal = shouldAutoRevealPickupCode(order, hasCode)
    LaunchedEffect(autoReveal, state, celebrating) {
        if (celebrating || !autoReveal || autoRevealedState == state.wireValue) return@LaunchedEffect
        // Deja ver a Vaini saludar antes de volar hacia el código.
        delay(if (reduceMotion) 300 else 1500)
        autoRevealedState = state.wireValue
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        revealed = true
    }
    LaunchedEffect(revealed) {
        val target = if (revealed) 1f else 0f
        if (reduceMotion) {
            reveal.snapTo(
                target,
            )
        } else {
            reveal.animateTo(target, spring(dampingRatio = 1f, stiffness = 14f))
        }
    }
    LaunchedEffect(hasCode) { if (!hasCode) revealed = false }
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
    val cheering = ready || intro.value > 0.02f
    val motion =
        SceneMotion(
            blink = if (reduceMotion) 1f else blink,
            wave = if (cheering && !reduceMotion) wave else 0f,
            happy = cheering,
        )

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .background(palette.stage),
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val stage = Size(widthPx, heightPx)
        val mosaic =
            remember(matrix, widthPx, heightPx, palette) {
                matrix?.let { buildMosaic(it, widthPx, heightPx, density, textMeasurer, palette) }
            }
        val p = reveal.value
        val camera = cameraAt(p, widthPx, heightPx)
        val sceneAlpha = 1f - segment(p, 0.46f, 0.62f)
        val speedBlur = if (p in 0.03f..0.5f) min(10f, abs(reveal.velocity) * 9f) else 0f
        val introBlur = if (reduceMotion) 0f else intro.value * 18f
        val sceneBlur = maxOf(speedBlur, introBlur)

        if (sceneAlpha > 0.001f) {
            Canvas(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0f, 0f)
                            scaleX = camera.zoom
                            scaleY = camera.zoom
                            translationX = camera.translation.x
                            translationY = camera.translation.y
                            alpha = sceneAlpha
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && sceneBlur > 0.5f) {
                                renderEffect = BlurEffect(sceneBlur, sceneBlur, TileMode.Clamp)
                            }
                        },
            ) {
                drawStorefront(stage, textMeasurer, palette, motion)
            }
        }

        if (mosaic != null && hasCode && p > 0.3f) {
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
            hidden = maxOf(eased(p, 0f, 0.2f), intro.value),
            backdrop = { drawStorefront(stage, textMeasurer, palette, motion) },
            onShowCode = { revealed = true },
            onViewTracking = onViewTracking,
            onCurrentWaiterCall = onCurrentWaiterCall,
            onCallWaiter = onCallWaiter,
            onCancelWaiter = onCancelWaiter,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        if (intro.value > 0.001f && celebration != null) {
            CelebrationCard(
                order = order,
                kind = celebration,
                shown = intro.value,
                badge = badge.value,
                check = check.value,
                dropPx = heightPx * 0.32f,
                backdrop = { drawStorefront(stage, textMeasurer, palette, motion) },
                modifier = Modifier.align(Alignment.Center),
            )
        }

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

/**
 * La confirmación de compra, en vidrio sobre la tienda. Al irse ([shown] de 1 a 0) cae hacia la hoja de
 * estado mientras la hoja sube, así se lee como la misma tarjeta que se acomoda abajo.
 */
@Composable
private fun CelebrationCard(
    order: OrderDetail,
    kind: PurchaseCelebrationKind,
    shown: Float,
    badge: Float,
    check: Float,
    dropPx: Float,
    backdrop: DrawScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalScene.current
    val paid = kind == PurchaseCelebrationKind.PAYMENT_CONFIRMED
    val title = if (paid) "¡Compra confirmada!" else "¡Pedido recibido!"
    val message =
        if (paid) {
            "Tu pago fue autorizado y tu pedido ya está en marcha."
        } else {
            "Paga en Caja para continuar con tu pedido."
        }
    val leaving = 1f - shown
    GlassPanel(
        backdrop = backdrop,
        shape = RoundedCornerShape(32.dp),
        modifier =
            modifier
                .padding(horizontal = 20.dp)
                .graphicsLayer {
                    translationY = dropPx * leaving * leaving
                    val scale = 1f - 0.08f * leaving
                    scaleX = scale
                    scaleY = scale
                    alpha = (shown * 1.6f).coerceIn(0f, 1f)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && leaving > 0.05f) {
                        val blur = leaving * 10.dp.toPx()
                        renderEffect = BlurEffect(blur, blur, TileMode.Decal)
                    }
                },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(116.dp)
                        .graphicsLayer {
                            scaleX = badge
                            scaleY = badge
                        }.clip(CircleShape)
                        .background(c.glassCard)
                        .border(1.dp, c.glassEdge, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(
                    Modifier
                        .size(84.dp)
                        .semantics { contentDescription = title },
                ) {
                    drawCircle(Lime)
                    drawCheck(check)
                }
            }
            Spacer(Modifier.height(22.dp))
            Text(
                title,
                color = c.ink,
                fontSize = 30.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(message, color = c.muted, fontSize = 15.sp, lineHeight = 21.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(c.glassCard)
                        .border(1.dp, c.glassEdge, RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Pedido #${order.summary.folio}",
                    color = c.ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f),
                )
                Text(moneyLabel(order.summary.total), color = c.ink, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

/** Palomita que se dibuja de izquierda a derecha según [progress]. */
private fun DrawScope.drawCheck(progress: Float) {
    if (progress <= 0f) return
    val w = size.width
    val path =
        Path().apply {
            moveTo(w * 0.29f, w * 0.52f)
            lineTo(w * 0.44f, w * 0.66f)
            lineTo(w * 0.72f, w * 0.37f)
        }
    val measure = PathMeasure().apply { setPath(path, false) }
    val partial = Path()
    measure.getSegment(0f, measure.length * progress.coerceIn(0f, 1f), partial, true)
    drawPath(partial, LimeInk, style = Stroke(width = w * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawMosaic(
    mosaic: Mosaic,
    p: Float,
    stage: Size,
) {
    drawFlightMosaic(
        mosaic.cells,
        mosaic.sceneCenter,
        mosaic.sceneSide,
        cameraAt(p, stage.width, stage.height),
        qrCodeRect(stage),
        p,
    )
}

@Composable
private fun GlassCircleButton(
    closing: Float,
    onClick: () -> Unit,
) {
    SeaGlassButton(
        tone = SeaGlassTone.vainilla,
        onClick = onClick,
        modifier = Modifier.size(42.dp),
        shape = CircleShape,
    ) {
        BackCloseIcons(closing)
    }
}

@Composable
private fun BackCloseIcons(closing: Float) {
    Box(contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = if (closing < 0.5f) "Regresar" else null,
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
internal fun GlassPanel(
    modifier: Modifier = Modifier,
    backdrop: (DrawScope.() -> Unit)?,
    shape: RoundedCornerShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    content: @Composable () -> Unit,
) {
    val c = LocalScene.current
    var left by remember { mutableFloatStateOf(0f) }
    var top by remember { mutableFloatStateOf(0f) }
    val frosted = backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .onGloballyPositioned {
                    val at = it.positionInRoot()
                    left = at.x
                    top = at.y
                }.clip(shape),
    ) {
        if (frosted && backdrop != null) {
            Canvas(
                modifier =
                    Modifier
                        .matchParentSize()
                        .blur(26.dp),
            ) {
                withTransform({ translate(-left, -top) }) { backdrop() }
            }
        }
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .background(if (frosted) c.glassTint else c.sheetSolid)
                    .border(1.dp, c.glassEdge, shape),
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
    onViewTracking: (() -> Unit)?,
    onCurrentWaiterCall: (suspend (Int) -> Result<TableCall?>)? = null,
    onCallWaiter: (suspend (Int, CallReason, String?) -> Result<TableCall>)? = null,
    onCancelWaiter: (suspend (TableCall) -> Result<TableCall>)? = null,
    modifier: Modifier = Modifier,
) {
    val c = LocalScene.current
    var height by remember { mutableFloatStateOf(0f) }
    val entrance = remember { Animatable(0f) }
    val reduceMotion = reducedMotion()
    LaunchedEffect(Unit) {
        if (reduceMotion) {
            entrance.snapTo(
                1f,
            )
        } else {
            entrance.animateTo(1f, spring(dampingRatio = 0.86f, stiffness = 180f))
        }
    }
    val progress = remember { Animatable((step?.ordinal ?: -1).toFloat() + 1f) }
    LaunchedEffect(step) {
        val target = (step?.ordinal ?: -1).toFloat() + 1f
        if (reduceMotion) {
            progress.snapTo(
                target,
            )
        } else {
            progress.animateTo(target, spring(dampingRatio = 0.9f, stiffness = 120f))
        }
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
                    .background(c.glassLine, CircleShape),
            )
            Spacer(Modifier.height(12.dp))
            Text("Pedido #${order.summary.folio}", color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            AnimatedContent(
                targetState = pickupTitle(order),
                transitionSpec = {
                    (
                        fadeIn(spring(stiffness = 180f)) + scaleIn(spring(stiffness = 180f), initialScale = 0.92f) +
                            slideInVertically { it / 4 }
                    ) togetherWith
                        (
                            fadeOut(spring(stiffness = 300f)) + scaleOut(targetScale = 1.04f) +
                                slideOutVertically { -it / 4 }
                        )
                },
                label = "pickup_title",
            ) { title ->
                Text(
                    title,
                    color = c.ink,
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
                Text(hint, color = c.muted, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
                                drawRoundRect(c.glassLine, cornerRadius = r)
                                if (fill >
                                    0f
                                ) {
                                    drawRoundRect(
                                        c.ink,
                                        size = Size(size.width * fill, size.height),
                                        cornerRadius = r,
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                item.label,
                                color = lerp(c.muted, c.ink, fill),
                                fontSize = 11.sp,
                                fontWeight = if (item == step) FontWeight.Black else FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            OrderSummaryCard(order)
            val tableSpaceId = openTableCallSpaceId(order)
            if (tableSpaceId != null && onCurrentWaiterCall != null && onCallWaiter != null && onCancelWaiter != null) {
                CallWaiterButton(
                    spaceId = tableSpaceId,
                    orderId = order.summary.id,
                    onCurrent = onCurrentWaiterCall,
                    onCall = onCallWaiter,
                    onCancel = onCancelWaiter,
                    textColor = c.ink,
                )
            }
            Spacer(Modifier.height(14.dp))
            if (hasCode || onViewTracking != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (hasCode) {
                        PillButton(
                            "Mostrar código",
                            primary = true,
                            onClick = onShowCode,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (onViewTracking != null) {
                        PillButton(
                            "Seguir pedido",
                            primary = !hasCode,
                            onClick = onViewTracking,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderSummaryCard(order: OrderDetail) {
    val c = LocalScene.current
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(c.glassCard)
                .border(1.dp, c.glassEdge, shape)
                .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        val visible = order.items.take(2)
        visible.forEach { item ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${item.quantity} × ${item.productName}",
                    color = c.ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(moneyLabel(item.subtotal), color = c.ink, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
        }
        val hidden = order.items.size - visible.size
        if (hidden > 0) Text("+$hidden más", color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Box(
            Modifier
                .padding(vertical = 6.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(c.glassLine),
        )
        Text(
            when (order.summary.destination) {
                OrderDestination.IN_SPACE -> "A ${order.summary.space?.name ?: "tu espacio"}"
                OrderDestination.TAKE_AWAY -> "Para llevar · recoge en barra"
            } + " · " + moneyLabel(order.summary.total),
            color = c.muted,
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
    onViewTracking: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val c = LocalScene.current
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
                    color = c.ink,
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
            Text("Código de recogida", color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(pickupCodeHint(order), color = c.ink, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text(
                order.items.joinToString(" · ") { it.productName },
                color = c.muted,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.LightMode,
                    contentDescription = null,
                    tint = c.muted,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("Subimos el brillo mientras está abierto.", color = c.muted, fontSize = 12.sp)
            }
            if (onViewTracking != null) {
                Spacer(Modifier.height(14.dp))
                PillButton(
                    "Seguir pedido",
                    primary = true,
                    onClick = onViewTracking,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
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
    SeaGlassButton(
        tone = if (primary) SeaGlassTone.lima else SeaGlassTone.vainilla,
        onClick = onClick,
        modifier = modifier.height(50.dp),
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.3).sp)
    }
}

/** Sube el brillo al máximo mientras el código está a la vista y lo regresa al cerrar. */
@Composable
internal fun ScreenBrightnessBoost(active: Boolean) {
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
