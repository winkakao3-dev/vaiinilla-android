package com.vaiinilla.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.repository.CashDrawerMovement
import com.vaiinilla.app.domain.repository.CashDrawerMovementType
import com.vaiinilla.app.domain.repository.CashDrawerRules
import com.vaiinilla.app.ui.components.RollingText
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.operational.CashDrawerUi
import com.vaiinilla.app.ui.screens.admin.AdminPrimaryButton
import com.vaiinilla.app.ui.screens.admin.moneyLabelCents

/** N11 «Movimientos del cajón»: cuánto debe haber, meter o sacar efectivo con su motivo y lo que pasó hoy. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CashDrawerSheet(
    expectedCash: String?,
    colors: OperationalColors,
    onDismiss: () -> Unit,
    drawer: CashDrawerUi = CashDrawerUi(),
    /** Null en solo lectura: no se ofrece meter ni sacar. */
    onSave: ((CashDrawerMovementType, String, String) -> Unit)? = null,
    onRetry: () -> Unit = {},
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        CashDrawerContent(
            expectedCash = expectedCash,
            colors = colors,
            modifier = Modifier.navigationBarsPadding().verticalScroll(rememberScrollState()),
            drawer = drawer,
            onSave = onSave,
            onRetry = onRetry,
        )
    }
}

/**
 * Lo que debe haber viene de `efectivo_esperado`. Una entrada (cambio del banco) o una salida (compra con efectivo
 * del cajón) se anota con su motivo en `POST sesiones-caja/{id}/movimientos`: el servidor la suma al esperado, al
 * cierre y al corte. Los renglones vienen de `GET` del mismo recurso.
 */
@Composable
internal fun CashDrawerContent(
    expectedCash: String?,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    drawer: CashDrawerUi = CashDrawerUi(),
    onSave: ((CashDrawerMovementType, String, String) -> Unit)? = null,
    onRetry: () -> Unit = {},
) {
    var formType by remember { mutableStateOf<CashDrawerMovementType?>(null) }
    var amount by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    // Anotado: el formulario se cierra y queda limpio para el siguiente.
    LaunchedEffect(drawer.savedCount) {
        if (drawer.savedCount > 0) {
            formType = null
            amount = ""
            reason = ""
        }
    }
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
        Text(
            "Movimientos del cajón",
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.8).sp,
            lineHeight = 32.sp,
            color = colors.textPrimary,
        )
        Column(
            modifier =
                Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .arrive(0)
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.cardBackground)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text("Debe haber en el cajón", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
            if (expectedCash != null) {
                RollingText(
                    text = moneyLabel(expectedCash),
                    style = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.2).sp),
                    color = colors.textPrimary,
                )
                Text(
                    "Lo calcula el servidor con el fondo, los cobros, las entradas y las salidas; compáralo con lo " +
                        "que cuentes.",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            } else {
                Text("—", fontSize = 34.sp, fontWeight = FontWeight.Black, color = colors.textPrimary)
                Text(
                    "El servidor aún no dijo cuánto debe haber.",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                )
            }
        }
        if (onSave != null) {
            Row(
                modifier = Modifier.padding(top = 12.dp).fillMaxWidth().arrive(1),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ActionPill(
                    label = "Meter dinero",
                    enabled = !drawer.saving,
                    loading = false,
                    colors = colors,
                    primary = formType == CashDrawerMovementType.IN,
                    outlined = formType != CashDrawerMovementType.IN,
                    modifier = Modifier.weight(1f),
                ) { formType = CashDrawerMovementType.IN }
                ActionPill(
                    label = "Sacar dinero",
                    enabled = !drawer.saving,
                    loading = false,
                    colors = colors,
                    primary = formType == CashDrawerMovementType.OUT,
                    outlined = formType != CashDrawerMovementType.OUT,
                    modifier = Modifier.weight(1f),
                ) { formType = CashDrawerMovementType.OUT }
            }
            AnimatedVisibility(
                visible = formType != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                val type = formType ?: CashDrawerMovementType.IN
                val normalized = normalizeCountedCash(amount)?.takeIf { (it.toBigDecimalOrNull()?.signum() ?: 0) > 0 }
                val invalidAmount = amount.isNotBlank() && normalized == null
                val reasonOk = CashDrawerRules.validReason(reason)
                Column(
                    modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CashAmountField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = if (type == CashDrawerMovementType.IN) "CUÁNTO METES" else "CUÁNTO SACAS",
                        colors = colors,
                        enabled = !drawer.saving,
                        isError = invalidAmount,
                        supporting = if (invalidAmount) "Un monto mayor a cero, con hasta dos decimales" else null,
                    )
                    OperationalField(
                        value = reason,
                        onValueChange = { reason = it.take(CashDrawerRules.REASON_MAX) },
                        label = "Motivo",
                        colors = colors,
                        enabled = !drawer.saving,
                        placeholder =
                            if (type == CashDrawerMovementType.IN) "Ej. cambio del banco" else "Ej. compra de hielo",
                        supporting = "Queda en el corte junto con tu nombre.",
                    )
                    drawer.saveError?.let { Text(it, fontSize = 13.sp, color = colors.textSecondary) }
                    AdminPrimaryButton(
                        title =
                            when {
                                drawer.saving -> "Anotando…"
                                normalized == null -> "Escribe el monto"
                                !reasonOk -> "Escribe el motivo"
                                type == CashDrawerMovementType.IN -> "Anotar entrada de ${moneyLabel(normalized)}"
                                else -> "Anotar salida de ${moneyLabel(normalized)}"
                            },
                        colors = colors,
                        busy = drawer.saving,
                        enabled = normalized != null && reasonOk && !drawer.saving,
                    ) { normalized?.let { onSave(type, it, reason.trim()) } }
                    TextButton(
                        onClick = { formType = null },
                        enabled = !drawer.saving,
                        modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp),
                    ) { Text("Cancelar", color = colors.textSecondary, fontWeight = FontWeight.Bold) }
                }
            }
        }
        Text(
            "HOY · MÁS NUEVO ARRIBA",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.5.sp,
            color = colors.textSecondary,
            modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp),
        )
        val report = drawer.report
        when {
            report != null && report.movements.isNotEmpty() ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    drawerMovementsNewestFirst(report.movements).forEach { movement ->
                        DrawerMovementRow(movement, colors)
                    }
                }
            report != null -> DrawerNote("Aún no hay movimientos en este turno.", colors)
            drawer.errorMessage != null ->
                Column {
                    DrawerNote(drawer.errorMessage, colors)
                    TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Reintentar", color = colors.accentLime, fontWeight = FontWeight.Bold)
                    }
                }
            else -> DrawerNote("Cargando movimientos…", colors)
        }
    }
}

