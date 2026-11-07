package com.vaiinilla.app.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vaiinilla.app.core.location.HomeAddressLocator
import com.vaiinilla.app.core.location.MapsLink
import com.vaiinilla.app.domain.model.AddressStreet
import com.vaiinilla.app.domain.model.DeliveryAddress
import com.vaiinilla.app.domain.model.HomeDeliveryOffer
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.order.OrderFlowUiState
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.DelayedMapListener
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import java.io.File
import kotlin.math.abs

/** Sin ubicación del cliente ni del negocio, el mapa abre en Chihuahua. */
private val FALLBACK_CENTER = MapsLink.Point(28.6353, -106.0889)
private const val STREET_ZOOM = 17.0
private const val CITY_ZOOM = 13.0
private const val SETTLE_MS = 350L

internal fun DeliveryAddress.point(): MapsLink.Point? {
    val lat = latitude ?: return null
    val lon = longitude ?: return null
    return MapsLink.Point(lat, lon)
}

private fun MapsLink.Point.near(other: MapsLink.Point?): Boolean =
    other != null && abs(latitude - other.latitude) < 1e-5 && abs(longitude - other.longitude) < 1e-5

/**
 * osmdroid guarda los mosaicos en almacenamiento externo si no se le dice otra cosa, y la política de uso de los
 * mosaicos de OpenStreetMap exige un User-Agent propio de la app.
 */
private fun configureOsm(context: Context) {
    val config = Configuration.getInstance()
    if (config.userAgentValue == context.packageName) return
    val app = context.applicationContext
    config.load(app, app.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    config.userAgentValue = app.packageName
    val base = File(app.cacheDir, "osmdroid")
    config.osmdroidBasePath = base
    config.osmdroidTileCache = File(base, "tiles")
}

/** El mismo filtro oscuro del lienzo D10: invertir, devolver el tono y bajar brillo, contraste y saturación. */
private fun darkTiles(): ColorMatrixColorFilter {
    val matrix =
        ColorMatrix(
            floatArrayOf(-1f, 0f, 0f, 0f, 255f, 0f, -1f, 0f, 0f, 255f, 0f, 0f, -1f, 0f, 255f, 0f, 0f, 0f, 1f, 0f),
        )
    matrix.postConcat(
        ColorMatrix(
            floatArrayOf(
                -0.574f, 1.43f, 0.144f, 0f, 0f,
                0.426f, 0.43f, 0.144f, 0f, 0f,
                0.426f, 1.43f, -0.856f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    )
    matrix.postConcat(ColorMatrix().apply { setScale(0.92f, 0.92f, 0.92f, 1f) })
    matrix.postConcat(
        ColorMatrix(
            floatArrayOf(0.86f, 0f, 0f, 0f, 17.9f, 0f, 0.86f, 0f, 0f, 17.9f, 0f, 0f, 0.86f, 0f, 17.9f, 0f, 0f, 0f, 1f, 0f),
        ),
    )
    matrix.postConcat(ColorMatrix().apply { setSaturation(0.35f) })
    return ColorMatrixColorFilter(matrix)
}

@Composable
private fun OsmMap(
    center: MapsLink.Point,
    zoom: Double,
    interactive: Boolean,
    modifier: Modifier = Modifier,
    onReady: (MapView) -> Unit = {},
    onTouch: () -> Unit = {},
    onMove: () -> Unit = {},
    onSettled: (MapsLink.Point) -> Unit = {},
) {
    val colors = LocalVaiinillaColors.current
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val latestTouch by rememberUpdatedState(onTouch)
    val latestMove by rememberUpdatedState(onMove)
    val latestSettled by rememberUpdatedState(onSettled)
    val map =
        remember {
            configureOsm(context)
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                setMultiTouchControls(interactive)
                isTilesScaledToDpi = true
                minZoomLevel = 4.0
                maxZoomLevel = 19.0
                controller.setZoom(zoom)
                controller.setCenter(GeoPoint(center.latitude, center.longitude))
                if (interactive) {
                    setOnTouchListener { _, event ->
                        if (event.actionMasked == android.view.MotionEvent.ACTION_DOWN) latestTouch()
                        false
                    }
                    addMapListener(
                        object : MapListener {
                            override fun onScroll(event: ScrollEvent?): Boolean {
                                latestMove()
                                return false
                            }

                            override fun onZoom(event: ZoomEvent?): Boolean = false
                        },
                    )
                    addMapListener(
                        DelayedMapListener(
                            object : MapListener {
                                override fun onScroll(event: ScrollEvent?): Boolean {
                                    latestSettled(MapsLink.Point(mapCenter.latitude, mapCenter.longitude))
                                    return true
                                }

                                override fun onZoom(event: ZoomEvent?): Boolean = false
                            },
                            SETTLE_MS,
                        ),
                    )
                }
            }
        }
    DisposableEffect(lifecycle, map) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> map.onResume()
                    Lifecycle.Event.ON_PAUSE -> map.onPause()
                    else -> Unit
                }
            }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            map.onDetach()
        }
    }
    LaunchedEffect(map) { onReady(map) }
    val background = colors.paper2.toArgb()
    AndroidView(
        factory = { map },
        modifier = modifier,
        update = { view ->
            view.overlayManager.tilesOverlay.setColorFilter(if (colors.isDark) darkTiles() else null)
            view.overlayManager.tilesOverlay.loadingBackgroundColor = background
            view.overlayManager.tilesOverlay.loadingLineColor = background
            if (!interactive) {
                val shown = MapsLink.Point(view.mapCenter.latitude, view.mapCenter.longitude)
                if (!center.near(shown)) view.controller.setCenter(GeoPoint(center.latitude, center.longitude))
                if (view.zoomLevelDouble != zoom) view.controller.setZoom(zoom)
            }
        },
    )
}

