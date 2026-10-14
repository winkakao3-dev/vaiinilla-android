package com.vaiinilla.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.order.TrayItemKind
import com.vaiinilla.app.ui.order.traySlots
import kotlinx.coroutines.delay

// El carrito en el Vainiverso es el mostrador: Vaini detrás con la bandeja, lo que pides cae sobre ella
// y la pizarra lleva el total. Vacío, Vaini busca con la mirada qué servirte.

private const val COUNTER_TOP_Y = 0.285f
private const val COUNTER_BOTTOM_Y = 0.4f

private val CookieDough = Color(0xFFC9935A)
private val CookieChip = Color(0xFF5A3A22)
private val PlateRim = Color(0xFFF7F3EA)
private val PlateFood = Color(0xFFE38B5A)
private val PlateLeaf = Color(0xFF8DAF48)
private val CupLid = Color(0xFF2E3B1E)
private val CounterLime = Color(0xFFB7D464)
private val CounterCream = Color(0xFFF6EFDD)
private val CounterVainiLime = Color(0xFFA8D52C)
private val CounterVainiInk = Color(0xFF1A1414)

private class CounterMotion(
    val blink: Float,
    val lookX: Float,
    val happy: Boolean,
    val questionBob: Float,
    val showQuestion: Boolean,
    /** Tipo de cada pieza y cuánto ha caído (0 arriba, 1 en la bandeja; el resorte pasa un poco de 1). */
    val items: List<Pair<TrayItemKind, Float>>,
    val total: String?,
)

/** El mostrador detrás de la lista del carrito. Va dentro de un `LocalScene` ya provisto. */
@Composable
internal fun CartCounterBackdrop(
    sheetTop: () -> Float,
    lines: List<CartLine>,
    totalLabel: String?,
) {
    val palette = LocalScene.current
    val reduceMotion = reducedMotion()
    val textMeasurer = rememberTextMeasurer()
    val slots = remember(lines) { traySlots(lines) }
    val empty = lines.isEmpty()

    // Cada pieza cae con su propio resorte; las que ya estaban al abrir caen escalonadas.
    val drops = remember { mutableStateMapOf<String, Animatable<Float, AnimationVector1D>>() }
    val initialCount = remember { slots.size }
    slots.forEachIndexed { index, slot ->
        key(slot.id) {
            val drop = remember { Animatable(if (reduceMotion) 1f else 0f) }
            DisposableEffect(drop) {
                drops[slot.id] = drop
                onDispose { drops.remove(slot.id) }
            }
            LaunchedEffect(drop) {
                if (reduceMotion) return@LaunchedEffect
                if (index < initialCount) delay(120L + index * 80L)
                drop.animateTo(1f, spring(dampingRatio = 0.48f, stiffness = 240f))
            }
        }
    }

    // Ojos felices un momento cada vez que llega algo a la bandeja.
    var happy by remember { mutableStateOf(false) }
    var lastCount by remember { mutableIntStateOf(slots.size) }
    LaunchedEffect(slots.size) {
        val grew = slots.size > lastCount
        lastCount = slots.size
        if (!grew) {
            happy = false
            return@LaunchedEffect
        }
        happy = true
        delay(1300)
        happy = false
    }

    val idle = rememberInfiniteTransition(label = "counter_vaini")
    val blink by idle.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                keyframes {
                    durationMillis = 3900
                    1f at 3300
                    0.06f at 3380
                    1f at 3490
                },
                RepeatMode.Restart,
            ),
        label = "blink",
    )
    val look by idle.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec =
            infiniteRepeatable(
                keyframes {
                    durationMillis = 4600
                    0f at 0
                    -1f at 500
                    -1f at 1500
                    1f at 2100
                    1f at 3200
                    0f at 3700
                },
                RepeatMode.Restart,
            ),
        label = "look",
    )
    val bob by idle.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec =
            infiniteRepeatable(
                keyframes {
                    durationMillis = 2000
                    0f at 0
                    1f at 1000
                    0f at 2000
                },
                RepeatMode.Restart,
            ),
        label = "bob",
    )

    VainiversoBackdrop(sheetTop = sheetTop) {
        drawCounter(
            stage = size,
            textMeasurer = textMeasurer,
            c = palette,
            m =
                CounterMotion(
                    blink = if (reduceMotion) 1f else blink,
                    lookX = if (empty) (if (reduceMotion) 0f else look) else 0.7f,
                    happy = happy,
                    questionBob = if (reduceMotion) 0.5f else bob,
                    showQuestion = empty,
                    items = slots.map { it.kind to (drops[it.id]?.value ?: 1f) },
                    total = totalLabel,
                ),
        )
    }
}

