package com.vaiinilla.app.ui.hotel.staff

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vaiinilla.app.ui.hotel.HotelPlaceholder

/** Check-in: Registrar llegada. Vacía; la rellena A2 (personal). */
@Composable
fun HotelCheckInScreen(
    onDone: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    HotelPlaceholder(frame = "Check-in", title = "Registrar llegada", modifier = modifier)
}
