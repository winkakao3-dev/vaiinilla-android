package com.vaiinilla.app.ui.screens

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.core.qr.QrCodeEncoder
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.order.MOSAIC_FLIP_FROM
import com.vaiinilla.app.ui.order.MOSAIC_FLIP_SPREAD
import com.vaiinilla.app.ui.order.mosaicCellStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

// Entrar a un lugar es la recogida al revés, como `StoreEntranceView.swift` en iOS: el QR que escaneaste se deshace
// en un mosaico que se vuelve la fachada, Vaini saluda en la puerta y la cámara entra por ella hasta que la luz de la
// tienda llena la pantalla. Ahí espera a que el menú esté listo y se desvanece sobre él. Si no se pudo entrar, la
// cámara sale por la puerta de regreso y se desvanece sobre la calle.

/** Una entrada en curso. [code]: lo que se escaneó, se escribió o llegó por enlace; sin código empieza en la fachada. */
internal data class StoreEntrance(
    val code: String?,
    val id: String = UUID.randomUUID().toString(),
)

internal enum class StoreEntranceOutcome { WAITING, READY, FAILED }

private const val DOOR_FOCUS_Y = 0.345f
private const val DOOR_ZOOM = 12f
private const val MOSAIC_CENTER_Y = 0.39f
private const val HOLD_AT = 0.9f
private const val START_WITHOUT_CODE = 0.42f

private fun smooth(
    p: Float,
    from: Float,
    to: Float,
): Float {
    val x = SceneCurve.segment(p, from, to)
    return x * x * (3 - 2 * x)
}

/**
 * La entrada encima de todo. [outcome] dice si el lugar ya abrió (se desvanece sobre el menú), si sigue cargando
 * (espera en la luz de la tienda) o si falló (sale por la puerta). [frozenAt] deja el recorrido quieto, para capturas.
 */
@Composable
internal fun StoreEntranceOverlay(
    entrance: StoreEntrance,
    outcome: StoreEntranceOutcome,
    onFinished: () -> Unit,
    frozenAt: Float? = null,
) {
    val palette = LocalScene.current
    val reduceMotion = reducedMotion()
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val p = remember(entrance.id) { Animatable(frozenAt ?: if (entrance.code == null) START_WITHOUT_CODE else 0f) }
    val fade = remember(entrance.id) { Animatable(1f) }
    var arrived by remember(entrance.id) { mutableStateOf(false) }
    var finishing by remember(entrance.id) { mutableStateOf(false) }
    val latestOnFinished by rememberUpdatedState(onFinished)

    LaunchedEffect(entrance.id) {
        if (frozenAt != null) return@LaunchedEffect
        if (reduceMotion) {
            p.snapTo(HOLD_AT)
        } else {
            // Críticamente amortiguado: llega sin rebotar; el recorrido con QR tarda más.
            val k = if (entrance.code == null) 34f else 15f
            launch { p.animateTo(HOLD_AT, spring(dampingRatio = 1f, stiffness = k)) }
            delay((6.6f / sqrt(k) * 1000).toLong())
        }
        arrived = true
    }
    LaunchedEffect(arrived, outcome) {
        if (!arrived || finishing) return@LaunchedEffect
        when (outcome) {
            StoreEntranceOutcome.WAITING -> Unit
            StoreEntranceOutcome.READY -> {
                finishing = true
                fade.animateTo(0f, tween(if (reduceMotion) 200 else 450))
                latestOnFinished()
            }
            StoreEntranceOutcome.FAILED -> {
                finishing = true
                if (!reduceMotion) launch { p.animateTo(START_WITHOUT_CODE, spring(dampingRatio = 1f, stiffness = 60f)) }
                delay(if (reduceMotion) 0 else 450)
                fade.animateTo(0f, tween(500))
                latestOnFinished()
            }
        }
    }

    val matrix = remember(entrance.code) { entrance.code?.let { runCatching { QrCodeEncoder.encode(it) }.getOrNull() } }
    val motion = rememberCheeringMotion(reduceMotion)
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = fade.value }
            .background(palette.stage)
            // La entrada tapa la pantalla: nada de abajo se toca mientras cruza la puerta.
            .pointerInput(Unit) {}
            .semantics { contentDescription = "Entrando" },
    ) {
        val stage = with(density) { Size(maxWidth.toPx(), maxHeight.toPx()) }
        val sceneCenter = Offset(stage.width / 2f, stage.height * MOSAIC_CENTER_Y)
        val mosaic =
            remember(matrix, stage, palette) {
                matrix?.let {
                    SceneMosaic.build(it, stage, sceneCenter, stage.width, density, palette.stage) { size ->
                        drawStorefront(size, textMeasurer, palette, SceneMotion())
                    }
                }
            }
        val progress = p.value
        val t = smooth(progress, 0.5f, 0.88f)
        val zoom = SceneCurve.lerp(1f, DOOR_ZOOM, t * t)
        val focus = Offset(stage.width / 2f, stage.height * DOOR_FOCUS_Y)
        val anchor = Offset(focus.x, SceneCurve.lerp(focus.y, stage.height / 2f, t))
        val camera = SceneCamera(zoom, anchor, focus)
        val sceneAlpha = if (mosaic == null) 1f else smooth(progress, 0.34f, 0.48f)
        val blur = if (reduceMotion) 0f else 9f * sin(PI.toFloat() * SceneCurve.segment(progress, 0.5f, 0.88f))
        if (sceneAlpha > 0.001f) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = camera.zoom
                        scaleY = camera.zoom
                        translationX = camera.translation.x
                        translationY = camera.translation.y
                        alpha = sceneAlpha
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blur > 0.5f) {
                            val px = blur * density.density
                            renderEffect = BlurEffect(px, px, TileMode.Clamp)
                        }
                    },
            ) { drawStorefront(size, textMeasurer, palette, motion()) }
        }
        if (mosaic != null && progress < 0.56f) {
            Canvas(Modifier.fillMaxSize()) { drawEntranceMosaic(mosaic, progress, sceneCenter) }
        }
        // Dentro: la luz cálida de la tienda llena la pantalla.
        Canvas(Modifier.fillMaxSize()) {
            drawRect(palette.shopLight.copy(alpha = smooth(progress, 0.78f, 0.9f)))
        }
    }
}

