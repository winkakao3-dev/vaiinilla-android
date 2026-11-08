package com.vaiinilla.app.ui.components

import com.vaiinilla.app.core.venue.LocalVenueContext
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.core.notifications.TableCallPushes
import com.vaiinilla.app.domain.model.SpaceCopy
import com.vaiinilla.app.domain.model.VenueContext
import com.vaiinilla.app.domain.repository.CallReason
import com.vaiinilla.app.domain.repository.CallStatus
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.ui.order.MenuWaiterCallState
import com.vaiinilla.app.ui.order.canCancelMenuCall
import com.vaiinilla.app.ui.sharedtable.SHARED_TABLE_ALIAS_MAX
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Cómo dice el cliente lo que necesita en la hoja; el personal ve [CallReason.staffLabel]. */
internal val CallReason.chipLabel: String
    get() =
        when (this) {
            CallReason.CUENTA -> "La cuenta"
            CallReason.PROBLEMA -> "Algo salió mal"
            CallReason.UTENSILIOS -> "Servilletas o cubiertos"
            CallReason.ATENCION -> "Otra cosa"
        }

/** El orden del dibujo: lo que más se pide primero y «Otra cosa» al final. */
private val ReasonOrder = listOf(CallReason.CUENTA, CallReason.PROBLEMA, CallReason.UTENSILIOS, CallReason.ATENCION)

/** En un hotel lo pedido se carga a la habitación y se paga al salir: a recepción no se le pide «la cuenta». */
internal fun callReasons(venue: VenueContext): List<CallReason> =
    if (venue.capabilities.waiterIsReception) ReasonOrder - CallReason.CUENTA else ReasonOrder

/**
 * La llamada al mesero de una mesa, para el menú y la mesa compartida: la mantiene al día mientras se ve y abre
 * la hoja (Doop L05) cuando el cliente elige qué necesita o quiere ver la llamada que ya hizo.
 */
