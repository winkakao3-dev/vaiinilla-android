package com.vaiinilla.app.ui.screens

import com.vaiinilla.app.core.location.HomeAddressLocator
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.outlined.NearMe
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Replay
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.guardianship.SchoolJoinCodeRules
import com.vaiinilla.app.domain.mode.MySchool
import com.vaiinilla.app.domain.mode.MySchools
import com.vaiinilla.app.domain.model.GuestVenueContext
import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.domain.model.PublicVideo
import com.vaiinilla.app.ui.components.EditorialSecondaryButton
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.ProfileInitialsButton
import com.vaiinilla.app.ui.components.SlidingSegments
import com.vaiinilla.app.ui.components.initialOf
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.discovery.DiscoveryUiState
import com.vaiinilla.app.ui.discovery.SEARCH_FAILED_MESSAGE
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.SeaGlassButton
import com.vaiinilla.app.ui.theme.SeaGlassTone
import com.vaiinilla.app.ui.theme.VaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.Normalizer
import kotlin.coroutines.cancellation.CancellationException

/**
 * Dónde pedir: la entrada a un lugar. Arriba el QR y el código de mesa, que es como llega casi todo el mundo;
 * después el último lugar donde pediste y tus escuelas. Los lugares no se listan al abrir: salen al buscar por
 * nombre. Con un solo lugar disponible se entra directo. Medidas del boceto «Dónde pedir · rediseño» de Doop.
 */
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
    onOpenSchool: (MySchool) -> Unit = {},
    onLoadMore: () -> Unit = {},
    onRetrySearch: () -> Unit = {},
    onEnableNearby: () -> Unit = {},
    onDisableNearby: () -> Unit = {},
    onNearbyDenied: () -> Unit = {},
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    var nearbyOpen by rememberSaveable { mutableStateOf(state.nearbyOrigin != null) }
    var nearbyAsk by remember { mutableStateOf(false) }
    val closeNearby = {
        nearbyOpen = false
        onDisableNearby()
    }
    BackHandler(enabled = nearbyOpen) { closeNearby() }
    val nearbyPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                nearbyOpen = true
                onEnableNearby()
            } else {
                nearbyOpen = false
                onNearbyDenied()
            }
        }
    val openNearby = {
        if (HomeAddressLocator.hasLocationPermission(context)) {
            nearbyOpen = true
            onEnableNearby()
        } else {
            nearbyAsk = true
        }
    }
    val reduced = reducedMotion()
    var codeSheetOpen by remember { mutableStateOf(false) }
    LaunchedEffect(tableCodeRequest) {
        if (tableCodeRequest > 0) codeSheetOpen = true
    }
    var tokenError by remember { mutableStateOf(false) }
    // Si se vuelve con texto buscado (o se recrea la pantalla), sigue en la búsqueda.
    var searching by rememberSaveable { mutableStateOf(state.query.isNotBlank()) }
    val closeSearch = {
        focusManager.clearFocus()
        onQueryChange("")
        searching = false
    }
    BackHandler(enabled = searching && !codeSheetOpen) { closeSearch() }

    val activeId = state.selected?.establishment?.id
    val enterVenue: (PublicEstablishment) -> Unit = { target ->
        haptics.click()
        // Desde aquí se entra sin mesa (para llevar); a la mesa se vuelve con su código o con «Estás aquí».
        if (target.id == activeId &&
            state.selected?.space == null
        ) {
            onContinueSelected()
        } else {
            onSelectEstablishment(target)
        }
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
    val otherSchools = MySchools.others(state.mySchools, activeId)
    // El lugar donde ya estás, cuando no es el de «Pediste aquí»: sin la lista abierta, es el camino de vuelta.
    val activeElsewhere = state.selected?.takeIf { it.establishment.id != state.lastPlace?.establishment?.id }
    val firstVisit = state.lastPlace == null && otherSchools.isEmpty() && state.selected == null
    // Los videos viven en la lista: el venue guardado en sesión no los trae.
    val activeVideos =
        state.establishments
            .firstOrNull { it.id == activeId }
            ?.localVideos
            .orEmpty()
    val swap = spring<Float>(dampingRatio = 0.86f, stiffness = 420f)

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
            AnimatedContent(
                targetState = searching to nearbyOpen,
                transitionSpec = {
                    if (reduced) {
                        fadeIn(snap()) togetherWith fadeOut(snap())
                    } else {
                        fadeIn(swap) togetherWith
                            fadeOut(swap)
                    }
                },
                label = "discovery-bar",
            ) { (inSearch, inNearby) ->
                if (inSearch) {
                    DiscoverySearchBar(query = state.query, onQueryChange = onQueryChange, onClose = closeSearch)
                } else {
                    DiscoveryTopBar(
                        title = if (inNearby) "Cerca de ti" else "Dónde pedir",
                        onBack = if (inNearby) closeNearby else onBack,
                        profileInitials = profileInitials,
                        onOpenAccount = {
                            haptics.click()
                            onOpenAccount()
                        },
                    )
                }
            }
            val listState = rememberLazyListState()
            // El aviso entra arriba de lo que ya se veía: sin esto la lista lo deja fuera de pantalla.
            val notice = state.suspendedMessage ?: state.errorMessage?.takeUnless { state.searchFailed }
            LaunchedEffect(notice) {
                if (notice != null) listState.animateScrollToItem(0)
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            ) {
                notices(state = state)
                if (nearbyOpen && !searching) {
                    nearbyResults(
                        state = state,
                        activeId = activeId,
                        activeSpaceName = state.selected?.space?.name,
                        onEnter = enterVenue,
                        onLoadMore = onLoadMore,
                    )
                } else if (searching) {
                    searchResults(
                        state = state,
                        activeId = activeId,
                        activeSpaceName = state.selected?.space?.name,
                        onEnter = enterVenue,
                        onRetry = onRetrySearch,
                        onLoadMore = onLoadMore,
                    )
                } else {
                    item(key = "entry") {
                        EntrySections(
                            state = state,
                            firstVisit = firstVisit,
                            activeElsewhere = activeElsewhere,
                            otherSchools = otherSchools,
                            onScan = {
                                haptics.click()
                                onOpenQrScanner()
                            },
                            onCode = {
                                haptics.click()
                                codeSheetOpen = true
                            },
                            onEnter = enterVenue,
                            onContinueActive = {
                                haptics.click()
                                onContinueSelected()
                            },
                            onOpenSchool = onOpenSchool,
                            onOpenSearch = {
                                haptics.click()
                                searching = true
                            },
                            onOpenNearby = {
                                haptics.click()
                                openNearby()
                            },
                            modifier =
                                Modifier.animateItem(
                                    fadeInSpec = if (reduced) snap() else spring(stiffness = 420f),
                                    placementSpec = null,
                                ),
                        )
                    }
                    if (activeVideos.isNotEmpty()) {
                        item(key = "videos") {
                            LocalVideosSection(videos = activeVideos, modifier = Modifier.padding(top = 24.dp))
                        }
                    }
                }
            }
        }

        if (nearbyAsk) {
            NearbyAskSheet(
                onUse = {
                    nearbyAsk = false
                    nearbyPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                },
                onDismiss = { nearbyAsk = false },
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
                        codeSheetOpen = false
                        onResolveSpace()
                    }
                },
            )
        }
    }
}

