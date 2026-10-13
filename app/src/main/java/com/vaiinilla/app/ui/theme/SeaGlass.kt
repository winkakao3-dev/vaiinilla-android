package com.vaiinilla.app.ui.theme

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.screens.scenePaletteForTheme
import java.nio.ByteBuffer
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

// Vidrio de mar: espejo de SeaGlass.swift (iOS), misma receta y mismas proporciones. Color pastel casi parejo,
// todo el canto aclarado (más abajo), canto apenas difuminado y una sombra gris neutra un poco más ancha que la
// pieza. Como `flattened` en iOS, la pieza se pinta una vez por tamaño en mapas de bits y se reutiliza; al
// presionar solo baja la sombra.

/** Si el Vainiverso está de noche (el tema oscuro o black). */
@Composable
internal fun seaGlassNight(): Boolean = scenePaletteForTheme().isNight

/** Tinta de una pieza de vidrio a la hora actual del Vainiverso. */
@Composable
internal fun seaGlassInk(tone: SeaGlassTone): Color = seaGlassShades(tone, seaGlassNight()).ink.toColor()

/**
 * La pieza de vidrio detrás de un contenido. [pressed] (0–1) la baja hacia el piso. [grounded] = false quita la
 * sombra: una pieza dentro de otra (el segmento elegido). La sombra se sale del marco, así que el contenedor no
 * debe recortar a sus límites.
 */
internal fun Modifier.seaGlass(
    tone: SeaGlassTone,
    shape: Shape,
    night: Boolean,
    grounded: Boolean = true,
    pressed: State<Float>? = null,
): Modifier = this then SeaGlassElement(tone, shape, night, grounded, pressed)

/**
 * Botón de vidrio de mar: se hunde con un resorte al presionar y regresa con rebote. [content] se pinta con la
 * tinta de la pieza. El tamaño y el relleno los decide el llamador con [modifier].
 */
@Composable
internal fun SeaGlassButton(
    tone: SeaGlassTone,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(percent = 50),
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val night = seaGlassNight()
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    // Resorte de iOS: rigidez 420, amortiguación 18 (proporción 18 / (2·√420) ≈ 0.44).
    val press =
        animateFloatAsState(
            targetValue = if (isPressed && enabled) 1f else 0f,
            animationSpec = if (reducedMotion()) snap() else spring(dampingRatio = 0.44f, stiffness = 420f),
            label = "sea-glass-press",
        )
    Row(
        modifier =
            modifier
                .graphicsLayer {
                    val scale = 1f - 0.045f * press.value
                    scaleX = scale
                    scaleY = scale
                    alpha = if (enabled) 1f else 0.5f
                }.seaGlass(tone, shape, night, pressed = press)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides seaGlassShades(tone, night).ink.toColor()) {
            content()
        }
    }
}

private data class SeaGlassElement(
    val tone: SeaGlassTone,
    val shape: Shape,
    val night: Boolean,
    val grounded: Boolean,
    val pressed: State<Float>?,
) : ModifierNodeElement<SeaGlassNode>() {
    override fun create() = SeaGlassNode(tone, shape, night, grounded, pressed)

    override fun update(node: SeaGlassNode) {
        node.update(tone, shape, night, grounded, pressed)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "seaGlass"
    }
}

private class SeaGlassNode(
    var tone: SeaGlassTone,
    var shape: Shape,
    var night: Boolean,
    var grounded: Boolean,
    var pressed: State<Float>?,
) : Modifier.Node(),
    DrawModifierNode {
    private var piece: SeaGlassBitmaps? = null
    private var pieceKey: Any? = null

    fun update(
        tone: SeaGlassTone,
        shape: Shape,
        night: Boolean,
        grounded: Boolean,
        pressed: State<Float>?,
    ) {
        this.tone = tone
        this.shape = shape
        this.night = night
        this.grounded = grounded
        this.pressed = pressed
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        if (size.width >= 1f && size.height >= 1f) {
            val key = listOf(tone, shape, night, grounded, size, layoutDirection, density)
            val bitmaps =
                piece?.takeIf { pieceKey == key }
                    ?: renderSeaGlass(tone, shape, night, grounded, size, layoutDirection, this).also {
                        piece = it
                        pieceKey = key
                    }
            val press = pressed?.value ?: 0f
            bitmaps.shadow?.let { shadow ->
                val depth = size.height * SeaGlassRecipe.DEPTH.toFloat() * (1f - 0.55f * press)
                drawImage(shadow.image, Offset(-shadow.margin, -shadow.margin + depth))
            }
            drawImage(bitmaps.body.image, Offset(-bitmaps.body.margin, -bitmaps.body.margin))
        }
        drawContent()
    }
}

