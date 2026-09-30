package com.vaiinilla.app.ui.reservations

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.domain.model.CourtDay
import com.vaiinilla.app.domain.model.CourtSchedule
import com.vaiinilla.app.domain.model.CreatedOrder
import com.vaiinilla.app.domain.model.Reservation
import com.vaiinilla.app.domain.model.ReservationPaymentMethod
import com.vaiinilla.app.domain.model.ReservationSlots
import com.vaiinilla.app.domain.model.ReservationState
import com.vaiinilla.app.domain.repository.ReservationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

data class ReservationsUiState(
    val loading: Boolean = true,
    val day: CourtDay? = null,
    /** "YYYY-MM-DD"; null = hoy. */
    val selectedDate: String? = null,
    val selectedCourtId: Int? = null,
    /** Inicio elegido; con [rentNow] la renta empieza en cuanto se pague. */
    val selectedStart: Instant? = null,
    val rentNow: Boolean = false,
    val selectedDuration: Int? = null,
    val mine: List<Reservation> = emptyList(),
    /** Reserva apartada esperando que el cliente elija cómo pagar. */
    val pending: Reservation? = null,
    val working: Boolean = false,
    val error: String? = null,
    /** Aviso de éxito ("Cancha apartada", "Reserva cancelada"). */
    val notice: String? = null,
    /** Pedido de renta listo para mostrarse en la confirmación de pedido (tarjeta, efectivo, saldo). */
    val paidOrder: CreatedOrder? = null,
) {
    val selectedCourt: CourtSchedule?
        get() = day?.courts?.firstOrNull { it.id == selectedCourtId }

    val canReserve: Boolean
        get() = selectedCourt != null && selectedDuration != null && (rentNow || selectedStart != null) && !working
}

/**
 * Canchas del cliente: horario del día, huecos libres, apartar y pagar. El pago crea un pedido de
 * renta normal, que la confirmación de pedido cobra y sigue igual que cualquier compra.
 */
