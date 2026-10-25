package com.vaiinilla.app.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.vaiinilla.app.core.permissions.openAppSettings
import com.vaiinilla.app.domain.guardianship.SchoolJoinCodeRules
import com.vaiinilla.app.domain.mode.MySchool
import com.vaiinilla.app.domain.mode.MySchools
import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.domain.model.PublicVideo
import com.vaiinilla.app.ui.components.ChoiceChip
import com.vaiinilla.app.ui.components.EditorialSearchField
import com.vaiinilla.app.ui.components.EditorialSecondaryButton
import com.vaiinilla.app.ui.components.GlassCircleButton
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ProfileInitialsButton
import com.vaiinilla.app.ui.components.SlidingSegments
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.discovery.DiscoveryUiState
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.SeaGlassButton
import com.vaiinilla.app.ui.theme.SeaGlassTone
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun DiscoveryScreen(
    state: DiscoveryUiState,
    onQueryChange: (String) -> Unit,
    onSpaceTokenChange: (String) -> Unit,
    onOpenQrScanner: () -> Unit = {},
    /** Sube cuando otra pantalla (el escáner sin cámara) pide escribir el código de mesa. */
    tableCodeRequest: Int = 0,
    onSelectEstablishment: (PublicEstablishment) -> Unit,
    onResolveSpace: () -> Unit,
    onContinueSelected: () -> Unit,
    profileInitials: String = "?",
    onOpenAccount: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    onEnableNearby: () -> Unit = {},
    onNearbyPermissionDenied: () -> Unit = {},
    onDisableNearby: () -> Unit = {},
    nfcAvailable: Boolean = rememberHasNfcReader(),
    onOpenSchool: (MySchool) -> Unit = {},
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val nearbyPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) onEnableNearby() else onNearbyPermissionDenied()
        }
    val onToggleNearby = {
        haptics.selection()
        when {
            state.nearbyOrigin != null -> onDisableNearby()
            state.locating -> Unit
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED -> onEnableNearby()
            else -> nearbyPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }
    var codeSheetOpen by remember { mutableStateOf(false) }
    LaunchedEffect(tableCodeRequest) {
        if (tableCodeRequest > 0) codeSheetOpen = true
    }
    var tokenError by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(VenueFilter.ALL) }

    val filteredEstablishments =
        remember(state.establishments, selectedFilter) {
            state.establishments.filter { est ->
                when (selectedFilter) {
                    VenueFilter.ALL -> true
                    VenueFilter.FREE -> !est.clientIdRequired
                    VenueFilter.ID_REQUIRED -> est.clientIdRequired
                }
            }
        }

    val activeId = state.selected?.establishment?.id
    val enterVenue: (PublicEstablishment) -> Unit = { target ->
        haptics.click()
        if (target.id != activeId) onSelectEstablishment(target) else onContinueSelected()
    }
    // Con un solo lugar no hay nada que elegir: se entra directo, una sola vez (al volver no rebota).
    var autoOpened by rememberSaveable { mutableStateOf(false) }
    val onlyVenue =
        onlyVenueToAutoOpen(
            establishments = state.establishments,
            query = state.query,
            loading = state.loading,
            hasActiveVenue = state.selected != null,
        )
    LaunchedEffect(onlyVenue?.id) {
        if (onlyVenue != null && !autoOpened) {
            autoOpened = true
            onSelectEstablishment(onlyVenue)
        }
    }
    // Los videos viven en la lista: el venue guardado en sesión no los trae.
    val activeVideos =
        state.establishments
            .firstOrNull { it.id == activeId }
            ?.localVideos
            .orEmpty()

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
                    .statusBarsPadding()
                    // Con la hoja del código abierta, lo de atrás queda bajo su velo: TalkBack tampoco lo recorre.
                    .then(if (codeSheetOpen) Modifier.clearAndSetSemantics {} else Modifier),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    GlassCircleButton(
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Volver",
                        onClick = {
                            haptics.click()
                            onBack()
                        },
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    "Dónde pedir",
                    color = colors.ink,
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f),
                )
                ProfileInitialsButton(
                    initials = profileInitials,
                    onClick = {
                        haptics.click()
                        onOpenAccount()
                    },
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding =
                    PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 0.dp,
                        bottom = 32.dp,
                    ),
            ) {
                val otherSchools = MySchools.others(state.mySchools, state.selected?.establishment?.id)
                if (otherSchools.isNotEmpty()) {
                    item(key = "my-schools") {
                        DiscoverySchoolsRow(
                            schools = otherSchools,
                            openingSchoolId = state.openingSchoolId,
                            onOpen = onOpenSchool,
                            modifier = Modifier.padding(top = 6.dp, bottom = 8.dp).animateItem(),
                        )
                    }
                }
                item {
                    EditorialSearchField(
                        value = state.query,
                        onValueChange = onQueryChange,
                        placeholder = "Buscar cafetería, facultad o campus…",
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                item {
                    FilterChipsBar(
                        selectedFilter = selectedFilter,
                        onSelectFilter = { selectedFilter = it },
                        nearbyActive = state.nearbyOrigin != null,
                        nearbyLocating = state.locating,
                        onToggleNearby = onToggleNearby,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                state.locationMessage?.let { message ->
                    item {
                        VenueNoticeCard(
                            message = message,
                            tone = colors.muted,
                            modifier = Modifier.padding(top = 14.dp),
                        )
                        if (
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            TextButton(onClick = { openAppSettings(context) }) { Text("Abrir ajustes") }
                        }
                    }
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
                    // Sin lugares listados basta la fila de QR, código y NFC de abajo; solo una búsqueda sin
                    // resultados lleva su aviso.
                    if (state.query.isNotBlank()) {
                        item {
                            NoMatches(
                                query = state.query,
                                onClearQuery = { onQueryChange("") },
                                modifier = Modifier.padding(top = 18.dp),
                            )
                        }
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
                    items(filteredEstablishments, key = { it.id }) { establishment ->
                        VenueRow(
                            establishment = establishment,
                            active = activeId == establishment.id,
                            onClick = { enterVenue(establishment) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
                if (activeVideos.isNotEmpty()) {
                    item {
                        LocalVideosSection(
                            videos = activeVideos,
                            modifier = Modifier.padding(top = 20.dp),
                        )
                    }
                }
                item {
                    QuickAccessRow(
                        onOpenQrScanner = onOpenQrScanner,
                        onUseCode = {
                            haptics.click()
                            codeSheetOpen = true
                        },
                        nfcAvailable = nfcAvailable,
                        modifier = Modifier.padding(top = 24.dp),
                    )
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
                        codeSheetOpen = false
                        onResolveSpace()
                    }
                },
            )
        }
    }
}

@Composable
private fun LocalVideosSection(
    videos: List<PublicVideo>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val context = LocalContext.current
    val haptics = rememberVaiinillaHaptics()
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeading(label = "VIDEOS DEL LOCAL")
        videos.forEach { video ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.paper2)
                        .border(1.dp, colors.line, RoundedCornerShape(20.dp))
                        .physicalPress(onClick = {
                            haptics.click()
                            openExternalVideo(context, video.url)
                        })
                        .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    video.title,
                    color = colors.ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = colors.muted,
                    modifier = Modifier.padding(start = 10.dp).size(18.dp),
                )
            }
        }
    }
}

private fun openExternalVideo(
    context: Context,
    url: String,
) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Sin app que abra enlaces web no hay nada que mostrar.
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

enum class VenueFilter(
    val label: String,
) {
    ALL("Todas"),
    FREE("Acceso libre"),
    ID_REQUIRED("Con matrícula"),
}

@Composable
private fun FilterChipsBar(
    selectedFilter: VenueFilter,
    onSelectFilter: (VenueFilter) -> Unit,
    nearbyActive: Boolean,
    nearbyLocating: Boolean,
    onToggleNearby: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = 2.dp, end = 2.dp, top = 10.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ChoiceChip(
            label = if (nearbyLocating) "Ubicándote…" else "Cerca de mí",
            selected = nearbyActive,
            onClick = onToggleNearby,
            height = 48.dp,
            seaGlass = true,
        )
        VenueFilter.entries.forEach { filter ->
            ChoiceChip(
                label = filter.label,
                selected = filter == selectedFilter,
                onClick = {
                    haptics.selection()
                    onSelectFilter(filter)
                },
                height = 48.dp,
                seaGlass = true,
            )
        }
    }
}

