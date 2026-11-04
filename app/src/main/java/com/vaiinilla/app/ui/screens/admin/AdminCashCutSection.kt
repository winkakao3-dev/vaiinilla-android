package com.vaiinilla.app.ui.screens.admin

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.vaiinilla.app.data.cashcut.CashCountResult
import com.vaiinilla.app.data.cashcut.CutCash
import com.vaiinilla.app.data.cashcut.CutShift
import com.vaiinilla.app.data.cashcut.SalesBreakdown
import com.vaiinilla.app.data.cashcut.staffRoleLabel
import com.vaiinilla.app.ui.admin.AdminCashCutUiState
import com.vaiinilla.app.ui.admin.AdminCashCutViewModel
import com.vaiinilla.app.ui.admin.CashCutTab
import com.vaiinilla.app.ui.admin.ReportPeriod
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.receiptLocalTime
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Spanish = Locale.forLanguageTag("es-MX")
private val DayFormat = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Spanish)

/** «lunes 9 de noviembre» con mayúscula. */
internal fun cutDayLabel(day: LocalDate?): String =
    day?.format(DayFormat)?.replaceFirstChar { it.titlecase(Spanish) } ?: "Hoy"

/** Monto con signo para la diferencia del arqueo: «+$10», «−$5», «$0». */
internal fun signedMoney(value: String?): String {
    val amount = value?.toBigDecimalOrNull() ?: return "—"
    return when (amount.signum()) {
        0 -> moneyLabel("0.00")
        1 -> "+" + moneyLabel(amount.toPlainString())
        else -> "−" + moneyLabel(amount.negate().toPlainString())
    }
}

private fun shareOf(
    amount: String,
    total: String,
): Float {
    val t = total.toBigDecimalOrNull() ?: return 0f
    if (t.signum() <= 0) return 0f
    return ((amount.toBigDecimalOrNull() ?: BigDecimal.ZERO).toFloat() / t.toFloat()).coerceIn(0f, 1f)
}

/**
 * Negocio › Cortes y reportes (Doop CR1-CR4): el corte de caja por día con sus turnos, el corte de cada turno y el
 * reporte contable con su CSV. Con [showReport] en false es el «Corte de caja» del menú de Caja (CR5): Caja no ve
 * el reporte contable.
 */
@Composable
internal fun AdminCashCutSection(
    viewModel: AdminCashCutViewModel,
    colors: OperationalColors,
    onBack: () -> Unit,
    showReport: Boolean = true,
) {
    val state by viewModel.uiState
    val context = LocalContext.current
    LaunchedEffect(state.csv) {
        state.csv?.let { csv -> viewModel.csvShared(shareCsv(context, csv.fileName, csv.content)) }
    }
    val shift = state.shiftCut
    if (shift != null) {
        AdminSectionPage(
            title = shiftTitle(shift.shift),
            eyebrow = if (shift.final) "CORTE FINAL DEL TURNO" else "CORTE PARCIAL · TURNO ABIERTO",
            colors = colors,
            notice = state.notice,
            noticeIsError = state.noticeIsError,
            onBack = viewModel::closeShift,
            trailing = {
                AdminRoundButton(Icons.Outlined.Refresh, "Actualizar", colors) { viewModel.openShift(shift.shift.id) }
            },
        ) {
            item(key = "shift-hours") {
                AdminHint(shiftHours(shift.shift), colors)
            }
            item(key = "shift-cash") { CashPanel(shift.shift.cash, colors, title = "Efectivo del turno") }
            breakdownItems(shift.breakdown, colors, keyPrefix = "shift")
        }
        return
    }
    AdminSectionPage(
        title = if (showReport) "Cortes y reportes" else "Corte de caja",
        colors = colors,
        notice = state.notice,
        noticeIsError = state.noticeIsError,
        onBack = onBack,
        trailing = { AdminRoundButton(Icons.Outlined.Refresh, "Actualizar", colors, viewModel::refresh) },
    ) {
        if (showReport) {
            item(key = "tabs") {
                AdminSegmented(
                    options = CashCutTab.entries.map { it to it.label },
                    selected = state.tab,
                    colors = colors,
                    onSelect = viewModel::selectTab,
                )
            }
        }
        if (!showReport || state.tab == CashCutTab.CUTS) {
            dayCutItems(state, viewModel, colors)
        } else {
            reportItems(state, viewModel, colors)
        }
    }
}

private fun shiftTitle(shift: CutShift): String =
    shift.openedBy?.let { "Turno de ${it.substringBefore(' ')}" } ?: "Turno"

