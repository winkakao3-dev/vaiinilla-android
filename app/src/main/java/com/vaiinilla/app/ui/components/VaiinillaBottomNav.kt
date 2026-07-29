package com.vaiinilla.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.theme.Coral
import com.vaiinilla.app.ui.theme.Ink
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

private val NavOuterShape = RoundedCornerShape(24.dp)
private val NavTabShape = RoundedCornerShape(18.dp)
private val NavIdle = Color(0xFFA8AAA3)
private val NavShadow = Color(0x42000000)
private val BounceEase = CubicBezierEasing(0.2f, 0.9f, 0.25f, 1f)

enum class StudentTab {
    MENU,
    ASSISTANT,
    ORDERS,
    WALLET,
    CART,
}

@Composable
fun VaiinillaBottomNav(
    activeTab: StudentTab,
    cartCount: Int,
    onMenu: () -> Unit,
    onAssistant: () -> Unit,
    onOrders: () -> Unit,
    onWallet: () -> Unit,
    onCart: () -> Unit,
    modifier: Modifier = Modifier,
    showDemoTabs: Boolean = false,
) {
    val tabs =
        listOf(
            NavTab(StudentTab.MENU, "Menú", Icons.Outlined.Home, onMenu),
            NavTab(StudentTab.ASSISTANT, "Asistente", Icons.Outlined.AutoAwesome, onAssistant),
            NavTab(StudentTab.ORDERS, "Pedidos", Icons.AutoMirrored.Outlined.ReceiptLong, onOrders),
            NavTab(StudentTab.WALLET, "Cartera", Icons.Outlined.AccountBalanceWallet, onWallet),
            NavTab(StudentTab.CART, "Carrito", Icons.Outlined.ShoppingCart, onCart),
        )

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, bottom = 15.dp)
                .height(68.dp)
                .shadow(elevation = 15.dp, shape = NavOuterShape, ambientColor = NavShadow, spotColor = NavShadow)
                .clip(NavOuterShape)
                .background(Ink)
                .padding(7.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEach { entry ->
            NavItem(
                modifier = Modifier.weight(1f),
                label = entry.label,
                icon = entry.icon,
                active = entry.tab == activeTab,
                badge = if (entry.tab == StudentTab.CART) cartCount else 0,
                bounceKey = if (entry.tab == activeTab) entry.tab.ordinal else -1,
                onClick = entry.onClick,
            )
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: Int = 0,
    bounceKey: Int = -1,
) {
    val colors = LocalVaiinillaColors.current
    val iconBounce = remember { Animatable(0f) }
    var lastBounceKey by remember { mutableIntStateOf(bounceKey) }

    LaunchedEffect(bounceKey) {
        if (bounceKey >= 0 && bounceKey != lastBounceKey) {
            lastBounceKey = bounceKey
            iconBounce.snapTo(0f)
            iconBounce.animateTo(1f, tween(430, easing = BounceEase))
        }
    }

    val iconScale =
        when {
            iconBounce.value <= 0f -> 1f
            iconBounce.value < 0.28f -> 1f - 0.16f * (iconBounce.value / 0.28f)
            iconBounce.value < 0.58f -> 0.84f + 0.29f * ((iconBounce.value - 0.28f) / 0.3f)
            iconBounce.value < 0.78f -> 1.13f - 0.16f * ((iconBounce.value - 0.58f) / 0.2f)
            else -> 1f
        }

    val foreground = if (active) colors.accentInk else NavIdle
    val tabBackground = if (active) colors.accent else Color.Transparent

    Box(
        modifier =
            modifier
                .fillMaxHeight()
                .clip(NavTabShape)
                .background(tabBackground)
                .physicalPress(scale = PhysicalPressScale.Nav, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        ) {
            Box {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = foreground,
                    modifier =
                        Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            },
                )
                if (badge > 0) {
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 6.dp, y = (-8).dp)
                                .height(17.dp)
                                .width(if (badge > 9) 22.dp else 17.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(Coral)
                                .border(2.dp, Ink, RoundedCornerShape(99.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = badge.coerceAtMost(99).toString(),
                            color = Color(0xFF28100D),
                            fontSize = 9.sp,
                            lineHeight = 9.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
            Text(
                text = label,
                color = foreground,
                fontSize = 9.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

private data class NavTab(
    val tab: StudentTab,
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)