/** El pin fijo del centro: se levanta mientras el mapa se mueve y la punta cae justo en el centro. */
@Composable
private fun MapPin(
    lifted: Boolean,
    tag: String?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val lift by animateDpAsState(
        if (lifted) (-12).dp else 0.dp,
        spring(dampingRatio = 0.55f, stiffness = 520f),
        label = "pin",
    )
    val shadow by animateFloatAsState(if (lifted) 0.18f else 0.38f, spring(stiffness = 520f), label = "sombra")
    Column(
        modifier =
            modifier.layoutCenteredOnTip(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (tag != null) {
            Text(
                tag,
                color = colors.onInk,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier =
                    Modifier
                        .offset { IntOffset(0, lift.roundToPx()) }
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.ink)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
            )
            Spacer(Modifier.height(2.dp))
        }
        Icon(
            Icons.Filled.LocationOn,
            contentDescription = null,
            tint = colors.accent,
            // offset { }: mientras se arrastra el mapa, el pin sube sin recomponer en cada cuadro.
            modifier = Modifier.size(44.dp).offset { IntOffset(0, lift.roundToPx()) },
        )
        Box(
            Modifier
                .offset(y = (-4).dp)
                .size(width = 14.dp, height = 5.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = shadow)),
        )
    }
}

/** La punta del ícono queda a ~7 dp del borde de abajo (la sombra y el margen del ícono). */
private fun Modifier.layoutCenteredOnTip(): Modifier =
    this.then(
        Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.place(0, -placeable.height / 2 + 7.dp.roundToPx())
            }
        },
    )

