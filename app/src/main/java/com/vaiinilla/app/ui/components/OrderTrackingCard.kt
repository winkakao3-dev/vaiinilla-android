package com.vaiinilla.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

private val TaskCardDark = Color(0xFF1C1D1B)
private val TaskCardText = Color(0xFFF5F2E8)

private data class TimelineStep(
    val state: OrderState,
    val title: (PaymentMethod) -> String,
    val description: (OrderDestination, PaymentMethod) -> String,
)

private val demoTimelineSteps = listOf(
    TimelineStep(OrderState.PENDING_PAYMENT, { payment ->
        if (payment.isInstantDemoPayment) "PAGO CONFIRMADO" else "POR COBRAR"
    }) { _, payment ->
        if (payment.isInstantDemoPayment) {
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
    val isReady = order.summary.state == OrderState.READY
    val cardBg = if (isReady) colors.yolk else TaskCardDark
    val cardText = if (isReady) colors.ink else TaskCardText
    val badgeBg = if (isReady) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.45f)
    val clickableModifier = if (onClick != null) {
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
                        text = order.summary.state.label.uppercase(),
                        color = colors.ink,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(moneyLabel(order.summary.total), color = cardText.copy(alpha = 0.82f), fontSize = 13.sp)
                Text(destinationDisplayLabel(order), color = cardText.copy(alpha = 0.82f), fontSize = 13.sp)
                Text(paymentMethodLabel(order.summary.paymentMethod), color = cardText.copy(alpha = 0.82f), fontSize = 13.sp)
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
    Column(modifier = modifier) {
        demoTimelineSteps.forEachIndexed { index, step ->
            val stepIndex = step.state.trackingIndex
            val currentIndex = current.trackingIndex
            val isDone = stepIndex < currentIndex
            val isCurrent = stepIndex == currentIndex
            TimelineRow(
                stepNumber = index + 1,
                title = step.title(paymentMethod),
                description = step.description(destination, paymentMethod),
                isDone = isDone,
                isCurrent = isCurrent,
                showLine = index < demoTimelineSteps.lastIndex,
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
    colors: com.vaiinilla.app.ui.theme.VaiinillaColors,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(colors.yolk.copy(alpha = 0.35f)),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCurrent -> colors.yolk
                                isDone -> colors.accent
                                else -> colors.paper2
                            },
                        ),
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
                    modifier = Modifier
                        .width(2.dp)
                        .height(40.dp)
                        .background(if (isDone) colors.accent else colors.paper2),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.padding(top = 4.dp, bottom = if (showLine) 8.dp else 0.dp)) {
            Text(
                title,
                color = if (isDone || isCurrent) colors.ink else colors.muted,
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
    val colors = LocalVaiinillaColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.paper2,
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            order.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("${item.quantity}× ${item.productName}", color = colors.ink, fontWeight = FontWeight.Bold)
                    Text(moneyLabel(item.subtotal), color = colors.ink, fontWeight = FontWeight.Black)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Total", color = colors.ink, fontWeight = FontWeight.Black)
                Text(moneyLabel(order.summary.total), color = colors.ink, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
        }
    }
}
