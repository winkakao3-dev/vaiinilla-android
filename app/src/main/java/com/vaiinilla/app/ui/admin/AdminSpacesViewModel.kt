package com.vaiinilla.app.ui.admin

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.admin.ManagedSpace
import com.vaiinilla.app.domain.admin.ReservationSchedule
import com.vaiinilla.app.domain.admin.SpacePatch
import com.vaiinilla.app.domain.admin.SpaceRules
import com.vaiinilla.app.domain.admin.SpaceType
import com.vaiinilla.app.domain.model.Reservation
import com.vaiinilla.app.domain.model.ReservationAccess
import com.vaiinilla.app.domain.model.ReservationPaymentMethod
import com.vaiinilla.app.domain.model.ReservationState
import com.vaiinilla.app.domain.repository.AdminSpacesRepository
import com.vaiinilla.app.domain.repository.CatalogRepositoryException
import com.vaiinilla.app.domain.repository.ReservationException
import com.vaiinilla.app.domain.repository.ReservationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

data class AdminSpacesUiState(
    val spaces: List<ManagedSpace>? = null,
    val error: String? = null,
    val schedule: ReservationSchedule? = null,
    val reservations: List<Reservation> = emptyList(),
    /** Espacio que se está guardando: su tarjeta muestra el progreso. */
    val busySpaceId: Int? = null,
    val saving: Boolean = false,
    val notice: String? = null,
    val noticeIsError: Boolean = false,
) {
    val hasCourts: Boolean get() = spaces.orEmpty().any { it.isCourt }
}

