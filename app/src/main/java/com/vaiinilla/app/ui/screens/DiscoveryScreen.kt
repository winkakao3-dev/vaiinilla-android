package com.vaiinilla.app.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.discovery.DiscoveryUiState
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import kotlinx.coroutines.launch

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
    var dockHeightPx by remember { mutableStateOf(0) }
    val dockHeight = with(LocalDensity.current) { dockHeightPx.toDp() }

    BackHandler(enabled = state.pendingSwitch != null) { onDismissSwitch() }
    BackHandler(enabled = state.pendingSwitch == null && codeSheetOpen) { codeSheetOpen = false }

    val activeId = state.selected?.establishment?.id
    val selection =
        pendingSelection
            ?: state.selected?.establishment
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
                        .height(52.dp)
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
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding =
                    PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 4.dp,
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
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Text(
                        "¿Dónde comes hoy?",
                        color = colors.ink,
                        fontFamily = VaiinillaSerif,
                        fontSize = 38.sp,
                        lineHeight = 42.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.6).sp,
                        modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
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
                state.suspendedMessage?.let { message ->
                    item {
                        VenueNoticeCard(message = message, tone = colors.coral, modifier = Modifier.padding(top = 14.dp))
                    }
                }
                state.errorMessage?.let { message ->
                    item {
                        VenueNoticeCard(message = message, tone = colors.coral, modifier = Modifier.padding(top = 14.dp))
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
                } else {
                    val recommended = state.establishments.first()
                    val others = state.establishments.filterNot { it.id == recommended.id }
                    item {
                        SectionHeading(
                            label = "RECOMENDADA PARA TI",
                            modifier = Modifier.padding(top = 26.dp),
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
                            },
                        )
                    }
                    if (others.isNotEmpty()) {
                        item {
                            SectionHeading(
                                label = "OTRAS SEDES DISPONIBLES",
                                trailing = "${others.size} activas",
                                modifier = Modifier.padding(top = 22.dp, bottom = 2.dp),
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

@Composable
private fun RecommendedVenueCard(
    establishment: PublicEstablishment,
    selected: Boolean,
    active: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(colors.paper2)
                .clickable(onClick = onClick)
                .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.paper),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Storefront,
                contentDescription = null,
                tint = colors.ink2,
                modifier = Modifier.size(30.dp),
            )
        }
        Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    establishment.name,
                    color = colors.ink,
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
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
            Text(
                if (establishment.clientIdRequired) {
                    "${establishment.clientIdLabel} requerida"
                } else {
                    "Acceso libre"
                },
                color = colors.muted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Box(
            modifier =
                Modifier
                    .padding(start = 10.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (selected) colors.accent else Color.Transparent)
                    .border(1.6.dp, if (selected) colors.accent else colors.line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = colors.accentInk,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
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
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.paper2)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
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
        Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    establishment.name,
                    color = colors.ink,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Black,
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
            Text(
                if (establishment.clientIdRequired) {
                    "${establishment.clientIdLabel} requerida"
                } else {
                    "Acceso libre"
                },
                color = colors.muted,
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Box(
            modifier =
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (selected) colors.accent else Color.Transparent)
                    .border(1.6.dp, if (selected) colors.accent else colors.line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = colors.accentInk,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

@Composable
private fun VenueDock(
    selection: PublicEstablishment?,
    isActive: Boolean,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(colors.ink)
                .padding(start = 18.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                when {
                    selection == null -> "ELIGE UNA CAFETERÍA"
                    isActive -> "CAFETERÍA ACTIVA"
                    else -> "SELECCIONADO"
                },
                color = colors.paper.copy(alpha = 0.65f),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.6.sp,
            )
            Text(
                selection?.name ?: "—",
                color = colors.paper,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Box(
            modifier =
                Modifier
                    .padding(start = 12.dp)
                    .height(50.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.accent)
                    .alpha(if (selection == null) 0.5f else 1f)
                    .then(
                        if (selection != null) {
                            Modifier.physicalPress(onClick = onContinue)
                        } else {
                            Modifier
                        },
                    )
                    .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Continuar",
                    color = colors.accentInk,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Black,
                )
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = colors.accentInk,
                    modifier = Modifier.padding(start = 7.dp).size(18.dp),
                )
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
                .clip(RoundedCornerShape(20.dp))
                .background(colors.paper2)
                .border(1.dp, colors.line, RoundedCornerShape(20.dp))
                .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.Bolt,
                contentDescription = null,
                tint = if (colors.isDark) colors.accent else colors.accentInk,
                modifier = Modifier.size(18.dp),
            )
            Text(
                "ACCESO RÁPIDO EN MESA",
                color = colors.ink,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.4.sp,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.paper)
                        .border(1.dp, colors.line, RoundedCornerShape(16.dp))
                        .physicalPress(onClick = onOpenQrScanner),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.QrCodeScanner,
                        contentDescription = null,
                        tint = colors.ink,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        "Escanear QR",
                        color = colors.ink,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(start = 7.dp),
                    )
                }
            }
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.paper)
                        .border(1.dp, colors.line, RoundedCornerShape(16.dp))
                        .physicalPress(onClick = onUseCode),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Tag,
                        contentDescription = null,
                        tint = colors.ink,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        "Usar código",
                        color = colors.ink,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(start = 7.dp),
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
                            color = colors.accentInk,
                        )
                    } else {
                        Text(
                            "Resolver espacio",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = colors.accentInk,
                        )
                    }
                }
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
