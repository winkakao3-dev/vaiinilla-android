package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.animateSelectionColor
import kotlinx.coroutines.delay

/** Qué tan vieja es una comanda. La cocina lo lee de lejos por el color, como en una pantalla de cocina. */
internal enum class KitchenAge { FRESH, WAITING, LATE }

internal fun kitchenAge(elapsedMs: Long): KitchenAge =
    when {
        elapsedMs >= 20 * 60_000 -> KitchenAge.LATE
        elapsedMs >= 10 * 60_000 -> KitchenAge.WAITING
        else -> KitchenAge.FRESH
    }

/** Minutos enteros; pasada una hora el minuto exacto ya no ayuda. */
internal fun kitchenAgeLabel(elapsedMs: Long): String {
    val minutes = elapsedMs.coerceAtLeast(0) / 60_000
    return if (minutes >= 60) "+1 h" else "$minutes min"
}

// Sobre el papel de la comanda: ámbar 4.9:1 y rojo 5.6:1.
private val KitchenAgeAmber = Color(0xFF9A5400)
private val KitchenAgeRed = Color(0xFFB3261E)

/** Tiempo en cocina de la comanda; se actualiza solo y cambia de color a los 10 y a los 20 minutos. */
@Composable
internal fun KitchenAgeText(
    createdAt: String,
    ink: Color,
    muted: Color,
    modifier: Modifier = Modifier,
) {
    val elapsed by produceState(elapsedSinceMs(createdAt), createdAt) {
        while (true) {
            delay(15_000)
            value = elapsedSinceMs(createdAt)
        }
    }
    val color =
        animateSelectionColor(
            when (elapsed?.let(::kitchenAge)) {
                KitchenAge.LATE -> KitchenAgeRed
                KitchenAge.WAITING -> KitchenAgeAmber
                else -> ink
            },
            "kitchen-age",
        )
    Row(verticalAlignment = Alignment.Bottom, modifier = modifier) {
        Text(
            elapsed?.let(::kitchenAgeLabel) ?: "--",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = color,
        )
        Text("  en cocina", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = muted)
    }
}
