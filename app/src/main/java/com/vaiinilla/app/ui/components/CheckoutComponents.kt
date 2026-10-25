package com.vaiinilla.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.SpaceCopy
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

@Composable
fun CheckoutDestinationPicker(
    selected: OrderDestination,
    selectedSpaceName: String,
    onSelect: (OrderDestination) -> Unit,
    modifier: Modifier = Modifier,
    showInSpace: Boolean = true,
    spaceType: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DestinationOption(
            title = "Para llevar",
            subtitle = "Recoge en barra",
            detail = null,
            icon = Icons.Outlined.ShoppingBag,
            selected = selected == OrderDestination.TAKE_AWAY,
            onClick = { onSelect(OrderDestination.TAKE_AWAY) },
            modifier = Modifier.weight(1f),
        )
        if (showInSpace) {
            DestinationOption(
                title = SpaceCopy.destinationTitle(spaceType),
                subtitle = SpaceCopy.destinationSubtitle(spaceType),
                detail = selectedSpaceName.ifBlank { "Dentro del establecimiento" },
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
    spaceType: String? = null,
) {
    val colors = LocalVaiinillaColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            SpaceCopy.pickPrompt(spaceType),
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
                val chipBackground =
                    animateSelectionColor(if (selected) colors.ink else colors.paper2, "space-chip-background")
                val chipContent =
                    animateSelectionColor(if (selected) colors.onInk else colors.ink, "space-chip-content")
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
                    color = chipBackground,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        space.name,
                        color = chipContent,
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
    detail: String?,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = LocalHapticFeedback.current
    val selectedBackground = if (colors.isDark) colors.paper2 else colors.ink
    val selectedForeground = if (colors.isDark) colors.ink else colors.onInk
    val background =
        animateSelectionColor(if (selected) selectedBackground else colors.paper2, "destination-background")
    val foreground = animateSelectionColor(if (selected) selectedForeground else colors.ink, "destination-foreground")
    val secondary =
        animateSelectionColor(
            if (selected) selectedForeground.copy(alpha = 0.78f) else colors.muted,
            "destination-secondary",
        )
    // De noche la tarjeta elegida conserva el fondo de las demás: la marca es el contorno en tinta.
    val borderColor = animateSelectionColor(if (selected) colors.ink else colors.line, "destination-border")
    val iconTile =
        animateSelectionColor(
            if (selected) selectedForeground.copy(alpha = 0.10f) else colors.paper,
            "destination-icon-tile",
        )
    val borderWidth by animateDpAsState(if (selected) 1.5.dp else 1.dp, label = "destination-border-width")
    Surface(
        modifier =
            modifier
                .clip(RoundedCornerShape(24.dp))
                .clickable(
                    onClick = {
                        if (!selected) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    },
                ).heightIn(min = 154.dp)
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(24.dp),
                ).semantics {
                    role = Role.RadioButton
                    this.selected = selected
                },
        color = background,
        shape = RoundedCornerShape(24.dp),
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            AnimatedVisibility(
                visible = selected,
                modifier = Modifier.align(Alignment.TopEnd),
                enter =
                    fadeIn(spring(stiffness = 500f)) +
                        scaleIn(spring(dampingRatio = 0.6f, stiffness = 500f), initialScale = 0.4f),
                exit = fadeOut(spring(stiffness = 600f)) + scaleOut(spring(stiffness = 600f), targetScale = 0.6f),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(selectedForeground),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = selectedBackground,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
            Column {
                Box(
                    modifier =
                        Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(iconTile),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(24.dp))
                }
                Text(
                    title,
                    color = foreground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(top = 14.dp),
                )
                Text(
                    subtitle,
                    color = secondary,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
                detail?.takeIf(String::isNotBlank)?.let { detailText ->
                    Text(
                        detailText,
                        color = secondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
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
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PaymentOption(
            icon = Icons.Outlined.Payments,
            title = "Efectivo",
            selected = selected == PaymentMethod.CASH,
            onClick = { onSelect(PaymentMethod.CASH) },
            modifier = Modifier.weight(1f),
        )
        PaymentOption(
            icon = Icons.Outlined.AccountBalanceWallet,
            title = "Saldo",
            selected = selected == PaymentMethod.BALANCE,
            onClick = { onSelect(PaymentMethod.BALANCE) },
            modifier = Modifier.weight(1f),
        )
        PaymentOption(
            icon = Icons.Outlined.CreditCard,
            title = "Tarjeta",
            selected = selected == PaymentMethod.STRIPE,
            onClick = { onSelect(PaymentMethod.STRIPE) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PaymentOption(
    icon: ImageVector,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = LocalHapticFeedback.current
    val background = animateSelectionColor(if (selected) colors.ink else colors.paper2, "payment-background")
    val foreground =
        animateSelectionColor(
            when {
                selected -> colors.onInk
                !enabled -> colors.muted
                else -> colors.ink
            },
            "payment-foreground",
        )
    val borderColor =
        animateSelectionColor(
            if (selected) colors.ink else colors.line,
            "payment-border",
        )
    Surface(
        modifier =
            modifier
                .heightIn(min = 74.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable(
                    enabled = enabled,
                    onClick = {
                        if (!selected) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    },
                ).border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(20.dp),
                ).semantics {
                    role = Role.RadioButton
                    this.selected = selected
                    if (!enabled) disabled()
                },
        color = background,
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.size(21.dp),
            )
            Text(
                title,
                color = foreground,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                maxLines = 1,
            )
        }
    }
}
