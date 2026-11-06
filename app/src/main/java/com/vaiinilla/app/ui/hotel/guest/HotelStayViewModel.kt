package com.vaiinilla.app.ui.hotel.guest

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.domain.hotel.HotelConfig
import com.vaiinilla.app.domain.hotel.HotelRepository
import com.vaiinilla.app.domain.hotel.Stay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class HotelStayUiState(
    val loading: Boolean = true,
    val stay: Stay? = null,
    /** Lo que el hotel ofrece; sin respuesta del servidor se usan los valores de siempre (sin salida tardía). */
    val config: HotelConfig = HotelConfig(),
    val error: String? = null,
    val linking: Boolean = false,
    val linkError: String? = null,
)

/** La estancia del huésped: la bienvenida (H01), la cuenta (H06) y el código de estancia la leen de aquí. */
@HiltViewModel
class HotelStayViewModel
    @Inject
    constructor(
        private val repository: HotelRepository,
    ) : ViewModel() {
        private val _state = mutableStateOf(HotelStayUiState())
        val state: State<HotelStayUiState> = _state

        fun load() {
            _state.value = _state.value.copy(loading = true, error = null)
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { repository.myStay() }
                val config =
                    result.getOrNull()?.settings
                        ?: withContext(Dispatchers.IO) { repository.config() }.getOrNull()
                _state.value =
                    result.fold(
                        onSuccess = { _state.value.copy(loading = false, stay = it, config = config ?: _state.value.config) },
                        onFailure = {
                            _state.value.copy(
                                loading = false,
                                error = hotelErrorMessage(it, "No pudimos cargar tu estancia."),
                            )
                        },
                    )
            }
        }

        /** Liga la cuenta con la estancia. [onLinked] corre en el hilo principal cuando el servidor la acepta. */
        fun link(
            code: String,
            spaceId: Int?,
            qrToken: String?,
            onLinked: () -> Unit,
        ) {
            if (_state.value.linking) return
            _state.value = _state.value.copy(linking = true, linkError = null)
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { repository.linkStay(code, spaceId, qrToken) }
                result.fold(
                    onSuccess = {
                        _state.value = _state.value.copy(linking = false, stay = it, linkError = null)
                        onLinked()
                    },
                    onFailure = {
                        _state.value = _state.value.copy(linking = false, linkError = hotelErrorMessage(it))
                    },
                )
            }
        }

        fun clearLinkError() {
            if (_state.value.linkError != null) _state.value = _state.value.copy(linkError = null)
        }
    }
