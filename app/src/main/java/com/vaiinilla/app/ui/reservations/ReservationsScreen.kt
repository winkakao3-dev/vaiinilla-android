package com.vaiinilla.app.ui.reservations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vaiinilla.app.domain.model.CourtDay
import com.vaiinilla.app.domain.model.CourtSchedule
import com.vaiinilla.app.domain.model.Reservation
import com.vaiinilla.app.domain.model.ReservationPaymentMethod
import com.vaiinilla.app.domain.model.ReservationSlots
import com.vaiinilla.app.domain.model.ReservationState
import com.vaiinilla.app.domain.model.Venue
import com.vaiinilla.app.domain.time.BusinessTime
import com.vaiinilla.app.ui.components.ClientCap
import com.vaiinilla.app.ui.components.ClientPageHeader
import com.vaiinilla.app.ui.components.ClientPillButton
import com.vaiinilla.app.ui.components.ClientSheetHandle
import com.vaiinilla.app.ui.components.EditorialAccentButton
import com.vaiinilla.app.ui.components.EditorialConfirmSheet
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.RollingText
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.screens.walletTile
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaColors
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private val MX = Locale.forLanguageTag("es-MX")
private val HOUR = DateTimeFormatter.ofPattern("HH:mm", MX)
private val DAY_SHORT = DateTimeFormatter.ofPattern("EEE d", MX)
private val DAY_LONG = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", MX)
private val DAY_NAME = DateTimeFormatter.ofPattern("EEEE d", MX)
private val WEEKDAY_SHORT = DateTimeFormatter.ofPattern("EEE", MX)
private val SPRING_COLOR = spring<Color>(stiffness = Spring.StiffnessMediumLow)

/**
 * Renta de canchas del cliente: elegir día, cancha, hora y duración, apartar 10 minutos y pagar.
 * La renta se paga primero; sin pagar no se puede pedir comida en la cancha.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationsScreen(
    state: ReservationsUiState,
    venueName: String?,
    onBack: () -> Unit,
    onSelectDate: (String) -> Unit,
    onSelectCourt: (Int) -> Unit,
    onRentNow: () -> Unit,
    onSelectStart: (Instant) -> Unit,
    onSelectDuration: (Int) -> Unit,
    onReserve: () -> Unit,
    onPay: (ReservationPaymentMethod) -> Unit,
    /** Boliche: cuántos juegan. */
    onSelectPlayers: (Int) -> Unit = {},
    onDismissPayment: () -> Unit,
    onResumePayment: (Reservation) -> Unit,
    onCancel: (Reservation) -> Unit,
    onDismissMessage: () -> Unit,
    /** Otro tiempo en la hoja de pago: suelta el apartado y aparta uno nuevo. */
    onChangeDuration: (Int) -> Unit = {},
    /** "Tarjeta" solo si el dueño la activó en su panel. */
    cardAvailable: Boolean = false,
    /** #8: "Saldo" solo si el negocio no lo apagó (`permite_saldo`); el backend lo rechaza si no. */
    balanceAvailable: Boolean = true,
    /** U03: cuántos juegan, desde la hoja de pago: suelta el apartado y aparta uno nuevo. */
    onChangePlayers: (Int) -> Unit = {},
    /** Sin cuenta: se pide el nombre y se entra como invitado antes de ver horarios. */
    onGuestNameChange: (String) -> Unit = {},
    onEnterAsGuest: () -> Unit = {},
    onSignIn: (() -> Unit)? = null,
    /** Cuando el lugar pide cuenta: llevar al registro. */
    onCreateAccount: (() -> Unit)? = null,
) {
    val colors = LocalVaiinillaColors.current
    val day = state.day
    val zone = remember(day?.timeZone) { BusinessTime.zoneOr(day?.timeZone) }
    var confirmCancel by remember { mutableStateOf<Reservation?>(null) }
    val listState = rememberLazyListState()
    // El aviso va arriba y el botón de apartar abajo: sin esto el error quedaba fuera de pantalla (D08).
    LaunchedEffect(state.error) {
        if (state.error != null) listState.animateScrollToItem(1)
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.paper)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "head") {
                ClientPageHeader(
                    title = "Renta tu ${Venue.court}",
                    subtitle = listOfNotNull(venueName, day?.let(::dayHeadline)).joinToString(" · "),
                    onBack = onBack,
                    modifier = Modifier.arrive(0),
                )
            }

            state.error?.let { message ->
                item(key = "error") { Banner(message, colors.coral, colors, onDismissMessage) }
            }
            state.notice?.let { message ->
                item(key = "notice") { Banner(message, colors.accent, colors, onDismissMessage) }
            }

            if (state.accountRequired) {
                item(key = "account-required") {
                    AccountRequiredCard(onCreateAccount = onCreateAccount, onSignIn = onSignIn, colors = colors)
                }
            } else if (state.guestEntry) {
                item(key = "guest-entry") {
                    GuestEntryCard(
                        name = state.guestName,
                        working = state.working,
                        onNameChange = onGuestNameChange,
                        onEnter = onEnterAsGuest,
                        onSignIn = onSignIn,
                        colors = colors,
                    )
                }
            } else if (day == null) {
                item(key = "loading") {
                    Text(
                        if (state.loading) "Cargando ${Venue.courts}…" else "Este lugar todavía no renta ${Venue.courts}.",
                        color = colors.muted,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(top = 24.dp).arrive(1),
                    )
                }
            } else if (day.courts.none { it.rentable }) {
                item(key = "none") {
                    Text(
                        "Por ahora las ${Venue.courts} de este lugar no se rentan desde la app. Pregunta en caja.",
                        color = colors.muted,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(top = 24.dp).arrive(1),
                    )
                }
            } else {
                item(key = "dates") {
                    DateStrip(
                        day = day,
                        selected = state.selectedDate ?: day.today,
                        onSelect = onSelectDate,
                        colors = colors,
                    )
                }
                // Un día sin servicio se dice una vez, no «Fuera de horario» en cada pista (en vivo, #16).
                if (day.closed) {
                    item(key = "closed-day") {
                        Text(
                            closedDayMessage(isToday = day.date == day.today),
                            color = colors.muted,
                            fontSize = 15.sp,
                            lineHeight = 21.sp,
                            modifier = Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp).arrive(2),
                        )
                    }
                }
                val listed = if (day.closed) emptyList() else day.courts.filter { it.rentable }
                itemsIndexed(listed, key = { _, court -> "court-${court.id}" }) { index, court ->
                    CourtCard(
                        court = court,
                        day = day,
                        zone = zone,
                        state = state,
                        order = index + 2,
                        onSelectCourt = onSelectCourt,
                        onRentNow = onRentNow,
                        onSelectStart = onSelectStart,
                        onSelectDuration = onSelectDuration,
                        onReserve = onReserve,
                        onSelectPlayers = onSelectPlayers,
                        colors = colors,
                        balanceAvailable = balanceAvailable,
                    )
                }
            }

            val mine = state.mine.filter { it.state.isLive || it.state == ReservationState.CONFLICT }
            if (mine.isNotEmpty()) {
                item(key = "mine-head") {
                    ClientCap("Mis reservas", modifier = Modifier.padding(start = 4.dp, top = 6.dp))
                }
                items(mine, key = { "mine-${it.id}" }) { reservation ->
                    Box(modifier = Modifier.animateItem()) {
                        MyReservationCard(
                            reservation = reservation,
                            zone = zone,
                            onPay = { onResumePayment(reservation) },
                            onCancel = { confirmCancel = reservation },
                            colors = colors,
                        )
                    }
                }
            }
        }

        confirmCancel?.let { reservation ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EditorialConfirmSheet(
                    title = "¿Cancelar la reserva?",
                    message =
                        if (reservation.state == ReservationState.PENDING_PAYMENT) {
                            "Se libera el horario y no se te cobra nada."
                        } else {
                            "Se libera la ${Venue.court}, pero lo que pagaste no se devuelve."
                        },
                    confirmLabel = "Sí, cancelar",
                    dismissLabel = "No, mantenerla",
                    onConfirm = {
                        onCancel(reservation)
                        confirmCancel = null
                    },
                    onDismiss = { confirmCancel = null },
                )
            }
        }
    }

    state.pending?.let { reservation ->
        ModalBottomSheet(
            onDismissRequest = onDismissPayment,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.paper,
            scrimColor = Color.Black.copy(alpha = 0.5f),
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            dragHandle = { ClientSheetHandle() },
        ) {
            ReservationPaymentContent(
                reservation = reservation,
                zone = zone,
                working = state.working,
                onPay = onPay,
                colors = colors,
                error = state.error,
                // Sin cuenta no hay saldo ni tarjeta: el backend los rechaza para invitados.
                cardAvailable = cardAvailable && !state.guestMode,
                balanceAvailable = balanceAvailable && !state.guestMode,
                day = day,
                startsNow = state.pendingStartsNow,
                onChangeDuration = onChangeDuration,
                onChangePlayers = onChangePlayers,
            )
        }
    }
}
/** «hoy, martes 3», «mañana, miércoles 4» o «jueves 5»: el día que se está viendo. */
private fun dayHeadline(day: CourtDay): String {
    val date = LocalDate.parse(day.date)
    val today = LocalDate.parse(day.today)
    val name = date.format(DAY_NAME)
    return when (date) {
        today -> "hoy, $name"
        today.plusDays(1) -> "mañana, $name"
        else -> name
    }
}

