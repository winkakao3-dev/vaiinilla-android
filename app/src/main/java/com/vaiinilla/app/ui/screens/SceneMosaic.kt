package com.vaiinilla.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.zxing.common.BitMatrix
import com.vaiinilla.app.ui.order.MOSAIC_FLIP_LENGTH
import com.vaiinilla.app.ui.order.mosaicCellStart
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// Lo común de las transformaciones del Vainiverso, como `SceneMosaic.swift` en iOS: los tramos de un recorrido, la
// cámara sobre la escena y el mosaico que vuelve un pedazo de la escena en un QR (o un QR en la escena). Lo usan la
// recogida, la entrada, la cartera y la mesa.

/** Tramos de un recorrido: cada parte lee su pedazo de un solo progreso 0–1. */
internal object SceneCurve {
    fun segment(
        p: Float,
        from: Float,
        to: Float,
    ): Float = ((p - from) / (to - from)).coerceIn(0f, 1f)

    fun eased(
        p: Float,
        from: Float,
        to: Float,
    ): Float = FastOutSlowInEasing.transform(segment(p, from, to))

    fun lerp(
        a: Float,
        b: Float,
        t: Float,
    ): Float = a + (b - a) * t

    fun lerp(
        a: Offset,
        b: Offset,
        t: Float,
    ): Offset = a + (b - a) * t
}

/** Cámara: `pantalla = anchor + (puntoEscena - focus) * zoom`. */
internal data class SceneCamera(
    val zoom: Float,
    val anchor: Offset,
    val focus: Offset,
) {
    fun toScreen(scene: Offset): Offset = anchor + (scene - focus) * zoom

    fun toScene(screen: Offset): Offset = focus + (screen - anchor) / zoom

    /** Traslado para un `graphicsLayer` con `transformOrigin = (0, 0)` y escala [zoom]. */
    val translation: Offset get() = Offset(anchor.x - focus.x * zoom, anchor.y - focus.y * zoom)

    companion object {
        /** De quieta (zoom 1) a tener [focus] en [target] con [zoom]; [t] de 0 a 1. */
        fun flight(
            focus: Offset,
            target: Offset,
            zoom: Float,
            t: Float,
        ): SceneCamera = SceneCamera(SceneCurve.lerp(1f, zoom, t), SceneCurve.lerp(focus, target, t), focus)
    }
}

/** Los colores finales del QR: tinta sobre papel, de máximo contraste a cualquier hora. */
internal val QrInk = Color(0xFF151513)
internal val QrPaper = Color(0xFFFBF8F0)

private const val SAMPLE_SCALE = 0.25f

/** El lugar del QR ya asentado, igual en la recogida y en la cartera. */
internal fun qrCodeRect(stage: Size): Rect {
    val side = min(stage.width * 0.74f, stage.height * 0.4f)
    val center = Offset(stage.width * 0.5f, stage.height * 0.38f)
    return Rect(center.x - side / 2f, center.y - side / 2f, center.x + side / 2f, center.y + side / 2f)
}

/**
 * Los colores de una escena bajo cada bloque de un QR: un cuadro de lado [side] centrado en [center], en coordenadas
 * de la escena, muestreado a 1/4 de resolución.
 */
internal class SceneMosaic(
    val matrix: BitMatrix,
    val sampled: Array<Color>,
) {
    val size: Int get() = matrix.width

    companion object {
        fun build(
            matrix: BitMatrix,
            stage: Size,
            center: Offset,
            side: Float,
            density: Density,
            outside: Color,
            draw: DrawScope.(Size) -> Unit,
        ): SceneMosaic {
            val bitmap =
                ImageBitmap(
                    (stage.width * SAMPLE_SCALE).roundToInt().coerceAtLeast(1),
                    (stage.height * SAMPLE_SCALE).roundToInt().coerceAtLeast(1),
                )
            CanvasDrawScope().draw(
                density,
                LayoutDirection.Ltr,
                androidx.compose.ui.graphics
                    .Canvas(bitmap),
                Size(bitmap.width.toFloat(), bitmap.height.toFloat()),
            ) {
                withTransform({ scale(SAMPLE_SCALE, SAMPLE_SCALE, pivot = Offset.Zero) }) { draw(stage) }
            }
            val pixels = bitmap.toPixelMap()
            val n = matrix.width
            val sampled =
                Array(n * n) { index ->
                    val x = ((center.x + ((index % n + 0.5f) / n - 0.5f) * side) * SAMPLE_SCALE).roundToInt()
                    val y = ((center.y + ((index / n + 0.5f) / n - 0.5f) * side) * SAMPLE_SCALE).roundToInt()
                    if (x in 0 until pixels.width && y in 0 until pixels.height) pixels[x, y] else outside
                }
            return SceneMosaic(matrix, sampled)
        }
    }
}

