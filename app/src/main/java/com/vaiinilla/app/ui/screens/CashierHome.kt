package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.repository.TableSpace
import com.vaiinilla.app.ui.components.NumberTicker
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.screens.admin.AdminSheet

/** Las dos cosas que la caja inicia en cualquier momento: un pedido de mostrador y una recarga. */
@Composable
internal fun CashierQuickActions(
    colors: OperationalColors,
    enabled: Boolean,
    onNewOrder: (() -> Unit)?,
    onReload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (onNewOrder != null) {
            QuickAction("Nuevo pedido", Icons.Outlined.Add, colors, enabled, Modifier.weight(1f), onNewOrder)
        }
        QuickAction(
            "Recargar saldo",
            Icons.Outlined.AccountBalanceWallet,
            colors,
            enabled,
            Modifier.weight(1f),
            onReload,
        )
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    colors: OperationalColors,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier =
            modifier
                .physicalPress(enabled = enabled, onClick = onClick)
                .height(52.dp)
                .clip(shape)
                .background(colors.cardInner)
                .border(1.dp, colors.cardBorder, shape)
                .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        Icon(icon, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(20.dp))
        Text(label, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
    }
}

/** Encabezado de la fila de trabajo: "Por cobrar" y cuántos pedidos esperan. */
@Composable
internal fun CashierQueueHeader(
    count: Int,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Por cobrar", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = colors.textPrimary)
        NumberTicker(value = count, label = "cashier-queue-count") { value ->
            Text(
                "$value",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = colors.background,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (value > 0) colors.textPrimary else colors.textMuted)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

/** Entrada al tablero de mesas y canchas, con cuántas tienen cuenta sin cobrar. */
@Composable
internal fun CashierSpacesEntry(
    pendingSpaces: Int?,
    colors: OperationalColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .physicalPress(scale = PhysicalPressScale.Nav, onClick = onClick)
                .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Mesas y canchas", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
            Text(
                when (pendingSpaces) {
                    null -> "Cuentas abiertas y turnos"
                    0 -> "Nada por cobrar"
                    1 -> "1 con cuenta por cobrar"
                    else -> "$pendingSpaces con cuenta por cobrar"
                },
                fontSize = 13.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textSecondary,
        )
    }
}

/** Una línea en lugar de un bloque vacío: la fila de trabajo está al día. */
@Composable
internal fun CashierQueueEmpty(
    colors: OperationalColors,
    modifier: Modifier = Modifier,
) {
    Text(
        "Nada por cobrar. Los pedidos de la app y del mostrador llegan aquí.",
        fontSize = 14.sp,
        color = colors.textSecondary,
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
    )
}

/** La fila de trabajo mientras termina la primera carga: todavía no se sabe si hay algo por cobrar. */
@Composable
internal fun CashierQueueLoading(
    colors: OperationalColors,
    modifier: Modifier = Modifier,
) {
    Text(
        "Cargando pedidos…",
        fontSize = 14.sp,
        color = colors.textSecondary,
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
    )
}

/** Caja toma pedidos como el mesero: para llevar o a una mesa (a su cuenta si el negocio deja pagar al final). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CashierNewOrderSheet(
    tables: List<TableSpace>,
    colors: OperationalColors,
    onDismiss: () -> Unit,
    onTakeAway: () -> Unit,
    onTable: (TableSpace) -> Unit,
) {
    AdminSheet(title = "Nuevo pedido", colors = colors, busy = false, onDismiss = onDismiss) {
        ActionPill(label = "Para llevar", enabled = true, loading = false, colors = colors, onClick = onTakeAway)
        Text("O en una mesa:", fontSize = 13.sp, color = colors.textSecondary)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tables.forEach { table ->
                ChipButton(label = table.name, enabled = true, colors = colors) { onTable(table) }
            }
        }
    }
}
