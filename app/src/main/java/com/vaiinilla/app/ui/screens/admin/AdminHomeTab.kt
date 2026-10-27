package com.vaiinilla.app.ui.screens.admin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.vaiinilla.app.domain.admin.AdminMetric
import com.vaiinilla.app.domain.admin.AdminPeriod
import com.vaiinilla.app.domain.admin.AdminSummary
import com.vaiinilla.app.domain.admin.AdminUnpaidAccount
import com.vaiinilla.app.domain.admin.AdminUnpaidAccounts
import com.vaiinilla.app.ui.admin.AdminPeriodPreset
import com.vaiinilla.app.ui.admin.AdminUiState
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.screens.ActionPill
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.OperationalField
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Inicio: cómo va el negocio en el periodo elegido, como el panel de la web. Arriba el negocio con su foto y si
 * recibe pedidos; las ventas en grande; lo que pasa ahora a la vista (sin deslizar: en hora pico nadie desliza) y
 * el resto del resumen abajo. Al bajar queda una barra fija con los periodos.
 */
@Composable
internal fun AdminHomeTab(
    state: AdminUiState,
    colors: OperationalColors,
    readOnly: Boolean,
    actions: AdminActions,
    header: AdminHeader,
    onOpenOrders: () -> Unit,
) {
    var choosingPeriod by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf<AdminUnpaidAccount?>(null) }
    val listState = rememberLazyListState()
    val heroGone by rememberHeroGone(listState)
    val status = state.operationalStatus
    val receiving = status?.let { it.acceptingOrders && it.withinOrderWindow }
    val periodChips: @Composable () -> Unit = {
        AdminChipRow {
            AdminPeriodPreset.entries.forEach { preset ->
                AdminGlassChip(preset.label, state.customPeriod == null && preset == state.preset) {
                    actions.onSelectPeriod(preset)
                }
            }
            AdminGlassChip("Otro", state.customPeriod != null) { choosingPeriod = true }
        }
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item(key = "hero") {
                AdminHero(
                    title = header.placeName,
                    subtitle = homeSubtitle(state.live),
                    colors = colors,
                    height = 330.dp,
                    photoUrl = header.photoUrl,
                    logoUrl = header.logoUrl,
                    showLogo = true,
                    logoName = header.placeName,
                    leading =
                        receiving?.let { on ->
                            {
                                AdminHeroPill(
                                    if (on) "Recibiendo pedidos" else "Sin recibir pedidos",
                                    dot = if (on) colors.accentLime else AdminCoral,
                                )
                            }
                        },
                    trailing = header.avatar,
                )
            }
            item(key = "periodo") {
                Column {
                    periodChips()
                    state.customPeriod?.let { period ->
                        Text(
                            "Del ${period.from.format(PeriodFormat)} al ${period.to.format(PeriodFormat)}",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 22.dp),
                        )
                    }
                }
            }
            val summary = state.summary
            when {
                summary == null && state.summaryError != null ->
                    item(key = "error") {
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            AdminLoadError(state.summaryError, colors, actions.onRefreshHome)
                        }
                    }
                summary == null ->
                    item(key = "cargando") {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = colors.textPrimary,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                else -> item(key = "ventas") { SalesHeadline(summary, state, colors) }
            }
            item(key = "ahora-titulo") {
                val live = state.live
                AdminCremeSectionTitle(
                    "Ahora mismo",
                    colors,
                    detail =
                        live?.activeOrders?.let {
                            val count = if (live.moreActiveOrders) "$it+" else "$it"
                            if (it == 1 && !live.moreActiveOrders) "1 pedido activo" else "$count pedidos activos"
                        },
                    action = "Ver pedidos",
                    onAction = onOpenOrders,
                )
            }
            item(key = "ahora") { LiveGrid(state, receiving, colors) }
            if (summary != null) {
                val tiles = secondaryKpis(summary)
                if (tiles.isNotEmpty()) {
                    item(key = "kpis") {
                        Column(
                            Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            tiles.chunked(2).forEach { pair ->
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    pair.forEach { (label, value) ->
                                        AdminStatTile(
                                            label,
                                            value,
                                            colors,
                                            modifier = Modifier.weight(1f),
                                            muted = state.summaryLoading,
                                        )
                                    }
                                    if (pair.size == 1) Box(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                if (summary.dailySales.size > 1) {
                    item(key = "por-dia") {
                        Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp)) {
                            DailySalesCard(summary, colors)
                        }
                    }
                }
                if (summary.topProducts.isNotEmpty()) {
                    item(key = "top-titulo") { AdminCremeSectionTitle("Lo más vendido", colors) }
                    item(key = "top") { TopProducts(summary, state, colors) }
                }
                val usedMethods = summary.paymentMethods.filter { it.orders > 0 }
                if (usedMethods.isNotEmpty()) {
                    item(key = "pago-titulo") { AdminCremeSectionTitle("Por método de pago", colors) }
                    item(key = "pago") { PaymentBars(usedMethods, colors) }
                }
                if (summary.ordersByStatus.isNotEmpty()) {
                    item(key = "estados") {
                        Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
                            AdminCard(colors) {
                                AdminSectionTitle("Pedidos por estado", colors)
                                summary.ordersByStatus.forEach { AdminValueRow(it.label, "${it.count}", colors) }
                            }
                        }
                    }
                }
            }
            state.unpaid?.takeIf { it.accounts.isNotEmpty() }?.let { unpaid ->
                item(key = "sin-pagar") {
                    Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
                        UnpaidCard(unpaid, colors, canClose = !readOnly) { closing = it }
                    }
                }
            }
        }
        AdminCompactBar(
            visible = heroGone,
            title = header.placeName,
            logoUrl = header.logoUrl,
            colors = colors,
            trailing = { header.avatar() },
            below = periodChips,
        )
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

