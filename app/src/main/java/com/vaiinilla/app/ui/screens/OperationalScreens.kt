package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.ui.components.OperationalEmptyState
import com.vaiinilla.app.ui.components.OrderSummaryCard
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.operational.OperationalUiState
import com.vaiinilla.app.ui.theme.Cream
import com.vaiinilla.app.ui.theme.MutedInk
import java.math.BigDecimal

@Composable
fun CashierOperationalScreen(
    state: OperationalUiState,
    onBack: () -> Unit,
    onOpenCashSession: () -> Unit,
    onCollect: (orderId: String, amount: String, version: Int) -> Unit,
    onDeliver: (orderId: String, version: Int) -> Unit,
) {
    val pending = state.orders.filter { it.summary.state == OrderState.PENDING_PAYMENT }
    val ready = state.orders.filter { it.summary.state == OrderState.READY }

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Cream)
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { OperationalHeader("Caja", "Ventanas 32–33", onBack) }
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
                    enabled = !state.acting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Abrir caja (500.00)", fontWeight = FontWeight.Black)
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
                OrderSummaryCard(
                    order = order,
                    actionLabel = "Confirmar entrega",
                    enabled = !state.acting,
                    onAction = { onDeliver(order.summary.id, order.summary.version) },
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
            enabled = !acting && change != null && change >= BigDecimal.ZERO,
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
        item { OperationalHeader("Cocina", "Ventanas 36–38", onBack) }
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
                    enabled = !state.acting,
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
        item { OperationalHeader("Mesero", "Ventanas 39–40", onBack) }
        if (ready.isEmpty()) {
            item {
                OperationalEmptyState(
                    title = "Sin mesas esperando",
                    message = "Los pedidos en espacio listos para entregar aparecerán aquí.",
                )
            }
        } else {
            items(ready, key = { it.summary.id }) { order ->
                OrderSummaryCard(
                    order = order,
                    actionLabel = "Confirmar entrega en espacio",
                    enabled = !state.acting,
                    onAction = { onDeliver(order.summary.id, order.summary.version) },
                )
            }
        }
    }
}

@Composable
private fun OperationalHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
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
            "Roles",
            modifier =
                Modifier
                    .padding(top = 8.dp)
                    .clickable(onClick = onBack),
            color = MutedInk,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
}
