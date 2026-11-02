package com.vaiinilla.app.ui.screens.admin

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.HomeOrderAction
import com.vaiinilla.app.domain.model.HomeOrderBoard
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.repository.BoardTable
import com.vaiinilla.app.domain.repository.SpaceAvailabilityState
import com.vaiinilla.app.domain.time.BusinessTime
import com.vaiinilla.app.ui.admin.AdminOrderFilter
import com.vaiinilla.app.ui.admin.AdminUiState
import com.vaiinilla.app.ui.components.chargedPaymentLabel
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.theme.Yolk
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Las mesas del tablero del mesero, para las fichas de arriba de Pedidos (marco 34). */
class AdminOrdersTables(
    val tables: List<BoardTable>,
    val onRefresh: () -> Unit,
    val onOpenSpace: (Int) -> Unit,
    val board: @Composable () -> Unit,
)

private enum class OrdersMode { ORDERS, AWAITING, AGENDA }

/**
 * Pedidos como el lienzo de Doop (marcos 34 a 36): las mesas en fichas arriba, los filtros y la lista. Tocar una mesa
 * abre el tablero del mesero en esa mesa; aquí solo se consultan los pedidos, los mueven Caja, Cocina y Mesero.
 */
@Composable
internal fun AdminOrdersTab(
    state: AdminUiState,
    colors: OperationalColors,
    actions: AdminActions,
    header: AdminHeader,
    tables: AdminOrdersTables?,
    onTakeOrder: (() -> Unit)?,
) {
    val reduced = reducedMotion()
    var showsTables by rememberSaveable { mutableStateOf(false) }
    Crossfade(
        targetState = showsTables && tables != null,
        animationSpec = if (reduced) snap() else spring(stiffness = 400f),
        label = "admin-orders",
    ) { tablesMode ->
        if (tablesMode && tables != null) {
            TablesView(colors, onBack = { showsTables = false }, board = tables.board)
        } else {
            OrdersPage(state, colors, actions, header, tables, onTakeOrder) { spaceId ->
                tables?.onOpenSpace?.invoke(spaceId)
                showsTables = true
            }
        }
    }
}

@Composable
private fun TablesView(
    colors: OperationalColors,
    onBack: () -> Unit,
    board: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(colors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AdminRoundButton(Icons.AutoMirrored.Outlined.ArrowBack, "Volver a pedidos", colors, onBack)
            Text("Mesas", color = colors.textPrimary, fontWeight = FontWeight.Black, fontSize = 22.sp)
        }
        Box(Modifier.weight(1f)) { board() }
    }
}

