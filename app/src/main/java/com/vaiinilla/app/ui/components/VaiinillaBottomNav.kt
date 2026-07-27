package com.vaiinilla.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
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
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.NavBorder
import com.vaiinilla.app.ui.theme.NavGlass
import com.vaiinilla.app.ui.theme.NavInsetHighlight
import com.vaiinilla.app.ui.theme.NavPill
import com.vaiinilla.app.ui.theme.NavShadow
import com.vaiinilla.app.ui.theme.NavTextActive
import com.vaiinilla.app.ui.theme.NavTextIdle

private val NavShape = RoundedCornerShape(44.dp)
private val PillShape = RoundedCornerShape(999.dp)
private val NavEase = CubicBezierEasing(0.22f, 0.8f, 0.25f, 1f)
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
        buildList {
            add(NavTab(StudentTab.MENU, "Menú", Icons.Outlined.Home, onMenu))
            if (showDemoTabs) {
                add(NavTab(StudentTab.ASSISTANT, "Asistente", Icons.Outlined.AutoAwesome, onAssistant))
            }
            add(NavTab(StudentTab.ORDERS, "Pedidos", Icons.Outlined.ReceiptLong, onOrders))
            if (showDemoTabs) {
                add(NavTab(StudentTab.WALLET, "Cartera", Icons.Outlined.AccountBalanceWallet, onWallet))
            }
            add(NavTab(StudentTab.CART, "Carrito", Icons.Outlined.ShoppingCart, onCart))
        }
    val activeIndex = tabs.indexOfFirst { it.tab == activeTab }.coerceAtLeast(0)
    val pillOffset by animateFloatAsState(
        targetValue = activeIndex.toFloat(),
        animationSpec = tween(durationMillis = 340, easing = NavEase),
        label = "nav-pill-offset",
    )

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .shadow(
                        elevation = 18.dp,
                        shape = NavShape,
                        clip = false,
                        ambientColor = NavShadow,
                        spotColor = NavShadow,
                    ).clip(NavShape)
                    .background(NavGlass)
                    .border(1.dp, NavBorder, NavShape)
                    .padding(9.dp),
        ) {
            val tabWidth = maxWidth / tabs.size
            Box(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .width(tabWidth)
                        .offset(x = tabWidth * pillOffset)
                        .clip(PillShape)
                        .background(NavPill)
                        .border(1.dp, NavInsetHighlight, PillShape),
            )

            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(NavInsetHighlight),
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                tabs.forEach { entry ->
                    NavItem(
                        modifier = Modifier.weight(1f),
                        label = entry.label,
                        icon = entry.icon,
                        active = entry.tab == activeTab,
                        badge = if (entry.tab == StudentTab.CART) cartCount else 0,
                        bounceKey = if (entry.tab == activeTab) activeIndex else -1,
                        onClick = entry.onClick,
                    )
                }
            }
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
    val labelBounce = remember { Animatable(0f) }
    var lastBounceKey by remember { mutableIntStateOf(bounceKey) }

    LaunchedEffect(bounceKey) {
        if (bounceKey >= 0 && bounceKey != lastBounceKey) {
            lastBounceKey = bounceKey
            iconBounce.snapTo(0f)
            labelBounce.snapTo(0f)
            iconBounce.animateTo(1f, tween(430, easing = BounceEase))
            labelBounce.animateTo(1f, tween(390, easing = BounceEase))
        }
    }

    val iconOffsetY =
        when {
            iconBounce.value <= 0f -> 0f
            iconBounce.value < 0.28f -> 2f * (iconBounce.value / 0.28f)
            iconBounce.value < 0.58f -> 2f - 6f * ((iconBounce.value - 0.28f) / 0.3f)
            iconBounce.value < 0.78f -> -4f + 5f * ((iconBounce.value - 0.58f) / 0.2f)
            else -> 0f
        }
    val iconScale =
        when {
            iconBounce.value <= 0f -> 1f
            iconBounce.value < 0.28f -> 1f - 0.16f * (iconBounce.value / 0.28f)
            iconBounce.value < 0.58f -> 0.84f + 0.29f * ((iconBounce.value - 0.28f) / 0.3f)
            iconBounce.value < 0.78f -> 1.13f - 0.16f * ((iconBounce.value - 0.58f) / 0.2f)
            else -> 1f
        }

    val foreground = if (active) NavTextActive else NavTextIdle

    Box(
        modifier =
            modifier
                .fillMaxHeight()
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
                            .size(27.dp)
                            .graphicsLayer {
                                translationY = iconOffsetY
                                scaleX = iconScale
                                scaleY = iconScale
                                transformOrigin =
                                    androidx.compose.ui.graphics
                                        .TransformOrigin(0.5f, 0.72f)
                            },
                )
                if (badge > 0) {
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-2).dp)
                                .height(15.dp)
                                .width(if (badge > 9) 20.dp else 15.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(Coral)
                                .border(2.dp, colors.ink, RoundedCornerShape(99.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = badge.coerceAtMost(99).toString(),
                            color = Color(0xFF21100D),
                            fontSize = 8.sp,
                            lineHeight = 8.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
            Text(
                text = label,
                color = foreground,
                fontSize = 14.sp,
                lineHeight = 15.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = (-0.35).sp,
                modifier =
                    Modifier.graphicsLayer {
                        translationY =
                            when {
                                labelBounce.value <= 0f -> 0f
                                labelBounce.value < 0.45f -> 1.5f * (labelBounce.value / 0.45f)
                                labelBounce.value < 0.72f -> 1.5f - 2.5f * ((labelBounce.value - 0.45f) / 0.27f)
                                else -> 0f
                            }
                    },
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
