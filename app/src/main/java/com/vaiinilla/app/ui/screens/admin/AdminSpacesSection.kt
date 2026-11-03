package com.vaiinilla.app.ui.screens.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.admin.ManagedSpace
import com.vaiinilla.app.domain.admin.ReservationSchedule
import com.vaiinilla.app.domain.admin.SpaceRules
import com.vaiinilla.app.domain.admin.SpaceType
import com.vaiinilla.app.domain.model.MoneyInput
import com.vaiinilla.app.domain.model.Reservation
import com.vaiinilla.app.domain.model.ReservationState
import com.vaiinilla.app.domain.time.BusinessTime
import com.vaiinilla.app.ui.admin.AdminSpacesViewModel
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.screens.ActionPill
import com.vaiinilla.app.ui.screens.ChipButton
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.OperationalField
import com.vaiinilla.app.ui.screens.prepareProductImage
import java.time.format.DateTimeFormatter

private sealed interface SpacesSheet {
    data object Create : SpacesSheet

    data object Batch : SpacesSheet

    data class Edit(
        val spaceId: Int,
    ) : SpacesSheet

    data class Delete(
        val space: ManagedSpace,
    ) : SpacesSheet

    data class Charge(
        val reservation: Reservation,
    ) : SpacesSheet

    data class Release(
        val reservation: Reservation,
    ) : SpacesSheet
}

/** La página abierta dentro de Mesas y espacios: un espacio con su QR (marco 49) o las reservas (marco 50). */
private sealed interface SpacesPage {
    data class Space(
        val id: Int,
    ) : SpacesPage

    data object Reservations : SpacesPage
}

/**
 * Mesas y espacios como el lienzo de Doop (marcos 45 a 50): los espacios agrupados por tipo, cada uno abre su página
 * con el QR, y las canchas suman sus reservas del día y el horario.
 */