@Composable
private fun OrdersPage(
    state: AdminUiState,
    colors: OperationalColors,
    actions: AdminActions,
    header: AdminHeader,
    tables: AdminOrdersTables?,
    onTakeOrder: (() -> Unit)?,
    onOpenTable: (Int) -> Unit,
) {
    var open by remember { mutableStateOf<OrderDetail?>(null) }
    var mode by rememberSaveable { mutableStateOf(OrdersMode.ORDERS) }
    val agendaOn = mode == OrdersMode.AGENDA && state.takesHomeOrders
    LaunchedEffect(agendaOn) { if (agendaOn) actions.onRefreshAgenda() }
    // Prender las entregas en Mi tienda cambia el estado operativo: sin releerlo, la Agenda no salía (EQUIPO-50).
    LaunchedEffect(Unit) {
        actions.onRefreshHome()
        tables?.onRefresh?.invoke()
    }
    val photos =
        remember(state.catalog) {
            state.catalog
                ?.products
                .orEmpty()
                .associate { it.id to it.imageUrl }
        }
    val awaiting =
        if (state.orderFilter == AdminOrderFilter.ACTIVE) {
            state.orders.filter { it.summary.state == OrderState.AWAITING_ACCEPTANCE }
        } else {
            emptyList()
        }
    val unpaidBySpace =
        remember(state.unpaid) {
            state.unpaid
                ?.accounts
                .orEmpty()
                .associateBy { it.spaceId }
        }
    AdminPage(
        caption = if (agendaOn) "Administración · Lo que entregas" else "Administración · Hoy",
        title = "Pedidos",
        colors = colors,
        trailing = { header.avatar() },
    ) {
        val boardTables = tables?.tables.orEmpty()
        if (boardTables.isNotEmpty() && !agendaOn) {
            item(key = "mesas") {
                Column(Modifier.adminRise(1).padding(top = 14.dp)) {
                    AdminCapsLabel("Mesas", colors, Modifier.padding(bottom = 8.dp))
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        boardTables.forEach { table ->
                            TableTile(
                                table,
                                unpaidBySpace[table.space.id]?.total,
                                colors,
                            ) { onOpenTable(table.space.id) }
                        }
                    }
                }
            }
        }
        item(key = "chips") {
            Row(
                Modifier
                    .adminRise(2)
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val activeCount =
                    if (state.orderFilter == AdminOrderFilter.ACTIVE &&
                        !state.ordersLoading
                    ) {
                        state.orders.size
                    } else {
                        null
                    }
                AdminFilterChip(
                    label = activeCount?.let { "Activos · $it" } ?: "Activos",
                    selected = mode == OrdersMode.ORDERS && state.orderFilter == AdminOrderFilter.ACTIVE,
                    colors = colors,
                    onClick = {
                        mode = OrdersMode.ORDERS
                        actions.onSelectOrderFilter(AdminOrderFilter.ACTIVE)
                    },
                )
                if (state.takesHomeOrders) {
                    AwaitingChip(awaiting.size, mode == OrdersMode.AWAITING, colors) {
                        mode = OrdersMode.AWAITING
                        actions.onSelectOrderFilter(AdminOrderFilter.ACTIVE)
                    }
                }
                AdminOrderFilter.entries.filter { it != AdminOrderFilter.ACTIVE }.forEach { filter ->
                    AdminFilterChip(
                        label = filter.label,
                        selected = mode == OrdersMode.ORDERS && state.orderFilter == filter,
                        colors = colors,
                        onClick = {
                            mode = OrdersMode.ORDERS
                            actions.onSelectOrderFilter(filter)
                        },
                    )
                }
                if (state.takesHomeOrders) {
                    AdminFilterChip(label = "Agenda", selected = agendaOn, colors = colors, onClick = {
                        mode = OrdersMode.AGENDA
                    })
                }
            }
        }
        item(key = "separacion") { Box(Modifier.height(12.dp)) }
        when {
            agendaOn -> agendaItems(state, colors, actions.onRefreshAgenda) { open = it }
            mode == OrdersMode.AWAITING ->
                orderItems(state, awaiting, colors, actions, photos, OrdersMode.AWAITING, onTakeOrder) { open = it }
            else ->
                orderItems(
                    state,
                    state.orders,
                    colors,
                    actions,
                    photos,
                    OrdersMode.ORDERS,
                    onTakeOrder,
                ) { open = it }
        }
    }
    open?.let { order ->
        OrderSheet(
            order = order,
            photos = photos,
            colors = colors,
            busy = state.saving,
            error = state.notice?.takeIf { state.noticeIsError },
            onHomeAction = { action -> actions.onHomeAction(order, action) { updated -> open = updated } },
        ) { open = null }
    }
}

/** «Por aceptar» con su cuenta en coral: los activos que esperan que el negocio diga sí o no. */
@Composable
private fun AwaitingChip(
    count: Int,
    selected: Boolean,
    colors: OperationalColors,
    onClick: () -> Unit,
) {
    val haptics = rememberVaiinillaHaptics()
    val bg =
        com.vaiinilla.app.ui.components.animateSelectionColor(
            if (selected) colors.textPrimary else colors.cardBackground,
            "por-aceptar",
        )
    Row(
        Modifier
            .physicalPress {
                if (!selected) haptics.selection()
                onClick()
            }.height(36.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, if (selected) colors.textPrimary else colors.cardBorder, CircleShape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "Por aceptar",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) colors.background else colors.textSecondary,
            maxLines = 1,
        )
        if (count > 0) {
            Box(
                Modifier.size(18.dp).clip(CircleShape).background(AdminCoral),
                contentAlignment = Alignment.Center,
            ) { Text("$count", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White) }
        }
    }
}

