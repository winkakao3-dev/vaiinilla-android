package com.vaiinilla.app.ui.hotel.staff

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vaiinilla.app.domain.hotel.Stay
import com.vaiinilla.app.domain.hotel.StayStatus
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import java.time.LocalDate

private const val REFRESH_MS = 20_000L

/**
 * Recepción: llegadas y salidas. Desde aquí se abren el check-in, el cargo manual y el cierre de la cuenta (S04),
 * que no tienen ruta propia: viven dentro de esta pantalla.
 */
@Composable
fun HotelReceptionScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HotelReceptionViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    HotelPolling(REFRESH_MS, viewModel::refresh)
    // Atrás sale de la pantalla abierta (check-in, cuenta, cargo) a la lista; y al cerrar Recepción no queda a medias.
    BackHandler(enabled = state.pane != ReceptionPane.Overview) { viewModel.close() }
    DisposableEffect(viewModel) { onDispose { viewModel.close() } }
    val today = LocalDate.now()
    when (val pane = state.pane) {
        ReceptionPane.Overview ->
            HotelReceptionContent(
                state = state,
                today = today,
                onCheckIn = { viewModel.open(ReceptionPane.CheckIn) },
                onCheckout = { viewModel.open(ReceptionPane.Checkout(it.id)) },
                onCharge = { viewModel.open(ReceptionPane.Charge(it.id)) },
                onBack = onBack,
                modifier = modifier,
                onArrive = viewModel::confirmArrival,
                onCancel = viewModel::cancelReservation,
            )
        ReceptionPane.CheckIn ->
            HotelCheckInScreen(
                state = state,
                today = today,
                onSubmit = viewModel::checkIn,
                onBack = viewModel::close,
                modifier = modifier,
            )
        is ReceptionPane.Checkout ->
            HotelCheckoutScreen(stayId = pane.stayId, onDone = viewModel::close, modifier = modifier, viewModel = viewModel)
        is ReceptionPane.Charge -> {
            HotelReceptionContent(
                state = state,
                today = today,
                onCheckIn = {},
                onCheckout = {},
                onCharge = {},
                onBack = onBack,
                modifier = modifier,
            )
            state.stayFor(pane.stayId)?.let { stay ->
                HotelManualChargeSheet(
                    stay = stay,
                    busy = state.busy,
                    error = state.error,
                    onSave = { concept, amount, category -> viewModel.addCharge(stay.id, concept, amount, category) },
                    onDismiss = viewModel::close,
                )
            }
        }
    }
}

@Composable
internal fun HotelReceptionContent(
    state: HotelReceptionUiState,
    today: LocalDate,
    onCheckIn: () -> Unit,
    onCheckout: (Stay) -> Unit,
    onCharge: (Stay) -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onArrive: (Stay) -> Unit = {},
    onCancel: (Stay) -> Unit = {},
) {
    val colors = LocalVaiinillaColors.current
    val leavingToday = state.inHouse.filter { (hotelDate(it.checkOut) ?: today) <= today }
    val staying = state.inHouse - leavingToday.toSet()
    HotelStaffPage(
        kicker = "Recepción",
        title = "Llegadas y salidas",
        modifier = modifier,
        onBack = onBack,
        bottomBar = {
            HotelButton(
                label = "Nuevo check-in",
                onClick = onCheckIn,
                kind = HotelButtonKind.Lime,
                height = 58.dp,
                fontSize = 16,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        state.error?.let { HotelNotice(it) }
        StaySection("SALEN HOY", leavingToday, onCheckout, onCharge, onArrive, onCancel, state.busy)
        StaySection("EN CASA", staying, onCheckout, onCharge, onArrive, onCancel, state.busy)
        StaySection("POR LLEGAR", state.arriving, onCheckout, onCharge, onArrive, onCancel, state.busy)
        if (state.loaded && state.stays.none { it.status == StayStatus.IN_HOUSE || it.status == StayStatus.RESERVED }) {
            Text(
                "No hay huéspedes por ahora.",
                color = colors.muted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
    }
}

@Composable
private fun StaySection(
    label: String,
    stays: List<Stay>,
    onCheckout: (Stay) -> Unit,
    onCharge: (Stay) -> Unit,
    onArrive: (Stay) -> Unit,
    onCancel: (Stay) -> Unit,
    busy: Boolean,
) {
    if (stays.isEmpty()) return
    HotelCaps("$label · ${stays.size}", modifier = Modifier.padding(start = 6.dp, top = 4.dp))
    stays.forEach { stay -> StayRow(stay, busy, onCheckout, onCharge, onArrive, onCancel) }
}

@Composable
private fun StayRow(
    stay: Stay,
    busy: Boolean,
    onCheckout: (Stay) -> Unit,
    onCharge: (Stay) -> Unit,
    onArrive: (Stay) -> Unit,
    onCancel: (Stay) -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val inHouse = stay.status == StayStatus.IN_HOUSE
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(colors.paper2).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column {
            Text(stay.guestName, color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Black)
            Text(
                listOfNotNull(stay.roomName, hotelDateRange(stay.checkIn, stay.checkOut).ifBlank { null }).joinToString(" · "),
                color = colors.muted,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (inHouse) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                HotelButton("Cargo", onClick = { onCharge(stay) }, kind = HotelButtonKind.Outline, height = 38.dp, fontSize = 13)
                HotelButton("Check-out", onClick = { onCheckout(stay) }, height = 38.dp, fontSize = 13)
            }
        } else if (stay.status == StayStatus.RESERVED) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                HotelButton("Cancelar", onClick = { onCancel(stay) }, kind = HotelButtonKind.Outline, enabled = !busy, height = 38.dp, fontSize = 13)
                HotelButton("Llegó", onClick = { onArrive(stay) }, enabled = !busy, height = 38.dp, fontSize = 13)
            }
        }
    }
}
