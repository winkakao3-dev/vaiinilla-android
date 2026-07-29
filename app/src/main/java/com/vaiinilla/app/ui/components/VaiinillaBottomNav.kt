package com.vaiinilla.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.theme.Coral
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

/** Uber navbar replica easing — references/examples/uber-navbar-replica.html */
private val UberNavEase = CubicBezierEasing(0.22f, 0.8f, 0.25f, 1f)

private val NavDockHeight = 84.dp
private val NavMaxWidth = 568.dp
private val NavDockBottom = 22.dp
private val NavDockHorizontal = 16.dp
private val NavInnerPaddingVertical = 10.dp
private val NavInnerPaddingHorizontal = 14.dp
private val NavIconBandHeight = 44.dp
private val NavIconBubbleSize = 44.dp
private val NavIconSize = 26.dp
private val NavIconLabelGap = 5.dp
private val NavLabelSize = 13.sp

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
    val colors = LocalVaiinillaColors.current
    val context = LocalContext.current
    val reducedMotion =
        remember {
            runCatching {
                android.provider.Settings.Global.getFloat(
                    context.contentResolver,
                    android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE,
                ) == 0f
            }.getOrDefault(false)
        }

    val tabs =
        listOf(
            NavTab(StudentTab.MENU, "Menú", Icons.Outlined.Home, onMenu),
            NavTab(StudentTab.ASSISTANT, "Asistente", Icons.Outlined.AutoAwesome, onAssistant),
            NavTab(StudentTab.ORDERS, "Pedidos", Icons.AutoMirrored.Outlined.ReceiptLong, onOrders),
            NavTab(StudentTab.WALLET, "Cartera", Icons.Outlined.AccountBalanceWallet, onWallet),
            NavTab(StudentTab.CART, "Carrito", Icons.Outlined.ShoppingCart, onCart),
        )

    val activeIndex = tabs.indexOfFirst { it.tab == activeTab }.coerceAtLeast(0)
    val bubbleIndexAnim = remember { Animatable(activeIndex.toFloat()) }
    val outerRadius = NavDockHeight / 2

    LaunchedEffect(activeIndex, reducedMotion) {
        if (reducedMotion) {
            bubbleIndexAnim.snapTo(activeIndex.toFloat())
        } else {
            bubbleIndexAnim.animateTo(
                activeIndex.toFloat(),
                animationSpec = tween(durationMillis = 340, easing = UberNavEase),
            )
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = NavDockHorizontal,
                    end = NavDockHorizontal,
                    bottom = NavDockBottom,
                ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val outerShape = RoundedCornerShape(outerRadius)
        Column(
            modifier =
                Modifier
                    .widthIn(max = NavMaxWidth)
                    .fillMaxWidth()
                    .height(NavDockHeight)
                    .floatingNavShadow(outerShape, colors.navShadow)
                    .clip(outerShape)
                    .background(colors.navGlass)
                    .border(1.dp, colors.navBorder, outerShape)
                    .padding(
                        horizontal = NavInnerPaddingHorizontal,
                        vertical = NavInnerPaddingVertical,
                    ),
        ) {
            BoxWithConstraints(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(NavIconBandHeight),
            ) {
                val tabWidth = maxWidth / tabs.size
                val bubbleCenter = tabWidth * (bubbleIndexAnim.value + 0.5f)
                val bubbleOffset = bubbleCenter - NavIconBubbleSize / 2

                Box(
                    modifier =
                        Modifier
                            .offset(x = bubbleOffset)
                            .size(NavIconBubbleSize)
                            .clip(CircleShape)
                            .background(colors.navPill),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colors.navInsetHighlight),
                    )
                }

                Row(modifier = Modifier.fillMaxSize()) {
                    tabs.forEach { entry ->
                        NavIconSlot(
                            modifier = Modifier.weight(1f),
                            label = entry.label,
                            icon = entry.icon,
                            active = entry.tab == activeTab,
                            badge = if (entry.tab == StudentTab.CART) cartCount else 0,
                            badgeBorder = colors.navGlass,
                            onClick = entry.onClick,
                        )
                    }
                }
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = NavIconLabelGap),
            ) {
                tabs.forEach { entry ->
                    NavLabelSlot(
                        modifier = Modifier.weight(1f),
                        label = entry.label,
                        active = entry.tab == activeTab,
                        onClick = entry.onClick,
                    )
                }
            }
        }
    }
}

private fun Modifier.floatingNavShadow(
    shape: RoundedCornerShape,
    shadowColor: Color,
): Modifier =
    this
        .shadow(
            elevation = 24.dp,
            shape = shape,
            clip = false,
            ambientColor = shadowColor.copy(alpha = shadowColor.alpha * 0.55f),
            spotColor = shadowColor.copy(alpha = shadowColor.alpha * 0.85f),
        )
        .shadow(
            elevation = 8.dp,
            shape = shape,
            clip = false,
            ambientColor = shadowColor.copy(alpha = shadowColor.alpha * 0.2f),
            spotColor = shadowColor.copy(alpha = shadowColor.alpha * 0.35f),
        )

@Composable
private fun NavIconSlot(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: Int = 0,
    badgeBorder: Color,
) {
    val colors = LocalVaiinillaColors.current
    val foreground = if (active) colors.navTextActive else colors.navTextIdle

    Box(
        modifier =
            modifier
                .height(NavIconBandHeight)
                .physicalPress(scale = PhysicalPressScale.Nav, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = foreground,
                modifier = Modifier.size(NavIconSize),
            )
            if (badge > 0) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-8).dp)
                            .height(16.dp)
                            .width(if (badge > 9) 20.dp else 16.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Coral)
                            .border(2.dp, badgeBorder, RoundedCornerShape(99.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = badge.coerceAtMost(99).toString(),
                        color = Color(0xFF28100D),
                        fontSize = 8.sp,
                        lineHeight = 8.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

@Composable
private fun NavLabelSlot(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val foreground = if (active) colors.navTextActive else colors.navTextIdle

    Box(
        modifier =
            modifier
                .physicalPress(scale = PhysicalPressScale.Nav, onClick = onClick),
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            text = label,
            color = foreground,
            fontSize = NavLabelSize,
            lineHeight = 14.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private data class NavTab(
    val tab: StudentTab,
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)