/** Un lugar de la lista: toda la fila entra. Solo datos reales; sin tiempos ni distancias inventadas. */
@Composable
private fun VenueRow(
    establishment: PublicEstablishment,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val access =
        if (establishment.clientIdRequired) "${establishment.clientIdLabel} requerida" else "Acceso libre"
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .physicalPress(scale = PhysicalPressScale.ProductCard, onClick = onClick)
                .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.paper2),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                establishment.name.take(1).uppercase(),
                color = colors.muted,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
            )
        }
        Column(modifier = Modifier.padding(horizontal = 14.dp).weight(1f)) {
            Text(
                establishment.name,
                color = colors.ink,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (active) "Aquí estás · $access" else access,
                color = colors.muted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.muted,
        )
    }
}

/** Con un solo lugar disponible (sin buscar y sin lugar activo) no hay nada que elegir. */
internal fun onlyVenueToAutoOpen(
    establishments: List<PublicEstablishment>,
    query: String,
    loading: Boolean,
    hasActiveVenue: Boolean,
): PublicEstablishment? = establishments.singleOrNull()?.takeIf { !loading && query.isBlank() && !hasActiveVenue }

/**
 * ¿Ya estás en la mesa? QR o código en dos tarjetas grandes y NFC a lo ancho. Va debajo de la lista, o sola
 * cuando no hay lugares listados (el negocio entra solo por QR o código).
 */