private fun DrawScope.drawCounter(
    stage: Size,
    textMeasurer: TextMeasurer,
    c: ScenePalette,
    m: CounterMotion,
) {
    val w = stage.width
    val h = stage.height
    val hair = 1.dp.toPx()
    val counterTop = h * COUNTER_TOP_Y

    // Pared con azulejos abajo y piso.
    drawRect(c.facadeLight, size = stage)
    val tileTop = h * 0.205f
    drawRect(c.facade, topLeft = Offset(0f, tileTop), size = Size(w, counterTop - tileTop))
    val tile = w / 14f
    var tx = 0f
    while (tx < w) {
        drawLine(c.facadeShade.copy(alpha = 0.45f), Offset(tx, tileTop), Offset(tx, counterTop), strokeWidth = hair)
        tx += tile
    }
    var ty = tileTop
    while (ty < counterTop) {
        drawLine(c.facadeShade.copy(alpha = 0.45f), Offset(0f, ty), Offset(w, ty), strokeWidth = hair)
        ty += tile * 0.6f
    }
    drawRect(c.sidewalk, topLeft = Offset(0f, h * COUNTER_BOTTOM_Y), size = Size(w, h * (1f - COUNTER_BOTTOM_Y)))

    // Lámparas colgantes.
    for (lx in listOf(0.5f)) {
        val x = w * lx
        val shadeY = h * 0.055f
        drawCircle(
            Brush.radialGradient(listOf(c.lampGlow, Color.Transparent), Offset(x, shadeY + hair * 6), w * 0.2f),
            radius = w * 0.2f,
            center = Offset(x, shadeY + hair * 6),
        )
        drawLine(c.signInk, Offset(x, 0f), Offset(x, shadeY), strokeWidth = hair)
        drawArc(
            c.signInk,
            180f,
            180f,
            true,
            topLeft = Offset(x - w * 0.045f, shadeY - hair * 2),
            size =
                Size(
                    w * 0.09f,
                    w * 0.06f,
                ),
        )
        drawCircle(c.windowLit, radius = hair * 3.5f, center = Offset(x, shadeY + w * 0.028f))
    }

    // Repisa con frascos, tazas y una planta.
    val shelfY = h * 0.135f
    val shelfX = w * 0.17f
    drawRect(c.shopShelf, topLeft = Offset(shelfX - w * 0.03f, shelfY), size = Size(w * 0.31f, hair * 5))
    for (bx in listOf(0.03f, 0.23f)) {
        drawRect(c.facadeShade, topLeft = Offset(shelfX + w * bx, shelfY + hair * 5), size = Size(hair * 3, hair * 10))
    }

    fun jar(
        cx: Float,
        fill: Color,
    ) {
        val jw = w * 0.07f
        val jh = w * 0.085f
        drawRoundRect(
            c.windowGlassLight.copy(alpha = 0.75f),
            Offset(cx - jw / 2f, shelfY - jh),
            Size(jw, jh),
            CornerRadius(
                hair * 5,
            ),
        )
        drawRoundRect(
            fill,
            Offset(cx - jw / 2f + hair * 2, shelfY - jh * 0.55f),
            Size(
                jw - hair * 4,
                jh * 0.55f - hair * 2,
            ),
            CornerRadius(hair * 4),
        )
        drawRoundRect(
            c.pot,
            Offset(cx - jw * 0.42f, shelfY - jh - hair * 4),
            Size(jw * 0.84f, hair * 5),
            CornerRadius(
                hair * 2,
            ),
        )
    }
    jar(shelfX + w * 0.03f, CookieDough)
    drawRoundRect(
        CounterCream,
        Offset(shelfX + w * 0.085f, shelfY - w * 0.05f),
        Size(w * 0.05f, w * 0.05f),
        CornerRadius(
            hair * 3,
        ),
    )
    drawRoundRect(
        CounterLime,
        Offset(shelfX + w * 0.145f, shelfY - w * 0.05f),
        Size(w * 0.05f, w * 0.05f),
        CornerRadius(
            hair * 3,
        ),
    )
    jar(shelfX + w * 0.24f, CookieChip.copy(alpha = 0.85f))

    // Pizarra: el total del carrito escrito con gis.
    val boardLeft = w * 0.56f
    val boardTop = h * 0.105f
    val boardSize = Size(w * 0.37f, h * 0.115f)
    drawRoundRect(
        c.shopShelf,
        Offset(boardLeft - hair * 4, boardTop - hair * 4),
        Size(
            boardSize.width + hair * 8,
            boardSize.height + hair * 8,
        ),
        CornerRadius(hair * 6),
    )
    drawRoundRect(c.signInk, Offset(boardLeft, boardTop), boardSize, CornerRadius(hair * 3))
    val chalk = c.signText
    val small =
        textMeasurer.measure(
            "TU BANDEJA",
            TextStyle(
                color = chalk.copy(alpha = 0.75f),
                fontSize = (w * 0.03f / density / fontScale).sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            ),
        )
    drawText(
        small,
        topLeft =
            Offset(
                boardLeft + (boardSize.width - small.size.width) / 2f,
                boardTop + boardSize.height * 0.14f,
            ),
    )
    val big =
        textMeasurer.measure(
            m.total ?: "vacía",
            TextStyle(
                color = chalk,
                fontSize = (w * (if (m.total == null) 0.06f else 0.075f) / density / fontScale).sp,
                fontFamily = VaiinillaSerif,
                fontWeight = FontWeight.Bold,
            ),
        )
    drawText(
        big,
        topLeft =
            Offset(
                boardLeft + (boardSize.width - big.size.width) / 2f,
                boardTop + boardSize.height * 0.38f,
            ),
    )
    drawLine(
        chalk.copy(alpha = 0.35f),
        Offset(boardLeft + boardSize.width * 0.2f, boardTop + boardSize.height * 0.88f),
        Offset(
            boardLeft + boardSize.width * 0.8f,
            boardTop + boardSize.height * 0.88f,
        ),
        strokeWidth = hair * 1.5f,
        cap = StrokeCap.Round,
    )

    // Vaini detrás del mostrador: asoma de la cintura para arriba y el mostrador le tapa las piernas.
    // Vaini a la izquierda y la bandeja a su derecha, bajo la pizarra: así lo que pides no le tapa la cara.
    val box = w * 0.34f
    val vainiX = w * 0.3f
    val vainiTop = counterTop - box * 0.8f
    withTransform({ translate(vainiX - box / 2f, vainiTop) }) {
        drawVainiStanding(
            boxPx = box,
            paperColor = CounterCream,
            accentColor = CounterVainiLime,
            inkColor = CounterVainiInk,
            blink = m.blink,
            happy = m.happy,
            lookX = m.lookX,
        )
    }
    if (m.showQuestion) {
        val q =
            textMeasurer.measure(
                "?",
                TextStyle(
                    color = CounterLime,
                    fontSize = (w * 0.09f / density / fontScale).sp,
                    fontFamily = VaiinillaSerif,
                    fontWeight = FontWeight.Bold,
                ),
            )
        val qy = vainiTop + box * 0.05f - m.questionBob * hair * 6
        drawText(q, topLeft = Offset(vainiX + box * 0.42f, qy))
    }

    // Mostrador: cubierta de madera y frente a rayas lima, como el toldo de la tienda.
    val frontTop = counterTop + h * 0.012f
    drawRect(c.shopShelf, topLeft = Offset(0f, counterTop - h * 0.004f), size = Size(w, h * 0.016f))
    drawLine(
        Color.White.copy(alpha = 0.25f),
        Offset(0f, counterTop - h * 0.004f),
        Offset(w, counterTop - h * 0.004f),
        strokeWidth =
            hair * 1.5f,
    )
    val stripes = 16
    val stripe = w / stripes
    for (i in 0 until stripes) {
        drawRect(
            if (i % 2 ==
                0
            ) {
                c.awning
            } else {
                c.awningDark
            },
            topLeft = Offset(i * stripe, frontTop),
            size =
                Size(
                    stripe + 0.5f,
                    h * COUNTER_BOTTOM_Y - frontTop,
                ),
        )
    }
    drawRect(Color(0x26000000), topLeft = Offset(0f, frontTop), size = Size(w, hair * 3))
    drawRect(c.doorGreen, topLeft = Offset(0f, h * COUNTER_BOTTOM_Y - h * 0.01f), size = Size(w, h * 0.01f))

    // Bandeja sobre el mostrador y lo que pediste encima.
    val trayW = w * 0.48f
    val trayX = w * 0.71f
    val trayH = h * 0.012f
    val trayTop = counterTop - trayH
    drawRoundRect(
        Color(0x33000000),
        Offset(trayX - trayW / 2f + hair * 2, trayTop + hair * 3),
        Size(trayW, trayH),
        CornerRadius(
            trayH / 2f,
        ),
    )
    drawRoundRect(c.pot, Offset(trayX - trayW / 2f, trayTop), Size(trayW, trayH), CornerRadius(trayH / 2f))
    drawLine(
        Color.White.copy(alpha = 0.22f),
        Offset(trayX - trayW / 2f + trayH, trayTop + hair),
        Offset(
            trayX + trayW / 2f - trayH,
            trayTop + hair,
        ),
        strokeWidth = hair,
    )
    val unit = w * 0.088f
    val n = m.items.size
    val spacing = minOf(w * 0.092f, (trayW - unit) / (n - 1).coerceAtLeast(1))
    m.items.forEachIndexed { i, (kind, drop) ->
        if (drop <= 0f) return@forEachIndexed
        val cx = trayX + (i - (n - 1) / 2f) * spacing
        val base = trayTop - (1f - drop) * h * 0.16f
        val landed = (drop - 0.85f).coerceAtLeast(0f) / 0.15f
        drawOval(
            Color(0x2A000000).copy(alpha = 0.16f * landed),
            Offset(cx - unit * 0.4f, trayTop - hair * 2),
            Size(
                unit * 0.8f,
                hair * 4,
            ),
        )
        drawTrayItem(kind, cx, base, unit, alpha = (drop * 3f).coerceIn(0f, 1f))
    }
}

