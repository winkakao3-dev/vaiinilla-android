package com.vaiinilla.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.R

@Composable
fun VaiinillaAssistantButton(
    onClick: () -> Unit,
    reactionSignal: Int,
    isDarkTheme: Boolean,
    hasPendingFirstSteps: Boolean,
    modifier: Modifier = Modifier,
    touchSize: androidx.compose.ui.unit.Dp = 44.dp,
) {
    var reactionTarget by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(reactionSignal) {
        if (reactionSignal > 0) {
            reactionTarget = 1f
            kotlinx.coroutines.delay(220)
            reactionTarget = 0f
        }
    }
    val energy by
        animateFloatAsState(
            targetValue = reactionTarget,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
            label = "assistant_header_orb_energy",
        )
    val reduceMotion = reducedMotion()
    val hintTransition = rememberInfiniteTransition(label = "assistant_hint_ring")
    val hintAlpha by
        if (!hasPendingFirstSteps || reduceMotion) {
            remember { mutableFloatStateOf(if (hasPendingFirstSteps) 0.72f else 0f) }
        } else {
            hintTransition.animateFloat(
                initialValue = 0.34f,
                targetValue = 0.86f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(1_200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "assistant_hint_ring_alpha",
            )
        }

    Box(
        modifier =
            modifier
                .size(touchSize)
                .semantics { contentDescription = "Abrir ayuda" }
                .physicalPress(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (hasPendingFirstSteps) {
            Box(
                Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .border(1.5.dp, Color(0xFFB7DE63).copy(alpha = hintAlpha), CircleShape),
            )
        }
        Image(
            painter = painterResource(R.drawable.mascot_question),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier =
                Modifier
                    .size(touchSize)
                    .scale(1f + energy * 0.04f),
        )
    }
}
