package com.vaiinilla.app.ui.operational

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderOrigin
import com.vaiinilla.app.domain.model.Reservation
import com.vaiinilla.app.domain.model.ReservationAccess
import com.vaiinilla.app.domain.model.ReservationPaymentMethod
import com.vaiinilla.app.domain.model.ReservationSlots
import com.vaiinilla.app.domain.model.SpaceDelivery
import com.vaiinilla.app.domain.model.Venue
import com.vaiinilla.app.domain.offline.OfflineStaffOrder
import com.vaiinilla.app.domain.offline.OfflineStaffOrderRules
import com.vaiinilla.app.domain.offline.OfflineStaffScope
import com.vaiinilla.app.domain.repository.AbonoMode
import com.vaiinilla.app.domain.repository.AbonoResult
import com.vaiinilla.app.domain.repository.AccountCollection
import com.vaiinilla.app.domain.repository.AccountPaymentMethod
import com.vaiinilla.app.domain.repository.BoardOrder
import com.vaiinilla.app.domain.repository.BoardTable
import com.vaiinilla.app.domain.repository.CallStatus
import com.vaiinilla.app.domain.repository.CatalogRepository
import com.vaiinilla.app.domain.repository.ReservationRepository
import com.vaiinilla.app.domain.repository.SpaceSessionDetail
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.domain.repository.TableSpace
import com.vaiinilla.app.domain.repository.WaiterBoard
import com.vaiinilla.app.domain.repository.WaiterRepository
import com.vaiinilla.app.domain.usecase.BuildCreateOrderRequestUseCase
import com.vaiinilla.app.domain.usecase.CreateRemoteOrderUseCase
import com.vaiinilla.app.domain.usecase.KITCHEN_NOTES_MAX
import com.vaiinilla.app.ui.order.CounterOrderKey
import com.vaiinilla.app.ui.order.counterOrderKeyFor
import com.vaiinilla.app.ui.order.createOrderFingerprint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * Lo elegido para rentar un espacio (U04): vive aquí y no en el picker, porque la hoja de la pista sale de
 * la composición mientras se cobra. Al cerrar el cobro sin cobrar, la duración y los jugadores regresan.
 */
data class RentalDraft(
    val spaceId: Int,
    val minutes: Int? = null,
    /** Boliche: cuántos juegan; 0 = sin anotar. */
    val players: Int = 0,
    /** #27: a nombre de quién; vacío = sin nombre. */
    val customerName: String = "",
)

data class WaiterUiState(
    val tables: List<BoardTable> = emptyList(),
    val callsEnabled: Boolean = true,
    val loading: Boolean = false,
    val acting: Boolean = false,
    /** El aviso actual es un error: se pinta con otro ícono para no parecer un éxito. */
    val toastIsError: Boolean = false,
    /**
     * Duraciones de renta que acepta el backend (`reservas/disponibilidad`). Vacía mientras no llegan: con otras
     * el backend responde 400, así que no se ofrecen unas supuestas (un boliche renta 1, 2 o 3 h, no turnos de pádel).
     */
    val rentalDurations: List<Int> = emptyList(),
    /** No se pudieron leer las duraciones: el picker ofrece reintentar. */
    val rentalDurationsFailed: Boolean = false,
    /** R25: cuántos caben en el espacio abierto (`capacidad` de la disponibilidad); sin dato, el tope del servidor. */
    val rentalMaxPlayers: Int = ReservationSlots.MAX_PLAYERS,
    val errorMessage: String? = null,
    val toastMessage: String? = null,
    val filterAttending: Boolean = false,
    /** Si es false, el mesero entrega en el espacio sin escanear el QR del cliente. */
    val deliveryRequiresQr: Boolean = true,
    /** El negocio deja pagar al final: el pedido de una mesa va a su cuenta y entra directo a cocina. */
    val allowsPayAtEnd: Boolean = false,
    /** Mesa a la que va el pedido que se está tomando; null = pedido de mostrador para Caja. */
    val counterOrderSpace: TableSpace? = null,
    /** «Te lo llevamos», «Pasa por él» o ambas, para los pedidos de un espacio. */
    val spaceDelivery: SpaceDelivery = SpaceDelivery.TABLE,
    /** En Caja, con las dos formas: el cliente pidió en la barra y pasa por él (true) o se lo llevan. */
    val counterPickUpAtBar: Boolean = false,
    /** Sesión, cuenta y estado del espacio abierto en la hoja; null si no hay ninguno. */
    val spaceDetail: SpaceSessionDetail? = null,
    val spaceDetailLoading: Boolean = false,
    /** Cambio de la última cuenta cobrada, para que se lo entregue al cliente. */
    val lastCollection: AccountCollection? = null,
    val lastAbono: AbonoResult? = null,
    /** Personas que faltan por pagar su parte, por espacio, tras un abono en partes iguales (I06). */
    val remainingParts: Map<Int, Int> = emptyMap(),
    /** Renta de mostrador apartada esperando que se cobre en efectivo. */
    val pendingRental: Reservation? = null,
    /** Lo elegido para rentar el espacio abierto; sobrevive a cerrar el cobro. */
    val rentalDraft: RentalDraft? = null,
    /** Pedido de mostrador: hoja abierta, catálogo cargado y envío en curso. */
    val counterOrderOpen: Boolean = false,
    val counterCatalog: Catalog? = null,
    val counterOrderSending: Boolean = false,
    val counterOrderError: String? = null,
    /** Cuándo llegó bien el tablero por última vez y cuántas cargas seguidas han fallado desde entonces. */
    val lastBoardRefreshAt: Instant? = null,
    val consecutiveBoardFailures: Int = 0,
    /** Pedidos tomados sin internet: por mandar y con problema (plan «Vaiinilla sin internet», fase 1). */
    val offlineOrders: OfflineStaffOrdersState = OfflineStaffOrdersState(),
    /** Espacio cuya hoja está abierta. Una pantalla nueva (Admin › Pedidos › Mesas) la abre desde aquí (R18). */
    val openSpaceId: Int? = null,
    /** Lo que el admin lleva armado en «Pedir»: sobrevive a cambiar de pestaña (R17). */
    val adminOrderDraft: StaffOrderDraft = StaffOrderDraft(),
) {
    /** Ya llegó al menos un tablero bueno. Las mesas solo entran con uno, así que también cuentan como prueba. */
    val boardLoaded: Boolean
        get() = lastBoardRefreshAt != null || tables.isNotEmpty()

    val boardLink: WaiterBoardLink
        get() = waiterBoardLink(boardLoaded, consecutiveBoardFailures)
}

