package com.vaiinilla.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.admin.AdminLiveCounts
import com.vaiinilla.app.domain.admin.AdminPeriod
import com.vaiinilla.app.domain.admin.AdminSummary
import com.vaiinilla.app.domain.admin.AdminUnpaidAccount
import com.vaiinilla.app.domain.admin.AdminUnpaidAccounts
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.ui.admin.AdminPeriodPreset
import com.vaiinilla.app.ui.admin.AdminUiState
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.screens.ActionPill
import com.vaiinilla.app.ui.screens.ChipButton
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.OperationalField
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Inicio: cómo va el negocio en el periodo elegido, como el panel de la web. */
@Composable
internal fun AdminHomeTab(
    state: AdminUiState,
    colors: OperationalColors,
    readOnly: Boolean,
    actions: AdminActions,
) {
    val haptics = rememberVaiinillaHaptics()
    var choosingPeriod by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf<AdminUnpaidAccount?>(null) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminPeriodPreset.entries.forEach { preset ->
                    ChipButton(
                        label = preset.label,
                        enabled = true,
                        colors = colors,
                        selected = state.customPeriod == null && preset == state.preset,
                        modifier = Modifier.weight(1f),
                    ) {
                        haptics.selection()
                        actions.onSelectPeriod(preset)
                    }
                }
                ChipButton(
                    label = "Otro",
                    enabled = true,
                    colors = colors,
                    selected = state.customPeriod != null,
                    modifier = Modifier.weight(1f),
                ) { choosingPeriod = true }
            }
            state.customPeriod?.let { period ->
                Text(
                    "Del ${period.from.format(PeriodFormat)} al ${period.to.format(PeriodFormat)}",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        state.live?.let { live -> item { LiveCard(live, colors) } }
        state.operationalStatus?.let { status -> item { StatusCard(status, colors) } }
        val summary = state.summary
        when {
            summary == null && state.summaryError != null ->
                item { AdminLoadError(state.summaryError, colors, actions.onRefreshHome) }
            summary == null ->
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = colors.textPrimary,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            else -> {
                item { KpiGrid(summary, colors, loading = state.summaryLoading) }
                if (summary.dailySales.size > 1) item { DailySalesCard(summary, colors) }
                val usedMethods = summary.paymentMethods.filter { it.orders > 0 }
                if (usedMethods.isNotEmpty()) {
                    item {
                        AdminCard(colors) {
                            AdminSectionTitle("Por método de pago", colors)
                            usedMethods.forEach {
                                AdminValueRow(
                                    it.label,
                                    moneyLabel(it.amount),
                                    colors,
                                    detail = adminOrdersText(it.orders),
                                )
                            }
                        }
                    }
                }
                if (summary.topProducts.isNotEmpty()) {
                    item {
                        AdminCard(colors) {
                            AdminSectionTitle("Lo más vendido", colors)
                            summary.topProducts.forEach { AdminValueRow(it.label, "${it.count}", colors) }
                        }
                    }
                }
                if (summary.ordersByStatus.isNotEmpty()) {
                    item {
                        AdminCard(colors) {
                            AdminSectionTitle("Pedidos por estado", colors)
                            summary.ordersByStatus.forEach { AdminValueRow(it.label, "${it.count}", colors) }
                        }
                    }
                }
            }
        }
        state.unpaid?.takeIf { it.accounts.isNotEmpty() }?.let { unpaid ->
            item { UnpaidCard(unpaid, colors, canClose = !readOnly) { closing = it } }
        }
        item { Box(Modifier.height(8.dp)) }
    }
    if (choosingPeriod) {
        PeriodPicker(colors = colors, onDismiss = { choosingPeriod = false }) { period ->
            choosingPeriod = false
            actions.onSelectCustomPeriod(period)
        }
    }
    closing?.let { account ->
        CloseWithoutChargingSheet(account, colors, state.saving, state.notice.takeIf { state.noticeIsError }, {
            closing =
                null
        }) { note ->
            actions.onCloseWithoutCharging(account, note) { closing = null }
        }
    }
}

@Composable
private fun LiveCard(
    live: AdminLiveCounts,
    colors: OperationalColors,
) {
    AdminCard(colors) {
        AdminSectionTitle("Ahora mismo", colors)
        live.activeOrders?.let {
            AdminValueRow(
                "Pedidos activos",
                if (live.moreActiveOrders) "$it+" else "$it",
                colors,
            )
        }
        live.cashOpen?.let { AdminValueRow("Caja", if (it) "Abierta" else "Cerrada", colors) }
        live.pendingInvitations?.let { AdminValueRow("Invitaciones pendientes", "$it", colors) }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun PeriodPicker(
    colors: OperationalColors,
    onDismiss: () -> Unit,
    onPick: (AdminPeriod) -> Unit,
) {
    val today = LocalDate.now()
    val todayMillis = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val pickerState =
        rememberDateRangePickerState(
            selectableDates =
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayMillis
                },
        )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = pickerState.selectedStartDateMillis != null,
                onClick = {
                    val start = pickerState.selectedStartDateMillis ?: return@TextButton
                    val end = pickerState.selectedEndDateMillis ?: start
                    onPick(AdminPeriod(utcDay(start), utcDay(end)))
                },
            ) { Text("Listo", color = colors.textPrimary, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = colors.textSecondary) } },
    ) {
        DateRangePicker(state = pickerState, title = {
            Text("Elige el periodo", modifier = Modifier.padding(16.dp))
        }, showModeToggle = false)
    }
}

