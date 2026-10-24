package com.vaiinilla.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.reducedMotion
import kotlinx.coroutines.launch
import kotlin.math.ln

// La cartera en el Vainiverso es la casita de Vaini: su frasco de tesoros se llena de monedas según el
// saldo, y la etiqueta del frasco es el saldo.

private const val FLOOR_Y = 0.36f
private const val JAR_SCALE_MAX = 2000.0

private val Coin = Color(0xFFE9C46A)
private val CoinRim = Color(0xFFC79A3A)
private val JarCream = Color(0xFFF6EFDD)
private val JarLime = Color(0xFFA8D52C)
private val JarInk = Color(0xFF1A1414)
private val Moon = Color(0xFFF3EBCF)

/** Qué tan lleno (0–1) se ve el frasco: sube rápido con poco saldo y se va llenando más lento después. */
internal fun jarFillLevel(balance: String?): Float {
    val value = balance?.trim()?.replace(",", "")?.toDoubleOrNull() ?: return 0f
    if (value <= 0.0) return 0f
    return (0.12 + 0.88 * ln(1 + value) / ln(1 + JAR_SCALE_MAX)).toFloat().coerceIn(0f, 1f)
}

/** El frasco de tesoros sobre la mesa; la cámara de "Recargar en caja" vuela hacia él. */
internal fun walletJarRect(stage: Size): Rect {
    val tableTop = stage.height * 0.285f
    val jarW = stage.width * 0.24f
    val jarH = stage.height * 0.12f
    val left = stage.width * 0.48f + (stage.width * 0.4f - jarW) / 2f
    return Rect(left, tableTop - jarH, left + jarW, tableTop)
}

internal class JarMotion(
    val blink: Float,
    val happy: Boolean,
    val fill: Float,
    /** Monedas cayendo al frasco tras una recarga (0 arriba, 1 dentro); null si no cae nada. */
    val falling: Float?,
    val label: String?,
)

/** La casita de Vaini detrás de la cartera. Va dentro de un `LocalScene` ya provisto. */
@Composable
internal fun WalletJarBackdrop(
    sheetTop: () -> Float,
    balance: String?,
) {
    val palette = LocalScene.current
    val reduceMotion = reducedMotion()
    val textMeasurer = rememberTextMeasurer()
    val target = jarFillLevel(balance)
    val fill = remember { Animatable(target) }
    val falling = remember { Animatable(1f) }
    var happy by remember { mutableStateOf(false) }
    var lastTarget by remember { mutableStateOf(target) }
    LaunchedEffect(target) {
        val grew = target > lastTarget
        lastTarget = target
        if (reduceMotion) {
            fill.snapTo(target)
            return@LaunchedEffect
        }
        if (grew) {
            // Recarga: caen monedas, el frasco sube y Vaini se alegra.
            happy = true
            falling.snapTo(0f)
            launch { falling.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 90f)) }
            kotlinx.coroutines.delay(350)
            fill.animateTo(target, spring(dampingRatio = 0.75f, stiffness = 60f))
            kotlinx.coroutines.delay(600)
            happy = false
        } else {
            fill.animateTo(target, spring(dampingRatio = 1f, stiffness = 80f))
        }
    }
    val idle = rememberInfiniteTransition(label = "jar_vaini")
    val blink by idle.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                keyframes {
                    durationMillis = 4100
                    1f at 3500
                    0.06f at 3580
                    1f at 3690
                },
                RepeatMode.Restart,
            ),
        label = "blink",
    )
    VainiversoBackdrop(sheetTop = sheetTop) {
        drawVainiHome(
            stage = size,
            textMeasurer = textMeasurer,
            c = palette,
            m =
                JarMotion(
                    blink = if (reduceMotion) 1f else blink,
                    happy = happy,
                    fill = fill.value,
                    falling = falling.value.takeIf { it < 1f },
                    label = balance?.let(::moneyLabel),
                ),
        )
    }
}