internal fun DrawScope.drawTrayItem(
    kind: TrayItemKind,
    cx: Float,
    base: Float,
    u: Float,
    alpha: Float,
) {
    val hair = 1.dp.toPx()
    when (kind) {
        TrayItemKind.DRINK -> {
            val top = base - u * 1.05f
            val cup =
                Path().apply {
                    moveTo(cx - u * 0.34f, top + u * 0.12f)
                    lineTo(cx + u * 0.34f, top + u * 0.12f)
                    lineTo(cx + u * 0.25f, base)
                    lineTo(cx - u * 0.25f, base)
                    close()
                }
            drawPath(cup, CounterCream, alpha = alpha)
            drawRect(
                CounterLime,
                topLeft = Offset(cx - u * 0.3f, top + u * 0.42f),
                size = Size(u * 0.6f, u * 0.26f),
                alpha = alpha,
            )
            drawRoundRect(
                CupLid,
                Offset(cx - u * 0.39f, top),
                Size(u * 0.78f, u * 0.14f),
                CornerRadius(hair * 2),
                alpha = alpha,
            )
            drawLine(
                CounterLime,
                Offset(cx + u * 0.06f, top),
                Offset(cx + u * 0.2f, top - u * 0.32f),
                strokeWidth =
                    hair * 3,
                cap = StrokeCap.Round,
                alpha = alpha,
            )
        }
        TrayItemKind.PASTRY -> {
            val r = u * 0.38f
            val center = Offset(cx, base - r)
            drawCircle(CookieDough, radius = r, center = center, alpha = alpha)
            drawCircle(
                Color.Black.copy(alpha = 0.08f),
                radius = r,
                center = center + Offset(0f, hair * 2),
                alpha = alpha,
                style =
                    Stroke(
                        hair * 2,
                    ),
            )
            for ((dx, dy) in listOf(
                -0.15f to -0.12f,
                0.14f to -0.18f,
                0.02f to 0.1f,
                -0.18f to 0.16f,
                0.18f to 0.12f,
            )) {
                drawCircle(CookieChip, radius = u * 0.05f, center = center + Offset(dx * u, dy * u), alpha = alpha)
            }
        }
        TrayItemKind.PLATE -> {
            val plateH = u * 0.2f
            drawArc(
                PlateFood,
                180f,
                180f,
                true,
                Offset(cx - u * 0.34f, base - plateH * 0.6f - u * 0.34f),
                Size(
                    u * 0.68f,
                    u * 0.68f,
                ),
                alpha = alpha,
            )
            drawCircle(
                PlateLeaf,
                radius = u * 0.07f,
                center = Offset(cx + u * 0.08f, base - plateH * 0.6f - u * 0.3f),
                alpha = alpha,
            )
            drawOval(PlateRim, Offset(cx - u * 0.5f, base - plateH), Size(u, plateH), alpha = alpha)
        }
    }
}

