package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.ui.components.OperationalEmptyState
import com.vaiinilla.app.ui.components.OrderSummaryCard
import com.vaiinilla.app.ui.components.OrderTimeline
import com.vaiinilla.app.ui.operational.OperationalUiState
import com.vaiinilla.app.ui.theme.Cream
import com.vaiinilla.app.ui.theme.Ink
import com.vaiinilla.app.ui.theme.Lime
import com.vaiinilla.app.ui.theme.MutedInk

@Composable
fun StudentTrackingScreen(
    state: OperationalUiState,
    trackingHint: (OrderDetail) -> String,
    onBack: () -> Unit,
    onOpenCatalog: () -> Unit,
    onSelectOrder: (String) -> Unit,
    onClearSelection: () -> Unit,
) {
    val selected = state.selectedOrder
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Seguimiento", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text(
                    "Roles",
                    modifier = Modifier.clickable(onClick = onBack),
                    color = MutedInk,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                "Ventanas 20–24 del mockup. Polling local cada 5 s.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedInk,
            )
        }

        if (selected == null) {
            item {
                Button(
                    onClick = onOpenCatalog,
                    colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink),
                ) {
                    Text("Ir al catálogo", fontWeight = FontWeight.Black)
                }
            }
            if (state.orders.isEmpty()) {
                item {
                    OperationalEmptyState(
                        title = "Sin pedidos activos",
                        message = "Crea un pedido en efectivo para ver el seguimiento aquí.",
                    )
                }
            } else {
                items(state.orders, key = { it.summary.id }) { order ->
                    OrderSummaryCard(
                        order = order,
                        modifier = Modifier.clickable { onSelectOrder(order.summary.id) },
                    )
                }
            }
        } else {
            item {
                Text(
                    "← Volver a la lista",
                    modifier = Modifier.clickable(onClick = onClearSelection),
                    color = MutedInk,
                    fontWeight = FontWeight.Bold,
                )
            }
            item {
                OrderSummaryCard(order = selected)
            }
            item {
                Text("Estado del pedido", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                OrderTimeline(current = selected.summary.state)
            }
            item {
                Text(
                    trackingHint(selected),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedInk,
                )
            }
        }
    }
}
