package com.vaiinilla.app.ui.hotel.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.hotel.HotelRepository
import com.vaiinilla.app.domain.hotel.HotelSummary
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.admin.AdminBar
import com.vaiinilla.app.ui.screens.admin.AdminLoadError
import com.vaiinilla.app.ui.screens.admin.AdminPanel
import com.vaiinilla.app.ui.screens.admin.AdminRow
import com.vaiinilla.app.ui.screens.admin.AdminRowDivider
import com.vaiinilla.app.ui.screens.admin.AdminRowValue
import com.vaiinilla.app.ui.screens.admin.coralText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Lo que el inicio del admin muestra del hotel: el resumen de hoy o por qué no se pudo cargar. */
data class HotelSummaryUiState(
    val summary: HotelSummary? = null,
    val error: String? = null,
)

@HiltViewModel
class HotelAdminSummaryViewModel
    @Inject
    constructor(
        private val repository: HotelRepository,
    ) : ViewModel() {
        private val _uiState = mutableStateOf(HotelSummaryUiState())
        val uiState: State<HotelSummaryUiState> = _uiState

        fun refresh() {
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { repository.summary() }
                _uiState.value =
                    result.fold(
                        onSuccess = { HotelSummaryUiState(summary = it) },
                        onFailure = {
                            _uiState.value.copy(error = it.toUserFacingMessage("No se pudo cargar el hotel."))
                        },
                    )
            }
        }
    }

/** Una solicitud lleva más de esto sin tomarse y el mosaico la marca en coral. */
internal const val HOTEL_SLOW_REQUEST_MINUTES = 10

/**
 * A03: ocupación, room service, solicitudes, lo más pedido y salidas de hoy, para el inicio del admin de un hotel
 * (marco A03 del lienzo de Doop, página «Hotel»).
 */
internal fun LazyListScope.hotelSummaryItems(
    state: HotelSummaryUiState,
    colors: OperationalColors,
    onRetry: () -> Unit,
) {
    val summary = state.summary
    if (summary == null) {
        state.error?.let { error -> item(key = "hotel-error") { AdminLoadError(error, colors, onRetry) } }
        return
    }
    item(key = "hotel-mosaicos") { HotelSummaryTiles(summary, colors) }
    item(key = "hotel-top") { HotelTopItemsPanel(summary, colors) }
    item(key = "hotel-salidas") { HotelDeparturesPanel(summary, colors) }
}

@Composable
internal fun HotelSummaryTiles(
    summary: HotelSummary,
    colors: OperationalColors,
) {
    val good = if (colors.isDark) colors.accentLime else colors.accentInk
    val slow = (summary.oldestRequestMinutes ?: 0) > HOTEL_SLOW_REQUEST_MINUTES
    Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HotelTile("Ocupación", "${summary.occupied} de ${summary.totalRooms}", colors, Modifier.weight(1f)) {
                AdminBar(
                    fraction = if (summary.totalRooms == 0) 0f else summary.occupied / summary.totalRooms.toFloat(),
                    colors = colors,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            HotelTile(
                "Room service hoy",
                summary.roomServiceToday?.let(::moneyLabel) ?: "—",
                colors,
                Modifier.weight(1f),
            ) { HotelTileNote("Cargo a habitación", good) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HotelTile("Pedidos a cuartos", "${summary.roomOrdersToday}", colors, Modifier.weight(1f)) {
                HotelTileNote(kitchenNow(summary.inKitchenNow), good)
            }
            HotelTile("Solicitudes abiertas", "${summary.openRequests}", colors, Modifier.weight(1f)) {
                val oldest = summary.oldestRequestMinutes
                when {
                    summary.openRequests == 0 -> HotelTileNote("Al corriente", good)
                    oldest != null ->
                        HotelTileNote(
                            "La más vieja lleva $oldest min",
                            if (slow) colors.coralText else good,
                        )
                }
            }
        }
    }
}

internal fun kitchenNow(count: Int): String =
    when (count) {
        0 -> "Cocina libre"
        1 -> "1 en cocina ahora"
        else -> "$count en cocina ahora"
    }

@Composable
private fun HotelTile(
    label: String,
    value: String,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    note: @Composable () -> Unit = {},
) {
    AdminPanel(colors, modifier, padding = PaddingValues(16.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textMuted, maxLines = 1)
        Text(
            value,
            fontSize = 32.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1.4).sp,
            color = colors.textPrimary,
            maxLines = 1,
            modifier = Modifier.padding(top = 4.dp),
        )
        note()
    }
}

@Composable
private fun HotelTileNote(
    text: String,
    tint: Color,
) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = tint,
        maxLines = 1,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
internal fun HotelTopItemsPanel(
    summary: HotelSummary,
    colors: OperationalColors,
) {
    AdminPanel(colors, Modifier.padding(top = 12.dp), padding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)) {
        AdminRow(title = "Lo más pedido", colors = colors, detail = "De room service")
        if (summary.topItems.isEmpty()) {
            Text(
                "Todavía no hay pedidos a las habitaciones.",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMuted,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )
        }
        summary.topItems.forEach { item ->
            AdminRowDivider(colors)
            AdminRow(title = item.name, colors = colors, detail = ordersLabel(item.quantity)) {
                item.total?.let { AdminRowValue(moneyLabel(it), colors) }
            }
        }
    }
}

@Composable
internal fun HotelDeparturesPanel(
    summary: HotelSummary,
    colors: OperationalColors,
) {
    val good = if (colors.isDark) colors.accentLime else colors.accentInk
    AdminPanel(colors, Modifier.padding(top = 12.dp), padding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)) {
        AdminRow(
            title = "Salidas hoy",
            colors = colors,
            detail = departuresDetail(summary),
        ) {
            Text(
                "${summary.departuresToday}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = good,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
    }
}

/** «304, 112 y 408 hacen check-out»; sin la lista del servidor, solo cuántas. */
internal fun departuresDetail(summary: HotelSummary): String {
    val rooms = summary.departures.map { it.roomName.removePrefix("Habitación").trim() }.filter { it.isNotEmpty() }
    return when {
        rooms.size == 1 -> "${rooms[0]} hace check-out"
        rooms.size > 1 -> rooms.dropLast(1).joinToString(", ") + " y " + rooms.last() + " hacen check-out"
        summary.departuresToday == 1 -> "Una habitación hace check-out"
        else -> "Habitaciones que hacen check-out"
    }
}

internal fun ordersLabel(count: Int): String = if (count == 1) "1 pedido" else "$count pedidos"
