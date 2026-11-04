package com.vaiinilla.app.ui.components

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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.repository.CallReason
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.ui.order.MenuWaiterCallState
import com.vaiinilla.app.ui.order.MenuWaiterIcon
import com.vaiinilla.app.ui.order.MenuWaiterPhase
import com.vaiinilla.app.ui.order.menuWaiterPillLabel
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.SeaGlassButton
import com.vaiinilla.app.ui.theme.SeaGlassTone

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
 * dos botones. Un toque abre la hoja de motivos (Doop L05); ya enviada, la píldora es el estado en vivo y
 * tocarla vuelve a abrir la hoja para ver o cancelar la llamada.
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
    /** «la Mesa 4», para la hoja antes de llamar. */
    place: String? = null,
    /** El asistente de pedidos, junto a «Mesero»; null si no está disponible. */
    onOpenAssistant: (() -> Unit)? = null,
) {
    val state =
        remember(spaceId) {
            MenuWaiterCallState(spaceId, onCurrent, onCall, onCancel, onJoinTable)
        }
    MenuWaiterLayer(
        state = state,
        visible = visible,
        cart = cart,
        place = place,
        modifier = modifier,
        onOpenAssistant = onOpenAssistant,
    )
}

@Composable
internal fun MenuWaiterLayer(
    state: MenuWaiterCallState,
    visible: Boolean,
    cart: MenuCartBar?,
    modifier: Modifier = Modifier,
    place: String? = null,
    onOpenAssistant: (() -> Unit)? = null,
) {
    val reduced = reducedMotion()
    WaiterCallHost(state = state, visible = visible, place = place)
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(
                        start = VaiinillaBottomNavSideMargin,
                        end = if (cart != null || onOpenAssistant != null) VaiinillaBottomNavSideMargin else 0.dp,
                        bottom = VaiinillaBottomNavTop + 10.dp,
                    ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AnimatedVisibility(
                visible = visible && cart != null,
                enter = if (reduced) fadeIn() else fadeIn(tween(200)) + slideInVertically(spring(dampingRatio = 0.62f, stiffness = 420f)) { it },
                exit = if (reduced) fadeOut() else fadeOut(tween(160)) + slideOutVertically { it / 2 },
            ) {
                // Se queda con el último carrito mientras sale, para que no se vacíe a media animación.
                val shown = remember { mutableStateOf(cart) }
                if (cart != null) shown.value = cart
                shown.value?.let { CartWaiterBar(state = state, cart = it, onOpenAssistant = onOpenAssistant) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
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
                if (onOpenAssistant != null) {
                    AnimatedVisibility(
                        visible = visible && cart == null,
                        enter = if (reduced) fadeIn() else fadeIn(tween(200)) + scaleIn(initialScale = 0.8f),
                        exit = if (reduced) fadeOut() else fadeOut(tween(160)) + scaleOut(targetScale = 0.8f),
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        AssistantPill(onOpenAssistant)
                    }
                }
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
    onOpenAssistant: (() -> Unit)? = null,
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
                .padding(start = if (onOpenAssistant != null) 7.dp else 20.dp, end = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onOpenAssistant != null) {
            AssistantDot(onOpenAssistant)
            Spacer(Modifier.width(12.dp))
        }
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

/** «Pregúntame qué pedir»: abre el asistente de pedidos, junto a «Mesero» (Doop CA1). */
@Composable
private fun AssistantPill(onOpen: () -> Unit) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    Row(
        modifier =
            Modifier
                .heightIn(min = 52.dp)
                .shadow(18.dp, RoundedCornerShape(percent = 50), clip = false)
                .clip(RoundedCornerShape(percent = 50))
                .background(colors.accent)
                .clickable(role = Role.Button) {
                    haptics.click()
                    onOpen()
                }.testTag(MENU_ASSISTANT_PILL_TAG)
                .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.accentInk, modifier = Modifier.size(18.dp))
        Text(
            "Pregúntame qué pedir",
            color = colors.accentInk,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** El asistente dentro de la barra del carrito: un círculo lima a la izquierda (Doop CA1b). */
@Composable
private fun AssistantDot(onOpen: () -> Unit) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    Box(
        modifier =
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(colors.accent)
                .clickable(role = Role.Button) {
                    haptics.click()
                    onOpen()
                }.semantics { contentDescription = "Pregúntale al asistente qué pedir" }
                .testTag(MENU_ASSISTANT_PILL_TAG),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.accentInk, modifier = Modifier.size(20.dp))
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

const val MENU_WAITER_PILL_TAG = "menu-waiter-pill"
const val MENU_CART_BAR_TAG = "menu-cart-bar"
const val MENU_ASSISTANT_PILL_TAG = "menu-assistant-pill"
