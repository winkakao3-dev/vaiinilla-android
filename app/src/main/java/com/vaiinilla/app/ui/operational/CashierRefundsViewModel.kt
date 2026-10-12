package com.vaiinilla.app.ui.operational

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.MutationIdempotency
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.model.PendingRefund
import com.vaiinilla.app.domain.model.RefundMethod
import com.vaiinilla.app.domain.repository.RefundRepository
import com.vaiinilla.app.ui.components.moneyLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class CashierRefundsUiState(
    val refunds: List<PendingRefund> = emptyList(),
    /** Devolución que se está confirmando y con qué método; null mientras no hay envío en curso. */
    val confirming: RefundConfirmation? = null,
    val notice: String? = null,
    /** Error al confirmar una devolución; se limpia al intentar otra. */
    val errorMessage: String? = null,
    /** Error al cargar la lista; se limpia cuando la carga vuelve a funcionar. */
    val loadError: String? = null,
)

data class RefundConfirmation(
    val refundId: String,
    val method: RefundMethod,
)

/** Método que se ofrece primero: el mismo por donde entró el dinero; lo mixto o el saldo lo elige Caja. */
internal fun suggestedRefundMethod(originalMethod: String): RefundMethod =
    if (originalMethod == "efectivo") RefundMethod.CASH else RefundMethod.CARD

/** Efectivo puro y Stripe se devuelven solos; el resto lo confirma Caja y puede ser en efectivo. */
internal fun offersCashRefund(originalMethod: String): Boolean = originalMethod != "efectivo"

internal fun refundNotice(amount: String): String = "Devolución de ${moneyLabel(amount)} confirmada."

/**
 * Devoluciones pendientes de Caja. Consulta cada pocos segundos mientras la pantalla está a la vista
 * (igual que el panel web); confirmar pide una acción explícita del cajero y reintenta con la misma
 * clave de idempotencia.
 */
@HiltViewModel
class CashierRefundsViewModel
    @Inject
    constructor(
        private val refundRepository: RefundRepository,
    ) : ViewModel() {
        private val _uiState = mutableStateOf(CashierRefundsUiState())
        val uiState: State<CashierRefundsUiState> = _uiState

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

        fun dismissNotice() {
            _uiState.value = _uiState.value.copy(notice = null)
        }

        /** Caja dice que ya devolvió [refund] con [method]. Una a la vez. */
        fun confirm(
            refund: PendingRefund,
            method: RefundMethod,
        ) {
            if (_uiState.value.confirming != null) return
            _uiState.value =
                _uiState.value.copy(
                    confirming = RefundConfirmation(refund.id, method),
                    notice = null,
                    errorMessage = null,
                )
            viewModelScope.launch {
                val key = MutationIdempotency.refundConfirmation(refund.id, method.wire)
                val result =
                    withContext(Dispatchers.IO) {
                        refundRepository.confirm(refund.id, method, note = null, idempotencyKey = key)
                    }
                result.fold(
                    onSuccess = { done ->
                        _uiState.value = _uiState.value.copy(notice = refundNotice(done.amount))
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(errorMessage = error.toUserFacingMessage())
                    },
                )
                _uiState.value = _uiState.value.copy(confirming = null)
                refresh()
            }
        }

        private suspend fun refresh() {
            val result = withContext(Dispatchers.IO) { refundRepository.pending() }
            result.fold(
                onSuccess = { refunds ->
                    _uiState.value = _uiState.value.copy(refunds = refunds, loadError = null)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(loadError = error.toUserFacingMessage())
                },
            )
        }

        override fun onCleared() {
            pollingJob?.cancel()
            super.onCleared()
        }

        private companion object {
            const val POLL_INTERVAL_MS = 10_000L
        }
    }
