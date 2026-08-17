package com.vaiinilla.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.Yolk

@Composable
fun OrderStateTrackingHero(
    state: OrderState,
    destination: OrderDestination,
    spaceName: String? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val content = trackingHeroContent(state, destination, spaceName)
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

private fun trackingHeroContent(
    state: OrderState,
    destination: OrderDestination,
    spaceName: String?,
): TrackingHeroContent {
    val darkBg = Color(0xFF1C1D1B)
    val darkText = Color(0xFFF5F2E8)
    return when (state) {
        OrderState.PENDING_PAYMENT ->
            TrackingHeroContent(
                eyebrow = "POR COBRAR",
                title = "Pasa a Caja",
                message = "Tu pedido está listo para pagarse en efectivo. Muéstrale el folio al cajero.",
                badge = "EFECTIVO",
                background = darkBg,
                text = darkText,
            )
        OrderState.PAID ->
            TrackingHeroContent(
                eyebrow = "COBRADO",
                title = "Cocina recibió tu comanda",
                message = "El pago quedó confirmado. En un momento empezarán a preparar tu pedido.",
                badge = "21 · COBRADO",
                background = darkBg,
                text = darkText,
            )
        OrderState.PREPARING ->
            TrackingHeroContent(
                eyebrow = "PREPARANDO",
                title = "Tu comida se está cocinando",
                message = "La cocina ya está trabajando en tu pedido. Te avisamos cuando esté listo.",
                badge = "22 · PREPARANDO",
                background = Color(0xFF2A2418),
                text = Yolk,
            )
        OrderState.READY -> {
            val pickup =
                if (destination == OrderDestination.IN_SPACE) {
                    "Tu pedido va en camino a ${spaceName ?: "tu mesa"}."
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
        OrderState.DELIVERED ->
            TrackingHeroContent(
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
    selectedSpaceName: String,
    onSelect: (OrderDestination) -> Unit,
    showInSpace: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DestinationOption(
            title = "Para llevar",
            subtitle = "Recoge tu pedido en barra.",
            icon = Icons.Outlined.ShoppingBag,
            selected = selected == OrderDestination.TAKE_AWAY,
            onClick = { onSelect(OrderDestination.TAKE_AWAY) },
            modifier = Modifier.weight(1f),
        )
        if (showInSpace) {
            DestinationOption(
                title = "En mesa",
                subtitle = "Te lo llevamos cuando esté listo.",
                icon = Icons.Outlined.Restaurant,
                selected = selected == OrderDestination.IN_SPACE,
                onClick = { onSelect(OrderDestination.IN_SPACE) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

data class CheckoutSpaceOption(
    val id: Int,
    val name: String,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckoutSpacePicker(
    selectedSpaceId: Int,
    spaces: List<CheckoutSpaceOption>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "Selecciona tu mesa",
            color = colors.ink,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
        FlowRow(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            spaces.forEach { space ->
                val selected = space.id == selectedSpaceId
                val haptics = LocalHapticFeedback.current
                Surface(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                if (!selected) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(space.id)
                            }.semantics {
                                role = Role.RadioButton
                                this.selected = selected
                            },
                    color = if (selected) colors.ink else colors.paper2,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        space.name,
                        color = if (selected) colors.paper else colors.ink,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        modifier =
                            Modifier
                                .heightIn(min = 48.dp)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
        }
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
    val haptics = LocalHapticFeedback.current
    val bg = if (selected) colors.ink else colors.paper2
    val fg = if (selected) colors.paper else colors.ink
    val muted = if (selected) colors.paper.copy(alpha = 0.72f) else colors.muted
    Surface(
        modifier =
            modifier
                .clip(RoundedCornerShape(24.dp))
                .clickable(
                    onClick = {
                        if (!selected) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    },
                ).heightIn(min = 148.dp)
                .semantics {
                    role = Role.RadioButton
                    this.selected = selected
                },
        color = bg,
        shape = RoundedCornerShape(24.dp),
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            if (selected) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(colors.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = colors.accentInk,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Column {
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(25.dp))
                Text(
                    title,
                    color = fg,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    lineHeight = 28.sp,
                    modifier = Modifier.padding(top = 22.dp),
                )
                Text(
                    subtitle,
                    color = muted,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
fun CheckoutPaymentPicker(
    selected: PaymentMethod,
    onSelect: (PaymentMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PaymentOption(
            icon = Icons.Outlined.Payments,
            title = "Efectivo en caja",
            subtitle = "Se envía a cocina después del cobro.",
            selected = selected == PaymentMethod.CASH,
            onClick = { onSelect(PaymentMethod.CASH) },
        )
        PaymentOption(
            icon = Icons.Outlined.AccountBalanceWallet,
            title = "Saldo Vaiinilla",
            subtitle = "Tu saldo en este establecimiento.",
            selected = selected == PaymentMethod.BALANCE,
            onClick = { onSelect(PaymentMethod.BALANCE) },
        )
    }
}

@Composable
private fun PaymentOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = LocalHapticFeedback.current
    val background = if (selected) colors.accent else colors.paper2
    val foreground =
        when {
            selected -> colors.accentInk
            !enabled -> colors.muted
            else -> colors.ink
        }
    val secondaryForeground =
        if (selected) {
            colors.accentInk.copy(alpha = 0.72f)
        } else {
            colors.muted
        }
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .clickable(
                    enabled = enabled,
                    onClick = {
                        if (!selected) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    },
                ).semantics {
                    role = Role.RadioButton
                    this.selected = selected
                    if (!enabled) disabled()
                },
        color = background,
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.ink.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = foreground,
                    modifier = Modifier.size(23.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = foreground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    color = secondaryForeground,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            Box(
                modifier =
                    Modifier
                        .size(22.dp)
                        .border(
                            width = 2.dp,
                            color = if (selected) colors.ink.copy(alpha = 0.35f) else colors.ink.copy(alpha = 0.18f),
                            shape = CircleShape,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Box(
                        modifier =
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(colors.ink),
                    )
                }
            }
        }
    }
}
