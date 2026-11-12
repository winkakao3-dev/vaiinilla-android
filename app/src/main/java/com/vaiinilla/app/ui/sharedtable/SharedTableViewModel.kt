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
private const val OTHERS_ATTEMPTS = 4
private const val OTHERS_RETRY_MS = 1_500L
private const val DEFAULT_WATCHER = "pantalla"
const val SHARED_TABLE_WATCHER_PAGE = "mesa"
const val SHARED_TABLE_WATCHER_ORDERS = "mis-pedidos"

data class SharedTableUiState(
    val loaded: Boolean = false,
    val table: SharedTable? = null,
    val aliasInput: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    /** El pedido cuyo «Esto lo pago yo» falló: el error se pinta junto a él (C05). */
    val errorFolio: Int? = null,
    /** Quienes ya están en la mesa del QR, antes de unirse. */
    val othersAtTable: List<String> = emptyList(),
    /** Eligió «Pedir solo yo» para el QR de ahora: ni la hoja del menú ni Pedidos lo vuelven a invitar. */
    val orderedAlone: Boolean = false,
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

        /** Al salir o cambiar de cuenta: el nombre y la mesa eran del usuario anterior (EQUIPO-28). */
        fun clearForSessionTermination() {
            pollingJob?.cancel()
            pollingJob = null
            watchers.clear()
            venueId = null
            watchedToken = null
            mutationVersion++
            _uiState.value = SharedTableUiState()
        }

        // Quiénes están a la vista (la página de la mesa, la tarjeta de Mis pedidos). Al pasar de una a otra, la
        // que se va se detiene después de que la nueva arrancó: con un solo interruptor apagaba el sondeo de la
        // nueva y el otro cliente no veía «Paga Ana» hasta reiniciar la app (C05).
        private val watchers = mutableSetOf<String>()

        /** [watcher] nombra quién mira; el sondeo sigue mientras quede alguno. Al volver, lee la mesa de una vez. */
        fun onVisible(watcher: String = DEFAULT_WATCHER) {
            watchers += watcher
            pollingJob?.cancel()
            pollingJob =
                viewModelScope.launch {
                    while (isActive) {
                        refresh()
                        delay(POLL_INTERVAL_MS)
                    }
                }
        }

        /** Si la mesa se sigue consultando; para las pruebas. */
        internal val polling: Boolean
            get() = pollingJob?.isActive == true

        /** Se puede llamar dos veces (al detenerse y al salir de la composición): solo quita a [watcher]. */
        fun onHidden(watcher: String = DEFAULT_WATCHER) {
            watchers -= watcher
            if (watchers.isNotEmpty()) return
            pollingJob?.cancel()
            pollingJob = null
        }

        private var watchedToken: String? = null

        /** Para probar sin esperar; en la app es [OTHERS_RETRY_MS]. */
        internal var othersRetryMs = OTHERS_RETRY_MS

        /**
         * Al llegar con un QR: lee quién ya está ahí para la invitación y si ya está sentado en alguna mesa.
         * Un fallo deja la invitación sin nombres. Otro QR vuelve a invitar aunque antes eligiera pedir solo.
         */
        fun watchSpace(qrToken: String?) {
            if (qrToken == watchedToken) return
            watchedToken = qrToken
            _uiState.value = _uiState.value.copy(othersAtTable = emptyList(), orderedAlone = false)
            if (qrToken == null) return
            viewModelScope.launch {
                // Al llegar por QR la sesión de ese negocio puede no estar lista todavía: sin insistir, la invitación
                // a compartir la mesa no salía y había que ir a Pedidos para unirse.
                repeat(OTHERS_ATTEMPTS) { attempt ->
                    if (attempt > 0) delay(othersRetryMs)
                    if (watchedToken != qrToken) return@launch
                    val others = withContext(Dispatchers.IO) { repository.othersAt(qrToken) }.getOrNull()
                    if (others != null) {
                        if (watchedToken == qrToken) _uiState.value = _uiState.value.copy(othersAtTable = others)
                        return@launch
                    }
                }
            }
            viewModelScope.launch { refresh() }
        }

        fun orderAlone() {
            _uiState.value = _uiState.value.copy(orderedAlone = true)
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
        ) = perform(folio = folio) { repository.claim(folio, payIt) }

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

        private fun perform(
            folio: Int? = null,
            call: () -> Result<SharedTable>,
        ) {
            mutationVersion++
            _uiState.value = _uiState.value.copy(busy = true, error = null, errorFolio = null)
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
                                errorFolio = folio,
                            )
                        },
                    )
                // Si el servidor dice que ya está pagado, la mesa que se ve está vieja: se vuelve a leer.
                if (result.isFailure && folio != null) refresh()
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