@Composable
internal fun AdminSpacesSection(
    viewModel: AdminSpacesViewModel,
    colors: OperationalColors,
    readOnly: Boolean,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState
    var sheet by remember { mutableStateOf<SpacesSheet?>(null) }
    var page by remember { mutableStateOf<SpacesPage?>(null) }
    val close = { sheet = null }
    val enabled = !readOnly && !state.saving
    val reduced = reducedMotion()

    AnimatedContent(
        targetState = page,
        transitionSpec = {
            if (reduced) {
                fadeIn(snap()) togetherWith fadeOut(snap())
            } else {
                val forward = targetState != null
                (
                    slideInHorizontally(
                        spring(stiffness = 500f),
                    ) { if (forward) it / 4 else -it / 4 } + fadeIn(spring())
                ) togetherWith
                    (
                        slideOutHorizontally(spring(stiffness = 500f)) { if (forward) -it / 4 else it / 4 } +
                            fadeOut(spring())
                    )
            }
        },
        label = "spaces-page",
    ) { current ->
        when (current) {
            is SpacesPage.Space -> {
                val space = state.spaces?.firstOrNull { it.id == current.id }
                if (space == null) {
                    LaunchedEffect(Unit) { page = null }
                } else {
                    SpaceDetailPage(space, colors, readOnly, viewModel, onBack = { page = null }) {
                        sheet = SpacesSheet.Edit(space.id)
                    }
                }
            }
            SpacesPage.Reservations ->
                AdminSectionPage(
                    title = "Reservas",
                    colors = colors,
                    notice = state.notice,
                    noticeIsError = state.noticeIsError,
                    onBack = { page = null },
                ) {
                    item { AdminH2("Hoy · ${TodayFormat.format(java.time.LocalDate.now(BusinessTime.zone))}", colors) }
                    item {
                        ReservationsCard(
                            state.reservations,
                            colors,
                            enabled,
                            onCharge = { sheet = SpacesSheet.Charge(it) },
                            onRelease = { sheet = SpacesSheet.Release(it) },
                        )
                    }
                    item { AdminH2("Horario de reservas", colors) }
                    item { ScheduleCard(state.schedule, colors, enabled, viewModel::saveSchedule) }
                }
            null ->
                SpacesList(
                    state,
                    colors,
                    readOnly,
                    enabled,
                    viewModel,
                    onBack,
                    onOpen = { page = it },
                ) { sheet = it }
        }
    }

    val error = state.notice.takeIf { state.noticeIsError }
    when (val current = sheet) {
        SpacesSheet.Create ->
            CreateSheet(
                colors,
                state.saving,
                error,
                close,
            ) { name, type -> viewModel.create(name, type, close) }
        SpacesSheet.Batch ->
            BatchSheet(colors, state.saving, error, close) { type, prefix, from, to ->
                viewModel.createBatch(type, prefix, from, to, close)
            }
        is SpacesSheet.Edit -> {
            val space = state.spaces?.firstOrNull { it.id == current.spaceId }
            if (space == null) {
                close()
            } else {
                EditSheet(space, colors, state.saving, error, viewModel, onDelete = {
                    sheet = SpacesSheet.Delete(space)
                }, onDismiss = close)
            }
        }
        is SpacesSheet.Delete ->
            AdminConfirmSheet(
                title = "Eliminar ${current.space.name}",
                message =
                    "Su QR deja de funcionar. Si tiene una cuenta abierta o reservas pendientes, " +
                        "no se podrá eliminar.",
                confirmLabel = "Eliminar",
                colors = colors,
                busy = state.saving,
                onDismiss = close,
            ) {
                viewModel.delete(current.space)
                page = null
                close()
            }
        is SpacesSheet.Charge ->
            ChargeSheet(current.reservation, colors, state.saving, error, close) { received ->
                viewModel.payReservation(current.reservation, received, close)
            }
        is SpacesSheet.Release ->
            AdminConfirmSheet(
                title = "Liberar cancha",
                message =
                    "Se cancela la reserva de ${current.reservation.customerName ?: "este cliente"} " +
                        "y la cancha queda libre.",
                confirmLabel = "Liberar cancha",
                colors = colors,
                busy = state.saving,
                onDismiss = close,
            ) {
                viewModel.cancelReservation(current.reservation)
                close()
            }
        null -> Unit
    }
}

@Composable
private fun SpacesList(
    state: com.vaiinilla.app.ui.admin.AdminSpacesUiState,
    colors: OperationalColors,
    readOnly: Boolean,
    enabled: Boolean,
    viewModel: AdminSpacesViewModel,
    onBack: () -> Unit,
    onOpen: (SpacesPage) -> Unit,
    onSheet: (SpacesSheet) -> Unit,
) {
    val spaces = state.spaces
    AdminSectionPage(
        title = "Mesas y espacios",
        colors = colors,
        notice = state.notice,
        noticeIsError = state.noticeIsError,
        onBack = onBack,
        trailing = {
            if (!readOnly && !spaces.isNullOrEmpty()) {
                AdminPrimaryButton(
                    title = "Espacio",
                    colors = colors,
                    fullWidth = false,
                    icon = Icons.Outlined.Add,
                    enabled = enabled,
                ) { onSheet(SpacesSheet.Create) }
            }
        },
    ) {
        item {
            Text(
                "Cada espacio tiene su QR. El cliente lo escanea y pide desde ahí.",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMuted,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        when {
            spaces == null && state.error != null ->
                item { AdminLoadError(state.error.orEmpty(), colors, viewModel::refresh) }
            spaces == null -> item { AdminHint("Cargando espacios…", colors) }
            spaces.isEmpty() -> item { SpacesEmpty(colors, readOnly, enabled, Modifier.adminRise(1), onSheet) }
            else ->
                spaceGroups(spaces).forEachIndexed { index, (title, members) ->
                    item(key = "g-$title") {
                        Column(Modifier.adminRise(index + 1).animateItem()) {
                            AdminCapsLabel(
                                "$title · ${members.size}",
                                colors,
                                Modifier.padding(top = 18.dp, bottom = 8.dp),
                            )
                            AdminPanel(colors, padding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)) {
                                members.forEachIndexed { row, space ->
                                    if (row > 0) AdminRowDivider(colors)
                                    SpaceRow(
                                        space,
                                        colors,
                                        state.busySpaceId == space.id,
                                    ) { onOpen(SpacesPage.Space(space.id)) }
                                }
                            }
                        }
                    }
                }
        }
        if (state.hasCourts && !spaces.isNullOrEmpty()) {
            item(key = "reservas") {
                Column {
                    AdminCapsLabel("Reservas", colors, Modifier.padding(top = 18.dp, bottom = 8.dp))
                    AdminPanel(colors, padding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)) {
                        AdminRow(
                            title = "Reservas de hoy",
                            colors = colors,
                            detail =
                                if (state.reservations.isEmpty()) {
                                    "Nadie ha reservado hoy · horario y anticipación"
                                } else {
                                    "${state.reservations.size} hoy · horario y anticipación"
                                },
                            onClick = { onOpen(SpacesPage.Reservations) },
                            leading = { SpaceIcon(Icons.Outlined.CalendarMonth) },
                        ) { Chevron(colors) }
                    }
                }
            }
        }
    }
}

