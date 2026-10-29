package com.vaiinilla.app.ui.screens

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SportsTennis
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
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
import com.vaiinilla.app.domain.model.OrderReservation
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.PREPARING_SKIPPED_NOTE
import com.vaiinilla.app.domain.model.PastOrderEntry
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.SpaceCopy
import com.vaiinilla.app.domain.model.StripePaymentStatus
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
import com.vaiinilla.app.ui.components.RentalTrackingTimeline
import com.vaiinilla.app.ui.components.SkeletonBlock
import com.vaiinilla.app.ui.components.StudentTab
import com.vaiinilla.app.ui.components.SwipeToDeleteOrder
import com.vaiinilla.app.ui.components.VaiinillaBottomNav
import com.vaiinilla.app.ui.components.VaiinillaBottomNavClearance
import com.vaiinilla.app.ui.components.VaiinillaQrCode
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.canAnnounceArrival
import com.vaiinilla.app.ui.components.destinationDisplayLabel
import com.vaiinilla.app.ui.components.isLiveRental
import com.vaiinilla.app.ui.components.isRental
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.needsPickupQr
import com.vaiinilla.app.ui.components.orderPaymentLabel
import com.vaiinilla.app.ui.components.orderStateLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.components.rentalScheduleLabel
import com.vaiinilla.app.ui.components.rentalStateLabel
import com.vaiinilla.app.ui.components.rentalStep
import com.vaiinilla.app.ui.components.rentalStepDescription
import com.vaiinilla.app.ui.components.scheduledDeliveryLabel
import com.vaiinilla.app.ui.components.trackingStepDescription
import com.vaiinilla.app.ui.components.trackingStepTitle
import com.vaiinilla.app.ui.operational.OperationalUiState
import com.vaiinilla.app.ui.order.MenuWaiterCallState
import com.vaiinilla.app.ui.order.OrderFlowUiState
import com.vaiinilla.app.ui.order.callRetrySeconds
import com.vaiinilla.app.ui.order.canCancelMenuCall
import com.vaiinilla.app.ui.order.cancellationMoneyNote
import com.vaiinilla.app.ui.order.orderCancellationNotice
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
    // Vainiverso: la tienda arriba y la lista en una hoja de vidrio que sube sobre ella.
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
    val cheering = state.orders.any { it.isActiveForCustomer && it.summary.state == OrderState.READY }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val windowPx = constraints.maxHeight * ORDERS_SCENE_WINDOW
        val windowDp = with(density) { windowPx.toDp() }
        val overview = selected == null
        val sheetTop = {
            when {
                !overview -> 0f
                listState.firstVisibleItemIndex == 0 -> windowPx - listState.firstVisibleItemScrollOffset
                else -> 0f
            }
        }
        CompositionLocalProvider(LocalScene provides palette) {
            VainiversoStoreBackdrop(sheetTop = sheetTop, cheering = cheering)
        }
        CompositionLocalProvider(LocalVaiinillaColors provides glassColors) {
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
                    LazyColumn(
                        state = if (inOverview) listState else detailListState,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .then(if (inOverview) Modifier else Modifier.statusBarsPadding()),
                        contentPadding =
                            PaddingValues(
                                start = 20.dp,
                                end = 20.dp,
                                top = if (inOverview) 0.dp else 18.dp,
                                bottom = VaiinillaBottomNavClearance + 48.dp,
                            ),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (inOverview) {
                            // Ventana a la tienda: la hoja de vidrio empieza donde termina este hueco.
                            item(key = "store-window") { Spacer(Modifier.height(windowDp)) }
                        }
                        item {
                            Column {
                                Text("Mis pedidos", color = colors.ink, fontWeight = FontWeight.Black, fontSize = 28.sp)
                                orderState.guestVenue?.establishment?.name?.let { venueName ->
                                    Text(
                                        venueName,
                                        color = colors.muted,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                            }
                        }
                        // Con pedidos ya cargados, un error de red o de "Ya llegué" no se veía en ningún lado (PEDIDO-05).
                        trackingNoticeFor(state)?.let { notice ->
                            item(key = "tracking-notice") { TrackingNotice(notice) }
                        }
                        if (animatedSelected == null) {
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
                                                if (animatedSelected.isRental) {
                                                    null
                                                } else {
                                                    itemImageUrls(animatedSelected, orderState.catalog)
                                                        .firstOrNull { it != null }
                                                },
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
                                if (animatedSelected.isRental) {
                                    item { RentalTrackingTimeline(order = animatedSelected) }
                                } else if (animatedSelected.summary.state.isTerminalWithoutDelivery) {
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
                                        )
                                    }
                                }
                                if (animatedSelected.summary.state == OrderState.CANCELED) {
                                    item { CancelReasonCard(animatedSelected) }
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
                                        Text(
                                            if (animatedSelected.isRental) "Ver mi renta" else "Ver recibo",
                                            fontWeight = FontWeight.Black,
                                        )
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
                                    item(key = "section-active") { OrdersSectionLabel("EN CURSO") }
                                    itemsIndexed(activeOrders, key = { _, o -> o.summary.id }) { index, order ->
                                        SwipeToDeleteOrder(
                                            orderFolio = order.summary.folio.toString(),
                                            onDelete = { onDeleteOrder(order.summary.id) },
                                            enabled = !order.isActiveForCustomer,
                                        ) {
                                            ActiveOrderCard(
                                                order = order,
                                                leadingImageUrl =
                                                    if (order.isRental) {
                                                        null
                                                    } else {
                                                        itemImageUrls(order, orderState.catalog)
                                                            .firstOrNull { it != null }
                                                    },
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
                                        // El juego es para la espera de la comida: no aparece con un pago por
                                        // confirmar, una renta o un pedido que ya está listo para recoger.
                                        if (index == 0 && order.isWaitingForFood) {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            WaitingArcadeCard()
                                        }
                                    }
                                }
                                if (pastOrders.isNotEmpty()) {
                                    val pastEntries = groupOrdersBySession(pastOrders)
                                    val pastRow: @Composable (OrderDetail) -> Unit = { order ->
                                        SwipeToDeleteOrder(
                                            orderFolio = order.summary.folio.toString(),
                                            onDelete = { onDeleteOrder(order.summary.id) },
                                        ) {
                                            PastOrderRow(
                                                order = order,
                                                imageUrls = itemImageUrls(order, orderState.catalog),
                                                onClick = { onSelectOrder(order.summary.id) },
                                            )
                                        }
                                    }
                                    item(key = "section-past") { OrdersSectionLabel("ANTERIORES") }
                                    items(pastEntries.take(3), key = { it.key }) { entry ->
                                        PastEntryContent(entry, pastRow)
                                    }
                                    if (showAllPast) {
                                        items(pastEntries.drop(3), key = { it.key }) { entry ->
                                            Box(modifier = Modifier.animateItem()) {
                                                PastEntryContent(entry, pastRow)
                                            }
                                        }
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
                            }
                        }
                    }
                }
            }
        }
        // Cuando la hoja llega arriba, la barra de estado queda sobre vidrio y no sobre el texto.
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .graphicsLayer { alpha = if (sheetTop() < size.height) 1f else 0f }
                .background(palette.stage.copy(alpha = 0.92f)),
        )
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

