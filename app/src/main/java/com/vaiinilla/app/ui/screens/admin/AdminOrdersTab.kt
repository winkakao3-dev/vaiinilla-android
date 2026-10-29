package com.vaiinilla.app.ui.screens.admin

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.TableRestaurant
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.time.BusinessTime
import com.vaiinilla.app.ui.admin.AdminOrderFilter
import com.vaiinilla.app.ui.admin.AdminUiState
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.chargedPaymentLabel
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.screens.ActionPill
import com.vaiinilla.app.ui.screens.OperationalColors
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Pedidos: el historial con los filtros de la web, como tarjetas con la foto del primer platillo, y las mesas como
 * las ve el mesero a un toque (para abrir, cerrar o cobrar una mesa). Sin tablero queda solo el historial.
 */
@Composable
internal fun AdminOrdersTab(
    state: AdminUiState,
    colors: OperationalColors,
    actions: AdminActions,
    header: AdminHeader,
    tables: (@Composable () -> Unit)?,
) {
    val reduced = reducedMotion()
    var showsTables by rememberSaveable { mutableStateOf(false) }
    Crossfade(
        targetState = showsTables && tables != null,
        animationSpec = if (reduced) snap() else spring(stiffness = 400f),
        label = "admin-orders",
    ) { tablesMode ->
        if (tablesMode && tables != null) {
            TablesView(header, colors, onHistory = { showsTables = false }, tables = tables)
        } else {
            AdminOrdersHistory(state, colors, actions, header, onOpenTables = tables?.let { { showsTables = true } })
        }
    }
}

@Composable
private fun TablesView(
    header: AdminHeader,
    colors: OperationalColors,
    onHistory: () -> Unit,
    tables: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(60.dp)
                    .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AdminVenueLogo(header.logoUrl, header.placeName, colors, size = 32.dp)
            Text(
                "MESAS",
                color = colors.textPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                modifier = Modifier.weight(1f),
            )
            AdminGlassChip("Ver pedidos", false, onHistory)
        }
        Box(Modifier.weight(1f)) { tables() }
    }
}

