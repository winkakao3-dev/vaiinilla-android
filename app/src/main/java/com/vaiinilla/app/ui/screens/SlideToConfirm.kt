package com.vaiinilla.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.animateSelectionColor
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val ThumbSize = 52.dp
private val TrackInset = 4.dp
private const val CONFIRM_FRACTION = 0.8f

/**
 * Deslizar para confirmar, para lo que no se deshace. Con lector de pantalla es un botón y el toque
 * confirma. Dispara una vez; si [busy] vuelve a false sin que la hoja se cierre (el servidor rechazó),
 * regresa al inicio para reintentar.
 */
@Composable
internal fun SlideToConfirm(
    label: String,
    enabled: Boolean,
    busy: Boolean,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    onConfirm: () -> Unit,
) {
    val haptics = rememberVaiinillaHaptics()
    val reduceMotion = reducedMotion()
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    var maxOffset by remember { mutableFloatStateOf(0f) }
    var fired by remember { mutableStateOf(false) }
    val active = enabled && !busy && !fired
    val thumbPx = with(LocalDensity.current) { (ThumbSize + TrackInset * 2).toPx() }

    suspend fun settle(target: Float) {
        if (reduceMotion) {
            offset.snapTo(target)
        } else {
            offset.animateTo(target, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow))
        }
    }

    fun confirm() {
        if (!active) return
        fired = true
        haptics.success()
        scope.launch { settle(maxOffset) }
        onConfirm()
    }

    LaunchedEffect(busy) {
        if (!busy && fired) {
            fired = false
            settle(0f)
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { maxOffset > 0f && offset.value >= maxOffset * CONFIRM_FRACTION }
            .distinctUntilChanged()
            .drop(1)
            .collect { if (it) haptics.selection() }
    }

    val dragState =
        rememberDraggableState { delta ->
            scope.launch { offset.snapTo((offset.value + delta).coerceIn(0f, maxOffset)) }
        }
    val thumbColor = animateSelectionColor(if (enabled) colors.accentLime else colors.cardBackground, "slide-thumb")
    val progress = if (maxOffset > 0f) offset.value / maxOffset else 0f
    val trackShape = RoundedCornerShape(30.dp)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(ThumbSize + TrackInset * 2)
                .clip(trackShape)
                .background(colors.cardInner)
                .border(1.dp, colors.cardBorder, trackShape)
                .onSizeChanged { maxOffset = (it.width - thumbPx).coerceAtLeast(0f) }
                .clearAndSetSemantics {
                    role = Role.Button
                    contentDescription = label
                    if (active) {
                        onClick(label = label) {
                            confirm()
                            true
                        }
                    } else {
                        disabled()
                    }
                }.draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    enabled = active,
                    onDragStopped = { velocity ->
                        val far = offset.value >= maxOffset * CONFIRM_FRACTION
                        val flung = velocity > 2_000f && offset.value >= maxOffset * 0.4f
                        if (far || flung) confirm() else settle(0f)
                    },
                ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .width(with(LocalDensity.current) { (thumbPx + offset.value).toDp() })
                    .background(colors.accentLime.copy(alpha = 0.28f * progress)),
        )
        Text(
            label,
            color = if (enabled) colors.textPrimary else colors.textMuted,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(start = ThumbSize + 12.dp, end = 16.dp)
                    .graphicsLayer { alpha = (1f - progress * 1.6f).coerceIn(0f, 1f) },
        )
        Box(
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset(offset.value.roundToInt(), 0) }
                    .padding(TrackInset)
                    .size(ThumbSize)
                    .clip(CircleShape)
                    .background(thumbColor),
            contentAlignment = Alignment.Center,
        ) {
            if (busy) {
                CircularProgressIndicator(
                    color = colors.accentInk,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = if (enabled) colors.accentInk else colors.textMuted,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
