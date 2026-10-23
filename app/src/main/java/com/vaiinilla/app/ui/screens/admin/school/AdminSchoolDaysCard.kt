package com.vaiinilla.app.ui.screens.admin.school

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.school.NoServiceDay
import com.vaiinilla.app.domain.school.SchoolDates
import com.vaiinilla.app.ui.admin.school.AdminSchoolUiState
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.screens.ActionPill
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.OperationalField
import com.vaiinilla.app.ui.screens.admin.AdminCard
import com.vaiinilla.app.ui.screens.admin.AdminHint
import com.vaiinilla.app.ui.screens.admin.AdminSectionTitle
import java.time.LocalDate

/** El calendario de la escuela: qué se vende cada día, cuánto se apartó y los días sin servicio. */
@Composable
internal fun MenuDaysCard(
    state: AdminSchoolUiState,
    products: List<Product>?,
    colors: OperationalColors,
    enabled: Boolean,
    actions: AdminSchoolActions,
) {
    val reduced = reducedMotion()
    val today = state.today
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedDate = selected?.let(LocalDate::parse)?.takeIf { it in state.dates } ?: today
    LoadOnSelect(selectedDate, actions.onLoadReserved)
    AdminCard(colors, Modifier.animateContentSize(schoolSpring())) {
        AdminSectionTitle("Cada día", colors)
        AdminHint(
            "Lo que se vende, lo que ya apartaron y los días sin servicio. Sin elegir productos, se vende todo el menú.",
            colors,
        )
        if (today == null || selectedDate == null) {
            AdminHint("Cargando los días…", colors)
            return@AdminCard
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.dates.forEach { date ->
                DayCell(date, today, date == selectedDate, state, colors) { selected = date.toString() }
            }
        }
        AnimatedContent(
            targetState = selectedDate,
            transitionSpec = {
                if (reduced) fadeIn(snap()) togetherWith fadeOut(snap()) else fadeIn() togetherWith fadeOut()
            },
            label = "school-day",
        ) { date ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val closure = state.menu?.closure(date)
                if (closure != null) {
                    ClosedDay(closure, colors, enabled && !state.saving) { actions.onReopenDay(date) }
                } else {
                    ReservedSummary(state, date, colors)
                    MenuEditor(state, date, today, products, colors, enabled, actions)
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    today: LocalDate,
    selected: Boolean,
    state: AdminSchoolUiState,
    colors: OperationalColors,
    onSelect: () -> Unit,
) {
    val haptics = rememberVaiinillaHaptics()
    val hasMenu = state.menu?.menu(date) != null
    val closed = state.menu?.closure(date) != null
    val shape = RoundedCornerShape(16.dp)
    val ink =
        when {
            selected -> colors.background
            SchoolDates.isWeekend(date) || closed -> colors.textSecondary
            else -> colors.textPrimary
        }
    val label =
        SchoolDates.longLabel(date, today) +
            if (closed) {
                ", sin servicio"
            } else if (hasMenu) {
                ", con menú"
            } else {
                ""
            }
    Column(
        modifier =
            Modifier
                .physicalPress(scale = PhysicalPressScale.Small) {
                    haptics.selection()
                    onSelect()
                }.size(width = 48.dp, height = 64.dp)
                .clip(shape)
                .background(if (selected) colors.textPrimary else colors.cardBackground)
                .border(1.dp, if (selected) colors.textPrimary else colors.cardBorder, shape)
                .semantics(mergeDescendants = true) {
                    role = Role.Tab
                    this.selected = selected
                    contentDescription = label
                },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            if (date == today) "Hoy" else SchoolDates.weekdayShort(date),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = ink,
        )
        Text(
            date.dayOfMonth.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = ink,
            textDecoration = if (closed) TextDecoration.LineThrough else null,
        )
        Box(
            Modifier
                .padding(top = 2.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(if (hasMenu && !closed) ink else colors.cardBackground.copy(alpha = 0f)),
        )
    }
}

@Composable
private fun ClosedDay(
    reason: String,
    colors: OperationalColors,
    enabled: Boolean,
    onReopen: () -> Unit,
) {
    Column(Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                Icons.Outlined.Bedtime,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Sin servicio", fontSize = 15.sp, fontWeight = FontWeight.Black, color = colors.textPrimary)
                Text(reason, fontSize = 14.sp, color = colors.textPrimary)
                Text("Nadie puede apartar este día.", fontSize = 13.sp, color = colors.textSecondary)
            }
        }
        SchoolButton("Volver a abrir", colors, enabled, onClick = onReopen)
    }
}

@Composable
private fun ReservedSummary(
    state: AdminSchoolUiState,
    date: LocalDate,
    colors: OperationalColors,
) {
    val day = state.reserved[date]
    Column(Modifier.padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (day == null) {
            AdminHint("Viendo lo apartado…", colors)
            return@Column
        }
        Text(
            when (day.orders) {
                0 -> "Nada apartado todavía"
                1 -> "Apartado · 1 pedido"
                else -> "Apartado · ${day.orders} pedidos"
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textSecondary,
        )
        day.lines.forEach { line ->
            Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
                Text(
                    line.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    line.quantity.toString(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun MenuEditor(
    state: AdminSchoolUiState,
    date: LocalDate,
    today: LocalDate,
    products: List<Product>?,
    colors: OperationalColors,
    enabled: Boolean,
    actions: AdminSchoolActions,
) {
    val haptics = rememberVaiinillaHaptics()
    val saved = state.menu?.menu(date).orEmpty()
    var picked by remember(date, saved) { mutableStateOf<Set<Int>?>(null) }
    var closing by remember(date) { mutableStateOf(false) }
    var reason by remember(date) { mutableStateOf("") }
    val current = picked ?: saved
    if (products == null) {
        AdminHint("Cargando el menú…", colors)
    } else {
        Column {
            products.filter { it.available }.forEach { product ->
                val isOn = product.id in current
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .physicalPress(enabled = enabled, scale = PhysicalPressScale.Small) {
                                haptics.selection()
                                picked = if (isOn) current - product.id else current + product.id
                            }.semantics(mergeDescendants = true) {
                                role = Role.Checkbox
                                this.selected = isOn
                            }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (isOn) colors.textPrimary else colors.cardBackground)
                            .border(
                                1.5.dp,
                                if (isOn) colors.textPrimary else colors.cardBorder,
                                RoundedCornerShape(7.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isOn) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = colors.background,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    Text(
                        product.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        moneyLabel(product.digitalPrice),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
    ActionPill(
        label =
            when {
                state.savingMenuDate == date -> "Guardando…"
                current.isEmpty() -> "Vender todo el menú este día"
                else -> "Guardar menú · ${current.size}"
            },
        enabled = enabled && !state.saving && picked != null && picked != saved,
        loading = state.savingMenuDate == date,
        colors = colors,
        modifier = Modifier.padding(top = 4.dp),
    ) {
        haptics.impact()
        actions.onSaveMenu(date, current) { picked = null }
    }
    if (closing) {
        val orders = state.reserved[date]?.orders ?: 0
        val what =
            when (orders) {
                0 -> "No hay nada apartado."
                1 -> "Se cancela 1 apartado y el saldo regresa a cada alumno."
                else -> "Se cancelan $orders apartados y el saldo regresa a cada alumno."
            }
        InlineConfirm(
            title = "¿Cerrar el ${SchoolDates.longLabel(date, today).lowercase()}?",
            message = "$what Ese día ya no se podrá apartar. Los tutores ven el motivo.",
            confirmLabel = "Cerrar el día",
            colors = colors,
            enabled = enabled && !state.saving,
            destructive = true,
            confirmEnabled = reason.trim().length in NoServiceDay.REASON_LENGTH,
            onCancel = { closing = false },
            extra = {
                OperationalField(
                    value = reason,
                    onValueChange = { reason = it.take(NoServiceDay.REASON_LENGTH.last) },
                    label = "Motivo",
                    colors = colors,
                    placeholder = "Por qué no hay servicio",
                    enabled = enabled,
                )
            },
        ) {
            closing = false
            actions.onCloseDay(date, reason)
        }
    } else {
        // Discreto a propósito: cerrar un día cancela lo apartado.
        Text(
            "Cerrar este día: no hay servicio",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .physicalPress(enabled = enabled && !state.saving, scale = PhysicalPressScale.Small) {
                        reason = ""
                        closing = true
                    }.semantics { role = Role.Button }
                    .padding(vertical = 10.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
