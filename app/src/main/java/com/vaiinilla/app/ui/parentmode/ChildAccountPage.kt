package com.vaiinilla.app.ui.parentmode

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
import com.vaiinilla.app.domain.guardianship.StudentRequestKind
import com.vaiinilla.app.domain.guardianship.compactMoney
import com.vaiinilla.app.ui.components.CremeDivider
import com.vaiinilla.app.ui.components.CremePill
import com.vaiinilla.app.ui.components.CremePillRow
import com.vaiinilla.app.ui.components.CremeStat
import com.vaiinilla.app.ui.components.CremeStatRow
import com.vaiinilla.app.ui.components.GlassCircleButton
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ToastPill
import com.vaiinilla.app.ui.components.cremeBleed
import com.vaiinilla.app.ui.components.minTouchTarget
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.screens.movementLabel
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

/** Cada cuánto pregunta la ficha por una solicitud que espera a la escuela. */
private const val REQUESTS_POLL_MS = 20_000L

private enum class ChildSheet { RELOAD, CONTROLS, WEEK, OWN_ACCOUNT, GUARDIAN_CODE, REFUND, DATA_REMOVAL, PIN }

/**
 * La casita de un hijo: su frasco con su saldo arriba y, en el vidrio, recargar, apartar, controles,
 * lo apartado, quién lo cuida y sus movimientos. Todo lo de su cuenta sale con su contexto.
 */
@Composable
internal fun ChildAccountPage(
    child: Dependent,
    services: ParentModeServices,
    onChildChanged: (Dependent) -> Unit,
    onUnlinked: (String) -> Unit,
    onBack: () -> Unit,
) {
    val state = remember(child.userId) { ChildAccountState(child, services) }
    ChildAccountContent(state = state, onChildChanged = onChildChanged, onUnlinked = onUnlinked, onBack = onBack)
}

