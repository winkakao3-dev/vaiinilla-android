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
import com.vaiinilla.app.domain.model.PaymentMethod

/**
 * Las comandas de Cocina: primero lo que falta preparar y al final lo listo, así al marcar una lista sube la
 * siguiente. Cada grupo va del más antiguo al más nuevo (el que llegó primero se prepara primero) y un pedido
 * que llega no desplaza al que se está atendiendo. Lo rechazado o entregado ya no es de Cocina y sale de la fila,
 * y un pedido con tarjeta o saldo que aún no se paga tampoco entra: Caja no puede cobrarlo.
 */
internal fun kitchenQueue(orders: List<OrderDetail>): List<OrderDetail> {
    val oldestFirst = orders.sortedBy { it.summary.createdAt }
    val pending =
        oldestFirst.filter {
            val state = it.summary.state
            state == OrderState.PAID ||
                state == OrderState.PREPARING ||
                (state == OrderState.PENDING_PAYMENT && it.summary.paymentMethod == PaymentMethod.CASH)
        }
    return pending + oldestFirst.filter { it.summary.state == OrderState.READY }
}

/**
 * La comanda abierta: la elegida a mano, o la primera de la fila. Una elegida que ya quedó lista cede su lugar a
 * la siguiente por preparar, para que "Marcar lista" no deje la tarjeta pegada en una comanda terminada.
 */
internal fun kitchenActiveOrder(
    queue: List<OrderDetail>,
    selectedOrderId: String?,
): OrderDetail? {
    val selected = queue.firstOrNull { it.summary.id == selectedOrderId }
    val firstPending = queue.firstOrNull { it.summary.state != OrderState.READY }
    return when {
        selected == null -> queue.firstOrNull()
        selected.summary.state == OrderState.READY && firstPending != null -> firstPending
        else -> selected
    }
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
