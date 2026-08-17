package com.vaiinilla.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.theme.Coral
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Process-local seed so a rare remount can spring from the previous index
 * (same role as Flutter's unbounded AnimationController value).
 */
internal object StudentNavPillMotion {
    var index: Float = 0f
    var lastTab: StudentTab = StudentTab.MENU
}

/**
 * Port of [SpringFloatingNavBar](https://github.com/Damantha126/Floating-Navbar-M3-Flutter)
 * (lib/Navigation/navbar.dart) to Compose — 4 student tabs (asistente hidden until a later update).
 *
 * Flutter SoT:
 * - SpringDescription(mass:1, stiffness:450, damping:28)
 * - Pill: left = index * itemWidth + inset, inset top/bottom 6
 * - Dock: height 68, radius 28, BackdropFilter blur 20 + translucent fill
 * - Selected icon scale 1.15, filled vs outlined
 *
 * Chrome (dock/pill/text) comes from the active theme: near-white in Light, Uber dark in
 * Dark/Amoled. See [com.vaiinilla.app.ui.theme.VaiinillaThemeMode.resolveColors].
 */
private val NavDockHeight = 68.dp
private val NavMaxWidth = 568.dp
private val NavDockGapAboveSafeArea = 8.dp
private val NavDockHorizontalMargin = 16.dp
private val NavDockShape = RoundedCornerShape(32.dp)
private val NavBubbleShape = RoundedCornerShape(24.dp)
private val NavPillInsetX = 6.dp
private val NavPillInsetY = 6.dp
private val NavDockElevation = 8.dp
private val NavIconSize = 24.dp
private val NavLabelSize = 12.sp
private val NavIconLabelGap = 5.dp
private val NavColorMotionMs = 200

/**
 * Flutter: damping / (2 * sqrt(stiffness * mass)) = 28 / (2 * sqrt(450)) ≈ 0.66
 */
private val NavPillSpring =
    spring<Float>(
        dampingRatio = 0.66f,
        stiffness = 450f,
    )

/** Content clearance: dock + float gap + breathing room (excludes system inset). */
val VaiinillaBottomNavClearance: Dp = NavDockHeight + NavDockGapAboveSafeArea + 16.dp

enum class StudentTab {
    MENU,
    ASSISTANT, // reserved; not in the dock until the next assistant drop
    ORDERS,
    WALLET,
    CART,
}

@Composable
fun VaiinillaBottomNav(
    activeTab: StudentTab,
    cartCount: Int,
    onTabSelected: (StudentTab) -> Unit,
    modifier: Modifier = Modifier,
    enableDrag: Boolean = true,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val colors = LocalVaiinillaColors.current
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
            NavTab(
                StudentTab.MENU,
                "Menú",
                Icons.Outlined.Home,
                Icons.Filled.Home,
            ),
            NavTab(
                StudentTab.ORDERS,
                "Pedidos",
                Icons.AutoMirrored.Outlined.ReceiptLong,
                Icons.AutoMirrored.Filled.ReceiptLong,
            ),
            NavTab(
                StudentTab.WALLET,
                "Cartera",
                Icons.Outlined.AccountBalanceWallet,
                Icons.Filled.AccountBalanceWallet,
            ),
            NavTab(
                StudentTab.CART,
                "Carrito",
                Icons.Outlined.ShoppingCart,
                Icons.Filled.ShoppingCart,
            ),
        )

    val activeIndex = tabs.indexOfFirst { it.tab == activeTab }.coerceAtLeast(0)
    val indexAnim = remember { Animatable(StudentNavPillMotion.index) }
    var dragIndex by remember { mutableFloatStateOf(StudentNavPillMotion.index) }
    var isDragging by remember { mutableStateOf(false) }
    val onTabSelectedLatest by rememberUpdatedState(onTabSelected)
    val activeTabLatest by rememberUpdatedState(activeTab)
    val activeIndexLatest by rememberUpdatedState(activeIndex)

    // Flutter didUpdateWidget → SpringSimulation to new index.
    LaunchedEffect(activeIndex, reducedMotion) {
        if (reducedMotion) {
            indexAnim.snapTo(activeIndex.toFloat())
        } else {
            indexAnim.animateTo(
                targetValue = activeIndex.toFloat(),
                animationSpec = NavPillSpring,
            )
        }
        StudentNavPillMotion.index = indexAnim.value
        StudentNavPillMotion.lastTab = activeTab
    }

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
        Box(
            modifier =
                Modifier
                    .widthIn(max = NavMaxWidth)
                    .fillMaxWidth()
                    .height(NavDockHeight)
                    .shadow(NavDockElevation, NavDockShape)
                    .clip(NavDockShape)
                    .background(colors.navGlass)
                    .border(1.dp, colors.navBorder, NavDockShape),
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val tabCount = tabs.size.coerceAtLeast(1)
                val itemWidth = maxWidth / tabCount
                val itemWidthPx = with(density) { itemWidth.toPx() }
                val pillInsetXPx = with(density) { NavPillInsetX.toPx() }
                val pillInsetYPx = with(density) { NavPillInsetY.toPx() }
                val lastIndex = (tabCount - 1).toFloat()

                val visualIndex = if (isDragging) dragIndex else indexAnim.value
                val dragModifier =
                    if (enableDrag && !reducedMotion) {
                        Modifier.pointerInput(itemWidthPx, lastIndex) {
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    // Freeze the visible position into synchronous drag state.
                                    // This avoids launching one coroutine per pointer movement.
                                    dragIndex = indexAnim.value.coerceIn(0f, lastIndex)
                                    isDragging = true
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    dragIndex =
                                        (dragIndex + dragAmount / itemWidthPx)
                                            .coerceIn(0f, lastIndex)
                                },
                                onDragEnd = {
                                    val releaseIndex = dragIndex.coerceIn(0f, lastIndex)
                                    val nearest = nearestStudentNavIndex(releaseIndex, tabCount)
                                    val targetIndex = nearest.toFloat()
                                    val tab = tabs[nearest].tab
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                                    // Snap navigation and pill from the exact same resolved slot.
                                    // Keep drag rendering active until Animatable is seeded from the
                                    // release position, preventing a one-frame jump back to the old tab.
                                    scope.launch {
                                        indexAnim.snapTo(releaseIndex)
                                        isDragging = false
                                        indexAnim.animateTo(targetIndex, NavPillSpring)
                                        StudentNavPillMotion.index = targetIndex
                                        StudentNavPillMotion.lastTab = tab
                                    }
                                    if (tab != activeTabLatest) {
                                        onTabSelectedLatest(tab)
                                    }
                                },
                                onDragCancel = {
                                    val releaseIndex = dragIndex.coerceIn(0f, lastIndex)
                                    val targetIndex = activeIndexLatest.toFloat()
                                    scope.launch {
                                        indexAnim.snapTo(releaseIndex)
                                        isDragging = false
                                        indexAnim.animateTo(targetIndex, NavPillSpring)
                                    }
                                },
                            )
                        }
                    } else {
                        Modifier
                    }

                Box(modifier = Modifier.fillMaxSize().then(dragModifier)) {
                    // Active pill — Flutter Positioned(left: value * itemWidth + inset, …)
                    val pillLeftPx = visualIndex * itemWidthPx + pillInsetXPx
                    Box(
                        modifier =
                            Modifier
                                .offset {
                                    IntOffset(
                                        x = pillLeftPx.roundToInt(),
                                        y = pillInsetYPx.roundToInt(),
                                    )
                                }.width(itemWidth - NavPillInsetX * 2)
                                .height(NavDockHeight - NavPillInsetY * 2)
                                .clip(NavBubbleShape)
                                .background(colors.navPill),
                    )

                    Row(modifier = Modifier.fillMaxSize()) {
                        tabs.forEachIndexed { index, entry ->
                            val selected = entry.tab == activeTab
                            // Soft highlight while dragging near this slot.
                            val near =
                                abs(visualIndex - index) < 0.5f
                            FloatingNavTab(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                label = entry.label,
                                icon = if (selected || near) entry.iconSelected else entry.iconIdle,
                                active = selected || near,
                                badge = if (entry.tab == StudentTab.CART) cartCount else 0,
                                reduceMotion = reducedMotion,
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onTabSelected(entry.tab)
                                },
                            )
                        }
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
) {
    val colors = LocalVaiinillaColors.current
    val foreground by animateColorAsState(
        targetValue = if (active) colors.navTextActive else colors.navTextIdle,
        animationSpec =
            if (reduceMotion) {
                tween(0)
            } else {
                tween(NavColorMotionMs)
            },
        label = "nav-fg",
    )
    // Flutter AnimatedScale(scale: selected ? 1.15 : 1)
    val iconScale by animateFloatAsState(
        targetValue = if (active) 1.15f else 1f,
        animationSpec =
            if (reduceMotion) {
                tween(0)
            } else {
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                )
            },
        label = "nav-icon-scale",
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0.6f,
        animationSpec =
            if (reduceMotion) {
                tween(0)
            } else {
                tween(NavColorMotionMs)
            },
        label = "nav-label-alpha",
    )

    Column(
        modifier =
            modifier
                .physicalPress(scale = PhysicalPressScale.Nav, onClick = onClick)
                .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
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
                            .offset(x = 8.dp, y = (-6).dp)
                            .height(16.dp)
                            .width(if (badge > 9) 20.dp else 16.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Coral)
                            .border(2.dp, colors.navPill, RoundedCornerShape(99.dp)),
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
        Text(
            text = label,
            color = foreground.copy(alpha = labelAlpha),
            fontSize = NavLabelSize,
            lineHeight = 13.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            letterSpacing = (-0.04).sp,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.padding(top = NavIconLabelGap),
        )
    }
}

internal fun nearestStudentNavIndex(
    index: Float,
    tabCount: Int,
): Int {
    if (tabCount <= 0) return 0
    return index.roundToInt().coerceIn(0, tabCount - 1)
}

private data class NavTab(
    val tab: StudentTab,
    val label: String,
    val iconIdle: ImageVector,
    val iconSelected: ImageVector,
)
