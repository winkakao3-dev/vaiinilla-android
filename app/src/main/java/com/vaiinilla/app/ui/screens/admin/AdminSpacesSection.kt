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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.vaiinilla.app.core.venue.LocalVenueContext
import com.vaiinilla.app.domain.admin.ManagedSpace
import com.vaiinilla.app.domain.admin.ReservationSchedule
import com.vaiinilla.app.domain.admin.SpaceDraft
import com.vaiinilla.app.domain.admin.SpaceRules
import com.vaiinilla.app.domain.admin.SpaceType
import com.vaiinilla.app.domain.model.MoneyInput
import com.vaiinilla.app.domain.model.Reservation
import com.vaiinilla.app.domain.model.ReservationState
import com.vaiinilla.app.domain.model.VenueContext
import com.vaiinilla.app.domain.time.BusinessTime
import com.vaiinilla.app.ui.admin.AdminSpacesViewModel
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.hotel.admin.HotelRoomsAdminSection
import com.vaiinilla.app.ui.screens.ActionPill
import com.vaiinilla.app.ui.screens.ChipButton
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.OperationalField
import com.vaiinilla.app.ui.screens.prepareProductImage
import kotlinx.coroutines.delay
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

    /** #28: cancelar una renta ya pagada que no ha empezado, con reembolso. */
    data class Refund(
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
    /** Cómo va cada espacio ahora, del tablero del mesero; vacío si no hay con qué cargarlo. */
    board: List<com.vaiinilla.app.domain.repository.BoardTable> = emptyList(),
) {
    val venue = LocalVenueContext.current
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
            SpacesPage.Reservations -> {
                // Las reservas llegan de los clientes sin aviso: con la página abierta se vuelven a leer cada tanto
                // (y al volver a la app); antes solo se veían al reiniciar (en vivo, #12).
                val lifecycleOwner = LocalLifecycleOwner.current
                LaunchedEffect(lifecycleOwner) {
                    lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        while (true) {
                            viewModel.refresh()
                            delay(RESERVATIONS_REFRESH_MS)
                        }
                    }
                }
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
                            state.schedule,
                            colors,
                            enabled,
                            onCharge = { sheet = SpacesSheet.Charge(it) },
                            onRelease = { sheet = SpacesSheet.Release(it) },
                            onRefund = { sheet = SpacesSheet.Refund(it) },
                        )
                    }
                    item { AdminH2("Horario de reservas", colors) }
                    item { ScheduleCard(state.schedule, colors, enabled, viewModel::saveSchedule) }
                }
            }
            null ->
                if (venue.capabilities.hasHotelAdmin) {
                    // En un hotel las mesas son habitaciones: su lista, el alta en lote y el QR de cada una (A01).
                    HotelRoomsAdminSection(colors, readOnly, onBack, onOpenRoom = { page = SpacesPage.Space(it) })
                } else {
                    SpacesList(
                        state,
                        colors,
                        readOnly,
                        enabled,
                        viewModel,
                        onBack,
                        board,
                        onOpen = { page = it },
                    ) { sheet = it }
                }
        }
    }

    val error = state.notice.takeIf { state.noticeIsError }
    when (val current = sheet) {
        SpacesSheet.Create, SpacesSheet.Batch ->
            CreateSheet(
                colors,
                state.saving,
                error,
                startWithMany = current == SpacesSheet.Batch,
                onDismiss = close,
                onCreateOne = { name, type, people -> viewModel.create(name, type, people, close) },
                onCreateMany = { type, prefix, from, to, people ->
                    viewModel.createBatch(type, prefix, from, to, people, close)
                },
            )
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
                title = "Liberar ${venue.vocabulary.court}",
                message =
                    "Se cancela la reserva de ${current.reservation.customerName ?: "este cliente"} " +
                        "y la ${venue.vocabulary.court} queda libre.",
                confirmLabel = "Liberar ${venue.vocabulary.court}",
                colors = colors,
                busy = state.saving,
                onDismiss = close,
            ) {
                viewModel.cancelReservation(current.reservation)
                close()
            }
        is SpacesSheet.Refund ->
            RefundSheet(current.reservation, colors, state.saving, error, close) { amount, reason ->
                viewModel.refundReservation(current.reservation, amount, reason, close)
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
    board: List<com.vaiinilla.app.domain.repository.BoardTable>,
    onOpen: (SpacesPage) -> Unit,
    onSheet: (SpacesSheet) -> Unit,
) {
    val venue = LocalVenueContext.current
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
                spaceGroups(venue, spaces).forEachIndexed { index, (title, members) ->
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
                                        board.firstOrNull { it.space.id == space.id }?.let(::boardStatus),
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

private fun boardStatus(table: com.vaiinilla.app.domain.repository.BoardTable): Pair<String, AdminBadgeTone> =
    when {
        table.call != null -> "Llamando" to AdminBadgeTone.DANGER
        table.orders.any { it.payAtEnd || it.paymentPending } -> "Cuenta abierta" to AdminBadgeTone.WARN
        table.orders.isNotEmpty() -> "Pidiendo" to AdminBadgeTone.OK
        table.availability != null &&
            table.availability.state != com.vaiinilla.app.domain.repository.SpaceAvailabilityState.LIBRE ->
            "Rentada" to AdminBadgeTone.OK
        else -> "Libre" to AdminBadgeTone.NEUTRAL
    }

private fun spaceGroups(
    venue: VenueContext,
    spaces: List<ManagedSpace>,
): List<Pair<String, List<ManagedSpace>>> {
    val sorted = spaces.sortedBy { naturalKey(it.name) }
    val order = SpaceType.entries.map { it.wire }.toMutableList()
    sorted.forEach { if (it.typeWire !in order) order.add(it.typeWire) }
    return order.mapNotNull { wire ->
        val members = sorted.filter { it.typeWire == wire }
        if (members.isEmpty()) return@mapNotNull null
        plural(SpaceType.entries.firstOrNull { it.wire == wire }?.label(venue) ?: wire) to members
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
    status: Pair<String, AdminBadgeTone>? = null,
    onClick: () -> Unit,
) {
    val venue = LocalVenueContext.current
    val price = space.hourlyPrice
    AdminRow(
        title = space.name,
        colors = colors,
        detail =
            when {
                space.isCourt && price != null -> "Renta ${moneyLabel(price)} por hora"
                space.capacity != null ->
                    if (space.capacity == 1) "1 persona" else "${space.capacity} personas"
                else -> space.code?.let { "Código $it" } ?: (space.type?.label(venue) ?: space.typeWire)
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
            status != null -> AdminBadge(status.first, colors, status.second)
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
    val venue = LocalVenueContext.current
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
            "Crea tus mesas, la barra o una ${venue.vocabulary.court}. Cada una tiene su QR para que el cliente pida desde su lugar.",
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
    val venue = LocalVenueContext.current
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
                    SpaceType.COURT ->
                        "Ponlo en la ${venue.vocabulary.court}. Al escanearlo, el cliente pide y su pedido llega aquí."
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
            AdminBigQr(
                url = space.qrUrl,
                name = space.name,
                colors = colors,
                onNotice = viewModel::notify,
                code = space.code,
            )
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
    startWithMany: Boolean,
    onDismiss: () -> Unit,
    onCreateOne: (String, SpaceType, Int?) -> Unit,
    onCreateMany: (SpaceType, String, Int?, Int?, Int?) -> Unit,
) {
    val venue = LocalVenueContext.current
    var many by remember { mutableStateOf(startWithMany) }
    var type by remember { mutableStateOf(SpaceType.TABLE) }
    var name by remember { mutableStateOf("") }
    var prefix by remember { mutableStateOf(SpaceType.TABLE.label(venue)) }
    var howMany by remember { mutableStateOf("6") }
    var from by remember { mutableStateOf("1") }
    var people by remember { mutableStateOf("") }
    AdminSheet(title = "Crear espacios", colors = colors, busy = saving, onDismiss = onDismiss) {
        AdminSegmented(
            options = listOf(false to "Uno", true to "Varios a la vez"),
            selected = many,
            colors = colors,
            onSelect = { many = it },
        )
        Text("Tipo", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            (if (many) SpaceType.batchTypes else SpaceType.entries).forEach { option ->
                // Fuera de un boliche el mismo tipo sirve para canchas y pistas: que se encuentre.
                val chip =
                    (if (option == SpaceType.COURT) venue.vocabulary.courtTypeChip else null) ?: option.label(venue)
                ChipButton(label = chip, enabled = !saving, colors = colors, selected = option == type) {
                    if (prefix == type.label(venue)) prefix = option.label(venue)
                    type = option
                }
            }
        }
        val peopleValue = people.toIntOrNull()
        val peopleField: @Composable (Modifier) -> Unit = { modifier ->
            OperationalField(
                value = people,
                onValueChange = { people = it.filter(Char::isDigit).take(2) },
                label = "Personas",
                colors = colors,
                enabled = !saving,
                placeholder = "4",
                keyboardType = KeyboardType.Number,
                modifier = modifier,
            )
        }
        if (many) {
            Text("Cuántos y cómo se llaman", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OperationalField(
                    value = prefix,
                    onValueChange = { prefix = it.take(70) },
                    label = "Nombre base",
                    colors = colors,
                    enabled = !saving,
                    modifier = Modifier.weight(1f),
                )
                OperationalField(
                    value = howMany,
                    onValueChange = { howMany = it.filter(Char::isDigit).take(3) },
                    label = "Cuántos",
                    colors = colors,
                    enabled = !saving,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OperationalField(
                    value = from,
                    onValueChange = { from = it.filter(Char::isDigit).take(5) },
                    label = "Empieza en",
                    colors = colors,
                    enabled = !saving,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
                peopleField(Modifier.weight(1f))
            }
            val count = howMany.toIntOrNull() ?: 0
            val first = from.toIntOrNull() ?: 0
            if (count in 1..SpaceRules.MAX_BATCH && prefix.isNotBlank()) {
                Text("Se van a crear", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    (0 until minOf(count, 12)).forEach { index ->
                        Text(
                            "${prefix.trim()} ${first + index}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary,
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(colors.cardInner)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                    if (count > 12) Text("y ${count - 12} más", fontSize = 12.sp, color = colors.textMuted)
                }
            }
            AdminSheetError(error)
            AdminPrimaryButton(
                title = "Crear $count ${spaceCountLabel(venue, type, count)} con su QR",
                colors = colors,
                busy = saving,
                enabled = count in 1..SpaceRules.MAX_BATCH && prefix.isNotBlank(),
            ) { onCreateMany(type, prefix, first, first + count - 1, peopleValue) }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OperationalField(
                    value = name,
                    onValueChange = { name = it.take(80) },
                    label = "Nombre",
                    colors = colors,
                    placeholder = "Ej. Mesa 5",
                    enabled = !saving,
                    modifier = Modifier.weight(2f),
                )
                peopleField(Modifier.weight(1f))
            }
            AdminSheetError(error)
            AdminPrimaryButton(
                title = "Crear con su QR",
                colors = colors,
                busy = saving,
                enabled = name.isNotBlank(),
            ) { onCreateOne(name, type, peopleValue) }
        }
    }
}

private fun spaceCountLabel(
    venue: VenueContext,
    type: SpaceType,
    count: Int,
): String {
    val one = type.label(venue).lowercase()
    return if (count == 1) one else plural(type.label(venue)).lowercase()
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
    val venue = LocalVenueContext.current
    val context = LocalContext.current
    var name by remember(space.id) { mutableStateOf(space.name) }
    var people by remember(space.id) { mutableStateOf(space.capacity?.toString().orEmpty()) }
    var rents by remember(space.id) { mutableStateOf(space.hourlyPrice != null) }
    var price by remember(space.id) { mutableStateOf(space.hourlyPrice?.removeSuffix(".00").orEmpty()) }
    var minimum by remember(space.id) { mutableStateOf(space.minimumMinutes) }
    var sport by remember(space.id) { mutableStateOf(space.sport.orEmpty()) }
    var surface by remember(space.id) { mutableStateOf(space.surface.orEmpty()) }
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
    AdminSheet(
        title =
            if (space.isCourt) {
                "Editar ${venue.vocabulary.court}"
            } else {
                "Editar ${space.type
                    ?.label(
                        venue,
                    )?.lowercase() ?: "espacio"}"
            },
        colors = colors,
        busy = saving,
        onDismiss = onDismiss,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OperationalField(
                value = name,
                onValueChange = { name = it.take(80) },
                label = "Nombre",
                colors = colors,
                enabled = !saving,
                modifier = Modifier.weight(2f),
            )
            OperationalField(
                value = people,
                onValueChange = { people = it.filter(Char::isDigit).take(2) },
                label = "Personas",
                colors = colors,
                enabled = !saving,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
        }
        if (!space.isCourt) {
            Text("Tipo", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SpaceType.entries.forEach { option ->
                    ChipButton(
                        label =
                            (if (option == SpaceType.COURT) venue.vocabulary.courtTypeChip else null)
                                ?: option.label(venue),
                        enabled = !saving,
                        colors = colors,
                        selected =
                            option.wire == space.typeWire,
                    ) {
                        viewModel.setType(space, option)
                    }
                }
            }
        }
        AdminToggleRow(
            title = "Activo",
            detail = "Desactivado, su QR no recibe pedidos.",
            checked = space.active,
            colors = colors,
            enabled = !saving,
            onChange = { viewModel.setActive(space, it) },
        )
        if (space.isCourt) {
            AdminToggleRow(
                title = "Se renta por hora",
                detail = "El cliente la reserva y paga la renta",
                checked = rents,
                colors = colors,
                enabled = !saving,
                onChange = { rents = it },
            )
            if (rents) {
                OperationalField(
                    value = price,
                    onValueChange = { price = MoneyInput.sanitize(it).take(8) },
                    label = "Precio por hora (MXN)",
                    colors = colors,
                    enabled = !saving,
                    keyboardType = KeyboardType.Decimal,
                )
                Text("Mínimo", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ChipButton(label = "Sin mínimo", enabled = !saving, colors = colors, selected = minimum == null) {
                        minimum =
                            null
                    }
                    SpaceRules.MINIMUM_RENTALS.forEach { minutes ->
                        ChipButton(
                            label = SpaceRules.minimumLabel(minutes),
                            enabled = !saving,
                            colors = colors,
                            selected = minimum == minutes,
                        ) { minimum = minutes }
                    }
                }
            }
            AdminSectionTitle("Ficha", colors)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OperationalField(
                    value = sport,
                    onValueChange = { sport = it.take(40) },
                    label = "Deporte",
                    colors = colors,
                    placeholder = venue.vocabulary.courtExampleSport,
                    enabled = !saving,
                    modifier = Modifier.weight(1f),
                )
                OperationalField(
                    value = surface,
                    onValueChange = { surface = it.take(40) },
                    label = "Superficie",
                    colors = colors,
                    placeholder = venue.vocabulary.courtExampleSurface,
                    enabled = !saving,
                    modifier = Modifier.weight(1f),
                )
            }
            Text("Características", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
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
                    enabled = newFeature.isNotBlank() && features.size < SpaceRules.MAX_FEATURES,
                    colors = colors,
                ) {
                    features.add(newFeature.trim())
                    newFeature = ""
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
            if (space.imageUrl != null) {
                ProductImage(
                    imageUrl = space.imageUrl,
                    contentDescription = space.name,
                    modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(18.dp)),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipButton(
                    label = if (space.imageUrl == null) "Subir foto" else "Cambiar foto",
                    enabled = !saving,
                    colors = colors,
                ) { picker.launch("image/*") }
                if (space.imageUrl != null) {
                    ChipButton(
                        label = "Quitar foto",
                        enabled = !saving,
                        colors = colors,
                    ) { viewModel.removeImage(space) }
                }
            }
        }
        AdminSheetError(error)
        ActionPill(
            label = "Guardar cambios",
            enabled = !saving && name.isNotBlank(),
            loading = saving,
            colors = colors,
        ) {
            viewModel.saveSpace(
                space,
                SpaceDraft(
                    name = name,
                    capacity = people.toIntOrNull(),
                    hourlyPrice = if (space.isCourt && rents) price else "",
                    minimumMinutes = minimum.takeIf { rents },
                    sport = sport,
                    surface = surface,
                    description = description,
                    features = features.toList(),
                ),
                onDismiss,
            )
        }
        AdminSecondaryButton("Eliminar espacio", colors, destructive = true, enabled = !saving, onClick = onDelete)
    }
}

/** Las reservas del día como el marco 50: hora, quién y su estado; los huecos del día salen como disponibles. */
@Composable
private fun ReservationsCard(
    reservations: List<Reservation>,
    schedule: ReservationSchedule?,
    colors: OperationalColors,
    enabled: Boolean,
    onCharge: (Reservation) -> Unit,
    onRelease: (Reservation) -> Unit,
    onRefund: (Reservation) -> Unit = {},
) {
    val now = java.time.Instant.now()
    AdminPanel(colors, padding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
        if (reservations.isEmpty()) {
            AdminRow(title = "Sin reservas hoy", colors = colors, detail = "Nadie la ha apartado") {
                AdminBadge("Libre", colors)
            }
        }
        reservations.forEachIndexed { index, reservation ->
            if (index > 0) AdminRowDivider(colors)
            val pending = reservation.state == ReservationState.PENDING_PAYMENT
            AdminRow(
                title = reservation.customerName ?: "Cliente",
                colors = colors,
                detail =
                    (reservation.courtName?.let { "$it · " } ?: "") +
                        // #13: cuántos juegan, para tener la pista lista con sus zapatos.
                        (
                            reservation.players
                                ?.takeIf { it > 0 }
                                ?.let { "$it ${if (it == 1) "jugador" else "jugadores"} · " } ?: ""
                        ) +
                        if (pending) {
                            "Paga al llegar · ${moneyLabel(
                                reservation.amount,
                            )}"
                        } else {
                            reservationLabel(reservation.state)
                        },
                leading = {
                    Column(Modifier.width(50.dp)) {
                        Text(
                            ReservationTimeFormat.format(reservation.start.atZone(BusinessTime.zone)),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = colors.textPrimary,
                        )
                        Text(
                            hoursLabel(reservation.durationMinutes),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textMuted,
                        )
                    }
                },
            ) {
                val (badge, tone) = reservationBadge(reservation.state)
                AdminBadge(badge, colors, tone)
            }
            if (pending) {
                Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminSecondaryButton(
                        "Liberar",
                        colors,
                        Modifier.weight(1f),
                        enabled = enabled,
                    ) { onRelease(reservation) }
                    AdminSecondaryButton(
                        "Cobrar",
                        colors,
                        Modifier.weight(1f),
                        filled = true,
                        enabled = enabled,
                    ) { onCharge(reservation) }
                }
            } else if (reservation.state == ReservationState.CONFIRMED && reservation.start.isAfter(now)) {
                // #28: pagada y sin empezar: el personal la cancela con reembolso (el cliente puede la suya).
                AdminSecondaryButton(
                    "Cancelar y reembolsar",
                    colors,
                    Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    destructive = true,
                    enabled = enabled,
                ) { onRefund(reservation) }
            }
        }
        // Un apartado vencido no ocupa la pista: lo que sigue libre se ofrece igual.
        freeGaps(reservations.filter { it.state != ReservationState.EXPIRED }, schedule).forEach { (start, minutes) ->
            AdminRowDivider(colors)
            AdminRow(
                title = "Disponible",
                colors = colors,
                detail = "Nadie la ha apartado",
                leading = {
                    Column(Modifier.width(50.dp)) {
                        Text(start, fontSize = 14.sp, fontWeight = FontWeight.Black, color = colors.textPrimary)
                        Text(
                            hoursLabel(minutes),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textMuted,
                        )
                    }
                },
            ) { AdminBadge("Libre", colors) }
        }
    }
}

private fun hoursLabel(minutes: Int): String =
    when {
        minutes % 60 == 0 -> "${minutes / 60} h"
        else -> "%.1f h".format(java.util.Locale.US, minutes / 60.0)
    }

/** Los huecos de hoy después de la última reserva, solo cuando hay un horario que no cruza la medianoche. */
private fun freeGaps(
    reservations: List<Reservation>,
    schedule: ReservationSchedule?,
): List<Pair<String, Int>> {
    if (schedule == null) return emptyList()
    val zone = BusinessTime.zone
    val closing = runCatching { java.time.LocalTime.parse(schedule.closing) }.getOrNull() ?: return emptyList()
    val opening = runCatching { java.time.LocalTime.parse(schedule.opening) }.getOrNull() ?: return emptyList()
    if (closing <= opening) return emptyList()
    val last = reservations.maxOfOrNull { it.end.atZone(zone).toLocalTime() } ?: return emptyList()
    val from = maxOf(last, opening)
    val minutes =
        java.time.Duration
            .between(from, closing)
            .toMinutes()
            .toInt()
    return if (minutes >= schedule.blockMinutes) listOf(ReservationTimeFormat.format(from) to minutes) else emptyList()
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
    val venue = LocalVenueContext.current
    var received by remember { mutableStateOf(reservation.amount.removeSuffix(".00")) }
    AdminSheet(title = "Cobrar renta", colors = colors, busy = saving, onDismiss = onDismiss) {
        AdminPanel(colors, padding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
            AdminValueRow("Espacio", reservation.courtName ?: venue.vocabulary.courtTitle, colors)
            reservation.customerName?.let { AdminValueRow("Cliente", it, colors) }
            reservation.players?.let { AdminValueRow("Jugadores", "$it", colors) }
            AdminValueRow("Horario", "Hoy · ${reservationTime(reservation)}", colors)
            AdminValueRow("Renta", hoursLabel(reservation.durationMinutes), colors)
        }
        OperationalField(
            value = received,
            onValueChange = { received = MoneyInput.sanitize(it).take(10) },
            label = "Efectivo recibido",
            colors = colors,
            enabled = !saving,
            keyboardType = KeyboardType.Decimal,
        )
        AdminHint("Se cobra en efectivo y necesita la caja abierta. Las rentas se cobran sin propina.", colors)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Total",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary,
                modifier = Modifier.weight(1f),
            )
            Text(
                moneyLabelCents(reservation.amount),
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = colors.textPrimary,
            )
        }
        AdminSheetError(error)
        AdminPrimaryButton(
            title = "Cobrar ${moneyLabelCents(reservation.amount)}",
            colors = colors,
            enabled = received.isNotBlank(),
            busy = saving,
        ) {
            onCharge(
                com.vaiinilla.app.domain.admin.AdminProductRules
                    .money(received) ?: received,
            )
        }
    }
}

/**
 * #28: cancelar una renta pagada desde Administración. El personal decide cuánto se devuelve (de 0 a lo
 * pagado) y por qué; el servidor lo regresa por la misma vía del pago (saldo, efectivo o tarjeta).
 */
@Composable
private fun RefundSheet(
    reservation: Reservation,
    colors: OperationalColors,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (amount: String, reason: String) -> Unit,
) {
    val venue = LocalVenueContext.current
    var amount by remember { mutableStateOf(reservation.amount.removeSuffix(".00")) }
    var reason by remember { mutableStateOf("") }
    val wire = MoneyInput.toWire(amount)
    val reasonOk = reason.trim().length in 3..240
    AdminSheet(title = "Cancelar renta pagada", colors = colors, busy = saving, onDismiss = onDismiss) {
        AdminPanel(colors, padding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
            AdminValueRow("Espacio", reservation.courtName ?: venue.vocabulary.courtTitle, colors)
            reservation.customerName?.let { AdminValueRow("Cliente", it, colors) }
            reservation.players?.let { AdminValueRow("Jugadores", "$it", colors) }
            AdminValueRow("Horario", reservationTime(reservation), colors)
            AdminValueRow("Renta", moneyLabelCents(reservation.amount), colors)
        }
        OperationalField(
            value = amount,
            onValueChange = { amount = MoneyInput.sanitize(it).take(10) },
            label = "Se devuelve",
            colors = colors,
            enabled = !saving,
            keyboardType = KeyboardType.Decimal,
        )
        OperationalField(
            value = reason,
            onValueChange = { reason = it.take(240) },
            label = "Motivo",
            colors = colors,
            placeholder = "Por qué se cancela",
            enabled = !saving,
            singleLine = false,
            minLines = 2,
        )
        AdminHint(
            "La ${venue.vocabulary.court} queda libre. " +
                "Se devuelve por donde pagó: saldo a su cartera, efectivo de la caja " +
                "y tarjeta queda pendiente para Caja. Pon 0 si no se devuelve nada.",
            colors,
        )
        AdminSheetError(error)
        AdminPrimaryButton(
            title = wire?.let { "Cancelar y devolver ${moneyLabelCents(it)}" } ?: "Cancelar y reembolsar",
            colors = colors,
            enabled = wire != null && reasonOk,
            busy = saving,
        ) { onConfirm(amount, reason) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
    var weekDays by remember(saved) { mutableStateOf(saved?.weekDays.orEmpty().toSet()) }
    var block by remember(saved) { mutableStateOf(saved?.blockMinutes ?: 30) }
    var durations by remember(saved) { mutableStateOf(saved?.durations?.toSet()) }
    if (saved == null) {
        AdminHint("Cargando…", colors)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WeekDayLetters.forEachIndexed { index, letter ->
                val day = index + 1
                val on = day in weekDays
                Box(
                    Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (on) colors.textPrimary else colors.cardBackground)
                        .physicalPress(enabled = enabled) { weekDays = if (on) weekDays - day else weekDays + day }
                        .semantics { contentDescription = "Día $letter, ${if (on) "abierto" else "cerrado"}" },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        letter,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = if (on) colors.background else colors.textMuted,
                    )
                }
            }
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
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Bloques de", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(30 to "30 min", 60 to "1 hora").forEach { (value, label) ->
                        AdminFilterChip(label, block == value, colors, onClick = { if (enabled) block = value })
                    }
                }
            }
            OperationalField(
                value = days,
                onValueChange = { days = it.filter(Char::isDigit).take(2) },
                label = "Días antes",
                colors = colors,
                enabled = enabled,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(0.7f),
            )
        }
        durations?.let { chosen ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Se renta por",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    (SpaceRules.RENTAL_DURATIONS + chosen).distinct().sorted().forEach { minutes ->
                        val on = minutes in chosen
                        AdminFilterChip(
                            durationChipLabel(minutes),
                            on,
                            colors,
                            onClick = {
                                // Siempre queda al menos una duración.
                                if (enabled && !(on && chosen.size == 1)) {
                                    durations = if (on) chosen - minutes else chosen + minutes
                                }
                            },
                        )
                    }
                }
            }
        }
        val draft =
            ReservationSchedule(
                opening,
                closing,
                days.toIntOrNull() ?: -1,
                weekDays.sorted(),
                block,
                durations?.sorted(),
            )
        AdminPrimaryButton(
            title = "Guardar horario",
            colors = colors,
            enabled = enabled && draft != saved,
        ) { onSave(draft) }
    }
}

