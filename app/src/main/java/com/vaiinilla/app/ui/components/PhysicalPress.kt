package com.vaiinilla.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

enum class PhysicalPressScale {
    Default,
    Small,
    Nav,
    ProductCard,
}

fun Modifier.physicalPress(
    enabled: Boolean = true,
    scale: PhysicalPressScale = PhysicalPressScale.Default,
    onClick: () -> Unit,
): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val targetScale = when {
        !pressed -> 1f
        scale == PhysicalPressScale.Small -> 0.93f
        scale == PhysicalPressScale.Nav -> 0.97f
        scale == PhysicalPressScale.ProductCard -> 0.955f
        else -> 0.965f
    }
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = if (pressed) {
            tween(durationMillis = 90)
        } else {
            tween(durationMillis = 240)
        },
        label = "physical-press-scale",
    )
    this
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
        }
        .pointerInput(enabled, onClick) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onPress = {
                    pressed = true
                    val released = tryAwaitRelease()
                    pressed = false
                    if (released) onClick()
                },
            )
        }
}
