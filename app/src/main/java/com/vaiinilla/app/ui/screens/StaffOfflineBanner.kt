package com.vaiinilla.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.vaiinilla.app.core.contract.UnreadableOrders
import com.vaiinilla.app.domain.offline.OfflineStaffOrder
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.operational.OfflineStaffOrdersState

private val OfflineYolk = Color(0xFFFFD15B)
private val OfflineInk = Color(0xFF3B2A00)

/**
 * N12: sin internet, una franja amarilla lo dice arriba en Caja, Cocina y Mesero. Mesero y Caja ya guardan los
 * pedidos nuevos en el teléfono y los mandan solos al volver la red ([savesOrders]); lo demás que se toque puede no
 * llegar, así que para eso la franja pide revisar cuando vuelva.
 */
@Composable
internal fun StaffOfflineBanner(
    offline: Boolean,
    modifier: Modifier = Modifier,
    /** Mesero y Caja: los pedidos nuevos se guardan aquí y se mandan solos (plan «Vaiinilla sin internet»). */
    savesOrders: Boolean = false,
) {
    val reduced = reducedMotion()
    // Entra con su resorte aunque la franja nazca ya sin red (el renglón de la lista aparece al perderla).
    val visible = remember { MutableTransitionState(false) }.apply { targetState = offline }
    AnimatedVisibility(
        visibleState = visible,
        modifier = modifier,
        enter =
            if (reduced) {
                EnterTransition.None
            } else {
                slideInVertically(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) { -it / 2 } +
                    expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) +
                    fadeIn()
            },
        exit = if (reduced) ExitTransition.None else shrinkVertically(spring(stiffness = Spring.StiffnessMedium)) + fadeOut(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(OfflineYolk)
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Outlined.WifiOff, contentDescription = null, tint = OfflineInk, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Sin internet", color = OfflineInk, fontWeight = FontWeight.Black, fontSize = 15.sp)
                Text(
                    if (savesOrders) {
                        "Los pedidos nuevos se guardan en este teléfono y se mandan solos al volver la red. Cocina " +
                            "no los ve hasta entonces: llévale la comanda. Lo demás puede no llegar."
                    } else {
                        "Lo que ves puede estar viejo y lo que toques puede no llegar. Cuando vuelva la red, revisa " +
                            "que se haya guardado."
                    },
                    color = OfflineInk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

/** Cuántos pedidos mandó el servidor que esta versión no supo leer; se lee en cada pantalla del personal. */
@Composable
internal fun rememberUnreadableOrders(): Int {
    val count by UnreadableOrders.count.collectAsState()
    return count
}

/**
 * Un pedido con un estado o un método de pago que esta versión no conoce se omite de la lista para que los demás
 * se sigan viendo. Sin esta franja el personal no sabría que le falta un pedido real: se le pide actualizar la app.
 */
@Composable
internal fun StaffUpdateAppBanner(
    count: Int,
    modifier: Modifier = Modifier,
) {
    if (count <= 0) return
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(OfflineYolk)
                .semantics { liveRegion = LiveRegionMode.Polite }
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Outlined.SystemUpdate, contentDescription = null, tint = OfflineInk, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Actualiza la app", color = OfflineInk, fontWeight = FontWeight.Black, fontSize = 15.sp)
            Text(
                if (count == 1) {
                    "Hay 1 pedido que esta versión no puede mostrar. Actualiza desde la tienda para verlo."
                } else {
                    "Hay $count pedidos que esta versión no puede mostrar. Actualiza desde la tienda para verlos."
                },
                color = OfflineInk,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/**
 * Pedidos que Mesero o Caja tomó sin internet: cuántos faltan por mandar y los que el servidor no aceptó, para
 * resolverlos a mano. Se ve mientras quede alguno, con o sin red.
 */
@Composable
internal fun OfflineOrdersCard(
    state: OfflineStaffOrdersState,
    onRetry: () -> Unit,
    onDismiss: (OfflineStaffOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.count == 0) return
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(OfflineYolk.copy(alpha = 0.35f))
                .semantics { liveRegion = LiveRegionMode.Polite }
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                val pending = state.pending.size
                Text(
                    when {
                        state.isSending -> "Mandando pedidos guardados…"
                        pending == 1 -> "1 pedido guardado por mandar"
                        pending > 1 -> "$pending pedidos guardados por mandar"
                        else -> "Pedidos guardados con problema"
                    },
                    color = OfflineInk,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                )
                if (pending > 0) {
                    Text(
                        state.pending.joinToString(" · ") { "${it.localFolio} ${it.placeLabel}" },
                        color = OfflineInk,
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                    )
                }
            }
            if (!state.isSending) {
                TextButton(onClick = onRetry) { Text("Reintentar", color = OfflineInk, fontWeight = FontWeight.Bold) }
            }
        }
        state.problems.forEach { order ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${order.localFolio} · ${order.placeLabel}",
                        color = OfflineInk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                    )
                    Text(order.summary, color = OfflineInk, fontSize = 12.5.sp, lineHeight = 17.sp)
                    Text(
                        order.problem.orEmpty(),
                        color = OfflineInk,
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                TextButton(onClick = { onDismiss(order) }) {
                    Text("Ya lo resolví", color = OfflineInk, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
