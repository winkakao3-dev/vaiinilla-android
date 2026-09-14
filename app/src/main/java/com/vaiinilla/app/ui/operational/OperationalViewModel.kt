package com.vaiinilla.app.ui.operational

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.MutationIdempotency
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.core.notifications.DeviceTokenRegistrar
import com.vaiinilla.app.core.runCatchingCancellable
import com.vaiinilla.app.data.order.DismissedClientOrdersStore
import com.vaiinilla.app.data.wallet.PendingWalletReload
import com.vaiinilla.app.data.wallet.PendingWalletReloadStore
import com.vaiinilla.app.domain.model.CatalogProductDraft
import com.vaiinilla.app.domain.model.ContractRules
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.WalletClient
import com.vaiinilla.app.domain.repository.CashSessionRepository
import com.vaiinilla.app.domain.repository.CatalogRepository
import com.vaiinilla.app.domain.repository.DeviceHeartbeatRepository
import com.vaiinilla.app.domain.repository.DeviceIdentity
import com.vaiinilla.app.domain.repository.WalletRepository
import com.vaiinilla.app.domain.repository.WalletRepositoryException
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
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
        private val deviceIdentity: DeviceIdentity,
        private val walletRepository: WalletRepository,
        private val catalogRepository: CatalogRepository,
        private val dismissedClientOrdersStore: DismissedClientOrdersStore,
        private val deviceTokenRegistrar: DeviceTokenRegistrar,
        private val pendingWalletReloadStore: PendingWalletReloadStore,
    ) : ViewModel() {
        private val _uiState = mutableStateOf(OperationalUiState())
        val uiState: State<OperationalUiState> = _uiState

        private var pollingJob: Job? = null
        private var lastUpdatedSince: String? = null
        private var roleGeneration: Long = 0
        private var dismissedClientOrderIds: Set<String> = dismissedClientOrdersStore.read()
        private var pendingWalletReload: PendingWalletReload? = null
        private var walletSearchJob: Job? = null
        private var mutationJob: Job? = null
        private val _orderAdvanceEvents = MutableSharedFlow<OrderAdvance>(extraBufferCapacity = 8)
        val orderAdvanceEvents: SharedFlow<OrderAdvance> = _orderAdvanceEvents
        private var clientTrackingBaselineReady = false
        private var mutationErrorVisible = false
        private var errorClearJob: Job? = null
        private val heartbeatCoordinator =
            OperationalHeartbeatCoordinator(
                scope = viewModelScope,
                repository = heartbeatRepository,
                deviceId = deviceIdentity::id,
                onResult = { online ->
                    if (_uiState.value.role != null) {
                        _uiState.value = _uiState.value.copy(heartbeatOnline = online)
                    }
                },
            )

        fun setRole(role: OperationalRole) {
            val roleChanged = _uiState.value.role != role
            if (roleChanged) {
                roleGeneration += 1
                lastUpdatedSince = null
                walletSearchJob?.cancel()
                walletSearchJob = null
                mutationJob?.cancel()
                mutationJob = null
                clientTrackingBaselineReady = false
                errorClearJob?.cancel()
                errorClearJob = null
                mutationErrorVisible = false
            }
            _uiState.value =
                _uiState.value.copy(
                    role = role,
                    orders = if (roleChanged) emptyList() else _uiState.value.orders,
                    latestClientOrder = if (roleChanged) null else _uiState.value.latestClientOrder,
                    menuOrder = if (roleChanged) null else _uiState.value.menuOrder,
                    selectedOrderId = null,
                    errorMessage = null,
                    cashSessionOpen = null,
                    walletClients = emptyList(),
                    walletSearchLoading = false,
                    walletReloadReceipt = null,
                    catalog = null,
                )
            pendingWalletReload = null
            refreshCashSession()
            refresh()
            if (role == OperationalRole.CASHIER) {
                refreshCatalog()
                reconcilePendingWalletReload()
            }
            startPolling()
            // Registra el token FCM apenas hay contexto: cliente recibe avances
            // de su pedido y staff recibe alertas de su establecimiento.
            deviceTokenRegistrar.register()
        }

        fun clearRole() {
            roleGeneration += 1
            pollingJob?.cancel()
            pollingJob = null
            walletSearchJob?.cancel()
            walletSearchJob = null
            mutationJob?.cancel()
            mutationJob = null
            heartbeatCoordinator.stop()
            lastUpdatedSince = null
            pendingWalletReload = null
            clientTrackingBaselineReady = false
            mutationErrorVisible = false
            errorClearJob?.cancel()
            errorClearJob = null
            _uiState.value = OperationalUiState()
        }

        fun selectOrder(orderId: String?) {
            dismissMutationError()
            _uiState.value = _uiState.value.copy(selectedOrderId = orderId)
        }

        fun dismissClientOrder(orderId: String) {
            if (orderId.isBlank()) return
            dismissedClientOrdersStore.dismiss(orderId)
            dismissedClientOrderIds = dismissedClientOrdersStore.read()
            if (_uiState.value.role == OperationalRole.CLIENT) {
                _uiState.value =
                    _uiState.value.copy(
                        orders = _uiState.value.orders.filterNot { it.summary.id == orderId },
                        menuOrder = _uiState.value.menuOrder?.takeUnless { it.summary.id == orderId },
                        selectedOrderId = _uiState.value.selectedOrderId.takeUnless { it == orderId },
                        errorMessage = null,
                    )
            }
        }

        fun refresh() {
            val role = _uiState.value.role ?: return
            val generation = roleGeneration
            if (role == OperationalRole.CLIENT && _uiState.value.orders.isEmpty()) {
                listOrders.cachedClientOrders()?.let { cached ->
                    if (generation == roleGeneration && _uiState.value.role == role) {
                        val visible =
                            filterDismissedClientOrders(
                                role = role,
                                orders = cached,
                                dismissedOrderIds = dismissedClientOrderIds,
                            )
                        if (visible.isNotEmpty()) {
                            _uiState.value =
                                _uiState.value.copy(
                                    orders = visible.sortedByDescending { it.summary.updatedAt },
                                    latestClientOrder =
                                        resolveLatestClientOrder(
                                            previous = _uiState.value.latestClientOrder,
                                            incoming = cached,
                                        ),
                                )
                        }
                    }
                }
            }
            _uiState.value =
                _uiState.value.copy(
                    loading = true,
                    errorMessage = _uiState.value.errorMessage.takeIf { mutationErrorVisible },
                )
            viewModelScope.launch {
                // Client orders always fetch the full list (like iOS): the delta can miss
                // transitions that don't bump updatedAt. Staff keeps the delta.
                val since = if (role == OperationalRole.CLIENT) null else lastUpdatedSince
                val result = withContext(Dispatchers.IO) { listOrders(role, since) }
                if (generation != roleGeneration || _uiState.value.role != role) return@launch
                result.fold(
                    onSuccess = { orders ->
                        if (role == OperationalRole.CLIENT) {
                            emitClientOrderAdvances(
                                previous = _uiState.value.orders,
                                incoming = orders,
                            )
                        }
                        // Full-list fetch for CLIENT replaces state (iOS parity); staff merges deltas.
                        val merged =
                            if (role == OperationalRole.CLIENT) {
                                orders
                            } else {
                                mergeOrders(_uiState.value.orders, orders)
                            }
                        val latestClientOrder =
                            if (role == OperationalRole.CLIENT) {
                                resolveLatestClientOrder(
                                    previous = _uiState.value.latestClientOrder,
                                    incoming = orders,
                                )
                            } else {
                                null
                            }
                        val newest = merged.maxOfOrNull { it.summary.updatedAt }
                        if (newest != null) {
                            lastUpdatedSince = newest
                        }
                        val visibleOrders =
                            filterDismissedClientOrders(
                                role = role,
                                orders = merged,
                                dismissedOrderIds = dismissedClientOrderIds,
                            )
                        _uiState.value =
                            _uiState.value.copy(
                                loading = false,
                                orders = visibleOrders.sortedByDescending { it.summary.updatedAt },
                                latestClientOrder = latestClientOrder,
                                menuOrder = visibleLatestMenuOrder(latestClientOrder, dismissedClientOrderIds),
                                lastSyncedAt = newest,
                                errorMessage =
                                    _uiState.value.errorMessage.takeIf { mutationErrorVisible },
                            )
                    },
                    onFailure = { error ->
                        mutationErrorVisible = false
                        _uiState.value =
                            _uiState.value.copy(
                                loading = false,
                                errorMessage = error.toUserFacingMessage(),
                            )
                    },
                )
            }
        }

        /**
         * Emits one event per client order whose state moved forward in the tracking flow
         * (or into a terminal state) since the previous visible list. The first successful
         * sync only seeds the baseline so reopening the app never replays old transitions.
         */
        private fun emitClientOrderAdvances(
            previous: List<OrderDetail>,
            incoming: List<OrderDetail>,
        ) {
            if (clientTrackingBaselineReady) {
                val previousStates = previous.associate { it.summary.id to it.summary.state }
                incoming.forEach { order ->
                    val before = previousStates[order.summary.id] ?: return@forEach
                    val after = order.summary.state
                    val advanced = after.trackingIndex > before.trackingIndex
                    val resolvedTerminal = after.isTerminalWithoutDelivery && before != after
                    if (before != after && (advanced || resolvedTerminal)) {
                        _orderAdvanceEvents.tryEmit(
                            OrderAdvance(
                                orderId = order.summary.id,
                                folio = order.summary.folio,
                                newState = after,
                            ),
                        )
                    }
                }
            }
            clientTrackingBaselineReady = true
        }

        fun refreshOrder(orderId: String) {
            val generation = roleGeneration
            viewModelScope.launch {
                withContext(Dispatchers.IO) { getOrder(orderId) }.onSuccess { order ->
                    if (generation != roleGeneration) return@onSuccess
                    val clientRole = _uiState.value.role == OperationalRole.CLIENT
                    val latestClientOrder =
                        if (clientRole) {
                            resolveLatestClientOrder(
                                previous = _uiState.value.latestClientOrder,
                                incoming = listOf(order),
                            )
                        } else {
                            null
                        }
                    if (clientRole && order.summary.id in dismissedClientOrderIds) {
                        _uiState.value =
                            _uiState.value.copy(
                                latestClientOrder = latestClientOrder,
                                menuOrder = visibleLatestMenuOrder(latestClientOrder, dismissedClientOrderIds),
                            )
                        return@onSuccess
                    }
                    val updated =
                        _uiState.value.orders
                            .filterNot { it.summary.id == orderId } + order
                    _uiState.value =
                        _uiState.value.copy(
                            orders = updated.sortedByDescending { it.summary.updatedAt },
                            latestClientOrder = latestClientOrder,
                            menuOrder = visibleLatestMenuOrder(latestClientOrder, dismissedClientOrderIds),
                            selectedOrderId = orderId,
                        )
                }
            }
        }

        fun openCashRegister(initialAmount: String = "500.00") {
            if (_uiState.value.role != OperationalRole.CASHIER || _uiState.value.acting) return
            val generation = roleGeneration
            mutationErrorVisible = false
            _uiState.value = _uiState.value.copy(acting = true, errorMessage = null)
            mutationJob =
                viewModelScope.launch {
                    val result =
                        withContext(Dispatchers.IO) {
                            openCashSession(initialAmount, UUID.randomUUID().toString())
                        }
                    if (generation != roleGeneration || _uiState.value.role != OperationalRole.CASHIER) return@launch
                    result
                        .onSuccess {
                            _uiState.value = _uiState.value.copy(acting = false, cashSessionOpen = true)
                        }.onFailure { error ->
                            // Si el POST llegó pero la respuesta se perdió, la sesión ya
                            // existe: releer el estado real evita que un reintento manual
                            // abra una segunda sesión o muestre un error espurio.
                            val alreadyOpen =
                                withContext(Dispatchers.IO) {
                                    cashSessionRepository.hasActiveSession().getOrDefault(false)
                                }
                            _uiState.value =
                                _uiState.value.copy(
                                    acting = false,
                                    cashSessionOpen = if (alreadyOpen) true else _uiState.value.cashSessionOpen,
                                )
                            if (!alreadyOpen) {
                                showMutationError(error.toUserFacingMessage())
                            }
                        }
                }
        }

        fun resolveWalletUserQr(rawValue: String) {
            if (_uiState.value.role != OperationalRole.CASHIER) return
            val payload =
                QrPayloadParser
                    .parse(rawValue)
                    .getOrElse { error ->
                        showMutationError(error.toUserFacingMessage("No se pudo leer el QR."))
                        return
                    }
            val userId = (payload as? QrPayload.User)?.userId
            if (userId == null) {
                showMutationError("Ese QR no es de un alumno. Pide el código de su cuenta.")
                return
            }
            walletSearchJob?.cancel()
            val generation = roleGeneration
            mutationErrorVisible = false
            _uiState.value = _uiState.value.copy(walletSearchLoading = true, errorMessage = null)
            walletSearchJob =
                viewModelScope.launch {
                    val scanned =
                        WalletClient(userId = userId, name = "Cliente escaneado", contextualId = userId)
                    val result = withContext(Dispatchers.IO) { walletRepository.searchClients(userId) }
                    if (generation != roleGeneration || _uiState.value.role != OperationalRole.CASHIER) return@launch
                    result
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
                walletSearchJob?.cancel()
                walletSearchJob = null
                _uiState.value =
                    _uiState.value.copy(
                        walletClients = emptyList(),
                        walletSearchLoading = false,
                    )
                showMutationError("Escribe al menos 2 caracteres para buscar un cliente.")
                return
            }
            walletSearchJob?.cancel()
            val generation = roleGeneration
            mutationErrorVisible = false
            _uiState.value = _uiState.value.copy(walletSearchLoading = true, errorMessage = null)
            walletSearchJob =
                viewModelScope.launch {
                    val result = withContext(Dispatchers.IO) { walletRepository.searchClients(normalizedQuery) }
                    if (generation != roleGeneration || _uiState.value.role != OperationalRole.CASHIER) return@launch
                    result
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
                                )
                            showMutationError(error.toUserFacingMessage("No se pudieron buscar clientes."))
                        }
                }
        }

        fun reloadWallet(
            userId: String,
            amount: String,
        ) {
            if (
                _uiState.value.role != OperationalRole.CASHIER ||
                _uiState.value.cashSessionOpen != true ||
                _uiState.value.acting
            ) {
                return
            }
            val normalizedAmount = amount.trim()
            val validAmount =
                ContractRules.isValidMoney(normalizedAmount) &&
                    runCatching { BigDecimal(normalizedAmount) > BigDecimal.ZERO }.getOrDefault(false)
            if (!validAmount) {
                showMutationError("El monto debe ser positivo y tener dos decimales (ej. 100.00).")
                return
            }
            val stored = pendingWalletReloadStore.read()
            if (stored != null && (stored.userId != userId || stored.amount != normalizedAmount)) {
                showMutationError(
                    "Hay una recarga anterior sin confirmar. " +
                        "Reintenta con el mismo cliente y monto (\$${stored.amount}) para confirmarla sin duplicarla.",
                )
                return
            }
            val attempt =
                stored
                    ?: PendingWalletReload(
                        userId = userId,
                        amount = normalizedAmount,
                        idempotencyKey = UUID.randomUUID().toString(),
                        storedAtEpochMs = System.currentTimeMillis(),
                    ).also {
                        // Persistir ANTES del POST: si el proceso muere tras el abono,
                        // la misma Idempotency-Key sobrevive y el retry confirma.
                        pendingWalletReloadStore.write(it)
                        pendingWalletReload = it
                    }
            val generation = roleGeneration
            mutationErrorVisible = false
            _uiState.value = _uiState.value.copy(acting = true, errorMessage = null)
            mutationJob =
                viewModelScope.launch {
                    val result =
                        withContext(Dispatchers.IO) {
                            walletRepository.reloadCash(attempt.userId, attempt.amount, attempt.idempotencyKey)
                        }
                    if (generation != roleGeneration || _uiState.value.role != OperationalRole.CASHIER) return@launch
                    result
                        .onSuccess { receipt ->
                            pendingWalletReload = null
                            pendingWalletReloadStore.clear()
                            _uiState.value =
                                _uiState.value.copy(
                                    acting = false,
                                    walletReloadReceipt = receipt,
                                    errorMessage = null,
                                )
                        }.onFailure { error ->
                            if (isDefinitiveWalletReloadFailure(error)) {
                                // El servidor procesó y rechazó la recarga: nada quedó abonado.
                                pendingWalletReload = null
                                pendingWalletReloadStore.clear()
                            }
                            _uiState.value =
                                _uiState.value.copy(acting = false)
                            showMutationError(error.toUserFacingMessage("No se pudo registrar la recarga."))
                        }
                }
        }

        /**
         * Reintenta una recarga que quedó persistida porque el proceso murió a mitad
         * del POST. El replay usa la MISMA Idempotency-Key, así que es seguro: si el
         * servidor ya la aplicó devuelve el comprobante, y si nunca llegó la completa
         * ahora. Solo corre bajo el mismo contexto JWT (cajero + establecimiento).
         */
        private fun reconcilePendingWalletReload() {
            val pending = pendingWalletReloadStore.read() ?: return
            pendingWalletReload = pending
            val generation = roleGeneration
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        walletRepository.reloadCash(pending.userId, pending.amount, pending.idempotencyKey)
                    }
                if (generation != roleGeneration) return@launch
                result
                    .onSuccess { receipt ->
                        pendingWalletReload = null
                        pendingWalletReloadStore.clear()
                        if (_uiState.value.role == OperationalRole.CASHIER) {
                            _uiState.value =
                                _uiState.value.copy(
                                    walletReloadReceipt = receipt,
                                    errorMessage = null,
                                )
                        }
                    }.onFailure { error ->
                        if (isDefinitiveWalletReloadFailure(error)) {
                            pendingWalletReload = null
                            pendingWalletReloadStore.clear()
                        } else if (_uiState.value.role == OperationalRole.CASHIER) {
                            showMutationError(
                                "Hay una recarga pendiente de confirmar. " +
                                    "Reintenta con el mismo cliente y monto (\$${pending.amount}).",
                            )
                        }
                    }
            }
        }

        fun collectCash(
            orderId: String,
            amountReceived: String,
            expectedVersion: Int,
        ) {
            val role = _uiState.value.role ?: return
            // El ticket pudo abrirse hace varios ciclos de polling: cobrar con la
            // versión que el servidor tiene ahora, no con la del render viejo.
            val freshest = _uiState.value.orders.firstOrNull { it.summary.id == orderId }
            val version = freshest?.summary?.version ?: expectedVersion
            val normalizedAmount = amountReceived.trim()
            val received =
                normalizedAmount
                    .takeIf { ContractRules.isValidMoney(it) }
                    ?.let { runCatching { BigDecimal(it) }.getOrNull() }
            if (received == null) {
                showMutationError("Escribe el efectivo recibido con dos decimales (ej. 100.00).")
                return
            }
            val total = freshest?.summary?.total?.let { runCatching { Money.parse(it) }.getOrNull() }
            if (total != null && received < total) {
                showMutationError("El efectivo recibido no cubre el total del pedido (\$${freshest.summary.total}).")
                return
            }
            val generation = roleGeneration
            mutationErrorVisible = false
            _uiState.value =
                _uiState.value.copy(acting = true, errorMessage = null, cashChangeNotice = null)
            mutationJob =
                viewModelScope.launch {
                    val result =
                        withContext(Dispatchers.IO) {
                            collectCash(
                                orderId = orderId,
                                amountReceived = normalizedAmount,
                                expectedVersion = version,
                                idempotencyKey =
                                    MutationIdempotency.cashCollection(orderId, normalizedAmount, version),
                            )
                        }
                    if (generation != roleGeneration || _uiState.value.role != role) return@launch
                    result
                        .onSuccess { receipt ->
                            val updated =
                                _uiState.value.orders
                                    .filterNot { it.summary.id == receipt.order.summary.id } + receipt.order
                            val change = runCatching { Money.parse(receipt.change) }.getOrNull()
                            _uiState.value =
                                _uiState.value.copy(
                                    acting = false,
                                    orders = updated.sortedByDescending { it.summary.updatedAt },
                                    selectedOrderId = receipt.order.summary.id,
                                    cashChangeNotice =
                                        if (change != null && change > BigDecimal.ZERO) {
                                            "Cobro confirmado · Cambio: \$${receipt.change}"
                                        } else {
                                            "Cobro confirmado"
                                        },
                                )
                        }.onFailure { error ->
                            _uiState.value = _uiState.value.copy(acting = false)
                            showMutationError(error.toUserFacingMessage())
                        }
                }
        }

        fun consumeCashChangeNotice() {
            if (_uiState.value.cashChangeNotice != null) {
                _uiState.value = _uiState.value.copy(cashChangeNotice = null)
            }
        }

        private fun freshVersion(
            orderId: String,
            fallback: Int,
        ): Int =
            _uiState.value.orders
                .firstOrNull { it.summary.id == orderId }
                ?.summary
                ?.version ?: fallback

        fun startKitchen(
            orderId: String,
            expectedVersion: Int,
        ) {
            val version = freshVersion(orderId, expectedVersion)
            performMutation {
                transitionOrder(
                    orderId = orderId,
                    targetState = OrderState.PREPARING,
                    expectedVersion = version,
                    idempotencyKey =
                        MutationIdempotency.orderTransition(
                            orderId,
                            OrderState.PREPARING.wireValue,
                            version,
                            pickupToken = null,
                        ),
                ).getOrThrow()
            }
        }

        fun markReady(
            orderId: String,
            expectedVersion: Int,
        ) {
            val version = freshVersion(orderId, expectedVersion)
            performMutation {
                transitionOrder(
                    orderId = orderId,
                    targetState = OrderState.READY,
                    expectedVersion = version,
                    idempotencyKey =
                        MutationIdempotency.orderTransition(
                            orderId,
                            OrderState.READY.wireValue,
                            version,
                            pickupToken = null,
                        ),
                ).getOrThrow()
            }
        }

        fun deliver(
            orderId: String,
            expectedVersion: Int,
            scannedPickupToken: String? = null,
        ) {
            // Si el escáner leyó el QR equivocado (cuenta del alumno o del
            // establecimiento), avisar en vez de mandar un token que el
            // servidor va a rechazar y que el polling borraría del banner.
            scannedPickupToken?.trim()?.takeIf(String::isNotEmpty)?.let { scanned ->
                when (QrPayloadParser.parse(scanned).getOrNull()) {
                    is QrPayload.User -> {
                        showMutationError("Ese QR es la cuenta del alumno, no su pedido. Pide el QR de recogida.")
                        return
                    }
                    is QrPayload.Establishment -> {
                        showMutationError("Ese QR es del establecimiento, no de un pedido.")
                        return
                    }
                    else -> Unit
                }
            }
            val freshest = _uiState.value.orders.firstOrNull { it.summary.id == orderId }
            val version = freshest?.summary?.version ?: expectedVersion
            val pickupToken =
                scannedPickupToken
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
                    ?: freshest?.pickupToken
            performMutation {
                transitionOrder(
                    orderId = orderId,
                    targetState = OrderState.DELIVERED,
                    expectedVersion = version,
                    idempotencyKey =
                        MutationIdempotency.orderTransition(
                            orderId,
                            OrderState.DELIVERED.wireValue,
                            version,
                            pickupToken,
                        ),
                    pickupToken = pickupToken,
                ).getOrThrow()
            }
        }

        fun trackingHint(order: OrderDetail): String =
            when (order.summary.state) {
                OrderState.PENDING_PAYMENT ->
                    when (order.summary.paymentMethod) {
                        PaymentMethod.CASH -> "Pasa a Caja para confirmar el pago en efectivo."
                        PaymentMethod.BALANCE -> "El saldo está pendiente de actualización."
                        PaymentMethod.STRIPE -> "Estamos verificando el pago con Vaiinilla."
                    }
                OrderState.PAID -> "Cocina recibirá la comanda en cuanto abra su pantalla."
                OrderState.PREPARING -> "Tu pedido se está preparando."
                OrderState.READY ->
                    if (order.summary.destination == OrderDestination.TAKE_AWAY) {
                        "Listo para recoger en barra."
                    } else {
                        "Listo para entrega en tu espacio."
                    }
                OrderState.DELIVERED -> "Pedido entregado. Gracias por usar Vaiinilla."
                OrderState.CANCELED -> "Este pedido fue cancelado."
                OrderState.NOT_PICKED_UP -> "El pedido terminó como no recogido."
                OrderState.EXPIRED -> "Este pedido expiró."
            }

        private fun refreshCashSession() {
            if (_uiState.value.role != OperationalRole.CASHIER) return
            val generation = roleGeneration
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { cashSessionRepository.hasActiveSession() }
                if (generation != roleGeneration || _uiState.value.role != OperationalRole.CASHIER) return@launch
                result
                    .onSuccess { open ->
                        _uiState.value = _uiState.value.copy(cashSessionOpen = open)
                    }.onFailure { error ->
                        mutationErrorVisible = false
                        _uiState.value =
                            _uiState.value.copy(
                                errorMessage = error.toUserFacingMessage(),
                            )
                    }
            }
        }

        private fun performMutation(block: () -> OrderDetail) {
            if (_uiState.value.acting) return
            val role = _uiState.value.role ?: return
            val generation = roleGeneration
            mutationErrorVisible = false
            _uiState.value = _uiState.value.copy(acting = true, errorMessage = null)
            mutationJob =
                viewModelScope.launch {
                    val result = runCatchingCancellable { withContext(Dispatchers.IO) { block() } }
                    if (generation != roleGeneration || _uiState.value.role != role) return@launch
                    result
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
                            _uiState.value = _uiState.value.copy(acting = false)
                            showMutationError(error.toUserFacingMessage())
                        }
                }
        }

        /**
         * Errores de acciones del usuario (cobrar, entregar, recargar) deben
         * sobrevivir al polling de 5s: si el refresh limpia errorMessage en su
         * siguiente ciclo, un cobro rechazado se ve como "no pasó nada". Los
         * errores de mutación quedan visibles ~9s; los errores de sync siguen
         * siendo transitorios y el próximo poll exitoso los limpia.
         */
        private fun showMutationError(message: String) {
            mutationErrorVisible = true
            errorClearJob?.cancel()
            errorClearJob =
                viewModelScope.launch {
                    delay(MUTATION_ERROR_VISIBLE_MS)
                    if (mutationErrorVisible) {
                        mutationErrorVisible = false
                        _uiState.value = _uiState.value.copy(errorMessage = null)
                    }
                }
            _uiState.value = _uiState.value.copy(errorMessage = message)
        }

        private fun dismissMutationError() {
            mutationErrorVisible = false
            errorClearJob?.cancel()
            errorClearJob = null
            if (_uiState.value.errorMessage != null) {
                _uiState.value = _uiState.value.copy(errorMessage = null)
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
                        delay(POLL_INTERVAL_MS)
                        refresh()
                    }
                }
        }

        fun onOperationalForeground(role: OperationalRole) {
            if (_uiState.value.role != role) return
            heartbeatCoordinator.start(role)
            _uiState.value = _uiState.value.copy(heartbeatOnline = false)
        }

        fun onOperationalBackground(role: OperationalRole) {
            if (_uiState.value.role != role) return
            heartbeatCoordinator.pause()
            _uiState.value = _uiState.value.copy(heartbeatOnline = null)
        }

        fun onRuntimeModeChanged() {
            val role = _uiState.value.role ?: return
            refreshCashSession()
            refresh()
            if (role == OperationalRole.CASHIER) refreshCatalog()
        }

        fun refreshCatalog() {
            if (_uiState.value.role != OperationalRole.CASHIER) return
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { catalogRepository.getCatalog() }
                _uiState.value =
                    _uiState.value.copy(
                        catalog = result.getOrNull(),
                        errorMessage =
                            when {
                                mutationErrorVisible -> _uiState.value.errorMessage
                                result.isFailure ->
                                    result.exceptionOrNull().toUserFacingMessage(
                                        _uiState.value.errorMessage ?: "No se pudo cargar el catálogo.",
                                    )
                                else -> null
                            },
                    )
            }
        }

        fun setProductAvailable(
            productId: Int,
            available: Boolean,
        ) {
            if (_uiState.value.acting) return
            mutationErrorVisible = false
            _uiState.value = _uiState.value.copy(acting = true, errorMessage = null)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        catalogRepository.setProductAvailability(
                            productId = productId,
                            available = available,
                            idempotencyKey = UUID.randomUUID().toString(),
                        )
                    }
                result.fold(
                    onSuccess = { updated ->
                        val catalog = _uiState.value.catalog
                        _uiState.value =
                            _uiState.value.copy(
                                acting = false,
                                catalog =
                                    catalog?.copy(
                                        products =
                                            catalog.products.map { product ->
                                                if (product.id == updated.id) updated else product
                                            },
                                    ),
                            )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(acting = false)
                        showMutationError(
                            error.toUserFacingMessage(
                                "No se pudo actualizar la disponibilidad (producto $productId).",
                            ),
                        )
                        refreshCatalog()
                    },
                )
            }
        }

        fun createCashierProduct(
            draft: CatalogProductDraft,
            imageBytes: ByteArray? = null,
            imageFilename: String? = null,
            imageMime: String? = null,
            onSuccess: (String?) -> Unit = {},
        ) {
            if (imageBytes != null && imageBytes.size > MAX_PRODUCT_IMAGE_BYTES) {
                showMutationError("La foto no puede pesar más de 5 MB.")
                return
            }
            if (_uiState.value.acting) return
            mutationErrorVisible = false
            _uiState.value = _uiState.value.copy(acting = true, errorMessage = null)
            viewModelScope.launch {
                val created =
                    withContext(Dispatchers.IO) {
                        catalogRepository.createProduct(draft, UUID.randomUUID().toString())
                    }
                val createdProduct =
                    created
                        .getOrElse { error ->
                            _uiState.value = _uiState.value.copy(acting = false)
                            showMutationError(error.toUserFacingMessage("No se pudo crear el producto."))
                            // Si el POST llegó pero la respuesta se perdió, el producto ya
                            // existe: el refresh lo muestra en lugar de dejar un duplicado.
                            refreshCatalog()
                            return@launch
                        }
                var finalProduct = createdProduct
                if (imageBytes != null && imageFilename != null && imageMime != null) {
                    val uploaded =
                        withContext(Dispatchers.IO) {
                            catalogRepository.uploadProductImage(
                                productId = createdProduct.id,
                                bytes = imageBytes,
                                filename = imageFilename,
                                mimeType = imageMime,
                                idempotencyKey = MutationIdempotency.productImage(createdProduct.id, imageBytes),
                            )
                        }
                    if (uploaded.isFailure) {
                        val catalog = _uiState.value.catalog
                        val warning =
                            uploaded.exceptionOrNull().toUserFacingMessage(
                                "El producto se creó, pero la foto no se subió.",
                            )
                        _uiState.value =
                            _uiState.value.copy(
                                acting = false,
                                catalog =
                                    catalog?.copy(
                                        products = catalog.products + createdProduct,
                                    ),
                                errorMessage = null,
                            )
                        onSuccess(warning)
                        refreshCatalog()
                        return@launch
                    }
                    finalProduct = uploaded.getOrThrow()
                }
                val catalog = _uiState.value.catalog
                _uiState.value =
                    _uiState.value.copy(
                        acting = false,
                        catalog =
                            catalog?.copy(
                                products =
                                    catalog.products
                                        .filterNot { product -> product.id == finalProduct.id } +
                                        finalProduct,
                            ),
                    )
                onSuccess(null)
                refreshCatalog()
            }
        }

        fun uploadCashierProductImage(
            productId: Int,
            bytes: ByteArray,
            filename: String,
            mimeType: String,
        ) {
            if (bytes.size > MAX_PRODUCT_IMAGE_BYTES) {
                showMutationError("La foto no puede pesar más de 5 MB.")
                return
            }
            if (_uiState.value.acting) return
            mutationErrorVisible = false
            _uiState.value = _uiState.value.copy(acting = true, errorMessage = null)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        catalogRepository.uploadProductImage(
                            productId = productId,
                            bytes = bytes,
                            filename = filename,
                            mimeType = mimeType,
                            idempotencyKey = MutationIdempotency.productImage(productId, bytes),
                        )
                    }
                result.fold(
                    onSuccess = { updated ->
                        val catalog = _uiState.value.catalog
                        _uiState.value =
                            _uiState.value.copy(
                                acting = false,
                                catalog =
                                    catalog?.copy(
                                        products =
                                            catalog.products.map { product ->
                                                if (product.id == updated.id) updated else product
                                            },
                                    ),
                            )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(acting = false)
                        showMutationError(
                            error.toUserFacingMessage("No se pudo subir la foto (producto $productId)."),
                        )
                    },
                )
            }
        }

        fun applyGalleryClientOrders(
            orders: List<OrderDetail>,
            selectedOrderId: String?,
        ) {
            pollingJob?.cancel()
            pollingJob = null
            val latestClientOrder = resolveLatestClientOrder(previous = null, incoming = orders)
            _uiState.value =
                _uiState.value.copy(
                    role = OperationalRole.CLIENT,
                    orders = orders.sortedByDescending { it.summary.updatedAt },
                    latestClientOrder = latestClientOrder,
                    menuOrder = visibleLatestMenuOrder(latestClientOrder, dismissedClientOrderIds),
                    selectedOrderId = selectedOrderId,
                    loading = false,
                    acting = false,
                    errorMessage = null,
                )
            mutationErrorVisible = false
        }

        override fun onCleared() {
            pollingJob?.cancel()
            super.onCleared()
        }

        private companion object {
            const val POLL_INTERVAL_MS = 5_000L
            const val MUTATION_ERROR_VISIBLE_MS = 9_000L
            const val MAX_PRODUCT_IMAGE_BYTES = 5 * 1024 * 1024
        }
    }

