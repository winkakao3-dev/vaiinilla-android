package com.vaiinilla.app.ui.reservations

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.data.guest.GuestIdentityRequiredException
import com.vaiinilla.app.data.guest.GuestReservationSession
import com.vaiinilla.app.domain.model.CourtDay
import com.vaiinilla.app.domain.model.CourtSchedule
import com.vaiinilla.app.domain.model.CreatedOrder
import com.vaiinilla.app.domain.model.Reservation
import com.vaiinilla.app.domain.model.ReservationAccess
import com.vaiinilla.app.domain.model.ReservationPaymentMethod
import com.vaiinilla.app.domain.model.ReservationSlots
import com.vaiinilla.app.domain.model.ReservationState
import com.vaiinilla.app.domain.repository.ReservationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
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
    /** Sin cuenta: se renta como invitado (sin saldo ni tarjeta). */
    val guestMode: Boolean = false,
    /** Sin cuenta y sin identidad de invitado: hay que dar el nombre y entrar antes de ver horarios. */
    val guestEntry: Boolean = false,
    val guestName: String = "",
    /** El lugar pide cuenta para rentar: al invitado no se le ofrece la renta sin cuenta. */
    val accountRequired: Boolean = false,
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
        private val guestSession: GuestReservationSession,
    ) : ViewModel() {
        private val _uiState = mutableStateOf(ReservationsUiState())
        val uiState: State<ReservationsUiState> = _uiState

        // El pedido de una renta pagada lo toma AppNavHost aunque la persona ya haya salido de Reservas con el
        // pago en vuelo; como estado de la ruta se quedaba esperando y saltaba solo días después (FLOW-16).
        private val paidOrders = Channel<CreatedOrder>(Channel.BUFFERED)
        val paidOrderEvents: Flow<CreatedOrder> = paidOrders.receiveAsFlow()

        private var pollingJob: Job? = null

        /** Cuenta con sesión, o invitado en [guestSlug] si no la hay. Null = no hay negocio que rentar. */
        private var reservationAccess: ReservationAccess? = null

        /** [hasAccount]: hay sesión de cliente. Si no, se renta como invitado en [guestSlug]. */
        fun onVisible(
            guestSlug: String?,
            hasAccount: Boolean,
        ) {
            pollingJob?.cancel()
            pollingJob = null
            val next: ReservationAccess? =
                when {
                    hasAccount -> ReservationAccess.Account
                    guestSlug != null -> ReservationAccess.Guest(guestSlug)
                    else -> null
                }
            if (next != reservationAccess) {
                // Otra identidad: lo que se mostró de la anterior (sus reservas, su pago) ya no aplica.
                reservationAccess = next
                _uiState.value =
                    _uiState.value.copy(
                        mine = emptyList(),
                        pending = null,
                        selectedStart = null,
                        rentNow = false,
                        selectedDuration = null,
                    )
            }
            val guest = next as? ReservationAccess.Guest
            val needsEntry = guest != null && !guestSession.hasIdentity()
            _uiState.value =
                _uiState.value.copy(
                    guestMode = guest != null,
                    guestEntry = needsEntry,
                    accountRequired = false,
                    loading = next != null && !needsEntry,
                    error = if (next == null) "Elige un lugar para ver sus canchas." else _uiState.value.error,
                )
            if (next != null && !needsEntry) startPolling()
        }

        /** Un fallo antes de consultar (preparar la cuenta en este lugar) se ve en el mismo aviso. */
        fun showError(message: String) {
            _uiState.value = _uiState.value.copy(loading = false, error = message)
        }

        fun onHidden() {
            pollingJob?.cancel()
            pollingJob = null
        }

        fun onGuestNameChange(name: String) {
            _uiState.value = _uiState.value.copy(guestName = name)
        }

        /** Alta del invitado con el nombre que escribió y los términos mostrados en pantalla. */
        fun enterAsGuest() {
            val state = _uiState.value
            val guest = reservationAccess as? ReservationAccess.Guest ?: return
            val name = state.guestName.trim().replace(WHITESPACE, " ")
            if (name.isEmpty()) {
                _uiState.value = state.copy(error = "Escribe tu nombre para entrar.")
                return
            }
            _uiState.value = state.copy(working = true, error = null)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        guestSession.enter(guest.establishmentSlug, name)
                    }
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(working = false, guestEntry = false, loading = true)
                        startPolling()
                    },
                    onFailure = { error ->
                        val message =
                            if ((error as? ApiClientException)?.code == GUEST_NOT_ALLOWED) {
                                null
                            } else {
                                error.userMessage()
                            }
                        _uiState.value =
                            _uiState.value.copy(
                                working = false,
                                error = message,
                                accountRequired = message == null || _uiState.value.accountRequired,
                            )
                    },
                )
            }
        }

        private fun startPolling() {
            pollingJob?.cancel()
            pollingJob =
                viewModelScope.launch {
                    while (isActive && !_uiState.value.guestEntry) {
                        refresh()
                        delay(POLL_INTERVAL_MS)
                    }
                }
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
            val access = reservationAccess ?: return
            val court = state.selectedCourt ?: return
            val minutes = state.selectedDuration ?: return
            _uiState.value = state.copy(working = true, error = null, notice = null)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        repository.create(
                            access = access,
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
            if (_uiState.value.working) return
            val reservation = _uiState.value.pending ?: return
            val access = reservationAccess ?: return
            _uiState.value = _uiState.value.copy(working = true, error = null)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        repository.pay(
                            access = access,
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
                                notice = paidNotice(method),
                            )
                        payment.order?.let { paidOrders.trySend(it) }
                        refresh()
                    },
                    onFailure = { error ->
                        val message =
                            if ((error as? ApiClientException)?.code == "INSUFFICIENT_WALLET_BALANCE") {
                                "No te alcanza el saldo para esta renta. Recarga o paga en caja."
                            } else {
                                error.userMessage()
                            }
                        _uiState.value = _uiState.value.copy(working = false, error = message)
                        refresh()
                    },
                )
            }
        }

        fun cancel(reservation: Reservation) {
            if (_uiState.value.working) return
            val access = reservationAccess ?: return
            _uiState.value = _uiState.value.copy(working = true, error = null)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        repository.cancel(access, reservation.id, UUID.randomUUID().toString())
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
            val access = reservationAccess ?: return
            val requestedDate = state.selectedDate
            val (dayResult, mineResult) =
                withContext(Dispatchers.IO) {
                    repository.day(state.selectedDate, access) to repository.list(access)
                }
            // El invitado se vence o se olvida: vuelve a la entrada en vez de mostrar un error cada 15 s.
            if (dayResult.exceptionOrNull() is GuestIdentityRequiredException ||
                mineResult.exceptionOrNull() is GuestIdentityRequiredException
            ) {
                _uiState.value = _uiState.value.copy(loading = false, guestEntry = true, error = null)
                return
            }
            val current = _uiState.value
            // Un sondeo que salió con la fecha anterior y termina después de cambiar de día trae el día viejo:
            // se descarta para no pintarlo bajo la fecha nueva (FLOW-16).
            val dateStillSelected = current.selectedDate == requestedDate
            val day = dayResult.getOrNull()?.takeIf { dateStillSelected }
            val courtId =
                current.selectedCourtId?.takeIf { id -> day?.courts?.any { it.id == id } == true }
                    ?: day?.courts?.firstOrNull { it.rentable }?.id
                    ?: day?.courts?.firstOrNull()?.id
            _uiState.value =
                current.copy(
                    loading = if (dateStillSelected) false else current.loading,
                    day = day ?: current.day,
                    accountRequired = current.guestMode && (day ?: current.day)?.requiresAccount == true,
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
                            ?: dayResult.exceptionOrNull()?.takeIf { dateStillSelected }?.let {
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
            const val GUEST_NOT_ALLOWED = "GUEST_NOT_ALLOWED"
            val WHITESPACE = Regex("\\s+")
        }
    }

internal fun Throwable.userMessage(): String = toUserFacingMessage("Algo salió mal. Intenta de nuevo.")