private fun spaceGroups(spaces: List<ManagedSpace>): List<Pair<String, List<ManagedSpace>>> {
    val sorted = spaces.sortedBy { naturalKey(it.name) }
    val order = SpaceType.entries.map { it.wire }.toMutableList()
    sorted.forEach { if (it.typeWire !in order) order.add(it.typeWire) }
    return order.mapNotNull { wire ->
        val members = sorted.filter { it.typeWire == wire }
        if (members.isEmpty()) return@mapNotNull null
        plural(SpaceType.entries.firstOrNull { it.wire == wire }?.label ?: wire) to members
    }
}

private fun plural(word: String): String {
    val last = word.lowercase().lastOrNull() ?: return word
    return if (last in "aeiou") word + "s" else word + "es"
}

@Composable
private fun SpaceRow(
    space: ManagedSpace,
    colors: OperationalColors,
    busy: Boolean,
    onClick: () -> Unit,
) {
    val price = space.hourlyPrice
    AdminRow(
        title = space.name,
        colors = colors,
        detail =
            when {
                space.isCourt && price != null -> "Renta ${moneyLabel(price)} por hora"
                else -> space.code?.let { "Código $it" } ?: (space.type?.label ?: space.typeWire)
            },
        dimmed = !space.active,
        onClick = onClick,
        leading = {
            if (space.isCourt && space.imageUrl != null) {
                AdminThumb(space.imageUrl, space.name, colors)
            } else {
                SpaceIcon(Icons.Outlined.QrCode2)
            }
        },
    ) {
        when {
            busy ->
                androidx.compose.material3.CircularProgressIndicator(
                    color = colors.textPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
            !space.active -> AdminBadge("Desactivado", colors, AdminBadgeTone.DANGER)
            space.isCourt && price != null -> AdminBadge("Se renta", colors, AdminBadgeTone.OK)
        }
        Chevron(colors)
    }
}

@Composable
private fun Chevron(colors: OperationalColors) {
    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.textMuted)
}

/** El cuadro crema con el ícono del QR de cada espacio. */
@Composable
private fun SpaceIcon(icon: ImageVector) {
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Cream),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = CreamInk, modifier = Modifier.size(20.dp)) }
}

private val Cream = Color(0xFFF5F1E5)
private val CreamInk = Color(0xFF1D1E1C)