@Composable
private fun Banner(
    message: String,
    tone: Color,
    colors: VaiinillaColors,
    onDismiss: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .arrive(0, key = message)
                .clip(RoundedCornerShape(18.dp))
                .background(tone.copy(alpha = 0.16f))
                .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(message, color = colors.ink, fontSize = 14.sp, lineHeight = 19.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = onDismiss) { Text("Ok", color = colors.ink, fontWeight = FontWeight.ExtraBold) }
    }
}

@Composable
private fun DateStrip(
    day: CourtDay,
    selected: String,
    onSelect: (String) -> Unit,
    colors: VaiinillaColors,
) {
    val today = remember(day.today) { LocalDate.parse(day.today) }
    val dates =
        remember(day.today, day.daysAhead) { (0..day.daysAhead.coerceAtMost(13)).map { today.plusDays(it.toLong()) } }
    LazyRow(modifier = Modifier.arrive(1), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(dates, key = { it.toString() }) { date ->
            val iso = date.toString()
            val isSelected = iso == selected
            val label =
                when (date) {
                    today -> "Hoy"
                    today.plusDays(1) -> "Mañana"
                    else -> date.format(DAY_SHORT).replace(".", "")
                }
            Chip(label = label, selected = isSelected, enabled = true, onClick = { onSelect(iso) }, colors = colors)
        }
    }
}