/** Mesas y espacios: alta, lote, QR, ficha de cancha y reservas del día. */
@HiltViewModel
class AdminSpacesViewModel
    @Inject
    constructor(
        private val repository: AdminSpacesRepository,
        private val reservations: ReservationRepository,
    ) : ViewModel() {
        private val _uiState = mutableStateOf(AdminSpacesUiState())
        val uiState: State<AdminSpacesUiState> = _uiState
        private var noticeJob: Job? = null
        private var scope: String? = null

        fun onVisible(scope: String) {
            if (scope != this.scope) {
                this.scope = scope
                _uiState.value = AdminSpacesUiState()
            }
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { repository.spaces() }
                _uiState.value =
                    result.fold(
                        onSuccess = { _uiState.value.copy(spaces = it, error = null) },
                        onFailure = {
                            _uiState.value.copy(
                                error = it.toUserFacingMessage("No se pudieron cargar los espacios."),
                            )
                        },
                    )
                if (_uiState.value.hasCourts) refreshCourts()
            }
        }

        private fun refreshCourts() {
            viewModelScope.launch {
                withContext(Dispatchers.IO) { repository.reservationSchedule() }.onSuccess {
                    _uiState.value = _uiState.value.copy(schedule = it)
                }
            }
            viewModelScope.launch {
                withContext(Dispatchers.IO) { reservations.list(ReservationAccess.Account) }.onSuccess { list ->
                    _uiState.value =
                        _uiState.value.copy(
                            reservations = list.filter { it.state != ReservationState.CANCELLED }.sortedBy { it.start },
                        )
                }
            }
        }

        fun create(
            name: String,
            type: SpaceType,
            onDone: () -> Unit,
        ) {
            SpaceRules.nameProblem(name)?.let { return showNotice(it, isError = true) }
            mutate("${name.trim()} creado", onDone = onDone) {
                repository.create(name, type, UUID.randomUUID().toString()).map { }
            }
        }

        fun createBatch(
            type: SpaceType,
            prefix: String,
            from: Int?,
            to: Int?,
            onDone: () -> Unit,
        ) {
            SpaceRules.batchProblem(prefix, from, to)?.let { return showNotice(it, isError = true) }
            if (_uiState.value.saving) return
            _uiState.value = _uiState.value.copy(saving = true)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        repository.createBatch(type, prefix, from!!, to!!, UUID.randomUUID().toString())
                    }
                _uiState.value = _uiState.value.copy(saving = false)
                result.fold(
                    onSuccess = { batch ->
                        val skipped = if (batch.skipped.isEmpty()) "" else " (${batch.skipped.size} ya existían)"
                        showNotice("Se crearon ${batch.created} de ${batch.requested}$skipped", isError = false)
                        onDone()
                        refresh()
                    },
                    onFailure = { showNotice(spaceErrorMessage(it), isError = true) },
                )
            }
        }

        fun rename(
            space: ManagedSpace,
            name: String,
        ) {
            SpaceRules.nameProblem(name)?.let { return showNotice(it, isError = true) }
            if (name.trim() == space.name) return
            patch(space, SpacePatch(name = name), "Nombre guardado")
        }

        fun setType(
            space: ManagedSpace,
            type: SpaceType,
        ) {
            if (type.wire !=
                space.typeWire
            ) {
                patch(space, SpacePatch(typeWire = type.wire), "Ahora es ${type.label.lowercase()}")
            }
        }

        fun setActive(
            space: ManagedSpace,
            active: Boolean,
        ) = patch(
            space,
            SpacePatch(active = active),
            if (active) "${space.name} activo" else "${space.name} desactivado",
        )

        fun setHourlyPrice(
            space: ManagedSpace,
            raw: String,
        ) {
            val price =
                when (val parsed = SpaceRules.hourlyPrice(raw)) {
                    SpaceRules.HourlyPrice.Invalid -> return showNotice(
                        "Escribe el precio por hora, por ejemplo 300.",
                        isError = true,
                    )
                    SpaceRules.HourlyPrice.None -> null
                    is SpaceRules.HourlyPrice.Value -> parsed.amount
                }
            patch(
                space,
                SpacePatch(hourlyPrice = price, setsHourlyPrice = true),
                if (price == null) "${space.name} ya no se renta" else "Precio por hora guardado",
            )
        }

        fun saveProfile(
            space: ManagedSpace,
            description: String,
            features: List<String>,
        ) {
            if (description.trim().length >
                500
            ) {
                return showNotice("La descripción admite hasta 500 letras.", isError = true)
            }
            patch(
                space,
                SpacePatch(description = description, features = SpaceRules.features(features), setsProfile = true),
                "Ficha de ${space.name} guardada",
            )
        }

        private fun patch(
            space: ManagedSpace,
            patch: SpacePatch,
            success: String,
        ) = mutate(success, spaceId = space.id) {
            repository.update(space.id, patch, UUID.randomUUID().toString()).map { }
        }

        fun delete(space: ManagedSpace) =
            mutate("${space.name} eliminado", spaceId = space.id) {
                repository.delete(space.id, UUID.randomUUID().toString())
            }

        fun uploadImage(
            space: ManagedSpace,
            bytes: ByteArray,
            filename: String,
            mimeType: String,
        ) = mutate("Foto de ${space.name} guardada", spaceId = space.id) {
            repository.uploadImage(space.id, bytes, filename, mimeType, UUID.randomUUID().toString()).map { }
        }

        fun removeImage(space: ManagedSpace) =
            mutate("Foto de ${space.name} quitada", spaceId = space.id) {
                repository.removeImage(space.id, UUID.randomUUID().toString()).map { }
            }

        fun saveSchedule(schedule: ReservationSchedule) {
            SpaceRules.scheduleProblem(schedule)?.let { return showNotice(it, isError = true) }
            mutate("Horario de reservas guardado") {
                repository.saveReservationSchedule(schedule, UUID.randomUUID().toString()).map { }
            }
        }

        /** Cobra en efectivo una renta apartada; necesita la caja abierta. */
        fun payReservation(
            reservation: Reservation,
            received: String,
            onDone: () -> Unit,
        ) = mutate("Renta cobrada", onDone = onDone) {
            reservations
                .pay(
                    ReservationAccess.Account,
                    reservation.id,
                    ReservationPaymentMethod.CASH,
                    received,
                    UUID.randomUUID().toString(),
                ).map { }
        }

        fun cancelReservation(reservation: Reservation) =
            mutate("Cancha liberada") {
                reservations.cancel(ReservationAccess.Account, reservation.id, UUID.randomUUID().toString()).map { }
            }

        private fun mutate(
            success: String,
            spaceId: Int? = null,
            onDone: () -> Unit = {},
            call: () -> Result<Unit>,
        ) {
            if (_uiState.value.saving) return
            _uiState.value = _uiState.value.copy(saving = true, busySpaceId = spaceId)
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { call() }
                _uiState.value = _uiState.value.copy(saving = false, busySpaceId = null)
                result.fold(
                    onSuccess = {
                        showNotice(success, isError = false)
                        onDone()
                    },
                    onFailure = { showNotice(spaceErrorMessage(it), isError = true) },
                )
                refresh()
            }
        }

        fun imageProblem(message: String) = showNotice(message, isError = true)

        fun notify(
            text: String,
            isError: Boolean,
        ) = showNotice(text, isError)

        fun dismissNotice() {
            noticeJob?.cancel()
            _uiState.value = _uiState.value.copy(notice = null)
        }

        private fun showNotice(
            text: String,
            isError: Boolean,
        ) {
            noticeJob?.cancel()
            _uiState.value = _uiState.value.copy(notice = text, noticeIsError = isError)
            noticeJob =
                viewModelScope.launch {
                    delay(NOTICE_MS)
                    _uiState.value = _uiState.value.copy(notice = null)
                }
        }

        private companion object {
            const val NOTICE_MS = 3_500L
        }
    }

internal fun spaceErrorMessage(error: Throwable): String {
    val code = (error as? CatalogRepositoryException)?.code ?: (error as? ReservationException)?.code
    return when (code) {
        "SPACE_NAME_CONFLICT" -> "Ya hay un espacio con ese nombre."
        "SPACE_HAS_RESERVATIONS" -> "Tiene reservas pendientes: cancélalas antes."
        "SPACE_HAS_OPEN_SESSION" -> "Tiene una cuenta abierta: ciérrala antes."
        "CASH_SESSION_REQUIRED", "CASH_SESSION_NOT_OPEN" -> "Abre la caja antes de cobrar."
        "RESERVATION_IN_PROGRESS" -> "La renta ya empezó: no se puede cancelar."
        "RESERVATION_EXPIRED" -> "El apartado ya venció."
        "RESERVATION_NOT_CANCELLABLE" -> "Esta reserva ya no se puede cancelar."
        else -> error.toUserFacingMessage("No se pudo guardar.")
    }
}
