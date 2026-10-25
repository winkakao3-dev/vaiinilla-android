package com.vaiinilla.app.ui.operational

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.MutationIdempotency
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.core.notifications.DeviceTokenRegistrar
import com.vaiinilla.app.core.runCatchingCancellable
import com.vaiinilla.app.data.guest.GuestCheckoutPolicy
import com.vaiinilla.app.data.guest.GuestOrderGateway
import com.vaiinilla.app.data.guest.GuestSessionStore
import com.vaiinilla.app.data.order.DismissedClientOrdersStore
import com.vaiinilla.app.data.wallet.PendingWalletReload
import com.vaiinilla.app.data.wallet.PendingWalletReloadStore
import com.vaiinilla.app.domain.model.CashCollectionResult
import com.vaiinilla.app.domain.model.CatalogProductDraft
import com.vaiinilla.app.domain.model.ContractRules
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.MoneyInput
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.cashAmountDue
import com.vaiinilla.app.domain.model.needsCashCollection
import com.vaiinilla.app.domain.repository.CallReason
import com.vaiinilla.app.domain.repository.CashSessionRepository
import com.vaiinilla.app.domain.repository.CatalogRepository
import com.vaiinilla.app.domain.repository.DeviceHeartbeatRepository
import com.vaiinilla.app.domain.repository.DeviceIdentity
import com.vaiinilla.app.domain.repository.OrderRepositoryException
import com.vaiinilla.app.domain.repository.StoreImageKind
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.domain.repository.WaiterRepository
import com.vaiinilla.app.domain.repository.WalletRepository
import com.vaiinilla.app.domain.repository.WalletRepositoryException
import com.vaiinilla.app.domain.usecase.AnnounceArrivalUseCase
import com.vaiinilla.app.domain.usecase.CancelOrderUseCase
import com.vaiinilla.app.domain.usecase.CloseCashSessionUseCase
import com.vaiinilla.app.domain.usecase.CollectCashUseCase
import com.vaiinilla.app.domain.usecase.GetOrderUseCase
import com.vaiinilla.app.domain.usecase.ListOrdersUseCase
import com.vaiinilla.app.domain.usecase.OpenCashSessionUseCase
import com.vaiinilla.app.domain.usecase.TransitionOrderUseCase
import com.vaiinilla.app.ui.components.moneyLabel
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
        private val cancelOrder: CancelOrderUseCase,
        private val announceArrivalUseCase: AnnounceArrivalUseCase,
        private val openCashSession: OpenCashSessionUseCase,
        private val closeCashSession: CloseCashSessionUseCase,
        private val cashSessionRepository: CashSessionRepository,
        private val heartbeatRepository: DeviceHeartbeatRepository,
        private val deviceIdentity: DeviceIdentity,
        private val walletRepository: WalletRepository,
        private val catalogRepository: CatalogRepository,
        private val dismissedClientOrdersStore: DismissedClientOrdersStore,
        private val deviceTokenRegistrar: DeviceTokenRegistrar,
        private val pendingWalletReloadStore: PendingWalletReloadStore,
        private val waiterRepository: WaiterRepository,
        private val guestOrders: GuestOrderGateway,
        private val guestSessionStore: GuestSessionStore,
        private val guestCheckoutPolicy: GuestCheckoutPolicy,
    ) : ViewModel() {
        private val _uiState = mutableStateOf(OperationalUiState())
        val uiState: State<OperationalUiState> = _uiState

        private var pollingJob: Job? = null
        private var appVisible = true
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

        // Cada refresh() lleva su número: una respuesta más vieja que la ya aplicada se descarta.
        private var refreshSeq = 0L
        private var appliedRefreshSeq = 0L
        private var pollsSinceFullSync = 0
        private var fullSyncRequested = false

        // Avisos de personal ya dados (o sembrados en la primera carga) para no repetir el mismo.
        private val alertedKeys = mutableSetOf<String>()
        private var staffBaselineReady = false

        // Sin repetición: un aviso que nadie escuchaba (pantalla apagada o en otro modo) ya no sirve después.
        private val _staffAlerts = MutableSharedFlow<StaffAlert>(extraBufferCapacity = 16)
        val staffAlerts: SharedFlow<StaffAlert> = _staffAlerts

        // Un cobro que terminó cuando el cajero ya había salido de la pantalla se avisa al volver.
        private var deferredCashierNotice: String? = null
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
                resetStaffSync()
            }
            _uiState.value =
                _uiState.value.copy(
                    role = role,
                    // La mutación del rol anterior se cancela y su resultado se descarta: no puede dejar
                    // la bandera pegada y apagar todos los botones del rol nuevo.
                    acting = if (roleChanged) false else _uiState.value.acting,
                    catalogBusy = if (roleChanged) false else _uiState.value.catalogBusy,
                    cashSessionCheckFailed = false,
                    orders = if (roleChanged) emptyList() else _uiState.value.orders,
                    latestClientOrder = if (roleChanged) null else _uiState.value.latestClientOrder,
                    menuOrder = if (roleChanged) null else _uiState.value.menuOrder,
                    selectedOrderId = null,
                    errorMessage = null,
                    lastRefreshOk = if (roleChanged) null else _uiState.value.lastRefreshOk,
                    cashSessionOpen = null,
                    walletClients = emptyList(),
                    walletSearchLoading = false,
                    walletReloadReceipt = null,
                    pendingWalletReload = null,
                    catalog = null,
                )
            pendingWalletReload = null
            if (role == OperationalRole.CASHIER) surfaceDeferredCashierNotice()
            // Administración tiene su propia pantalla y su propio ViewModel: aquí no se sondean pedidos.
            if (role == OperationalRole.ADMIN) {
                pollingJob?.cancel()
                return
            }
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
            // La mutación en vuelo NO se cancela: su POST ya salió y el cobro de Caja deja su aviso para
            // cuando el cajero vuelva; las demás se descartan al ver que cambió la generación.
            heartbeatCoordinator.stop()
            resetStaffSync()
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

        /** Pedidos del cliente: los de la cuenta o, sin cuenta, los del invitado en el negocio que mira. */
        private fun loadOrders(
            role: OperationalRole,
            updatedSince: String?,
        ): Result<List<OrderDetail>> {
            val venue = guestSessionStore.readVenue()
            val slug = venue?.establishment?.slug
            if (role != OperationalRole.CLIENT) return listOrders(role, updatedSince)
            if (slug == null || !guestCheckoutPolicy.allows(venue)) {
                return listOrders(role, updatedSince).map(listOrders::withRecoveredPickupTokens)
            }
            if (!guestOrders.hasIdentity()) return Result.success(emptyList())
            return guestOrders.list(slug)
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
            // Mientras la carga siga fallando el error se queda: limpiarlo en cada sondeo lo haría parpadear.
            _uiState.value =
                _uiState.value.copy(
                    loading = true,
                    errorMessage =
                        _uiState.value.errorMessage.takeIf {
                            mutationErrorVisible || _uiState.value.lastRefreshOk == false
                        },
                )
            val seq = ++refreshSeq
            // El personal pide cambios, pero el servidor no avisa de lo que dejó de ser suyo (cobrado por otro
            // cajero, cancelado, expirado): cada cierto tiempo se pide la lista completa y se concilia.
            val fullSync = role != OperationalRole.CLIENT && isFullSyncDue()
            viewModelScope.launch {
                // Client orders always fetch the full list (like iOS): the delta can miss
                // transitions that don't bump updatedAt. Staff keeps the delta.
                val since = if (role == OperationalRole.CLIENT || fullSync) null else lastUpdatedSince
                val result = withContext(Dispatchers.IO) { loadOrders(role, since) }
                if (generation != roleGeneration || _uiState.value.role != role) return@launch
                // Una respuesta que salió antes de otra ya aplicada traería el estado viejo encima del nuevo.
                if (seq < appliedRefreshSeq) return@launch
                appliedRefreshSeq = seq
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
                            when {
                                role == OperationalRole.CLIENT -> orders
                                fullSync -> reconcileOrders(_uiState.value.orders, orders)
                                else -> mergeOrders(_uiState.value.orders, orders)
                            }
                        if (fullSync) {
                            pollsSinceFullSync = 0
                            fullSyncRequested = false
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
                        if (role == OperationalRole.CASHIER || role == OperationalRole.KITCHEN) {
                            announceStaffOrders(role, visibleOrders)
                        }
                        _uiState.value =
                            _uiState.value.copy(
                                loading = false,
                                orders = visibleOrders.sortedByDescending { it.summary.updatedAt },
                                latestClientOrder = latestClientOrder,
                                menuOrder = visibleLatestMenuOrder(latestClientOrder, dismissedClientOrderIds),
                                lastSyncedAt = newest,
                                lastRefreshOk = true,
                                errorMessage =
                                    _uiState.value.errorMessage.takeIf { mutationErrorVisible },
                            )
                    },
                    onFailure = { error ->
                        mutationErrorVisible = false
                        _uiState.value =
                            _uiState.value.copy(
                                loading = false,
                                lastRefreshOk = false,
                                errorMessage = error.toUserFacingMessage(),
                            )
                    },
                )
            }
        }

        /** La primera carga solo siembra lo que ya había; después, cada pedido nuevo para este rol avisa una vez. */
        private fun announceStaffOrders(
            role: OperationalRole,
            orders: List<OrderDetail>,
        ) {
            val fresh = staffAlertsFor(role, orders).filter { alertedKeys.add(it.key) }
            if (staffBaselineReady) fresh.forEach { _staffAlerts.tryEmit(it) }
            staffBaselineReady = true
        }

        private fun isFullSyncDue(): Boolean {
            if (fullSyncRequested || lastUpdatedSince == null) return true
            pollsSinceFullSync += 1
            return pollsSinceFullSync >= FULL_SYNC_EVERY_POLLS
        }

        /** Pide la lista completa en el siguiente refresh (tras un error de estado o un cierre de caja). */
        private fun requestFullSync() {
            fullSyncRequested = true
            refresh()
        }

        private fun resetStaffSync() {
            refreshSeq = 0
            appliedRefreshSeq = 0
            pollsSinceFullSync = 0
            fullSyncRequested = false
            alertedKeys.clear()
            staffBaselineReady = false
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

        /** Abre la caja con el fondo que el cajero contó; el servidor lo guarda como monto inicial del arqueo. */
        fun openCashRegister(initialAmount: String) {
            if (_uiState.value.role != OperationalRole.CASHIER || _uiState.value.acting) return
            val wireAmount = MoneyInput.toWire(initialAmount)
            if (wireAmount == null) {
                showMutationError("Escribe el efectivo con el que abres la caja (puede ser 0).")
                return
            }
            val generation = roleGeneration
            mutationErrorVisible = false
            _uiState.value = _uiState.value.copy(acting = true, errorMessage = null)
            mutationJob =
                viewModelScope.launch {
                    val result =
                        withContext(Dispatchers.IO) {
                            openCashSession(wireAmount, UUID.randomUUID().toString())
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

        fun requestCloseCash() {
            if (_uiState.value.role != OperationalRole.CASHIER || _uiState.value.cashSessionOpen != true) return
            _uiState.value = _uiState.value.copy(closeCash = CloseCashUi())
        }

        fun dismissCloseCash() {
            if (_uiState.value.closeCash?.closing == true) return
            _uiState.value = _uiState.value.copy(closeCash = null)
        }

        /** Cierra la caja con el efectivo contado. Con cuentas por cobrar el servidor lo rechaza y se avisa. */
        fun closeCashRegister(finalAmount: String) {
            val current = _uiState.value.closeCash ?: return
            if (_uiState.value.role != OperationalRole.CASHIER || current.closing) return
            val generation = roleGeneration
            _uiState.value = _uiState.value.copy(closeCash = CloseCashUi(closing = true))
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        cashSessionRepository.activeSessionId().mapCatching { id ->
                            val sessionId = id ?: error("No hay una caja abierta.")
                            closeCashSession(sessionId, finalAmount, UUID.randomUUID().toString()).getOrThrow()
                        }
                    }
                if (generation != roleGeneration || _uiState.value.role != OperationalRole.CASHIER) return@launch
                result
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(closeCash = null, cashSessionOpen = false)
                        // Al cerrar, el servidor expira los pedidos por cobrar: que no queden como cobrables.
                        requestFullSync()
                    }.onFailure { error ->
                        val blocked =
                            error is OrderRepositoryException && error.code == CASH_HAS_OPEN_ACCOUNTS_CODE
                        // Si la respuesta del cierre se perdió, la caja ya pudo cerrarse: se relee el estado real
                        // en vez de dejar "abierta" en pantalla y un error que el reintento no puede arreglar.
                        val stillOpen =
                            if (blocked) {
                                true
                            } else {
                                withContext(Dispatchers.IO) { cashSessionRepository.hasActiveSession() }
                                    .getOrDefault(true)
                            }
                        if (generation != roleGeneration || _uiState.value.role != OperationalRole.CASHIER) {
                            return@launch
                        }
                        _uiState.value =
                            if (stillOpen) {
                                _uiState.value.copy(
                                    closeCash =
                                        CloseCashUi(
                                            blockedByAccounts = blocked,
                                            errorMessage = if (blocked) null else error.toUserFacingMessage(),
                                        ),
                                )
                            } else {
                                _uiState.value.copy(closeCash = null, cashSessionOpen = false)
                            }
                        if (!stillOpen) requestFullSync()
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
                    // El QR trae solo un id: se resuelve a nombre antes de pedir el monto, para que el cajero
                    // vea a quién abona y un QR ajeno o inexistente se rechace aquí.
                    val result = withContext(Dispatchers.IO) { walletRepository.lookupClient(userId) }
                    if (generation != roleGeneration || _uiState.value.role != OperationalRole.CASHIER) return@launch
                    result
                        .onSuccess { client ->
                            _uiState.value =
                                _uiState.value.copy(walletClients = listOf(client), walletSearchLoading = false)
                        }.onFailure { error ->
                            _uiState.value =
                                _uiState.value.copy(walletClients = emptyList(), walletSearchLoading = false)
                            val notClient = (error as? WalletRepositoryException)?.httpStatus == 404
                            showMutationError(
                                if (notClient) {
                                    "Ese QR no es de un cliente de este negocio."
                                } else {
                                    error.toUserFacingMessage("No se pudo identificar al cliente. Inténtalo otra vez.")
                                },
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
                publishPendingWalletReload(stored)
                showMutationError(
                    "Hay una recarga anterior sin confirmar (${pendingReloadLabel(stored)}). " +
                        "Confírmala o descártala antes de hacer otra.",
                )
                return
            }
            val userName =
                _uiState.value.walletClients
                    .firstOrNull { it.userId == userId }
                    ?.name
            val attempt =
                stored
                    ?: PendingWalletReload(
                        userId = userId,
                        amount = normalizedAmount,
                        idempotencyKey = UUID.randomUUID().toString(),
                        storedAtEpochMs = System.currentTimeMillis(),
                        userName = userName,
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
                                    pendingWalletReload = null,
                                    errorMessage = null,
                                )
                        }.onFailure { error ->
                            if (isDefinitiveWalletReloadFailure(error)) {
                                // El servidor procesó y rechazó la recarga: nada quedó abonado.
                                pendingWalletReload = null
                                pendingWalletReloadStore.clear()
                            }
                            _uiState.value = _uiState.value.copy(acting = false)
                            showMutationError(error.toUserFacingMessage("No se pudo registrar la recarga."))
                        }
                }
        }

        /**
         * Al abrir Caja con una recarga persistida (el proceso murió a mitad del POST): si es reciente se
         * confirma sola con la MISMA Idempotency-Key, que es segura; si es vieja se deja a la vista para que el
         * cajero decida, porque abonar saldo días después de un cobro que nadie recuerda no es confirmar.
         */
        private fun reconcilePendingWalletReload() {
            val pending = pendingWalletReloadStore.read() ?: return
            pendingWalletReload = pending
            if (isStalePendingReload(pending, System.currentTimeMillis())) {
                publishPendingWalletReload(pending)
                return
            }
            replayPendingWalletReload(pending, userInitiated = false)
        }

        /** El cajero confirma la recarga pendiente que ve en la hoja. */
        fun confirmPendingWalletReload() {
            val pending = pendingWalletReloadStore.read() ?: return
            if (_uiState.value.role != OperationalRole.CASHIER || _uiState.value.acting) return
            replayPendingWalletReload(pending, userInitiated = true)
        }

        /** El cajero descarta la recarga pendiente: no se abonó nada o ya se resolvió fuera de la app. */
        fun discardPendingWalletReload() {
            pendingWalletReload = null
            pendingWalletReloadStore.clear()
            _uiState.value = _uiState.value.copy(pendingWalletReload = null)
        }

        private fun publishPendingWalletReload(pending: PendingWalletReload) {
            _uiState.value =
                _uiState.value.copy(
                    pendingWalletReload = PendingReloadUi(userName = pending.userName, amount = pending.amount),
                )
        }

        private fun replayPendingWalletReload(
            pending: PendingWalletReload,
            userInitiated: Boolean,
        ) {
            val generation = roleGeneration
            if (userInitiated) _uiState.value = _uiState.value.copy(acting = true, errorMessage = null)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        walletRepository.reloadCash(pending.userId, pending.amount, pending.idempotencyKey)
                    }
                if (generation != roleGeneration) return@launch
                val isCashier = _uiState.value.role == OperationalRole.CASHIER
                result
                    .onSuccess { receipt ->
                        pendingWalletReload = null
                        pendingWalletReloadStore.clear()
                        if (isCashier) {
                            _uiState.value =
                                _uiState.value.copy(
                                    acting = false,
                                    walletReloadReceipt = receipt,
                                    pendingWalletReload = null,
                                    // La hoja de recarga no está abierta al confirmar en segundo plano.
                                    actionNotice = "Se confirmó la recarga pendiente de ${moneyLabel(pending.amount)}",
                                    errorMessage = null,
                                )
                        }
                    }.onFailure { error ->
                        if (isDefinitiveWalletReloadFailure(error)) {
                            pendingWalletReload = null
                            pendingWalletReloadStore.clear()
                        }
                        if (isCashier) {
                            val stillPending = pendingWalletReloadStore.read()
                            _uiState.value =
                                _uiState.value.copy(
                                    acting = false,
                                    pendingWalletReload =
                                        stillPending?.let {
                                            PendingReloadUi(userName = it.userName, amount = it.amount)
                                        },
                                )
                            if (userInitiated || stillPending != null) {
                                showMutationError(
                                    "No se pudo confirmar la recarga pendiente (${pendingReloadLabel(pending)}). " +
                                        "Inténtalo otra vez o descártala.",
                                )
                            }
                        }
                    }
            }
        }

        fun collectCash(
            orderId: String,
            amountReceived: String,
            expectedVersion: Int,
            tip: String? = null,
        ) {
            val role = _uiState.value.role ?: return
            // Dos toques en el mismo cuadro lanzarían dos cobros y perderían el rastro del primero.
            if (_uiState.value.acting) return
            // El ticket pudo abrirse hace varios ciclos de polling: cobrar con la
            // versión que el servidor tiene ahora, no con la del render viejo.
            val freshest = _uiState.value.orders.firstOrNull { it.summary.id == orderId }
            val version = freshest?.summary?.version ?: expectedVersion
            // "200" o "200,5" son montos válidos: el contrato los quiere con dos decimales ("200.00").
            val normalizedAmount = MoneyInput.toWire(amountReceived) ?: amountReceived.trim()
            val received =
                normalizedAmount
                    .takeIf { ContractRules.isValidMoney(it) }
                    ?.let { runCatching { BigDecimal(it) }.getOrNull() }
            if (received == null) {
                showMutationError("Escribe el efectivo recibido (ej. 100 o 100.50).")
                return
            }
            val total = freshest?.summary?.cashAmountDue?.let { runCatching { Money.parse(it) }.getOrNull() }
            val tipAmount = tip?.let { runCatching { BigDecimal(it) }.getOrNull() } ?: BigDecimal.ZERO
            if (total != null && received < total + tipAmount) {
                showMutationError("El efectivo recibido no cubre el total del pedido más la propina.")
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
                                // La propina es parte del cobro: otra propina es otro cobro.
                                idempotencyKey =
                                    MutationIdempotency.cashCollection(
                                        orderId,
                                        normalizedAmount + "|" + tip.orEmpty(),
                                        version,
                                    ),
                                tip = tip,
                            )
                        }
                    if (generation != roleGeneration || _uiState.value.role != role) {
                        // El cajero salió de la pantalla con el cobro en vuelo: el dinero ya se movió, así que
                        // se le dice al volver en vez de dejarlo sin saber si cobró.
                        result.onSuccess { receipt -> deferredCashierNotice = cashCollectionNotice(receipt) }
                        return@launch
                    }
                    result
                        .onSuccess { receipt ->
                            val updated =
                                _uiState.value.orders
                                    .filterNot { it.summary.id == receipt.order.summary.id } + receipt.order
                            _uiState.value =
                                _uiState.value.copy(
                                    acting = false,
                                    orders = updated.sortedByDescending { it.summary.updatedAt },
                                    selectedOrderId = receipt.order.summary.id,
                                    cashChangeNotice = cashCollectionNotice(receipt),
                                )
                            // Cobrado el pedido, el servidor deja de ofrecerlo a Caja: se concilia la lista.
                            requestFullSync()
                        }.onFailure { error ->
                            _uiState.value = _uiState.value.copy(acting = false)
                            showMutationError(error.toUserFacingMessage())
                            if (isStaleOrderError(error)) requestFullSync()
                        }
                }
        }

        private fun surfaceDeferredCashierNotice() {
            val notice = deferredCashierNotice ?: return
            deferredCashierNotice = null
            _uiState.value = _uiState.value.copy(cashChangeNotice = notice)
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
            performMutation(successNotice = ::kitchenActionNotice) {
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

        /** Drive-thru: el cliente avisa que ya llegó; Cocina y Caja lo ven en el pedido. */
        fun announceArrival(orderId: String) {
            performMutation { announceArrivalUseCase(orderId).getOrThrow() }
        }

        /** Cocina rechaza un pedido que no puede preparar; el cliente verá el motivo. */
        fun rejectOrder(
            orderId: String,
            expectedVersion: Int,
            reason: String,
        ) {
            val motivo = reason.trim()
            if (motivo.length < MIN_REJECTION_REASON) {
                showMutationError("Escribe el motivo del rechazo (mínimo $MIN_REJECTION_REASON letras).")
                return
            }
            val version = freshVersion(orderId, expectedVersion)
            performMutation(successNotice = ::kitchenActionNotice) {
                cancelOrder(
                    orderId = orderId,
                    expectedVersion = version,
                    reason = motivo,
                    idempotencyKey = MutationIdempotency.orderCancellation(orderId, version, motivo),
                ).getOrThrow()
            }
        }

        /** Cocina quita un solo artículo que no puede preparar; el cliente verá el motivo y el total baja. */
        fun rejectItem(
            orderId: String,
            itemId: Int,
            expectedVersion: Int,
            reason: String,
        ) {
            val motivo = reason.trim()
            if (motivo.length < MIN_REJECTION_REASON) {
                showMutationError("Escribe el motivo (mínimo $MIN_REJECTION_REASON letras).")
                return
            }
            val version = freshVersion(orderId, expectedVersion)
            performMutation(successNotice = { order -> "Se quitó un artículo de la comanda #${order.summary.folio}" }) {
                cancelOrder
                    .rejectItem(
                        orderId = orderId,
                        itemId = itemId,
                        expectedVersion = version,
                        reason = motivo,
                        idempotencyKey = MutationIdempotency.itemRejection(orderId, itemId, version, motivo),
                    ).getOrThrow()
            }
        }

        fun markReady(
            orderId: String,
            expectedVersion: Int,
        ) {
            val version = freshVersion(orderId, expectedVersion)
            performMutation(successNotice = ::kitchenActionNotice) {
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

        /** Relee si la caja está abierta; otro dispositivo o el administrador pudo abrirla o cerrarla. */
        fun refreshCashSession() {
            if (_uiState.value.role != OperationalRole.CASHIER) return
            val generation = roleGeneration
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { cashSessionRepository.hasActiveSession() }
                if (generation != roleGeneration || _uiState.value.role != OperationalRole.CASHIER) return@launch
                result
                    .onSuccess { open ->
                        _uiState.value = _uiState.value.copy(cashSessionOpen = open, cashSessionCheckFailed = false)
                    }.onFailure {
                        // Con la caja ya conocida se conserva lo último; solo si nunca se supo, la pantalla pide reintentar.
                        _uiState.value =
                            _uiState.value.copy(cashSessionCheckFailed = _uiState.value.cashSessionOpen == null)
                    }
            }
        }

        /** [successNotice] arma la confirmación con el pedido que devolvió el servidor, nunca antes. */
        private fun performMutation(
            successNotice: (OrderDetail) -> String? = { null },
            block: () -> OrderDetail,
        ) {
            val role = _uiState.value.role ?: return
            if (_uiState.value.acting) {
                // El personal tocó un botón que se ignoró: que lo sepa en vez de quedarse esperando.
                if (role != OperationalRole.CLIENT) showMutationError("Espera un momento: hay otra acción en curso.")
                return
            }
            val generation = roleGeneration
            mutationErrorVisible = false
            _uiState.value = _uiState.value.copy(acting = true, errorMessage = null, actionNotice = null)
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
                                    actionNotice = successNotice(order),
                                )
                        }.onFailure { error ->
                            _uiState.value = _uiState.value.copy(acting = false)
                            showMutationError(staffMutationMessage(error))
                            // Pedido cancelado, ya avanzado o de otro: la tarjeta que se ve ya no es la real.
                            if (isStaleOrderError(error)) requestFullSync()
                        }
                }
        }

        fun consumeActionNotice() {
            if (_uiState.value.actionNotice != null) {
                _uiState.value = _uiState.value.copy(actionNotice = null)
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

        fun dismissMutationError() {
            mutationErrorVisible = false
            errorClearJob?.cancel()
            errorClearJob = null
            if (_uiState.value.errorMessage != null) {
                _uiState.value = _uiState.value.copy(errorMessage = null)
            }
        }

        /** La app pasó a segundo plano: sin nadie mirando, el sondeo de 5 s solo gasta batería y datos. */
        fun onAppHidden() {
            appVisible = false
            pollingJob?.cancel()
            pollingJob = null
        }

        fun onAppVisible() {
            if (appVisible) return
            appVisible = true
            val role = _uiState.value.role ?: return
            if (role == OperationalRole.ADMIN) return
            refresh()
            startPolling()
        }

        private fun startPolling() {
            pollingJob?.cancel()
            if (!appVisible) {
                pollingJob = null
                return
            }
            pollingJob =
                viewModelScope.launch {
                    while (isActive) {
                        delay(POLL_INTERVAL_MS)
                        refresh()
                        refreshCashSession()
                    }
                }
        }

        fun onOperationalForeground(role: OperationalRole) {
            if (_uiState.value.role != role) return
            heartbeatCoordinator.start(role)
            _uiState.value = _uiState.value.copy(heartbeatOnline = false)
            // Al volver de segundo plano pudo pasar de todo: se concilia la lista completa.
            if (role != OperationalRole.CLIENT && role != OperationalRole.ADMIN) requestFullSync()
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
            loadStoreImages()
            val generation = roleGeneration
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { catalogRepository.getCatalog() }
                if (generation != roleGeneration || _uiState.value.role != OperationalRole.CASHIER) return@launch
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
                                // Un error de la carga de pedidos sigue ahí hasta que esa carga salga bien.
                                else -> _uiState.value.errorMessage.takeIf { _uiState.value.lastRefreshOk == false }
                            },
                    )
            }
        }

        /** Un servidor que aún no da las imágenes a Caja responde 403: la tarjeta simplemente no aparece. */
        private fun loadStoreImages() {
            val generation = roleGeneration
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { catalogRepository.getStoreImages() }
                if (generation != roleGeneration) return@launch
                result.onSuccess { images -> _uiState.value = _uiState.value.copy(storeImages = images) }
            }
        }

        fun uploadStoreImage(
            kind: StoreImageKind,
            bytes: ByteArray,
            filename: String,
            mimeType: String,
        ) {
            if (_uiState.value.storeImageBusy != null) return
            if (bytes.size > MAX_PRODUCT_IMAGE_BYTES) {
                _uiState.value = _uiState.value.copy(storeImageNotice = "La imagen no puede pesar más de 5 MB.")
                return
            }
            _uiState.value = _uiState.value.copy(storeImageBusy = kind, storeImageNotice = null)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        catalogRepository.uploadStoreImage(
                            kind = kind,
                            bytes = bytes,
                            filename = filename,
                            mimeType = mimeType,
                            idempotencyKey = MutationIdempotency.storeImage(kind.wireValue, bytes),
                        )
                    }
                _uiState.value =
                    result.fold(
                        onSuccess = { images ->
                            _uiState.value.copy(
                                storeImages = images,
                                storeImageBusy = null,
                                storeImageNotice = storeImageDone(kind, removed = false),
                            )
                        },
                        onFailure = { error ->
                            _uiState.value.copy(
                                storeImageBusy = null,
                                storeImageNotice = error.toUserFacingMessage("No se pudo subir la imagen."),
                            )
                        },
                    )
            }
        }

        fun removeStoreImage(kind: StoreImageKind) {
            if (_uiState.value.storeImageBusy != null) return
            _uiState.value = _uiState.value.copy(storeImageBusy = kind, storeImageNotice = null)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        catalogRepository.removeStoreImage(kind, UUID.randomUUID().toString())
                    }
                _uiState.value =
                    result.fold(
                        onSuccess = { images ->
                            _uiState.value.copy(
                                storeImages = images,
                                storeImageBusy = null,
                                storeImageNotice = storeImageDone(kind, removed = true),
                            )
                        },
                        onFailure = { error ->
                            _uiState.value.copy(
                                storeImageBusy = null,
                                storeImageNotice = error.toUserFacingMessage("No se pudo quitar la imagen."),
                            )
                        },
                    )
            }
        }

        private fun storeImageDone(
            kind: StoreImageKind,
            removed: Boolean,
        ): String =
            when (kind) {
                StoreImageKind.LOGO -> if (removed) "Logo quitado." else "Logo actualizado. Tus clientes ya lo ven."
                StoreImageKind.PHOTO ->
                    if (removed) "Foto del lugar quitada." else "Foto del lugar actualizada. Tus clientes ya la ven."
            }

        fun setProductAvailable(
            productId: Int,
            available: Boolean,
        ) {
            if (_uiState.value.catalogBusy) return
            mutationErrorVisible = false
            _uiState.value = _uiState.value.copy(catalogBusy = true, errorMessage = null)
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
                                catalogBusy = false,
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
                        _uiState.value = _uiState.value.copy(catalogBusy = false)
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
            if (_uiState.value.catalogBusy) return
            mutationErrorVisible = false
            _uiState.value = _uiState.value.copy(catalogBusy = true, errorMessage = null)
            viewModelScope.launch {
                val created =
                    withContext(Dispatchers.IO) {
                        catalogRepository.createProduct(draft, UUID.randomUUID().toString())
                    }
                val createdProduct =
                    created
                        .getOrElse { error ->
                            _uiState.value = _uiState.value.copy(catalogBusy = false)
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
                                catalogBusy = false,
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
                        catalogBusy = false,
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
            if (_uiState.value.catalogBusy) return
            mutationErrorVisible = false
            _uiState.value = _uiState.value.copy(catalogBusy = true, errorMessage = null)
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
                                catalogBusy = false,
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
                        _uiState.value = _uiState.value.copy(catalogBusy = false)
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
                    lastRefreshOk = true,
                )
            mutationErrorVisible = false
        }

        override fun onCleared() {
            pollingJob?.cancel()
            super.onCleared()
        }

        suspend fun currentWaiterCall(espacioId: Int): Result<TableCall?> =
            withContext(Dispatchers.IO) { waiterRepository.currentCall(espacioId) }

        suspend fun callWaiter(
            espacioId: Int,
            reason: CallReason,
            orderId: String?,
        ): Result<TableCall> =
            withContext(Dispatchers.IO) {
                waiterRepository.call(espacioId, reason, orderId, UUID.randomUUID().toString())
            }

        suspend fun cancelWaiterCall(call: TableCall): Result<TableCall> =
            withContext(Dispatchers.IO) {
                waiterRepository.cancelCall(call, UUID.randomUUID().toString())
            }

        private companion object {
            const val CASH_HAS_OPEN_ACCOUNTS_CODE = "CASH_SESSION_HAS_OPEN_ACCOUNTS"
            const val POLL_INTERVAL_MS = 5_000L
            const val MUTATION_ERROR_VISIBLE_MS = 9_000L
            const val MAX_PRODUCT_IMAGE_BYTES = 5 * 1024 * 1024
            const val MIN_REJECTION_REASON = 3
            const val FULL_SYNC_EVERY_POLLS = 6
        }
    }

