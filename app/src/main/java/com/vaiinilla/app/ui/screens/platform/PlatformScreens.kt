package com.vaiinilla.app.ui.screens.platform

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.data.platform.PlatformStore
import com.vaiinilla.app.domain.model.AssistantMode
import com.vaiinilla.app.ui.components.TotpMfaChallengeForm
import com.vaiinilla.app.ui.components.initialOf
import com.vaiinilla.app.ui.platform.PlatformPage
import com.vaiinilla.app.ui.platform.PlatformUiState
import com.vaiinilla.app.ui.platform.PlatformViewModel
import com.vaiinilla.app.ui.platform.PulsePeriod
import com.vaiinilla.app.ui.platform.slugFrom
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_IMAGE_BYTES = 5 * 1024 * 1024

internal val STORE_TYPES =
    listOf(
        "cafeteria" to "Cafetería",
        "restaurante" to "Restaurante",
        "bar" to "Bar",
        "padel" to "Pádel",
        "boliche" to "Boliche",
        "food_truck" to "Food truck",
        "drive_thru" to "Drive-thru",
        "comedor" to "Comedor",
        "evento" to "Evento",
        "reposteria" to "Repostería",
    )

internal val TIME_ZONES =
    listOf(
        "America/Chihuahua" to "Chihuahua",
        "America/Mexico_City" to "Ciudad de México",
        "America/Monterrey" to "Monterrey",
        "America/Ciudad_Juarez" to "Ciudad Juárez",
        "America/Hermosillo" to "Hermosillo",
        "America/Mazatlan" to "Mazatlán",
        "America/Tijuana" to "Tijuana",
        "America/Cancun" to "Cancún",
    )

internal fun adminLabel(store: PlatformStore): String? =
    when {
        store.estado == "suspendido" -> "Suspendida"
        store.administrador == "asignado" -> "Activa · con administrador"
        store.administrador == "invitado" -> "Invitación enviada"
        store.administrador == "sin_asignar" -> "Falta administrador"
        else -> null
    }

@Composable
fun PlatformScreen(
    viewModel: PlatformViewModel,
    onExit: () -> Unit,
) {
    val state = viewModel.state.value
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.open() }
    BackHandler { if (!viewModel.back()) onExit() }
    LaunchedEffect(state.stripeUrl) {
        state.stripeUrl?.let {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
            viewModel.stripeUrlOpened()
        }
    }
    val colors = LocalVaiinillaColors.current
    com.vaiinilla.app.ui.components.ResizeForKeyboard()
    Box(Modifier.fillMaxSize().background(colors.paper)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Box(Modifier.weight(1f)) {
                when (val page = state.page) {
                    PlatformPage.Pulse -> PulsePage(state, viewModel)
                    PlatformPage.Stores -> StoresPage(state, viewModel)
                    is PlatformPage.Form -> StoreFormPage(state, viewModel, page.storeId)
                    is PlatformPage.Detail -> DetailPage(state, viewModel, page.storeId)
                    is PlatformPage.Assign -> AssignPage(state, viewModel, page.storeId)
                    is PlatformPage.Suspend -> SuspendPage(state, viewModel, page.storeId)
                    is PlatformPage.Stripe -> StripePage(state, viewModel, page.storeId)
                }
            }
            if (state.page == PlatformPage.Pulse || state.page == PlatformPage.Stores) {
                PlatformDock(state.page, viewModel, onExit)
            }
        }
        if (state.needsConfirm) ConfirmOverlay(state, viewModel, onExit)
    }
}

@Composable
private fun PlatformDock(
    page: PlatformPage,
    viewModel: PlatformViewModel,
    onExit: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(colors.paper2)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DockItem("Pulso", page == PlatformPage.Pulse, Modifier.weight(1f)) { viewModel.navigate(PlatformPage.Pulse) }
        DockItem("Tiendas", page == PlatformPage.Stores, Modifier.weight(1f)) { viewModel.navigate(PlatformPage.Stores) }
        DockItem("Salir", false, Modifier.weight(1f), onExit)
    }
}

