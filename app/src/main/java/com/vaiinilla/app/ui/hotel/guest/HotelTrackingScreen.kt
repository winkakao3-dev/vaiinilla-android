package com.vaiinilla.app.ui.hotel.guest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.activeItems
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

/** En qué paso va un pedido a la habitación: 0 recibido, 1 preparando, 2 en camino, 3 entregado. */
internal fun hotelTrackingStage(state: OrderState): Int =
    when (state) {
        OrderState.DELIVERED -> 3
        OrderState.READY, OrderState.ON_THE_WAY -> 2
        OrderState.PREPARING -> 1
        else -> 0
    }

private val TRACKING_STEPS = listOf("Recibido", "Preparando", "En camino a tu cuarto", "Entregado")

/** Los dos renglones grandes del seguimiento; el segundo va en lima. El servidor no manda un tiempo de llegada. */
internal fun hotelTrackingHeadline(state: OrderState): Pair<String, String> =
    when {
        state == OrderState.DELIVERED -> "Entregado" to "buen provecho"
        state == OrderState.CANCELED -> "Pedido" to "cancelado"
        state.isTerminalWithoutDelivery -> "Pedido" to "sin entregar"
        else ->
            when (hotelTrackingStage(state)) {
                2 -> "Va en camino" to "a tu cuarto"
                1 -> "Lo estamos" to "preparando"
                else -> "Recibimos" to "tu pedido"
            }
    }

/**
 * H04: el seguimiento de un pedido a la habitación. Sin nombre del personal y sin tiempo de llegada inventado: la
 * línea de tiempo sigue el estado del pedido. El cargo a la habitación aparece cuando el pedido va a la cuenta.
 */
@Composable
fun HotelTrackingContent(
    order: OrderDetail,
    onCallReception: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val state = order.summary.state
    val stage = hotelTrackingStage(state)
    val closed = state.isTerminalWithoutDelivery
    val (first, second) = hotelTrackingHeadline(state)
    var showItems by rememberSaveable(order.summary.id) { mutableStateOf(false) }
    val items = order.activeItems
    Box(modifier = modifier.fillMaxSize().background(colors.paper)) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(
                        rememberScrollState(),
                    ).padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val place =
                    order.summary.space
                        ?.name
                        ?.let { " · $it" }
                        .orEmpty()
                Text(
                    "Pedido #${order.summary.folio}$place",
                    color = colors.muted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    first,
                    color = colors.ink,
                    fontSize = 44.sp,
                    lineHeight = 46.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.5).sp,
                )
                Text(
                    second,
                    color = colors.accent,
                    fontSize = 44.sp,
                    lineHeight = 46.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.5).sp,
                )
            }
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!closed) {
                    HotelPanel {
                        TRACKING_STEPS.forEachIndexed { index, label ->
                            TimelineRow(
                                index = index,
                                label = label,
                                detail = stepDetail(index, items.map { it.productName }),
                                done = index < stage || state == OrderState.DELIVERED,
                                current = index == stage && state != OrderState.DELIVERED,
                                last = index == TRACKING_STEPS.lastIndex,
                            )
                        }
                    }
                }
                if (showItems) {
                    HotelPanel(title = "Tu pedido") {
                        items.forEach { item ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    "${item.quantity} × ${item.productName}",
                                    color = colors.ink,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    moneyLabel(item.subtotal),
                                    color = colors.ink,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    style = TextStyle(fontFeatureSettings = "tnum"),
                                )
                            }
                        }
                    }
                }
                if (order.summary.payAtEnd) {
                    HotelPanel {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    "CARGO A LA HABITACIÓN",
                                    color = colors.muted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp,
                                )
                                Text(
                                    "Se paga al salir",
                                    color = colors.muted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Text(
                                moneyLabel(order.summary.total),
                                color = colors.ink,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                style = TextStyle(fontFeatureSettings = "tnum"),
                            )
                        }
                    }
                }
            }
            Box(Modifier.height(96.dp))
        }
        Row(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (onCallReception != null && !closed && state != OrderState.DELIVERED) {
                PillButton(
                    "Llamar a recepción",
                    filled = false,
                    onClick = onCallReception,
                    modifier = Modifier.weight(1f).testTag("hotel_call_reception"),
                )
            }
            PillButton(
                if (showItems) "Ocultar pedido" else "Ver pedido",
                filled = true,
                onClick = { showItems = !showItems },
                modifier = Modifier.weight(1f).testTag("hotel_view_order"),
            )
        }
    }
}

private fun stepDetail(
    index: Int,
    names: List<String>,
): String =
    when (index) {
        0 -> "La cocina ya lo tiene"
        1 -> names.take(2).joinToString(" y ").ifEmpty { "Lo preparan para ti" }
        2 -> "Te lo llevan a tu puerta"
        else -> "Que lo disfrutes"
    }

@Composable
private fun TimelineRow(
    index: Int,
    label: String,
    detail: String,
    done: Boolean,
    current: Boolean,
    last: Boolean,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.semantics(mergeDescendants = true) {},
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier =
                    Modifier
                        .size(28.dp)
                        .background(
                            when {
                                done -> colors.accent
                                current -> colors.ink
                                else -> colors.line
                            },
                            CircleShape,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (done) "✓" else "${index + 1}",
                    color =
                        if (done) {
                            colors.accentInk
                        } else if (current) {
                            colors.paper
                        } else {
                            colors.muted
                        },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            if (!last) Box(Modifier.width(2.dp).height(24.dp).background(colors.line))
        }
        Column(
            modifier = Modifier.padding(bottom = if (last) 0.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                label,
                color = if (done || current) colors.ink else colors.muted,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
            )
            Text(detail, color = colors.muted, fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PillButton(
    label: String,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val shape = RoundedCornerShape(29.dp)
    Box(
        modifier =
            modifier
                .height(58.dp)
                .physicalPress(onClick = onClick)
                .clip(shape)
                .background(if (filled) colors.ink else colors.paper2)
                .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (filled) colors.onInk else colors.ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}