private fun shiftHours(shift: CutShift): String {
    val from = shift.openedAt?.let(::receiptLocalTime) ?: "—"
    val to = shift.closedAt?.let(::receiptLocalTime) ?: "sigue abierto"
    val who =
        listOfNotNull(
            shift.openedBy?.let { "abrió $it" },
            shift.closedBy?.let { "cerró $it" },
            "cierre automático".takeIf { shift.automatic },
        ).joinToString(" · ")
    return "De $from a $to" + if (who.isNotEmpty()) " · $who" else ""
}

private fun LazyListScope.dayCutItems(
    state: AdminCashCutUiState,
    viewModel: AdminCashCutViewModel,
    colors: OperationalColors,
) {
    item(key = "day") {
        val day = state.shownDay
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AdminRoundButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Día anterior", colors) {
                viewModel.moveDay(-1)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    cutDayLabel(day),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                state.dayCut?.let { cut ->
                    Box(Modifier.padding(top = 4.dp)) {
                        AdminBadge(
                            when {
                                cut.final -> "Corte final"
                                cut.shifts.isEmpty() -> "Sin turnos"
                                else -> "Corte parcial"
                            },
                            colors,
                            tone = if (cut.final) AdminBadgeTone.OK else AdminBadgeTone.WARN,
                        )
                    }
                }
            }
            AdminRoundButton(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Día siguiente", colors) {
                viewModel.moveDay(1)
            }
        }
    }
    val cut = state.dayCut
    when {
        cut == null && state.dayError != null ->
            item(key = "day-error") { AdminLoadError(state.dayError, colors, viewModel::loadDay) }
        cut == null -> item(key = "day-loading") { AdminHint("Cargando el corte…", colors) }
        cut.shifts.isEmpty() ->
            item(key = "day-empty") {
                AdminEmptyState(
                    icon = Icons.Outlined.PointOfSale,
                    title = "No hubo caja este día",
                    message = "El corte sale de los turnos de Caja. Abre la caja para empezar uno.",
                    colors = colors,
                )
            }
        else -> {
            item(key = "day-cash") {
                CashPanel(
                    cut.cash,
                    colors,
                    title = "Efectivo del día",
                    pendingNote = "Falta cerrar un turno para comparar con lo contado.".takeIf { !cut.final },
                )
            }
            item(key = "shifts-title") {
                AdminH2(if (cut.shifts.size == 1) "1 turno" else "${cut.shifts.size} turnos", colors)
            }
            item(key = "shifts") {
                AdminPanel(colors, padding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)) {
                    cut.shifts.forEachIndexed { index, shift ->
                        if (index > 0) AdminRowDivider(colors)
                        AdminRow(
                            title = "Turno ${index + 1}" + (shift.openedBy?.let { " · $it" } ?: ""),
                            detail = shiftHours(shift),
                            colors = colors,
                            onClick = { viewModel.openShift(shift.id) },
                        ) {
                            ArqueoBadge(shift.cash, colors, open = shift.open)
                        }
                    }
                }
            }
            breakdownItems(cut.breakdown, colors, keyPrefix = "day")
        }
    }
}

private fun LazyListScope.reportItems(
    state: AdminCashCutUiState,
    viewModel: AdminCashCutViewModel,
    colors: OperationalColors,
) {
    item(key = "periods") {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReportPeriod.entries.forEach { period ->
                AdminFilterChip(
                    label = period.label,
                    selected = state.period == period,
                    colors = colors,
                    onClick = { viewModel.selectPeriod(period) },
                )
            }
        }
    }
    val report = state.report
    when {
        report == null && state.reportError != null ->
            item(key = "report-error") { AdminLoadError(state.reportError, colors, viewModel::loadReport) }
        report == null -> item(key = "report-loading") { AdminHint("Cargando el reporte…", colors) }
        else -> {
            item(key = "report-range") {
                AdminHint(
                    if (report.from == report.to) {
                        cutDayLabel(runCatching { LocalDate.parse(report.from) }.getOrNull())
                    } else {
                        "Del ${report.from} al ${report.to}"
                    } + ". Una venta cuenta el día en que se cobró.",
                    colors,
                )
            }
            item(key = "export") {
                AdminPrimaryButton(
                    title = "Compartir para el contador (CSV)",
                    colors = colors,
                    icon = Icons.Outlined.Share,
                    busy = state.exporting,
                    onClick = viewModel::exportCsv,
                )
            }
            breakdownItems(report.breakdown, colors, keyPrefix = "report", withProducts = true, withDays = true)
        }
    }
}

