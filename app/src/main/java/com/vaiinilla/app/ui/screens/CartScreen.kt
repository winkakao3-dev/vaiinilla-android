package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.vaiinilla.app.ui.components.EmptyState
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.VaiinillaBottomNavClearance
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.paymentMethodLabel
import com.vaiinilla.app.ui.components.physicalPress
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

    val destinationLabel =
        when (state.checkoutDestination) {
            OrderDestination.TAKE_AWAY -> "Para llevar"
            OrderDestination.IN_SPACE -> "En mesa"
        }
    val productCountLabel =
        if (state.cartLines.size == 1) {
            "1 producto"
        } else {
            "${state.cartLines.size} productos"
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
                    top = 10.dp,
                    bottom = VaiinillaBottomNavClearance + 78.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item {
                CartTopBar(onBack = onMenu)
            }
            item {
                Text(
                    "Revisa antes de pedir",
                    color = colors.muted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
                Text(
                    "Tu pedido",
                    color = colors.ink,
                    fontSize = 36.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1.6).sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
                )
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
                    Spacer(Modifier.height(12.dp))
                }

                item {
                    CartSectionHead("Entrega", destinationLabel)
                    CheckoutDestinationPicker(
                        selected = state.checkoutDestination,
                        selectedSpaceName = state.selectedSpaceName,
                        onSelect = onDestinationChange,
                        showInSpace = canChooseInSpace,
                    )
                }
                if (state.checkoutDestination == OrderDestination.IN_SPACE && canChooseInSpace) {
                    item {
                        Spacer(Modifier.height(10.dp))
                        CheckoutSpacePicker(
                            selectedSpaceId = state.selectedSpaceId,
                            spaces = checkoutSpaces,
                            onSelect = onSpaceChange,
                        )
                    }
                }
                item {
                    Spacer(Modifier.height(32.dp))
                    CartSectionHead("Pago", "Elige uno")
                    CheckoutPaymentPicker(
                        selected = state.checkoutPayment,
                        onSelect = onPaymentChange,
                    )
                }
                item {
                    Spacer(Modifier.height(32.dp))
                    CartSectionHead("Notas", "Opcional")
                    CartNotesField(
                        value = state.kitchenNotes,
                        onValueChange = onNotesChange,
                    )
                }
                item {
                    Spacer(Modifier.height(32.dp))
                    CartSectionHead("Resumen", productCountLabel)
                    OrderSummaryCard(state = state)
                }
                val blockerMessage =
                    if (state.requiresOperationalReady && !guestAuthRequired && state.operationalStatus != null) {
                        state.operationalBlockerMessage
                    } else {
                        null
                    }
                if (blockerMessage != null && blockerMessage != state.createOrderError) {
                    item {
                        Spacer(Modifier.height(12.dp))
                        WarningBanner(message = blockerMessage)
                    }
                }
                state.createOrderError?.let { error ->
                    item {
                        Spacer(Modifier.height(12.dp))
                        ErrorBanner(message = error)
                    }
                }
            }
        }

        if (state.cartLines.isNotEmpty()) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(start = 20.dp, end = 20.dp, bottom = 90.dp),
            ) {
                CheckoutDockButton(
                    label = confirmLabel(guestAuthRequired),
                    price = moneyLabel(state.cartPreviewTotal),
                    enabled = canConfirm,
                    loading = state.creatingOrder,
                    onClick = onConfirm,
                )
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
private fun CartTopBar(onBack: () -> Unit) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.paper2)
                    .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = "Volver",
                tint = colors.ink,
                modifier = Modifier.size(21.dp),
            )
        }
        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.ink),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "VA",
                color = colors.paper,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                letterSpacing = 0.6.sp,
            )
        }
    }
}

