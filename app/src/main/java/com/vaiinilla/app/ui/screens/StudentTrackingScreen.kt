package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderItem
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.ui.components.EmptyState
import com.vaiinilla.app.ui.components.OrderDetailSummary
import com.vaiinilla.app.ui.components.OrderTrackingCard
import com.vaiinilla.app.ui.components.OrderTrackingTimeline
import com.vaiinilla.app.ui.components.StudentTab
import com.vaiinilla.app.ui.components.VaiinillaBottomNav
import com.vaiinilla.app.ui.components.VaiinillaBottomNavClearance
import com.vaiinilla.app.ui.operational.OperationalUiState
import com.vaiinilla.app.ui.order.OrderFlowUiState
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics

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
    onViewSticker: () -> Unit = {},
    onRefresh: () -> Unit = {},
) {
    val haptics = rememberVaiinillaHaptics()
    LaunchedEffect(Unit) {
        if (state.role != OperationalRole.CLIENT) {
            // Role is set by AppNavHost before navigation.
        }
    }

    val selected = state.selectedOrder
    val colors = LocalVaiinillaColors.current
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.paper),
    ) {
        PullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = {
                haptics.impact()
                onRefresh()
            },
            modifier = Modifier.fillMaxSize(),
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Text("Mis pedidos", color = colors.ink, fontWeight = FontWeight.Black, fontSize = 22.sp)
                }

                when {
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
                    selected != null -> {
                        item {
                            OrderTrackingCard(order = selected, showEyebrow = true)
                        }
                        item {
                            TrackingSectionHead()
                        }
                        item {
                            OrderTrackingTimeline(
                                current = selected.summary.state,
                                destination = selected.summary.destination,
                                paymentMethod = selected.summary.paymentMethod,
                            )
                        }
                        item {
                            Text("Resumen", color = colors.ink, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                        item {
                            OrderDetailSummary(order = selected)
                        }
                        item {
                            Button(
                                onClick = onViewSticker,
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
                                Text("Ver sticker", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    else -> {
                        items(state.orders, key = { it.summary.id }) { order ->
                            OrderTrackingCard(
                                order = order,
                                showEyebrow = false,
                                onClick = { onSelectOrder(order.summary.id) },
                            )
                        }
                    }
                }
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text("Seguimiento", color = colors.ink, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text("Actualización en vivo", color = colors.muted, fontSize = 12.sp)
    }
}