/** El arqueo: lo que debe haber, lo contado y si cuadra, sobra o falta. */
@Composable
private fun CashPanel(
    cash: CutCash,
    colors: OperationalColors,
    title: String,
    pendingNote: String? = null,
) {
    AdminPanel(colors, highlighted = cash.result == CashCountResult.SHORT) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMuted,
                modifier = Modifier.weight(1f),
            )
            ArqueoBadge(cash, colors, open = cash.counted == null)
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BigFigure("Debe haber", moneyLabel(cash.expected), colors, Modifier.weight(1f))
            BigFigure("Contado", cash.counted?.let(::moneyLabel) ?: "—", colors, Modifier.weight(1f))
            BigFigure("Diferencia", signedMoney(cash.difference), colors, Modifier.weight(1f))
        }
        Column(Modifier.padding(top = 10.dp)) {
            CashLine("Fondo inicial", cash.float, colors)
            CashLine("Cobros en efectivo", cash.cashSales, colors)
            CashLine("Propinas en efectivo", cash.tips, colors)
            if (cash.reloads.isNotZero()) CashLine("Recargas de saldo", cash.reloads, colors)
            if (cash.cashIn.isNotZero()) CashLine("Entradas al cajón", cash.cashIn, colors)
            if (cash.cashOut.isNotZero()) CashLine("Salidas del cajón", cash.cashOut, colors, negative = true)
            if (cash.refunds.isNotZero()) CashLine("Devoluciones en efectivo", cash.refunds, colors, negative = true)
        }
        pendingNote?.let { AdminHint(it, colors) }
    }
}

private fun String.isNotZero(): Boolean = (toBigDecimalOrNull()?.signum() ?: 0) != 0

@Composable
private fun BigFigure(
    label: String,
    value: String,
    colors: OperationalColors,
    modifier: Modifier,
) {
    Column(modifier) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textMuted, maxLines = 1)
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = colors.textPrimary, maxLines = 1)
    }
}

@Composable
private fun CashLine(
    label: String,
    amount: String,
    colors: OperationalColors,
    negative: Boolean = false,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            (if (negative) "−" else "") + moneyLabel(amount),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
        )
    }
}

@Composable
private fun ArqueoBadge(
    cash: CutCash,
    colors: OperationalColors,
    open: Boolean,
) {
    val result = cash.result
    when {
        open -> AdminBadge("Abierto", colors, tone = AdminBadgeTone.WARN, dot = true)
        result == CashCountResult.BALANCED -> AdminBadge("Cuadra", colors, tone = AdminBadgeTone.OK)
        result == CashCountResult.OVER ->
            AdminBadge("Sobra ${signedMoney(cash.difference)}", colors, tone = AdminBadgeTone.WARN)
        result == CashCountResult.SHORT ->
            AdminBadge("Falta ${signedMoney(cash.difference)}", colors, tone = AdminBadgeTone.DANGER)
        else -> AdminBadge("Cerrado", colors)
    }
}

