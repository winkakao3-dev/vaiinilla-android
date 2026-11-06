package com.vaiinilla.app.ui.hotel.staff

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vaiinilla.app.ui.hotel.HotelPlaceholder

/** Cargo: Captura de cargo manual. Vacía; la rellena A2 (personal). */
@Composable
fun HotelManualChargeSheet(
    stayId: String,
    onDone: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    HotelPlaceholder(frame = "Cargo", title = "Captura de cargo manual", modifier = modifier)
}
