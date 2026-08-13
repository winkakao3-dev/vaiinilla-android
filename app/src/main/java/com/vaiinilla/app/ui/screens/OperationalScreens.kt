package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.RoomService
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.mode.RestrictedMode
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.ui.components.AuthHeroSheetScaffold
import com.vaiinilla.app.ui.components.AuthInkSubmitButton
import com.vaiinilla.app.ui.components.OperationalEmptyState
import com.vaiinilla.app.ui.components.OrderSummaryCard
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.operational.OperationalUiState
import com.vaiinilla.app.ui.theme.Coral
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.MutedInk
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import java.math.BigDecimal

@Composable
fun CashierOperationalScreen(
    state: OperationalUiState,
    onBack: () -> Unit,
    onOpenCashSession: () -> Unit,
    onCollect: (orderId: String, amount: String, version: Int) -> Unit,
    onDeliver: (orderId: String, version: Int) -> Unit,
    onScanDeliver: (orderId: String, version: Int) -> Unit = { _, _ -> },
    onChangeMode: (() -> Unit)? = null,
    restrictedMode: RestrictedMode? = null,
) {
    val pending = state.orders.filter { it.summary.state == OrderState.PENDING_PAYMENT }
    val ready = state.orders.filter { it.summary.state == OrderState.READY }

    AuthHeroSheetScaffold(
        kicker = "Caja",
        title = "Ventanilla de pagos.",
        intro = "Cobra en efectivo y entrega en barra.",
        loading = false,
        showBack = true,
        onBack = onBack,
        kickerIcon = Icons.Outlined.PointOfSale,
        scrollSheet = false,
    ) {
        WorkerModeLink(onChangeMode)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
        restrictedMode?.let { mode -> item { RestrictedModeNotice(mode) } }
        state.errorMessage?.let { message -> item { OperationalError(message) } }
        item {
            val open = state.cashSessionOpen
            Text(
                text =
                    when (open) {
                        true -> "Sesión de caja abierta"
                        false -> "Sesión de caja cerrada — ábrela para recibir pedidos"
                        null -> "Consultando sesión de caja…"
                    },
                color = MutedInk,
            )
            if (open == false) {
                AuthInkSubmitButton(
                    text = "Abrir caja (500.00)",
                    onClick = onOpenCashSession,
                    enabled = !state.acting && restrictedMode != RestrictedMode.READ_ONLY,
                )
            }
        }
        item {
            Text("Recarga de saldo", color = MutedInk, fontWeight = FontWeight.Black)
            Text(
                "Cuando el servidor publique Entrega 03, aquí se busca al alumno y se acredita efectivo. Hoy no hay ruta de recarga.",
                color = MutedInk,
            )
        }
        item { SectionLabel("Por cobrar") }
        if (pending.isEmpty()) {
            item {
                OperationalEmptyState(
                    title = "Sin pedidos por cobrar",
                    message = "Cuando un alumno confirme en efectivo, aparecerá aquí.",
                )
            }
        } else {
            items(pending, key = { it.summary.id }) { order ->
                CashCollectionCard(
                    order = order,
                    acting = state.acting,
                    onCollect = onCollect,
                    restrictedMode = restrictedMode,
                )
            }
        }
        item { SectionLabel("Entregas en barra") }
        if (ready.isEmpty()) {
            item {
                OperationalEmptyState(
                    title = "Sin entregas listas",
                    message = "Los pedidos para llevar listos se entregan desde aquí.",
                )
            }
        } else {
            items(ready, key = { it.summary.id }) { order ->
                val hasLocalPickupToken = !order.pickupToken.isNullOrBlank()
                OrderSummaryCard(
                    order = order,
                    actionLabel = if (hasLocalPickupToken) "Confirmar entrega" else "Escanear QR y entregar",
                    enabled = !state.acting && restrictedMode != RestrictedMode.READ_ONLY,
                    onAction = {
                        if (hasLocalPickupToken) {
                            onDeliver(order.summary.id, order.summary.version)
                        } else {
                            onScanDeliver(order.summary.id, order.summary.version)
                        }
                    },
                )
            }
        }
        }
    }
}

@Composable
private fun CashCollectionCard(
    order: OrderDetail,
    acting: Boolean,
    onCollect: (orderId: String, amount: String, version: Int) -> Unit,
    restrictedMode: RestrictedMode?,
) {
    var received by remember(order.summary.id) { mutableStateOf(order.summary.total) }
    val change =
        runCatching {
            BigDecimal(received).subtract(BigDecimal(order.summary.total))
        }.getOrNull()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = received,
            onValueChange = { received = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = restrictedMode != RestrictedMode.READ_ONLY,
            label = { Text("Efectivo recibido") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )
        Text(
            text =
                if (change != null && change >= BigDecimal.ZERO) {
                    "Cambio: ${moneyLabel(change.toPlainString())}"
                } else {
                    "Monto insuficiente"
                },
            color = MutedInk,
        )
        OrderSummaryCard(
            order = order,
            actionLabel = "Confirmar cobro",
            enabled =
                !acting &&
                    restrictedMode != RestrictedMode.READ_ONLY &&
                    change != null &&
                    change >= BigDecimal.ZERO,
            onAction = { onCollect(order.summary.id, received, order.summary.version) },
        )
    }
}