private class MarginBitmap(
    val image: ImageBitmap,
    val margin: Float,
)

private class SeaGlassBitmaps(
    val body: MarginBitmap,
    val shadow: MarginBitmap?,
)

private fun renderSeaGlass(
    tone: SeaGlassTone,
    shape: Shape,
    night: Boolean,
    grounded: Boolean,
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density,
): SeaGlassBitmaps {
    val s = seaGlassShades(tone, night)
    val outline = shape.createOutline(size, layoutDirection, density)
    return SeaGlassBitmaps(
        body = renderBody(s, outline, size, rimWidth = density.density),
        shadow = if (grounded) renderShadow(s, outline, size) else null,
    )
}

/** La sombra: la silueta crecida 4 % del alto por lado, gris neutra y desenfocada. Se mueve al dibujar. */
private fun renderShadow(
    s: SeaGlassShades,
    outline: Outline,
    size: Size,
): MarginBitmap {
    val h = size.height
    val margin = ceil(h * 0.32f)
    val bitmap = blankBitmap(size, margin)
    val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = s.shadow.argb()
            gaussian(h * 0.03f)?.let { maskFilter = it }
        }
    Canvas(bitmap).drawPath(outline.grownPath(h * 0.04f, margin, margin), paint)
    return MarginBitmap(bitmap.asImageBitmap(), margin)
}

/**
 * El cuerpo: degradado casi parejo, la luz del canto (una franja abajo y un halo por toda la orilla) y el filo
 * de 1 dp, recortados a la pieza; luego todo se desenfoca alto/56 para que el canto quede pulido.
 */
private fun renderBody(
    s: SeaGlassShades,
    outline: Outline,
    size: Size,
    rimWidth: Float,
): MarginBitmap {
    val h = size.height
    val soft = h * SeaGlassRecipe.SOFT_EDGE.toFloat()
    val margin = ceil(soft * 3f + 2f)
    val bitmap = blankBitmap(size, margin)
    val canvas = Canvas(bitmap)
    canvas.save()
    canvas.clipPath(outline.grownPath(0f, margin, margin))
    canvas.drawPaint(
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader =
                LinearGradient(
                    0f,
                    margin,
                    0f,
                    margin + h,
                    intArrayOf(s.top.argb(), s.mid.argb(), s.low.argb(), s.bottom.argb()),
                    floatArrayOf(0f, 0.45f, 0.68f, 1f),
                    Shader.TileMode.CLAMP,
                )
        },
    )
    innerGlow(canvas, outline, s.edge, h, margin, spread = -h * 0.06f, y = -h * 0.12f, blur = h * 0.08f)
    innerGlow(canvas, outline, s.edge, h, margin, spread = h * 0.01f, y = 0f, blur = h * 0.1f)
    canvas.drawPath(
        outline.grownPath(-rimWidth / 2f, margin, margin),
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = rimWidth
            color = s.rim.argb()
        },
    )
    canvas.restore()
    gaussianBlur(bitmap, soft)
    return MarginBitmap(bitmap.asImageBitmap(), margin)
}

/**
 * Sombra interior, como `innerGlow` en iOS y `box-shadow: inset` en el laboratorio: un anillo de 0.6 del alto
 * que empieza [spread] hacia adentro de la orilla (negativo: afuera), movido [y] y desenfocado. Solo lo que el
 * desenfoque mete dentro de la pieza se ve.
 */
private fun innerGlow(
    canvas: Canvas,
    outline: Outline,
    color: SeaGlassRgba,
    h: Float,
    margin: Float,
    spread: Float,
    y: Float,
    blur: Float,
) {
    val band = h * 0.6f
    val ring =
        Path().apply {
            fillType = Path.FillType.EVEN_ODD
            addPath(outline.grownPath(band - spread, margin, margin + y))
            addPath(outline.grownPath(-spread, margin, margin + y))
        }
    val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color.argb()
            gaussian(blur)?.let { maskFilter = it }
        }
    canvas.drawPath(ring, paint)
}

private fun blankBitmap(
    size: Size,
    margin: Float,
): Bitmap =
    Bitmap.createBitmap(
        (size.width + 2 * margin).roundToInt().coerceAtLeast(1),
        (size.height + 2 * margin).roundToInt().coerceAtLeast(1),
        Bitmap.Config.ARGB_8888,
    )

