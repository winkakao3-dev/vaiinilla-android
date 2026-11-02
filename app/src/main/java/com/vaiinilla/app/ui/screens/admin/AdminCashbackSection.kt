package com.vaiinilla.app.ui.screens.admin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.admin.CashbackRule
import com.vaiinilla.app.domain.admin.CashbackRules
import com.vaiinilla.app.ui.admin.AdminPeriodPreset
import com.vaiinilla.app.ui.admin.AdminStoreViewModel
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.screens.OperationalColors
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Cashback y wallet como el lienzo de Doop (marcos 55 y 56): el porcentaje en grande sobre lima cuando está activo, sus
 * días, horario y vigencia, y abajo cómo se mueve la cartera de los clientes en el periodo elegido.
 */
@Composable
internal fun AdminCashbackSection(
    viewModel: AdminStoreViewModel,
    colors: OperationalColors,
    readOnly: Boolean,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState
    val saved = state.cashback ?: CashbackRule("Regla principal", "5", null, null, null, null, null, active = false)
    var draft by remember { mutableStateOf<CashbackRule?>(null) }
    var reviewing by remember { mutableStateOf(false) }
    LaunchedEffect(state.cashback, state.cashbackLoaded) { if (state.cashbackLoaded) draft = saved }

    AdminSectionPage(
        title = "Cashback y wallet",
        colors = colors,
        notice = state.notice,
        noticeIsError = state.noticeIsError,
        onBack = onBack,
    ) {
        item { AdminCapsLabel("Regla principal", colors, Modifier.padding(bottom = 8.dp)) }
        item {
            val rule = draft
            if (rule == null) {
                AdminHint("Cargando la regla…", colors)
            } else {
                Box(Modifier.adminRise(1)) {
                    RuleEditor(
                        rule,
                        saved,
                        colors,
                        readOnly,
                        onChange = { draft = it },
                        onReview = { reviewing = true },
                    )
                }
            }
        }
        state.cashback?.updatedAt?.let { adminShortDate(it) }?.let { updated ->
            item {
                Box(
                    Modifier.padding(top = 10.dp, start = 4.dp),
                ) { AdminHint("Última actualización: $updated", colors) }
            }
        }
        item { AdminCapsLabel("Cartera de clientes", colors, Modifier.padding(top = 22.dp, bottom = 8.dp)) }
        item { Box(Modifier.adminRise(3)) { Wallet(viewModel, colors) } }
    }

    val pending = draft
    if (reviewing && pending != null) {
        ReviewSheet(state.cashback, pending, colors, state.saving, state.notice.takeIf { state.noticeIsError }, {
            reviewing = false
        }) { viewModel.saveCashback(pending) { reviewing = false } }
    }
}

private fun percentOf(rule: CashbackRule): BigDecimal = rule.percent.toBigDecimalOrNull() ?: BigDecimal.ZERO

private fun percentLabel(value: BigDecimal): String = value.stripTrailingZeros().toPlainString()

