package com.vaiinilla.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.Category
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.model.SpaceCopy
import com.vaiinilla.app.ui.components.CheckoutDestinationPicker
import com.vaiinilla.app.ui.components.CheckoutSpaceOption
import com.vaiinilla.app.ui.components.CheckoutSpacePicker
import com.vaiinilla.app.ui.components.EditorialAccentButton
import com.vaiinilla.app.ui.components.GlassCircleButton
import com.vaiinilla.app.ui.components.NumberTicker
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.ProfileInitialsButton
import com.vaiinilla.app.ui.components.SHEET_HANDLE_DESCRIPTION
import com.vaiinilla.app.ui.components.VaiinillaBottomNavClearance
import com.vaiinilla.app.ui.components.animateSelectionColor
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.order.OrderFlowUiState
import com.vaiinilla.app.ui.order.canCreateOrder
import com.vaiinilla.app.ui.order.canPayAtEnd
import com.vaiinilla.app.ui.order.canPayWithCard
import com.vaiinilla.app.ui.order.cartPreviewTotal
import com.vaiinilla.app.ui.order.checkoutSpace
import com.vaiinilla.app.ui.order.checkoutSpaces
import com.vaiinilla.app.ui.order.hasUnresolvedStripePayment
import com.vaiinilla.app.ui.order.isBalancePaymentAffordable
import com.vaiinilla.app.ui.order.operationalBlockerMessage
import com.vaiinilla.app.ui.order.requiresOperationalReady
import com.vaiinilla.app.ui.order.selectedSpaceName
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.SeaGlassButton
import com.vaiinilla.app.ui.theme.SeaGlassTone
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    state: OrderFlowUiState,
    onMenu: () -> Unit,
    onQuantityChange: (lineKey: String, delta: Int) -> Unit,
    onNotesChange: (String) -> Unit,
    onDestinationChange: (OrderDestination) -> Unit,
    onSpaceChange: (Int) -> Unit = {},
    onPaymentChange: (PaymentMethod) -> Unit,
    onPayAtEnd: () -> Unit = {},
    onConfirm: () -> Unit,
    onResolvePendingStripePayment: () -> Unit = {},
    walletBalance: String? = null,
    guestAuthRequired: Boolean = false,
    /** Compra sin cuenta: sin saldo Vaiinilla y con el nombre del invitado si falta. */
    guestCheckout: Boolean = false,
    onGuestNameChange: (String) -> Unit = {},
    profileInitials: String = "?",
    onOpenAccount: () -> Unit = {},
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    recentOrders: List<OrderDetail> = emptyList(),
    onSelectOrder: (String) -> Unit = {},
    /** El alta en el negocio corre antes del pedido: el botón espera igual que con el pedido. */
    preparingCheckout: Boolean = false,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var paymentDialogOpen by remember { mutableStateOf(false) }
    BackHandler(enabled = paymentDialogOpen) {
        paymentDialogOpen = false
    }
    val checkoutSpaces = state.checkoutSpaces.map { space -> CheckoutSpaceOption(space.id, space.name) }
    val canChooseInSpace = checkoutSpaces.isNotEmpty()
    // A guest must be able to continue into Firebase auth before the protected
    // operational-status check runs. submitOrder performs that check after auth.
    val canConfirm =
        if (guestAuthRequired) {
            state.cartLines.isNotEmpty() &&
                !state.creatingOrder &&
                !preparingCheckout &&
                !state.hasUnresolvedStripePayment
        } else {
            state.cartLines.isNotEmpty() &&
                !state.creatingOrder &&
                !preparingCheckout &&
                !state.hasUnresolvedStripePayment &&
                (state.canCreateOrder || state.operationalStatus == null)
        }

    val destinationLabel =
        when (state.checkoutDestination) {
            OrderDestination.TAKE_AWAY -> "Para llevar"
            OrderDestination.IN_SPACE -> SpaceCopy.destinationTitle(state.checkoutSpace?.type)
        }
    val productCountLabel =
        if (state.cartLines.size == 1) {
            "1 producto"
        } else {
            "${state.cartLines.size} productos"
        }

    // Vainiverso: el mostrador con Vaini y la bandeja arriba; la lista en una hoja de vidrio que sube.
    val palette = scenePaletteForTheme()
    val listState = rememberLazyListState()
    val glassColors =
        colors.copy(
            paper = Color.Transparent,
            solidPaper = colors.onInk,
            paper2 = palette.glassCard,
            line = palette.glassLine,
            ink = palette.ink,
            muted = palette.muted,
        )
    // Con varios productos el mostrador se asoma menos, para que el cobro se vea sin hacer scroll.
    val sceneWindow by animateFloatAsState(
        targetValue = cartSceneWindow(state.cartLines.size),
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 300f),
        label = "cart-scene-window",
    )
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val windowPx = constraints.maxHeight * sceneWindow
        val windowDp = with(density) { windowPx.toDp() }
        val sheetTop = {
            if (listState.firstVisibleItemIndex == 0) windowPx - listState.firstVisibleItemScrollOffset else 0f
        }
        CompositionLocalProvider(LocalScene provides palette) {
            CartCounterBackdrop(
                sheetTop = sheetTop,
                lines = state.cartLines,
                totalLabel = if (state.cartLines.isEmpty()) null else moneyLabel(state.cartPreviewTotal),
            )
        }
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier =
                Modifier
                    .fillMaxSize()
                    .imePadding(),
        ) {
            CompositionLocalProvider(LocalVaiinillaColors provides glassColors) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 0.dp,
                            // Con productos la barra de pestañas se va y el botón de pagar toma su lugar.
                            bottom =
                                if (state.cartLines.isEmpty()) {
                                    VaiinillaBottomNavClearance + 48.dp
                                } else {
                                    navBarBottom + 120.dp
                                },
                        ),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    item(key = "counter-window") { Spacer(Modifier.height(windowDp)) }
                    // Con productos, el pizarrón del mostrador ya dice "Tu bandeja": el título sobra y quita espacio al cobro.
                    if (state.cartLines.isEmpty()) {
                        item {
                            Spacer(Modifier.height(22.dp))
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
                    } else {
                        item { Spacer(Modifier.height(18.dp)) }
                    }

                    if (state.cartLines.isEmpty()) {
                        item { EmptyCart(onMenu, recentOrders, onSelectOrder) }
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

                        if (!canChooseInSpace) {
                            item {
                                Spacer(Modifier.height(18.dp))
                                PickupLine(
                                    readyInMinutes = state.cartLines.maxOfOrNull { it.product.estimatedTimeMinutes },
                                )
                            }
                        } else {
                            item {
                                Spacer(Modifier.height(22.dp))
                                CartSectionHead(title = "Entrega", meta = "Elige cómo recibirlo")
                                CheckoutDestinationPicker(
                                    selected = state.checkoutDestination,
                                    selectedSpaceName = state.selectedSpaceName,
                                    onSelect = {
                                        haptics.selection()
                                        onDestinationChange(it)
                                    },
                                    showInSpace = canChooseInSpace,
                                    spaceType = state.checkoutSpace?.type,
                                )
                            }
                        }
                        if (state.checkoutDestination == OrderDestination.IN_SPACE && checkoutSpaces.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(14.dp))
                                CheckoutSpacePicker(
                                    selectedSpaceId = state.checkoutSpace?.id ?: checkoutSpaces.first().id,
                                    spaces = checkoutSpaces,
                                    onSelect = {
                                        haptics.selection()
                                        onSpaceChange(it)
                                    },
                                    spaceType = state.checkoutSpace?.type,
                                )
                            }
                        }
                        if (state.guestEntryRequired) {
                            item {
                                Spacer(Modifier.height(16.dp))
                                GuestNameField(
                                    value = state.guestName,
                                    onValueChange = onGuestNameChange,
                                )
                            }
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
                            when {
                                state.hasUnresolvedStripePayment ->
                                    "Tienes un pago Stripe pendiente. " +
                                        "Resuélvelo antes de crear otro pedido para evitar cobros duplicados."
                                state.requiresOperationalReady &&
                                    !guestAuthRequired &&
                                    state.operationalStatus != null ->
                                    state.operationalBlockerMessage
                                else -> null
                            }
                        if (blockerMessage != null && blockerMessage != state.createOrderError) {
                            item {
                                Spacer(Modifier.height(12.dp))
                                WarningBanner(
                                    message = blockerMessage,
                                    actionLabel =
                                        if (state.hasUnresolvedStripePayment) "Revisar pago pendiente" else null,
                                    actionLoading = state.resolvingPendingStripePayment,
                                    onAction =
                                        if (state.hasUnresolvedStripePayment) {
                                            onResolvePendingStripePayment
                                        } else {
                                            null
                                        },
                                )
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
            }

            if (shouldShowCheckoutDock(state.cartLines.isNotEmpty(), imeVisible)) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                ) {
                    if (state.hasUnresolvedStripePayment) {
                        CheckoutDockButton(
                            label = "Continuar pago pendiente",
                            subtitle = "Abre tu pedido anterior y revisa Stripe",
                            price = "Abrir",
                            enabled = !state.resolvingPendingStripePayment,
                            loading = state.resolvingPendingStripePayment,
                            loadingLabel = "Buscando tu pago",
                            loadingSubtitle = "Consultando el pedido pendiente…",
                            onClick = {
                                haptics.impact()
                                onResolvePendingStripePayment()
                            },
                        )
                    } else {
                        CheckoutDockButton(
                            label = confirmLabel(guestAuthRequired),
                            subtitle = "$productCountLabel · $destinationLabel",
                            price = moneyLabel(state.cartPreviewTotal),
                            enabled = canConfirm,
                            loading = state.creatingOrder || preparingCheckout,
                            onClick = {
                                haptics.impact()
                                paymentDialogOpen = true
                            },
                        )
                    }
                }
            }

            if (paymentDialogOpen) {
                PaymentMethodOverlay(
                    total = moneyLabel(state.cartPreviewTotal),
                    initialMethod = state.checkoutPayment,
                    initialPayAtEnd = state.checkoutPayAtEnd,
                    balanceAffordable = state.isBalancePaymentAffordable(walletBalance),
                    showBalance = !guestCheckout,
                    walletBalance = walletBalance,
                    payAtEndAvailable = state.canPayAtEnd,
                    cardAvailable = state.canPayWithCard,
                    onDismiss = { paymentDialogOpen = false },
                    onSelectPayAtEnd = {
                        paymentDialogOpen = false
                        haptics.selection()
                        onPayAtEnd()
                        onConfirm()
                    },
                    onSelect = { method ->
                        paymentDialogOpen = false
                        haptics.selection()
                        onPaymentChange(method)
                        onConfirm()
                    },
                )
            }
        }
        // La barra de estado queda sobre vidrio cuando la hoja llega arriba; el regreso y la cuenta flotan encima.
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .graphicsLayer { alpha = if (sheetTop() < size.height) 1f else 0f }
                .background(palette.stage.copy(alpha = 0.92f)),
        )
        Box(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            CartTopBar(
                onBack = {
                    haptics.click()
                    onMenu()
                },
                profileInitials = profileInitials,
                onOpenAccount = onOpenAccount,
            )
        }
    }
}

