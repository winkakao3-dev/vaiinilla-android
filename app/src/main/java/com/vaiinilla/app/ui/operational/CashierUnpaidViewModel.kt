package com.vaiinilla.app.ui.operational

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.admin.AdminUnpaidAccount
import com.vaiinilla.app.domain.admin.AdminUnpaidAccounts
import com.vaiinilla.app.domain.repository.AccountCollection
import com.vaiinilla.app.domain.repository.AccountPaymentMethod
import com.vaiinilla.app.domain.repository.AdminRepository
import com.vaiinilla.app.domain.repository.WaiterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

/** Lo cobrado de una cuenta, para enseñar el cambio. */
data class CashierUnpaidCollected(
    val spaceName: String,
    val collection: AccountCollection,
)

data class CashierUnpaidUiState(
    val accounts: AdminUnpaidAccounts? = null,
    val loading: Boolean = false,
    val error: String? = null,
    /** [unpaidAccountKey] de la cuenta que se está cobrando. */
    val charging: String? = null,
    val chargeError: String? = null,
    val lastCollected: CashierUnpaidCollected? = null,
)

/** Un espacio puede traer su turno abierto y, aparte, lo que dejaron turnos cerrados: la llave los separa. */
fun unpaidAccountKey(account: AdminUnpaidAccount): String = "${account.spaceId}:${account.sessionId ?: "sin-turno"}"

/**
 * R1 (G09): «Cuentas por cobrar» de Caja. Lista todas las cuentas a pagar al final que siguen sin cobrar, también las
 * de un turno que ya se cerró (nadie más las puede cobrar), y las cobra en efectivo o terminal con
 * `POST espacios/:id/sesion/cobros` sin depender de una sesión viva: manda los pedidos de esa cuenta y
 * `total_esperado` = su restante (contrato E/F). Es la misma lista que bloquea el cierre de caja.
 */
@HiltViewModel
class CashierUnpaidViewModel
    @Inject
    constructor(
        private val adminRepository: AdminRepository,
        private val waiterRepository: WaiterRepository,
    ) : ViewModel() {
        private val _uiState = mutableStateOf(CashierUnpaidUiState())
        val uiState: State<CashierUnpaidUiState> = _uiState

        private var loadJob: Job? = null

        /** Llaves de cobro por intento: reintentar el mismo cobro tras un error de red no cobra dos veces. */
        private val chargeKeys = mutableMapOf<String, String>()

        fun refresh() {
            loadJob?.cancel()
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            loadJob =
                viewModelScope.launch {
                    val result = withContext(Dispatchers.IO) { adminRepository.unpaidAccounts() }
                    result.fold(
                        onSuccess = { accounts ->
                            _uiState.value = _uiState.value.copy(accounts = accounts, loading = false)
                        },
                        onFailure = { error ->
                            _uiState.value =
                                _uiState.value.copy(
                                    loading = false,
                                    error = error.toUserFacingMessage("No se pudieron cargar las cuentas."),
                                )
                        },
                    )
                }
        }

        fun dismissCollected() {
            _uiState.value = _uiState.value.copy(lastCollected = null)
        }

        fun clearChargeError() {
            _uiState.value = _uiState.value.copy(chargeError = null)
        }

        /** Cobra lo que falta de la cuenta. [received] solo en efectivo, ya con dos decimales. */
        fun charge(
            account: AdminUnpaidAccount,
            method: AccountPaymentMethod,
            received: String?,
        ) {
            if (_uiState.value.charging != null) return
            val accountKey = unpaidAccountKey(account)
            val orderIds = account.orderLines.map { it.id }.takeIf { it.isNotEmpty() }
            val attempt = listOf(accountKey, method.wire, received, account.remaining, orderIds).joinToString("|")
            val idempotencyKey = chargeKeys.getOrPut(attempt) { UUID.randomUUID().toString() }
            _uiState.value = _uiState.value.copy(charging = accountKey, chargeError = null)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        waiterRepository.collectAccount(
                            spaceId = account.spaceId,
                            method = method,
                            received = received.takeIf { method == AccountPaymentMethod.CASH },
                            expectedTotal = account.remaining,
                            orderIds = orderIds,
                            idempotencyKey = idempotencyKey,
                        )
                    }
                result.fold(
                    onSuccess = { collection ->
                        chargeKeys.remove(attempt)
                        _uiState.value =
                            _uiState.value.copy(
                                charging = null,
                                lastCollected = CashierUnpaidCollected(account.spaceName, collection),
                            )
                    },
                    onFailure = { error ->
                        _uiState.value =
                            _uiState.value.copy(
                                charging = null,
                                chargeError = error.toUserFacingMessage("No se pudo cobrar la cuenta."),
                            )
                    },
                )
                // Con éxito o con un 409 (la cuenta cambió), la lista se vuelve a pedir para cobrar lo de ahora.
                refresh()
            }
        }
    }
