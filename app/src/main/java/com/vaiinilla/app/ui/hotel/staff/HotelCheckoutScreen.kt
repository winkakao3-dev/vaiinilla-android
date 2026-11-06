package com.vaiinilla.app.ui.hotel.staff

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vaiinilla.app.ui.hotel.HotelPlaceholder

/** S04: Check-out. Vacía; la rellena A2 (personal). */
@Composable
fun HotelCheckoutScreen(
    stayId: String,
    onDone: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    HotelPlaceholder(frame = "S04", title = "Check-out", modifier = modifier)
}