/** Desenfoque gaussiano de [sigma] px (lo que iOS llama radio). Skia lo pide en su propio radio. */
private fun gaussian(sigma: Float): BlurMaskFilter? {
    val radius = (sigma - 0.5f) / 0.57735f
    return if (radius > 0.5f) BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL) else null
}

/**
 * La silueta crecida [by] px por lado (encogida si es negativo), movida a ([dx], [dy]). Las esquinas crecen con
 * ella, como `inset(by:)` en SwiftUI; una cápsula sigue siendo cápsula.
 */
private fun Outline.grownPath(
    by: Float,
    dx: Float,
    dy: Float,
): Path =
    when (this) {
        is Outline.Rectangle ->
            Path().apply {
                addRect(
                    rect.left - by + dx,
                    rect.top - by + dy,
                    rect.right + by + dx,
                    rect.bottom + by + dy,
                    Path.Direction.CW,
                )
            }
        is Outline.Rounded -> {
            val r = roundRect

            fun grow(radius: Float) = max(radius + by, 0f)
            Path().apply {
                addRoundRect(
                    RectF(r.left - by + dx, r.top - by + dy, r.right + by + dx, r.bottom + by + dy),
                    floatArrayOf(
                        grow(r.topLeftCornerRadius.x),
                        grow(r.topLeftCornerRadius.y),
                        grow(r.topRightCornerRadius.x),
                        grow(r.topRightCornerRadius.y),
                        grow(r.bottomRightCornerRadius.x),
                        grow(r.bottomRightCornerRadius.y),
                        grow(r.bottomLeftCornerRadius.x),
                        grow(r.bottomLeftCornerRadius.y),
                    ),
                    Path.Direction.CW,
                )
            }
        }
        // Formas libres: se mueven pero no crecen. Las piezas del Vainiverso son cápsulas, círculos y rectángulos
        // redondeados.
        is Outline.Generic -> Path(path.asAndroidPath()).apply { offset(dx, dy) }
    }

/**
 * Desenfoque gaussiano de la imagen completa (color y alfa), aproximado con tres pasadas de caja sobre los
 * píxeles premultiplicados: así la orilla transparente no oscurece el color.
 */
private fun gaussianBlur(
    bitmap: Bitmap,
    sigma: Float,
) {
    if (sigma < 0.5f) return
    val radius = ((sqrt(4f * sigma * sigma + 1f) - 1f) / 2f).roundToInt().coerceAtLeast(1)
    val w = bitmap.width
    val h = bitmap.height
    val buffer = ByteBuffer.allocate(w * h * 4)
    bitmap.copyPixelsToBuffer(buffer)
    val a = buffer.array()
    val src = IntArray(a.size) { a[it].toInt() and 0xFF }
    val tmp = IntArray(src.size)
    repeat(3) {
        boxPass(src, tmp, w, h, radius, horizontal = true)
        boxPass(tmp, src, w, h, radius, horizontal = false)
    }
    for (i in a.indices) a[i] = src[i].toByte()
    buffer.rewind()
    bitmap.copyPixelsFromBuffer(buffer)
}

private fun boxPass(
    src: IntArray,
    dst: IntArray,
    w: Int,
    h: Int,
    r: Int,
    horizontal: Boolean,
) {
    val lines = if (horizontal) h else w
    val length = if (horizontal) w else h
    val window = 2 * r + 1
    for (line in 0 until lines) {
        for (channel in 0 until 4) {
            fun at(i: Int): Int {
                val p = i.coerceIn(0, length - 1)
                val index = if (horizontal) line * w + p else p * w + line
                return src[index * 4 + channel]
            }
            var sum = 0
            for (i in -r..r) sum += at(i)
            for (i in 0 until length) {
                val index = if (horizontal) line * w + i else i * w + line
                dst[index * 4 + channel] = (sum + window / 2) / window
                sum += at(i + r + 1) - at(i - r)
            }
        }
    }
}

private fun SeaGlassRgba.toColor(): Color = Color(red.toFloat(), green.toFloat(), blue.toFloat(), alpha.toFloat())

private fun SeaGlassRgba.argb(): Int =
    android.graphics.Color.argb(
        (alpha * 255).roundToInt(),
        (red * 255).roundToInt(),
        (green * 255).roundToInt(),
        (blue * 255).roundToInt(),
    )
