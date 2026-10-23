package com.vaiinilla.app.ui.counter

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.counter.ReservedCalendar
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.screens.ChipButton
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.admin.AdminSheet

/** En Cocina de una escuela, arriba de la fila: abre lo que ya apartaron los papás. */
@Composable
fun KitchenReservedEntry(
    colors: OperationalColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .physicalPress(
                    scale = PhysicalPressScale.Nav,
                    onClick = onClick,
                ).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Para mañana", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
            Text(
                "Lo que ya apartaron los papás, por día",
                fontSize = 13.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary)
    }
}

/** Los próximos días y cuántas piezas de cada producto, para preparar a tiempo. */
@Composable
fun ReservedOrdersSheet(
    state: ReservedOrdersUiState,
    colors: OperationalColors,
    onSelect: (String) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = rememberVaiinillaHaptics()
    val reduced = reducedMotion()
    AdminSheet(title = "Apartado", colors = colors, busy = false, onDismiss = onDismiss) {
        Text(
            "Lo que ya pidieron los papás",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary,
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReservedCalendar.dates(state.today).forEach { date ->
                ChipButton(
                    label = ReservedCalendar.label(date, state.today),
                    enabled = true,
                    selected = date == state.selectedDate,
                    colors = colors,
                ) {
                    haptics.selection()
                    onSelect(date)
                }
            }
        }
        AnimatedContent(
            targetState = state.selectedDate,
            transitionSpec = {
                if (reduced) {
                    fadeIn(snap()) togetherWith fadeOut(snap())
                } else {
                    fadeIn(spring(stiffness = 400f)) togetherWith
                        fadeOut()
                }
            },
            label = "reserved-day",
        ) { date ->
            val day = state.days[date]
            val label = ReservedCalendar.label(date, state.today)
            Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                when {
                    day?.closedReason != null ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Bedtime,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                "Sin servicio: ${day.closedReason}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                            )
                        }
                    day != null && day.lines.isEmpty() ->
                        Text(
                            "Nada apartado para ${if (label == "Hoy" || label == "Mañana") label.lowercase() else "el ${label.lowercase()}"}.",
                            fontSize = 15.sp,
                            color = colors.textSecondary,
                        )
                    day != null -> {
                        Text(
                            "${day.units} ${if (day.units == 1) "pieza" else "piezas"} · ${day.orders} ${if (day.orders == 1) "pedido" else "pedidos"}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                        day.lines.forEach { line ->
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                        .semantics(mergeDescendants = true) {
                                            contentDescription =
                                                "${line.quantity} ${line.name}"
                                        },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    "${line.quantity}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = colors.textPrimary,
                                    modifier = Modifier.widthIn(min = 36.dp),
                                )
                                Text(
                                    line.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary,
                                )
                            }
                            HorizontalDivider(color = colors.cardBorder)
                        }
                    }
                    state.error != null ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(state.error, fontSize = 14.sp, color = colors.textPrimary)
                            Text(
                                "Intentar otra vez",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                modifier = Modifier.physicalPress(onClick = onRetry),
                            )
                        }
                    else -> Text("Cargando…", fontSize = 14.sp, color = colors.textSecondary)
                }
            }
        }
    }
}
