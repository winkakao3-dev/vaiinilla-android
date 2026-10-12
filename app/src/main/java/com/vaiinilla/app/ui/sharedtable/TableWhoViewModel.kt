package com.vaiinilla.app.ui.sharedtable

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.model.SharedTable
import com.vaiinilla.app.domain.model.TableSession
import com.vaiinilla.app.domain.repository.SharedTableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val PARTICIPANT_NAME_TAKEN = "PARTICIPANT_NAME_TAKEN"

data class TableWhoUiState(
    val loading: Boolean = true,
    val session: TableSession? = null,
    /** No se pudo leer la sesión del QR. */
    val loadError: String? = null,
    val aliasInput: String = "",
    val busy: Boolean = false,
    /** Error al sentarse: un nombre repetido trae el mensaje del servidor tal cual. */
    val joinError: String? = null,
    /** "No soy X": muestra la lista de participantes en vez de solo "Continuar como X". */
    val showList: Boolean = false,
    /** Formulario para agregar un nombre nuevo. */
    val showAdd: Boolean = false,
    /** Ya se sentó en la mesa: la pantalla pasa al menú. */
    val seated: Boolean = false,
) {
    /** Ofrece seguir con el nombre que el servidor ya reconoce. */
    val offerContinue: Boolean
        get() = session?.me != null && !showList

    val canSubmitAlias: Boolean
        get() = aliasInput.isNotBlank() && !busy
}

/**
 * "¿Quién eres?" al entrar a una mesa por su QR, antes de pedir: se elige un participante, se
 * continúa como el que ya reconoce el servidor o se agrega un nombre nuevo. Igual que la PWA
 * (`table-who-page.tsx`). La verdad es el servidor: `me` viene de `/mesas/espacio/:token`.
 */
@HiltViewModel
class TableWhoViewModel
    @Inject
    constructor(
        private val repository: SharedTableRepository,
    ) : ViewModel() {
        private val _uiState = mutableStateOf(TableWhoUiState())
        val uiState: State<TableWhoUiState> = _uiState

        private var qrToken: String? = null

        fun load(qrToken: String) {
            if (this.qrToken == qrToken && _uiState.value.session != null) return
            this.qrToken = qrToken
            _uiState.value = TableWhoUiState(loading = true)
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { repository.session(qrToken) }
                _uiState.value =
                    result.fold(
                        onSuccess = { session ->
                            TableWhoUiState(
                                loading = false,
                                session = session,
                                showAdd = session.participants.isEmpty(),
                            )
                        },
                        onFailure = { error ->
                            TableWhoUiState(
                                loading = false,
                                loadError = error.toUserFacingMessage("No pudimos ver quién está en la mesa."),
                            )
                        },
                    )
            }
        }

        /** Al salir de la pantalla: la siguiente visita empieza de cero, sin un "seated" de antes. */
        fun reset() {
            qrToken = null
            _uiState.value = TableWhoUiState()
        }

        fun updateAlias(value: String) {
            _uiState.value = _uiState.value.copy(aliasInput = value.take(SHARED_TABLE_ALIAS_MAX), joinError = null)
        }

        /** "No soy X": la lista de participantes para elegir a otra persona. */
        fun showParticipantList() {
            val session = _uiState.value.session ?: return
            _uiState.value =
                _uiState.value.copy(
                    showList = true,
                    showAdd = session.participants.isEmpty(),
                    joinError = null,
                )
        }

        fun showAddForm() {
            _uiState.value = _uiState.value.copy(showAdd = true, joinError = null)
        }

        fun submitAlias() {
            val alias =
                _uiState.value.aliasInput
                    .trim()
                    .replace(Regex("\\s+"), " ")
            if (alias.isEmpty()) return
            perform { repository.join(requireToken(), alias) }
        }

        fun continueAs(participantId: String) {
            perform { repository.continueAs(requireToken(), participantId) }
        }

        private fun requireToken(): String = requireNotNull(qrToken) { "La mesa no se cargó." }

        private fun perform(call: () -> Result<SharedTable>) {
            if (_uiState.value.busy) return
            _uiState.value = _uiState.value.copy(busy = true, joinError = null)
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { call() }
                _uiState.value =
                    result.fold(
                        onSuccess = { _uiState.value.copy(busy = false, seated = true) },
                        onFailure = { error ->
                            _uiState.value.copy(
                                busy = false,
                                joinError = joinErrorMessage(error),
                            )
                        },
                    )
            }
        }

        private fun joinErrorMessage(error: Throwable): String =
            (error as? ApiClientException)
                ?.takeIf { it.code == PARTICIPANT_NAME_TAKEN }
                ?.message
                ?: error.toUserFacingMessage("No pudimos sentarte en la mesa.")
    }
