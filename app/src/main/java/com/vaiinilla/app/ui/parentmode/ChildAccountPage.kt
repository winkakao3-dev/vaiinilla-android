package com.vaiinilla.app.ui.parentmode

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.vaiinilla.app.domain.guardianship.ChildMovement
import com.vaiinilla.app.domain.guardianship.Dependent
import com.vaiinilla.app.domain.guardianship.ScheduledOrder
import com.vaiinilla.app.domain.guardianship.ScheduledOrderRules
import com.vaiinilla.app.domain.guardianship.SchoolCalendar
import com.vaiinilla.app.domain.guardianship.compactMoney
import com.vaiinilla.app.ui.components.GlassCircleButton
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ToastPill
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.minTouchTarget
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.screens.movementLabel
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

/** Cada cuánto pregunta la ficha por una solicitud que espera a la escuela. */
private const val REQUESTS_POLL_MS = 20_000L

internal enum class ChildSheet { RELOAD, CONTROLS, WEEK, OWN_ACCOUNT, GUARDIAN_CODE, REFUND, DATA_REMOVAL, PIN }

/** El durazno del hijo en Doop: su ficha, su recarga y la página de seguridad lo usan igual. */
internal val ChildAvatarFill = Color(0xFFF6C9A8)
internal val ChildAvatarInk = Color(0xFF4A2412)

/**
 * La ficha de un hijo como el dibujo de Doop (E07): su saldo con la barra de hoy, cuatro botones redondos, su comida
 * apartada y lo que cuida la caja. Lo de seguridad y tutores vive en su propia página (E11).
 */
@Composable
internal fun ChildAccountPage(
    child: Dependent,
    services: ParentModeServices,
    onChildChanged: (Dependent) -> Unit,
    onUnlinked: (String) -> Unit,
    onBack: () -> Unit,
    /** Recién agregado y el tutor eligió recargarle: la ficha abre con la hoja de recarga. */
    startWithReload: Boolean = false,
    /** El monto que eligió en el paso 3 de agregarlo. */
    initialReloadAmount: Int? = null,
    /** Llegó por el aviso de un pedido: la ficha abre ese pedido para verlo (P08). */
    initialOrderId: String? = null,
) {
    val state = remember(child.userId) { ChildAccountState(child, services) }
    ChildAccountContent(
        state = state,
        onChildChanged = onChildChanged,
        onUnlinked = onUnlinked,
        onBack = onBack,
        startWithReload = startWithReload,
        initialReloadAmount = initialReloadAmount,
        initialOrderId = initialOrderId,
    )
}

