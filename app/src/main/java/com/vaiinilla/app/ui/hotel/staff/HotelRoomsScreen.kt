package com.vaiinilla.app.ui.hotel.staff

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vaiinilla.app.domain.hotel.HotelRequest
import com.vaiinilla.app.domain.hotel.HotelRoom
import com.vaiinilla.app.domain.hotel.RequestStatus
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import kotlinx.coroutines.delay
import java.time.Instant

private const val REFRESH_MS = 15_000L

/** S03: Estado de habitaciones y solicitudes. La abre el ama de llaves desde la vista de mesero. */
@Composable
fun HotelRoomsScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HotelRoomsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.refresh()
            delay(REFRESH_MS)
        }
    }
    HotelRoomsContent(
        state = state,
        now = Instant.now(),
        onFilter = viewModel::setFilter,
        onFloor = viewModel::nextFloor,
        onRoom = viewModel::toggleCleaning,
        onTake = viewModel::take,
        onResolve = viewModel::resolve,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
internal fun HotelRoomsContent(
    state: HotelRoomsUiState,
    now: Instant,
    onFilter: (RoomsFilter) -> Unit,
    onFloor: () -> Unit,
    onRoom: (HotelRoom) -> Unit,
    onTake: (HotelRequest) -> Unit,
    onResolve: (HotelRequest) -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val floor = state.floorShown
    HotelStaffPage(
        kicker = if (floor != null) "Ama de llaves · Piso $floor" else "Ama de llaves",
        title = "Habitaciones",
        modifier = modifier,
        onBack = onBack,
        onKicker = if (state.floors.size > 1) onFloor else null,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RoomsFilter.entries.forEach { filter ->
                HotelChip(
                    label = "${filter.label} ${state.count(filter)}",
                    selected = state.filter == filter,
                    onClick = { onFilter(filter) },
                )
            }
        }
        val rooms = state.visibleRooms
        if (rooms.isEmpty()) {
            Text(
                if (state.loaded) "No hay habitaciones con ese filtro." else "Buscando habitaciones…",
                color = colors.muted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 2.dp)) {
                rooms.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { room ->
                            RoomTile(room, enabled = state.busyId == null, onClick = { onRoom(room) }, modifier = Modifier.weight(1f))
                        }
                        repeat(4 - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        }
        state.error?.let { HotelNotice(it) }
        HotelCaps("SOLICITUDES DE HUÉSPEDES", modifier = Modifier.padding(start = 6.dp, top = 6.dp))
        if (state.requests.isEmpty()) {
            Text(
                "Nadie ha pedido nada por ahora.",
                color = colors.muted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
        state.requests.forEach { request ->
            RequestRow(
                request = request,
                now = now,
                busy = state.busyId == request.id,
                onTake = { onTake(request) },
                onResolve = { onResolve(request) },
            )
        }
    }
}

private fun toneColor(tone: RoomTone): Color =
    when (tone) {
        RoomTone.OCCUPIED -> HotelBlue
        RoomTone.DIRTY -> HotelAmber
        RoomTone.READY -> Color(0xFFB9D86D)
        RoomTone.DO_NOT_DISTURB -> Color(0xFFF15B55)
    }

/** El cuadrito de Doop: todo el recuadro con un tinte leve del estado y la etiqueta en el color pleno, sin franjas. */
@Composable
private fun RoomTile(
    room: HotelRoom,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val tone = roomTone(room)
    val color = toneColor(tone)
    Column(
        modifier =
            modifier
                .height(74.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(color.copy(alpha = 0.16f))
                .physicalPress(enabled = enabled && !room.occupied, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(hotelRoomNumber(room.name), color = colors.ink, fontSize = 19.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Text(
            tone.label,
            color = color,
            fontSize = 10.5.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 2,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun RequestRow(
    request: HotelRequest,
    now: Instant,
    busy: Boolean,
    onTake: () -> Unit,
    onResolve: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val taken = request.status == RequestStatus.TAKEN
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(colors.paper2)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(requestTitle(request), color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Black)
            Text(
                requestSubtitle(request, now),
                color = colors.muted,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        HotelButton(
            label = if (taken) "Listo" else "Tomar",
            onClick = if (taken) onResolve else onTake,
            enabled = !busy,
            height = 38.dp,
            fontSize = 13,
        )
    }
}

/** «304 · 2 toallas»: el número de la habitación, la cantidad si pasa de una y lo que pidió. */
internal fun requestTitle(request: HotelRequest): String {
    val room = hotelRoomNumber(request.roomName)
    val what = request.type.label.lowercase().replaceFirstChar { it.uppercase() }
    val label = if (request.quantity > 1) "${request.quantity} ${request.type.label.lowercase()}" else what
    return if (room.isBlank()) label else "$room · $label"
}

/** «Pedido hace 6 min · Después de las 11:00», o quién la tomó. */
internal fun requestSubtitle(
    request: HotelRequest,
    now: Instant,
): String {
    val head =
        when {
            request.status == RequestStatus.TAKEN -> "Tomada por ${request.takenBy ?: "el personal"}"
            else -> hotelAge(request.createdAt, now)?.let { "Pedido $it" } ?: "Pedido"
        }
    return request.note?.takeIf { it.isNotBlank() }?.let { "$head · $it" } ?: head
}
