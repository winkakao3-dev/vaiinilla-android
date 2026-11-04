package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.localhub.LocalTicket
import com.vaiinilla.app.domain.localhub.LocalTicketState
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val LocalYolk = Color(0xFFFFD15B)
private val LocalInk = Color(0xFF3B2A00)
private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Cocina sin internet: las comandas que llegaron por la Caja del local. Cada una se quita sola cuando su pedido real
 * aparece en el tablero (mismo id), así que nunca se ven dos veces.
 */
@Composable
internal fun LocalTicketsCard(
    tickets: List<LocalTicket>,
    connected: Boolean,
    onReady: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Sin comandas no se muestra nada: con internet Cocina trabaja con su tablero normal.
    if (tickets.isEmpty()) return
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(LocalYolk.copy(alpha = 0.35f))
                .semantics { liveRegion = LiveRegionMode.Polite }
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Comandas sin internet (por la Caja del local)",
            color = LocalInk,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
        )
        if (!connected) {
            Text(
                "No encuentro la Caja ahora; esto es lo último que llegó.",
                color = LocalInk,
                fontSize = 12.5.sp,
            )
        }
        tickets.forEach { ticket ->
            val ready = ticket.state != LocalTicketState.PENDING
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "${ticket.localFolio} · ${ticket.placeLabel} · " +
                            TimeFormat.format(ticket.capturedAt.atZone(ZoneId.systemDefault())),
                        color = LocalInk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                    ticket.items.forEach { item ->
                        Text(
                            buildString {
                                append("${item.quantity}× ${item.name}")
                                if (item.notes.isNotBlank()) append(" — ${item.notes}")
                            },
                            color = LocalInk,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                        )
                    }
                    Text("Tomó: ${ticket.takenBy}", color = LocalInk, fontSize = 12.sp)
                }
                if (ready) {
                    Text("Listo", color = LocalInk, fontWeight = FontWeight.Bold)
                } else {
                    TextButton(onClick = { onReady(ticket.id) }) {
                        Text("Listo", color = LocalInk, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

/** Solo las que todavía no llegan como pedido real; las de barra (`caja`) no son de Cocina. */
internal fun kitchenLocalTickets(
    tickets: List<LocalTicket>,
    knownOrderIds: Set<String>,
): List<LocalTicket> =
    tickets
        .filter { it.id !in knownOrderIds && it.state != LocalTicketState.DELIVERED }
        .map { ticket -> ticket.copy(items = ticket.items.filter { it.station != "caja" }) }
        .filter { it.items.isNotEmpty() }
        .sortedBy { it.capturedAt }