/** Una mesa del marco 34: su nombre y lo que pasa en ella, con el borde del color de lo urgente. */
@Composable
private fun TableTile(
    table: BoardTable,
    unpaidTotal: String?,
    colors: OperationalColors,
    onClick: () -> Unit,
) {
    val owes = unpaidTotal?.toBigDecimalOrNull()?.signum() == 1
    val (label, tint, highlighted) =
        when {
            table.call != null -> Triple("Te llaman", colors.coralText, true)
            table.orders.any { it.state == OrderState.READY } -> Triple("Listo para llevar", colors.accentInk, true)
            owes -> Triple("${moneyLabel(unpaidTotal.orEmpty())} sin pagar", Yolk, true)
            table.orders.isNotEmpty() -> Triple("Preparando", colors.accentInk, false)
            table.availability?.state?.let { it != SpaceAvailabilityState.LIBRE } == true ->
                Triple("Ocupada", colors.textSecondary, false)
            else -> Triple("Libre", colors.textMuted, false)
        }
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .physicalPress(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "${table.space.name}, $label" }
            .width(78.dp)
            .height(70.dp)
            .clip(shape)
            .background(colors.cardBackground)
            .border(1.dp, if (highlighted) tint.copy(alpha = 0.7f) else colors.cardBorder, shape)
            .padding(horizontal = 11.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            table.space.name,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = tint, maxLines = 2, lineHeight = 13.sp)
    }
}

/** La lista del filtro elegido, en la misma columna que el encabezado. */
private fun LazyListScope.orderItems(
    state: AdminUiState,
    orders: List<OrderDetail>,
    colors: OperationalColors,
    actions: AdminActions,
    photos: Map<Int, String?>,
    mode: OrdersMode,
    onTakeOrder: (() -> Unit)?,
    onOpen: (OrderDetail) -> Unit,
) {
    val error = state.ordersError
    if (error != null && orders.isEmpty()) {
        item(key = "error") { AdminLoadError(error, colors, actions.onRefreshOrders) }
    } else if (!state.ordersLoading && orders.isEmpty()) {
        item(key = "vacio") {
            val (title, message) = emptyCopy(mode, state.orderFilter)
            val takeOrder =
                onTakeOrder != null && mode == OrdersMode.ORDERS && state.orderFilter == AdminOrderFilter.ACTIVE
            AdminEmptyState(
                icon = Icons.Outlined.ConfirmationNumber,
                title = title,
                message = message,
                colors = colors,
                action = if (takeOrder) "Tomar un pedido" else null,
                onAction = if (takeOrder) onTakeOrder else null,
            )
        }
    }
    items(orders, key = { it.summary.id }) { order ->
        OrderRowCard(
            order = order,
            photo = order.items.firstOrNull()?.let { photos[it.productId] },
            colors = colors,
            modifier = Modifier.animateItem().padding(vertical = 4.dp),
        ) { onOpen(order) }
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
    } else if (state.ordersCursor != null && mode == OrdersMode.ORDERS) {
        item(key = "mas") {
            AdminSecondaryButton(
                title = "Ver más pedidos",
                colors = colors,
                modifier = Modifier.padding(top = 8.dp),
                onClick = actions.onLoadMoreOrders,
            )
        }
    }
}

private fun emptyCopy(
    mode: OrdersMode,
    filter: AdminOrderFilter,
): Pair<String, String> =
    when {
        mode == OrdersMode.AWAITING ->
            "Nada por aceptar" to
                "Los encargos y pedidos a domicilio nuevos esperan aquí tu sí."
        filter == AdminOrderFilter.ACTIVE -> "Sin pedidos activos" to "Cuando alguien pida, lo ves aquí al momento."
        filter == AdminOrderFilter.DELIVERED -> "Nada entregado todavía" to "Lo que entregues hoy sale en esta lista."
        filter == AdminOrderFilter.INCIDENTS ->
            "Sin incidencias" to
                "Aquí salen los cancelados y los que nadie recogió."
        else -> "Sin pedidos" to "Cuando haya pedidos, salen en esta lista."
    }

