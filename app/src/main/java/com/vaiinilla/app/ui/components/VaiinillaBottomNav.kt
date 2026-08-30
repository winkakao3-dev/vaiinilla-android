package com.vaiinilla.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.R
import com.vaiinilla.app.ui.theme.Coral
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.ui.graphics.lerp as lerpColor

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
 * - Dock proportions now follow the supplied Vaiinilla navbar reference.
 * - Selected icon scale 1.15, filled vs outlined
 *
 * Chrome (dock/pill/text) comes from the active theme: near-white in Light, Uber dark in
 * Dark/Amoled. See [com.vaiinilla.app.ui.theme.VaiinillaThemeMode.resolveColors].
 */
private val NavDockHeight = 55.dp
private val NavMaxWidth = 568.dp
private val NavDockGapAboveSafeArea = 8.dp
private val NavDockHorizontalMargin = 19.dp
private val NavDockShape = RoundedCornerShape(28.dp)
private val NavBubbleShape = RoundedCornerShape(25.dp)
private val NavContentInsetX = 10.dp
private val NavPillExtraWidth = 12.dp
private val NavPillInsetY = 3.dp
private val NavDockElevation = 5.dp
private val NavIconSize = 24.dp
private val NavLabelSize = 8.sp
private val NavIconLabelGap = 4.dp
private val NavColorMotionMs = 240

/**
 * Flutter: damping / (2 * sqrt(stiffness * mass)) = 28 / (2 * sqrt(450)) ≈ 0.66
 */
private val NavMotionEase = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

private val NavPillSpring =
    spring<Float>(
        dampingRatio = 0.82f,
        stiffness = 310f,
    )

