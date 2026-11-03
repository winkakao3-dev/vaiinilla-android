package com.vaiinilla.app.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.mode.RestrictedMode
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderItem
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.SpaceCopy
import com.vaiinilla.app.domain.model.activeItems
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
) {
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
    var rejectingOrderId by remember { mutableStateOf<String?>(null) }
    var rejectingItem by remember { mutableStateOf<Pair<String, OrderItem>?>(null) }
    var moreOrderId by remember { mutableStateOf<String?>(null) }
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
        when (tab) {
            KitchenTab.NEW -> newOrders
            KitchenTab.PREPARING -> preparingOrders
            KitchenTab.READY -> readyOrders
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
                                onChangeMode?.invoke() ?: onBack()
                            },
                        )
                    }
                }
            }

            item(key = "kitchen-title") {
                Text(
                    "Comandas",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

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

            if (tabOrders.isEmpty()) {
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

            items(tabOrders, key = { it.summary.id }) { order ->
                val orderTab = kitchenTabOf(order)
                val inKitchen = order.summary.state == OrderState.PAID || order.summary.state == OrderState.PREPARING
                // Cocina quita artículos de un pedido cobrado o en preparación; el último ya no se quita, se
                // rechaza el pedido entero. El servidor solo deja quitar lo que se prepara en Cocina.
                val canRemoveItems = canAct && order.activeItems.size > 1 && inKitchen && !state.acting
                // Sin cobrar, el servidor no deja rechazar: solo cobrado o en preparación.
                val canReject = canAct && inKitchen
                val first = order.summary.id == tabOrders.first().summary.id
                KitchenOrderCard(
                    order = order,
                    acting = state.acting,
                    canAct = canAct,
                    canRemove = { item -> canRemoveItems && item.preparationStation == PreparationStation.KITCHEN },
                    onRemoveItem = { item -> rejectingItem = order.summary.id to item },
                    onPrimary = {
                        haptics.impact()
                        selectedOrderId = order.summary.id
                        if (orderTab == KitchenTab.PREPARING) {
                            onReady(order.summary.id, order.summary.version)
                        } else {
                            onStart(order.summary.id, order.summary.version)
                        }
                    },
                    onMore = if (canReject) ({ moreOrderId = order.summary.id }) else null,
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
            tab == KitchenTab.READY -> "Nada listo por ahora" to "Lo que marques lista aparece aquí hasta que lo entreguen."
            otherWork && tab == KitchenTab.NEW -> "Sin comandas nuevas" to "Lo que está en la estufa sigue en Preparando."
            otherWork -> "Nada en preparación" to "Empieza una de las Nuevas."
            else -> "Cocina al día" to "No hay comandas pendientes en este momento."
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

/**
 * La comanda en papel de K01 y K02: la alergia hasta arriba (Lalo la vio tarde cuando iba al fondo), el folio
 * grande, el tiempo con color y una sola acción. Rechazar todo vive en «Más opciones», lejos de Marcar lista.
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
    primaryAnchor: String,
    registry: com.vaiinilla.app.ui.components.AssistantAnchorRegistry,
    modifier: Modifier = Modifier,
) {
    val tab = kitchenTabOf(order)
    val summary = order.summary
    val place =
        buildList {
            add(SpaceCopy.staffDestination(summary.destination, summary.space))
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
                }
                KitchenAgeChip(createdAt = kitchenAgeStart(summary))
            }
            Column(modifier = Modifier.padding(top = 8.dp)) {
                // Lo rechazado no se prepara: no aparece en la comanda.
                order.activeItems.forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider(color = KitchenLine.copy(alpha = 0.6f))
                    KitchenItemLine(
                        item = item,
                        removable = canRemove(item),
                        onRemove = { onRemoveItem(item) },
                    )
                }
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
                    KitchenPrimaryButton(
                        label = if (tab == KitchenTab.PREPARING) "Marcar lista" else "Empezar a preparar",
                        lime = tab == KitchenTab.PREPARING,
                        enabled = canAct && !acting,
                        loading = acting,
                        modifier = Modifier.padding(top = 12.dp).assistantAnchor(registry, primaryAnchor),
                        onClick = onPrimary,
                    )
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

@Composable
private fun KitchenItemLine(
    item: OrderItem,
    removable: Boolean,
    onRemove: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Text(
            "${item.quantity}×",
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = KitchenInk,
            modifier = Modifier.width(34.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(item.productName, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = KitchenInk)
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp).navigationBarsPadding().padding(bottom = 20.dp),
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