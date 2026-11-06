package com.vaiinilla.app.ui.hotel.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.RetryKeys
import com.vaiinilla.app.domain.hotel.ChargeCategory
import com.vaiinilla.app.domain.hotel.CheckoutMethod
import com.vaiinilla.app.domain.hotel.HotelErrors
import com.vaiinilla.app.domain.hotel.HotelRepository
import com.vaiinilla.app.domain.hotel.HotelRoom
import com.vaiinilla.app.domain.hotel.NewStay
import com.vaiinilla.app.domain.hotel.Stay
import com.vaiinilla.app.domain.hotel.StayStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/** Qué ve Recepción: la lista de llegadas y salidas, o una de las pantallas que se abren desde ella. */
sealed interface ReceptionPane {
    data object Overview : ReceptionPane

    data object CheckIn : ReceptionPane

    data class Checkout(
        val stayId: String,
    ) : ReceptionPane

    data class Charge(
        val stayId: String,
    ) : ReceptionPane
}

data class HotelReceptionUiState(
    val stays: List<Stay> = emptyList(),
    val rooms: List<HotelRoom> = emptyList(),
    val pane: ReceptionPane = ReceptionPane.Overview,
    val loaded: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
    /** La cuenta que se está cerrando (S04), con sus cargos y su room service. */
    val detail: Stay? = null,
    val detailLoading: Boolean = false,
    val method: CheckoutMethod = CheckoutMethod.CARD,
    /** El check-in recién hecho: trae el código de estancia que recepción le da al huésped. */
    val created: Stay? = null,
) {
    val inHouse: List<Stay> get() = stays.filter { it.status == StayStatus.IN_HOUSE }

    val arriving: List<Stay> get() = stays.filter { it.status == StayStatus.RESERVED }

    val freeRooms: List<HotelRoom> get() = rooms.filter { !it.occupied }.sortedBy { it.name }

    fun stayFor(stayId: String): Stay? = stays.firstOrNull { it.id == stayId }
}

/** «90», «90.5», «1,250.75» → «90.00», «90.50», «1250.75»; vacío, cero, negativo o más de 2 decimales → null. */
internal fun parseAmount(text: String): String? {
    val clean = text.trim().replace(",", "").removePrefix("$")
    val value = clean.toBigDecimalOrNull() ?: return null
    if (value.signum() <= 0 || value.scale() > 2) return null
    return value.setScale(2, RoundingMode.UNNECESSARY).toPlainString()
}

/**
 * Recepción: llegadas y salidas, check-in, cargos manuales y el cierre de la cuenta (S04). La app no cobra: el
 * cierre registra la forma de pago con la que el huésped pagó en recepción.
 */
