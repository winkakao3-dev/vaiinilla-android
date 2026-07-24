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
import androidx.compose.material.icons.outlined.ShoppingCart
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.DemoCheckoutFixtures
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.ui.components.CheckoutDestinationPicker
import com.vaiinilla.app.ui.components.CheckoutPaymentPicker
import com.vaiinilla.app.ui.components.DemoEmptyState
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.StudentTab
import com.vaiinilla.app.ui.components.VaiinillaBottomNav
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.paymentMethodLabel
import com.vaiinilla.app.ui.order.OrderFlowUiState
import com.vaiinilla.app.ui.order.canCreateOrder
import com.vaiinilla.app.ui.order.cartItemCount
import com.vaiinilla.app.ui.order.cartPreviewTotal
import com.vaiinilla.app.ui.order.hasSufficientBalance
import com.vaiinilla.app.ui.order.operationalBlockerMessage
import com.vaiinilla.app.ui.order.requiresOperationalReady
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

@Composable
fun CartScreen(
    state: OrderFlowUiState,
    walletBalance: Int = 200,
    onMenu: () -> Unit,
    onQuantityChange: (lineKey: String, delta: Int) -> Unit,
    onNotesChange: (String) -> Unit,
    onDestinationChange: (OrderDestination) -> Unit,
    onPaymentChange: (PaymentMethod) -> Unit,
    onConfirm: () -> Unit,
    onOpenTracking: () -> Unit = {},
    onOpenAssistant: () -> Unit = {},
    onOpenWallet: () -> Unit = {},
) {
    val colors = LocalVaiinillaColors.current
    val insufficientBalance = state.checkoutPayment == PaymentMethod.BALANCE &&
        !state.hasSufficientBalance(walletBalance)
    val canConfirm = state.canCreateOrder && !insufficientBalance

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Tu pedido", color = colors.ink, fontWeight = FontWeight.Black)
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
                item {
                    CheckoutDestinationPicker(
                        selected = state.checkoutDestination,
                        onSelect = onDestinationChange,
                    )
                }
                item { SectionTitle("Pago") }
                item {
                    CheckoutPaymentPicker(
                        selected = state.checkoutPayment,
                        walletBalance = walletBalance,
                        onSelect = onPaymentChange,
                    )
                }
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
                item { OrderSummaryCard(state = state) }
                if (state.requiresOperationalReady) {
                    state.operationalBlockerMessage?.let { blocker ->
                        item {
                            WarningBanner(message = blocker)
                        }
                    }
                }
                if (insufficientBalance) {
                    item {
                        WarningBanner(message = "Saldo insuficiente. Tienes $$walletBalance disponible.")
                    }
                }
                state.createOrderError?.let { error ->
                    item {
                        ErrorBanner(message = error)
                    }
                }
                item {
                    Button(
                        onClick = onConfirm,
                        enabled = canConfirm,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.accentInk,
                            disabledContainerColor = colors.paper2,
                            disabledContentColor = colors.muted,
                        ),
                    ) {
                        if (state.creatingOrder) {
                            CircularProgressIndicator(
                                color = colors.accentInk,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp),
                            )
                        } else {
                            Text(confirmLabel(state.checkoutPayment), fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        VaiinillaBottomNav(
            activeTab = StudentTab.CART,
            cartCount = state.cartItemCount,
            onMenu = onMenu,
            onAssistant = onOpenAssistant,
            onOrders = onOpenTracking,
            onWallet = onOpenWallet,
            onCart = {},
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private fun confirmLabel(payment: PaymentMethod): String = when (payment) {
    PaymentMethod.CASH -> "Confirmar pedido"
    PaymentMethod.BALANCE -> "Pagar con saldo"
    PaymentMethod.CARD -> "Pagar con tarjeta"
}

@Composable
private fun CartLineCard(line: CartLine, onMinus: () -> Unit, onPlus: () -> Unit) {
    val colors = LocalVaiinillaColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.paper2,
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
                Text(line.product.name, color = colors.ink, fontWeight = FontWeight.Black)
                val variants = line.product.optionGroups
                    .flatMap { it.options }
                    .filter { it.id in line.selectedOptionIds }
                    .joinToString(" · ") { option ->
                        if (option.extraPrice == "0.00") option.name else "${option.name} +${moneyLabel(option.extraPrice)}"
                    }
                if (variants.isNotBlank()) {
                    Text(variants, color = colors.muted, modifier = Modifier.padding(top = 4.dp))
                }
                Text(
                    moneyLabel(Money.cartLinePreview(line)),
                    color = colors.ink,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 7.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                QuantityButton(icon = Icons.Rounded.Remove, description = "Quitar uno", onClick = onMinus)
                Text(
                    line.quantity.toString(),
                    color = colors.ink,
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
    val colors = LocalVaiinillaColors.current
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(colors.ink),
    ) {
        Icon(icon, contentDescription = description, tint = colors.paper, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    val colors = LocalVaiinillaColors.current
    Text(
        title,
        color = colors.ink,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(top = 10.dp, start = 4.dp),
    )
}

@Composable
private fun OrderSummaryCard(state: OrderFlowUiState) {
    val colors = LocalVaiinillaColors.current
    val destinationLabel = when (state.checkoutDestination) {
        OrderDestination.TAKE_AWAY -> "Para llevar"
        OrderDestination.IN_SPACE -> DemoCheckoutFixtures.SPACE_NAME
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.ink,
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            SummaryRow("Subtotal", moneyLabel(state.cartPreviewTotal), colors)
            SummaryRow("Entrega", destinationLabel, colors)
            SummaryRow("Método", paymentMethodLabel(state.checkoutPayment), colors)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Total", color = colors.paper, fontWeight = FontWeight.Black)
                Text(moneyLabel(state.cartPreviewTotal), color = colors.paper, fontWeight = FontWeight.Black)
            }
            Text(
                "El servidor confirmará precios y total al crear el pedido.",
                color = colors.paper.copy(alpha = 0.65f),
                modifier = Modifier.padding(top = 9.dp),
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, colors: com.vaiinilla.app.ui.theme.VaiinillaColors) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = colors.paper.copy(alpha = 0.75f))
        Text(value, color = colors.paper, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun WarningBanner(message: String) {
    Surface(
        color = Color(0xFFFFF1CC),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(message, color = Color(0xFF171817), modifier = Modifier.padding(14.dp))
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Surface(
        color = Color(0xFFFFDED9),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(message, color = Color(0xFF171817), modifier = Modifier.padding(14.dp))
    }
}

@Composable
private fun EmptyCart(onMenu: () -> Unit) {
    DemoEmptyState(
        icon = Icons.Outlined.ShoppingCart,
        title = "Tu pedido está vacío",
        message = "Agrega algo del menú para empezar.",
        actionLabel = "Ver menú",
        onAction = onMenu,
    )
}