/**
 * Pedido a medio armar del admin en «Pedir»: las líneas (por id de línea, ver [StaffOrderLines]), el nombre y a dónde
 * va. Vive en el ViewModel para no perderse al ir a otra pestaña y volver (R17).
 */
data class StaffOrderDraft(
    val lines: Map<Int, CartLine> = emptyMap(),
    val name: String = "",
    val space: TableSpace? = null,
)

@HiltViewModel
class WaiterViewModel
    @Inject
    constructor(
        private val waiterRepository: WaiterRepository,
        private val reservationRepository: ReservationRepository,
        private val catalogRepository: CatalogRepository,
        private val buildCreateOrderRequest: BuildCreateOrderRequestUseCase,
        private val createOrder: CreateRemoteOrderUseCase,
        private val offlineOrders: OfflineStaffOrderQueue,
        private val rescuer: OfflineStaffOrderRescuer,
    ) : ViewModel() {
        private val _uiState = mutableStateOf(WaiterUiState())
        val uiState: State<WaiterUiState> = _uiState

        private val _newCallEvents = MutableSharedFlow<TableCall>(extraBufferCapacity = 8)
        val newCallEvents: SharedFlow<TableCall> = _newCallEvents

        private var pollingJob: Job? = null
        private var boardJob: Job? = null
        private val pendingKeys = mutableMapOf<String, String>()
        private var openSpaceId: Int?
            get() = _uiState.value.openSpaceId
            set(value) {
                if (_uiState.value.openSpaceId != value) _uiState.value = _uiState.value.copy(openSpaceId = value)
            }
        private var knownCallIds: Set<String>? = null
        private var visible = false
        private var toastClearJob: Job? = null
        private var counterOrderKey: CounterOrderKey? = null

        private var contextKey: String? = null

        init {
            viewModelScope.launch {
                offlineOrders.state.collectLatest { queue ->
                    _uiState.value = _uiState.value.copy(offlineOrders = queue)
                }
            }
            offlineOrders.onSent = { localFolio, folio ->
                viewModelScope.launch { showToast("Ya volvió la red: el pedido $localFolio quedó como #$folio") }
            }
            offlineOrders.onAlreadyRegistered = { localFolio ->
                viewModelScope.launch { showToast("El pedido $localFolio sí había llegado; ya está en la lista") }
            }
        }

        /**
         * El tablero es de una membresía en un negocio. Al cambiar de negocio o de usuario se olvida todo: lo
         * conocido de uno hacía que las llamadas pendientes del otro se anunciaran como nuevas, y un refresco en
         * vuelo pintaba mesas ajenas (FLOW-24).
         */
        fun bindContext(key: String?) {
            // La cola sin conexión es de la membresía: cada mesero manda lo suyo con su sesión al volver la red.
            offlineOrders.start(key?.let { OfflineStaffScope(membershipId = it) })
            if (key == contextKey) return
            val previous = contextKey
            contextKey = key
            if (previous == null) return
            boardJob?.cancel()
            toastClearJob?.cancel()
            pendingKeys.clear()
            openSpaceId = null
            knownCallIds = null
            knownRejectionIds = null
            counterOrderKey = null
            _uiState.value = WaiterUiState()
            if (visible && key != null) refresh()
        }

        fun onVisible() {
            visible = true
            refresh()
            startPolling()
        }

        fun onHidden() {
            visible = false
            pollingJob?.cancel()
            pollingJob = null
        }

        fun setFilterAttending(attending: Boolean) {
            _uiState.value = _uiState.value.copy(filterAttending = attending)
        }

        /** Una carga del tablero a la vez: otra en vuelo se cancela para que la vieja no pise a la nueva (STAFF-19). */
        fun refresh() {
            _uiState.value = _uiState.value.copy(loading = true, errorMessage = null)
            boardJob?.cancel()
            boardJob = viewModelScope.launch { loadBoard() }
        }

        private suspend fun loadBoard() {
            val result = withContext(Dispatchers.IO) { waiterRepository.board() }
            result.fold(
                onSuccess = { board ->
                    _uiState.value = _uiState.value.withBoard(board, Instant.now())
                    detectNewCalls(board.tables)
                    detectNewRejections(board.tables)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.withBoardFailure(error.toUserFacingMessage())
                },
            )
        }

        private var knownRejectionIds: Set<String>? = null

        /**
         * Avisa cuando Cocina o Caja cancela un pedido de una mesa. La primera carga solo aprende lo que
         * ya había, para no repetir avisos viejos al abrir la app.
         */
        private fun detectNewRejections(tables: List<BoardTable>) {
            val all = tables.flatMap { table -> table.recentRejections.map { table.space to it } }
            val known = knownRejectionIds
            knownRejectionIds = all.map { it.second.id }.toSet()
            if (known == null) return
            val fresh = all.filter { it.second.id !in known }
            if (fresh.isEmpty()) return
            val (space, rejected) = fresh.first()
            val reason = rejected.reason?.let { ": $it" }.orEmpty()
            val extra = if (fresh.size > 1) " (y ${fresh.size - 1} más)" else ""
            showToast(
                "${rejected.byLabel} ${rejected.verb} el #${rejected.folio} de ${space.name}$reason$extra",
                isError = true,
            )
        }

        private fun detectNewCalls(tables: List<BoardTable>) {
            val pending = tables.mapNotNull { it.call }.filter { it.status == CallStatus.PENDIENTE }
            val known = knownCallIds
            if (known != null) {
                val fresh = pending.filter { it.id !in known }
                if (fresh.isNotEmpty()) {
                    fresh.forEach { _newCallEvents.tryEmit(it) }
                    val first = fresh.first()
                    showToast(
                        if (fresh.size == 1) {
                            "${first.space.name} te llama · ${first.reason.staffLabel}"
                        } else {
                            "${fresh.size} mesas te llaman · ${first.space.name} y ${fresh.size - 1} más"
                        },
                    )
                }
            }
            knownCallIds = pending.map { it.id }.toSet()
        }

        fun markGoing(call: TableCall) {
            if (!beginAction()) return
            viewModelScope.launch {
                val result =
                    actionCall {
                        waiterRepository.transitionCall(
                            call,
                            CallStatus.EN_CAMINO,
                            keyFor("voy", call.id, call.version),
                        )
                    }
                result.fold(
                    onSuccess = {
                        forgetKey("voy", call.id, call.version)
                        showToast("Vas a ${call.space.name}")
                        refresh()
                    },
                    onFailure = { error ->
                        showToast(error.toUserFacingMessage(), isError = true)
                        refresh()
                    },
                )
            }
        }

        fun markAttended(call: TableCall) {
            if (!beginAction()) return
            viewModelScope.launch {
                val result =
                    actionCall {
                        waiterRepository.transitionCall(
                            call,
                            CallStatus.ATENDIDA,
                            keyFor("atendida", call.id, call.version),
                        )
                    }
                result.fold(
                    onSuccess = {
                        forgetKey("atendida", call.id, call.version)
                        showToast("${call.space.name} atendida")
                        refresh()
                    },
                    onFailure = { error ->
                        showToast(error.toUserFacingMessage(), isError = true)
                        refresh()
                    },
                )
            }
        }

        fun deliver(
            order: BoardOrder,
            qrToken: String?,
        ) {
            val token = qrToken?.trim().orEmpty()
            val requiresQr = order.needsQrToDeliver(_uiState.value.deliveryRequiresQr)
            if (requiresQr && token.isEmpty()) {
                showToast("Pega el código del QR para confirmar la entrega.")
                return
            }
            if (!beginAction()) return
            viewModelScope.launch {
                val result =
                    actionCall {
                        waiterRepository.deliver(
                            order,
                            token.takeIf { requiresQr && it.isNotEmpty() },
                            keyFor("entregar", order.id, order.version),
                        )
                    }
                result.fold(
                    onSuccess = {
                        forgetKey("entregar", order.id, order.version)
                        showToast("#${order.folio} entregado")
                        refresh()
                        openSpaceId?.let { loadSpaceDetail(it, silent = true) }
                    },
                    onFailure = { error ->
                        showToast(error.toUserFacingMessage(), isError = true)
                    },
                )
            }
        }

        /** Abre el detalle de un espacio: su sesión, su cuenta y su turno. */
        fun openSpace(spaceId: Int) {
            openSpaceId = spaceId
            _uiState.value =
                _uiState.value.copy(
                    spaceDetail = null,
                    lastCollection = null,
                    lastAbono = null,
                    // Cada espacio tiene su mínimo: no se muestran las del anterior mientras llegan las suyas.
                    rentalDurations = emptyList(),
                    rentalDurationsFailed = false,
                    rentalMaxPlayers = ReservationSlots.MAX_PLAYERS,
                    rentalDraft = _uiState.value.rentalDraft?.takeIf { it.spaceId == spaceId },
                )
            loadSpaceDetail(spaceId, silent = false)
            loadRentalDurations()
            loadStationsCatalog()
        }

        /** N08: la estación de cada producto separa lo que se lleva ya de lo que hace Cocina; sin catálogo no se separa. */
        private fun loadStationsCatalog() {
            if (_uiState.value.counterCatalog != null) return
            viewModelScope.launch {
                val catalog =
                    withContext(Dispatchers.IO) { catalogRepository.getCatalog() }.getOrNull() ?: return@launch
                if (_uiState.value.counterCatalog ==
                    null
                ) {
                    _uiState.value = _uiState.value.copy(counterCatalog = catalog)
                }
            }
        }

        /**
         * Cada vez que se abre un espacio: el backend dice qué duraciones se pueden rentar (otras responden 400) y
         * el dueño puede cambiarlas en cualquier momento. Mientras llegan se conservan las últimas conocidas.
         */
        fun loadRentalDurations() {
            val spaceId = openSpaceId
            viewModelScope.launch {
                val day = withContext(Dispatchers.IO) { reservationRepository.day(null, ReservationAccess.Account) }
                day
                    .onSuccess { day ->
                        if (openSpaceId != spaceId) return@onSuccess
                        // Las del espacio abierto respetan su mínimo; el backend rechaza las demás.
                        val court = day.courts.firstOrNull { it.id == spaceId }
                        val durations = court?.durations?.takeIf { it.isNotEmpty() } ?: day.durations.sorted()
                        val maxPlayers = court?.maxPlayers ?: ReservationSlots.MAX_PLAYERS
                        _uiState.value =
                            _uiState.value.copy(
                                rentalDurations = durations,
                                rentalDurationsFailed = durations.isEmpty(),
                                rentalMaxPlayers = maxPlayers,
                                // R25: lo anotado antes no pasa de lo que cabe en esta pista.
                                rentalDraft =
                                    _uiState.value.rentalDraft?.let { draft ->
                                        draft.copy(players = draft.players.coerceAtMost(maxPlayers))
                                    },
                            )
                    }.onFailure {
                        _uiState.value = _uiState.value.copy(rentalDurationsFailed = true)
                    }
            }
        }

        fun closeSpace() {
            openSpaceId = null
            _uiState.value = _uiState.value.copy(spaceDetail = null, spaceDetailLoading = false, rentalDraft = null)
        }

        /** U04: guarda lo elegido en el picker de la renta del espacio. */
        fun updateRentalDraft(
            spaceId: Int,
            minutes: Int?,
            players: Int,
            customerName: String,
        ) {
            _uiState.value =
                _uiState.value.copy(
                    rentalDraft =
                        RentalDraft(
                            spaceId = spaceId,
                            minutes = minutes,
                            players = players.coerceIn(0, _uiState.value.rentalMaxPlayers),
                            customerName = customerName,
                        ),
                )
        }

        fun dismissCollection() {
            _uiState.value = _uiState.value.copy(lastCollection = null, lastAbono = null)
        }

        private fun loadSpaceDetail(
            spaceId: Int,
            silent: Boolean,
        ) {
            if (!silent) _uiState.value = _uiState.value.copy(spaceDetailLoading = true)
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { waiterRepository.spaceSession(spaceId) }
                if (openSpaceId != spaceId) return@launch
                result.fold(
                    onSuccess = { detail ->
                        _uiState.value = _uiState.value.copy(spaceDetail = detail, spaceDetailLoading = false)
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(spaceDetailLoading = false)
                        if (!silent) showToast(error.toUserFacingMessage(), isError = true)
                    },
                )
            }
        }

        /** Abre el turno de una cancha (con duración) o la cuenta de una mesa (sin duración). */
        fun openTurn(
            spaceId: Int,
            durationMinutes: Int?,
        ) = runSpaceAction(spaceId, "Turno abierto") {
            waiterRepository
                .openSession(spaceId, durationMinutes, keyFor("open-turn", spaceId, durationMinutes))
                .onSuccess { forgetKey("open-turn", spaceId, durationMinutes) }
        }

        fun extendTurn(
            spaceId: Int,
            minutes: Int,
        ) {
            val version =
                _uiState.value.spaceDetail
                    ?.session
                    ?.version
            runSpaceAction(spaceId, "+$minutes min") {
                waiterRepository
                    .extendSession(spaceId, minutes, version, keyFor("extend-turn", spaceId, minutes, version))
                    .onSuccess { forgetKey("extend-turn", spaceId, minutes, version) }
            }
        }

        /** Abre la hoja de pedido de mostrador; el catálogo se carga la primera vez. */
        fun openCounterOrder() = openOrderSheet(space = null)

        /** El mesero toma el pedido de una mesa: va a esa mesa y, si el negocio lo deja, a su cuenta. */
        fun openTableOrder(space: TableSpace) = openOrderSheet(space)

        private fun openOrderSheet(space: TableSpace?) {
            // Cada pedido arranca en «Te lo llevamos»: un «Pasa por él» del anterior no se hereda (R38).
            _uiState.value =
                _uiState.value.copy(
                    counterOrderOpen = true,
                    counterOrderError = null,
                    counterOrderSpace = space,
                    counterPickUpAtBar = false,
                )
            if (_uiState.value.counterCatalog != null) return
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { catalogRepository.getCatalog() }
                result.fold(
                    onSuccess = { catalog -> _uiState.value = _uiState.value.copy(counterCatalog = catalog) },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(counterOrderError = error.toUserFacingMessage())
                    },
                )
            }
        }

        fun closeCounterOrder() {
            if (_uiState.value.counterOrderSending) return
            _uiState.value =
                _uiState.value.copy(
                    counterOrderOpen = false,
                    counterOrderError = null,
                    counterOrderSpace = null,
                    counterPickUpAtBar = false,
                )
        }

        /** En Caja: «Pasa por él» (true) o «Te lo llevamos» (false) para el pedido de un espacio. */
        fun updateCounterPickUpAtBar(pickUpAtBar: Boolean) {
            _uiState.value = _uiState.value.copy(counterPickUpAtBar = pickUpAtBar)
        }

        /** Admin › «Pedir» al entrar: vuelve a abrir el pedido hacia donde iba el borrador (mostrador o su mesa). */
        fun openAdminOrder() = openOrderSheet(_uiState.value.adminOrderDraft.space)

        /** Admin › «Pedir»: cambia a dónde va el pedido sin tocar lo armado. */
        fun pickAdminOrderTarget(space: TableSpace?) {
            _uiState.value =
                _uiState.value.copy(adminOrderDraft = _uiState.value.adminOrderDraft.copy(space = space))
            openOrderSheet(space)
        }

        fun updateAdminOrderDraft(
            lines: Map<Int, CartLine>,
            name: String,
        ) {
            _uiState.value =
                _uiState.value.copy(
                    adminOrderDraft = _uiState.value.adminOrderDraft.copy(lines = lines, name = name),
                )
        }

        /**
         * Admin › «Pedir» al salir de la pestaña: cierra la hoja compartida (si no, el tablero de Mesas la abriría)
         * pero el borrador y su destino se quedan para cuando vuelva (R17).
         */
        fun parkAdminOrder() = closeCounterOrder()

        /** Manda el borrador del admin; al salir bien se limpia y el siguiente arranca en mostrador. */
        fun submitAdminOrder() {
            val draft = _uiState.value.adminOrderDraft
            if (draft.lines.isEmpty()) return
            if (_uiState.value.counterOrderSpace?.id != draft.space?.id) openOrderSheet(draft.space)
            submitCounterOrder(
                lines = draft.lines.values.toList(),
                customerName = draft.name,
                // La pestaña, si se ve, vuelve a abrir el mostrador sola al ver la hoja cerrada.
                onSent = { _uiState.value = _uiState.value.copy(adminOrderDraft = StaffOrderDraft()) },
            )
        }

        /**
         * Envía el pedido de mostrador. Solo efectivo, para cobrarse en Caja; el total y el folio los calcula el
         * servidor. Un reintento del mismo pedido reusa la llave; si cambió, se genera una nueva.
         */
        fun submitCounterOrder(
            lines: List<CartLine>,
            customerName: String,
            cashier: CashierContext? = null,
            /** Caja eligió cobrarlo ya: recibe el id para abrir su cobro (G03). */
            onCreated: ((orderId: String) -> Unit)? = null,
            /** Además del aviso: el admin limpia su borrador (R17). */
            onSent: (() -> Unit)? = null,
        ) {
            if (_uiState.value.counterOrderSending) return
            val space = _uiState.value.counterOrderSpace
            val toAccount = space != null && _uiState.value.allowsPayAtEnd
            val request =
                buildCreateOrderRequest(
                    lines = lines,
                    kitchenNotes = "",
                    destination = if (space != null) OrderDestination.IN_SPACE else OrderDestination.TAKE_AWAY,
                    spaceId = space?.id,
                    payAtEnd = toAccount,
                    origin = OrderOrigin.MOSTRADOR,
                    customerName = customerName,
                ).let { built ->
                    // R24: cada línea lleva su nota en `notas`; el resumen de `notas_cocina` se corta al límite del
                    // servidor para que muchas notas no hagan rechazar el pedido.
                    built.copy(
                        kitchenNotes = built.kitchenNotes.take(KITCHEN_NOTES_MAX),
                        pickUpAtBar =
                            space != null &&
                                _uiState.value.spaceDelivery.pickUpAtBar(_uiState.value.counterPickUpAtBar),
                    )
                }
            val key =
                counterOrderKeyFor(counterOrderKey, createOrderFingerprint(request)) {
                    UUID.randomUUID().toString()
                }
            counterOrderKey = key
            if (!offlineOrders.hasInternet()) {
                keepOffline(request, key.key, lines, space, cashier, toAccount)
                return
            }
            _uiState.value = _uiState.value.copy(counterOrderSending = true, counterOrderError = null)
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { createOrder(request, key.key) }
                _uiState.value = _uiState.value.copy(counterOrderSending = false)
                result.fold(
                    onSuccess = { created ->
                        counterOrderKey = null
                        _uiState.value =
                            _uiState.value.copy(
                                counterOrderOpen = false,
                                counterOrderError = null,
                                counterOrderSpace = null,
                                counterPickUpAtBar = false,
                            )
                        onSent?.invoke()
                        if (onCreated != null) {
                            onCreated(created.summary.id)
                        } else {
                            showToast(counterOrderToast(created.summary.folio, space?.name, toAccount, cashier))
                        }
                    },
                    onFailure = { error ->
                        if (OfflineStaffOrderRules.shouldKeepOffline(error)) {
                            keepOffline(request, key.key, lines, space, cashier, toAccount)
                        } else {
                            _uiState.value = _uiState.value.copy(counterOrderError = error.toUserFacingMessage())
                        }
                    },
                )
            }
        }

        /**
         * Sin internet el pedido no se pierde: se guarda en el teléfono con un folio local y se manda solo al volver la
         * red. Cocina todavía no lo ve (eso llega con el concentrador del local, fase 2), así que el aviso lo dice.
         */
        private fun keepOffline(
            request: CreateOrderRequest,
            idempotencyKey: String,
            lines: List<CartLine>,
            space: TableSpace?,
            cashier: CashierContext?,
            toAccount: Boolean,
        ) {
            val saved =
                offlineOrders.enqueue(
                    request = request,
                    idempotencyKey = idempotencyKey,
                    roleInitial = if (cashier != null) 'C' else 'M',
                    placeLabel = space?.name ?: "Mostrador",
                    summary = offlineSummary(lines),
                    // Caja cobra el efectivo en el momento (el aviso se lo pide): el pedido llega cobrado.
                    collectNow = cashier != null && !toAccount,
                )
            if (saved == null) {
                _uiState.value =
                    _uiState.value.copy(counterOrderError = "Sin internet. Revisa la conexión e inténtalo de nuevo.")
                return
            }
            counterOrderKey = null
            _uiState.value =
                _uiState.value.copy(counterOrderOpen = false, counterOrderError = null, counterOrderSpace = null)
            showToast(offlineOrderToast(saved.localFolio, space?.name, toAccount, cashier))
        }

        /** Manda ya lo guardado sin conexión, también lo apartado por un rechazo (tocó «Reintentar»). */
        fun retryOfflineOrders() = offlineOrders.retryAll()

        /** Lo guardado de otras membresías de la cuenta (otro modo, app reiniciada) se manda con su propio token. */
        fun rescueOfflineOrders(modes: List<AuthorizedMode>) = rescuer.rescue(modes)

        /** El personal ya resolvió a mano un pedido con problema: se quita de la lista. */
        fun dismissOfflineOrder(order: OfflineStaffOrder) = offlineOrders.dismiss(order)

        fun startRental(
            spaceId: Int,
            minutes: Int,
            start: Instant?,
            players: Int? = null,
            customerName: String? = null,
        ) {
            if (!beginAction()) return
            val name = customerName?.trim()?.takeIf { it.isNotEmpty() }
            viewModelScope.launch {
                val result =
                    actionCall {
                        reservationRepository.create(
                            access = ReservationAccess.Account,
                            courtId = spaceId,
                            start = start,
                            durationMinutes = minutes,
                            customerName = name,
                            idempotencyKey = keyFor("rent", spaceId, start, minutes, players, name),
                            players = players,
                        )
                    }
                result.onSuccess { forgetKey("rent", spaceId, start, minutes, players, name) }
                result.fold(
                    onSuccess = { reservation -> _uiState.value = _uiState.value.copy(pendingRental = reservation) },
                    onFailure = { error ->
                        refreshCapacityAfter(error)
                        showToast(error.toUserFacingMessage(), isError = true)
                    },
                )
            }
        }

        /** R25: el dueño cambió la capacidad de la pista: se vuelve a leer para limitar el selector. */
        private fun refreshCapacityAfter(error: Throwable) {
            val overCapacity =
                generateSequence(error) { it.cause }.any {
                    (it as? ApiClientException)?.code == "PLAYERS_OVER_CAPACITY"
                }
            if (overCapacity) loadRentalDurations()
        }

        /**
         * U04: cambia la duración o los jugadores de la renta apartada sin salir del cobro. El servidor no edita
         * un apartado, así que se suelta y se vuelve a apartar, como `changePendingDuration` del cliente. Si soltar
         * sale bien pero volver a apartar no, lo elegido regresa a la hoja de la pista para reintentar.
         */
        fun changePendingRental(
            minutes: Int,
            players: Int?,
        ) {
            val current = _uiState.value.pendingRental ?: return
            val wantedPlayers = players?.takeIf { it > 0 }
            if (minutes == current.durationMinutes && wantedPlayers == current.players) return
            if (!beginAction()) return
            val draft = _uiState.value.rentalDraft?.takeIf { it.spaceId == current.courtId }
            val name = draft?.customerName?.trim()?.takeIf { it.isNotEmpty() } ?: current.customerName
            // Una renovación empieza al terminar el turno; una renta "ahora" se vuelve a pedir desde ahora.
            val start = current.start.takeIf { it.isAfter(Instant.now()) }
            val cancelKey = keyFor("cancelar-renta", current.id)
            val createKey = keyFor("rent-change", current.id, minutes, wantedPlayers)
            var released = false
            viewModelScope.launch {
                val result =
                    actionCall {
                        reservationRepository
                            .cancel(
                                access = ReservationAccess.Account,
                                reservationId = current.id,
                                idempotencyKey = cancelKey,
                            ).mapCatching {
                                released = true
                                reservationRepository
                                    .create(
                                        access = ReservationAccess.Account,
                                        courtId = current.courtId,
                                        start = start,
                                        durationMinutes = minutes,
                                        customerName = name,
                                        idempotencyKey = createKey,
                                        players = wantedPlayers,
                                    ).getOrThrow()
                            }
                    }
                if (released) forgetKey("cancelar-renta", current.id)
                result.fold(
                    onSuccess = { reservation ->
                        forgetKey("rent-change", current.id, minutes, wantedPlayers)
                        _uiState.value =
                            _uiState.value.copy(
                                pendingRental = reservation,
                                rentalDraft =
                                    (draft ?: RentalDraft(current.courtId))
                                        .copy(minutes = minutes, players = wantedPlayers ?: 0),
                            )
                    },
                    onFailure = { error ->
                        refreshCapacityAfter(error)
                        if (released) {
                            // El horario anterior ya se soltó: se regresa a la hoja con lo nuevo para reintentar.
                            _uiState.value =
                                _uiState.value.copy(
                                    pendingRental = null,
                                    rentalDraft =
                                        (draft ?: RentalDraft(current.courtId))
                                            .copy(minutes = minutes, players = wantedPlayers ?: 0),
                                )
                            showToast("No se pudo volver a apartar: ${error.toUserFacingMessage()}", isError = true)
                        } else {
                            showToast("No se pudo cambiar la renta: ${error.toUserFacingMessage()}", isError = true)
                        }
                    },
                )
                if (released) refresh()
            }
        }

        /** Cobra la renta apartada: con el cobro se ocupa la cancha (o se alarga su turno). */
        fun confirmRental(received: String) {
            val reservation = _uiState.value.pendingRental ?: return
            if (!beginAction()) return
            viewModelScope.launch {
                val result =
                    actionCall {
                        reservationRepository.pay(
                            access = ReservationAccess.Account,
                            reservationId = reservation.id,
                            method = ReservationPaymentMethod.CASH,
                            cashReceived = received,
                            idempotencyKey = keyFor("pay-rental", reservation.id, received),
                        )
                    }
                result.onSuccess { forgetKey("pay-rental", reservation.id, received) }
                result.fold(
                    onSuccess = { payment ->
                        // Cobrada: lo elegido ya no hace falta.
                        _uiState.value = _uiState.value.copy(pendingRental = null, rentalDraft = null)
                        val change =
                            payment.cashChange
                                ?.takeIf { it != "0.00" }
                                ?.let { " · cambio $$it" }
                                .orEmpty()
                        showToast("${Venue.courtTitle} rentada$change")
                    },
                    onFailure = { error -> showToast(error.toUserFacingMessage(), isError = true) },
                )
                refresh()
                loadSpaceDetail(reservation.courtId, silent = true)
            }
        }

        /**
         * Se cerró el cobro sin cobrar: se libera el horario apartado y lo elegido (duración y jugadores)
         * regresa a la hoja de la pista para corregirlo (U04). [players] son los que se tocaron en el cobro y
         * aún no se mandaban (R16): no se pierden al cerrar.
         */
        fun cancelPendingRental(players: Int? = null) {
            val reservation = _uiState.value.pendingRental ?: return
            val kept = _uiState.value.rentalDraft?.takeIf { it.spaceId == reservation.courtId }
            val draft =
                (kept ?: RentalDraft(reservation.courtId))
                    .copy(
                        minutes = reservation.durationMinutes,
                        players = (players ?: reservation.players ?: 0).coerceIn(0, _uiState.value.rentalMaxPlayers),
                    )
            _uiState.value = _uiState.value.copy(pendingRental = null, rentalDraft = draft)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        reservationRepository.cancel(
                            access = ReservationAccess.Account,
                            reservationId = reservation.id,
                            idempotencyKey = keyFor("cancelar-renta", reservation.id),
                        )
                    }
                result.onSuccess { forgetKey("cancelar-renta", reservation.id) }
                result.onFailure {
                    // Sigue apartado: se devuelve a la pantalla para poder reintentar con la misma llave.
                    _uiState.value = _uiState.value.copy(pendingRental = reservation)
                    showToast("No se pudo liberar el horario apartado: ${it.toUserFacingMessage()}", isError = true)
                }
                refresh()
            }
        }

        /** Libera el espacio. El servidor no lo permite con pedidos sin cobrar. */
        fun releaseSpace(spaceId: Int) {
            val version =
                _uiState.value.spaceDetail
                    ?.session
                    ?.version
            _uiState.value = _uiState.value.copy(remainingParts = _uiState.value.remainingParts - spaceId)
            runSpaceAction(spaceId, "Espacio liberado") {
                waiterRepository.releaseSpace(spaceId, version, UUID.randomUUID().toString())
            }
        }

        /**
         * Cobra la cuenta del espacio, en efectivo o con la terminal de tarjeta del negocio: toda si
         * [orderIds] es null, o solo esos pedidos (dividir la cuenta). `expectedTotal` evita cobrar
         * mal si cambió. Con la terminal no hay efectivo recibido: [received] va null.
         */
        fun collectAccount(
            spaceId: Int,
            method: AccountPaymentMethod,
            received: String?,
            expectedTotal: String,
            orderIds: List<String>? = null,
            tip: String? = null,
        ) {
            if (!beginAction()) return
            viewModelScope.launch {
                val result =
                    actionCall {
                        waiterRepository.collectAccount(
                            spaceId,
                            method,
                            received,
                            expectedTotal,
                            orderIds,
                            keyFor("cobrar", spaceId, method.wire, received, expectedTotal, orderIds, tip),
                            tip,
                        )
                    }
                result.fold(
                    onSuccess = { collection ->
                        forgetKey("cobrar", spaceId, method.wire, received, expectedTotal, orderIds, tip)
                        _uiState.value = _uiState.value.copy(lastCollection = collection)
                        showToast(if (collection.settled) "Cuenta cobrada" else "Cobro registrado")
                    },
                    onFailure = { error -> showToast(error.toUserFacingMessage(), isError = true) },
                )
                refresh()
                loadSpaceDetail(spaceId, silent = true)
            }
        }

        /**
         * Abona a la cuenta por monto o en partes iguales (docs/dividir-cuenta.md). La llave sale
         * del abono mismo: repetirlo tras un error de red no cobra dos veces, y un abono nuevo
         * trae otra llave porque lo que falta ya cambió.
         */
        fun abonar(
            spaceId: Int,
            method: AccountPaymentMethod,
            mode: AbonoMode,
            amount: String?,
            parts: Int?,
            received: String?,
            expectedRemaining: String,
            tip: String? = null,
        ) {
            if (!beginAction()) return
            val key =
                abonoIdempotencyKey(spaceId, method, mode, amount, parts, expectedRemaining, tip)
            viewModelScope.launch {
                val result =
                    actionCall {
                        waiterRepository.abonar(
                            spaceId,
                            method,
                            mode,
                            amount,
                            parts,
                            received,
                            expectedRemaining,
                            key,
                            tip,
                        )
                    }
                result.fold(
                    onSuccess = { abono ->
                        val saved = _uiState.value.remainingParts
                        val nextParts =
                            when {
                                abono.settled -> saved - spaceId
                                mode == AbonoMode.PARTS && parts != null ->
                                    saved + (spaceId to (parts - 1).coerceAtLeast(1))
                                // Un abono por monto rompe la cuenta de partes: se vuelve a elegir.
                                else -> saved - spaceId
                            }
                        _uiState.value = _uiState.value.copy(lastAbono = abono, remainingParts = nextParts)
                        showToast(if (abono.settled) "Cuenta saldada" else "Abono registrado")
                    },
                    onFailure = { error -> showToast(error.toUserFacingMessage(), isError = true) },
                )
                refresh()
                loadSpaceDetail(spaceId, silent = true)
            }
        }

        private fun runSpaceAction(
            spaceId: Int,
            successMessage: String,
            call: () -> Result<Unit>,
        ) {
            if (!beginAction()) return
            viewModelScope.launch {
                val result = actionCall { call() }
                result.fold(
                    onSuccess = { showToast(successMessage) },
                    onFailure = { error -> showToast(error.toUserFacingMessage(), isError = true) },
                )
                refresh()
                loadSpaceDetail(spaceId, silent = true)
            }
        }

        /**
         * Una acción a la vez, pero nunca en silencio: con otra en curso se avisa en lugar de ignorar el toque.
         * Antes el toque se perdía sin decir nada y, si la acción anterior no volvía, los botones se quedaban muertos.
         */
        private fun beginAction(): Boolean {
            if (_uiState.value.acting) {
                showToast("Espera un momento: todavía se está guardando lo anterior.")
                return false
            }
            _uiState.value = _uiState.value.copy(acting = true)
            return true
        }

        /** Corre la llamada fuera del hilo principal y apaga `acting` pase lo que pase: error, excepción o cancelación. */
        private suspend fun <T> actionCall(call: () -> Result<T>): Result<T> =
            try {
                withContext(Dispatchers.IO) {
                    try {
                        call()
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        Result.failure(error)
                    }
                }
            } finally {
                _uiState.value = _uiState.value.copy(acting = false)
            }

        /**
         * Llave de idempotencia por intento: si la respuesta se perdió y el mesero reintenta lo mismo, sale la misma
         * llave y el servidor devuelve el resultado en lugar de cobrar o entregar dos veces (STAFF-10). Se olvida al
         * tener éxito, para que una acción igual más tarde no repita una respuesta vieja.
         */
        private fun keyFor(vararg parts: Any?): String =
            pendingKeys.getOrPut(parts.joinToString("|")) { UUID.randomUUID().toString() }

        private fun forgetKey(vararg parts: Any?) {
            pendingKeys.remove(parts.joinToString("|"))
        }

        private fun showToast(
            message: String,
            isError: Boolean = false,
        ) {
            toastClearJob?.cancel()
            _uiState.value = _uiState.value.copy(toastMessage = message, toastIsError = isError)
            toastClearJob =
                viewModelScope.launch {
                    delay(TOAST_VISIBLE_MS)
                    _uiState.value = _uiState.value.copy(toastMessage = null)
                }
        }

        private fun startPolling() {
            pollingJob?.cancel()
            pollingJob =
                viewModelScope.launch {
                    while (isActive && visible) {
                        awaitNextStaffPoll(POLL_INTERVAL_MS)
                        if (!visible) break
                        loadBoard()
                        openSpaceId?.let { loadSpaceDetail(it, silent = true) }
                    }
                }
        }

        override fun onCleared() {
            // La cola vive lo que la app; esta pantalla ya no recibe sus avisos.
            offlineOrders.onSent = null
            offlineOrders.onAlreadyRegistered = null
            pollingJob?.cancel()
            boardJob?.cancel()
            super.onCleared()
        }

        private companion object {
            const val POLL_INTERVAL_MS = 5_000L
            const val TOAST_VISIBLE_MS = 4_200L
        }
    }

