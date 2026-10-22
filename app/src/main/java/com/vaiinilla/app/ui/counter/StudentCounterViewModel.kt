package com.vaiinilla.app.ui.counter

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.counter.ConnectivityWatcher
import com.vaiinilla.app.domain.counter.CounterScope
import com.vaiinilla.app.domain.counter.OfflineCounterRules
import com.vaiinilla.app.domain.counter.OfflineCounterStore
import com.vaiinilla.app.domain.counter.SchoolPolicy
import com.vaiinilla.app.domain.counter.StudentCard
import com.vaiinilla.app.domain.counter.StudentCounterMessages
import com.vaiinilla.app.domain.counter.StudentCounterRepository
import com.vaiinilla.app.domain.counter.StudentCredential
import com.vaiinilla.app.domain.counter.StudentCredentialKind
import com.vaiinilla.app.domain.counter.StudentCredentialPayload
import com.vaiinilla.app.domain.counter.StudentPurchaseItem
import com.vaiinilla.app.domain.counter.StudentPurchaseRules
import com.vaiinilla.app.domain.counter.StudentTodayOrder
import com.vaiinilla.app.ui.components.moneyLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

sealed interface ChargePhase {
    data object Editing : ChargePhase

    data object Charging : ChargePhase

    data class Charged(
        val folio: Int,
    ) : ChargePhase

    /** Sin conexión: quedó guardada y se manda sola. */
    data object Queued : ChargePhase

    data class Failed(
        val message: String,
    ) : ChargePhase
}

data class StudentCounterUiState(
    val policy: SchoolPolicy? = null,
    val query: String = "",
    val results: List<StudentCard> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val hasSearched: Boolean = false,
    /** Los resultados salen de las fichas guardadas hoy porque no hay conexión. */
    val searchedOffline: Boolean = false,
    val selected: StudentCard? = null,
    val cart: Map<Int, Int> = emptyMap(),
    /** Producto → opciones elegidas; una elección por producto, como al apartar. */
    val cartChoices: Map<Int, List<Int>> = emptyMap(),
    val chargePhase: ChargePhase = ChargePhase.Editing,
    val credential: StudentCredential? = null,
    val issuingCredential: Boolean = false,
    /** La credencial con la que se abrió la ficha: con QR obligatorio, es lo que deja entregar. */
    val scannedCredential: String? = null,
    val deliveringOrderId: String? = null,
    /** Lo que dicta el alumno cuando su tutor le puso PIN; se borra al cambiar de alumno o al cobrar. */
    val pin: String = "",
    val notice: String? = null,
) {
    val itemCount: Int get() = cart.values.sum()

    /** Sin PIN puesto siempre se puede; con PIN, hasta que tenga sus 4 dígitos. */
    val pinReady: Boolean
        get() = selected?.requiresPin != true || isValidPin(pin)

    fun quantity(productId: Int): Int = cart[productId] ?: 0

    companion object {
        fun isValidPin(pin: String): Boolean = pin.length == 4 && pin.all { it in '0'..'9' }
    }
}

/**
 * Caja con un alumno sin teléfono: encontrarlo, ver su ficha, venderle con su saldo, entregarle y darle
 * credencial. Tiene también la cola sin conexión, que vive todo el turno de Caja.
 */