@HiltViewModel
class ReservationsViewModel
    @Inject
    constructor(
        private val repository: ReservationRepository,
    ) : ViewModel() {
        private val _uiState = mutableStateOf(ReservationsUiState())
        val uiState: State<ReservationsUiState> = _uiState

        private var pollingJob: Job? = null

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

        fun selectDate(date: String) {
            val today = _uiState.value.day?.today
            _uiState.value =
                _uiState.value.copy(
                    selectedDate = date.takeIf { it != today },
                    selectedStart = null,
                    rentNow = false,
                    selectedDuration = null,
                    loading = true,
                )
            viewModelScope.launch { refresh() }
        }

        fun selectCourt(courtId: Int) {
            _uiState.value =
                _uiState.value.copy(
                    selectedCourtId = courtId,
                    selectedStart = null,
                    rentNow = false,
                    selectedDuration = null,
                )
        }

        fun selectRentNow() {
            val state = _uiState.value
            val day = state.day ?: return
            val court = state.selectedCourt ?: return
            val durations = ReservationSlots.availableDurations(day, court, day.now)
            _uiState.value =
                state.copy(
                    rentNow = true,
                    selectedStart = null,
                    selectedDuration = durations.firstOrNull(),
                )
        }

        fun selectStart(start: Instant) {
            val state = _uiState.value
            val day = state.day ?: return
            val court = state.selectedCourt ?: return
            val durations = ReservationSlots.availableDurations(day, court, start)
            _uiState.value =
                state.copy(
                    rentNow = false,
                    selectedStart = start,
                    selectedDuration = state.selectedDuration?.takeIf { it in durations } ?: durations.firstOrNull(),
                )
        }

        fun selectDuration(minutes: Int) {
            _uiState.value = _uiState.value.copy(selectedDuration = minutes)
        }

        fun dismissMessages() {
            _uiState.value = _uiState.value.copy(error = null, notice = null)
        }

        /** Aparta la cancha 10 minutos y abre la elección del pago. */
        fun reserve() {
            val state = _uiState.value
            if (!state.canReserve) return
            val court = state.selectedCourt ?: return
            val minutes = state.selectedDuration ?: return
            _uiState.value = state.copy(working = true, error = null, notice = null)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        repository.create(
                            courtId = court.id,
                            start = if (state.rentNow) null else state.selectedStart,
                            durationMinutes = minutes,
                            customerName = null,
                            idempotencyKey = UUID.randomUUID().toString(),
                        )
                    }
                result.fold(
                    onSuccess = { reservation ->
                        _uiState.value =
                            _uiState.value.copy(
                                working = false,
                                pending = reservation,
                                selectedStart = null,
                                rentNow = false,
                            )
                        refresh()
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(working = false, error = error.userMessage())
                        refresh()
                    },
                )
            }
        }

        /** Retoma el pago de una reserva que sigue apartada. */
        fun resumePayment(reservation: Reservation) {
            if (reservation.state != ReservationState.PENDING_PAYMENT) return
            _uiState.value = _uiState.value.copy(pending = reservation, error = null)
        }

        fun dismissPayment() {
            _uiState.value = _uiState.value.copy(pending = null)
        }

        fun pay(method: ReservationPaymentMethod) {
            val reservation = _uiState.value.pending ?: return
            _uiState.value = _uiState.value.copy(working = true, error = null)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        repository.pay(
                            reservationId = reservation.id,
                            method = method,
                            cashReceived = null,
                            idempotencyKey = UUID.randomUUID().toString(),
                        )
                    }
                result.fold(
                    onSuccess = { payment ->
                        _uiState.value =
                            _uiState.value.copy(
                                working = false,
                                pending = null,
                                paidOrder = payment.order,
                                notice = paidNotice(method),
                            )
                        refresh()
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(working = false, error = error.userMessage())
                        refresh()
                    },
                )
            }
        }

        /** La confirmación ya tomó el pedido de renta. */
        fun consumePaidOrder() {
            _uiState.value = _uiState.value.copy(paidOrder = null)
        }

        fun cancel(reservation: Reservation) {
            _uiState.value = _uiState.value.copy(working = true, error = null)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        repository.cancel(reservation.id, UUID.randomUUID().toString())
                    }
                result.fold(
                    onSuccess = {
                        _uiState.value =
                            _uiState.value.copy(
                                working = false,
                                pending = null,
                                notice = "Reserva cancelada.",
                            )
                        refresh()
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(working = false, error = error.userMessage())
                    },
                )
            }
        }

        private suspend fun refresh() {
            val state = _uiState.value
            val (dayResult, mineResult) =
                withContext(Dispatchers.IO) {
                    repository.day(state.selectedDate) to repository.list()
                }
            val day = dayResult.getOrNull()
            val current = _uiState.value
            val courtId =
                current.selectedCourtId?.takeIf { id -> day?.courts?.any { it.id == id } == true }
                    ?: day?.courts?.firstOrNull { it.rentable }?.id
                    ?: day?.courts?.firstOrNull()?.id
            _uiState.value =
                current.copy(
                    loading = false,
                    day = day ?: current.day,
                    selectedCourtId = courtId,
                    mine = mineResult.getOrNull() ?: current.mine,
                    // Un inicio que alguien más tomó mientras tanto deja de estar elegido.
                    selectedStart =
                        current.selectedStart?.takeIf { start ->
                            val court = day?.courts?.firstOrNull { it.id == courtId }
                            day == null || court == null || ReservationSlots.isStartAvailable(day, court, start)
                        },
                    error =
                        current.error
                            ?: dayResult.exceptionOrNull()?.let {
                                "No pudimos cargar las canchas. ${it.userMessage()}"
                            },
                )
        }

        override fun onCleared() {
            pollingJob?.cancel()
            super.onCleared()
        }

        private fun paidNotice(method: ReservationPaymentMethod): String =
            when (method) {
                ReservationPaymentMethod.BALANCE -> "¡Listo! Tu cancha quedó pagada."
                ReservationPaymentMethod.CARD -> "Completa el pago con tarjeta para confirmar tu cancha."
                ReservationPaymentMethod.CASH -> "Paga en caja antes de que venza el apartado."
            }

        private companion object {
            const val POLL_INTERVAL_MS = 15_000L
        }
    }

internal fun Throwable.userMessage(): String =
    message?.takeIf { it.isNotBlank() } ?: "Algo salió mal. Intenta de nuevo."