/** El mapa chico del carrito (D9): no se mueve; tocarlo abre el mapa completo. */
@Composable
internal fun HomeMapCard(
    point: MapsLink.Point?,
    onOpen: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, spring(dampingRatio = 0.6f, stiffness = 600f), label = "mapa")
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(176.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.clip(shape)
                .border(1.dp, colors.line, shape)
                .background(colors.paper2),
    ) {
        OsmMap(
            center = point ?: FALLBACK_CENTER,
            zoom = if (point != null) 16.0 else CITY_ZOOM,
            interactive = false,
            modifier = Modifier.fillMaxSize(),
        )
        if (point != null) MapPin(lifted = false, tag = null, modifier = Modifier.align(Alignment.Center))
        Row(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.ink)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (point != null) "Ajustar en el mapa" else "Elegir en el mapa",
                color = colors.onInk,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.solidPaper ?: colors.paper),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.OpenInFull, contentDescription = null, tint = colors.ink, modifier = Modifier.size(18.dp))
        }
        // Encima del mapa para que el toque no lo reciba la vista de osmdroid.
        Box(
            Modifier
                .matchParentSize()
                .clickable(interactionSource = interaction, indication = null, onClick = onOpen)
                .semantics {
                    role = Role.Button
                    contentDescription = if (point != null) "Ajustar en el mapa" else "Elegir en el mapa"
                },
        )
    }
}

/** A dónde va y si llega el negocio, bajo el mapa chico. */
@Composable
internal fun HomeAddressSummary(
    state: OrderFlowUiState,
    offer: HomeDeliveryOffer,
) {
    val colors = LocalVaiinillaColors.current
    val address = state.homeCheckout.address.address
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.paper2)
                .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("TE LO LLEVAN A", color = colors.muted, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
        Text(
            address.ifBlank { "Aún no eliges dónde. Toca el mapa." },
            color = if (address.isBlank()) colors.muted else colors.ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 20.sp,
        )
        QuoteChips(state, offer)
    }
}

@Composable
private fun QuoteChips(
    state: OrderFlowUiState,
    offer: HomeDeliveryOffer,
) {
    val colors = LocalVaiinillaColors.current
    AnimatedContent(
        targetState = Triple(state.homeCheckout.quote, state.deliveryQuoteError, state.quotingDelivery),
        transitionSpec = { fadeIn(spring(stiffness = 300f)) togetherWith fadeOut(spring(stiffness = 300f)) },
        label = "cotizacion-envio",
    ) { (quote, error, quoting) ->
        when {
            quoting -> Chip("Calculando el envío…", colors.paper, colors.muted, colors)
            error != null -> Text(error, color = colors.coral, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            quote != null && quote.reaches -> {
                val fee = quote.fee ?: offer.fee
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(
                        listOfNotNull("Sí llegamos", quote.distanceKm?.let { "$it km" }).joinToString(" · "),
                        colors.accent,
                        colors.accentInk,
                        colors,
                    )
                    Chip(
                        if (Money.parse(fee).signum() == 0) "Envío gratis" else "Envío ${moneyLabel(fee)}",
                        colors.paper,
                        colors.ink,
                        colors,
                    )
                }
            }
            quote != null ->
                Chip(
                    offer.radiusKm.takeIf { it.isNotBlank() }?.let { "Fuera de la zona (hasta $it km)" }
                        ?: "Fuera de la zona de entrega",
                    colors.coral.copy(alpha = 0.16f),
                    colors.coral,
                    colors,
                )
            else -> Unit
        }
    }
}