/** El historial con los filtros de la web. Aquí solo se consultan: los mueven Caja, Cocina y Mesero. */
@Composable
private fun AdminOrdersHistory(
    state: AdminUiState,
    colors: OperationalColors,
    actions: AdminActions,
    header: AdminHeader,
    onOpenTables: (() -> Unit)?,
) {
    var open by remember { mutableStateOf<OrderDetail?>(null) }
    val listState = rememberLazyListState()
    val heroGone by rememberHeroGone(listState)
    val photos =
        remember(state.catalog) {
            state.catalog
                ?.products
                .orEmpty()
                .associate { it.id to it.imageUrl }
        }
    val chips: @Composable () -> Unit = {
        AdminChipRow {
            if (onOpenTables != null) AdminGlassChip("Mesas", false, onOpenTables)
            AdminOrderFilter.entries.forEach { filter ->
                AdminGlassChip(filter.label, filter == state.orderFilter) { actions.onSelectOrderFilter(filter) }
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item(key = "hero") {
                val live = state.live
                AdminHero(
                    title = "Pedidos",
                    subtitle =
                        live?.activeOrders?.let {
                            val count = if (live.moreActiveOrders) "$it+" else "$it"
                            if (it == 1 && !live.moreActiveOrders) "1 activo ahora" else "$count activos ahora"
                        },
                    colors = colors,
                    height = 210.dp,
                    leading =
                        onOpenTables?.let { open ->
                            { AdminHeroPill("Mesas", icon = Icons.Outlined.TableRestaurant, onClick = open) }
                        },
                    trailing = header.avatar,
                )
            }
            item(key = "chips") { chips() }
            val error = state.ordersError
            if (error != null && state.orders.isEmpty()) {
                item(key = "error") {
                    Box(Modifier.padding(horizontal = 16.dp)) { AdminLoadError(error, colors, actions.onRefreshOrders) }
                }
            } else if (!state.ordersLoading && state.orders.isEmpty()) {
                item(key = "vacio") {
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        AdminCard(colors) {
                            Text(
                                "Sin pedidos aquí",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = colors.textPrimary,
                            )
                            Text(
                                "Cuando haya pedidos con este filtro, salen en esta lista.",
                                fontSize = 13.sp,
                                color = colors.textSecondary,
                            )
                        }
                    }
                }
            }
            items(state.orders, key = { it.summary.id }) { order ->
                OrderCard(
                    order = order,
                    photo = order.items.firstOrNull()?.let { photos[it.productId] },
                    colors = colors,
                    modifier = Modifier.animateItem().padding(horizontal = 20.dp, vertical = 6.dp),
                ) { open = order }
            }
            if (state.ordersLoading) {
                item(key = "cargando") {
                    Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = colors.textPrimary,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            } else if (state.ordersCursor != null) {
                item(key = "mas") {
                    Box(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        ActionPill(
                            label = "Ver más pedidos",
                            enabled = true,
                            loading = false,
                            colors = colors,
                            primary = false,
                            onClick = actions.onLoadMoreOrders,
                        )
                    }
                }
            }
        }
        AdminCompactBar(
            visible = heroGone,
            title = header.placeName,
            logoUrl = header.logoUrl,
            colors = colors,
            trailing = { header.avatar() },
            below = chips,
        )
    }
    open?.let { order -> OrderSheet(order, colors) { open = null } }
}

/** Un pedido: la foto del primer platillo, el folio grande, su estado y qué lleva. */
@Composable
private fun OrderCard(
    order: OrderDetail,
    photo: String?,
    colors: OperationalColors,
    modifier: Modifier,
    onOpen: () -> Unit,
) {
    val summary = order.summary
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .physicalPress(onClick = onOpen)
                .clip(shape)
                .background(colors.cardBackground)
                .border(
                    1.dp,
                    if (summary.state ==
                        OrderState.READY
                    ) {
                        colors.textPrimary.copy(alpha = 0.4f)
                    } else {
                        colors.cardBorder
                    },
                    shape,
                ).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val thumb = Modifier.size(76.dp).clip(RoundedCornerShape(18.dp))
        if (photo != null) {
            ProductImage(
                imageUrl = photo,
                contentDescription = "",
                modifier = thumb,
                placeholder = { m -> Box(m.background(colors.cardInner)) },
                card = true,
            )
        } else {
            Box(thumb.background(colors.cardInner), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.ReceiptLong, contentDescription = null, tint = colors.textMuted)
            }
        }
        Column(Modifier.weight(1f)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "#${summary.folio}",
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    letterSpacing = (-0.6).sp,
                    color = colors.textPrimary,
                )
                StateTag(summary.state, colors)
            }
            Text(
                order.items.joinToString(" · ") { "${it.quantity} × ${it.productName}" },
                fontSize = 13.5.sp,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                listOfNotNull(
                    whereLabel(order),
                    timeLabel(summary.createdAt),
                    moneyLabel(summary.total),
                ).joinToString(" · "),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun StateTag(
    state: OrderState,
    colors: OperationalColors,
) {
    val (background, content) =
        when {
            state.isTerminalWithoutDelivery -> AdminCoral.copy(alpha = 0.16f) to AdminCoral
            state == OrderState.READY -> colors.textPrimary to colors.background
            state == OrderState.PREPARING || state == OrderState.PAID ->
                colors.accentLime.copy(alpha = 0.18f) to colors.textPrimary
            else -> colors.cardInner to colors.textSecondary
        }
    Text(
        state.label,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = content,
        maxLines = 1,
        modifier =
            Modifier
                .clip(CircleShape)
                .background(background)
                .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun OrderSheet(
    order: OrderDetail,
    colors: OperationalColors,
    onDismiss: () -> Unit,
) {
    val summary = order.summary
    AdminSheet(title = "Pedido #${summary.folio}", colors = colors, busy = false, onDismiss = onDismiss) {
        AdminCard(colors) {
            AdminValueRow("Estado", summary.state.label, colors)
            AdminValueRow(
                "Pago",
                (chargedPaymentLabel(summary) ?: summary.paymentMethod.label) +
                    if (summary.paymentPending) " · pendiente" else "",
                colors,
            )
            whereLabel(order)?.let { AdminValueRow("Destino", it, colors) }
            whoLabel(order)?.let { AdminValueRow("Cliente", it, colors) }
            timeLabel(summary.createdAt)?.let { AdminValueRow("Hora", it, colors, detail = summary.operationalDate) }
            summary.cancelReason?.let { Text("Motivo: $it", fontSize = 13.sp, color = AdminCoral) }
            order.arrivedAt?.let { arrived ->
                Text(
                    "Ya llegó a las ${ArrivalFormat.format(arrived.atZone(BusinessTime.zone))}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
            }
        }
        AdminCard(colors) {
            AdminSectionTitle("Productos", colors)
            order.items.forEach { item ->
                val details =
                    buildList {
                        if (item.options.isNotEmpty()) add(item.options.joinToString(", ") { it.name })
                        if (item.notes.isNotBlank()) add("Nota: ${item.notes}")
                        item.rejection?.let { add("Quitado: ${it.reason}") }
                    }
                AdminValueRow(
                    label = "${item.quantity} × ${item.productName}",
                    value = moneyLabel(item.subtotal),
                    colors = colors,
                    detail = details.joinToString("\n").ifBlank { null },
                )
            }
            if (order.kitchenNotes.isNotBlank()) {
                Text(
                    "Notas: ${order.kitchenNotes}",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.cardBorder))
            if (summary.combinedSavings.toBigDecimalOrNull()?.signum() == 1) {
                AdminValueRow("Ahorro por combinar", "-${moneyLabel(summary.combinedSavings)}", colors)
            }
            if (summary.cashbackAwarded.toBigDecimalOrNull()?.signum() == 1) {
                AdminValueRow("Cashback otorgado", moneyLabel(summary.cashbackAwarded), colors)
            }
            AdminValueRow("Total", moneyLabel(summary.total), colors, strong = true)
        }
    }
}

private fun whereLabel(order: OrderDetail): String? =
    when {
        order.summary.space != null -> order.summary.space.name
        order.summary.destination == OrderDestination.TAKE_AWAY -> OrderDestination.TAKE_AWAY.label
        else -> null
    }

private fun whoLabel(order: OrderDetail): String? =
    order.customerName?.takeIf { it.isNotBlank() } ?: order.user?.name?.takeIf { it.isNotBlank() }

private val ArrivalFormat = DateTimeFormatter.ofPattern("HH:mm")

private val TimeFormat = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.forLanguageTag("es-MX"))

private fun timeLabel(iso: String): String? =
    runCatching { TimeFormat.format(Instant.parse(iso).atZone(BusinessTime.zone)) }.getOrNull()