// Al pagar: la caja registradora está sobre el mostrador, a la derecha, fuera de cuadro en el carrito. Medidas en
// anchos y altos de pantalla, como `CartCounterScene.swift` en iOS.

private const val REGISTER_CENTER_X = 1.34f
private const val REGISTER_WIDTH = 0.56f
private const val REGISTER_HEIGHT = 0.095f
private val LcdGreen = Color(0xFF2E3B1E)

/** El cuerpo de la caja, en coordenadas de la escena. */
private fun registerRect(stage: Size): Rect {
    val w = stage.width * REGISTER_WIDTH
    val h = stage.height * REGISTER_HEIGHT
    val left = stage.width * REGISTER_CENTER_X - w / 2f
    val bottom = stage.height * COUNTER_TOP_Y
    return Rect(left, bottom - h, left + w, bottom)
}

/** La ranura por donde sale el ticket, arriba del cuerpo, a la izquierda de la pantallita. */
internal fun registerSlot(stage: Size): Rect {
    val body = registerRect(stage)
    val left = body.left + stage.width * 0.03f
    val top = body.top + stage.height * 0.004f
    return Rect(left, top, left + stage.width * 0.32f, top + stage.height * 0.008f)
}

/** Del mostrador como en el carrito ([t] = 0) a la caja al centro y un poco más cerca ([t] = 1). */
internal fun checkoutCamera(
    t: Float,
    stage: Size,
): SceneCamera {
    val slot = registerSlot(stage)
    return SceneCamera.flight(
        focus = Offset(slot.center.x, slot.top),
        target = Offset(stage.width * 0.5f, stage.height * 0.56f),
        zoom = 1.1f,
        t = t,
    )
}