/** Los bloques que comparten el corte y el reporte. */
private fun LazyListScope.breakdownItems(
    b: SalesBreakdown,
    colors: OperationalColors,
    keyPrefix: String,
    withProducts: Boolean = false,
    withDays: Boolean = false,
) {
    item(key = "$keyPrefix-sales") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AdminStatPanel("Ventas", moneyLabel(b.summary.sales), colors, Modifier.weight(1f))
            AdminStatPanel("Pedidos", b.summary.orders.toString(), colors, Modifier.weight(1f))
        }
    }
    item(key = "$keyPrefix-sales2") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AdminStatPanel("Ticket promedio", moneyLabel(b.summary.averageTicket), colors, Modifier.weight(1f))
            AdminStatPanel("Propinas", moneyLabel(b.tips.total), colors, Modifier.weight(1f))
        }
    }
    item(key = "$keyPrefix-payments-title") { AdminH2("Por forma de pago", colors) }
    item(key = "$keyPrefix-payments") {
        LinesPanel(colors, emptyText = "Sin ventas todavía.", empty = b.payments.isEmpty()) {
            b.payments.forEachIndexed { i, p ->
                if (i > 0) AdminRowDivider(colors)
                BarRow(p.label, "${p.orders} pedidos", p.amount, shareOf(p.amount, b.summary.sales), colors)
            }
        }
    }
    item(key = "$keyPrefix-extras") {
        AdminPanel(colors) {
            CashLine("Propinas con terminal", b.tips.terminal, colors)
            CashLine("Propinas en efectivo", b.tips.cash, colors)
            if (b.summary.rentals.isNotZero()) CashLine("Rentas de pista o cancha", b.summary.rentals, colors)
            if (b.summary.discounts.isNotZero()) CashLine("Descuentos", b.summary.discounts, colors, negative = true)
            if (b.summary.delivery.isNotZero()) CashLine("Envíos", b.summary.delivery, colors)
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text(
                    "Cancelados",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${b.cancelled.orders} · ${moneyLabel(b.cancelled.amount)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
            }
        }
    }
    item(key = "$keyPrefix-cat-title") { AdminH2("Por categoría", colors) }
    item(key = "$keyPrefix-cat") {
        val total = b.summary.itemsAmount
        LinesPanel(colors, emptyText = "Sin artículos vendidos.", empty = b.categories.isEmpty()) {
            b.categories.forEachIndexed { i, c ->
                if (i > 0) AdminRowDivider(colors)
                BarRow(c.name, "${c.quantity} vendidos", c.amount, shareOf(c.amount, total), colors)
            }
        }
    }
    if (withProducts) {
        item(key = "$keyPrefix-prod-title") { AdminH2("Lo más vendido", colors) }
        item(key = "$keyPrefix-prod") {
            LinesPanel(colors, emptyText = "Sin artículos vendidos.", empty = b.products.isEmpty()) {
                b.products.take(TOP_PRODUCTS).forEachIndexed { i, p ->
                    if (i > 0) AdminRowDivider(colors)
                    AdminRow(title = p.name, detail = "${p.category} · ${p.quantity} vendidos", colors = colors) {
                        AdminRowValue(moneyLabel(p.amount), colors)
                    }
                }
            }
        }
    }
    item(key = "$keyPrefix-staff-title") { AdminH2("Por mesero", colors) }
    item(key = "$keyPrefix-staff") {
        LinesPanel(colors, emptyText = "Sin pedidos.", empty = b.staff.isEmpty()) {
            b.staff.forEachIndexed { i, s ->
                if (i > 0) AdminRowDivider(colors)
                val role = staffRoleLabel(s.role).takeIf { it.isNotEmpty() && s.userId != null }
                BarRow(
                    s.label,
                    listOfNotNull(role, "${s.orders} pedidos", s.averageTicket?.let { "ticket ${moneyLabel(it)}" })
                        .joinToString(" · "),
                    s.sales,
                    shareOf(s.sales, b.summary.sales),
                    colors,
                )
            }
        }
    }
    item(key = "$keyPrefix-space-title") { AdminH2("Por mesa o pista", colors) }
    item(key = "$keyPrefix-space") {
        LinesPanel(colors, emptyText = "Sin pedidos.", empty = b.spaces.isEmpty()) {
            b.spaces.forEachIndexed { i, s ->
                if (i > 0) AdminRowDivider(colors)
                val detail =
                    if (s.rentals.isNotZero()) {
                        "${s.orders} pedidos · renta ${moneyLabel(s.rentals)} · consumo ${moneyLabel(s.consumption)}"
                    } else {
                        "${s.orders} pedidos"
                    }
                BarRow(s.label, detail, s.sales, shareOf(s.sales, b.summary.sales), colors)
            }
        }
    }
    if (withDays && b.days.size > 1) {
        item(key = "$keyPrefix-days-title") { AdminH2("Por día", colors) }
        item(key = "$keyPrefix-days") {
            LinesPanel(colors, emptyText = "", empty = false) {
                b.days.forEachIndexed { i, d ->
                    if (i > 0) AdminRowDivider(colors)
                    AdminRow(
                        title = cutDayLabel(runCatching { LocalDate.parse(d.date) }.getOrNull()),
                        detail = "${d.orders} pedidos",
                        colors = colors,
                    ) { AdminRowValue(moneyLabel(d.sales), colors) }
                }
            }
        }
    }
}

private const val TOP_PRODUCTS = 15

@Composable
private fun LinesPanel(
    colors: OperationalColors,
    emptyText: String,
    empty: Boolean,
    content: @Composable () -> Unit,
) {
    AdminPanel(colors, padding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
        if (empty) {
            Text(
                emptyText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMuted,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        } else {
            content()
        }
    }
}

/** Una fila con su barra de qué parte del total es. */
@Composable
private fun BarRow(
    title: String,
    detail: String,
    amount: String,
    fraction: Float,
    colors: OperationalColors,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary,
                    maxLines = 2,
                )
                Text(detail, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = colors.textMuted)
            }
            AdminRowValue(moneyLabel(amount), colors)
        }
        AdminBar(fraction, colors, Modifier.padding(top = 6.dp))
    }
}

/** Guarda el CSV en la caché y abre «Compartir» (correo, WhatsApp, Drive). */
private fun shareCsv(
    context: Context,
    fileName: String,
    content: String,
): Boolean =
    runCatching {
        val folder = File(context.cacheDir, "reportes").apply { mkdirs() }
        val file = File(folder, fileName.replace(Regex("[^A-Za-z0-9._-]+"), "-"))
        file.writeText(content, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.product_photos", file)
        val send =
            Intent(Intent.ACTION_SEND)
                .setType("text/csv")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .putExtra(Intent.EXTRA_SUBJECT, fileName.removeSuffix(".csv").replace('-', ' '))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, "Reporte contable").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess
