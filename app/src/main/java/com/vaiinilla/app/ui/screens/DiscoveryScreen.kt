package com.vaiinilla.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.ui.components.EditorialConfirmSheet
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.discovery.DiscoveryUiState
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

@Composable
fun DiscoveryScreen(
    state: DiscoveryUiState,
    onQueryChange: (String) -> Unit,
    onSpaceTokenChange: (String) -> Unit,
    onOpenQrScanner: () -> Unit = {},
    onSelectEstablishment: (PublicEstablishment) -> Unit,
    onResolveSpace: () -> Unit,
    onConfirmSwitch: () -> Unit,
    onDismissSwitch: () -> Unit,
    onContinueSelected: () -> Unit,
    profileInitials: String = "?",
    onOpenAccount: () -> Unit = {},
    onBack: (() -> Unit)? = null,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val focusManager = LocalFocusManager.current
    var codeSheetOpen by remember { mutableStateOf(false) }
    var tokenError by remember { mutableStateOf(false) }
    var pendingSelection by remember { mutableStateOf<PublicEstablishment?>(null) }
    var selectedFilter by remember { mutableStateOf(VenueFilter.ALL) }
    var dockPulseCount by remember { mutableIntStateOf(0) }
    var dockHeightPx by remember { mutableStateOf(0) }
    val dockHeight = with(LocalDensity.current) { dockHeightPx.toDp() }

    BackHandler(enabled = state.pendingSwitch != null) { onDismissSwitch() }

    val filteredEstablishments =
        remember(state.establishments, selectedFilter) {
            state.establishments.filter { est ->
                when (selectedFilter) {
                    VenueFilter.ALL, VenueFilter.OPEN -> true
                    VenueFilter.FREE -> !est.clientIdRequired
                    VenueFilter.ID_REQUIRED -> est.clientIdRequired
                }
            }
        }

    val activeId = state.selected?.establishment?.id
    val selection =
        pendingSelection
            ?: state.selected?.establishment
            ?: filteredEstablishments.firstOrNull()
            ?: state.establishments.firstOrNull()

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.paper)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = {
                            haptics.click()
                            onBack()
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                } else {
                    Spacer(Modifier.width(8.dp))
                }
                Spacer(Modifier.weight(1f))
                Box(
                    modifier =
                        Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(colors.ink)
                            .clickable {
                                haptics.click()
                                onOpenAccount()
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(profileInitials, color = colors.paper, fontWeight = FontWeight.Black)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding =
                    PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 0.dp,
                        bottom = if (dockHeightPx > 0) dockHeight + 26.dp else 216.dp,
                    ),
            ) {
                item {
                    Text(
                        "LOCALIZACIÓN",
                        color = if (colors.isDark) colors.accent else colors.accentInk,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.8.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Text(
                        "¿Dónde comes hoy?",
                        color = colors.ink,
                        fontSize = 34.sp,
                        lineHeight = 38.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.6).sp,
                        modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
                    )
                    Text(
                        if (state.selected != null) {
                            "Cambia cuando quieras o entra con el QR de tu mesa."
                        } else {
                            "Elige tu cafetería para mostrarte el menú correcto y los tiempos exactos de barra."
                        },
                        color = colors.muted,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                item {
                    DiscoverySearchField(
                        value = state.query,
                        onValueChange = onQueryChange,
                        modifier = Modifier.padding(top = 18.dp),
                    )
                }
                item {
                    FilterChipsBar(
                        selectedFilter = selectedFilter,
                        onSelectFilter = { selectedFilter = it },
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                state.suspendedMessage?.let { message ->
                    item {
                        VenueNoticeCard(
                            message = message,
                            tone = colors.coral,
                            modifier = Modifier.padding(top = 14.dp),
                        )
                    }
                }
                state.errorMessage?.let { message ->
                    item {
                        VenueNoticeCard(
                            message = message,
                            tone = colors.coral,
                            modifier = Modifier.padding(top = 14.dp),
                        )
                    }
                }
                if (state.loading && state.establishments.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = colors.accent)
                        }
                    }
                } else if (state.establishments.isEmpty()) {
                    item {
                        VenueEmptyState(
                            query = state.query,
                            onClearQuery = { onQueryChange("") },
                            onOpenQrScanner = onOpenQrScanner,
                        )
                    }
                } else if (filteredEstablishments.isEmpty()) {
                    item {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 18.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(colors.paper2)
                                    .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "No hay cafeterías disponibles para este filtro.",
                                color = colors.muted,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                } else {
                    val recommended = filteredEstablishments.first()
                    val others = filteredEstablishments.filterNot { it.id == recommended.id }
                    item {
                        SectionHeading(
                            label = "RECOMENDADA PARA TI",
                            modifier = Modifier.padding(top = 20.dp),
                        )
                    }
                    item {
                        RecommendedVenueCard(
                            establishment = recommended,
                            selected = selection?.id == recommended.id,
                            active = activeId == recommended.id,
                            onClick = {
                                haptics.selection()
                                pendingSelection = recommended
                                dockPulseCount++
                            },
                        )
                    }
                    if (others.isNotEmpty()) {
                        item {
                            SectionHeading(
                                label = "OTRAS SEDES DISPONIBLES",
                                trailing = "${others.size} activas",
                                modifier = Modifier.padding(top = 20.dp, bottom = 2.dp),
                            )
                        }
                        items(others, key = { it.id }) { establishment ->
                            VenueSelectRow(
                                establishment = establishment,
                                selected = selection?.id == establishment.id,
                                active = activeId == establishment.id,
                                onClick = {
                                    haptics.selection()
                                    pendingSelection = establishment
                                    dockPulseCount++
                                },
                            )
                        }
                    }
                }
                item {
                    QuickAccessCard(
                        onOpenQrScanner = onOpenQrScanner,
                        onUseCode = {
                            haptics.click()
                            codeSheetOpen = true
                        },
                        modifier = Modifier.padding(top = 22.dp),
                    )
                }
            }
        }

        VenueDock(
            selection = selection,
            isActive = selection != null && selection.id == activeId,
            dockPulseCount = dockPulseCount,
            onContinue = {
                val target = selection ?: return@VenueDock
                haptics.click()
                if (target.id != activeId) {
                    onSelectEstablishment(target)
                } else {
                    onContinueSelected()
                }
            },
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { dockHeightPx = it.height },
        )

        if (state.pendingSwitch != null) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(colors.paper.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                EditorialConfirmSheet(
                    title = "¿Cambiar de cafetería?",
                    message =
                        "Tu carrito pertenece a otra cafetería. Si continúas, no se mezclará con este pedido — " +
                            "quedará guardado por separado en la cafetería anterior.",
                    confirmLabel = "Cambiar",
                    dismissLabel = "Cancelar",
                    onConfirm = onConfirmSwitch,
                    onDismiss = onDismissSwitch,
                )
            }
        }

        if (codeSheetOpen) {
            SpaceCodeSheet(
                token = state.spaceTokenInput,
                resolving = state.resolving,
                showError = tokenError,
                onTokenChange = {
                    tokenError = false
                    onSpaceTokenChange(it)
                },
                onCancel = { codeSheetOpen = false },
                onResolve = {
                    if (state.spaceTokenInput.isBlank()) {
                        tokenError = true
                    } else {
                        codeSheetOpen = false
                        onResolveSpace()
                    }
                },
            )
        }
    }
}