private fun homeSubtitle(live: AdminLiveCounts?): String =
    when (live?.cashOpen) {
        true -> "Administración · Caja abierta"
        false -> "Administración · Caja cerrada"
        null -> "Administración"
    }

/** Las ventas del periodo en grande; al cambiar de periodo la cifra entra con un resorte. */
@Composable
private fun SalesHeadline(
    summary: AdminSummary,
    state: AdminUiState,
    colors: OperationalColors,
) {
    val reduced = reducedMotion()
    val period = if (state.customPeriod != null) "periodo elegido" else state.preset.label.lowercase()
    Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 10.dp)) {
        Text(
            "VENTAS · ${period.uppercase()}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp,
            color = colors.textSecondary,
        )
        AnimatedContent(
            targetState = moneyLabel(summary.sales),
            transitionSpec = {
                if (reduced) {
                    fadeIn(snap()) togetherWith fadeOut(snap())
                } else {
                    (
                        fadeIn(spring(stiffness = 500f)) +
                            slideInVertically(spring(dampingRatio = 0.8f, stiffness = 500f)) { it / 3 }
                    ) togetherWith fadeOut(spring(stiffness = 900f))
                }
            },
            label = "admin-ventas",
        ) { value ->
            Text(
                value,
                fontSize = 60.sp,
                lineHeight = 66.sp,
                letterSpacing = (-2).sp,
                fontWeight = FontWeight.Black,
                color = if (state.summaryLoading) colors.textMuted else colors.textPrimary,
                maxLines = 1,
            )
        }
        Text(
            "${adminOrdersText(summary.orders)} · ticket promedio ${moneyLabel(summary.averageTicket)}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textSecondary,
        )
    }
}