@Composable
fun KitchenOperationalScreen(
    state: OperationalUiState,
    onBack: () -> Unit,
    onStart: (orderId: String, version: Int) -> Unit,
    onReady: (orderId: String, version: Int) -> Unit,
    onChangeMode: (() -> Unit)? = null,
    restrictedMode: RestrictedMode? = null,
) {
    val active =
        state.orders.filter {
            it.summary.state == OrderState.PAID || it.summary.state == OrderState.PREPARING
        }
    AuthHeroSheetScaffold(
        kicker = "Cocina",
        title = "Comandas en fuego.",
        intro = "Empieza preparación y marca cuando esté listo.",
        loading = false,
        showBack = true,
        onBack = onBack,
        kickerIcon = Icons.Outlined.Restaurant,
        scrollSheet = false,
    ) {
        WorkerModeLink(onChangeMode)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
        restrictedMode?.let { mode -> item { RestrictedModeNotice(mode) } }
        state.errorMessage?.let { message -> item { OperationalError(message) } }
        if (active.isEmpty()) {
            item {
                OperationalEmptyState(
                    title = "Sin comandas",
                    message = "Cuando Caja confirme un pago con items de cocina, aparecerán aquí.",
                )
            }
        } else {
            items(active, key = { it.summary.id }) { order ->
                val action =
                    if (order.summary.state == OrderState.PAID) {
                        "Empezar preparación" to { onStart(order.summary.id, order.summary.version) }
                    } else {
                        "Marcar como listo" to { onReady(order.summary.id, order.summary.version) }
                    }
                OrderSummaryCard(
                    order = order,
                    actionLabel = action.first,
                    enabled = !state.acting && restrictedMode != RestrictedMode.READ_ONLY,
                    onAction = action.second,
                )
            }
        }
        }
    }
}

@Composable
fun WaiterOperationalScreen(
    state: OperationalUiState,
    onBack: () -> Unit,
    onDeliver: (orderId: String, version: Int) -> Unit,
    onScanDeliver: (orderId: String, version: Int) -> Unit = { _, _ -> },
    onChangeMode: (() -> Unit)? = null,
    restrictedMode: RestrictedMode? = null,
) {
    val ready = state.orders.filter { it.summary.state == OrderState.READY }
    AuthHeroSheetScaffold(
        kicker = "Mesero",
        title = "Entrega en el espacio.",
        intro = "Los pedidos listos para mesa aparecen aquí.",
        loading = false,
        showBack = true,
        onBack = onBack,
        kickerIcon = Icons.Outlined.RoomService,
        scrollSheet = false,
    ) {
        WorkerModeLink(onChangeMode)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
        restrictedMode?.let { mode -> item { RestrictedModeNotice(mode) } }
        state.errorMessage?.let { message -> item { OperationalError(message) } }
        if (ready.isEmpty()) {
            item {
                OperationalEmptyState(
                    title = "Sin mesas esperando",
                    message = "Los pedidos en espacio listos para entregar aparecerán aquí.",
                )
            }
        } else {
            items(ready, key = { it.summary.id }) { order ->
                val hasLocalPickupToken = !order.pickupToken.isNullOrBlank()
                OrderSummaryCard(
                    order = order,
                    actionLabel =
                        if (hasLocalPickupToken) {
                            "Confirmar entrega en espacio"
                        } else {
                            "Escanear QR y entregar en espacio"
                        },
                    enabled = !state.acting && restrictedMode != RestrictedMode.READ_ONLY,
                    onAction = {
                        if (hasLocalPickupToken) {
                            onDeliver(order.summary.id, order.summary.version)
                        } else {
                            onScanDeliver(order.summary.id, order.summary.version)
                        }
                    },
                )
            }
        }
        }
    }
}

@Preview(name = "Caja", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun CashierOperationalScreenPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        CashierOperationalScreen(
            state = OperationalUiState(cashSessionOpen = false),
            onBack = {},
            onOpenCashSession = {},
            onCollect = { _, _, _ -> },
            onDeliver = { _, _ -> },
        )
    }
}

@Preview(name = "Cocina", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun KitchenOperationalScreenPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        KitchenOperationalScreen(
            state = OperationalUiState(),
            onBack = {},
            onStart = { _, _ -> },
            onReady = { _, _ -> },
        )
    }
}

@Preview(name = "Mesero", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun WaiterOperationalScreenPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        WaiterOperationalScreen(
            state = OperationalUiState(),
            onBack = {},
            onDeliver = { _, _ -> },
        )
    }
}

@Composable
private fun WorkerModeLink(onChangeMode: (() -> Unit)?) {
    if (onChangeMode == null) return
    val colors = LocalVaiinillaColors.current
    Text(
        "Cambiar modo",
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .clickable(role = Role.Button, onClick = onChangeMode),
        color = colors.muted,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun RestrictedModeNotice(mode: RestrictedMode) {
    Text(
        text =
            when (mode) {
                RestrictedMode.READ_ONLY ->
                    "Este establecimiento está en solo lectura. Las acciones operativas están deshabilitadas."
                RestrictedMode.OPERATIONAL_CLOSE ->
                    "Este establecimiento está en cierre operativo. El servidor limita las acciones disponibles."
            },
        color = MutedInk,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
}

@Composable
private fun OperationalError(message: String) {
    Text(message, color = Coral, fontWeight = FontWeight.Bold)
}