@Composable
internal fun ChildAccountContent(
    state: ChildAccountState,
    onChildChanged: (Dependent) -> Unit,
    onUnlinked: (String) -> Unit,
    onBack: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val scope = rememberCoroutineScope()
    var sheet by rememberSaveable { mutableStateOf<ChildSheet?>(null) }
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

    // Sin foto ni dibujo: aquí se maneja dinero y datos de menores. Papel liso con un halo suave arriba.
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.paper)
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(colors.paper2, Color.Transparent),
                            center = Offset(size.width / 2f, 0f),
                            radius = size.width,
                        ),
                        size = Size(size.width, 300.dp.toPx()),
                    )
                }.testTag("child-account"),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("child-account-list"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
        ) {
            item(key = "window") { Spacer(Modifier.statusBarsPadding().height(72.dp)) }
            item(key = "header") { ChildHeader(state) }
            item(key = "actions") {
                ChildActions(
                    onReload = { sheet = ChildSheet.RELOAD },
                    onWeek = { sheet = ChildSheet.WEEK },
                    onControls = { sheet = ChildSheet.CONTROLS },
                    stats =
                        listOf(
                            // El servidor todavía no manda lo que le queda hoy: la columna queda en «—».
                            CremeStat("Hoy le quedan", null, highlighted = true),
                            CremeStat(
                                "Apartado",
                                state.scheduled.size.let {
                                    if (it ==
                                        1
                                    ) {
                                        "1 día"
                                    } else {
                                        "$it días"
                                    }
                                },
                            ),
                            CremeStat("Tutores", child.guardians.size.toString()),
                            CremeStat("PIN en Caja", if (child.hasPin) "Sí" else "No"),
                        ),
                )
            }
            state.loadError?.let { message ->
                item(key = "error") {
                    Column(modifier = Modifier.padding(top = 20.dp)) {
                        Text(
                            "Sin conexión con su cuenta",
                            color = colors.ink,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                        )
                        Text(message, color = colors.muted, fontSize = 14.sp)
                        Text(
                            "Reintentar",
                            color = colors.ink,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            modifier =
                                Modifier
                                    .padding(
                                        top = 6.dp,
                                    ).physicalPress(scale = PhysicalPressScale.Small) {
                                        scope.launch { state.load() }
                                    }.minTouchTarget(),
                        )
                    }
                }
            }
            if (state.ordersFailed) {
                item(key = "orders-error") {
                    Column(modifier = Modifier.padding(top = 20.dp)) {
                        Text(
                            "No pudimos traer lo apartado",
                            color = colors.ink,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                        )
                        Text(
                            "Antes de apartar otra vez, vuelve a intentarlo para no repetir un día.",
                            color = colors.muted,
                            fontSize = 14.sp,
                        )
                        Text(
                            "Reintentar",
                            color = colors.ink,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            modifier =
                                Modifier
                                    .padding(top = 6.dp)
                                    .physicalPress(scale = PhysicalPressScale.Small) {
                                        scope.launch { state.refreshScheduled() }
                                    }.minTouchTarget(),
                        )
                    }
                }
            }
            if (state.scheduled.isNotEmpty()) {
                item(key = "scheduled-title") { SectionHeading("Apartado") }
                items(state.scheduled, key = { "s-${it.id}" }) { order ->
                    ScheduledRow(
                        order,
                        canceling = state.cancelingOrderId == order.id,
                        cancelable =
                            ScheduledOrderRules.canCancel(
                                order.statusWire,
                                order.deliveryDate,
                                state.schedule?.cancelCutoff,
                                now = state.schedule?.let { LocalDateTime.now() },
                            ),
                    ) { orderToCancel = order }
                }
            }
            if (child.guardians.isNotEmpty()) {
                item(key = "guardians") {
                    ChildGuardiansSection(
                        child = child,
                        busyGuardianId = state.changingGuardianId,
                        onInvite = { sheet = ChildSheet.GUARDIAN_CODE },
                        onRemove = { guardian ->
                            scope.launch { state.removeGuardian(guardian)?.let(onChildChanged) }
                        },
                        onMakePrincipal = { guardian ->
                            scope.launch { state.makePrincipal(guardian)?.let(onChildChanged) }
                        },
                    )
                }
            }
            val movements = state.wallet?.movements
            if (movements != null) {
                item(key = "movements-title") { SectionHeading("Movimientos") }
                if (movements.isEmpty()) {
                    item(key = "movements-empty") {
                        Text(
                            "Aquí verás sus recargas y lo que compre con saldo.",
                            color = colors.muted,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
                items(movements.take(15), key = { "m-${it.id}" }) { MovementRow(it) }
            } else if (state.isLoading) {
                item(key = "movements-loading") { Spacer(Modifier.height(16.dp)) }
            }
            if (!child.hasOwnAccount) {
                item(key = "own-account") {
                    QuietRow(
                        icon = Icons.Outlined.PhoneIphone,
                        title = "Pasar a su propia cuenta",
                        detail = "Cuando tenga teléfono, entra con su Google y sigue con todo lo suyo.",
                        ink = colors.ink,
                        muted = colors.muted,
                        modifier = Modifier.padding(top = 30.dp).testTag("child-own-account"),
                    ) { sheet = ChildSheet.OWN_ACCOUNT }
                }
            }
            if (child.isPrincipal) {
                item(key = "counter") {
                    ChildCounterSecuritySection(
                        child = child,
                        isBlocking = state.isBlockingCredential,
                        onPin = { sheet = ChildSheet.PIN },
                        onBlockCredential = { scope.launch { state.blockCredential()?.let(onChildChanged) } },
                        modifier = Modifier.padding(top = if (child.hasOwnAccount) 30.dp else 14.dp),
                    )
                }
                item(key = "requests") {
                    StudentRequestsSection(
                        balance = state.balance,
                        requests = state.requests,
                        modifier = Modifier.padding(top = 14.dp),
                    ) { kind ->
                        sheet =
                            if (kind == StudentRequestKind.REFUND) ChildSheet.REFUND else ChildSheet.DATA_REMOVAL
                    }
                }
            }
        }
        GlassCircleButton(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Regresar",
            onClick = onBack,
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(start = 16.dp, top = 6.dp),
        )
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

    val close = { sheet = null }
    when (sheet) {
        ChildSheet.RELOAD ->
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
                onDismiss = close,
            )
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
            )
        ChildSheet.OWN_ACCOUNT -> AccountCodeSheet(child, state.services.guardianship, onDismiss = close)
        ChildSheet.GUARDIAN_CODE -> GuardianCodeSheet(child, state.services.guardianship, onDismiss = close)
        ChildSheet.REFUND, ChildSheet.DATA_REMOVAL ->
            StudentRequestSheet(
                kind = if (sheet == ChildSheet.REFUND) StudentRequestKind.REFUND else StudentRequestKind.DATA_REMOVAL,
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
private fun ChildHeader(state: ChildAccountState) {
    val colors = LocalVaiinillaColors.current
    val reduced = reducedMotion()
    val child = state.child
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InitialsAvatar(child.initials, colors.paper2, colors.ink, colors.line, Modifier.size(52.dp))
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(child.name, color = colors.ink, fontWeight = FontWeight.Black, fontSize = 24.sp)
                child.schoolLine?.let {
                    Text(
                        it,
                        color = colors.muted,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
        AnimatedContent(
            targetState = compactMoney(state.balance),
            transitionSpec = {
                if (reduced) {
                    fadeIn(snap()) togetherWith fadeOut(snap())
                } else {
                    fadeIn(spring(stiffness = 300f)) togetherWith
                        fadeOut(spring(stiffness = 600f))
                }
            },
            label = "child-balance",
        ) { text ->
            Text(
                text,
                color = colors.ink,
                fontWeight = FontWeight.Black,
                fontSize = 52.sp,
                lineHeight = 58.sp,
                letterSpacing = (-1.6).sp,
                modifier =
                    Modifier
                        .padding(top = 18.dp)
                        .semantics { contentDescription = "Saldo de ${child.firstName}: $text" },
            )
        }
        controlsSummary(child)?.let {
            Text(it, color = colors.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/** "Hasta $60 al día · Alergias: Huevo, Soya" */
internal fun controlsSummary(child: Dependent): String? {
    val controls = child.controls ?: return null
    val parts =
        buildList {
            controls.dailyLimit?.let { add("Hasta ${compactMoney(it)} al día") }
            if (controls.allergens.isNotEmpty()) {
                add(
                    "Alergias: " +
                        controls.allergens
                            .map { it.label }
                            .sorted()
                            .joinToString(", "),
                )
            }
        }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

@Composable
private fun ChildActions(
    onReload: () -> Unit,
    onWeek: () -> Unit,
    onControls: () -> Unit,
    stats: List<CremeStat>,
) {
    Column(modifier = Modifier.padding(top = 20.dp)) {
        CremePillRow(
            listOf(
                CremePill("Recargar con tarjeta", Icons.Outlined.CreditCard, onReload, "child-reload"),
                CremePill("Apartar comida", Icons.Outlined.CalendarMonth, onWeek, "child-week"),
                CremePill("Controles", Icons.Outlined.Tune, onControls, "child-controls"),
            ),
            modifier = Modifier.cremeBleed(20.dp),
        )
        CremeDivider(Modifier.padding(top = 20.dp).cremeBleed(20.dp))
        CremeStatRow(stats, Modifier.padding(top = 18.dp).cremeBleed(20.dp))
    }
}

@Composable
internal fun SectionHeading(text: String) {
    Text(
        text,
        color = LocalVaiinillaColors.current.ink,
        fontWeight = FontWeight.Black,
        fontSize = 19.sp,
        modifier = Modifier.padding(top = 30.dp, bottom = 4.dp),
    )
}

@Composable
private fun ScheduledRow(
    order: ScheduledOrder,
    canceling: Boolean,
    cancelable: Boolean,
    onCancel: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val date = order.deliveryDate.orEmpty()
    val today = LocalDate.now().toString()
    val label = SchoolCalendar.longLabel(date, today) + (order.deliverySlot?.let { " · ${it.replace("-", "–")}" } ?: "")
    Column {
        Row(modifier = Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.Top) {
            Column(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(colors.paper2),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    SchoolCalendar.weekdayShort(date),
                    color = colors.muted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                )
                Text(
                    SchoolCalendar.dayNumber(date),
                    color = colors.ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                )
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 13.dp)) {
                Text(label, color = colors.ink, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text(order.summary, color = colors.muted, fontSize = 12.sp, maxLines = 2)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(compactMoney(order.total), color = colors.ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
                when {
                    canceling ->
                        CircularProgressIndicator(
                            color = colors.ink,
                            strokeWidth = 2.dp,
                            modifier = Modifier.padding(top = 6.dp).size(16.dp),
                        )
                    cancelable ->
                        Text(
                            "Cancelar",
                            color = colors.ink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier =
                                Modifier
                                    .padding(top = 6.dp)
                                    .physicalPress(scale = PhysicalPressScale.Small, onClick = onCancel)
                                    .minTouchTarget()
                                    .semantics { contentDescription = "Cancelar lo apartado para $label" },
                        )
                }
            }
        }
        HorizontalDivider(color = colors.line, thickness = 1.dp, modifier = Modifier.padding(start = 57.dp))
    }
}

@Composable
private fun MovementRow(movement: ChildMovement) {
    val colors = LocalVaiinillaColors.current
    Column {
        Row(modifier = Modifier.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(colors.paper2),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (movement.isDebit) Icons.Outlined.ShoppingCart else Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    tint = colors.ink,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 13.dp)) {
                Text(
                    movementLabel(movement.type, movement.description),
                    color = colors.ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                )
                Text("Saldo después: ${compactMoney(movement.balanceAfter)}", color = colors.muted, fontSize = 12.sp)
            }
            Text(
                if (movement.isDebit) {
                    "−${compactMoney(
                        movement.amount.negate(),
                    )}"
                } else {
                    "+${compactMoney(movement.amount)}"
                },
                color = colors.ink,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
            )
        }
        HorizontalDivider(color = colors.line, thickness = 1.dp, modifier = Modifier.padding(start = 57.dp))
    }
}
