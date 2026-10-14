package com.vaiinilla.app.ui.screens

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.order.orderTraySlots
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

// Pagar en el Vainiverso es la caja, como `CheckoutJourney.swift` en iOS: del carrito al pedido es una sola toma. La
// hoja del carrito baja, la cámara se corre por el mostrador hasta la caja registradora, la caja imprime el ticket y
// el ticket sube a volverse la tarjeta de "¡Pedido recibido!" (la de `PickupJourneyScreen`), que después cae a la
// hoja de estado.

/** Hasta dónde llega el primer resorte (imprimir); el segundo lleva el ticket a la tarjeta. */
internal const val CHECKOUT_PRINTED = 0.6f

private val TicketInk = Color(0xFF1A1414)

/**
 * El tramo de la caja. [t] va de 0 (como el carrito: la hoja arriba, el mostrador quieto) a [CHECKOUT_PRINTED]
 * (el ticket impreso) y a 1 (el ticket ya es la tarjeta en [cardRect], en coordenadas de la pantalla).
 */
@Composable
internal fun CheckoutStage(
    t: Float,
    order: OrderDetail,
    cardRect: Rect?,
    reduceMotion: Boolean,
) {
    val palette = LocalScene.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val total = moneyLabel(order.summary.total)
    val kinds = remember(order.items) { orderTraySlots(order.items).map { it.kind } }
    val happy = t > CHECKOUT_PRINTED - 0.04f
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .clearAndSetSemantics {},
    ) {
        val stage = with(density) { Size(maxWidth.toPx(), maxHeight.toPx()) }
        val camera = checkoutCamera(SceneCurve.eased(t, 0.04f, 0.36f), stage)
        val counterAlpha = 1f - SceneCurve.eased(t, 0.68f, 0.92f)
        val counterBlur = if (reduceMotion) 0f else 18f * SceneCurve.eased(t, 0.6f, 0.86f)
        val sheetAway = SceneCurve.eased(t, 0f, 0.22f)
        val sheetTop = stage.height * cartSceneWindow(order.items.size)
        val sheetY = sheetTop + (stage.height - sheetTop + with(density) { 60.dp.toPx() }) * sheetAway
        val drawCounter: DrawScope.() -> Unit = {
            withTransform({
                translate(camera.translation.x, camera.translation.y)
                scale(camera.zoom, camera.zoom, pivot = Offset.Zero)
            }) { drawCheckoutCounter(stage, textMeasurer, palette, kinds, total, happy) }
        }
        if (counterAlpha > 0.001f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = counterAlpha
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && counterBlur > 0.5f) {
                            val px = counterBlur * density.density
                            renderEffect = BlurEffect(px, px, TileMode.Clamp)
                        }
                    },
            ) {
                // Igual que el carrito: nítido arriba de la hoja y desenfocado debajo de ella.
                if (sheetAway < 1f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Canvas(Modifier.fillMaxSize().blur(24.dp)) { drawCounter() }
                }
                Canvas(Modifier.fillMaxSize()) {
                    clipRect(bottom = if (sheetAway < 1f) sheetY else size.height) { drawCounter() }
                }
            }
        }
        if (sheetAway < 1f) {
            val shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = sheetY }
                    .clip(shape)
                    .background(palette.glassTint)
                    .border(1.dp, palette.glassEdge, shape),
            )
        }
        CheckoutTicketFlight(
            t = t,
            order = order,
            stage = stage,
            camera = camera,
            cardRect = cardRect,
            reduceMotion = reduceMotion,
        )
    }
}

