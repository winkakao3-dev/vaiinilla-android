package com.vaiinilla.app.ui.screens

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.core.notifications.TableCallPushes
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderItem
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.PastOrderEntry
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.SpaceCopy
import com.vaiinilla.app.domain.model.StripePaymentStatus
import com.vaiinilla.app.domain.model.Venue
import com.vaiinilla.app.domain.model.activeItems
import com.vaiinilla.app.domain.model.groupOrdersBySession
import com.vaiinilla.app.domain.model.rejectedItemsHint
import com.vaiinilla.app.domain.model.skipsKitchen
import com.vaiinilla.app.domain.repository.CallReason
import com.vaiinilla.app.domain.repository.CallStatus
import com.vaiinilla.app.domain.repository.CallsUnavailableException
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.domain.repository.callWaiterSpaceId
import com.vaiinilla.app.domain.repository.openTableCallSpaceId
import com.vaiinilla.app.ui.components.ArrivalNotice
import com.vaiinilla.app.ui.components.EmptyState
import com.vaiinilla.app.ui.components.OrderDetailSummary
import com.vaiinilla.app.ui.components.OrderTrackingCard
import com.vaiinilla.app.ui.components.OrderTrackingTimeline
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.RentalStep
import com.vaiinilla.app.ui.components.SkeletonBlock
import com.vaiinilla.app.ui.components.StudentTab
import com.vaiinilla.app.ui.components.SwipeToDeleteOrder
import com.vaiinilla.app.ui.components.VaiinillaBottomNav
import com.vaiinilla.app.ui.components.VaiinillaBottomNavClearance
import com.vaiinilla.app.ui.components.VaiinillaQrCode
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.canAnnounceArrival
import com.vaiinilla.app.ui.components.cremeMoment
import com.vaiinilla.app.ui.components.destinationDisplayLabel
import com.vaiinilla.app.ui.components.isLiveRental
import com.vaiinilla.app.ui.components.isRental
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.needsPickupQr
import com.vaiinilla.app.ui.components.orderStateLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.components.rentalScheduleLabel
import com.vaiinilla.app.ui.components.rentalStateLabel
import com.vaiinilla.app.ui.components.rentalStep
import com.vaiinilla.app.ui.components.trackingStepDescription
import com.vaiinilla.app.ui.operational.OperationalUiState
import com.vaiinilla.app.ui.order.MenuWaiterCallState
import com.vaiinilla.app.ui.order.OrderFlowUiState
import com.vaiinilla.app.ui.order.callRetrySeconds
import com.vaiinilla.app.ui.order.canCancelMenuCall
import com.vaiinilla.app.ui.order.cancellationMoneyNote
import com.vaiinilla.app.ui.order.orderCancellationNotice
import com.vaiinilla.app.ui.order.spaceDelivery
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.SeaGlassButton
import com.vaiinilla.app.ui.theme.SeaGlassTone
import com.vaiinilla.app.ui.theme.VaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentTrackingScreen(
    state: OperationalUiState,
    orderState: OrderFlowUiState,
    onMenu: () -> Unit,
    onAssistant: () -> Unit,
    onWallet: () -> Unit,
    onCart: () -> Unit,
    onOpenCatalog: () -> Unit,
    onSelectOrder: (String) -> Unit,
    onBackFromSelectedOrder: () -> Unit = {},
    onDeleteOrder: (String) -> Unit = {},
    onViewReceipt: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onAnnounceArrival: (String) -> Unit = {},
    canCallWaiter: (OrderDetail) -> Boolean = { false },
    onCurrentWaiterCall: (suspend (Int) -> Result<TableCall?>)? = null,
    onCallWaiter: (suspend (Int, CallReason, String?) -> Result<TableCall>)? = null,
    onCancelWaiter: (suspend (TableCall) -> Result<TableCall>)? = null,
    /** La mesa compartida, arriba de la lista (vacía si el cliente no está en una mesa). */
    sharedTable: @Composable () -> Unit = {},
    /** Para «Mi código» en una renta: el QR personal con el que Caja cobra. */
    userId: String? = null,
) {
    val haptics = rememberVaiinillaHaptics()
    LaunchedEffect(Unit) {
        if (state.role != OperationalRole.CLIENT) {
            // Role is set by AppNavHost before navigation.
        }
    }

    val selected = state.selectedOrder
    var showAllPast by rememberSaveable { mutableStateOf(false) }
    val colors = LocalVaiinillaColors.current
    val reduceMotion = reducedMotion()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    // Preview del back predictivo en el detalle: el contenido sigue al dedo
    // hacia el borde de origen y se atenua; al soltar vuelve a la lista.
    var detailBackProgress by remember { mutableFloatStateOf(0f) }
    var detailBackSign by remember { mutableFloatStateOf(1f) }
    var detailBackSettleJob by remember { mutableStateOf<Job?>(null) }
    val detailBackShiftPx = with(density) { 120.dp.toPx() }
    PredictiveBackHandler(enabled = selected != null) { events ->
        detailBackSettleJob?.cancel()
        try {
            events.collect { event ->
                detailBackSign = if (event.swipeEdge == BackEventCompat.EDGE_LEFT) 1f else -1f
                detailBackProgress = if (reduceMotion) 0f else event.progress
            }
            detailBackProgress = 0f
            onBackFromSelectedOrder()
        } catch (e: CancellationException) {
            detailBackSettleJob =
                scope.launch {
                    Animatable(detailBackProgress).animateTo(
                        targetValue = 0f,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 600f),
                    ) { detailBackProgress = value }
                }
            throw e
        }
    }
    val listState = rememberLazyListState()
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.paper),
    ) {
        PullToRefreshBox(
            isRefreshing = false,
            onRefresh = {
                haptics.impact()
                onRefresh()
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            AnimatedContent(
                targetState = selected?.summary?.id,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .offset {
                            IntOffset(
                                (detailBackShiftPx * detailBackSign * detailBackProgress).roundToInt(),
                                0,
                            )
                        }.graphicsLayer {
                            alpha = 1f - detailBackProgress * 0.12f
                        },
                transitionSpec = {
                    if (reduceMotion) {
                        fadeIn(animationSpec = tween(0)) togetherWith fadeOut(animationSpec = tween(0))
                    } else if (targetState != null) {
                        (
                            slideInVertically(
                                animationSpec = spring(dampingRatio = 0.82f, stiffness = 430f),
                                initialOffsetY = { height -> height / 12 },
                            ) +
                                fadeIn(animationSpec = tween(durationMillis = 190)) +
                                scaleIn(
                                    initialScale = 0.985f,
                                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 520f),
                                )
                        ) togetherWith
                            (
                                slideOutVertically(
                                    animationSpec = tween(durationMillis = 150),
                                    targetOffsetY = { height -> -height / 22 },
                                ) +
                                    fadeOut(animationSpec = tween(durationMillis = 125)) +
                                    scaleOut(targetScale = 0.992f, animationSpec = tween(durationMillis = 150))
                            )
                    } else {
                        (
                            slideInVertically(
                                animationSpec = spring(dampingRatio = 0.84f, stiffness = 440f),
                                initialOffsetY = { height -> -height / 18 },
                            ) +
                                fadeIn(animationSpec = tween(durationMillis = 190)) +
                                scaleIn(
                                    initialScale = 0.992f,
                                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 520f),
                                )
                        ) togetherWith
                            (
                                slideOutVertically(
                                    animationSpec = tween(durationMillis = 145),
                                    targetOffsetY = { height -> height / 14 },
                                ) +
                                    fadeOut(animationSpec = tween(durationMillis = 120)) +
                                    scaleOut(targetScale = 0.985f, animationSpec = tween(durationMillis = 145))
                            )
                    }
                },
                label = "orders-overview-detail",
            ) { animatedSelectedId ->
                val animatedSelected =
                    animatedSelectedId?.let { id ->
                        state.orders.firstOrNull { it.summary.id == id }
                            ?: selected?.takeIf { it.summary.id == id }
                    }
                val inOverview = animatedSelectedId == null
                val detailListState = rememberLazyListState()
                // Una renta abierta es su propia página, con la cancha en grande (Doop L09).
                if (animatedSelected?.isRental == true) {
                    RentalCourtPage(
                        order = animatedSelected,
                        catalog = orderState.catalog,
                        userId = userId,
                        onOpenMenu = onOpenCatalog,
                        onViewRental = onViewReceipt,
                        onCurrentWaiterCall = onCurrentWaiterCall,
                        onCallWaiter = onCallWaiter,
                        onCancelWaiter = onCancelWaiter,
                        spaceDelivery = orderState.spaceDelivery,
                    )
                } else {
                    LazyColumn(
                        state = if (inOverview) listState else detailListState,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .statusBarsPadding(),
                        contentPadding =
                            PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = VaiinillaBottomNavClearance + 48.dp,
                            ),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        item {
                            val deliveryFirst =
                                animatedSelected == null &&
                                    state.orders.firstOrNull { it.isActiveForCustomer }?.homeDelivery != null
                            Column(modifier = Modifier.padding(start = 4.dp)) {
                                if (deliveryFirst) OrdersSectionLabel("EN CURSO")
                                Text(
                                    if (deliveryFirst) "Mis pedidos" else "Pedidos",
                                    color = colors.ink,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 30.sp,
                                    letterSpacing = (-1.2).sp,
                                )
                            }
                        }
                        // Con pedidos ya cargados, un error de red o de "Ya llegué" no se veía en ningún lado (PEDIDO-05).
                        trackingNoticeFor(state)?.let { notice ->
                            item(key = "tracking-notice") { TrackingNotice(notice) }
                        }
                        // Sin pedidos la mesa va arriba; con pedidos, debajo del que está en curso (Doop L10).
                        if (animatedSelected == null && state.orders.isEmpty()) {
                            item(key = "shared-table") { sharedTable() }
                        }

                        when {
                            // Antes que el esqueleto: si la carga sigue fallando, el sondeo no lo hace parpadear.
                            state.orders.isEmpty() && state.lastRefreshOk == false -> {
                                item(key = "orders-error") {
                                    EmptyState(
                                        icon = Icons.Outlined.ReceiptLong,
                                        title = "No pudimos cargar tus pedidos",
                                        message = "Revisa tu conexión e inténtalo otra vez.",
                                        modifier =
                                            Modifier.semantics(mergeDescendants = true) {
                                                liveRegion = LiveRegionMode.Polite
                                            },
                                        actionLabel = "Reintentar",
                                        onAction = onRefresh,
                                    )
                                }
                            }
                            state.orders.isEmpty() && state.loading -> {
                                items(3) { OrderTrackingSkeleton() }
                            }
                            state.orders.isEmpty() -> {
                                item {
                                    EmptyState(
                                        icon = Icons.Outlined.ReceiptLong,
                                        title = "Sin pedidos activos",
                                        message = "Cuando confirmes uno aparecerá aquí.",
                                        actionLabel = "Pedir algo",
                                        onAction = onOpenCatalog,
                                    )
                                }
                            }
                            animatedSelected != null -> {
                                item(key = "selected-${animatedSelected.summary.id}") {
                                    SwipeToDeleteOrder(
                                        orderFolio = animatedSelected.summary.folio.toString(),
                                        onDelete = { onDeleteOrder(animatedSelected.summary.id) },
                                        enabled = !animatedSelected.isActiveForCustomer,
                                    ) {
                                        OrderTrackingCard(
                                            order = animatedSelected,
                                            showEyebrow = true,
                                            leadingImageUrl =
                                                itemImageUrls(animatedSelected, orderState.catalog)
                                                    .firstOrNull { it != null },
                                        )
                                    }
                                }
                                if (animatedSelected.canAnnounceArrival(
                                        orderState.operationalStatus?.isDriveThru == true,
                                    )
                                ) {
                                    item {
                                        ArrivalNotice(
                                            order = animatedSelected,
                                            enabled = !state.acting,
                                            onAnnounce = { onAnnounceArrival(animatedSelected.summary.id) },
                                        )
                                    }
                                }
                                item {
                                    TrackingSectionHead()
                                }
                                if (animatedSelected.summary.state.isTerminalWithoutDelivery) {
                                    // Cinco pasos pendientes de un pedido que ya no avanza confundían (PEDIDO-07).
                                    if (animatedSelected.summary.state != OrderState.CANCELED) {
                                        item { ClosedOrderCard(animatedSelected) }
                                    }
                                } else {
                                    item {
                                        OrderTrackingTimeline(
                                            current = animatedSelected.summary.state,
                                            destination = animatedSelected.summary.destination,
                                            paymentMethod = animatedSelected.summary.paymentMethod,
                                            paymentStatus = animatedSelected.payment?.status,
                                            spaceType = animatedSelected.summary.space?.type,
                                            payAtEnd = animatedSelected.summary.payAtEnd,
                                            skipsKitchen = animatedSelected.skipsKitchen,
                                            pickUpAtBar = animatedSelected.summary.pickUpAtBar,
                                        )
                                    }
                                }
                                if (animatedSelected.summary.state == OrderState.CANCELED) {
                                    item { CancelReasonCard(animatedSelected) }
                                }
                                if (animatedSelected.homeDelivery != null) {
                                    item { HomeDeliveryOrderCard(animatedSelected) }
                                }
                                if (
                                    animatedSelected.summary.state == OrderState.READY &&
                                    animatedSelected.needsPickupQr(
                                        orderState.operationalStatus?.deliveryRequiresQr != false,
                                    )
                                ) {
                                    item { PickupCodeCard(animatedSelected) }
                                }
                                item {
                                    Text("Resumen", color = colors.ink, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                }
                                item {
                                    OrderDetailSummary(order = animatedSelected)
                                }
                                item {
                                    Button(
                                        onClick = onViewReceipt,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape =
                                            androidx.compose.foundation.shape
                                                .RoundedCornerShape(18.dp),
                                        colors =
                                            ButtonDefaults.buttonColors(
                                                containerColor = colors.paper2,
                                                contentColor = colors.ink,
                                            ),
                                    ) {
                                        Text("Ver recibo", fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                            else -> {
                                val activeOrders = state.orders.filter { it.isActiveForCustomer }
                                val pastOrders = state.orders.filterNot { it.isActiveForCustomer }
                                // Ya entregado, el pedido baja a ANTERIORES pero la mesa sigue: ahí se pide la cuenta.
                                val tableCallOrder =
                                    if (activeOrders.any(canCallWaiter)) {
                                        null
                                    } else {
                                        pastOrders.firstOrNull { openTableCallSpaceId(it) != null }
                                    }
                                val tableCallSpaceId = tableCallOrder?.let { openTableCallSpaceId(it) }
                                if (tableCallOrder != null &&
                                    tableCallSpaceId != null &&
                                    onCurrentWaiterCall != null &&
                                    onCallWaiter != null &&
                                    onCancelWaiter != null
                                ) {
                                    item(key = "table-call-${tableCallOrder.summary.id}") {
                                        TableCallCard(
                                            order = tableCallOrder,
                                            spaceId = tableCallSpaceId,
                                            onCurrent = onCurrentWaiterCall,
                                            onCall = onCallWaiter,
                                            onCancel = onCancelWaiter,
                                        )
                                    }
                                }
                                if (activeOrders.isNotEmpty()) {
                                    items(activeOrders, key = { it.summary.id }) { order ->
                                        SwipeToDeleteOrder(
                                            orderFolio = order.summary.folio.toString(),
                                            onDelete = { onDeleteOrder(order.summary.id) },
                                            enabled = !order.isActiveForCustomer,
                                        ) {
                                            if (order.homeDelivery != null) {
                                                DeliveryTrackingCard(
                                                    order = order,
                                                    onOpenFull = { onSelectOrder(order.summary.id) },
                                                )
                                            } else {
                                                ActiveOrderCard(
                                                    order = order,
                                                    onOpenFull = { onSelectOrder(order.summary.id) },
                                                    showArrival =
                                                        order.canAnnounceArrival(
                                                            orderState.operationalStatus?.isDriveThru == true,
                                                        ),
                                                    arrivalEnabled = !state.acting,
                                                    onAnnounceArrival = { onAnnounceArrival(order.summary.id) },
                                                    deliveryRequiresQr =
                                                        orderState.operationalStatus?.deliveryRequiresQr != false,
                                                    canCallWaiter = canCallWaiter,
                                                    onCurrentWaiterCall = onCurrentWaiterCall,
                                                    onCallWaiter = onCallWaiter,
                                                    onCancelWaiter = onCancelWaiter,
                                                )
                                            }
                                        }
                                    }
                                }
                                item(key = "shared-table") { sharedTable() }
                                if (pastOrders.isNotEmpty()) {
                                    val pastEntries = groupOrdersBySession(pastOrders)
                                    val venueName = orderState.guestVenue?.establishment?.name
                                    val pastRow: @Composable (OrderDetail) -> Unit = { order ->
                                        SwipeToDeleteOrder(
                                            orderFolio = order.summary.folio.toString(),
                                            onDelete = { onDeleteOrder(order.summary.id) },
                                        ) {
                                            PastOrderRow(
                                                order = order,
                                                venueName = venueName,
                                                onClick = { onSelectOrder(order.summary.id) },
                                            )
                                        }
                                    }
                                    item(key = "section-past") { OrdersSectionLabel("ANTERIORES") }
                                    item(key = "section-past-group") {
                                        PastOrdersGroup(
                                            entries = if (showAllPast) pastEntries else pastEntries.take(3),
                                            row = pastRow,
                                        )
                                    }
                                    if (pastEntries.size > 3) {
                                        item(key = "section-past-more") {
                                            ShowMorePastOrdersButton(
                                                hiddenCount = pastEntries.size - 3,
                                                expanded = showAllPast,
                                                onClick = {
                                                    haptics.impact()
                                                    showAllPast = !showAllPast
                                                },
                                            )
                                        }
                                    }
                                }
                                // El juego es para la espera de la comida: no aparece con un pago por
                                // confirmar, una renta o un pedido que ya está listo para recoger.
                                if (activeOrders.firstOrNull()?.isWaitingForFood == true) {
                                    item(key = "waiting-arcade") { WaitingArcadeCard() }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val ORDERS_SCENE_WINDOW = 0.4f

/** Mis pedidos mientras la cuenta todavía no está lista en este lugar: carga, error o entrar, nunca en blanco. */
@Composable
fun OrdersAccessGate(
    loading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onSignIn: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val gate =
        when {
            loading -> OrdersGate.Loading
            errorMessage != null -> OrdersGate.Error
            else -> OrdersGate.SignIn
        }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.paper)
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Mis pedidos", color = colors.ink, fontWeight = FontWeight.Black, fontSize = 28.sp)
        AnimatedContent(
            targetState = gate,
            transitionSpec = {
                (
                    fadeIn(
                        spring(stiffness = 500f),
                    ) + slideInVertically(spring(dampingRatio = 0.85f)) { it / 16 }
                ) togetherWith
                    fadeOut(spring(stiffness = 700f))
            },
            label = "orders-access-gate",
        ) { current ->
            when (current) {
                OrdersGate.Loading ->
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        repeat(3) { OrderTrackingSkeleton() }
                    }
                OrdersGate.Error ->
                    EmptyState(
                        icon = Icons.Outlined.ReceiptLong,
                        title = "No pudimos abrir tus pedidos",
                        // El detalle técnico del error no le sirve a quien pide: se dice qué hacer.
                        message = "Revisa tu conexión e inténtalo otra vez.",
                        actionLabel = "Reintentar",
                        onAction = onRetry,
                    )
                OrdersGate.SignIn ->
                    EmptyState(
                        icon = Icons.Outlined.ReceiptLong,
                        title = "Entra para ver tus pedidos",
                        message = "Tus pedidos de este lugar se guardan en tu cuenta.",
                        actionLabel = "Entrar",
                        onAction = onSignIn,
                    )
            }
        }
    }
}

private enum class OrdersGate { Loading, Error, SignIn }

@Composable
private fun OrderTrackingSkeleton() {
    val colors = LocalVaiinillaColors.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.paper2,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SkeletonBlock(modifier = Modifier.size(width = 64.dp, height = 22.dp), corner = 11.dp)
                SkeletonBlock(modifier = Modifier.size(width = 74.dp, height = 22.dp), corner = 11.dp)
            }
            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.7f).height(18.dp), corner = 9.dp)
            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.45f).height(14.dp), corner = 7.dp)
            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.55f).height(44.dp), corner = 14.dp)
        }
    }
}

