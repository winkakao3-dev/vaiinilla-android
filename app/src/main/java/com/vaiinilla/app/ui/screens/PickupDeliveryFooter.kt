package com.vaiinilla.app.ui.screens

import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.minTouchTarget
import com.vaiinilla.app.ui.components.PhysicalPressScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.SpaceCopy
import com.vaiinilla.app.domain.model.activeItems
import com.vaiinilla.app.domain.model.needsCashCollection

/**
 * La tarjeta de C08 bajo el visor: a quién se entrega y qué lleva, para que el cajero lo compare con la bolsa
 * mientras el cliente acerca su QR.
 */
@Composable
internal fun PickupDeliveryFooter(
    order: OrderDetail,
    bottomInsets: WindowInsets = WindowInsets(0),
    /** La cámara no lee el QR: cierra el visor y abre la búsqueda de Caja (Doop C08). */
    onCantScan: (() -> Unit)? = null,
) {
    val colors = rememberOperationalColors()
    val summary = order.summary
    val paid = !summary.needsCashCollection
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(colors.background)
                .windowInsetsPadding(bottomInsets)
                .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(colors.accentLime),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, tint = colors.accentInk)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "PEDIDO #${summary.folio} · ${
                        SpaceCopy.staffDestination(summary.destination, summary.space, summary.pickUpAtBar).uppercase()
                    }",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    staffCustomerName(order) ?: "Cliente",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val tint = if (paid) colors.accentLime else Color(0xFFFF7A6E)
            Text(
                if (paid) "PAGADO" else "FALTA PAGAR",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = tint,
                modifier =
                    Modifier
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.cardBackground)
                    .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            order.activeItems.take(5).forEach { item ->
                val options = item.options.joinToString(" · ") { it.name }
                Text(
                    "${item.quantity} × ${item.productName}" + if (options.isNotEmpty()) " · $options" else "",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (order.activeItems.size > 5) {
                Text("y ${order.activeItems.size - 5} más", fontSize = 13.sp, color = colors.textSecondary)
            }
        }
        if (onCantScan != null) {
            Text(
                "¿No lee? Búscalo por nombre o folio",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .minTouchTarget()
                        .physicalPress(scale = PhysicalPressScale.Small, onClick = onCantScan)
                        .semantics { role = Role.Button },
            )
        }
    }
}