/**
 * Dibuja los bloques en [rect]. Cada uno va del color de la escena al del QR ([toQr]) o al revés, según su [flip]
 * (0–1), con un pequeño latido mientras gira. Las juntas ([gap]) se cierran para que el QR escanee.
 */
internal fun DrawScope.drawMosaicCells(
    mosaic: SceneMosaic,
    rect: Rect,
    gap: Float,
    alpha: Float,
    toQr: Boolean,
    flip: (col: Int, row: Int) -> Float,
) {
    val n = mosaic.size
    val cell = rect.width / n
    for (row in 0 until n) {
        for (col in 0 until n) {
            val t = flip(col, row)
            val scene = mosaic.sampled[row * n + col]
            val code = if (mosaic.matrix[col, row]) QrInk else QrPaper
            val color = if (toQr) lerp(scene, code, t) else lerp(code, scene, t)
            val bump = 1f - 0.22f * sin(PI.toFloat() * t)
            val side = (cell - gap) * bump
            val cx = rect.left + (col + 0.5f) * cell
            val cy = rect.top + (row + 0.5f) * cell
            drawRect(
                color.copy(alpha = color.alpha * alpha),
                topLeft = Offset(cx - side / 2f, cy - side / 2f),
                size = Size(side + 0.5f, side + 0.5f),
            )
        }
    }
}

/** La tarjeta clara detrás del QR, con su margen silencioso de 1.6 bloques. */
internal fun DrawScope.drawQrCard(
    codeRect: Rect,
    modules: Int,
    alpha: Float,
) {
    if (alpha <= 0f) return
    val pad = codeRect.width / modules * 1.6f
    val card = codeRect.inflate(pad)
    val radius = CornerRadius(24.dp.toPx())
    drawRoundRect(
        Color.Black.copy(alpha = 0.06f * alpha),
        topLeft = card.topLeft + Offset(0f, 10.dp.toPx()),
        size = card.size,
        cornerRadius = radius,
    )
    drawRoundRect(QrPaper.copy(alpha = alpha), topLeft = card.topLeft, size = card.size, cornerRadius = radius)
}

/**
 * El final de un vuelo hacia un QR, igual en la recogida y en la cartera: los bloques salen del cuadro de la escena
 * que tiene la cámara enfrente, giran al QR del centro hacia afuera y se asientan en su tarjeta en [finalRect].
 */
internal fun DrawScope.drawFlightMosaic(
    mosaic: SceneMosaic,
    sceneCenter: Offset,
    sceneSide: Float,
    camera: SceneCamera,
    finalRect: Rect,
    p: Float,
) {
    val n = mosaic.size
    val settle = SceneCurve.eased(p, 0.5f, 0.88f)
    val side = SceneCurve.lerp(sceneSide * camera.zoom, finalRect.width, settle)
    val center = SceneCurve.lerp(camera.toScreen(sceneCenter), finalRect.center, settle)
    val rect = Rect(center.x - side / 2f, center.y - side / 2f, center.x + side / 2f, center.y + side / 2f)
    val cell = side / n

    drawQrCard(rect, n, SceneCurve.eased(p, 0.6f, 0.86f))
    // Las juntas entre bloques se ven mientras es mosaico y se cierran antes del final para que escanee.
    drawMosaicCells(
        mosaic,
        rect,
        gap = cell * 0.08f * (1f - SceneCurve.segment(p, 0.78f, 0.9f)),
        alpha = SceneCurve.eased(p, 0.3f, 0.44f),
        toQr = true,
    ) { col, row ->
        val start = mosaicCellStart(col, row, n)
        SceneCurve.eased(p, start, start + MOSAIC_FLIP_LENGTH)
    }
}