/**
 * Una recarga rechazada por el servidor (4xx) nunca abonó saldo: el registro
 * pendiente se puede descartar. Errores de red/timeout/5xx son ambiguos — el
 * POST pudo haber llegado — así que el registro se conserva para el replay.
 */
internal fun isDefinitiveWalletReloadFailure(error: Throwable): Boolean =
    (error as? WalletRepositoryException)?.httpStatus in 400..499

internal fun resolveLatestClientOrder(
    previous: OrderDetail?,
    incoming: List<OrderDetail>,
): OrderDetail? =
    (incoming + listOfNotNull(previous))
        .maxByOrNull { it.summary.createdAt }

internal fun visibleLatestMenuOrder(
    latest: OrderDetail?,
    dismissedOrderIds: Set<String>,
): OrderDetail? = latest?.takeUnless { it.summary.id in dismissedOrderIds }

internal fun filterDismissedClientOrders(
    role: OperationalRole?,
    orders: List<OrderDetail>,
    dismissedOrderIds: Set<String>,
): List<OrderDetail> =
    if (role == OperationalRole.CLIENT) {
        orders.filterNot { it.summary.id in dismissedOrderIds }
    } else {
        orders
    }

/** A client order moved forward in the tracking flow; surfaced as a system notification. */
data class OrderAdvance(
    val orderId: String,
    val folio: Int,
    val newState: OrderState,
)