@Composable
private fun DockItem(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(if (selected) colors.ink else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) colors.onInk else colors.muted, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
    }
}

@Composable
private fun PageColumn(
    caption: String?,
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    state: PlatformUiState,
    viewModel: PlatformViewModel,
    content: @Composable () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
    ) {
        if (onBack != null) {
            Box(
                Modifier
                    .padding(top = 8.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.paper2)
                    .clickable(onClickLabel = "Volver", onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver", tint = colors.ink) }
        }
        Spacer(Modifier.padding(top = 12.dp))
        caption?.let {
            Text(it.uppercase(), color = colors.muted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
        }
        Text(
            title,
            color = colors.ink,
            fontSize = 30.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = 6.dp).semantics { heading() },
        )
        subtitle?.let {
            Text(it, color = colors.muted, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
        }
        state.notice?.let { Banner(it, false) }
        state.error?.let { Banner(it, true) }
        content()
    }
}

@Composable
private fun Banner(
    text: String,
    isError: Boolean,
) {
    val colors = LocalVaiinillaColors.current
    Text(
        text,
        color = if (isError) colors.coral else colors.ink2,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier =
            Modifier
                .padding(top = 14.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.paper2)
                .padding(14.dp),
    )
}

@Composable
private fun PrimaryButton(
    text: String,
    busy: Boolean,
    enabled: Boolean = true,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val active = enabled && !busy
    val background = if (danger) colors.coral else colors.accent
    Box(
        Modifier
            .padding(top = 20.dp)
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(if (active) background else colors.paper2)
            .clickable(enabled = active, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(22.dp), color = colors.ink, strokeWidth = 2.dp)
        } else {
            Text(
                text,
                color = if (active) colors.accentInk else colors.muted,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@Composable
private fun TextAction(
    text: String,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Box(
        Modifier.padding(top = 8.dp).fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = colors.ink2, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    hint: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    minHeight: Int = 50,
) {
    val colors = LocalVaiinillaColors.current
    Column(Modifier.padding(top = 14.dp)) {
        Text(label, color = colors.ink2, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 7.dp, start = 2.dp))
        BasicTextField(
            value = value,
            onValueChange = onValue,
            textStyle = TextStyle(color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            cursorBrush = SolidColor(colors.ink),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(colors.paper2)
                    .border(1.dp, colors.line, RoundedCornerShape(15.dp))
                    .padding(14.dp),
        )
        hint?.let { Text(it, color = colors.muted, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 5.dp, start = 2.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chips(
    options: List<Pair<String, String>>,
    selected: String?,
    onSelect: (String) -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        options.forEach { (value, label) ->
            val on = value == selected
            Box(
                Modifier
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(if (on) colors.ink else colors.paper2)
                    .clickable(role = Role.RadioButton) { onSelect(value) }
                    .padding(horizontal = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (on) colors.onInk else colors.ink2, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun Kpi(
    value: String,
    label: String,
    modifier: Modifier,
    warn: Boolean = false,
) {
    val colors = LocalVaiinillaColors.current
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(colors.paper2).padding(12.dp)) {
        Text(
            value,
            color = if (warn) colors.yolk else colors.ink,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 20.sp),
        )
        Text(label, color = colors.muted, fontSize = 11.5.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 3.dp))
    }
}

private fun money(value: String?): String =
    value?.toBigDecimalOrNull()?.let { "$" + String.format(java.util.Locale.US, "%,.2f", it) } ?: "—"

@Composable
private fun PulsePage(
    state: PlatformUiState,
    viewModel: PlatformViewModel,
) {
    val colors = LocalVaiinillaColors.current
    val summary = state.metrics?.resumen
    val op = state.metrics?.operacion
    PageColumn("Plataforma", "Pulso", state = state, viewModel = viewModel) {
        Spacer(Modifier.padding(top = 14.dp))
        Chips(PulsePeriod.entries.map { it.name to it.label }, state.period.name) { viewModel.selectPeriod(PulsePeriod.valueOf(it)) }
        Column(
            Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.accent).padding(18.dp),
        ) {
            Text("Ventas de todas las tiendas", color = colors.accentInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(money(summary?.sales), color = colors.accentInk, fontSize = 34.sp, fontWeight = FontWeight.Black)
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Kpi(summary?.pedidos?.toString() ?: "—", "Pedidos pagados", Modifier.weight(1f))
            Kpi(money(summary?.averageTicket), "Ticket promedio", Modifier.weight(1f))
            Kpi(money(summary?.comisiones), "Comisiones", Modifier.weight(1f))
        }
        Text("Cómo está la red ahora", color = colors.ink2, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Kpi(op?.active?.toString() ?: "—", "Activas", Modifier.weight(1f))
            Kpi(op?.suspended?.toString() ?: "—", "Suspendidas", Modifier.weight(1f))
            Kpi(op?.invitations?.toString() ?: "—", "Invitaciones", Modifier.weight(1f))
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Kpi(op?.openRegisters?.toString() ?: "—", "Cajas abiertas", Modifier.weight(1f))
            Kpi(op?.receiving?.toString() ?: "—", "Recibiendo pedidos", Modifier.weight(1f))
            Kpi(op?.idle?.toString() ?: "—", "Activas sin operar", Modifier.weight(1f), warn = (op?.idle ?: 0) > 0)
        }
        Text(
            "Solo datos agregados: no se ven clientes, pedidos sueltos ni carteras.",
            color = colors.muted,
            fontSize = 12.5.sp,
            modifier = Modifier.padding(top = 14.dp),
        )
    }
}

@Composable
private fun StoresPage(
    state: PlatformUiState,
    viewModel: PlatformViewModel,
) {
    val colors = LocalVaiinillaColors.current
    var query by rememberSaveable { mutableStateOf(state.query) }
    PageColumn("Plataforma", "Tiendas", state = state, viewModel = viewModel) {
        PrimaryButton("Crear tienda", false) { viewModel.navigate(PlatformPage.Form(null)) }
        Field("Buscar por nombre", query, {
            query = it
            viewModel.setStoreFilter(state.stateFilter, it)
        })
        Spacer(Modifier.padding(top = 10.dp))
        Chips(
            listOf("" to "Todas", "activo" to "Activas", "suspendido" to "Suspendidas"),
            state.stateFilter.orEmpty(),
        ) { viewModel.setStoreFilter(it.ifEmpty { null }, query) }
        Spacer(Modifier.padding(top = 10.dp))
        if (state.storesLoaded && state.stores.isEmpty()) {
            Text("No hay tiendas con esa búsqueda.", color = colors.muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
        }
        state.stores.forEach { store ->
            Column(
                Modifier
                    .padding(bottom = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.paper2)
                    .clickable(role = Role.Button) { viewModel.navigate(PlatformPage.Detail(store.id)) }
                    .padding(14.dp),
            ) {
                Text(store.nombre, color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "${STORE_TYPES.firstOrNull { it.first == store.tipo }?.second ?: store.tipo} · ${store.slug}",
                    color = colors.muted,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                adminLabel(store)?.let {
                    Text(
                        it,
                        color = if (store.administrador == "asignado" && store.estado == "activo") colors.ink2 else colors.coral,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
        if (state.storesCursor != null) TextAction("Ver más tiendas") { viewModel.loadMoreStores() }
    }
}

@Composable
private fun StoreFormPage(
    state: PlatformUiState,
    viewModel: PlatformViewModel,
    storeId: String?,
) {
    val store = state.stores.firstOrNull { it.id == storeId }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(store?.nombre.orEmpty()) }
    var slug by rememberSaveable { mutableStateOf(store?.slug.orEmpty()) }
    var slugTouched by rememberSaveable { mutableStateOf(store != null) }
    var type by rememberSaveable { mutableStateOf(store?.tipo ?: "cafeteria") }
    var zone by rememberSaveable { mutableStateOf(store?.timeZone ?: "America/Chihuahua") }
    var close by rememberSaveable { mutableStateOf(store?.forcedClose?.take(5) ?: "03:00") }
    // Qué asistentes de IA tiene; solo al editar y si el servidor ya lo conoce.
    var assistant by rememberSaveable { mutableStateOf(store?.assistantMode) }
    var more by rememberSaveable { mutableStateOf(false) }
    var description by rememberSaveable { mutableStateOf(store?.descripcion.orEmpty()) }
    var address by rememberSaveable { mutableStateOf(store?.direccion.orEmpty()) }
    var hours by rememberSaveable { mutableStateOf(store?.horario.orEmpty()) }
    var maps by rememberSaveable { mutableStateOf("") }
    var instagram by rememberSaveable { mutableStateOf(store?.instagramUrl.orEmpty()) }
    var facebook by rememberSaveable { mutableStateOf(store?.facebookUrl.orEmpty()) }
    var tiktok by rememberSaveable { mutableStateOf(store?.tiktokUrl.orEmpty()) }
    var whatsapp by rememberSaveable { mutableStateOf(store?.whatsappUrl.orEmpty()) }
    var website by rememberSaveable { mutableStateOf(store?.websiteUrl.orEmpty()) }
    var image by remember { mutableStateOf<Pair<ByteArray, String>?>(null) }
    var imageError by remember { mutableStateOf<String?>(null) }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val read =
                    withContext(Dispatchers.IO) {
                        runCatching {
                            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }?.let { it to mime }
                        }.getOrNull()
                    }
                when {
                    read == null -> imageError = "No se pudo leer esa foto."
                    read.first.size > MAX_IMAGE_BYTES -> imageError = "La foto pesa más de 5 MB."
                    else -> {
                        image = read
                        imageError = null
                    }
                }
            }
        }
    val closeValid = Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(close)
    val slugValid = Regex("^[a-z0-9]+(-[a-z0-9]+)*$").matches(slug)
    PageColumn(
        null,
        if (store == null) "Crear tienda" else "Datos de la tienda",
        if (store == null) "Solo lo básico. Lo demás lo arma el administrador con el asistente." else null,
        onBack = { viewModel.back() },
        state = state,
        viewModel = viewModel,
    ) {
        Field("Nombre", name, {
            name = it
            if (!slugTouched) slug = slugFrom(it)
        })
        Field("Enlace", slug, {
            slug = it.lowercase()
            slugTouched = true
        }, hint = if (slug.isEmpty() || slugValid) "Se llena solo con el nombre. No se repite entre tiendas." else "Solo letras sin acento, números y guiones.")
        SectionLabel("Tipo de negocio")
        Chips(STORE_TYPES, type) { type = it }
        SectionLabel("Zona horaria")
        Chips(TIME_ZONES, zone) { zone = it }
        Field(
            "Cierre forzado",
            close,
            { close = it.take(5) },
            hint = "Hora del día en que se cierran los pendientes, como 03:00.",
            keyboardType = KeyboardType.Number,
        )
        assistant?.let { mode ->
            SectionLabel("Asistente de IA")
            Chips(AssistantMode.entries.map { it.wire to it.label }, mode) { assistant = it }
        }
        TextAction(if (more) "Ocultar foto, dirección y redes" else "Foto, dirección, horario y redes (opcional)") { more = !more }
        if (more) {
            TextAction(if (image != null) "Foto lista · cambiar" else "Elegir foto") {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            imageError?.let { Banner(it, true) }
            Field("Descripción", description, { description = it.take(500) }, minHeight = 80)
            Field("Dirección o zona", address, { address = it.take(240) })
            Field("Horario visible", hours, { hours = it.take(160) }, hint = "Ejemplo: Lun a Vie, 8:00 a 20:00")
            Field("Enlace de Google Maps", maps, { maps = it }, hint = "Pega el enlace y sacamos la ubicación.", keyboardType = KeyboardType.Uri)
            Field("Instagram (https://…)", instagram, { instagram = it }, keyboardType = KeyboardType.Uri)
            Field("Facebook (https://…)", facebook, { facebook = it }, keyboardType = KeyboardType.Uri)
            Field("TikTok (https://…)", tiktok, { tiktok = it }, keyboardType = KeyboardType.Uri)
            Field("WhatsApp (https://wa.me/…)", whatsapp, { whatsapp = it }, keyboardType = KeyboardType.Uri)
            Field("Sitio web (https://…)", website, { website = it }, keyboardType = KeyboardType.Uri)
        }
        PrimaryButton(
            if (store == null) "Crear y asignar administrador" else "Guardar cambios",
            state.busy,
            enabled = name.isNotBlank() && slugValid && closeValid,
        ) {
            // Solo si lo cambió: si no, pisaría lo que el dueño cambió mientras tanto.
            val assistantChange =
                listOfNotNull(assistant?.takeIf { it != store?.assistantMode }?.let { "asistente_modo" to it })
            val optional =
                mapOf(
                    "descripcion" to description,
                    "direccion" to address,
                    "horario" to hours,
                    "instagram_url" to instagram,
                    "facebook_url" to facebook,
                    "tiktok_url" to tiktok,
                    "whatsapp_url" to whatsapp,
                    "sitio_web_url" to website,
                ).mapValues { it.value.trim().ifEmpty { null } }
                    .filter { it.value != null || store != null }
            viewModel.saveStore(
                storeId,
                mapOf(
                    "nombre" to name.trim(),
                    "slug" to slug,
                    "tipo" to type,
                    "zona_horaria" to zone,
                    "hora_cierre_forzado" to "$close:00",
                ) + optional + assistantChange,
                image,
                maps,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = LocalVaiinillaColors.current.ink2,
        fontSize = 13.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp, start = 2.dp),
    )
}

@Composable
private fun DetailRow(
    title: String,
    subtitle: String,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        Modifier
            .padding(bottom = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.paper2)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = if (danger) colors.coral else colors.ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = colors.muted, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = colors.muted)
    }
}

@Composable
private fun DetailPage(
    state: PlatformUiState,
    viewModel: PlatformViewModel,
    storeId: String,
) {
    val store = state.stores.firstOrNull { it.id == storeId }
    if (store == null) {
        PageColumn(null, "Tienda", onBack = { viewModel.back() }, state = state, viewModel = viewModel) {
            Banner("No encontramos esta tienda. Vuelve a la lista.", true)
        }
        return
    }
    val suspended = store.estado == "suspendido"
    PageColumn(
        null,
        store.nombre,
        "${STORE_TYPES.firstOrNull { it.first == store.tipo }?.second ?: store.tipo} · ${store.slug}",
        onBack = { viewModel.back() },
        state = state,
        viewModel = viewModel,
    ) {
        if (suspended) store.suspensionReason?.let { Banner("Suspendida: $it", true) }
        SectionLabel("Administración")
        if (store.administrador == "asignado") {
            Text("Ya tiene administrador. Su personal se maneja desde su tienda.", color = LocalVaiinillaColors.current.muted, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
        } else {
            DetailRow(
                if (store.administrador == "invitado") "Cambiar invitación" else "Asignar administrador",
                if (store.administrador == "invitado") "Hay una invitación pendiente" else "Todavía no tiene",
            ) { viewModel.navigate(PlatformPage.Assign(storeId)) }
        }
        SectionLabel("Tienda")
        DetailRow("Datos de la tienda", "Nombre, tipo, horario, foto y redes") { viewModel.navigate(PlatformPage.Form(storeId)) }
        DetailRow("Pagos con Stripe", if (store.stripe == null) "Sin cuenta conectada" else "Ver estado de la cuenta") { viewModel.navigate(PlatformPage.Stripe(storeId)) }
        SectionLabel("Estado")
        DetailRow(
            if (suspended) "Reactivar tienda" else "Suspender tienda",
            "Pide un motivo y queda registrado",
            danger = !suspended,
        ) { viewModel.navigate(PlatformPage.Suspend(storeId)) }
    }
}

@Composable
private fun AssignPage(
    state: PlatformUiState,
    viewModel: PlatformViewModel,
    storeId: String,
) {
    val store = state.stores.firstOrNull { it.id == storeId }
    var email by rememberSaveable { mutableStateOf("") }
    val valid = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email.trim())
    PageColumn(
        store?.nombre,
        "¿Quién va a administrar esta tienda?",
        "Escribe el correo con el que esa persona entra a Vaiinilla.",
        state = state,
        viewModel = viewModel,
    ) {
        Field("Correo del administrador", email, { email = it }, keyboardType = KeyboardType.Email)
        Hint("Si ya tiene cuenta", "Queda como administrador de inmediato. Lo verá al entrar a la app.")
        Hint("Si todavía no tiene cuenta", "Le mandamos una invitación a ese correo. Al crear su cuenta con él en los próximos 3 días, la tienda ya estará ahí.")
        PrimaryButton("Asignar administrador", state.busy, enabled = valid) { viewModel.assignAdmin(storeId, email) }
        TextAction("Hacerlo después") { viewModel.navigate(PlatformPage.Detail(storeId)) }
    }
}

@Composable
private fun Hint(
    title: String,
    body: String,
) {
    val colors = LocalVaiinillaColors.current
    Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.paper2).padding(14.dp)) {
        Text(title, color = colors.ink, fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold)
        Text(body, color = colors.muted, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun SuspendPage(
    state: PlatformUiState,
    viewModel: PlatformViewModel,
    storeId: String,
) {
    val store = state.stores.firstOrNull { it.id == storeId }
    val suspend = store?.estado != "suspendido"
    var reason by rememberSaveable { mutableStateOf("") }
    PageColumn(
        null,
        "¿${if (suspend) "Suspender" else "Reactivar"} ${store?.nombre.orEmpty()}?",
        if (suspend) {
            "Deja de recibir pedidos nuevos. Los pedidos que ya estaban en curso se pueden terminar y su personal solo podrá cerrar lo pendiente."
        } else {
            "La tienda vuelve a recibir pedidos."
        },
        onBack = { viewModel.back() },
        state = state,
        viewModel = viewModel,
    ) {
        Field("Motivo", reason, { reason = it.take(500) }, hint = "Mínimo 10 letras. Queda registrado.", minHeight = 110)
        PrimaryButton(
            if (suspend) "Suspender tienda" else "Reactivar tienda",
            state.busy,
            enabled = reason.trim().length >= 10,
            danger = suspend,
        ) { viewModel.setSuspended(storeId, suspend, reason) }
    }
}

@Composable
private fun StripePage(
    state: PlatformUiState,
    viewModel: PlatformViewModel,
    storeId: String,
) {
    val colors = LocalVaiinillaColors.current
    val store = state.stores.firstOrNull { it.id == storeId }
    val stripe = state.stripe
    var accountId by rememberSaveable { mutableStateOf("") }
    val canEnable = stripe?.chargesEnabled == true && stripe.payoutsEnabled
    PageColumn(
        store?.nombre,
        "Pagos con Stripe",
        "El dueño llena sus datos directo con Stripe. Cuando Stripe los apruebe, aquí se activa el cobro con tarjeta.",
        onBack = { viewModel.back() },
        state = state,
        viewModel = viewModel,
    ) {
        Column(Modifier.padding(top = 18.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.paper2).padding(16.dp)) {
            StripeLine("Cuenta", if (stripe == null) "Sin conectar" else if (stripe.detailsSubmitted) "Datos enviados" else "Datos pendientes")
            StripeLine("Puede cobrar", if (stripe?.chargesEnabled == true) "Sí" else "No")
            StripeLine("Puede recibir depósitos", if (stripe?.payoutsEnabled == true) "Sí" else "No")
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Tarjeta en la tienda", color = colors.ink2, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Switch(
                    checked = stripe?.enabled == true,
                    enabled = !state.busy && (canEnable || stripe?.enabled == true),
                    onCheckedChange = { viewModel.setStripeEnabled(storeId, it) },
                )
            }
        }
        PrimaryButton(if (stripe == null) "Empezar datos en Stripe" else "Continuar datos en Stripe", state.busy) { viewModel.startStripe(storeId) }
        Field("Vincular una cuenta que ya existe", accountId, { accountId = it }, hint = "Empieza con acct_. Vacío busca la única cuenta disponible.")
        TextAction("Vincular cuenta") { viewModel.linkStripe(storeId, accountId) }
    }
}

@Composable
private fun StripeLine(
    label: String,
    value: String,
) {
    val colors = LocalVaiinillaColors.current
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, color = colors.ink2, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(value, color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun ConfirmOverlay(
    state: PlatformUiState,
    viewModel: PlatformViewModel,
    onExit: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    var password by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    val challenge = state.challenge
    var factor by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler { viewModel.cancelConfirm(onExit) }
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.paper)
            .clickable(enabled = false) {}
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("PLATAFORMA", color = colors.muted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp, modifier = Modifier.padding(top = 30.dp))
        Text("Confirma que eres tú", color = colors.ink, fontSize = 30.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 6.dp).semantics { heading() })
        Text(
            "Desde aquí se crean tiendas de todos. Por eso te pedimos tu contraseña y tu código cada vez. Tu sesión dura 10 minutos y, al vencer, no pierdes lo que estabas llenando.",
            color = colors.muted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
        )
        if (challenge == null) {
            state.confirmError?.let { Banner(it, true) }
            Field("Contraseña", password, { password = it }, keyboardType = KeyboardType.Password, password = true)
            PrimaryButton("Continuar", state.confirming, enabled = password.isNotEmpty()) {
                viewModel.confirmPassword(password)
                code = ""
            }
            TextAction("Salir") { viewModel.cancelConfirm(onExit) }
        } else {
            val selected = factor ?: challenge.factors.firstOrNull()?.uid
            TotpMfaChallengeForm(
                factors = challenge.factors,
                selectedFactorUid = selected,
                code = code,
                loading = state.confirming,
                errorMessage = state.confirmError,
                onFactorSelected = { factor = it },
                onCodeChange = { code = it },
                onSubmit = { selected?.let { viewModel.confirmCode(it, code) } },
                onCancel = { viewModel.cancelConfirm(onExit) },
            )
        }
    }
}

/** Marco AS0: lo primero que ve quien acaba de recibir una tienda, antes de la bienvenida del asistente. */
@Composable
fun StoreAccessScreen(
    storeName: String,
    busy: Boolean,
    onConfigure: () -> Unit,
    onLater: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    BackHandler(onBack = onLater)
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Spacer(Modifier.weight(0.4f))
        Box(
            Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(colors.accent),
            contentAlignment = Alignment.Center,
        ) {
            Text(initialOf(storeName), color = colors.accentInk, fontSize = 30.sp, fontWeight = FontWeight.Black)
        }
        Text(storeName.uppercase(), color = colors.muted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp, modifier = Modifier.padding(top = 26.dp))
        Text("Tienes acceso a esta tienda", color = colors.ink, fontSize = 34.sp, lineHeight = 36.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp).semantics { heading() })
        Text(
            "Eres su administrador. Vamos a configurarla para que empiece a recibir pedidos.",
            color = colors.ink2,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 14.dp),
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton("Vamos a configurarla", busy, onClick = onConfigure)
        TextAction("Ahora no", onLater)
    }
}