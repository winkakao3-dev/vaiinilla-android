package com.vaiinilla.app.ui.hotel.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.domain.hotel.HotelRepository
import com.vaiinilla.app.domain.hotel.HotelRequest
import com.vaiinilla.app.domain.hotel.HotelRoom
import com.vaiinilla.app.domain.hotel.RequestAction
import com.vaiinilla.app.domain.hotel.RoomCleaning
import com.vaiinilla.app.core.network.RetryKeys
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class RoomsFilter(
    val label: String,
) {
    ALL("Todas"),
    DIRTY("Sucias"),
    CLEAN("Limpias"),
    OCCUPIED("Ocupadas"),
}

/** Cómo se ve una habitación en el tablero del ama de llaves (S03). */
enum class RoomTone(
    val label: String,
) {
    OCCUPIED("OCUPADA"),
    DIRTY("POR LIMPIAR"),
    READY("LISTA"),
    DO_NOT_DISTURB("NO MOLESTAR"),
}

internal fun roomTone(room: HotelRoom): RoomTone =
    when {
        room.doNotDisturb -> RoomTone.DO_NOT_DISTURB
        room.occupied -> RoomTone.OCCUPIED
        room.cleaning == RoomCleaning.DIRTY -> RoomTone.DIRTY
        else -> RoomTone.READY
    }

internal fun RoomsFilter.matches(room: HotelRoom): Boolean =
    when (this) {
        RoomsFilter.ALL -> true
        RoomsFilter.DIRTY -> room.cleaning == RoomCleaning.DIRTY
        RoomsFilter.CLEAN -> room.cleaning == RoomCleaning.READY && !room.occupied
        RoomsFilter.OCCUPIED -> room.occupied
    }

data class HotelRoomsUiState(
    val rooms: List<HotelRoom> = emptyList(),
    val requests: List<HotelRequest> = emptyList(),
    val filter: RoomsFilter = RoomsFilter.ALL,
    /** Piso del tablero; null = el primero que tenga trabajo (se resuelve en [floorShown]). */
    val floor: Int? = null,
    val loaded: Boolean = false,
    val error: String? = null,
    /** Solicitud o habitación con una escritura en curso, para no mandar dos veces. */
    val busyId: String? = null,
) {
    val floors: List<Int> get() = rooms.mapNotNull { it.floor }.distinct().sorted()

    /** El piso que se ve: el elegido, o el primero con habitaciones por limpiar, o el más bajo. */
    val floorShown: Int?
        get() =
            floor?.takeIf { it in floors }
                ?: rooms.filter { it.cleaning == RoomCleaning.DIRTY }.mapNotNull { it.floor }.minOrNull()
                ?: floors.firstOrNull()

    val visibleRooms: List<HotelRoom>
        get() {
            val shown = floorShown
            // Una habitación sin piso no pertenece a ninguno: se ve en todos, para que nadie se quede sin limpiar.
            return rooms
                .filter { shown == null || it.floor == null || it.floor == shown }
                .filter { filter.matches(it) }
                .sortedBy { it.name }
        }

    fun count(filter: RoomsFilter): Int = rooms.count { filter.matches(it) }
}

/**
 * S03: el tablero del ama de llaves. Las llamadas del repositorio son bloqueantes, así que corren en
 * `Dispatchers.IO`; cada escritura lleva su propia llave de idempotencia.
 */
@HiltViewModel
class HotelRoomsViewModel
    @Inject
    constructor(
        private val repository: HotelRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(HotelRoomsUiState())
        val state: StateFlow<HotelRoomsUiState> = _state.asStateFlow()

        private val retryKeys = RetryKeys()

        // El último aviso que puso la propia lectura: solo ese se borra cuando la lectura vuelve a salir bien.
        private var refreshError: String? = null

        fun refresh() {
            viewModelScope.launch {
                val rooms = withContext(Dispatchers.IO) { repository.rooms() }
                val requests = withContext(Dispatchers.IO) { repository.openRequests() }
                val failure = (rooms.exceptionOrNull() ?: requests.exceptionOrNull())?.let { staffErrorMessage(it, "No se pudo actualizar.") }
                _state.update { current ->
                    current.copy(
                        rooms = rooms.getOrDefault(current.rooms),
                        requests = requests.getOrDefault(current.requests).sortedBy { it.createdAt },
                        loaded = current.loaded || rooms.isSuccess,
                        // Si «Tomar» o «Listo» falló (otra persona la tomó), el aviso sobrevive a la recarga.
                        error = failure ?: current.error?.takeUnless { it == refreshError },
                    )
                }
                refreshError = failure
            }
        }

        fun setFilter(filter: RoomsFilter) = _state.update { it.copy(filter = filter) }

        /** Toca el piso del encabezado: pasa al siguiente. */
        fun nextFloor() =
            _state.update { current ->
                val floors = current.floors
                if (floors.size < 2) return@update current
                val index = floors.indexOf(current.floorShown)
                current.copy(floor = floors[(index + 1) % floors.size])
            }

        fun take(request: HotelRequest) = transition(request, RequestAction.TAKE)

        fun resolve(request: HotelRequest) = transition(request, RequestAction.RESOLVE)

        private fun transition(
            request: HotelRequest,
            action: RequestAction,
        ) {
            if (_state.value.busyId != null) return
            _state.update { it.copy(busyId = request.id, error = null) }
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        retryKeys.withKey("transition", request.id, action, request.version) { key ->
                            repository.transition(request.id, action, key, request.version)
                        }
                    }
                _state.update { it.copy(busyId = null, error = result.exceptionOrNull()?.let { e -> staffErrorMessage(e, "No se pudo guardar.") }) }
                refresh()
            }
        }

        /** Una habitación vacía alterna entre lista y por limpiar; la ocupada no se toca desde aquí. */
        fun toggleCleaning(room: HotelRoom) {
            if (room.occupied || _state.value.busyId != null) return
            val next = if (room.cleaning == RoomCleaning.DIRTY) RoomCleaning.READY else RoomCleaning.DIRTY
            _state.update { it.copy(busyId = "room-${room.id}", error = null) }
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) { retryKeys.withKey("cleaning", room.id, next) { key -> repository.setCleaning(room.id, next, key) } }
                _state.update { it.copy(busyId = null, error = result.exceptionOrNull()?.let { e -> staffErrorMessage(e, "No se pudo guardar.") }) }
                refresh()
            }
        }
    }
