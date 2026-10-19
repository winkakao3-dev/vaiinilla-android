package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.repository.TableSpace
import com.vaiinilla.app.ui.components.NumberTicker
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.screens.admin.AdminSheet
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

/** Las dos cosas que la caja inicia en cualquier momento: un pedido de mostrador y una recarga. */
@Composable
internal fun CashierQuickActions(
    colors: OperationalColors,
    enabled: Boolean,
    onNewOrder: (() -> Unit)?,
    onReload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (onNewOrder != null) {
            QuickAction("Nuevo pedido", Icons.Outlined.Add, colors, enabled, Modifier.weight(1f), onNewOrder)
        }
        QuickAction(
            "Recargar saldo",
            Icons.Outlined.AccountBalanceWallet,
            colors,
            enabled,
            Modifier.weight(1f),
            onReload,
        )
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    colors: OperationalColors,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier =
            modifier
                .physicalPress(enabled = enabled, onClick = onClick)
                .height(52.dp)
                .clip(shape)
                .background(colors.cardInner)
                .border(1.dp, colors.cardBorder, shape)
                .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        Icon(icon, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(20.dp))
        Text(label, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
    }
}

/** Encabezado de la fila de trabajo: "Por cobrar" y cuántos pedidos esperan. */
@Composable
internal fun CashierQueueHeader(
    count: Int,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Por cobrar", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = colors.textPrimary)
        NumberTicker(value = count, label = "cashier-queue-count") { value ->
            Text(
                "$value",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = colors.background,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (value > 0) colors.textPrimary else colors.textMuted)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

/** Entrada al tablero de mesas y canchas, con cuántas tienen cuenta sin cobrar. */
@Composable
internal fun CashierSpacesEntry(
    pendingSpaces: Int?,
    colors: OperationalColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .physicalPress(scale = PhysicalPressScale.Nav, onClick = onClick)
                .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Mesas y canchas", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
            Text(
                when (pendingSpaces) {
                    null -> "Cuentas abiertas y turnos"
                    0 -> "Nada por cobrar"
                    1 -> "1 con cuenta por cobrar"
                    else -> "$pendingSpaces con cuenta por cobrar"
                },
                fontSize = 13.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textSecondary,
        )
    }
}

/** Una línea en lugar de un bloque vacío: la fila de trabajo está al día. */
@Composable
internal fun CashierQueueEmpty(
    colors: OperationalColors,
    modifier: Modifier = Modifier,
) {
    Text(
        "Nada por cobrar. Los pedidos de la app y del mostrador llegan aquí.",
        fontSize = 14.sp,
        color = colors.textSecondary,
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
    )
}

/** La fila de trabajo mientras termina la primera carga: todavía no se sabe si hay algo por cobrar. */
@Composable
internal fun CashierQueueLoading(
    colors: OperationalColors,
    modifier: Modifier = Modifier,
) {
    Text(
        "Cargando pedidos…",
        fontSize = 14.sp,
        color = colors.textSecondary,
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
    )
}

/** Caja toma pedidos como el mesero: para llevar o a una mesa (a su cuenta si el negocio deja pagar al final). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CashierNewOrderSheet(
    tables: List<TableSpace>,
    tablesLoading: Boolean,
    tablesError: String?,
    colors: OperationalColors,
    onDismiss: () -> Unit,
    onTakeAway: () -> Unit,
    onTable: (TableSpace) -> Unit,
    onRefreshTables: () -> Unit,
) {
    // Hasta que corre la carga de la hoja, todavía no se sabe si hay mesas.
    var refreshRequested by remember { mutableStateOf(false) }
    // Mientras reintenta, el tablero borra su error: la tarjeta se queda con el anterior.
    var retryError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        onRefreshTables()
        refreshRequested = true
    }
    LaunchedEffect(tablesLoading) {
        if (!tablesLoading) retryError = null
    }
    val status =
        cashierTablesStatus(
            hasTables = tables.isNotEmpty(),
            loading = tablesLoading || !refreshRequested,
            errorMessage = tablesError,
            retryError = retryError,
        )
    AdminSheet(title = "Nuevo pedido", colors = colors, busy = false, onDismiss = onDismiss) {
        ActionPill(label = "Para llevar", enabled = true, loading = false, colors = colors, onClick = onTakeAway)
        when (status) {
            null -> {
                Text("O en una mesa:", fontSize = 13.sp, color = colors.textSecondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tables.forEach { table ->
                        ChipButton(label = table.name, enabled = true, colors = colors) { onTable(table) }
                    }
                }
            }
            CashierTablesStatus.Loading -> CashierTablesLoading(colors)
            is CashierTablesStatus.Failed ->
                CashierTablesError(
                    message = status.message,
                    retrying = status.retrying,
                    colors = colors,
                    onRetry = {
                        retryError = status.message
                        onRefreshTables()
                    },
                )
            CashierTablesStatus.Empty ->
                Text(
                    "Este negocio todavía no tiene mesas. Se agregan en Administración, en Mesas y espacios.",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                )
        }
    }
}

/** Por qué la hoja de Nuevo pedido no muestra mesas; null si hay mesas que mostrar. */
internal sealed interface CashierTablesStatus {
    data object Loading : CashierTablesStatus

    data class Failed(
        val message: String,
        val retrying: Boolean,
    ) : CashierTablesStatus

    data object Empty : CashierTablesStatus
}

/**
 * Sin mesas se dice por qué: todavía cargan, no se pudieron traer (con reintentar) o el negocio no tiene.
 * Antes una falla del tablero dejaba solo "Para llevar", como si no hubiera mesas.
 */
internal fun cashierTablesStatus(
    hasTables: Boolean,
    loading: Boolean,
    errorMessage: String?,
    retryError: String?,
): CashierTablesStatus? =
    when {
        hasTables -> null
        loading && retryError != null -> CashierTablesStatus.Failed(retryError, retrying = true)
        loading -> CashierTablesStatus.Loading
        errorMessage != null -> CashierTablesStatus.Failed(errorMessage, retrying = false)
        else -> CashierTablesStatus.Empty
    }

@Composable
private fun CashierTablesLoading(colors: OperationalColors) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.semantics(mergeDescendants = true) {},
    ) {
        CircularProgressIndicator(color = colors.textSecondary, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
        Text("Cargando mesas…", fontSize = 13.sp, color = colors.textSecondary)
    }
}

/** El fondo coral de los errores del staff; su "Reintentar" va en contorno porque aquí el lima ya es "Para llevar". */
@Composable
private fun CashierTablesError(
    message: String,
    retrying: Boolean,
    colors: OperationalColors,
    onRetry: () -> Unit,
) {
    val haptics = rememberVaiinillaHaptics()
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(LocalVaiinillaColors.current.coral.copy(alpha = 0.16f))
                .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Column(
            modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "No se pudieron cargar las mesas",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
            Text("$message Para llevar sigue funcionando.", fontSize = 13.sp, color = colors.textSecondary)
        }
        Row(
            modifier =
                Modifier
                    .padding(top = 4.dp)
                    .physicalPress(scale = PhysicalPressScale.Small, enabled = !retrying) {
                        haptics.selection()
                        onRetry()
                    }.height(48.dp)
                    .clip(CircleShape)
                    .background(colors.surfacePaper)
                    .border(1.dp, colors.cardBorder, CircleShape)
                    .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (retrying) {
                CircularProgressIndicator(
                    color = colors.textPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                if (retrying) "Cargando…" else "Reintentar",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
        }
    }
}
