package com.vaiinilla.app.ui.reservations

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
import com.vaiinilla.app.ui.components.EditorialAccentButton
import com.vaiinilla.app.ui.components.EditorialConfirmSheet
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.RollingText
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaColors
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MX = Locale.forLanguageTag("es-MX")
private val HOUR = DateTimeFormatter.ofPattern("HH:mm", MX)
private val DAY_SHORT = DateTimeFormatter.ofPattern("EEE d", MX)
private val DAY_LONG = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", MX)
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
    onDismissPayment: () -> Unit,
    onResumePayment: (Reservation) -> Unit,
    onCancel: (Reservation) -> Unit,
    onDismissMessage: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val day = state.day
    val zone = remember(day?.timeZone) { runCatching { ZoneId.of(day?.timeZone) }.getOrDefault(ZoneId.systemDefault()) }
    var confirmCancel by remember { mutableStateOf<Reservation?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(colors.paper)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item(key = "head") { Header(venueName = venueName, onBack = onBack, colors = colors) }

            state.error?.let { message ->
                item(key = "error") { Banner(message, colors.coral, colors, onDismissMessage) }
            }
            state.notice?.let { message ->
                item(key = "notice") { Banner(message, colors.accent, colors, onDismissMessage) }
            }

            if (day == null) {
                item(key = "loading") {
                    Text(
                        if (state.loading) "Cargando canchas…" else "Este lugar todavía no renta canchas.",
                        color = colors.muted,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(top = 24.dp).arrive(1),
                    )
                }
            } else if (day.courts.none { it.rentable }) {
                item(key = "none") {
                    Text(
                        "Por ahora las canchas de este lugar no se rentan desde la app. Pregunta en caja.",
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
                item(key = "courts") {
                    CourtStrip(
                        day = day,
                        zone = zone,
                        selectedId = state.selectedCourtId,
                        onSelect = onSelectCourt,
                        colors = colors,
                    )
                }
                val court = state.selectedCourt
                if (court != null && !court.profile.isEmpty) {
                    item(key = "profile-${court.id}") { CourtProfileCard(court = court, colors = colors) }
                }
                if (court != null && court.rentable) {
                    if (ReservationSlots.canRentNow(day, court)) {
                        item(key = "now") {
                            RentNowCard(selected = state.rentNow, onClick = onRentNow, colors = colors)
                        }
                    }
                    item(key = "hours") {
                        HourGrid(
                            day = day,
                            court = court,
                            zone = zone,
                            selected = state.selectedStart,
                            onSelect = onSelectStart,
                            colors = colors,
                        )
                    }
                    val start = if (state.rentNow) day.now else state.selectedStart
                    if (start != null) {
                        item(key = "durations") {
                            DurationRow(
                                available = ReservationSlots.availableDurations(day, court, start),
                                all = day.durations,
                                selected = state.selectedDuration,
                                onSelect = onSelectDuration,
                                colors = colors,
                            )
                        }
                        item(key = "summary") {
                            Summary(
                                state = state,
                                day = day,
                                court = court,
                                start = start,
                                zone = zone,
                                onReserve = onReserve,
                                colors = colors,
                            )
                        }
                    }
                }
            }

            val mine = state.mine.filter { it.state.isLive || it.state == ReservationState.CONFLICT }
            if (mine.isNotEmpty()) {
                item(key = "mine-head") {
                    Text(
                        "MIS RESERVAS",
                        color = colors.muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.4.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(mine, key = { "mine-${it.id}" }) { reservation ->
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

        confirmCancel?.let { reservation ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EditorialConfirmSheet(
                    title = "¿Cancelar la reserva?",
                    message =
                        if (reservation.state == ReservationState.PENDING_PAYMENT) {
                            "Se libera el horario y no se te cobra nada."
                        } else {
                            "Se libera la cancha, pero lo que pagaste no se devuelve."
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
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismissPayment,
            sheetState = sheetState,
            containerColor = colors.paper,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        ) {
            ReservationPaymentContent(
                reservation = reservation,
                zone = zone,
                working = state.working,
                onPay = onPay,
                colors = colors,
            )
        }
    }
}

@Composable
private fun Header(
    venueName: String?,
    onBack: () -> Unit,
    colors: VaiinillaColors,
) {
    Column(modifier = Modifier.arrive(0)) {
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 0.dp)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver", tint = colors.ink)
        }
        Text(
            "CANCHAS",
            color = colors.muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.4.sp,
        )
        Text(
            "Renta tu cancha",
            color = colors.ink,
            fontSize = 34.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1.4).sp,
        )
        Text(
            listOfNotNull(venueName, "se paga al apartar").joinToString(" · "),
            color = colors.muted,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
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

@Composable
private fun CourtStrip(
    day: CourtDay,
    zone: ZoneId,
    selectedId: Int?,
    onSelect: (Int) -> Unit,
    colors: VaiinillaColors,
) {
    LazyRow(modifier = Modifier.arrive(2), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(day.courts.filter { it.rentable }, key = { it.id }) { court ->
            val selected = court.id == selectedId
            val container by animateColorAsState(
                if (selected) colors.ink else colors.paper2,
                SPRING_COLOR,
                label = "court-bg",
            )
            val ink by animateColorAsState(
                if (selected) colors.paper else colors.ink,
                SPRING_COLOR,
                label = "court-ink",
            )
            val isToday = day.date == day.today
            val busyUntil = if (isToday) ReservationSlots.busyUntil(court, day.now) else null
            val looksFree = if (isToday) busyUntil == null else court.busy.isEmpty()
            Column(
                modifier =
                    Modifier
                        .width(156.dp)
                        .physicalPress { onSelect(court.id) }
                        .clip(RoundedCornerShape(22.dp))
                        .background(container)
                        .padding(16.dp),
            ) {
                Text(court.name, color = ink, fontSize = 17.sp, fontWeight = FontWeight.Black)
                court.customerPricePerHour?.let {
                    Text(
                        "$${it.cashOrBalance} / hora",
                        color = ink.copy(alpha = 0.72f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (looksFree) colors.accent else colors.coral),
                    )
                    Text(
                        when {
                            busyUntil != null -> "Se libera ${busyUntil.atZone(zone).format(HOUR)}"
                            isToday -> "Libre ahora"
                            court.busy.isEmpty() -> "Libre todo el día"
                            else -> "Con horarios ocupados"
                        },
                        color = ink.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
    }
}

/** Ficha que escribió el dueño: foto (se abre en grande), descripción y características. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CourtProfileCard(
    court: CourtSchedule,
    colors: VaiinillaColors,
) {
    val profile = court.profile
    var zoomed by remember(court.id) { mutableStateOf(false) }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .arrive(3, key = court.id)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.paper2),
    ) {
        profile.imageUrl?.let { url ->
            ProductImage(
                imageUrl = url,
                contentDescription = "Foto de ${court.name}",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                        .physicalPress { zoomed = true },
                contentScale = ContentScale.Crop,
            )
        }
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(court.name, color = colors.ink, fontSize = 20.sp, fontWeight = FontWeight.Black)
            profile.description?.let {
                Text(it, color = colors.ink.copy(alpha = 0.8f), fontSize = 14.sp, lineHeight = 20.sp)
            }
            if (profile.features.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    profile.features.forEach { feature ->
                        Text(
                            feature,
                            color = colors.ink,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(colors.paper)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
    val zoomUrl = profile.imageUrl
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

@Composable
private fun RentNowCard(
    selected: Boolean,
    onClick: () -> Unit,
    colors: VaiinillaColors,
) {
    val container by animateColorAsState(
        if (selected) colors.accent else colors.accent.copy(alpha = 0.22f),
        SPRING_COLOR,
        label = "now-bg",
    )
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .arrive(3)
                .physicalPress(onClick = onClick)
                .clip(RoundedCornerShape(22.dp))
                .background(container)
                .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Rentar ahora", color = colors.accentInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(
                "Empieza en cuanto pagues.",
                color = colors.accentInk.copy(alpha = 0.78f),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            if (selected) "Elegido" else "Elegir",
            color = colors.accentInk,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HourGrid(
    day: CourtDay,
    court: CourtSchedule,
    zone: ZoneId,
    selected: Instant?,
    onSelect: (Instant) -> Unit,
    colors: VaiinillaColors,
) {
    val starts = remember(day, court.id) { ReservationSlots.startTimes(day) }
    Column(modifier = Modifier.arrive(4), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            if (day.date == day.today) "O aparta una hora de hoy" else "Elige la hora",
            color = colors.ink,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
        )
        if (starts.isEmpty()) {
            Text("Ya no quedan horarios este día.", color = colors.muted, fontSize = 14.sp)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            starts.forEach { start ->
                val free = ReservationSlots.isStartAvailable(day, court, start)
                Chip(
                    label = start.atZone(zone).format(HOUR),
                    selected = start == selected,
                    enabled = free,
                    onClick = { onSelect(start) },
                    colors = colors,
                )
            }
        }
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
    Column(modifier = Modifier.arrive(5), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("¿Cuánto tiempo?", color = colors.ink, fontSize = 17.sp, fontWeight = FontWeight.Black)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(all, key = { it }) { minutes ->
                Chip(
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
private fun Summary(
    state: ReservationsUiState,
    day: CourtDay,
    court: CourtSchedule,
    start: Instant,
    zone: ZoneId,
    onReserve: () -> Unit,
    colors: VaiinillaColors,
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
                .arrive(6)
                .clip(RoundedCornerShape(26.dp))
                .border(1.dp, colors.line, RoundedCornerShape(26.dp))
                .padding(20.dp),
    ) {
        Text(
            "${court.name} · ${dayLabel(day, zone, start)}",
            color = colors.muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            if (end != null) {
                "${if (state.rentNow) "Ahora" else start.atZone(zone).format(HOUR)} – ${end.atZone(zone).format(HOUR)}"
            } else {
                "Elige cuánto tiempo"
            },
            color = colors.ink,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.8).sp,
            modifier = Modifier.padding(top = 4.dp),
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
                    "  con efectivo o saldo",
                    color = colors.muted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
        Text(
            "Se aparta ${day.holdMinutes} minutos mientras pagas. Si cancelas, lo pagado no se devuelve.",
            color = colors.muted,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier.padding(top = 10.dp),
        )
        Spacer(Modifier.height(16.dp))
        EditorialAccentButton(
            text = if (state.working) "Apartando…" else "Apartar cancha",
            onClick = onReserve,
            enabled = state.canReserve,
        )
    }
}

@Composable
internal fun ReservationPaymentContent(
    reservation: Reservation,
    zone: ZoneId,
    working: Boolean,
    onPay: (ReservationPaymentMethod) -> Unit,
    colors: VaiinillaColors,
) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(reservation.id) {
        while (true) {
            delay(1_000)
            nowMs = System.currentTimeMillis()
        }
    }
    val remaining = reservation.holdExpiresAt?.let { (it.toEpochMilli() - nowMs).coerceAtLeast(0) }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.arrive(0)) {
            Text(
                "Paga tu cancha",
                color = colors.ink,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.8).sp,
            )
            Text(
                "${reservation.courtName ?: "Cancha"} · ${reservation.start.atZone(zone).format(HOUR)} – " +
                    reservation.end.atZone(zone).format(HOUR),
                color = colors.muted,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (remaining != null) {
                Text(
                    if (remaining >
                        0
                    ) {
                        "Apartada por ${clock(remaining)}"
                    } else {
                        "Venció el apartado: vuelve a elegir el horario."
                    },
                    color = if (remaining > 60_000) colors.ink else colors.coral,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        val prices = reservation.customerPrice
        PayOption(
            title = "Saldo",
            detail = "Se paga al instante con tu cartera.",
            amount = prices?.cashOrBalance ?: reservation.amount,
            index = 1,
            enabled = !working && remaining != 0L,
            onClick = { onPay(ReservationPaymentMethod.BALANCE) },
            colors = colors,
        )
        PayOption(
            title = "Tarjeta",
            detail = "Pago seguro con tu tarjeta.",
            amount = prices?.card ?: reservation.amount,
            index = 2,
            enabled = !working && remaining != 0L,
            onClick = { onPay(ReservationPaymentMethod.CARD) },
            colors = colors,
        )
        PayOption(
            title = "Efectivo en caja",
            detail = "Paga en caja antes de que venza el apartado.",
            amount = prices?.cashOrBalance ?: reservation.amount,
            index = 3,
            enabled = !working && remaining != 0L,
            onClick = { onPay(ReservationPaymentMethod.CASH) },
            colors = colors,
        )
    }
}

@Composable
private fun PayOption(
    title: String,
    detail: String,
    amount: String,
    index: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    colors: VaiinillaColors,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .arrive(index)
                .physicalPress(enabled = enabled, onClick = onClick)
                .clip(RoundedCornerShape(22.dp))
                .background(colors.paper2)
                .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 14.dp)) {
            Text(title, color = colors.ink, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Text(
                detail,
                color = colors.muted,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text("$$amount", color = colors.ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
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
    val tone =
        when (reservation.state) {
            ReservationState.PENDING_PAYMENT -> colors.yolk
            ReservationState.CONFLICT -> colors.coral
            else -> colors.accent
        }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .arrive(0, key = reservation.id)
                .clip(RoundedCornerShape(22.dp))
                .background(colors.paper2)
                .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                reservation.courtName ?: "Cancha",
                color = colors.ink,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f),
            )
            Text(
                reservation.state.label,
                color = colors.ink,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(tone.copy(alpha = 0.35f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Text(
            "${reservation.start.atZone(zone).format(DAY_LONG)} · ${reservation.start.atZone(zone).format(HOUR)} – " +
                reservation.end.atZone(zone).format(HOUR),
            color = colors.muted,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (reservation.state == ReservationState.PENDING_PAYMENT) {
                SmallAction("Pagar", filled = true, onClick = onPay, colors = colors)
            }
            val cancellable =
                reservation.state == ReservationState.PENDING_PAYMENT ||
                    (reservation.state == ReservationState.CONFIRMED && !started)
            if (cancellable) {
                SmallAction("Cancelar", filled = false, onClick = onCancel, colors = colors)
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
            selected -> colors.paper
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
