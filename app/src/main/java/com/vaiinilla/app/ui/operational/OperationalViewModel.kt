package com.vaiinilla.app.ui.operational

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.domain.model.ContractRules
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.WalletClient
import com.vaiinilla.app.domain.repository.CashSessionRepository
import com.vaiinilla.app.domain.repository.DeviceHeartbeatRepository
import com.vaiinilla.app.domain.repository.WalletRepository
import com.vaiinilla.app.domain.usecase.CollectCashUseCase
import com.vaiinilla.app.domain.usecase.GetOrderUseCase
import com.vaiinilla.app.domain.usecase.ListOrdersUseCase
import com.vaiinilla.app.domain.usecase.OpenCashSessionUseCase
import com.vaiinilla.app.domain.usecase.TransitionOrderUseCase
import com.vaiinilla.app.ui.discovery.QrPayload
import com.vaiinilla.app.ui.discovery.QrPayloadParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class OperationalViewModel
    @Inject
    constructor(
        private val listOrders: ListOrdersUseCase,
        private val getOrder: GetOrderUseCase,
        private val collectCash: CollectCashUseCase,
        private val transitionOrder: TransitionOrderUseCase,
        private val openCashSession: OpenCashSessionUseCase,
        private val cashSessionRepository: CashSessionRepository,
        private val heartbeatRepository: DeviceHeartbeatRepository,
        private val walletRepository: WalletRepository,
    ) : ViewModel() {
        private val _uiState = mutableStateOf(OperationalUiState())
        val uiState: State<OperationalUiState> = _uiState

        private var pollingJob: Job? = null
        private var lastUpdatedSince: String? = null
        private var pendingWalletReload: PendingWalletReload? = null

        fun setRole(role: OperationalRole) {
            _uiState.value =
                _uiState.value.copy(
                    role = role,
                    selectedOrderId = null,
                    errorMessage = null,
                    cashSessionOpen = null,
                    walletClients = emptyList(),
                    walletSearchLoading = false,
                    walletReloadReceipt = null,
                )
            pendingWalletReload = null
            sendHeartbeatIfNeeded()
            refreshCashSession()
            refresh()
            startPolling()
        }

        fun clearRole() {
            pollingJob?.cancel()
            pollingJob = null
            lastUpdatedSince = null
            pendingWalletReload = null
            _uiState.value = OperationalUiState()
        }

        fun selectOrder(orderId: String?) {
            _uiState.value = _uiState.value.copy(selectedOrderId = orderId, errorMessage = null)
        }

        fun refresh() {
            val role = _uiState.value.role ?: return
            _uiState.value = _uiState.value.copy(loading = true, errorMessage = null)
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { listOrders(role, lastUpdatedSince) }
                result.fold(
                    onSuccess = { orders ->
                        val merged = mergeOrders(_uiState.value.orders, orders)
                        val newest = merged.maxOfOrNull { it.summary.updatedAt }
                        if (newest != null) {
                            lastUpdatedSince = newest
                        }
                        _uiState.value =
                            _uiState.value.copy(
                                loading = false,
                                orders = merged.sortedByDescending { it.summary.updatedAt },
                                lastSyncedAt = newest,
                                errorMessage = null,
                            )
                    },
                    onFailure = { error ->
                        _uiState.value =
                            _uiState.value.copy(
                                loading = false,
                                errorMessage = error.message ?: error.javaClass.simpleName,
                            )
                    },
                )
            }
        }

        fun refreshOrder(orderId: String) {
            viewModelScope.launch {
                withContext(Dispatchers.IO) { getOrder(orderId) }.onSuccess { order ->
                    val updated =
                        _uiState.value.orders
                            .filterNot { it.summary.id == orderId } + order
                    _uiState.value =
                        _uiState.value.copy(
                            orders = updated.sortedByDescending { it.summary.updatedAt },
                            selectedOrderId = orderId,
                        )
                }
            }
        }

        fun openCashRegister(initialAmount: String = "500.00") {
            _uiState.value = _uiState.value.copy(acting = true, errorMessage = null)
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    openCashSession(initialAmount, UUID.randomUUID().toString())
                }.onSuccess {
                    sendHeartbeatIfNeeded()
                    _uiState.value = _uiState.value.copy(acting = false, cashSessionOpen = true)
                }.onFailure { error ->
                    _uiState.value =
                        _uiState.value.copy(
                            acting = false,
                            errorMessage = error.message ?: error.javaClass.simpleName,
                        )
                }
            }
        }

        fun resolveWalletUserQr(rawValue: String) {
            if (_uiState.value.role != OperationalRole.CASHIER) return
            val payload = QrPayloadParser.parse(rawValue).getOrElse { error ->
                _uiState.value =
                    _uiState.value.copy(
                        errorMessage = error.message ?: "No se pudo leer el QR.",
                    )
                return
            }
            val userId = (payload as? QrPayload.User)?.userId
            if (userId == null) {
                _uiState.value =
                    _uiState.value.copy(
                        errorMessage = "Ese QR no es de un alumno. Pide el código de su cuenta.",
                    )
                return
            }
            _uiState.value = _uiState.value.copy(walletSearchLoading = true, errorMessage = null)
            viewModelScope.launch {
                val scanned =
                    WalletClient(userId = userId, name = "Cliente escaneado", contextualId = userId)
                withContext(Dispatchers.IO) { walletRepository.searchClients(userId) }
                    .onSuccess { clients ->
                        _uiState.value =
                            _uiState.value.copy(
                                walletClients = clients.ifEmpty { listOf(scanned) },
                                walletSearchLoading = false,
                            )
                    }.onFailure {
                        _uiState.value =
                            _uiState.value.copy(
                                walletClients = listOf(scanned),
                                walletSearchLoading = false,
                            )
                    }
            }
        }

        fun searchWalletClients(query: String) {
            if (_uiState.value.role != OperationalRole.CASHIER) return
            val normalizedQuery = query.trim()
            if (normalizedQuery.length < 2) {
                _uiState.value =
                    _uiState.value.copy(
                        walletClients = emptyList(),
                        walletSearchLoading = false,
                        errorMessage = "Escribe al menos 2 caracteres para buscar un cliente.",
                    )
                return
            }
            _uiState.value = _uiState.value.copy(walletSearchLoading = true, errorMessage = null)
            viewModelScope.launch {
                withContext(Dispatchers.IO) { walletRepository.searchClients(normalizedQuery) }
                    .onSuccess { clients ->
                        _uiState.value =
                            _uiState.value.copy(
                                walletClients = clients,
                                walletSearchLoading = false,
                            )
                    }.onFailure { error ->
                        _uiState.value =
                            _uiState.value.copy(
                                walletClients = emptyList(),
                                walletSearchLoading = false,
                                errorMessage = error.message ?: "No se pudieron buscar clientes.",
                            )
                    }
            }
        }

        fun reloadWallet(
            userId: String,
            amount: String,
        ) {
            if (_uiState.value.role != OperationalRole.CASHIER || _uiState.value.cashSessionOpen != true) return
            val normalizedAmount = amount.trim()
            val validAmount =
                ContractRules.isValidMoney(normalizedAmount) &&
                    runCatching { BigDecimal(normalizedAmount) > BigDecimal.ZERO }.getOrDefault(false)
            if (!validAmount) {
                _uiState.value =
                    _uiState.value.copy(
                        errorMessage = "El monto debe ser positivo y tener dos decimales (ej. 100.00).",
                    )
                return
            }
            val attempt =
                pendingWalletReload
                    ?.takeIf { it.userId == userId && it.amount == normalizedAmount }
                    ?: PendingWalletReload(
                        userId = userId,
                        amount = normalizedAmount,
                        idempotencyKey = UUID.randomUUID().toString(),
                    ).also { pendingWalletReload = it }
            _uiState.value = _uiState.value.copy(acting = true, errorMessage = null)
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    walletRepository.reloadCash(attempt.userId, attempt.amount, attempt.idempotencyKey)
                }.onSuccess { receipt ->
                    pendingWalletReload = null
                    _uiState.value =
                        _uiState.value.copy(
                            acting = false,
                            walletReloadReceipt = receipt,
                            errorMessage = null,
                        )
                }.onFailure { error ->
                    _uiState.value =
                        _uiState.value.copy(
                            acting = false,
                            errorMessage = error.message ?: "No se pudo registrar la recarga.",
                        )
                }
            }
        }

        fun collectCash(
            orderId: String,
            amountReceived: String,
            expectedVersion: Int,
        ) {
            performMutation {
                collectCash(
                    orderId = orderId,
                    amountReceived = amountReceived,
                    expectedVersion = expectedVersion,
                    idempotencyKey = UUID.randomUUID().toString(),
                ).getOrThrow()
            }
        }

        fun startKitchen(
            orderId: String,
            expectedVersion: Int,
        ) {
            performMutation {
                transitionOrder(
                    orderId = orderId,
                    targetState = OrderState.PREPARING,
                    expectedVersion = expectedVersion,
                    idempotencyKey = UUID.randomUUID().toString(),
                ).getOrThrow()
            }
        }

        fun markReady(
            orderId: String,
            expectedVersion: Int,
        ) {
            performMutation {
                transitionOrder(
                    orderId = orderId,
                    targetState = OrderState.READY,
                    expectedVersion = expectedVersion,
                    idempotencyKey = UUID.randomUUID().toString(),
                ).getOrThrow()
            }
        }

        fun deliver(
            orderId: String,
            expectedVersion: Int,
            scannedPickupToken: String? = null,
        ) {
            val pickupToken =
                scannedPickupToken
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
                    ?: _uiState.value.orders
                        .firstOrNull { it.summary.id == orderId }
                        ?.pickupToken
            performMutation {
                transitionOrder(
                    orderId = orderId,
                    targetState = OrderState.DELIVERED,
                    expectedVersion = expectedVersion,
                    idempotencyKey = UUID.randomUUID().toString(),
                    pickupToken = pickupToken,
                ).getOrThrow()
            }
        }

        fun trackingHint(order: OrderDetail): String =
            when (order.summary.state) {
                OrderState.PENDING_PAYMENT -> "Pasa a Caja para confirmar el pago en efectivo."
                OrderState.PAID -> "Cocina recibirá la comanda en cuanto abra su pantalla."
                OrderState.PREPARING -> "Tu pedido se está preparando."
                OrderState.READY ->
                    if (order.summary.destination == OrderDestination.TAKE_AWAY) {
                        "Listo para recoger en barra."
                    } else {
                        "Listo para entrega en tu espacio."
                    }
                OrderState.DELIVERED -> "Pedido entregado. Gracias por usar Vaiinilla."
            }

        private fun refreshCashSession() {
            if (_uiState.value.role != OperationalRole.CASHIER) return
            viewModelScope.launch {
                withContext(Dispatchers.IO) { cashSessionRepository.hasActiveSession() }
                    .onSuccess { open ->
                        _uiState.value = _uiState.value.copy(cashSessionOpen = open)
                    }.onFailure { error ->
                        _uiState.value =
                            _uiState.value.copy(
                                errorMessage = error.message ?: error.javaClass.simpleName,
                            )
                    }
            }
        }

        private fun performMutation(block: () -> OrderDetail) {
            _uiState.value = _uiState.value.copy(acting = true, errorMessage = null)
            viewModelScope.launch {
                runCatching { withContext(Dispatchers.IO) { block() } }
                    .onSuccess { order ->
                        val updated =
                            _uiState.value.orders
                                .filterNot { it.summary.id == order.summary.id } + order
                        _uiState.value =
                            _uiState.value.copy(
                                acting = false,
                                orders = updated.sortedByDescending { it.summary.updatedAt },
                                selectedOrderId = order.summary.id,
                            )
                    }.onFailure { error ->
                        _uiState.value =
                            _uiState.value.copy(
                                acting = false,
                                errorMessage = error.message ?: error.javaClass.simpleName,
                            )
                    }
            }
        }

        private fun mergeOrders(
            current: List<OrderDetail>,
            incoming: List<OrderDetail>,
        ): List<OrderDetail> {
            val byId = current.associateBy { it.summary.id }.toMutableMap()
            incoming.forEach { order -> byId[order.summary.id] = order }
            return byId.values.toList()
        }

        private fun startPolling() {
            pollingJob?.cancel()
            pollingJob =
                viewModelScope.launch {
                    while (isActive) {
                        sendHeartbeatIfNeeded()
                        delay(POLL_INTERVAL_MS)
                        refresh()
                    }
                }
        }

        private fun sendHeartbeatIfNeeded() {
            val role = _uiState.value.role ?: return
            if (role == OperationalRole.CLIENT) return
            viewModelScope.launch(Dispatchers.IO) {
                heartbeatRepository.sendHeartbeat(
                    deviceId = "android-${role.wireValue}",
                    role = role,
                    idempotencyKey = UUID.randomUUID().toString(),
                )
            }
        }

        fun onRuntimeModeChanged() {
            val role = _uiState.value.role ?: return
            refreshCashSession()
            refresh()
        }

        fun applyGalleryClientOrders(
            orders: List<OrderDetail>,
            selectedOrderId: String?,
        ) {
            pollingJob?.cancel()
            pollingJob = null
            _uiState.value =
                _uiState.value.copy(
                    role = OperationalRole.CLIENT,
                    orders = orders.sortedByDescending { it.summary.updatedAt },
                    selectedOrderId = selectedOrderId,
                    loading = false,
                    acting = false,
                    errorMessage = null,
                )
        }

        override fun onCleared() {
            pollingJob?.cancel()
            super.onCleared()
        }

        private companion object {
            const val POLL_INTERVAL_MS = 5_000L
        }
    }

private data class PendingWalletReload(
    val userId: String,
    val amount: String,
    val idempotencyKey: String,
)
