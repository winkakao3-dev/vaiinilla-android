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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

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
): Modifier =
    composed {
        var pressed by remember { mutableStateOf(false) }
        val targetScale =
            when {
                !pressed -> 1f
                scale == PhysicalPressScale.Small -> 0.93f
                scale == PhysicalPressScale.Nav -> 0.97f
                scale == PhysicalPressScale.ProductCard -> 0.955f
                else -> 0.965f
            }
        val animatedScale by animateFloatAsState(
            targetValue = targetScale,
            animationSpec =
                if (reducedMotion()) {
                    tween(durationMillis = 0)
                } else if (pressed) {
                    tween(durationMillis = 90)
                } else {
                    tween(durationMillis = 240)
                },
            label = "physical-press-scale",
        )
        this
            .semantics {
                if (enabled) {
                    role = Role.Button
                    onClick {
                        onClick()
                        true
                    }
                }
            }.graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }.pointerInput(enabled, onClick) {
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

@androidx.compose.runtime.Composable
internal fun reducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            ) == 0f
        }.getOrDefault(false)
    }
}