/** Vaini parpadea y saluda en la puerta mientras entras. */
@Composable
private fun rememberCheeringMotion(reduceMotion: Boolean): () -> SceneMotion {
    val idle = rememberInfiniteTransition(label = "entrance_vaini")
    val blink =
        idle.animateFloat(
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
    val wave =
        idle.animateFloat(
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
    return {
        SceneMotion(
            blink = if (reduceMotion) 1f else blink.value,
            wave = if (reduceMotion) 0f else wave.value,
            happy = true,
        )
    }
}

/** El QR en su tarjeta crece hasta cubrir la escena y cada bloque gira al color de lo que tiene debajo. */
private fun DrawScope.drawEntranceMosaic(
    mosaic: SceneMosaic,
    p: Float,
    sceneCenter: Offset,
) {
    val stage = size
    val n = mosaic.size
    val qrSide = minOf(stage.width * 0.74f, stage.height * 0.4f)
    val qrCenter = Offset(stage.width / 2f, stage.height * 0.42f)
    val grow = smooth(p, 0.12f, 0.4f)
    val side = SceneCurve.lerp(qrSide, stage.width, grow)
    val center = SceneCurve.lerp(qrCenter, sceneCenter, grow)
    val rect = Rect(center.x - side / 2f, center.y - side / 2f, center.x + side / 2f, center.y + side / 2f)
    val cell = side / n
    val alpha = 1f - smooth(p, 0.42f, 0.54f)

    val cardAlpha = 1f - smooth(p, 0.04f, 0.2f)
    if (cardAlpha > 0f) {
        val card = rect.inflate(cell * 1.6f)
        drawRoundRect(
            Color(0xFFFBF8F0).copy(alpha = cardAlpha),
            topLeft = card.topLeft,
            size = card.size,
            cornerRadius = CornerRadius(24.dp.toPx()),
        )
    }
    // El mismo orden que la recogida: del centro hacia afuera.
    drawMosaicCells(
        mosaic,
        rect,
        gap = cell * 0.08f * SceneCurve.segment(p, 0.04f, 0.12f),
        alpha = alpha,
        toQr = false,
    ) { col, row ->
        val order = (mosaicCellStart(col, row, n) - MOSAIC_FLIP_FROM) / MOSAIC_FLIP_SPREAD
        val start = 0.06f + order * 0.22f
        smooth(p, start, start + 0.14f)
    }
}