private fun LazyListScope.notices(state: DiscoveryUiState) {
    state.suspendedMessage?.let { message ->
        item(key = "suspended") { VenueNoticeCard(message = message, modifier = Modifier.padding(bottom = 14.dp)) }
    }
    // La búsqueda caída se explica junto a «Reintentar», en los resultados.
    state.errorMessage?.takeUnless { state.searchFailed }?.let { message ->
        item(key = "error") { VenueNoticeCard(message = message, modifier = Modifier.padding(bottom = 14.dp)) }
    }
}

@Composable
private fun DiscoveryTopBar(
    title: String = "Dónde pedir",
    onBack: (() -> Unit)?,
    profileInitials: String,
    onOpenAccount: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    Row(
        modifier = Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            DiscoveryCircleButton(
                description = "Volver",
                onClick = {
                    haptics.click()
                    onBack()
                },
            )
            Spacer(Modifier.width(12.dp))
        }
        Text(
            title,
            color = colors.ink,
            fontSize = 24.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.4).sp,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        ProfileInitialsButton(initials = profileInitials, onClick = onOpenAccount, flat = true)
    }
}

/** Al buscar, el buscador sube a la barra y el título se va; atrás sale de la búsqueda. */
@Composable
private fun DiscoverySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val reduced = reducedMotion()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Row(
        modifier = Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DiscoveryCircleButton(description = "Salir de la búsqueda", onClick = onClose)
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(colors.paper2)
                    .border(2.dp, colors.ink, CircleShape)
                    .padding(start = 15.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.ink, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(9.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(
                        "Buscar un lugar por nombre",
                        color = colors.muted,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Search),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(focus)
                            .testTag("guest-establishment-search")
                            .semantics { contentDescription = "Buscar un lugar por nombre" },
                )
            }
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter =
                    if (reduced) {
                        fadeIn(snap())
                    } else {
                        fadeIn() +
                            scaleIn(spring(stiffness = 600f), initialScale = 0.6f)
                    },
                exit = if (reduced) fadeOut(snap()) else fadeOut() + scaleOut(targetScale = 0.6f),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(44.dp)
                            .physicalPress(scale = PhysicalPressScale.Small) {
                                haptics.selection()
                                onQueryChange("")
                            }.semantics { contentDescription = "Borrar búsqueda" },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier.size(26.dp).clip(CircleShape).background(raisedFill(colors)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = null,
                            tint = colors.muted,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EntrySections(
    state: DiscoveryUiState,
    firstVisit: Boolean,
    activeElsewhere: GuestVenueContext?,
    otherSchools: List<MySchool>,
    onScan: () -> Unit,
    onCode: () -> Unit,
    onEnter: (PublicEstablishment) -> Unit,
    onContinueActive: () -> Unit,
    onOpenSchool: (MySchool) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenNearby: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val activeId = state.selected?.establishment?.id
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "Pide y paga desde tu cel, sin hacer fila.",
            color = colors.muted,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.discoveryRise(0),
        )
        SectionLabel("¿Ya estás en el lugar?", Modifier.discoveryRise(1))
        Row(
            modifier = Modifier.fillMaxWidth().discoveryRise(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ScanCard(tall = firstVisit, onClick = onScan, modifier = Modifier.weight(1f))
            CodeCard(tall = firstVisit, onClick = onCode, modifier = Modifier.weight(1f))
        }
        if (firstVisit) {
            FirstVisitHint(Modifier.padding(top = 12.dp).discoveryRise(3))
        }
        state.lastPlace?.let { place ->
            val here = place.establishment.id == activeId
            SectionLabel("Pediste aquí", Modifier.discoveryRise(3))
            PlaceCard(
                establishment = place.establishment,
                detail = place.summary(),
                actionTitle = if (here) "Seguir pidiendo" else "Pedir otra vez",
                again = !here,
                onClick = { onEnter(place.establishment) },
                modifier = Modifier.testTag("guest-last-place").discoveryRise(4),
            )
        }
        activeElsewhere?.let { active ->
            SectionLabel("Estás aquí", Modifier.discoveryRise(4))
            PlaceCard(
                establishment = active.establishment,
                detail = active.space?.let { "En ${it.name}" } ?: "Tu carrito te espera",
                actionTitle = "Seguir pidiendo",
                again = false,
                onClick = onContinueActive,
                modifier = Modifier.testTag("guest-active-place").discoveryRise(5),
            )
        }
        if (otherSchools.isNotEmpty()) {
            DiscoverySchoolsRow(
                schools = otherSchools,
                openingSchoolId = state.openingSchoolId,
                onOpen = onOpenSchool,
                modifier = Modifier.padding(top = 22.dp).discoveryRise(6),
            )
        }
        SearchLauncher(
            onClick = onOpenSearch,
            modifier = Modifier.padding(top = if (firstVisit) 28.dp else 24.dp).discoveryRise(7),
        )
        NearbyRow(onClick = onOpenNearby, modifier = Modifier.padding(top = 10.dp).discoveryRise(7))
    }
}

@Composable
private fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Text(
        text,
        color = colors.muted,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(top = 22.dp, bottom = 10.dp).semantics { heading() },
    )
}