/** Un pedido del marco 34: foto, folio con quién es, qué lleva, el total y su estado. */
@Composable
private fun OrderRowCard(
    order: OrderDetail,
    photo: String?,
    colors: OperationalColors,
    modifier: Modifier,
    onOpen: () -> Unit,
) {
    val summary = order.summary
    val shape = RoundedCornerShape(20.dp)
    val faded = summary.state == OrderState.DELIVERED || summary.state.isTerminalWithoutDelivery
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .alpha(if (faded) 0.8f else 1f)
                .physicalPress(onClick = onOpen)
                .semantics(mergeDescendants = true) {
                    contentDescription = "Pedido ${summary.folio}, ${whoLabel(order)}, ${summary.state.label}"
                }.clip(shape)
                .background(colors.cardBackground)
                .border(
                    1.dp,
                    if (summary.state == OrderState.AWAITING_ACCEPTANCE) Yolk.copy(alpha = 0.6f) else colors.cardBorder,
                    shape,
                ).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AdminThumb(photo, order.items.firstOrNull()?.productName ?: "Pedido", colors, size = 46.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "#${summary.folio} · ${whoLabel(order)}",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                orderSummaryLine(order),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                moneyLabel(summary.total),
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (summary.state == OrderState.DELIVERED) colors.textSecondary else colors.textPrimary,
            )
            AdminBadge(statusLabel(order), colors, statusTone(summary.state))
        }
    }
}

private fun orderSummaryLine(order: OrderDetail): String {
    val summary = order.summary
    val products = order.items.joinToString(", ") { "${it.quantity} × ${it.productName}" }
    val how =
        when {
            summary.destination == OrderDestination.HOME_DELIVERY -> "A domicilio"
            summary.paymentPending -> "por cobrar"
            else -> chargedPaymentLabel(summary) ?: summary.paymentMethod.label
        }
    return listOfNotNull(products.ifBlank { null }, how, timeLabel(summary.createdAt)).joinToString(" · ")
}

private fun statusLabel(order: OrderDetail): String {
    val state = order.summary.state
    return when {
        state == OrderState.AWAITING_ACCEPTANCE -> "Por aceptar"
        state == OrderState.PREPARING || state == OrderState.PAID -> "En cocina"
        order.summary.paymentPending && !state.isTerminalWithoutDelivery && state != OrderState.DELIVERED ->
            "Por cobrar"
        else -> state.label
    }
}

private fun statusTone(state: OrderState): AdminBadgeTone =
    when {
        state.isTerminalWithoutDelivery -> AdminBadgeTone.DANGER
        state == OrderState.DELIVERED -> AdminBadgeTone.NEUTRAL
        state == OrderState.READY || state == OrderState.ON_THE_WAY -> AdminBadgeTone.OK
        else -> AdminBadgeTone.WARN
    }

@Composable
private fun OrderSheet(
    order: OrderDetail,
    photos: Map<Int, String?>,
    colors: OperationalColors,
    busy: Boolean = false,
    error: String? = null,
    onHomeAction: (HomeOrderAction) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val summary = order.summary
    AdminSheet(title = "Pedido #${summary.folio}", colors = colors, busy = busy, onDismiss = onDismiss) {
        // El aviso de error del panel queda detrás de la hoja: sin esto, «Aceptar encargo» fallaba en silencio.
        AdminSheetError(error)
        HomeOrderActionsCard(order, busy, colors, onHomeAction)
        HomeDeliveryAdminCard(order, colors)
        AdminCard(colors) {
            AdminValueRow("Estado", HomeOrderBoard.stateLabel(order), colors)
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
                AdminRow(
                    title = "${item.quantity} × ${item.productName}",
                    colors = colors,
                    detail = details.joinToString("\n").ifBlank { null },
                    leading = { AdminThumb(photos[item.productId], item.productName, colors) },
                ) { AdminRowValue(moneyLabel(item.subtotal), colors) }
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
        order.summary.destination == OrderDestination.HOME_DELIVERY -> OrderDestination.HOME_DELIVERY.label
        else -> null
    }

private fun whoLabel(order: OrderDetail): String? =
    order.customerName?.takeIf { it.isNotBlank() } ?: order.user?.name?.takeIf { it.isNotBlank() }

private val ArrivalFormat = DateTimeFormatter.ofPattern("HH:mm")

private val TimeFormat = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.forLanguageTag("es-MX"))

private fun timeLabel(iso: String): String? =
    runCatching { TimeFormat.format(Instant.parse(iso).atZone(BusinessTime.zone)) }.getOrNull()
