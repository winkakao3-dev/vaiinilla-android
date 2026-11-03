package com.vaiinilla.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.core.notifications.TableCallPushes
import com.vaiinilla.app.domain.repository.CallReason
import com.vaiinilla.app.domain.repository.CallStatus
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.ui.order.MenuWaiterCallState
import com.vaiinilla.app.ui.order.MenuWaiterIcon
import com.vaiinilla.app.ui.order.MenuWaiterPhase
import com.vaiinilla.app.ui.order.canCancelMenuCall
import com.vaiinilla.app.ui.order.menuWaiterPillLabel
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.SeaGlassButton
import com.vaiinilla.app.ui.theme.SeaGlassTone
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Lo que lleva el carrito, para la barra que junta "Ver carrito" y "Mesero". */
data class MenuCartBar(
    val count: Int,
    /** Total ya con formato de dinero ("$325"). */
    val total: String,
    val onOpen: () -> Unit,
)

/**
 * "Llamar al mesero" siempre a la mano en el menú de una mesa. Sin carrito es una píldora sobre la barra de
 * abajo; con algo en el carrito ([cart]) es el botón "Mesero" dentro de la barra "Ver carrito", para no apilar
 * dos botones. Un toque abre los motivos encima; ya enviada, la píldora es el estado en vivo y tocarla deja
 * cancelar.
 */
@Composable
fun MenuWaiterLayer(
    spaceId: Int,
    visible: Boolean,
    cart: MenuCartBar?,
    onCurrent: suspend (Int) -> Result<TableCall?>,
    onCall: suspend (Int, CallReason, String?) -> Result<TableCall>,
    onCancel: suspend (TableCall) -> Result<TableCall>,
    onJoinTable: (suspend () -> Result<Unit>)?,
    modifier: Modifier = Modifier,
) {
    val state =
        remember(spaceId) {
            MenuWaiterCallState(spaceId, onCurrent, onCall, onCancel, onJoinTable)
        }
    MenuWaiterLayer(state = state, visible = visible, cart = cart, modifier = modifier)
}

@Composable
internal fun MenuWaiterLayer(
    state: MenuWaiterCallState,
    visible: Boolean,
    cart: MenuCartBar?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val reduced = reducedMotion()

    // Recupera una llamada abierta y la mantiene fresca mientras el menú está a la vista.
    LaunchedEffect(state) {
        while (true) {
            state.refresh()
            TableCallPushes.awaitNextPoll(MENU_CALL_POLL_MS)
        }
    }
    LaunchedEffect(state.cooldown) {
        if (state.cooldown > 0) {
            delay(1_000)
            state.tick()
        }
    }
    LaunchedEffect(visible) { if (!visible) state.close() }

    val expanded = visible && state.expanded
    BackHandler(enabled = expanded) { state.close() }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(160)),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = if (colors.isDark) 0.32f else 0.16f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClickLabel = "Cerrar",
                        ) { state.close() },
            )
        }
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(
                        start = VaiinillaBottomNavSideMargin,
                        end = if (expanded || cart != null) VaiinillaBottomNavSideMargin else 0.dp,
                        bottom = VaiinillaBottomNavTop + 10.dp,
                    ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AnimatedVisibility(
                visible = expanded,
                enter =
                    if (reduced) {
                        fadeIn()
                    } else {
                        fadeIn(tween(200)) +
                            scaleIn(
                                initialScale = 0.6f,
                                transformOrigin = TransformOrigin(0f, 1f),
                                animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
                            ) +
                            slideInVertically { it / 6 }
                    },
                exit =
                    if (reduced) {
                        fadeOut()
                    } else {
                        fadeOut(tween(140)) +
                            scaleOut(targetScale = 0.8f, transformOrigin = TransformOrigin(0f, 1f)) +
                            slideOutVertically { it / 8 }
                    },
            ) {
                WaiterCallCard(state = state)
            }
            AnimatedVisibility(
                visible = visible && cart != null,
                enter = if (reduced) fadeIn() else fadeIn(tween(200)) + slideInVertically(spring(dampingRatio = 0.62f, stiffness = 420f)) { it },
                exit = if (reduced) fadeOut() else fadeOut(tween(160)) + slideOutVertically { it / 2 },
            ) {
                // Se queda con el último carrito mientras sale, para que no se vacíe a media animación.
                val shown = remember { mutableStateOf(cart) }
                if (cart != null) shown.value = cart
                shown.value?.let { CartWaiterBar(state = state, cart = it) }
            }
            AnimatedVisibility(
                visible = visible && cart == null && state.phase != MenuWaiterPhase.UNAVAILABLE,
                enter =
                    if (reduced) {
                        fadeIn()
                    } else {
                        fadeIn(tween(200)) +
                            scaleIn(
                                initialScale = 0.8f,
                                transformOrigin = TransformOrigin(0f, 1f),
                                animationSpec = spring(dampingRatio = 0.62f, stiffness = 420f),
                            )
                    },
                exit =
                    if (reduced) {
                        fadeOut()
                    } else {
                        fadeOut(tween(160)) + scaleOut(targetScale = 0.8f, transformOrigin = TransformOrigin(0f, 1f))
                    },
            ) {
                WaiterPill(state = state)
            }
        }
    }
}

