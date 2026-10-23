package com.vaiinilla.app.ui.operational

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.repository.PendingRefund
import com.vaiinilla.app.domain.repository.PendingRefundRepository
import com.vaiinilla.app.domain.repository.RefundMethod
import com.vaiinilla.app.ui.components.moneyLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

data class PendingRefundsUiState(
    val refunds: List<PendingRefund> = emptyList(),
    /** La devolución y el método que se están confirmando. */
    val confirming: Pair<String, RefundMethod>? = null,
    val notice: String? = null,
    val noticeIsError: Boolean = false,
)

/** Devoluciones pendientes de Caja (G08, H03): se consultan mientras Caja está a la vista, como en la web. */
@HiltViewModel
class PendingRefundsViewModel
    internal constructor(
        private val repository: PendingRefundRepository,
        private val io: CoroutineDispatcher,
        scopeOverride: CoroutineScope?,
    ) : ViewModel() {
        @Inject
        constructor(repository: PendingRefundRepository) : this(repository, Dispatchers.IO, null)

        private val work: CoroutineScope = scopeOverride ?: viewModelScope
        private val _uiState = mutableStateOf(PendingRefundsUiState())
        val uiState: State<PendingRefundsUiState> = _uiState
        private var pollJob: Job? = null

        // Reintentar la misma confirmación no la duplica: una llave por devolución y método.
        private val keys = mutableMapOf<Pair<String, RefundMethod>, String>()

        fun onVisible() {
            if (pollJob?.isActive == true) return
            pollJob =
                work.launch {
                    while (isActive) {
                        refresh()
                        delay(POLL_INTERVAL_MS)
                    }
                }
        }

        fun onHidden() {
            pollJob?.cancel()
            pollJob = null
        }

        suspend fun refresh() {
            // Si la consulta falla, se quedan las que ya se veían: no se esconde dinero por devolver.
            withContext(io) { repository.pending() }
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(refunds = list.filter { it.state == PENDING })
                }
        }

        fun confirm(
            refund: PendingRefund,
            method: RefundMethod,
        ) {
            if (_uiState.value.confirming != null) return
            val target = refund.id to method
            val key = keys.getOrPut(target) { UUID.randomUUID().toString() }
            _uiState.value = _uiState.value.copy(confirming = target, notice = null)
            work.launch {
                val result = withContext(io) { repository.confirm(refund.id, method, null, key) }
                result
                    .onSuccess { done ->
                        keys.remove(target)
                        _uiState.value =
                            _uiState.value.copy(
                                confirming = null,
                                refunds = _uiState.value.refunds.filterNot { it.id == refund.id },
                                notice = "Devolución de ${moneyLabel(done.amount)} confirmada.",
                                noticeIsError = false,
                            )
                    }.onFailure { error ->
                        // Ya confirmada desde otro lado: sale de la lista igual.
                        val alreadyDone = (error as? ApiClientException)?.code == ALREADY_DONE_CODE
                        _uiState.value =
                            _uiState.value.copy(
                                confirming = null,
                                refunds =
                                    if (alreadyDone) {
                                        _uiState.value.refunds.filterNot { it.id == refund.id }
                                    } else {
                                        _uiState.value.refunds
                                    },
                                notice = error.toUserFacingMessage("No se pudo confirmar la devolución."),
                                noticeIsError = true,
                            )
                        refresh()
                    }
            }
        }

        fun dismissNotice() {
            _uiState.value = _uiState.value.copy(notice = null)
        }

        companion object {
            const val POLL_INTERVAL_MS = 10_000L
            private const val PENDING = "pendiente"
            private const val ALREADY_DONE_CODE = "REFUND_ALREADY_DONE"
        }
    }
