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
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.ui.components.animateSelectionColor
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Qué tan vieja es una comanda. La cocina lo lee de lejos por el color, como en una pantalla de cocina. */
internal enum class KitchenAge { FRESH, WAITING, LATE }

internal fun kitchenAge(elapsedMs: Long): KitchenAge =
    when {
        elapsedMs >= 20 * 60_000 -> KitchenAge.LATE
        elapsedMs >= 10 * 60_000 -> KitchenAge.WAITING
        else -> KitchenAge.FRESH
    }

/**
 * Desde cuándo cuenta el tiempo en cocina. Un pedido apartado para otro día (padres e hijos) se creó días antes:
 * cuenta desde el inicio de su franja si es posterior a la creación (STAFF-20). La franja es hora local del negocio;
 * el teléfono de cocina está en ese mismo lugar.
 */
internal fun kitchenAgeStart(
    summary: OrderSummary,
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val created = runCatching { Instant.parse(summary.createdAt) }.getOrNull() ?: return summary.createdAt
    val slotStart =
        runCatching {
            val day = LocalDate.parse(summary.deliveryDate?.take(10))
            val time =
                summary.deliverySlot
                    ?.substringBefore('-')
                    ?.trim()
                    ?.let(LocalTime::parse) ?: LocalTime.MIDNIGHT
            day.atTime(time).atZone(zone).toInstant()
        }.getOrNull()
    return if (slotStart != null && slotStart.isAfter(created)) slotStart.toString() else summary.createdAt
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
