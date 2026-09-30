package com.vaiinilla.app.ui.operational

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.model.Reservation
import com.vaiinilla.app.domain.model.ReservationPaymentMethod
import com.vaiinilla.app.domain.repository.AccountCollection
import com.vaiinilla.app.domain.repository.BoardOrder
import com.vaiinilla.app.domain.repository.BoardTable
import com.vaiinilla.app.domain.repository.CallStatus
import com.vaiinilla.app.domain.repository.ReservationRepository
import com.vaiinilla.app.domain.repository.SpaceSessionDetail
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.domain.repository.WaiterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

data class WaiterUiState(
    val tables: List<BoardTable> = emptyList(),
    val callsEnabled: Boolean = true,
    val loading: Boolean = false,
    val acting: Boolean = false,
    val errorMessage: String? = null,
    val toastMessage: String? = null,
    val filterAttending: Boolean = false,
    /** Si es false, el mesero entrega en el espacio sin escanear el QR del cliente. */
    val deliveryRequiresQr: Boolean = true,
    /** Sesión, cuenta y estado del espacio abierto en la hoja; null si no hay ninguno. */
    val spaceDetail: SpaceSessionDetail? = null,
    val spaceDetailLoading: Boolean = false,
    /** Cambio de la última cuenta cobrada, para que se lo entregue al cliente. */
    val lastCollection: AccountCollection? = null,
    /** Renta de mostrador apartada esperando que se cobre en efectivo. */
    val pendingRental: Reservation? = null,
)

