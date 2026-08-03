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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.theme.Coral
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

/** Uber navbar replica easing — references/examples/uber-navbar-replica.html */
private val UberNavEase = CubicBezierEasing(0.22f, 0.8f, 0.25f, 1f)

private val NavDockHeight = 78.dp
private val NavMaxWidth = 420.dp
private val NavDockGapAboveSafeArea = 20.dp
private val NavDockHorizontalMargin = 28.dp
private val NavInnerPadding = 8.dp
private val NavIconCapsuleWidth = 56.dp
private val NavIconCapsuleHeight = 36.dp
private val NavIconCapsuleShape = RoundedCornerShape(percent = 50)
private val NavIconSize = 24.dp
private val NavLabelSize = 11.sp
private val NavIconLabelGap = 3.dp

/** Content clearance: dock + float gap + breathing room (excludes system inset). */
val VaiinillaBottomNavClearance: Dp = NavDockHeight + NavDockGapAboveSafeArea + 16.dp

enum class StudentTab {
    MENU,
    ASSISTANT,
    ORDERS,
    WALLET,
    CART,
}

/**
 * Floating capsule dock — inset from all screen edges, content peeks around it.
 * Colors from [LocalVaiinillaColors] only (theme-independent structure).
 */
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
    val density = LocalDensity.current
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
    val activeIndexAnim = remember { Animatable(activeIndex.toFloat()) }
    val capsuleShape = RoundedCornerShape(percent = 50)

    LaunchedEffect(activeIndex, reducedMotion) {
        if (reducedMotion) {
            activeIndexAnim.snapTo(activeIndex.toFloat())
        } else {
            activeIndexAnim.animateTo(
                activeIndex.toFloat(),
                animationSpec = tween(durationMillis = 380, easing = UberNavEase),
            )
        }
    }

    // Transparent positioning shell — never paints edge-to-edge chrome.
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(
                    start = NavDockHorizontalMargin,
                    end = NavDockHorizontalMargin,
                    bottom = NavDockGapAboveSafeArea,
                ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val shadowLift = with(density) { 10.dp.toPx() }
        Box(
            modifier =
                Modifier
                    .widthIn(max = NavMaxWidth)
                    .fillMaxWidth()
                    .height(NavDockHeight)
                    .drawBehind {
                        // Soft float shadow — visible on cream/light paper where elevation is weak.
                        val radius = size.height / 2f
                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.22f),
                            topLeft = Offset(0f, shadowLift * 0.35f),
                            size = Size(size.width, size.height),
                            cornerRadius = CornerRadius(radius, radius),
                        )
                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.10f),
                            topLeft = Offset(-4.dp.toPx(), shadowLift * 0.9f),
                            size = Size(size.width + 8.dp.toPx(), size.height),
                            cornerRadius = CornerRadius(radius, radius),
                        )
                    }.clip(capsuleShape)
                    .background(colors.navGlass)
                    .border(1.dp, colors.navBorder, capsuleShape)
                    .padding(NavInnerPadding),
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val tabWidth = maxWidth / tabs.size
                val bandTop = 2.dp
                val capsuleCenterX = tabWidth * (activeIndexAnim.value + 0.5f)
                val capsuleOffsetX = capsuleCenterX - NavIconCapsuleWidth / 2

                // Soft oval active highlight — icon only, slides between tabs.
                Box(
                    modifier =
                        Modifier
                            .offset(x = capsuleOffsetX, y = bandTop)
                            .size(width = NavIconCapsuleWidth, height = NavIconCapsuleHeight)
                            .clip(NavIconCapsuleShape)
                            .background(colors.navPill),
                )

                Row(modifier = Modifier.fillMaxSize()) {
                    tabs.forEach { entry ->
                        FloatingNavTab(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            label = entry.label,
                            icon = entry.icon,
                            active = entry.tab == activeTab,
                            badge = if (entry.tab == StudentTab.CART) cartCount else 0,
                            badgeBorder = colors.navGlass,
                            reduceMotion = reducedMotion,
                            onClick = entry.onClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingNavTab(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    badge: Int = 0,
    badgeBorder: Color,
) {
    val colors = LocalVaiinillaColors.current
    val foreground = if (active) colors.navTextActive else colors.navTextIdle
    val iconScale by animateFloatAsState(
        targetValue = if (active) 1.06f else 1f,
        animationSpec = if (reduceMotion) tween(0) else tween(220, easing = UberNavEase),
        label = "nav-icon-scale",
    )

    Column(
        modifier =
            modifier
                .physicalPress(scale = PhysicalPressScale.Nav, onClick = onClick)
                .padding(top = 2.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Box(
            modifier =
                Modifier
                    .height(NavIconCapsuleHeight)
                    .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Box {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = foreground,
                    modifier =
                        Modifier
                            .size(NavIconSize)
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
                                .offset(x = 7.dp, y = (-7).dp)
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
        Spacer(Modifier.height(NavIconLabelGap))
        Text(
            text = label,
            color = foreground,
            fontSize = NavLabelSize,
            lineHeight = 12.sp,
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
