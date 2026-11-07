package com.vaiinilla.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.SoupKitchen
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.core.venue.LocalVenueContext
import com.vaiinilla.app.domain.mode.RestrictedMode
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderItem
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.SpaceCopy
import com.vaiinilla.app.domain.model.activeItems
import com.vaiinilla.app.domain.model.offlineCaptureLabel
import com.vaiinilla.app.domain.model.skipsKitchen
import com.vaiinilla.app.ui.components.ASSISTANT_ANCHOR_KITCHEN_CARD
import com.vaiinilla.app.ui.components.OperationalAssistantHost
import com.vaiinilla.app.ui.components.OperationalAssistantPalette
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.SlidingSegments
import com.vaiinilla.app.ui.components.ToastPill
import com.vaiinilla.app.ui.components.VaiinillaAssistantButton
import com.vaiinilla.app.ui.components.animateSelectionColor
import com.vaiinilla.app.ui.components.assistantAnchor
import com.vaiinilla.app.ui.components.assistantKitchenReadyAnchor
import com.vaiinilla.app.ui.components.assistantKitchenStartAnchor
import com.vaiinilla.app.ui.components.assistantQueueOrderAnchor
import com.vaiinilla.app.ui.components.kitchenAssistantGuides
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.rememberAssistantAnchorRegistry
import com.vaiinilla.app.ui.components.rememberOperationalAssistantController
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.counter.TicketAllergyBand
import com.vaiinilla.app.ui.operational.OperationalUiState
import com.vaiinilla.app.ui.operational.QueuePresentation
import com.vaiinilla.app.ui.operational.WaiterBoardLink
import com.vaiinilla.app.ui.operational.operationalLink
import com.vaiinilla.app.ui.operational.queuePresentation
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.hotel.staff.HotelKitchenCard
import com.vaiinilla.app.ui.hotel.staff.HotelKitchenHeader
import com.vaiinilla.app.ui.theme.LocalVaiinillaThemeMode
import com.vaiinilla.app.ui.theme.LocalVaiinillaThemeModeChanger
import kotlinx.coroutines.delay

/** Las tres pestañas de Cocina del lienzo de Doop (K01, K02): lo que llegó, lo que está en la estufa y lo listo. */
internal enum class KitchenTab(
    val label: String,
) {
    NEW("Nuevas"),
    PREPARING("Preparando"),
    READY("Listas"),
}

internal fun kitchenTabOf(order: OrderDetail): KitchenTab =
    when (order.summary.state) {
        OrderState.PREPARING -> KitchenTab.PREPARING
        OrderState.READY -> KitchenTab.READY
        else -> KitchenTab.NEW
    }

/**
 * Lo listo ya no es trabajo de Cocina (W08) y no está en la fila, pero se ve en su pestaña hasta que se entrega
 * para que la cocina sepa qué salió. Un pedido solo de barra nunca pasó por Cocina y no aparece.
 */
internal fun kitchenReadyOrders(orders: List<OrderDetail>): List<OrderDetail> =
    orders
        .filter { it.summary.state == OrderState.READY && !it.skipsKitchen }
        .sortedBy { it.summary.createdAt }

/** Sin elegir pestaña, Cocina abre en la primera que tiene trabajo; si no hay nada, en Nuevas. */

/** Un producto sumado de todas las comandas por hacer: «4 · Crepa de cajeta · #14 ×2 · #16 · #18». */
internal data class KitchenProductTotal(
    val name: String,
    val quantity: Int,
    val folios: List<Pair<Int, Int>>,
)

/** Lo que Cocina tiene que hacer, sumado por producto y sus opciones, de lo que más se repite a lo que menos. */
internal fun kitchenProductTotals(orders: List<OrderDetail>): List<KitchenProductTotal> =
    orders
        .flatMap { order ->
            order.activeItems
                .filter { it.preparationStation == PreparationStation.KITCHEN }
                .map { item -> Triple(kitchenProductName(item), order.summary.folio, item.quantity) }
        }.groupBy { it.first }
        .map { (name, lines) ->
            KitchenProductTotal(
                name = name,
                quantity = lines.sumOf { it.third },
                folios =
                    lines
                        .groupBy { it.second }
                        .map { (folio, perFolio) -> folio to perFolio.sumOf { it.third } },
            )
        }.sortedByDescending { it.quantity }

private fun kitchenProductName(item: OrderItem): String =
    (listOf(item.productName) + item.options.map { it.name }).joinToString(" · ")

/** Cocina ve en grande lo suyo; lo que se entrega en Caja queda en una línea aparte. */
internal fun kitchenSplitItems(items: List<OrderItem>): Pair<List<OrderItem>, List<OrderItem>> {
    val (kitchen, other) = items.partition { it.preparationStation == PreparationStation.KITCHEN }
    return if (kitchen.isEmpty()) items to emptyList() else kitchen to other
}

/**
 * Con `cocina_prepara_sin_cobrar` apagado, el servidor rechaza empezar un pedido por cobrar (409
 * ORDER_PAYMENT_PENDING): la comanda se ve, pero espera a que Caja la cobre (R14).
 */
