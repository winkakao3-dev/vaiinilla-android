package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.theme.Cream
import com.vaiinilla.app.ui.theme.CreamDeep
import com.vaiinilla.app.ui.theme.Ink
import com.vaiinilla.app.ui.theme.Lime
import com.vaiinilla.app.ui.theme.MutedInk

@Composable
fun OrderConfirmationScreen(
    order: OrderDetail?,
    onReturnToMenu: () -> Unit,
) {
    if (order == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Cream),
            contentAlignment = Alignment.Center,
        ) {
            Button(
                onClick = onReturnToMenu,
                colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink),
            ) {
                Text("Volver al menú", fontWeight = FontWeight.Black)
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("PEDIDO CREADO", color = MutedInk, fontWeight = FontWeight.Black)
                Text(
                    "Tu pase de Caja está listo.",
                    color = Ink,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    "Págalo en efectivo. El pedido permanece por cobrar hasta que Caja confirme el pago.",
                    color = MutedInk,
                    modifier = Modifier.padding(top = 9.dp),
                )
            }
            Box(
                modifier = Modifier
                    .padding(start = 14.dp)
                    .background(Lime, RoundedCornerShape(22.dp))
                    .padding(horizontal = 17.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("✓", color = Ink, fontWeight = FontWeight.Black)
            }
        }

        Spacer(Modifier.height(26.dp))
        CashPass(order)
        Spacer(Modifier.height(18.dp))
        Surface(
            color = CreamDeep,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Qué sigue", color = Ink, fontWeight = FontWeight.Black)
                Text(
                    "Presenta el folio en Caja y realiza el pago en efectivo. El avance posterior pertenece al seguimiento de pedidos.",
                    color = MutedInk,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onReturnToMenu,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Cream),
        ) {
            Text("Volver al menú", fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun CashPass(order: OrderDetail) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Ink,
        shape = RoundedCornerShape(30.dp),
        shadowElevation = 18.dp,
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("VAIINILLA / PASE DE CAJA", color = Cream, fontWeight = FontWeight.Black)
                Surface(color = Lime, shape = RoundedCornerShape(99.dp)) {
                    Text("POR COBRAR", color = Ink, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
            Text(
                "#${order.summary.folio}",
                color = Cream,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text("Número de orden", color = Cream.copy(alpha = 0.62f))

            Spacer(Modifier.height(22.dp))
            order.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("${item.quantity} × ${item.productName}", color = Cream, modifier = Modifier.weight(1f))
                    Text(moneyLabel(item.subtotal), color = Cream, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(Modifier.height(18.dp))
            PassRow("Pago", "Efectivo")
            PassRow("Destino", "Para llevar")
            PassRow("Estado", "Por cobrar")
            order.kitchenNotes.takeIf { it.isNotBlank() }?.let { notes ->
                PassRow("Notas", notes)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text("TOTAL", color = Cream.copy(alpha = 0.62f), fontWeight = FontWeight.Bold)
                    Text("Confirmado por el fixture", color = Cream.copy(alpha = 0.62f))
                }
                Text(moneyLabel(order.summary.total), color = Cream, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun PassRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Cream.copy(alpha = 0.62f))
        Text(value, color = Cream, fontWeight = FontWeight.ExtraBold)
    }
}