@Composable
private fun RuleEditor(
    rule: CashbackRule,
    saved: CashbackRule,
    colors: OperationalColors,
    readOnly: Boolean,
    onChange: (CashbackRule) -> Unit,
    onReview: () -> Unit,
) {
    val percent = percentOf(rule)

    fun setPercent(value: BigDecimal) {
        onChange(rule.copy(percent = percentLabel(value.max(BigDecimal.ZERO).min(BigDecimal(100)))))
    }
    val haptics = rememberVaiinillaHaptics()
    val bg by animateColorAsState(
        if (rule.active) colors.accentLime else colors.cardBackground,
        spring(),
        label = "cashback-bg",
    )
    val ink = if (rule.active) colors.accentInk else colors.textPrimary
    Column(Modifier.animateContentSize(spring(stiffness = 500f))) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(bg)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AnimatedContent(
                targetState = percent,
                transitionSpec = {
                    val up = targetState > initialState
                    (slideInVertically(spring(stiffness = 600f)) { if (up) it else -it } + fadeIn()) togetherWith
                        (slideOutVertically(spring(stiffness = 600f)) { if (up) -it else it } + fadeOut())
                },
                label = "cashback-percent",
            ) { value ->
                Text(
                    "${percentLabel(value)}%",
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2).sp,
                    color = ink,
                    maxLines = 1,
                )
            }
            Text(
                if (rule.active) "de cada compra regresa al saldo del cliente" else "Apagado: no se regresa saldo",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (rule.active) colors.accentInk else colors.textMuted,
                modifier = Modifier.weight(1f),
            )
            if (!readOnly) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoundStep(Icons.Outlined.Add, "Subir porcentaje", ink) {
                        haptics.click()
                        setPercent(percent + BigDecimal.ONE)
                    }
                    RoundStep(Icons.Outlined.Remove, "Bajar porcentaje", ink) {
                        haptics.click()
                        setPercent(percent - BigDecimal.ONE)
                    }
                }
            }
        }
        AdminPanel(
            colors,
            Modifier.padding(top = 10.dp),
            padding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
        ) {
            AdminToggleRow(
                title = "Cashback activo",
                checked = rule.active,
                colors = colors,
                onChange = { onChange(rule.copy(active = it)) },
                detail = "Automático al entregar cada pedido",
                enabled = !readOnly,
            )
        }
        Question("Días", colors)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val days = rule.activeDays.orEmpty()
            CashbackRules.WEEK.forEach { (day, label) ->
                val on = days.isEmpty() || day in days
                val dayBg by animateColorAsState(
                    if (on) colors.textPrimary else colors.cardBackground,
                    spring(),
                    label = "dia-$day",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .physicalPress(enabled = !readOnly, scale = PhysicalPressScale.Small) {
                            haptics.selection()
                            val base = if (days.isEmpty()) CashbackRules.WEEK.map { it.first } else days
                            val next = if (day in base) base - day else base + day
                            onChange(
                                rule.copy(
                                    activeDays =
                                        next
                                            .takeUnless {
                                                it.isEmpty() ||
                                                    it.size == CashbackRules.WEEK.size
                                            }?.sorted(),
                                ),
                            )
                        }.semantics {
                            contentDescription = label
                            selected = on
                        }.heightIn(min = 42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(dayBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label.take(1).uppercase(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = if (on) colors.background else colors.textMuted,
                    )
                }
            }
        }
        Question("Horario", colors)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdminTextField(
                label = "Desde",
                value = rule.startTime.orEmpty(),
                onValueChange = { onChange(rule.copy(startTime = timeInput(it))) },
                colors = colors,
                modifier = Modifier.weight(1f),
                placeholder = "Todo el día",
                keyboardType = KeyboardType.Number,
                enabled = !readOnly,
            )
            AdminTextField(
                label = "Hasta",
                value = rule.endTime.orEmpty(),
                onValueChange = { onChange(rule.copy(endTime = timeInput(it))) },
                colors = colors,
                modifier = Modifier.weight(1f),
                placeholder = "—",
                keyboardType = KeyboardType.Number,
                enabled = !readOnly,
            )
        }
        Question("Vigencia de la regla", colors)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdminTextField(
                label = "Desde",
                value = rule.validFrom.orEmpty(),
                onValueChange = { onChange(rule.copy(validFrom = dateInput(it))) },
                colors = colors,
                modifier = Modifier.weight(1f),
                placeholder = "Siempre",
                keyboardType = KeyboardType.Number,
                enabled = !readOnly,
            )
            AdminTextField(
                label = "Hasta",
                value = rule.validTo.orEmpty(),
                onValueChange = { onChange(rule.copy(validTo = dateInput(it))) },
                colors = colors,
                modifier = Modifier.weight(1f),
                placeholder = "AAAA-MM-DD",
                keyboardType = KeyboardType.Number,
                enabled = !readOnly,
            )
        }
        AdminTextField(
            label = "Nombre de la regla",
            value = rule.name,
            onValueChange = { onChange(rule.copy(name = it.take(120))) },
            colors = colors,
            modifier = Modifier.padding(top = 8.dp),
            enabled = !readOnly,
        )
        if (!readOnly && rule != saved) {
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminSecondaryButton(
                    title = "Descartar",
                    colors = colors,
                    modifier = Modifier.weight(1f),
                    outlined = true,
                ) { onChange(saved) }
                AdminPrimaryButton(
                    title = "Revisar cambio",
                    colors = colors,
                    modifier = Modifier.weight(1f),
                    enabled = CashbackRules.percent(rule.percent) != null,
                    onClick = onReview,
                )
            }
        }
    }
}

@Composable
private fun Question(
    text: String,
    colors: OperationalColors,
) {
    Text(
        text,
        fontSize = 14.sp,
        fontWeight = FontWeight.ExtraBold,
        color = colors.textPrimary,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun RoundStep(
    icon: ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
            .size(36.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f))
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp)) }
}

@Composable
private fun Wallet(
    viewModel: AdminStoreViewModel,
    colors: OperationalColors,
) {
    val state by viewModel.uiState
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AdminPeriodPreset.entries.forEach { preset ->
                AdminFilterChip(
                    label = preset.label,
                    selected = preset == state.walletPreset,
                    colors = colors,
                    onClick = { viewModel.selectWalletPeriod(preset) },
                )
            }
        }
        val wallet = state.wallet
        if (wallet == null) {
            AdminHint("Cargando…", colors)
            return@Column
        }
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdminStatPanel("Recargas", moneyLabel(wallet.reloads), colors, Modifier.weight(1f).fillMaxHeight())
            AdminStatPanel(
                "Compras con saldo",
                moneyLabel(wallet.purchases),
                colors,
                Modifier.weight(1f).fillMaxHeight(),
            )
            AdminStatPanel("Cashback dado", moneyLabel(wallet.cashback), colors, Modifier.weight(1f).fillMaxHeight())
        }
        AdminPanel(colors, padding = PaddingValues(horizontal = 16.dp, vertical = 2.dp)) {
            AdminRow(
                title = "Cancelaciones devueltas",
                colors = colors,
                detail = wallet.canceledOrders?.let { if (it == 1) "1 pedido cancelado" else "$it pedidos cancelados" },
            ) { AdminRowValue(moneyLabel(wallet.cancellations), colors) }
            wallet.movements.forEach { movement ->
                AdminRowDivider(colors)
                AdminRow(
                    title = movement.label,
                    colors = colors,
                    detail = if (movement.orders == 1) "1 operación" else "${movement.orders} operaciones",
                ) { AdminRowValue(moneyLabel(movement.amount), colors) }
            }
        }
        Box(Modifier.padding(horizontal = 4.dp)) {
            AdminHint(
                "Conciliación: ${wallet.walletsReviewed} carteras revisadas" +
                    if (wallet.alerts > 0) ", ${wallet.alerts} con alerta." else ", sin alertas.",
                colors,
            )
        }
    }
}

