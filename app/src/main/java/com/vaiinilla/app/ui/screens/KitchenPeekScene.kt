package com.vaiinilla.app.ui.screens

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.order.TrayItemKind
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

// "Preparando" en el Vainiverso, como `KitchenPeekScene.swift` en iOS: la cámara se asoma por la vitrina izquierda
// de la tienda y adentro está la cocina, con Vaini preparando tu pedido. Cuando está listo se enciende el letrero de
// la cocina, la cámara sale y Vaini ya espera en la puerta; de ahí sigue el vuelo al QR de siempre.

/**
 * Lo que se ve por la vitrina: qué se prepara, qué tan encendido está el letrero de "Listo" (0–1), cuánto tapa la
 * cocina a la vitrina de siempre (0–1) y en qué punto va el vapor (0–1, se repite).
 */
internal data class KitchenPeek(
    val item: TrayItemKind,
    val lit: Float,
    val alpha: Float,
    val steam: Float,
    val steamMoving: Boolean = true,
)

/** La barra de la cocina, en fracción del alto del vidrio. */
private const val KITCHEN_COUNTER_TOP = 0.7f

private val KitchenCream = Color(0xFFF6EFDD)
private val KitchenVainiLime = Color(0xFFA8D52C)
private val KitchenVainiInk = Color(0xFF1A1414)

/** De la tienda completa a la vitrina izquierda, centrada arriba de la hoja de estado y con su marco a la vista. */
internal fun kitchenCamera(
    t: Float,
    stage: Size,
): SceneCamera {
    val glass = vitrineGlass(0, stage)
    return SceneCamera.flight(
        focus = glass.center,
        target = Offset(stage.width * 0.5f, stage.height * 0.3f),
        zoom = min(stage.width * 0.8f / glass.width, stage.height * 0.42f / glass.height),
        t = t,
    )
}

/** Lo que se prepara, en la barra: su centro, la base y la unidad de tamaño de `drawTrayItem`. */
private class KitchenSpot(
    val cx: Float,
    val base: Float,
    val unit: Float,
)

private fun kitchenItemSpot(glass: Rect): KitchenSpot {
    val top = glass.top + glass.height * KITCHEN_COUNTER_TOP
    return KitchenSpot(glass.left + glass.width * 0.22f, top + glass.height * 0.045f * 0.3f, glass.width * 0.2f)
}

/** La cocina por la vitrina izquierda, encima de la vitrina de siempre. Va dentro de `drawStorefront`. */
internal fun DrawScope.drawKitchenPeek(
    stage: Size,
    textMeasurer: TextMeasurer,
    c: ScenePalette,
    kitchen: KitchenPeek,
    motion: SceneMotion,
) {
    val g = vitrineGlass(0, stage)
    withLayerAlpha(g, kitchen.alpha) {
        drawKitchenBack(g, textMeasurer, c, kitchen)
        // Vaini detrás de la barra, con las piernas escondidas y la campana arriba.
        val ready = kitchen.lit > 0.5f
        val box = g.width * 0.5f
        val left = g.left + g.width * 0.6f - box / 2f
        val top = g.top + g.height * KITCHEN_COUNTER_TOP - box * 0.73f
        withTransform({ translate(left, top) }) {
            drawVainiStanding(
                boxPx = box,
                paperColor = KitchenCream,
                accentColor = KitchenVainiLime,
                inkColor = KitchenVainiInk,
                shadowColor = Color.Transparent,
                blink = motion.blink,
                waveDegrees = if (ready) motion.wave else 0f,
                happy = ready,
                lookX = if (ready) 0f else -0.7f,
            )
        }
        drawKitchenFront(g, c, kitchen)
        drawKitchenSteam(g, kitchen)
    }
}

/** Dibuja [block] recortado a [bounds] y con una sola transparencia para todo, sin que se sumen las capas. */
internal fun DrawScope.withLayerAlpha(
    bounds: Rect,
    alpha: Float,
    block: DrawScope.() -> Unit,
) {
    if (alpha <= 0.001f) return
    if (alpha >= 0.999f) {
        clipRect(bounds.left, bounds.top, bounds.right, bounds.bottom) { block() }
        return
    }
    drawIntoCanvas { it.saveLayer(bounds, Paint().apply { this.alpha = alpha }) }
    clipRect(bounds.left, bounds.top, bounds.right, bounds.bottom) { block() }
    drawIntoCanvas { it.restore() }
}