/** El mostrador al pagar: todo ya en la bandeja, la cubierta sigue hasta la caja y la caja muestra el total. */
internal fun DrawScope.drawCheckoutCounter(
    stage: Size,
    textMeasurer: TextMeasurer,
    c: ScenePalette,
    items: List<TrayItemKind>,
    total: String,
    happy: Boolean,
) {
    val w = stage.width
    val h = stage.height
    val hair = 1.dp.toPx()
    val counterTop = h * COUNTER_TOP_Y
    val tileTop = h * 0.205f
    // Arriba de la escena (la cámara baja al acercarse a la caja) sigue la misma pared lisa.
    // Encima del mostrador para que no se vea la orilla de la escena entre las dos.
    drawRect(c.facadeLight, topLeft = Offset(-w, -h), size = Size(w * 4f, h + tileTop))
    drawCounter(
        stage = stage,
        textMeasurer = textMeasurer,
        c = c,
        m =
            CounterMotion(
                blink = 1f,
                lookX = 0.9f,
                happy = happy,
                questionBob = 0f,
                showQuestion = false,
                items = items.map { it to 1f },
                total = total,
            ),
    )

    // La pared sigue a la derecha, donde está la caja; empieza un pelo antes para que no quede costura.
    val bayLeft = w - hair
    val bayWidth = w * 0.9f
    drawRect(c.facadeLight, topLeft = Offset(bayLeft, 0f), size = Size(bayWidth, tileTop))
    drawRect(c.facade, topLeft = Offset(bayLeft, tileTop), size = Size(bayWidth, counterTop - tileTop))
    val tile = w / 14f
    var tx = bayLeft
    while (tx < bayLeft + bayWidth) {
        drawLine(c.facadeShade.copy(alpha = 0.45f), Offset(tx, tileTop), Offset(tx, counterTop), strokeWidth = hair)
        tx += tile
    }
    var ty = tileTop
    while (ty < counterTop) {
        drawLine(
            c.facadeShade.copy(alpha = 0.45f),
            Offset(bayLeft, ty),
            Offset(bayLeft + bayWidth, ty),
            strokeWidth = hair,
        )
        ty += tile * 0.6f
    }
    drawRect(
        c.sidewalk,
        topLeft = Offset(bayLeft, h * COUNTER_BOTTOM_Y),
        size = Size(bayWidth, h * (1f - COUNTER_BOTTOM_Y)),
    )

    // Letrero de gis sobre la caja.
    val body = registerRect(stage)
    val sign = Rect(body.center.x - w * 0.13f, h * 0.1f, body.center.x + w * 0.13f, h * 0.15f)
    for (fraction in listOf(0.2f, 0.8f)) {
        val x = sign.left + sign.width * fraction
        drawLine(c.signInk, Offset(x, h * 0.06f), Offset(x, sign.top), strokeWidth = hair)
    }
    val frame = sign.inflate(hair * 3)
    drawRoundRect(c.shopShelf, frame.topLeft, frame.size, CornerRadius(hair * 5))
    drawRoundRect(c.signInk, sign.topLeft, sign.size, CornerRadius(hair * 3))
    val label =
        textMeasurer.measure(
            "CAJA",
            TextStyle(
                color = c.signText,
                fontSize = (sign.height * 0.5f / density / fontScale).sp,
                fontFamily = VaiinillaSerif,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            ),
        )
    drawText(
        label,
        topLeft = Offset(sign.center.x - label.size.width / 2f, sign.center.y - label.size.height / 2f),
    )

    // La cubierta y el frente a rayas siguen hasta la caja.
    val frontTop = counterTop + h * 0.012f
    drawRect(c.shopShelf, topLeft = Offset(bayLeft, counterTop - h * 0.004f), size = Size(bayWidth, h * 0.016f))
    drawLine(
        Color.White.copy(alpha = 0.25f),
        Offset(bayLeft, counterTop - h * 0.004f),
        Offset(bayLeft + bayWidth, counterTop - h * 0.004f),
        strokeWidth = hair * 1.5f,
    )
    val stripe = w / 16f
    var i = 16
    while (i * stripe < bayLeft + bayWidth) {
        drawRect(
            if (i % 2 == 0) c.awning else c.awningDark,
            topLeft = Offset(i * stripe, frontTop),
            size = Size(stripe + 0.5f, h * COUNTER_BOTTOM_Y - frontTop),
        )
        i++
    }
    drawRect(Color(0x26000000), topLeft = Offset(bayLeft, frontTop), size = Size(bayWidth, hair * 3))
    drawRect(
        c.doorGreen,
        topLeft = Offset(bayLeft, h * COUNTER_BOTTOM_Y - h * 0.01f),
        size = Size(bayWidth, h * 0.01f),
    )

    // La caja: cuerpo oscuro, teclas, cajón, la ranura del ticket y la pantallita con el total.
    drawOval(
        Color.Black.copy(alpha = 0.14f),
        topLeft = Offset(body.left - hair * 4, body.bottom - hair * 4),
        size = Size(body.width + hair * 8, hair * 9),
    )
    drawRoundRect(c.signInk, body.topLeft, body.size, CornerRadius(hair * 9))
    drawRoundRect(
        Color.White.copy(alpha = 0.14f),
        Offset(body.left + hair * 6, body.top + hair),
        Size(body.width - hair * 12, hair * 3),
        CornerRadius(hair),
    )
    val keysTop = body.top + body.height * 0.26f
    val key = body.width * 0.075f
    for (row in 0 until 2) {
        for (col in 0 until 5) {
            drawRoundRect(
                if (col == 4 && row == 1) SceneLime else CounterCream.copy(alpha = 0.9f),
                Offset(body.left + body.width * 0.07f + col * key * 1.32f, keysTop + row * key * 0.92f),
                Size(key, key * 0.62f),
                CornerRadius(hair * 2),
            )
        }
    }
    val drawerTop = body.top + body.height * 0.7f
    drawLine(
        Color.White.copy(alpha = 0.16f),
        Offset(body.left + hair * 4, drawerTop),
        Offset(body.right - hair * 4, drawerTop),
        strokeWidth = hair,
    )
    drawRoundRect(
        Color.White.copy(alpha = 0.3f),
        Offset(body.center.x - body.width * 0.08f, drawerTop + body.height * 0.1f),
        Size(body.width * 0.16f, hair * 4),
        CornerRadius(hair * 2),
    )
    val slot = registerSlot(stage)
    drawRoundRect(Color.Black.copy(alpha = 0.6f), slot.topLeft, slot.size, CornerRadius(hair * 2))

    val display =
        Rect(
            body.right - body.width * 0.3f,
            body.top - h * 0.052f,
            body.right - body.width * 0.04f,
            body.top - h * 0.01f,
        )
    drawRect(
        c.signInk,
        topLeft = Offset(display.center.x - hair * 3, display.bottom),
        size = Size(hair * 6, body.top - display.bottom),
    )
    drawRoundRect(c.signInk, display.topLeft, display.size, CornerRadius(hair * 5))
    val lcd = display.deflate(hair * 4)
    drawRoundRect(LcdGreen, lcd.topLeft, lcd.size, CornerRadius(hair * 2))
    val amount =
        textMeasurer.measure(
            total,
            TextStyle(
                color = SceneLime,
                fontSize = (lcd.height * 0.62f / density / fontScale).sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            ),
        )
    drawText(
        amount,
        topLeft = Offset(lcd.right - hair * 4 - amount.size.width, lcd.center.y - amount.size.height / 2f),
    )
}