/** Una cancha como en L07 de Doop: foto, estado, precio, «Rentar ahora» y las horas del día. */
@Composable
private fun CourtCard(
    court: CourtSchedule,
    day: CourtDay,
    zone: ZoneId,
    state: ReservationsUiState,
    order: Int,
    onSelectCourt: (Int) -> Unit,
    onRentNow: () -> Unit,
    onSelectStart: (Instant) -> Unit,
    onSelectDuration: (Int) -> Unit,
    onReserve: () -> Unit,
    colors: VaiinillaColors,
    onSelectPlayers: (Int) -> Unit = {},
    balanceAvailable: Boolean = true,
) {
    val selected = court.id == state.selectedCourtId
    val isToday = day.date == day.today
    val busyUntil = if (isToday) ReservationSlots.busyUntil(court, day.now) else null
    val canNow = isToday && ReservationSlots.canRentNow(day, court)
    val starts = remember(day, court.id) { ReservationSlots.startTimes(day) }
    val price = court.customerPricePerHour?.let { "${moneyLabel(it.cashOrBalance)} la hora" }
    val trait = (court.profile.surface ?: court.profile.features.firstOrNull())?.replaceFirstChar { it.lowercase() }
    val detail =
        if (busyUntil != null) {
            listOfNotNull("Se libera a las ${busyUntil.atZone(zone).format(HOUR)}", price)
        } else {
            listOfNotNull(price, trait)
        }
    val start =
        when {
            !selected -> null
            // "Rentar ahora" aparta de una vez: el tiempo se elige en la hoja de pago. En un boliche el resumen se
            // abre desde ahora para preguntar cuántos juegan antes de apartar (R11).
            state.rentNow -> day.now.takeIf { Venue.isBowling && state.pending == null }
            else -> state.selectedStart
        }
    var zoomed by remember(court.id) { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .arrive(order, key = court.id)
                .clip(RoundedCornerShape(22.dp))
                .background(colors.paper2)
                .animateContentSize(spring(dampingRatio = 0.88f, stiffness = 380f))
                .padding(bottom = 14.dp),
    ) {
        court.profile.imageUrl?.let { url ->
            ProductImage(
                imageUrl = url,
                contentDescription = "Foto de ${court.name}",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(118.dp)
                        .physicalPress { zoomed = true },
                contentScale = ContentScale.Crop,
            )
        }
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                listOfNotNull(court.name, court.profile.sport).joinToString(" · "),
                color = colors.ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            CourtStatusTag(
                status = courtStatus(day = day, court = court, freeNow = canNow),
                opensAt = day.opensAt.atZone(zone).format(HOUR),
                colors = colors,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
        if (detail.isNotEmpty()) {
            Text(
                detail.joinToString(" · "),
                color = colors.muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp),
            )
        }
        if (canNow) {
            RentNowPill(
                selected = selected && state.rentNow,
                working = state.working,
                onClick = {
                    if (!selected) onSelectCourt(court.id)
                    onRentNow()
                },
                colors = colors,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
            )
        }
        ClientCap(
            if (isToday) "O aparta una hora de hoy" else "Elige la hora",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp),
        )
        if (starts.isEmpty()) {
            Text(
                "Ya no quedan horarios este día.",
                color = colors.muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 9.dp),
            )
        } else {
            HourRow(
                starts = starts,
                day = day,
                court = court,
                zone = zone,
                selected = state.selectedStart.takeIf { selected && !state.rentNow },
                onSelect = { chosen ->
                    if (!selected) onSelectCourt(court.id)
                    onSelectStart(chosen)
                },
                colors = colors,
                modifier = Modifier.padding(top = 9.dp),
            )
        }
        if (start != null) {
            Column(modifier = Modifier.arrive(0, key = if (state.rentNow) RENT_NOW_KEY else start)) {
                DurationRow(
                    available = ReservationSlots.availableDurations(day, court, start),
                    all = ReservationSlots.durationsFor(day, court),
                    selected = state.selectedDuration,
                    onSelect = onSelectDuration,
                    colors = colors,
                )
                Summary(
                    state = state,
                    day = day,
                    court = court,
                    start = start,
                    zone = zone,
                    onReserve = onReserve,
                    onSelectPlayers = onSelectPlayers,
                    colors = colors,
                    balanceAvailable = balanceAvailable,
                )
            }
        }
    }

    val zoomUrl = court.profile.imageUrl
    if (zoomed && zoomUrl != null) {
        Dialog(onDismissRequest = { zoomed = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .physicalPress { zoomed = false },
                contentAlignment = Alignment.Center,
            ) {
                ProductImage(
                    imageUrl = zoomUrl,
                    contentDescription = "Foto de ${court.name}",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit,
                )
                IconButton(
                    onClick = { zoomed = false },
                    modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp),
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Cerrar foto", tint = Color.White)
                }
            }
        }
    }
}

/** Lo que dice la etiqueta de cada cancha o pista. */
internal enum class CourtStatus {
    BUSY,
    FREE_NOW,
    BUSY_SOON,
    CLOSING_SOON,
    OPENS_LATER,
    CLOSED_FOR_TODAY,
    FREE_ALL_DAY,
    SOME_BUSY,
}

/**
 * Hoy: ocupada si alguien la usa ahora; libre si se puede rentar ya; antes de abrir o después de cerrar, el horario;
 * y dentro del horario sin caber la renta más corta, que se ocupa pronto o que ya va a cerrar. Una ocupada decía
 * «Fuera de horario» (en vivo, #16).
 */
internal fun courtStatus(
    day: CourtDay,
    court: CourtSchedule,
    freeNow: Boolean,
): CourtStatus {
    if (day.date != day.today) return if (court.busy.isEmpty()) CourtStatus.FREE_ALL_DAY else CourtStatus.SOME_BUSY
    return when {
        ReservationSlots.busyUntil(court, day.now) != null -> CourtStatus.BUSY
        freeNow -> CourtStatus.FREE_NOW
        day.now.isBefore(day.opensAt) -> CourtStatus.OPENS_LATER
        !day.now.isBefore(day.closesAt) -> CourtStatus.CLOSED_FOR_TODAY
        else -> {
            val shortest = ReservationSlots.durationsFor(day, court).minOrNull() ?: day.blockMinutes
            val until = day.now.plus(Duration.ofMinutes(shortest.toLong()))
            val reservedSoon = court.busy.any { it.start.isAfter(day.now) && it.start.isBefore(until) }
            if (reservedSoon) CourtStatus.BUSY_SOON else CourtStatus.CLOSING_SOON
        }
    }
}

internal fun closedDayMessage(isToday: Boolean): String =
    if (isToday) {
        "Hoy el lugar no abre: no se rentan ${Venue.courts}. Elige otro día para apartar."
    } else {
        "Ese día el lugar no abre. Elige otro día para apartar."
    }

@Composable
private fun CourtStatusTag(
    status: CourtStatus,
    opensAt: String,
    colors: VaiinillaColors,
    modifier: Modifier = Modifier,
) {
    val free = colors.accent.copy(alpha = 0.14f) to if (colors.isDark) colors.accent else colors.accentInk
    val busy = colors.coral.copy(alpha = 0.14f) to coralInk(colors)
    val quiet = colors.ink.copy(alpha = 0.08f) to colors.muted
    val (label, tone) =
        when (status) {
            CourtStatus.BUSY -> "Ocupada" to busy
            CourtStatus.FREE_NOW -> "Libre ahora" to free
            CourtStatus.BUSY_SOON -> "Se ocupa pronto" to busy
            CourtStatus.CLOSING_SOON -> "Cierra pronto" to quiet
            CourtStatus.OPENS_LATER -> "Abre a las $opensAt" to quiet
            CourtStatus.CLOSED_FOR_TODAY -> "Ya cerró por hoy" to quiet
            CourtStatus.FREE_ALL_DAY -> "Libre todo el día" to free
            CourtStatus.SOME_BUSY -> "Con horarios ocupados" to quiet
        }
    StatusTag(label = label, background = tone.first, content = tone.second, modifier = modifier)
}