/** Lo de ahorita, todo a la vista en mosaicos: si la cocina se cae, se ve sin buscarlo. */
@Composable
private fun LiveGrid(
    state: AdminUiState,
    receiving: Boolean?,
    colors: OperationalColors,
) {
    val status = state.operationalStatus
    val live = state.live
    val tiles =
        buildList {
            receiving?.let {
                add(
                    Triple(
                        "Pedidos",
                        if (it) "Recibiendo" else "Sin recibir",
                        if (it) colors.accentLime else AdminCoral,
                    ),
                )
            }
            (live?.cashOpen ?: status?.cashSessionOpen)?.let {
                add(Triple("Caja", if (it) "Abierta" else "Cerrada", if (it) colors.accentLime else colors.textMuted))
            }
            status?.let {
                add(
                    Triple(
                        "Cocina",
                        if (it.kitchenOnline) "En línea" else "Sin conexión",
                        if (it.kitchenOnline) colors.accentLime else AdminCoral,
                    ),
                )
                add(
                    Triple(
                        "Cajero",
                        if (it.cashierOnline) "En línea" else "Sin conexión",
                        if (it.cashierOnline) colors.accentLime else colors.textMuted,
                    ),
                )
            }
            live?.pendingInvitations?.let {
                add(
                    Triple(
                        "Invitaciones",
                        if (it == 1) "1 pendiente" else "$it pendientes",
                        if (it > 0) colors.textPrimary else colors.textMuted,
                    ),
                )
            }
        }
    Column(
        Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        tiles.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { (label, value, dot) ->
                    AdminStatTile(label, value, colors, modifier = Modifier.weight(1f), dot = dot)
                }
                if (pair.size == 1) Box(Modifier.weight(1f))
            }
        }
        status?.outsideOrderHoursMessage?.let { Text(it, fontSize = 12.sp, color = colors.textSecondary) }
    }
}

private fun secondaryKpis(summary: AdminSummary): List<Pair<String, String>> =
    buildList {
        add("Productos vendidos" to "${summary.productsSold}")
        summary.tips?.let { add("Propinas" to moneyLabel(it)) }
        add("Recargas de saldo" to moneyLabel(summary.walletReloads))
        summary.canceledOrders?.let { add("Cancelados" to "$it") }
        summary.commissions?.let { add("Comisiones" to moneyLabel(it)) }
    }

/** Lo más vendido con la foto del platillo, si el menú la tiene. */
@Composable
private fun TopProducts(
    summary: AdminSummary,
    state: AdminUiState,
    colors: OperationalColors,
) {
    val photos =
        remember(state.catalog) {
            state.catalog
                ?.products
                .orEmpty()
                .associate { it.name.trim().lowercase() to it.imageUrl }
        }
    Column(Modifier.padding(horizontal = 22.dp)) {
        summary.topProducts.forEachIndexed { index, item ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    "${index + 1}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.textSecondary,
                    modifier = Modifier.width(22.dp),
                )
                val photo = photos[item.label.trim().lowercase()]
                val thumb = Modifier.size(54.dp).clip(RoundedCornerShape(16.dp))
                if (photo != null) {
                    ProductImage(
                        imageUrl = photo,
                        contentDescription = item.label,
                        modifier = thumb,
                        placeholder = { m -> Box(m.background(colors.cardInner)) },
                        card = true,
                    )
                } else {
                    Box(thumb.background(colors.cardInner))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        item.label,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (item.count == 1) "1 vendido" else "${item.count} vendidos",
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
}

/** Cuánto entró por cada forma de pago, con una barra por su parte del total. */
@Composable
private fun PaymentBars(
    methods: List<AdminMetric>,
    colors: OperationalColors,
) {
    val amounts = methods.map { it.amount.toBigDecimalOrNull()?.toFloat() ?: 0f }
    val total = amounts.sum().takeIf { it > 0f } ?: 1f
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(colors.cardBackground)
            .border(1.dp, colors.cardBorder, shape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        methods.zip(amounts).forEachIndexed { index, (method, amount) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(method.label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Text(
                    moneyLabel(method.amount),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.cardInner),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth((amount / total).coerceIn(0.02f, 1f))
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (index % 2 == 0) colors.textPrimary else colors.accentLime),
                )
            }
        }
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
