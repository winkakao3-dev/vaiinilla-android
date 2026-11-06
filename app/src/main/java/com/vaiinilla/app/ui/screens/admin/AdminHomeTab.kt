package com.vaiinilla.app.ui.screens.admin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.admin.AdminCount
import com.vaiinilla.app.domain.admin.AdminMetric
import com.vaiinilla.app.domain.admin.AdminPeriod
import com.vaiinilla.app.domain.admin.AdminSummary
import com.vaiinilla.app.domain.admin.AdminUnpaidAccount
import com.vaiinilla.app.domain.admin.AdminUnpaidAccounts
import com.vaiinilla.app.domain.admin.FORCED_CLOSE_REASON_MAX
import com.vaiinilla.app.domain.admin.FORCED_CLOSE_REASON_MIN
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.ui.admin.AdminPeriodPreset
import com.vaiinilla.app.ui.admin.AdminUiState
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.theme.Yolk
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Un paso de "Para dejar lista tu tienda"; [section] es a dónde lleva tocarlo. Uno [optional] (domicilio, tarjeta)
 * se ofrece pero no cuenta para que la tienda esté lista: un restaurante sin domicilio ni Stripe ya puede vender.
 */
internal data class AdminSetupStep(
    val title: String,
    val detail: String?,
    val done: Boolean,
    val section: AdminSection? = null,
    val optional: Boolean = false,
    /** Cómo se recuerda en el teléfono que el dueño dijo «No quiero» a este paso opcional. */
    val key: String = title,
)

/**
 * Quita los pasos opcionales que el dueño rechazó con «No quiero» y que siguen sin hacerse: ya no salen ni
 * cuentan como pendientes. Si después los configura, vuelven como hechos.
 */
internal fun List<AdminSetupStep>.withoutDeclined(declined: Set<String>): List<AdminSetupStep> =
    filterNot { it.optional && !it.done && it.key in declined }

/** Lista cuando no falta ningún paso obligatorio. */
internal fun List<AdminSetupStep>.storeReady(): Boolean = none { !it.done && !it.optional }

/**
 * Se ve mientras falte algo obligatorio. Antes también se quedaba con "sin ventas" en el periodo, y como el periodo
 * es "Hoy", volvía cada mañana hasta la primera venta: lista la tienda, ya no estorba.
 */
internal fun showsStoreSetup(setup: List<AdminSetupStep>): Boolean = setup.isNotEmpty() && !setup.storeReady()

/**
 * Inicio, como el lienzo de Doop (marcos 07 y 21 a 23): la foto del negocio con su nombre en grande, si recibe
 * pedidos y la caja, lo que falta para dejar lista la tienda, las ventas del periodo y el resto del resumen.
 */