@Composable
private fun DrawerNote(
    text: String,
    colors: OperationalColors,
) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = colors.textSecondary,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun DrawerMovementRow(
    movement: CashDrawerMovement,
    colors: OperationalColors,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(colors.cardBackground)
                .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(colors.cardInner),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                when {
                    movement.type == "apertura" -> Icons.Outlined.SwapHoriz
                    movement.outgoing -> Icons.Outlined.ArrowUpward
                    else -> Icons.Outlined.ArrowDownward
                },
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                drawerMovementTitle(movement),
                fontSize = 14.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary,
            )
            drawerMovementDetail(movement)?.let {
                Text(
                    it,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Text(
            (if (movement.outgoing) "−" else "+") + moneyLabelCents(movement.amount),
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            color = colors.textPrimary,
        )
    }
}

/** Del más nuevo al más viejo; las fechas ISO en UTC se ordenan como texto. */
internal fun drawerMovementsNewestFirst(movements: List<CashDrawerMovement>): List<CashDrawerMovement> =
    movements.sortedByDescending { it.createdAt }

/** Qué fue, con las mismas palabras que el corte. */
internal fun drawerMovementTitle(movement: CashDrawerMovement): String {
    val folio = movement.orderFolio?.let { " #$it" }.orEmpty()
    return when (movement.type) {
        "apertura" -> "Fondo al abrir"
        "cobro" -> "Cobro en efectivo$folio"
        "propina" -> "Propina en efectivo$folio"
        "wallet_recharge" -> "Recarga de saldo"
        "ajuste_entrada" -> "Entrada al cajón"
        "ajuste_salida" -> if (movement.isRefund) "Devolución en efectivo$folio" else "Salida del cajón"
        else -> if (movement.outgoing) "Salida del cajón" else "Entrada al cajón"
    }
}

/** El motivo, quién lo anotó y a qué hora. */
internal fun drawerMovementDetail(movement: CashDrawerMovement): String? =
    listOfNotNull(
        movement.reason?.takeIf { it.isNotBlank() },
        movement.registeredBy?.takeIf { it.isNotBlank() },
        movementClock(movement.createdAt),
    ).joinToString(" · ").ifEmpty { null }
