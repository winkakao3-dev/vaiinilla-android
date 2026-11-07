package com.vaiinilla.app.ui.hotel.guest

import com.vaiinilla.app.core.venue.LocalVenueContext
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Bathtub
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.hotel.HotelConfig
import com.vaiinilla.app.domain.hotel.HotelRequest
import com.vaiinilla.app.domain.hotel.RequestType
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.hotel.hotelViewModel
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

/** H05: pedir algo al hotel. Lo elegido se envía junto con «Enviar solicitud · N». */
@Composable
fun HotelRequestsScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HotelRequestsViewModel = hotelViewModel(),
    stayViewModel: HotelStayViewModel = hotelViewModel(),
) {
    val venue = LocalVenueContext.current
    LaunchedEffect(Unit) {
        viewModel.load()
        stayViewModel.load()
    }
    val state = viewModel.state.value
    LaunchedEffect(state.sentCount) {
        if (state.sentCount > 0) {
            viewModel.clearSent()
            onBack()
        }
    }
    HotelRequestsContent(
        roomName =
            stayViewModel.state.value.stay
                ?.roomName ?: venue.vocabulary.roomTitle,
        state = state,
        onTap = viewModel::tap,
        onToggleDoNotDisturb = viewModel::toggleDoNotDisturb,
        onSubmit = viewModel::submit,
        onCancel = viewModel::cancel,
        modifier = modifier,
    )
}

private data class RequestOption(
    val type: RequestType,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
)

private val OPTIONS =
    listOf(
        RequestOption(RequestType.TOWELS, "Toallas", "Baño o alberca", Icons.Outlined.Bathtub),
        RequestOption(RequestType.PILLOWS, "Almohadas", "Suaves o firmes", Icons.Outlined.Bed),
        RequestOption(RequestType.CLEANING, "Limpieza", "Limpiamos tu habitación hoy", Icons.Outlined.CleaningServices),
        RequestOption(RequestType.CHARGER, "Cargador o plancha", "Te lo llevamos", Icons.Outlined.Power),
    )

@Composable
fun HotelRequestsContent(
    roomName: String,
    state: HotelRequestsUiState,
    onTap: (RequestType) -> Unit,
    onToggleDoNotDisturb: () -> Unit,
    onSubmit: () -> Unit,
    onCancel: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Box(modifier = modifier.fillMaxSize().background(colors.paper)) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(
                        rememberScrollState(),
                    ).padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HotelPageTitle(eyebrow = roomName, title = "Pedir al hotel")
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OPTIONS.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEach { option ->
                            HotelRequestTile(
                                title = option.title,
                                subtitle = option.subtitle,
                                icon = option.icon,
                                badge = badgeFor(option.type, state.picked[option.type]),
                                onClick = { onTap(option.type) },
                                modifier = Modifier.weight(1f).aspectRatio(0.85f),
                                testTag = "hotel_request_${option.type.wire}",
                            )
                        }
                    }
                }
                val config = state.config
                val lateCheckout = config.lateCheckoutTime != null
                if (config.doNotDisturbEnabled || lateCheckout) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (config.doNotDisturbEnabled) {
                            HotelRequestTile(
                                title = "Sin molestar",
                                subtitle = "No tocar la puerta",
                                icon = Icons.Outlined.DoNotDisturbOn,
                                badge = if (state.doNotDisturb) "Sí" else null,
                                onClick = onToggleDoNotDisturb,
                                modifier = Modifier.weight(1f).aspectRatio(0.85f),
                                testTag = "hotel_request_dnd",
                            )
                        }
                        if (lateCheckout) {
                            HotelRequestTile(
                                title = "Salida tardía",
                                subtitle = lateCheckoutNote(config),
                                icon = Icons.Outlined.AccessTime,
                                badge = badgeFor(RequestType.LATE_CHECKOUT, state.picked[RequestType.LATE_CHECKOUT]),
                                onClick = { onTap(RequestType.LATE_CHECKOUT) },
                                modifier = Modifier.weight(1f).aspectRatio(0.85f),
                                testTag = "hotel_request_${RequestType.LATE_CHECKOUT.wire}",
                            )
                        }
                        if (config.doNotDisturbEnabled != lateCheckout) Spacer(Modifier.weight(1f))
                    }
                }
            }
            if (state.open.isNotEmpty()) {
                OpenRequests(state.open, onCancel)
            }
            state.error?.let {
                Text(
                    it,
                    color = colors.coral,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp).testTag("hotel_requests_error"),
                )
            }
            Spacer(Modifier.height(96.dp))
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(colors.paper.copy(alpha = 0f), colors.paper)))
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .navigationBarsPadding(),
        ) {
            val count = state.pendingCount
            HotelPrimaryPill(
                label = if (count > 0) "Enviar solicitud · $count" else "Elige lo que necesitas",
                onClick = onSubmit,
                enabled = count > 0,
                loading = state.submitting,
                testTag = "hotel_requests_submit",
            )
        }
    }
}

/** «Hasta las 14:00 · $350»: la hora y el precio que puso el hotel; sin precio configurado no se dice ninguno. */
internal fun lateCheckoutNote(config: HotelConfig): String {
    val until = config.lateCheckoutTime?.let { "Hasta las $it" } ?: "Pide más tiempo"
    val price = config.lateCheckoutPrice?.let(::moneyLabel)
    return listOfNotNull(until, price).joinToString(" · ")
}

/** La cantidad para lo que se pide por cantidad; «✓» para lo que solo se pide. Sin elegir no hay marca. */
private fun badgeFor(
    type: RequestType,
    picked: Int?,
): String? =
    when {
        picked == null || picked == 0 -> null
        type in QUANTITY_REQUESTS -> picked.toString()
        else -> "✓"
    }

@Composable
private fun OpenRequests(
    open: List<HotelRequest>,
    onCancel: (String) -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        HotelPanel(title = "Tus solicitudes") {
            open.forEach { request ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val quantity = if (request.quantity > 1) "${request.quantity} × " else ""
                        Text(
                            quantity + request.type.label,
                            color = colors.ink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            request.status.label,
                            color = colors.muted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (request.status == com.vaiinilla.app.domain.hotel.RequestStatus.PENDING) {
                        Text(
                            "Cancelar",
                            color = colors.accent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            modifier =
                                Modifier
                                    .physicalPress(
                                        onClick = { onCancel(request.id) },
                                    ).padding(vertical = 12.dp, horizontal = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
