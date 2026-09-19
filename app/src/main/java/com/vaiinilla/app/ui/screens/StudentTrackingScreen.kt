package com.vaiinilla.app.ui.screens

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandMore
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
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
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
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderItem
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.StripePaymentStatus
import com.vaiinilla.app.ui.components.EmptyState
import com.vaiinilla.app.ui.components.OrderDetailSummary
import com.vaiinilla.app.ui.components.OrderTrackingCard
import com.vaiinilla.app.ui.components.OrderTrackingTimeline
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.SkeletonBlock
import com.vaiinilla.app.ui.components.StudentTab
import com.vaiinilla.app.ui.components.SwipeToDeleteOrder
import com.vaiinilla.app.ui.components.VaiinillaBottomNav
import com.vaiinilla.app.ui.components.VaiinillaBottomNavClearance
import com.vaiinilla.app.ui.components.VaiinillaQrCode
import com.vaiinilla.app.ui.components.destinationDisplayLabel
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.paymentMethodLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.components.trackingStepDescription
import com.vaiinilla.app.ui.components.trackingStepTitle
import com.vaiinilla.app.ui.operational.OperationalUiState
import com.vaiinilla.app.ui.order.OrderFlowUiState
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import kotlinx.coroutines.Job
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
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
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

                    when {
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
                            item {
                                TrackingSectionHead()
                            }
                            item {
                                OrderTrackingTimeline(
                                    current = animatedSelected.summary.state,
                                    destination = animatedSelected.summary.destination,
                                    paymentMethod = animatedSelected.summary.paymentMethod,
                                    paymentStatus = animatedSelected.payment?.status,
                                )
                            }
                            if (animatedSelected.summary.state == OrderState.READY) {
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
                            val activeOrders = state.orders.filter { !it.summary.state.isPast }
                            val pastOrders = state.orders.filter { it.summary.state.isPast }
                            if (activeOrders.isNotEmpty()) {
                                item(key = "section-active") { OrdersSectionLabel("EN CURSO") }
                                itemsIndexed(activeOrders, key = { _, o -> o.summary.id }) { index, order ->
                                    SwipeToDeleteOrder(
                                        orderFolio = order.summary.folio.toString(),
                                        onDelete = { onDeleteOrder(order.summary.id) },
                                    ) {
                                        ActiveOrderCard(
                                            order = order,
                                            leadingImageUrl =
                                                itemImageUrls(order, orderState.catalog)
                                                    .firstOrNull { it != null },
                                            onOpenFull = { onSelectOrder(order.summary.id) },
                                        )
                                    }
                                    if (index == 0) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        WaitingRunnerCard()
                                    }
                                }
                            }
                            if (pastOrders.isNotEmpty()) {
                                item(key = "section-past") { OrdersSectionLabel("ANTERIORES") }
                                items(pastOrders.take(3), key = { it.summary.id }) { order ->
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
                                if (showAllPast) {
                                    items(pastOrders.drop(3), key = { it.summary.id }) { order ->
                                        Box(modifier = Modifier.animateItem()) {
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
                                    }
                                }
                                if (pastOrders.size > 3) {
                                    item(key = "section-past-more") {
                                        ShowMorePastOrdersButton(
                                            hiddenCount = pastOrders.size - 3,
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
}

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
                    "El código de recogida no está disponible en este dispositivo. Conserva la app donde hiciste el pedido.",
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

private val OrderState.isPast: Boolean
    get() = this == OrderState.DELIVERED || isTerminalWithoutDelivery

private val OrderTrackingCardBg = Color(0xFF1C1D1B)
private val OrderTrackingCardText = Color(0xFFF5F2E8)

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
) {
    val colors = LocalVaiinillaColors.current
    var expanded by rememberSaveable(order.summary.id) { mutableStateOf(false) }
    val summary = order.summary
    val reduceMotion = reducedMotion()
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
                OrderStatusPill(state = summary.state)
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OrderItemThumb(imageUrl = leadingImageUrl, size = 48.dp, corner = 14.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        order.items.joinToString(" · ") { "${it.quantity} ${it.productName}" },
                        color = OrderTrackingCardText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${destinationDisplayLabel(order)} · ${paymentMethodLabel(summary.paymentMethod)}",
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
            OrderProgressBar(
                currentIndex = summary.state.trackingIndex,
                modifier = Modifier.padding(top = 12.dp),
            )
            Row(
                modifier = Modifier.padding(top = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    summary.state.label,
                    color = OrderTrackingCardText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    trackingStepDescription(
                        summary.state,
                        summary.destination,
                        summary.paymentMethod,
                        order.payment?.status,
                    ),
                    color = OrderTrackingCardText.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (summary.state == OrderState.READY) {
                InlinePickupQr(order = order)
            }
            CardDivider()
            // Solo fade: la altura la anima animateContentSize del contenedor.
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(if (reduceMotion) snap() else tween(280, delayMillis = 110)),
                exit = fadeOut(if (reduceMotion) snap() else tween(110)),
            ) {
                Column {
                    CompactTrackingSteps(
                        current = summary.state,
                        destination = summary.destination,
                        paymentMethod = summary.paymentMethod,
                        paymentStatus = order.payment?.status,
                    )
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
                "El personal lo llevará a tu mesa. Muéstrales este QR."
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
) {
    val colors = LocalVaiinillaColors.current
    val reduceMotion = reducedMotion()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        OrderState.trackingFlow.forEachIndexed { index, _ ->
            val fillFraction by animateFloatAsState(
                targetValue = if (index <= currentIndex) 1f else 0f,
                animationSpec =
                    tween(
                        durationMillis = if (reduceMotion) 0 else 420,
                        delayMillis = if (reduceMotion) 0 else index * 60,
                        easing = CardTrackEase,
                    ),
                label = "order-progress-fill",
            )
            val isCurrent = index == currentIndex
            val isPending = index > currentIndex
            val infinite = rememberInfiniteTransition(label = "order-progress-pulse")
            val pulse by infinite.animateFloat(
                initialValue = 0.55f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(800, easing = CardTrackEase),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "order-progress-pulse-alpha",
            )
            val pendingPulse by infinite.animateFloat(
                initialValue = 0.16f,
                targetValue = 0.32f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(1600, delayMillis = index * 180, easing = CardTrackEase),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "order-progress-pending-alpha",
            )
            val segmentAlpha = if (isCurrent && !reduceMotion) pulse else 1f
            val trackAlpha = if (isPending && !reduceMotion) pendingPulse else 0.22f
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = trackAlpha)),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(fillFraction)
                            .fillMaxHeight()
                            .graphicsLayer { alpha = segmentAlpha }
                            .background(
                                if (isCurrent) colors.accent2 else colors.accent,
                            ),
                )
            }
        }
    }
}

@Composable
private fun CompactTrackingSteps(
    current: OrderState,
    destination: OrderDestination,
    paymentMethod: PaymentMethod,
    paymentStatus: StripePaymentStatus?,
) {
    val colors = LocalVaiinillaColors.current
    val reduceMotion = reducedMotion()
    val currentIndex = current.trackingIndex
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OrderState.trackingFlow.forEachIndexed { index, stepState ->
            val isDone = index < currentIndex
            val isCurrent = index == currentIndex
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
                            (index + 1).toString(),
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
                    trackingStepTitle(stepState, paymentMethod, paymentStatus),
                    color = titleColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp,
                    modifier = Modifier.padding(start = 10.dp),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    trackingStepDescription(stepState, destination, paymentMethod, paymentStatus),
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
private fun PastOrderRow(
    order: OrderDetail,
    imageUrls: List<String?> = emptyList(),
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val summary = order.summary
    val isReady = summary.state == OrderState.READY
    val contentColor = if (isReady) colors.accentInk else OrderTrackingCardText
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .physicalPress(onClick = onClick),
        color = if (isReady) colors.accent else OrderTrackingCardBg,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "#${summary.folio}",
                        color = contentColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        "${destinationDisplayLabel(order)} · ${paymentMethodLabel(summary.paymentMethod)}",
                        color = contentColor.copy(alpha = 0.62f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    moneyLabel(summary.total),
                    color = contentColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    modifier = Modifier.padding(end = 12.dp),
                )
                OrderStatusPill(state = summary.state, onAccent = isReady)
            }
            // Los thumbnails van en su propia línea: en la fila principal
            // competían con total+pill y colapsaban el texto a cero.
            if (imageUrls.isNotEmpty()) {
                OrderThumbRow(
                    imageUrls = imageUrls,
                    onAccent = isReady,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

private fun itemImageUrls(
    order: OrderDetail,
    catalog: Catalog?,
): List<String?> {
    val seen = mutableSetOf<Int>()
    return order.items
        .filter { seen.add(it.productId) }
        .map { item -> catalog?.products?.firstOrNull { it.id == item.productId }?.imageUrl }
}

@Composable
private fun OrderItemThumb(
    imageUrl: String?,
    size: Dp,
    corner: Dp,
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
                Icons.Outlined.Restaurant,
                contentDescription = null,
                tint = OrderTrackingCardText.copy(alpha = 0.55f),
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
) {
    Surface(
        color = Color.White.copy(alpha = if (onAccent) 0.55f else 0.16f),
        shape = RoundedCornerShape(10.dp),
    ) {
        Text(
            state.label.uppercase(),
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