internal const val WAITER_OFFLINE_AFTER_FAILURES = 2

/** Lo que dice la píldora de conexión del tablero. */
enum class WaiterBoardLink { Connecting, Live, Offline }

/**
 * Sin ningún tablero bueno, un solo fallo ya es "Sin conexión" y antes del primero se está conectando.
 * Con un tablero bueno a la vista, se tolera un fallo suelto: hacen falta dos seguidos.
 */
internal fun waiterBoardLink(
    boardLoaded: Boolean,
    consecutiveFailures: Int,
): WaiterBoardLink =
    when {
        !boardLoaded && consecutiveFailures > 0 -> WaiterBoardLink.Offline
        !boardLoaded -> WaiterBoardLink.Connecting
        consecutiveFailures >= WAITER_OFFLINE_AFTER_FAILURES -> WaiterBoardLink.Offline
        else -> WaiterBoardLink.Live
    }

internal fun WaiterUiState.withBoard(
    board: WaiterBoard,
    receivedAt: Instant,
): WaiterUiState =
    copy(
        tables = board.tables,
        callsEnabled = board.callsEnabled,
        deliveryRequiresQr = board.deliveryRequiresQr,
        allowsPayAtEnd = board.allowsPayAtEnd,
        spaceDelivery = board.spaceDelivery,
        loading = false,
        errorMessage = null,
        lastBoardRefreshAt = receivedAt,
        consecutiveBoardFailures = 0,
    )

