package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.counter.ConnectivityWatcher
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.CreatedOrder
import com.vaiinilla.app.domain.offline.OfflineStaffOrder
import com.vaiinilla.app.domain.offline.OfflineStaffOrderRules
import com.vaiinilla.app.domain.offline.OfflineStaffOrderStore
import com.vaiinilla.app.domain.offline.OfflineStaffOrdersSnapshot
import com.vaiinilla.app.domain.offline.OfflineStaffScope
import com.vaiinilla.app.domain.offline.cashNotRegisteredProblem
import com.vaiinilla.app.domain.outbox.DrainOutcome
import com.vaiinilla.app.domain.outbox.OutboxScheduler
import com.vaiinilla.app.domain.repository.OrderRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Lo que Mesero y Caja muestran de la cola de pedidos sin conexión. */
data class OfflineStaffOrdersState(
    /** Por mandar, del más viejo al más nuevo. */
    val pending: List<OfflineStaffOrder> = emptyList(),
    /** Rechazados por el servidor o demasiado viejos: el personal los resuelve a mano. */
    val problems: List<OfflineStaffOrder> = emptyList(),
    val isSending: Boolean = false,
) {
    val count: Int get() = pending.size + problems.size

    /** Pedidos que Caja ya cobró en efectivo y que aún no están registrados en el servidor (por mandar o con problema). */
    val unregisteredCash: List<OfflineStaffOrder> get() = (pending + problems).filter { it.collectedCash }
}

/**
 * Mesero y Caja sin internet (fase 1 del plan «Vaiinilla sin internet»): un pedido que no se pudo mandar se guarda
 * en el teléfono con su propio id y folio local, y se manda solo cuando vuelve la conexión o cada 30 segundos. Es la
 * cola de la cafetería ([com.vaiinilla.app.ui.counter.OfflineCounterQueue]) para cualquier pedido del personal.
 *
 * Vive lo que vive la app (no la pantalla): el mesero puede cambiar de pestaña mientras se manda lo guardado.
 */
