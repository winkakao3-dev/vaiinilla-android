package com.vaiinilla.app.ui.hotel.staff

import com.vaiinilla.app.core.venue.LocalVenueContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.SpaceCopy
import com.vaiinilla.app.domain.model.activeItems
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.screens.KitchenAge
import com.vaiinilla.app.ui.screens.MoreDots
import com.vaiinilla.app.ui.screens.elapsedSinceMs
import com.vaiinilla.app.ui.screens.kitchenAge
import com.vaiinilla.app.ui.screens.kitchenAgeLabel
import com.vaiinilla.app.ui.screens.kitchenAgeStart
import com.vaiinilla.app.ui.screens.kitchenSplitItems
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import kotlinx.coroutines.delay

/**
 * S01 · Cocina, pedidos a habitaciones. Se usa solo cuando el negocio es un hotel ([com.vaiinilla.app.domain.model.VenueContext.isHotel]):
 * una lista de tarjetas oscuras, una por habitación, con la hora que lleva, lo que se prepara y un solo botón.
 */
@Composable
internal fun HotelKitchenHeader(
    count: Int,
    modifier: Modifier = Modifier,
    onSoldOut: (() -> Unit)? = null,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 6.dp, top = 4.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Cocina · room service", color = colors.muted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                if (count == 1) "1 pedido" else "$count pedidos",
                color = colors.ink,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.6).sp,
            )
        }
        if (onSoldOut != null) {
            HotelChip(label = "¿Se acabó algo?", selected = false, onClick = onSoldOut)
        }
    }
}

@Composable
internal fun HotelKitchenCard(
    order: OrderDetail,
    acting: Boolean,
    canAct: Boolean,
    readyDeadline: Long?,
    onPrimary: () -> Unit,
    onUndoReady: () -> Unit,
    onMore: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val venue = LocalVenueContext.current
    val colors = LocalVaiinillaColors.current
    val summary = order.summary
    val preparing = summary.state == OrderState.PREPARING
    val start = kitchenAgeStart(summary)
    val elapsed by produceState(elapsedSinceMs(start), start) {
        while (true) {
            delay(15_000)
            value = elapsedSinceMs(start)
        }
    }
    val age = elapsed?.let { kitchenAge(it) } ?: KitchenAge.FRESH
    val scheduled = elapsed != null && elapsed!! < 0 && !summary.deliverySlot.isNullOrBlank()
    val room = summary.space?.let { SpaceCopy.short(venue, it.type, it.name) } ?: "#${summary.folio}"
    val (items, _) = kitchenSplitItems(order.activeItems)
    val alert =
        buildList {
            order.user
                ?.allergies
                ?.takeIf { it.isNotEmpty() }
                ?.let { add("Alergia: ${it.joinToString(", ")}") }
            order.kitchenNotes
                .trim()
                .takeIf { it.isNotEmpty() }
                ?.let(::add)
        }.joinToString(" · ")
    HotelCard(modifier = modifier, border = if (age == KitchenAge.LATE && !scheduled) colors.coral else null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    room,
                    color = colors.ink,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.8).sp,
                )
            }
            when {
                scheduled ->
                    HotelTag(
                        "Programado ${summary.deliverySlot!!.substringBefore('-').trim().trimStart('0')}",
                        HotelAmber.copy(alpha = 0.18f),
                        HotelAmber,
                    )
                age == KitchenAge.LATE -> HotelTag(ageText(elapsed), colors.coral, Color.White)
                age == KitchenAge.WAITING -> HotelTag(ageText(elapsed), HotelAmber.copy(alpha = 0.18f), HotelAmber)
                else -> HotelTag(ageText(elapsed), colors.ink.copy(alpha = 0.1f), colors.muted)
            }
            if (onMore != null) {
                Box(
                    modifier =
                        Modifier
                            .padding(start = 4.dp)
                            .size(36.dp)
                            .physicalPress(onClick = onMore)
                            .semantics { contentDescription = "Más opciones de la comanda ${summary.folio}" },
                    contentAlignment = Alignment.Center,
                ) { MoreDots(colors.muted) }
            }
        }
        Column(modifier = Modifier.padding(top = 6.dp)) {
            items.forEach { item ->
                Text(
                    "${item.quantity} × ${item.productName}",
                    color = colors.ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp),
                )
                val detail = item.options.joinToString(" · ") { it.name }
                if (detail.isNotEmpty()) {
                    Text(detail, color = colors.muted, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            if (alert.isNotEmpty()) {
                Text(
                    alert,
                    color = HotelAmber,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        if (readyDeadline != null) {
            UndoReady(readyDeadline, onUndoReady, Modifier.padding(top = 14.dp))
        } else {
            HotelButton(
                label = if (preparing) "Marcar listo" else "Empezar",
                onClick = onPrimary,
                kind =
                    when {
                        preparing -> HotelButtonKind.Lime
                        scheduled -> HotelButtonKind.Outline
                        else -> HotelButtonKind.Ink
                    },
                enabled = canAct && !acting,
                modifier = Modifier.padding(top = 14.dp).fillMaxWidth(),
            )
        }
    }
}

private fun ageText(elapsedMs: Long?): String {
    val label = kitchenAgeLabel(elapsedMs ?: 0)
    return if (label.startsWith("+")) "Hace más de 1 h" else "Hace $label"
}

/** «Lista» espera unos segundos para poder deshacerse: la cuenta atrás va aquí, en el estilo de la tarjeta. */
@Composable
private fun UndoReady(
    deadline: Long,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val left by produceState(secondsLeft(deadline), deadline) {
        while (true) {
            value = secondsLeft(deadline)
            delay(250)
        }
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Marcada como lista", color = colors.accent, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        HotelButton(
            "Deshacer · ${left}s",
            onClick = onUndo,
            kind = HotelButtonKind.Outline,
            height = 40.dp,
            fontSize = 13,
        )
    }
}

private fun secondsLeft(deadline: Long): Long = ((deadline - System.currentTimeMillis()).coerceAtLeast(0) + 999) / 1000