@Composable
private fun StatusTag(
    label: String,
    background: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .height(24.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(background)
                .padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = content, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

private fun coralInk(colors: VaiinillaColors): Color = if (colors.isDark) Color(0xFFFF8B86) else colors.coral

@Composable
private fun RentNowPill(
    selected: Boolean,
    working: Boolean,
    onClick: () -> Unit,
    colors: VaiinillaColors,
    modifier: Modifier = Modifier,
) {
    val container by animateColorAsState(if (selected) colors.accent else colors.ink, SPRING_COLOR, label = "now-bg")
    val content by animateColorAsState(if (selected) colors.accentInk else colors.onInk, SPRING_COLOR, label = "now-ink")
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(46.dp)
                .physicalPress(enabled = !working, onClick = onClick)
                .clip(RoundedCornerShape(99.dp))
                .background(container),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(visible = selected) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = null,
                tint = content,
                modifier = Modifier.padding(end = 6.dp).size(18.dp),
            )
        }
        Text(
            if (selected && working) "Apartando…" else "Rentar ahora",
            color = content,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun HourRow(
    starts: List<Instant>,
    day: CourtDay,
    court: CourtSchedule,
    zone: ZoneId,
    selected: Instant?,
    onSelect: (Instant) -> Unit,
    colors: VaiinillaColors,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    // Se abre en la primera hora libre, dejando una tachada antes para que se entienda por qué se saltó.
    val firstOpen =
        remember(starts, court.busy) {
            starts.indexOfFirst { ReservationSlots.isStartAvailable(day, court, it) }.coerceAtLeast(1) - 1
        }
    LaunchedEffect(firstOpen) { listState.scrollToItem(firstOpen) }
    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        items(starts, key = { it.toEpochMilli() }) { start ->
            HourChip(
                label = start.atZone(zone).format(HOUR),
                selected = start == selected,
                enabled = ReservationSlots.isStartAvailable(day, court, start),
                onClick = { onSelect(start) },
                colors = colors,
            )
        }
    }
}

@Composable
private fun HourChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    colors: VaiinillaColors,
) {
    val container by animateColorAsState(
        if (selected) colors.ink else walletTile(colors),
        SPRING_COLOR,
        label = "hour-bg",
    )
    val ink by animateColorAsState(
        if (selected) colors.onInk else colors.ink.copy(alpha = 0.88f),
        SPRING_COLOR,
        label = "hour-ink",
    )
    val open = enabled || selected
    Box(
        modifier =
            Modifier
                .height(44.dp)
                .graphicsLayer { alpha = if (open) 1f else 0.35f }
                .physicalPress(enabled = enabled, scale = PhysicalPressScale.Small, onClick = onClick)
                .clip(RoundedCornerShape(12.dp))
                .background(container)
                .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = ink,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.ExtraBold,
            textDecoration = if (open) null else TextDecoration.LineThrough,
        )
    }
}

@Composable
private fun DurationRow(
    available: List<Int>,
    all: List<Int>,
    selected: Int?,
    onSelect: (Int) -> Unit,
    colors: VaiinillaColors,
) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        ClientCap("Cuánto tiempo", modifier = Modifier.padding(horizontal = 16.dp))
        LazyRow(
            modifier = Modifier.padding(top = 9.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            items(all, key = { it }) { minutes ->
                HourChip(
                    label = durationLabel(minutes),
                    selected = minutes == selected,
                    enabled = minutes in available,
                    onClick = { onSelect(minutes) },
                    colors = colors,
                )
            }
        }
    }
}
@Composable
private fun AccountRequiredCard(
    onCreateAccount: (() -> Unit)?,
    onSignIn: (() -> Unit)?,
    colors: VaiinillaColors,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .arrive(1)
                .clip(RoundedCornerShape(26.dp))
                .border(1.dp, colors.line, RoundedCornerShape(26.dp))
                .padding(20.dp),
    ) {
        Text(
            "Necesitas una cuenta",
            color = colors.ink,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.6).sp,
        )
        Text(
            "Aquí las ${Venue.courts} se rentan con cuenta. Crea una o inicia sesión para apartar.",
            color = colors.muted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
        Spacer(Modifier.height(16.dp))
        if (onCreateAccount != null) {
            EditorialAccentButton(text = "Crear cuenta", onClick = onCreateAccount)
        }
        if (onSignIn != null) {
            TextButton(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) {
                Text("Ya tengo cuenta, iniciar sesión", color = colors.ink)
            }
        }
    }
}

@Composable
private fun GuestEntryCard(
    name: String,
    working: Boolean,
    onNameChange: (String) -> Unit,
    onEnter: () -> Unit,
    onSignIn: (() -> Unit)?,
    colors: VaiinillaColors,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .arrive(1)
                .clip(RoundedCornerShape(26.dp))
                .border(1.dp, colors.line, RoundedCornerShape(26.dp))
                .padding(20.dp),
    ) {
        Text(
            "Renta sin cuenta",
            color = colors.ink,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.6).sp,
        )
        Text(
            "Para ver horarios y apartar una ${Venue.court} solo necesitamos tu nombre. " +
                "Al entrar aceptas los términos y el aviso de privacidad vigentes.",
            color = colors.muted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Tu nombre") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            enabled = !working,
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.ink,
                    unfocusedTextColor = colors.ink,
                    focusedBorderColor = colors.ink,
                    unfocusedBorderColor = colors.line,
                    focusedLabelColor = colors.muted,
                    unfocusedLabelColor = colors.muted,
                    cursorColor = colors.ink,
                ),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
        Spacer(Modifier.height(12.dp))
        EditorialAccentButton(
            text = if (working) "Entrando…" else "Continuar",
            onClick = onEnter,
            enabled = !working && name.isNotBlank(),
        )
        if (onSignIn != null) {
            TextButton(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) {
                Text("¿Ya tienes cuenta? Inicia sesión", color = colors.ink)
            }
        }
    }
}

