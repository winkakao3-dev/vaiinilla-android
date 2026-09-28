package com.vaiinilla.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Color que sigue a la selección con un resorte; con animaciones del sistema apagadas cambia directo. */
@Composable
internal fun animateSelectionColor(
    target: Color,
    label: String,
): Color {
    val spec = if (reducedMotion()) snap<Color>() else spring(stiffness = Spring.StiffnessMedium)
    return animateColorAsState(targetValue = target, animationSpec = spec, label = label).value
}
