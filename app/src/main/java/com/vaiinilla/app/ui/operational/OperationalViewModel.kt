package com.vaiinilla.app.ui.operational

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.contract.UnreadableOrders
import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.RetryKeys
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.core.notifications.DeviceTokenRegistrar
import com.vaiinilla.app.core.notifications.OrderPushes
import com.vaiinilla.app.core.runCatchingCancellable
import com.vaiinilla.app.core.security.JwtContextScope
import com.vaiinilla.app.core.security.SecureSessionStore
import com.vaiinilla.app.data.guest.GuestCheckoutPolicy
import com.vaiinilla.app.data.guest.GuestOrderGateway
import com.vaiinilla.app.data.guest.GuestSessionStore
import com.vaiinilla.app.data.order.DismissedClientOrdersStore
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.RefundOutcome
import com.vaiinilla.app.domain.model.needsCashCollection
import com.vaiinilla.app.domain.repository.CallReason
import com.vaiinilla.app.domain.repository.OrderRepositoryException
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.domain.repository.WaiterRepository
import com.vaiinilla.app.domain.usecase.AnnounceArrivalUseCase
import com.vaiinilla.app.domain.usecase.GetOrderUseCase
import com.vaiinilla.app.domain.usecase.ListOrdersUseCase
import com.vaiinilla.app.ui.components.moneyLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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
        private val announceArrivalUseCase: AnnounceArrivalUseCase,
        private val dismissedClientOrdersStore: DismissedClientOrdersStore,
        private val deviceTokenRegistrar: DeviceTokenRegistrar,
        private val waiterRepository: WaiterRepository,
        private val guestOrders: GuestOrderGateway,
        private val guestSessionStore: GuestSessionStore,
        private val guestCheckoutPolicy: GuestCheckoutPolicy,
        private val sessionStore: SecureSessionStore,
    ) : ViewModel() {
        private val _uiState = mutableStateOf(OperationalUiState())
        val uiState: State<OperationalUiState> = _uiState

        private var pollingJob: Job? = null
        private var roleGeneration: Long = 0
        private var dismissedClientOrderIds: Set<String> = dismissedClientOrdersStore.read()
        private var mutationJob: Job? = null

        // Un solo consumidor (AppNavHost) que puede suscribirse unos fotogramas tarde: un canal con búfer guarda
        // el aviso hasta que llega; un SharedFlow sin repetición lo descartaba (FLOW-08).
        private val orderAdvances = Channel<OrderAdvance>(Channel.BUFFERED)
        val orderAdvanceEvents: Flow<OrderAdvance> = orderAdvances.receiveAsFlow()
        private var clientTrackingBaselineReady = false
        private var mutationErrorVisible = false

        /** Llamar al mesero: el reintento reusa la llave hasta que confirman. */
        private val retryKeys = RetryKeys()
        private var errorClearJob: Job? = null

        // Cada refresh() lleva su número: una respuesta más vieja que la ya aplicada se descarta.
        private var refreshSeq = 0L
        private var appliedRefreshSeq = 0L

        // El negocio del último rol puesto; cambiar de negocio con el mismo rol limpia como cambiar de rol.
        private var roleEstablishmentId: String? = null

        // El negocio del JWT con que se leyeron los pedidos del cliente. Con cuenta, al entrar a otro lugar el JWT
        // cambia después de poner el rol: el pedido del anterior («En la cancha · Pista 3») se quedaba (#10).
        private var clientOrdersEstablishmentId: String? = null

        /** Cliente o Administración; el personal de Caja, Cocina y Mesero tiene su propio ViewModel. */
        fun setRole(role: OperationalRole) {
            val establishmentId = sessionStore.readAccessToken()?.let(JwtContextScope::establishmentId)
            val establishmentChanged = isOtherEstablishment(roleEstablishmentId, establishmentId)
            if (establishmentId != null) roleEstablishmentId = establishmentId
            val roleChanged = _uiState.value.role != role || establishmentChanged
            if (roleChanged) {
                roleGeneration += 1
                mutationJob?.cancel()
                mutationJob = null
                clientTrackingBaselineReady = false
                errorClearJob?.cancel()
                errorClearJob = null
                mutationErrorVisible = false
                resetRefreshSeq()
                // Los pedidos que el rol anterior no pudo leer no son de este rol: la franja se recalcula.
                UnreadableOrders.clear()
            }
            _uiState.value =
                _uiState.value.copy(
                    role = role,
                    // La mutación del rol anterior se cancela y su resultado se descarta: no puede dejar
                    // la bandera pegada y apagar todos los botones del rol nuevo.
                    acting = if (roleChanged) false else _uiState.value.acting,
                    orders = if (roleChanged) emptyList() else _uiState.value.orders,
                    latestClientOrder = if (roleChanged) null else _uiState.value.latestClientOrder,
                    menuOrder = if (roleChanged) null else _uiState.value.menuOrder,
                    selectedOrderId = null,
                    errorMessage = null,
                    lastRefreshOk = if (roleChanged) null else _uiState.value.lastRefreshOk,
                )
            // Administración tiene su propia pantalla y su propio ViewModel: aquí no se sondean pedidos.
            if (role == OperationalRole.ADMIN) {
                pollingJob?.cancel()
                return
            }
            refresh()
            startPolling()
            // Registra el token FCM apenas hay contexto: el cliente recibe los avances de su pedido.
            deviceTokenRegistrar.register()
        }

        /** Suelta el rol (cambio de modo o vuelta a Modos): el cliente no recuerda nada del turno anterior. */
        fun releaseRole() {
            roleGeneration += 1
            pollingJob?.cancel()
            pollingJob = null
            clientTrackingBaselineReady = false
            mutationErrorVisible = false
            errorClearJob?.cancel()
            errorClearJob = null
            resetRefreshSeq()
            UnreadableOrders.clear()
            _uiState.value = OperationalUiState()
        }

        /** Vuelve a Modos. */
        fun clearRole() = releaseRole()

        private fun resetRefreshSeq() {
            refreshSeq = 0
            appliedRefreshSeq = 0
        }

        /** Pedidos del cliente: los de la cuenta o, sin cuenta, los del invitado en el negocio que mira. */
        private fun loadOrders(): Result<List<OrderDetail>> {
            val venue = guestSessionStore.readVenue()
            val slug = venue?.establishment?.slug
            if (slug == null || !guestCheckoutPolicy.allows(venue)) {
                return listOrders(OperationalRole.CLIENT, null).map(listOrders::withRecoveredPickupTokens)
            }
            if (!guestOrders.hasIdentity()) return Result.success(emptyList())
            return guestOrders.list(slug)
        }

        fun refresh() {
            val role = _uiState.value.role ?: return
            if (role != OperationalRole.CLIENT) return
            val generation = roleGeneration
            dropClientOrdersOfAnotherEstablishment()
            val pollEstablishment = clientOrdersEstablishmentId
            if (_uiState.value.orders.isEmpty()) {
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
            viewModelScope.launch {
                // El cliente siempre pide la lista completa (como iOS): el delta puede perder transiciones que
                // no mueven updatedAt.
                val result = withContext(Dispatchers.IO) { loadOrders() }
                if (generation != roleGeneration || _uiState.value.role != role) return@launch
                // Salió con el JWT del negocio anterior y ya se cambió: traería sus pedidos de vuelta.
                if (pollEstablishment != clientOrdersEstablishmentId) return@launch
                // Una respuesta que salió antes de otra ya aplicada traería el estado viejo encima del nuevo.
                if (seq < appliedRefreshSeq) return@launch
                appliedRefreshSeq = seq
                result.fold(
                    onSuccess = { orders ->
                        emitClientOrderAdvances(
                            previous = _uiState.value.orders,
                            incoming = orders,
                        )
                        // El pedido abierto que la lista ya no trae no se suelta: se pide por su id, así su página
                        // sigue al servidor (una renta cobrada en Caja dejaba de verse en «Por pagar»; en vivo, D03).
                        val keptSelected =
                            _uiState.value.selectedOrder
                                ?.takeIf { selected -> orders.none { it.summary.id == selected.summary.id } }
                        val merged = keepNewerVersions(_uiState.value.orders, orders) + listOfNotNull(keptSelected)
                        val latestClientOrder =
                            resolveLatestClientOrder(
                                previous = _uiState.value.latestClientOrder,
                                incoming = orders,
                            )
                        val newest = merged.maxOfOrNull { it.summary.updatedAt }
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
                                lastRefreshOk = true,
                                errorMessage = _uiState.value.errorMessage.takeIf { mutationErrorVisible },
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

        /** [block] devuelve el pedido que dejó el servidor; el cliente solo avisa que llegó (drive-thru). */
        private fun performMutation(block: () -> OrderDetail) {
            if (_uiState.value.role == null || _uiState.value.acting) return
            val role = _uiState.value.role
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
                                )
                        }.onFailure { error ->
                            _uiState.value = _uiState.value.copy(acting = false)
                            showMutationError(staffMutationMessage(error))
                            // Pedido cancelado, ya avanzado o de otro: la tarjeta que se ve ya no es la real.
                            if (isStaleOrderError(error)) refresh()
                        }
                }
        }

        private fun startPolling() {
            pollingJob?.cancel()
            pollingJob =
                viewModelScope.launch {
                    while (isActive) {
                        // El alumno sigue su pedido al ritmo de siempre; un push de pedido adelanta la consulta.
                        OrderPushes.awaitNextPoll { delay(POLL_INTERVAL_MS) }
                        refresh()
                    }
                }
        }

        override fun onCleared() {
            pollingJob?.cancel()
            super.onCleared()
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

        /** Drive-thru: el cliente avisa que ya llegó; Cocina y Caja lo ven en el pedido. */
        fun announceArrival(orderId: String) {
            performMutation { announceArrivalUseCase(orderId).getOrThrow() }
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
            const val POLL_INTERVAL_MS = 5_000L
            const val MUTATION_ERROR_VISIBLE_MS = 9_000L
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
