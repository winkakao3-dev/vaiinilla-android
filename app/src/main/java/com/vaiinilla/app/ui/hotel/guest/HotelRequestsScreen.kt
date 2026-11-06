package com.vaiinilla.app.ui.hotel.guest

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vaiinilla.app.ui.hotel.HotelPlaceholder

/** H05: Pedir al hotel. Vacía; la rellena A1 (huésped). */
@Composable
fun HotelRequestsScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    HotelPlaceholder(frame = "H05", title = "Pedir al hotel", modifier = modifier)
}
