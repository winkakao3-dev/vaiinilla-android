package com.vaiinilla.app.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.admin.CashbackRule
import com.vaiinilla.app.domain.admin.CashbackRules
import com.vaiinilla.app.ui.admin.AdminPeriodPreset
import com.vaiinilla.app.ui.admin.AdminStoreViewModel
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.screens.ActionPill
import com.vaiinilla.app.ui.screens.ChipButton
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.OperationalField

/** Cashback y wallet: la regla que regresa saldo al entregar y cómo se mueve la cartera. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AdminCashbackSection(
    viewModel: AdminStoreViewModel,
    colors: OperationalColors,
    readOnly: Boolean,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState
    val saved = state.cashback
    var draft by remember { mutableStateOf<CashbackRule?>(null) }
    var reviewing by remember { mutableStateOf(false) }
    LaunchedEffect(saved, state.cashbackLoaded) {
        if (state.cashbackLoaded && draft == null) {
            draft = saved ?: CashbackRule("Regla principal", "5", null, null, null, null, null, active = false)
        }
    }
    val current = draft
    val enabled = !readOnly && !state.saving

    AdminSectionPage(
        title = "Cashback y wallet",
        colors = colors,
        notice = state.notice,
        noticeIsError = state.noticeIsError,
        onBack = onBack,
    ) {
        item {
            AdminCard(colors) {
                AdminSectionTitle("Cashback", colors)
                AdminHint("Automático al entregar: el cliente recibe en su saldo el porcentaje de lo que pagó.", colors)
                if (current == null) {
                    AdminHint("Cargando la regla…", colors)
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (current.active) "Activa" else "Inactiva",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = current.active,
                            onCheckedChange = { draft = current.copy(active = it) },
                            enabled = enabled,
                            colors =
                                SwitchDefaults.colors(
                                    checkedTrackColor = colors.accentLime,
                                    checkedThumbColor = colors.accentInk,
                                ),
                        )
                    }
                    OperationalField(
                        value = current.name,
                        onValueChange = { draft = current.copy(name = it.take(120)) },
                        label = "Nombre de la regla",
                        colors = colors,
                        enabled = enabled,
                    )
                    OperationalField(
                        value = current.percent,
                        onValueChange = {
                            draft =
                                current.copy(percent = it.filter { c -> c.isDigit() || c == '.' }.take(6))
                        },
                        label = "Porcentaje",
                        colors = colors,
                        supporting = "Ejemplo: 5.00 equivale a 5%.",
                        enabled = enabled,
                        keyboardType = KeyboardType.Decimal,
                    )
                    Text(
                        "Horario (opcional)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OperationalField(
                            value = current.startTime.orEmpty(),
                            onValueChange = { draft = current.copy(startTime = timeInput(it)) },
                            label = "Desde",
                            colors = colors,
                            placeholder = "08:00",
                            enabled = enabled,
                            modifier = Modifier.weight(1f),
                        )
                        OperationalField(
                            value = current.endTime.orEmpty(),
                            onValueChange = { draft = current.copy(endTime = timeInput(it)) },
                            label = "Hasta",
                            colors = colors,
                            placeholder = "11:00",
                            enabled = enabled,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    AdminHint("Si termina antes de empezar, cruza la medianoche.", colors)
                    Text("Días activos", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        CashbackRules.WEEK.forEach { (day, label) ->
                            val days = current.activeDays.orEmpty()
                            ChipButton(label = label, enabled = enabled, colors = colors, selected = day in days) {
                                val next = if (day in days) days - day else days + day
                                draft = current.copy(activeDays = next.ifEmpty { null })
                            }
                        }
                    }
                    AdminHint("Sin días marcados, aplica todos los días.", colors)
                    Text(
                        "Vigencia (opcional)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OperationalField(
                            value = current.validFrom.orEmpty(),
                            onValueChange = { draft = current.copy(validFrom = dateInput(it)) },
                            label = "Desde",
                            colors = colors,
                            placeholder = "2026-11-01",
                            enabled = enabled,
                            modifier = Modifier.weight(1f),
                        )
                        OperationalField(
                            value = current.validTo.orEmpty(),
                            onValueChange = { draft = current.copy(validTo = dateInput(it)) },
                            label = "Hasta",
                            colors = colors,
                            placeholder = "2026-12-31",
                            enabled = enabled,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (!readOnly) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ActionPill(
                                label = "Descartar",
                                enabled = enabled && current != saved,
                                loading = false,
                                colors = colors,
                                primary = false,
                                modifier = Modifier.weight(1f),
                            ) { draft = saved ?: current.copy(active = false) }
                            ActionPill(
                                label = "Revisar cambio",
                                enabled = enabled,
                                loading = false,
                                colors = colors,
                                modifier = Modifier.weight(1f),
                            ) {
                                reviewing = true
                            }
                        }
                    }
                    saved?.updatedAt?.let { adminShortDate(it) }?.let { AdminHint("Última actualización: $it", colors) }
                }
            }
        }
        item { WalletSummary(viewModel, colors) }
    }

    val pending = draft
    if (reviewing && pending != null) {
        AdminSheet(title = "Revisar cambio", colors = colors, busy = state.saving, onDismiss = { reviewing = false }) {
            AdminValueRow(
                "Estado",
                "${if (saved?.active == true) "Activa" else "Inactiva"} → ${if (pending.active) "Activa" else "Inactiva"}",
                colors,
            )
            AdminValueRow(
                "Porcentaje",
                "${saved?.percent ?: "—"}% → ${CashbackRules.percent(pending.percent) ?: pending.percent}%",
                colors,
            )
            AdminValueRow("Días", CashbackRules.daysLabel(pending.activeDays), colors)
            AdminValueRow(
                "Vigencia",
                if (pending.validFrom == null &&
                    pending.validTo == null
                ) {
                    "Siempre"
                } else {
                    "${pending.validFrom ?: "…"} a ${pending.validTo ?: "…"}"
                },
                colors,
            )
            AdminSheetError(state.notice.takeIf { state.noticeIsError })
            ActionPill(label = "Confirmar y guardar", enabled = true, loading = state.saving, colors = colors) {
                viewModel.saveCashback(pending) { reviewing = false }
            }
        }
    }
}

@Composable
private fun WalletSummary(
    viewModel: AdminStoreViewModel,
    colors: OperationalColors,
) {
    val state by viewModel.uiState
    AdminCard(colors) {
        AdminSectionTitle("Cartera de clientes", colors)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdminPeriodPreset.entries.forEach { preset ->
                ChipButton(
                    label = preset.label,
                    enabled = true,
                    colors = colors,
                    selected =
                        preset == state.walletPreset,
                    modifier = Modifier.weight(1f),
                ) {
                    viewModel.selectWalletPeriod(preset)
                }
            }
        }
        val wallet = state.wallet
        if (wallet == null) {
            AdminHint("Cargando…", colors)
        } else {
            AdminValueRow("Recargas", moneyLabel(wallet.reloads), colors)
            AdminValueRow("Compras con saldo", moneyLabel(wallet.purchases), colors)
            AdminValueRow("Cashback otorgado", moneyLabel(wallet.cashback), colors)
            AdminValueRow(
                "Cancelaciones devueltas",
                moneyLabel(wallet.cancellations),
                colors,
                detail = wallet.canceledOrders?.let { if (it == 1) "1 pedido cancelado" else "$it pedidos cancelados" },
            )
            if (wallet.movements.isNotEmpty()) {
                Text("Movimientos por tipo", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                wallet.movements.forEach { movement ->
                    AdminValueRow(
                        movement.label,
                        moneyLabel(movement.amount),
                        colors,
                        detail = if (movement.orders == 1) "1 operación" else "${movement.orders} operaciones",
                    )
                }
            }
            AdminHint(
                "Conciliación: ${wallet.walletsReviewed} carteras revisadas" +
                    if (wallet.alerts > 0) ", ${wallet.alerts} con alerta." else ", sin alertas.",
                colors,
            )
        }
    }
}

/** Deja escribir "8", "08:", "08:30"; vacío = sin hora. */
private fun timeInput(raw: String): String? = raw.filter { it.isDigit() || it == ':' }.take(5).ifEmpty { null }

private fun dateInput(raw: String): String? = raw.filter { it.isDigit() || it == '-' }.take(10).ifEmpty { null }
