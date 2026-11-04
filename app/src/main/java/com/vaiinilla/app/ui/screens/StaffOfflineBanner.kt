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
import com.vaiinilla.app.ui.components.reducedMotion

private val OfflineYolk = Color(0xFFFFD15B)
private val OfflineInk = Color(0xFF3B2A00)

/**
 * N12: sin internet, una franja amarilla lo dice arriba en Caja, Cocina y Mesero. La app todavía no guarda lo que
 * se toca para mandarlo después, así que la franja no lo promete: pide revisar que se guardó cuando vuelva la red.
 */
@Composable
internal fun StaffOfflineBanner(
    offline: Boolean,
    modifier: Modifier = Modifier,
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
                    "Lo que ves puede estar viejo y lo que toques puede no llegar. Cuando vuelva la red, revisa que se haya guardado.",
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