@HiltViewModel
class StudentCounterViewModel
    internal constructor(
        private val repository: StudentCounterRepository,
        store: OfflineCounterStore,
        connectivity: ConnectivityWatcher,
        private val io: CoroutineDispatcher,
        scopeOverride: CoroutineScope?,
    ) : ViewModel() {
        @Inject
        constructor(
            repository: StudentCounterRepository,
            store: OfflineCounterStore,
            connectivity: ConnectivityWatcher,
        ) : this(repository, store, connectivity, Dispatchers.IO, null)

        private val work: CoroutineScope = scopeOverride ?: viewModelScope
        private val _uiState = mutableStateOf(StudentCounterUiState())
        val uiState: State<StudentCounterUiState> = _uiState

        val offline = OfflineCounterQueue(repository, store, connectivity, work, io)
        val offlineState: StateFlow<OfflineCounterState> = offline.state

        private var chargeKey = UUID.randomUUID().toString()
        private var searchJob: Job? = null

        private var state: StudentCounterUiState
            get() = _uiState.value
            set(value) {
                _uiState.value = value
            }

        /**
         * El turno de Caja de esta membresía en este negocio; null al dejar de ser Caja. Lo guardado se queda
         * en el teléfono y se vuelve a mandar cuando esa misma membresía regrese a Caja.
         */
        fun setShift(scope: CounterScope?) {
            if (scope == null) {
                offline.stop()
                state = StudentCounterUiState()
                return
            }
            offline.start(scope)
            refreshPolicy()
        }

        fun refreshPolicy() {
            work.launch {
                withContext(io) { repository.schoolPolicy() }.onSuccess { state = state.copy(policy = it) }
            }
        }

        override fun onCleared() {
            offline.stop()
            super.onCleared()
        }

        // Buscar

        fun updateQuery(text: String) {
            state = state.copy(query = text)
            searchJob?.cancel()
            searchJob =
                work.launch {
                    delay(SEARCH_DEBOUNCE_MS)
                    search()
                }
        }

        suspend fun search() {
            val text = state.query.trim()
            if (text.length < 2) {
                state =
                    state.copy(results = emptyList(), searchError = null, hasSearched = false, searchedOffline = false)
                return
            }
            state = state.copy(isSearching = true)
            val result = withContext(io) { repository.search(text) }
            if (state.query.trim() != text) {
                state = state.copy(isSearching = false)
                return
            }
            val error = result.exceptionOrNull()
            state =
                when {
                    error == null -> {
                        val found = result.getOrThrow()
                        offline.remember(found)
                        state.copy(
                            results = found,
                            searchError = null,
                            hasSearched = true,
                            searchedOffline = false,
                            isSearching = false,
                        )
                    }
                    OfflineCounterRules.isConnectionProblem(error) ->
                        state.copy(
                            results = offline.cachedCards(text),
                            searchError = null,
                            hasSearched = true,
                            searchedOffline = true,
                            isSearching = false,
                        )
                    else ->
                        state.copy(
                            searchError = error.toUserFacingMessage("No pudimos buscar."),
                            isSearching = false,
                        )
                }
        }

        fun retrySearch() {
            work.launch { search() }
        }

        /** Lo que leyó la cámara o el NFC. Un QR que no es credencial de alumno no se manda. */
        fun openCredential(
            scanned: String,
            fromNfc: Boolean = false,
        ) {
            if (!fromNfc && !StudentCredentialPayload.isStudentCredential(scanned)) {
                state = state.copy(notice = "Ese QR no es una credencial de alumno.")
                return
            }
            work.launch {
                withContext(io) { repository.find(scanned) }
                    .onSuccess { card ->
                        if (card == null) {
                            state =
                                state.copy(notice = "Esa credencial ya no sirve. Repónla desde la ficha del alumno.")
                        } else {
                            // Con la ficha ya abierta del mismo alumno, la credencial solo destraba la entrega.
                            if (state.selected?.userId != card.userId) select(card)
                            state = state.copy(scannedCredential = scanned.trim(), selected = card)
                        }
                    }.onFailure {
                        state = state.copy(notice = it.toUserFacingMessage("No pudimos leer la credencial."))
                    }
            }
        }

        fun select(card: StudentCard) {
            offline.remember(listOf(card))
            chargeKey = UUID.randomUUID().toString()
            state =
                state.copy(
                    selected = card,
                    cart = emptyMap(),
                    cartChoices = emptyMap(),
                    chargePhase = ChargePhase.Editing,
                    credential = null,
                    scannedCredential = null,
                    pin = "",
                )
        }

        fun closeStudent() {
            state = state.copy(selected = null, scannedCredential = null, pin = "", credential = null)
        }

        fun updatePin(typed: String) {
            state = state.copy(pin = typed.filter { it in '0'..'9' }.take(4))
        }

        fun consumeNotice() {
            state = state.copy(notice = null)
        }

        /** Vuelve a pedir la ficha para que el saldo, lo gastado y los pedidos del día queden al día. */
        suspend fun refreshSelected() {
            val current = state.selected ?: return
            val text = current.studentId ?: current.name
            val fresh =
                withContext(io) { repository.search(text) }
                    .getOrNull()
                    ?.firstOrNull { it.userId == current.userId } ?: return
            offline.remember(listOf(fresh))
            state =
                state.copy(
                    selected = if (state.selected?.userId == fresh.userId) fresh else state.selected,
                    results = state.results.map { if (it.userId == fresh.userId) fresh else it },
                )
        }

        // Vender

        fun step(
            productId: Int,
            delta: Int,
            optionIds: List<Int>? = null,
        ) {
            val next = (state.quantity(productId) + delta).coerceIn(0, StudentPurchaseRules.MAX_PER_PRODUCT)
            val cart = if (next == 0) state.cart - productId else state.cart + (productId to next)
            val choices =
                when {
                    next == 0 -> state.cartChoices - productId
                    optionIds == null -> state.cartChoices
                    optionIds.isEmpty() -> state.cartChoices - productId
                    else -> state.cartChoices + (productId to optionIds.sorted())
                }
            var phase = state.chargePhase
            if (phase != ChargePhase.Charging && phase != ChargePhase.Editing) {
                phase = ChargePhase.Editing
                chargeKey = UUID.randomUUID().toString()
            } else if (phase == ChargePhase.Editing && cart != state.cart) {
                // Otro cuerpo, otra llave: la anterior solo sirve para reintentar lo mismo.
                chargeKey = UUID.randomUUID().toString()
            }
            state = state.copy(cart = cart, cartChoices = choices, chargePhase = phase)
        }

        /** Desde la ficha: con otras opciones, reemplaza lo elegido; con las mismas, suma. */
        fun add(
            productId: Int,
            optionIds: List<Int>,
            quantity: Int,
        ) {
            val same = (state.cartChoices[productId] ?: emptyList()) == optionIds.sorted()
            val current = if (same) state.quantity(productId) else 0
            step(productId, current + quantity - state.quantity(productId), optionIds)
        }

        fun charge(total: BigDecimal) {
            val student = state.selected ?: return
            if (state.cart.isEmpty() || state.chargePhase == ChargePhase.Charging || !state.pinReady) return
            state = state.copy(chargePhase = ChargePhase.Charging)
            val items =
                state.cart.keys.sorted().map {
                    StudentPurchaseItem(it, state.cart[it] ?: 1, state.cartChoices[it].orEmpty())
                }
            val key = chargeKey
            val pin = if (student.requiresPin) state.pin else null
            work.launch {
                val result = withContext(io) { repository.purchase(student.userId, items, pin, key) }
                val error = result.exceptionOrNull()
                when {
                    error == null -> {
                        chargeKey = UUID.randomUUID().toString()
                        state =
                            state.copy(
                                chargePhase = ChargePhase.Charged(result.getOrThrow().summary.folio),
                                pin = "",
                                cart = emptyMap(),
                                cartChoices = emptyMap(),
                            )
                        refreshSelected()
                        offline.flush()
                    }
                    OfflineCounterRules.isConnectionProblem(error) -> queueOffline(student, items, total, key)
                    else -> {
                        // Un PIN equivocado no se reintenta solo: el alumno lo vuelve a dictar. La llave se queda,
                        // por si el cobro sí entró y solo se perdió la respuesta.
                        state =
                            state.copy(
                                pin = "",
                                chargePhase = ChargePhase.Failed(StudentCounterMessages.chargeFailure(error)),
                            )
                    }
                }
            }
        }

        /** La llave es la del intento que falló: si sí había entrado, el servidor no la cobra dos veces. */
        private fun queueOffline(
            student: StudentCard,
            items: List<StudentPurchaseItem>,
            total: BigDecimal,
            key: String,
        ) {
            state =
                when (val refusal = offline.refusal(total, student)) {
                    OfflineCounterRules.Refusal.NeedsPin ->
                        state.copy(
                            pin = "",
                            chargePhase =
                                ChargePhase.Failed(
                                    "Sin conexión no se puede revisar su PIN. Cóbrale cuando vuelva la red.",
                                ),
                        )
                    is OfflineCounterRules.Refusal.OverAllowance ->
                        state.copy(
                            chargePhase =
                                ChargePhase.Failed(
                                    if (refusal.allowance.signum() > 0) {
                                        "Sin conexión solo se le pueden cobrar ${moneyLabel(refusal.allowance)} más."
                                    } else {
                                        "Sin conexión ya no se le puede cobrar más."
                                    },
                                ),
                        )
                    null -> {
                        offline.enqueue(student, items, total, key)
                        chargeKey = UUID.randomUUID().toString()
                        state.copy(chargePhase = ChargePhase.Queued, cart = emptyMap(), cartChoices = emptyMap())
                    }
                }
        }

        // Entregar

        fun deliver(order: StudentTodayOrder) {
            if (state.deliveringOrderId != null) return
            state = state.copy(deliveringOrderId = order.id)
            val credential = state.scannedCredential
            work.launch {
                val result =
                    withContext(io) {
                        repository.deliver(order.id, order.version, credential, UUID.randomUUID().toString())
                    }
                state =
                    result.fold(
                        onSuccess = {
                            state.copy(
                                notice = "Entregado: #${it.summary.folio}",
                                deliveringOrderId = null,
                            )
                        },
                        onFailure = {
                            state.copy(notice = StudentCounterMessages.deliveryFailure(it), deliveringOrderId = null)
                        },
                    )
                if (result.isSuccess) refreshSelected()
            }
        }

        // Credencial

        fun issueQrCredential() = issueCredential(StudentCredentialKind.QR, null)

        /** La tarjeta que Caja acercó al teléfono queda como la credencial NFC del alumno. */
        fun linkNfcCard(uid: String) = issueCredential(StudentCredentialKind.NFC, uid)

        private fun issueCredential(
            kind: StudentCredentialKind,
            nfcUid: String?,
        ) {
            val student = state.selected ?: return
            if (state.issuingCredential) return
            state = state.copy(issuingCredential = true)
            work.launch {
                val result =
                    withContext(io) {
                        repository.assignCredential(student.userId, kind, nfcUid, UUID.randomUUID().toString())
                    }
                state =
                    result.fold(
                        onSuccess = { credential ->
                            state.copy(
                                credential = credential,
                                issuingCredential = false,
                                notice =
                                    if (kind ==
                                        StudentCredentialKind.NFC
                                    ) {
                                        "Tarjeta ligada a ${student.firstName}."
                                    } else {
                                        state.notice
                                    },
                            )
                        },
                        onFailure = {
                            state.copy(
                                issuingCredential = false,
                                notice = it.toUserFacingMessage("No pudimos hacer la credencial."),
                            )
                        },
                    )
            }
        }

        private companion object {
            const val SEARCH_DEBOUNCE_MS = 320L
        }
    }

/** El dinero de Caja con el formato compacto de la app ($50, $12.50). */
internal fun moneyLabel(value: BigDecimal): String =
    moneyLabel(value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