internal fun kitchenWaitsForCashier(
    order: OrderDetail,
    kitchenPreparesUnpaid: Boolean,
): Boolean = !kitchenPreparesUnpaid && order.summary.state == OrderState.PENDING_PAYMENT

internal fun kitchenDefaultTab(queue: List<OrderDetail>): KitchenTab =
    when {
        queue.any { kitchenTabOf(it) == KitchenTab.NEW } -> KitchenTab.NEW
        queue.any { kitchenTabOf(it) == KitchenTab.PREPARING } -> KitchenTab.PREPARING
        else -> KitchenTab.NEW
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitchenOperationalScreen(
    state: OperationalUiState,
    onBack: () -> Unit,
    onStart: (orderId: String, version: Int) -> Unit,
    onReady: (orderId: String, version: Int) -> Unit,
    onReject: (orderId: String, version: Int, reason: String) -> Unit = { _, _, _ -> },
    /** Quitar un solo artículo de la comanda (Cocina no puede hacerlo con el último). */
    onRejectItem: (orderId: String, itemId: Int, version: Int, reason: String) -> Unit = { _, _, _, _ -> },
    onChangeMode: (() -> Unit)? = null,
    restrictedMode: RestrictedMode? = null,
    assistantUserKey: String = "kitchen",
    /** Cocina de una escuela: "Para mañana" (padres e hijos); null fuera de una escuela. */
    reservedEntry: (@Composable () -> Unit)? = null,
    onActionNoticed: () -> Unit = {},
    onDismissError: () -> Unit = {},
    onRefresh: () -> Unit = {},
    placeName: String = "",
    /** N03: apagar o prender un producto desde Cocina; null si esta Cocina no lo ofrece. */
    onToggleProductAvailable: ((productId: Int, available: Boolean) -> Unit)? = null,
    onLoadCatalog: () -> Unit = {},
) {
    val venue = LocalVenueContext.current
    val colors = rememberOperationalColors()
    val themeMode = LocalVaiinillaThemeMode.current
    val themeChanger = LocalVaiinillaThemeModeChanger.current
    val haptics = rememberVaiinillaHaptics()
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var assistantPulse by remember { mutableIntStateOf(0) }
    val assistantRegistry = rememberAssistantAnchorRegistry()
    val assistantController = rememberOperationalAssistantController(assistantUserKey, OperationalRole.KITCHEN)
    val assistantFocusRequester = remember { FocusRequester() }

    var selectedOrderId by remember { mutableStateOf<String?>(null) }
    var chosenTab by rememberSaveable { mutableStateOf<KitchenTab?>(null) }
    // S01: en un hotel la cocina es un solo tablero de habitaciones (HotelKitchenBoard); sin pestañas ni vista por producto.
    val hotelBoard = venue.capabilities.staffWorksRooms
    var byProductChosen by rememberSaveable { mutableStateOf(false) }
    val byProduct = byProductChosen && !hotelBoard
    // N02: «lista» espera 10 s para poder deshacerla sin servidor; al salir de la pantalla se manda ya.
    val pendingReady = remember { mutableStateMapOf<String, Pair<Int, Long>>() }
    val currentOnReady by rememberUpdatedState(onReady)
    pendingReady.toList().forEach { (id, entry) ->
        key(id) {
            LaunchedEffect(id, entry.second) {
                delay((entry.second - System.currentTimeMillis()).coerceAtLeast(0))
                if (pendingReady.remove(id) != null) currentOnReady(id, entry.first)
            }
        }
    }
    // Salir por «Cambiar de modo» o Atrás limpia el rol antes de que la pantalla se vaya, y sin rol el servidor
    // ya no recibe la marca: se manda antes de salir (B04).
    val flushPendingReady = {
        pendingReady.toList().forEach { (id, entry) -> currentOnReady(id, entry.first) }
        pendingReady.clear()
    }
    BackHandler(enabled = pendingReady.isNotEmpty()) {
        flushPendingReady()
        onBack()
    }
    DisposableEffect(Unit) {
        onDispose { flushPendingReady() }
    }
    var rejectingOrderId by remember { mutableStateOf<String?>(null) }
    var rejectingItem by remember { mutableStateOf<Pair<String, OrderItem>?>(null) }
    var moreOrderId by remember { mutableStateOf<String?>(null) }
    var soldOutOpen by rememberSaveable { mutableStateOf(false) }
    var lastTurnedOff by remember { mutableStateOf<String?>(null) }
    val queueOrders = kitchenQueue(state.orders)
    val readyOrders = kitchenReadyOrders(state.orders)
    val activeOrder = kitchenActiveOrder(queueOrders, selectedOrderId)
    // Tocar el push de una comanda abre su pestaña.
    LaunchedEffect(state.selectedOrderId) {
        queueOrders.firstOrNull { it.summary.id == state.selectedOrderId }?.let { order ->
            selectedOrderId = order.summary.id
            chosenTab = kitchenTabOf(order)
        }
    }
    val tab = chosenTab ?: kitchenDefaultTab(queueOrders)
    val newOrders = queueOrders.filter { kitchenTabOf(it) == KitchenTab.NEW }
    val preparingOrders = queueOrders.filter { kitchenTabOf(it) == KitchenTab.PREPARING }
    val tabOrders =
        if (hotelBoard) {
            queueOrders
        } else {
            when (tab) {
                KitchenTab.NEW -> newOrders
                KitchenTab.PREPARING -> preparingOrders
                KitchenTab.READY -> readyOrders
            }
        }
    val upcomingOrders = queueOrders.filterNot { it.summary.id == activeOrder?.summary?.id }
    val orderState = activeOrder?.summary?.state ?: OrderState.PAID
    val isPreparing = orderState == OrderState.PREPARING
    val isReady = orderState == OrderState.READY || orderState == OrderState.DELIVERED
    val nextGuidableOrder = upcomingOrders.firstOrNull()
    val kitchenQueue = queuePresentation(queueOrders.isNotEmpty(), state.lastRefreshOk)
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2_200)
            toastMessage = null
        }
    }
    // La confirmación llega cuando el servidor aceptó el cambio, no al tocar el botón.
    LaunchedEffect(state.actionNotice) {
        state.actionNotice?.let {
            haptics.success()
            toastMessage = it
            onActionNoticed()
        }
    }
    val kitchenGuides =
        kitchenAssistantGuides(
            orderId = activeOrder?.summary?.id,
            orderFolio = activeOrder?.summary?.folio,
            isPreparing = isPreparing,
            isReady = isReady,
            nextOrderId = nextGuidableOrder?.summary?.id,
            nextOrderFolio = nextGuidableOrder?.summary?.folio,
        )
    val kitchenAssistantPalette =
        OperationalAssistantPalette(
            background = colors.background,
            surface = colors.cardBackground,
            surface2 = colors.cardInner,
            ink = colors.textPrimary,
            muted = colors.textSecondary,
            line = colors.cardBorder,
            lime = colors.accentLime,
            limeInk = colors.accentInk,
            isDark = colors.isDark,
        )
    LaunchedEffect(state.orders) {
        state.orders.forEach { order ->
            when (order.summary.state) {
                OrderState.PREPARING -> assistantController.onStateChanged("comanda-preparando:${order.summary.id}")
                OrderState.READY, OrderState.DELIVERED ->
                    assistantController.onStateChanged("comanda-lista:${order.summary.id}")
                else -> Unit
            }
        }
    }
    val canAct = restrictedMode != RestrictedMode.READ_ONLY

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            if (event.changes.any { it.pressed && !it.previousPressed }) {
                                assistantPulse += 1
                            }
                        }
                    }
                },
    ) {
        val unreadableOrders = rememberUnreadableOrders()
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "kitchen-top") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    KitchenStatusPill(
                        link = operationalLink(state.lastRefreshOk),
                        inQueue = queueOrders.size,
                        colors = colors,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (assistantController.activeGuide == null) {
                            VaiinillaAssistantButton(
                                onClick = {
                                    haptics.selection()
                                    assistantController.open()
                                },
                                reactionSignal = assistantPulse,
                                isDarkTheme = colors.isDark,
                                hasPendingFirstSteps = false,
                                modifier = Modifier.focusRequester(assistantFocusRequester),
                                touchSize = 48.dp,
                            )
                        }
                        StaffAvatarMenu(
                            initials = "CK",
                            modeLabel = "Cocina",
                            placeName = placeName,
                            colors = colors,
                            themeMode = themeMode,
                            onThemeChange =
                                themeChanger?.let { change ->
                                    { mode ->
                                        haptics.selection()
                                        change(mode)
                                    }
                                },
                            onChangeMode = {
                                haptics.impact()
                                flushPendingReady()
                                onChangeMode?.invoke() ?: onBack()
                            },
                        )
                    }
                }
            }

            if (state.lastRefreshOk == false && queueOrders.isNotEmpty()) {
                item(key = "kitchen-offline") { StaffOfflineBanner(offline = true, modifier = Modifier.animateItem()) }
            }

            if (unreadableOrders > 0) {
                item(key = "kitchen-update-app") {
                    StaffUpdateAppBanner(count = unreadableOrders, modifier = Modifier.animateItem())
                }
            }

            if (hotelBoard) {
                item(key = "kitchen-title") {
                    HotelKitchenHeader(
                        count = queueOrders.size,
                        onSoldOut =
                            if (onToggleProductAvailable != null && canAct) {
                                {
                                    haptics.selection()
                                    lastTurnedOff = null
                                    onLoadCatalog()
                                    soldOutOpen = true
                                }
                            } else {
                                null
                            },
                    )
                }
            }

            if (!hotelBoard) item(key = "kitchen-title") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Comandas",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp,
                        color = colors.textPrimary,
                    )
                    if (onToggleProductAvailable != null && canAct) {
                        Text(
                            "¿Se acabó algo?",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.textPrimary,
                            modifier =
                                Modifier
                                    .physicalPress(scale = PhysicalPressScale.Small) {
                                        haptics.selection()
                                        lastTurnedOff = null
                                        onLoadCatalog()
                                        soldOutOpen = true
                                    }.clip(CircleShape)
                                    .background(colors.cardBackground)
                                    .border(1.dp, colors.cardBorder, CircleShape)
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                }
            }

            if (!hotelBoard) item(key = "kitchen-view") {
                SlidingSegments(
                    labels = listOf("Por comanda", "Por producto"),
                    selectedIndex = if (byProduct) 1 else 0,
                    onSelect = { index ->
                        haptics.selection()
                        byProductChosen = index == 1
                    },
                    trackColor = colors.cardBackground,
                    indicatorColor = colors.textPrimary,
                    selectedContentColor = colors.background,
                    idleContentColor = colors.textSecondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (byProduct) {
                val todo = newOrders + preparingOrders
                item(key = "kitchen-products") {
                    val totals = remember(todo) { kitchenProductTotals(todo) }
                    if (totals.isEmpty()) {
                        KitchenEmptyCard(
                            tab = KitchenTab.NEW,
                            loading = kitchenQueue == QueuePresentation.Loading,
                            otherWork = false,
                            colors = colors,
                            modifier = Modifier.animateItem(),
                        )
                    } else {
                        KitchenProductList(
                            totals = totals,
                            ordersCount = todo.size,
                            colors = colors,
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }

            if (!byProduct && !hotelBoard) {
                item(key = "kitchen-tabs") {
                    val counts = listOf(newOrders.size, preparingOrders.size, readyOrders.size)
                    SlidingSegments(
                        labels = KitchenTab.entries.mapIndexed { i, t -> "${t.label} ${counts[i]}" },
                        selectedIndex = tab.ordinal,
                        onSelect = { index ->
                            haptics.selection()
                            chosenTab = KitchenTab.entries[index]
                        },
                        trackColor = colors.cardBackground,
                        indicatorColor = colors.textPrimary,
                        selectedContentColor = colors.background,
                        idleContentColor = colors.textSecondary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Sin comandas cargadas, la tarjeta de la fila ya muestra el error.
            state.errorMessage?.takeUnless { kitchenQueue == QueuePresentation.Error }?.let { message ->
                item(key = "kitchen-error") {
                    OperationalErrorCard(
                        message = message,
                        colors = colors,
                        onDismiss = onDismissError,
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            if (reservedEntry != null) {
                item(key = "kitchen-reserved") {
                    Box(modifier = Modifier.animateItem()) { reservedEntry() }
                }
            }

            if (!byProduct && tabOrders.isEmpty()) {
                item(key = "kitchen-empty-${tab.name}") {
                    when {
                        tab != KitchenTab.READY && kitchenQueue == QueuePresentation.Error ->
                            OperationalErrorCard(
                                title = "No se pudieron cargar las comandas",
                                message = state.errorMessage ?: "Revisa la conexión e inténtalo otra vez.",
                                colors = colors,
                                onRetry = onRefresh,
                            )
                        else ->
                            KitchenEmptyCard(
                                tab = tab,
                                loading = kitchenQueue == QueuePresentation.Loading,
                                otherWork = queueOrders.isNotEmpty(),
                                colors = colors,
                                modifier = Modifier.animateItem(),
                            )
                    }
                }
            }

            items(if (byProduct) emptyList() else tabOrders, key = { it.summary.id }) { order ->
                val orderTab = kitchenTabOf(order)
                val inKitchen = order.summary.state == OrderState.PAID || order.summary.state == OrderState.PREPARING
                // Cocina quita artículos de un pedido cobrado o en preparación; el último ya no se quita, se
                // rechaza el pedido entero. El servidor solo deja quitar lo que se prepara en Cocina.
                val canRemoveItems = canAct && order.activeItems.size > 1 && inKitchen && !state.acting
                // Sin cobrar, el servidor no deja rechazar: solo cobrado o en preparación.
                val canReject = canAct && inKitchen
                val first = order.summary.id == tabOrders.first().summary.id
                if (hotelBoard) {
                    HotelKitchenCard(
                        order = order,
                        acting = state.acting,
                        canAct = canAct,
                        readyDeadline = pendingReady[order.summary.id]?.second,
                        onPrimary = {
                            haptics.impact()
                            selectedOrderId = order.summary.id
                            if (orderTab == KitchenTab.PREPARING) {
                                pendingReady[order.summary.id] =
                                    order.summary.version to System.currentTimeMillis() + KITCHEN_UNDO_MS
                            } else {
                                onStart(order.summary.id, order.summary.version)
                            }
                        },
                        onUndoReady = {
                            haptics.selection()
                            pendingReady.remove(order.summary.id)
                        },
                        onMore = if (canReject) ({ moreOrderId = order.summary.id }) else null,
                        modifier =
                            Modifier
                                .animateItem()
                                .assistantAnchor(assistantRegistry, assistantQueueOrderAnchor(order.summary.id)),
                    )
                    return@items
                }
                KitchenOrderCard(
                    order = order,
                    acting = state.acting,
                    canAct = canAct,
                    waitsForCashier = kitchenWaitsForCashier(order, state.kitchenPreparesUnpaid),
                    canRemove = { item -> canRemoveItems && item.preparationStation == PreparationStation.KITCHEN },
                    onRemoveItem = { item -> rejectingItem = order.summary.id to item },
                    onPrimary = {
                        haptics.impact()
                        selectedOrderId = order.summary.id
                        if (orderTab == KitchenTab.PREPARING) {
                            pendingReady[order.summary.id] =
                                order.summary.version to System.currentTimeMillis() + KITCHEN_UNDO_MS
                        } else {
                            onStart(order.summary.id, order.summary.version)
                        }
                    },
                    onMore = if (canReject) ({ moreOrderId = order.summary.id }) else null,
                    readyDeadline = pendingReady[order.summary.id]?.second,
                    onUndoReady = {
                        haptics.selection()
                        pendingReady.remove(order.summary.id)
                    },
                    primaryAnchor =
                        if (orderTab == KitchenTab.PREPARING) {
                            assistantKitchenReadyAnchor(order.summary.id)
                        } else {
                            assistantKitchenStartAnchor(order.summary.id)
                        },
                    modifier =
                        Modifier
                            .animateItem()
                            .assistantAnchor(assistantRegistry, assistantQueueOrderAnchor(order.summary.id))
                            .then(
                                if (first) {
                                    Modifier.assistantAnchor(assistantRegistry, ASSISTANT_ANCHOR_KITCHEN_CARD)
                                } else {
                                    Modifier
                                },
                            ),
                    registry = assistantRegistry,
                )
            }
        }

        ToastPill(
            message = toastMessage,
            containerColor = colors.textPrimary,
            contentColor = colors.background,
            badgeColor = colors.accentLime,
            badgeContentColor = colors.accentInk,
        )

        OperationalAssistantHost(
            controller = assistantController,
            registry = assistantRegistry,
            guides = kitchenGuides,
            manualTitle = "Manual de Cocina",
            palette = kitchenAssistantPalette,
            buttonFocusRequester = assistantFocusRequester,
            modifier = Modifier.fillMaxSize(),
        )
    }

    if (soldOutOpen && onToggleProductAvailable != null) {
        KitchenSoldOutSheet(
            products = state.catalog?.products,
            failed = state.catalogFailed,
            busy = state.catalogBusy,
            queue = queueOrders,
            lastTurnedOff = lastTurnedOff,
            errorMessage = state.errorMessage,
            colors = colors,
            onToggle = { product, available ->
                lastTurnedOff = if (available) null else product.name
                onToggleProductAvailable(product.id, available)
            },
            onRetry = onLoadCatalog,
            onDismiss = { soldOutOpen = false },
        )
    }

    val moreOrder = queueOrders.firstOrNull { it.summary.id == moreOrderId }
    if (moreOrder != null) {
        KitchenMoreSheet(
            folio = moreOrder.summary.folio,
            colors = colors,
            onDismiss = { moreOrderId = null },
            onReject = {
                moreOrderId = null
                rejectingOrderId = moreOrder.summary.id
            },
        )
    }

    val itemToReject = rejectingItem
    val itemOrder = queueOrders.firstOrNull { it.summary.id == itemToReject?.first }
    if (itemToReject != null && itemOrder != null) {
        KitchenRejectDialog(
            folio = itemOrder.summary.folio,
            itemName = itemToReject.second.productName,
            colors = colors,
            onDismiss = { rejectingItem = null },
            onConfirm = { reason ->
                rejectingItem = null
                haptics.impact()
                onRejectItem(itemOrder.summary.id, itemToReject.second.id, itemOrder.summary.version, reason)
            },
        )
    }

    val rejectingOrder = queueOrders.firstOrNull { it.summary.id == rejectingOrderId }
    if (rejectingOrder != null) {
        KitchenRejectDialog(
            folio = rejectingOrder.summary.folio,
            colors = colors,
            onDismiss = { rejectingOrderId = null },
            onConfirm = { reason ->
                rejectingOrderId = null
                haptics.impact()
                onReject(rejectingOrder.summary.id, rejectingOrder.summary.version, reason)
            },
        )
    }
}

/** «● Cocina · 5 en fila» en lima si está en vivo; sin conexión lo dice en coral para que nadie confíe en lo viejo. */
@Composable
private fun KitchenStatusPill(
    link: WaiterBoardLink,
    inQueue: Int,
    colors: OperationalColors,
) {
    val coral = LocalVaiinillaColors.current.coral
    val tint =
        animateSelectionColor(
            when (link) {
                WaiterBoardLink.Live -> colors.accentLime
                WaiterBoardLink.Connecting -> colors.textSecondary
                WaiterBoardLink.Offline -> coral
            },
            "kitchen-pill",
        )
    val label =
        when (link) {
            WaiterBoardLink.Offline -> "Sin conexión"
            WaiterBoardLink.Connecting -> "Conectando…"
            WaiterBoardLink.Live -> "Cocina"
        }
    Row(
        modifier =
            Modifier
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.16f))
                .padding(horizontal = 11.dp, vertical = 6.dp)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(tint))
        Text(
            "${label.uppercase()} · $inQueue EN FILA",
            color = tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.7.sp,
        )
    }
}

@Composable
private fun KitchenEmptyCard(
    tab: KitchenTab,
    loading: Boolean,
    otherWork: Boolean,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
) {
    val (title, body) =
        when {
            loading -> "Cargando comandas…" to null
            tab == KitchenTab.READY ->
                "Nada listo por ahora" to
                    "Lo que marques lista aparece aquí hasta que lo entreguen."
            otherWork && tab == KitchenTab.NEW ->
                "Sin comandas nuevas" to
                    "Lo que está en la estufa sigue en Preparando."
            otherWork -> "Nada en preparación" to "Empieza una de las Nuevas."
            else -> {
                KitchenAllClear(colors, modifier)
                return
            }
        }
    Surface(
        modifier = modifier.fillMaxWidth().padding(top = 6.dp),
        shape = RoundedCornerShape(24.dp),
        color = colors.cardBackground,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, fontWeight = FontWeight.Black, fontSize = 18.sp, color = colors.textPrimary)
            body?.let {
                Text(it, fontSize = 13.sp, color = colors.textSecondary, textAlign = TextAlign.Center)
            }
        }
    }
}

/** K04 de Doop: sin nada en ninguna pestaña, la olla flota en el centro en vez de una tarjeta. */
@Composable
private fun KitchenAllClear(
    colors: OperationalColors,
    modifier: Modifier = Modifier,
) {
    val accent = colors.accentLime
    val bob =
        if (rememberReducedMotion()) {
            0f
        } else {
            val transition = rememberInfiniteTransition(label = "kitchen-all-clear")
            val y by transition.animateFloat(
                initialValue = 0f,
                targetValue = -6f,
                animationSpec = infiniteRepeatable(tween(1_200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "kitchen-all-clear-bob",
            )
            y
        }
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 72.dp, start = 30.dp, end = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .graphicsLayer { translationY = bob.dp.toPx() }
                    .size(120.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(colors.cardBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.SoupKitchen, contentDescription = null, tint = accent, modifier = Modifier.size(56.dp))
        }
        Text(
            "Todo al día",
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            color = colors.textPrimary,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            "No hay comandas pendientes. Cuando entre una, te llega un aviso aunque la pantalla esté apagada.",
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/**
 * La comanda en papel de K01 y K02: la alergia hasta arriba (Lalo la vio tarde cuando iba al fondo), el folio * grande, el tiempo con color y una sola acción. Rechazar todo vive en «Más opciones», lejos de Marcar lista.
 */
@Composable
private fun KitchenOrderCard(
    order: OrderDetail,
    acting: Boolean,
    canAct: Boolean,
    canRemove: (OrderItem) -> Boolean,
    onRemoveItem: (OrderItem) -> Unit,
    onPrimary: () -> Unit,
    onMore: (() -> Unit)?,
    readyDeadline: Long?,
    onUndoReady: () -> Unit,
    primaryAnchor: String,
    registry: com.vaiinilla.app.ui.components.AssistantAnchorRegistry,
    modifier: Modifier = Modifier,
    /** El negocio solo prepara lo cobrado y este pedido sigue por cobrar: no hay «Empezar a preparar» (R14). */
    waitsForCashier: Boolean = false,
) {
    val venue = LocalVenueContext.current
    val tab = kitchenTabOf(order)
    val summary = order.summary
    val place =
        buildList {
            add(SpaceCopy.staffDestination(venue, summary.destination, summary.space, summary.pickUpAtBar))
            staffCustomerName(order)?.let(::add)
            when {
                summary.state == OrderState.PENDING_PAYMENT && !summary.payAtEnd -> add("falta pagar")
                summary.payAtEnd && summary.paymentPending -> add("a la cuenta")
            }
        }.joinToString(" · ") + arrivalSuffix(order)
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = KitchenPaper) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 14.dp)) {
            TicketAllergyBand(order.user?.allergies.orEmpty())
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        "#${summary.folio}",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp,
                        color = KitchenInk,
                    )
                    Text(
                        place,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = KitchenMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    // Llegó al volver la red: probablemente ya se preparó con su folio provisional.
                    order.offlineCaptureLabel()?.let { label ->
                        Text(
                            "$label · revisa si ya se preparó",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = KitchenInk,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                KitchenAgeChip(createdAt = kitchenAgeStart(summary))
            }
            // Lo rechazado no se prepara: no aparece en la comanda.
            val (kitchenItems, cashierItems) = remember(order) { kitchenSplitItems(order.activeItems) }
            // N02: con varias cosas se palomea lo que ya salió; solo vive en este teléfono.
            val checkable = tab == KitchenTab.PREPARING && kitchenItems.size > 1
            var checked by rememberSaveable(summary.id) { mutableStateOf(setOf<Int>()) }
            if (checkable) {
                val done = kitchenCheckedCount(kitchenItems, checked)
                val progress by animateFloatAsState(
                    targetValue = done / kitchenItems.size.toFloat(),
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
                    label = "kitchen-check-progress",
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(KitchenLine),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(progress)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(KitchenInk),
                        )
                    }
                    Text(
                        "$done de ${kitchenItems.size}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = KitchenMuted,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }
            Column(modifier = Modifier.padding(top = 8.dp)) {
                kitchenItems.forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider(color = KitchenLine.copy(alpha = 0.6f))
                    KitchenItemLine(
                        item = item,
                        removable = canRemove(item),
                        onRemove = { onRemoveItem(item) },
                        checked = if (checkable) item.id in checked else null,
                        onToggle = {
                            checked = if (item.id in checked) checked - item.id else checked + item.id
                        },
                    )
                }
            }
            if (cashierItems.isNotEmpty()) {
                Text(
                    (if (summary.space != null) "Las lleva el mesero: " else "Lo de Caja va aparte: ") +
                        cashierItems.joinToString(", ") { "${it.quantity}× ${it.productName}" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = KitchenMuted,
                    modifier =
                        Modifier
                            .padding(top = 6.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(KitchenLine.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                )
            }
            KitchenNotesNote(order.kitchenNotes, ink = KitchenInk, muted = KitchenMuted)
            when (tab) {
                KitchenTab.READY ->
                    Text(
                        "Lista · espera a que la recojan",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = KitchenReadyInk,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                else ->
                    if (tab == KitchenTab.NEW && waitsForCashier) {
                        Text(
                            "Falta cobrar en Caja · se empieza cuando lo cobren",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = KitchenMuted,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    } else if (readyDeadline != null) {
                        KitchenUndoReady(
                            deadline = readyDeadline,
                            onUndo = onUndoReady,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    } else {
                        KitchenPrimaryButton(
                            label = if (tab == KitchenTab.PREPARING) "Marcar lista" else "Empezar a preparar",
                            lime = tab == KitchenTab.PREPARING,
                            enabled = canAct && !acting,
                            loading = acting,
                            modifier = Modifier.padding(top = 12.dp).assistantAnchor(registry, primaryAnchor),
                            onClick = onPrimary,
                        )
                    }
            }
            if (onMore != null) {
                HorizontalDivider(color = KitchenLine, modifier = Modifier.padding(top = 14.dp))
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .physicalPress(scale = PhysicalPressScale.Small, enabled = !acting, onClick = onMore)
                            .heightIn(min = 48.dp)
                            .semantics { contentDescription = "Más opciones de la comanda ${summary.folio}" },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Más opciones", fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, color = KitchenMuted)
                    Spacer(Modifier.width(6.dp))
                    MoreDots(KitchenMuted)
                }
            }
        }
    }
}

/** Tres puntos dibujados: la fuente no trae «⋯» en todos los teléfonos. */
@Composable
internal fun MoreDots(color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(3) { Box(Modifier.size(4.dp).clip(CircleShape).background(color)) }
    }
}

// El mismo papel que el ticket de Caja: las dos pantallas imprimen la misma comanda.
private val KitchenPaper = Color(0xFFF5ECDA)
private val KitchenInk = Color(0xFF1D1C18)
private val KitchenMuted = Color(0xFF6B6656)
private val KitchenLine = Color(0xFFD8CDB4)
private val KitchenReadyInk = Color(0xFF3B5310)
private val KitchenRemoveInk = Color(0xFF8A1D17)

/** N02: la barra de «Deshacer» mientras corren los 10 segundos antes de mandar «lista». */
@Composable
private fun KitchenUndoReady(
    deadline: Long,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(deadline) {
        while (now < deadline) {
            delay(200)
            now = System.currentTimeMillis()
        }
    }
    val left = ((deadline - now).coerceAtLeast(0) + 999) / 1000
    val progress = ((deadline - now).coerceAtLeast(0) / KITCHEN_UNDO_MS.toFloat()).coerceIn(0f, 1f)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(KitchenLine.copy(alpha = 0.6f))
                    .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
                    .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Marcada como lista",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = KitchenInk,
                modifier = Modifier.weight(1f),
            )
            CircularProgressIndicator(
                progress = { progress },
                color = KitchenInk,
                trackColor = KitchenLine,
                strokeWidth = 3.dp,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Deshacer",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = KitchenPaper,
                modifier =
                    Modifier
                        .clip(CircleShape)
                        .background(KitchenInk)
                        .physicalPress(scale = PhysicalPressScale.Small, onClick = onUndo)
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 18.dp, vertical = 13.dp),
            )
        }
        Text(
            "En $left s se avisa al cliente. Antes puedes regresarla a «Preparando».",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = KitchenMuted,
            modifier = Modifier.padding(top = 6.dp, start = 4.dp),
        )
    }
}

internal const val KITCHEN_UNDO_MS = 10_000L

/** Cuántas líneas ya se palomearon, de las que siguen en la comanda. */
internal fun kitchenCheckedCount(
    items: List<OrderItem>,
    checked: Set<Int>,
): Int = items.count { it.id in checked }

@Composable
private fun KitchenItemLine(
    item: OrderItem,
    removable: Boolean,
    onRemove: () -> Unit,
    checked: Boolean? = null,
    onToggle: () -> Unit = {},
) {
    val done = checked == true
    val inkColor by animateColorAsState(if (done) KitchenMuted else KitchenInk, label = "kitchen-item-ink")
    val strike = if (done) TextDecoration.LineThrough else TextDecoration.None
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (checked != null) {
                        Modifier
                            .toggleable(value = done, role = Role.Checkbox, onValueChange = { onToggle() })
                    } else {
                        Modifier
                    },
                ).padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (checked != null) {
            val box by animateColorAsState(if (done) KitchenInk else Color.Transparent, label = "kitchen-check")
            Box(
                modifier =
                    Modifier
                        .padding(end = 10.dp)
                        .size(24.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(box)
                        .border(2.dp, if (done) KitchenInk else KitchenLine, RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (done) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = KitchenPaper,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Text(
            "${item.quantity}×",
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = inkColor,
            textDecoration = strike,
            modifier = Modifier.width(34.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.productName,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = inkColor,
                textDecoration = strike,
            )
            val detail = item.options.joinToString(" · ") { it.name }
            if (detail.isNotEmpty()) {
                Text(detail, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = KitchenMuted)
            }
        }
        if (removable) {
            // «Quitar» sonaba a borrar sin más; «No hay» es lo que pasa en la cocina.
            Text(
                "No hay",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = KitchenRemoveInk,
                modifier =
                    Modifier
                        .padding(start = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .physicalPress(scale = PhysicalPressScale.Small, onClick = onRemove)
                        .semantics { contentDescription = "No hay ${item.productName}" }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun KitchenPrimaryButton(
    label: String,
    lime: Boolean,
    enabled: Boolean,
    loading: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = rememberOperationalColors()
    val fill = if (lime) colors.accentLime else KitchenInk
    val ink = if (lime) colors.accentInk else KitchenPaper
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .physicalPress(enabled = enabled, onClick = onClick)
                .height(56.dp)
                .clip(CircleShape)
                .background(if (enabled || loading) fill else KitchenLine),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = ink,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(22.dp).semantics { contentDescription = "Guardando la comanda" },
            )
        } else {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Black, color = if (enabled) ink else KitchenMuted)
        }
    }
}

/** «Más opciones» de una comanda: rechazar todo el pedido, con su propio paso de confirmación y motivo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KitchenMoreSheet(
    folio: Int,
    colors: OperationalColors,
    onDismiss: () -> Unit,
    onReject: () -> Unit,
) {
    val coral = LocalVaiinillaColors.current.coral
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 22.dp,
                    ).navigationBarsPadding()
                    .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Comanda #$folio", fontSize = 22.sp, fontWeight = FontWeight.Black, color = colors.textPrimary)
            Text(
                "Si solo falta un platillo, usa «No hay» en esa línea; lo demás sigue.",
                fontSize = 13.sp,
                color = colors.textSecondary,
            )
            Row(
                modifier =
                    Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .physicalPress(scale = PhysicalPressScale.Small, onClick = onReject)
                        .height(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(coral.copy(alpha = 0.14f))
                        .border(1.dp, coral.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Outlined.Close, contentDescription = null, tint = coral, modifier = Modifier.size(20.dp))
                Text("Rechazar todo el pedido", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = coral)
            }
        }
    }
}

@Composable
private fun KitchenProductList(
    totals: List<KitchenProductTotal>,
    ordersCount: Int,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Todo lo que falta, sumado: haces las crepas de una vez en vez de ir comanda por comanda.",
            fontSize = 13.sp,
            color = colors.textSecondary,
        )
        Text(
            "POR HACER · $ordersCount ${if (ordersCount == 1) "COMANDA" else "COMANDAS"}",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.4.sp,
            color = colors.textSecondary,
        )
        Surface(shape = RoundedCornerShape(22.dp), color = KitchenPaper, modifier = Modifier.fillMaxWidth()) {
            Column {
                totals.forEachIndexed { index, total ->
                    if (index >
                        0
                    ) {
                        HorizontalDivider(color = KitchenLine, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${total.quantity}",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            color = KitchenInk,
                            modifier = Modifier.width(64.dp),
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(total.name, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = KitchenInk)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                total.folios.forEach { (folio, quantity) ->
                                    Text(
                                        if (quantity > 1) "#$folio ×$quantity" else "#$folio",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = KitchenMuted,
                                        modifier =
                                            Modifier
                                                .clip(CircleShape)
                                                .background(KitchenLine.copy(alpha = 0.6f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp),
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
