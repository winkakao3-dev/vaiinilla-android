package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.ui.components.ComingSoonSheet
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.StudentTab
import com.vaiinilla.app.ui.components.VaiinillaBottomNav
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.order.OrderFlowUiState
import com.vaiinilla.app.ui.order.canSubmitCart
import com.vaiinilla.app.ui.order.operationalBlockerMessage
import com.vaiinilla.app.ui.order.cartItemCount
import com.vaiinilla.app.ui.order.cartPreviewTotal
import com.vaiinilla.app.ui.theme.Cream
import com.vaiinilla.app.ui.theme.CreamDeep
import com.vaiinilla.app.ui.theme.Ink
import com.vaiinilla.app.ui.theme.Lime
import com.vaiinilla.app.ui.theme.MutedInk

@Composable
fun CartScreen(
    state: OrderFlowUiState,
    onMenu: () -> Unit,
    onQuantityChange: (lineKey: String, delta: Int) -> Unit,
    onNotesChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onOpenTracking: () -> Unit = {},
) {
    var comingSoonTitle by remember { mutableStateOf<String?>(null) }
    var comingSoonDescription by remember { mutableStateOf<String?>(null) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Tu pedido", color = Ink, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
            }

            if (state.cartLines.isEmpty()) {
                item { EmptyCart(onMenu) }
            } else {
                items(state.cartLines, key = CartLine::key) { line ->
                    CartLineCard(
                        line = line,
                        onMinus = { onQuantityChange(line.key, -1) },
                        onPlus = { onQuantityChange(line.key, 1) },
                    )
                }

                item { SectionTitle("Entrega") }
                item { TakeAwayCard() }
                item { SectionTitle("Pago") }
                item { CashPaymentCard() }
                item {
                    OutlinedTextField(
                        value = state.kitchenNotes,
                        onValueChange = onNotesChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Notas para cocina") },
                        placeholder = { Text("Ej. sin cebolla, salsa aparte") },
                        minLines = 3,
                        shape = RoundedCornerShape(17.dp),
                    )
                }
                item { OrderSummaryCard(total = state.cartPreviewTotal) }
                state.operationalBlockerMessage?.let { blocker ->
                    item {
                        Surface(
                            color = Color(0xFFFFF1CC),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(blocker, color = Ink, modifier = Modifier.padding(14.dp))
                        }
                    }
                }
                state.createOrderError?.let { error ->
                    item {
                        Surface(
                            color = Color(0xFFFFDED9),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(error, color = Ink, modifier = Modifier.padding(14.dp))
                        }
                    }
                }
                item {
                    Button(
                        onClick = onConfirm,
                        enabled = state.canSubmitCart,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Lime,
                            contentColor = Ink,
                            disabledContainerColor = CreamDeep,
                            disabledContentColor = MutedInk,
                        ),
                    ) {
                        if (state.creatingOrder) {
                            CircularProgressIndicator(
                                color = Ink,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp),
                            )
                        } else {
                            Text("Confirmar pedido", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        VaiinillaBottomNav(
            activeTab = StudentTab.CART,
            cartCount = state.cartItemCount,
            onMenu = onMenu,
            onAssistant = {
                comingSoonTitle = "Asistente Vaiinilla"
                comingSoonDescription = "Recomendaciones y chat guiado llegarán en la siguiente fase."
            },
            onOrders = onOpenTracking,
            onWallet = {
                comingSoonTitle = "Cartera"
                comingSoonDescription = "Saldo, recargas y stickers digitales llegarán en la siguiente fase."
            },
            onCart = {},
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        val title = comingSoonTitle
        val description = comingSoonDescription
        if (title != null && description != null) {
            ComingSoonSheet(
                title = title,
                description = description,
                onDismiss = {
                    comingSoonTitle = null
                    comingSoonDescription = null
                },
            )
        }
    }
}

@Composable
private fun CartLineCard(line: CartLine, onMinus: () -> Unit, onPlus: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CreamDeep,
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProductImage(
                imageUrl = line.product.imageUrl,
                contentDescription = line.product.name,
                modifier = Modifier
                    .size(74.dp)
                    .clip(RoundedCornerShape(18.dp)),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(line.product.name, color = Ink, fontWeight = FontWeight.Black)
                val variants = line.product.optionGroups
                    .flatMap { it.options }
                    .filter { it.id in line.selectedOptionIds }
                    .joinToString(" · ") { option ->
                        if (option.extraPrice == "0.00") option.name else "${option.name} +${moneyLabel(option.extraPrice)}"
                    }
                if (variants.isNotBlank()) {
                    Text(variants, color = MutedInk, modifier = Modifier.padding(top = 4.dp))
                }
                Text(
                    moneyLabel(Money.cartLinePreview(line)),
                    color = Ink,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 7.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                QuantityButton(icon = Icons.Rounded.Remove, description = "Quitar uno", onClick = onMinus)
                Text(
                    line.quantity.toString(),
                    color = Ink,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 7.dp),
                )
                QuantityButton(icon = Icons.Rounded.Add, description = "Agregar uno", onClick = onPlus)
            }
        }
    }
}

@Composable
private fun QuantityButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(Ink),
    ) {
        Icon(icon, contentDescription = description, tint = Cream, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        color = Ink,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(top = 10.dp, start = 4.dp),
    )
}

@Composable
private fun TakeAwayCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Ink,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Para llevar", color = Cream, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            Surface(color = Lime, shape = RoundedCornerShape(99.dp)) {
                Text("SELECCIONADO", color = Ink, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
            }
        }
    }
}

@Composable
private fun CashPaymentCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CreamDeep,
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Ink),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Payments, contentDescription = null, tint = Cream)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text("Efectivo en Caja", color = Ink, fontWeight = FontWeight.Black)
                Text("El pedido nace por cobrar y se prepara después del pago.", color = MutedInk)
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Lime),
                contentAlignment = Alignment.Center,
            ) {
                Text("✓", color = Ink, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun OrderSummaryCard(total: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Ink,
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            SummaryRow("Subtotal visual", moneyLabel(total))
            SummaryRow("Método", "Efectivo")
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Total visual", color = Cream, fontWeight = FontWeight.Black)
                Text(moneyLabel(total), color = Cream, fontWeight = FontWeight.Black)
            }
            Text(
                "El servidor confirmará precios y total al crear el pedido.",
                color = Cream.copy(alpha = 0.65f),
                modifier = Modifier.padding(top = 9.dp),
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Cream.copy(alpha = 0.75f))
        Text(value, color = Cream, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun EmptyCart(onMenu: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CreamDeep,
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Outlined.Payments, contentDescription = null, tint = MutedInk, modifier = Modifier.size(42.dp))
            Text("Tu carrito está vacío", color = Ink, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 14.dp))
            Text("Agrega algo del menú para comenzar.", color = MutedInk, modifier = Modifier.padding(top = 6.dp))
            Button(
                onClick = onMenu,
                colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink),
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
            ) {
                Text("Ver menú", fontWeight = FontWeight.Black)
            }
        }
    }
}
