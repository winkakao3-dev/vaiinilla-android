package com.vaiinilla.app.ui.hotel.staff

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vaiinilla.app.ui.hotel.HotelPlaceholder

/** Recepción: Llegadas y salidas. Vacía; la rellena A2 (personal). */
@Composable
fun HotelReceptionScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    HotelPlaceholder(frame = "Recepción", title = "Llegadas y salidas", modifier = modifier)
}
