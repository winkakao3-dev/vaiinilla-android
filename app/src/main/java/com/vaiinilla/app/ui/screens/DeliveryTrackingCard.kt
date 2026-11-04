package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.SoupKitchen
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.order.journeySchedule
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

private data class DeliveryStep(
    val icon: ImageVector,
    val title: String,
    val detail: String,
)

private val DeliverySteps =
    listOf(
        DeliveryStep(Icons.Outlined.HourglassTop, "Por aceptar", "El negocio revisa tu pedido y te confirma."),
        DeliveryStep(Icons.Outlined.Check, "Confirmado", "El negocio aceptó tu pedido."),
        DeliveryStep(Icons.Outlined.SoupKitchen, "Preparando", "Tu pedido se está preparando."),
        DeliveryStep(Icons.Outlined.TwoWheeler, "En camino", "Tu pedido va en camino. Ten tu código a la mano."),
        DeliveryStep(Icons.Outlined.CheckCircle, "Entregado", "Pedido completado."),
    )

internal fun deliveryStepIndex(state: OrderState): Int =
    when (state) {
        OrderState.AWAITING_ACCEPTANCE -> 0
        OrderState.PENDING_PAYMENT -> 1
        OrderState.READY, OrderState.ON_THE_WAY -> 3
        OrderState.DELIVERED -> 4
        else -> 2
    }

/** Un pedido a domicilio en Mis pedidos: en qué paso va, qué falta y lo que se paga al recibir (Doop 16). */
@Composable
internal fun DeliveryTrackingCard(
    order: OrderDetail,
    onOpenFull: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val summary = order.summary
    val current = deliveryStepIndex(summary.state)
    val delivery = order.homeDelivery
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(colors.paper2)
                .padding(16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "#${summary.folio}",
                color = colors.ink,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                DeliverySteps[current].title.uppercase(),
                color = colors.yolk,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                modifier =
                    Modifier
                        .clip(CircleShape)
                        .background(colors.yolk.copy(alpha = 0.16f))
                        .padding(horizontal = 11.dp, vertical = 5.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    order.items.joinToString(" · ") { "${it.quantity} ${it.productName}" },
                    color = colors.ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(
                        "A domicilio",
                        summary.paymentMethod.label,
                        journeySchedule(order)?.substringBefore(" · ")?.replaceFirstChar { it.uppercase() },
                    ).joinToString(" · "),
                    color = colors.muted,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(moneyLabel(summary.total), color = colors.ink, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.6).sp)
        }
        Spacer(Modifier.height(16.dp))
        DeliveryStepIcons(current)
        Spacer(Modifier.height(14.dp))
        DeliverySteps.forEachIndexed { index, step ->
            DeliveryStepRow(step, index, current, last = index == DeliverySteps.lastIndex)
        }
        val due = delivery?.dueOnArrival?.toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
        val deposit = delivery?.deposit?.toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
        if (due != null || deposit != null) {
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (deposit != null) {
                    // Con tarjeta no se cobra nada hasta que el negocio acepta: antes de eso no se puede decir «Pagaste».
                    val paid = order.payment != null && summary.state != OrderState.AWAITING_ACCEPTANCE && summary.state != OrderState.PENDING_PAYMENT
                    MoneyLine(if (paid) "Pagaste" else "Al aceptar", moneyLabel(deposit.toPlainString()), if (paid) colors.accent else colors.ink)
                }
                if (due != null) MoneyLine("Al recibir", moneyLabel(due.toPlainString()), colors.ink)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(colors.ink)
                    .physicalPress(onClick = onOpenFull),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Ver pedido completo", color = colors.paper, fontSize = 15.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = colors.paper, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun MoneyLine(
    label: String,
    amount: String,
    tint: Color,
) {
    val colors = LocalVaiinillaColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = colors.ink2, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(5.dp))
        Text(amount, color = tint, fontSize = 13.5.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun DeliveryStepIcons(current: Int) {
    val colors = LocalVaiinillaColors.current
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        DeliverySteps.forEachIndexed { index, step ->
            if (index > 0) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(1.5.dp)
                        .background(if (index <= current) colors.yolk else colors.line),
                )
            }
            val active = index == current
            val done = index < current
            Box(
                modifier =
                    Modifier
                        .size(if (active) 30.dp else 26.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                active -> colors.yolk
                                done -> colors.yolk.copy(alpha = 0.22f)
                                else -> colors.ink.copy(alpha = 0.08f)
                            },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    step.icon,
                    contentDescription = null,
                    tint =
                        when {
                            active -> Color(0xFF1C1C19)
                            done -> colors.yolk
                            else -> colors.muted
                        },
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun DeliveryStepRow(
    step: DeliveryStep,
    index: Int,
    current: Int,
    last: Boolean,
) {
    val colors = LocalVaiinillaColors.current
    val active = index == current
    val done = index < current
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(modifier = Modifier.width(14.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .padding(top = 4.dp)
                    .size(9.dp)
                    .clip(CircleShape)
                    .then(
                        when {
                            active -> Modifier.background(colors.yolk)
                            done -> Modifier.background(colors.yolk.copy(alpha = 0.5f))
                            else -> Modifier.background(colors.paper2).border(1.5.dp, colors.line, CircleShape)
                        },
                    ),
            )
            if (!last) {
                Box(Modifier.width(1.5.dp).weight(1f).background(colors.line))
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.padding(bottom = if (last) 8.dp else 10.dp)) {
            Text(
                step.title.uppercase(),
                color = if (active) colors.yolk else colors.muted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.3.sp,
            )
            Text(
                step.detail,
                color = if (active) colors.ink else colors.muted,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}