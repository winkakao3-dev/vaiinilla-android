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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.ui.components.DemoEmptyState
import com.vaiinilla.app.ui.components.OrderDetailSummary
import com.vaiinilla.app.ui.components.OrderStateTrackingHero
import com.vaiinilla.app.ui.components.OrderTrackingCard
import com.vaiinilla.app.ui.components.OrderTrackingTimeline
import com.vaiinilla.app.ui.components.StudentTab
import com.vaiinilla.app.ui.components.VaiinillaBottomNav
import com.vaiinilla.app.ui.operational.OperationalUiState
import com.vaiinilla.app.ui.order.OrderFlowUiState
import com.vaiinilla.app.ui.order.cartItemCount
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

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
) {
    LaunchedEffect(Unit) {
        if (state.role != OperationalRole.CLIENT) {
            // Role is set by AppNavHost before navigation.
        }
    }

    val selected = state.selectedOrder
    val colors = LocalVaiinillaColors.current
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text("Mis pedidos", color = colors.ink, fontWeight = FontWeight.Black, fontSize = 22.sp)
            }

            when {
                state.orders.isEmpty() -> {
                    item {
                        DemoEmptyState(
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
                        OrderStateTrackingHero(
                            state = selected.summary.state,
                            destination = selected.summary.destination,
                        )
                    }
                    item {
                        OrderTrackingCard(order = selected, showEyebrow = false)
                    }
                    item {
                        TrackingSectionHead()
                    }
                    item {
                        OrderTrackingTimeline(current = selected.summary.state)
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
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
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

        VaiinillaBottomNav(
            activeTab = StudentTab.ORDERS,
            cartCount = orderState.cartItemCount,
            onMenu = onMenu,
            onAssistant = onAssistant,
            onOrders = {},
            onWallet = onWallet,
            onCart = onCart,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

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