/** La pared de la cocina, detrás de Vaini: azulejos, la repisa con frascos, la campana y el letrero de "Listo". */
private fun DrawScope.drawKitchenBack(
    g: Rect,
    textMeasurer: TextMeasurer,
    c: ScenePalette,
    kitchen: KitchenPeek,
) {
    val hair = 0.5.dp.toPx()

    fun x(f: Float) = g.left + g.width * f

    fun y(f: Float) = g.top + g.height * f

    drawRect(
        Brush.verticalGradient(listOf(c.shopLight, c.shopLightDeep), startY = g.top, endY = g.bottom),
        topLeft = g.topLeft,
        size = g.size,
    )

    // Azulejos de metro entre la repisa y la barra.
    val tile = g.width / 8f
    val tileHeight = tile * 0.5f
    var row = 0
    var rowTop = y(0.4f)
    val joint = c.shopShelf.copy(alpha = 0.45f)
    while (rowTop < y(KITCHEN_COUNTER_TOP)) {
        drawRect(joint, topLeft = Offset(g.left, rowTop), size = Size(g.width, hair))
        var jx = g.left + if (row % 2 == 0) 0f else tile / 2f
        while (jx < g.right) {
            drawRect(joint, topLeft = Offset(jx, rowTop), size = Size(hair, tileHeight))
            jx += tile
        }
        rowTop += tileHeight
        row++
    }

    // La repisa con tres frascos.
    val shelfY = y(0.32f)
    drawRect(c.shopShelf, topLeft = Offset(x(0.04f), shelfY), size = Size(g.width * 0.3f, hair * 3))
    listOf(KitchenCream, SceneLime, KitchenCream).forEachIndexed { index, color ->
        val jarLeft = x(0.08f + index * 0.085f)
        val jarTop = shelfY - g.height * 0.07f
        val jarWidth = g.width * 0.055f
        drawRoundRect(
            color.copy(alpha = 0.9f),
            Offset(jarLeft, jarTop),
            Size(jarWidth, g.height * 0.07f),
            CornerRadius(hair * 2),
        )
        drawRect(c.shopShelf, topLeft = Offset(jarLeft - hair, jarTop - hair * 2), size = Size(jarWidth + hair * 2, hair * 3))
    }

    // La campana sobre Vaini y su luz, que cae sobre la barra.
    val hoodX = x(0.6f)
    val hoodBottom = y(0.15f)
    val cone =
        Path().apply {
            moveTo(hoodX - g.width * 0.2f, hoodBottom)
            lineTo(hoodX + g.width * 0.2f, hoodBottom)
            lineTo(hoodX + g.width * 0.36f, y(KITCHEN_COUNTER_TOP))
            lineTo(hoodX - g.width * 0.36f, y(KITCHEN_COUNTER_TOP))
            close()
        }
    drawPath(
        cone,
        Brush.verticalGradient(
            listOf(c.lampGlow, c.lampGlow.copy(alpha = 0f)),
            startY = hoodBottom,
            endY = y(KITCHEN_COUNTER_TOP),
        ),
    )
    val hood =
        Path().apply {
            moveTo(hoodX - g.width * 0.11f, g.top)
            lineTo(hoodX + g.width * 0.11f, g.top)
            lineTo(hoodX + g.width * 0.24f, hoodBottom)
            lineTo(hoodX - g.width * 0.24f, hoodBottom)
            close()
        }
    drawPath(hood, c.signInk)
    drawRect(
        c.shopShelf,
        topLeft = Offset(hoodX - g.width * 0.24f, hoodBottom - hair * 3),
        size = Size(g.width * 0.48f, hair * 3),
    )

    // Cuando tu pedido sale, la cocina se ilumina y el letrero se enciende como caja de luz.
    val sign = Rect(Offset(x(0.05f), y(0.05f)), Size(g.width * 0.26f, g.height * 0.1f))
    if (kitchen.lit > 0f) {
        drawRect(c.windowLit.copy(alpha = 0.22f * kitchen.lit), topLeft = g.topLeft, size = g.size)
        drawPath(cone, c.windowLit.copy(alpha = 0.3f * kitchen.lit))
        val glow = sign.width * 0.9f
        drawCircle(
            Brush.radialGradient(
                listOf(c.windowLit.copy(alpha = 0.7f * kitchen.lit), Color.Transparent),
                center = sign.center,
                radius = glow,
            ),
            radius = glow,
            center = sign.center,
        )
    }
    drawRoundRect(c.signInk, sign.topLeft, sign.size, CornerRadius(hair * 3))
    val lightbox = sign.deflate(hair * 1.5f)
    drawRoundRect(c.windowLit.copy(alpha = kitchen.lit), lightbox.topLeft, lightbox.size, CornerRadius(hair * 2))
    val word =
        textMeasurer.measure(
            "LISTO",
            TextStyle(
                color = lerp(c.shopShelf.copy(alpha = 0.22f), c.signInk, kitchen.lit),
                fontSize = (sign.height * 0.5f / density / fontScale).sp,
                fontFamily = VaiinillaSerif,
                fontWeight = FontWeight.Bold,
                letterSpacing = (sign.height * 0.06f / density / fontScale).sp,
            ),
        )
    drawText(word, topLeft = Offset(sign.center.x - word.size.width / 2f, sign.center.y - word.size.height / 2f))
}

