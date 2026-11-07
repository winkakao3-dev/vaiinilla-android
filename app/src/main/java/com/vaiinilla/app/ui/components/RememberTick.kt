package com.vaiinilla.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay

/**
 * Un reloj que sube cada [periodMs]. Devuelve el [State] sin leerlo: quien lo lea (dentro de un derivedStateOf o en
 * la hoja que muestra la hora) es lo único que se vuelve a dibujar; la pantalla que lo crea no se invalida.
 */
@Composable
fun rememberTick(periodMs: Long): State<Int> {
    val tick = remember { mutableIntStateOf(0) }
    LaunchedEffect(periodMs) {
        while (true) {
            delay(periodMs)
            tick.intValue += 1
        }
    }
    return tick
}