/** «Revisar cambio»: antes y ahora lado a lado, y cuánto regresa una compra de ejemplo. */
@Composable
private fun ReviewSheet(
    before: CashbackRule?,
    after: CashbackRule,
    colors: OperationalColors,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AdminSheet(title = "Revisar cambio", colors = colors, busy = saving, onDismiss = onDismiss) {
        Text(
            "Aplica a las compras desde que guardes. Lo que tus clientes ya tienen no cambia.",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textMuted,
        )
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.cardBackground)) {
            CompareRow("Antes", "Ahora", colors, header = true)
            CompareRow(
                before?.let { "${it.percent}%" } ?: "—",
                "${CashbackRules.percent(after.percent) ?: after.percent}%",
                colors,
                changed = before?.percent != after.percent,
            )
            CompareRow(
                before?.let { if (it.active) "Activa" else "Apagada" } ?: "—",
                if (after.active) "Activa" else "Apagada",
                colors,
                changed = before?.active != after.active,
            )
            CompareRow(
                before?.let { CashbackRules.daysLabel(it.activeDays) } ?: "—",
                CashbackRules.daysLabel(after.activeDays),
                colors,
                changed = before?.activeDays != after.activeDays,
            )
            CompareRow(
                before?.let(::hours) ?: "—",
                hours(after),
                colors,
                changed = before?.let(::hours) != hours(after),
            )
            CompareRow(
                before?.let(::validity) ?: "—",
                validity(after),
                colors,
                changed = before?.let(::validity) != validity(after),
            )
        }
        CashbackRules.percent(after.percent)?.toBigDecimalOrNull()?.let { percent ->
            val back = Example.multiply(percent).divide(BigDecimal(100), 2, RoundingMode.HALF_UP).toPlainString()
            Text(
                buildAnnotatedString {
                    append("Con este cambio, una compra de ")
                    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append(moneyLabel("480.00")) }
                    append(" le regresa ")
                    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append(moneyLabel(back)) }
                    append(" de saldo al cliente.")
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.accentLime.copy(alpha = 0.12f))
                        .border(1.dp, colors.accentLime.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .padding(14.dp),
            )
        }
        AdminSheetError(error)
        AdminPrimaryButton(
            title = "Guardar regla",
            colors = colors,
            modifier = Modifier.fillMaxWidth(),
            busy = saving,
            onClick = onSave,
        )
    }
}

private val Example = BigDecimal(480)

@Composable
private fun CompareRow(
    left: String,
    right: String,
    colors: OperationalColors,
    header: Boolean = false,
    changed: Boolean = false,
) {
    Column {
        if (!header) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.cardBorder))
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            CompareCell(left, colors, header, strong = false, accent = false, Modifier.weight(1f))
            Box(Modifier.width(1.dp).fillMaxHeight().background(colors.cardBorder))
            CompareCell(right, colors, header, strong = !header, accent = changed && !header, Modifier.weight(1f))
        }
    }
}

@Composable
private fun CompareCell(
    text: String,
    colors: OperationalColors,
    header: Boolean,
    strong: Boolean,
    accent: Boolean,
    modifier: Modifier,
) {
    Text(
        if (header) text.uppercase() else text,
        fontSize = if (header) 11.sp else 14.sp,
        fontWeight = if (header || strong) FontWeight.ExtraBold else FontWeight.SemiBold,
        letterSpacing = if (header) 1.4.sp else 0.sp,
        color =
            when {
                header -> colors.textMuted
                accent -> colors.accentInk
                else -> colors.textPrimary
            },
        modifier = modifier.padding(horizontal = 14.dp, vertical = 12.dp),
    )
}

private fun hours(rule: CashbackRule): String {
    val start = rule.startTime
    val end = rule.endTime
    return if (start == null || end == null) "Todo el día" else "$start–$end"
}

private fun validity(rule: CashbackRule): String =
    if (rule.validFrom == null &&
        rule.validTo == null
    ) {
        "Siempre"
    } else {
        "${rule.validFrom ?: "…"} a ${rule.validTo ?: "…"}"
    }

/** Deja escribir "8", "08:", "08:30"; vacío = sin hora. */
private fun timeInput(raw: String): String? = raw.filter { it.isDigit() || it == ':' }.take(5).ifEmpty { null }

private fun dateInput(raw: String): String? = raw.filter { it.isDigit() || it == '-' }.take(10).ifEmpty { null }
