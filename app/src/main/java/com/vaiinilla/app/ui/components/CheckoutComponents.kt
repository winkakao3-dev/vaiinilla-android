package com.vaiinilla.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.DemoCheckoutFixtures
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.Yolk

@Composable
fun OrderStateTrackingHero(
    state: OrderState,
    destination: OrderDestination,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val content = trackingHeroContent(state, destination)
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = content.background,
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = content.eyebrow,
                color = content.text.copy(alpha = 0.65f),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp,
            )
            Text(
                text = content.title,
                color = content.text,
                fontSize = 26.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = content.message,
                color = content.text.copy(alpha = 0.82f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (content.badge != null) {
                Surface(
                    color = Color.White.copy(alpha = 0.42f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(top = 14.dp),
                ) {
                    Text(
                        text = content.badge,
                        color = colors.ink,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

private data class TrackingHeroContent(
    val eyebrow: String,
    val title: String,
    val message: String,
    val badge: String?,
    val background: Color,
    val text: Color,
)

private fun trackingHeroContent(state: OrderState, destination: OrderDestination): TrackingHeroContent {
    val darkBg = Color(0xFF1C1D1B)
    val darkText = Color(0xFFF5F2E8)
    return when (state) {
        OrderState.PENDING_PAYMENT -> TrackingHeroContent(
            eyebrow = "POR COBRAR",
            title = "Pasa a Caja",
            message = "Tu pedido está listo para pagarse en efectivo. Muéstrale el folio al cajero.",
            badge = "EFECTIVO",
            background = darkBg,
            text = darkText,
        )
        OrderState.PAID -> TrackingHeroContent(
            eyebrow = "COBRADO",
            title = "Cocina recibió tu comanda",
            message = "El pago quedó confirmado. En un momento empezarán a preparar tu pedido.",
            badge = "21 · COBRADO",
            background = darkBg,
            text = darkText,
        )
        OrderState.PREPARING -> TrackingHeroContent(
            eyebrow = "PREPARANDO",
            title = "Tu comida se está cocinando",
            message = "La cocina ya está trabajando en tu pedido. Te avisamos cuando esté listo.",
            badge = "22 · PREPARANDO",
            background = Color(0xFF2A2418),
            text = Yolk,
        )
        OrderState.READY -> {
            val pickup = if (destination == OrderDestination.IN_SPACE) {
                "Tu pedido va en camino a ${DemoCheckoutFixtures.SPACE_NAME}."
            } else {
                "Recógelo en la barra cuando veas este estado."
            }
            TrackingHeroContent(
                eyebrow = "LISTO",
                title = if (destination == OrderDestination.IN_SPACE) "En camino a tu mesa" else "Listo para recoger",
                message = pickup,
                badge = "23 · LISTO",
                background = Color(0xFF1D250C),
                text = Color(0xFFD7EF8B),
            )
        }
        OrderState.DELIVERED -> TrackingHeroContent(
            eyebrow = "ENTREGADO",
            title = "¡Buen provecho!",
            message = "Tu pedido fue entregado. Gracias por usar Vaiinilla.",
            badge = "24 · ENTREGADO",
            background = darkBg,
            text = darkText,
        )
    }
}

@Composable
fun CheckoutDestinationPicker(
    selected: OrderDestination,
    onSelect: (OrderDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DestinationOption(
            title = "Para llevar",
            subtitle = "Recoges en barra",
            icon = Icons.Outlined.ShoppingBag,
            selected = selected == OrderDestination.TAKE_AWAY,
            onClick = { onSelect(OrderDestination.TAKE_AWAY) },
            modifier = Modifier.weight(1f),
        )
        DestinationOption(
            title = "En mesa",
            subtitle = DemoCheckoutFixtures.SPACE_NAME,
            icon = Icons.Outlined.Restaurant,
            selected = selected == OrderDestination.IN_SPACE,
            onClick = { onSelect(OrderDestination.IN_SPACE) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DestinationOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val bg = if (selected) colors.ink else colors.paper2
    val fg = if (selected) colors.paper else colors.ink
    val muted = if (selected) colors.paper.copy(alpha = 0.72f) else colors.muted
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = bg,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
            Text(title, color = fg, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 10.dp))
            Text(subtitle, color = muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            if (selected) {
                Text(
                    "SELECCIONADO",
                    color = colors.accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
fun CheckoutPaymentPicker(
    selected: PaymentMethod,
    walletBalance: Int,
    onSelect: (PaymentMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PaymentOption(
            title = "Efectivo en Caja",
            subtitle = "Paga en mostrador y luego se prepara",
            selected = selected == PaymentMethod.CASH,
            onClick = { onSelect(PaymentMethod.CASH) },
        )
        PaymentOption(
            title = "Saldo Vaiinilla",
            subtitle = "Disponible: $$walletBalance",
            selected = selected == PaymentMethod.BALANCE,
            onClick = { onSelect(PaymentMethod.BALANCE) },
        )
        PaymentOption(
            title = "Tarjeta •••• 4242",
            subtitle = "Cargo inmediato en la demo",
            selected = selected == PaymentMethod.CARD,
            onClick = { onSelect(PaymentMethod.CARD) },
        )
    }
}

@Composable
private fun PaymentOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .then(
                if (selected) {
                    Modifier.border(2.dp, colors.accent, RoundedCornerShape(20.dp))
                } else {
                    Modifier
                },
            ),
        color = colors.paper2,
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = colors.ink, fontWeight = FontWeight.Black)
                Text(subtitle, color = colors.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
            }
            if (selected) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