/** Qué pasó en la fila de pedidos que el personal debe notar aunque no esté mirando la pantalla. */
enum class StaffAlertKind { NEW_KITCHEN_ORDER, CASH_DUE, READY_FOR_PICKUP }

data class StaffAlert(
    val kind: StaffAlertKind,
    val orderId: String,
    val folio: Int,
) {
    internal val key: String get() = "$kind:$orderId"
}

/** Avisos que corresponden a este rol con los pedidos que tiene hoy: comandas por empezar, efectivo por cobrar, listos. */
internal fun staffAlertsFor(
    role: OperationalRole,
    orders: List<OrderDetail>,
): List<StaffAlert> =
    orders.mapNotNull { order ->
        val summary = order.summary
        val kind =
            when (role) {
                OperationalRole.KITCHEN ->
                    StaffAlertKind.NEW_KITCHEN_ORDER.takeIf {
                        summary.state == OrderState.PENDING_PAYMENT || summary.state == OrderState.PAID
                    }
                OperationalRole.CASHIER ->
                    when {
                        summary.state == OrderState.PENDING_PAYMENT && summary.needsCashCollection ->
                            StaffAlertKind.CASH_DUE
                        summary.state == OrderState.READY && summary.destination == OrderDestination.TAKE_AWAY ->
                            StaffAlertKind.READY_FOR_PICKUP
                        else -> null
                    }
                else -> null
            }
        kind?.let { StaffAlert(it, summary.id, summary.folio) }
    }

/** El pedido que se tocó ya no está como se veía: cancelado, expirado, cobrado por otro o con otra versión. */
internal fun isStaleOrderError(error: Throwable): Boolean =
    generateSequence(error) { it.cause }.any { cause ->
        (cause is ApiClientException && cause.httpStatus in setOf(403, 404, 409)) ||
            (cause is OrderRepositoryException && cause.code in STALE_ORDER_CODES)
    }

private val STALE_ORDER_CODES =
    setOf(
        "ORDER_INVALID_STATE",
        "ORDER_NOT_FOUND",
        "ORDER_VERSION_CONFLICT",
        "ORDER_ALREADY_PAID",
        "ORDER_PAYMENT_PENDING",
    )

/** El error de una acción del personal; la versión desactualizada se dice como lo que es, sin código. */
internal fun staffMutationMessage(error: Throwable): String {
    val conflict =
        generateSequence(error) { it.cause }.any {
            (it as? OrderRepositoryException)?.code == "ORDER_VERSION_CONFLICT" ||
                (it as? ApiClientException)?.code == "ORDER_VERSION_CONFLICT"
        }
    return if (conflict) {
        "El pedido cambió mientras lo tenías abierto. Ya lo actualizamos: revísalo e inténtalo de nuevo."
    } else {
        error.toUserFacingMessage()
    }
}

/** Lo que Caja dice al cobrar: el folio y el cambio, que el cajero necesita mientras cuenta el efectivo. */
internal fun cashCollectionNotice(receipt: CashCollectionResult): String {
    val change = runCatching { Money.parse(receipt.change) }.getOrNull()
    val folio = receipt.order.summary.folio
    return if (change != null && change > BigDecimal.ZERO) {
        "Pedido #$folio cobrado · Cambio: ${moneyLabel(receipt.change)}"
    } else {
        "Pedido #$folio cobrado · Sin cambio"
    }
}

/** El servidor devuelve a lo más esta cantidad por página; una lista de ese tamaño puede estar cortada. */
internal const val FULL_LIST_LIMIT = 50

/**
 * Una recarga rechazada por el servidor (4xx) nunca abonó saldo: el registro
 * pendiente se puede descartar. Errores de red/timeout/5xx son ambiguos — el
 * POST pudo haber llegado — así que el registro se conserva para el replay.
 */
internal fun isDefinitiveWalletReloadFailure(error: Throwable): Boolean =
    (error as? WalletRepositoryException)?.httpStatus.let { status ->
        status != null && status in 400..499 && status != HTTP_REQUEST_TIMEOUT && status != HTTP_TOO_MANY_REQUESTS
    }

private const val HTTP_REQUEST_TIMEOUT = 408
private const val HTTP_TOO_MANY_REQUESTS = 429

