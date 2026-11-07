package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.time.BusinessTime
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
 * cuenta desde el inicio de su franja si es posterior a la creación (STAFF-20). La franja es hora local del negocio,
 * no la del teléfono.
 */
internal fun kitchenAgeStart(
    summary: OrderSummary,
    zone: ZoneId = BusinessTime.zone,
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
/** El chip de K01: el tiempo en un fondo verde, ámbar o rojo, para leerlo desde la estufa. */
@Composable
internal fun KitchenAgeChip(
    createdAt: String,
    modifier: Modifier = Modifier,
) {
    val elapsed by produceState(elapsedSinceMs(createdAt), createdAt) {
        while (true) {
            delay(15_000)
            value = elapsedSinceMs(createdAt)
        }
    }
    val (bg, fg) =
        when (elapsed?.let(::kitchenAge)) {
            KitchenAge.LATE -> Color(0xFFFBD2CF) to Color(0xFF8A1D17)
            KitchenAge.WAITING -> Color(0xFFFFE9AE) to Color(0xFF6B4C00)
            else -> Color(0xFFE5EFC9) to Color(0xFF3B5310)
        }
    val background = animateSelectionColor(bg, "kitchen-age-bg")
    val ink = animateSelectionColor(fg, "kitchen-age-ink")
    Row(
        modifier =
            modifier
                .clip(CircleShape)
                .background(background)
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = ink, modifier = Modifier.size(14.dp))
        Text(
            elapsed?.let(::kitchenAgeLabel) ?: "--",
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = ink,
        )
    }
}