/** Fracción de la pantalla para el mostrador: con más de un producto baja a lo justo para ver a Vaini. */
internal fun cartSceneWindow(lineCount: Int): Float = if (lineCount > 1) 0.3f else 0.42f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentMethodOverlay(
    total: String,
    initialMethod: PaymentMethod,
    initialPayAtEnd: Boolean,
    balanceAffordable: Boolean,
    showBalance: Boolean,
    walletBalance: String?,
    payAtEndAvailable: Boolean,
    onDismiss: () -> Unit,
    onSelectPayAtEnd: () -> Unit,
    onSelect: (PaymentMethod) -> Unit,
    /** "Pago con Stripe" solo si el dueño activó la tarjeta en su panel (el cobro sigue intacto). */
    cardAvailable: Boolean = false,
) {
    val colors = LocalVaiinillaColors.current
    // Arranca con el último método que usó, si hoy sigue disponible: pagar vuelve a ser un toque.
    val initialAllowed =
        when (initialMethod) {
            PaymentMethod.CASH -> true
            PaymentMethod.BALANCE -> showBalance && balanceAffordable
            PaymentMethod.STRIPE -> cardAvailable
        }
    var payAtEndSelected by remember { mutableStateOf(initialPayAtEnd && payAtEndAvailable) }
    var selectedMethod by remember {
        mutableStateOf(if (!payAtEndSelected && initialAllowed) initialMethod else null)
    }
    var selectionLocked by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val hasSelection = selectedMethod != null || payAtEndSelected

    fun continueWithSelection() {
        if (selectionLocked) return
        if (payAtEndSelected) {
            selectionLocked = true
            onSelectPayAtEnd()
            return
        }
        val method = selectedMethod ?: return
        selectionLocked = true
        onSelect(method)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.paper,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier =
                    Modifier
                        .padding(top = 12.dp, bottom = 6.dp)
                        .size(width = 38.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.muted.copy(alpha = 0.35f))
                        .semantics { contentDescription = SHEET_HANDLE_DESCRIPTION },
            )
        },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 22.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header Row matching reference
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "¿Cómo quieres pagar?",
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = colors.ink,
                )
                Box(
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.paper2)
                            .clickable(enabled = !selectionLocked, onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Cerrar",
                        tint = colors.ink,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // Payment Options styled as cards
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (payAtEndAvailable) {
                    PaymentMethodCardOption(
                        icon = Icons.Outlined.Receipt,
                        title = "Pagar al final",
                        subtitle = "Pide lo que quieras y paga toda la cuenta al irte",
                        selected = payAtEndSelected,
                        enabled = !selectionLocked,
                        onClick = {
                            payAtEndSelected = true
                            selectedMethod = null
                        },
                    )
                }
                PaymentMethodCardOption(
                    icon = Icons.Outlined.Payments,
                    title = "Pago en caja",
                    subtitle = "Pagas en caja y se prepara al pagar",
                    selected = selectedMethod == PaymentMethod.CASH,
                    enabled = !selectionLocked,
                    onClick = {
                        payAtEndSelected = false
                        selectedMethod = PaymentMethod.CASH
                    },
                )
                // El saldo es de la cuenta: un invitado no lo ve.
                if (showBalance) {
                    PaymentMethodCardOption(
                        icon = Icons.Outlined.AccountBalanceWallet,
                        title = "Saldo Vaiinilla",
                        subtitle =
                            when {
                                walletBalance == null -> "Usa el saldo de tu cuenta"
                                balanceAffordable -> "Disponible: ${moneyLabel(walletBalance)}"
                                else -> "No alcanza · tienes ${moneyLabel(walletBalance)}"
                            },
                        selected = selectedMethod == PaymentMethod.BALANCE,
                        enabled = !selectionLocked && balanceAffordable,
                        onClick = {
                            payAtEndSelected = false
                            selectedMethod = PaymentMethod.BALANCE
                        },
                    )
                }
                if (cardAvailable) {
                    PaymentMethodCardOption(
                        icon = Icons.Outlined.CreditCard,
                        title = "Pago con Stripe",
                        subtitle = "Débito o crédito, aquí mismo",
                        selected = selectedMethod == PaymentMethod.STRIPE,
                        enabled = !selectionLocked,
                        onClick = {
                            payAtEndSelected = false
                            selectedMethod = PaymentMethod.STRIPE
                        },
                    )
                }
            }

            // Action Button matching reference
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .height(52.dp)
                        .physicalPress(
                            enabled = !selectionLocked && hasSelection,
                            onClick = ::continueWithSelection,
                        ),
                color = if (hasSelection) colors.accent else colors.paper2,
                contentColor = if (hasSelection) colors.accentInk else colors.muted,
                shape = RoundedCornerShape(26.dp),
                shadowElevation = if (hasSelection) 6.dp else 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = paymentActionLabel(selectedMethod, payAtEndSelected, total),
                        color = if (hasSelection) colors.accentInk else colors.muted,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodCardOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val borderColor = animateSelectionColor(if (selected) colors.ink else colors.line, "payment-card-border")
    val borderWidth by animateDpAsState(if (selected) 1.5.dp else 1.dp, label = "payment-card-border-width")
    val radioFill = animateSelectionColor(if (selected) colors.ink else Color.Transparent, "payment-card-radio")
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(borderWidth, borderColor, RoundedCornerShape(20.dp))
                .physicalPress(enabled = enabled, onClick = onClick)
                .semantics {
                    role = Role.RadioButton
                    this.selected = selected
                },
        color = colors.paper2,
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) colors.ink else colors.muted,
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (enabled) colors.ink else colors.muted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                )
                Text(
                    text = subtitle,
                    color = colors.muted,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Box(
                modifier =
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(radioFill)
                        .border(1.5.dp, if (selected) Color.Transparent else colors.line, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = selected,
                    enter = scaleIn(spring(dampingRatio = 0.55f, stiffness = 600f)) + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = colors.onInk,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

/** El botón dice lo que pasa al tocarlo: con efectivo se pide y se paga después en caja. */
internal fun paymentActionLabel(
    method: PaymentMethod?,
    payAtEnd: Boolean,
    total: String,
): String =
    when {
        payAtEnd -> "Pedir · pagas al final"
        method == PaymentMethod.CASH -> "Pedir · pagas $total en caja"
        method == PaymentMethod.BALANCE -> "Pagar $total con saldo"
        method == PaymentMethod.STRIPE -> "Pagar $total"
        else -> "Elige cómo pagar"
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
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        GlassCircleButton(
            icon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
            contentDescription = "Volver",
            onClick = onBack,
            iconSize = 22.dp,
        )
        ProfileInitialsButton(profileInitials, onClick = onOpenAccount)
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

/** Con una sola forma de entrega no hay nada que elegir: se dice en una línea. */
@Composable
private fun PickupLine(readyInMinutes: Int?) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.Outlined.ShoppingBag,
            contentDescription = null,
            tint = colors.ink,
            modifier = Modifier.size(20.dp),
        )
        Text(
            listOfNotNull(
                "Para llevar · recoges en barra",
                readyInMinutes?.takeIf { it > 0 }?.let { "listo en ~$it min" },
            ).joinToString(" · "),
            color = colors.ink,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun confirmLabel(guestAuthRequired: Boolean): String =
    when {
        guestAuthRequired -> "Continuar para pagar"
        else -> "Pagar"
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
            placeholder = { m -> ProductMonogram(line.product.name, m) },
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                line.product.name,
                color = colors.ink,
                fontSize = 17.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.35).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                variants.ifBlank { "Preparación original" },
                color = colors.muted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (line.notes.isNotEmpty()) {
                Text(
                    "Nota: ${line.notes}",
                    color = colors.ink,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AnimatedContent(
                targetState = line.quantity,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInVertically { height -> height } + fadeIn()) togetherWith
                            (slideOutVertically { height -> -height } + fadeOut())
                    } else {
                        (slideInVertically { height -> -height } + fadeIn()) togetherWith
                            (slideOutVertically { height -> height } + fadeOut())
                    }.using(SizeTransform(clip = false))
                },
                label = "cart_item_price_ticker",
            ) { animatedQuantity ->
                Text(
                    moneyLabel(Money.cartLinePreview(line.copy(quantity = animatedQuantity))),
                    color = colors.ink,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
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
    val foreground = colors.ink
    Surface(
        color = colors.ink.copy(alpha = 0.06f),
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier = Modifier.height(44.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuantityButton(
                icon = Icons.Rounded.Remove,
                description = "Quitar uno",
                tint = foreground,
                onClick = onMinus,
            )
            AnimatedContent(
                targetState = quantity,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInVertically { height -> height } + fadeIn()) togetherWith
                            (slideOutVertically { height -> -height } + fadeOut())
                    } else {
                        (slideInVertically { height -> -height } + fadeIn()) togetherWith
                            (slideOutVertically { height -> height } + fadeOut())
                    }.using(SizeTransform(clip = false))
                },
                label = "cart_stepper_ticker",
            ) { count ->
                Text(
                    count.toString(),
                    color = foreground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(28.dp),
                )
            }
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
                .clip(CircleShape)
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun GuestNameField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Tu nombre") },
        supportingText = {
            Text(
                "Para confirmar solo necesitamos tu nombre. " +
                    "Al continuar aceptas los términos y el aviso de privacidad vigentes.",
            )
        },
        singleLine = true,
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.ink,
                unfocusedTextColor = colors.ink,
                focusedBorderColor = colors.ink,
                unfocusedBorderColor = colors.line,
                focusedLabelColor = colors.muted,
                unfocusedLabelColor = colors.muted,
                cursorColor = colors.ink,
            ),
        modifier = Modifier.fillMaxWidth(),
    )
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
    loadingLabel: String = "Confirmando pedido",
    loadingSubtitle: String = "Un momento…",
    onClick: () -> Unit,
) {
    // Como `CheckoutDockButton` en iOS: una pieza de vidrio redondeada de 74, lima si se puede pedir.
    SeaGlassButton(
        tone = if (enabled || loading) SeaGlassTone.lima else SeaGlassTone.vainilla,
        onClick = onClick,
        modifier =
            Modifier
                .padding(bottom = 8.dp)
                .fillMaxWidth()
                .height(74.dp),
        shape = RoundedCornerShape(28.dp),
        enabled = enabled && !loading,
    ) {
        val ink = LocalContentColor.current
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(loadingLabel, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        loadingSubtitle,
                        color = ink.copy(alpha = 0.72f),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                CircularProgressIndicator(color = ink, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 22.sp, maxLines = 1)
                    Text(
                        subtitle,
                        color = if (enabled) ink.copy(alpha = 0.72f) else ink,
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
                            .background(ink.copy(alpha = 0.2f)),
                )
                AnimatedContent(
                    targetState = price,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInVertically { height -> height } + fadeIn()) togetherWith
                                (slideOutVertically { height -> -height } + fadeOut())
                        } else {
                            (slideInVertically { height -> -height } + fadeIn()) togetherWith
                                (slideOutVertically { height -> height } + fadeOut())
                        }.using(SizeTransform(clip = false))
                    },
                    label = "checkout_price_ticker",
                ) { p ->
                    Text(p, color = ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }
        }
    }
}

