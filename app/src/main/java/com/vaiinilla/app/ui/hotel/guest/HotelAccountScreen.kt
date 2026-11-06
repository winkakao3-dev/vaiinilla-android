package com.vaiinilla.app.ui.hotel.guest

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vaiinilla.app.ui.hotel.HotelPlaceholder

/** H06: Mi cuenta. Vacía; la rellena A1 (huésped). */
@Composable
fun HotelAccountScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    HotelPlaceholder(frame = "H06", title = "Mi cuenta", modifier = modifier)
}