private fun utcDay(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

private val PeriodFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-MX"))

@Composable
private fun CloseWithoutChargingSheet(
    account: AdminUnpaidAccount,
    colors: OperationalColors,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var note by remember { mutableStateOf("") }
    AdminSheet(
        title = "Cerrar ${account.spaceName} sin cobrar",
        colors = colors,
        busy = saving,
        onDismiss = onDismiss,
    ) {
        Text("Úsalo solo si el cliente se fue sin pagar. No cobra nada.", fontSize = 15.sp, color = colors.textPrimary)
        AdminValueRow("Cuenta", moneyLabel(account.total), colors, detail = adminOrdersText(account.orders))
        OperationalField(
            value = note,
            onValueChange = { note = it.take(240) },
            label = "Motivo (opcional)",
            colors = colors,
            placeholder = "Se fue sin pagar",
            enabled = !saving,
        )
        AdminSheetError(error)
        ActionPill(label = "Cerrar sin cobrar", enabled = true, loading = saving, colors = colors) { onConfirm(note) }
    }
}

@Composable
private fun StatusCard(
    status: OperationalStatus,
    colors: OperationalColors,
) {
    AdminCard(colors) {
        AdminSectionTitle("Ahora", colors)
        StatusLine("Recibiendo pedidos", status.acceptingOrders && status.withinOrderWindow, colors)
        StatusLine("Caja abierta", status.cashSessionOpen, colors)
        StatusLine("Caja en línea", status.cashierOnline, colors)
        StatusLine("Cocina en línea", status.kitchenOnline, colors)
        status.outsideOrderHoursMessage?.let { Text(it, fontSize = 12.sp, color = colors.textSecondary) }
    }
}

@Composable
private fun StatusLine(
    label: String,
    on: Boolean,
    colors: OperationalColors,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier =
            Modifier.semantics(mergeDescendants = true) {
                contentDescription =
                    "$label: ${if (on) "sí" else "no"}"
            },
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (on) colors.accentLime else colors.cardBorder),
        )
        Text(label, fontSize = 14.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
        Text(
            if (on) "Sí" else "No",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (on) colors.textPrimary else colors.textSecondary,
        )
    }
}

@Composable
private fun KpiGrid(
    summary: AdminSummary,
    colors: OperationalColors,
    loading: Boolean,
) {
    val tiles =
        buildList {
            add("Ventas" to moneyLabel(summary.sales))
            add("Pedidos" to "${summary.orders}")
            add("Ticket promedio" to moneyLabel(summary.averageTicket))
            add("Productos vendidos" to "${summary.productsSold}")
            summary.tips?.let { add("Propinas" to moneyLabel(it)) }
            add("Recargas de saldo" to moneyLabel(summary.walletReloads))
            summary.canceledOrders?.let { add("Cancelados" to "$it") }
            summary.commissions?.let { add("Comisiones" to moneyLabel(it)) }
        }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (label, value) -> KpiTile(label, value, colors, loading, Modifier.weight(1f)) }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun KpiTile(
    label: String,
    value: String,
    colors: OperationalColors,
    loading: Boolean,
    modifier: Modifier,
) {
    AdminCard(colors, modifier = modifier) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = if (loading) colors.textMuted else colors.textPrimary,
            maxLines = 1,
        )
    }
}

/** Barras de ventas por día: la más alta llena la altura. */
@Composable
private fun DailySalesCard(
    summary: AdminSummary,
    colors: OperationalColors,
) {
    val values = summary.dailySales.map { it.sales.toBigDecimalOrNull() ?: BigDecimal.ZERO }
    val max = values.maxOrNull()?.takeIf { it > BigDecimal.ZERO } ?: BigDecimal.ONE
    AdminCard(colors) {
        AdminSectionTitle("Ventas por día", colors)
        Row(
            modifier = Modifier.fillMaxWidth().height(96.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            summary.dailySales.zip(values).forEach { (day, value) ->
                val fraction = (value.toFloat() / max.toFloat()).coerceIn(0.03f, 1f)
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(if (value > BigDecimal.ZERO) colors.textPrimary else colors.cardBorder)
                            .semantics { contentDescription = "${day.date}: ${moneyLabel(day.sales)}" },
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(summary.dailySales.first().date, fontSize = 11.sp, color = colors.textSecondary)
            Text(summary.dailySales.last().date, fontSize = 11.sp, color = colors.textSecondary)
        }
    }
}

@Composable
private fun UnpaidCard(
    unpaid: AdminUnpaidAccounts,
    colors: OperationalColors,
    canClose: Boolean,
    onClose: (AdminUnpaidAccount) -> Unit,
) {
    AdminCard(colors) {
        AdminSectionTitle("Cuentas sin pagar", colors)
        if (unpaid.abandoned > 0) {
            Text(
                if (unpaid.abandoned ==
                    1
                ) {
                    "1 cuenta quedó sin pagar en un espacio ya liberado."
                } else {
                    "${unpaid.abandoned} cuentas quedaron sin pagar en espacios ya liberados."
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AdminCoral,
            )
        }
        unpaid.accounts.forEach { account ->
            AdminValueRow(
                label = account.spaceName,
                value = moneyLabel(account.total),
                colors = colors,
                detail =
                    buildString {
                        append(adminOrdersText(account.orders))
                        if (account.hours > 0) append(" · hace ${account.hours} h")
                        if (account.abandoned) append(" · espacio liberado")
                    },
            )
            if (canClose && !account.abandoned) {
                Text(
                    "Cerrar sin cobrar",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AdminCoral,
                    modifier =
                        Modifier
                            .physicalPress(
                                scale = PhysicalPressScale.Small,
                            ) { onClose(account) }
                            .padding(vertical = 6.dp),
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.cardBorder))
        AdminValueRow("Total", moneyLabel(unpaid.total), colors, strong = true)
    }
}

internal fun adminOrdersText(count: Int): String = if (count == 1) "1 pedido" else "$count pedidos"