@Composable
private fun OrderSummaryCard(state: OrderFlowUiState) {
    val colors = LocalVaiinillaColors.current
    // Entrega siempre cuesta $0, así que subtotal y total eran el mismo número dos veces: queda el total.
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.paper2,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
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
                NumberTicker(
                    value = state.cartPreviewTotal.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    label = "cart-total-ticker",
                ) { total ->
                    Text(
                        moneyLabel(total.setScale(2, RoundingMode.HALF_UP).toPlainString()),
                        color = colors.ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        lineHeight = 30.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun WarningBanner(
    message: String,
    actionLabel: String? = null,
    actionLoading: Boolean = false,
    onAction: (() -> Unit)? = null,
) {
    val colors = LocalVaiinillaColors.current
    Surface(
        color = colors.yolk.copy(alpha = 0.35f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = message,
                color = colors.ink,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (actionLabel != null && onAction != null) {
                Surface(
                    color = colors.ink,
                    contentColor = colors.onInk,
                    shape = RoundedCornerShape(999.dp),
                    modifier =
                        Modifier.physicalPress(
                            enabled = !actionLoading,
                            scale = PhysicalPressScale.Small,
                            onClick = onAction,
                        ),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (actionLoading) {
                            CircularProgressIndicator(
                                color = colors.onInk,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        Text(
                            text = if (actionLoading) "Abriendo pago…" else actionLabel,
                            color = colors.onInk,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
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
private fun EmptyCart(
    onMenu: () -> Unit,
    recentOrders: List<OrderDetail>,
    onSelectOrder: (String) -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "¿Qué se te antoja?",
            color = colors.ink,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Text(
            "Pide algo del menú y aparece aquí.",
            color = colors.muted,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        EditorialAccentButton(
            text = "Ver menú",
            onClick = onMenu,
            modifier = Modifier.padding(top = 20.dp).widthIn(max = 270.dp),
        )

        if (recentOrders.isNotEmpty()) {
            Text(
                "PEDIDOS ANTERIORES",
                color = colors.muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.2.sp,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp, bottom = 10.dp),
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colors.paper2,
                shape = RoundedCornerShape(20.dp),
            ) {
                Column {
                    recentOrders.forEachIndexed { index, order ->
                        RecentOrderRow(
                            order = order,
                            onClick = { onSelectOrder(order.summary.id) },
                        )
                        if (index != recentOrders.lastIndex) {
                            Box(
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
    }
}

@Composable
private fun RecentOrderRow(
    order: OrderDetail,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val summary = order.summary
    val itemsLabel =
        order.items.joinToString(" · ") { item -> "${item.quantity}× ${item.productName}" }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .physicalPress(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                itemsLabel,
                color = colors.ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "#${summary.folio} · ${summary.state.label}",
                color = colors.muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 3.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            moneyLabel(summary.total),
            color = colors.ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            modifier = Modifier.padding(start = 12.dp),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.muted,
            modifier = Modifier.padding(start = 4.dp).size(20.dp),
        )
    }
}

internal fun shouldShowCheckoutDock(
    hasCartItems: Boolean,
    imeVisible: Boolean,
): Boolean = hasCartItems && !imeVisible
