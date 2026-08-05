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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.Category
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.ui.components.CheckoutDestinationPicker
import com.vaiinilla.app.ui.components.CheckoutPaymentPicker
import com.vaiinilla.app.ui.components.CheckoutSpaceOption
import com.vaiinilla.app.ui.components.CheckoutSpacePicker
import com.vaiinilla.app.ui.components.EditorialNotesField
import com.vaiinilla.app.ui.components.EmptyState
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.VaiinillaBottomNavClearance
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.paymentMethodLabel
import com.vaiinilla.app.ui.order.OrderFlowUiState
import com.vaiinilla.app.ui.order.canCreateOrder
import com.vaiinilla.app.ui.order.cartPreviewTotal
import com.vaiinilla.app.ui.order.operationalBlockerMessage
import com.vaiinilla.app.ui.order.requiresOperationalReady
import com.vaiinilla.app.ui.order.selectedSpaceName
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode

@Composable
fun CartScreen(
    state: OrderFlowUiState,
    onMenu: () -> Unit,
    onQuantityChange: (lineKey: String, delta: Int) -> Unit,
    onNotesChange: (String) -> Unit,
    onDestinationChange: (OrderDestination) -> Unit,
    onSpaceChange: (Int) -> Unit = {},
    onPaymentChange: (PaymentMethod) -> Unit,
    onConfirm: () -> Unit,
    onOpenTracking: () -> Unit = {},
    onOpenAssistant: () -> Unit = {},
    onOpenWallet: () -> Unit = {},
    guestAuthRequired: Boolean = false,
) {
    val colors = LocalVaiinillaColors.current
    val checkoutSpaces =
        state.guestVenue
            ?.space
            ?.let { space -> listOf(CheckoutSpaceOption(space.id, space.name)) }
            .orEmpty()
    val canChooseInSpace = checkoutSpaces.isNotEmpty()
    // A guest must be able to continue into Firebase auth before the protected
    // operational-status check runs. submitOrder performs that check after auth.
    val canConfirm =
        if (guestAuthRequired) {
            state.cartLines.isNotEmpty() && !state.creatingOrder
        } else {
            state.cartLines.isNotEmpty() &&
                !state.creatingOrder &&
                (state.canCreateOrder || state.operationalStatus == null)
        }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.paper),
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            contentPadding =
                PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 18.dp,
                    bottom = VaiinillaBottomNavClearance + 48.dp,
                ),
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
                        selectedSpaceName = state.selectedSpaceName,
                        onSelect = onDestinationChange,
                        showInSpace = canChooseInSpace,
                    )
                }
                if (state.checkoutDestination == OrderDestination.IN_SPACE && canChooseInSpace) {
                    item {
                        CheckoutSpacePicker(
                            selectedSpaceId = state.selectedSpaceId,
                            spaces = checkoutSpaces,
                            onSelect = onSpaceChange,
                        )
                    }
                }
                item { SectionTitle("Pago") }
                item {
                    CheckoutPaymentPicker(
                        selected = state.checkoutPayment,
                        onSelect = onPaymentChange,
                    )
                }
                item {
                    EditorialNotesField(
                        value = state.kitchenNotes,
                        onValueChange = onNotesChange,
                        label = "Notas para cocina",
                        placeholder = "Ej. sin cebolla, salsa aparte",
                    )
                }
                item { OrderSummaryCard(state = state) }
                if (state.requiresOperationalReady && !guestAuthRequired && state.operationalStatus != null) {
                    state.operationalBlockerMessage?.let { blocker ->
                        item {
                            WarningBanner(message = blocker)
                        }
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
                        colors =
                            ButtonDefaults.buttonColors(
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
                            Text(
                                confirmLabel(state.checkoutPayment, guestAuthRequired),
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Carrito", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun CartScreenPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        CartScreen(
            state = OrderFlowUiState(loading = false),
            onMenu = {},
            onQuantityChange = { _, _ -> },
            onNotesChange = {},
            onDestinationChange = {},
            onPaymentChange = {},
            onConfirm = {},
        )
    }
}

@Preview(name = "Carrito · para llevar + efectivo", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun CartTakeAwayCashPreview() {
    val burrito =
        Product(
            id = 2,
            categoryId = 2,
            preparationStation = PreparationStation.KITCHEN,
            name = "Burrito norteño",
            description = "Burrito de asada con queso y salsa verde.",
            ingredients = "Tortilla, asada, queso, salsa",
            allergens = "Gluten, lácteos",
            estimatedTimeMinutes = 12,
            counterPrice = "64.00",
            digitalPrice = "64.00",
            available = true,
            imageUrl = "burrito_norteno",
            optionGroups = emptyList(),
        )
    val previewState =
        OrderFlowUiState(
            loading = false,
            catalog =
                Catalog(
                    categories = listOf(Category(id = 2, name = "Comida", order = 1)),
                    products = listOf(burrito),
                    cursor = null,
                ),
            operationalStatus =
                OperationalStatus(
                    acceptingOrders = true,
                    cashSessionOpen = true,
                    cashierOnline = true,
                    kitchenOnline = true,
                    estimatedTimeMinutes = 15,
                    consultedAt = "preview",
                ),
            cartLines = listOf(CartLine(product = burrito, quantity = 1, selectedOptionIds = emptySet())),
            checkoutDestination = OrderDestination.TAKE_AWAY,
            checkoutPayment = PaymentMethod.CASH,
        )

    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        CartScreen(
            state = previewState,
            onMenu = {},
            onQuantityChange = { _, _ -> },
            onNotesChange = {},
            onDestinationChange = {},
            onPaymentChange = {},
            onConfirm = {},
        )
    }
}

@Composable
private fun confirmLabel(
    payment: PaymentMethod,
    guestAuthRequired: Boolean,
): String =
    when {
        guestAuthRequired -> "Continuar para confirmar"
        else -> "Confirmar pedido"
    }

@Composable
private fun CartLineCard(
    line: CartLine,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
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
                modifier =
                    Modifier
                        .size(74.dp)
                        .clip(RoundedCornerShape(18.dp)),
            )
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
            ) {
                Text(line.product.name, color = colors.ink, fontWeight = FontWeight.Black)
                val variants =
                    line.product.optionGroups
                        .flatMap { it.options }
                        .filter { it.id in line.selectedOptionIds }
                        .joinToString(" · ") { option ->
                            if (option.extraPrice ==
                                "0.00"
                            ) {
                                option.name
                            } else {
                                "${option.name} +${moneyLabel(option.extraPrice)}"
                            }
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
            QuantityStepper(
                quantity = line.quantity,
                onMinus = onMinus,
                onPlus = onPlus,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun QuantityStepper(
    quantity: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Surface(
        modifier = modifier,
        color = colors.paper,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            QuantityButton(icon = Icons.Rounded.Remove, description = "Quitar uno", onClick = onMinus)
            Text(
                quantity.toString(),
                color = colors.ink,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(20.dp),
            )
            QuantityButton(icon = Icons.Rounded.Add, description = "Agregar uno", onClick = onPlus)
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
        modifier =
            Modifier
                .size(48.dp)
                .padding(7.dp)
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
    val destinationLabel =
        when (state.checkoutDestination) {
            OrderDestination.TAKE_AWAY -> "Para llevar"
            OrderDestination.IN_SPACE -> state.selectedSpaceName
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
private fun SummaryRow(
    label: String,
    value: String,
    colors: com.vaiinilla.app.ui.theme.VaiinillaColors,
) {
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
    val colors = LocalVaiinillaColors.current
    Surface(
        color = colors.yolk.copy(alpha = 0.35f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(message, color = colors.ink, modifier = Modifier.padding(14.dp))
    }
}

@Composable
private fun ErrorBanner(message: String) {
    val colors = LocalVaiinillaColors.current
    Surface(
        color = colors.coral.copy(alpha = 0.22f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(message, color = colors.ink, modifier = Modifier.padding(14.dp))
    }
}

@Composable
private fun EmptyCart(onMenu: () -> Unit) {
    EmptyState(
        icon = Icons.Outlined.ShoppingCart,
        title = "Tu pedido está vacío",
        message = "Agrega algo del menú para empezar.",
        actionLabel = "Ver menú",
        onAction = onMenu,
    )
}
