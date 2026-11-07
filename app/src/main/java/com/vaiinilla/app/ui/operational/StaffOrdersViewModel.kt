package com.vaiinilla.app.ui.operational

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.contract.UnreadableOrders
import com.vaiinilla.app.core.network.ApiErrorMapper
import com.vaiinilla.app.core.network.MutationIdempotency
import com.vaiinilla.app.core.network.RetryKeys
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.core.notifications.DeviceTokenRegistrar
import com.vaiinilla.app.core.notifications.OrderPushes
import com.vaiinilla.app.core.runCatchingCancellable
import com.vaiinilla.app.core.security.JwtContextScope
import com.vaiinilla.app.core.security.SecureSessionStore
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderTransitions
import com.vaiinilla.app.domain.repository.CatalogRepository
import com.vaiinilla.app.domain.repository.DeviceHeartbeatRepository
import com.vaiinilla.app.domain.repository.DeviceIdentity
import com.vaiinilla.app.domain.usecase.CancelOrderUseCase
import com.vaiinilla.app.domain.usecase.GetOrderUseCase
import com.vaiinilla.app.domain.usecase.ListOrdersUseCase
import com.vaiinilla.app.domain.usecase.TransitionOrderUseCase
import com.vaiinilla.app.ui.discovery.QrPayload
import com.vaiinilla.app.ui.discovery.QrPayloadParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/** Lo que la pantalla de un rol de personal necesita saber de su ViewModel para sonar y marcar presencia. */
interface OperationalPresence {
    val staffAlerts: SharedFlow<StaffAlert>

    fun onOperationalForeground(role: OperationalRole)

    fun onOperationalBackground(role: OperationalRole)
}

/**
 * Lo común de Caja, Cocina y Mesero: la fila de pedidos con su sondeo y su conciliación completa, los avisos, las
 * acciones sobre un pedido y la presencia del dispositivo. Cada rol es su propio ViewModel con su propio estado; lo
 * que solo es de un rol (el cajón, el menú de Cocina) vive en su subclase.
 */