@Composable
internal fun ChildAccountContent(
    state: ChildAccountState,
    onChildChanged: (Dependent) -> Unit,
    onUnlinked: (String) -> Unit,
    onBack: () -> Unit,
    startWithReload: Boolean = false,
    initialReloadAmount: Int? = null,
    startOnSecurity: Boolean = false,
    initialOrderId: String? = null,
) {
    val colors = LocalVaiinillaColors.current
    val reduced = reducedMotion()
    val scope = rememberCoroutineScope()
    var sheet by rememberSaveable { mutableStateOf(if (startWithReload) ChildSheet.RELOAD else null) }
    var showSecurity by rememberSaveable { mutableStateOf(startOnSecurity) }
    // Lo que pidió el hijo, solo para verlo: desde su comida apartada o desde el aviso del pedido (P08).
    var openOrderId by rememberSaveable { mutableStateOf(initialOrderId) }
    // Otro aviso de pedido con la ficha ya abierta: abre ese pedido.
    LaunchedEffect(initialOrderId) { initialOrderId?.let { openOrderId = it } }
    var confirmsBlock by remember { mutableStateOf(false) }
    // Solo la primera recarga llega con el monto elegido; si la cierra y la vuelve a abrir, empieza de cero.
    var pendingReloadAmount by rememberSaveable { mutableStateOf(initialReloadAmount) }
    var orderToCancel by remember { mutableStateOf<ScheduledOrder?>(null) }
    LaunchedEffect(state) { state.load() }
    // La lista de hijos lee el saldo de `child`: se mantiene al día con la cartera que esta ficha acaba de ver.
    val liveBalance = state.wallet?.balance
    LaunchedEffect(liveBalance) {
        if (liveBalance != null && state.child.balance?.compareTo(liveBalance) != 0) {
            val updated = state.child.copy(balance = liveBalance)
            state.updateChild(updated)
            onChildChanged(updated)
        }
    }
    // Con una devolución o baja esperando a la escuela, la ficha pregunta de vez en cuando (solo a la vista).
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(state, state.hasPendingRequest) {
        if (!state.hasPendingRequest) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (state.hasPendingRequest) {
                delay(REQUESTS_POLL_MS)
                state.refreshRequests()
            }
        }
    }
    LaunchedEffect(state.notice) {
        if (state.notice != null) {
            delay(2600)
            state.dismissNotice()
        }
    }
    // La tutela se quitó mientras la ficha estaba abierta: no se deja una cuenta que ya no es suya.
    LaunchedEffect(state.accessLost) {
        if (state.accessLost) onUnlinked(state.child.userId)
    }
    val child = state.child
    val canBlock = child.isPrincipal && child.hasCredential

    // Sin foto ni dibujo: aquí se maneja dinero y datos de menores. Papel liso, tarjetas de papel 2.
    Box(modifier = Modifier.fillMaxSize().background(colors.paper).testTag("child-account")) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("child-account-list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
        ) {
            item(key = "window") { Spacer(Modifier.statusBarsPadding().height(58.dp)) }
            item(key = "header") { ChildHeader(child, Modifier.arrive(0)) }
            item(key = "balance") { BalanceCard(state, Modifier.padding(top = 18.dp).arrive(1)) }
            item(key = "actions") {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 18.dp).arrive(2)) {
                    RoundAction("Recargar", Icons.Rounded.Add, "child-reload", primary = true) { sheet = ChildSheet.RELOAD }
                    RoundAction("Apartar", Icons.Outlined.CalendarMonth, "child-week") { sheet = ChildSheet.WEEK }
                    RoundAction("Controles", Icons.Outlined.Tune, "child-controls") { sheet = ChildSheet.CONTROLS }
                    if (canBlock) {
                        RoundAction("Bloquear", Icons.Outlined.Lock, "child-block", busy = state.isBlockingCredential) {
                            confirmsBlock = true
                        }
                    }
                }
            }
            state.loadError?.let { message ->
                item(key = "error") {
                    RetryNote("Sin conexión con su cuenta", message) { scope.launch { state.load() } }
                }
            }
            if (state.ordersFailed) {
                item(key = "orders-error") {
                    RetryNote(
                        "No pudimos traer lo apartado",
                        "Antes de apartar otra vez, vuelve a intentarlo para no repetir un día.",
                    ) { scope.launch { state.refreshScheduled() } }
                }
            }
            item(key = "scheduled-title") {
                CardCaption(
                    "Su comida apartada",
                    action = if (state.scheduled.isEmpty()) "Apartar" else "Editar",
                    onAction = { sheet = ChildSheet.WEEK },
                    modifier = Modifier.padding(top = 26.dp).arrive(3),
                )
            }
            item(key = "scheduled") {
                ScheduledCard(state, Modifier.arrive(3), onOpen = { openOrderId = it.id }) { orderToCancel = it }
            }
            item(key = "counter-title") {
                CardCaption("Lo que cuida la caja", modifier = Modifier.padding(top = 26.dp).arrive(4))
            }
            item(key = "counter") {
                CounterCard(state, onControls = { sheet = ChildSheet.CONTROLS }, modifier = Modifier.arrive(4))
            }
        }
        GlassCircleButton(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Regresar",
            onClick = onBack,
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(start = 16.dp, top = 6.dp),
        )
        SecurityPill(
            onClick = { showSecurity = true },
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(end = 16.dp, top = 6.dp),
        )
        FullScreenPage(visible = showSecurity, reduced = reduced) {
            BackHandler { showSecurity = false }
            ChildSecurityPage(
                state = state,
                onChildChanged = onChildChanged,
                onSheet = { sheet = it },
                onBlockCredential = { confirmsBlock = true },
                onBack = { showSecurity = false },
            )
        }
        FullScreenPage(visible = openOrderId != null, reduced = reduced) {
            openOrderId?.let { id ->
                BackHandler { openOrderId = null }
                GuardianOrderPage(
                    orderId = id,
                    childName = child.firstName,
                    orders = state.services.orders,
                    onBack = { openOrderId = null },
                )
            }
        }
        val isError = state.noticeIsError
        ToastPill(
            message = state.notice,
            containerColor = colors.ink,
            contentColor = colors.onInk,
            badgeColor = if (isError) colors.coral else colors.accent,
            badgeContentColor = colors.accentInk,
            icon = if (isError) Icons.Rounded.ErrorOutline else Icons.Rounded.Check,
        )
    }

    orderToCancel?.let { order ->
        ParentConfirmDialog(
            title = "¿Cancelar lo apartado?",
            message = "El saldo regresa a ${child.firstName} al momento.",
            confirmLabel = "Cancelar y regresar ${compactMoney(order.total)}",
            destructive = true,
            onConfirm = { scope.launch { state.cancel(order) } },
            onDismiss = { orderToCancel = null },
        )
    }
    if (confirmsBlock) {
        ParentConfirmDialog(
            title = "¿Bloquear la credencial de ${child.firstName}?",
            message =
                "Su QR y su tarjeta dejan de servir al momento. Hasta " +
                    "que Caja le dé otra, lo encuentran por su nombre.",
            confirmLabel = "Bloquear",
            destructive = true,
            onConfirm = { scope.launch { state.blockCredential()?.let(onChildChanged) } },
            onDismiss = { confirmsBlock = false },
        )
    }

    val close = { sheet = null }
    var reloadOverWeek by remember { mutableStateOf(false) }

    @Composable
    fun ReloadSheet(onClosed: () -> Unit) =
        ChildReloadSheet(
            child = child,
            balance = state.balance,
            account = state.account,
            range = state.wallet?.reloadRange,
            unsettledReloadId = state.unsettledReloadId,
            onUnsettledChange = { state.unsettledReloadId = it },
            onCredited = { amount ->
                state.say("Listo: ${compactMoney(amount)} para ${child.firstName}.")
                scope.launch { state.refreshWallet() }
            },
            onDismiss = {
                pendingReloadAmount = null
                onClosed()
            },
            initialAmount = pendingReloadAmount,
        )
    when (sheet) {
        ChildSheet.RELOAD -> ReloadSheet(close)
        ChildSheet.CONTROLS ->
            ChildControlsSheet(
                child = child,
                services = state.services,
                onSaved = { updated ->
                    state.updateChild(updated)
                    onChildChanged(updated)
                    state.say("Controles guardados.")
                },
                onUnlinked = {
                    sheet = null
                    onUnlinked(child.userId)
                },
                onDismiss = close,
            )
        ChildSheet.WEEK ->
            WeekPlannerSheet(
                child = child,
                account = state.account,
                catalog = state.services.catalog,
                balance = state.balance,
                onReserved = { days -> scope.launch { state.didReserve(days) } },
                onDismiss = close,
                onReload = { reloadOverWeek = true },
            ).also { if (reloadOverWeek) ReloadSheet { reloadOverWeek = false } }
        ChildSheet.OWN_ACCOUNT -> AccountCodeSheet(child, state.services.guardianship, onDismiss = close)
        ChildSheet.GUARDIAN_CODE -> GuardianCodeSheet(child, state.services.guardianship, onDismiss = close)
        ChildSheet.REFUND, ChildSheet.DATA_REMOVAL ->
            StudentRequestSheet(
                kind =
                    if (sheet == ChildSheet.REFUND) {
                        com.vaiinilla.app.domain.guardianship.StudentRequestKind.REFUND
                    } else {
                        com.vaiinilla.app.domain.guardianship.StudentRequestKind.DATA_REMOVAL
                    },
                child = child,
                balance = state.balance,
                guardianship = state.services.guardianship,
                onSent = { request ->
                    state.didSend(request)
                    state.say("Le avisamos a la escuela.")
                },
                onDismiss = close,
            )
        ChildSheet.PIN ->
            ChildPinSheet(child, state.services.guardianship, onSaved = {
                onChildChanged(state.didSetPin(it))
            }, onDismiss = close)
        null -> Unit
    }
}