internal val OrderTrackingCardBg = Color(0xE01C1D1B)
internal val OrderTrackingCardText = Color(0xFFF5F2E8)

@Composable
private fun OrdersSectionLabel(text: String) {
    val colors = LocalVaiinillaColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(colors.accent),
        )
        Text(
            text,
            color = colors.muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.4.sp,
        )
    }
}

@Composable
private fun ActiveOrderCard(
    order: OrderDetail,
    leadingImageUrl: String? = null,
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
    var expanded by rememberSaveable(order.summary.id) { mutableStateOf(false) }
    val summary = order.summary
    val reduceMotion = reducedMotion()
    Surface(
        // Tocar la tarjeta abre el pedido; sus botones (mesero, seguimiento) siguen respondiendo aparte.
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onOpenFull),
        color = OrderTrackingCardBg,
        shape = RoundedCornerShape(24.dp),
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
                    "#${summary.folio}",
                    color = OrderTrackingCardText.copy(alpha = 0.55f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                )
                OrderStatusPill(state = summary.state, label = customerStateLabel(order))
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OrderItemThumb(
                    imageUrl = leadingImageUrl,
                    size = 48.dp,
                    corner = 14.dp,
                    placeholder = if (order.isRental) Icons.Outlined.SportsTennis else Icons.Outlined.Restaurant,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        order.activeItems.joinToString(" · ") { "${it.quantity} ${it.productName}" },
                        color = OrderTrackingCardText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listOf(
                            destinationDisplayLabel(order),
                            order.reservation?.let { rentalScheduleLabel(it) },
                            scheduledDeliveryLabel(summary),
                            orderPaymentLabel(summary),
                        ).filterNotNull().joinToString(" · "),
                        color = OrderTrackingCardText.copy(alpha = 0.58f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    moneyLabel(summary.total),
                    color = OrderTrackingCardText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                )
            }
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
            if (rental != null) {
                OrderProgressBar(
                    currentIndex = rentalProgressIndex(rentalNow),
                    steps = RentalProgressSteps,
                    modifier = Modifier.padding(top = 12.dp),
                )
            } else {
                OrderProgressBar(
                    currentIndex = summary.state.trackingIndex,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            Row(
                modifier = Modifier.padding(top = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    customerStateLabel(order),
                    color = OrderTrackingCardText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    if (rental != null) {
                        rentalNow
                            ?.let { rentalStepDescription(it, rental, summary.paymentMethod, order.payment?.status) }
                            .orEmpty()
                    } else {
                        trackingStepDescription(
                            summary.state,
                            summary.destination,
                            summary.paymentMethod,
                            order.payment?.status,
                            summary.space?.type,
                            summary.payAtEnd,
                        )
                    },
                    color = OrderTrackingCardText.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
            CardDivider()
            // Solo fade: la altura la anima animateContentSize del contenedor.
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(if (reduceMotion) snap() else tween(280, delayMillis = 110)),
                exit = fadeOut(if (reduceMotion) snap() else tween(110)),
            ) {
                Column {
                    if (rental != null) {
                        RentalCompactSteps(order = order, reservation = rental, current = rentalNow)
                    } else {
                        CompactTrackingSteps(
                            current = summary.state,
                            destination = summary.destination,
                            paymentMethod = summary.paymentMethod,
                            paymentStatus = order.payment?.status,
                            spaceType = summary.space?.type,
                            payAtEnd = summary.payAtEnd,
                            skipsKitchen = order.skipsKitchen,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onOpenFull,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = colors.accent,
                                contentColor = colors.accentInk,
                            ),
                    ) {
                        Text("Ver pedido completo", fontWeight = FontWeight.Black, fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                    CardDivider()
                }
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .physicalPress(
                            scale = PhysicalPressScale.Small,
                        ) {
                            expanded = !expanded
                        },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (expanded) "Ocultar seguimiento" else "Ver seguimiento",
                    color = OrderTrackingCardText.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                val chevronRotation by animateFloatAsState(
                    targetValue = if (expanded) 180f else 0f,
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    label = "tracking-chevron",
                )
                Icon(
                    Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = OrderTrackingCardText.copy(alpha = 0.5f),
                    modifier =
                        Modifier
                            .size(16.dp)
                            .rotate(chevronRotation),
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
                if (going) "${call.takenBy?.name ?: "Tu mesero"} va en camino" else "Llamando a tu mesero…",
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
            if (order.summary.destination == OrderDestination.IN_SPACE) {
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

private val CardTrackEase = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

@Composable
private fun OrderProgressBar(
    currentIndex: Int,
    modifier: Modifier = Modifier,
    steps: List<ProgressStep> = FoodProgressSteps,
) {
    val colors = LocalVaiinillaColors.current
    val reduceMotion = reducedMotion()
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        steps.forEachIndexed { index, step ->
            val isDone = index < currentIndex
            val isCurrent = index == currentIndex
            val nodeFill =
                when {
                    isCurrent -> colors.accent2
                    isDone -> colors.accent
                    else -> Color.White.copy(alpha = 0.14f)
                }
            val iconTint =
                if (isDone || isCurrent) {
                    colors.accentInk
                } else {
                    OrderTrackingCardText.copy(alpha = 0.42f)
                }
            Box(
                modifier =
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(nodeFill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = step.icon,
                    contentDescription = step.label,
                    tint = iconTint,
                    modifier = Modifier.size(12.dp),
                )
            }
            if (index < steps.lastIndex) {
                val railFill by animateColorAsState(
                    targetValue = if (index < currentIndex) colors.accent else Color.White.copy(alpha = 0.22f),
                    animationSpec =
                        tween(
                            durationMillis = if (reduceMotion) 0 else 420,
                            delayMillis = if (reduceMotion) 0 else index * 60,
                            easing = CardTrackEase,
                        ),
                    label = "order-progress-rail",
                )
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(railFill),
                )
            }
        }
    }
}

private data class ProgressStep(
    val icon: ImageVector,
    val label: String,
)

private val FoodProgressSteps: List<ProgressStep>
    get() = OrderState.trackingFlow.map { ProgressStep(it.trackingIcon, it.label) }

private val RentalProgressSteps: List<ProgressStep>
    get() =
        listOf(
            ProgressStep(Icons.AutoMirrored.Outlined.ReceiptLong, "Por pagar"),
            ProgressStep(Icons.Outlined.EventAvailable, "Reservada"),
            ProgressStep(Icons.Outlined.SportsTennis, "En juego"),
            ProgressStep(Icons.Outlined.Verified, "Terminada"),
        )

/** Terminada marca todos los pasos como hechos; una renta caída, ninguno. */
private fun rentalProgressIndex(step: RentalStep?): Int =
    when (step) {
        null -> -1
        RentalStep.DONE -> RentalStep.entries.size
        else -> step.ordinal
    }

private val OrderState.trackingIcon: ImageVector
    get() =
        when (this) {
            OrderState.PENDING_PAYMENT -> Icons.AutoMirrored.Outlined.ReceiptLong
            OrderState.PAID -> Icons.Outlined.CheckCircle
            OrderState.PREPARING -> Icons.Outlined.Restaurant
            OrderState.READY -> Icons.Outlined.Notifications
            OrderState.DELIVERED -> Icons.Outlined.Verified
            else -> Icons.Outlined.Check
        }

@Composable
private fun CompactTrackingSteps(
    current: OrderState,
    destination: OrderDestination,
    paymentMethod: PaymentMethod,
    paymentStatus: StripePaymentStatus?,
    spaceType: String? = null,
    payAtEnd: Boolean = false,
    skipsKitchen: Boolean = false,
) {
    CompactStepList(
        steps =
            OrderState.trackingFlow.map { stepState ->
                val skipped = skipsKitchen && stepState == OrderState.PREPARING
                CompactStep(
                    title = trackingStepTitle(stepState, paymentMethod, paymentStatus, payAtEnd),
                    description =
                        if (skipped) {
                            PREPARING_SKIPPED_NOTE
                        } else {
                            trackingStepDescription(
                                stepState,
                                destination,
                                paymentMethod,
                                paymentStatus,
                                spaceType,
                                payAtEnd,
                            )
                        },
                    skipped = skipped,
                )
            },
        currentIndex = current.trackingIndex,
    )
}

@Composable
private fun RentalCompactSteps(
    order: OrderDetail,
    reservation: OrderReservation,
    current: RentalStep?,
) {
    CompactStepList(
        steps =
            RentalStep.entries.map { step ->
                CompactStep(
                    title = step.title,
                    description =
                        rentalStepDescription(step, reservation, order.summary.paymentMethod, order.payment?.status),
                )
            },
        currentIndex = rentalProgressIndex(current),
    )
}

private data class CompactStep(
    val title: String,
    val description: String,
    /** "Preparando" en un pedido sin cocina: no aplica. */
    val skipped: Boolean = false,
)

@Composable
private fun CompactStepList(
    steps: List<CompactStep>,
    currentIndex: Int,
) {
    val colors = LocalVaiinillaColors.current
    val reduceMotion = reducedMotion()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        steps.forEachIndexed { index, step ->
            val isDone = !step.skipped && index < currentIndex
            val isCurrent = !step.skipped && index == currentIndex
            val duration = if (reduceMotion) 0 else 420
            val delay = if (reduceMotion) 0 else index * 70
            val spec = tween<Color>(duration, delayMillis = delay, easing = CardTrackEase)
            val circleColor by animateColorAsState(
                targetValue =
                    when {
                        isDone -> colors.accent
                        isCurrent -> colors.accent2
                        else -> Color.White.copy(alpha = 0.12f)
                    },
                animationSpec = spec,
                label = "compact-step-circle",
            )
            val titleColor by animateColorAsState(
                targetValue =
                    if (isDone || isCurrent) {
                        OrderTrackingCardText
                    } else {
                        OrderTrackingCardText.copy(alpha = 0.38f)
                    },
                animationSpec = spec,
                label = "compact-step-title",
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(circleColor),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isDone) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = null,
                            tint = colors.accentInk,
                            modifier = Modifier.size(12.dp),
                        )
                    } else {
                        Text(
                            if (step.skipped) "–" else (index + 1).toString(),
                            color =
                                if (isCurrent) {
                                    colors.accentInk
                                } else {
                                    OrderTrackingCardText.copy(alpha = 0.45f)
                                },
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                        )
                    }
                }
                Text(
                    step.title,
                    color = titleColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp,
                    textDecoration = if (step.skipped) TextDecoration.LineThrough else null,
                    modifier = Modifier.padding(start = 10.dp),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    step.description,
                    color = OrderTrackingCardText.copy(alpha = 0.45f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    lineHeight = 13.sp,
                    modifier = Modifier.widthIn(max = 150.dp),
                )
            }
        }
    }
}

@Composable
private fun PastEntryContent(
    entry: PastOrderEntry,
    row: @Composable (OrderDetail) -> Unit,
) {
    when (entry) {
        is PastOrderEntry.Single -> row(entry.order)
        is PastOrderEntry.TableSession -> TableSessionCard(entry, row)
    }
}

@Composable
private fun TableSessionCard(
    session: PastOrderEntry.TableSession,
    row: @Composable (OrderDetail) -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val count = session.orders.size
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(colors.paper2)
                .border(1.dp, colors.line, RoundedCornerShape(26.dp))
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "${session.orders
                .first()
                .summary.space
                ?.name ?: "Mesa"} · " +
                "$count ${if (count == 1) "pedido" else "pedidos"}",
            color = colors.ink,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
        )
        session.orders.forEach { order -> row(order) }
    }
}

