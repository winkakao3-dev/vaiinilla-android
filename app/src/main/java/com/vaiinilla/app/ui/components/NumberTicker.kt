package com.vaiinilla.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset

/**
 * Cifra que rueda: al subir el valor entra desde abajo, al bajar desde arriba.
 * Con animaciones del sistema apagadas solo cambia el valor.
 */
@Composable
fun <T : Comparable<T>> NumberTicker(
    value: T,
    modifier: Modifier = Modifier,
    label: String = "number-ticker",
    content: @Composable (T) -> Unit,
) {
    val reduced = reducedMotion()
    AnimatedContent(
        targetState = value,
        modifier = modifier,
        transitionSpec = {
            if (reduced) {
                fadeIn(snap()) togetherWith fadeOut(snap())
            } else {
                val rising = targetState > initialState
                val offsetSpec =
                    spring<IntOffset>(
                        dampingRatio = 0.85f,
                        stiffness = Spring.StiffnessMediumLow,
                    )
                (
                    slideInVertically(offsetSpec) { height -> if (rising) height else -height } +
                        fadeIn(spring(stiffness = 300f))
                ) togetherWith
                    (
                        slideOutVertically(offsetSpec) { height -> if (rising) -height else height } +
                            fadeOut(spring(stiffness = 500f))
                    )
            }.using(SizeTransform(clip = false))
        },
        label = label,
        content = { content(it) },
    )
}