@Composable
private fun ChildHeader(
    child: Dependent,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Row(modifier = modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        InitialsAvatar(
            child.initials.take(1),
            ChildAvatarFill,
            ChildAvatarInk,
            Color.Transparent,
            Modifier.size(58.dp),
            shape = RoundedCornerShape(20.dp),
            fontSize = 24,
        )
        Column(modifier = Modifier.padding(start = 14.dp)) {
            Text(child.name, color = colors.ink, fontWeight = FontWeight.Black, fontSize = 26.sp, letterSpacing = (-1).sp)
            child.schoolLine?.let {
                Text(it, color = colors.muted, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}

/** La pastilla de arriba a la derecha que abre la página de seguridad y tutores (E11). */
@Composable
private fun SecurityPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    Box(
        modifier =
            modifier
                .testTag("child-security")
                .physicalPress(scale = PhysicalPressScale.Small) {
                    haptics.click()
                    onClick()
                }.height(40.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(colors.paper2)
                .semantics { role = Role.Button }
                .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("Seguridad y tutores", color = colors.ink2, fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp)
    }
}

/** «Su saldo» con la barra de lo que le queda hoy; sin `disponible_hoy` del servidor, solo su límite. */
@Composable
private fun BalanceCard(
    state: ChildAccountState,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val reduced = reducedMotion()
    val child = state.child
    val controls = child.controls
    Column(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(colors.paper2).padding(16.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.weight(1f)) {
                Caption("Su saldo")
                AnimatedContent(
                    targetState = compactMoney(state.balance),
                    transitionSpec = {
                        if (reduced) {
                            fadeIn(snap()) togetherWith fadeOut(snap())
                        } else {
                            fadeIn(spring(stiffness = 300f)) togetherWith fadeOut(spring(stiffness = 600f))
                        }
                    },
                    label = "child-balance",
                ) { text ->
                    Text(
                        text,
                        color = colors.ink,
                        fontWeight = FontWeight.Black,
                        fontSize = 38.sp,
                        lineHeight = 44.sp,
                        letterSpacing = (-1.6).sp,
                        modifier = Modifier.semantics { contentDescription = "Saldo de ${child.firstName}: $text" },
                    )
                }
            }
            val alert = controls?.lowBalanceAlert
            if (child.isLowBalance && alert != null) {
                Tag("Bajó de ${compactMoney(alert)}", colors.coral.copy(alpha = 0.16f), colors.errorText)
            }
        }
        val limit = controls?.dailyLimit
        val available = controls?.availableToday
        when {
            limit != null && available != null -> {
                val spent = controls.spentToday ?: (limit - available).max(BigDecimal.ZERO)
                Row(modifier = Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        buildAnnotatedString {
                            append("Hoy le quedan ")
                            withStyle(SpanStyle(color = colors.ink, fontWeight = FontWeight.Black)) {
                                append(compactMoney(available))
                            }
                            append(" de ${compactMoney(limit)}")
                        },
                        color = colors.ink2,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Text("gastó ${compactMoney(spent)}", color = colors.muted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                TodayBar(fraction = spentFraction(spent, limit), exhausted = available.signum() == 0)
            }
            limit != null ->
                Text(
                    "Hasta ${compactMoney(limit)} al día",
                    color = colors.ink2,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 12.dp),
                )
            controls != null ->
                Text(
                    "Sin límite diario",
                    color = colors.muted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 12.dp),
                )
        }
    }
}

internal fun spentFraction(
    spent: BigDecimal,
    limit: BigDecimal,
): Float =
    if (limit.signum() <= 0) {
        1f
    } else {
        spent.divide(limit, 4, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
    }

@Composable
private fun TodayBar(
    fraction: Float,
    exhausted: Boolean,
) {
    val colors = LocalVaiinillaColors.current
    val reduced = reducedMotion()
    val progress = remember { Animatable(if (reduced) fraction else 0f) }
    LaunchedEffect(fraction, reduced) {
        if (reduced) progress.snapTo(fraction) else progress.animateTo(fraction, spring(dampingRatio = 0.85f, stiffness = 120f))
    }
    val fill =
        when {
            exhausted -> colors.coral
            fraction >= 0.5f -> colors.yolk
            else -> colors.accent
        }
    Box(
        modifier =
            Modifier
                .padding(top = 7.dp)
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(colors.line),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(progress.value.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(fill),
        )
    }
}

/** Uno de los cuatro botones redondos de la ficha: el de recargar va en lima, los demás en papel. */
@Composable
private fun androidx.compose.foundation.layout.RowScope.RoundAction(
    label: String,
    icon: ImageVector,
    tag: String,
    primary: Boolean = false,
    busy: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    Column(
        modifier =
            Modifier
                .weight(1f)
                .testTag(tag)
                .physicalPress(enabled = !busy, scale = PhysicalPressScale.Small) {
                    haptics.click()
                    onClick()
                }.semantics(mergeDescendants = true) { role = Role.Button },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (primary) colors.accent else colors.paper2),
            contentAlignment = Alignment.Center,
        ) {
            if (busy) {
                CircularProgressIndicator(color = colors.ink, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (primary) colors.accentInk else colors.ink2,
                    modifier = Modifier.size(if (primary) 28.dp else 22.dp),
                )
            }
        }
        Text(label, color = colors.ink2, fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp)
    }
}

/** La tira de lunes a viernes y lo apartado, cada uno con su «Cancelar» mientras se pueda. */
@Composable
private fun ScheduledCard(
    state: ChildAccountState,
    modifier: Modifier = Modifier,
    onOpen: (ScheduledOrder) -> Unit = {},
    onCancel: (ScheduledOrder) -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val today = LocalDate.now()
    val reservedDates = state.scheduled.mapNotNull { it.deliveryDate }.toSet()
    Column(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(colors.paper2).padding(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            schoolWeek(today).forEach { day ->
                val iso = day.toString()
                val reserved = iso in reservedDates
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (reserved) colors.accent.copy(alpha = 0.14f) else colors.line)
                            .semantics(mergeDescendants = true) {
                                contentDescription =
                                    SchoolCalendar.longLabel(iso, today.toString()) + if (reserved) ", apartado" else ""
                            },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        SchoolCalendar.weekdayShort(iso).uppercase(),
                        color = if (reserved) colors.accent else colors.muted,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                    )
                    Text(SchoolCalendar.dayNumber(iso), color = colors.ink, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }
        }
        if (state.scheduled.isEmpty()) {
            Text(
                if (state.isLoading) "Buscando lo apartado…" else "Nada apartado todavía.",
                color = colors.muted,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 4.dp, top = 10.dp),
            )
        }
        state.scheduled.forEach { order ->
            ScheduledRow(
                order = order,
                today = today,
                canceling = state.cancelingOrderId == order.id,
                cancelable =
                    ScheduledOrderRules.canCancel(
                        order.statusWire,
                        order.deliveryDate,
                        state.schedule?.cancelCutoff,
                        now = state.schedule?.let { LocalDateTime.now() },
                    ),
                onOpen = { onOpen(order) },
            ) { onCancel(order) }
        }
    }
}

