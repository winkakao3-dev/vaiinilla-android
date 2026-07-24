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
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.ui.theme.Cream
import com.vaiinilla.app.ui.theme.CreamDeep
import com.vaiinilla.app.ui.theme.Ink
import com.vaiinilla.app.ui.theme.Lime
import com.vaiinilla.app.ui.theme.MutedInk
import com.vaiinilla.app.ui.theme.Yolk

private val TaskCardDark = Color(0xFF1C1D1B)
private val TaskCardText = Color(0xFFF5F2E8)

private data class TimelineStep(
    val state: OrderState,
    val title: String,
    val description: String,
)

private val demoTimelineSteps = listOf(
    TimelineStep(OrderState.PENDING_PAYMENT, "POR COBRAR", "Caja espera el pago en efectivo."),
    TimelineStep(OrderState.PAID, "COBRADO", "Cocina recibió la comanda."),
    TimelineStep(OrderState.PREPARING, "PREPARANDO", "Tu comida se está preparando."),
    TimelineStep(OrderState.READY, "LISTO", "Recógelo en la barra."),
    TimelineStep(OrderState.DELIVERED, "ENTREGADO", "Pedido completado."),
)

@Composable
fun OrderTrackingCard(
    order: OrderDetail,
    modifier: Modifier = Modifier,
    showEyebrow: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val clickableModifier = if (onClick != null) {
        modifier.physicalPress(onClick = onClick)
    } else {
        modifier
    }
    Surface(
        modifier = clickableModifier.fillMaxWidth(),
        color = TaskCardDark,
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
                            color = TaskCardText.copy(alpha = 0.62f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp,
                        )
                    }
                    Text(
                        text = "#${order.summary.folio}",
                        color = TaskCardText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(top = if (showEyebrow) 4.dp else 0.dp),
                    )
                }
                Surface(
                    color = Color.White.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = order.summary.state.label.uppercase(),
                        color = Ink,
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
                Text(moneyLabel(order.summary.total), color = TaskCardText.copy(alpha = 0.82f), fontSize = 13.sp)
                Text(order.summary.destination.label, color = TaskCardText.copy(alpha = 0.82f), fontSize = 13.sp)
                Text(paymentLabel(order.summary.paymentMethod), color = TaskCardText.copy(alpha = 0.82f), fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun OrderTrackingTimeline(
    current: OrderState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        demoTimelineSteps.forEachIndexed { index, step ->
            val stepIndex = step.state.trackingIndex
            val currentIndex = current.trackingIndex
            val isDone = stepIndex < currentIndex
            val isCurrent = stepIndex == currentIndex
            TimelineRow(
                stepNumber = index + 1,
                title = step.title,
                description = step.description,
                isDone = isDone,
                isCurrent = isCurrent,
                showLine = index < demoTimelineSteps.lastIndex,
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
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCurrent -> Yolk
                            isDone -> Lime
                            else -> CreamDeep
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isDone || isCurrent) {
                    Text("✓", color = Ink, fontWeight = FontWeight.Black, fontSize = 12.sp)
                } else {
                    Text(
                        stepNumber.toString(),
                        color = MutedInk,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                    )
                }
            }
            if (showLine) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(40.dp)
                        .background(if (isDone) Lime else CreamDeep),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.padding(top = 4.dp, bottom = if (showLine) 8.dp else 0.dp)) {
            Text(title, color = if (isDone || isCurrent) Ink else MutedInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
            Text(description, color = MutedInk, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
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
        color = CreamDeep,
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
                    Text("${item.quantity}× ${item.productName}", color = Ink, fontWeight = FontWeight.Bold)
                    Text(moneyLabel(item.subtotal), color = Ink, fontWeight = FontWeight.Black)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Total", color = Ink, fontWeight = FontWeight.Black)
                Text(moneyLabel(order.summary.total), color = Ink, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
        }
    }
}

private fun paymentLabel(method: PaymentMethod): String = when (method) {
    PaymentMethod.CASH -> "Efectivo"
}