@Composable
private fun Summary(
    state: ReservationsUiState,
    day: CourtDay,
    court: CourtSchedule,
    start: Instant,
    zone: ZoneId,
    onReserve: () -> Unit,
    colors: VaiinillaColors,
    onSelectPlayers: (Int) -> Unit = {},
    /** #8: sin saldo, el precio dice solo «en efectivo». */
    balanceAvailable: Boolean = true,
) {
    val minutes = state.selectedDuration
    val end = minutes?.let { start.plusSeconds(it * 60L) }
    val price =
        minutes?.let { m ->
            court.customerPricePerHour?.let { ReservationSlots.amountFor(it.cashOrBalance, m) }
        }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.line),
        )
        Text(
            "${court.name} · ${dayLabel(day, zone, start)}",
            color = colors.muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 14.dp),
        )
        Text(
            if (end != null) {
                "${if (state.rentNow) "Ahora" else start.atZone(zone).format(HOUR)} – ${end.atZone(zone).format(HOUR)}"
            } else {
                "Elige cuánto tiempo"
            },
            color = colors.ink,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.8).sp,
            modifier = Modifier.padding(top = 2.dp),
        )
        if (price != null) {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 10.dp)) {
                Text("$", color = colors.ink, fontSize = 22.sp, fontWeight = FontWeight.Black)
                RollingText(
                    text = price,
                    style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Black),
                    color = colors.ink,
                )
                Text(
                    if (balanceAvailable && !state.guestMode) "  con efectivo o saldo" else "  en efectivo",
                    color = colors.muted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
        Text(
            "Se aparta ${if (day.holdMinutes == 1) "1 minuto" else "${day.holdMinutes} minutos"} mientras pagas. " +
                "Si cancelas, lo pagado no se devuelve.",
            color = colors.muted,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier.padding(top = 10.dp),
        )
        if (Venue.isBowling) {
            PlayersRow(
                players = state.players,
                enabled = !state.working,
                onChange = onSelectPlayers,
                colors = colors,
                max = court.maxPlayers,
            )
        }
        Spacer(Modifier.height(16.dp))
        EditorialAccentButton(
            text =
                when {
                    state.working -> "Apartando…"
                    // R11: en un boliche no se aparta sin saber cuántos juegan.
                    Venue.isBowling && state.players == 0 -> "Anota cuántos juegan"
                    else -> "Apartar ${Venue.court}"
                },
            onClick = onReserve,
            enabled = state.canReserve,
        )
    }
}

const val RESERVATION_PAY_TAG = "reservation-pay"

/** Cuánto se espera tras el último toque de jugadores antes de volver a apartar. */
private const val PLAYERS_SETTLE_MS = 700L

/** Llave de la animación del resumen de «Rentar ahora»: su inicio (la hora del servidor) cambia en cada sondeo. */
private const val RENT_NOW_KEY = "rentar-ahora"

/** Boliche: cuántos juegan en la pista, para tenerla lista con sus zapatos. */
@Composable
private fun PlayersRow(
    players: Int,
    enabled: Boolean,
    onChange: (Int) -> Unit,
    colors: VaiinillaColors,
    /** R25: la capacidad de la pista; el «+» se apaga al llegar. */
    max: Int = ReservationSlots.MAX_PLAYERS,
    /** R11: con un apartado ya hecho no se regresa a «Sin anotar». */
    min: Int = 0,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text("¿Cuántos juegan?", color = colors.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (players == 0) "Sin anotar" else "$players ${if (players == 1) "jugador" else "jugadores"}",
                color = colors.ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
            )
        }
        listOf("−" to players - 1, "+" to players + 1).forEach { (label, next) ->
            val active = enabled && next in min..max
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (active) colors.ink else colors.line)
                        .clickable(enabled = active) { onChange(next) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = colors.paper, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

/**
 * Pago de una cancha apartada como el dibujo de Doop (L08): cuánto tiempo, cómo pagas y un solo botón.
 * El servidor no cambia la duración de un apartado, así que otro tiempo suelta este y aparta uno nuevo
 * ([onChangeDuration]).
 */
@Composable
internal fun ReservationPaymentContent(
    reservation: Reservation,
    zone: ZoneId,
    working: Boolean,
    onPay: (ReservationPaymentMethod) -> Unit,
    colors: VaiinillaColors,
    cardAvailable: Boolean = false,
    balanceAvailable: Boolean = true,
    error: String? = null,
    day: CourtDay? = null,
    /** "Rentar ahora": el tiempo empezó al apartar, no a una hora elegida. */
    startsNow: Boolean = false,
    onChangeDuration: (Int) -> Unit = {},
    /** U03: boliche, cuántos juegan; null no muestra el stepper. */
    onChangePlayers: ((Int) -> Unit)? = null,
) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    // Cambiar jugadores vuelve a apartar: los toques se juntan y se manda solo el último.
    val askPlayers = Venue.isBowling && onChangePlayers != null
    var players by remember(reservation.id) { mutableIntStateOf(reservation.players ?: 0) }
    val playersDirty = askPlayers && players != (reservation.players ?: 0)
    // R11: un apartado sin jugadores (de antes de pedirlos) no se paga ni se cambia hasta anotarlos.
    val playersMissing = askPlayers && players == 0
    // Si volver a apartar falla y el apartado sigue, el stepper regresa a lo que tiene la reserva.
    LaunchedEffect(working) { if (!working) players = reservation.players ?: 0 }
    if (askPlayers) {
        LaunchedEffect(reservation.id, players) {
            if (players == (reservation.players ?: 0)) return@LaunchedEffect
            delay(PLAYERS_SETTLE_MS)
            onChangePlayers?.invoke(players)
        }
    }
    LaunchedEffect(reservation.id) {
        while (true) {
            delay(1_000)
            nowMs = System.currentTimeMillis()
        }
    }
    val remaining = reservation.holdExpiresAt?.let { (it.toEpochMilli() - nowMs).coerceAtLeast(0) }
    val expired = remaining == 0L
    val methods =
        listOfNotNull(
            ReservationPaymentMethod.CARD.takeIf { cardAvailable },
            ReservationPaymentMethod.BALANCE.takeIf { balanceAvailable },
            ReservationPaymentMethod.CASH,
        )
    var picked by rememberSaveable(reservation.id) { mutableStateOf(methods.first()) }
    val method = picked.takeIf { it in methods } ?: methods.first()
    var requested by remember(reservation.id) { mutableStateOf<Int?>(null) }
    val court = day?.courts?.firstOrNull { it.id == reservation.courtId }
    val held = reservation.durationMinutes
    val durations =
        if (day != null && court != null) {
            (ReservationSlots.durationsFor(day, court) + held).distinct().sorted()
        } else {
            listOf(held)
        }
    val available =
        if (day != null && court != null) {
            val withoutThisHold = court.copy(busy = court.busy.filterNot { it.start == reservation.start && it.end == reservation.end })
            ReservationSlots.availableDurations(day, withoutThisHold, if (startsNow) day.now else reservation.start) + held
        } else {
            listOf(held)
        }
    val heldPrice =
        reservation.customerPrice?.let { if (method == ReservationPaymentMethod.CARD) it.card else it.cashOrBalance }
            ?: reservation.amount
    val hourPrice =
        court?.customerPricePerHour?.let { if (method == ReservationPaymentMethod.CARD) it.card else it.cashOrBalance }
    val changing = working && requested != null
    val times = "${reservation.start.atZone(zone).format(HOUR)} – ${reservation.end.atZone(zone).format(HOUR)}"
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 6.dp).arrive(0),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    listOfNotNull(reservation.courtName ?: Venue.courtTitle, court?.profile?.sport).joinToString(" · "),
                    color = colors.ink,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    lineHeight = 30.sp,
                )
                Text(
                    when {
                        startsNow -> "Desde ahora hasta las ${reservation.end.atZone(zone).format(HOUR)}"
                        day != null -> "${dayLabel(day, zone, reservation.start)} · $times"
                        else -> times
                    },
                    color = colors.muted,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (remaining != null) HoldTag(remaining, colors)
        }
        ClientCap("Elige cuánto tiempo", modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp))
        DurationTiles(
            durations = durations,
            available = available,
            selected = requested.takeIf { changing } ?: held,
            priceFor = { minutes ->
                if (minutes == held) heldPrice else hourPrice?.let { ReservationSlots.amountFor(it, minutes) }
            },
            enabled = !working && !expired && !playersDirty && !playersMissing,
            onSelect = { minutes ->
                if (minutes != held) {
                    requested = minutes
                    onChangeDuration(minutes)
                }
            },
            colors = colors,
            modifier = Modifier.padding(top = 10.dp).arrive(1),
        )
        if (askPlayers) {
            // U03: «Rentar ahora» aparta sin pasar por el resumen; aquí se anota cuántos juegan.
            Box(Modifier.padding(horizontal = 20.dp).arrive(1)) {
                PlayersRow(
                    players = players,
                    enabled = !working && !expired,
                    onChange = { players = it },
                    colors = colors,
                    max = court?.maxPlayers ?: ReservationSlots.MAX_PLAYERS,
                    min = 1,
                )
            }
        }
        ClientCap("Cómo pagas", modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp))
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            methods.forEachIndexed { index, option ->
                PayMethodOption(
                    method = option,
                    selected = option == method,
                    enabled = !working && !expired,
                    onClick = { picked = option },
                    colors = colors,
                    modifier = Modifier.arrive(index + 2),
                )
            }
        }
        // La hoja tapa el aviso de la pantalla: si el pago falla, el motivo va aquí, junto a las opciones.
        error?.let { message ->
            Text(
                message,
                color = coralInk(colors),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp,
                modifier =
                    Modifier
                        .padding(start = 20.dp, end = 20.dp, top = 12.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Text(
            if (expired) {
                "Venció el apartado: vuelve a elegir el horario."
            } else {
                "Lo que pidas del menú te llega a la ${Venue.court}. Si cancelas, lo pagado no se devuelve."
            },
            color = if (expired) coralInk(colors) else colors.muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 18.sp,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp),
        )
        ClientPillButton(
            text =
                when {
                    changing || playersDirty -> "Apartando…"
                    working -> "Pagando…"
                    playersMissing -> "Anota cuántos juegan"
                    method == ReservationPaymentMethod.CASH -> "Pagar ${moneyLabel(heldPrice)} en caja"
                    else -> "Pagar ${moneyLabel(heldPrice)}"
                },
            onClick = { onPay(method) },
            enabled = !working && !expired && !playersDirty && !playersMissing,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp).testTag(RESERVATION_PAY_TAG),
        )
    }
}