private val NavTapMotion =
    tween<Float>(
        durationMillis = 220,
        easing = NavMotionEase,
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
    hazeState: HazeState? = null,
    onTabSelected: (StudentTab) -> Unit,
    onTabPreparing: (StudentTab) -> Unit = {},
    modifier: Modifier = Modifier,
    enableDrag: Boolean = false,
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
                R.drawable.ic_nav_home_reference,
                iconWidth = 24.55.dp,
                iconHeight = 21.82.dp,
            ),
            NavTab(
                StudentTab.ORDERS,
                "Pedidos",
                R.drawable.ic_nav_orders_reference,
                iconWidth = 24.25.dp,
                iconHeight = 18.79.dp,
            ),
            NavTab(
                StudentTab.WALLET,
                "Cartera",
                R.drawable.ic_nav_wallet_reference,
                iconWidth = 17.27.dp,
                iconHeight = 22.12.dp,
            ),
            NavTab(
                StudentTab.CART,
                "Carrito",
                R.drawable.ic_nav_cart_reference,
                iconWidth = 23.03.dp,
                iconHeight = 20.31.dp,
            ),
        )

    val activeIndex = tabs.indexOfFirst { it.tab == activeTab }.coerceAtLeast(0)
    val indexAnim = remember { Animatable(StudentNavPillMotion.index) }
    var dragIndex by remember { mutableFloatStateOf(StudentNavPillMotion.index) }
    var isDragging by remember { mutableStateOf(false) }
    var optimisticTargetIndex by remember { mutableStateOf<Int?>(null) }
    var motionJob by remember { mutableStateOf<Job?>(null) }
    val onTabSelectedLatest by rememberUpdatedState(onTabSelected)
    val onTabPreparingLatest by rememberUpdatedState(onTabPreparing)
    val activeTabLatest by rememberUpdatedState(activeTab)
    val activeIndexLatest by rememberUpdatedState(activeIndex)

    // Navigation confirms the selected route after the local pill has already started moving.
    // If this route change matches our optimistic target, do not restart the animation.
    LaunchedEffect(activeIndex, reducedMotion) {
        val optimisticTarget = optimisticTargetIndex
        if (optimisticTarget == activeIndex) {
            optimisticTargetIndex = null
            StudentNavPillMotion.lastTab = activeTab
            return@LaunchedEffect
        }
        optimisticTargetIndex = null
        motionJob?.cancel()
        motionJob = null
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
                    .then(
                        if (hazeState != null) {
                            Modifier.hazeEffect(hazeState) {
                                blurEffect {
                                    blurEnabled = true
                                    blurRadius = 20.dp
                                    noiseFactor = 0f
                                    backgroundColor = Color.Transparent
                                    colorEffects =
                                        listOf(
                                            HazeColorEffect.tint(colors.navGlass.copy(alpha = 0.72f)),
                                        )
                                    fallbackTint = HazeColorEffect.tint(colors.navGlass.copy(alpha = 0.94f))
                                }
                            }
                        } else {
                            Modifier.background(colors.navGlass)
                        },
                    ).border(1.dp, colors.navBorder, NavDockShape),
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val tabCount = tabs.size.coerceAtLeast(1)
                val contentWidth = maxWidth - NavContentInsetX * 2
                val itemWidth = contentWidth / tabCount
                val basePillWidth = itemWidth + NavPillExtraWidth
                val itemWidthPx = with(density) { itemWidth.toPx() }
                val contentInsetXPx = with(density) { NavContentInsetX.toPx() }
                val pillInsetYPx = with(density) { NavPillInsetY.toPx() }
                val lastIndex = (tabCount - 1).toFloat()

                val visualIndex = if (isDragging) dragIndex else indexAnim.value
                val boundedVisualIndex = visualIndex.coerceIn(0f, lastIndex)
                val segmentProgress = boundedVisualIndex - floor(boundedVisualIndex)
                val stretchProgress = sin(PI.toFloat() * segmentProgress).coerceIn(0f, 1f)
                val stretchWidth = itemWidth * (0.72f * stretchProgress)
                val pillWidth = basePillWidth + stretchWidth
                val pillWidthPx = with(density) { pillWidth.toPx() }
                val dragModifier =
                    if (enableDrag && !reducedMotion) {
                        Modifier.pointerInput(itemWidthPx, lastIndex) {
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    motionJob?.cancel()
                                    motionJob = null
                                    optimisticTargetIndex = null
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
                                    optimisticTargetIndex = nearest
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                                    // Snap navigation and pill from the exact same resolved slot.
                                    // Keep drag rendering active until Animatable is seeded from the
                                    // release position, preventing a one-frame jump back to the old tab.
                                    motionJob?.cancel()
                                    motionJob =
                                        scope.launch {
                                            indexAnim.snapTo(releaseIndex)
                                            isDragging = false
                                            launch {
                                                delay(24)
                                                onTabPreparingLatest(tab)
                                            }
                                            indexAnim.animateTo(targetIndex, NavPillSpring)
                                            StudentNavPillMotion.index = targetIndex
                                            StudentNavPillMotion.lastTab = tab
                                            if (tab != activeTabLatest) {
                                                onTabSelectedLatest(tab)
                                            }
                                        }
                                },
                                onDragCancel = {
                                    val releaseIndex = dragIndex.coerceIn(0f, lastIndex)
                                    val targetIndex = activeIndexLatest.toFloat()
                                    motionJob?.cancel()
                                    motionJob =
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
                    val pillCenterPx =
                        contentInsetXPx + (boundedVisualIndex + 0.5f) * itemWidthPx
                    val pillLeftPx = pillCenterPx - pillWidthPx / 2f
                    Box(
                        modifier =
                            Modifier
                                .offset {
                                    IntOffset(
                                        x = pillLeftPx.roundToInt(),
                                        y = pillInsetYPx.roundToInt(),
                                    )
                                }.width(pillWidth)
                                .height(NavDockHeight - NavPillInsetY * 2)
                                .graphicsLayer {
                                    scaleY = 1f + 0.025f * stretchProgress
                                }.clip(NavBubbleShape)
                                .background(colors.navPill),
                    )

                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = NavContentInsetX),
                    ) {
                        tabs.forEachIndexed { index, entry ->
                            val selection =
                                (1f - abs(boundedVisualIndex - index.toFloat())).coerceIn(0f, 1f)
                            FloatingNavTab(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                label = entry.label,
                                iconRes = entry.iconRes,
                                selection = selection,
                                directSelection = isDragging,
                                badge = if (entry.tab == StudentTab.CART) cartCount else 0,
                                iconWidth = entry.iconWidth,
                                iconHeight = entry.iconHeight,
                                reduceMotion = reducedMotion,
                                onClick = {
                                    val targetIndex = index
                                    val targetValue = targetIndex.toFloat()
                                    if (targetIndex == activeIndexLatest && optimisticTargetIndex == null) {
                                        return@FloatingNavTab
                                    }
                                    optimisticTargetIndex = targetIndex
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    motionJob?.cancel()
                                    motionJob =
                                        scope.launch {
                                            launch {
                                                delay(24)
                                                onTabPreparingLatest(entry.tab)
                                            }
                                            if (reducedMotion) {
                                                indexAnim.snapTo(targetValue)
                                            } else {
                                                indexAnim.animateTo(targetValue, NavTapMotion)
                                            }
                                            StudentNavPillMotion.index = targetValue
                                            StudentNavPillMotion.lastTab = entry.tab
                                            // Only after the liquid motion has completed may content swap.
                                            if (entry.tab != activeTabLatest) {
                                                onTabSelectedLatest(entry.tab)
                                            }
                                        }
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
    iconRes: Int,
    selection: Float,
    directSelection: Boolean,
    onClick: () -> Unit,
    reduceMotion: Boolean,
    iconWidth: Dp,
    iconHeight: Dp,
    modifier: Modifier = Modifier,
    badge: Int = 0,
) {
    val colors = LocalVaiinillaColors.current
    val animatedSelection by animateFloatAsState(
        targetValue = selection,
        animationSpec =
            if (reduceMotion) {
                tween(0)
            } else {
                tween(NavColorMotionMs, easing = NavMotionEase)
            },
        label = "nav-selection",
    )
    val visualSelection = if (directSelection) selection else animatedSelection
    val foreground = lerpColor(colors.navTextIdle, colors.navTextActive, visualSelection)

    Column(
        modifier =
            modifier
                .physicalPress(scale = PhysicalPressScale.Nav, onClick = onClick)
                .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(width = iconWidth, height = iconHeight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
                tint = foreground,
                modifier = Modifier.fillMaxSize(),
            )
            if (badge > 0) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 7.dp, y = (-4).dp)
                            .height(18.dp)
                            .width(if (badge > 9) 23.dp else 18.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Coral)
                            .border(1.dp, colors.navPill, RoundedCornerShape(99.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = badge.coerceAtMost(99).toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        lineHeight = 10.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
        Text(
            text = label,
            color = foreground,
            fontSize = NavLabelSize,
            lineHeight = 10.sp,
            fontWeight = if (visualSelection >= 0.5f) FontWeight.SemiBold else FontWeight.Normal,
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
    val iconRes: Int,
    val iconWidth: Dp = NavIconSize,
    val iconHeight: Dp = NavIconSize,
)
