package com.vaiinilla.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.vaiinilla.app.ui.theme.Coral
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.lerp as lerpColor

/*
 * Barra del alumno al estilo de la barra de pestañas de iOS 26: una cápsula de vidrio con las
 * pestañas y, a su derecha, un círculo del mismo vidrio con el perfil. Al tocar, la lente se
 * levanta, sigue al dedo y se asienta con un resorte en la pestaña donde lo sueltas.
 */

private val BarHeight = 62.dp
private val BarMaxWidth = 560.dp
private val BarSideMargin = 16.dp
private val BarBottomGap = 6.dp
private val ProfileGap = 8.dp
private val LensInset = 5.dp
private val IconSize = 24.dp
private val LabelSize = 10.5.sp
private const val MAX_FONT_SCALE = 1.3f

/** Cuánto crece la lente levantada; en iOS es la gota que se ve más grande que la pestaña. */
private const val LENS_LIFT_SCALE = 0.16f

/** Cuánto se estira la lente según su velocidad, en anchos de pestaña. */
private const val LENS_STRETCH = 0.22f

private val LensSettle = spring<Float>(dampingRatio = 0.72f, stiffness = 380f)
private val LensFollow = spring<Float>(dampingRatio = 0.9f, stiffness = 1100f)
private val LiftSpring = spring<Float>(dampingRatio = 0.6f, stiffness = 520f)

/** Lo que el contenido deja libre abajo: la barra, su hueco y un respiro (sin el área del sistema). */
val VaiinillaBottomNavClearance: Dp = BarHeight + BarBottomGap + 16.dp

/** Dónde termina la barra por arriba (sin el área del sistema): lo que flota encima va arriba de esto. */
val VaiinillaBottomNavTop: Dp = BarHeight + BarBottomGap

/** Margen lateral de la barra, para alinear con su orilla lo que flota encima. */
val VaiinillaBottomNavSideMargin: Dp = BarSideMargin

/** Pestañas que muestra la barra hoy; el asistente sigue reservado. */
internal val StudentNavTabs = listOf(StudentTab.MENU, StudentTab.ORDERS, StudentTab.WALLET, StudentTab.CART)

enum class StudentTab {
    MENU,
    ASSISTANT, // reservada; no entra en la barra hasta que salga el asistente
    ORDERS,
    WALLET,
    CART,
}

/** Lo que dibuja el círculo de la derecha: iniciales con sesión, una silueta sin ella. */
data class NavProfile(
    val initials: String?,
)

@Composable
fun VaiinillaBottomNav(
    activeTab: StudentTab,
    cartCount: Int,
    onTabSelected: (StudentTab) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    profile: NavProfile? = null,
    onOpenProfile: () -> Unit = {},
    onTabPreparing: (StudentTab) -> Unit = {},
    visibleTabs: List<StudentTab> = StudentNavTabs,
) {
    val glass = navGlassStyle()
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = BarSideMargin, end = BarSideMargin, bottom = BarBottomGap),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .weight(1f, fill = false)
                    .widthIn(max = BarMaxWidth)
                    .fillMaxWidth()
                    .height(BarHeight)
                    .navGlass(backdrop, glass),
        ) {
            TabStrip(
                tabs = visibleTabs,
                activeTab = activeTab,
                cartCount = cartCount,
                glass = glass,
                onTabSelected = onTabSelected,
                onTabPreparing = onTabPreparing,
            )
        }
        if (profile != null) {
            Spacer(Modifier.width(ProfileGap))
            ProfileOrb(profile = profile, backdrop = backdrop, glass = glass, onClick = onOpenProfile)
        }
    }
}

