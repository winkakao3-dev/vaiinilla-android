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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.ui.theme.Coral
import com.vaiinilla.app.ui.theme.CreamDeep
import com.vaiinilla.app.ui.theme.Ink
import com.vaiinilla.app.ui.theme.Lime
import com.vaiinilla.app.ui.theme.MutedInk

@Composable
fun OrderStatusBadge(state: OrderState) {
    val (background, foreground) =
        when (state) {
            OrderState.PENDING_PAYMENT -> CreamDeep to Ink
            OrderState.PAID -> Lime to Ink
            OrderState.PREPARING -> Coral to Ink
            OrderState.READY -> Lime to Ink
            OrderState.DELIVERED -> CreamDeep to MutedInk
        }
    Surface(
        color = background,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = state.label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = foreground,
        )
    }
}

@Composable
fun OrderTimeline(current: OrderState) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        OrderState.trackingFlow.forEachIndexed { index, step ->
            val completed = step.trackingIndex <= current.trackingIndex
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier =
                            Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (completed) Lime else CreamDeep),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (completed) {
                            Text("✓", color = Ink, fontWeight = FontWeight.Black)
                        }
                    }
                    if (index < OrderState.trackingFlow.lastIndex) {
                        Box(
                            modifier =
                                Modifier
                                    .width(2.dp)
                                    .height(36.dp)
                                    .background(if (completed) Lime else CreamDeep),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = step.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (completed) Ink else MutedInk,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = timelineCopy(step),
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedInk,
                    )
                }
            }
        }
    }
}

private fun timelineCopy(step: OrderState): String =
    when (step) {
        OrderState.PENDING_PAYMENT -> "Esperando pago en Caja."
        OrderState.PAID -> "Cobro confirmado."
        OrderState.PREPARING -> "Cocina preparando tu pedido."
        OrderState.READY -> "Listo para entregar."
        OrderState.DELIVERED -> "Entrega completada."
    }

@Composable
fun OrderSummaryCard(
    order: OrderDetail,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pedido #${order.summary.folio}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MutedInk,
                    )
                    Text(
                        text = order.items.joinToString { "${it.quantity}× ${it.productName}" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                OrderStatusBadge(order.summary.state)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(order.summary.destination.label, color = MutedInk)
                Text(moneyLabel(order.summary.total), fontWeight = FontWeight.Black)
            }
            if (!order.kitchenNotes.isBlank()) {
                Text(
                    text = "Notas: ${order.kitchenNotes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedInk,
                )
            }
            if (actionLabel != null && onAction != null) {
                androidx.compose.material3.Button(
                    onClick = onAction,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Lime,
                            contentColor = Ink,
                        ),
                ) {
                    Text(actionLabel, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun OperationalEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MutedInk)
    }
}
