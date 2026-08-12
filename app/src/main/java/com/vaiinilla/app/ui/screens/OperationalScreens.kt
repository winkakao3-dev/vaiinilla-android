package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.domain.mode.RestrictedMode
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.ui.components.OperationalEmptyState
import com.vaiinilla.app.ui.components.OrderSummaryCard
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.operational.OperationalUiState
import com.vaiinilla.app.ui.theme.Coral
import com.vaiinilla.app.ui.theme.Cream
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
    onSearchWalletClients: (String) -> Unit = {},
    onReloadWallet: (userId: String, amount: String) -> Unit = { _, _ -> },
    onChangeMode: (() -> Unit)? = null,
    restrictedMode: RestrictedMode? = null,
) {
    val pending = state.orders.filter { it.summary.state == OrderState.PENDING_PAYMENT }
    val ready = state.orders.filter { it.summary.state == OrderState.READY }
    var walletSearch by remember { mutableStateOf("") }
    var walletAmount by remember { mutableStateOf("100.00") }

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Cream)
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { OperationalHeader("Caja", "Ventanas 32–33", onBack, onChangeMode) }
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
                androidx.compose.material3.Button(
                    onClick = onOpenCashSession,
                    enabled = !state.acting && restrictedMode != RestrictedMode.READ_ONLY,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Abrir caja (500.00)", fontWeight = FontWeight.Black)
                }
            }
        }
        item { SectionLabel("Recargas de saldo") }
        item {
            OutlinedTextField(
                value = walletSearch,
                onValueChange = { walletSearch = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.cashSessionOpen == true && !state.acting,
                label = { Text("Nombre, matrícula o identificador") },
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = walletAmount,
                    onValueChange = { walletAmount = it },
                    modifier = Modifier.weight(1f),
                    enabled = state.cashSessionOpen == true && !state.acting,
                    label = { Text("Monto") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                androidx.compose.material3.Button(
                    onClick = { onSearchWalletClients(walletSearch) },
                    enabled = state.cashSessionOpen == true && !state.acting && walletSearch.isNotBlank(),
                    modifier = Modifier.defaultMinSize(minWidth = 112.dp),
                ) { Text("Buscar") }
            }
        }
        if (state.walletClients.isEmpty() && walletSearch.isNotBlank()) {
            item {
                Text("Busca un cliente del establecimiento para registrar efectivo.", color = MutedInk)
            }
        } else {
            items(state.walletClients, key = { it.userId }) { client ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(client.name, fontWeight = FontWeight.Black)
                    Text(
                        listOfNotNull(client.contextualId, client.enrollment).joinToString(" · "),
                        color = MutedInk,
                    )
                    androidx.compose.material3.Button(
                        onClick = { onReloadWallet(client.userId, walletAmount) },
                        enabled = state.cashSessionOpen == true && !state.acting,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Registrar recarga") }
                }
            }
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
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Cream)
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { OperationalHeader("Cocina", "Ventanas 36–38", onBack, onChangeMode) }
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
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Cream)
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { OperationalHeader("Mesero", "Ventanas 39–40", onBack, onChangeMode) }
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
private fun OperationalHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    onChangeMode: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MutedInk)
        }
        Text(
            if (onChangeMode == null) "Roles" else "Cambiar modo",
            modifier =
                Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .padding(horizontal = 8.dp, vertical = 12.dp)
                    .clickable(role = Role.Button, onClick = onChangeMode ?: onBack),
            color = MutedInk,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
}

@Composable
private fun OperationalError(message: String) {
    Text(message, color = Coral, fontWeight = FontWeight.Bold)
}
