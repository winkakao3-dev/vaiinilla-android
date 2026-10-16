package com.vaiinilla.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.ui.admin.AdminOrderFilter
import com.vaiinilla.app.ui.admin.AdminUiState
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.screens.ActionPill
import com.vaiinilla.app.ui.screens.ChipButton
import com.vaiinilla.app.ui.screens.OperationalColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Pedidos e historial, con los filtros de la web. Aquí solo se consultan: los mueven Caja, Cocina y Mesero. */
@Composable
internal fun AdminOrdersTab(
    state: AdminUiState,
    colors: OperationalColors,
    actions: AdminActions,
) {
    val haptics = rememberVaiinillaHaptics()
    var open by remember { mutableStateOf<OrderDetail?>(null) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminOrderFilter.entries.forEach { filter ->
                    ChipButton(
                        label = filter.label,
                        enabled = true,
                        colors = colors,
                        selected =
                            filter == state.orderFilter,
                    ) {
                        haptics.selection()
                        actions.onSelectOrderFilter(filter)
                    }
                }
            }
        }
        val error = state.ordersError
        if (error != null && state.orders.isEmpty()) {
            item { AdminLoadError(error, colors, actions.onRefreshOrders) }
        } else if (!state.ordersLoading && state.orders.isEmpty()) {
            item {
                AdminCard(colors) {
                    Text("Sin pedidos aquí", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                    Text(
                        "Cuando haya pedidos con este filtro, salen en esta lista.",
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                    )
                }
            }
        }
        items(state.orders, key = { it.summary.id }) { order -> OrderRow(order, colors) { open = order } }
        if (state.ordersLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = colors.textPrimary,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        } else if (state.ordersCursor != null) {
            item {
                ActionPill(
                    label = "Ver más pedidos",
                    enabled = true,
                    loading = false,
                    colors = colors,
                    primary = false,
                    onClick = actions.onLoadMoreOrders,
                )
            }
        }
        item { Box(Modifier.height(8.dp)) }
    }
    open?.let { order -> OrderSheet(order, colors) { open = null } }
}

@Composable
private fun OrderRow(
    order: OrderDetail,
    colors: OperationalColors,
    onOpen: () -> Unit,
) {
    val summary = order.summary
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .physicalPress(onClick = onOpen)
                .clip(shape)
                .background(colors.cardBackground)
                .border(1.dp, colors.cardBorder, shape)
                .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text("Pedido #${summary.folio}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
            Text(
                listOfNotNull(timeLabel(summary.createdAt), whereLabel(order), whoLabel(order)).joinToString(" · "),
                fontSize = 12.sp,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(moneyLabel(summary.total), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
            StateTag(summary.state, colors)
        }
    }
}

@Composable
private fun StateTag(
    state: OrderState,
    colors: OperationalColors,
) {
    val tint = if (state.isTerminalWithoutDelivery) AdminCoral else colors.textSecondary
    Text(state.label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tint)
}

@Composable
private fun OrderSheet(
    order: OrderDetail,
    colors: OperationalColors,
    onDismiss: () -> Unit,
) {
    val summary = order.summary
    AdminSheet(title = "Pedido #${summary.folio}", colors = colors, busy = false, onDismiss = onDismiss) {
        AdminCard(colors) {
            AdminValueRow("Estado", summary.state.label, colors)
            AdminValueRow(
                "Pago",
                summary.paymentMethod.label + if (summary.paymentPending) " · pendiente" else "",
                colors,
            )
            whereLabel(order)?.let { AdminValueRow("Destino", it, colors) }
            whoLabel(order)?.let { AdminValueRow("Cliente", it, colors) }
            timeLabel(summary.createdAt)?.let { AdminValueRow("Hora", it, colors, detail = summary.operationalDate) }
            summary.cancelReason?.let { Text("Motivo: $it", fontSize = 13.sp, color = AdminCoral) }
            order.arrivedAt?.let { arrived ->
                Text(
                    "Ya llegó a las ${ArrivalFormat.format(arrived.atZone(ZoneId.systemDefault()))}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
            }
        }
        AdminCard(colors) {
            AdminSectionTitle("Productos", colors)
            order.items.forEach { item ->
                val details =
                    buildList {
                        if (item.options.isNotEmpty()) add(item.options.joinToString(", ") { it.name })
                        if (item.notes.isNotBlank()) add("Nota: ${item.notes}")
                        item.rejection?.let { add("Quitado: ${it.reason}") }
                    }
                AdminValueRow(
                    label = "${item.quantity} × ${item.productName}",
                    value = moneyLabel(item.subtotal),
                    colors = colors,
                    detail = details.joinToString("\n").ifBlank { null },
                )
            }
            if (order.kitchenNotes.isNotBlank()) {
                Text(
                    "Notas: ${order.kitchenNotes}",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.cardBorder))
            if (summary.combinedSavings.toBigDecimalOrNull()?.signum() == 1) {
                AdminValueRow("Ahorro por combinar", "-${moneyLabel(summary.combinedSavings)}", colors)
            }
            if (summary.cashbackAwarded.toBigDecimalOrNull()?.signum() == 1) {
                AdminValueRow("Cashback otorgado", moneyLabel(summary.cashbackAwarded), colors)
            }
            AdminValueRow("Total", moneyLabel(summary.total), colors, strong = true)
        }
    }
}

private fun whereLabel(order: OrderDetail): String? =
    when {
        order.summary.space != null -> order.summary.space.name
        order.summary.destination == OrderDestination.TAKE_AWAY -> OrderDestination.TAKE_AWAY.label
        else -> null
    }

private fun whoLabel(order: OrderDetail): String? =
    order.customerName?.takeIf { it.isNotBlank() } ?: order.user?.name?.takeIf { it.isNotBlank() }

private val ArrivalFormat = DateTimeFormatter.ofPattern("HH:mm")

private val TimeFormat = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.forLanguageTag("es-MX"))

private fun timeLabel(iso: String): String? =
    runCatching { TimeFormat.format(Instant.parse(iso).atZone(ZoneId.systemDefault())) }.getOrNull()
