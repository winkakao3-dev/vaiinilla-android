package com.vaiinilla.app.ui.screens

import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.mode.RestrictedMode
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.repository.BoardOrder
import com.vaiinilla.app.domain.repository.BoardTable
import com.vaiinilla.app.domain.repository.CallStatus
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.domain.repository.TableState
import com.vaiinilla.app.domain.repository.sortWaiterTables
import com.vaiinilla.app.domain.repository.tableState
import com.vaiinilla.app.ui.components.OperationalAssistantHost
import com.vaiinilla.app.ui.components.OperationalAssistantPalette
import com.vaiinilla.app.ui.components.VaiinillaAssistantButton
import com.vaiinilla.app.ui.components.VaiinillaMark
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaiterOperationalScreen(
    state: WaiterUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit = {},
    onGoing: (TableCall) -> Unit = {},
    onAttended: (TableCall) -> Unit = {},
    onDeliver: (BoardOrder, String) -> Unit = { _, _ -> },
    onFilterAttending: (Boolean) -> Unit = {},
    newCallTable: TableCall? = null,
    onAlertConsumed: () -> Unit = {},
    onChangeMode: (() -> Unit)? = null,
    restrictedMode: RestrictedMode? = null,
    placeName: String = "",
    assistantUserKey: String = "waiter",
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
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
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
                                "Cuenta de mesero",
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
                                "MS",
                                color = colors.background,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        "Mesas en vivo",
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.6.sp,
                    )
                    Text(
                        if (placeName.isNotBlank()) "Mesas · $placeName" else "Mesas",
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        letterSpacing = (-1.2).sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        "Llamadas primero. Toca una mesa para atenderla.",
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    WaiterSummaryPill(
                        count = calling,
                        label = "llamando",
                        highlighted = calling > 0,
                        colors = colors,
                    )
                    WaiterSummaryPill(
                        count = readyCount,
                        label = if (readyCount == 1) "listo" else "listos",
                        highlighted = readyCount > 0,
                        colors = colors,
                    )
                    WaiterSummaryPill(
                        count = activeCount,
                        label = "activas",
                        highlighted = false,
                        colors = colors,
                    )
                }
            }

            item {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .background(colors.cardInner)
                            .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    listOf(false to "Todas", true to "Por atender").forEach { (value, label) ->
                        val selected = state.filterAttending == value
                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .clip(CircleShape)
                                    .background(if (selected) colors.accentLime else Color.Transparent)
                                    .clickable {
                                        haptics.selection()
                                        onFilterAttending(value)
                                    }.padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (selected) colors.accentInk else colors.textSecondary,
                            )
                        }
                    }
                }
            }

            if (!state.callsEnabled) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = colors.cardBackground,
                        modifier = Modifier.fillMaxWidth().border(1.dp, colors.cardBorder, RoundedCornerShape(18.dp)),
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
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.cardBackground,
                        modifier = Modifier.fillMaxWidth().border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp)),
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
                items(6) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.cardBackground,
                        modifier = Modifier.fillMaxWidth().border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp)),
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().height(76.dp))
                    }
                }
            } else {
                items(sorted, key = { it.space.id }) { table ->
                    WaiterTableCard(
                        table = table,
                        tick = tick,
                        colors = colors,
                        modifier = Modifier.animateItem(),
                        onClick = {
                            haptics.selection()
                            openTableId = table.space.id
                        },
                    )
                }
                if (sorted.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(26.dp),
                            color = colors.cardBackground,
                            modifier =
                                Modifier.fillMaxWidth().border(
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

        AnimatedVisibility(
            visible = state.toastMessage != null,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.textPrimary,
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier.size(20.dp).clip(CircleShape).background(colors.accentLime),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = colors.accentInk,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                    Text(
                        state.toastMessage.orEmpty(),
                        color = colors.background,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

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
            onDismiss = { openTableId = null },
            onGoing = { onGoing(it) },
            onAttended = { onAttended(it) },
            onDeliver = {
                openTableId = null
                delivering = it
            },
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

@Composable
private fun WaiterSummaryPill(
    count: Int,
    label: String,
    highlighted: Boolean,
    colors: OperationalColors,
) {
    Box(
        modifier =
            Modifier
                .clip(CircleShape)
                .background(if (highlighted) colors.accentLime else colors.cardBackground)
                .border(1.dp, if (highlighted) colors.accentLime else colors.cardBorder, CircleShape)
                .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            "$count $label",
            color = if (highlighted) colors.accentInk else colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun WaiterTableCard(
    table: BoardTable,
    tick: Int,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val tableState = table.tableState()
    val readyOrders = table.orders.filter { it.state == OrderState.READY }
    // tick entra en la clave para que el "hace 0:42" avance cada segundo.
    val subtitle = remember(table, readyOrders, tick) { waiterTableSubtitle(table, readyOrders) }
    val border =
        when (tableState) {
            TableState.CALL -> colors.highlightBorder
            TableState.READY -> colors.highlightBorder
            else -> colors.cardBorder
        }
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .border(
                    if (tableState == TableState.CALL ||
                        tableState == TableState.READY
                    ) {
                        2.dp
                    } else {
                        1.dp
                    },
                    border,
                    RoundedCornerShape(20.dp),
                ).clickable(onClick = onClick)
                .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        color = colors.cardBackground,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(width = 56.dp, height = 56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (tableState == TableState.CALL || tableState == TableState.READY) {
                                colors.accentLime
                            } else {
                                colors.textPrimary
                            },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    waiterShortName(table.space.name),
                    color =
                        if (tableState == TableState.CALL ||
                            tableState == TableState.READY
                        ) {
                            colors.accentInk
                        } else {
                            colors.background
                        },
                    fontWeight = FontWeight.Black,
                    fontSize = 19.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(modifier = Modifier.weight(1f).animateContentSize()) {
                Text(
                    table.space.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 1.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier =
                    Modifier
                        .clip(CircleShape)
                        .background(colors.pillBackground)
                        .border(1.dp, colors.pillBorder, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    when (tableState) {
                        TableState.CALL -> "Llamando"
                        TableState.READY -> "Listo"
                        TableState.ACTIVE -> "${table.orders.size}"
                        TableState.FREE -> "Libre"
                    },
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun waiterTableSubtitle(
    table: BoardTable,
    readyOrders: List<BoardOrder>,
): String {
    val call = table.call
    return when {
        call != null -> {
            val whenText =
                if (call.status == CallStatus.EN_CAMINO) {
                    "Va ${call.takenBy?.name ?: "alguien"}"
                } else {
                    "Llamando · ${waiterSince(call.createdAt)}"
                }
            "$whenText · ${call.reason.staffLabel}"
        }
        readyOrders.isNotEmpty() -> "#${readyOrders.joinToString(", #") { it.folio.toString() }} listo"
        table.orders.isNotEmpty() -> "${table.orders.size} ${if (table.orders.size == 1) "pedido" else "pedidos"}"
        else -> "Libre"
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
    onDismiss: () -> Unit,
    onGoing: (TableCall) -> Unit,
    onAttended: (TableCall) -> Unit,
    onDeliver: (BoardOrder) -> Unit,
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
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.cardBackground,
                    modifier = Modifier.fillMaxWidth().border(1.dp, colors.cardBorder, RoundedCornerShape(18.dp)),
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