internal fun DrawScope.drawVainiHome(
    stage: Size,
    textMeasurer: TextMeasurer,
    c: ScenePalette,
    m: JarMotion,
) {
    val w = stage.width
    val h = stage.height
    val hair = 1.dp.toPx()
    val floor = h * FLOOR_Y

    // Pared con rayas de papel tapiz, zoclo y piso.
    drawRect(c.facadeLight, size = stage)
    var stripe = 0f
    while (stripe < w) {
        drawRect(c.facade.copy(alpha = 0.35f), topLeft = Offset(stripe, 0f), size = Size(w * 0.035f, floor))
        stripe += w * 0.11f
    }
    drawRect(c.sidewalk, topLeft = Offset(0f, floor), size = Size(w, h - floor))
    drawRect(c.facadeShade, topLeft = Offset(0f, floor - h * 0.012f), size = Size(w, h * 0.012f))

    // Ventana: de día cielo, de noche luna y estrellas.
    val winLeft = w * 0.08f
    val winTop = h * 0.085f
    val winSize = Size(w * 0.3f, h * 0.13f)
    drawRoundRect(
        c.windowFrame,
        Offset(winLeft - hair * 5, winTop - hair * 5),
        Size(
            winSize.width + hair * 10,
            winSize.height + hair * 10,
        ),
        CornerRadius(hair * 4),
    )
    val night = c.litWindows.isNotEmpty()
    drawRect(
        Brush.verticalGradient(
            if (night) listOf(c.skyTop, c.skyBottom) else listOf(c.windowGlassLight, c.skyBottom),
            startY = winTop,
            endY = winTop + winSize.height,
        ),
        topLeft = Offset(winLeft, winTop),
        size = winSize,
    )
    clipRect(winLeft, winTop, winLeft + winSize.width, winTop + winSize.height) {
        if (night) {
            drawCircle(
                Moon,
                radius = winSize.height * 0.16f,
                center =
                    Offset(
                        winLeft + winSize.width * 0.7f,
                        winTop + winSize.height * 0.3f,
                    ),
            )
            for ((sx, sy) in listOf(0.18f to 0.25f, 0.35f to 0.6f, 0.52f to 0.2f, 0.85f to 0.72f)) {
                drawCircle(
                    Moon.copy(alpha = 0.8f),
                    radius = hair * 1.4f,
                    center =
                        Offset(
                            winLeft + winSize.width * sx,
                            winTop + winSize.height * sy,
                        ),
                )
            }
        } else {
            drawCircle(
                Color.White.copy(alpha = 0.7f),
                radius = winSize.height * 0.14f,
                center =
                    Offset(
                        winLeft + winSize.width * 0.3f,
                        winTop + winSize.height * 0.45f,
                    ),
            )
            drawCircle(
                Color.White.copy(alpha = 0.7f),
                radius = winSize.height * 0.11f,
                center =
                    Offset(
                        winLeft + winSize.width * 0.42f,
                        winTop + winSize.height * 0.42f,
                    ),
            )
        }
    }
    drawLine(
        c.windowFrame,
        Offset(winLeft + winSize.width / 2f, winTop),
        Offset(
            winLeft + winSize.width / 2f,
            winTop + winSize.height,
        ),
        strokeWidth = hair * 4,
    )
    drawRect(
        c.facadeShade,
        topLeft = Offset(winLeft - hair * 10, winTop + winSize.height + hair * 5),
        size =
            Size(
                winSize.width + hair * 20,
                hair * 6,
            ),
    )

    // Lámpara de pie con su luz cálida.
    val lampX = w * 0.93f
    drawCircle(
        Brush.radialGradient(listOf(c.lampGlow, Color.Transparent), Offset(lampX, h * 0.12f), w * 0.3f),
        radius = w * 0.3f,
        center = Offset(lampX, h * 0.12f),
    )
    drawLine(c.signInk, Offset(lampX, h * 0.12f), Offset(lampX, floor), strokeWidth = hair * 2)
    drawArc(c.awning, 180f, 180f, true, Offset(lampX - w * 0.06f, h * 0.1f), Size(w * 0.12f, w * 0.08f))

    // Vaini junto a su mesa.
    val box = w * 0.3f
    withTransform({ translate(w * 0.27f - box / 2f, floor - box * 0.76f) }) {
        drawVainiStanding(
            boxPx = box,
            paperColor = JarCream,
            accentColor = JarLime,
            inkColor = JarInk,
            blink = m.blink,
            happy = m.happy,
            lookX = 0.6f,
        )
    }

    // Mesa.
    val tableTop = h * 0.285f
    val tableLeft = w * 0.48f
    val tableW = w * 0.4f
    drawRoundRect(c.shopShelf, Offset(tableLeft, tableTop), Size(tableW, h * 0.012f), CornerRadius(hair * 3))
    for (lx in listOf(0.06f, 0.9f)) {
        drawRect(
            c.pot,
            topLeft = Offset(tableLeft + tableW * lx, tableTop + h * 0.012f),
            size =
                Size(
                    hair * 5,
                    floor - tableTop - h * 0.012f,
                ),
        )
    }

    // El frasco de tesoros: las monedas llegan hasta el nivel del saldo.
    val jarFrame = walletJarRect(stage)
    val jarW = jarFrame.width
    val jarH = jarFrame.height
    val jarLeft = jarFrame.left
    val jarTop = jarFrame.top
    val inner = hair * 4
    val innerTop = jarTop + jarH * 0.12f
    val innerBottom = tableTop - inner
    val coinTop = innerBottom - (innerBottom - innerTop) * m.fill.coerceIn(0f, 1f)
    clipRect(jarLeft + inner, coinTop, jarLeft + jarW - inner, innerBottom) {
        val coinW = jarW * 0.2f
        val coinH = coinW * 0.42f
        var row = 0
        var y = innerBottom - coinH
        while (y > coinTop - coinH) {
            val offset = if (row % 2 == 0) 0f else coinW * 0.5f
            var x = jarLeft + inner - coinW * 0.25f + offset
            while (x < jarLeft + jarW) {
                drawOval(CoinRim, Offset(x, y + hair), Size(coinW, coinH))
                drawOval(Coin, Offset(x, y), Size(coinW, coinH))
                x += coinW * 0.95f
            }
            y -= coinH * 0.62f
            row++
        }
    }
    m.falling?.let { t ->
        val coinW = jarW * 0.2f
        for ((i, dx) in listOf(-0.2f, 0.05f, 0.25f).withIndex()) {
            val local = ((t - i * 0.12f) / 0.76f).coerceIn(0f, 1f)
            if (local <= 0f || local >= 1f) continue
            val cy = jarTop - h * 0.12f + (coinTop - jarTop + h * 0.12f) * local
            drawOval(Coin, Offset(jarLeft + jarW / 2f + jarW * dx - coinW / 2f, cy), Size(coinW, coinW * 0.42f))
        }
    }
    drawRoundRect(
        c.windowGlassLight.copy(alpha = 0.28f),
        Offset(jarLeft, jarTop),
        Size(jarW, jarH),
        CornerRadius(
            hair * 12,
        ),
    )
    drawRoundRect(
        Color.White.copy(alpha = 0.5f),
        Offset(jarLeft, jarTop),
        Size(jarW, jarH),
        CornerRadius(hair * 12),
        style =
            Stroke(
                hair * 2,
            ),
    )
    drawRoundRect(
        Color.White.copy(alpha = 0.35f),
        Offset(jarLeft + jarW * 0.1f, jarTop + jarH * 0.18f),
        Size(
            hair * 4,
            jarH * 0.5f,
        ),
        CornerRadius(hair * 2),
    )
    drawRoundRect(
        c.pot,
        Offset(jarLeft + jarW * 0.1f, jarTop - hair * 8),
        Size(jarW * 0.8f, hair * 10),
        CornerRadius(
            hair * 3,
        ),
    )

    // Etiqueta de papel con el saldo.
    m.label?.let { label ->
        val text =
            textMeasurer.measure(
                label,
                TextStyle(
                    color = JarInk,
                    fontSize = (w * 0.045f / density / fontScale).sp,
                    fontFamily = VaiinillaSerif,
                    fontWeight = FontWeight.Bold,
                ),
            )
        val tagW = text.size.width + hair * 16
        val tagH = text.size.height + hair * 6
        val tagLeft = jarLeft + (jarW - tagW) / 2f
        val tagTop = jarTop + jarH * 0.42f
        drawRoundRect(JarCream, Offset(tagLeft, tagTop), Size(tagW, tagH), CornerRadius(hair * 3))
        drawRect(JarLime, topLeft = Offset(tagLeft, tagTop), size = Size(tagW, hair * 3))
        drawText(text, topLeft = Offset(tagLeft + hair * 8, tagTop + hair * 3))
    }

    // Una maceta en la esquina.
    drawCircle(c.leafDark, radius = w * 0.05f, center = Offset(w * 0.08f, floor - w * 0.09f))
    drawCircle(c.leaf, radius = w * 0.042f, center = Offset(w * 0.06f, floor - w * 0.11f))
    drawRoundRect(c.pot, Offset(w * 0.04f, floor - w * 0.06f), Size(w * 0.08f, w * 0.06f), CornerRadius(hair * 3))
}