/** Lunes a viernes de esta semana; en fin de semana, los de la que viene. */
internal fun schoolWeek(today: LocalDate): List<LocalDate> {
    val monday =
        if (today.dayOfWeek >= DayOfWeek.SATURDAY) {
            today.plusDays((8 - today.dayOfWeek.value).toLong())
        } else {
            today.minusDays((today.dayOfWeek.value - 1).toLong())
        }
    return (0L..4L).map(monday::plusDays)
}

/**
 * «Se recoge el jueves 24 a las 12:00» bajo lo apartado de un hijo (P08). Antes solo había hora si el pedido traía
 * franja, y sin franja el tutor no veía cuándo se recoge.
 */
internal fun guardianPickupLine(
    dayLabel: String,
    slot: String?,
): String {
    val day =
        when (dayLabel) {
            "Hoy" -> "hoy"
            "Mañana" -> "mañana"
            else -> "el ${dayLabel.lowercase()}"
        }
    val start = slot?.substringBefore("-")?.trim()
    val hour = if (start.isNullOrEmpty()) "" else " a las $start"
    return "Se recoge $day$hour"
}

@Composable
private fun ScheduledRow(
    order: ScheduledOrder,
    today: LocalDate,
    canceling: Boolean,
    cancelable: Boolean,
    onOpen: () -> Unit = {},
    onCancel: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val date = order.deliveryDate.orEmpty()
    val dayLabel = SchoolCalendar.longLabel(date, today.toString())
    val title = if (date == today.toString()) "Hoy: ${order.summary}" else "$dayLabel: ${order.summary}"
    val pickup = guardianPickupLine(dayLabel, order.deliverySlot) + " · "
    Row(modifier = Modifier.padding(start = 4.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        // Tocar el renglón abre el pedido para verlo; «Cancelar» sigue siendo su propio botón.
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .physicalPress(scale = PhysicalPressScale.Small, onClick = onOpen)
                    .semantics { contentDescription = "Ver lo que pidió para $dayLabel" }
                    .padding(end = 10.dp),
        ) {
            Text(
                title,
                color = colors.ink,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(pickup + compactMoney(order.total), color = colors.muted, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        when {
            canceling -> CircularProgressIndicator(color = colors.ink, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
            cancelable ->
                Text(
                    "Cancelar",
                    color = colors.ink2,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    modifier =
                        Modifier
                            .physicalPress(scale = PhysicalPressScale.Small, onClick = onCancel)
                            .minTouchTarget()
                            .semantics { contentDescription = "Cancelar lo apartado para $dayLabel" },
                )
            else -> scheduledStatusLabel(order.statusWire)?.let { Tag(it, colors.line, colors.ink2) }
        }
    }
}

internal fun scheduledStatusLabel(statusWire: String): String? =
    when (statusWire) {
        "cobrado" -> "Pagado"
        "preparando" -> "Preparando"
        "listo" -> "Listo"
        else -> null
    }

/** Alergias, lo que no puede comprar y sus movimientos: lo que la caja revisa antes de venderle. */
@Composable
private fun CounterCard(
    state: ChildAccountState,
    onControls: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val reduced = reducedMotion()
    val controls = state.child.controls
    val blockedCategories = controls?.blockedCategoryIds.orEmpty()
    // La ficha no trae el catálogo: los nombres de lo bloqueado se piden solo si hay algo bloqueado.
    val categoryNames by produceState<Map<Int, String>>(emptyMap(), blockedCategories) {
        if (blockedCategories.isNotEmpty()) {
            withContext(Dispatchers.IO) { state.services.catalog.getCatalog() }
                .onSuccess { catalog -> value = catalog.categories.associate { it.id to it.name } }
        }
    }
    var showMovements by rememberSaveable { mutableStateOf(false) }
    val movements = state.wallet?.movements
    Column(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(colors.paper2)) {
        CardRow(
            title = "Alergias",
            detail =
                controls
                    ?.allergens
                    ?.map { it.label }
                    ?.sorted()
                    ?.takeIf { it.isNotEmpty() }
                    ?.joinToString(", ")
                    ?.let { "$it · no se le vende" }
                    ?: "Ninguna anotada",
            tag = "child-allergies",
            onClick = onControls,
        )
        CardDivider()
        CardRow(
            title = "No puede comprar",
            detail = blockedLine(blockedCategories, categoryNames, controls?.blockedProductIds?.size ?: 0),
            tag = "child-blocked",
            onClick = onControls,
        )
        CardDivider()
        CardRow(
            title = "Últimos movimientos",
            detail =
                when {
                    movements == null -> if (state.isLoading) "Buscando…" else "No pudimos traerlos"
                    movements.isEmpty() -> "Aquí verás sus recargas y lo que compre con saldo."
                    else -> movements.first().let { "${movementLabel(it.type, it.description)} ${signedMoney(it)}" }
                },
            tag = "child-movements",
            enabled = !movements.isNullOrEmpty(),
            expanded = showMovements,
        ) { showMovements = !showMovements }
        AnimatedVisibility(
            visible = showMovements && !movements.isNullOrEmpty(),
            enter = if (reduced) fadeIn(snap()) else fadeIn() + expandVertically(spring(dampingRatio = 0.86f, stiffness = 380f)),
            exit = if (reduced) fadeOut(snap()) else fadeOut() + shrinkVertically(spring(stiffness = 500f)),
        ) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                movements.orEmpty().take(15).forEach { MovementRow(it) }
            }
        }
    }
}

private fun blockedLine(
    categoryIds: Set<Int>,
    names: Map<Int, String>,
    productCount: Int,
): String {
    val parts =
        buildList {
            if (categoryIds.isNotEmpty()) {
                val known = categoryIds.mapNotNull(names::get).sorted()
                add(
                    if (known.size == categoryIds.size) {
                        known.joinToString(", ")
                    } else if (categoryIds.size == 1) {
                        "1 categoría"
                    } else {
                        "${categoryIds.size} categorías"
                    },
                )
            }
            if (productCount > 0) add(if (productCount == 1) "1 producto" else "$productCount productos")
        }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" y ") ?: "Nada bloqueado"
}

private fun signedMoney(movement: ChildMovement): String =
    if (movement.isDebit) "−${compactMoney(movement.amount.negate())}" else "+${compactMoney(movement.amount)}"

@Composable
private fun MovementRow(movement: ChildMovement) {
    val colors = LocalVaiinillaColors.current
    Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(
                movementLabel(movement.type, movement.description),
                color = colors.ink,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            Text("Saldo después: ${compactMoney(movement.balanceAfter)}", color = colors.muted, fontSize = 12.sp)
        }
        Text(signedMoney(movement), color = colors.ink, fontWeight = FontWeight.Black, fontSize = 14.sp)
    }
}

