package com.vaiinilla.app.ui.components

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Sin esto el sistema también desplaza la ventana al abrir el teclado y, sumado a imePadding,
 * el contenido sube el doble y se sale de la pantalla (queda un hueco negro del alto del teclado).
 *
 * Cuenta cuántas pantallas lo piden: al pasar de una a otra la nueva entra antes de que la vieja se vaya, y si cada
 * una regresara «el modo de antes» al salir, la vieja dejaba la ventana otra vez en desplazar.
 */
@Composable
fun ResizeForKeyboard() {
    val window = (LocalContext.current as? Activity)?.window
    DisposableEffect(window) {
        if (window == null) return@DisposableEffect onDispose {}
        val token =
            KeyboardResizeCounter.acquire(
                current = window.attributes.softInputMode,
                setMode = window::setSoftInputMode,
                resize = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE,
            )
        onDispose { KeyboardResizeCounter.release(token, window::setSoftInputMode) }
    }
}

/** El conteo de [ResizeForKeyboard], aparte para probarlo sin ventana. */
internal object KeyboardResizeCounter {
    private var holders = 0
    private var original: Int? = null

    /** Pone el modo de encoger; el primero guarda el modo que había. */
    fun acquire(
        current: Int,
        setMode: (Int) -> Unit,
        resize: Int,
    ): Any {
        if (holders == 0) original = current
        holders++
        setMode(resize)
        return Any()
    }

    /** El último en salir regresa el modo que había antes del primero. */
    fun release(
        @Suppress("UNUSED_PARAMETER") token: Any,
        setMode: (Int) -> Unit,
    ) {
        if (holders == 0) return
        holders--
        if (holders == 0) {
            original?.let(setMode)
            original = null
        }
    }

    internal fun resetForTest() {
        holders = 0
        original = null
    }
}
