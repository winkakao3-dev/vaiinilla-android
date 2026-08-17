package com.vaiinilla.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
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
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.VaiinillaMark
import com.vaiinilla.app.ui.components.VenueCardSkeleton
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.discovery.DiscoveryUiState
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val focusManager = LocalFocusManager.current
    var codeSheetOpen by remember { mutableStateOf(false) }
    var tokenError by remember { mutableStateOf(false) }
    val selectedId = state.selected?.establishment?.id
    val cafeCount = state.establishments.size
    val bottomClearance = if (state.selected != null) 116.dp else 40.dp

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.paper)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                },
    ) {
        PullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = {
                haptics.impact()
                onQueryChange(state.query)
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = bottomClearance),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                item {
                    DiscoveryBrandRow(
                        initials = profileInitials,
                        onOpenAccount = onOpenAccount,
                    )
                }
                item {
                    Column(modifier = Modifier.padding(horizontal = 2.dp, vertical = 0.dp).padding(bottom = 24.dp)) {
                        Text(
                            "Antes de pedir",
                            color = colors.muted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.6.sp,
                        )
                        Text(
                            "¿Dónde comes hoy?",
                            color = colors.ink,
                            fontSize = 36.sp,
                            lineHeight = 40.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-2).sp,
                            modifier = Modifier.padding(top = 8.dp, bottom = 10.dp),
                        )
                        Text(
                            "Elige tu cafetería o escanea el QR del espacio para abrir el menú correcto.",
                            color = colors.muted,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                state.selected?.let { selected ->
                    item {
                        ActiveVenueCard(
                            name = selected.establishment.name,
                            onContinue = {
                                haptics.impact()
                                onContinueSelected()
                            },
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                    }
                }
                item {
                    DiscoverySearchField(
                        value = state.query,
                        onValueChange = onQueryChange,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 30.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        QuickAccessCard(
                            modifier = Modifier.weight(1f),
                            ink = true,
                            icon = Icons.Outlined.QrCodeScanner,
                            title = "Escanear QR",
                            subtitle = "Del comedor o mesa",
                            onClick = {
                                haptics.click()
                                onOpenQrScanner()
                            },
                        )
                        QuickAccessCard(
                            modifier = Modifier.weight(1f),
                            ink = false,
                            icon = Icons.AutoMirrored.Outlined.Notes,
                            title = "Usar código",
                            subtitle = "Token del espacio",
                            onClick = {
                                haptics.selection()
                                tokenError = false
                                codeSheetOpen = true
                            },
                        )
                    }
                }
                if (state.suspendedMessage != null) {
                    item {
                        Text(
                            state.suspendedMessage,
                            color = colors.coral,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                }
                if (state.errorMessage != null) {
                    item {
                        Text(
                            state.errorMessage,
                            color = colors.coral,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                }
                item {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 2.dp,
                                    vertical = 0.dp,
                                ).padding(bottom = 12.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            "Cafeterías",
                            color = colors.ink,
                            fontSize = 20.sp,
                            lineHeight = 28.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            if (cafeCount == 1) "1 disponible" else "$cafeCount disponibles",
                            color = colors.muted,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (state.loading && state.establishments.isEmpty()) {
                    items(3) {
                        VenueCardSkeleton(modifier = Modifier.padding(bottom = 10.dp))
                    }
                }
                if (!state.loading && state.establishments.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.paper2,
                            shape = RoundedCornerShape(24.dp),
                        ) {
                            Text(
                                "No encontramos una cafetería con ese nombre.",
                                color = colors.muted,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(28.dp, 28.dp),
                            )
                        }
                    }
                }
                items(state.establishments, key = { it.id }) { establishment ->
                    EstablishmentCard(
                        establishment = establishment,
                        selected = establishment.id == selectedId,
                        onClick = {
                            haptics.click()
                            onSelectEstablishment(establishment)
                        },
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }
            }
        }

        if (state.selected != null) {
            DiscoveryBottomBar(
                name = state.selected.establishment.name,
                onContinue = onContinueSelected,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
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
                        tokenError = false
                        codeSheetOpen = false
                        onResolveSpace()
                    }
                },
            )
        }

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
    }
}

@Composable
private fun DiscoveryBrandRow(
    initials: String,
    onOpenAccount: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VaiinillaMark(
            modifier = Modifier.size(36.dp),
        )
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text("Vaiinilla", color = colors.ink, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                "Comedor conectado",
                color = colors.muted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.paper2)
                    .physicalPress(scale = PhysicalPressScale.Small, onClick = onOpenAccount),
            contentAlignment = Alignment.Center,
        ) {
            Text(initials, color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ActiveVenueCard(
    name: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(176.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(colors.accent),
    ) {
        Text(
            "V",
            modifier = Modifier.align(Alignment.TopEnd).padding(end = 10.dp),
            color = colors.accentInk.copy(alpha = 0.08f),
            fontSize = 148.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-12).sp,
            maxLines = 1,
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        "Activa ahora",
                        color = colors.accentInk.copy(alpha = 0.66f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp,
                    )
                    Text(
                        name,
                        color = colors.accentInk,
                        fontSize = 30.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1.2).sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Surface(color = Color.White.copy(alpha = 0.48f), shape = RoundedCornerShape(99.dp)) {
                    Text(
                        "Lista para pedir",
                        color = colors.accentInk,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }
            ContinueInkButton(label = "Seguir al menú", onClick = onContinue)
        }
    }
}

@Composable
private fun ContinueInkButton(
    label: String,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.ink)
                .physicalPress(onClick = onClick)
                .padding(start = 18.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = colors.paper,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = colors.paper,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

@Composable
private fun DiscoverySearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Box(modifier = modifier.fillMaxWidth().height(58.dp)) {
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.paper2)
                    .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.muted, modifier = Modifier.size(20.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                        .semantics { contentDescription = "Buscar cafetería" },
                singleLine = true,
                textStyle = TextStyle(color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                cursorBrush = SolidColor(colors.ink),
                decorationBox = { input ->
                    Box {
                        if (value.isBlank()) {
                            Text(
                                "Buscar cafetería",
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
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.ink.copy(alpha = 0.07f))
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
}

@Composable
private fun QuickAccessCard(
    modifier: Modifier,
    ink: Boolean,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val bg = if (ink) colors.ink else colors.paper2
    val fg = if (ink) colors.paper else colors.ink
    val iconBadgeBg = if (ink) colors.accent else colors.paper
    val iconTint = if (ink) colors.accentInk else colors.ink

    Column(
        modifier =
            modifier
                .height(126.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(bg)
                .physicalPress(onClick = onClick)
                .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier =
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(iconBadgeBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        }
        Column {
            Text(title, color = fg, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                color = fg.copy(alpha = 0.68f),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@Composable
private fun EstablishmentCard(
    establishment: PublicEstablishment,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val meta =
        if (establishment.clientIdRequired) {
            "${establishment.clientIdLabel} requerida al pedir"
        } else {
            "${establishment.clientIdLabel} opcional"
        }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(82.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(if (selected) colors.accent2.copy(alpha = 0.35f) else colors.paper2)
                .then(
                    if (selected) {
                        Modifier.border(2.dp, colors.accent.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                    } else {
                        Modifier
                    },
                ).physicalPress(onClick = onClick)
                .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.paper),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Storefront,
                contentDescription = null,
                tint = colors.ink,
                modifier = Modifier.size(25.dp),
            )
        }
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    establishment.name,
                    color = colors.ink,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (selected) {
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
            Text(
                meta,
                color = colors.muted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.paper),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = colors.ink,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

@Composable
private fun DiscoveryBottomBar(
    name: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.paper)
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.accent)
                    .physicalPress(onClick = onContinue)
                    .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Cafetería activa",
                    color = colors.accentInk.copy(alpha = 0.70f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    name,
                    color = colors.accentInk,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.accentInk.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = colors.accentInk,
                    modifier = Modifier.size(17.dp),
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

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.54f))
                .clickable(onClick = onCancel),
    ) {
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .offset {
                        IntOffset(0, dragOffsetY.value.toInt())
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
                "Código del espacio",
                color = colors.ink,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                "Escribe el token que aparece junto al QR del comedor, mesa o cancha.",
                color = colors.muted,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 7.dp, bottom = 18.dp),
            )
            BasicTextField(
                value = token,
                onValueChange = onTokenChange,
                singleLine = true,
                textStyle = TextStyle(color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                cursorBrush = SolidColor(colors.ink),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.paper2)
                        .border(1.dp, colors.line, RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp),
                decorationBox = { input ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (token.isBlank()) {
                            Text("Ej. patio norte 04", color = colors.muted, fontSize = 16.sp)
                        }
                        input()
                    }
                },
            )
            if (showError) {
                Text(
                    "Escribe un código para continuar.",
                    color = colors.coral,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 9.dp, start = 3.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(width = 96.dp, height = 54.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.paper2)
                            .physicalPress(onClick = onCancel),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Cancelar", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.ink)
                }
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.accent)
                            .physicalPress(enabled = !resolving, onClick = onResolve),
                    contentAlignment = Alignment.Center,
                ) {
                    if (resolving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = colors.ink,
                        )
                    } else {
                        Text("Resolver espacio", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.ink)
                    }
                }
            }
        }
    }
}

@Preview(name = "Descubrir cafetería", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun DiscoveryScreenPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        DiscoveryScreen(
            state =
                DiscoveryUiState(
                    establishments =
                        listOf(
                            PublicEstablishment("1", "saulP1", "saulp1", "Matrícula", true),
                            PublicEstablishment("2", "America", "america", "Matrícula", true),
                        ),
                ),
            onQueryChange = {},
            onSpaceTokenChange = {},
            onSelectEstablishment = {},
            onResolveSpace = {},
            onConfirmSwitch = {},
            onDismissSwitch = {},
            onContinueSelected = {},
            profileInitials = "DR",
        )
    }
}
