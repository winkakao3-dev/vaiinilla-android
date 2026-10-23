package com.vaiinilla.app.ui.counter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.counter.OfflineStudentPurchase
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.screens.OperationalColors

/** Arriba de Alumnos: lo vendido sin conexión que falta mandar y lo que el servidor no aceptó. */
@Composable
internal fun OfflineCounterSection(
    state: OfflineCounterState,
    colors: OperationalColors,
    onSettle: (OfflineStudentPurchase) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberVaiinillaHaptics()
    var settling by remember { mutableStateOf<OfflineStudentPurchase?>(null) }
    val reduced = reducedMotion()
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .animateContentSize(if (reduced) snap() else spring(dampingRatio = 0.86f, stiffness = 500f))
                .padding(top = if (state.count > 0) 12.dp else 0.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AnimatedVisibility(visible = state.pending.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
            PendingRow(state, colors)
        }
        state.problems.forEach { purchase ->
            ProblemRow(purchase, colors) {
                haptics.selection()
                settling = purchase
            }
        }
    }
    settling?.let { purchase ->
        AlertDialog(
            onDismissRequest = { settling = null },
            containerColor = colors.background,
            title = {
                Text(
                    "¿Ya le cobraste ${moneyLabel(purchase.total)} a ${purchase.studentName} de otra forma?",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            },
            text = { Text("Se quita de la lista. No se le descuenta nada de su saldo.", color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    settling = null
                    onSettle(purchase)
                }) { Text("Sí, quitarlo", color = colors.textPrimary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { settling = null }) { Text("No", color = colors.textSecondary) }
            },
        )
    }
}

@Composable
private fun PendingRow(
    state: OfflineCounterState,
    colors: OperationalColors,
) {
    val count = state.pending.size
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.cardInner)
                .border(1.dp, colors.cardBorder, shape)
                .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Outlined.WifiOff,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(18.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${if (count == 1) "1 venta" else "$count ventas"} por mandar · ${moneyLabel(state.pendingTotal)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
            Text("Se cobran solas cuando vuelva la conexión.", fontSize = 12.sp, color = colors.textSecondary)
        }
        if (state.isSending) {
            CircularProgressIndicator(color = colors.textSecondary, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ProblemRow(
    purchase: OfflineStudentPurchase,
    colors: OperationalColors,
    onSettle: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.cardInner)
                .border(1.dp, CounterCoral.copy(alpha = 0.5f), shape)
                .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = CounterCoral,
                modifier = Modifier.size(18.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Por resolver · ${purchase.studentName}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
                Text(
                    "${moneyLabel(purchase.total)} no se cobró: ${purchase.problem.orEmpty()}",
                    fontSize = 12.sp,
                    color = colors.textPrimary,
                )
            }
        }
        Text(
            "Ya se lo cobré de otra forma",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            modifier =
                Modifier
                    .padding(start = 30.dp)
                    .physicalPress(scale = PhysicalPressScale.Small, onClick = onSettle)
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}

/** En Caja, bajo "Mesas y canchas": abre la hoja de Alumnos y dice cuántas ventas sin conexión faltan. */
@Composable
fun CashierStudentsEntry(
    offline: OfflineCounterState,
    colors: OperationalColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val caption =
        when {
            offline.problems.isNotEmpty() -> "${offline.problems.size} por resolver"
            offline.pending.isNotEmpty() -> "${offline.pending.size} por mandar al volver la conexión"
            else -> "Sin teléfono: por nombre, matrícula o credencial"
        }
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
            Text("Alumnos", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
            Text(
                caption,
                fontSize = 13.sp,
                fontWeight = if (offline.problems.isEmpty()) FontWeight.Normal else FontWeight.SemiBold,
                color = if (offline.problems.isEmpty()) colors.textSecondary else CounterCoral,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary)
    }
}
