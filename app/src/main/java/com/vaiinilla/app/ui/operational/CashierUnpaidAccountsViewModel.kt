package com.vaiinilla.app.ui.operational

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.repository.UnpaidAccounts
import com.vaiinilla.app.domain.repository.WaiterRepository
import com.vaiinilla.app.ui.components.moneyLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class CashierUnpaidUiState(
    val accounts: UnpaidAccounts? = null,
    val loadError: String? = null,
)

/** "hace menos de 1 h" o "hace N h": el backend da horas enteras. */
internal fun unpaidAgeLabel(hours: Int): String = if (hours < 1) "hace menos de 1 h" else "hace $hours h"

/** Resumen bajo el título: total, número de cuentas y, si hay, quién se fue sin pagar. */
internal fun unpaidSummary(
    total: String,
    accountCount: Int,
    abandonedCount: Int,
): String {
    val accounts = if (accountCount == 1) "cuenta" else "cuentas"
    val base = "${moneyLabel(total)} en $accountCount $accounts"
    if (abandonedCount == 0) return base
    val left = if (abandonedCount == 1) "se fue sin pagar" else "se fueron sin pagar"
    return "$base · $abandonedCount $left"
}

/**
 * Cuentas abiertas que nadie ha cobrado y quién se fue sin pagar. Consulta cada 30 s mientras Caja
 * está a la vista, como el Resumen del panel web.
 */
@HiltViewModel
class CashierUnpaidAccountsViewModel
    @Inject
    constructor(
        private val waiterRepository: WaiterRepository,
    ) : ViewModel() {
        private val _uiState = mutableStateOf(CashierUnpaidUiState())
        val uiState: State<CashierUnpaidUiState> = _uiState

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

        private suspend fun refresh() {
            val result = withContext(Dispatchers.IO) { waiterRepository.unpaidAccounts() }
            result.fold(
                onSuccess = { accounts -> _uiState.value = CashierUnpaidUiState(accounts = accounts) },
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
            const val POLL_INTERVAL_MS = 30_000L
        }
    }
