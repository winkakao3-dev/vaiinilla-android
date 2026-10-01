package com.vaiinilla.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ARRIVAL_HOUR = DateTimeFormatter.ofPattern("HH:mm")

/** Un pedido para llevar sigue vivo: ya se puede avisar que llegaste (hasta que se entregue). */
fun OrderDetail.canAnnounceArrival(driveThru: Boolean): Boolean =
    driveThru &&
        summary.destination == OrderDestination.TAKE_AWAY &&
        summary.state in
        setOf(OrderState.PENDING_PAYMENT, OrderState.PAID, OrderState.PREPARING, OrderState.READY)

/**
 * Drive-thru: "Ya llegué" avisa a Cocina y Caja para que preparen o entreguen justo ahora. Una vez
 * avisado, el botón se cambia por la hora a la que avisaste.
 */
@Composable
fun ArrivalNotice(
    order: OrderDetail,
    enabled: Boolean,
    onAnnounce: () -> Unit,
    modifier: Modifier = Modifier,
    /** En la tarjeta oscura de "En curso" el texto va claro; en la pantalla del pedido, oscuro. */
    onDark: Boolean = false,
) {
    val colors = LocalVaiinillaColors.current
    val textColor = if (onDark) Color(0xFFF5F2E8) else colors.ink
    val hintColor = if (onDark) Color(0xFFF5F2E8).copy(alpha = 0.62f) else colors.muted
    val arrivedAt = order.arrivedAt
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (arrivedAt == null) {
            EditorialAccentButton(
                text = "Ya llegué",
                onClick = onAnnounce,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Avisa a la cocina y la caja que ya estás aquí.",
                color = hintColor,
                fontSize = 12.sp,
            )
        } else {
            Text(
                "Avisaste que llegaste a las ${arrivedAt.atZone(
                    ZoneId.systemDefault(),
                ).format(ARRIVAL_HOUR)}. La cocina y la caja ya lo saben.",
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.accent.copy(alpha = 0.22f))
                        .padding(12.dp),
            )
        }
    }
}
