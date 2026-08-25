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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.draw.rotate
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
    var showAllVenues by remember { mutableStateOf(false) }

    val selectedId = state.selected?.establishment?.id
    val cafeCount = state.establishments.size
    val showVenueResults = showAllVenues || state.query.isNotBlank()
    val quickVenues =
        buildList {
            state.selected?.establishment?.let(::add)
            state.establishments.forEach { establishment ->
                if (none { it.id == establishment.id }) add(establishment)
            }
        }.take(2)

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
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                item {
                    DiscoveryBrandRow(
                        initials = profileInitials,
                        onOpenAccount = onOpenAccount,
                    )
                }
                item {
                    Column(
                        modifier =
                            Modifier
                                .padding(horizontal = 2.dp)
                                .padding(bottom = 20.dp),
                    ) {
                        Text(
                            "ANTES DE PEDIR",
                            color = colors.accentInk.copy(alpha = if (colors.isDark) 0.88f else 0.78f),
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.2.sp,
                        )
                        Text(
                            "¿Dónde comes hoy?",
                            color = colors.ink,
                            fontSize = 36.sp,
                            lineHeight = 40.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1.9).sp,
                            modifier = Modifier.padding(top = 7.dp, bottom = 7.dp),
                        )
                        Text(
                            if (state.selected != null) {
                                "Continúa en tu cafetería activa o cambia de espacio cuando lo necesites."
                            } else {
                                "Elige tu cafetería o entra con el QR o código de tu espacio."
                            },
                            color = colors.muted,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                state.selected?.let { selected ->
                    item {
                        ActiveVenueCard(
                            name = selected.establishment.name,
                            clientIdLabel = selected.establishment.clientIdLabel,
                            clientIdRequired = selected.establishment.clientIdRequired,
                            onContinue = {
                                haptics.impact()
                                onContinueSelected()
                            },
                            modifier = Modifier.padding(bottom = 18.dp),
                        )
                    }
                }

                item {
                    DiscoverySectionHeader(
                        title = if (state.selected != null) "¿Quieres cambiar?" else "Elige tu cafetería",
                        meta = "Elige cómo",
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }
                item {
                    DiscoverySearchField(
                        value = state.query,
                        onValueChange = { query ->
                            showAllVenues = query.isNotBlank()
                            onQueryChange(query)
                        },
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
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
                        DiscoveryInlineMessage(
                            message = state.suspendedMessage,
                            error = true,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                }
                if (state.errorMessage != null && state.query.isBlank()) {
                    item {
                        DiscoveryInlineMessage(
                            message = state.errorMessage,
                            error = true,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                }

                if (quickVenues.isNotEmpty() && state.query.isBlank()) {
                    item {
                        DiscoverySectionHeader(
                            title = "Acceso rápido",
                            meta = if (quickVenues.size == 1) "1 cafetería" else "${quickVenues.size} cafeterías",
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            quickVenues.forEach { establishment ->
                                CompactVenueCard(
                                    establishment = establishment,
                                    selected = establishment.id == selectedId,
                                    onClick = {
                                        haptics.click()
                                        if (establishment.id == selectedId) {
                                            onContinueSelected()
                                        } else {
                                            onSelectEstablishment(establishment)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (quickVenues.size == 1) {
                                Box(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                item {
                    AllVenuesToggle(
                        count = cafeCount,
                        expanded = showVenueResults,
                        loading = state.loading,
                        onClick = {
                            haptics.selection()
                            focusManager.clearFocus()
                            if (state.query.isNotBlank()) {
                                onQueryChange("")
                                showAllVenues = false
                            } else {
                                showAllVenues = !showAllVenues
                            }
                        },
                        modifier = Modifier.padding(bottom = if (showVenueResults) 14.dp else 0.dp),
                    )
                }

                if (showVenueResults) {
                    if (state.loading && state.establishments.isEmpty()) {
                        items(3) {
                            VenueCardSkeleton(modifier = Modifier.padding(bottom = 10.dp))
                        }
                    } else if (!state.loading && state.establishments.isEmpty()) {
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
                                    modifier = Modifier.padding(22.dp),
                                )
                            }
                        }
                    } else {
                        items(state.establishments, key = { it.id }) { establishment ->
                            EstablishmentCard(
                                establishment = establishment,
                                selected = establishment.id == selectedId,
                                onClick = {
                                    haptics.click()
                                    if (establishment.id == selectedId) {
                                        onContinueSelected()
                                    } else {
                                        onSelectEstablishment(establishment)
                                    }
                                },
                                modifier = Modifier.padding(bottom = 10.dp),
                            )
                        }
                    }
                }
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
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
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
    clientIdLabel: String,
    clientIdRequired: Boolean,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(colors.accent),
    ) {
        repeat(2) { index ->
            Box(
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = (58 + index * 34).dp, y = (-20 + index * 54).dp)
                        .size(width = 190.dp, height = 30.dp)
                        .rotate(-24f)
                        .background(colors.accentInk.copy(alpha = 0.06f)),
            )
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Surface(
                        color = colors.accentInk.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(99.dp),
                    ) {
                        Text(
                            "CAFETERÍA ACTIVA",
                            color = colors.accentInk.copy(alpha = 0.78f),
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        )
                    }
                    Text(
                        name,
                        color = colors.accentInk,
                        fontSize = 30.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1.2).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 7.dp),
                    )
                    Text(
                        "$clientIdLabel ${if (clientIdRequired) "requerida" else "opcional"}",
                        color = colors.accentInk.copy(alpha = 0.68f),
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Surface(
                    color = Color.White.copy(alpha = 0.58f),
                    shape = RoundedCornerShape(99.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF72A52A)),
                        )
                        Text(
                            "Lista para pedir",
                            color = colors.accentInk,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            ContinueInkButton(
                label = "Seguir al menú",
                subtitle = "Entrar a $name",
                onClick = onContinue,
            )
        }
    }
}

@Composable
private fun ContinueInkButton(
    label: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(62.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(colors.ink)
                .physicalPress(onClick = onClick)
                .padding(start = 14.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color.White.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Storefront,
                contentDescription = null,
                tint = colors.paper,
                modifier = Modifier.size(19.dp),
            )
        }
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(
                label,
                color = colors.paper,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                subtitle,
                color = colors.paper.copy(alpha = 0.68f),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        Box(
            modifier =
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color.White.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = colors.paper,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun DiscoverySectionHeader(
    title: String,
    meta: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            title,
            color = colors.ink,
            fontSize = 17.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.25).sp,
        )
        Text(
            meta,
            color = colors.muted,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
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
                .height(68.dp)
                .clip(RoundedCornerShape(21.dp))
                .background(colors.paper2)
                .border(1.dp, colors.line, RoundedCornerShape(21.dp))
                .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                tint = colors.accentInk,
                modifier = Modifier.size(21.dp),
            )
        }
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Buscar cafetería" },
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
                                "Buscar otra cafetería",
                                color = colors.ink,
                                fontSize = 15.sp,
                                lineHeight = 19.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        input()
                    }
                },
            )
            Text(
                if (value.isBlank()) "Escribe el nombre del espacio" else "Buscando coincidencias",
                color = colors.muted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
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
        } else {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = colors.ink,
                modifier = Modifier.size(18.dp),
            )
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
    val background = if (ink) colors.ink else colors.paper2
    val foreground = if (ink) colors.paper else colors.ink
    val iconBadgeBackground = if (ink) colors.accent else colors.paper
    val iconTint = if (ink) colors.accentInk else colors.ink

    Column(
        modifier =
            modifier
                .heightIn(min = 124.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(background)
                .then(
                    if (!ink) Modifier.border(1.dp, colors.line, RoundedCornerShape(24.dp)) else Modifier,
                ).physicalPress(onClick = onClick)
                .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBadgeBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(21.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = foreground,
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    color = foreground.copy(alpha = 0.66f),
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (ink) Color.White.copy(alpha = 0.10f) else colors.paper),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = foreground,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

@Composable
private fun CompactVenueCard(
    establishment: PublicEstablishment,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val background = if (selected) colors.accent2.copy(alpha = 0.62f) else colors.paper2
    Box(
        modifier =
            modifier
                .height(100.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(background)
                .border(
                    1.dp,
                    if (selected) colors.accent.copy(alpha = 0.55f) else colors.line,
                    RoundedCornerShape(22.dp),
                ).physicalPress(onClick = onClick)
                .padding(12.dp),
    ) {
        if (selected) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 42.dp, y = (-10).dp)
                        .size(width = 120.dp, height = 24.dp)
                        .rotate(-24f)
                        .background(colors.accentInk.copy(alpha = 0.05f)),
            )
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(colors.paper),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Storefront,
                contentDescription = null,
                tint = colors.ink,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(end = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    establishment.name,
                    color = colors.ink,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (selected) {
                    Box(
                        modifier =
                            Modifier
                                .padding(start = 5.dp)
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF72A52A)),
                    )
                }
            }
            Text(
                if (selected) {
                    "Activa ahora"
                } else if (establishment.clientIdRequired) {
                    "${establishment.clientIdLabel} requerida"
                } else {
                    "${establishment.clientIdLabel} opcional"
                },
                color = colors.muted,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.paper),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = colors.ink,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun AllVenuesToggle(
    count: Int,
    expanded: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, colors.line, RoundedCornerShape(20.dp))
                .physicalPress(onClick = onClick)
                .semantics(mergeDescendants = true) {
                    contentDescription = if (expanded) "Ocultar cafeterías" else "Ver todas las cafeterías"
                }.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(colors.accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Storefront,
                contentDescription = null,
                tint = colors.accentInk,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            if (expanded) "Ocultar cafeterías" else "Ver todas las cafeterías",
            color = colors.ink,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 11.dp).weight(1f),
        )
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = colors.ink,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                if (count == 1) "1 disponible" else "$count disponibles",
                color = colors.muted,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Icon(
            Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = colors.ink,
            modifier = Modifier.padding(start = 8.dp).size(16.dp),
        )
    }
}

@Composable
private fun DiscoveryInlineMessage(
    message: String,
    error: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (error) colors.coral.copy(alpha = 0.12f) else colors.paper2,
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(
            message,
            color = if (error) colors.coral else colors.ink,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
        )
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
