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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
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
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
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
    profileInitials: String = "?",
    onOpenAccount: () -> Unit = {},
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
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
                .background(colors.paper)
                .imePadding(),
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
                    top = 8.dp,
                    bottom = VaiinillaBottomNavClearance + 126.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item {
                CartTopBar(
                    onBack = {
                        haptics.click()
                        onMenu()
                    },
                    profileInitials = profileInitials,
                    onOpenAccount = onOpenAccount,
                )
            }
            item {
                Text(
                    "REVISA Y CONFIRMA",
                    color = colors.muted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.2.sp,
                )
                Text(
                    "Tu pedido",
                    color = colors.ink,
                    fontSize = 38.sp,
                    lineHeight = 42.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1.8).sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
                )
            }

            if (state.cartLines.isEmpty()) {
                item { EmptyCart(onMenu) }
            } else {
                item {
                    CartSectionHead("Pedido", productCountLabel)
                    CartItemsPanel(
                        lines = state.cartLines,
                        onQuantityChange = { lineKey, delta ->
                            haptics.click()
                            onQuantityChange(lineKey, delta)
                        },
                    )
                }

                item {
                    Spacer(Modifier.height(22.dp))
                    CartSectionHead(
                        title = "Entrega",
                        meta = if (canChooseInSpace) "Elige cómo recibirlo" else "Recoge en barra",
                    )
                    CheckoutDestinationPicker(
                        selected = state.checkoutDestination,
                        selectedSpaceName = state.selectedSpaceName,
                        onSelect = {
                            haptics.selection()
                            onDestinationChange(it)
                        },
                        showInSpace = canChooseInSpace,
                    )
                }
                if (state.checkoutDestination == OrderDestination.IN_SPACE && checkoutSpaces.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(14.dp))
                        CheckoutSpacePicker(
                            selectedSpaceId = state.selectedSpaceId ?: checkoutSpaces.first().id,
                            spaces = checkoutSpaces,
                            onSelect = {
                                haptics.selection()
                                onSpaceChange(it)
                            },
                        )
                    }
                }
                item {
                    Spacer(Modifier.height(22.dp))
                    CartSectionHead("Pago", "Elige tu método")
                    CheckoutPaymentPicker(
                        selected = state.checkoutPayment,
                        onSelect = {
                            haptics.selection()
                            onPaymentChange(it)
                        },
                    )
                }
                item {
                    Spacer(Modifier.height(16.dp))
                    CartNotesField(
                        value = state.kitchenNotes,
                        onValueChange = onNotesChange,
                    )
                }
                item {
                    Spacer(Modifier.height(16.dp))
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

        if (shouldShowCheckoutDock(state.cartLines.isNotEmpty(), imeVisible)) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = VaiinillaBottomNavClearance + 2.dp,
                        ),
            ) {
                CheckoutDockButton(
                    label = confirmLabel(guestAuthRequired),
                    subtitle = "$productCountLabel · $destinationLabel",
                    price = moneyLabel(state.cartPreviewTotal),
                    enabled = canConfirm,
                    loading = state.creatingOrder,
                    onClick = {
                        haptics.impact()
                        onConfirm()
                    },
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
private fun CartTopBar(
    onBack: () -> Unit,
    profileInitials: String,
    onOpenAccount: () -> Unit,
) {
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
                    .physicalPress(scale = PhysicalPressScale.Small, onClick = onBack),
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
                    .background(colors.paper2)
                    .physicalPress(scale = PhysicalPressScale.Small, onClick = onOpenAccount),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                profileInitials,
                color = colors.ink,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
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
                .padding(start = 2.dp, end = 2.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            title,
            color = colors.ink,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp,
        )
        Text(
            meta,
            color = colors.muted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
private fun confirmLabel(guestAuthRequired: Boolean): String =
    when {
        guestAuthRequired -> "Continuar para confirmar"
        else -> "Confirmar pedido"
    }

@Composable
private fun CartItemsPanel(
    lines: List<CartLine>,
    onQuantityChange: (lineKey: String, delta: Int) -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.paper2,
        shape = RoundedCornerShape(26.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            lines.forEachIndexed { index, line ->
                CartLineRow(
                    line = line,
                    onMinus = { onQuantityChange(line.key, -1) },
                    onPlus = { onQuantityChange(line.key, 1) },
                )
                if (index != lines.lastIndex) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colors.line),
                    )
                }
            }
        }
    }
}

