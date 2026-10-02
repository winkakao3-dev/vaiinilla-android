package com.vaiinilla.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.SharedTable
import com.vaiinilla.app.domain.model.SharedTableOrder
import com.vaiinilla.app.domain.model.SpaceCopy
import com.vaiinilla.app.ui.components.EditorialAccentButton
import com.vaiinilla.app.ui.components.EditorialPrimaryButton
import com.vaiinilla.app.ui.components.EditorialTextField
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.sharedtable.SharedTableUiState
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import java.math.BigDecimal

/**
 * El estado de un pedido de la mesa. A la cuenta, "cobrado" solo dice que Cocina lo recibió:
 * mientras no se pague, está en la cuenta.
 */
internal fun tableOrderStateLabel(order: SharedTableOrder): String =
    when {
        order.pendingPayment && order.status == OrderState.PAID -> "En la cuenta"
        order.pendingPayment -> order.status.label
        else -> "${order.status.label} · pagado"
    }

private fun money(value: BigDecimal): String = moneyLabel(value.setScale(2).toPlainString())

/**
 * La mesa compartida en Mis pedidos, con la misma tarjeta oscura que los pedidos. Sin mesa,
 * invita a unirse con un alias si el cliente llegó por el QR de una mesa.
 */
@Composable
fun SharedTableCard(
    state: SharedTableUiState,
    qrToken: String?,
    onAliasChange: (String) -> Unit,
    onJoin: () -> Unit,
    onClaim: (folio: Int, payIt: Boolean) -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.loaded) return
    val table = state.table
    if (table == null && qrToken == null) return
    Surface(
        modifier = modifier.fillMaxWidth().arrive(0, key = table?.spaceId ?: "unirse"),
        color = OrderTrackingCardBg,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .animateContentSize(
                        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
                    ).padding(16.dp),
        ) {
            if (table == null) {
                JoinTable(state, onAliasChange, onJoin)
            } else {
                TableContent(table, state.busy, onClaim, onLeave)
            }
            state.error?.let {
                Text(
                    it,
                    color = LocalVaiinillaColors.current.coral,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun JoinTable(
    state: SharedTableUiState,
    onAliasChange: (String) -> Unit,
    onJoin: () -> Unit,
) {
    Text(
        "Mesa compartida",
        color = OrderTrackingCardText.copy(alpha = 0.55f),
        fontSize = 13.sp,
        fontWeight = FontWeight.Black,
    )
    Text(
        "¿Compartes la mesa?",
        color = OrderTrackingCardText,
        fontSize = 16.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(top = 10.dp),
    )
    Text(
        "Únete con un nombre para ver los pedidos de todos y la cuenta de la mesa. Los demás solo ven ese nombre.",
        color = OrderTrackingCardText.copy(alpha = 0.62f),
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
    )
    EditorialTextField(
        value = state.aliasInput,
        onValueChange = onAliasChange,
        label = "Tu nombre en la mesa",
        placeholder = "Ana",
    )
    Spacer(Modifier.height(12.dp))
    EditorialAccentButton(text = "Unirme a la mesa", onClick = onJoin, enabled = state.canJoin)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TableContent(
    table: SharedTable,
    busy: Boolean,
    onClaim: (Int, Boolean) -> Unit,
    onLeave: () -> Unit,
) {
    val noun = SpaceCopy.kindLabel(table.spaceType).lowercase()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            table.spaceName,
            color = OrderTrackingCardText.copy(alpha = 0.55f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
        )
        Surface(color = Color.White.copy(alpha = 0.16f), shape = RoundedCornerShape(10.dp)) {
            Text(
                (if (table.participants.size == 1) "Solo tú" else "${table.participants.size} personas").uppercase(),
                color = OrderTrackingCardText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
    FlowRow(
        modifier = Modifier.padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        table.participants.forEach { person ->
            TableChip(text = if (person.isMe) "${person.alias} (tú)" else person.alias, on = person.isMe)
        }
    }
    val groups = table.orderedGroups.filter { it.orders.isNotEmpty() }
    if (!table.accountOpen || groups.isEmpty()) {
        Text(
            "Aún no hay pedidos en la cuenta de la $noun.",
            color = OrderTrackingCardText.copy(alpha = 0.62f),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 14.dp),
        )
    } else {
        groups.forEachIndexed { index, group ->
            Column(modifier = Modifier.padding(top = 14.dp).arrive(index + 1, key = group.alias)) {
                Text(
                    (if (group.isMe) "Tus pedidos" else group.alias ?: "Otros en la $noun").uppercase(),
                    color = OrderTrackingCardText.copy(alpha = 0.55f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.4.sp,
                )
                group.orders.forEach { order -> TableOrderRow(order, busy, onClaim) }
            }
        }
    }
    CardRule()
    SumRow("Pagado", money(table.totals.paid))
    SumRow("Por pagar de la $noun", money(table.totals.pending))
    SumRow("Tu parte por pagar", money(table.myShare.pending), highlight = true)
    Spacer(Modifier.height(12.dp))
    EditorialPrimaryButton(
        text = "Salir de la $noun",
        onClick = onLeave,
        enabled = !busy,
        background = Color.Transparent,
        contentColor = OrderTrackingCardText,
        modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp)),
    )
}

@Composable
private fun TableOrderRow(
    order: SharedTableOrder,
    busy: Boolean,
    onClaim: (Int, Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    order.itemsSummary.ifBlank { "Pedido #${order.folio}" },
                    color = OrderTrackingCardText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(tableOrderStateLabel(order), color = OrderTrackingCardText.copy(alpha = 0.6f), fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(money(order.total), color = OrderTrackingCardText, fontSize = 14.sp, fontWeight = FontWeight.Black)
                if (order.pendingPayment) {
                    val payer = order.payer
                    if (payer != null && !order.iPayIt) {
                        Text(
                            "Paga $payer",
                            color = OrderTrackingCardText.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    } else {
                        TableChip(
                            text = if (order.iPayIt) "Lo pago yo ✓" else "Esto lo pago yo",
                            on = order.iPayIt,
                            enabled = !busy,
                            onClick = { onClaim(order.folio, !order.iPayIt) },
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(1.dp)
                .border(0.5.dp, Color.White.copy(alpha = 0.12f)),
        )
    }
}

/** Píldora de la mesa: encendida en lima, como el resto de la app. */
@Composable
private fun TableChip(
    text: String,
    on: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val colors = LocalVaiinillaColors.current
    val bg by animateColorAsState(if (on) colors.accent else Color.Transparent, label = "chip-bg")
    val fg by animateColorAsState(if (on) colors.accentInk else OrderTrackingCardText, label = "chip-fg")
    val base =
        modifier
            .border(1.dp, if (on) Color.Transparent else Color.White.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
    Surface(
        modifier =
            if (onClick != null) {
                base
                    .semantics {
                        role = Role.Button
                        selected = on
                    }.physicalPress(enabled = enabled, scale = PhysicalPressScale.Small, onClick = onClick)
            } else {
                base
            },
        color = bg,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text,
            color = fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun SumRow(
    label: String,
    value: String,
    highlight: Boolean = false,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            color = if (highlight) colors.accent else OrderTrackingCardText.copy(alpha = 0.8f),
            fontSize = 14.sp,
            fontWeight = if (highlight) FontWeight.Black else FontWeight.Normal,
        )
        Text(
            value,
            color = OrderTrackingCardText,
            fontSize = 14.sp,
            fontWeight = if (highlight) FontWeight.Black else FontWeight.Normal,
        )
    }
}

@Composable
private fun CardRule() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .height(1.dp)
                .border(0.5.dp, Color.White.copy(alpha = 0.12f)),
    )
}
