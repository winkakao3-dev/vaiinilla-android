package com.vaiinilla.app.ui.hotel.staff

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vaiinilla.app.ui.hotel.HotelPlaceholder

/** S03: Estado de habitaciones y solicitudes. Vacía; la rellena A2 (personal). */
@Composable
fun HotelRoomsScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    HotelPlaceholder(frame = "S03", title = "Estado de habitaciones y solicitudes", modifier = modifier)
}