/** El ticket: sale de la ranura mientras se imprime y después vuela, se ensancha y se vuelve vidrio. */
@Composable
private fun CheckoutTicketFlight(
    t: Float,
    order: OrderDetail,
    stage: Size,
    camera: SceneCamera,
    cardRect: Rect?,
    reduceMotion: Boolean,
) {
    val palette = LocalScene.current
    val density = LocalDensity.current
    var ticketHeight by remember { mutableFloatStateOf(with(density) { 150.dp.toPx() }) }
    val printed = SceneCurve.segment(t, 0.28f, CHECKOUT_PRINTED)
    if (printed <= 0f) return
    val slotScene = registerSlot(stage)
    val slot =
        Rect(camera.toScreen(slotScene.topLeft), camera.toScreen(slotScene.bottomRight))
    val inset = with(density) { 4.dp.toPx() }
    val width = slot.width - inset * 2
    val printedRect = Rect(slot.left + inset, slot.top - ticketHeight, slot.left + inset + width, slot.top)
    val card =
        cardRect ?: with(density) {
            Rect(20.dp.toPx(), stage.height * 0.3f, stage.width - 20.dp.toPx(), stage.height * 0.72f)
        }
    val morph = SceneCurve.eased(t, CHECKOUT_PRINTED + 0.02f, 0.9f)
    val rect = lerpRect(printedRect, card, morph)
    // Sigue siendo papel mientras vuela; se vuelve vidrio ya del tamaño de la tarjeta.
    val glass = SceneCurve.segment(t, 0.86f, 0.97f)
    val words = SceneCurve.segment(t, 0.74f, 0.88f)
    // Se arranca con un saltito y se endereza mientras vuela.
    val hop = with(density) { -10.dp.toPx() } * sin(PI.toFloat() * SceneCurve.segment(t, CHECKOUT_PRINTED, CHECKOUT_PRINTED + 0.1f))
    val tilt = -3f * sin(PI.toFloat() * SceneCurve.segment(t, CHECKOUT_PRINTED, 0.9f))
    val lift = sin(PI.toFloat() * morph)
    val radius = SceneCurve.lerp(3f, 32f, morph).dp
    val glassShape = RoundedCornerShape(radius)
    val clipBottom = if (morph > 0f) stage.height else slot.top

    Box(
        Modifier
            .fillMaxSize()
            // Mientras se imprime, la parte que falta sigue dentro de la caja, debajo de la ranura.
            .drawWithContent { clipRect(bottom = clipBottom) { this@drawWithContent.drawContent() } }
            .graphicsLayer { alpha = 1f - SceneCurve.segment(t, 0.97f, 1f) },
    ) {
        Box(
            Modifier
                .offset {
                    IntOffset(
                        rect.left.roundToInt(),
                        (rect.top + hop + ticketHeight * (1f - printed)).roundToInt(),
                    )
                }.size(with(density) { rect.width.toDp() }, with(density) { rect.height.toDp() })
                .graphicsLayer { rotationZ = tilt },
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 1f - glass }
                    .shadow(
                        elevation = (6f + 14f * lift).dp,
                        shape = RoundedCornerShape(radius),
                        ambientColor = Color.Black.copy(alpha = 0.12f + 0.1f * lift),
                        spotColor = Color.Black.copy(alpha = 0.12f + 0.1f * lift),
                        clip = false,
                    ),
            ) {
                Canvas(Modifier.fillMaxSize()) { drawPath(ticketPaper(size, 6.dp.toPx()), QrPaper) }
            }
            if (glass > 0f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = glass }
                        .clip(glassShape)
                        .background(palette.glassTint)
                        .border(1.dp, palette.glassEdge, glassShape),
                )
            }
            CheckoutTicket(
                order = order,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .requiredWidth(with(density) { width.toDp() })
                        .onSizeChanged { ticketHeight = it.height.toFloat() }
                        .graphicsLayer {
                            alpha = 1f - words
                            val scale = 1f - 0.08f * words
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0.5f, 0f)
                            val soft = if (reduceMotion) 0f else 6f * words * density.density
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && soft > 0.5f) {
                                renderEffect = BlurEffect(soft, soft, TileMode.Decal)
                            }
                        },
            )
        }
    }
}

private fun lerpRect(
    a: Rect,
    b: Rect,
    t: Float,
): Rect =
    Rect(
        SceneCurve.lerp(a.left, b.left, t),
        SceneCurve.lerp(a.top, b.top, t),
        SceneCurve.lerp(a.right, b.right, t),
        SceneCurve.lerp(a.bottom, b.bottom, t),
    )

/** El papel del ticket con los bordes en zigzag de arriba y de abajo. */
private fun ticketPaper(
    size: Size,
    tooth: Float,
): Path {
    val count = max((size.width / tooth).toInt(), 1)
    val step = size.width / count
    return Path().apply {
        moveTo(0f, tooth / 2f)
        for (i in 0 until count) {
            val x = i * step
            lineTo(x + step / 2f, 0f)
            lineTo(x + step, tooth / 2f)
        }
        lineTo(size.width, size.height - tooth / 2f)
        for (i in 0 until count) {
            val x = size.width - i * step
            lineTo(x - step / 2f, size.height)
            lineTo(x - step, size.height - tooth / 2f)
        }
        close()
    }
}

/** Lo que imprime la caja: el lugar, el folio, lo pedido y el total. */
@Composable
private fun CheckoutTicket(
    order: OrderDetail,
    modifier: Modifier = Modifier,
) {
    val shown = order.items.take(3)
    val mono = TextStyle(color = TicketInk, fontSize = 10.sp, lineHeight = 13.sp, fontFamily = FontFamily.Monospace)
    Column(modifier.padding(start = 10.dp, end = 10.dp, top = 14.dp, bottom = 16.dp)) {
        Text(
            "VAIINILLA",
            style =
                mono.copy(
                    fontSize = 12.sp,
                    fontFamily = VaiinillaSerif,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                ),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(3.dp))
        Text(
            "Pedido #${order.summary.folio}",
            style = mono.copy(textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth(),
        )
        TicketDashes()
        shown.forEach { item ->
            Row(Modifier.padding(vertical = 1.5.dp)) {
                Text(
                    "${item.quantity}× ${item.productName}",
                    style = mono,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(4.dp))
                Text(moneyLabel(item.subtotal), style = mono)
            }
        }
        if (order.items.size > shown.size) {
            Text("+${order.items.size - shown.size} más", style = mono, modifier = Modifier.padding(vertical = 1.5.dp))
        }
        TicketDashes()
        Row {
            Text("TOTAL", style = mono.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
            Text(moneyLabel(order.summary.total), style = mono.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun TicketDashes() {
    Canvas(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .height(1.dp),
    ) {
        val dash = 3.dp.toPx()
        drawLine(
            TicketInk.copy(alpha = 0.35f),
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
        )
    }
}