@Composable
private fun SectionHeading(
    label: String,
    trailing: String? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = colors.muted,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.6.sp,
        )
        Spacer(Modifier.weight(1f))
        if (trailing != null) {
            Text(
                trailing,
                color = colors.muted,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

enum class VenueFilter(val label: String) {
    ALL("Todas"),
    OPEN("Abiertas ahora"),
    FREE("Acceso libre"),
    ID_REQUIRED("Con matrícula"),
}

@Composable
private fun FilterChipsBar(
    selectedFilter: VenueFilter,
    onSelectFilter: (VenueFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        VenueFilter.entries.forEach { filter ->
            val isSelected = filter == selectedFilter
            Row(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) colors.ink else colors.paper2)
                        .then(
                            if (!isSelected) {
                                Modifier.border(1.dp, colors.line, RoundedCornerShape(12.dp))
                            } else {
                                Modifier
                            },
                        )
                        .clickable {
                            haptics.selection()
                            onSelectFilter(filter)
                        }
                        .padding(horizontal = 13.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                if (filter == VenueFilter.OPEN) {
                    Box(
                        modifier =
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(colors.accent),
                    )
                }
                Text(
                    filter.label,
                    color = if (isSelected) colors.paper else colors.ink,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun CheckDot(
    selected: Boolean,
    size: androidx.compose.ui.unit.Dp = 26.dp,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Box(
        modifier =
            modifier
                .size(size)
                .clip(CircleShape)
                .background(if (selected) colors.accent else Color.Transparent)
                .border(
                    1.6.dp,
                    if (selected) colors.accent else colors.line,
                    CircleShape,
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = colors.accentInk,
                modifier = Modifier.size(size * 0.55f),
            )
        }
    }
}

@Composable
private fun RecommendedVenueCard(
    establishment: PublicEstablishment,
    selected: Boolean,
    active: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val meta =
        if (establishment.clientIdRequired) {
            "${establishment.clientIdLabel} requerida"
        } else {
            "Acceso libre"
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (selected) (if (colors.isDark) colors.paper2 else Color.White) else colors.paper2)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) colors.ink else colors.line,
                    shape = RoundedCornerShape(20.dp),
                )
                .physicalPress(onClick = onClick)
                .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(colors.paper),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Storefront,
                contentDescription = null,
                tint = colors.ink2,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(
            modifier = Modifier.padding(start = 13.dp).weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    establishment.name,
                    color = colors.ink,
                    fontSize = 16.5.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (active) {
                    Box(
                        modifier =
                            Modifier
                                .padding(start = 7.dp)
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(colors.accent),
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    meta,
                    color = colors.ink2,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.paper)
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text("🚶", fontSize = 11.sp)
                    Text(
                        "3 min",
                        color = colors.muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Text(
                "Menú del día · Terraza disponible",
                color = colors.muted,
                fontSize = 11.sp,
            )
        }
        CheckDot(
            selected = selected,
            size = 26.dp,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Composable
private fun VenueSelectRow(
    establishment: PublicEstablishment,
    selected: Boolean,
    active: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val meta =
        if (establishment.clientIdRequired) {
            "${establishment.clientIdLabel} requerida"
        } else {
            "Acceso libre"
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (selected) (if (colors.isDark) colors.paper2 else Color.White) else colors.paper2)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) colors.ink else colors.line,
                    shape = RoundedCornerShape(20.dp),
                )
                .physicalPress(onClick = onClick)
                .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.paper),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Storefront,
                contentDescription = null,
                tint = colors.ink2,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(
            modifier = Modifier.padding(start = 13.dp).weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    establishment.name,
                    color = colors.ink,
                    fontSize = 15.5.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (active) {
                    Box(
                        modifier =
                            Modifier
                                .padding(start = 7.dp)
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(colors.accent),
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    meta,
                    color = colors.ink2,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.paper)
                            .padding(horizontal = 7.dp, vertical = 2.5.dp),
                )
                Text(
                    "Campus",
                    color = colors.muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        CheckDot(
            selected = selected,
            size = 26.dp,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Composable
private fun VenueDock(
    selection: PublicEstablishment?,
    isActive: Boolean,
    dockPulseCount: Int = 0,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val haptics = rememberVaiinillaHaptics()
    val reduceMotion = reducedMotion()
    val enabled = selection != null
    val knobSize = 56.dp
    val trackPadding = 4.dp
    val dragX = remember { Animatable(0f) }
    var directDragX by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isDone by remember { mutableStateOf(false) }
    var showTip by remember { mutableStateOf(false) }
    var motionJob by remember { mutableStateOf<Job?>(null) }
    var maxDragPx by remember { mutableStateOf(0f) }
    var labelWidthPx by remember { mutableStateOf(0f) }
    var trackHeightPx by remember { mutableStateOf(0f) }

    // Pulse animation on venue card selection (1.02x scale for 150ms)
    val dockScale = remember { Animatable(1f) }
    LaunchedEffect(dockPulseCount) {
        if (dockPulseCount > 0) {
            dockScale.animateTo(1.02f, tween(80, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)))
            dockScale.animateTo(1f, tween(120, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)))
        }
    }

    LaunchedEffect(showTip) {
        if (showTip) {
            delay(1600)
            showTip = false
        }
    }

    // Knob "nudge" cada ~3.4s mientras está armado — enseña el gesto.
    val idleMotion = rememberInfiniteTransition(label = "dock-idle")
    val nudge by idleMotion.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    keyframes {
                        durationMillis = 3400
                        0f at 0
                        0f at 2448
                        8f at 2720
                        6f at 3060
                        0f at 3400
                    },
                repeatMode = RepeatMode.Restart,
            ),
        label = "dock-nudge",
    )

    // Anillo expansivo al confirmar.
    val ringProgress = remember { Animatable(0f) }
    LaunchedEffect(isDone) {
        if (isDone) {
            ringProgress.snapTo(0f)
            ringProgress.animateTo(1f, tween(800, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)))
        }
    }

    fun settleKnob(
        target: Float,
        onArrive: (() -> Unit)? = null,
    ) {
        motionJob?.cancel()
        motionJob =
            scope.launch {
                dragX.snapTo(target)
                isDragging = false
                if (target >= maxDragPx * 0.70f) {
                    haptics.click()
                    dragX.animateTo(
                        maxDragPx,
                        spring(stiffness = Spring.StiffnessMedium),
                    )
                    isDone = true
                    delay(if (reduceMotion) 120 else 550)
                    onContinue()
                    isDone = false
                    dragX.snapTo(0f)
                } else {
                    dragX.animateTo(
                        0f,
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    )
                }
            }
    }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0f to colors.paper.copy(alpha = 0f),
                        0.3f to colors.paper.copy(alpha = 0.95f),
                        1f to colors.paper,
                    ),
                )
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp)
                .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (showTip) {
                Row(
                    modifier =
                        Modifier
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(colors.ink)
                            .padding(horizontal = 13.dp, vertical = 7.dp),
                ) {
                    Text(
                        "Desliza → no toques",
                        color = colors.accent2,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .graphicsLayer {
                            scaleX = dockScale.value
                            scaleY = dockScale.value
                        }
                        .clip(RoundedCornerShape(32.dp))
                        .background(colors.paper2)
                        .border(1.dp, colors.line, RoundedCornerShape(32.dp))
                        .onSizeChanged { newSize ->
                            trackHeightPx = newSize.height.toFloat()
                            maxDragPx =
                                (
                                    newSize.width -
                                        with(density) { (knobSize + trackPadding * 2).toPx() }
                                ).coerceAtLeast(0f)
                        }
                        .semantics { contentDescription = "Desliza para entrar" }
                        .pointerInput(maxDragPx, enabled) {
                            detectTapGestures {
                                if (!isDone) showTip = true
                            }
                        }
                        .then(
                            if (enabled && !isDone) {
                                Modifier.pointerInput(maxDragPx) {
                                    detectHorizontalDragGestures(
                                        onDragStart = {
                                            motionJob?.cancel()
                                            showTip = false
                                            directDragX = dragX.value.coerceIn(0f, maxDragPx)
                                            isDragging = true
                                        },
                                        onDragEnd = {
                                            val releaseX = directDragX.coerceIn(0f, maxDragPx)
                                            settleKnob(releaseX)
                                        },
                                        onDragCancel = {
                                            val releaseX = directDragX.coerceIn(0f, maxDragPx)
                                            settleKnob(0f)
                                        },
                                    ) { change, dragAmount ->
                                        change.consume()
                                        directDragX =
                                            (directDragX + dragAmount)
                                                .coerceIn(0f, maxDragPx)
                                    }
                                }
                            } else {
                                Modifier
                            },
                        ),
                contentAlignment = Alignment.CenterStart,
            ) {
                val knobPx = with(density) { knobSize.toPx() }
                val padPx = with(density) { trackPadding.toPx() }
                val nudgePx = with(density) { nudge.dp.toPx() }
                val baseX = if (isDragging) directDragX else dragX.value
                val canNudge = enabled && !isDragging && !isDone && !reduceMotion
                val knobX = baseX + if (canNudge) nudgePx else 0f
                val progress = (baseX / maxDragPx.coerceAtLeast(1f)).coerceIn(0f, 1f)

                // Relleno lima que crece bajo el knob (empieza en 64dp)
                Box(
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .width(
                                with(density) {
                                    (knobX + knobPx + padPx * 2).toDp()
                                },
                            )
                            .clip(RoundedCornerShape(32.dp))
                            .background(
                                if (enabled) {
                                    Brush.horizontalGradient(
                                        listOf(colors.accent2, colors.accent),
                                    )
                                } else {
                                    SolidColor(colors.line)
                                }
                            ),
                )

                // Dynamic label matching prototype (two-line centered)
                val eyebrow =
                    when {
                        selection == null -> "ELIGE UNA CAFETERÍA"
                        isDone -> "CAFETERÍA ACTIVA"
                        progress >= 0.70f -> "SUELTA PARA ENTRAR"
                        else -> "DESLIZA PARA ENTRAR"
                    }

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 54.dp, end = 24.dp)
                            .alpha(
                                if (isDone) {
                                    1f
                                } else {
                                    (1f - (progress / 0.8f)).coerceIn(0f, 1f)
                                },
                            ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        eyebrow,
                        color = colors.muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(top = 1.dp),
                    ) {
                        Text(
                            (selection?.name ?: "—").uppercase(),
                            color = colors.ink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "›››",
                            color = colors.muted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1).sp,
                        )
                    }
                }

                // Anillo expansivo al confirmar.
                if (isDone) {
                    val ringScale = 0.9f + ringProgress.value * 1.5f
                    Box(
                        modifier =
                            Modifier
                                .offset { IntOffset(padPx.roundToInt(), 0) }
                                .size(knobSize)
                                .graphicsLayer {
                                    scaleX = ringScale
                                    scaleY = ringScale
                                    alpha = (0.9f * (1f - ringProgress.value))
                                }
                                .border(
                                    2.5.dp,
                                    colors.accent,
                                    CircleShape,
                                ),
                    )
                }

                // Flecha sobre el fill lima (sin bola propia enfrente, perfectamente centrada).
                Box(
                    modifier =
                        Modifier
                            .offset { IntOffset((knobX + padPx).roundToInt(), 0) }
                            .size(knobSize),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isDone) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = colors.ink,
                            modifier = Modifier.size(24.dp),
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = colors.ink,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAccessCard(
    onOpenQrScanner: () -> Unit,
    onUseCode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(colors.paper2)
                .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Outlined.Bolt,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(16.dp),
            )
            Text(
                "ACCESO RÁPIDO EN MESA",
                color = colors.muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.3.sp,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.paper)
                        .border(1.dp, colors.line, RoundedCornerShape(14.dp))
                        .physicalPress(onClick = onOpenQrScanner),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Outlined.QrCodeScanner,
                        contentDescription = null,
                        tint = colors.ink,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        "Escanear QR",
                        color = colors.ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.paper)
                        .border(1.dp, colors.line, RoundedCornerShape(14.dp))
                        .physicalPress(onClick = onUseCode),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Outlined.Tag,
                        contentDescription = null,
                        tint = colors.ink,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        "Usar código",
                        color = colors.ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun VenueEmptyState(
    query: String,
    onClearQuery: () -> Unit,
    onOpenQrScanner: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.paper2)
                .border(1.dp, colors.line, RoundedCornerShape(20.dp))
                .padding(18.dp),
    ) {
        Text(
            "Sin coincidencias",
            color = colors.ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            if (query.isBlank()) {
                "Todavía no hay cafeterías disponibles. Intenta de nuevo en un momento."
            } else {
                "No encontramos “$query”. Revisa la escritura o usa el QR de tu mesa."
            },
            color = colors.muted,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 6.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (query.isNotBlank()) {
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(colors.paper)
                            .border(1.dp, colors.line, RoundedCornerShape(15.dp))
                            .physicalPress(onClick = onClearQuery),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Limpiar búsqueda", color = colors.ink, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(colors.accent)
                        .physicalPress(onClick = onOpenQrScanner),
                contentAlignment = Alignment.Center,
            ) {
                Text("Escanear QR", color = colors.accentInk, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun VenueNoticeCard(
    message: String,
    tone: Color,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Text(
        message,
        color = colors.ink,
        fontSize = 12.5.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(tone.copy(alpha = 0.14f))
                .border(1.dp, tone.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
    )
}

@Composable
private fun DiscoverySearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.paper2)
                .border(1.dp, colors.line, RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.Search,
            contentDescription = null,
            tint = colors.ink2,
            modifier = Modifier.size(21.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                Modifier
                    .padding(start = 11.dp)
                    .weight(1f)
                    .semantics { contentDescription = "Buscar cafetería, facultad o campus" },
            singleLine = true,
            textStyle =
                TextStyle(
                    color = colors.ink,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Bold,
                ),
            cursorBrush = SolidColor(colors.ink),
            decorationBox = { input ->
                Box {
                    if (value.isBlank()) {
                        Text(
                            "Buscar cafetería, facultad o campus…",
                            color = colors.muted,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    input()
                }
            },
        )
        if (value.isNotBlank()) {
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(colors.paper)
                        .clickable { onValueChange("") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Limpiar búsqueda",
                    tint = colors.ink,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun SpaceCodeSheet(
    token: String,
    resolving: Boolean,
    showError: Boolean,
    onTokenChange: (String) -> Unit,
    onCancel: () -> Unit,
    onResolve: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val coroutineScope = rememberCoroutineScope()
    val dragOffsetY = remember { Animatable(0f) }
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 90.dp.toPx() }

    // Preview del back predictivo, igual que la ficha de producto: la hoja sigue
    // al dedo y el scrim se abre para asomar discovery; al soltar se cierra.
    var backProgress by remember { mutableFloatStateOf(0f) }
    var backSettleJob by remember { mutableStateOf<Job?>(null) }
    val backShiftPx = with(density) { 200.dp.toPx() }
    PredictiveBackHandler(enabled = true) { events ->
        backSettleJob?.cancel()
        try {
            events.collect { event ->
                backProgress = event.progress
            }
            backProgress = 0f
            onCancel()
        } catch (e: CancellationException) {
            backSettleJob =
                coroutineScope.launch {
                    Animatable(backProgress).animateTo(
                        targetValue = 0f,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 600f),
                    ) { backProgress = value }
                }
            throw e
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = (0.54f * (1f - backProgress * 0.9f)).coerceIn(0f, 1f)))
                .clickable(onClick = onCancel),
    ) {
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .offset {
                        IntOffset(0, (dragOffsetY.value + backShiftPx * backProgress).toInt())
                    }.clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(colors.paper)
                    .clickable(enabled = false) {}
                    .navigationBarsPadding()
                    .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 22.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = {},
                                onDragEnd = {
                                    if (dragOffsetY.value > dismissThresholdPx) {
                                        onCancel()
                                    } else {
                                        coroutineScope.launch {
                                            dragOffsetY.animateTo(
                                                0f,
                                                spring(dampingRatio = 0.82f, stiffness = 450f),
                                            )
                                        }
                                    }
                                },
                                onDragCancel = {
                                    coroutineScope.launch {
                                        dragOffsetY.animateTo(
                                            0f,
                                            spring(dampingRatio = 0.82f, stiffness = 450f),
                                        )
                                    }
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    val newOffset = (dragOffsetY.value + dragAmount).coerceAtLeast(0f)
                                    coroutineScope.launch {
                                        dragOffsetY.snapTo(newOffset)
                                    }
                                },
                            )
                        },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(width = 42.dp, height = 5.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(colors.paper2),
                )
            }
            Text(
                "Ingresa el código de mesa",
                color = colors.ink,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                "Escribe los dígitos que aparecen en el tent card de tu mesa.",
                color = colors.muted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )

            // Slot display
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (index in 0 until 3) {
                        val char =
                            when {
                                index < token.length -> token[index].toString()
                                index == token.length -> "•"
                                else -> ""
                            }
                        val isFilled = index < token.length
                        val isCurrent = index == token.length
                        Box(
                            modifier =
                                Modifier
                                    .size(width = 50.dp, height = 58.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isFilled) {
                                            if (colors.isDark) colors.paper2 else Color.White
                                        } else {
                                            colors.paper2
                                        },
                                    )
                                    .border(
                                        width = if (isFilled || isCurrent) 2.dp else 1.dp,
                                        color = if (isFilled) colors.ink else if (isCurrent) colors.accent else colors.line,
                                        shape = RoundedCornerShape(14.dp),
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                char,
                                color = colors.ink,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
            }

            if (showError) {
                Text(
                    "Escribe un código para continuar.",
                    color = colors.coral,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                )
            }

            val canResolve = token.isNotBlank() && !resolving
            val haptics = rememberVaiinillaHaptics()

            // Keypad 3x4
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                ).forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowKeys.forEach { digit ->
                            Box(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.paper2)
                                        .physicalPress {
                                            haptics.click()
                                            if (token.length < 8) onTokenChange(token + digit)
                                        },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    digit,
                                    color = colors.ink,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Borrar / Clear
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.paper2)
                                .physicalPress {
                                    haptics.click()
                                    if (token.isNotEmpty()) onTokenChange(token.dropLast(1))
                                },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "BORRAR",
                            color = colors.muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    // "0"
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.paper2)
                                .physicalPress {
                                    haptics.click()
                                    if (token.length < 8) onTokenChange(token + "0")
                                },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "0",
                            color = colors.ink,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    // OK / Listo
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.accent)
                                .physicalPress(enabled = canResolve) {
                                    haptics.click()
                                    if (canResolve) onResolve()
                                },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "LISTO",
                            color = colors.accentInk,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // Confirmar y sentarme
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .height(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (canResolve) colors.ink else colors.ink.copy(alpha = 0.4f))
                        .physicalPress(enabled = canResolve, onClick = onResolve),
                contentAlignment = Alignment.Center,
            ) {
                if (resolving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = colors.paper,
                    )
                } else {
                    Text(
                        "Confirmar y sentarme",
                        color = colors.paper,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onCancel)
                        .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Cancelar",
                    color = colors.muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Preview(name = "Elegir cafetería", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun DiscoveryScreenPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        DiscoveryScreen(
            state = DiscoveryUiState(),
            onQueryChange = {},
            onSpaceTokenChange = {},
            onSelectEstablishment = {},
            onResolveSpace = {},
            onConfirmSwitch = {},
            onDismissSwitch = {},
            onContinueSelected = {},
        )
    }
}