private val WeekDayLetters = listOf("L", "M", "M", "J", "V", "S", "D")

/** "1 h", "1 h 30", "30 min". */
private fun durationChipLabel(minutes: Int): String =
    when {
        minutes < 60 -> "$minutes min"
        minutes % 60 == 0 -> "${minutes / 60} h"
        else -> "${minutes / 60} h ${minutes % 60}"
    }

private val ReservationTimeFormat = DateTimeFormatter.ofPattern("HH:mm")

private fun reservationTime(reservation: Reservation): String {
    val zone = BusinessTime.zone
    return "${ReservationTimeFormat.format(
        reservation.start.atZone(zone),
    )}–${ReservationTimeFormat.format(reservation.end.atZone(zone))}"
}

/**
 * R27: la insignia de cada estado. Un apartado vencido nunca se pagó y uno en conflicto hay que revisarlo:
 * ninguno sale en verde como «Pagada».
 */
internal fun reservationBadge(state: ReservationState): Pair<String, AdminBadgeTone> =
    when (state) {
        ReservationState.PENDING_PAYMENT -> "Por cobrar" to AdminBadgeTone.WARN
        ReservationState.CONFIRMED, ReservationState.IN_PROGRESS, ReservationState.FINISHED ->
            "Pagada" to AdminBadgeTone.OK
        ReservationState.EXPIRED -> "Vencida" to AdminBadgeTone.NEUTRAL
        ReservationState.CONFLICT -> "Revisar" to AdminBadgeTone.DANGER
        ReservationState.CANCELLED -> "Cancelada" to AdminBadgeTone.NEUTRAL
    }

private fun reservationLabel(state: ReservationState): String =
    when (state) {
        ReservationState.PENDING_PAYMENT -> "Por pagar en efectivo"
        ReservationState.CONFIRMED, ReservationState.IN_PROGRESS, ReservationState.FINISHED -> "Pagada"
        else -> state.label
    }

/** "Mesa 2" antes que "Mesa 10": los números se comparan como números. */
private fun naturalKey(name: String): String = name.replace(Regex("\\d+")) { it.value.padStart(8, '0') }.lowercase()

/** Cada cuánto se vuelven a leer las reservas con su página abierta. */
private const val RESERVATIONS_REFRESH_MS = 15_000L