@Composable
internal fun AdminHomeTab(
    state: AdminUiState,
    colors: OperationalColors,
    readOnly: Boolean,
    actions: AdminActions,
    header: AdminHeader,
    setup: List<AdminSetupStep>,
    onOpenOrders: () -> Unit,
    onOpenSection: (AdminSection) -> Unit,
    onSeeAsCustomer: () -> Unit = {},
    /** El asistente de configuración; null lo esconde. */
    onOpenAssistant: (() -> Unit)? = null,
    /** «No quiero» en un paso opcional; null lo esconde (solo lectura). */
    onDeclineStep: ((AdminSetupStep) -> Unit)? = null,
) {
    var choosingPeriod by remember { mutableStateOf(false) }
    val presentDialog = LocalAdminDialog.current
    val status = state.operationalStatus
    val receiving = status?.let { it.acceptingOrders && it.withinOrderWindow }
    val summary = state.summary
    AdminHeroPage(
        cover = header.photoUrl,
        caption = "Administración · ${header.placeName}",
        title = "Mi negocio",
        colors = colors,
        compactTitle = header.placeName,
        compactLogo = header.logoUrl,
        trailing = { header.avatar() },
    ) {
        item(key = "estado") { StatusLine(state, receiving, colors, Modifier.adminRise(1)) }
        status?.outsideOrderHoursMessage?.let { message ->
            item(key = "fuera-de-horario") {
                Text(
                    message,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMuted,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp),
                )
            }
        }
        item(key = "tienda-lista") {
            AnimatedVisibility(
                visible = showsStoreSetup(setup),
                enter =
                    fadeIn(spring(stiffness = 400f)) + expandVertically(spring(dampingRatio = 0.85f, stiffness = 400f)),
                exit = fadeOut(spring(stiffness = 600f)) + shrinkVertically(spring(stiffness = 600f)),
            ) {
                SetupChecklist(
                    setup,
                    colors,
                    Modifier.adminRise(2),
                    onOpenSection,
                    onSeeAsCustomer,
                    onOpenAssistant,
                    onDeclineStep,
                )
            }
        }
        item(key = "ventas-titulo") {
            SalesHeader(periodLabel(state), colors, Modifier.adminRise(3)) { choosingPeriod = true }
        }
        item(key = "ventas") {
            Box(Modifier.adminRise(3)) {
                when {
                    summary == null && state.summaryError != null ->
                        AdminLoadError(state.summaryError, colors, actions.onRefreshHome)
                    summary == null ->
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = colors.textPrimary,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    else -> SalesCard(summary, state.summaryLoading, colors)
                }
            }
        }
        if (summary != null) summaryBlocks(summary, state, colors)
        state.unpaid?.takeIf { it.accounts.isNotEmpty() }?.let { unpaid ->
            item(key = "sin-pagar") {
                UnpaidBlock(
                    unpaid,
                    colors,
                    canClose = !readOnly,
                    modifier = Modifier.adminRise(6),
                    onOpenOrders = onOpenOrders,
                ) { account ->
                    // R3: se pide el motivo y se dice qué se pierde; lo abonado sí entró a la caja.
                    presentDialog(
                        AdminDialogRequest(
                            title = "¿Cerrar la cuenta de ${account.spaceName} sin cobrar?",
                            message = forcedCloseMessage(account),
                            confirm = "Cerrar sin cobrar",
                            fieldPlaceholder = "¿Por qué? (p. ej. se fue sin pagar)",
                            accepts = { it.trim().length in FORCED_CLOSE_REASON_MIN..FORCED_CLOSE_REASON_MAX },
                        ) { reason ->
                            suspendCancellableCoroutine { done ->
                                actions.onCloseWithoutCharging(account, reason) { ok ->
                                    if (done.isActive) done.resume(ok)
                                }
                            }
                        },
                    )
                }
            }
        }
    }
    if (choosingPeriod) {
        PeriodSheet(state, colors, onDismiss = { choosingPeriod = false }) { choice, custom ->
            choosingPeriod = false
            when (choice) {
                AdminPeriodChoice.TODAY -> actions.onSelectPeriod(AdminPeriodPreset.TODAY)
                AdminPeriodChoice.WEEK -> actions.onSelectPeriod(AdminPeriodPreset.WEEK)
                AdminPeriodChoice.YESTERDAY, AdminPeriodChoice.MONTH ->
                    choice.period()?.let(actions.onSelectCustomPeriod)
                AdminPeriodChoice.CUSTOM -> custom?.let(actions.onSelectCustomPeriod)
            }
        }
    }
}

@Composable
private fun StatusLine(
    state: AdminUiState,
    receiving: Boolean?,
    colors: OperationalColors,
    modifier: Modifier,
) {
    val cashOpen = state.live?.cashOpen ?: state.operationalStatus?.cashSessionOpen
    val cashLine =
        when (cashOpen) {
            null -> null
            false -> "Caja cerrada"
            true -> state.cashOpeningAmount?.let { "Caja abierta · fondo ${moneyLabel(it)}" } ?: "Caja abierta"
        }
    Row(
        modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        receiving?.let {
            AdminBadge(
                if (it) "Recibiendo pedidos" else "Sin recibir pedidos",
                colors,
                tone = if (it) AdminBadgeTone.OK else AdminBadgeTone.DANGER,
                dot = true,
            )
        }
        Spacer(Modifier.weight(1f))
        cashLine?.let {
            Text(it, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = colors.textMuted, maxLines = 1)
        }
    }
}

