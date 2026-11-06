package com.vaiinilla.app.ui.hotel.guest

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vaiinilla.app.ui.hotel.HotelPlaceholder

/** H01: Bienvenida del huésped. Vacía; la rellena A1 (huésped). */
@Composable
fun HotelWelcomeScreen(
    onOrder: () -> Unit = {},
    onRequests: () -> Unit = {},
    onAccount: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    HotelPlaceholder(frame = "H01", title = "Bienvenida del huésped", modifier = modifier)
}