@Composable
private fun CartSectionHead(
    title: String,
    meta: String,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            title,
            color = colors.ink,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.7).sp,
        )
        Text(
            meta,
            color = colors.muted,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun confirmLabel(
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
        shape = RoundedCornerShape(28.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ProductImage(
                imageUrl = line.product.imageUrl,
                contentDescription = line.product.name,
                modifier =
                    Modifier
                        .width(92.dp)
                        .height(112.dp)
                        .clip(RoundedCornerShape(20.dp)),
            )
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(112.dp)
                        .padding(vertical = 4.dp),
            ) {
                Text(
                    line.product.name,
                    color = colors.ink,
                    fontSize = 20.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                )
                val variants =
                    line.product.optionGroups
                        .flatMap { it.options }
                        .filter { it.id in line.selectedOptionIds }
                        .joinToString(" · ") { option ->
                            if (option.extraPrice == "0.00") {
                                option.name
                            } else {
                                "${option.name} +${moneyLabel(option.extraPrice)}"
                            }
                        }
                if (variants.isNotBlank()) {
                    Text(
                        variants,
                        color = colors.muted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        moneyLabel(Money.cartLinePreview(line)),
                        color = colors.ink,
                        fontSize = 26.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.8).sp,
                    )
                    QuantityStepper(
                        quantity = line.quantity,
                        onMinus = onMinus,
                        onPlus = onPlus,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuantityStepper(
    quantity: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Surface(
        color = colors.paper,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            QuantityButton(icon = Icons.Rounded.Remove, description = "Quitar uno", onClick = onMinus)
            Text(
                quantity.toString(),
                color = colors.ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(30.dp),
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
    Box(
        modifier =
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.ink)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = colors.paper, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun CartNotesField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val limited = if (value.length <= NOTES_MAX) value else value.take(NOTES_MAX)
    Box(modifier = Modifier.fillMaxWidth()) {
        BasicTextField(
            value = limited,
            onValueChange = { next -> onValueChange(next.take(NOTES_MAX)) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(122.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, colors.line, RoundedCornerShape(22.dp))
                    .padding(16.dp),
            textStyle =
                TextStyle(
                    color = colors.ink,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                ),
            cursorBrush = SolidColor(colors.ink),
            decorationBox = { input ->
                Box {
                    if (limited.isBlank()) {
                        Text(
                            "Ej. sin cebolla, salsa aparte",
                            color = colors.muted,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                        )
                    }
                    input()
                }
            },
        )
        Text(
            "${limited.length}/$NOTES_MAX",
            color = colors.muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 13.dp, bottom = 11.dp),
        )
    }
}

private const val NOTES_MAX = 90

@Composable
private fun CheckoutDockButton(
    label: String,
    price: String,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .physicalPress(
                    enabled = enabled && !loading,
                    scale = PhysicalPressScale.Default,
                    onClick = onClick,
                ),
        color = if (enabled) colors.accent else colors.paper2,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = if (enabled) 8.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (loading) {
                Text("Confirmando pedido", color = colors.accentInk, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                CircularProgressIndicator(
                    color = colors.accentInk,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Text(
                    label,
                    color = if (enabled) colors.accentInk else colors.muted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                            Modifier
                                .padding(end = 14.dp)
                                .width(1.dp)
                                .height(16.dp)
                                .background(colors.ink.copy(alpha = 0.14f)),
                    )
                    Text(
                        price,
                        color = if (enabled) colors.accentInk else colors.muted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                }
            }
        }
    }
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
        Box {
            Text(
                "V",
                color = colors.paper.copy(alpha = 0.035f),
                fontSize = 150.sp,
                fontWeight = FontWeight.Bold,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 10.dp, y = 56.dp),
            )
            Column(modifier = Modifier.padding(20.dp)) {
                SummaryRow("Subtotal", moneyLabel(state.cartPreviewTotal), colors)
                SummaryRow("Entrega", destinationLabel, colors)
                SummaryRow("Método", paymentMethodLabel(state.checkoutPayment), colors)
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .height(1.dp)
                            .background(colors.paper.copy(alpha = 0.12f)),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Total",
                        color = colors.paper,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        lineHeight = 32.sp,
                    )
                    Text(
                        moneyLabel(state.cartPreviewTotal),
                        color = colors.paper,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        lineHeight = 32.sp,
                    )
                }
                Text(
                    "El servidor confirmará precios y disponibilidad antes de crear el pedido.",
                    color = Color(0xFF8F918B),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
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