@Composable
private fun FirstVisitHint(modifier: Modifier = Modifier) {
    val colors = LocalVaiinillaColors.current
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.accent.copy(alpha = 0.08f))
                .border(1.dp, colors.accent.copy(alpha = 0.35f), shape)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
        Text(
            buildAnnotatedString {
                withStyle(
                    SpanStyle(color = colors.ink, fontWeight = FontWeight.ExtraBold),
                ) { append("¿No ves un QR? ") }
                append(
                    "Pídele el código de 4 números a quien te atiende. Si tu mesa tiene una etiqueta para acercar el " +
                        "cel, también abre el menú.",
                )
            },
            color = colors.ink2,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** La acción principal: la tarjeta de tinta con el QR y una línea lima que lo recorre, como la cámara al leerlo. */
@Composable
private fun ScanCard(
    tall: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val reduced = reducedMotion()
    val shape = RoundedCornerShape(22.dp)
    val height = if (tall) 200.dp else 172.dp
    val sweep =
        if (reduced) {
            0.5f
        } else {
            val transition = rememberInfiniteTransition(label = "scan-sweep")
            val value by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Reverse),
                label = "scan-sweep-y",
            )
            value
        }
    val sweepLow = if (tall) 16.dp else 14.dp
    val sweepHigh = if (tall) 100.dp else 88.dp
    Box(
        modifier =
            modifier
                .height(height)
                .shadow(14.dp, shape, ambientColor = Color.Black, spotColor = Color.Black.copy(alpha = 0.35f))
                .clip(shape)
                .background(colors.ink)
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
                .testTag("guest-scan-qr")
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = "Escanear QR. Apunta la cámara al QR de tu mesa"
                },
    ) {
        Icon(
            Icons.Outlined.QrCode2,
            contentDescription = null,
            tint = colors.onInk,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 10.dp, bottom = 10.dp)
                    .rotate(-12f)
                    .size(if (tall) 84.dp else 74.dp),
        )
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset { IntOffset(0, -lerpDp(sweepLow, sweepHigh, sweep).roundToPx()) }
                    .padding(end = 4.dp)
                    .rotate(-12f)
                    .shadow(7.dp, CircleShape, ambientColor = colors.accent, spotColor = colors.accent)
                    .size(width = if (tall) 104.dp else 96.dp, height = 3.dp)
                    .clip(CircleShape)
                    .background(colors.accent),
        )
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Escanear QR",
                color = colors.onInk,
                fontSize = 17.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                "Apunta la cámara al QR de tu mesa",
                color = colors.onInk.copy(alpha = 0.68f),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp).width(120.dp),
            )
        }
    }
}

