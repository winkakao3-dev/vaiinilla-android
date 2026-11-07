package com.vaiinilla.app.ui.hotel.guest

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.RetryKeys
import com.vaiinilla.app.domain.hotel.HotelConfig
import com.vaiinilla.app.domain.hotel.HotelRepository
import com.vaiinilla.app.domain.hotel.HotelRequest
import com.vaiinilla.app.domain.hotel.RequestType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Lo que se puede pedir por cantidad; el resto se pide o no se pide. */
internal val QUANTITY_REQUESTS = setOf(RequestType.TOWELS, RequestType.PILLOWS, RequestType.CHARGER)
internal const val MAX_REQUEST_QUANTITY = 4

data class HotelRequestsUiState(
    /** Lo elegido en la pantalla y todavía sin enviar, con su cantidad. */
    val picked: Map<RequestType, Int> = emptyMap(),
    val doNotDisturb: Boolean = false,
    /** Lo que tenía «Sin molestar» al abrir; solo se envía si cambió. */
    val doNotDisturbAtStart: Boolean = false,
    val open: List<HotelRequest> = emptyList(),
    val config: HotelConfig = HotelConfig(),
    val submitting: Boolean = false,
    val error: String? = null,
    /** Cuántas cosas se enviaron en el último envío; la pantalla lo avisa y se cierra. */
    val sentCount: Int = 0,
) {
    val pendingCount: Int get() = picked.size + if (doNotDisturb != doNotDisturbAtStart) 1 else 0
}

/** H05: lo que el huésped le pide al hotel. Cada cosa viaja como una solicitud con su propia llave de idempotencia. */
@HiltViewModel
class HotelRequestsViewModel
    @Inject
    constructor(
        private val repository: HotelRepository,
    ) : ViewModel() {
        private val _state = mutableStateOf(HotelRequestsUiState())
        val state: State<HotelRequestsUiState> = _state

        // Un reintento de lo mismo no crea otra solicitud: la llave sigue hasta que el servidor la acepta. Si cambia la
        // cantidad es otra solicitud y estrena llave: la misma llave con otro cuerpo el servidor la rechaza.
        private val retryKeys = RetryKeys()
        private var cancelling = false

        fun load() {
            viewModelScope.launch {
                val stay = withContext(Dispatchers.IO) { repository.myStay() }.getOrNull()
                val open =
                    withContext(
                        Dispatchers.IO,
                    ) { repository.myRequests() }.getOrNull().orEmpty().filter { it.status.isOpen }
                val config = stay?.settings ?: withContext(Dispatchers.IO) { repository.config() }.getOrNull()
                val dnd = stay?.doNotDisturb ?: false
                _state.value =
                    _state.value.copy(
                        open = open,
                        doNotDisturb = dnd,
                        doNotDisturbAtStart = dnd,
                        config = config ?: _state.value.config,
                    )
            }
        }

        /** Una cosa por cantidad suma uno (hasta cuatro y vuelve a cero); las demás se prenden y se apagan. */
        fun tap(type: RequestType) {
            val current = _state.value.picked[type] ?: 0
            val next =
                if (type in QUANTITY_REQUESTS) {
                    if (current >= MAX_REQUEST_QUANTITY) 0 else current + 1
                } else {
                    if (current > 0) 0 else 1
                }
            val picked = if (next == 0) _state.value.picked - type else _state.value.picked + (type to next)
            _state.value = _state.value.copy(picked = picked, error = null)
        }

        fun toggleDoNotDisturb() {
            _state.value = _state.value.copy(doNotDisturb = !_state.value.doNotDisturb, error = null)
        }

        fun submit() {
            val start = _state.value
            if (start.submitting || start.pendingCount == 0) return
            _state.value = start.copy(submitting = true, error = null)
            viewModelScope.launch {
                var sent = 0
                var failure: Throwable? = null
                for ((type, quantity) in _state.value.picked) {
                    val result =
                        withContext(Dispatchers.IO) {
                            retryKeys.withKey(
                                "request",
                                type,
                                quantity,
                            ) { key -> repository.createRequest(type, quantity, null, key) }
                        }
                    if (result.isSuccess) {
                        sent++
                        _state.value = _state.value.copy(picked = _state.value.picked - type)
                    } else {
                        failure = result.exceptionOrNull()
                        break
                    }
                }
                if (failure == null && _state.value.doNotDisturb != _state.value.doNotDisturbAtStart) {
                    val wanted = _state.value.doNotDisturb
                    val result = withContext(Dispatchers.IO) { repository.setDoNotDisturb(wanted) }
                    if (result.isSuccess) {
                        sent++
                        _state.value = _state.value.copy(doNotDisturbAtStart = wanted)
                    } else {
                        failure = result.exceptionOrNull()
                    }
                }
                _state.value =
                    _state.value.copy(
                        submitting = false,
                        sentCount = if (failure == null) sent else 0,
                        error =
                            failure?.let {
                                val prefix = if (sent > 0) "Enviamos $sent, pero faltaron algunas. " else ""
                                prefix + hotelErrorMessage(it, "No pudimos enviar tu solicitud.")
                            },
                    )
            }
        }

        fun cancel(requestId: String) {
            if (cancelling) return
            cancelling = true
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        retryKeys.withKey("cancel", requestId) { key -> repository.cancelRequest(requestId, key) }
                    }
                cancelling = false
                _state.value =
                    result.fold(
                        onSuccess = { _state.value.copy(open = _state.value.open.filterNot { it.id == requestId }) },
                        onFailure = { _state.value.copy(error = hotelErrorMessage(it, "No pudimos cancelarla.")) },
                    )
            }
        }

        fun clearSent() {
            _state.value = _state.value.copy(sentCount = 0)
        }
    }
