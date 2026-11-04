package com.vaiinilla.app.ui.components

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Sin esto el sistema también desplaza la ventana al abrir el teclado y, sumado a imePadding,
 * el contenido sube el doble y se sale de la pantalla.
 */
@Composable
fun ResizeForKeyboard() {
    val window = (LocalContext.current as? Activity)?.window
    DisposableEffect(window) {
        val previous = window?.attributes?.softInputMode
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        onDispose { previous?.let { window.setSoftInputMode(it) } }
    }
}