@Composable
private fun CartLineRow(
    line: CartLine,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
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
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ProductImage(
            imageUrl = line.product.imageUrl,
            contentDescription = line.product.name,
            modifier =
                Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(18.dp)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                line.product.name,
                color = colors.ink,
                fontSize = 19.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.35).sp,
                maxLines = 1,
            )
            Text(
                variants.ifBlank { "Preparación original" },
                color = colors.muted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 1,
            )
            Text(
                moneyLabel(Money.cartLinePreview(line)),
                color = colors.ink,
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        QuantityStepper(
            quantity = line.quantity,
            onMinus = onMinus,
            onPlus = onPlus,
        )
    }
}

@Composable
private fun QuantityStepper(
    quantity: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val background = if (colors.isDark) colors.paper2 else colors.ink
    val foreground = if (colors.isDark) colors.ink else colors.paper
    Surface(
        color = background,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuantityButton(
                icon = Icons.Rounded.Remove,
                description = "Quitar uno",
                tint = foreground,
                onClick = onMinus,
            )
            Text(
                quantity.toString(),
                color = foreground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(28.dp),
            )
            QuantityButton(
                icon = Icons.Rounded.Add,
                description = "Agregar uno",
                tint = foreground,
                onClick = onPlus,
            )
        }
    }
}

@Composable
private fun QuantityButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun CartNotesField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val limited = if (value.length <= NOTES_MAX) value else value.take(NOTES_MAX)
    BasicTextField(
        value = limited,
        onValueChange = { next -> onValueChange(next.take(NOTES_MAX)) },
        modifier =
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, colors.line, RoundedCornerShape(22.dp)),
        textStyle =
            TextStyle(
                color = colors.ink,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        cursorBrush = SolidColor(colors.ink),
        maxLines = 2,
        decorationBox = { input ->
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = null,
                    tint = colors.ink,
                    modifier = Modifier.size(22.dp),
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (limited.isBlank()) {
                        Text(
                            "Añadir nota para cocina",
                            color = colors.ink,
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    input()
                }
                Surface(
                    color = colors.paper2,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        if (limited.isBlank()) "Opcional" else "${limited.length}/$NOTES_MAX",
                        color = colors.muted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    )
                }
            }
        },
    )
}

private const val NOTES_MAX = 90

@Composable
private fun CheckoutDockButton(
    label: String,
    subtitle: String,
    price: String,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val dockBackground = if (colors.isDark) colors.paper2 else colors.ink
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(94.dp),
        color = dockBackground,
        shape = RoundedCornerShape(30.dp),
        shadowElevation = if (enabled) 12.dp else 0.dp,
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .physicalPress(
                        enabled = enabled && !loading,
                        scale = PhysicalPressScale.Default,
                        onClick = onClick,
                    ),
            color = if (enabled) colors.accent else colors.paper2,
            shape = RoundedCornerShape(22.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (loading) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Confirmando pedido",
                            color = colors.accentInk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                        )
                        Text(
                            "Un momento…",
                            color = colors.accentInk.copy(alpha = 0.72f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    CircularProgressIndicator(
                        color = colors.accentInk,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            label,
                            color = if (enabled) colors.accentInk else colors.muted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            lineHeight = 22.sp,
                            maxLines = 1,
                        )
                        Text(
                            subtitle,
                            color = if (enabled) colors.accentInk.copy(alpha = 0.72f) else colors.muted,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 2.dp),
                            maxLines = 1,
                        )
                    }
                    Box(
                        modifier =
                            Modifier
                                .padding(horizontal = 16.dp)
                                .width(1.dp)
                                .height(34.dp)
                                .background(colors.ink.copy(alpha = 0.14f)),
                    )
                    Text(
                        price,
                        color = if (enabled) colors.accentInk else colors.muted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
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
            OrderDestination.IN_SPACE -> state.selectedSpaceName.ifBlank { "En mesa" }
        }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.paper2,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            SummaryRow("Subtotal", moneyLabel(state.cartPreviewTotal))
            SummaryRow("Entrega · $destinationLabel", "$0")
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(1.dp)
                        .background(colors.line),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Total",
                    color = colors.ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                )
                Text(
                    moneyLabel(state.cartPreviewTotal),
                    color = colors.ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    lineHeight = 30.sp,
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = colors.muted,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            value,
            color = colors.ink,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
        )
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

internal fun shouldShowCheckoutDock(
    hasCartItems: Boolean,
    imeVisible: Boolean,
): Boolean = hasCartItems && !imeVisible
