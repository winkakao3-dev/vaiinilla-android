package com.vaiinilla.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Corre [onStart] mientras la pantalla está en composición y la app a la vista, y [onStop] al salir de la
 * pantalla o al pasar la app a segundo plano. Los sondeos de una pantalla cuelgan de aquí: atados solo a la
 * composición, seguían pidiendo datos con la app en segundo plano (FLOW-15).
 */
@Composable
fun WhileStarted(
    vararg keys: Any?,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val start by rememberUpdatedState(onStart)
    val stop by rememberUpdatedState(onStop)
    DisposableEffect(lifecycleOwner, *keys) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> start()
                    Lifecycle.Event.ON_STOP -> stop()
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stop()
        }
    }
}
