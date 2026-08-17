package com.vaiinilla.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaColors

private val TaskCardDark = Color(0xFF1C1D1B)
private val TaskCardText = Color(0xFFF5F2E8)
private val TrackEase = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

private data class TimelineStep(
    val state: OrderState,
    val title: (PaymentMethod) -> String,
    val description: (OrderDestination, PaymentMethod) -> String,
)

private val timelineSteps =
    listOf(
        TimelineStep(OrderState.PENDING_PAYMENT, { payment ->
            if (payment != PaymentMethod.CASH) "PAGO CONFIRMADO" else "POR COBRAR"
        }) { _, payment ->
            if (payment != PaymentMethod.CASH) {
                "Saldo descontado y pedido enviado."
            } else {
                "Caja espera el pago en efectivo."
            }
        },
        TimelineStep(OrderState.PAID, { _ -> "COBRADO" }) { _, _ ->
            "Cocina recibió la comanda."
        },
        TimelineStep(OrderState.PREPARING, { _ -> "PREPARANDO" }) { _, _ ->
            "Tu comida se está preparando."
        },
        TimelineStep(OrderState.READY, { _ -> "LISTO" }) { destination, _ ->
            if (destination == OrderDestination.IN_SPACE) {
                "El mesero lo llevará a tu mesa."
            } else {
                "Recógelo en la barra."
            }
        },
        TimelineStep(OrderState.DELIVERED, { _ -> "ENTREGADO" }) { _, _ ->
            "Pedido completado."
        },
    )

private fun destinationDisplayLabel(order: OrderDetail): String =
    order.summary.space?.name ?: order.summary.destination.label