/** Qué pasó con un pedido que expiró o no se recogió, y a quién preguntar. */
@Composable
private fun ClosedOrderCard(order: OrderDetail) {
    val colors = LocalVaiinillaColors.current
    val notPickedUp = order.summary.state == OrderState.NOT_PICKED_UP
    Surface(
        modifier = Modifier.fillMaxWidth().arrive(0, key = order.summary.id),
        color = colors.paper2,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                if (notPickedUp) "No se recogió a tiempo" else "Este pedido expiró",
                color = colors.coral,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
            )
            Text(
                "Pasó el tiempo sin que se cobrara o se recogiera. Si ya pagaste algo, pregunta en el negocio.",
                color = colors.ink,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/** Aviso sobre la lista de pedidos: último cambio que falló o datos que no se pudieron actualizar. */
internal fun trackingNoticeFor(state: OperationalUiState): String? =
    state.errorMessage?.takeIf { state.orders.isNotEmpty() }
        ?: "Sin conexión. Mostramos lo último que vimos; desliza hacia abajo para actualizar."
            .takeIf { state.orders.isNotEmpty() && state.lastRefreshOk == false }

@Composable
private fun TrackingNotice(message: String) {
    val colors = LocalVaiinillaColors.current
    Surface(
        color = colors.coral.copy(alpha = 0.22f),
        shape = RoundedCornerShape(16.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(message, color = colors.ink, fontSize = 14.sp, modifier = Modifier.padding(14.dp))
    }
}

/** Quién canceló el pedido y por qué: Cocina, Caja o el propio cliente. */
@Composable
private fun CancelReasonCard(order: OrderDetail) {
    val colors = LocalVaiinillaColors.current
    val by =
        when (order.summary.canceledByRole) {
            "cocina" -> "Cocina"
            "cajero" -> "Caja"
            "admin" -> "El establecimiento"
            "cliente" -> "Tú"
            else -> null
        }
    val reason = order.summary.cancelReason?.takeIf { it.isNotBlank() }
    Surface(
        modifier = Modifier.fillMaxWidth().arrive(0, key = order.summary.id),
        color = colors.paper2,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                if (by != null && by != "Tú") "$by canceló tu pedido" else "Pedido cancelado",
                color = colors.coral,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
            )
            if (reason != null) {
                Text(
                    reason,
                    color = colors.ink,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            cancellationMoneyNote(order)?.let { note ->
                Text(
                    note,
                    color = colors.muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun PickupCodeCard(order: OrderDetail) {
    val colors = LocalVaiinillaColors.current
    val token = order.pickupToken?.takeIf { it.isNotBlank() }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.paper2,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Código de recogida",
                color = colors.ink,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (order.summary.destination == OrderDestination.IN_SPACE) {
                    "Muéstrale este QR al personal para confirmar la entrega."
                } else {
                    "Muéstralo en Caja cuando recojas tu pedido."
                },
                color = colors.muted,
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(16.dp))
            if (token != null) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Box(Modifier.padding(14.dp), contentAlignment = Alignment.Center) {
                        VaiinillaQrCode(value = token, qrSize = 190.dp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    token,
                    color = colors.ink,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    "Estamos trayendo tu código. Si no aparece, dile al personal tu folio ${order.summary.folio}.",
                    color = colors.coral,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Preview(name = "Pedidos y tracking", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun StudentTrackingScreenPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        StudentTrackingScreen(
            state = OperationalUiState(role = OperationalRole.CLIENT),
            orderState = OrderFlowUiState(),
            onMenu = {},
            onAssistant = {},
            onWallet = {},
            onCart = {},
            onOpenCatalog = {},
            onSelectOrder = {},
        )
    }
}

@Preview(name = "Pedidos · por cobrar", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun StudentTrackingPendingPaymentPreview() {
    val pendingOrder = pendingPaymentPreviewOrder()
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        Box(modifier = Modifier.fillMaxSize()) {
            StudentTrackingScreen(
                state =
                    OperationalUiState(
                        role = OperationalRole.CLIENT,
                        orders = listOf(pendingOrder),
                        selectedOrderId = pendingOrder.summary.id,
                    ),
                orderState = OrderFlowUiState(loading = false),
                onMenu = {},
                onAssistant = {},
                onWallet = {},
                onCart = {},
                onOpenCatalog = {},
                onSelectOrder = {},
            )
            VaiinillaBottomNav(
                activeTab = StudentTab.ORDERS,
                cartCount = 2,
                onTabSelected = {},
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

private fun pendingPaymentPreviewOrder(): OrderDetail =
    OrderDetail(
        summary =
            OrderSummary(
                id = "preview-pending-payment",
                folio = 3472,
                operationalDate = "2026-07-20",
                state = OrderState.PENDING_PAYMENT,
                paymentMethod = PaymentMethod.CASH,
                destination = OrderDestination.TAKE_AWAY,
                space = null,
                subtotal = "101.00",
                combinedSavings = "0.00",
                cashbackAwarded = "0.00",
                total = "101.00",
                version = 1,
                createdAt = "2026-07-20T15:05:00.000Z",
                updatedAt = "2026-07-20T15:05:00.000Z",
            ),
        user = null,
        kitchenNotes = "Salsa aparte",
        items =
            listOf(
                OrderItem(
                    id = 501,
                    productId = 2,
                    productName = "Burrito norteño",
                    preparationStation = PreparationStation.KITCHEN,
                    quantity = 1,
                    unitDigitalPrice = "76.00",
                    subtotal = "76.00",
                    options = emptyList(),
                ),
                OrderItem(
                    id = 502,
                    productId = 3,
                    productName = "Agua de jamaica",
                    preparationStation = PreparationStation.KITCHEN,
                    quantity = 1,
                    unitDigitalPrice = "25.00",
                    subtotal = "25.00",
                    options = emptyList(),
                ),
            ),
    )

@Composable
private fun TrackingSectionHead() {
    val colors = LocalVaiinillaColors.current
    Text("Seguimiento", color = colors.ink, fontWeight = FontWeight.Black, fontSize = 18.sp)
}

/** Una renta queda "entregada" al pagarse, pero sigue en curso hasta que termina su horario. */
private val OrderDetail.isActiveForCustomer: Boolean
    get() = if (isRental) isLiveRental() else !summary.state.isClosed

private val OrderDetail.isWaitingForFood: Boolean
    get() = !isRental && summary.state in setOf(OrderState.PAID, OrderState.PREPARING)

private fun customerStateLabel(order: OrderDetail): String =
    if (order.isRental) rentalStateLabel(order) else orderStateLabel(order.summary)

/** En ANTERIORES, un cancelado dice a dónde regresó el dinero, como «Cancelado · a tu saldo» (Doop L10). */
internal fun pastOrderStateLabel(order: OrderDetail): String {
    val base = customerStateLabel(order)
    if (order.summary.state != OrderState.CANCELED || order.summary.payAtEnd) return base
    val destination =
        when (order.summary.paymentMethod) {
            PaymentMethod.BALANCE -> "a tu saldo"
            PaymentMethod.STRIPE ->
                when (order.payment?.status) {
                    StripePaymentStatus.REFUND_PENDING,
                    StripePaymentStatus.REFUNDING,
                    StripePaymentStatus.REFUNDED,
                    -> "a tu tarjeta"
                    else -> null
                }
            else -> null
        }
    return destination?.let { "$base · $it" } ?: base
}

/** «Pan francés y 2 Capuchino · te lo llevan a la mesa», lo que va en la tarjeta de AHORA (Doop L10). */
private fun activeOrderLine(order: OrderDetail): String {
    val summary = order.summary
    val names = order.activeItems.map { if (it.quantity > 1) "${it.quantity} ${it.productName}" else it.productName }
    val what =
        when (names.size) {
            0 -> null
            1 -> names.first()
            else -> names.dropLast(1).joinToString(", ") + " y " + names.last()
        }
    val step =
        if (order.reservation != null) {
            order.reservation?.let { rentalScheduleLabel(it) }
        } else if (summary.pickUpAtBar && !summary.state.isTerminalWithoutDelivery) {
            "pasa por él a la barra"
        } else if (summary.space?.type == "mesa" && !summary.state.isTerminalWithoutDelivery) {
            "te lo llevan a la mesa"
        } else if (summary.space?.type == "cancha" && !summary.state.isTerminalWithoutDelivery) {
            "te lo llevan a la ${Venue.court}"
        } else {
            trackingStepDescription(
                summary.state,
                summary.destination,
                summary.paymentMethod,
                order.payment?.status,
                summary.space?.type,
                summary.payAtEnd,
                summary.pickUpAtBar,
            ).trimEnd('.').replaceFirstChar { it.lowercase() }
        }
    return listOfNotNull(what, step?.takeIf { it.isNotBlank() }).joinToString(" · ")
}

/** Los pasos del pedido como segmentos con su nombre abajo; el actual va a medio llenar (Doop L10). */
@Composable
private fun SegmentedProgress(
    currentIndex: Int,
    labels: List<String>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val reduceMotion = reducedMotion()
    Row(
        modifier =
            modifier.fillMaxWidth().semantics(mergeDescendants = true) {
                contentDescription =
                    labels.getOrNull(currentIndex)?.let { "Paso ${currentIndex + 1} de ${labels.size}: $it" }.orEmpty()
            },
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val fill by animateFloatAsState(
                targetValue =
                    when {
                        index < currentIndex -> 1f
                        index == currentIndex -> 0.45f
                        else -> 0f
                    },
                animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.9f, stiffness = 260f),
                label = "segment-fill",
            )
            val current = index == currentIndex
            Column(Modifier.weight(1f)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(fill)
                            .height(6.dp)
                            .background(colors.accent),
                    )
                }
                Text(
                    label,
                    color = if (current) OrderTrackingCardText else OrderTrackingCardText.copy(alpha = 0.6f),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign =
                        when (index) {
                            0 -> TextAlign.Start
                            labels.lastIndex -> TextAlign.End
                            else -> TextAlign.Center
                        },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
            }
        }
    }
}

internal val OrderTrackingCardBg = Color(0xFF292A27)
internal val OrderTrackingCardText = Color(0xFFF5F2E8)

@Composable
private fun OrdersSectionLabel(text: String) {
    Text(
        text,
        color = LocalVaiinillaColors.current.muted,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 2.2.sp,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
    )
}

@Composable
private fun ActiveOrderCard(
    order: OrderDetail,
    onOpenFull: () -> Unit,
    showArrival: Boolean = false,
    arrivalEnabled: Boolean = true,
    onAnnounceArrival: () -> Unit = {},
    deliveryRequiresQr: Boolean = true,
    canCallWaiter: (OrderDetail) -> Boolean = { false },
    onCurrentWaiterCall: (suspend (Int) -> Result<TableCall?>)? = null,
    onCallWaiter: (suspend (Int, CallReason, String?) -> Result<TableCall>)? = null,
    onCancelWaiter: (suspend (TableCall) -> Result<TableCall>)? = null,
) {
    val colors = LocalVaiinillaColors.current
    val summary = order.summary
    val reduceMotion = reducedMotion()
    Surface(
        // Tocar la tarjeta abre el pedido; sus botones (mesero, seguimiento) siguen respondiendo aparte.
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onOpenFull),
        color = OrderTrackingCardBg,
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .clipToBounds()
                    .animateContentSize(
                        animationSpec =
                            if (reduceMotion) {
                                snap<IntSize>()
                            } else {
                                spring<IntSize>(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessLow,
                                )
                            },
                    ).padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "AHORA · ${destinationDisplayLabel(order).uppercase()}",
                    color = colors.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    "#${summary.folio}",
                    color = OrderTrackingCardText.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Text(
                if (summary.state == OrderState.PREPARING &&
                    !order.isRental
                ) {
                    "En cocina"
                } else {
                    customerStateLabel(order)
                },
                color = OrderTrackingCardText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.8).sp,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                activeOrderLine(order),
                color = OrderTrackingCardText.copy(alpha = 0.62f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            order.rejectedItemsHint?.let { hint ->
                // Rechazo por artículo: el cliente ve qué se quitó y por qué.
                Text(
                    hint,
                    color = LocalVaiinillaColors.current.coral,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp).arrive(0, key = hint),
                )
            }
            val rental = order.reservation
            val rentalNow = rentalStep(order)
            SegmentedProgress(
                currentIndex = if (rental != null) rentalProgressIndex(rentalNow) else foodProgressIndex(summary.state),
                labels = if (rental != null) RentalProgressSteps else foodProgressSteps(order),
                modifier = Modifier.padding(top = 12.dp),
            )
            if (summary.state == OrderState.READY && order.needsPickupQr(deliveryRequiresQr)) {
                InlinePickupQr(order = order)
            }
            if (showArrival) {
                ArrivalNotice(order = order, enabled = arrivalEnabled, onAnnounce = onAnnounceArrival, onDark = true)
            }
            val callSpaceId = if (canCallWaiter(order)) callWaiterSpaceId(order) else null
            if (callSpaceId != null && onCurrentWaiterCall != null && onCallWaiter != null && onCancelWaiter != null) {
                CallWaiterButton(
                    spaceId = callSpaceId,
                    orderId = order.summary.id,
                    onCurrent = onCurrentWaiterCall,
                    onCall = onCallWaiter,
                    onCancel = onCancelWaiter,
                )
            }
        }
    }
}

/** La mesa de un pedido ya entregado: el cliente sigue sentado y puede llamar al mesero o pedir la cuenta. */
@Composable
private fun TableCallCard(
    order: OrderDetail,
    spaceId: Int,
    onCurrent: suspend (Int) -> Result<TableCall?>,
    onCall: suspend (Int, CallReason, String?) -> Result<TableCall>,
    onCancel: suspend (TableCall) -> Result<TableCall>,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        color = OrderTrackingCardBg,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                order.summary.space
                    ?.name
                    ?.takeIf { it.isNotBlank() } ?: "Tu mesa",
                color = OrderTrackingCardText.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                "¿Necesitas algo más o la cuenta?",
                color = OrderTrackingCardText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 4.dp),
            )
            CallWaiterButton(
                spaceId = spaceId,
                orderId = order.summary.id,
                onCurrent = onCurrent,
                onCall = onCall,
                onCancel = onCancel,
            )
        }
    }
}

private enum class CallWaiterView { IDLE, REASONS, OPEN, UNAVAILABLE }

/**
 * "Llamar al mesero": un toque abre tres motivos y el botón se vuelve estado en vivo. Va dentro de la
 * tarjeta oscura del pedido; sobre vidrio claro se le pasa la tinta en [textColor].
 */
@Composable
internal fun CallWaiterButton(
    spaceId: Int,
    orderId: String,
    onCurrent: suspend (Int) -> Result<TableCall?>,
    onCall: suspend (Int, CallReason, String?) -> Result<TableCall>,
    onCancel: suspend (TableCall) -> Result<TableCall>,
    textColor: Color = OrderTrackingCardText,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val scope = rememberCoroutineScope()
    var view by remember(orderId) { mutableStateOf(CallWaiterView.IDLE) }
    var call by remember(orderId) { mutableStateOf<TableCall?>(null) }
    var busy by remember(orderId) { mutableStateOf(false) }
    var error by remember(orderId) { mutableStateOf<String?>(null) }
    var cooldown by remember(orderId) { mutableIntStateOf(0) }

    // Recupera una llamada abierta al recargar y la mantiene fresca mientras la tarjeta vive.
    LaunchedEffect(spaceId) {
        while (true) {
            onCurrent(spaceId).fold(
                onSuccess = { current ->
                    // Se cerró sin cancelarla aquí, casi siempre porque la atendieron: el servidor pide 15 s.
                    if (current == null && call != null) cooldown = MenuWaiterCallState.ATTENDED_COOLDOWN_SECONDS
                    call = current
                    view =
                        when {
                            current != null -> CallWaiterView.OPEN
                            view == CallWaiterView.OPEN -> CallWaiterView.IDLE
                            else -> view
                        }
                },
                onFailure = { cause ->
                    if (cause is CallsUnavailableException) view = CallWaiterView.UNAVAILABLE
                },
            )
            TableCallPushes.awaitNextPoll(CALL_POLL_MS)
        }
    }

    LaunchedEffect(cooldown) {
        if (cooldown > 0) {
            delay(1_000)
            cooldown -= 1
        }
    }

    fun send(reason: CallReason) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            onCall(spaceId, reason, orderId).fold(
                onSuccess = { created ->
                    call = created
                    view = CallWaiterView.OPEN
                    haptics.impact()
                },
                // Si falla, los motivos se quedan con el error a la vista para reintentar; si el servidor pide
                // esperar, el botón cuenta los segundos que dijo.
                onFailure = { cause ->
                    val wait = callRetrySeconds(cause)
                    if (wait != null) {
                        cooldown = wait
                        view = CallWaiterView.IDLE
                    } else {
                        error = cause.toUserFacingMessage()
                    }
                },
            )
            busy = false
        }
    }

    fun cancel() {
        val current = call ?: return
        if (busy) return
        busy = true
        error = null
        scope.launch {
            onCancel(current).fold(
                onSuccess = {
                    call = null
                    view = CallWaiterView.IDLE
                    // Tras cancelar se puede volver a llamar de inmediato.
                    cooldown = 0
                },
                onFailure = { cause -> error = cause.toUserFacingMessage() },
            )
            busy = false
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        AnimatedContent(
            targetState = view,
            transitionSpec = {
                (fadeIn(tween(200)) + scaleIn(initialScale = 0.97f, animationSpec = tween(200))) togetherWith
                    (fadeOut(tween(120)) + scaleOut(targetScale = 0.99f, animationSpec = tween(120))) using
                    // Sin recorte: la sombra del vidrio baja por fuera de la pieza.
                    SizeTransform(clip = false)
            },
            label = "call-waiter-view",
        ) { current ->
            when (current) {
                CallWaiterView.UNAVAILABLE -> {
                    Text(
                        "Llamar al mesero todavía no está disponible en esta cafetería.",
                        color = textColor.copy(alpha = 0.55f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    )
                }
                CallWaiterView.OPEN -> {
                    val openCall = call
                    if (openCall != null) {
                        CallLiveRow(
                            call = openCall,
                            busy = busy,
                            colors = colors,
                            textColor = textColor,
                            onCancel = ::cancel,
                        )
                    }
                }
                CallWaiterView.REASONS -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "¿Qué necesitas?",
                            color = textColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                        )
                        CallReason.entries.forEach { reason ->
                            SeaGlassButton(
                                tone = SeaGlassTone.vainilla,
                                onClick = { send(reason) },
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                enabled = !busy,
                            ) {
                                Text(
                                    reason.label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                                )
                            }
                        }
                        Text(
                            "Cancelar",
                            color = textColor.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .physicalPress(scale = PhysicalPressScale.Small) {
                                        view = CallWaiterView.IDLE
                                    }.padding(vertical = 7.dp),
                        )
                    }
                }
                CallWaiterView.IDLE -> {
                    val waiting = cooldown > 0
                    SeaGlassButton(
                        tone = if (waiting) SeaGlassTone.vainilla else SeaGlassTone.lima,
                        onClick = {
                            haptics.selection()
                            view = CallWaiterView.REASONS
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        enabled = !waiting,
                    ) {
                        Icon(Icons.Outlined.Notifications, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(
                            if (waiting) "Puedes volver a llamar en $cooldown s" else "Llamar al mesero",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }
        error?.let { message ->
            Text(
                message,
                color = colors.coral,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun CallLiveRow(
    call: TableCall,
    busy: Boolean,
    colors: VaiinillaColors,
    textColor: Color,
    onCancel: () -> Unit,
) {
    val going = call.status == CallStatus.EN_CAMINO
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier.size(30.dp).clip(CircleShape).background(colors.accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Notifications,
                contentDescription = null,
                tint = colors.accentInk,
                modifier = Modifier.size(16.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (going) "Tu mesero va en camino" else "Llamando a tu mesero…",
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                if (going) {
                    if (call.space.name.isBlank()) {
                        "Va a ${SpaceCopy.yourPlace(
                            call.space.type,
                        )}"
                    } else {
                        "Va a ${call.space.name}"
                    }
                } else {
                    "${call.space.name} · ${call.reason.label}"
                },
                color = textColor.copy(alpha = 0.58f),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 1.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // También en camino: el servidor deja cancelarla mientras siga abierta.
        if (canCancelMenuCall(call)) {
            SeaGlassButton(
                tone = SeaGlassTone.vainilla,
                onClick = onCancel,
                modifier = Modifier.height(34.dp),
                enabled = !busy,
            ) {
                Text(
                    "Cancelar",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp),
                )
            }
        }
    }
}

private const val CALL_POLL_MS = 5_000L

@Composable
private fun InlinePickupQr(order: OrderDetail) {
    val token = order.pickupToken?.takeIf { it.isNotBlank() } ?: return
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(color = Color.White, shape = RoundedCornerShape(16.dp)) {
            Box(Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                VaiinillaQrCode(value = token, qrSize = 150.dp)
            }
        }
        Text(
            if (order.summary.pickUpAtBar) {
                "Pasa a la barra por él y muéstralo en Caja."
            } else if (order.summary.destination == OrderDestination.IN_SPACE) {
                "El personal lo llevará a ${SpaceCopy.yourPlace(order.summary.space?.type)}. Muéstrales este QR."
            } else {
                "Muéstralo en Caja cuando recojas tu pedido."
            },
            color = OrderTrackingCardText.copy(alpha = 0.55f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            token,
            color = OrderTrackingCardText.copy(alpha = 0.4f),
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** «Pagado, Recibido, Preparando, En tu mesa» (Doop L10): el último paso dice dónde termina. */
private fun foodProgressSteps(order: OrderDetail): List<String> {
    val summary = order.summary
    val first = if (summary.state == OrderState.PENDING_PAYMENT || summary.payAtEnd) "Por pagar" else "Pagado"
    val last =
        when {
            summary.pickUpAtBar -> "En la barra"
            summary.space?.type == "mesa" -> "En tu mesa"
            summary.space?.type == "cancha" -> "En tu ${Venue.court}"
            else -> "Listo"
        }
    return listOf(first, "Recibido", "Preparando", last)
}

private fun foodProgressIndex(state: OrderState): Int =
    when (state) {
        OrderState.PENDING_PAYMENT, OrderState.AWAITING_ACCEPTANCE -> 0
        OrderState.PREPARING -> 2
        OrderState.READY, OrderState.ON_THE_WAY -> 3
        OrderState.DELIVERED -> 4
        else -> 1
    }

private val RentalProgressSteps = listOf("Por pagar", "Reservada", "En juego", "Terminada")

/** Terminada marca todos los pasos como hechos; una renta caída, ninguno. */
private fun rentalProgressIndex(step: RentalStep?): Int =
    when (step) {
        null -> -1
        RentalStep.DONE -> RentalStep.entries.size
        else -> step.ordinal
    }

/** ANTERIORES en una sola tarjeta, un renglón por pedido (Doop L10). */
@Composable
private fun PastOrdersGroup(
    entries: List<PastOrderEntry>,
    row: @Composable (OrderDetail) -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val reduceMotion = reducedMotion()
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(colors.paper2)
                .animateContentSize(
                    animationSpec =
                        if (reduceMotion) {
                            snap()
                        } else {
                            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                        },
                ),
    ) {
        entries.forEachIndexed { index, entry ->
            key(entry.key) {
                if (index > 0) PastOrdersDivider()
                PastEntryContent(entry, row)
            }
        }
    }
}

@Composable
private fun PastOrdersDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(LocalVaiinillaColors.current.line),
    )
}

@Composable
private fun PastEntryContent(
    entry: PastOrderEntry,
    row: @Composable (OrderDetail) -> Unit,
) {
    when (entry) {
        is PastOrderEntry.Single -> row(entry.order)
        is PastOrderEntry.TableSession -> TableSessionRows(entry, row)
    }
}

/** Los pedidos de una misma visita a la mesa, bajo el nombre de la mesa. */
@Composable
private fun TableSessionRows(
    session: PastOrderEntry.TableSession,
    row: @Composable (OrderDetail) -> Unit,
) {
    val count = session.orders.size
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "${session.orders
                .first()
                .summary.space
                ?.name ?: "Mesa"} · $count ${if (count == 1) "pedido" else "pedidos"}",
            color = LocalVaiinillaColors.current.muted,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )
        session.orders.forEachIndexed { index, order ->
            if (index > 0) PastOrdersDivider()
            row(order)
        }
    }
}

/** «#214 · Pastelería Vaini», o «Cancha 1 · Club Pádel Norte» en una renta (Doop L10). */
internal fun pastOrderTitle(
    order: OrderDetail,
    venueName: String?,
): String {
    val summary = order.summary
    val lead = if (order.isRental) summary.space?.name ?: "Renta" else "#${summary.folio}"
    val tail =
        venueName ?: order.items
            .firstOrNull()
            ?.productName
            .takeUnless { order.isRental }
    return listOfNotNull(lead, tail).joinToString(" · ")
}

/** «Hoy 1:05 pm · para llevar · 2 artículos»; en una renta, su día y horario. */
internal fun pastOrderSubtitle(order: OrderDetail): String {
    order.reservation?.takeIf { order.isRental }?.let { return rentalScheduleLabel(it) }
    val summary = order.summary
    val where =
        destinationDisplayLabel(order).let {
            if (summary.space ==
                null
            ) {
                it.replaceFirstChar { c -> c.lowercase() }
            } else {
                it
            }
        }
    val count = order.items.sumOf { it.quantity }
    val what = if (count > 1) "$count artículos" else order.items.firstOrNull()?.productName
    return listOfNotNull(cremeMoment(summary.createdAt), where, what).joinToString(" · ")
}

/** Un pedido anterior como renglón: folio y lugar, cuándo y a dónde, el total y su estado en una pastilla. */
@Composable
private fun PastOrderRow(
    order: OrderDetail,
    venueName: String?,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val summary = order.summary
    val canceled = summary.state == OrderState.CANCELED || summary.state == OrderState.NOT_PICKED_UP
    // El azul claro de Doop no se lee sobre crema; en tema claro va uno más oscuro.
    val canceledTint = if (colors.isDark) Color(0xFF8FC7FF) else Color(0xFF1F5F99)
    val tagText = if (canceled) canceledTint else colors.ink.copy(alpha = 0.86f)
    val tagBg = if (canceled) canceledTint.copy(alpha = 0.14f) else colors.ink.copy(alpha = 0.06f)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(colors.paper2)
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                pastOrderTitle(order, venueName),
                color = colors.ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                pastOrderSubtitle(order),
                color = colors.muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            // Como iOS: el motivo del rechazo también se lee en la lista, sin abrir el pedido.
            orderCancellationNotice(order)?.let { notice ->
                notice.reason?.let { reason ->
                    Text(
                        "${notice.title}: $reason",
                        color = colors.muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                moneyLabel(summary.total),
                color = colors.ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
            Text(
                pastOrderStateLabel(order),
                color = tagText,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 14.sp,
                maxLines = 1,
                modifier =
                    Modifier
                        .padding(top = 4.dp)
                        .clip(CircleShape)
                        .background(tagBg)
                        .padding(horizontal = 9.dp, vertical = 4.dp),
            )
        }
    }
}

internal fun itemImageUrls(
    order: OrderDetail,
    catalog: Catalog?,
): List<String?> {
    val seen = mutableSetOf<Int>()
    return order.items
        .filter { seen.add(it.productId) }
        // La foto que manda el pedido gana: no depende de que el catálogo ya haya cargado.
        .map { item -> item.imageUrl ?: catalog?.products?.firstOrNull { it.id == item.productId }?.imageUrl }
}

@Composable
private fun OrderItemThumb(
    imageUrl: String?,
    size: Dp,
    corner: Dp,
    placeholder: ImageVector = Icons.Outlined.Restaurant,
    tint: Color = OrderTrackingCardText.copy(alpha = 0.55f),
) {
    Box(
        modifier =
            Modifier
                .size(size)
                .clip(RoundedCornerShape(corner))
                .background(Color.White.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl != null) {
            ProductImage(
                imageUrl = imageUrl,
                contentDescription = "",
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                placeholder,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(size * 0.45f),
            )
        }
    }
}

@Composable
private fun OrderThumbRow(
    imageUrls: List<String?>,
    onAccent: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val shown = imageUrls.take(3)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        shown.forEach { url ->
            OrderItemThumb(imageUrl = url, size = 34.dp, corner = 10.dp)
        }
        if (imageUrls.size > shown.size) {
            Box(
                modifier =
                    Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = if (onAccent) 0.3f else 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "+${imageUrls.size - shown.size}",
                    color = if (onAccent) colors.accentInk else OrderTrackingCardText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
private fun ShowMorePastOrdersButton(
    hiddenCount: Int,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val reduceMotion = reducedMotion()
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec =
            if (reduceMotion) {
                snap()
            } else {
                spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                )
            },
        label = "showMoreChevron",
    )
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.physicalPress(onClick = onClick),
            color = colors.paper2,
            shape = CircleShape,
            border = BorderStroke(1.dp, colors.line),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (expanded) "Mostrar menos" else "Mostrar $hiddenCount más",
                    color = colors.ink.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp,
                )
                Icon(
                    Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = colors.ink.copy(alpha = 0.72f),
                    modifier =
                        Modifier
                            .size(14.dp)
                            .rotate(chevronRotation),
                )
            }
        }
    }
}

@Composable
private fun OrderStatusPill(
    state: OrderState,
    onAccent: Boolean = false,
    label: String = state.label,
) {
    Surface(
        color = Color.White.copy(alpha = if (onAccent) 0.55f else 0.16f),
        shape = RoundedCornerShape(10.dp),
    ) {
        Text(
            label.uppercase(),
            color = if (onAccent) LocalVaiinillaColors.current.accentInk else OrderTrackingCardText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}
