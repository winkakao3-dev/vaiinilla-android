package com.vaiinilla.app.ui.screens

import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.mode.RestrictedMode
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.SpaceCopy
import com.vaiinilla.app.domain.repository.AccountCollection
import com.vaiinilla.app.domain.repository.BoardOrder
import com.vaiinilla.app.domain.repository.BoardTable
import com.vaiinilla.app.domain.repository.CallStatus
import com.vaiinilla.app.domain.repository.SpaceAvailabilityState
import com.vaiinilla.app.domain.repository.SpaceSessionDetail
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.domain.repository.TableState
import com.vaiinilla.app.domain.repository.sortWaiterTables
import com.vaiinilla.app.domain.repository.tableState
import com.vaiinilla.app.ui.components.NumberTicker
import com.vaiinilla.app.ui.components.OperationalAssistantHost
import com.vaiinilla.app.ui.components.OperationalAssistantPalette
import com.vaiinilla.app.ui.components.SlidingSegments
import com.vaiinilla.app.ui.components.ToastPill
import com.vaiinilla.app.ui.components.TurnProgressBar
import com.vaiinilla.app.ui.components.VaiinillaAssistantButton
import com.vaiinilla.app.ui.components.VaiinillaMark
import com.vaiinilla.app.ui.components.animateSelectionColor
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberAssistantAnchorRegistry
import com.vaiinilla.app.ui.components.rememberOperationalAssistantController
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.components.waiterAssistantGuides
import com.vaiinilla.app.ui.discovery.QrScannerDialog
import com.vaiinilla.app.ui.operational.WaiterUiState
import com.vaiinilla.app.ui.theme.LocalVaiinillaThemeMode
import com.vaiinilla.app.ui.theme.LocalVaiinillaThemeModeChanger
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import kotlinx.coroutines.delay
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
    onCollectAccount: (Int, String, String, List<String>?) -> Unit = { _, _, _, _ -> },
    onDismissCollection: () -> Unit = {},
    /** Renta de mostrador de una cancha con precio: aparta (ahora o renovación) y abre el cobro. */
    onStartRental: (Int, Int, Instant?) -> Unit = { _, _, _ -> },
    onConfirmRental: (String) -> Unit = {},
    onCancelRental: () -> Unit = {},
    onFilterAttending: (Boolean) -> Unit = {},
    newCallTable: TableCall? = null,
    onAlertConsumed: () -> Unit = {},
    onChangeMode: (() -> Unit)? = null,
    restrictedMode: RestrictedMode? = null,
    placeName: String = "",
    assistantUserKey: String = "waiter",
    /** Quién usa el tablero: el mesero, o Caja cuando entra a cobrar cuentas. */
    kicker: String = "MESERO",
    roleSubtitle: String = "Cuenta de mesero",
    roleChip: String = "MS",
) {
    val colors = rememberOperationalColors()
    val themeMode = LocalVaiinillaThemeMode.current
    val themeChanger = LocalVaiinillaThemeModeChanger.current
    val haptics = rememberVaiinillaHaptics()
    val context = LocalContext.current
    var openTableId by remember { mutableStateOf<Int?>(null) }
    var delivering by remember { mutableStateOf<BoardOrder?>(null) }
    var scannerOpen by remember { mutableStateOf(false) }
    var assistantPulse by remember { mutableIntStateOf(0) }
    var tick by remember { mutableIntStateOf(0) }
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
            playWaiterAlert(context)
            onAlertConsumed()
        }
    }

    val sorted =
        remember(state.tables, state.filterAttending) {
            val list = sortWaiterTables(state.tables)
            if (state.filterAttending) {
                list.filter { it.tableState() == TableState.CALL || it.tableState() == TableState.READY }
            } else {
                list
            }
        }
    val calling = state.tables.count { it.tableState() == TableState.CALL }
    val readyCount = state.tables.sumOf { table -> table.orders.count { it.state == OrderState.READY } }
    val activeCount = state.tables.count { it.orders.isNotEmpty() }
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
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = fullWidth) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        VaiinillaMark(modifier = Modifier.size(34.dp))
                        Column {
                            Text(
                                "Vaiinilla",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = colors.textPrimary,
                            )
                            Text(
                                roleSubtitle,
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
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
                        IconButton(
                            onClick = {
                                haptics.selection()
                                val next =
                                    when (themeMode) {
                                        VaiinillaThemeMode.Light -> VaiinillaThemeMode.Dark
                                        VaiinillaThemeMode.Dark -> VaiinillaThemeMode.Amoled
                                        VaiinillaThemeMode.Amoled -> VaiinillaThemeMode.Light
                                        VaiinillaThemeMode.System -> VaiinillaThemeMode.Dark
                                    }
                                themeChanger?.invoke(next)
                            },
                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.cardBackground)
                                    .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp)),
                        ) {
                            Icon(
                                imageVector =
                                    when (themeMode) {
                                        VaiinillaThemeMode.Light -> Icons.Outlined.LightMode
                                        VaiinillaThemeMode.Dark -> Icons.Outlined.DarkMode
                                        VaiinillaThemeMode.Amoled -> Icons.Outlined.Diamond
                                        VaiinillaThemeMode.System -> Icons.Outlined.AutoMode
                                    },
                                contentDescription = "Cambiar tema",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Box(
                            modifier =
                                Modifier
                                    .size(width = 48.dp, height = 40.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.textPrimary)
                                    .clickable {
                                        haptics.impact()
                                        onChangeMode?.invoke() ?: onBack()
                                    },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                roleChip,
                                color = colors.background,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
            }

            item(span = fullWidth) {
                WaiterTitleBlock(
                    kicker = kicker,
                    placeName = placeName,
                    online = state.errorMessage == null || state.tables.isNotEmpty(),
                    colors = colors,
                )
            }

            item(span = fullWidth) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    WaiterSummaryCard(
                        count = calling,
                        label = "llamando",
                        kind = if (calling > 0) SummaryKind.Call else SummaryKind.Plain,
                        colors = colors,
                        modifier = Modifier.weight(1f),
                    )
                    WaiterSummaryCard(
                        count = readyCount,
                        label = if (readyCount == 1) "listo" else "listos",
                        kind = if (readyCount > 0) SummaryKind.Ready else SummaryKind.Plain,
                        colors = colors,
                        modifier = Modifier.weight(1f),
                    )
                    WaiterSummaryCard(
                        count = activeCount,
                        label = "activas",
                        kind = SummaryKind.Plain,
                        colors = colors,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item(span = fullWidth) {
                SlidingSegments(
                    labels = listOf("Todas", "Por atender"),
                    selectedIndex = if (state.filterAttending) 1 else 0,
                    onSelect = { index ->
                        haptics.selection()
                        onFilterAttending(index == 1)
                    },
                    trackColor = colors.cardBackground,
                    indicatorColor = colors.textPrimary,
                    selectedContentColor = colors.background,
                    idleContentColor = colors.textSecondary,
                    modifier = Modifier.fillMaxWidth(),
                    verticalPadding = 9.dp,
                    cornerRadius = 14.dp,
                    borderColor = colors.cardBorder,
                    indicatorDamping = 0.6f,
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
                            Button(
                                onClick = onRefresh,
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = colors.accentLime,
                                        contentColor = colors.accentInk,
                                    ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(48.dp),
                            ) {
                                Text("Reintentar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            if (state.loading && state.tables.isEmpty()) {
                items(9) {
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
                items(sorted, key = { it.space.id }) { table ->
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
                if (sorted.isEmpty()) {
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

        ToastPill(
            message = state.toastMessage,
            containerColor = colors.textPrimary,
            contentColor = colors.background,
            badgeColor = colors.accentLime,
            badgeContentColor = colors.accentInk,
        )

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

    if (open != null) {
        WaiterTableSheet(
            table = open,
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
                if (state.deliveryRequiresQr) {
                    openTableId = null
                    onCloseSpace()
                    delivering = it
                } else {
                    // El establecimiento dispensa el QR en espacios: se confirma la entrega y listo.
                    onDeliver(it, null)
                }
            },
            deliveryRequiresQr = state.deliveryRequiresQr,
            onOpenTurn = { onOpenTurn(open.space.id, it) },
            onExtendTurn = { onExtendTurn(open.space.id, it) },
            rentable = open.availability?.rentable == true,
            onStartRental = { minutes, start -> onStartRental(open.space.id, minutes, start) },
            onReleaseSpace = { onReleaseSpace(open.space.id) },
            onCollectAccount = { received, total, ids -> onCollectAccount(open.space.id, received, total, ids) },
            onDismissCollection = onDismissCollection,
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

    delivering?.let { order ->
        WaiterDeliverSheet(
            order = order,
            acting = state.acting,
            colors = colors,
            onDismiss = { delivering = null },
            onScan = { scannerOpen = true },
            onConfirm = { code ->
                onDeliver(order, code)
                delivering = null
            },
        )
    }

    val scannerOrder = delivering
    if (scannerOpen && scannerOrder != null) {
        QrScannerDialog(
            onClose = { scannerOpen = false },
            helperText = "Apunta al QR del pedido del cliente",
            onPayload = { raw ->
                scannerOpen = false
                onDeliver(scannerOrder, raw)
                delivering = null
            },
        )
    }
}

private val WaiterCoral = Color(0xFFE5645A)
private val WaiterTileHeight = 116.dp

private val fullWidth: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }

private enum class SummaryKind { Plain, Call, Ready }

/** Kicker con guion lima, título grande y el estado de conexión: la cabecera del tablero web. */
@Composable
private fun WaiterTitleBlock(
    kicker: String,
    placeName: String,
    online: Boolean,
    colors: OperationalColors,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(width = 18.dp, height = 3.dp).clip(CircleShape).background(colors.accentLime))
                Text(
                    if (placeName.isBlank()) kicker else "$kicker · ${placeName.uppercase()}",
                    color = colors.textSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.6.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                "Mesas",
                color = colors.textPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 40.sp,
                letterSpacing = (-1.5).sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        WaiterLivePill(online = online, colors = colors)
    }
}

@Composable
private fun WaiterLivePill(
    online: Boolean,
    colors: OperationalColors,
) {
    val dot = animateSelectionColor(if (online) colors.accentLime else WaiterCoral, "live-dot")
    Row(
        modifier =
            Modifier
                .clip(CircleShape)
                .background(colors.cardBackground)
                .border(1.dp, colors.cardBorder, CircleShape)
                .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .pulseGlow(active = online, color = colors.accentLime, spread = 8.dp, corner = 4.dp)
                .clip(CircleShape)
                .background(dot),
        )
        Text(
            if (online) "En vivo" else "Sin conexión",
            color = colors.textSecondary,
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
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

@Composable
private fun WaiterSummaryCard(
    count: Int,
    label: String,
    kind: SummaryKind,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
) {
    val background =
        animateSelectionColor(
            when (kind) {
                SummaryKind.Call -> lerp(colors.cardBackground, WaiterCoral, 0.14f)
                SummaryKind.Ready -> lerp(colors.cardBackground, colors.accentLime, 0.22f)
                SummaryKind.Plain -> colors.cardBackground
            },
            "summary-background",
        )
    val border =
        animateSelectionColor(
            when (kind) {
                SummaryKind.Call -> WaiterCoral.copy(alpha = 0.45f)
                SummaryKind.Ready -> colors.accentLime.copy(alpha = 0.6f)
                SummaryKind.Plain -> colors.cardBorder
            },
            "summary-border",
        )
    val labelColor =
        animateSelectionColor(
            if (kind == SummaryKind.Call) WaiterCoral else colors.textSecondary,
            "summary-label",
        )
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(16.dp))
                .background(background)
                .border(1.dp, border, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        NumberTicker(value = count, label = "summary-count") { value ->
            Text(
                "$value",
                color = colors.textPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 26.sp,
            )
        }
        Text(label, color = labelColor, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Mosaico de una mesa: número grande arriba, estado abajo; el color cuenta la urgencia. */
@Composable
private fun WaiterTile(
    table: BoardTable,
    tick: Int,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val state = table.tableState()
    val going = table.call?.status == CallStatus.EN_CAMINO
    val ringing = state == TableState.CALL && !going
    val shape = RoundedCornerShape(20.dp)
    val readyOrders = table.orders.filter { it.state == OrderState.READY }
    // tick entra en la clave para que el "0:42" avance cada segundo.
    val caption = remember(table, readyOrders, tick) { waiterTileCaption(table, readyOrders, tick) }
    val container =
        animateSelectionColor(
            when {
                ringing -> WaiterCoral
                state == TableState.CALL -> lerp(colors.cardBackground, WaiterCoral, 0.18f)
                state == TableState.READY -> lerp(colors.cardBackground, colors.accentLime, 0.28f)
                state == TableState.ACTIVE -> colors.cardBackground
                else -> Color.Transparent
            },
            "tile-container",
        )
    val border =
        animateSelectionColor(
            when (state) {
                TableState.CALL -> WaiterCoral
                TableState.READY -> colors.accentLime.copy(alpha = 0.7f)
                else -> colors.cardBorder
            },
            "tile-border",
        )
    val numberColor = animateSelectionColor(if (ringing) Color.White else colors.textPrimary, "tile-number")
    val captionColor =
        animateSelectionColor(
            when {
                ringing -> Color.White.copy(alpha = 0.9f)
                state == TableState.CALL -> WaiterCoral
                state == TableState.READY -> colors.textPrimary
                else -> colors.textSecondary
            },
            "tile-caption",
        )
    val alpha by animateFloatAsState(
        targetValue = if (state == TableState.FREE) 0.5f else 1f,
        animationSpec = if (reducedMotion()) snap() else spring(stiffness = Spring.StiffnessMediumLow),
        label = "tile-alpha",
    )
    Column(
        modifier =
            modifier
                .physicalPress(onClick = onClick)
                .pulseGlow(active = ringing, color = WaiterCoral, spread = 7.dp, corner = 20.dp)
                .graphicsLayer { this.alpha = alpha }
                .height(WaiterTileHeight)
                .clip(shape)
                .background(container)
                .border(1.dp, border, shape)
                .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Mesa y cancha pueden compartir número: el tipo va junto al número.
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                waiterShortName(table.space.name),
                color = numberColor,
                fontWeight = FontWeight.Black,
                fontSize = 30.sp,
                letterSpacing = (-0.9).sp,
                lineHeight = 32.sp,
                maxLines = 1,
            )
            Text(
                SpaceCopy.kindLabel(table.space.type).uppercase(),
                color = captionColor,
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp,
                letterSpacing = 0.8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 6.dp, bottom = 5.dp),
            )
        }
        Column {
            Text(
                caption,
                color = captionColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.5.sp,
                lineHeight = 14.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            // Avance del turno: se ve de un vistazo cuánto le queda a cada cancha.
            val progress = spaceTileProgress(table.availability, tick)
            if (progress != null) {
                val overdue =
                    table.availability?.state.let {
                        it == SpaceAvailabilityState.EN_GRACIA || it == SpaceAvailabilityState.POR_COBRAR
                    }
                TurnProgressBar(
                    fraction = progress,
                    track = if (ringing) Color.White.copy(alpha = 0.3f) else colors.pillBackground,
                    fill =
                        when {
                            table.availability?.state == SpaceAvailabilityState.POR_COBRAR -> WaiterCoral
                            overdue -> Color(0xFFE9A23B)
                            else -> colors.accentLime
                        },
                    modifier = Modifier.padding(top = 7.dp),
                    height = 4.dp,
                )
            }
        }
    }
}

private fun waiterTileCaption(
    table: BoardTable,
    readyOrders: List<BoardOrder>,
    tick: Int,
): String {
    val call = table.call
    return when {
        call != null -> {
            val whenText =
                if (call.status == CallStatus.EN_CAMINO) {
                    "Va ${call.takenBy?.name ?: "alguien"}"
                } else {
                    "Llamando"
                }
            "$whenText · ${waiterSince(call.createdAt)}\n${call.reason.staffLabel}"
        }
        readyOrders.isNotEmpty() -> {
            // Una cancha con un pedido listo sigue teniendo turno: no se pierde el tiempo restante.
            val ready = "#${readyOrders.joinToString(", #") { it.folio.toString() }} listo"
            spaceAvailabilityCaption(table.availability, tick)?.let { "$ready\n$it" } ?: ready
        }
        else -> {
            val occupied = spaceAvailabilityCaption(table.availability, tick)
            val next = nextReservationCaption(table.availability?.nextReservationStart)
            when {
                occupied != null -> listOfNotNull(occupied, next).joinToString("\n")
                table.orders.isNotEmpty() ->
                    "${table.orders.size} ${if (table.orders.size == 1) "pedido" else "pedidos"}"
                next != null -> "Libre\n$next"
                else -> "Libre"
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

internal fun waiterShortName(name: String): String {
    val match = Regex("(\\d+)\\s*$").find(name)
    return match?.groupValues?.getOrNull(1) ?: name
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WaiterTableSheet(
    table: BoardTable,
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
    onOpenTurn: (Int?) -> Unit,
    onExtendTurn: (Int) -> Unit,
    onReleaseSpace: () -> Unit,
    onCollectAccount: (String, String, List<String>?) -> Unit,
    onDismissCollection: () -> Unit,
    rentable: Boolean = false,
    onStartRental: (Int, Instant?) -> Unit = { _, _ -> },
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
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 36.dp)
                    .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    table.space.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = colors.textPrimary,
                )
                Box(
                    modifier =
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colors.cardBackground)
                            .border(1.dp, colors.cardBorder, CircleShape)
                            .clickable { onDismiss() },
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
            Text(
                if (table.orders.isEmpty()) {
                    "Sin pedidos en curso"
                } else {
                    "${table.orders.size} ${if (table.orders.size == 1) "pedido" else "pedidos"} en curso"
                },
                color = colors.textSecondary,
                fontSize = 13.sp,
            )
            val call = table.call
            if (call != null) {
                val going = call.status == CallStatus.EN_CAMINO
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color =
                        animateSelectionColor(
                            if (going) colors.cardBackground else lerp(colors.cardBackground, WaiterCoral, 0.14f),
                            "sheet-call-background",
                        ),
                    modifier =
                        Modifier.fillMaxWidth().border(
                            1.dp,
                            animateSelectionColor(
                                if (going) colors.cardBorder else WaiterCoral.copy(alpha = 0.4f),
                                "sheet-call-border",
                            ),
                            RoundedCornerShape(18.dp),
                        ),
                ) {
                    Column(modifier = Modifier.padding(16.dp).animateContentSize()) {
                        Text(
                            call.reason.staffLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colors.textPrimary,
                        )
                        val callSubtitle =
                            remember(call, tick) {
                                buildString {
                                    append(call.clientName ?: "Cliente")
                                    append(" · hace ${waiterSince(call.createdAt)}")
                                    if (call.takenBy != null) append(" · va ${call.takenBy.name}")
                                }
                            }
                        Text(
                            callSubtitle,
                            color = colors.textSecondary,
                            fontSize = 12.5.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Button(
                            onClick = {
                                if (call.status == CallStatus.PENDIENTE) onGoing(call) else onAttended(call)
                            },
                            enabled = !acting && !readOnly,
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = colors.accentLime,
                                    contentColor = colors.accentInk,
                                ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(50.dp),
                        ) {
                            Text(
                                if (call.status == CallStatus.PENDIENTE) "Voy" else "Atendida",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
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
                onCollect = onCollectAccount,
                onRelease = onReleaseSpace,
                onDismissCollection = onDismissCollection,
                rentable = rentable,
                onRent = onStartRental,
            )
            table.orders.forEach { order ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.cardBackground,
                    modifier = Modifier.fillMaxWidth().border(1.dp, colors.cardBorder, RoundedCornerShape(18.dp)),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp).animateContentSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "#${order.folio} · ${order.clientName ?: "Cliente"}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = colors.textPrimary,
                            )
                            Text(
                                order.itemsSummary,
                                fontSize = 12.5.sp,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        if (order.state == OrderState.READY) {
                            Button(
                                onClick = { onDeliver(order) },
                                enabled = !readOnly,
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = colors.accentLime,
                                        contentColor = colors.accentInk,
                                    ),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            ) {
                                Text("Entregar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        } else {
                            Box(
                                modifier =
                                    Modifier
                                        .clip(CircleShape)
                                        .background(colors.pillBackground)
                                        .border(1.dp, colors.pillBorder, CircleShape)
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                            ) {
                                Text(
                                    order.state.label,
                                    color = colors.textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
            Text(
                "Cerrar",
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
                    modifier =
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colors.cardBackground)
                            .border(1.dp, colors.cardBorder, CircleShape)
                            .clickable { onDismiss() },
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
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Código del QR") },
                placeholder = { Text("Pega el código del QR") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
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

private fun playWaiterAlert(context: Context) {
    try {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        RingtoneManager.getRingtone(context, uri)?.play()
    } catch (_: Exception) {
    }
    try {
        val pattern = longArrayOf(0, 120, 80, 120)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(VibratorManager::class.java)
            val vibrator = manager?.defaultVibrator
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, -1)
            }
        }
    } catch (_: Exception) {
    }
}

private val RESERVATION_HOUR =
    java.time.format.DateTimeFormatter
        .ofPattern("HH:mm")

/** "Reservada 19:00" para la siguiente reserva de la cancha, con la hora local del teléfono. */
private fun nextReservationCaption(startIso: String?): String? =
    startIso
        ?.let { runCatching { java.time.Instant.parse(it) }.getOrNull() }
        ?.let { "Reservada ${it.atZone(java.time.ZoneId.systemDefault()).format(RESERVATION_HOUR)}" }