@HiltViewModel
class WaiterViewModel
    @Inject
    constructor(
        private val waiterRepository: WaiterRepository,
        private val reservationRepository: ReservationRepository,
    ) : ViewModel() {
        private val _uiState = mutableStateOf(WaiterUiState())
        val uiState: State<WaiterUiState> = _uiState

        private val _newCallEvents = MutableSharedFlow<TableCall>(extraBufferCapacity = 8)
        val newCallEvents: SharedFlow<TableCall> = _newCallEvents

        private var pollingJob: Job? = null
        private var openSpaceId: Int? = null
        private var knownCallIds: Set<String>? = null
        private var visible = false
        private var toastClearJob: Job? = null

        fun onVisible() {
            visible = true
            refresh()
            startPolling()
        }

        fun onHidden() {
            visible = false
            pollingJob?.cancel()
            pollingJob = null
        }

        fun setFilterAttending(attending: Boolean) {
            _uiState.value = _uiState.value.copy(filterAttending = attending)
        }

        fun refresh() {
            _uiState.value = _uiState.value.copy(loading = true, errorMessage = null)
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { waiterRepository.board() }
                result.fold(
                    onSuccess = { board ->
                        _uiState.value =
                            _uiState.value.copy(
                                tables = board.tables,
                                callsEnabled = board.callsEnabled,
                                deliveryRequiresQr = board.deliveryRequiresQr,
                                loading = false,
                                errorMessage = null,
                            )
                        detectNewCalls(board.tables)
                    },
                    onFailure = { error ->
                        _uiState.value =
                            _uiState.value.copy(
                                loading = false,
                                errorMessage = error.toUserFacingMessage(),
                            )
                    },
                )
            }
        }

        private fun detectNewCalls(tables: List<BoardTable>) {
            val pending = tables.mapNotNull { it.call }.filter { it.status == CallStatus.PENDIENTE }
            val known = knownCallIds
            if (known != null) {
                val fresh = pending.filter { it.id !in known }
                if (fresh.isNotEmpty()) {
                    val first = fresh.first()
                    _newCallEvents.tryEmit(first)
                    showToast("${first.space.name} te llama · ${first.reason.staffLabel}")
                }
            }
            knownCallIds = pending.map { it.id }.toSet()
        }

        fun markGoing(call: TableCall) {
            if (_uiState.value.acting) return
            _uiState.value = _uiState.value.copy(acting = true)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        waiterRepository.transitionCall(call, CallStatus.EN_CAMINO, UUID.randomUUID().toString())
                    }
                _uiState.value = _uiState.value.copy(acting = false)
                result.fold(
                    onSuccess = {
                        showToast("Vas a ${call.space.name}")
                        refresh()
                    },
                    onFailure = { error ->
                        showToast(error.toUserFacingMessage())
                        refresh()
                    },
                )
            }
        }

        fun markAttended(call: TableCall) {
            if (_uiState.value.acting) return
            _uiState.value = _uiState.value.copy(acting = true)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        waiterRepository.transitionCall(call, CallStatus.ATENDIDA, UUID.randomUUID().toString())
                    }
                _uiState.value = _uiState.value.copy(acting = false)
                result.fold(
                    onSuccess = {
                        showToast("${call.space.name} atendida")
                        refresh()
                    },
                    onFailure = { error ->
                        showToast(error.toUserFacingMessage())
                        refresh()
                    },
                )
            }
        }

        fun deliver(
            order: BoardOrder,
            qrToken: String?,
        ) {
            if (_uiState.value.acting) return
            val token = qrToken?.trim().orEmpty()
            val requiresQr = _uiState.value.deliveryRequiresQr
            if (requiresQr && token.isEmpty()) {
                showToast("Pega el código del QR para confirmar la entrega.")
                return
            }
            _uiState.value = _uiState.value.copy(acting = true)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        waiterRepository.deliver(
                            order,
                            token.takeIf { requiresQr && it.isNotEmpty() },
                            UUID.randomUUID().toString(),
                        )
                    }
                _uiState.value = _uiState.value.copy(acting = false)
                result.fold(
                    onSuccess = {
                        showToast("#${order.folio} entregado")
                        refresh()
                        openSpaceId?.let { loadSpaceDetail(it, silent = true) }
                    },
                    onFailure = { error ->
                        showToast(error.toUserFacingMessage())
                    },
                )
            }
        }

        /** Abre el detalle de un espacio: su sesión, su cuenta y su turno. */
        fun openSpace(spaceId: Int) {
            openSpaceId = spaceId
            _uiState.value = _uiState.value.copy(spaceDetail = null, lastCollection = null)
            loadSpaceDetail(spaceId, silent = false)
        }

        fun closeSpace() {
            openSpaceId = null
            _uiState.value = _uiState.value.copy(spaceDetail = null, spaceDetailLoading = false)
        }

        fun dismissCollection() {
            _uiState.value = _uiState.value.copy(lastCollection = null)
        }

        private fun loadSpaceDetail(
            spaceId: Int,
            silent: Boolean,
        ) {
            if (!silent) _uiState.value = _uiState.value.copy(spaceDetailLoading = true)
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { waiterRepository.spaceSession(spaceId) }
                if (openSpaceId != spaceId) return@launch
                result.fold(
                    onSuccess = { detail ->
                        _uiState.value = _uiState.value.copy(spaceDetail = detail, spaceDetailLoading = false)
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(spaceDetailLoading = false)
                        if (!silent) showToast(error.toUserFacingMessage())
                    },
                )
            }
        }

        /** Abre el turno de una cancha (con duración) o la cuenta de una mesa (sin duración). */
        fun openTurn(
            spaceId: Int,
            durationMinutes: Int?,
        ) = runSpaceAction(spaceId, "Turno abierto") {
            waiterRepository.openSession(spaceId, durationMinutes, UUID.randomUUID().toString())
        }

        fun extendTurn(
            spaceId: Int,
            minutes: Int,
        ) {
            val version =
                _uiState.value.spaceDetail
                    ?.session
                    ?.version
            runSpaceAction(spaceId, "+$minutes min") {
                waiterRepository.extendSession(spaceId, minutes, version, UUID.randomUUID().toString())
            }
        }

        /**
         * Renta de mostrador de una cancha con precio: se aparta ([start] null = ahora; o el fin del
         * turno para renovar) y se abre el cobro en efectivo. La renta se paga primero.
         */
        fun startRental(
            spaceId: Int,
            minutes: Int,
            start: Instant?,
        ) {
            if (_uiState.value.acting) return
            _uiState.value = _uiState.value.copy(acting = true)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        reservationRepository.create(
                            courtId = spaceId,
                            start = start,
                            durationMinutes = minutes,
                            customerName = null,
                            idempotencyKey = UUID.randomUUID().toString(),
                        )
                    }
                _uiState.value = _uiState.value.copy(acting = false)
                result.fold(
                    onSuccess = { reservation -> _uiState.value = _uiState.value.copy(pendingRental = reservation) },
                    onFailure = { error -> showToast(error.toUserFacingMessage()) },
                )
            }
        }

        /** Cobra la renta apartada: con el cobro se ocupa la cancha (o se alarga su turno). */
        fun confirmRental(received: String) {
            val reservation = _uiState.value.pendingRental ?: return
            if (_uiState.value.acting) return
            _uiState.value = _uiState.value.copy(acting = true)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        reservationRepository.pay(
                            reservationId = reservation.id,
                            method = ReservationPaymentMethod.CASH,
                            cashReceived = received,
                            idempotencyKey = UUID.randomUUID().toString(),
                        )
                    }
                _uiState.value = _uiState.value.copy(acting = false)
                result.fold(
                    onSuccess = { payment ->
                        _uiState.value = _uiState.value.copy(pendingRental = null)
                        val change =
                            payment.cashChange
                                ?.takeIf { it != "0.00" }
                                ?.let { " · cambio $$it" }
                                .orEmpty()
                        showToast("Cancha rentada$change")
                    },
                    onFailure = { error -> showToast(error.toUserFacingMessage()) },
                )
                refresh()
                loadSpaceDetail(reservation.courtId, silent = true)
            }
        }

        /** Se cerró el cobro sin cobrar: se libera el horario apartado. */
        fun cancelPendingRental() {
            val reservation = _uiState.value.pendingRental ?: return
            _uiState.value = _uiState.value.copy(pendingRental = null)
            viewModelScope.launch(Dispatchers.IO) {
                reservationRepository.cancel(reservation.id, UUID.randomUUID().toString())
            }
        }

        /** Libera el espacio. El servidor no lo permite con pedidos sin cobrar. */
        fun releaseSpace(spaceId: Int) {
            val version =
                _uiState.value.spaceDetail
                    ?.session
                    ?.version
            runSpaceAction(spaceId, "Espacio liberado") {
                waiterRepository.releaseSpace(spaceId, version, UUID.randomUUID().toString())
            }
        }

        /**
         * Cobra en efectivo la cuenta del espacio: toda si [orderIds] es null, o solo esos pedidos
         * (dividir la cuenta). `expectedTotal` evita cobrar mal si cambió.
         */
        fun collectAccount(
            spaceId: Int,
            received: String,
            expectedTotal: String,
            orderIds: List<String>? = null,
        ) {
            if (_uiState.value.acting) return
            _uiState.value = _uiState.value.copy(acting = true)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        waiterRepository.collectAccount(
                            spaceId,
                            received,
                            expectedTotal,
                            orderIds,
                            UUID.randomUUID().toString(),
                        )
                    }
                _uiState.value = _uiState.value.copy(acting = false)
                result.fold(
                    onSuccess = { collection ->
                        _uiState.value = _uiState.value.copy(lastCollection = collection)
                        showToast(if (collection.settled) "Cuenta cobrada" else "Cobro registrado")
                    },
                    onFailure = { error -> showToast(error.toUserFacingMessage()) },
                )
                refresh()
                loadSpaceDetail(spaceId, silent = true)
            }
        }

        private fun runSpaceAction(
            spaceId: Int,
            successMessage: String,
            call: () -> Result<Unit>,
        ) {
            if (_uiState.value.acting) return
            _uiState.value = _uiState.value.copy(acting = true)
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { call() }
                _uiState.value = _uiState.value.copy(acting = false)
                result.fold(
                    onSuccess = { showToast(successMessage) },
                    onFailure = { error -> showToast(error.toUserFacingMessage()) },
                )
                refresh()
                loadSpaceDetail(spaceId, silent = true)
            }
        }

        private fun showToast(message: String) {
            toastClearJob?.cancel()
            _uiState.value = _uiState.value.copy(toastMessage = message)
            toastClearJob =
                viewModelScope.launch {
                    delay(TOAST_VISIBLE_MS)
                    _uiState.value = _uiState.value.copy(toastMessage = null)
                }
        }

        private fun startPolling() {
            pollingJob?.cancel()
            pollingJob =
                viewModelScope.launch {
                    while (isActive && visible) {
                        delay(POLL_INTERVAL_MS)
                        if (!visible) break
                        val result = withContext(Dispatchers.IO) { waiterRepository.board() }
                        result.fold(
                            onSuccess = { board ->
                                _uiState.value =
                                    _uiState.value.copy(
                                        tables = board.tables,
                                        callsEnabled = board.callsEnabled,
                                        deliveryRequiresQr = board.deliveryRequiresQr,
                                        errorMessage = null,
                                    )
                                detectNewCalls(board.tables)
                                openSpaceId?.let { loadSpaceDetail(it, silent = true) }
                            },
                            onFailure = { error ->
                                _uiState.value =
                                    _uiState.value.copy(
                                        errorMessage = error.toUserFacingMessage(),
                                    )
                            },
                        )
                    }
                }
        }

        override fun onCleared() {
            pollingJob?.cancel()
            super.onCleared()
        }

        private companion object {
            const val POLL_INTERVAL_MS = 5_000L
            const val TOAST_VISIBLE_MS = 4_200L
        }
    }
