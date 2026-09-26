package com.vaiinilla.app.ui.operational

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.repository.BoardOrder
import com.vaiinilla.app.domain.repository.BoardTable
import com.vaiinilla.app.domain.repository.CallStatus
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
)

@HiltViewModel
class WaiterViewModel
    @Inject
    constructor(
        private val waiterRepository: WaiterRepository,
    ) : ViewModel() {
        private val _uiState = mutableStateOf(WaiterUiState())
        val uiState: State<WaiterUiState> = _uiState

        private val _newCallEvents = MutableSharedFlow<TableCall>(extraBufferCapacity = 8)
        val newCallEvents: SharedFlow<TableCall> = _newCallEvents

        private var pollingJob: Job? = null
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
            qrToken: String,
        ) {
            if (_uiState.value.acting) return
            val token = qrToken.trim()
            if (token.isEmpty()) {
                showToast("Pega el código del QR para confirmar la entrega.")
                return
            }
            _uiState.value = _uiState.value.copy(acting = true)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        waiterRepository.deliver(order, token, UUID.randomUUID().toString())
                    }
                _uiState.value = _uiState.value.copy(acting = false)
                result.fold(
                    onSuccess = {
                        showToast("#${order.folio} entregado")
                        refresh()
                    },
                    onFailure = { error ->
                        showToast(error.toUserFacingMessage())
                    },
                )
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
                                        errorMessage = null,
                                    )
                                detectNewCalls(board.tables)
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
