package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.ui.components.VaiinillaQrCode
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.order.OrderLinkUiState
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaColors

/** Seguimiento de un pedido por su enlace, sin cuenta. Solo lee; el QR de recogida sale cuando está listo. */
@Composable
fun OrderLinkTrackingScreen(
    state: OrderLinkUiState,
    onBack: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.paper)
                .statusBarsPadding(),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(start = 4.dp).defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver", tint = colors.ink)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text("Seguimiento", color = colors.accent, fontWeight = FontWeight.Black, letterSpacing = 1.3.sp)
                Text(
                    state.order?.let { "Tu pedido #${it.summary.folio}" } ?: "Tu pedido",
                    color = colors.ink,
                    fontSize = 28.sp,
                    lineHeight = 31.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            when {
                state.gone ->
                    item(key = "gone") {
                        TrackingCard(colors.paper2) {
                            Text(
                                "Este pedido ya no está disponible.",
                                color = colors.ink,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                            )
                        }
                    }
                state.order != null -> orderContent(state.order, colors)
                state.error == null ->
                    item(key = "loading") {
                        Text("Buscando tu pedido…", color = colors.muted, fontSize = 14.sp)
                    }
            }
            state.error?.let { message ->
                item(key = "error") {
                    Text(message, color = colors.coral, fontSize = 13.sp)
                }
            }
        }
    }
}

private fun LazyListScope.orderContent(
    order: OrderDetail,
    colors: VaiinillaColors,
) {
    item(key = "status-${order.summary.id}") {
        TrackingCard(colors.paper2, modifier = Modifier.arrive(0, key = order.summary.id)) {
            Text("Estado", color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Text(
                order.summary.state.label,
                color = colors.ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
    val pickupToken = order.pickupToken?.takeIf { order.summary.state == OrderState.READY && it.isNotBlank() }
    if (pickupToken != null) {
        item(key = "qr-${order.summary.id}") {
            Surface(
                modifier = Modifier.fillMaxWidth().arrive(1, key = pickupToken),
                color = Color.White,
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Muestra este QR al recoger", color = colors.ink, fontWeight = FontWeight.Black)
                    VaiinillaQrCode(
                        value = pickupToken,
                        qrSize = 190.dp,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
    }
    items(order.items, key = { "${order.summary.id}-${it.id}" }) { line ->
        Text(
            "${line.quantity} × ${line.productName}",
            color = colors.ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
    item(key = "total-${order.summary.id}") {
        Text(
            "Total ${moneyLabel(order.summary.total)}",
            color = colors.ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun TrackingCard(
    background: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = background,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) { content() }
    }
}
