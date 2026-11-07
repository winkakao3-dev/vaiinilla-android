package com.vaiinilla.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.vaiinilla.app.core.venue.LocalVenueContext
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
import com.vaiinilla.app.ui.components.CheckoutSpaceDeliveryPicker
import com.vaiinilla.app.ui.components.CheckoutSpaceOption
import com.vaiinilla.app.ui.components.CheckoutSpacePicker
import com.vaiinilla.app.ui.components.CremeDivider
import com.vaiinilla.app.ui.components.CremeHero
import com.vaiinilla.app.ui.components.CremePill
import com.vaiinilla.app.ui.components.CremePillRow
import com.vaiinilla.app.ui.components.CremeVenueChip
import com.vaiinilla.app.ui.components.EditorialAccentButton
import com.vaiinilla.app.ui.components.EditorialSecondaryButton
import com.vaiinilla.app.ui.components.GlassCircleButton
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.ProfileInitialsButton
import com.vaiinilla.app.ui.components.SHEET_HANDLE_DESCRIPTION
import com.vaiinilla.app.ui.components.VaiinillaBottomNavClearance
import com.vaiinilla.app.ui.components.WholeWordTitle
import com.vaiinilla.app.ui.components.animateSelectionColor
import com.vaiinilla.app.ui.components.cremeMoment
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.order.OrderFlowUiState
import com.vaiinilla.app.ui.order.asksForDeliveryDate
import com.vaiinilla.app.ui.order.asksSpaceDelivery
import com.vaiinilla.app.ui.order.canCreateOrder
import com.vaiinilla.app.ui.order.canDeliverHome
import com.vaiinilla.app.ui.hotel.guest.HotelCheckoutRoute
import com.vaiinilla.app.ui.order.canPayAtEnd
import com.vaiinilla.app.ui.order.canPayAtRegister
import com.vaiinilla.app.ui.order.isHotelRoomOrder
import com.vaiinilla.app.ui.order.canPayWithBalance
import com.vaiinilla.app.ui.order.canPayWithCard
import com.vaiinilla.app.ui.order.cartItemCount
import com.vaiinilla.app.ui.order.cartLineTotal
import com.vaiinilla.app.ui.order.cartTotalFor
import com.vaiinilla.app.ui.order.cashbackLine
import com.vaiinilla.app.ui.order.checkoutScheduleProblem
import com.vaiinilla.app.ui.order.checkoutSpace
import com.vaiinilla.app.ui.order.checkoutSpaces
import com.vaiinilla.app.ui.order.checkoutTotal
import com.vaiinilla.app.ui.order.effectiveHomePayment
import com.vaiinilla.app.ui.order.hasUnresolvedStripePayment
import com.vaiinilla.app.ui.order.homeDeliveryFee
import com.vaiinilla.app.ui.order.isBalancePaymentAffordable
import com.vaiinilla.app.ui.order.isHomeDelivery
import com.vaiinilla.app.ui.order.operationalBlockerMessage
import com.vaiinilla.app.ui.order.pickUpAtBarForOrder
import com.vaiinilla.app.ui.order.requiresOperationalReady
import com.vaiinilla.app.ui.order.selectedSpaceName
import com.vaiinilla.app.ui.order.spaceDelivery
import com.vaiinilla.app.ui.parentmode.LocalChildControls
import com.vaiinilla.app.ui.parentmode.dailyLimitLine
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.SeaGlassButton
import com.vaiinilla.app.ui.theme.SeaGlassTone
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import kotlinx.coroutines.launch
import java.math.BigDecimal

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
    /** «Pasa por él» (true) o «Te lo llevamos» (false); solo se pregunta si el negocio ofrece las dos. */
    onPickUpAtBarChange: (Boolean) -> Unit = {},
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
    /** Al tocar Pagar se vuelve a pedir el estado operativo: de él sale "Pagar al final". */
    onCheckoutOpened: () -> Unit = {},
    /** El lugar de la portada y del chip; con productos, la portada es la foto del primero. */
    venueName: String? = null,
    venuePhotoUrl: String? = null,
    /** La pill «Cartera» del carrito vacío. */
    onWallet: () -> Unit = {},
    /** A domicilio y encargos (CASA-05 a 08); sin `reparto` en el estado de la tienda no se ve nada de esto. */
    home: HomeCheckoutActions = HomeCheckoutActions(),
    /** Con silla activa en una mesa o pista, el servidor pide confirmar «Para llevar» (R15). */
    onConfirmTakeAwayWithSeat: () -> Unit = {},
    onDismissTakeAwayWithSeat: () -> Unit = {},
) {
    val venue = LocalVenueContext.current
    // El teclado: la ventana se encoge en vez de desplazarse; si no, el sistema sube toda la pantalla y además
    // imePadding deja otro hueco del alto del teclado (la pantalla «se levanta» sola).
    com.vaiinilla.app.ui.components.ResizeForKeyboard()
    // El pedido a una habitación de hotel se arma como el marco H03 de Doop.
    if (state.isHotelRoomOrder && state.cartLines.isNotEmpty()) {
        HotelCheckoutRoute(
            state = state,
            onNotesChange = onNotesChange,
            onPayAtEnd = onPayAtEnd,
            onPaymentChange = onPaymentChange,
            onConfirm = onConfirm,
            onCheckoutOpened = onCheckoutOpened,
            onResolvePendingStripePayment = onResolvePendingStripePayment,
        )
        return
    }
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
                !state.hasUnresolvedStripePayment &&
                state.unavailableCartKeys.isEmpty()
        } else {
            state.cartLines.isNotEmpty() &&
                !state.creatingOrder &&
                !preparingCheckout &&
                !state.hasUnresolvedStripePayment &&
                state.unavailableCartKeys.isEmpty() &&
                // A domicilio sin ninguna forma de pago marcada no se puede pedir (R23).
                !(state.isHomeDelivery && state.effectiveHomePayment == null) &&
                (state.canCreateOrder || state.operationalStatus == null)
        }

    val destinationLabel =
        when (state.checkoutDestination) {
            OrderDestination.TAKE_AWAY -> "Para llevar"
            OrderDestination.IN_SPACE -> SpaceCopy.destinationTitle(venue, state.checkoutSpace?.type)
            OrderDestination.HOME_DELIVERY -> "A domicilio"
        }
    val productCountLabel =
        if (state.cartLines.size == 1) {
            "1 producto"
        } else {
            "${state.cartLines.size} productos"
        }

    val listState = rememberLazyListState()
    // El rechazo del servidor llega con la hoja de pago ya cerrada y va al final de la lista: se baja a
    // verlo, si no parece que tocar Pedir no hizo nada (M05).
    LaunchedEffect(state.createOrderError) {
        if (state.createOrderError == null) return@LaunchedEffect
        val last = listState.layoutInfo.totalItemsCount - 1
        if (last >= 0) listState.animateScrollToItem(last)
    }
    // L01: la barra se tapa en cuanto el fondo de la foto llega a ella, no hasta que la foto sale de la lista;
    // si no, lo que sigue a la foto pasaba bajo la barra de estado (con letra grande, mucho más).
    val statusBarPx = WindowInsets.statusBars.getTop(density)
    val heroPx = with(density) { (if (state.cartLines.isNotEmpty()) CART_HERO_HEIGHT else 400.dp).roundToPx() }
    val heroGone by remember(statusBarPx, heroPx) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset >= heroPx - statusBarPx
        }
    }
    val notesFocus = remember { FocusRequester() }
    var notesOpen by remember { mutableStateOf(false) }
    var deliverySheetOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val hasLines = state.cartLines.isNotEmpty()
    // Un encargo con días de preparación no está «listo en ~5 min»: ese tiempo es del mostrador (EQUIPO-50).
    val readyInMinutes =
        state.cartLines
            .takeIf { lines -> lines.none { (it.product.preparationDays ?: 0) > 0 } }
            ?.maxOfOrNull { it.product.estimatedTimeMinutes }
            ?.takeIf { it > 0 }
    Box(modifier = Modifier.fillMaxSize().background(colors.paper)) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier =
                Modifier
                    .fillMaxSize()
                    .imePadding(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        // Con productos la barra de pestañas se va y el botón de pagar toma su lugar.
                        bottom = if (hasLines) navBarBottom + 120.dp else VaiinillaBottomNavClearance + 48.dp,
                    ),
            ) {
                item(key = "hero") {
                    CartHero(
                        // Con productos, la foto de lo que pide (CR3); si ninguno tiene foto, la de la tienda. Antes
                        // se ponía la del primero aunque no tuviera, y quedaba un hueco negro.
                        photoUrl =
                            state.cartLines
                                .firstNotNullOfOrNull { line -> line.product.imageUrl.takeIf { it.isNotBlank() } }
                                ?: venuePhotoUrl?.takeIf { it.isNotBlank() },
                        height = if (hasLines) CART_HERO_HEIGHT else 400.dp,
                        venueName = venueName,
                        productCount = if (hasLines) state.cartItemCount else 0,
                        profileInitials = profileInitials,
                        onBack = {
                            haptics.click()
                            onMenu()
                        },
                        onOpenAccount = onOpenAccount,
                        onClear =
                            if (hasLines) {
                                {
                                    if (!state.creatingOrder) {
                                        haptics.click()
                                        state.cartLines.forEach { line -> onQuantityChange(line.key, -line.quantity) }
                                    }
                                }
                            } else {
                                null
                            },
                    )
                }
                if (!hasLines) {
                    item(key = "pills") {
                        CremePillRow(
                            pills =
                                listOf(
                                    CremePill("Ver menú", Icons.Outlined.Restaurant, onMenu, "cart-menu"),
                                    CremePill("Cartera", Icons.Outlined.AccountBalanceWallet, onWallet, "cart-wallet"),
                                ),
                            modifier = Modifier.padding(top = 18.dp),
                        )
                    }
                    item(key = "empty") { EmptyCart(recentOrders, onSelectOrder) }
                } else {
                    // Entrega, lugar y «¿Cómo te llega?» en un solo renglón; «Cambiar» abre la hoja (CR3/CR2).
                    val choosesDelivery = canChooseInSpace || state.canDeliverHome
                    item(key = "delivery") {
                        Box(Modifier.padding(horizontal = 16.dp).padding(top = 4.dp)) {
                            if (choosesDelivery) {
                                DeliverySummaryRow(
                                    state = state,
                                    readyInMinutes = readyInMinutes,
                                    onChange = {
                                        haptics.click()
                                        deliverySheetOpen = true
                                    },
                                )
                            } else {
                                PickupLine(readyInMinutes = readyInMinutes)
                            }
                        }
                    }
                    item(key = "lines-head") {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 18.dp, end = 16.dp, top = 16.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Lo que pediste",
                                color = colors.ink2,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.weight(1f),
                            )
                            AddMorePill(onClick = onMenu)
                        }
                    }
                    item(key = "lines") {
                        Box(Modifier.padding(horizontal = 16.dp)) {
                            CartItemsPanel(
                                lines = state.cartLines,
                                lineTotal = state::cartLineTotal,
                                // El pedido ya salió con estas líneas: no se editan hasta que responda (COMPRA-04).
                                editable = !state.creatingOrder,
                                unavailableKeys = state.unavailableCartKeys,
                                onQuantityChange = { lineKey, delta ->
                                    haptics.click()
                                    onQuantityChange(lineKey, delta)
                                },
                            )
                        }
                    }
                    if (state.guestEntryRequired) {
                        item(key = "guest") {
                            Box(Modifier.padding(horizontal = 20.dp).padding(top = 16.dp)) {
                                GuestNameField(
                                    value = state.guestName,
                                    onValueChange = onGuestNameChange,
                                )
                            }
                        }
                    }
                    item(key = "notes") {
                        Box(Modifier.padding(horizontal = 16.dp).padding(top = 10.dp)) {
                            if (notesOpen || state.kitchenNotes.isNotBlank()) {
                                CartNotesField(
                                    value = state.kitchenNotes,
                                    onValueChange = onNotesChange,
                                    focusRequester = notesFocus,
                                )
                            } else {
                                NotesCollapsedRow {
                                    notesOpen = true
                                    scope.launch {
                                        // Espera a que el campo exista antes de darle el foco.
                                        kotlinx.coroutines.yield()
                                        runCatching { notesFocus.requestFocus() }
                                    }
                                }
                            }
                        }
                    }
                    item(key = "total") {
                        Column(Modifier.padding(horizontal = 18.dp).padding(top = 14.dp)) {
                            state.homeDeliveryFee?.let { fee ->
                                Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                                    Text(
                                        "Envío",
                                        color = colors.muted,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        if (Money.parse(fee).signum() == 0) "Gratis" else moneyLabel(fee),
                                        color = colors.ink2,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                                Text(
                                    "Total · ${cartCountLabel(state.cartItemCount)}",
                                    color = colors.muted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    moneyLabel(state.checkoutTotal),
                                    color = colors.ink,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                )
                            }
                            // Con el cashback prendido, cuánto deja este pedido, junto al total; apagado o sin cuenta
                            // no se menciona (24).
                            state.cashbackLine().takeUnless { guestCheckout }?.let { cashback ->
                                Text(
                                    cashback,
                                    color = colors.ink2,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                            // El alumno con límite diario ve cuánto le queda hoy antes de pagar.
                            dailyLimitLine(
                                LocalChildControls.current,
                                state.checkoutTotal.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                            )?.let {
                                Text(
                                    it,
                                    color = colors.muted,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                        }
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
                        item(key = "blocker") {
                            Box(Modifier.padding(horizontal = 20.dp).padding(top = 12.dp)) {
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
                    }
                    state.checkoutScheduleProblem?.takeIf { it != state.createOrderError }?.let { problem ->
                        item(key = "schedule") {
                            Box(Modifier.padding(horizontal = 20.dp).padding(top = 12.dp)) {
                                WarningBanner(message = problem)
                            }
                        }
                    }
                    state.cartNotice?.let { notice ->
                        item(key = "notice") {
                            Box(Modifier.padding(horizontal = 20.dp).padding(top = 12.dp)) {
                                WarningBanner(message = notice)
                            }
                        }
                    }
                    state.createOrderError?.let { error ->
                        item(key = "error") {
                            Box(Modifier.padding(horizontal = 20.dp).padding(top = 12.dp)) {
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
                            price = moneyLabel(state.checkoutTotal),
                            enabled = canConfirm,
                            loading = state.creatingOrder || preparingCheckout,
                            onClick = {
                                haptics.impact()
                                // A domicilio la forma de pago ya se eligió en el carrito.
                                if (state.isHomeDelivery) {
                                    onConfirm()
                                } else {
                                    paymentDialogOpen = true
                                    onCheckoutOpened()
                                }
                            },
                        )
                    }
                }
            }

            if (deliverySheetOpen && hasLines) {
                DeliverySheet(onDone = { deliverySheetOpen = false }) {
                    if (state.canDeliverHome && !canChooseInSpace) {
                        CartSectionHead(title = "Entrega", meta = "Elige cómo recibirlo")
                        HomeDestinationPicker(state.checkoutDestination, home.onDestinationChange)
                    } else if (canChooseInSpace) {
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
                            spaceDelivery = state.spaceDelivery,
                        )
                    }
                    if (state.checkoutDestination == OrderDestination.IN_SPACE && checkoutSpaces.isNotEmpty()) {
                        Box(Modifier.padding(top = 14.dp)) {
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
                    if (state.asksSpaceDelivery) {
                        Column(Modifier.padding(top = 16.dp)) {
                            CartSectionHead(title = "¿Cómo te llega?", meta = "Tú eliges")
                            CheckoutSpaceDeliveryPicker(
                                pickUpAtBar = state.checkoutPickUpAtBar,
                                onSelect = {
                                    haptics.selection()
                                    onPickUpAtBarChange(it)
                                },
                            )
                        }
                    }
                    if (state.isHomeDelivery) {
                        Box(Modifier.padding(top = 16.dp)) { HomeDeliverySection(state, home) }
                    }
                    if (state.asksForDeliveryDate) {
                        Box(Modifier.padding(top = 16.dp)) { DeliveryDatePicker(state, home.onSelectDate) }
                    }
                }
            }

            if (paymentDialogOpen) {
                PaymentMethodOverlay(
                    totalFor = { method -> moneyLabel(state.cartTotalFor(method)) },
                    initialMethod = state.checkoutPayment,
                    initialPayAtEnd = state.checkoutPayAtEnd,
                    balanceAffordable = state.isBalancePaymentAffordable(walletBalance),
                    showBalance = !guestCheckout && state.canPayWithBalance,
                    walletBalance = walletBalance,
                    payAtEndAvailable = state.canPayAtEnd,
                    cardAvailable = state.canPayWithCard,
                    cashAvailable = state.canPayAtRegister,
                    // «Pasa por él» (elegido o porque el negocio solo entrega en barra): no se lleva al lugar.
                    toTable = state.checkoutDestination == OrderDestination.IN_SPACE && !state.pickUpAtBarForOrder,
                    spaceType = state.checkoutSpace?.type,
                    hotelRoom = state.checkoutSpace?.name?.takeIf { state.isHotelRoomOrder },
                    cashbackLine = state.cashbackLine().takeUnless { guestCheckout },
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
            state.takeAwayWithSeatPrompt?.let { message ->
                TakeAwayWithSeatDialog(
                    message = message,
                    onConfirm = onConfirmTakeAwayWithSeat,
                    onDismiss = onDismissTakeAwayWithSeat,
                )
            }
        }
        // Al bajar, la foto se va y la barra de estado queda sobre el papel.
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .graphicsLayer { alpha = if (heroGone) 1f else 0f }
                .background(colors.paper),
        )
    }
}

/** Fracción de la pantalla para el mostrador: con más de un producto baja a lo justo para ver a Vaini. */
internal fun cartSceneWindow(lineCount: Int): Float = if (lineCount > 1) 0.3f else 0.42f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentMethodOverlay(
    totalFor: (PaymentMethod) -> String,
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
    /** «Pago en caja» solo si el dueño no lo apagó (`permite_pago_en_caja`). */
    cashAvailable: Boolean = true,
    /** Comer en la mesa: nadie recoge en barra, se lo llevan. */
    toTable: Boolean = false,
    spaceType: String? = null,
    /** Nombre de la habitación cuando el pedido va a un cuarto de hotel: «pagar al final» pasa a ser el cargo a ella. */
    hotelRoom: String? = null,
    /** «Ganas $4.60 de cashback (1%)…»; `null` con el cashback apagado (24). */
    cashbackLine: String? = null,
) {
    val venue = LocalVenueContext.current
    val colors = LocalVaiinillaColors.current
    val preferPayAtEnd = initialPayAtEnd || hotelRoom != null
    // Arranca con el último método que usó, si hoy sigue disponible: pagar vuelve a ser un toque.
    val initialAllowed =
        when (initialMethod) {
            PaymentMethod.CASH -> cashAvailable
            PaymentMethod.BALANCE -> showBalance && balanceAffordable
            PaymentMethod.STRIPE -> cardAvailable
        }
    var payAtEndSelected by remember { mutableStateOf(preferPayAtEnd && payAtEndAvailable) }
    var selectedMethod by remember {
        mutableStateOf(if (!payAtEndSelected && initialAllowed) initialMethod else null)
    }
    var selectionLocked by remember { mutableStateOf(false) }
    var selectionTouched by remember { mutableStateOf(false) }
    // El estado operativo y el saldo pueden llegar con la hoja ya abierta: la preselección se recalcula mientras la
    // persona no haya elegido nada (COMPRA-17).
    LaunchedEffect(payAtEndAvailable, initialAllowed) {
        if (selectionTouched) return@LaunchedEffect
        payAtEndSelected = preferPayAtEnd && payAtEndAvailable
        selectedMethod = if (!payAtEndSelected && initialAllowed) initialMethod else null
    }
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
        scrimColor = Color.Black.copy(alpha = 0.38f),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
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
                            .minimumInteractiveComponentSize()
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
                        title = if (hotelRoom != null) "Cargar a la ${hotelRoom.replaceFirstChar { it.lowercase() }}" else "Pagar al final",
                        subtitle =
                            if (hotelRoom != null) "Se paga al salir" else "Pide lo que quieras y paga toda la cuenta al irte",
                        selected = payAtEndSelected,
                        enabled = !selectionLocked,
                        onClick = {
                            selectionTouched = true
                            payAtEndSelected = true
                            selectedMethod = null
                        },
                    )
                }
                // En un hotel no hay caja a la que ir: solo se ofrece si el hotel apagó el cargo y la tarjeta.
                if (cashAvailable && (hotelRoom == null || (!payAtEndAvailable && !cardAvailable))) {
                    PaymentMethodCardOption(
                        icon = Icons.Outlined.Payments,
                        title = "Pago en caja",
                        subtitle =
                            if (toTable) {
                                SpaceCopy.payAtCashierHint(
                                   venue,
                                    spaceType,
                                )
                            } else {
                                "Pagas en caja antes de recogerlo"
                            },
                        selected = selectedMethod == PaymentMethod.CASH,
                        enabled = !selectionLocked,
                        onClick = {
                            selectionTouched = true
                            payAtEndSelected = false
                            selectedMethod = PaymentMethod.CASH
                        },
                    )
                }
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
                            selectionTouched = true
                            payAtEndSelected = false
                            selectedMethod = PaymentMethod.BALANCE
                        },
                    )
                }
                if (cardAvailable) {
                    PaymentMethodCardOption(
                        icon = Icons.Outlined.CreditCard,
                        title = if (hotelRoom != null) "Tarjeta ahora" else "Pago con Stripe",
                        subtitle = if (hotelRoom != null) "Pago seguro con Stripe" else "Débito o crédito, aquí mismo",
                        selected = selectedMethod == PaymentMethod.STRIPE,
                        enabled = !selectionLocked,
                        onClick = {
                            selectionTouched = true
                            payAtEndSelected = false
                            selectedMethod = PaymentMethod.STRIPE
                        },
                    )
                }
            }

            cashbackLine?.let {
                Text(it, color = colors.ink2, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
                        text =
                            paymentActionLabel(
                                selectedMethod,
                                payAtEndSelected,
                                totalFor(
                                    selectedMethod ?: initialMethod,
                                ),
                                toRoom = hotelRoom != null,
                            ),
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
    val borderColor = animateSelectionColor(if (selected) colors.accent else colors.line, "payment-card-border")
    val borderWidth by animateDpAsState(if (selected) 2.dp else 1.dp, label = "payment-card-border-width")
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(borderWidth, borderColor, RoundedCornerShape(18.dp))
                .physicalPress(enabled = enabled, onClick = onClick)
                .semantics {
                    role = Role.RadioButton
                    this.selected = selected
                },
        color = colors.paper2,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint =
                    if (!enabled) {
                        colors.muted
                    } else if (selected) {
                        colors.accent
                    } else {
                        colors.ink
                    },
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (enabled) colors.ink else colors.muted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
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
            AnimatedVisibility(
                visible = selected,
                enter = scaleIn(spring(dampingRatio = 0.55f, stiffness = 600f)) + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

/** El botón dice lo que pasa al tocarlo: con efectivo se pide y se paga después en caja. */
internal fun paymentActionLabel(
    method: PaymentMethod?,
    payAtEnd: Boolean,
    total: String,
    toRoom: Boolean = false,
): String =
    when {
        payAtEnd && toRoom -> "Pedir · $total a mi habitación"
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
private fun CartHero(
    photoUrl: String?,
    height: Dp,
    venueName: String?,
    profileInitials: String,
    onBack: () -> Unit,
    onOpenAccount: () -> Unit,
    /** Con productos: cuántos van, en la pastilla de la tienda. */
    productCount: Int = 0,
    /** Con productos, «Vaciar» es el botón redondo de la derecha (CR3); null deja las iniciales. */
    onClear: (() -> Unit)? = null,
) {
    val colors = LocalVaiinillaColors.current
    val withLines = productCount > 0
    CremeHero(
        height = height,
        photoUrl = photoUrl,
        leading = {
            GlassCircleButton(
                icon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = "Volver",
                onClick = onBack,
                size = 44.dp,
                iconSize = 22.dp,
            )
        },
        trailing = {
            if (onClear != null) {
                GlassCircleButton(
                    icon = Icons.Outlined.DeleteOutline,
                    contentDescription = "Vaciar el carrito",
                    onClick = onClear,
                    size = 44.dp,
                    iconSize = 20.dp,
                )
            } else {
                ProfileInitialsButton(profileInitials, onClick = onOpenAccount)
            }
        },
    ) {
        Text(
            if (withLines) "REVISA Y PAGA" else "REVISA Y CONFIRMA",
            color = if (withLines && colors.isDark) colors.accent else colors.muted,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.2.sp,
        )
        WholeWordTitle(
            text = "Tu pedido",
            color = colors.ink,
            fontSize = 40.sp,
            lineHeight = 42.sp,
            letterSpacing = (-1.8).sp,
            maxLines = 2,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (!venueName.isNullOrBlank()) {
            CremeVenueChip(
                label = if (withLines) "$venueName · ${cartCountLabel(productCount)}" else venueName,
                initialOf = venueName,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

/** La foto de arriba con productos: más baja que la del carrito vacío para que todo quepa sin scroll (CR3). */
private val CART_HERO_HEIGHT = 268.dp

private fun cartCountLabel(count: Int): String = if (count == 1) "1 producto" else "$count productos"

/**
 * Entrega, lugar y cómo llega en un solo renglón con borde lima (CR3). «Cambiar» abre la hoja con las opciones
 * (CR2), en lugar de las tarjetas, el selector de mesa y «¿Cómo te llega?» que alargaban el carrito.
 */
@Composable
private fun DeliverySummaryRow(
    state: OrderFlowUiState,
    readyInMinutes: Int?,
    onChange: () -> Unit,
) {
    val venue = LocalVenueContext.current
    val colors = LocalVaiinillaColors.current
    val title =
        when {
            state.isHomeDelivery -> "A domicilio"
            state.checkoutDestination == OrderDestination.IN_SPACE ->
                "${SpaceCopy.destinationTitle(venue, state.checkoutSpace?.type)} · ${state.selectedSpaceName}"
            else -> "Para llevar"
        }
    val how =
        when {
            state.isHomeDelivery -> "Te lo llevamos a tu casa"
            state.checkoutDestination == OrderDestination.IN_SPACE ->
                if (state.pickUpAtBarForOrder) "Pasas por él a la barra" else "Te lo llevamos"
            else -> "Recoges en barra"
        }
    val subtitle = listOfNotNull(how, readyInMinutes?.let { "listo en ~$it min" }).joinToString(" · ")
    val icon =
        when {
            state.isHomeDelivery -> Icons.Outlined.Home
            state.checkoutDestination == OrderDestination.IN_SPACE -> Icons.Outlined.Restaurant
            else -> Icons.Outlined.ShoppingBag
        }
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.paper2)
                .background(colors.accent.copy(alpha = 0.10f))
                .border(1.5.dp, colors.accent.copy(alpha = 0.45f), shape)
                .clickable(role = Role.Button, onClick = onChange)
                .semantics { contentDescription = "Entrega: $title, $subtitle. Cambiar" }
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(colors.accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.accentInk, modifier = Modifier.size(19.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = colors.ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                color = colors.muted,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text("Cambiar", color = colors.ink, fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/** La hoja de «Cambiar» (CR2): las mismas opciones de entrega de antes y «Listo» para cerrar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeliverySheet(
    onDone: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    ModalBottomSheet(
        onDismissRequest = onDone,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.paper,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
        ) {
            Text(
                "¿Cómo lo quieres?",
                color = colors.ink,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(bottom = 14.dp),
            )
            content()
            Box(
                Modifier
                    .padding(top = 20.dp)
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(27.dp))
                    .background(colors.ink)
                    .clickable(role = Role.Button, onClick = onDone),
                contentAlignment = Alignment.Center,
            ) {
                Text("Listo", color = colors.onInk, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

/** «+ Agregar más» junto a «Lo que pediste». */
@Composable
private fun AddMorePill(onClick: () -> Unit) {
    val colors = LocalVaiinillaColors.current
    Row(
        Modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(50))
            .border(1.5.dp, colors.line, RoundedCornerShape(50))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 9.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = colors.ink, modifier = Modifier.size(16.dp))
        Text("Agregar más", color = colors.ink, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/** La nota para cocina cerrada: un renglón punteado; tocarlo abre el campo. */
@Composable
private fun NotesCollapsedRow(onOpen: () -> Unit) {
    val colors = LocalVaiinillaColors.current
    val dash = colors.ink.copy(alpha = 0.22f)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(16.dp))
            .drawBehind {
                drawRoundRect(
                    color = dash,
                    cornerRadius =
                        androidx.compose.ui.geometry
                            .CornerRadius(16.dp.toPx()),
                    style =
                        androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect =
                                androidx.compose.ui.graphics.PathEffect
                                    .dashPathEffect(floatArrayOf(10f, 8f)),
                        ),
                )
            }.clickable(role = Role.Button, onClick = onOpen)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Outlined.Edit, contentDescription = null, tint = colors.muted, modifier = Modifier.size(18.dp))
        Text("Nota para cocina", color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        Text("· Opcional", color = colors.muted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun CartSectionHead(
    title: String,
    meta: String?,
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
        // Con peso: un título largo se corta con «…» en vez de sacar el dato de la derecha.
        Text(
            title,
            color = colors.ink,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (meta != null) {
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
    lineTotal: (CartLine) -> String,
    editable: Boolean,
    unavailableKeys: Set<String>,
    onQuantityChange: (lineKey: String, delta: Int) -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.paper2,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)) {
            lines.forEachIndexed { index, line ->
                CartLineRow(
                    line = line,
                    lineTotal = lineTotal,
                    editable = editable,
                    unavailable = line.key in unavailableKeys,
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
    lineTotal: (CartLine) -> String,
    editable: Boolean,
    unavailable: Boolean,
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
    // Renglón corto (CR3): miniatura, nombre y «$290 · 2 × $145»; sin la columna de precio grande de antes.
    val total = moneyLabel(lineTotal(line))
    val detail =
        listOfNotNull(
            if (line.quantity > 1) "${line.quantity} × ${moneyLabel(lineTotal(line.copy(quantity = 1)))}" else null,
            variants.takeIf { it.isNotBlank() },
        ).joinToString(" · ")
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ProductImage(
            imageUrl = line.product.imageUrl,
            contentDescription = line.product.name,
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp)),
            placeholder = { m -> ProductMonogram(line.product.name, m) },
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                line.product.name,
                color = colors.ink,
                fontSize = 15.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = colors.ink, fontWeight = FontWeight.ExtraBold)) { append(total) }
                    if (detail.isNotEmpty()) append(" · $detail")
                },
                color = colors.muted,
                fontSize = 12.5.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (unavailable) {
                Text(
                    "Ya no está disponible · quítalo para pagar",
                    color = colors.ink,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier =
                        Modifier
                            .padding(top = 4.dp)
                            .background(colors.coral.copy(alpha = 0.28f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            if (line.notes.isNotEmpty()) {
                Text(
                    "Nota: ${line.notes}",
                    color = colors.ink,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        QuantityStepper(
            quantity = line.quantity,
            enabled = editable,
            onMinus = onMinus,
            onPlus = onPlus,
        )
    }
}

@Composable
private fun QuantityStepper(
    quantity: Int,
    enabled: Boolean,
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
            modifier = Modifier.height(QUANTITY_BUTTON_SIZE),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuantityButton(
                icon = Icons.Rounded.Remove,
                description = "Quitar uno",
                tint = foreground,
                enabled = enabled,
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
                    modifier = Modifier.width(24.dp),
                )
            }
            QuantityButton(
                icon = Icons.Rounded.Add,
                description = "Agregar uno",
                tint = foreground,
                enabled = enabled,
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
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(QUANTITY_BUTTON_SIZE)
                .clip(CircleShape)
                .physicalPress(enabled = enabled, scale = PhysicalPressScale.Small, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (enabled) tint else tint.copy(alpha = 0.38f),
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Área de toque mínima de Quitar y Agregar (BOT-04). */
private val QUANTITY_BUTTON_SIZE = 48.dp

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
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
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
    focusRequester: FocusRequester? = null,
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
                .border(1.dp, colors.line, RoundedCornerShape(22.dp))
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
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
    recentOrders: List<OrderDetail>,
    onSelectOrder: (String) -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp).padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "¿Qué se te antoja?",
                color = colors.ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                "Pide algo del menú y aparece aquí.",
                color = colors.muted,
                fontSize = 15.sp,
            )
        }
        if (recentOrders.isNotEmpty()) {
            CremeDivider(Modifier.padding(top = 24.dp))
            Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 20.dp)) {
                Text(
                    "PEDIDOS ANTERIORES",
                    color = colors.muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.2.sp,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                recentOrders.forEach { order ->
                    RecentOrderRow(order = order, onClick = { onSelectOrder(order.summary.id) })
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
        order.items.joinToString(", ") { item -> "${item.quantity}× ${item.productName}" }
    val detail =
        listOfNotNull(itemsLabel.ifBlank { null }, cremeMoment(summary.createdAt)?.replaceFirstChar { it.lowercase() })
            .joinToString(" · ")
    Column(modifier = Modifier.fillMaxWidth().physicalPress(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(colors.paper2),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.ShoppingCart,
                    contentDescription = null,
                    tint = colors.ink,
                    modifier = Modifier.size(21.dp),
                )
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 13.dp)) {
                Text(
                    "#${summary.folio} · ${summary.state.label}",
                    color = colors.ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    detail,
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
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.padding(start = 4.dp).size(20.dp),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(start = 57.dp)
                .height(1.dp)
                .background(colors.line),
        )
    }
}

internal fun shouldShowCheckoutDock(
    hasCartItems: Boolean,
    imeVisible: Boolean,
): Boolean = hasCartItems && !imeVisible

/** «Para llevar» con lugar en una mesa o pista: se confirma antes de reenviar el pedido (R15). */
@Composable
private fun TakeAwayWithSeatDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.paper,
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("¿Para llevar?", color = colors.ink, fontWeight = FontWeight.Black, fontSize = 19.sp)
                Text(
                    message,
                    color = colors.muted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 10.dp, bottom = 18.dp),
                )
                EditorialAccentButton(text = "Sí, para llevar", onClick = onConfirm)
                EditorialSecondaryButton(
                    text = "Cambiar destino",
                    onClick = onDismiss,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
