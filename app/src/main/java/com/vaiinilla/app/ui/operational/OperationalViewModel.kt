package com.vaiinilla.app.ui.operational

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.contract.UnreadableOrders
import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.MutationIdempotency
import com.vaiinilla.app.core.network.RetryKeys
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.core.notifications.DeviceTokenRegistrar
import com.vaiinilla.app.core.notifications.OrderPushes
import com.vaiinilla.app.core.runCatchingCancellable
import com.vaiinilla.app.core.security.JwtContextScope
import com.vaiinilla.app.core.security.SecureSessionStore
import com.vaiinilla.app.data.cashcut.CashCutRepository
import com.vaiinilla.app.data.guest.GuestCheckoutPolicy
import com.vaiinilla.app.data.guest.GuestOrderGateway
import com.vaiinilla.app.data.guest.GuestSessionStore
import com.vaiinilla.app.data.order.DismissedClientOrdersStore
import com.vaiinilla.app.data.wallet.PendingWalletReload
import com.vaiinilla.app.data.wallet.PendingWalletReloadStore
import com.vaiinilla.app.domain.model.CatalogProductDraft
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderTransitions
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.RefundOutcome
import com.vaiinilla.app.domain.model.cashAmountDue
import com.vaiinilla.app.domain.model.needsCashCollection
import com.vaiinilla.app.domain.repository.CallReason
import com.vaiinilla.app.domain.repository.CashDrawerMovementType
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class OperationalViewModel
    @Inject
    constructor(
        private val listOrders: ListOrdersUseCase,
        private val getOrder: GetOrderUseCase,
        private val transitionOrder: TransitionOrderUseCase,
        private val cancelOrder: CancelOrderUseCase,
        private val announceArrivalUseCase: AnnounceArrivalUseCase,
        private val heartbeatRepository: DeviceHeartbeatRepository,
        private val deviceIdentity: DeviceIdentity,
        private val catalogRepository: CatalogRepository,
        private val dismissedClientOrdersStore: DismissedClientOrdersStore,
        private val deviceTokenRegistrar: DeviceTokenRegistrar,
        private val waiterRepository: WaiterRepository,
        private val guestOrders: GuestOrderGateway,
        private val guestSessionStore: GuestSessionStore,
        private val guestCheckoutPolicy: GuestCheckoutPolicy,
        private val sessionStore: SecureSessionStore,
    ) : ViewModel(),
        OperationalPresence {
        private val _uiState = mutableStateOf(OperationalUiState())
        val uiState: State<OperationalUiState> = _uiState

        private var pollingJob: Job? = null
        private var lastUpdatedSince: String? = null
        private var roleGeneration: Long = 0
        private var dismissedClientOrderIds: Set<String> = dismissedClientOrdersStore.read()
        private var mutationJob: Job? = null

        // Un solo consumidor (AppNavHost) que puede suscribirse unos fotogramas tarde: un canal con búfer guarda
        // el aviso hasta que llega; un SharedFlow sin repetición lo descartaba (FLOW-08).
        private val orderAdvances = Channel<OrderAdvance>(Channel.BUFFERED)
        val orderAdvanceEvents: Flow<OrderAdvance> = orderAdvances.receiveAsFlow()
        private var clientTrackingBaselineReady = false
        private var mutationErrorVisible = false

        /** Abrir/cerrar caja, crear producto o llamar mesero: el reintento reusa la llave hasta que confirman. */
        private val retryKeys = RetryKeys()
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
        override val staffAlerts: SharedFlow<StaffAlert> = _staffAlerts

        // El negocio del último rol puesto; cambiar de negocio con el mismo rol limpia como cambiar de rol.
        private var roleEstablishmentId: String? = null

        // El negocio del JWT con que se leyeron los pedidos del cliente. Con cuenta, al entrar a otro lugar el JWT
        // cambia después de poner el rol: el pedido del anterior («En la cancha · Pista 3») se quedaba (#10).
        private var clientOrdersEstablishmentId: String? = null
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
            val establishmentId = sessionStore.readAccessToken()?.let(JwtContextScope::establishmentId)
            val establishmentChanged = isOtherEstablishment(roleEstablishmentId, establishmentId)
            val previousEstablishmentId = roleEstablishmentId
            if (establishmentId != null) roleEstablishmentId = establishmentId
            // Caja de otro negocio con el mismo rol: los pedidos y el aviso «Renta … cobrada» eran del anterior.
            val roleChanged = _uiState.value.role != role || establishmentChanged
            val previousState = _uiState.value
            if (roleChanged) {
                // De Caja a cliente y de vuelta, sin pasar por Modos: también se recuerda lo último de Caja.
                snapshotStaffState(previousEstablishmentId)
                roleGeneration += 1
                lastUpdatedSince = null
                mutationJob?.cancel()
                mutationJob = null
                clientTrackingBaselineReady = false
                errorClearJob?.cancel()
                errorClearJob = null
                mutationErrorVisible = false
                resetStaffSync()
                // Los pedidos que el rol anterior no pudo leer no son de este rol: la franja se recalcula.
                UnreadableOrders.clear()
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
                    cashDrawer = CashDrawerUi(),
                    cashChangeNotice = if (establishmentChanged) null else _uiState.value.cashChangeNotice,
                )
            // Con el mismo rol se conserva lo último conocido mientras se vuelve a pedir: borrarlo dejaba Caja en
            // «Revisando la caja» y quitaba «Cerrar caja» del menú hasta que contestara el servidor (en vivo, USAGI).
            val known = if (roleChanged) takeStaffSnapshot(role, roleEstablishmentId) else previousState
            if (known != null && role != OperationalRole.CLIENT && role != OperationalRole.ADMIN) {
                _uiState.value = _uiState.value.keepKnownStaffState(known, restoreOrders = roleChanged)
            }
            // Administración tiene su propia pantalla y su propio ViewModel: aquí no se sondean pedidos.
            if (role == OperationalRole.ADMIN) {
                pollingJob?.cancel()
                return
            }
            refresh()
            startPolling()
            // Registra el token FCM apenas hay contexto: cliente recibe avances
            // de su pedido y staff recibe alertas de su establecimiento.
            deviceTokenRegistrar.register()
        }

        /** El JWT de cliente es de otro negocio que el de la lista: lo del anterior se suelta antes de leer. */
        private fun dropClientOrdersOfAnotherEstablishment() {
            val current = sessionStore.readAccessToken()?.let(JwtContextScope::establishmentId)
            val previous = clientOrdersEstablishmentId
            if (current != null) clientOrdersEstablishmentId = current
            if (!isOtherEstablishment(previous, current)) return
            _uiState.value =
                _uiState.value.copy(
                    orders = emptyList(),
                    latestClientOrder = null,
                    menuOrder = null,
                    selectedOrderId = null,
                )
        }

        /**
         * Lo último que se supo de un rol de personal al soltarlo. Volver al mismo rol y negocio poco después (Atrás
         * a Por cobrar, reabrir el modo) arranca con esto en vez de «Revisando la caja» y «Cargando pedidos», y sin
         * perder «Cerrar caja»; la primera carga trae la lista completa y la concilia.
         */
        private var lastStaffState: StaffStateSnapshot? = null

        /** Suelta el rol sin tirar lo que se recuerda del turno: el que entra a otro modo no pierde su caja. */
        fun releaseRole() {
            snapshotStaffState()
            roleGeneration += 1
            pollingJob?.cancel()
            pollingJob = null
            // La mutación en vuelo NO se cancela: su POST ya salió y el cobro de Caja deja su aviso para
            // cuando el cajero vuelva; las demás se descartan al ver que cambió la generación.
            heartbeatCoordinator.stop()
            resetStaffSync()
            lastUpdatedSince = null
            clientTrackingBaselineReady = false
            mutationErrorVisible = false
            errorClearJob?.cancel()
            errorClearJob = null
            UnreadableOrders.clear()
            _uiState.value = OperationalUiState()
        }

        /** Vuelve a Modos. */
        fun clearRole() = releaseRole()

        private fun snapshotStaffState(establishmentId: String? = roleEstablishmentId) {
            val current = _uiState.value
            val role = current.role ?: return
            if (role == OperationalRole.CLIENT || role == OperationalRole.ADMIN) return
            // Solo lo que de verdad se cargó: un estado a medias no ahorra nada.
            if (current.lastRefreshOk != true && current.cashSessionOpen == null) return
            lastStaffState =
                StaffStateSnapshot(
                    role = role,
                    establishmentId = establishmentId,
                    takenAtMs = System.currentTimeMillis(),
                    state = current,
                )
        }

        /** La foto del mismo rol y negocio, si es reciente; se usa una sola vez. */
        private fun takeStaffSnapshot(
            role: OperationalRole,
            establishmentId: String?,
        ): OperationalUiState? {
            val snapshot = lastStaffState ?: return null
            if (!snapshot.matches(role, establishmentId, System.currentTimeMillis())) return null
            lastStaffState = null
            return snapshot.state
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

        private var kitchenPolicyLoaded = false

        private fun refreshKitchenPolicy(generation: Long) {
            kitchenPolicyLoaded = true
            viewModelScope.launch {
                val status = withContext(Dispatchers.IO) { catalogRepository.getOperationalStatus() }.getOrNull()
                if (generation != roleGeneration || _uiState.value.role != OperationalRole.KITCHEN) return@launch
                if (status == null) {
                    // Sin respuesta se vuelve a intentar en el siguiente sondeo; se queda lo último que se supo.
                    kitchenPolicyLoaded = false
                    return@launch
                }
                _uiState.value = _uiState.value.copy(kitchenPreparesUnpaid = status.kitchenPreparesUnpaid)
            }
        }

        fun refresh() {
            val role = _uiState.value.role ?: return
            val generation = roleGeneration
            if (role == OperationalRole.CLIENT) dropClientOrdersOfAnotherEstablishment()
            val pollEstablishment = clientOrdersEstablishmentId
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
            // Cocina relee si el negocio prepara lo que falta cobrar (R14): al entrar y con cada sincronización
            // completa, no en cada sondeo.
            if (role == OperationalRole.KITCHEN && (fullSync || !kitchenPolicyLoaded)) refreshKitchenPolicy(generation)
            viewModelScope.launch {
                // Client orders always fetch the full list (like iOS): the delta can miss
                // transitions that don't bump updatedAt. Staff keeps the delta.
                val since = if (role == OperationalRole.CLIENT || fullSync) null else lastUpdatedSince
                val result = withContext(Dispatchers.IO) { loadOrders(role, since) }
                if (generation != roleGeneration || _uiState.value.role != role) return@launch
                // Salió con el JWT del negocio anterior y ya se cambió: traería sus pedidos de vuelta.
                if (role == OperationalRole.CLIENT && pollEstablishment != clientOrdersEstablishmentId) return@launch
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
                        // El pedido abierto que la lista ya no trae no se suelta: se pide por su id, así su página
                        // sigue al servidor (una renta cobrada en Caja dejaba de verse en «Por pagar»; en vivo, D03).
                        val keptSelected =
                            _uiState.value.selectedOrder
                                ?.takeIf { role == OperationalRole.CLIENT }
                                ?.takeIf { selected -> orders.none { it.summary.id == selected.summary.id } }
                        // Full-list fetch for CLIENT replaces state (iOS parity); staff merges deltas.
                        val merged =
                            when {
                                role == OperationalRole.CLIENT ->
                                    keepNewerVersions(_uiState.value.orders, orders) + listOfNotNull(keptSelected)
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
                            lastUpdatedSince = deltaCursor(newest)
                        }
                        val visibleOrders =
                            filterDismissedClientOrders(
                                role = role,
                                orders = merged,
                                dismissedOrderIds = dismissedClientOrderIds,
                            )
                        if (role == OperationalRole.KITCHEN) {
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
                        keptSelected?.takeUnless { it.summary.state.isClosed }?.let { refreshOrder(it.summary.id) }
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
            kitchenPolicyLoaded = false
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
                    if (announcesClientAdvance(before, after)) {
                        orderAdvances.trySend(
                            OrderAdvance(
                                orderId = order.summary.id,
                                folio = order.summary.folio,
                                newState = after,
                                inSpace = order.summary.destination == OrderDestination.IN_SPACE,
                                spaceType = order.summary.space?.type,
                                acceptedAwaitingPayment = acceptedAwaitingPayment(before, after),
                                pickUpAtBar = order.summary.pickUpAtBar,
                                homeDelivery = order.summary.destination == OrderDestination.HOME_DELIVERY,
                                rental = order.reservation != null,
                                rentalSpaceName = order.reservation?.space?.name,
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

        /**
         * El pedido que se ve ya no está en un estado desde el que se pueda pedir [target] (otro dispositivo lo
         * movió): se avisa y se pide la lista completa en vez de mandar una petición que el servidor rechazaría.
         * Si el pedido no está en la lista no se sabe, y decide el servidor.
         */
        private fun staleForTransition(
            orderId: String,
            target: OrderState,
        ): Boolean {
            val current = _uiState.value.orders.firstOrNull { it.summary.id == orderId } ?: return false
            if (OrderTransitions.canRequest(current.summary.state, target)) return false
            showMutationError("Ese pedido ya cambió de estado. Actualizando la lista…")
            requestFullSync()
            return true
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
            if (staleForTransition(orderId, OrderState.PREPARING)) return
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
            performMutation(successNotice = ::itemRejectedNotice) {
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
            if (staleForTransition(orderId, OrderState.READY)) return
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
            if (staleForTransition(orderId, OrderState.DELIVERED)) return
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
                OrderState.AWAITING_ACCEPTANCE -> "El negocio está revisando tu encargo. Te avisamos al aceptarlo."
                OrderState.ON_THE_WAY ->
                    order.homeDelivery?.code?.let { "Va en camino. Dicta el código $it al recibirlo." }
                        ?: "Tu pedido va en camino."
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

        private fun startPolling() {
            pollingJob?.cancel()
            pollingJob =
                viewModelScope.launch {
                    while (isActive) {
                        // El alumno sigue su pedido al ritmo de siempre; el personal baja a un minuto minimizado.
                        // Un push de pedido adelanta la consulta.
                        OrderPushes.awaitNextPoll {
                            if (_uiState.value.role == OperationalRole.CLIENT) {
                                delay(POLL_INTERVAL_MS)
                            } else {
                                awaitNextStaffPoll(POLL_INTERVAL_MS)
                            }
                        }
                        refresh()
                    }
                }
        }

        override fun onOperationalForeground(role: OperationalRole) {
            if (_uiState.value.role != role) return
            heartbeatCoordinator.start(role)
            _uiState.value = _uiState.value.copy(heartbeatOnline = false)
            // Al volver de segundo plano pudo pasar de todo: se concilia la lista completa.
            if (role != OperationalRole.CLIENT && role != OperationalRole.ADMIN) requestFullSync()
        }

        override fun onOperationalBackground(role: OperationalRole) {
            if (_uiState.value.role != role) return
            heartbeatCoordinator.pause()
            _uiState.value = _uiState.value.copy(heartbeatOnline = null)
        }

        fun onRuntimeModeChanged() {
            if (_uiState.value.role == null) return
            refresh()
        }

        fun refreshCatalog() {
            if (_uiState.value.role == OperationalRole.KITCHEN) refreshKitchenCatalog()
        }

        /** N03: Cocina lee el menú para apagar lo que se acabó; un fallo se queda en la hoja y no toca la fila de comandas. */
        private fun refreshKitchenCatalog() {
            val generation = roleGeneration
            _uiState.value = _uiState.value.copy(catalogFailed = false)
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { catalogRepository.getCatalog() }
                if (generation != roleGeneration || _uiState.value.role != OperationalRole.KITCHEN) return@launch
                _uiState.value =
                    _uiState.value.copy(
                        catalog = result.getOrNull() ?: _uiState.value.catalog,
                        catalogFailed = result.isFailure,
                    )
            }
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
                val signature = arrayOf<Any?>("call-waiter", espacioId, reason, orderId)
                waiterRepository
                    .call(espacioId, reason, orderId, retryKeys.keyFor(*signature))
                    .onSuccess { retryKeys.forget(*signature) }
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
                        summary.state == OrderState.READY &&
                            (summary.destination == OrderDestination.TAKE_AWAY || summary.pickUpAtBar) ->
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

/** Otro negocio solo si los dos se conocen: un token sin el claim no cuenta como cambio. */
internal fun isOtherEstablishment(
    previous: String?,
    current: String?,
): Boolean = previous != null && current != null && previous != current

/** El servidor devuelve a lo más esta cantidad por página; una lista de ese tamaño puede estar cortada. */
internal const val FULL_LIST_LIMIT = 50

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
    return keepNewerVersions(current, incoming)
}

/**
 * Lista completa del cliente: manda el servidor, salvo un pedido que aquí ya va en una versión más nueva (una
 * respuesta lenta que salió antes del último cambio no lo regresa a su estado anterior).
 */
internal fun keepNewerVersions(
    current: List<OrderDetail>,
    incoming: List<OrderDetail>,
): List<OrderDetail> {
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
): OrderDetail? = latest?.takeUnless { it.summary.id in dismissedOrderIds || it.summary.state.isClosed }

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

/**
 * `actualizado_en` es la hora del inicio de la transacción: una que confirma tarde trae un sello anterior al que
 * ya tenemos y un cursor exacto la saltaría para siempre. Se pide con un margen; mezclar es idempotente (STAFF-11).
 */
internal fun deltaCursor(newest: String): String =
    runCatching { Instant.parse(newest).minus(DELTA_CURSOR_MARGIN).toString() }.getOrDefault(newest)

private val DELTA_CURSOR_MARGIN: Duration = Duration.ofSeconds(30)

/** Confirmación de Cocina según el estado que devolvió el servidor; otro estado no confirma nada. */
internal fun kitchenActionNotice(order: OrderDetail): String? =
    when (order.summary.state) {
        OrderState.PREPARING -> "Comanda #${order.summary.folio} en preparación"
        OrderState.READY -> "Comanda #${order.summary.folio} lista"
        OrderState.CANCELED -> withRefund("Comanda #${order.summary.folio} rechazada", order.refund)
        else -> null
    }

internal fun itemRejectedNotice(order: OrderDetail): String =
    withRefund("Se quitó un artículo de la comanda #${order.summary.folio}", order.refund)

/** Lo que Caja debe hacer con el dinero del pedido o artículo ya pagado que se rechazó (CAJA-06). */
private fun withRefund(
    base: String,
    refund: RefundOutcome?,
): String {
    val action =
        when {
            refund == null -> null
            refund.cashHandedBack != null -> "Entrega ${moneyLabel(refund.cashHandedBack)} en efectivo al cliente."
            refund.pendingForCashier != null ->
                "Devolución pendiente de ${moneyLabel(refund.pendingForCashier)}: confírmala en Devoluciones."
            refund.returnedToBalance != null -> "Se regresaron ${moneyLabel(refund.returnedToBalance)} a su saldo."
            refund.cardRefund -> "El reembolso a su tarjeta ya está en camino."
            else -> null
        }
    return if (action == null) base else "$base. $action"
}

/** A client order moved forward in the tracking flow; surfaced as a system notification. */
data class OrderAdvance(
    val orderId: String,
    val folio: Int,
    val newState: OrderState,
    val inSpace: Boolean = false,
    val spaceType: String? = null,
    /** El negocio aceptó el encargo y ahora le toca pagar al cliente. */
    val acceptedAwaitingPayment: Boolean = false,
    /** «Pasa por él»: listo dice que pase a la barra, no que va a su lugar. */
    val pickUpAtBar: Boolean = false,
    /** A domicilio: listo dice que está por salir, no que se recoge en la barra. */
    val homeDelivery: Boolean = false,
    /** Renta de cancha: el aviso habla del horario confirmado, no de un pedido entregado. */
    val rental: Boolean = false,
    val rentalSpaceName: String? = null,
)

/**
 * Si un cambio de estado del pedido merece aviso al cliente: cuando avanza en la línea de tiempo, cuando termina
 * sin entrega, o cuando le pide algo aunque la línea no se mueva. Aceptar un encargo (`por_aceptar → por_cobrar`)
 * y que un domicilio salga (`listo → en_camino`) ocupan el mismo paso de la línea, así que solo con el índice no
 * se avisaban.
 */
internal fun announcesClientAdvance(
    before: OrderState,
    after: OrderState,
): Boolean {
    if (before == after) return false
    return after.trackingIndex > before.trackingIndex ||
        after.isTerminalWithoutDelivery ||
        acceptedAwaitingPayment(before, after) ||
        (before == OrderState.READY && after == OrderState.ON_THE_WAY)
}

internal fun acceptedAwaitingPayment(
    before: OrderState,
    after: OrderState,
): Boolean = before == OrderState.AWAITING_ACCEPTANCE && after == OrderState.PENDING_PAYMENT
