package com.vaiinilla.app.ui.hotel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.domain.hotel.HotelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** A dónde manda el QR de una habitación: la bienvenida (H01) si ya hay estancia ligada, o el código de estancia. */
@HiltViewModel
class HotelEntryViewModel
    @Inject
    constructor(
        private val repository: HotelRepository,
    ) : ViewModel() {
        enum class Destination { RESOLVING, HOME, CODE }

        private val _destination = mutableStateOf(Destination.RESOLVING)
        val destination: State<Destination> = _destination

        fun resolve() {
            _destination.value = Destination.RESOLVING
            viewModelScope.launch {
                val stay = withContext(Dispatchers.IO) { repository.myStay() }
                // Sin sesión o sin conexión también se pide el código: ahí se entra o se reintenta.
                _destination.value = if (stay.getOrNull() != null) Destination.HOME else Destination.CODE
            }
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
    LaunchedEffect(Unit) { viewModel.resolve() }
    val destination = viewModel.destination.value
    LaunchedEffect(destination) {
        when (destination) {
            HotelEntryViewModel.Destination.HOME -> onHome()
            HotelEntryViewModel.Destination.CODE -> onCode()
            HotelEntryViewModel.Destination.RESOLVING -> Unit
        }
    }
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}