@Composable
private fun QuickAccessRow(
    onOpenQrScanner: () -> Unit,
    onUseCode: () -> Unit,
    nfcAvailable: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "¿Ya estás en tu mesa?",
            color = colors.muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuickAccessCard(
                icon = Icons.Outlined.QrCodeScanner,
                label = "Escanear QR",
                hint = "Con la cámara",
                modifier = Modifier.weight(1f),
                onClick = onOpenQrScanner,
            )
            QuickAccessCard(
                icon = Icons.Outlined.Tag,
                label = "Usar código",
                hint = "De tu mesa o tu escuela",
                modifier = Modifier.weight(1f),
                onClick = onUseCode,
            )
        }
        if (nfcAvailable) {
            NfcCard(modifier = Modifier.padding(top = 10.dp))
        }
    }
}

/** La etiqueta la lee la app sola mientras está abierta; la tarjeta explica cómo y prende el NFC si hace falta. */
@Composable
private fun NfcCard(modifier: Modifier = Modifier) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val context = LocalContext.current
    val still = reducedMotion()
    var hintShown by rememberSaveable { mutableStateOf(false) }
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 84.dp)
                .physicalPress(scale = PhysicalPressScale.Small) {
                    haptics.click()
                    openNfcSettingsIfOff(context)
                    hintShown = !hintShown
                }.clip(shape)
                .background(colors.paper2)
                .border(1.dp, colors.line, shape)
                .then(
                    if (still) {
                        Modifier
                    } else {
                        Modifier.animateContentSize(
                            spring(dampingRatio = 0.82f, stiffness = 380f),
                        )
                    },
                ),
    ) {
        Icon(
            Icons.Outlined.Nfc,
            contentDescription = null,
            tint = colors.ink.copy(alpha = 0.82f),
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-18).dp)
                    .rotate(-12f)
                    .size(52.dp),
        )
        Column(modifier = Modifier.padding(16.dp).padding(end = 84.dp)) {
            Text(
                "Acercar al NFC",
                color = colors.ink,
                fontSize = 17.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                "Si tu mesa tiene etiqueta",
                color = colors.muted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 3.dp),
            )
            if (hintShown) {
                Text(
                    "Acerca la parte de atrás de tu teléfono a la etiqueta: Vaiinilla abre tu mesa sola.",
                    color = colors.ink,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

/**
 * Tarjeta alta con el título arriba y el ícono en grande saliéndose por la esquina, como las
 * tarjetas de entrada de Swiggy: se reconoce de lejos qué hace cada una sin leer.
 */
@Composable
private fun QuickAccessCard(
    icon: ImageVector,
    label: String,
    hint: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier =
            modifier
                .heightIn(min = 172.dp)
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
                .clip(shape)
                .background(colors.paper2)
                .border(1.dp, colors.line, shape),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = colors.ink.copy(alpha = 0.82f),
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 14.dp, y = 16.dp)
                    .rotate(-12f)
                    .size(104.dp),
        )
        Column(modifier = Modifier.padding(16.dp).padding(end = 4.dp)) {
            Text(
                label,
                color = colors.ink,
                fontSize = 17.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                hint,
                color = colors.muted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@Composable
private fun NoMatches(
    query: String,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "Sin coincidencias",
            color = colors.ink,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            "No encontramos “$query” en la lista. Si ya estás ahí, escanea su QR o escribe el código.",
            color = colors.muted,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 6.dp),
        )
        EditorialSecondaryButton(
            text = "Limpiar búsqueda",
            onClick = onClearQuery,
            modifier = Modifier.padding(top = 12.dp),
        )
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

/** Tecla plana del código de mesa: el teclado es herramienta, no acción; no lleva vidrio ni color. */
@Composable
private fun KeypadKey(
    modifier: Modifier = Modifier,
    description: String? = null,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Box(
        modifier =
            modifier
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.paper2)
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
                .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.ink) { content() }
    }
}

/** Largo de los códigos de mesa impresos junto al QR. */
internal const val TABLE_CODE_LENGTH = 4

