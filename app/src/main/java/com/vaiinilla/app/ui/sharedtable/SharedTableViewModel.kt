package com.vaiinilla.app.ui.sharedtable

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.model.SharedTable
import com.vaiinilla.app.domain.repository.SharedTableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

const val SHARED_TABLE_ALIAS_MAX = 30
private const val POLL_INTERVAL_MS = 5_000L

data class SharedTableUiState(
    val loaded: Boolean = false,
    val table: SharedTable? = null,
    val aliasInput: String = "",
    val busy: Boolean = false,
    val error: String? = null,
) {
    val canJoin: Boolean
        get() = aliasInput.isNotBlank() && !busy
}

/**
 * Mesa compartida del cliente: unirse con un alias, ver la mesa, "esto lo pago yo" y salir.
 * Se consulta cada 5 s solo mientras la pantalla está a la vista (no hay Realtime).
 */
@HiltViewModel
class SharedTableViewModel
    @Inject
    constructor(
        private val repository: SharedTableRepository,
    ) : ViewModel() {
        private val _uiState = mutableStateOf(SharedTableUiState())
        val uiState: State<SharedTableUiState> = _uiState

        private var pollingJob: Job? = null
        private var venueId: String? = null

        // Sube al empezar y al terminar cada cambio: una consulta que arrancó antes de que el cambio terminara trae
        // la mesa de antes y no debe pisarla (COMPRA-19).
        private var mutationVersion = 0

        /**
         * La mesa es del negocio donde se cargó. Al entrar a otro negocio se olvida: si no, el menú de allá
         * llamaba al mesero a la mesa de acá y el servidor respondía 404 (C08, K02).
         */
        fun bindVenue(establishmentId: String?) {
            if (establishmentId == venueId) return
            val changed = venueId != null
            venueId = establishmentId
            if (changed) {
                // Un sondeo que salió antes ya trae la mesa del negocio anterior.
                mutationVersion++
                _uiState.value = SharedTableUiState(aliasInput = _uiState.value.aliasInput)
            }
        }

        fun onVisible() {
            pollingJob?.cancel()
            pollingJob =
                viewModelScope.launch {
                    while (isActive) {
                        refresh()
                        delay(POLL_INTERVAL_MS)
                    }
                }
        }

        fun onHidden() {
            pollingJob?.cancel()
            pollingJob = null
        }

        fun updateAlias(value: String) {
            _uiState.value = _uiState.value.copy(aliasInput = value.take(SHARED_TABLE_ALIAS_MAX), error = null)
        }

        fun join(qrToken: String) {
            val alias =
                _uiState.value.aliasInput
                    .trim()
                    .replace(Regex("\\s+"), " ")
            if (alias.isEmpty() || _uiState.value.busy) return
            perform { repository.join(qrToken, alias) }
        }

        /** Llamar al mesero desde el menú sin haberse unido: lo sienta con su nombre, sin pasar por la tarjeta. */
        suspend fun joinForWaiterCall(
            qrToken: String,
            alias: String,
        ): Result<Unit> =
            withContext(Dispatchers.IO) { repository.join(qrToken, alias) }
                .onSuccess { _uiState.value = _uiState.value.copy(loaded = true, table = it) }
                .map { }

        fun claim(
            folio: Int,
            payIt: Boolean,
        ) = perform { repository.claim(folio, payIt) }

        fun leave() {
            if (_uiState.value.busy) return
            mutationVersion++
            _uiState.value = _uiState.value.copy(busy = true, error = null)
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { repository.leave() }
                mutationVersion++
                _uiState.value =
                    result.fold(
                        onSuccess = { _uiState.value.copy(busy = false, table = null) },
                        onFailure = {
                            _uiState.value.copy(
                                busy = false,
                                error = it.toUserFacingMessage("No se pudo salir de la mesa."),
                            )
                        },
                    )
            }
        }

        private fun perform(call: () -> Result<SharedTable>) {
            mutationVersion++
            _uiState.value = _uiState.value.copy(busy = true, error = null)
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { call() }
                mutationVersion++
                _uiState.value =
                    result.fold(
                        onSuccess = { _uiState.value.copy(busy = false, loaded = true, table = it) },
                        onFailure = {
                            _uiState.value.copy(
                                busy = false,
                                error = it.toUserFacingMessage("No se pudo actualizar la mesa."),
                            )
                        },
                    )
            }
        }

        private suspend fun refresh() {
            val versionAtStart = mutationVersion
            val result = withContext(Dispatchers.IO) { repository.current() }
            if (versionAtStart != mutationVersion || _uiState.value.busy) return
            // Una consulta fallida no borra la mesa que ya se ve; la siguiente lo intenta otra vez.
            result.onSuccess { _uiState.value = _uiState.value.copy(loaded = true, table = it) }
            result.onFailure { if (!_uiState.value.loaded) _uiState.value = _uiState.value.copy(loaded = true) }
        }
    }