/** El código de 4 números: las casillas dicen qué se va a escribir antes de abrir el teclado. */
@Composable
private fun CodeCard(
    tall: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val reduced = reducedMotion()
    val shape = RoundedCornerShape(22.dp)
    var caretOn by remember { mutableStateOf(true) }
    LaunchedEffect(reduced) {
        if (reduced) return@LaunchedEffect
        // Parpadeo a pasos, como el cursor del boceto, no un desvanecido.
        while (true) {
            delay(550)
            caretOn = !caretOn
        }
    }
    Column(
        modifier =
            modifier
                .height(if (tall) 200.dp else 172.dp)
                .clip(shape)
                .background(colors.paper2)
                .border(1.dp, colors.line, shape)
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
                .padding(16.dp)
                .testTag("guest-table-code")
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = "Código de tu mesa. Los 4 números que vienen junto al QR"
                },
    ) {
        Text(
            "Código de tu mesa",
            color = colors.ink,
            fontSize = 17.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            "Los 4 números que vienen junto al QR",
            color = colors.muted,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp).width(120.dp),
        )
        Spacer(Modifier.weight(1f).heightIn(min = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("4", "8", "", "").forEachIndexed { index, digit ->
                val current = index == 2
                val box = RoundedCornerShape(if (tall) 9.dp else 8.dp)
                Box(
                    modifier =
                        Modifier
                            .size(width = if (tall) 26.dp else 24.dp, height = if (tall) 34.dp else 30.dp)
                            .clip(box)
                            .background(raisedFill(colors))
                            .border(
                                if (current) 2.dp else 1.dp,
                                when {
                                    !current -> colors.line
                                    caretOn -> colors.muted
                                    else -> Color.Transparent
                                },
                                box,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        digit,
                        color = colors.ink,
                        fontSize = if (tall) 15.sp else 14.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

/** Un lugar con su foto y un botón para volver: «Pediste aquí» y «Estás aquí». */
@Composable
private fun PlaceCard(
    establishment: PublicEstablishment,
    detail: String,
    actionTitle: String,
    again: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.paper2)
                .border(1.dp, colors.line, shape)
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
                .padding(12.dp)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = "${establishment.name}. $detail. $actionTitle"
                },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        VenueThumbnail(establishment = establishment, size = 76.dp, corner = 16.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                establishment.name,
                color = colors.ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                detail,
                color = colors.muted,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            Row(
                modifier =
                    Modifier
                        .padding(top = 9.dp)
                        .height(32.dp)
                        .clip(CircleShape)
                        .background(colors.ink)
                        .padding(horizontal = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    if (again) Icons.Rounded.Replay else Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = colors.onInk,
                    modifier = Modifier.size(14.dp),
                )
                Text(actionTitle, color = colors.onInk, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun SearchLauncher(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(CircleShape)
                .background(colors.paper2)
                .border(1.dp, colors.line, CircleShape)
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
                .padding(horizontal = 18.dp)
                .testTag("guest-open-search")
                .semantics(mergeDescendants = true) { role = Role.Button },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.ink, modifier = Modifier.size(20.dp))
        Text("Buscar un lugar por nombre", color = colors.muted, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** «Cerca de ti» de la entrada (marco DP1): abre el permiso o, si ya lo dio, la lista. */
@Composable
private fun NearbyRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
                .padding(vertical = 8.dp)
                .testTag("guest-nearby")
                .semantics(mergeDescendants = true) { role = Role.Button },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(colors.paper2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.NearMe, contentDescription = null, tint = colors.ink, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text("Cerca de ti", color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text(
                "Ordena los lugares por distancia. Pide tu ubicación solo para eso.",
                color = colors.muted,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.muted)
    }
}

/** Pedir la ubicación con su porqué antes del permiso del sistema (marco DP4). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NearbyAskSheet(
    onUse: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.paper2,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(104.dp).border(1.dp, colors.accent.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(82.dp).border(2.dp, colors.accent.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(62.dp).clip(CircleShape).background(colors.ink),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.NearMe, contentDescription = null, tint = colors.onInk, modifier = Modifier.size(26.dp))
                    }
                }
            }
            Text(
                "¿Te enseñamos lo que está cerca?",
                color = colors.ink,
                fontSize = 26.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.8).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 18.dp),
            )
            Text(
                "Ordenamos los lugares del más cercano al más lejano.",
                color = colors.muted,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            Column(Modifier.fillMaxWidth().padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    "Solo mientras tienes la app abierta.",
                    "Solo para ordenar la lista, nada más.",
                    "Si dices que no, igual puedes buscar por nombre.",
                ).forEach { line ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                        Text(line, color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Box(
                Modifier
                    .padding(top = 22.dp)
                    .fillMaxWidth()
                    .height(54.dp)
                    .physicalPress(onClick = onUse)
                    .clip(CircleShape)
                    .background(colors.accent)
                    .semantics { role = Role.Button },
                contentAlignment = Alignment.Center,
            ) {
                Text("Usar mi ubicación", color = colors.accentInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
            }
            Text(
                "Ahora no, buscar por nombre",
                color = colors.ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier =
                    Modifier
                        .padding(top = 8.dp)
                        .physicalPress(scale = PhysicalPressScale.Small, onClick = onDismiss)
                        .padding(12.dp),
            )
        }
    }
}

/** La lista de «Cerca de ti» (marco DP5). El servidor la ordena; aún no manda los metros de cada lugar. */
private fun LazyListScope.nearbyResults(
    state: DiscoveryUiState,
    activeId: String?,
    activeSpaceName: String?,
    onEnter: (PublicEstablishment) -> Unit,
    onLoadMore: () -> Unit,
) {
    item(key = "nearby-caption") {
        val colors = LocalVaiinillaColors.current
        Row(
            Modifier.padding(top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Outlined.NearMe, contentDescription = null, tint = colors.accent, modifier = Modifier.size(14.dp))
            Text(
                state.locationMessage ?: if (state.locating) "Buscando dónde estás…" else "Del más cercano al más lejano",
                color = colors.muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
    if ((state.loading || state.locating) && state.establishments.isEmpty()) {
        items(3) { VenueSkeleton() }
        return
    }
    val list = state.establishments
    itemsIndexed(list, key = { _, it -> "cerca-${it.id}" }) { index, establishment ->
        VenueRow(
            establishment = establishment,
            query = "",
            active = activeId == establishment.id,
            activeSpaceName = activeSpaceName,
            divider = index != list.lastIndex,
            onClick = { onEnter(establishment) },
            modifier = Modifier.animateItem(),
        )
    }
    if (state.nextCursor != null) {
        item(key = "nearby-more") {
            EditorialSecondaryButton(
                text = if (state.loadingMore) "Cargando…" else "Ver más lugares",
                onClick = onLoadMore,
                enabled = !state.loadingMore,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
private fun LazyListScope.searchResults(
    state: DiscoveryUiState,
    activeId: String?,
    activeSpaceName: String?,
    onEnter: (PublicEstablishment) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
) {
    val query = state.query.trim()
    if (query.isNotEmpty()) {
        when {
            state.searchFailed ->
                item(key = "failed") {
                    Column(Modifier.padding(top = 12.dp)) {
                        VenueNoticeCard(message = state.errorMessage ?: SEARCH_FAILED_MESSAGE)
                        TextButton(onClick = onRetry) { Text("Reintentar") }
                    }
                }
            state.loading && state.establishments.isEmpty() ->
                items(3) { index ->
                    VenueSkeleton(Modifier.padding(top = if (index == 0) 12.dp else 0.dp))
                }
            state.establishments.isEmpty() ->
                item(key = "empty") {
                    Text(
                        "No encontramos «$query».",
                        color = LocalVaiinillaColors.current.ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                }
            else -> {
                val list = state.establishments
                item(key = "count") {
                    val colors = LocalVaiinillaColors.current
                    AnimatedContent(
                        targetState = list.size,
                        transitionSpec = {
                            (slideInVertically(spring(stiffness = 500f)) { it / 2 } + fadeIn()) togetherWith
                                (slideOutVertically(spring(stiffness = 500f)) { -it / 2 } + fadeOut())
                        },
                        label = "venue-count",
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    ) { count ->
                        Text(
                            if (count == 1) "1 lugar" else "$count lugares",
                            color = colors.muted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                itemsIndexed(list, key = { _, it -> it.id }) { index, establishment ->
                    VenueRow(
                        establishment = establishment,
                        query = query,
                        active = activeId == establishment.id,
                        activeSpaceName = activeSpaceName,
                        divider = index != list.lastIndex,
                        onClick = { onEnter(establishment) },
                        modifier = Modifier.animateItem().discoveryRise(index.coerceAtMost(8)),
                    )
                }
                if (state.nextCursor != null) {
                    item(key = "load-more") {
                        EditorialSecondaryButton(
                            text = if (state.loadingMore) "Cargando…" else "Ver más lugares",
                            onClick = onLoadMore,
                            enabled = !state.loadingMore,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }
        }
    }
    item(key = "not-listed") {
        val colors = LocalVaiinillaColors.current
        Text(
            buildAnnotatedString {
                withStyle(
                    SpanStyle(color = colors.ink, fontWeight = FontWeight.ExtraBold),
                ) { append("¿No sale tu lugar? ") }
                append("Algunos solo abren con su QR o su código. Regresa y escanéalo.")
            },
            color = colors.muted,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 14.dp),
        )
    }
}

/** Un lugar de los resultados: toda la fila entra. Lo que coincide con la búsqueda va resaltado. */
@Composable
private fun VenueRow(
    establishment: PublicEstablishment,
    query: String,
    active: Boolean,
    activeSpaceName: String?,
    divider: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val access =
        if (establishment.clientIdRequired) "${establishment.clientIdLabel} requerida" else "Abierto a todos"
    val subtitle =
        when {
            !active -> access
            activeSpaceName.isNullOrBlank() -> "Aquí estás · $access"
            else -> "Estás en $activeSpaceName · aquí pides para llevar"
        }
    val name = remember(establishment.name, query, colors) { highlightedName(establishment.name, query, colors) }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 76.dp)
                    .physicalPress(scale = PhysicalPressScale.ProductCard, onClick = onClick)
                    .testTag("establishment-${establishment.slug}")
                    .semantics(mergeDescendants = true) { contentDescription = "${establishment.name}. $subtitle" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            VenueThumbnail(establishment = establishment, size = 52.dp, corner = 15.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    subtitle,
                    color = colors.muted,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.muted)
        }
        if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
    }
}

/** El nombre con la parte que coincide en negrita sobre lima; sin distinguir mayúsculas ni acentos. */
internal fun highlightedName(
    name: String,
    query: String,
    colors: VaiinillaColors,
): AnnotatedString {
    val needle = query.trim()
    val start = if (needle.isEmpty()) -1 else foldForSearch(name).indexOf(foldForSearch(needle))
    return buildAnnotatedString {
        withStyle(SpanStyle(color = colors.ink2, fontWeight = FontWeight.Bold)) { append(name) }
        if (start >= 0) {
            addStyle(
                SpanStyle(
                    color = colors.ink,
                    fontWeight = FontWeight.Black,
                    background = colors.accent.copy(alpha = 0.18f),
                ),
                start,
                start + needle.length,
            )
        }
    }
}

/** Letra por letra, para que cada índice siga apuntando a la misma letra del nombre original. */
private fun foldForSearch(text: String): String =
    text
        .map { char ->
            Normalizer
                .normalize(char.toString(), Normalizer.Form.NFD)
                .first()
                .lowercaseChar()
        }.joinToString("")

/** La foto del lugar (o su logo); sin ninguna, la inicial sobre el relleno de la tarjeta. */
@Composable
private fun VenueThumbnail(
    establishment: PublicEstablishment,
    size: Dp,
    corner: Dp,
) {
    val colors = LocalVaiinillaColors.current
    val shape = RoundedCornerShape(corner)
    val url = establishment.photoUrl ?: establishment.logoUrl
    Box(
        modifier = Modifier.size(size).clip(shape).background(raisedFill(colors)),
        contentAlignment = Alignment.Center,
    ) {
        val initial: @Composable (Modifier) -> Unit = {
            Text(
                initialOf(establishment.name),
                color = colors.muted,
                fontSize = (size.value * 0.38f).sp,
                fontWeight = FontWeight.Black,
            )
        }
        if (url != null) {
            ProductImage(
                imageUrl = url,
                contentDescription = "",
                modifier = Modifier.fillMaxSize(),
                placeholder = { initial(it) },
                card = true,
            )
        } else {
            initial(Modifier)
        }
    }
}

@Composable
private fun VenueSkeleton(modifier: Modifier = Modifier) {
    val colors = LocalVaiinillaColors.current
    val reduced = reducedMotion()
    val alpha =
        if (reduced) {
            1f
        } else {
            val transition = rememberInfiniteTransition(label = "venue-skeleton")
            val value by transition.animateFloat(
                initialValue = 1f,
                targetValue = 0.55f,
                animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
                label = "venue-skeleton-alpha",
            )
            value
        }
    Row(
        modifier = modifier.fillMaxWidth().height(76.dp).graphicsLayer { this.alpha = alpha },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(15.dp)).background(colors.paper2))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(width = 150.dp, height = 13.dp).clip(CircleShape).background(colors.line))
            Box(Modifier.size(width = 110.dp, height = 10.dp).clip(CircleShape).background(colors.line))
        }
    }
}

@Composable
private fun DiscoveryCircleButton(
    description: String,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Box(
        modifier =
            Modifier
                .size(44.dp)
                .shadow(8.dp, CircleShape, spotColor = Color.Black.copy(alpha = if (colors.isDark) 0.35f else 0.1f))
                .clip(CircleShape)
                .background(colors.paper2)
                .border(1.dp, colors.line, CircleShape)
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
                .semantics {
                    role = Role.Button
                    contentDescription = description
                },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = colors.ink)
    }
}

/** El relleno más claro de las casillas y el botón de borrar: el boceto lo usa y la paleta no lo tiene. */
private fun raisedFill(colors: VaiinillaColors): Color = if (colors.isDark) Color(0xFF33342F) else Color(0xFFDEDACD)

private fun lerpDp(
    start: Dp,
    stop: Dp,
    fraction: Float,
): Dp = start + (stop - start) * fraction

/** Entran una tras otra desde abajo al abrir la pantalla. */
@Composable
private fun Modifier.discoveryRise(index: Int): Modifier {
    val reduced = reducedMotion()
    var shown by rememberSaveable { mutableStateOf(reduced) }
    val progress = remember { Animatable(if (shown) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!shown) {
            delay(index * 40L)
            progress.animateTo(1f, spring(dampingRatio = 0.9f, stiffness = 200f))
            shown = true
        }
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 18.dp.toPx()
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
        SectionLabel("Videos del lugar")
        videos.forEach { video ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
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

/** Con un solo lugar disponible (sin buscar y sin lugar activo) no hay nada que elegir. */
internal fun onlyVenueToAutoOpen(
    establishments: List<PublicEstablishment>,
    query: String,
    loading: Boolean,
    hasActiveVenue: Boolean,
    nearbyActive: Boolean = false,
): PublicEstablishment? =
    establishments
        .singleOrNull()
        // Con "Cerca de mí" el único resultado es de un filtro, no el único negocio que existe (COMPRA-13).
        ?.takeIf { !loading && query.isBlank() && !hasActiveVenue && !nearbyActive }

@Composable
private fun VenueNoticeCard(
    message: String,
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
                .background(colors.coral.copy(alpha = 0.12f))
                .padding(horizontal = 13.dp, vertical = 11.dp),
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