/** Un renglón de tarjeta como los de Doop: título, detalle y flecha. */
@Composable
internal fun CardRow(
    title: String,
    detail: String?,
    tag: String,
    modifier: Modifier = Modifier,
    titleColor: Color = LocalVaiinillaColors.current.ink,
    enabled: Boolean = true,
    expanded: Boolean? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val reduced = reducedMotion()
    val turn = remember { Animatable(0f) }
    LaunchedEffect(expanded, reduced) {
        val target = if (expanded == true) 90f else 0f
        if (reduced) turn.snapTo(target) else turn.animateTo(target, spring(dampingRatio = 0.7f, stiffness = 400f))
    }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag(tag)
                .physicalPress(enabled = enabled, scale = PhysicalPressScale.Small) {
                    haptics.click()
                    onClick()
                }.semantics(mergeDescendants = true) { role = Role.Button }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = titleColor, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            if (detail != null) {
                Text(
                    detail,
                    color = colors.muted,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else if (enabled) {
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(16.dp).graphicsLayer { rotationZ = turn.value },
            )
        }
    }
}

@Composable
internal fun CardDivider() {
    HorizontalDivider(color = LocalVaiinillaColors.current.ink.copy(alpha = 0.1f), thickness = 1.dp)
}

/** La etiqueta en versalitas de Doop (11 sp, espaciada). */
@Composable
internal fun Caption(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text.uppercase(),
        color = LocalVaiinillaColors.current.muted,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 11.sp,
        letterSpacing = 2.2.sp,
        modifier = modifier,
    )
}

@Composable
internal fun CardCaption(
    text: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Caption(text, Modifier.weight(1f))
        if (action != null) {
            Text(
                action,
                color = if (colors.isDark) colors.accent else colors.accentInk,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                modifier = Modifier.physicalPress(scale = PhysicalPressScale.Small, onClick = onAction).minTouchTarget(),
            )
        }
    }
}

@Composable
internal fun Tag(
    text: String,
    background: Color,
    ink: Color,
) {
    Box(
        modifier = Modifier.height(26.dp).clip(RoundedCornerShape(percent = 50)).background(background).padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = ink, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
    }
}

@Composable
private fun RetryNote(
    title: String,
    message: String,
    onRetry: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Column(modifier = Modifier.padding(start = 4.dp, top = 20.dp)) {
        Text(title, color = colors.ink, fontWeight = FontWeight.Black, fontSize = 16.sp)
        Text(message, color = colors.muted, fontSize = 14.sp)
        Text(
            "Reintentar",
            color = colors.ink,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 6.dp).physicalPress(scale = PhysicalPressScale.Small, onClick = onRetry).minTouchTarget(),
        )
    }
}