@Composable
private fun WaiterPill(state: MenuWaiterCallState) {
    val haptics = rememberVaiinillaHaptics()
    val label = menuWaiterPillLabel(state.call, state.cooldown)
    val waiting = state.call == null && state.cooldown > 0
    SeaGlassButton(
        tone = if (state.call != null) SeaGlassTone.lima else SeaGlassTone.vainilla,
        onClick = {
            haptics.click()
            state.tap()
        },
        enabled = !waiting,
        modifier =
            Modifier
                .heightIn(min = 52.dp)
                .testTag(MENU_WAITER_PILL_TAG)
                // Al limpiar la semántica se va también el toque del botón: se vuelve a declarar para TalkBack.
                .clearAndSetSemantics {
                    contentDescription = label.title
                    role = Role.Button
                    if (waiting) {
                        disabled()
                    } else {
                        onClick {
                            haptics.click()
                            state.tap()
                            true
                        }
                    }
                },
    ) {
        Spacer(Modifier.width(18.dp))
        WaiterIcon(icon = label.icon)
        AnimatedContent(
            targetState = label.title,
            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
            label = "menu-waiter-title",
        ) { title ->
            Text(
                title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Spacer(Modifier.width(18.dp))
    }
}

/**
 * Una sola barra de tinta: "Ver carrito · N" y el total abren el carrito; adentro, a la derecha, "Mesero" llama.
 * Si el negocio no tiene llamadas, queda solo el carrito. Salta un poco con cada producto que entra.
 */
@Composable
private fun CartWaiterBar(
    state: MenuWaiterCallState,
    cart: MenuCartBar,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val reduced = reducedMotion()
    val bump = remember { Animatable(1f) }
    var lastCount by remember { mutableIntStateOf(cart.count) }
    LaunchedEffect(cart.count) {
        val grew = cart.count > lastCount
        lastCount = cart.count
        if (grew && !reduced) {
            bump.animateTo(1.04f, spring(dampingRatio = 1f, stiffness = 900f))
            bump.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 420f))
        }
    }
    val barPress = remember { MutableInteractionSource() }
    val barPressed by barPress.collectIsPressedAsState()
    val barScale by animateFloatAsState(
        if (barPressed) 0.97f else 1f,
        if (reduced) snap() else spring(dampingRatio = 0.44f, stiffness = 420f),
        label = "cart-bar-press",
    )
    val shape = RoundedCornerShape(percent = 50)
    val products = if (cart.count == 1) "1 producto" else "${cart.count} productos"
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .graphicsLayer {
                    scaleX = barScale * bump.value
                    scaleY = barScale * bump.value
                }.shadow(18.dp, shape, clip = false)
                .clip(shape)
                .background(colors.ink)
                .clickable(interactionSource = barPress, indication = null, role = Role.Button) {
                    haptics.click()
                    cart.onOpen()
                }.semantics(mergeDescendants = false) {
                    contentDescription = "Ver carrito: $products, ${cart.total}"
                }.testTag(MENU_CART_BAR_TAG)
                .padding(start = 20.dp, end = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Ver carrito · ", color = colors.onInk, fontSize = 15.5.sp, fontWeight = FontWeight.Black, maxLines = 1)
        NumberTicker(value = cart.count, label = "cart-bar-count") { value ->
            Text("$value", color = colors.onInk, fontSize = 15.5.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.weight(1f))
        RollingText(
            text = cart.total,
            style = TextStyle(fontSize = 15.5.sp, fontWeight = FontWeight.Black),
            color = colors.onInk,
            modifier = Modifier.padding(end = if (state.phase == MenuWaiterPhase.UNAVAILABLE) 13.dp else 8.dp),
        )
        if (state.phase != MenuWaiterPhase.UNAVAILABLE) WaiterSegment(state = state)
    }
}

@Composable
private fun WaiterSegment(state: MenuWaiterCallState) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val reduced = reducedMotion()
    val label = menuWaiterPillLabel(state.call, state.cooldown)
    val waiting = state.call == null && state.cooldown > 0
    val press = remember { MutableInteractionSource() }
    val pressed by press.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && !waiting) 0.93f else 1f,
        if (reduced) snap() else spring(dampingRatio = 0.44f, stiffness = 420f),
        label = "cart-bar-waiter-press",
    )
    val calling = state.call != null
    Row(
        modifier =
            Modifier
                .height(42.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = if (waiting) 0.6f else 1f
                }.clip(RoundedCornerShape(percent = 50))
                .background(if (calling) colors.accent else colors.onInk)
                .clickable(interactionSource = press, indication = null, enabled = !waiting, role = Role.Button) {
                    haptics.click()
                    state.tap()
                }.testTag(MENU_WAITER_PILL_TAG)
                .clearAndSetSemantics {
                    contentDescription = label.title
                    role = Role.Button
                    if (waiting) {
                        disabled()
                    } else {
                        onClick {
                            haptics.click()
                            state.tap()
                            true
                        }
                    }
                }.padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val ink = if (calling) colors.accentInk else colors.ink
        CompositionLocalProvider(LocalContentColor provides ink) { WaiterIcon(icon = label.icon) }
        AnimatedContent(
            targetState = label.shortTitle,
            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
            label = "cart-bar-waiter-title",
        ) { title ->
            Text(
                title,
                color = ink,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                modifier = Modifier.padding(start = 7.dp),
            )
        }
    }
}