/** Una recarga persistida de otro turno ya no se repite sola: hace falta que el cajero la reconozca. */
internal fun isStalePendingReload(
    pending: PendingWalletReload,
    nowMs: Long,
): Boolean = pending.storedAtEpochMs <= 0L || nowMs - pending.storedAtEpochMs > PENDING_RELOAD_AUTO_RETRY_MS

private const val PENDING_RELOAD_AUTO_RETRY_MS = 30 * 60_000L

private fun pendingReloadLabel(pending: PendingWalletReload): String =
    moneyLabel(pending.amount) + (pending.userName?.let { " a $it" }.orEmpty())

/** Suma lo que llegó a lo que ya hay; un pedido que llega con versión anterior a la guardada no la pisa. */
internal fun mergeOrders(
    current: List<OrderDetail>,
    incoming: List<OrderDetail>,
): List<OrderDetail> {
    val byId = current.associateBy { it.summary.id }.toMutableMap()
    incoming.forEach { order -> byId[order.summary.id] = newerOrder(byId[order.summary.id], order) }
    return byId.values.toList()
}

/**
 * Lista completa del servidor: lo que ya no llega dejó de ser de este rol (cobrado por otro cajero, cancelado,
 * expirado, entregado) y se retira. Con la página llena la lista puede estar cortada, así que solo se suma.
 */
internal fun reconcileOrders(
    current: List<OrderDetail>,
    incoming: List<OrderDetail>,
): List<OrderDetail> {
    if (incoming.size >= FULL_LIST_LIMIT) return mergeOrders(current, incoming)
    val localById = current.associateBy { it.summary.id }
    return incoming.map { order -> newerOrder(localById[order.summary.id], order) }
}

private fun newerOrder(
    known: OrderDetail?,
    incoming: OrderDetail,
): OrderDetail = if (known != null && known.summary.version > incoming.summary.version) known else incoming

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

/** Confirmación de Cocina según el estado que devolvió el servidor; otro estado no confirma nada. */
internal fun kitchenActionNotice(order: OrderDetail): String? =
    when (order.summary.state) {
        OrderState.PREPARING -> "Comanda #${order.summary.folio} en preparación"
        OrderState.READY -> "Comanda #${order.summary.folio} lista"
        OrderState.CANCELED -> "Comanda #${order.summary.folio} rechazada"
        else -> null
    }

/** A client order moved forward in the tracking flow; surfaced as a system notification. */
data class OrderAdvance(
    val orderId: String,
    val folio: Int,
    val newState: OrderState,
)