/** Delante de Vaini: la barra con tu pedido y el reflejo del vidrio. */
private fun DrawScope.drawKitchenFront(
    g: Rect,
    c: ScenePalette,
    kitchen: KitchenPeek,
) {
    val hair = 0.5.dp.toPx()

    fun x(f: Float) = g.left + g.width * f

    // La barra: cubierta clara y frente de tablones, para que no se confunda con el marco verde de la vitrina.
    val top = g.top + g.height * KITCHEN_COUNTER_TOP
    val slab = g.height * 0.045f
    val frontTop = top + slab
    val frontSize = Size(g.width, g.bottom - frontTop)
    drawRect(c.shopShelf, topLeft = Offset(g.left, frontTop), size = frontSize)
    drawRect(Color.Black.copy(alpha = 0.18f), topLeft = Offset(g.left, frontTop), size = frontSize)
    var plank = g.left + g.width / 6f
    while (plank < g.right) {
        drawRect(Color.Black.copy(alpha = 0.12f), topLeft = Offset(plank, frontTop), size = Size(hair, frontSize.height))
        plank += g.width / 6f
    }
    drawRect(c.shopShelf, topLeft = Offset(g.left, top), size = Size(g.width, slab))
    drawRect(Color.White.copy(alpha = 0.35f), topLeft = Offset(g.left, top), size = Size(g.width, hair))
    drawRect(Color.Black.copy(alpha = 0.18f), topLeft = Offset(g.left, frontTop), size = Size(g.width, hair * 2))

    val spot = kitchenItemSpot(g)
    drawTrayItem(kitchen.item, spot.cx, spot.base, spot.unit, alpha = 1f)

    // El vidrio de la vitrina: dos reflejos en diagonal que no pasan por la cara de Vaini.
    val sheen = Color.White.copy(alpha = if (c.isNight) 0.04f else 0.1f)
    for ((from, width) in listOf(0.3f to 0.12f, 0.46f to 0.04f)) {
        val band =
            Path().apply {
                moveTo(x(from), g.top)
                lineTo(x(from + width), g.top)
                lineTo(x(from + width - 0.3f), g.bottom)
                lineTo(x(from - 0.3f), g.bottom)
                close()
            }
        drawPath(band, sheen)
    }
}

/** Tres bocanadas que suben de lo que se prepara, se ensanchan y se desvanecen. Ya listo, el vapor se calma. */
private fun DrawScope.drawKitchenSteam(
    g: Rect,
    kitchen: KitchenPeek,
) {
    val spot = kitchenItemSpot(g)
    val from = spot.base - spot.unit * (if (kitchen.item == TrayItemKind.DRINK) 1.2f else 0.8f)
    val rise = g.height * 0.2f
    val area =
        Rect(
            Offset(spot.cx - spot.unit * 0.55f, from - rise - spot.unit * 0.2f),
            Size(spot.unit * 1.1f, rise + spot.unit * 0.4f),
        )
    // Medidas en un cuadro de 100 de ancho, escalado al área, igual que en iOS.
    val scale = area.width / 100f
    val height = area.height / scale
    val bottom = height - 20f
    val top = 20f
    val calm = 1f - 0.6f * kitchen.lit
    for (index in 0 until 3) {
        val baseX = 50f + (index - 1) * 14.5f
        val p: Float
        val x: Float
        val grow: Float
        val opacity: Float
        if (kitchen.steamMoving) {
            p = (kitchen.steam + index / 3f) % 1f
            x = baseX + sin(p * 2 * PI.toFloat() + index * 2.1f) * 9f
            grow = 0.7f + p
            opacity = 0.5f * sin(PI.toFloat() * p)
        } else {
            // Quietas a distintas alturas, como con movimiento reducido en iOS.
            p = (index + 0.5f) / 3f
            x = baseX
            grow = 0.7f + p
            opacity = 0.35f
        }
        val y = bottom + (top - bottom) * p
        drawCircle(
            Color.White.copy(alpha = opacity * calm),
            radius = 9f * grow * scale,
            center = Offset(area.left + x * scale, area.top + y * scale),
        )
    }
}