/** Se valida solo al completar el código: una vez por código, nunca mientras ya se está validando. */
internal fun shouldAutoResolveTableCode(
    previous: String,
    next: String,
    resolving: Boolean,
): Boolean = !resolving && previous.length < TABLE_CODE_LENGTH && next.length == TABLE_CODE_LENGTH

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
    // onResolve lee el código del estado de la pantalla: se llama ya recompuesto con el dígito nuevo.
    val latestResolve by rememberUpdatedState(onResolve)
    var previousToken by remember { mutableStateOf(token) }
    // Mesa | Escuela: el de la escuela son letras (SANDBX) y viaja como codigo_union al unirse.
    var school by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(token) {
        if (!school && shouldAutoResolveTableCode(previousToken, token, resolving)) latestResolve()
        previousToken = token
    }

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
                if (school) "Ingresa el código de tu escuela" else "Ingresa el código de mesa",
                color = colors.ink,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                if (school) {
                    "Son las letras que la escuela le dio a los tutores, por ejemplo SANDBX."
                } else {
                    "Escribe los 4 dígitos que aparecen junto al QR de tu mesa."
                },
                color = colors.muted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )

            SlidingSegments(
                labels = listOf("Mesa", "Escuela"),
                selectedIndex = if (school) 1 else 0,
                onSelect = {
                    school = it == 1
                    onTokenChange("")
                },
                trackColor = colors.paper2,
                indicatorColor = colors.ink,
                selectedContentColor = colors.onInk,
                idleContentColor = colors.ink,
                enabled = !resolving,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
            if (school) {
                SchoolCodeField(token = token, enabled = !resolving, onTokenChange = onTokenChange, onDone = onResolve)
            }

            val canResolve = !resolving && if (school) SchoolJoinCodeRules.isComplete(token) else token.isNotBlank()

            // Slot display
            if (!school) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    // Los códigos son de 4 dígitos (hasta 8 si algún día crecen): siempre se ven todos.
                    // Con más de 5 casillas se angostan para que 8 quepan en un teléfono de 360dp.
                    val slots = (token.length + 1).coerceIn(4, 8)
                    val slotWidth = if (slots <= 5) 50.dp else 34.dp
                    Row(horizontalArrangement = Arrangement.spacedBy(if (slots <= 5) 10.dp else 5.dp)) {
                        for (index in 0 until slots) {
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
                                        .size(width = slotWidth, height = 58.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isFilled) {
                                                if (colors.isDark) colors.paper2 else Color.White
                                            } else {
                                                colors.paper2
                                            },
                                        ).border(
                                            width = if (isFilled || isCurrent) 2.dp else 1.dp,
                                            color =
                                                if (isFilled) {
                                                    colors.ink
                                                } else if (isCurrent) {
                                                    colors.muted
                                                } else {
                                                    colors.line
                                                },
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

                val haptics = rememberVaiinillaHaptics()

                // Keypad 3x4
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
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
                                KeypadKey(Modifier.weight(1f), onClick = {
                                    haptics.click()
                                    if (token.length < 8) onTokenChange(token + digit)
                                }) { Text(digit, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Spacer(Modifier.weight(1f))
                        KeypadKey(Modifier.weight(1f), onClick = {
                            haptics.click()
                            if (token.length < 8) onTokenChange(token + "0")
                        }) { Text("0", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                        KeypadKey(Modifier.weight(1f), description = "Borrar", onClick = {
                            haptics.click()
                            if (token.isNotEmpty()) onTokenChange(token.dropLast(1))
                        }) { Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = null) }
                    }
                }
            }

            // Confirmar y sentarme
            SeaGlassButton(
                tone = SeaGlassTone.lima,
                onClick = onResolve,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(52.dp),
                enabled = canResolve,
            ) {
                if (resolving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    when {
                        resolving -> "Validando…"
                        school -> "Entrar a la escuela"
                        else -> "Confirmar y sentarme"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
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
            onContinueSelected = {},
        )
    }
}

/** El código de la escuela: letras y números sin ambiguos, hasta ocho, con el teclado del sistema. */
@Composable
private fun SchoolCodeField(
    token: String,
    enabled: Boolean,
    onTokenChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    BasicTextField(
        value = token,
        onValueChange = { onTokenChange(SchoolJoinCodeRules.sanitized(it)) },
        enabled = enabled,
        singleLine = true,
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Go,
            ),
        keyboardActions = KeyboardActions(onGo = { if (SchoolJoinCodeRules.isComplete(token)) onDone() }),
        textStyle =
            TextStyle(
                color = colors.ink,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center,
            ),
        cursorBrush = SolidColor(colors.ink),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .focusRequester(focus)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.paper2)
                .border(1.dp, colors.line, RoundedCornerShape(16.dp))
                .padding(vertical = 14.dp)
                .semantics { contentDescription = "Código de la escuela" },
    )
}
