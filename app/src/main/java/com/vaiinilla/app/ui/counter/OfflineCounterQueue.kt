package com.vaiinilla.app.ui.counter

import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.counter.ConnectivityWatcher
import com.vaiinilla.app.domain.counter.CounterScope
import com.vaiinilla.app.domain.counter.OfflineCounterRules
import com.vaiinilla.app.domain.counter.OfflineCounterSnapshot
import com.vaiinilla.app.domain.counter.OfflineCounterStore
import com.vaiinilla.app.domain.counter.OfflineStudentPurchase
import com.vaiinilla.app.domain.counter.StudentCard
import com.vaiinilla.app.domain.counter.StudentCounterRepository
import com.vaiinilla.app.domain.counter.StudentPurchaseItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/** Lo que la fila de Alumnos y la hoja muestran de la cola. */
data class OfflineCounterState(
    /** Por mandar, de la más vieja a la más nueva. */
    val pending: List<OfflineStudentPurchase> = emptyList(),
    /** Rechazadas por el servidor o demasiado viejas: Caja las cobra de otra forma. */
    val problems: List<OfflineStudentPurchase> = emptyList(),
    val isSending: Boolean = false,
) {
    val count: Int get() = pending.size + problems.size
    val pendingTotal: BigDecimal get() = pending.fold(BigDecimal.ZERO) { sum, it -> sum + it.total }
}

/**
 * Caja sin internet: guarda las ventas a alumnos que no se pudieron mandar y las manda solas cuando vuelve
 * la conexión o cada 30 segundos. Vive todo el turno de Caja, no solo mientras está abierta la hoja de Alumnos.
 */
class OfflineCounterQueue(
    private val repository: StudentCounterRepository,
    private val store: OfflineCounterStore,
    private val connectivity: ConnectivityWatcher,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher,
    private val now: () -> Instant = Instant::now,
    private val today: () -> String = { LocalDate.now().toString() },
) {
    private val _state = MutableStateFlow(OfflineCounterState())
    val state: StateFlow<OfflineCounterState> = _state.asStateFlow()

    private var counterScope: CounterScope? = null
    private var snapshot = OfflineCounterSnapshot(day = today())
    private var reconnect: AutoCloseable? = null
    private var retryJob: Job? = null
    private val sending = Mutex()

    /** Otra membresía u otro negocio tiene su propia cola: la anterior se queda guardada en su archivo. */
    fun start(counterScope: CounterScope) {
        if (this.counterScope == counterScope) return
        stop()
        this.counterScope = counterScope
        snapshot = store.load(counterScope, today())
        publish()
        reconnect = connectivity.onReconnect { scope.launch { flush() } }
        scope.launch { flush() }
    }

    fun stop() {
        reconnect?.close()
        reconnect = null
        retryJob?.cancel()
        retryJob = null
        counterScope = null
        snapshot = OfflineCounterSnapshot(day = today())
        _state.value = OfflineCounterState()
    }

    fun remember(cards: List<StudentCard>) {
        if (counterScope == null || cards.isEmpty()) return
        rollDay()
        snapshot = cards.fold(snapshot) { acc, card -> acc.remember(card) }
        save()
    }

    fun cachedCards(text: String): List<StudentCard> {
        rollDay()
        return snapshot.cardsMatching(text)
    }

    fun refusal(
        total: BigDecimal,
        card: StudentCard,
    ): OfflineCounterRules.Refusal? = OfflineCounterRules.refusal(total, card, snapshot.queued(card.userId))

    /** Se guarda en disco antes de cualquier reintento; la llave es la del intento que falló. */
    fun enqueue(
        card: StudentCard,
        items: List<StudentPurchaseItem>,
        total: BigDecimal,
        key: String,
    ) {
        val purchase =
            OfflineStudentPurchase(
                id = key,
                studentId = card.userId,
                studentName = card.name,
                items = items,
                total = total,
                createdAt = now(),
            )
        snapshot = snapshot.copy(purchases = snapshot.purchases.filterNot { it.id == key } + purchase)
        save()
        publish()
    }

    /** Manda las pendientes una por una. Se detiene en la primera falla de conexión y reintenta después. */
    suspend fun flush() {
        if (counterScope == null || snapshot.pending.isEmpty()) return
        if (!sending.tryLock()) return
        _state.value = _state.value.copy(isSending = true)
        try {
            for (purchase in snapshot.pending) {
                if (OfflineCounterRules.isTooOld(purchase, now())) {
                    mark(purchase.id, OfflineCounterRules.TOO_OLD_PROBLEM)
                    continue
                }
                val result =
                    withContext(io) {
                        repository.purchase(
                            purchase.studentId,
                            purchase.items,
                            pin = null,
                            idempotencyKey = purchase.id,
                        )
                    }
                val error = result.exceptionOrNull()
                when {
                    error == null -> {
                        snapshot = snapshot.copy(purchases = snapshot.purchases.filterNot { it.id == purchase.id })
                        save()
                        publish()
                    }
                    OfflineCounterRules.isConnectionProblem(error) -> return
                    else -> mark(purchase.id, error.toUserFacingMessage("No se pudo cobrar."))
                }
            }
        } finally {
            sending.unlock()
            _state.value = _state.value.copy(isSending = false)
        }
    }

    /** Caja ya lo resolvió de otra forma (efectivo, otro día): se quita de la lista. */
    fun dismiss(purchase: OfflineStudentPurchase) {
        snapshot = snapshot.copy(purchases = snapshot.purchases.filterNot { it.id == purchase.id })
        save()
        publish()
    }

    private fun mark(
        id: String,
        problem: String,
    ) {
        snapshot =
            snapshot.copy(purchases = snapshot.purchases.map { if (it.id == id) it.copy(problem = problem) else it })
        save()
        publish()
    }

    /** La red puede estar bien y el servidor caído: mientras haya pendientes, se intenta cada 30 segundos. */
    private fun keepRetrying() {
        if (retryJob?.isActive == true) return
        retryJob =
            scope.launch {
                while (isActive) {
                    delay(OfflineCounterRules.RETRY_INTERVAL_MS)
                    if (snapshot.pending.isEmpty()) break
                    flush()
                }
            }
    }

    private fun rollDay() {
        snapshot = snapshot.startingDay(today())
    }

    private fun save() {
        counterScope?.let { store.save(it, snapshot) }
    }

    private fun publish() {
        _state.value = _state.value.copy(pending = snapshot.pending, problems = snapshot.problems)
        if (snapshot.pending.isNotEmpty()) keepRetrying()
    }
}