@Composable
private fun SetupChecklist(
    setup: List<AdminSetupStep>,
    colors: OperationalColors,
    modifier: Modifier,
    onOpenSection: (AdminSection) -> Unit,
    onSeeAsCustomer: () -> Unit,
    onOpenAssistant: (() -> Unit)?,
    onDeclineStep: ((AdminSetupStep) -> Unit)?,
) {
    val ready = setup.storeReady()
    Column(modifier) {
        AdminH2(if (ready) "Tu tienda está lista" else "Para dejar lista tu tienda", colors)
        if (!ready && onOpenAssistant != null) {
            AssistantEntryCard(colors, Modifier.padding(bottom = 10.dp), onOpenAssistant)
        }
        AdminPanel(
            colors,
            padding =
                androidx.compose.foundation.layout
                    .PaddingValues(horizontal = 16.dp, vertical = 2.dp),
        ) {
            setup.forEachIndexed { index, step ->
                if (index > 0) AdminRowDivider(colors)
                AdminRow(
                    title = step.title,
                    colors = colors,
                    modifier = Modifier.semantics { stateDescription = if (step.done) "Listo" else "Falta" },
                    onClick = step.section?.takeIf { !step.done }?.let { section -> { onOpenSection(section) } },
                    leading = {
                        Box(
                            Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    if (step.done) colors.accentLime else colors.textPrimary.copy(alpha = 0.08f),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (step.done) Icons.Rounded.Check else Icons.Rounded.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (step.done) colors.accentInk else colors.textMuted,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    },
                ) {
                    if (step.optional && !step.done && onDeclineStep != null) {
                        DeclineStepButton(step.title, colors) { onDeclineStep(step) }
                    } else {
                        step.detail?.let { AdminRowValue(it, colors, muted = true) }
                    }
                }
            }
        }
        if (ready) {
            AdminPrimaryButton(
                title = "Así la ve tu cliente",
                colors = colors,
                modifier = Modifier.padding(top = 12.dp),
                icon = Icons.Outlined.ShoppingCart,
                onClick = onSeeAsCustomer,
            )
        }
    }
}

/** «No quiero» (marco 07c): el paso opcional se va de la lista y ya no cuenta como pendiente. */
@Composable
private fun DeclineStepButton(
    title: String,
    colors: OperationalColors,
    onClick: () -> Unit,
) {
    val haptics = rememberVaiinillaHaptics()
    Box(
        Modifier
            .heightIn(min = 44.dp)
            .physicalPress(scale = PhysicalPressScale.Small) {
                haptics.selection()
                onClick()
            }.semantics { contentDescription = "No quiero $title" },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .height(30.dp)
                .clip(RoundedCornerShape(50))
                .border(1.dp, colors.cardBorder, RoundedCornerShape(50))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("No quiero", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary, maxLines = 1)
        }
    }
}