/** Marco 46: la cuadrícula punteada con el QR al centro y los dos caminos para empezar. */
@Composable
private fun SpacesEmpty(
    colors: OperationalColors,
    readOnly: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onSheet: (SpacesSheet) -> Unit,
) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.padding(top = 72.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(2) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(3) { column ->
                        val cell = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
                        if (row == 0 && column == 1) {
                            Box(cell.background(Cream), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.QrCode2, contentDescription = null, tint = CreamInk)
                            }
                        } else {
                            Box(cell.dashedBorder(colors.cardBorder))
                        }
                    }
                }
            }
        }
        Text(
            "Aún no tienes espacios",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.4).sp,
            color = colors.textPrimary,
            modifier = Modifier.padding(top = 28.dp),
        )
        Text(
            "Crea tus mesas, la barra o una cancha. Cada una tiene su QR para que el cliente pida desde su lugar.",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 12.dp, end = 12.dp),
        )
        if (!readOnly) {
            AdminPrimaryButton(
                title = "Nuevo espacio",
                colors = colors,
                modifier = Modifier.padding(top = 26.dp).fillMaxWidth(),
                icon = Icons.Outlined.Add,
                enabled = enabled,
            ) { onSheet(SpacesSheet.Create) }
            AdminSecondaryButton(
                title = "Crear varios a la vez",
                colors = colors,
                modifier = Modifier.padding(top = 10.dp).fillMaxWidth(),
                outlined = true,
                enabled = enabled,
            ) { onSheet(SpacesSheet.Batch) }
        }
    }
}

private fun Modifier.dashedBorder(color: Color): Modifier =
    drawBehind {
        drawRoundRect(
            color = color,
            cornerRadius = CornerRadius(14.dp.toPx()),
            style =
                Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
                ),
        )
    }

/** Un espacio con su QR (marco 49): dónde ponerlo, su QR con compartir, copiar y NFC, y el lápiz para editarlo. */
@Composable
private fun SpaceDetailPage(
    space: ManagedSpace,
    colors: OperationalColors,
    readOnly: Boolean,
    viewModel: AdminSpacesViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val state by viewModel.uiState
    AdminSectionPage(
        title = space.name,
        colors = colors,
        notice = state.notice,
        noticeIsError = state.noticeIsError,
        onBack = onBack,
        trailing = {
            if (!readOnly) AdminRoundButton(Icons.Outlined.EditNote, "Editar ${space.name}", colors, onEdit)
        },
    ) {
        item {
            Text(
                when (space.type) {
                    SpaceType.COURT -> "Ponlo en la cancha. Al escanearlo, el cliente pide y su pedido llega aquí."
                    SpaceType.BAR -> "Ponlo en la barra. Al escanearlo, el cliente pide y su pedido llega a la barra."
                    else -> "Ponlo en la mesa. Al escanearlo, el cliente pide y su pedido llega a esta mesa."
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMuted,
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 6.dp),
            )
        }
        if (!space.active) item { AdminHint("Está desactivado: su QR no recibe pedidos.", colors) }
        item {
            AdminPanel(colors, Modifier.adminRise(1).padding(top = 10.dp)) {
                AdminQrBlock(
                    url = space.qrUrl,
                    name = space.name,
                    colors = colors,
                    onNotice = viewModel::notify,
                    code = space.code,
                )
            }
        }
    }
}