abstract class StaffOrdersViewModel(
    protected val role: OperationalRole,
    protected val listOrders: ListOrdersUseCase,
    protected val getOrder: GetOrderUseCase,
    protected val transitionOrder: TransitionOrderUseCase,
    protected val cancelOrder: CancelOrderUseCase,
    heartbeatRepository: DeviceHeartbeatRepository,
    deviceIdentity: DeviceIdentity,
    private val deviceTokenRegistrar: DeviceTokenRegistrar,
    private val sessionStore: SecureSessionStore,
    protected val catalogRepository: CatalogRepository,
) : ViewModel(),
    OperationalPresence {
    protected val state = mutableStateOf(OperationalUiState())
    val uiState: State<OperationalUiState> = state

    private var pollingJob: Job? = null
    private var lastUpdatedSince: String? = null
    protected var roleGeneration: Long = 0
        private set
    protected var mutationJob: Job? = null
    protected var mutationErrorVisible = false

    /** Abrir/cerrar caja, crear producto: el reintento reusa la llave hasta que confirman. */
    protected val retryKeys = RetryKeys()
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
    protected var roleEstablishmentId: String? = null
        private set

    /**
     * Lo último que se supo del rol al soltarlo. Volver al mismo rol y negocio poco después (Atrás a Por cobrar,
     * reabrir el modo) arranca con esto en vez de «Revisando la caja» y «Cargando pedidos»; la primera carga trae la
     * lista completa y la concilia.
     */
    private var lastStaffState: StaffStateSnapshot? = null
    private val heartbeatCoordinator =
        OperationalHeartbeatCoordinator(
            scope = viewModelScope,
            repository = heartbeatRepository,
            deviceId = deviceIdentity::id,
            onResult = { online ->
                if (state.value.role != null) {
                    state.value = state.value.copy(heartbeatOnline = online)
                }
            },
        )

    /** Pone el rol (o lo reanuda) y empieza a sondear. Llamarlo de nuevo con el mismo rol y negocio solo refresca. */
    fun start() {
        val establishmentId = sessionStore.readAccessToken()?.let(JwtContextScope::establishmentId)
        val establishmentChanged = isOtherEstablishment(roleEstablishmentId, establishmentId)
        val previousEstablishmentId = roleEstablishmentId
        if (establishmentId != null) roleEstablishmentId = establishmentId
        // Caja de otro negocio con el mismo rol: los pedidos y el aviso «Renta … cobrada» eran del anterior.
        val roleChanged = state.value.role != role || establishmentChanged
        val previousState = state.value
        if (roleChanged) {
            snapshotStaffState(previousEstablishmentId)
            roleGeneration += 1
            lastUpdatedSince = null
            mutationJob?.cancel()
            mutationJob = null
            errorClearJob?.cancel()
            errorClearJob = null
            mutationErrorVisible = false
            onRoleReset(establishmentChanged)
            resetStaffSync()
            // Los pedidos que otro rol no pudo leer no son de este rol: la franja se recalcula.
            UnreadableOrders.clear()
        }
        state.value =
            state.value.copy(
                role = role,
                // La mutación del rol anterior se cancela y su resultado se descarta: no puede dejar
                // la bandera pegada y apagar todos los botones del rol nuevo.
                acting = if (roleChanged) false else state.value.acting,
                catalogBusy = if (roleChanged) false else state.value.catalogBusy,
                cashSessionCheckFailed = false,
                orders = if (roleChanged) emptyList() else state.value.orders,
                latestClientOrder = null,
                menuOrder = null,
                selectedOrderId = null,
                errorMessage = null,
                queue = if (roleChanged) QueueSync.Pending else state.value.queue,
                cashSessionOpen = null,
                walletClients = emptyList(),
                walletSearchLoading = false,
                walletReloadReceipt = null,
                pendingWalletReload = null,
                catalog = null,
                cashDrawer = CashDrawerUi(),
                cashChangeNotice = if (establishmentChanged) null else state.value.cashChangeNotice,
            )
        // Con el mismo rol se conserva lo último conocido mientras se vuelve a pedir: borrarlo dejaba Caja en
        // «Revisando la caja» y quitaba «Cerrar caja» del menú hasta que contestara el servidor (en vivo, USAGI).
        val known = if (roleChanged) takeStaffSnapshot(role, roleEstablishmentId) else previousState
        if (known != null) {
            state.value = state.value.keepKnownStaffState(known, restoreOrders = roleChanged)
        }
        onRoleStarted()
        onPolled()
        refresh()
        onAfterFirstRefresh()
        startPolling()
        // Registra el token FCM apenas hay contexto: el personal recibe alertas de su establecimiento.
        deviceTokenRegistrar.register()
    }

    /** Sale del rol sin olvidar nada del turno (cambio de modo): lo último se recuerda para volver pronto. */
    fun release() {
        // Soltar un rol que nadie tiene no toca nada: la franja de pedidos ilegibles es global.
        if (state.value.role == null) return
        snapshotStaffState()
        roleGeneration += 1
        pollingJob?.cancel()
        pollingJob = null
        // La mutación en vuelo NO se cancela: su POST ya salió y el cobro de Caja deja su aviso para
        // cuando el cajero vuelva; las demás se descartan al ver que cambió la generación.
        heartbeatCoordinator.stop()
        resetStaffSync()
        lastUpdatedSince = null
        mutationErrorVisible = false
        errorClearJob?.cancel()
        errorClearJob = null
        UnreadableOrders.clear()
        onRoleReset(establishmentChanged = false)
        state.value = OperationalUiState()
    }

    /** Vuelve a Modos: además de soltar el rol, olvida lo que solo sirve dentro de él. */
    fun clearRole() {
        release()
        onRoleCleared()
    }

    /** El rol cambia de negocio o arranca de cero: sus subclases sueltan lo suyo (búsquedas, recarga pendiente). */
    protected open fun onRoleReset(establishmentChanged: Boolean) = Unit

    /** Ya con el estado puesto y antes de pedir pedidos. */
    protected open fun onRoleStarted() = Unit

    /** Relee lo que cada rol sondea además de los pedidos (Caja: si la caja está abierta). */
    protected open fun onPolled() = Unit

    /** Tras lanzar la primera lectura de pedidos (Caja: catálogo y recarga pendiente). */
    protected open fun onAfterFirstRefresh() = Unit

    protected open fun onRoleCleared() = Unit

    /** Cocina relee su política en cada sincronización completa. */
    protected open fun onRefreshStarted(
        fullSync: Boolean,
        generation: Long,
    ) = Unit

    /** Menú y fotos de cada rol; Mesero no lee catálogo aquí. */
    open fun refreshCatalog() = Unit

    private fun snapshotStaffState(establishmentId: String? = roleEstablishmentId) {
        val current = state.value
        if (current.role == null) return
        // Solo lo que de verdad se cargó: un estado a medias no ahorra nada.
        if (current.queue !is QueueSync.Live && current.cashSessionOpen == null) return
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
        state.value = state.value.copy(selectedOrderId = orderId)
    }

    fun refresh() {
        if (state.value.role == null) return
        val generation = roleGeneration
        // El fallo de la lectura vive en `queue` y se queda hasta que una lectura salga bien; `errorMessage` es solo de
        // acciones y se limpia por su propio reloj.
        state.value =
            state.value.copy(
                loading = true,
                errorMessage = state.value.errorMessage.takeIf { mutationErrorVisible },
            )
        val seq = ++refreshSeq
        // El personal pide cambios, pero el servidor no avisa de lo que dejó de ser suyo (cobrado por otro
        // cajero, cancelado, expirado): cada cierto tiempo se pide la lista completa y se concilia.
        val fullSync = isFullSyncDue()
        onRefreshStarted(fullSync, generation)
        viewModelScope.launch {
            val since = if (fullSync) null else lastUpdatedSince
            val result = withContext(Dispatchers.IO) { listOrders(role, since) }
            if (generation != roleGeneration || state.value.role != role) return@launch
            // Una respuesta que salió antes de otra ya aplicada traería el estado viejo encima del nuevo.
            if (seq < appliedRefreshSeq) return@launch
            appliedRefreshSeq = seq
            result.fold(
                onSuccess = { orders ->
                    val merged =
                        if (fullSync) {
                            reconcileOrders(state.value.orders, orders)
                        } else {
                            mergeOrders(state.value.orders, orders)
                        }
                    if (fullSync) {
                        pollsSinceFullSync = 0
                        fullSyncRequested = false
                    }
                    val newest = merged.maxOfOrNull { it.summary.updatedAt }
                    if (newest != null) {
                        lastUpdatedSince = deltaCursor(newest)
                    }
                    if (role == OperationalRole.CASHIER || role == OperationalRole.KITCHEN) {
                        announceStaffOrders(role, merged)
                    }
                    state.value =
                        state.value.copy(
                            loading = false,
                            orders = merged.sortedByDescending { it.summary.updatedAt },
                            latestClientOrder = null,
                            menuOrder = null,
                            lastSyncedAt = newest,
                            queue = QueueSync.Live,
                            errorMessage = state.value.errorMessage.takeIf { mutationErrorVisible },
                        )
                },
                onFailure = { error ->
                    // Un fallo de lectura no borra el error de una acción que el personal aún está leyendo.
                    state.value =
                        state.value.copy(
                            loading = false,
                            queue = QueueSync.Failed(ApiErrorMapper.map(error)),
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
    protected fun requestFullSync() {
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

    fun refreshOrder(orderId: String) {
        val generation = roleGeneration
        viewModelScope.launch {
            withContext(Dispatchers.IO) { getOrder(orderId) }.onSuccess { order ->
                if (generation != roleGeneration) return@onSuccess
                val updated =
                    state.value.orders
                        .filterNot { it.summary.id == orderId } + order
                state.value =
                    state.value.copy(
                        orders = updated.sortedByDescending { it.summary.updatedAt },
                        latestClientOrder = null,
                        menuOrder = null,
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
    protected fun staleForTransition(
        orderId: String,
        target: OrderState,
    ): Boolean {
        val current = state.value.orders.firstOrNull { it.summary.id == orderId } ?: return false
        if (OrderTransitions.canRequest(current.summary.state, target)) return false
        showMutationError("Ese pedido ya cambió de estado. Actualizando la lista…")
        requestFullSync()
        return true
    }

    protected fun freshVersion(
        orderId: String,
        fallback: Int,
    ): Int =
        state.value.orders
            .firstOrNull { it.summary.id == orderId }
            ?.summary
            ?.version ?: fallback

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
        val freshest = state.value.orders.firstOrNull { it.summary.id == orderId }
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

    /** [successNotice] arma la confirmación con el pedido que devolvió el servidor, nunca antes. */
    protected fun performMutation(
        successNotice: (OrderDetail) -> String? = { null },
        block: () -> OrderDetail,
    ) {
        if (state.value.role == null) return
        if (state.value.acting) {
            // El personal tocó un botón que se ignoró: que lo sepa en vez de quedarse esperando.
            showMutationError("Espera un momento: hay otra acción en curso.")
            return
        }
        val generation = roleGeneration
        mutationErrorVisible = false
        state.value = state.value.copy(acting = true, errorMessage = null, actionNotice = null)
        mutationJob =
            viewModelScope.launch {
                val result = runCatchingCancellable { withContext(Dispatchers.IO) { block() } }
                if (generation != roleGeneration || state.value.role != role) return@launch
                result
                    .onSuccess { order ->
                        val updated =
                            state.value.orders
                                .filterNot { it.summary.id == order.summary.id } + order
                        state.value =
                            state.value.copy(
                                acting = false,
                                orders = updated.sortedByDescending { it.summary.updatedAt },
                                selectedOrderId = order.summary.id,
                                actionNotice = successNotice(order),
                            )
                    }.onFailure { error ->
                        state.value = state.value.copy(acting = false)
                        showMutationError(staffMutationMessage(error))
                        // Pedido cancelado, ya avanzado o de otro: la tarjeta que se ve ya no es la real.
                        if (isStaleOrderError(error)) requestFullSync()
                    }
            }
    }

    fun consumeActionNotice() {
        if (state.value.actionNotice != null) {
            state.value = state.value.copy(actionNotice = null)
        }
    }

    /**
     * Errores de acciones del usuario (cobrar, entregar, recargar) deben
     * sobrevivir al polling de 5s: si el refresh limpia errorMessage en su
     * siguiente ciclo, un cobro rechazado se ve como "no pasó nada". Los
     * errores de mutación quedan visibles ~9s; los errores de sync siguen
     * siendo transitorios y el próximo poll exitoso los limpia.
     */
    protected fun showMutationError(message: String) {
        mutationErrorVisible = true
        errorClearJob?.cancel()
        errorClearJob =
            viewModelScope.launch {
                delay(MUTATION_ERROR_VISIBLE_MS)
                if (mutationErrorVisible) {
                    mutationErrorVisible = false
                    state.value = state.value.copy(errorMessage = null)
                }
            }
        state.value = state.value.copy(errorMessage = message)
    }

    fun dismissMutationError() {
        mutationErrorVisible = false
        errorClearJob?.cancel()
        errorClearJob = null
        if (state.value.errorMessage != null) {
            state.value = state.value.copy(errorMessage = null)
        }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob =
            viewModelScope.launch {
                while (isActive) {
                    // El personal baja a un minuto minimizado; un push de pedido adelanta la consulta.
                    OrderPushes.awaitNextPoll { awaitNextStaffPoll(POLL_INTERVAL_MS) }
                    refresh()
                    onPolled()
                }
            }
    }

    override fun onOperationalForeground(role: OperationalRole) {
        if (state.value.role != role) return
        heartbeatCoordinator.start(role)
        state.value = state.value.copy(heartbeatOnline = false)
        // Al volver de segundo plano pudo pasar de todo: se concilia la lista completa.
        requestFullSync()
    }

    override fun onOperationalBackground(role: OperationalRole) {
        if (state.value.role != role) return
        heartbeatCoordinator.pause()
        state.value = state.value.copy(heartbeatOnline = null)
    }

    fun setProductAvailable(
        productId: Int,
        available: Boolean,
    ) {
        if (state.value.catalogBusy) return
        mutationErrorVisible = false
        state.value = state.value.copy(catalogBusy = true, errorMessage = null)
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
                    val catalog = state.value.catalog
                    state.value =
                        state.value.copy(
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
                    state.value = state.value.copy(catalogBusy = false)
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

    override fun onCleared() {
        pollingJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val POLL_INTERVAL_MS = 5_000L
        const val MUTATION_ERROR_VISIBLE_MS = 9_000L
        const val MIN_REJECTION_REASON = 3
        const val FULL_SYNC_EVERY_POLLS = 6
    }
}
