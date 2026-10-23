package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState

/**
 * Las comandas de Cocina como en iOS: primero lo que falta preparar y al final lo listo, así al marcar una lista
 * sube la siguiente. Lo rechazado o entregado ya no es de Cocina y sale de la fila.
 */
internal fun kitchenQueue(orders: List<OrderDetail>): List<OrderDetail> {
    val pending =
        orders.filter {
            it.summary.state == OrderState.PENDING_PAYMENT ||
                it.summary.state == OrderState.PAID ||
                it.summary.state == OrderState.PREPARING
        }
    return pending + orders.filter { it.summary.state == OrderState.READY }
}

/** La nota del pedido ("sin cebolla") con el resumen de las notas por producto, tal como la manda el cliente. */
@Composable
internal fun KitchenNotesNote(
    notes: String,
    ink: Color,
    muted: Color,
) {
    val text = notes.trim()
    if (text.isEmpty()) return
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .background(ink.copy(alpha = 0.05f), RoundedCornerShape(13.dp))
                .padding(11.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.Notes,
            contentDescription = null,
            tint = muted,
            modifier = Modifier.size(16.dp),
        )
        Text(text, color = muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 16.sp)
    }
}
