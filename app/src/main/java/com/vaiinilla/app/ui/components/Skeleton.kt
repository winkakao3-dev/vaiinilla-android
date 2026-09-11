package com.vaiinilla.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

/**
 * Placeholder block with a soft sweep highlight. Used while a first load is in
 * flight so the screen paints its final structure and never shows a false
 * "empty" state.
 */
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    corner: Dp = 12.dp,
) {
    val colors = LocalVaiinillaColors.current
    val reduceMotion = reducedMotion()
    val transition = rememberInfiniteTransition(label = "skeleton_sweep")
    val progress by
        if (reduceMotion) {
            remember { mutableFloatStateOf(0.5f) }
        } else {
            transition.animateFloat(
                initialValue = -0.5f,
                targetValue = 1.5f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = 1_150, easing = LinearEasing),
                    ),
                label = "skeleton_progress",
            )
        }

    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(corner))
                .drawBehind {
                    val base = colors.paper2
                    val highlight = colors.ink.copy(alpha = 0.06f)
                    val sweep = size.width * 0.9f
                    val center = size.width * progress
                    drawRect(
                        brush =
                            Brush.linearGradient(
                                colors = listOf(base, highlight, base),
                                start = Offset(center - sweep / 2f, 0f),
                                end = Offset(center + sweep / 2f, 0f),
                            ),
                    )
                },
    )
}