/** "Apartada 4:52": amarilla mientras corre, roja en el último minuto y al vencer. */
@Composable
private fun HoldTag(
    remaining: Long,
    colors: VaiinillaColors,
) {
    val urgent = remaining < 60_000
    Box(
        modifier =
            Modifier
                .height(30.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background((if (urgent) colors.coral else colors.yolk).copy(alpha = if (colors.isDark) 0.14f else 0.3f))
                .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (remaining > 0) "Apartada ${clock(remaining)}" else "Venció",
            color =
                when {
                    urgent -> coralInk(colors)
                    colors.isDark -> colors.yolk
                    else -> colors.ink
                },
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Black,
            style = TextStyle(fontFeatureSettings = "tnum"),
            maxLines = 1,
        )
    }
}

@Composable
private fun DurationTiles(
    durations: List<Int>,
    available: List<Int>,
    selected: Int,
    priceFor: (Int) -> String?,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
    colors: VaiinillaColors,
    modifier: Modifier = Modifier,
) {
    // Tres o cuatro caben a lo ancho, como el dibujo; con más se desliza para que no se aprieten.
    if (durations.size <= 4) {
        Row(
            modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            durations.forEach { minutes ->
                DurationTile(
                    minutes = minutes,
                    price = priceFor(minutes),
                    selected = minutes == selected,
                    enabled = enabled && minutes in available,
                    onClick = { onSelect(minutes) },
                    colors = colors,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    } else {
        LazyRow(
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(durations, key = { it }) { minutes ->
                DurationTile(
                    minutes = minutes,
                    price = priceFor(minutes),
                    selected = minutes == selected,
                    enabled = enabled && minutes in available,
                    onClick = { onSelect(minutes) },
                    colors = colors,
                    modifier = Modifier.width(104.dp),
                )
            }
        }
    }
}

@Composable
private fun DurationTile(
    minutes: Int,
    price: String?,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    colors: VaiinillaColors,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberVaiinillaHaptics()
    val still = reducedMotion()
    val background by animateColorAsState(
        if (selected) colors.ink else colors.paper2,
        if (still) snap() else spring(stiffness = Spring.StiffnessMediumLow),
        label = "duration-background",
    )
    val content by animateColorAsState(
        if (selected) colors.paper else colors.ink,
        if (still) snap() else spring(stiffness = Spring.StiffnessMediumLow),
        label = "duration-content",
    )
    Column(
        modifier =
            modifier
                .height(58.dp)
                .graphicsLayer { alpha = if (enabled || selected) 1f else 0.35f }
                .physicalPress(enabled = enabled && !selected, scale = PhysicalPressScale.Small) {
                    haptics.selection()
                    onClick()
                }.clip(RoundedCornerShape(16.dp))
                .background(background)
                .semantics {
                    role = Role.RadioButton
                    this.selected = selected
                },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(durationLabel(minutes), color = content, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1)
        price?.let {
            Text(
                moneyLabel(it),
                color = content.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun PayMethodOption(
    method: ReservationPaymentMethod,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    colors: VaiinillaColors,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberVaiinillaHaptics()
    val still = reducedMotion()
    val shape = RoundedCornerShape(18.dp)
    val border by animateColorAsState(
        if (selected) colors.accent else Color.Transparent,
        if (still) snap() else spring(stiffness = Spring.StiffnessMediumLow),
        label = "method-border",
    )
    val ring by animateDpAsState(
        if (selected) 7.dp else 2.dp,
        if (still) snap() else spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "method-ring",
    )
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .physicalPress(enabled = enabled, scale = PhysicalPressScale.Small) {
                    if (!selected) haptics.selection()
                    onClick()
                }.clip(shape)
                .background(if (selected) colors.accent.copy(alpha = 0.08f) else colors.paper2)
                .border(1.5.dp, border, shape)
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .semantics {
                    role = Role.RadioButton
                    this.selected = selected
                },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(method.label, color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                when (method) {
                    ReservationPaymentMethod.CARD -> "Pago seguro con tu tarjeta"
                    ReservationPaymentMethod.BALANCE -> "Se paga al instante con tu cartera"
                    ReservationPaymentMethod.CASH -> "Paga en caja antes de que venza el apartado"
                },
                color = colors.muted,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Box(
            modifier =
                Modifier
                    .size(22.dp)
                    .border(ring, if (selected) colors.accent else colors.muted.copy(alpha = 0.35f), CircleShape),
        )
    }
}
@Composable
private fun MyReservationCard(
    reservation: Reservation,
    zone: ZoneId,
    onPay: () -> Unit,
    onCancel: () -> Unit,
    colors: VaiinillaColors,
) {
    val started = !reservation.start.isAfter(Instant.now())
    val pending = reservation.state == ReservationState.PENDING_PAYMENT
    val tone =
        when (reservation.state) {
            ReservationState.PENDING_PAYMENT -> colors.yolk
            ReservationState.CONFLICT -> colors.coral
            else -> colors.accent
        }
    val toneInk =
        when {
            !colors.isDark -> colors.ink
            reservation.state == ReservationState.CONFLICT -> coralInk(colors)
            else -> tone
        }
    val startAt = reservation.start.atZone(zone)
    val cancellable = pending || (reservation.state == ReservationState.CONFIRMED && !started)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .arrive(0, key = reservation.id)
                .clip(RoundedCornerShape(22.dp))
                .background(colors.paper2)
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(tone.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${startAt.format(WEEKDAY_SHORT).replace(".", "").uppercase(MX)}\n${startAt.dayOfMonth}",
                    color = toneInk,
                    fontSize = 12.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${reservation.courtName ?: Venue.courtTitle} · ${startAt.format(HOUR)}–${reservation.end.atZone(zone).format(HOUR)}",
                    color = colors.ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (pending) "Paga para confirmar tu horario" else startAt.format(DAY_LONG),
                    color = colors.muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            StatusTag(
                label = if (pending) "Por pagar" else reservation.state.label,
                background = tone.copy(alpha = 0.14f),
                content = toneInk,
            )
        }
        if (pending || cancellable) {
            Row(
                modifier = Modifier.padding(start = 56.dp, top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (pending) SmallAction("Pagar", filled = true, onClick = onPay, colors = colors)
                if (cancellable) SmallAction("Cancelar", filled = false, onClick = onCancel, colors = colors)
            }
        }
    }
}

@Composable
private fun SmallAction(
    label: String,
    filled: Boolean,
    onClick: () -> Unit,
    colors: VaiinillaColors,
) {
    Text(
        label,
        color = if (filled) colors.accentInk else colors.ink,
        fontSize = 13.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier =
            Modifier
                .physicalPress(onClick = onClick)
                .clip(RoundedCornerShape(50))
                .background(if (filled) colors.accent else Color.Transparent)
                .border(1.dp, if (filled) Color.Transparent else colors.line, RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun Chip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    colors: VaiinillaColors,
) {
    val container by animateColorAsState(
        when {
            selected -> colors.ink
            enabled -> colors.paper2
            else -> colors.paper
        },
        SPRING_COLOR,
        label = "chip-bg",
    )
    val ink by animateColorAsState(
        when {
            selected -> colors.onInk
            enabled -> colors.ink
            else -> colors.muted.copy(alpha = 0.55f)
        },
        SPRING_COLOR,
        label = "chip-ink",
    )
    Text(
        label,
        color = ink,
        fontSize = 14.sp,
        fontWeight = FontWeight.ExtraBold,
        textDecoration = if (enabled || selected) null else TextDecoration.LineThrough,
        modifier =
            Modifier
                .physicalPress(enabled = enabled, onClick = onClick)
                .clip(RoundedCornerShape(50))
                .background(container)
                .border(1.dp, if (enabled || selected) Color.Transparent else colors.line, RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

private fun durationLabel(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        rest == 0 -> "$hours h"
        hours == 0 -> "$rest min"
        else -> "$hours h $rest"
    }
}

private fun dayLabel(
    day: CourtDay,
    zone: ZoneId,
    start: Instant,
): String {
    val date = start.atZone(zone).toLocalDate()
    val today = LocalDate.parse(day.today)
    return when (date) {
        today -> "Hoy"
        today.plusDays(1) -> "Mañana"
        else -> date.format(DAY_LONG)
    }
}

private fun clock(ms: Long): String {
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}

/**
 * Rentar una pista desde su tarjeta sin salir de donde está el cliente (marco RP1): hora, tiempo y total en una hoja;
 * al apartar, la misma hoja pasa al pago. Si el pago sale o se suelta el apartado, se cierra sola.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CourtRentSheet(
    state: ReservationsUiState,
    onDismiss: () -> Unit,
    onPickNow: () -> Unit,
    onSelectStart: (Instant) -> Unit,
    onSelectDuration: (Int) -> Unit,
    onSelectPlayers: (Int) -> Unit,
    onReserve: () -> Unit,
    onPay: (ReservationPaymentMethod) -> Unit,
    onChangeDuration: (Int) -> Unit,
    onChangePlayers: (Int) -> Unit,
    onOtherDays: () -> Unit,
    cardAvailable: Boolean,
    balanceAvailable: Boolean,
) {
    val colors = LocalVaiinillaColors.current
    val day = state.day
    val zone = remember(day?.timeZone) { BusinessTime.zoneOr(day?.timeZone) }
    val pending = state.pending
    var held by remember { mutableStateOf(false) }
    LaunchedEffect(pending == null) {
        if (pending != null) {
            held = true
        } else if (held) {
            held = false
            // Con error el apartado se perdió al cambiarlo: la hoja sigue abierta para elegir otra vez.
            if (state.error == null) onDismiss()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.paper,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = { ClientSheetHandle() },
    ) {
        if (pending != null) {
            ReservationPaymentContent(
                reservation = pending,
                zone = zone,
                working = state.working,
                onPay = onPay,
                colors = colors,
                error = state.error,
                // Sin cuenta no hay saldo ni tarjeta: el backend los rechaza para invitados.
                cardAvailable = cardAvailable && !state.guestMode,
                balanceAvailable = balanceAvailable && !state.guestMode,
                day = day,
                startsNow = state.pendingStartsNow,
                onChangeDuration = onChangeDuration,
                onChangePlayers = onChangePlayers,
            )
        } else {
            val court = state.selectedCourt
            if (day == null || court == null || !court.rentable) {
                Text(
                    when {
                        day == null && state.loading -> "Cargando…"
                        day == null -> state.error ?: "Este lugar todavía no renta ${Venue.courts}."
                        else -> "Esta ${Venue.court} no se renta desde la app. Pregunta en caja."
                    },
                    color = colors.muted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 40.dp),
                )
            } else {
                CourtRentChoice(
                    state = state,
                    day = day,
                    court = court,
                    zone = zone,
                    colors = colors,
                    onDismiss = onDismiss,
                    onPickNow = onPickNow,
                    onSelectStart = onSelectStart,
                    onSelectDuration = onSelectDuration,
                    onSelectPlayers = onSelectPlayers,
                    onReserve = onReserve,
                    onOtherDays = onOtherDays,
                )
            }
        }
    }
}

@Composable
private fun CourtRentChoice(
    state: ReservationsUiState,
    day: CourtDay,
    court: CourtSchedule,
    zone: ZoneId,
    colors: VaiinillaColors,
    onDismiss: () -> Unit,
    onPickNow: () -> Unit,
    onSelectStart: (Instant) -> Unit,
    onSelectDuration: (Int) -> Unit,
    onSelectPlayers: (Int) -> Unit,
    onReserve: () -> Unit,
    onOtherDays: () -> Unit,
) {
    val canNow = day.date == day.today && ReservationSlots.canRentNow(day, court)
    // Libre ahora: «Ahora» ya viene elegido; apartar sigue siendo decisión del botón.
    LaunchedEffect(court.id, canNow) {
        if (canNow && state.selectedStart == null && !state.rentNow) onPickNow()
    }
    val start = if (state.rentNow) day.now else state.selectedStart
    val minutes = state.selectedDuration
    val end = if (start != null && minutes != null) start.plusSeconds(minutes * 60L) else null
    val startLabel = if (state.rentNow) "Ahora" else start?.atZone(zone)?.format(HOUR)
    val endLabel = end?.atZone(zone)?.format(HOUR)
    val price =
        minutes?.let { m ->
            court.customerPricePerHour?.let { ReservationSlots.amountFor(it.cashOrBalance, m) }
        }
    val busyUntil = if (day.date == day.today) ReservationSlots.busyUntil(court, day.now) else null
    val status =
        listOfNotNull(
            when {
                busyUntil != null -> "Se libera a las ${busyUntil.atZone(zone).format(HOUR)}"
                canNow -> "Libre ahora"
                else -> null
            },
            court.customerPricePerHour?.let { "${moneyLabel(it.cashOrBalance)} la hora" },
        ).joinToString(" · ")
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    court.name,
                    color = colors.ink,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.6).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (status.isNotEmpty()) {
                    Text(
                        status,
                        color = if (canNow && colors.isDark) colors.accent else colors.ink2,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Outlined.Close, contentDescription = "Cerrar", tint = colors.ink)
            }
        }
        Row(
            Modifier
                .padding(start = 16.dp, end = 16.dp, top = 14.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.paper2)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    startLabel ?: "–",
                    color = colors.ink,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.8).sp,
                )
                Text("Empieza", color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            minutes?.let {
                Text(
                    durationLabel(it),
                    color = colors.ink,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(colors.line)
                            .padding(horizontal = 11.dp, vertical = 6.dp),
                )
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(
                    endLabel ?: "–",
                    color = colors.ink,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.8).sp,
                )
                Text("Termina", color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        val starts = remember(day, court.id) { ReservationSlots.startTimes(day) }
        ClientCap("Hora", modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp))
        Row(Modifier.padding(top = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            if (canNow) {
                Box(Modifier.padding(start = 16.dp)) {
                    HourChip(
                        label = "Ahora",
                        selected = state.rentNow,
                        enabled = !state.working,
                        onClick = onPickNow,
                        colors = colors,
                    )
                }
            }
            HourRow(
                starts = starts,
                day = day,
                court = court,
                zone = zone,
                selected = state.selectedStart.takeIf { !state.rentNow },
                onSelect = onSelectStart,
                colors = colors,
                modifier = Modifier.weight(1f),
            )
        }
        if (start != null) {
            DurationRow(
                available = ReservationSlots.availableDurations(day, court, start),
                all = ReservationSlots.durationsFor(day, court),
                selected = minutes,
                onSelect = onSelectDuration,
                colors = colors,
            )
        }
        Column(Modifier.padding(horizontal = 16.dp)) {
            if (Venue.isBowling) {
                PlayersRow(
                    players = state.players,
                    enabled = !state.working,
                    onChange = onSelectPlayers,
                    colors = colors,
                    max = court.maxPlayers,
                )
            }
            Text(
                "Se aparta ${if (day.holdMinutes == 1) "1 minuto" else "${day.holdMinutes} minutos"} mientras pagas. " +
                    "Si cancelas, lo pagado no se devuelve.",
                color = colors.muted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 14.dp),
            )
            state.error?.let {
                Text(
                    it,
                    color = colors.coralText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            EditorialAccentButton(
                text =
                    when {
                        state.working -> "Apartando…"
                        start == null -> "Elige la hora"
                        minutes == null -> "Elige cuánto tiempo"
                        Venue.isBowling && state.players == 0 -> "Anota cuántos juegan"
                        price == null -> "Apartar ${Venue.court}"
                        state.rentNow -> "Apartar desde ahora · ${moneyLabel(price)}"
                        else -> "Apartar $startLabel a $endLabel · ${moneyLabel(price)}"
                    },
                onClick = onReserve,
                enabled = state.canReserve,
            )
            Box(
                Modifier
                    .padding(top = 4.dp)
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(role = Role.Button, onClick = onOtherDays),
                contentAlignment = Alignment.Center,
            ) {
                Text("Ver otros días", color = colors.ink2, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
