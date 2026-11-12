package com.vaiinilla.app.ui.hotel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import com.vaiinilla.app.domain.hotel.HotelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** A dónde manda el QR de una habitación: la bienvenida (H01) si ya hay estancia ligada, o el código de estancia. */
@HiltViewModel
class HotelEntryViewModel
    @Inject
    constructor(
        private val repository: HotelRepository,
    ) : ViewModel() {
        enum class Destination { HOME, CODE }

        /**
         * Pregunta cada vez: este ViewModel vive con la actividad, y un destino guardado mandaba a pedir el código a
         * quien ya había ligado su estancia y volvía a escanear su QR.
         */
        suspend fun resolve(): Destination {
            val stay = withContext(Dispatchers.IO) { repository.myStay() }
            // Sin sesión o sin conexión también se pide el código: ahí se entra o se reintenta.
            return if (stay.getOrNull() != null) Destination.HOME else Destination.CODE
        }
    }

/** Se abre al escanear el QR de una habitación (`tipo: habitacion`) en lugar del catálogo. */
@Composable
fun HotelEntryScreen(
    onHome: () -> Unit,
    onCode: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HotelEntryViewModel = hotelViewModel(),
) {
    LaunchedEffect(Unit) {
        when (viewModel.resolve()) {
            HotelEntryViewModel.Destination.HOME -> onHome()
            HotelEntryViewModel.Destination.CODE -> onCode()
        }
    }
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}