@HiltViewModel
class HotelReceptionViewModel
    @Inject
    constructor(
        private val repository: HotelRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(HotelReceptionUiState())
        val state: StateFlow<HotelReceptionUiState> = _state.asStateFlow()

        // Un reintento tras una respuesta perdida manda la misma llave y no repite el cargo, el check-in ni el cobro.
        private val retryKeys = RetryKeys()

        // El último aviso que puso la propia lectura: solo ese se borra cuando la lectura vuelve a salir bien.
        private var refreshError: String? = null

        fun refresh() {
            viewModelScope.launch {
                val stays = withContext(Dispatchers.IO) { repository.stays(null) }
                val rooms = withContext(Dispatchers.IO) { repository.rooms() }
                val failure = (stays.exceptionOrNull() ?: rooms.exceptionOrNull())?.let { staffErrorMessage(it, "No se pudo actualizar.") }
                _state.update { current ->
                    current.copy(
                        stays = stays.getOrDefault(current.stays),
                        rooms = rooms.getOrDefault(current.rooms),
                        loaded = current.loaded || stays.isSuccess,
                        // El error de una escritura (cargo, check-out…) no se pierde porque la lista se recargó.
                        error = failure ?: current.error?.takeUnless { it == refreshError },
                    )
                }
                refreshError = failure
            }
        }

        fun open(pane: ReceptionPane) {
            _state.update { it.copy(pane = pane, error = null, created = null) }
            if (pane is ReceptionPane.Checkout) loadDetail(pane.stayId)
        }

        fun close() = _state.update { it.copy(pane = ReceptionPane.Overview, error = null, created = null, detail = null) }

        fun setMethod(method: CheckoutMethod) = _state.update { it.copy(method = method) }

        /** La cuenta completa de una estancia, con cargos y pedidos, para S04. */
        fun loadDetail(
            stayId: String,
            force: Boolean = false,
            keepError: Boolean = false,
        ) {
            if (!force && _state.value.detail?.id == stayId) return
            _state.update { it.copy(detailLoading = true, detail = null, error = if (keepError) it.error else null) }
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { repository.stay(stayId) }
                val failure = result.exceptionOrNull()?.let { e -> staffErrorMessage(e, "No se pudo abrir la cuenta.") }
                _state.update {
                    it.copy(detailLoading = false, detail = result.getOrNull(), error = failure ?: if (keepError) it.error else null)
                }
            }
        }

        fun checkIn(draft: NewStay) {
            if (_state.value.busy) return
            _state.update { it.copy(busy = true, error = null) }
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { retryKeys.withKey("check-in", draft) { key -> repository.checkIn(draft, key) } }
                _state.update {
                    it.copy(
                        busy = false,
                        created = result.getOrNull(),
                        error = result.exceptionOrNull()?.let { e -> staffErrorMessage(e, "No se pudo hacer el check-in.") },
                    )
                }
                if (result.isSuccess) refresh()
            }
        }

        /** Una reserva que llegó pasa a «en casa» y ya puede pedir con su código. */
        fun confirmArrival(stay: Stay) =
            changeStay(stay, "arrival", "No se pudo registrar la llegada.") { id, key -> repository.confirmArrival(id, stay.version, key) }

        fun cancelReservation(stay: Stay) =
            changeStay(stay, "cancel", "No se pudo cancelar la reserva.") { id, key -> repository.cancelStay(id, stay.version, key) }

        private fun changeStay(
            stay: Stay,
            operation: String,
            fallback: String,
            call: (String, String) -> Result<Stay>,
        ) {
            if (_state.value.busy) return
            _state.update { it.copy(busy = true, error = null) }
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { retryKeys.withKey(operation, stay.id, stay.version) { key -> call(stay.id, key) } }
                _state.update { it.copy(busy = false, error = result.exceptionOrNull()?.let { e -> staffErrorMessage(e, fallback) }) }
                refresh()
            }
        }

        fun addCharge(
            stayId: String,
            concept: String,
            amount: String,
            category: ChargeCategory,
        ) {
            if (_state.value.busy) return
            _state.update { it.copy(busy = true, error = null) }
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        retryKeys.withKey("charge", stayId, concept, amount, category) { key ->
                            repository.addCharge(stayId, concept, amount, category, key)
                        }
                    }
                _state.update {
                    it.copy(
                        busy = false,
                        pane = if (result.isSuccess) ReceptionPane.Overview else it.pane,
                        error = result.exceptionOrNull()?.let { e -> staffErrorMessage(e, "No se pudo agregar el cargo.") },
                    )
                }
                if (result.isSuccess) refresh()
            }
        }

        /** Cierra la cuenta con la forma de pago elegida; [onDone] corre cuando el servidor la acepta. */
        fun checkout(
            stayId: String,
            onDone: () -> Unit = {},
        ) {
            if (_state.value.busy) return
            val method = _state.value.method
            val seen = _state.value.detail?.takeIf { it.id == stayId }
            // Se manda el total que recepción tiene a la vista: si la cuenta cambió, el servidor responde ACCOUNT_TOTAL_CHANGED.
            val total = seen?.let { checkoutBreakdown(it).total.setScale(2, RoundingMode.HALF_UP).toPlainString() }
            _state.update { it.copy(busy = true, error = null) }
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        retryKeys.withKey("checkout", stayId, method, total, seen?.version) { key ->
                            // Sin monto recibido el servidor cobra justo lo pendiente: las noches y los cargos no pasan por caja.
                            repository.checkout(
                                stayId = stayId,
                                method = method,
                                idempotencyKey = key,
                                receivedAmount = null,
                                expectedTotal = total,
                                expectedVersion = seen?.version,
                            )
                        }
                    }
                val changed = hotelErrorCode(result.exceptionOrNull()) == HotelErrors.ACCOUNT_TOTAL_CHANGED
                _state.update {
                    it.copy(
                        busy = false,
                        pane = if (result.isSuccess) ReceptionPane.Overview else it.pane,
                        detail = if (result.isSuccess) null else it.detail,
                        error =
                            when {
                                changed -> "La cuenta cambió mientras la revisabas. Ya está al día; revisa el total y vuelve a cerrarla."
                                else -> result.exceptionOrNull()?.let { e -> staffErrorMessage(e, "No se pudo cerrar la cuenta.") }
                            },
                    )
                }
                if (changed) loadDetail(stayId, force = true, keepError = true)
                if (result.isSuccess) {
                    refresh()
                    onDone()
                }
            }
        }
    }

/** Lo que S04 pone en pantalla, calculado de la estancia. */
internal data class CheckoutBreakdown(
    val nights: Int,
    val rate: String?,
    val stayAmount: BigDecimal,
    val roomServiceCount: Int,
    val roomServiceTotal: BigDecimal,
    /** Cargos agrupados: minibar, spa y salida tardía suman por categoría; los «otro» van uno por uno. */
    val charges: List<Pair<String, BigDecimal>>,
    val total: BigDecimal,
)

internal fun checkoutBreakdown(stay: Stay): CheckoutBreakdown {
    val nights = stay.nights.coerceAtLeast(1)
    val rate = stay.nightlyRate?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val stayAmount = rate.multiply(BigDecimal(nights))
    // Solo lo que el check-out cobra: un pedido que alguien ya pagó aparte no entra en el total del servidor.
    val roomOrders = stay.orders.filter { it.pendingPayment }
    val roomService = roomOrders.fold(BigDecimal.ZERO) { sum, order -> sum + (order.total.toBigDecimalOrNull() ?: BigDecimal.ZERO) }
    val grouped =
        stay.charges
            .groupBy { if (it.category == ChargeCategory.OTHER) it.concept else it.category.label }
            .map { (label, items) -> label to items.fold(BigDecimal.ZERO) { sum, c -> sum + (c.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO) } }
    val computed = stayAmount + roomService + grouped.fold(BigDecimal.ZERO) { sum, line -> sum + line.second }
    return CheckoutBreakdown(
        nights = nights,
        rate = stay.nightlyRate,
        stayAmount = stayAmount,
        roomServiceCount = roomOrders.size,
        roomServiceTotal = roomService,
        charges = grouped,
        total = stay.total?.toBigDecimalOrNull() ?: computed,
    )
}