/**
 * Pedido anterior en vidrio: la foto del primer producto, qué se pidió, a dónde y cómo se pagó, y el
 * total con un punto de color por estado. Tocarlo abre la tienda con ese pedido.
 */
@Composable
private fun PastOrderRow(
    order: OrderDetail,
    imageUrls: List<String?> = emptyList(),
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val summary = order.summary
    val shape = RoundedCornerShape(22.dp)
    val first = order.items.firstOrNull()
    val title =
        when {
            first == null -> "Pedido #${summary.folio}"
            order.items.size == 1 -> "${first.quantity} × ${first.productName}"
            else -> "${first.productName} y ${order.items.size - 1} más"
        }
    val stateDot =
        when (summary.state) {
            OrderState.DELIVERED, OrderState.READY -> colors.accent
            OrderState.CANCELED, OrderState.NOT_PICKED_UP -> colors.coral
            else -> colors.muted.copy(alpha = 0.6f)
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.paper2)
                .border(1.dp, colors.line, shape)
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
                .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            OrderItemThumb(imageUrl = imageUrls.firstOrNull(), size = 52.dp, corner = 16.dp, tint = colors.muted)
            if (imageUrls.size > 1) {
                Text(
                    "+${imageUrls.size - 1}",
                    color = colors.accentInk,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                            .clip(CircleShape)
                            .background(colors.accent)
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = colors.ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "#${summary.folio} · ${destinationDisplayLabel(order)} · ${orderPaymentLabel(summary)}",
                color = colors.muted,
                fontSize = 12.sp,
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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                Box(
                    Modifier
                        .size(7.dp)
                        .background(stateDot, CircleShape),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    customerStateLabel(order),
                    color = colors.muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
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

@Composable
private fun CardDivider() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp)
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.12f)),
    )
}
