package com.vaiinilla.app.ui.hotel.guest

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vaiinilla.app.ui.hotel.HotelPlaceholder

/** Código: Código de estancia. Vacía; la rellena A1 (huésped). */
@Composable
fun HotelStayCodeScreen(
    onLinked: () -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    HotelPlaceholder(frame = "Código", title = "Código de estancia", modifier = modifier)
}