private val TodayFormat = DateTimeFormatter.ofPattern("EEE d MMM", java.util.Locale.forLanguageTag("es-MX"))

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateSheet(
    colors: OperationalColors,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onCreate: (String, SpaceType) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(SpaceType.TABLE) }
    AdminSheet(title = "Nuevo espacio", colors = colors, busy = saving, onDismiss = onDismiss) {
        OperationalField(value = name, onValueChange = {
            name = it.take(80)
        }, label = "Nombre", colors = colors, placeholder = "Ej. Mesa 5", enabled = !saving)
        Text("Tipo", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SpaceType.entries.forEach { option ->
                ChipButton(label = option.label, enabled = !saving, colors = colors, selected = option == type) {
                    type =
                        option
                }
            }
        }
        AdminSheetError(error)
        ActionPill(label = "Crear espacio", enabled = name.isNotBlank(), loading = saving, colors = colors) {
            onCreate(name, type)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BatchSheet(
    colors: OperationalColors,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onCreate: (SpaceType, String, Int?, Int?) -> Unit,
) {
    var type by remember { mutableStateOf(SpaceType.SEAT) }
    var prefix by remember { mutableStateOf("Asiento") }
    var from by remember { mutableStateOf("1") }
    var to by remember { mutableStateOf("100") }
    AdminSheet(title = "Crear varios a la vez", colors = colors, busy = saving, onDismiss = onDismiss) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SpaceType.batchTypes.forEach { option ->
                ChipButton(label = option.label, enabled = !saving, colors = colors, selected = option == type) {
                    if (prefix == type.label) prefix = option.label
                    type = option
                }
            }
        }
        OperationalField(value = prefix, onValueChange = {
            prefix = it.take(70)
        }, label = "Nombre base", colors = colors, enabled = !saving)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OperationalField(
                value = from,
                onValueChange = { from = it.filter(Char::isDigit).take(5) },
                label = "Del",
                colors = colors,
                enabled = !saving,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
            OperationalField(
                value = to,
                onValueChange = { to = it.filter(Char::isDigit).take(5) },
                label = "Al",
                colors = colors,
                enabled = !saving,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
        }
        val count = (to.toIntOrNull() ?: 0) - (from.toIntOrNull() ?: 0) + 1
        AdminHint(
            if (count >
                0
            ) {
                "Se crean \"${prefix.trim()} ${from}\" a \"${prefix.trim()} $to\" ($count). Los que ya existan se omiten."
            } else {
                "Revisa los números."
            },
            colors,
        )
        AdminSheetError(error)
        ActionPill(label = "Crear", enabled = count in 1..SpaceRules.MAX_BATCH, loading = saving, colors = colors) {
            onCreate(type, prefix, from.toIntOrNull(), to.toIntOrNull())
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditSheet(
    space: ManagedSpace,
    colors: OperationalColors,
    saving: Boolean,
    error: String?,
    viewModel: AdminSpacesViewModel,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var name by remember(space.id) { mutableStateOf(space.name) }
    var price by remember(
        space.id,
        space.hourlyPrice,
    ) { mutableStateOf(space.hourlyPrice?.removeSuffix(".00").orEmpty()) }
    var description by remember(space.id) { mutableStateOf(space.description.orEmpty()) }
    val features = remember(space.id) { mutableStateListOf<String>().apply { addAll(space.features) } }
    var newFeature by remember { mutableStateOf("") }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            val prepared = runCatching { prepareProductImage(context, uri) }.getOrNull()
            if (prepared == null) {
                viewModel.imageProblem("No se pudo preparar la foto. Prueba otra imagen de hasta 16 MB.")
            } else {
                viewModel.uploadImage(space, prepared.bytes, prepared.filename, prepared.mimeType)
            }
        }
    AdminSheet(title = space.name, colors = colors, busy = saving, onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OperationalField(value = name, onValueChange = {
                name = it.take(80)
            }, label = "Nombre", colors = colors, enabled = !saving, modifier = Modifier.weight(1f))
            ChipButton(
                label = "Guardar",
                enabled = !saving && name.trim() != space.name && name.isNotBlank(),
                colors = colors,
            ) {
                viewModel.rename(space, name)
            }
        }
        Text("Tipo", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SpaceType.entries.forEach { option ->
                ChipButton(
                    label = option.label,
                    enabled = !saving,
                    colors = colors,
                    selected =
                        option.wire == space.typeWire,
                ) {
                    viewModel.setType(space, option)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Activo", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                AdminHint("Desactivado, su QR no recibe pedidos.", colors)
            }
            Switch(
                checked = space.active,
                onCheckedChange = { viewModel.setActive(space, it) },
                enabled = !saving,
                colors =
                    SwitchDefaults.colors(
                        checkedTrackColor = colors.accentLime,
                        checkedThumbColor = colors.accentInk,
                    ),
            )
        }
        if (space.isCourt) {
            AdminSectionTitle("Renta de la cancha", colors)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OperationalField(
                    value = price,
                    onValueChange = { price = MoneyInput.sanitize(it).take(8) },
                    label = "Precio por hora (MXN)",
                    colors = colors,
                    placeholder = "Vacío = no se renta",
                    enabled = !saving,
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
                ChipButton(
                    label = "Guardar",
                    enabled = !saving,
                    colors = colors,
                ) { viewModel.setHourlyPrice(space, price) }
            }
            AdminSectionTitle("Ficha de la cancha", colors)
            if (space.imageUrl != null) {
                ProductImage(
                    imageUrl = space.imageUrl,
                    contentDescription = space.name,
                    modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(18.dp)),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipButton(
                    label =
                        if (space.imageUrl ==
                            null
                        ) {
                            "Subir foto"
                        } else {
                            "Cambiar foto"
                        },
                    enabled = !saving,
                    colors = colors,
                ) {
                    picker.launch("image/*")
                }
                if (space.imageUrl !=
                    null
                ) {
                    ChipButton(
                        label = "Quitar foto",
                        enabled = !saving,
                        colors = colors,
                    ) { viewModel.removeImage(space) }
                }
            }
            OperationalField(
                value = description,
                onValueChange = { description = it.take(500) },
                label = "Descripción",
                colors = colors,
                enabled = !saving,
                singleLine = false,
                minLines = 2,
            )
            Text("Características", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                (
                    features +
                        SpaceRules.FEATURE_SUGGESTIONS.filter { s -> features.none { it.equals(s, ignoreCase = true) } }
                ).forEach { feature ->
                    val on = features.any { it.equals(feature, ignoreCase = true) }
                    ChipButton(
                        label = feature,
                        enabled = !saving && (on || features.size < SpaceRules.MAX_FEATURES),
                        colors = colors,
                        selected = on,
                    ) {
                        if (on) {
                            features.removeAll {
                                it.equals(
                                    feature,
                                    ignoreCase = true,
                                )
                            }
                        } else {
                            features.add(feature)
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OperationalField(
                    value = newFeature,
                    onValueChange = { newFeature = it.take(30) },
                    label = "Otra característica",
                    colors = colors,
                    enabled = !saving && features.size < SpaceRules.MAX_FEATURES,
                    modifier = Modifier.weight(1f),
                )
                ChipButton(
                    label = "Agregar",
                    enabled =
                        newFeature.isNotBlank() && features.size < SpaceRules.MAX_FEATURES,
                    colors = colors,
                ) {
                    features.add(newFeature.trim())
                    newFeature = ""
                }
            }
            ChipButton(label = "Guardar ficha", enabled = !saving, colors = colors) {
                viewModel.saveProfile(space, description, features.toList())
            }
        }
        AdminSheetError(error)
        ActionPill(
            label = "Eliminar espacio",
            enabled = !saving,
            loading = false,
            colors = colors,
            primary = false,
            onClick = onDelete,
        )
    }
}

@Composable
private fun ReservationsCard(
    reservations: List<Reservation>,
    colors: OperationalColors,
    enabled: Boolean,
    onCharge: (Reservation) -> Unit,
    onRelease: (Reservation) -> Unit,
) {
    AdminCard(colors) {
        AdminSectionTitle("Reservas de hoy", colors)
        if (reservations.isEmpty()) AdminHint("No hay canchas reservadas hoy.", colors)
        reservations.forEach { reservation ->
            AdminValueRow(
                label = "${reservationTime(reservation)} · ${reservation.courtName ?: "Cancha"}",
                value = moneyLabel(reservation.amount),
                colors = colors,
                detail =
                    listOfNotNull(
                        reservation.customerName ?: "Cliente",
                        reservationLabel(reservation.state),
                    ).joinToString(" · "),
            )
            if (reservation.state == ReservationState.PENDING_PAYMENT) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipButton(label = "Cobrar", enabled = enabled, colors = colors) { onCharge(reservation) }
                    ChipButton(label = "Liberar cancha", enabled = enabled, colors = colors) { onRelease(reservation) }
                }
            }
        }
    }
}

@Composable
private fun ChargeSheet(
    reservation: Reservation,
    colors: OperationalColors,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onCharge: (String) -> Unit,
) {
    var received by remember { mutableStateOf(reservation.amount.removeSuffix(".00")) }
    AdminSheet(title = "Cobrar renta", colors = colors, busy = saving, onDismiss = onDismiss) {
        AdminValueRow(
            "${reservation.courtName ?: "Cancha"} · ${reservationTime(reservation)}",
            moneyLabel(reservation.amount),
            colors,
        )
        OperationalField(
            value = received,
            onValueChange = { received = MoneyInput.sanitize(it).take(10) },
            label = "Efectivo recibido",
            colors = colors,
            enabled = !saving,
            keyboardType = KeyboardType.Decimal,
        )
        AdminHint("Se cobra en efectivo y necesita la caja abierta.", colors)
        AdminSheetError(error)
        ActionPill(label = "Cobrar", enabled = received.isNotBlank(), loading = saving, colors = colors) {
            onCharge(
                com.vaiinilla.app.domain.admin.AdminProductRules
                    .money(received) ?: received,
            )
        }
    }
}

@Composable
private fun ScheduleCard(
    saved: ReservationSchedule?,
    colors: OperationalColors,
    enabled: Boolean,
    onSave: (ReservationSchedule) -> Unit,
) {
    var opening by remember(saved) { mutableStateOf(saved?.opening.orEmpty()) }
    var closing by remember(saved) { mutableStateOf(saved?.closing.orEmpty()) }
    var days by remember(saved) { mutableStateOf(saved?.daysAhead?.toString().orEmpty()) }
    AdminCard(colors) {
        AdminSectionTitle("Horario de reservas", colors)
        if (saved == null) {
            AdminHint("Cargando…", colors)
            return@AdminCard
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OperationalField(
                value = opening,
                onValueChange = { opening = it.filter { c -> c.isDigit() || c == ':' }.take(5) },
                label = "Abre",
                colors = colors,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
            OperationalField(
                value = closing,
                onValueChange = { closing = it.filter { c -> c.isDigit() || c == ':' }.take(5) },
                label = "Cierra",
                colors = colors,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
        }
        AdminHint("Si cierra antes de abrir, el horario cruza la medianoche.", colors)
        OperationalField(
            value = days,
            onValueChange = { days = it.filter(Char::isDigit).take(2) },
            label = "Días de anticipación para reservar",
            colors = colors,
            supporting = "De 0 a 60.",
            enabled = enabled,
            keyboardType = KeyboardType.Number,
        )
        val draft = ReservationSchedule(opening, closing, days.toIntOrNull() ?: -1)
        ChipButton(label = "Guardar horario", enabled = enabled && draft != saved, colors = colors) { onSave(draft) }
    }
}

private val ReservationTimeFormat = DateTimeFormatter.ofPattern("HH:mm")

private fun reservationTime(reservation: Reservation): String {
    val zone = BusinessTime.zone
    return "${ReservationTimeFormat.format(
        reservation.start.atZone(zone),
    )}–${ReservationTimeFormat.format(reservation.end.atZone(zone))}"
}

private fun reservationLabel(state: ReservationState): String =
    when (state) {
        ReservationState.PENDING_PAYMENT -> "Por pagar en efectivo"
        ReservationState.CONFIRMED, ReservationState.IN_PROGRESS, ReservationState.FINISHED -> "Pagada"
        else -> state.label
    }

/** "Mesa 2" antes que "Mesa 10": los números se comparan como números. */
private fun naturalKey(name: String): String = name.replace(Regex("\\d+")) { it.value.padStart(8, '0') }.lowercase()