/** Un fallo no borra las mesas: el mesero sigue viendo el último tablero bueno. */
internal fun WaiterUiState.withBoardFailure(message: String): WaiterUiState =
    copy(
        loading = false,
        errorMessage = message,
        consecutiveBoardFailures = consecutiveBoardFailures + 1,
    )

/** Llave de idempotencia de un abono: la misma para el mismo abono sobre el mismo restante. */
internal fun abonoIdempotencyKey(
    spaceId: Int,
    method: AccountPaymentMethod,
    mode: AbonoMode,
    amount: String?,
    parts: Int?,
    expectedRemaining: String,
    tip: String?,
): String =
    UUID
        .nameUUIDFromBytes(
            listOf("abono", spaceId, method.wire, mode.wire, amount, parts, expectedRemaining, tip)
                .joinToString("|")
                .toByteArray(),
        ).toString()

/** Quien crea el pedido es la propia Caja: el aviso no puede decir "enviado a Caja" (CAJA-21). */
data class CashierContext(
    val cashOpen: Boolean?,
)

internal fun offlineSummary(lines: List<CartLine>): String =
    lines.joinToString(", ") { "${it.quantity}× ${it.product.name}" }.take(OFFLINE_SUMMARY_MAX)

private const val OFFLINE_SUMMARY_MAX = 160

internal fun offlineOrderToast(
    localFolio: String,
    spaceName: String?,
    toAccount: Boolean,
    cashier: CashierContext?,
): String {
    val label = if (spaceName == null) "Pedido $localFolio" else "Pedido $localFolio de $spaceName"
    return when {
        cashier != null -> "Sin internet: $label guardado. Cóbralo en efectivo; se registra solo al volver la red."
        toAccount -> "Sin internet: $label guardado. Avísale a cocina; se manda solo al volver la red."
        else -> "Sin internet: $label guardado. Llévalo a Caja; se manda solo al volver la red."
    }
}

internal fun counterOrderToast(
    folio: Int,
    spaceName: String?,
    toAccount: Boolean,
    cashier: CashierContext?,
): String {
    val label = if (spaceName == null) "Pedido #$folio" else "Pedido #$folio de $spaceName"
    return when {
        toAccount -> "$label enviado a cocina"
        cashier == null -> "$label enviado a Caja"
        cashier.cashOpen == false -> "$label creado. Abre la caja para poder cobrarlo."
        else -> "$label creado. Cóbralo desde la lista de Caja."
    }
}
