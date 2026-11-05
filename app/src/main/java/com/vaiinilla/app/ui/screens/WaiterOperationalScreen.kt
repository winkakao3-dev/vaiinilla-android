package com.vaiinilla.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.RoomService
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.mode.RestrictedMode
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.SpaceCopy
import com.vaiinilla.app.domain.offline.OfflineStaffOrder
import com.vaiinilla.app.domain.repository.AbonoMode
import com.vaiinilla.app.domain.repository.AccountCollection
import com.vaiinilla.app.domain.repository.AccountPaymentMethod
import com.vaiinilla.app.domain.repository.BoardOrder
import com.vaiinilla.app.domain.repository.BoardTable
import com.vaiinilla.app.domain.repository.CallReason
import com.vaiinilla.app.domain.repository.CallStatus
import com.vaiinilla.app.domain.repository.SpaceAccount
import com.vaiinilla.app.domain.repository.SpaceAvailabilityState
import com.vaiinilla.app.domain.repository.SpaceSessionDetail
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.domain.repository.TableSpace
import com.vaiinilla.app.domain.repository.TableState
import com.vaiinilla.app.domain.repository.sortWaiterTables
import com.vaiinilla.app.domain.repository.tableState
import com.vaiinilla.app.domain.time.BusinessTime
import com.vaiinilla.app.ui.components.EditorialAccentButton
import com.vaiinilla.app.ui.components.NumberTicker
import com.vaiinilla.app.ui.components.OperationalAssistantHost
import com.vaiinilla.app.ui.components.OperationalAssistantPalette
import com.vaiinilla.app.ui.components.ToastPill
import com.vaiinilla.app.ui.components.TurnProgressBar
import com.vaiinilla.app.ui.components.VaiinillaAssistantButton
import com.vaiinilla.app.ui.components.animateSelectionColor
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberAssistantAnchorRegistry
import com.vaiinilla.app.ui.components.rememberOperationalAssistantController
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.components.waiterAssistantGuides
import com.vaiinilla.app.ui.discovery.QrScannerDialog
import com.vaiinilla.app.ui.operational.WaiterBoardLink
import com.vaiinilla.app.ui.operational.WaiterUiState
import com.vaiinilla.app.ui.operational.playAlertFeedback
import com.vaiinilla.app.ui.theme.LocalVaiinillaThemeMode
import com.vaiinilla.app.ui.theme.LocalVaiinillaThemeModeChanger
import com.vaiinilla.app.ui.theme.Yolk
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaiterOperationalScreen(
    state: WaiterUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit = {},
    onGoing: (TableCall) -> Unit = {},
    onAttended: (TableCall) -> Unit = {},
    onDeliver: (BoardOrder, String?) -> Unit = { _, _ -> },
    onOpenSpace: (Int) -> Unit = {},
    onCloseSpace: () -> Unit = {},
    onOpenTurn: (Int, Int?) -> Unit = { _, _ -> },
    onExtendTurn: (Int, Int) -> Unit = { _, _ -> },
    onReleaseSpace: (Int) -> Unit = {},
    onCollectAccount: (
        Int,
        AccountPaymentMethod,
        String?,
        String,
        List<String>?,
        String?,
    ) -> Unit = { _, _, _, _, _, _ -> },
    /** Abonos por monto o partes iguales: el espacio y el abono. */
    onAbono: ((Int, AccountPaymentMethod, AbonoMode, String?, Int?, String?, String, String?) -> Unit)? = null,
    onDismissCollection: () -> Unit = {},
    /** Renta de mostrador de una cancha con precio: aparta (ahora o renovación) y abre el cobro. */
    onStartRental: (Int, Int, Instant?, Int?) -> Unit = { _, _, _, _ -> },
    /** Vuelve a pedir al servidor los tiempos de renta. */
    onRetryRentalDurations: () -> Unit = {},
    onConfirmRental: (String) -> Unit = {},
    onCancelRental: () -> Unit = {},
    onFilterAttending: (Boolean) -> Unit = {},
    /** Pedido de mostrador a nombre de un cliente que no usa la app; se cobra en efectivo en Caja. */
    onOpenCounterOrder: () -> Unit = {},
    /** Tomar el pedido de una mesa desde su hoja. */
    onOpenTableOrder: (TableSpace) -> Unit = {},
    onCloseCounterOrder: () -> Unit = {},
    onSubmitCounterOrder: (List<CartLine>, String) -> Unit = { _, _ -> },
    /** Pedidos guardados sin internet: mandarlos ya, o quitar uno que se resolvió a mano. */
    onRetryOfflineOrders: () -> Unit = {},
    onDismissOfflineOrder: (OfflineStaffOrder) -> Unit = {},
    newCallTable: TableCall? = null,
    onAlertConsumed: () -> Unit = {},
    onChangeMode: (() -> Unit)? = null,
    restrictedMode: RestrictedMode? = null,
    placeName: String = "",
    assistantUserKey: String = "waiter",
    /** Quién usa el tablero: el mesero, o Caja cuando entra a cobrar cuentas. */
    kicker: String = "MESERO",
    roleChip: String = "MS",
    /** Dentro de Pedidos de Administración: sin encabezado propio (lo pone esa pantalla) y con "Para llevar". */
    embedded: Boolean = false,
    /** Cobrar la cuenta y entregar son de Mesero y Caja: el backend se los niega a Administración. */
    canChargeAndDeliver: Boolean = true,
) {
    val colors = rememberOperationalColors()
    val themeMode = LocalVaiinillaThemeMode.current
    val themeChanger = LocalVaiinillaThemeModeChanger.current
    val haptics = rememberVaiinillaHaptics()
    val context = LocalContext.current
    // R18: una pantalla recién compuesta (Admin › Pedidos › tocar una mesa) abre la hoja que el ViewModel ya tiene abierta.
    var openTableId by remember { mutableStateOf(state.openSpaceId) }
    LaunchedEffect(state.openSpaceId) { state.openSpaceId?.let { openTableId = it } }
    var delivering by remember { mutableStateOf<BoardOrder?>(null) }
    var scannerOpen by remember { mutableStateOf(false) }
    var assistantPulse by remember { mutableIntStateOf(0) }
    var tick by remember { mutableIntStateOf(0) }
    var kindFilter by remember { mutableStateOf<String?>(null) }
    var choosingScan by remember { mutableStateOf(false) }
    val assistantRegistry = rememberAssistantAnchorRegistry()
    val assistantController = rememberOperationalAssistantController(assistantUserKey, OperationalRole.WAITER)
    val assistantFocusRequester = remember { FocusRequester() }
    val readOnly = restrictedMode == RestrictedMode.READ_ONLY
    val assistantPalette =
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

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            tick += 1
        }
    }

    LaunchedEffect(newCallTable) {
        if (newCallTable != null) {
            playAlertFeedback(context)
            onAlertConsumed()
        }
    }

    val sorted =
        remember(state.tables, state.filterAttending, kindFilter) {
            val list = sortWaiterTables(state.tables).filter { kindFilter == null || it.space.type == kindFilter }
            if (state.filterAttending) {
                list.filter { it.tableState() == TableState.CALL || it.tableState() == TableState.READY }
            } else {
                list
            }
        }
    val nextUp =
        remember(state.tables, kindFilter, tick) {
            waiterNextUp(state.tables.filter { kindFilter == null || it.space.type == kindFilter })
        }
    val calling = state.tables.count { it.tableState() == TableState.CALL }
    val readyCount = state.tables.sumOf { table -> table.orders.count { it.state == OrderState.READY } }
    // Lo listo que pide el QR del cliente: es lo que el botón Escanear QR puede entregar.
    val scannable =
        state.tables.flatMap { table ->
            table.orders
                .filter {
                    it.state == OrderState.READY &&
                        !it.mustCollectBeforeDelivery &&
                        it.needsQrToDeliver(state.deliveryRequiresQr)
                }.map { table.space.name to it }
        }
    val kinds = remember(state.tables) { state.tables.map { it.space.type }.distinct() }
    val open = state.tables.firstOrNull { it.space.id == openTableId }

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
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(if (embedded) Modifier else Modifier.statusBarsPadding()),
            contentPadding =
                PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = if (embedded) 4.dp else 12.dp,
                    bottom = 120.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (embedded) {
                item(span = fullWidth) {
                    WaiterEmbeddedHeader(
                        link = state.boardLink,
                        canTakeOrders = !readOnly,
                        colors = colors,
                        onTakeAway = {
                            haptics.selection()
                            onOpenCounterOrder()
                        },
                    )
                }
            } else {
                item(span = fullWidth) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        WaiterLivePill(link = state.boardLink, colors = colors, who = kicker)
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
                                    touchSize = 56.dp,
                                )
                            }
                            StaffAvatarMenu(
                                initials = roleChip,
                                modeLabel = if (roleChip == "CJ") "Caja" else "Mesero",
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

                item(span = fullWidth) {
                    Text(
                        "Mesas",
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 44.sp,
                        letterSpacing = (-1.6).sp,
                        lineHeight = 46.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
                    )
                }
            }

            if (state.boardLink == WaiterBoardLink.Offline && state.tables.isNotEmpty()) {
                item(key = "waiter-offline", span = fullWidth) {
                    StaffOfflineBanner(offline = true, modifier = Modifier.animateItem(), savesOrders = true)
                }
            }

            if (state.offlineOrders.count > 0) {
                item(key = "waiter-offline-orders", span = fullWidth) {
                    OfflineOrdersCard(
                        state = state.offlineOrders,
                        onRetry = onRetryOfflineOrders,
                        onDismiss = onDismissOfflineOrder,
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            if (unreadableOrders > 0) {
                item(key = "waiter-update-app", span = fullWidth) {
                    StaffUpdateAppBanner(count = unreadableOrders, modifier = Modifier.animateItem())
                }
            }

            // Sin un tablero bueno, los ceros dirían que no hay nada que hacer.
            if (state.boardLoaded) {
                item(span = fullWidth) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        WaiterAlertCard(
                            count = calling,
                            label = "Te están llamando",
                            tint = WaiterCoral,
                            colors = colors,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                haptics.selection()
                                kindFilter = null
                                onFilterAttending(true)
                            },
                        )
                        WaiterAlertCard(
                            count = readyCount,
                            label = if (readyCount == 1) "Listo para entregar" else "Listos para entregar",
                            tint = colors.accentLime,
                            colors = colors,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                haptics.selection()
                                kindFilter = null
                                onFilterAttending(true)
                            },
                        )
                    }
                }
            }

            item(span = fullWidth) {
                WaiterFilterRow(
                    attending = state.filterAttending,
                    kind = kindFilter,
                    kinds = kinds,
                    colors = colors,
                    onAll = {
                        haptics.selection()
                        kindFilter = null
                        onFilterAttending(false)
                    },
                    onAttending = {
                        haptics.selection()
                        kindFilter = null
                        onFilterAttending(true)
                    },
                    onKind = { type ->
                        haptics.selection()
                        kindFilter = type
                        onFilterAttending(false)
                    },
                )
            }

            if (!state.callsEnabled) {
                item(key = "calls-disabled", span = fullWidth) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = colors.cardBackground,
                        modifier =
                            Modifier
                                .animateItem()
                                .fillMaxWidth()
                                .border(1.dp, colors.cardBorder, RoundedCornerShape(18.dp)),
                    ) {
                        Text(
                            "Las llamadas de mesa todavía no están activas en el servidor. " +
                                "Ya ves los pedidos listos y puedes entregarlos.",
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            if (state.errorMessage != null && state.tables.isEmpty()) {
                item(key = "board-error", span = fullWidth) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.cardBackground,
                        modifier =
                            Modifier
                                .animateItem()
                                .fillMaxWidth()
                                .border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp)),
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                state.errorMessage,
                                color = colors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            EditorialAccentButton(
                                text = "Reintentar",
                                onClick = onRefresh,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                    }
                }
            }

            if (state.loading && state.tables.isEmpty()) {
                items(6) {
                    Box(
                        modifier =
                            Modifier
                                .height(WaiterTileHeight)
                                .clip(RoundedCornerShape(20.dp))
                                .background(colors.cardBackground)
                                .border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp)),
                    )
                }
            } else {
                val callsUp = nextUp.count { it.kind == NextUpKind.Call }
                if (state.filterAttending && callsUp > 0) {
                    item(key = "calls-head", span = fullWidth) {
                        WaiterCallsHeader(count = callsUp, colors = colors, modifier = Modifier.animateItem())
                    }
                }
                if (state.filterAttending) {
                    itemsIndexed(nextUp, key = {
                        _,
                        it,
                        ->
                        "next-${it.kind}-${it.spaceId}-${it.title}"
                    }, span = {
                        _,
                        _,
                        ->
                        fullWidth()
                    }) { index, item ->
                        WaiterNextUpRow(
                            item = item,
                            colors = colors,
                            modifier = Modifier.animateItem().arrive(index, item.spaceId),
                            onClick = {
                                haptics.selection()
                                openTableId = item.spaceId
                                onOpenSpace(item.spaceId)
                            },
                        )
                    }
                }
                items(if (state.filterAttending) emptyList() else sorted, key = { it.space.id }) { table ->
                    WaiterTile(
                        table = table,
                        tick = tick,
                        colors = colors,
                        modifier = Modifier.animateItem(),
                        onClick = {
                            haptics.selection()
                            openTableId = table.space.id
                            onOpenSpace(table.space.id)
                        },
                    )
                }
                if ((if (state.filterAttending) nextUp.isEmpty() else sorted.isEmpty()) && state.boardLoaded) {
                    item(key = "board-empty", span = fullWidth) {
                        Surface(
                            shape = RoundedCornerShape(26.dp),
                            color = colors.cardBackground,
                            modifier =
                                Modifier.animateItem().fillMaxWidth().border(
                                    1.dp,
                                    colors.cardBorder,
                                    RoundedCornerShape(26.dp),
                                ),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    "Nada por atender",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = colors.textPrimary,
                                )
                                Text(
                                    "Todo en orden.",
                                    fontSize = 13.sp,
                                    color = colors.textSecondary,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!embedded && !readOnly) {
            WaiterBottomBar(
                canScan = canChargeAndDeliver && scannable.isNotEmpty(),
                colors = colors,
                onTakeOrder = {
                    haptics.selection()
                    onOpenCounterOrder()
                },
                onScan = {
                    haptics.selection()
                    val only = scannable.singleOrNull()
                    if (only != null) {
                        delivering = only.second
                        scannerOpen = true
                    } else {
                        choosingScan = true
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        ToastPill(
            message = state.toastMessage,
            containerColor = colors.textPrimary,
            contentColor = colors.background,
            badgeColor = if (state.toastIsError) WaiterCoral else colors.accentLime,
            badgeContentColor = if (state.toastIsError) colors.background else colors.accentInk,
            icon = if (state.toastIsError) Icons.Rounded.PriorityHigh else Icons.Rounded.Check,
        )

        if (!embedded) {
            OperationalAssistantHost(
                controller = assistantController,
                registry = assistantRegistry,
                guides = remember { waiterAssistantGuides() },
                manualTitle = "Manual de Mesero",
                palette = assistantPalette,
                buttonFocusRequester = assistantFocusRequester,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    // La hoja de cobro de la renta toma el lugar de la de la mesa; al cerrarla, la mesa vuelve.
    if (open != null && state.pendingRental == null) {
        WaiterTableSheet(
            table = open,
            stations = remember(state.counterCatalog) { productStations(state.counterCatalog) },
            tick = tick,
            acting = state.acting,
            readOnly = readOnly,
            colors = colors,
            spaceDetail = state.spaceDetail,
            spaceDetailLoading = state.spaceDetailLoading,
            lastCollection = state.lastCollection,
            onDismiss = {
                openTableId = null
                onCloseSpace()
            },
            onGoing = { onGoing(it) },
            onAttended = { onAttended(it) },
            onDeliver = {
                if (it.needsQrToDeliver(state.deliveryRequiresQr)) {
                    openTableId = null
                    onCloseSpace()
                    delivering = it
                } else {
                    // El establecimiento dispensa el QR en espacios: se confirma la entrega y listo.
                    onDeliver(it, null)
                }
            },
            deliveryRequiresQr = state.deliveryRequiresQr,
            canChargeAndDeliver = canChargeAndDeliver,
            onOpenTurn = { onOpenTurn(open.space.id, it) },
            onExtendTurn = { onExtendTurn(open.space.id, it) },
            rentable = open.availability?.rentable == true,
            onStartRental = { minutes, start, players -> onStartRental(open.space.id, minutes, start, players) },
            rentalDurations = state.rentalDurations,
            rentalDurationsFailed = state.rentalDurationsFailed,
            onRetryDurations = onRetryRentalDurations,
            onTakeOrder = {
                openTableId = null
                onCloseSpace()
                onOpenTableOrder(open.space)
            },
            toastMessage = state.toastMessage,
            toastIsError = state.toastIsError,
            onReleaseSpace = { onReleaseSpace(open.space.id) },
            onCollectAccount = { method, received, total, ids, tip ->
                onCollectAccount(open.space.id, method, received, total, ids, tip)
            },
            onAbono =
                onAbono?.let { abonar ->
                    { method, mode, amount, parts, received, expected, tip ->
                        abonar(open.space.id, method, mode, amount, parts, received, expected, tip)
                    }
                },
            onDismissCollection = onDismissCollection,
            remainingParts = state.remainingParts[open.space.id],
        )
    }

    state.pendingRental?.let { rental ->
        RentalCollectSheet(
            rental = rental,
            confirming = state.acting,
            colors = colors,
            onDismiss = onCancelRental,
            onConfirm = onConfirmRental,
        )
    }

    if (state.counterOrderOpen) {
        CounterOrderSheet(
            state = state,
            colors = colors,
            onDismiss = onCloseCounterOrder,
            onSubmit = onSubmitCounterOrder,
            submitAction =
                when {
                    state.counterOrderSpace == null -> "Enviar a Caja"
                    // A la cuenta de la mesa: nace cobrado y entra directo a cocina.
                    state.allowsPayAtEnd -> "Enviar a cocina · a la cuenta"
                    else -> "Enviar a Caja"
                },
        )
    }

    // La hoja trabaja con el pedido vigente del tablero, no con la versión que tenía al abrirse (STAFF-15).
    val liveDelivering =
        delivering?.let { held ->
            state.tables.firstNotNullOfOrNull { table -> table.orders.firstOrNull { it.id == held.id } }
        }
    var deliverSent by remember { mutableStateOf(false) }
    LaunchedEffect(deliverSent, state.acting) {
        if (deliverSent && !state.acting) {
            deliverSent = false
            if (liveDelivering == null || liveDelivering.state != OrderState.READY) delivering = null
        }
    }
    LaunchedEffect(liveDelivering?.state, delivering?.id) {
        if (delivering != null && (liveDelivering == null || liveDelivering.state != OrderState.READY)) {
            delivering = null
        }
    }
    delivering?.let { held ->
        val order = liveDelivering ?: held
        WaiterDeliverSheet(
            order = order,
            acting = state.acting,
            colors = colors,
            onDismiss = { delivering = null },
            onScan = { scannerOpen = true },
            onConfirm = { code ->
                deliverSent = true
                onDeliver(order, code)
            },
        )
    }

    val scannerOrder = liveDelivering ?: delivering
    if (scannerOpen && scannerOrder != null) {
        QrScannerDialog(
            onClose = { scannerOpen = false },
            title = "Apunta al QR del pedido del cliente",
            onPayload = { raw ->
                scannerOpen = false
                deliverSent = true
                onDeliver(scannerOrder, raw)
            },
        )
    }

    if (choosingScan) {
        WaiterScanChooserSheet(
            orders = scannable,
            colors = colors,
            onDismiss = { choosingScan = false },
            onPick = { order ->
                choosingScan = false
                delivering = order
                scannerOpen = true
            },
        )
    }
}

private val WaiterCoral = Color(0xFFE5645A)
private val WaiterTileHeight = 116.dp

private val fullWidth: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }

/** El tablero dentro de Pedidos de Administración: el estado de conexión y "Para llevar", sin título ni avatar. */
@Composable
private fun WaiterEmbeddedHeader(
    link: WaiterBoardLink,
    canTakeOrders: Boolean,
    colors: OperationalColors,
    onTakeAway: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WaiterLivePill(link = link, colors = colors)
        Spacer(Modifier.weight(1f))
        Row(
            modifier =
                Modifier
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(colors.cardInner)
                    .border(1.dp, colors.cardBorder, CircleShape)
                    .physicalPress(enabled = canTakeOrders, onClick = onTakeAway)
                    .graphicsLayer { alpha = if (canTakeOrders) 1f else 0.5f }
                    .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Outlined.ShoppingBag,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(18.dp),
            )
            Text("Para llevar", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)
        }
    }
}

@Composable
internal fun WaiterLivePill(
    link: WaiterBoardLink,
    colors: OperationalColors,
    /** Quién está en el tablero ("MESERO", "CAJA"); el nombre de la persona todavía no llega del servidor. */
    who: String? = null,
) {
    val online = link == WaiterBoardLink.Live
    val tone =
        animateSelectionColor(
            when (link) {
                WaiterBoardLink.Live -> tileToneColor(TileTone.Ready, colors)
                WaiterBoardLink.Connecting -> colors.textMuted
                WaiterBoardLink.Offline -> WaiterCoral
            },
            "live-dot",
        )
    val background =
        animateSelectionColor(
            if (online) lerp(colors.cardBackground, colors.accentLime, 0.14f) else colors.cardBackground,
            "live-background",
        )
    Row(
        modifier =
            Modifier
                .clip(CircleShape)
                .background(background)
                .border(1.dp, colors.cardBorder, CircleShape)
                .padding(horizontal = 12.dp, vertical = 7.dp)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .pulseGlow(active = online, color = colors.accentLime, spread = 8.dp, corner = 4.dp)
                .clip(CircleShape)
                .background(tone),
        )
        val status =
            when (link) {
                WaiterBoardLink.Live -> "En vivo"
                WaiterBoardLink.Connecting -> "Conectando…"
                WaiterBoardLink.Offline -> "Sin conexión"
            }
        Text(
            (if (who.isNullOrBlank()) status else "$status · $who").uppercase(),
            color = if (online) tone else colors.textSecondary,
            fontWeight = FontWeight.Black,
            fontSize = 11.5.sp,
            letterSpacing = 1.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Halo que se expande y se desvanece detrás del elemento; con animaciones apagadas no se dibuja. */
@Composable
private fun Modifier.pulseGlow(
    active: Boolean,
    color: Color,
    spread: Dp,
    corner: Dp,
    periodMillis: Int = 1200,
): Modifier {
    if (!active || reducedMotion()) return this
    val transition = rememberInfiniteTransition(label = "pulse-glow")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMillis, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "pulse-glow-progress",
    )
    return drawBehind {
        val grow = spread.toPx() * progress
        drawRoundRect(
            color = color.copy(alpha = 0.55f * (1f - progress)),
            topLeft = Offset(-grow, -grow),
            size = Size(size.width + 2f * grow, size.height + 2f * grow),
            cornerRadius = CornerRadius(corner.toPx() + grow),
        )
    }
}

/** "1 · Te están llamando": en color cuando hay algo, gris cuando no; tocarla filtra lo que hay que atender. */
@Composable
private fun WaiterAlertCard(
    count: Int,
    label: String,
    tint: Color,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val active = count > 0
    val shape = RoundedCornerShape(22.dp)
    val background = animateSelectionColor(if (active) tint else colors.cardBackground, "alert-background")
    val ink = animateSelectionColor(if (active) Color(0xFF16160F) else colors.textSecondary, "alert-ink")
    val number = animateSelectionColor(if (active) Color(0xFF16160F) else colors.textPrimary, "alert-number")
    Column(
        modifier =
            modifier
                .physicalPress(enabled = active, onClick = onClick)
                .clip(shape)
                .background(background)
                .border(1.dp, if (active) Color.Transparent else colors.cardBorder, shape)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        NumberTicker(value = count, label = "alert-count") { value ->
            Text("$value", color = number, fontSize = 30.sp, fontWeight = FontWeight.Black, lineHeight = 32.sp)
        }
        Text(label, color = ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp, maxLines = 2)
    }
}

@Composable
private fun WaiterFilterRow(
    attending: Boolean,
    kind: String?,
    kinds: List<String>,
    colors: OperationalColors,
    onAll: () -> Unit,
    onAttending: () -> Unit,
    onKind: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WaiterFilterChip("Todas", !attending && kind == null, colors, onAll)
        WaiterFilterChip("Por atender", attending, colors, onAttending)
        // Con un solo tipo de espacio el filtro repetiría "Todas".
        if (kinds.size > 1) {
            kinds.forEach { type ->
                WaiterFilterChip(kindPlural(type), !attending && kind == type, colors) { onKind(type) }
            }
        }
    }
}

private fun kindPlural(type: String): String {
    val label = SpaceCopy.kindLabel(type)
    return if (label.endsWith("l")) "${label}es" else "${label}s"
}

@Composable
private fun WaiterFilterChip(
    label: String,
    selected: Boolean,
    colors: OperationalColors,
    onClick: () -> Unit,
) {
    val background =
        animateSelectionColor(if (selected) colors.textPrimary else colors.cardBackground, "chip-background")
    val ink = animateSelectionColor(if (selected) colors.background else colors.textPrimary, "chip-ink")
    Box(
        modifier =
            Modifier
                .heightIn(min = 44.dp)
                .clip(CircleShape)
                .background(background)
                .border(1.dp, if (selected) Color.Transparent else colors.cardBorder, CircleShape)
                .physicalPress(onClick = onClick)
                .semantics { this.selected = selected }
                .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
    }
}

/** Las dos cosas que el mesero hace todo el turno, siempre a la mano abajo. */
@Composable
private fun WaiterBottomBar(
    canScan: Boolean,
    colors: OperationalColors,
    onTakeOrder: () -> Unit,
    onScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(0f to colors.background.copy(alpha = 0f), 0.35f to colors.background),
                ).navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .weight(1.15f)
                    .height(58.dp)
                    .clip(CircleShape)
                    .background(colors.textPrimary)
                    .physicalPress(onClick = onTakeOrder),
            contentAlignment = Alignment.Center,
        ) {
            Text("Tomar pedido", color = colors.background, fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .height(58.dp)
                    .clip(CircleShape)
                    .background(colors.cardBackground)
                    .border(1.dp, colors.cardBorder, CircleShape)
                    .physicalPress(enabled = canScan, onClick = onScan)
                    .graphicsLayer { alpha = if (canScan) 1f else 0.45f },
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.QrCodeScanner,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(20.dp),
            )
            Text("Escanear QR", color = colors.textPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp)
        }
    }
}

/** Con varios pedidos listos, primero se elige cuál y luego se escanea el QR de ese cliente. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WaiterScanChooserSheet(
    orders: List<Pair<String, BoardOrder>>,
    colors: OperationalColors,
    onDismiss: () -> Unit,
    onPick: (BoardOrder) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("¿Cuál vas a entregar?", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = colors.textPrimary)
            Text(
                "Elige el pedido y escanea el QR del cliente.",
                color = colors.textSecondary,
                fontSize = 13.sp,
            )
            orders.forEach { (place, order) ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.cardBackground)
                            .border(1.dp, colors.cardBorder, RoundedCornerShape(18.dp))
                            .physicalPress(onClick = { onPick(order) })
                            .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "#${order.folio} · $place",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colors.textPrimary,
                        )
                        Text(
                            "${order.clientName ?: "Cliente"} · ${order.itemsSummary}",
                            color = colors.textSecondary,
                            fontSize = 12.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        Icons.Outlined.QrCodeScanner,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/** Cómo se pinta el estado de un mosaico; el color solo marca qué tan urgente es. */
internal enum class TileTone { Call, Ready, Kitchen, Turn, Collect, Open, Free }

internal data class WaiterTileStatus(
    val label: String,
    val detail: String?,
    val tone: TileTone,
)

/** El estado del mosaico en mayúsculas ("LLAMANDO · 1 min", "2 LISTOS", "QUEDAN 32 MIN") y una línea de detalle. */
internal fun waiterTileStatus(
    table: BoardTable,
    now: Instant = Instant.now(),
): WaiterTileStatus {
    val call = table.call
    val ready = table.orders.filter { it.state == OrderState.READY }
    val availability = table.availability
    return when {
        call != null ->
            WaiterTileStatus(
                label =
                    if (call.status == CallStatus.EN_CAMINO) {
                        "VA ${(call.takenBy?.name ?: "alguien").uppercase()}"
                    } else {
                        "LLAMANDO · ${minutesSince(call.createdAt, now)}"
                    },
                detail = call.reason.staffLabel,
                tone = TileTone.Call,
            )
        ready.isNotEmpty() ->
            WaiterTileStatus(
                label = "${ready.size} ${if (ready.size == 1) "LISTO" else "LISTOS"}",
                detail = ready.joinToString(" · ") { "#${it.folio}" },
                tone = TileTone.Ready,
            )
        availability?.state == SpaceAvailabilityState.POR_COBRAR ->
            WaiterTileStatus("POR COBRAR", "Se libera al cobrar", TileTone.Collect)
        availability?.state == SpaceAvailabilityState.EN_GRACIA ->
            WaiterTileStatus("TURNO TERMINADO", "Renovar o liberar", TileTone.Collect)
        availability?.isHeldAt(now) == true ->
            if (availability.isWaitingPayment) {
                WaiterTileStatus("APARTADA", "Esperando el pago", TileTone.Turn)
            } else {
                WaiterTileStatus("RESERVADA AHORA", "Se abre sola", TileTone.Turn)
            }
        availability?.state == SpaceAvailabilityState.OCUPADA && availability.endsAt != null -> {
            val left =
                runCatching { Duration.between(now, Instant.parse(availability.endsAt)).toMillis() }.getOrNull() ?: 0L
            WaiterTileStatus(
                label = "QUEDAN ${remainingText(left).uppercase()}",
                detail = ordersCaption(table) ?: if (availability.settled) "Turno pagado" else "Por cobrar",
                tone = TileTone.Turn,
            )
        }
        table.orders.isNotEmpty() ->
            WaiterTileStatus(
                label = if (table.orders.any { it.state == OrderState.PREPARING }) "EN COCINA" else "POR PREPARAR",
                detail = ordersCaption(table),
                tone = TileTone.Kitchen,
            )
        availability?.state == SpaceAvailabilityState.OCUPADA ->
            WaiterTileStatus(
                "CUENTA ABIERTA",
                spaceAvailabilityCaption(availability, 0)?.substringBefore('\n'),
                TileTone.Open,
            )
        else -> {
            val next = nextReservationCaption(availability?.nextReservationStart)
            if (next != null) {
                WaiterTileStatus(next.uppercase(), "Se abre sola", TileTone.Turn)
            } else {
                WaiterTileStatus("LIBRE", null, TileTone.Free)
            }
        }
    }
}

private fun ordersCaption(table: BoardTable): String? =
    table.orders.size
        .takeIf { it > 0 }
        ?.let { "$it ${if (it == 1) "pedido" else "pedidos"}" }

private fun minutesSince(
    iso: String,
    now: Instant,
): String {
    val ms = runCatching { Duration.between(Instant.parse(iso), now).toMillis() }.getOrNull() ?: return "ahora"
    return when {
        ms < 60_000 -> "ahora"
        ms < 3_600_000 -> "${ms / 60_000} min"
        else -> "${ms / 3_600_000} h"
    }
}

/** El color del estado; en claro va más oscuro para que se lea sobre el papel. */
private fun tileToneColor(
    tone: TileTone,
    colors: OperationalColors,
): Color =
    when (tone) {
        TileTone.Call -> if (colors.isDark) WaiterCoral else Color(0xFFC0392B)
        TileTone.Ready -> if (colors.isDark) colors.accentLime else Color(0xFF4F6B12)
        TileTone.Kitchen -> if (colors.isDark) Color(0xFFF2C94C) else Color(0xFF8A6100)
        TileTone.Turn -> if (colors.isDark) Color(0xFF8EC5F5) else Color(0xFF1F6FB2)
        TileTone.Collect -> if (colors.isDark) Color(0xFFE9A23B) else Color(0xFF9A5B00)
        TileTone.Open, TileTone.Free -> colors.textMuted
    }

/** Qué tipo de pendiente es; el orden del enum es el de urgencia. */
internal enum class NextUpKind { Call, Deliver, TurnEnding, Collect }

internal data class WaiterNextUp(
    val kind: NextUpKind,
    val spaceId: Int,
    val title: String,
    val detail: String,
    val action: String,
    /** N10: el motivo de la llamada da el ícono; null en lo que no es llamada. */
    val reason: CallReason? = null,
    /** Cuándo llamó, para poner arriba la llamada más vieja. */
    val since: String? = null,
)

/** El ícono de cada motivo de llamada (N10): la cuenta, cubiertos, un problema o atención. */
internal fun callReasonIcon(reason: CallReason?) =
    when (reason) {
        CallReason.CUENTA -> Icons.Outlined.ReceiptLong
        CallReason.UTENSILIOS -> Icons.Outlined.Restaurant
        CallReason.PROBLEMA -> Icons.Outlined.WarningAmber
        CallReason.ATENCION, null -> Icons.Outlined.Notifications
    }

private const val TURN_ENDING_MS = 15 * 60_000L

/** Lo que sigue en todo el lugar, de lo más urgente a lo menos: llamadas, lo listo, turnos por acabar y cuentas por cobrar. */
internal fun waiterNextUp(
    tables: List<BoardTable>,
    now: Instant = Instant.now(),
): List<WaiterNextUp> =
    tables
        .flatMap { table ->
            val name = table.space.name
            val id = table.space.id
            buildList {
                table.call?.let { call ->
                    val since = minutesSince(call.createdAt, now).let { if (it == "ahora") "ahora" else "hace $it" }
                    // N10: el motivo va en grande para saber qué llevar antes de ir a la mesa.
                    if (call.status == CallStatus.EN_CAMINO) {
                        val detail = listOfNotNull(call.reason.staffLabel, since, call.clientName).joinToString(" · ")
                        add(
                            WaiterNextUp(
                                NextUpKind.Call,
                                id,
                                "Va ${call.takenBy?.name ?: "alguien"} a $name",
                                detail,
                                "Ver",
                                call.reason,
                                call.createdAt,
                            ),
                        )
                    } else {
                        val detail = listOfNotNull(name, since, call.clientName).joinToString(" · ")
                        add(
                            WaiterNextUp(
                                NextUpKind.Call,
                                id,
                                call.reason.staffLabel,
                                detail,
                                "Voy",
                                call.reason,
                                call.createdAt,
                            ),
                        )
                    }
                }
                table.orders.filter { it.state == OrderState.READY }.forEach { order ->
                    val detail =
                        listOfNotNull(
                            order.itemsSummary.takeIf { it.isNotBlank() },
                            if (order.mustCollectBeforeDelivery) "cobrar antes" else "listo",
                        ).joinToString(" · ")
                    add(
                        WaiterNextUp(
                            NextUpKind.Deliver,
                            id,
                            "Llevar #${order.folio} a $name",
                            detail,
                            if (order.mustCollectBeforeDelivery) "Cobrar" else "Entregar",
                        ),
                    )
                }
                val availability = table.availability
                val left =
                    availability
                        ?.endsAt
                        ?.takeIf { availability.state == SpaceAvailabilityState.OCUPADA }
                        ?.let { runCatching { Duration.between(now, Instant.parse(it)).toMillis() }.getOrNull() }
                if (left != null && left in 0..TURN_ENDING_MS) {
                    add(
                        WaiterNextUp(
                            NextUpKind.TurnEnding,
                            id,
                            "A $name le quedan ${remainingText(left)}",
                            "Pregúntales si alargan el turno",
                            "Ver",
                        ),
                    )
                }
                if (availability?.state == SpaceAvailabilityState.POR_COBRAR ||
                    availability?.state == SpaceAvailabilityState.EN_GRACIA
                ) {
                    add(
                        WaiterNextUp(
                            NextUpKind.Collect,
                            id,
                            "$name ya terminó",
                            "Falta cobrar para liberarla",
                            "Cobrar",
                        ),
                    )
                }
            }
        }.sortedWith(compareBy<WaiterNextUp> { it.kind.ordinal }.thenBy { it.since.orEmpty() })

/** N10: «Te llaman · 3» y los motivos que el cliente puede escoger, para saber qué llevar. */
@Composable
private fun WaiterCallsHeader(
    count: Int,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp)) {
        Text(
            "Te llaman · $count",
            color = colors.textPrimary,
            fontWeight = FontWeight.Black,
            fontSize = 30.sp,
            letterSpacing = (-0.8).sp,
        )
        Text(
            "El motivo va arriba: ya sabes qué llevar antes de ir a la mesa.",
            color = colors.textSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            "LO QUE EL CLIENTE PUEDE ESCOGER",
            color = colors.textMuted,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CallReason.entries.forEach { reason ->
                Row(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(colors.cardBackground)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        callReasonIcon(reason),
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(reason.label, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun WaiterNextUpRow(
    item: WaiterNextUp,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val tone =
        when (item.kind) {
            NextUpKind.Call -> TileTone.Call
            NextUpKind.Deliver -> TileTone.Ready
            NextUpKind.TurnEnding -> TileTone.Turn
            NextUpKind.Collect -> TileTone.Collect
        }
    val accent = tileToneColor(tone, colors)
    val urgent = item.kind == NextUpKind.Call || item.kind == NextUpKind.Deliver
    val ringing = item.kind == NextUpKind.Call && item.action == "Voy"
    val shape = RoundedCornerShape(20.dp)
    Surface(
        shape = shape,
        color = if (urgent) accent.copy(alpha = if (colors.isDark) 0.14f else 0.10f) else colors.cardBackground,
        modifier =
            modifier
                .fillMaxWidth()
                .border(if (ringing) 2.dp else 1.dp, if (ringing) accent else colors.cardBorder, shape)
                .clip(shape)
                .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector =
                        when (item.kind) {
                            NextUpKind.Call -> callReasonIcon(item.reason)
                            NextUpKind.Deliver -> Icons.Outlined.RoomService
                            NextUpKind.TurnEnding -> Icons.Outlined.Schedule
                            NextUpKind.Collect -> Icons.Outlined.Payments
                        },
                    contentDescription = null,
                    tint = Color(0xFF1B1B18),
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    item.title,
                    fontWeight = if (item.kind == NextUpKind.Call) FontWeight.Black else FontWeight.Bold,
                    fontSize = if (item.kind == NextUpKind.Call) 17.sp else 16.sp,
                    letterSpacing = if (item.kind == NextUpKind.Call) (-0.3).sp else 0.sp,
                    color = colors.textPrimary,
                )
                if (item.detail.isNotBlank()) {
                    Text(item.detail, fontSize = 13.sp, color = colors.textSecondary, maxLines = 2)
                }
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = if (urgent) accent else colors.cardBorder,
            ) {
                Text(
                    item.action,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (urgent) Color(0xFF1B1B18) else colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun WaiterTile(
    table: BoardTable,
    tick: Int,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    // tick entra en la clave para que "Quedan 32 min" y "Llamando · 1 min" avancen solos.
    val status = remember(table, tick) { waiterTileStatus(table) }
    val urgent = status.tone == TileTone.Call || status.tone == TileTone.Ready
    val ringing = status.tone == TileTone.Call && table.call?.status != CallStatus.EN_CAMINO
    val shape = RoundedCornerShape(22.dp)
    val container =
        animateSelectionColor(
            when (status.tone) {
                TileTone.Call -> lerp(colors.cardBackground, WaiterCoral, 0.16f)
                TileTone.Ready -> lerp(colors.cardBackground, colors.accentLime, 0.12f)
                else -> colors.cardBackground
            },
            "tile-container",
        )
    val border =
        animateSelectionColor(
            when (status.tone) {
                TileTone.Call -> WaiterCoral
                TileTone.Ready -> colors.accentLime
                else -> colors.cardBorder
            },
            "tile-border",
        )
    val tone = animateSelectionColor(tileToneColor(status.tone, colors), "tile-tone")
    Column(
        modifier =
            modifier
                .physicalPress(onClick = onClick)
                .pulseGlow(active = ringing, color = WaiterCoral, spread = 7.dp, corner = 22.dp)
                .heightIn(min = WaiterTileHeight)
                .clip(shape)
                .background(container)
                .border(if (urgent) 1.5.dp else 1.dp, border, shape)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        BasicText(
            table.space.name,
            style =
                TextStyle(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.4).sp,
                    lineHeight = 1.1.em,
                ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(minFontSize = 15.sp, maxFontSize = 21.sp, stepSize = 1.sp),
        )
        Column(modifier = Modifier.padding(top = 14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(tone))
                Text(
                    status.label,
                    color = tone,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.5.sp,
                    letterSpacing = 0.6.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            status.detail?.let {
                Text(
                    it,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            // Avance del turno: se ve de un vistazo cuánto le queda a cada cancha.
            val progress = spaceTileProgress(table.availability, tick)?.takeIf { status.tone == TileTone.Turn }
            if (progress != null) {
                TurnProgressBar(
                    fraction = progress,
                    track = colors.pillBackground,
                    fill = tone,
                    modifier = Modifier.padding(top = 8.dp),
                    height = 4.dp,
                )
            }
        }
    }
}

private fun waiterSince(iso: String): String {
    val ms = elapsedSinceMs(iso) ?: return "ahora"
    return when {
        // Las llamadas vencen a los 10 min; el reloj mm:ss solo tiene sentido dentro de la hora.
        ms < 3_600_000 -> {
            val totalSeconds = (ms / 1_000).toInt().coerceAtLeast(0)
            "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
        }
        ms < 86_400_000 -> "hace ${ms / 3_600_000} h"
        else -> "hace ${ms / 86_400_000} d"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WaiterTableSheet(
    table: BoardTable,
    /** N08: la estación de cada producto por nombre; vacío si el catálogo no llegó. */
    stations: Map<String, PreparationStation> = emptyMap(),
    tick: Int,
    acting: Boolean,
    readOnly: Boolean,
    colors: OperationalColors,
    spaceDetail: SpaceSessionDetail?,
    spaceDetailLoading: Boolean,
    lastCollection: AccountCollection?,
    onDismiss: () -> Unit,
    onGoing: (TableCall) -> Unit,
    onAttended: (TableCall) -> Unit,
    onDeliver: (BoardOrder) -> Unit,
    deliveryRequiresQr: Boolean,
    canChargeAndDeliver: Boolean,
    onOpenTurn: (Int?) -> Unit,
    onExtendTurn: (Int) -> Unit,
    onReleaseSpace: () -> Unit,
    onCollectAccount: (AccountPaymentMethod, String?, String, List<String>?, String?) -> Unit,
    onDismissCollection: () -> Unit,
    onAbono: AbonoHandler? = null,
    /** Personas que faltan tras un abono en partes; la hoja abre en Partes con ese número (I06). */
    remainingParts: Int? = null,
    rentable: Boolean = false,
    onStartRental: (Int, Instant?, Int?) -> Unit = { _, _, _ -> },
    rentalDurations: List<Int> = DefaultRentalDurations,
    rentalDurationsFailed: Boolean = false,
    onRetryDurations: (() -> Unit)? = null,
    /** Tomar un pedido para esta mesa; null cuando no aplica. */
    onTakeOrder: (() -> Unit)? = null,
    /** La hoja vive en su propia ventana: el aviso de la pantalla queda debajo y no se ve. */
    toastMessage: String? = null,
    toastIsError: Boolean = false,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Cobrar reemplaza el contenido de esta misma hoja en lugar de abrir otra encima.
    var collecting by remember(table.space.id) { mutableStateOf(false) }
    LaunchedEffect(lastCollection) { if (lastCollection != null) collecting = false }
    val account = spaceDetail?.account
    val reduced = reducedMotion()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        BackHandler(enabled = collecting && account != null) { collecting = false }
        Box {
            AnimatedContent(
                targetState = collecting && account != null,
                transitionSpec = {
                    if (reduced) {
                        fadeIn(snap()) togetherWith fadeOut(snap())
                    } else {
                        val direction = if (targetState) 1 else -1
                        (
                            slideInHorizontally(spring(dampingRatio = 0.9f, stiffness = 500f)) { it / 4 * direction } +
                                fadeIn(spring(stiffness = 400f))
                        ) togetherWith
                            (
                                slideOutHorizontally(
                                    spring(dampingRatio = 0.9f, stiffness = 500f),
                                ) { -it / 4 * direction } +
                                    fadeOut(spring(stiffness = 600f))
                            )
                    }
                },
                label = "table-sheet-pane",
            ) { showCollect ->
                if (showCollect && account != null) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 22.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = { collecting = false }) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "Volver a la cuenta",
                                    tint = colors.textPrimary,
                                )
                            }
                            Column(
                                Modifier.weight(1f).padding(end = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    "Cobrar cuenta",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    color = colors.textPrimary,
                                )
                                Text(
                                    "${table.space.name} · ${moneyLabel(account.pending)}",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textSecondary,
                                )
                            }
                        }
                        CollectAccountContent(
                            total = account.pending,
                            // El servidor suma toda la cuenta sin cobrar, también el efectivo por cobrar (STAFF-03).
                            payable = account.orders.filter { it.pending },
                            confirming = acting,
                            colors = colors,
                            onConfirm = onCollectAccount,
                            remaining = account.remaining,
                            paidIn = account.paidIn,
                            onAbono = onAbono,
                            initialPeople = remainingParts,
                            initialSplit = if (remainingParts != null) SplitMode.PARTS else SplitMode.ALL,
                            showsHeader = false,
                        )
                    }
                } else {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                // Con una cuenta larga, lo de abajo (entregar, liberar) quedaba fuera de la pantalla.
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 22.dp)
                                .padding(bottom = 36.dp)
                                .animateContentSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        val readyHere = table.orders.count { it.state == OrderState.READY }
                        val openedAt =
                            table.availability
                                ?.startedAt
                                ?.let { runCatching { Instant.parse(it) }.getOrNull() }
                                ?.let { BusinessTime.format(it, RESERVATION_HOUR) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    listOfNotNull(
                                        SpaceCopy.kindLabel(table.space.type).uppercase(),
                                        openedAt?.let { "CUENTA ABIERTA $it" },
                                    ).joinToString(" · "),
                                    color = colors.textSecondary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.5.sp,
                                    letterSpacing = 1.4.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    table.space.name,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 30.sp,
                                    letterSpacing = (-1).sp,
                                    lineHeight = 33.sp,
                                    color = colors.textPrimary,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                            if (readyHere > 0) {
                                Text(
                                    "$readyHere ${if (readyHere == 1) "LISTO" else "LISTOS"}",
                                    color = tileToneColor(TileTone.Ready, colors),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.5.sp,
                                    letterSpacing = 0.6.sp,
                                    modifier =
                                        Modifier
                                            .padding(bottom = 6.dp)
                                            .clip(CircleShape)
                                            .background(lerp(colors.cardBackground, colors.accentLime, 0.2f))
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }
                        val call = table.call
                        if (call != null) {
                            WaiterCallCard(
                                call = call,
                                table = table,
                                tick = tick,
                                enabled = !acting && !readOnly,
                                colors = colors,
                                onGoing = { onGoing(call) },
                                onAttended = { onAttended(call) },
                            )
                        }
                        // Lo que Cocina o Caja canceló hace poco: el mesero ya no debe esperarlo ni llevarlo.
                        table.recentRejections.forEach { rejected ->
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = lerp(colors.cardBackground, WaiterCoral, 0.14f),
                                modifier =
                                    Modifier.fillMaxWidth().border(
                                        1.dp,
                                        WaiterCoral.copy(alpha = 0.4f),
                                        RoundedCornerShape(18.dp),
                                    ),
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        "#${rejected.folio} · ${rejected.byLabel} lo ${rejected.verb}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = colors.textPrimary,
                                    )
                                    rejected.reason?.let {
                                        Text(
                                            it,
                                            fontSize = 12.5.sp,
                                            color = colors.textSecondary,
                                            modifier = Modifier.padding(top = 2.dp),
                                        )
                                    }
                                }
                            }
                        }
                        // Lo que está en curso va antes de la cuenta: lo listo es lo más urgente y, si no, el mesero
                        // sabe en qué va cada pedido sin esperar a que llegue a listo.
                        // La cuenta completa en una sola lista: lo listo primero, luego lo que va en cocina y al final
                        // lo ya entregado, cada uno con lo que cuesta cuando la cuenta ya llegó.
                        val sheetOrders = remember(table.orders, account) { waiterSheetOrders(table.orders, account) }
                        if (sheetOrders.isNotEmpty()) {
                            Text(
                                "PEDIDOS DE ESTA CUENTA",
                                color = colors.textSecondary,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.5.sp,
                                letterSpacing = 1.4.sp,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(colors.cardBackground)
                                        .border(1.dp, colors.cardBorder, RoundedCornerShape(22.dp)),
                            ) {
                                sheetOrders.forEachIndexed { index, row ->
                                    if (index > 0) {
                                        HorizontalDivider(
                                            color = colors.cardBorder,
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                        )
                                    }
                                    WaiterSheetOrderRow(
                                        order = row.order,
                                        stations = stations,
                                        amount = row.amount,
                                        readOnly = readOnly,
                                        canDeliver = canChargeAndDeliver && row.live,
                                        colors = colors,
                                        onDeliver = onDeliver,
                                    )
                                }
                            }
                        }
                        // Con la cuenta a la vista, "Tomar pedido" va junto a "Cobrar cuenta"; si no, va solo.
                        val accountHasOrders = account?.orders?.isNotEmpty() == true
                        if (onTakeOrder != null && !readOnly && !accountHasOrders) {
                            ActionPill(
                                label = "Tomar pedido",
                                enabled = !acting,
                                loading = false,
                                colors = colors,
                                // Sin cobrar ni entregar, pedir es lo principal de la hoja.
                                primary = !canChargeAndDeliver,
                                onClick = onTakeOrder,
                            )
                        }
                        WaiterSpaceSection(
                            space = table.space,
                            detail = spaceDetail,
                            loading = spaceDetailLoading,
                            lastCollection = lastCollection,
                            tick = tick,
                            acting = acting,
                            readOnly = readOnly,
                            colors = colors,
                            onOpenTurn = onOpenTurn,
                            onExtend = onExtendTurn,
                            onRelease = onReleaseSpace,
                            onDismissCollection = onDismissCollection,
                            rentable = rentable,
                            onRent = onStartRental,
                            onStartCollect = { collecting = true },
                            pricePerHour = table.availability?.pricePerHour,
                            hasReadyOrders = table.orders.any { it.state == OrderState.READY },
                            ordersInCourse = table.orders.size,
                            rentalDurations = rentalDurations,
                            rentalDurationsFailed = rentalDurationsFailed,
                            onRetryDurations = onRetryDurations,
                            canCharge = canChargeAndDeliver,
                            showOrders = false,
                            onTakeOrder = onTakeOrder?.takeIf { !readOnly },
                        )
                        if (accountHasOrders && table.orders.isNotEmpty()) {
                            // Cobrar antes de entregar se vale, pero que no pase sin darse cuenta.
                            val count = table.orders.size
                            Text(
                                if (count == 1) {
                                    "Ojo: 1 pedido de esta cuenta todavía no se entrega."
                                } else {
                                    "Ojo: $count pedidos de esta cuenta todavía no se entregan."
                                },
                                color = Yolk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
            ToastPill(
                message = toastMessage,
                containerColor = colors.textPrimary,
                contentColor = colors.background,
                badgeColor = if (toastIsError) WaiterCoral else colors.accentLime,
                badgeContentColor = if (toastIsError) colors.background else colors.accentInk,
                icon = if (toastIsError) Icons.Rounded.PriorityHigh else Icons.Rounded.Check,
            )
        }
    }
}

/** Un renglón del resumen del pedido: «2× Coca-Cola de vidrio». */
internal data class SummaryLine(
    val quantity: Int,
    val name: String,
)

internal data class StationSplit(
    val counter: List<SummaryLine>,
    val kitchen: List<SummaryLine>,
)

internal fun productStations(catalog: Catalog?): Map<String, PreparationStation> =
    catalog?.products?.associate { it.name.trim().lowercase() to it.preparationStation }.orEmpty()

/**
 * N08: parte «2× Coca-Cola, 1× Crepa de cajeta» en lo que se sirve en Caja y lo que hace Cocina, según la estación
 * de cada producto en el catálogo. Lo que no se reconoce cuenta como de Cocina, para nunca decir «llévalo ya» de más.
 */
internal fun splitByStation(
    itemsSummary: String,
    stations: Map<String, PreparationStation>,
): StationSplit {
    val lines =
        itemsSummary.split(",").mapNotNull { raw ->
            val text = raw.trim()
            if (text.isEmpty()) return@mapNotNull null
            val match = Regex("""^(\d+)\s*[×x]\s*(.+)$""").find(text)
            if (match !=
                null
            ) {
                SummaryLine(match.groupValues[1].toInt(), match.groupValues[2].trim())
            } else {
                SummaryLine(1, text)
            }
        }
    // R8: «ninguna» (sin preparación) también se recoge en Caja; solo lo desconocido o de Cocina espera a Cocina.
    val (counter, kitchen) =
        lines.partition { line -> stations[line.name.lowercase()].let { it != null && it != PreparationStation.KITCHEN } }
    return StationSplit(counter = counter, kitchen = kitchen)
}

/** N08: lo de Caja se lleva ya y la comida espera a Cocina; «Ya las llevé» es una marca solo de este teléfono. */
@Composable
private fun WaiterStationSplit(
    orderId: String,
    split: StationSplit,
    readOnly: Boolean,
    colors: OperationalColors,
) {
    val haptics = rememberVaiinillaHaptics()
    var carried by rememberSaveable(orderId) { mutableStateOf(false) }
    val blue = tileToneColor(TileTone.Turn, colors)
    val yolk = tileToneColor(TileTone.Kitchen, colors)
    val counterBg = animateSelectionColor(if (carried) colors.cardInner else blue.copy(alpha = 0.12f), "counter-part")
    val counterBorder = animateSelectionColor(if (carried) colors.cardBorder else blue, "counter-border")
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(counterBg)
                    .border(if (carried) 1.dp else 2.dp, counterBorder, RoundedCornerShape(20.dp))
                    .padding(14.dp),
        ) {
            StationHeader(
                icon = Icons.Outlined.LocalBar,
                tint = blue,
                title = if (carried) "Lo de Caja · ya lo llevaste" else "Lo de Caja · llévalo ya",
                subtitle = "No pasa por Cocina: lo recoges en Caja",
                colors = colors,
            )
            split.counter.forEach { StationLine(it, colors, done = carried) }
            if (!readOnly) {
                Row(
                    modifier =
                        Modifier
                            .padding(top = 10.dp)
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(CircleShape)
                            .background(if (carried) colors.buttonSecondary else blue)
                            .physicalPress {
                                if (carried) haptics.selection() else haptics.success()
                                carried = !carried
                            }.semantics {
                                contentDescription =
                                    if (carried) "Desmarcar lo de Caja" else "Ya llevé lo de Caja"
                            },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (carried) Icons.Outlined.Undo else Icons.Rounded.Check,
                        contentDescription = null,
                        tint = if (carried) colors.textPrimary else Color(0xFF0B2236),
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        if (carried) "Desmarcar" else "Ya lo llevé",
                        color = if (carried) colors.textPrimary else Color(0xFF0B2236),
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.cardInner)
                    .padding(14.dp),
        ) {
            StationHeader(
                icon = Icons.Outlined.Restaurant,
                tint = yolk,
                title = "Comida · la hace Cocina",
                subtitle = "Te avisamos cuando esté lista",
                colors = colors,
            )
            split.kitchen.forEach { StationLine(it, colors, done = false) }
        }
    }
}

@Composable
private fun StationHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    colors: OperationalColors,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF1B1B18), modifier = Modifier.size(20.dp))
        }
        Column {
            Text(title, color = colors.textPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Text(subtitle, color = colors.textSecondary, fontSize = 12.5.sp)
        }
    }
}

@Composable
private fun StationLine(
    line: SummaryLine,
    colors: OperationalColors,
    done: Boolean,
) {
    Text(
        "${line.quantity} × ${line.name}",
        color = if (done) colors.textMuted else colors.textPrimary,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        modifier = Modifier.padding(top = 8.dp),
    )
}

/**
 * Un pedido de la cuenta. Listo, se entrega con dos toques: el primero pregunta y el segundo
 * confirma, porque un toque por error lo marcaba entregado sin forma de deshacerlo.
 */
@Composable
internal fun WaiterSheetOrderRow(
    order: BoardOrder,
    readOnly: Boolean,
    canDeliver: Boolean,
    colors: OperationalColors,
    onDeliver: (BoardOrder) -> Unit,
    amount: String? = null,
    stations: Map<String, PreparationStation> = emptyMap(),
) {
    // La pregunta se queda hasta que el mesero confirme o cancele: si se iba sola, parecía que el botón no hacía nada.
    var confirming by remember(order.id) { mutableStateOf(false) }
    val split =
        remember(order.itemsSummary, stations) {
            splitByStation(order.itemsSummary, stations).takeIf { it.counter.isNotEmpty() && it.kitchen.isNotEmpty() }
        }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp).animateContentSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "#${order.folio} · ${order.clientName ?: "Cliente"}",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = colors.textPrimary,
                )
                Text(
                    listOfNotNull(order.itemsSummary.takeIf { it.isNotBlank() }, amount?.let { moneyLabel(it) })
                        .joinToString(" · "),
                    fontSize = 12.5.sp,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            when {
                // El servidor no entrega un pedido sin pagar: se avisa en lugar de ofrecer un botón que daría 409.
                order.state == OrderState.READY && order.mustCollectBeforeDelivery ->
                    OrderStateChip("Cobrar primero", colors.textPrimary, colors.pillBackground)
                order.state == OrderState.READY && canDeliver && !confirming ->
                    Box(
                        modifier =
                            Modifier
                                .clip(CircleShape)
                                .background(colors.accentLime)
                                .physicalPress(enabled = !readOnly, onClick = { confirming = true })
                                .padding(horizontal = 18.dp, vertical = 11.dp),
                    ) {
                        Text("Entregar", color = colors.accentInk, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                order.state == OrderState.PREPARING || order.state == OrderState.PAID -> {
                    val tone = tileToneColor(TileTone.Kitchen, colors)
                    OrderStateChip("EN COCINA", tone, tone.copy(alpha = 0.16f))
                }
                else -> OrderStateChip(order.state.label.uppercase(), colors.textSecondary, colors.pillBackground)
            }
        }
        if (split != null && (order.state == OrderState.PREPARING || order.state == OrderState.PAID)) {
            WaiterStationSplit(orderId = order.id, split = split, readOnly = readOnly, colors = colors)
        }
        // La confirmación va en su propio renglón: junto al pedido lo apretaba a una palabra por línea (I03).
        if (confirming && order.state == OrderState.READY && canDeliver) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { confirming = false }, enabled = !readOnly) {
                    Text("Cancelar", color = colors.textSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Button(
                    onClick = {
                        confirming = false
                        onDeliver(order)
                    },
                    enabled = !readOnly,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = colors.accentLime,
                            contentColor = colors.accentInk,
                        ),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text("¿Ya lo entregaste?", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun OrderStateChip(
    label: String,
    ink: Color,
    background: Color,
) {
    Text(
        label,
        color = ink,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.6.sp,
        maxLines = 1,
        modifier =
            Modifier
                .clip(CircleShape)
                .background(background)
                .padding(horizontal = 11.dp, vertical = 6.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WaiterDeliverSheet(
    order: BoardOrder,
    acting: Boolean,
    colors: OperationalColors,
    onDismiss: () -> Unit,
    onScan: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var code by remember(order.id) { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Entregar #${order.folio}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = colors.textPrimary,
                )
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.cardBackground)
                                .border(1.dp, colors.cardBorder, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Cerrar",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
            Text(
                "${order.clientName ?: "Cliente"} · ${order.itemsSummary}",
                color = colors.textSecondary,
                fontSize = 13.sp,
            )
            Button(
                onClick = onScan,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colors.accentLime,
                        contentColor = colors.accentInk,
                    ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                Text("Escanear el QR", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            OperationalField(
                value = code,
                onValueChange = { code = it },
                label = "Código del QR",
                colors = colors,
                placeholder = "Pega el código del QR",
            )
            Button(
                onClick = { onConfirm(code) },
                enabled = !acting && code.trim().isNotEmpty(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colors.buttonSecondary,
                        contentColor = colors.buttonSecondaryInk,
                    ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                Text(
                    if (acting) "Entregando…" else "Confirmar entrega",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
            Text(
                "Cancelar",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onDismiss() }
                        .padding(vertical = 6.dp),
                color = colors.textSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private val RESERVATION_HOUR =
    java.time.format.DateTimeFormatter
        .ofPattern("HH:mm")

/** "Reservada 19:00" para la siguiente reserva de la cancha, con la hora del negocio. */
private fun nextReservationCaption(startIso: String?): String? =
    startIso
        ?.let { runCatching { java.time.Instant.parse(it) }.getOrNull() }
        ?.let { "Reservada ${BusinessTime.format(it, RESERVATION_HOUR)}" }

/** Un renglón de la cuenta en la hoja: `live` si viene del tablero y se puede entregar desde aquí. */
internal data class WaiterSheetOrder(
    val order: BoardOrder,
    val amount: String?,
    val live: Boolean,
)

/**
 * Junta lo del tablero (en curso, con su estado al momento) con lo de la cuenta (lo que cuesta y lo ya
 * entregado). Lo listo va primero; lo entregado, al final.
 */
internal fun waiterSheetOrders(
    board: List<BoardOrder>,
    account: SpaceAccount?,
): List<WaiterSheetOrder> {
    val totals = account?.orders?.associate { it.id to it.total }.orEmpty()
    val live =
        board
            .sortedBy { if (it.state == OrderState.READY) 0 else 1 }
            .map { WaiterSheetOrder(it, totals[it.id], live = true) }
    val boardIds = board.map { it.id }.toSet()
    val settledOnes =
        account
            ?.orders
            .orEmpty()
            .filter { it.id !in boardIds }
            .map { accountOrder ->
                WaiterSheetOrder(
                    order =
                        BoardOrder(
                            id = accountOrder.id,
                            folio = accountOrder.folio,
                            state =
                                OrderState.entries.firstOrNull { it.wireValue == accountOrder.state }
                                    ?: OrderState.DELIVERED,
                            version = 0,
                            clientName = accountOrder.clientName,
                            itemsSummary = accountOrder.itemsSummary,
                            updatedAt = "",
                        ),
                    amount = accountOrder.total,
                    live = false,
                )
            }
    return live + settledOnes
}

/** Una mesa que llama: qué pide, desde cuándo y lo último que pidió, con «Voy en camino» y «Ya la atendí». */
@Composable
private fun WaiterCallCard(
    call: TableCall,
    table: BoardTable,
    tick: Int,
    enabled: Boolean,
    colors: OperationalColors,
    onGoing: () -> Unit,
    onAttended: () -> Unit,
) {
    val going = call.status == CallStatus.EN_CAMINO
    val since = remember(call, tick) { minutesSince(call.createdAt, Instant.now()) }
    val last = table.orders.maxByOrNull { it.updatedAt }
    val lastCaption =
        remember(last, tick) {
            last?.let { "#${it.folio} · ${it.state.label.lowercase()}, ${minutesSince(it.updatedAt, Instant.now())}" }
        }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(64.dp)
                    .pulseGlow(active = !going, color = WaiterCoral, spread = 8.dp, corner = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(WaiterCoral),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Notifications,
                contentDescription = null,
                tint = Color(0xFF16160F),
                modifier = Modifier.size(30.dp),
            )
        }
        Text(
            buildString {
                append(if (going) "Va ${call.takenBy?.name ?: "alguien"}" else "Llamando")
                append(" · ")
                append(if (since == "ahora") "ahora" else "hace $since")
            }.uppercase(),
            color = colors.textSecondary,
            fontWeight = FontWeight.Black,
            fontSize = 11.5.sp,
            letterSpacing = 1.4.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            "${call.space.name} te llama",
            color = colors.textPrimary,
            fontWeight = FontWeight.Black,
            fontSize = 30.sp,
            lineHeight = 33.sp,
            letterSpacing = (-1).sp,
            textAlign = TextAlign.Center,
        )
        Text(
            listOfNotNull(call.reason.staffLabel, call.clientName).joinToString(" · "),
            color = colors.textSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        lastCaption?.let { caption ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.cardInner)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Último pedido", color = colors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    caption,
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (!going) {
            CallButton("Voy en camino", primary = true, enabled = enabled, colors = colors, onClick = onGoing)
        }
        CallButton("Ya la atendí", primary = going, enabled = enabled, colors = colors, onClick = onAttended)
        if (!going) {
            Text(
                "«Voy en camino» le avisa a la mesa que ya la viste.",
                color = colors.textSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CallButton(
    label: String,
    primary: Boolean,
    enabled: Boolean,
    colors: OperationalColors,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .height(56.dp)
                .clip(CircleShape)
                .background(if (primary) colors.textPrimary else Color.Transparent)
                .border(1.dp, if (primary) Color.Transparent else colors.cardBorder, CircleShape)
                .physicalPress(enabled = enabled, onClick = onClick)
                .graphicsLayer { alpha = if (enabled) 1f else 0.5f },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (primary) colors.background else colors.textPrimary,
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
        )
    }
}