@Composable
private fun TabStrip(
    tabs: List<StudentTab>,
    activeTab: StudentTab,
    cartCount: Int,
    glass: NavGlassStyle,
    onTabSelected: (StudentTab) -> Unit,
    onTabPreparing: (StudentTab) -> Unit,
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val reduced = reducedMotion()
    val colors = LocalVaiinillaColors.current

    val activeIndex = tabs.indexOf(activeTab).coerceAtLeast(0)
    val lensIndex = remember { Animatable(activeIndex.toFloat()) }
    val lift = remember { Animatable(0f) }
    var touching by remember { mutableStateOf(false) }
    val onSelected by rememberUpdatedState(onTabSelected)
    val onPreparing by rememberUpdatedState(onTabPreparing)
    val currentActive by rememberUpdatedState(activeIndex)

    // La ruta también cambia sin la barra (un botón de "ver pedido", el atrás del sistema).
    LaunchedEffect(activeIndex, reduced) {
        if (touching) return@LaunchedEffect
        if (reduced) lensIndex.snapTo(activeIndex.toFloat()) else lensIndex.animateTo(activeIndex.toFloat(), LensSettle)
    }

    fun commit(index: Int) {
        val tab = tabs[index]
        if (index != currentActive) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onPreparing(tab)
            onSelected(tab)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(LensInset)) {
        val count = tabs.size.coerceAtLeast(1)
        val slot = maxWidth / count
        val slotPx = with(density) { slot.toPx() }
        val last = (count - 1).toFloat()
        fun indexAt(x: Float) = (x / slotPx - 0.5f).coerceIn(0f, last)

        val gesture =
            Modifier.pointerInput(slotPx, count, reduced) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    touching = true
                    scope.launch { lift.animateTo(1f, if (reduced) spring() else LiftSpring) }
                    scope.launch {
                        val target = indexAt(down.position.x).roundToInt().toFloat()
                        if (reduced) lensIndex.snapTo(target) else lensIndex.animateTo(target, LensSettle)
                    }
                    var lastX = down.position.x
                    var moved = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        if (abs(change.position.x - down.position.x) > viewConfiguration.touchSlop) moved = true
                        if (moved && change.position.x != lastX) {
                            lastX = change.position.x
                            val follow = indexAt(lastX)
                            scope.launch { lensIndex.animateTo(follow, LensFollow) }
                            change.consume()
                        }
                    }
                    val chosen = indexAt(lastX).roundToInt()
                    touching = false
                    scope.launch { lift.animateTo(0f, if (reduced) spring() else LiftSpring) }
                    scope.launch {
                        if (reduced) lensIndex.snapTo(chosen.toFloat()) else lensIndex.animateTo(chosen.toFloat(), LensSettle)
                    }
                    commit(chosen)
                }
            }

        val position = lensIndex.value.coerceIn(0f, last)
        val stretch = (abs(lensIndex.velocity) / 9f).coerceIn(0f, 1f)
        val liftValue = lift.value

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(gesture)
                    .drawBehind {
                        val baseWidth = slotPx
                        val width = baseWidth * (1f + LENS_STRETCH * stretch)
                        val height = size.height * (1f - 0.06f * stretch)
                        val scale = 1f + LENS_LIFT_SCALE * liftValue
                        val w = width * scale
                        val h = height * (1f + LENS_LIFT_SCALE * 0.55f * liftValue)
                        val center = Offset((position + 0.5f) * slotPx, size.height / 2f)
                        val topLeft = Offset(center.x - w / 2f, center.y - h / 2f)
                        val radius = CornerRadius(h / 2f)
                        // En reposo es un gris plano; levantada se vuelve gota clara con borde y sombra.
                        if (liftValue > 0.01f) {
                            drawRoundRect(
                                color = glass.lensShadow.copy(alpha = glass.lensShadow.alpha * liftValue),
                                topLeft = topLeft + Offset(0f, 3.dp.toPx() * liftValue),
                                size = Size(w, h),
                                cornerRadius = radius,
                            )
                        }
                        drawRoundRect(
                            color = lerpColor(glass.lensRest, glass.lensLifted, liftValue),
                            topLeft = topLeft,
                            size = Size(w, h),
                            cornerRadius = radius,
                        )
                        if (liftValue > 0.01f) {
                            drawRoundRect(
                                brush =
                                    Brush.verticalGradient(
                                        listOf(glass.rim.copy(alpha = glass.rim.alpha * liftValue), Color.Transparent),
                                        startY = topLeft.y,
                                        endY = topLeft.y + h,
                                    ),
                                topLeft = topLeft,
                                size = Size(w, h),
                                cornerRadius = radius,
                                style = Stroke(width = 1.dp.toPx()),
                            )
                        }
                    },
        ) {
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, min(density.fontScale, MAX_FONT_SCALE)),
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    tabs.forEachIndexed { index, tab ->
                        val nearness = (1f - abs(position - index)).coerceIn(0f, 1f)
                        TabItem(
                            item = navItemFor(tab),
                            selected = index == activeIndex,
                            nearness = nearness,
                            pressed = touching && abs(position - index) < 0.5f,
                            badge = if (tab == StudentTab.CART) cartCount else 0,
                            idle = colors.navTextIdle,
                            active = colors.navTextActive,
                            badgeRing = glass.badgeRing,
                            onSemanticClick = { commit(index) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabItem(
    item: NavItem,
    selected: Boolean,
    nearness: Float,
    pressed: Boolean,
    badge: Int,
    idle: Color,
    active: Color,
    badgeRing: Color,
    onSemanticClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = lerpColor(idle, active, nearness)
    val press by animateFloatAsState(
        targetValue = if (pressed) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "tab_press",
    )
    Column(
        modifier =
            modifier
                .semantics(mergeDescendants = true) {
                    role = Role.Tab
                    this.selected = selected
                    contentDescription = item.label
                    onClick(label = item.label) {
                        onSemanticClick()
                        true
                    }
                }.graphicsLayer {
                    scaleX = press
                    scaleY = press
                },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(modifier = Modifier.size(IconSize), contentAlignment = Alignment.Center) {
            // El ícono relleno aparece conforme la lente llega, igual que el .fill de SF Symbols.
            Icon(
                imageVector = item.outlined,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 1f - nearness },
            )
            Icon(
                imageVector = item.filled,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = nearness },
            )
            CartBadge(
                count = badge,
                ring = badgeRing,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 9.dp, y = (-5).dp),
            )
        }
        Text(
            text = item.label,
            color = tint,
            fontSize = LabelSize,
            lineHeight = 12.sp,
            fontWeight = if (nearness > 0.5f) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun CartBadge(
    count: Int,
    ring: Color,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = count > 0,
        modifier = modifier,
        enter = scaleIn(spring(dampingRatio = 0.5f, stiffness = 600f)) + fadeIn(),
        exit = scaleOut(spring(stiffness = 700f)) + fadeOut(),
    ) {
        val pop = remember { Animatable(1f) }
        LaunchedEffect(count) {
            pop.snapTo(1.3f)
            pop.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 520f))
        }
        Box(
            modifier =
                Modifier
                    .graphicsLayer {
                        scaleX = pop.value
                        scaleY = pop.value
                    }.height(18.dp)
                    .widthIn(min = 18.dp)
                    .background(ring, CircleShape)
                    .padding(1.5.dp)
                    .background(Coral, CircleShape)
                    .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = count.coerceAtLeast(1),
                transitionSpec = {
                    val up = targetState > initialState
                    (slideInVertically { if (up) it else -it } + fadeIn()) togetherWith
                        (slideOutVertically { if (up) -it else it } + fadeOut())
                },
                label = "cart_badge_count",
            ) { value ->
                Text(
                    text = if (value > 99) "99+" else value.toString(),
                    color = Color.White,
                    // La burbuja mide 18 dp fijos: con la letra del sistema al doble taparía el ícono.
                    fontSize = with(LocalDensity.current) { 10.dp.toSp() },
                    lineHeight = with(LocalDensity.current) { 10.dp.toSp() },
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ProfileOrb(
    profile: NavProfile,
    backdrop: Backdrop?,
    glass: NavGlassStyle,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val initials = profile.initials?.takeIf { it.isNotBlank() && it != "?" }
    Box(
        modifier =
            Modifier
                .size(BarHeight)
                .physicalPress(scale = PhysicalPressScale.Nav, onClick = onClick)
                .semantics {
                    role = Role.Button
                    contentDescription = "Tu cuenta"
                }.navGlass(backdrop, glass),
        contentAlignment = Alignment.Center,
    ) {
        if (initials != null) {
            Box(
                modifier =
                    Modifier
                        .size(BarHeight - 10.dp)
                        .background(
                            Brush.linearGradient(listOf(colors.navTextActive.copy(alpha = 0.78f), colors.navTextActive)),
                            CircleShape,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontSize = with(LocalDensity.current) { 18.dp.toSp() },
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.2.sp,
                )
            }
        } else {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = colors.navTextIdle,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

private data class NavItem(
    val label: String,
    val outlined: ImageVector,
    val filled: ImageVector,
)

private fun navItemFor(tab: StudentTab): NavItem =
    when (tab) {
        StudentTab.MENU -> NavItem("Menú", Icons.Outlined.Home, Icons.Rounded.Home)
        StudentTab.ORDERS -> NavItem("Pedidos", Icons.AutoMirrored.Outlined.ReceiptLong, Icons.AutoMirrored.Rounded.ReceiptLong)
        StudentTab.WALLET -> NavItem("Cartera", Icons.Outlined.AccountBalanceWallet, Icons.Rounded.AccountBalanceWallet)
        StudentTab.CART -> NavItem("Carrito", Icons.Outlined.ShoppingCart, Icons.Rounded.ShoppingCart)
        StudentTab.ASSISTANT -> error("El asistente aún no está en la barra")
    }