@Composable
internal fun WaiterCallHost(
    state: MenuWaiterCallState,
    visible: Boolean = true,
    /** «la Mesa 4», para decir a dónde va antes de llamar. */
    place: String? = null,
) {
    LaunchedEffect(state) {
        while (true) {
            state.refresh()
            TableCallPushes.awaitNextPoll(WAITER_CALL_POLL_MS)
        }
    }
    LaunchedEffect(state.cooldown) {
        if (state.cooldown > 0) {
            delay(1_000)
            state.tick()
        }
    }
    LaunchedEffect(visible) { if (!visible) state.close() }
    if (visible && state.expanded) WaiterCallSheet(state = state, place = place)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WaiterCallSheet(
    state: MenuWaiterCallState,
    place: String?,
) {
    val colors = LocalVaiinillaColors.current
    ModalBottomSheet(
        onDismissRequest = state::close,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.paper,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = { ClientSheetHandle() },
    ) {
        WaiterCallContent(
            state = state,
            place = place,
            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WaiterCallContent(
    state: MenuWaiterCallState,
    place: String?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val scope = rememberCoroutineScope()
    val call = state.call
    Column(modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        WaiterCallHeader(call = call, place = call?.let { SpaceCopy.named(it.space.type, it.space.name) } ?: place)
        if (call == null && state.askingName) {
            WaiterCallNameStep(state = state)
            return@Column
        }
        if (call == null) {
            Row(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("¿Qué necesitas?", color = colors.ink2, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Así llega preparado",
                    color = colors.muted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 5.dp),
                )
            }
        } else {
            Text(
                "Lo que pediste",
                color = colors.ink2,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp),
            )
        }
        FlowRow(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Ya enviada, el motivo no se cambia: el servidor solo lo recibe al crear la llamada.
            val reasons = if (call == null) callReasons(LocalVenueContext.current) else listOf(call.reason)
            reasons.forEach { reason ->
                ReasonChip(
                    reason = reason,
                    selected = call != null,
                    enabled = call == null && !state.busy,
                    onClick = {
                        haptics.selection()
                        scope.launch { if (state.send(reason)) haptics.impact() }
                    },
                )
            }
        }
        if (state.busy && call == null) {
            Row(
                modifier = Modifier.padding(start = 20.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = colors.muted)
                Text("Avisando…", color = colors.muted, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (call != null && canCancelMenuCall(call)) {
            Text(
                "Si lo cancelas, puedes volver a llamarlo enseguida.",
                color = colors.muted,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp),
            )
        }
        state.error?.let { message ->
            Text(
                message,
                color = colors.coralText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp).testTag("menu-waiter-error"),
            )
        }
        if (call != null) {
            Spacer(Modifier.height(36.dp))
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (canCancelMenuCall(call)) {
                    ClientPillButton(
                        text = "Cancelar llamada",
                        onClick = { scope.launch { state.cancel() } },
                        primary = false,
                        enabled = !state.busy,
                        height = 54.dp,
                        modifier = Modifier.weight(1f).testTag("menu-waiter-cancel"),
                    )
                }
                ClientPillButton(text = "Listo", onClick = state::close, height = 54.dp, modifier = Modifier.weight(1f))
            }
        } else {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WaiterCallHeader(
    call: TableCall?,
    place: String?,
) {
    val venue = LocalVenueContext.current
    val colors = LocalVaiinillaColors.current
    val going = call?.status == CallStatus.EN_CAMINO
    // El cliente nunca ve el nombre del mesero, aunque el servidor lo mande: cuida al personal (Kao, 3 nov).
    val title =
        when {
            venue.capabilities.waiterIsReception && call == null -> "Llamar a recepción"
            venue.capabilities.waiterIsReception && going -> "Ya vienen a tu habitación"
            venue.capabilities.waiterIsReception -> "Ya le avisamos a recepción"
            call == null -> "Llamar a tu mesero"
            going -> "Tu mesero va en camino"
            else -> "Ya le avisamos a tu mesero"
        }
    val subtitle =
        when {
            place == null -> if (call == null) "Elige qué necesitas y le avisamos" else null
            call == null -> "Le avisamos que vaya a $place"
            else -> "Va a $place"
        }
    Row(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BellBadge(ringing = call != null && !going, walking = going)
        Column(Modifier.weight(1f)) {
            AnimatedContent(
                targetState = title,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "waiter-call-title",
            ) { text ->
                Text(
                    text,
                    color = colors.ink,
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.9).sp,
                )
            }
            subtitle?.let {
                Text(
                    it,
                    color = colors.muted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}

/** La campana en el círculo lima; mientras nadie la atiende, una onda sale de ella. */
@Composable
private fun BellBadge(
    ringing: Boolean,
    walking: Boolean,
) {
    val colors = LocalVaiinillaColors.current
    val reduced = reducedMotion()
    Box(Modifier.size(60.dp).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        if (ringing) {
            val (scale, alpha) =
                if (reduced) {
                    1.25f to 0.25f
                } else {
                    val wave = rememberInfiniteTransition(label = "waiter-bell-wave")
                    val progress by wave.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(1_600, easing = LinearOutSlowInEasing), RepeatMode.Restart),
                        label = "waiter-bell-wave-progress",
                    )
                    (0.8f + 0.8f * progress) to (0.7f * (1f - progress))
                }
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }.clip(CircleShape)
                    .background(colors.accent),
            )
        }
        Box(
            Modifier.fillMaxSize().clip(CircleShape).background(colors.accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (walking) Icons.AutoMirrored.Outlined.DirectionsWalk else Icons.Outlined.Notifications,
                contentDescription = null,
                tint = colors.accentInk,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
private fun ReasonChip(
    reason: CallReason,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val fill by animateColorAsState(if (selected) colors.ink else colors.paper2, label = "reason-fill")
    val ink by animateColorAsState(if (selected) colors.paper else colors.ink2, label = "reason-ink")
    Box(
        modifier =
            Modifier
                .height(44.dp)
                .physicalPress(enabled = enabled, scale = PhysicalPressScale.Small, onClick = onClick)
                .clip(RoundedCornerShape(percent = 50))
                .background(fill)
                .semantics {
                    role = Role.Button
                    this.selected = selected
                }.testTag("menu-waiter-reason-${reason.wireValue}")
                .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(reason.chipLabel, color = ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/**
 * Sin cuenta, antes de la primera llamada: su nombre, con el mismo aviso de términos que al pedir. Así el
 * mesero sabe a quién va y otro invitado de la mesa no choca con «Cliente».
 */
@Composable
private fun WaiterCallNameStep(state: MenuWaiterCallState) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    val submit: () -> Unit = { scope.launch { if (state.sendWithName(name)) haptics.impact() } }
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp)) {
        Text("¿A quién le avisamos?", color = colors.ink2, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(SHARED_TABLE_ALIAS_MAX) },
            label = { Text("Tu nombre") },
            supportingText = {
                Text("Al continuar aceptas los términos y el aviso de privacidad vigentes.")
            },
            singleLine = true,
            enabled = !state.busy,
            keyboardOptions =
                KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { submit() }),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.ink,
                    unfocusedTextColor = colors.ink,
                    focusedBorderColor = colors.ink,
                    unfocusedBorderColor = colors.line,
                    focusedLabelColor = colors.muted,
                    unfocusedLabelColor = colors.muted,
                    cursorColor = colors.ink,
                ),
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp).testTag("menu-waiter-name"),
        )
        state.error?.let { message ->
            Text(
                message,
                color = colors.coralText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp).testTag("menu-waiter-error"),
            )
        }
        Spacer(Modifier.height(20.dp))
        ClientPillButton(
            text =
                when {
                    state.busy -> "Avisando…"
                    LocalVenueContext.current.capabilities.waiterIsReception -> "Llamar a recepción"
                    else -> "Llamar al mesero"
                },
            onClick = submit,
            enabled = !state.busy && name.isNotBlank(),
            height = 54.dp,
            modifier = Modifier.fillMaxWidth().testTag("menu-waiter-name-send"),
        )
    }
}

private const val WAITER_CALL_POLL_MS = 5_000L