@Composable
private fun Chip(
    label: String,
    background: Color,
    content: Color,
    colors: VaiinillaColors,
) {
    Text(
        label,
        color = content,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier =
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(background)
                .border(1.dp, if (background == colors.paper) colors.line else Color.Transparent, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

/**
 * El mapa completo (D10): el pin se queda en el centro y se mueve el mapa. Cada vez que el mapa se detiene se busca la
 * calle de ese punto y se le pregunta al negocio si llega. Cerrar con la X deja la dirección como estaba.
 */
@Composable
internal fun HomeMapPicker(
    state: OrderFlowUiState,
    offer: HomeDeliveryOffer,
    actions: HomeCheckoutActions,
    onClose: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    var dialogView by remember { mutableStateOf<android.view.View?>(null) }

    // Dentro de un Dialog el teclado es de la ventana del diálogo, no de la actividad.
    fun hideKeyboard() {
        val view = dialogView ?: return
        view.context.getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(view.windowToken, 0)
    }
    val scope = rememberCoroutineScope()
    val haptics = rememberVaiinillaHaptics()
    val latestState by rememberUpdatedState(state)
    val previous = remember { state.homeCheckout.address }
    val start =
        remember {
            previous.point()
                ?: HomeAddressLocator.currentLocation(context)
        }
    var map by remember { mutableStateOf<MapView?>(null) }
    var chosen by remember { mutableStateOf(start != null) }
    var moving by remember { mutableStateOf(false) }
    var street by remember { mutableStateOf(previous.address.takeIf { previous.point() != null }) }
    var search by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    val sheet = remember { Animatable(1f) }
    LaunchedEffect(Unit) { sheet.animateTo(0f, spring(dampingRatio = 0.86f, stiffness = 380f)) }

    fun close(keep: Boolean) {
        settleJob?.cancel()
        if (!keep) {
            val now = latestState.homeCheckout.address
            if (now.address != previous.address || !previous.point().let { it?.near(now.point()) ?: (now.point() == null) }) {
                actions.onEditAddress { it.copy(address = previous.address) }
                previous.point()?.let { point ->
                    actions.onQuoteStart()
                    actions.onQuote(point.latitude, point.longitude, previous.address, null)
                }
            }
        }
        scope.launch {
            sheet.animateTo(1f, spring(dampingRatio = 1f, stiffness = 500f))
            onClose()
        }
    }

    fun settle(point: MapsLink.Point) {
        moving = false
        if (!chosen) return
        val current = latestState.homeCheckout
        if (point.near(current.address.point()) && current.quote != null) return
        settleJob?.cancel()
        settleJob =
            scope.launch {
                street = null
                val found =
                    HomeAddressLocator.address(context, point)
                        ?: "${AddressStreet.LOCATION_PREFIX}%.5f, %.5f".format(point.latitude, point.longitude)
                val text = found.take(160)
                street = text
                actions.onEditAddress { it.copy(address = text) }
                actions.onQuoteStart()
                actions.onQuote(point.latitude, point.longitude, text, null)
            }
    }

    fun flyTo(point: MapsLink.Point) {
        chosen = true
        notice = null
        map?.controller?.animateTo(GeoPoint(point.latitude, point.longitude), STREET_ZOOM, 700L)
    }

    fun goToMyLocation() {
        val here = HomeAddressLocator.currentLocation(context)
        if (here == null) notice = "No pudimos saber dónde estás. Busca tu calle." else flyTo(here)
    }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) goToMyLocation() else notice = "Sin permiso de ubicación. Busca tu calle o mueve el mapa."
        }

    fun runSearch() {
        val text = search.trim()
        if (text.isEmpty() || searching) return
        hideKeyboard()
        focus.clearFocus()
        searching = true
        scope.launch {
            val point = runCatching { HomeAddressLocator.locate(context, text) }.getOrNull()
            searching = false
            if (point == null) {
                notice =
                    if (MapsLink.find(text) == null) {
                        "No encontramos esa dirección. Agrega la colonia y la ciudad."
                    } else {
                        "Ese enlace no trae el lugar. En Google Maps toca Compartir y pega el enlace otra vez."
                    }
            } else {
                flyTo(point)
            }
        }
    }

    Dialog(onDismissRequest = { close(keep = false) }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val view = LocalView.current
        LaunchedEffect(view) { dialogView = view }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val travel = constraints.maxHeight.toFloat()
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp)
                        .graphicsLayer { translationY = sheet.value * travel }
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(colors.solidPaper ?: colors.paper)
                        .navigationBarsPadding()
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "¿Dónde te lo dejamos?",
                        color = colors.ink,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.4).sp,
                        modifier = Modifier.weight(1f),
                    )
                    MapButton(Icons.Outlined.Close, "Cerrar el mapa", colors) {
                        haptics.click()
                        close(keep = false)
                    }
                }
                OutlinedTextField(
                    value = search,
                    onValueChange = {
                        search = it.take(MapsLink.MAX_TEXT)
                        notice = null
                    },
                    placeholder = { Text("Busca tu calle o pega un enlace de Maps", color = colors.muted, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.muted) },
                    trailingIcon =
                        if (searching) {
                            { Text("Buscando…", color = colors.muted, fontSize = 12.sp, modifier = Modifier.padding(end = 12.dp)) }
                        } else {
                            null
                        },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { runSearch() }),
                    shape = RoundedCornerShape(16.dp),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.ink,
                            unfocusedBorderColor = colors.line,
                            focusedTextColor = colors.ink,
                            unfocusedTextColor = colors.ink,
                            cursorColor = colors.ink,
                            focusedContainerColor = colors.paper2,
                            unfocusedContainerColor = colors.paper2,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, colors.line, RoundedCornerShape(24.dp))
                            .background(colors.paper2),
                ) {
                    OsmMap(
                        center = start ?: FALLBACK_CENTER,
                        zoom = if (start != null) STREET_ZOOM else CITY_ZOOM,
                        interactive = true,
                        modifier = Modifier.fillMaxSize(),
                        onReady = { view ->
                            map = view
                            start?.let(::settle)
                        },
                        onTouch = {
                            hideKeyboard()
                            focus.clearFocus()
                            chosen = true
                            notice = null
                        },
                        onMove = { if (chosen) moving = true },
                        onSettled = ::settle,
                    )
                    MapPin(
                        lifted = moving,
                        tag = if (chosen) "Te lo dejan aquí" else null,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    Column(
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MapButton(Icons.Outlined.Add, "Acercar", colors) {
                            haptics.selection()
                            map?.controller?.zoomIn()
                        }
                        MapButton(Icons.Outlined.Remove, "Alejar", colors) {
                            haptics.selection()
                            map?.controller?.zoomOut()
                        }
                    }
                    Box(Modifier.align(Alignment.BottomEnd).padding(12.dp)) {
                        MapButton(Icons.Outlined.MyLocation, "Ir a mi ubicación", colors, accent = true) {
                            haptics.click()
                            if (HomeAddressLocator.hasLocationPermission(context)) {
                                goToMyLocation()
                            } else {
                                permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            }
                        }
                    }
                    Text(
                        "© OpenStreetMap",
                        color = colors.muted,
                        fontSize = 10.sp,
                        modifier =
                            Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background((colors.solidPaper ?: colors.paper).copy(alpha = 0.8f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.paper2)
                            .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        notice ?: "Mueve el mapa para ajustar el pin",
                        color = if (notice != null) colors.coral else colors.muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    AnimatedContent(
                        targetState =
                            when {
                                !chosen -> "Busca tu calle o toca Mi ubicación"
                                moving -> "Suelta el mapa para elegir este punto"
                                street == null -> "Buscando la dirección…"
                                else -> street.orEmpty()
                            },
                        transitionSpec = { fadeIn(spring(stiffness = 400f)) togetherWith fadeOut(spring(stiffness = 400f)) },
                        label = "calle",
                    ) { line ->
                        Text(line, color = colors.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
                    }
                    if (chosen && !moving && street != null) QuoteChips(state, offer)
                    val quote = state.homeCheckout.quote
                    val ready = chosen && !moving && street != null && !state.quotingDelivery && quote?.reaches == true
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .heightIn(min = 52.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (ready) colors.ink else colors.line)
                                .physicalPress(enabled = ready, scale = PhysicalPressScale.Small) {
                                    haptics.success()
                                    close(keep = true)
                                }.semantics { role = Role.Button },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (quote != null && !quote.reaches && !moving && !state.quotingDelivery) {
                                "No llegamos hasta aquí"
                            } else {
                                "Usar esta ubicación"
                            },
                            color = if (ready) colors.onInk else colors.muted,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapButton(
    icon: ImageVector,
    label: String,
    colors: VaiinillaColors,
    accent: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (accent) colors.accent else colors.solidPaper ?: colors.paper)
                .border(1.dp, if (accent) Color.Transparent else colors.line, CircleShape)
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
                .semantics {
                    role = Role.Button
                    contentDescription = label
                },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (accent) colors.accentInk else colors.ink, modifier = Modifier.size(20.dp))
    }
}