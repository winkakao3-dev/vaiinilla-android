package com.vaiinilla.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

/**
 * "Llamar al mesero" siempre a la mano en el menú de una mesa: una píldora sobre la barra de abajo, del lado
 * contrario a "Pedir ya". Un toque abre los motivos encima de ella; ya enviada, la píldora es el estado en vivo
 * y tocarla deja cancelar. Con "Pedir ya" a la vista ([compact]) se encoge para que quepan las dos.
 */
@Composable
fun MenuWaiterLayer(
    spaceId: Int,
    visible: Boolean,
    compact: Boolean,
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
    MenuWaiterLayer(state = state, visible = visible, compact = compact, modifier = modifier)
}

@Composable
internal fun MenuWaiterLayer(
    state: MenuWaiterCallState,
    visible: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val reduced = reducedMotion()

    // Recupera una llamada abierta y la mantiene fresca mientras el menú está a la vista.
    LaunchedEffect(state) {
        while (true) {
            state.refresh()
            delay(MENU_CALL_POLL_MS)
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
                        end = if (expanded) VaiinillaBottomNavSideMargin else 0.dp,
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
                visible = visible && state.phase != MenuWaiterPhase.UNAVAILABLE,
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
                WaiterPill(state = state, compact = compact)
            }
        }
    }
}

@Composable
private fun WaiterPill(
    state: MenuWaiterCallState,
    compact: Boolean,
) {
    val haptics = rememberVaiinillaHaptics()
    val label = menuWaiterPillLabel(state.call, state.cooldown)
    val waiting = state.call == null && state.cooldown > 0
    SeaGlassButton(
        // Con "Pedir ya" al lado, el lima es suyo: la llamada en curso se queda en vainilla.
        tone = if (state.call != null && !compact) SeaGlassTone.lima else SeaGlassTone.vainilla,
        onClick = {
            haptics.click()
            state.tap()
        },
        enabled = !waiting,
        modifier =
            Modifier
                .heightIn(min = 52.dp)
                .testTag(MENU_WAITER_PILL_TAG)
                .clearAndSetSemantics {
                    contentDescription = label.title
                    role = Role.Button
                },
    ) {
        Spacer(Modifier.width(18.dp))
        WaiterIcon(icon = label.icon)
        AnimatedContent(
            targetState = if (compact) label.shortTitle else label.title,
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
                "${call.takenBy?.name?.takeIf {
                    it.isNotBlank()
                } ?: "Tu mesero"} va en camino"
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
private const val MENU_CALL_POLL_MS = 5_000L
