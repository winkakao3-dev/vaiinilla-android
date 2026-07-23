package com.vaiinilla.app.ui.operational

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.security.RoleAccessTokenStore
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.repository.CashSessionRepository
import com.vaiinilla.app.domain.usecase.CollectCashUseCase
import com.vaiinilla.app.domain.usecase.GetOrderUseCase
import com.vaiinilla.app.domain.usecase.ListOrdersUseCase
import com.vaiinilla.app.domain.usecase.OpenCashSessionUseCase
import com.vaiinilla.app.domain.usecase.TransitionOrderUseCase
import com.vaiinilla.app.domain.repository.DeviceHeartbeatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class OperationalViewModel @Inject constructor(
    private val listOrders: ListOrdersUseCase,
    private val getOrder: GetOrderUseCase,
    private val collectCash: CollectCashUseCase,
    private val transitionOrder: TransitionOrderUseCase,
    private val openCashSession: OpenCashSessionUseCase,
    private val cashSessionRepository: CashSessionRepository,
    private val heartbeatRepository: DeviceHeartbeatRepository,
    private val roleAccessTokenStore: RoleAccessTokenStore,
) : ViewModel() {
    private val _uiState = mutableStateOf(OperationalUiState())
    val uiState: State<OperationalUiState> = _uiState

    private var pollingJob: Job? = null
    private var lastUpdatedSince: String? = null

    fun setRole(role: OperationalRole) {
        roleAccessTokenStore.applyRole(role)
        _uiState.value = _uiState.value.copy(
            role = role,
            selectedOrderId = null,
            errorMessage = null,
            cashSessionOpen = null,
        )
        sendHeartbeatIfNeeded()
        refreshCashSession()
        refresh()
        startPolling()
    }

    fun clearRole() {
        pollingJob?.cancel()
        pollingJob = null
        lastUpdatedSince = null
        _uiState.value = OperationalUiState()
    }

    fun selectOrder(orderId: String?) {
        _uiState.value = _uiState.value.copy(selectedOrderId = orderId, errorMessage = null)
    }

    fun refresh() {
        val role = _uiState.value.role ?: return
        _uiState.value = _uiState.value.copy(loading = true, errorMessage = null)
        val result = listOrders(role, lastUpdatedSince)
        result.fold(
            onSuccess = { orders ->
                val merged = mergeOrders(_uiState.value.orders, orders)
                val newest = merged.maxOfOrNull { it.summary.updatedAt }
                if (newest != null) {
                    lastUpdatedSince = newest
                }
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    orders = merged.sortedByDescending { it.summary.updatedAt },
                    lastSyncedAt = newest,
                    errorMessage = null,
                )
            },
            onFailure = { error ->
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    errorMessage = error.message,
                )
            },
        )
    }

    fun refreshOrder(orderId: String) {
        getOrder(orderId).onSuccess { order ->
            val updated = _uiState.value.orders
                .filterNot { it.summary.id == orderId } + order
            _uiState.value = _uiState.value.copy(
                orders = updated.sortedByDescending { it.summary.updatedAt },
                selectedOrderId = orderId,
            )
        }
    }

    fun openCashRegister(initialAmount: String = "500.00") {
        _uiState.value = _uiState.value.copy(acting = true, errorMessage = null)
        openCashSession(initialAmount, UUID.randomUUID().toString())
            .onSuccess {
                sendHeartbeatIfNeeded()
                _uiState.value = _uiState.value.copy(acting = false, cashSessionOpen = true)
            }
            .onFailure { error ->
                _uiState.value = _uiState.value.copy(acting = false, errorMessage = error.message)
            }
    }

    fun collectCash(orderId: String, amountReceived: String) {
        performMutation {
            collectCash(orderId, amountReceived, UUID.randomUUID().toString()).getOrThrow()
        }
    }

    fun startKitchen(orderId: String, expectedVersion: Int) {
        performMutation {
            transitionOrder(
                orderId = orderId,
                targetState = OrderState.PREPARING,
                expectedVersion = expectedVersion,
                idempotencyKey = UUID.randomUUID().toString(),
            ).getOrThrow()
        }
    }

    fun markReady(orderId: String, expectedVersion: Int) {
        performMutation {
            transitionOrder(
                orderId = orderId,
                targetState = OrderState.READY,
                expectedVersion = expectedVersion,
                idempotencyKey = UUID.randomUUID().toString(),
            ).getOrThrow()
        }
    }

    fun deliver(orderId: String, expectedVersion: Int) {
        val pickupToken = _uiState.value.orders.firstOrNull { it.summary.id == orderId }?.pickupToken
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

    fun trackingHint(order: OrderDetail): String = when (order.summary.state) {
        OrderState.PENDING_PAYMENT -> "Pasa a Caja para confirmar el pago en efectivo."
        OrderState.PAID -> "Cocina recibirá la comanda en cuanto abra su pantalla."
        OrderState.PREPARING -> "Tu pedido se está preparando."
        OrderState.READY -> if (order.summary.destination == OrderDestination.TAKE_AWAY) {
            "Listo para recoger en barra."
        } else {
            "Listo para entrega en tu espacio."
        }
        OrderState.DELIVERED -> "Pedido entregado. Gracias por usar Vaiinilla."
    }

    private fun refreshCashSession() {
        if (_uiState.value.role != OperationalRole.CASHIER) return
        cashSessionRepository.hasActiveSession()
            .onSuccess { open ->
                _uiState.value = _uiState.value.copy(cashSessionOpen = open)
            }
            .onFailure { error ->
                _uiState.value = _uiState.value.copy(errorMessage = error.message)
            }
    }

    private fun performMutation(block: () -> OrderDetail) {
        _uiState.value = _uiState.value.copy(acting = true, errorMessage = null)
        runCatching { block() }
            .onSuccess { order ->
                val updated = _uiState.value.orders
                    .filterNot { it.summary.id == order.summary.id } + order
                _uiState.value = _uiState.value.copy(
                    acting = false,
                    orders = updated.sortedByDescending { it.summary.updatedAt },
                    selectedOrderId = order.summary.id,
                )
            }
            .onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    acting = false,
                    errorMessage = error.message,
                )
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
        pollingJob = viewModelScope.launch {
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
        heartbeatRepository.sendHeartbeat(
            deviceId = "android-${role.wireValue}",
            role = role,
            idempotencyKey = UUID.randomUUID().toString(),
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