/** La campana se mece mientras el mesero no ha visto la llamada. */
@Composable
private fun WaiterIcon(icon: MenuWaiterIcon) {
    val vector =
        when (icon) {
            MenuWaiterIcon.BELL -> Icons.Outlined.Notifications
            MenuWaiterIcon.RINGING -> Icons.Outlined.NotificationsActive
            MenuWaiterIcon.WALKING -> Icons.AutoMirrored.Outlined.DirectionsWalk
        }
    val ringing = icon == MenuWaiterIcon.RINGING && !reducedMotion()
    val angle =
        if (ringing) {
            val swing = rememberInfiniteTransition(label = "menu-waiter-bell")
            swing
                .animateFloat(
                    initialValue = 0f,
                    targetValue = 0f,
                    animationSpec =
                        infiniteRepeatable(
                            keyframes {
                                durationMillis = 1_400
                                14f at 120
                                -12f at 260
                                8f at 400
                                0f at 540
                            },
                            repeatMode = RepeatMode.Restart,
                        ),
                    label = "menu-waiter-bell-angle",
                ).value
        } else {
            0f
        }
    Icon(vector, contentDescription = null, modifier = Modifier.size(18.dp).rotate(angle))
}

@Composable
private fun WaiterCallCard(state: MenuWaiterCallState) {
    val colors = LocalVaiinillaColors.current
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .shadow(24.dp, shape, clip = false)
                .clip(shape)
                .background(colors.solidPaper ?: colors.paper)
                .border(1.dp, colors.line, shape)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val call = state.call
        if (call != null) {
            CallStatusContent(state = state, call = call)
        } else {
            ReasonsContent(state = state)
        }
        state.error?.let { message ->
            Text(
                message,
                color = colors.coral,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("menu-waiter-error"),
            )
        }
    }
}

@Composable
private fun ReasonsContent(state: MenuWaiterCallState) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val scope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "¿Qué necesitas?",
            color = colors.ink,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 2.dp),
        )
        CallReason.entries.forEach { reason ->
            SeaGlassButton(
                tone = SeaGlassTone.vainilla,
                onClick = {
                    haptics.selection()
                    scope.launch { if (state.send(reason)) haptics.impact() }
                },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth().height(46.dp).testTag("menu-waiter-reason-${reason.wireValue}"),
            ) {
                Text(
                    reason.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                )
            }
        }
        if (state.busy) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = colors.muted)
                Text("Avisando…", color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CallStatusContent(
    state: MenuWaiterCallState,
    call: TableCall,
) {
    val colors = LocalVaiinillaColors.current
    val scope = rememberCoroutineScope()
    val going = call.status == CallStatus.EN_CAMINO
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            if (going) {
                "Tu mesero va en camino"
            } else {
                "Ya le avisamos a tu mesero"
            },
            color = colors.ink,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            listOf(call.space.name, call.reason.label).filter { it.isNotBlank() }.joinToString(" · "),
            color = colors.muted,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    if (canCancelMenuCall(call)) {
        Text(
            "Si lo cancelas, puedes volver a llamarlo enseguida.",
            color = colors.muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
    Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (canCancelMenuCall(call)) {
            SeaGlassButton(
                tone = SeaGlassTone.vainilla,
                onClick = { scope.launch { state.cancel() } },
                enabled = !state.busy,
                modifier = Modifier.weight(1f).height(44.dp).testTag("menu-waiter-cancel"),
            ) {
                Text(
                    "Cancelar llamada",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
        SeaGlassButton(
            tone = SeaGlassTone.lima,
            onClick = { state.close() },
            modifier = Modifier.weight(1f).height(44.dp),
        ) {
            Text(
                "Listo",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

const val MENU_WAITER_PILL_TAG = "menu-waiter-pill"
const val MENU_CART_BAR_TAG = "menu-cart-bar"
private const val MENU_CALL_POLL_MS = 5_000L