class OfflineStaffOrderQueue(
    private val repository: OrderRepository,
    private val store: OfflineStaffOrderStore,
    private val connectivity: ConnectivityWatcher,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val now: () -> Instant = Instant::now,
    private val today: () -> String = { LocalDate.now().toString() },
    /** Pide un recorrido de la bandeja de salida: con la app cerrada, WorkManager manda lo guardado. */
    private val outbox: OutboxScheduler = OutboxScheduler.None,
) {
    private val _state = MutableStateFlow(OfflineStaffOrdersState())
    val state: StateFlow<OfflineStaffOrdersState> = _state.asStateFlow()

    private var staffScope: OfflineStaffScope? = null
    private var snapshot = OfflineStaffOrdersSnapshot(day = today())
    private var reconnect: AutoCloseable? = null
    private var retryJob: Job? = null
    private var flushJob: Job? = null
    private val sending = Mutex()

    /**
     * Errores 5xx seguidos por pedido: al tercero se aparta y la fila sigue. En memoria a propósito: si matan la app
     * la cuenta empieza de nuevo, lo peor es esperar tres intentos más.
     */
    private val serverFailures = mutableMapOf<String, Int>()

    /** Pedidos que se acaban de mandar bien, para que la pantalla avise con su folio real. */
    var onSent: ((localFolio: String, folio: Int) -> Unit)? = null

    /** El intento con red sí había llegado: el pedido ya estaba en el servidor con su folio. */
    var onAlreadyRegistered: ((localFolio: String) -> Unit)? = null

    /** Llegó con su folio real, pero el efectivo que Caja ya cobró no entró a ninguna caja: hay que avisarle. */
    var onCashNotRegistered: ((localFolio: String, folio: Int) -> Unit)? = null

    /** Otra membresía tiene su propia cola: la anterior se queda guardada en su archivo. */
    @Synchronized
    fun start(staffScope: OfflineStaffScope?) {
        if (this.staffScope == staffScope) return
        stop()
        if (staffScope == null) return
        this.staffScope = staffScope
        snapshot = store.load(staffScope, today())
        publish()
        reconnect = connectivity.onReconnect { launchFlush() }
        launchFlush()
    }

    @Synchronized
    fun stop() {
        reconnect?.close()
        reconnect = null
        retryJob?.cancel()
        retryJob = null
        // Un envío en curso no debe seguir mandando pedidos de la cola anterior con la sesión nueva.
        flushJob?.cancel()
        flushJob = null
        staffScope = null
        snapshot = OfflineStaffOrdersSnapshot(day = today())
        _state.value = OfflineStaffOrdersState()
    }

    /**
     * Guarda en disco el pedido que no se pudo mandar y devuelve su folio local. Lo manda la cola, no quien llama:
     * aunque la pantalla se cierre, el pedido sale en cuanto vuelve la red.
     */
    @Synchronized
    fun enqueue(
        request: CreateOrderRequest,
        /** La llave del intento con red que falló: si en realidad sí llegó, el servidor lo dice y no se duplica. */
        idempotencyKey: String,
        roleInitial: Char,
        placeLabel: String,
        summary: String,
        /** Caja ya cobró en efectivo: llega cobrado (`cobrar_ahora`) y no queda por cobrar. */
        collectNow: Boolean = false,
    ): OfflineStaffOrder? {
        if (staffScope == null) return null
        snapshot = snapshot.startingDay(today())
        val capturedAt = now()
        val id = UUID.randomUUID().toString()
        val folio = OfflineStaffOrderRules.localFolio(roleInitial, store.deviceTag(), snapshot.nextSequence)
        val order =
            OfflineStaffOrder(
                id = id,
                // La misma llave del intento con red. Si ese intento sí llegó (se perdió solo la respuesta), el
                // servidor responde IDEMPOTENCY_KEY_REUSED porque este cuerpo trae `sin_conexion`: ya está registrado.
                idempotencyKey = idempotencyKey,
                localFolio = folio,
                body = repository.offlineOrderBody(request, id, capturedAt, folio, collectNow),
                placeLabel = placeLabel,
                summary = summary,
                capturedAt = capturedAt,
                collectedCash = collectNow,
            )
        snapshot = snapshot.copy(nextSequence = snapshot.nextSequence + 1, orders = snapshot.orders + order)
        save()
        publish()
        outbox.requestDrain()
        return order
    }

    /** Sin internet comprobado el pedido se guarda de una vez, sin esperar a que falle el intento con red. */
    fun hasInternet(): Boolean = connectivity.hasInternet()

    fun launchFlush() {
        if (flushJob?.isActive == true) return
        flushJob = scope.launch { flush() }
    }

    /** Manda los pendientes en el orden en que se tomaron. Se detiene en la primera falla de conexión. */
    suspend fun flush() {
        if (staffScope == null || snapshot.pending.isEmpty()) return
        if (!sending.tryLock()) return
        val flushScope = staffScope
        _state.value = _state.value.copy(isSending = true)
        try {
            for (order in snapshot.pending) {
                if (staffScope != flushScope) return
                if (OfflineStaffOrderRules.isTooOld(order, now())) {
                    mark(order.id, OfflineStaffOrderRules.TOO_OLD_PROBLEM)
                    continue
                }
                val result = withContext(io) { repository.sendOfflineOrder(order.body, order.idempotencyKey) }
                if (staffScope != flushScope) return
                val error = result.exceptionOrNull()
                when {
                    error == null -> {
                        forgetFailures(order.id)
                        val created = result.getOrNull()
                        if (created != null && order.collectedCash && created.cashCollectionNotApplied) {
                            // El pedido sí quedó, pero el efectivo no: se queda a la vista hasta que Caja lo cobre.
                            markCashNotRegistered(order.id, created)
                            onCashNotRegistered?.invoke(order.localFolio, created.summary.folio)
                        } else {
                            remove(order.id)
                            created?.let { onSent?.invoke(order.localFolio, it.summary.folio) }
                        }
                    }
                    OfflineStaffOrderRules.alreadyRegistered(error) ||
                        OfflineStaffOrderRules.acceptedUnreadable(error) -> {
                        forgetFailures(order.id)
                        remove(order.id)
                        onAlreadyRegistered?.invoke(order.localFolio)
                    }
                    OfflineStaffOrderRules.isServerError(error) && serverFailed(order.id) -> {
                        mark(order.id, OfflineStaffOrderRules.SERVER_FAILURE_PROBLEM)
                    }
                    OfflineStaffOrderRules.shouldRetryLater(error) -> {
                        if (!OfflineStaffOrderRules.isServerError(error)) forgetFailures(order.id)
                        return
                    }
                    else -> mark(order.id, error.toUserFacingMessage("No se pudo registrar el pedido."))
                }
            }
        } finally {
            sending.unlock()
            _state.value = _state.value.copy(isSending = false)
        }
    }

    /** Solo cuentan los 5xx seguidos: un intento que entra o que falla por red reinicia la cuenta. */
    @Synchronized
    private fun forgetFailures(id: String) {
        serverFailures.remove(id)
    }

    /** Cuenta un 5xx de [id]; `true` cuando ya van [OfflineStaffOrderRules.MAXIMUM_SERVER_FAILURES]. */
    @Synchronized
    private fun serverFailed(id: String): Boolean {
        val failures = (serverFailures[id] ?: 0) + 1
        if (failures >= OfflineStaffOrderRules.MAXIMUM_SERVER_FAILURES) {
            serverFailures.remove(id)
            return true
        }
        serverFailures[id] = failures
        return false
    }

    /** El personal ya lo resolvió de otra forma (lo capturó a mano, se canceló): se quita de la lista. */
    @Synchronized
    fun dismiss(order: OfflineStaffOrder) = remove(order.id)

    /**
     * «Reintentar»: los apartados vuelven a la fila (el personal corrigió lo que faltaba: abrió la caja,
     * reactivó el producto) y se manda todo. Los de más de 12 h no: el servidor ya no los acepta.
     */
    fun retryAll() {
        synchronized(this) {
            snapshot =
                snapshot.copy(
                    orders =
                        snapshot.orders.map {
                            // «Cobrado sin registrar» ya está en el servidor: reenviarlo no registra el efectivo.
                            val fixable =
                                it.problem != null &&
                                    it.registeredFolio == null &&
                                    !OfflineStaffOrderRules.isTooOld(it, now())
                            if (fixable) it.copy(problem = null) else it
                        },
                )
            save()
            publish()
        }
        launchFlush()
    }

    /**
     * Pedidos de otra membresía que se quedaron en este teléfono (mataron la app, cambiaron de modo, cerraron
     * sesión y volvieron a entrar): se mandan con el token de esa membresía ([send]) sin tener que abrir su modo.
     * La cola del modo abierto se manda con [flush]; esta no la toca.
     */
    suspend fun rescue(
        rescued: OfflineStaffScope,
        /** El pedido creado si la respuesta se entendió; null si llegó (2xx) pero no se pudo leer. */
        send: suspend (body: String, idempotencyKey: String) -> Result<CreatedOrder?>,
    ): DrainOutcome {
        if (synchronized(this) { rescued == staffScope }) {
            launchFlush()
            return if (snapshot.pending.isEmpty()) DrainOutcome.DONE else DrainOutcome.RETRY_LATER
        }
        if (!sending.tryLock()) return DrainOutcome.RETRY_LATER
        try {
            var stored = withContext(io) { store.load(rescued, today()) }
            for (order in stored.pending) {
                if (synchronized(this) { rescued == staffScope }) return DrainOutcome.DONE
                var registeredFolio: Int? = null
                val problem =
                    if (OfflineStaffOrderRules.isTooOld(order, now())) {
                        OfflineStaffOrderRules.TOO_OLD_PROBLEM
                    } else {
                        val result = withContext(io) { send(order.body, order.idempotencyKey) }
                        val error = result.exceptionOrNull()
                        val created = result.getOrNull()
                        when {
                            error == null &&
                                created != null &&
                                order.collectedCash &&
                                created.cashCollectionNotApplied -> {
                                forgetFailures(order.id)
                                registeredFolio = created.summary.folio
                                onCashNotRegistered?.invoke(order.localFolio, created.summary.folio)
                                cashNotRegisteredProblem(created.summary.folio, created.cashCollection?.message)
                            }
                            error == null || OfflineStaffOrderRules.alreadyRegistered(error) -> {
                                forgetFailures(order.id)
                                null
                            }
                            OfflineStaffOrderRules.isServerError(error) && serverFailed(order.id) ->
                                OfflineStaffOrderRules.SERVER_FAILURE_PROBLEM
                            OfflineStaffOrderRules.shouldRetryLater(error) -> {
                                if (!OfflineStaffOrderRules.isServerError(error)) forgetFailures(order.id)
                                return DrainOutcome.RETRY_LATER
                            }
                            else -> error.toUserFacingMessage("No se pudo registrar el pedido.")
                        }
                    }
                stored =
                    if (problem == null) {
                        stored.copy(orders = stored.orders.filterNot { it.id == order.id })
                    } else {
                        stored.copy(
                            orders =
                                stored.orders.map {
                                    if (it.id == order.id) {
                                        it.copy(problem = problem, registeredFolio = registeredFolio)
                                    } else {
                                        it
                                    }
                                },
                        )
                    }
                withContext(io) { store.save(rescued, stored) }
            }
            return DrainOutcome.DONE
        } finally {
            sending.unlock()
        }
    }

    @Synchronized
    private fun remove(id: String) {
        snapshot = snapshot.copy(orders = snapshot.orders.filterNot { it.id == id })
        save()
        publish()
    }

    @Synchronized
    private fun markCashNotRegistered(
        id: String,
        created: CreatedOrder,
    ) {
        val problem = cashNotRegisteredProblem(created.summary.folio, created.cashCollection?.message)
        snapshot =
            snapshot.copy(
                orders =
                    snapshot.orders.map {
                        if (it.id == id) it.copy(problem = problem, registeredFolio = created.summary.folio) else it
                    },
            )
        save()
        publish()
    }

    @Synchronized
    private fun mark(
        id: String,
        problem: String,
    ) {
        snapshot = snapshot.copy(orders = snapshot.orders.map { if (it.id == id) it.copy(problem = problem) else it })
        save()
        publish()
    }

    /** La red puede estar bien y el servidor caído: mientras haya pendientes, se intenta cada 30 segundos. */
    private fun keepRetrying() {
        if (retryJob?.isActive == true) return
        retryJob =
            scope.launch {
                while (isActive) {
                    delay(OfflineStaffOrderRules.RETRY_INTERVAL_MS)
                    if (snapshot.pending.isEmpty()) break
                    flush()
                }
            }
    }

    private fun save() {
        staffScope?.let { store.save(it, snapshot) }
    }

    private fun publish() {
        _state.value = _state.value.copy(pending = snapshot.pending, problems = snapshot.problems)
        if (snapshot.pending.isNotEmpty()) keepRetrying()
    }
}
