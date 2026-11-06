package com.vaiinilla.app.ui.hotel.staff

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.hotel.HotelRoom
import com.vaiinilla.app.domain.hotel.NewStay
import com.vaiinilla.app.domain.hotel.Stay
import com.vaiinilla.app.ui.screens.OperationalField
import com.vaiinilla.app.ui.screens.rememberOperationalColors
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import java.time.LocalDate

/**
 * Check-in en recepción. No tiene marco propio en Doop: usa los mismos componentes que S04. Al terminar muestra
 * el código de estancia de 6 dígitos que recepción le dice al huésped.
 */
@Composable
internal fun HotelCheckInScreen(
    state: HotelReceptionUiState,
    today: LocalDate,
    onSubmit: (NewStay) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HotelCheckInContent(
        rooms = state.freeRooms,
        created = state.created,
        busy = state.busy,
        error = state.error,
        today = today,
        onSubmit = onSubmit,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
internal fun HotelCheckInContent(
    rooms: List<HotelRoom>,
    created: Stay?,
    busy: Boolean,
    error: String?,
    today: LocalDate,
    onSubmit: (NewStay) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialName: String = "",
    initialRate: String = "",
) {
    val colors = LocalVaiinillaColors.current
    val operational = rememberOperationalColors()
    if (created != null) {
        HotelStaffPage(kicker = "Recepción · Check-in", title = "Listo, ${created.guestName}", modifier = modifier, onBack = onBack) {
            HotelCard {
                HotelCaps("CÓDIGO DE ESTANCIA", modifier = Modifier.padding(bottom = 6.dp))
                Text(
                    created.code.orEmpty().chunked(3).joinToString(" "),
                    color = colors.ink,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.8).sp,
                )
                Text(
                    "Díselo al huésped: lo escribe en la app al escanear el QR de ${created.roomName ?: "su habitación"}.",
                    color = colors.muted,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            HotelButton("Listo", onClick = onBack, kind = HotelButtonKind.Lime, height = 56.dp, modifier = Modifier.fillMaxWidth())
        }
        return
    }
    var name by rememberSaveable { mutableStateOf(initialName) }
    var contact by rememberSaveable { mutableStateOf("") }
    var rate by rememberSaveable { mutableStateOf(initialRate) }
    var nights by rememberSaveable { mutableIntStateOf(1) }
    var roomId by rememberSaveable { mutableStateOf<Int?>(null) }
    val chosen = rooms.firstOrNull { it.id == roomId }
    val parsedRate = parseAmount(rate)
    val ready = name.isNotBlank() && chosen != null && parsedRate != null
    HotelStaffPage(
        kicker = "Recepción",
        title = "Nuevo check-in",
        modifier = modifier,
        onBack = onBack,
        bottomBar = {
            HotelButton(
                label = if (busy) "Registrando…" else "Registrar llegada",
                onClick = {
                    onSubmit(
                        NewStay(
                            guestName = name.trim(),
                            contact = contact.trim().ifBlank { null },
                            roomId = chosen!!.id,
                            checkIn = today.toString(),
                            checkOut = today.plusDays(nights.toLong()).toString(),
                            nightlyRate = parsedRate,
                        ),
                    )
                },
                kind = HotelButtonKind.Lime,
                enabled = ready && !busy,
                height = 60.dp,
                fontSize = 16,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        HotelCard() {
            HotelCaps("HUÉSPED", modifier = Modifier.padding(bottom = 10.dp))
            OperationalField(value = name, onValueChange = { name = it }, label = "Nombre", colors = operational, placeholder = "Laura Ramos")
            OperationalField(
                value = contact,
                onValueChange = { contact = it },
                label = "Teléfono o correo (opcional)",
                colors = operational,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        HotelCard() {
            HotelCaps("HABITACIÓN LIBRE", modifier = Modifier.padding(bottom = 10.dp))
            if (rooms.isEmpty()) {
                Text("No hay habitaciones libres.", color = colors.muted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rooms.forEach { room ->
                        HotelChip(hotelRoomNumber(room.name), selected = room.id == roomId, onClick = { roomId = room.id }, onCard = true)
                    }
                }
            }
        }
        HotelCard() {
            HotelCaps("NOCHES Y TARIFA", modifier = Modifier.padding(bottom = 10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${if (nights == 1) "1 noche" else "$nights noches"} · sale el ${hotelDateRange(today.plusDays(nights.toLong()).toString(), null)}",
                    color = colors.ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                HotelButton("−", onClick = { if (nights > 1) nights -= 1 }, kind = HotelButtonKind.Outline, height = 38.dp, modifier = Modifier.padding(end = 8.dp))
                HotelButton("+", onClick = { if (nights < 60) nights += 1 }, kind = HotelButtonKind.Outline, height = 38.dp)
            }
            OperationalField(
                value = rate,
                onValueChange = { rate = it },
                label = "Tarifa por noche",
                colors = operational,
                placeholder = "1650",
                supporting = "La pone el hotel; la app no trae precios.",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        error?.let { HotelNotice(it) }
    }
}