@Composable
private fun SalesHeader(
    label: String,
    colors: OperationalColors,
    modifier: Modifier,
    onChoose: () -> Unit,
) {
    val haptics = rememberVaiinillaHaptics()
    Row(modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        AdminCapsLabel("Ventas · $label", colors, Modifier.weight(1f))
        Row(
            Modifier
                .heightIn(min = 44.dp)
                .physicalPress(scale = PhysicalPressScale.Small) {
                    haptics.selection()
                    onChoose()
                }.semantics { contentDescription = "Periodo: $label" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.cardBackground)
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

/** Las ventas en grande: los pesos en negro y los centavos chicos; al cambiar de periodo entra con un resorte. */
@Composable
private fun SalesCard(
    summary: AdminSummary,
    loading: Boolean,
    colors: OperationalColors,
) {
    val reduced = reducedMotion()
    AdminPanel(
        colors,
        padding =
            androidx.compose.foundation.layout
                .PaddingValues(horizontal = 20.dp, vertical = 18.dp),
    ) {
        AnimatedContent(
            targetState = summary.sales,
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
            val label = moneyLabel(value)
            val cut = label.lastIndexOf('.').takeIf { it > 0 } ?: label.length
            Text(
                buildAnnotatedString {
                    append(label.substring(0, cut))
                    withStyle(SpanStyle(fontSize = 22.sp, color = colors.textMuted)) { append(label.substring(cut)) }
                },
                fontSize = 50.sp,
                lineHeight = 54.sp,
                letterSpacing = (-1.8).sp,
                fontWeight = FontWeight.Black,
                color = if (loading) colors.textMuted else colors.textPrimary,
                maxLines = 1,
                // Un monto nunca se trunca: se achica antes que cortarse.
                autoSize = TextAutoSize.StepBased(minFontSize = 26.sp, maxFontSize = 50.sp),
            )
        }
        Text(
            "${if (summary.orders == 1) "1 pedido cobrado" else "${summary.orders} pedidos cobrados"} · " +
                "ticket promedio ${moneyLabel(summary.averageTicket)}",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textMuted,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.summaryBlocks(
    summary: AdminSummary,
    state: AdminUiState,
    colors: OperationalColors,
) {
    val top = summary.topProducts.take(5)
    if (top.isNotEmpty()) {
        item(key = "top") {
            Column(Modifier.adminRise(4)) {
                AdminH2("Lo más vendido", colors)
                val photos =
                    remember(state.catalog) {
                        state.catalog
                            ?.products
                            .orEmpty()
                            .associate { it.name.trim().lowercase() to it.imageUrl }
                    }
                AdminPanel(
                    colors,
                    padding =
                        androidx.compose.foundation.layout
                            .PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                ) {
                    top.forEachIndexed { index, item ->
                        if (index > 0) AdminRowDivider(colors)
                        AdminRow(
                            title = item.label,
                            colors = colors,
                            detail = if (item.count == 1) "1 vendido" else "${item.count} vendidos",
                            leading = { AdminThumb(photos[item.label.trim().lowercase()], item.label, colors) },
                        ) {
                            item.amount?.let { AdminRowValue(moneyLabel(it), colors) }
                        }
                    }
                }
            }
        }
    }
    val methods = summary.paymentMethods.filter { it.orders > 0 }
    if (methods.isNotEmpty()) {
        item(key = "pago") { PaymentBars(methods, colors, Modifier.adminRise(5)) }
    }
    if (summary.ordersByStatus.isNotEmpty()) {
        item(key = "estados") {
            Column(Modifier.adminRise(5)) {
                AdminH2("Pedidos por estado", colors)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    summary.ordersByStatus.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach {
                                AdminCountTile(
                                    "${it.count}",
                                    it.label,
                                    colors,
                                    Modifier.weight(1f),
                                    tint = statusTint(it, colors),
                                )
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
    val tips = summary.tips
    val canceled = summary.canceledOrders
    if (tips != null || canceled != null) {
        item(key = "propinas-cancelados") {
            Row(Modifier.adminRise(5), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                tips?.let { AdminCountTile(moneyLabel(it), "Propinas", colors, Modifier.weight(1f)) }
                canceled?.let {
                    AdminCountTile("$it", if (it == 1) "Cancelado" else "Cancelados", colors, Modifier.weight(1f))
                }
            }
        }
    }
    if (summary.dailySales.size > 1) {
        item(key = "por-dia") { DailySales(summary, colors, Modifier.adminRise(6)) }
    }
}

private fun statusTint(
    row: AdminCount,
    colors: OperationalColors,
): Color? =
    when (OrderState.entries.firstOrNull { it.wireValue == row.key }) {
        OrderState.AWAITING_ACCEPTANCE, OrderState.PENDING_PAYMENT -> if (colors.isDark) Yolk else null
        OrderState.DELIVERED -> if (colors.isDark) colors.accentLime else colors.accentInk
        OrderState.CANCELED -> colors.coralText
        else -> null
    }

@Composable
private fun PaymentBars(
    methods: List<AdminMetric>,
    colors: OperationalColors,
    modifier: Modifier,
) {
    val amounts = methods.map { it.amount.toBigDecimalOrNull()?.toFloat() ?: 0f }
    val total = amounts.sum().takeIf { it > 0f } ?: 1f
    Column(modifier) {
        AdminH2("Por método de pago", colors)
        AdminPanel(colors) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                methods.zip(amounts).forEachIndexed { index, (method, amount) ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                method.label,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                moneyLabel(method.amount),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = colors.textPrimary,
                            )
                        }
                        AdminBar(
                            fraction = (amount / total).coerceIn(0.02f, 1f),
                            colors = colors,
                            fill = if (index == 0) colors.accentLime else colors.textSecondary,
                        )
                    }
                }
            }
        }
    }
}

/** Barras de ventas por día: la de hoy en lima; cada una crece desde abajo con un resorte, una tras otra. */
@Composable
private fun DailySales(
    summary: AdminSummary,
    colors: OperationalColors,
    modifier: Modifier,
) {
    val days = summary.dailySales
    val values = days.map { it.sales.toBigDecimalOrNull() ?: BigDecimal.ZERO }
    val max = values.maxOrNull()?.takeIf { it > BigDecimal.ZERO } ?: BigDecimal.ONE
    val total = values.fold(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP).toPlainString()
    val everyLabel = days.size <= 7
    Column(modifier) {
        AdminH2("Ventas por día", colors)
        AdminPanel(colors) {
            Row(Modifier.fillMaxWidth()) {
                Text(
                    if (days.size == 1) "Último día" else "Últimos ${days.size} días",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMuted,
                    modifier = Modifier.weight(1f),
                )
                Text(moneyLabel(total), fontSize = 14.sp, fontWeight = FontWeight.Black, color = colors.textPrimary)
            }
            Row(
                Modifier.fillMaxWidth().height(128.dp).padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(if (everyLabel) 10.dp else 3.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                days.forEachIndexed { index, day ->
                    val last = index == days.lastIndex
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clearAndSetSemantics { contentDescription = "${day.date}: ${moneyLabel(day.sales)}" },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                            DailyBar(
                                fraction = (values[index].toFloat() / max.toFloat()).coerceIn(0.04f, 1f),
                                highlighted = last,
                                delayMs = index * 40L,
                                colors = colors,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (everyLabel || last || index == 0) dayLabel(day.date, last, short = everyLabel) else " ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (last) colors.textPrimary else colors.textMuted,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyBar(
    fraction: Float,
    highlighted: Boolean,
    delayMs: Long,
    colors: OperationalColors,
) {
    val reduced = reducedMotion()
    val grow = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduced) {
            delay(150 + delayMs)
            grow.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 260f))
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(fraction)
            .graphicsLayer {
                scaleY = grow.value.coerceAtLeast(0.001f)
                transformOrigin = TransformOrigin(0.5f, 1f)
            }.clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
            .background(
                if (highlighted) {
                    colors.accentLime
                } else {
                    colors.textPrimary.copy(
                        alpha = if (colors.isDark) 0.09f else 0.12f,
                    )
                },
            ),
    )
}

private val WeekdayFormat = DateTimeFormatter.ofPattern("EEE", Locale.forLanguageTag("es-MX"))
private val ShortDayFormat = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-MX"))

/** "Mié" para la semana y "27 oct" con más días; el último, si es hoy, dice "Hoy". */
private fun dayLabel(
    value: String,
    last: Boolean,
    short: Boolean,
): String {
    val date = runCatching { LocalDate.parse(value) }.getOrNull() ?: return value
    if (last && date == LocalDate.now()) return "Hoy"
    val text = date.format(if (short) WeekdayFormat else ShortDayFormat).replace(".", "")
    return text.replaceFirstChar { it.uppercase() }
}

/** R3: el texto del aviso de «Cerrar sin cobrar», con lo abonado y lo que falta. */
internal fun forcedCloseMessage(account: AdminUnpaidAccount): String {
    val paid = account.paid.toBigDecimalOrNull()?.signum() == 1
    val lost = moneyLabel(account.remaining)
    return buildString {
        if (paid) {
            append("Ya abonaron ${moneyLabel(account.paid)}: eso sí se queda en la caja. ")
            append("Se pierden los $lost que faltan. ")
        } else {
            append("Los $lost no entran a la caja ni a las ventas. ")
        }
        append("Lo que no se pagó se cancela y sale de Cocina. ")
        append("${account.spaceName} queda libre para otro cliente.")
    }
}

@Composable
private fun UnpaidBlock(
    unpaid: AdminUnpaidAccounts,
    colors: OperationalColors,
    canClose: Boolean,
    modifier: Modifier,
    onOpenOrders: () -> Unit,
    onClose: (AdminUnpaidAccount) -> Unit,
) {
    Column(modifier) {
        AdminH2("Cuentas sin pagar", colors)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            unpaid.accounts.forEach { account ->
                AdminPanel(colors) {
                    AdminRow(
                        title = account.spaceName,
                        colors = colors,
                        detail =
                            listOfNotNull(
                                adminOrdersText(account.orders),
                                account.hours.takeIf { it > 0 }?.let { "hace $it h" },
                                // R33: el monto de la derecha ya descuenta lo abonado.
                                account.paid
                                    .takeIf { it.toBigDecimalOrNull()?.signum() == 1 }
                                    ?.let { "abonado ${moneyLabel(it)}" },
                                "espacio liberado".takeIf { account.abandoned },
                            ).joinToString(" · "),
                        leading = { AdminIconBox(Icons.Outlined.QrCode2, colors) },
                    ) {
                        AdminRowValue(moneyLabel(account.remaining), colors)
                    }
                    if (canClose && !account.abandoned) {
                        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AdminSecondaryButton(
                                "Cerrar sin cobrar",
                                colors,
                                Modifier.weight(1f),
                                outlined = true,
                            ) { onClose(account) }
                            AdminPrimaryButton("Cobrar", colors, Modifier.weight(1f), onClick = onOpenOrders)
                        }
                    } else if (account.abandoned) {
                        // R1: sin turno, la cobra Caja desde «Cuentas por cobrar» (el admin no cobra cuentas).
                        Text(
                            "Cóbrala en Caja › Cuentas por cobrar.",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textMuted,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                }
            }
        }
        Text(
            when (unpaid.abandoned) {
                0 -> "Una cuenta sin pagar se queda abierta hasta que Caja la cobra o tú la cierras."
                1 -> "1 cuenta quedó sin pagar en un espacio ya liberado. Por cobrar en total: ${moneyLabel(
                    unpaid.remaining,
                )}."
                else ->
                    "${unpaid.abandoned} cuentas quedaron sin pagar en espacios ya liberados. " +
                        "Por cobrar en total: ${moneyLabel(unpaid.remaining)}."
            },
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 17.sp,
            color = if (unpaid.abandoned > 0) colors.coralText else colors.textMuted,
            modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 12.dp),
        )
    }
}

/** Los periodos de "Elige el periodo" del lienzo (marco 21). */
private enum class AdminPeriodChoice {
    TODAY,
    YESTERDAY,
    WEEK,
    MONTH,
    CUSTOM,
    ;

    fun period(today: LocalDate = LocalDate.now()): AdminPeriod? =
        when (this) {
            YESTERDAY -> today.minusDays(1).let { AdminPeriod(it, it) }
            MONTH -> AdminPeriod(today.withDayOfMonth(1), today)
            else -> null
        }

    val buttonTitle: String
        get() =
            when (this) {
                TODAY -> "Ver ventas de hoy"
                YESTERDAY -> "Ver ventas de ayer"
                WEEK -> "Ver los últimos 7 días"
                MONTH -> "Ver ventas del mes"
                CUSTOM -> "Ver esas fechas"
            }

    companion object {
        fun current(state: AdminUiState): AdminPeriodChoice {
            val custom = state.customPeriod
            return when {
                custom == null && state.preset == AdminPeriodPreset.TODAY -> TODAY
                custom == null && state.preset == AdminPeriodPreset.WEEK -> WEEK
                custom == null -> MONTH
                custom == YESTERDAY.period() -> YESTERDAY
                custom == MONTH.period() -> MONTH
                else -> CUSTOM
            }
        }
    }
}

private fun shortDay(date: LocalDate): String = date.format(ShortDayFormat).replace(".", "")

private fun periodLabel(state: AdminUiState): String {
    val custom = state.customPeriod ?: return state.preset.label
    return when (custom) {
        AdminPeriodChoice.YESTERDAY.period() -> "Ayer"
        AdminPeriodChoice.MONTH.period() -> "Este mes"
        else -> "${shortDay(custom.from)} – ${shortDay(custom.to)}"
    }
}

/** "Elige el periodo": la lista del lienzo; en la elegida, lo vendido. "Elegir fechas" abre el calendario. */
@Composable
private fun PeriodSheet(
    state: AdminUiState,
    colors: OperationalColors,
    onDismiss: () -> Unit,
    onApply: (AdminPeriodChoice, AdminPeriod?) -> Unit,
) {
    val current = remember { AdminPeriodChoice.current(state) }
    var choice by remember { mutableStateOf(current) }
    var custom by remember { mutableStateOf(state.customPeriod.takeIf { current == AdminPeriodChoice.CUSTOM }) }
    var pickingDates by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val week = AdminPeriod.lastDays(7, today)
    AdminSheet(title = "Elige el periodo", colors = colors, busy = false, onDismiss = onDismiss) {
        Text(
            "Las ventas, lo más vendido y los métodos de pago se calculan con este periodo.",
            fontSize = 13.sp,
            color = colors.textMuted,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        val sales: @Composable (AdminPeriodChoice) -> Unit = { option ->
            if (option == current) {
                state.summary?.sales?.let {
                    Text(
                        moneyLabel(it),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textPrimary,
                    )
                }
            }
        }
        AdminRadioRow("Hoy", choice == AdminPeriodChoice.TODAY, colors, {
            choice = AdminPeriodChoice.TODAY
        }, detail = dayName(today)) {
            sales(AdminPeriodChoice.TODAY)
        }
        AdminRadioRow(
            "Ayer",
            choice == AdminPeriodChoice.YESTERDAY,
            colors,
            { choice = AdminPeriodChoice.YESTERDAY },
            detail = dayName(today.minusDays(1)),
        ) { sales(AdminPeriodChoice.YESTERDAY) }
        AdminRadioRow(
            "Últimos 7 días",
            choice == AdminPeriodChoice.WEEK,
            colors,
            { choice = AdminPeriodChoice.WEEK },
            detail = "${shortDay(week.from)} – ${shortDay(week.to)}",
        ) { sales(AdminPeriodChoice.WEEK) }
        AdminRadioRow(
            "Este mes",
            choice == AdminPeriodChoice.MONTH,
            colors,
            { choice = AdminPeriodChoice.MONTH },
            detail = today.format(MonthFormat).replaceFirstChar { it.uppercase() },
        ) { sales(AdminPeriodChoice.MONTH) }
        AdminRadioRow(
            "Elegir fechas",
            choice == AdminPeriodChoice.CUSTOM,
            colors,
            {
                choice = AdminPeriodChoice.CUSTOM
                pickingDates = true
            },
            detail = custom?.let { "${shortDay(it.from)} – ${shortDay(it.to)}" } ?: "Desde y hasta",
        ) {
            Icon(
                Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        AdminPrimaryButton(
            choice.buttonTitle,
            colors,
            enabled = choice != AdminPeriodChoice.CUSTOM || custom != null,
        ) { onApply(choice, custom) }
    }
    if (pickingDates) {
        DateRangeDialog(colors, onDismiss = { pickingDates = false }) {
            pickingDates = false
            custom = it
        }
    }
}

private val MonthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-MX"))
private val DayNameFormat = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-MX"))

private fun dayName(date: LocalDate): String = date.format(DayNameFormat).replaceFirstChar { it.uppercase() }

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeDialog(
    colors: OperationalColors,
    onDismiss: () -> Unit,
    onPick: (AdminPeriod) -> Unit,
) {
    val todayMillis =
        LocalDate
            .now()
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
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
        DateRangePicker(
            state = pickerState,
            title = { Text("Desde y hasta", modifier = Modifier.padding(16.dp)) },
            showModeToggle = false,
        )
    }
}

private fun utcDay(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

internal fun adminOrdersText(count: Int): String = if (count == 1) "1 pedido" else "$count pedidos"