@Composable
fun OrderTrackingCard(
    order: OrderDetail,
    modifier: Modifier = Modifier,
    showEyebrow: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val colors = LocalVaiinillaColors.current
    val reduceMotion = reducedMotion()
    val isReady = order.summary.state == OrderState.READY
    val cardBg by animateColorAsState(
        targetValue = if (isReady) colors.yolk else TaskCardDark,
        animationSpec = tween(if (reduceMotion) 0 else 550, easing = TrackEase),
        label = "tracking-card-bg",
    )
    val cardText by animateColorAsState(
        targetValue = if (isReady) colors.ink else TaskCardText,
        animationSpec = tween(if (reduceMotion) 0 else 550, easing = TrackEase),
        label = "tracking-card-text",
    )
    val badgeBg = if (isReady) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.45f)
    val clickableModifier =
        if (onClick != null) {
            modifier.physicalPress(onClick = onClick)
        } else {
            modifier
        }
    Surface(
        modifier = clickableModifier.fillMaxWidth(),
        color = cardBg,
        shape = RoundedCornerShape(26.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    if (showEyebrow) {
                        Text(
                            text = "Pedido actual",
                            color = cardText.copy(alpha = 0.62f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp,
                        )
                    }
                    Text(
                        text = "#${order.summary.folio}",
                        color = cardText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(top = if (showEyebrow) 4.dp else 0.dp),
                    )
                }
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text =
                            order.summary.state.label
                                .uppercase(),
                        color = colors.ink,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(moneyLabel(order.summary.total), color = cardText.copy(alpha = 0.82f), fontSize = 13.sp)
                Text(destinationDisplayLabel(order), color = cardText.copy(alpha = 0.82f), fontSize = 13.sp)
                Text(
                    paymentMethodLabel(order.summary.paymentMethod),
                    color = cardText.copy(alpha = 0.82f),
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
fun OrderTrackingTimeline(
    current: OrderState,
    destination: OrderDestination = OrderDestination.TAKE_AWAY,
    paymentMethod: PaymentMethod = PaymentMethod.CASH,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val view = LocalView.current
    val currentIndex = current.trackingIndex
    var lastIndex by remember { mutableIntStateOf(currentIndex) }
    LaunchedEffect(currentIndex) {
        if (currentIndex > lastIndex) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
        lastIndex = currentIndex
    }
    Column(modifier = modifier) {
        timelineSteps.forEachIndexed { index, step ->
            val stepIndex = step.state.trackingIndex
            TimelineRow(
                stepNumber = index + 1,
                title = step.title(paymentMethod),
                description = step.description(destination, paymentMethod),
                isDone = stepIndex < currentIndex,
                isCurrent = stepIndex == currentIndex,
                showLine = index < timelineSteps.lastIndex,
                staggerMs = index * 70,
                colors = colors,
            )
        }
    }
}

@Composable
private fun TimelineRow(
    stepNumber: Int,
    title: String,
    description: String,
    isDone: Boolean,
    isCurrent: Boolean,
    showLine: Boolean,
    staggerMs: Int,
    colors: VaiinillaColors,
) {
    val reduceMotion = reducedMotion()
    val duration = if (reduceMotion) 0 else 520
    val spec = tween<Color>(duration, delayMillis = if (reduceMotion) 0 else staggerMs, easing = TrackEase)
    val floatSpec = tween<Float>(duration, delayMillis = if (reduceMotion) 0 else staggerMs, easing = TrackEase)
    val circleColor by animateColorAsState(
        targetValue =
            when {
                isCurrent -> colors.yolk
                isDone -> colors.accent
                else -> colors.paper2
            },
        animationSpec = spec,
        label = "track-circle",
    )
    val titleColor by animateColorAsState(
        targetValue = if (isDone || isCurrent) colors.ink else colors.muted,
        animationSpec = spec,
        label = "track-title",
    )
    val lineFill by animateFloatAsState(
        targetValue = if (isDone) 1f else 0f,
        animationSpec = floatSpec,
        label = "track-line",
    )
    val checkScale by animateFloatAsState(
        targetValue = if (isDone || isCurrent) 1f else 0.72f,
        animationSpec = floatSpec,
        label = "track-check",
    )
    val infinite = rememberInfiniteTransition(label = "track-halo")
    val pulse by infinite.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.18f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(900, easing = TrackEase),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "track-halo-scale",
    )
    val haloPulse = if (isCurrent && !reduceMotion) pulse else 1f
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(38.dp)) {
                if (isCurrent) {
                    Box(
                        modifier =
                            Modifier
                                .size(38.dp)
                                .graphicsLayer {
                                    scaleX = haloPulse
                                    scaleY = haloPulse
                                    alpha = 0.45f
                                }.clip(CircleShape)
                                .background(colors.yolk.copy(alpha = 0.35f)),
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .size(28.dp)
                            .graphicsLayer {
                                scaleX = checkScale
                                scaleY = checkScale
                            }.clip(CircleShape)
                            .background(circleColor),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isDone || isCurrent) {
                        Text("✓", color = colors.ink, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    } else {
                        Text(
                            stepNumber.toString(),
                            color = colors.muted,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
            if (showLine) {
                Box(
                    modifier =
                        Modifier
                            .width(2.dp)
                            .height(40.dp)
                            .background(colors.paper2),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(lineFill)
                                .align(Alignment.TopCenter)
                                .background(colors.accent),
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.padding(top = 4.dp, bottom = if (showLine) 8.dp else 0.dp)) {
            Text(
                title,
                color = titleColor,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
            )
            Text(description, color = colors.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
fun OrderDetailSummary(
    order: OrderDetail,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = TaskCardDark,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier =
                Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 12.dp,
                ),
        ) {
            order.items.forEachIndexed { index, item ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${item.quantity} × ${item.productName}",
                        color = TaskCardText.copy(alpha = 0.86f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = moneyLabel(item.subtotal),
                        color = TaskCardText,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
                if (index < order.items.lastIndex) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(TaskCardText.copy(alpha = 0.12f)),
                    )
                }
            }
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(TaskCardText.copy(alpha = 0.16f)),
            )
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Total",
                    color = TaskCardText,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                )
                Text(
                    moneyLabel(order.summary.total),
                    color = TaskCardText,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                )
            }
        }
    }
}
