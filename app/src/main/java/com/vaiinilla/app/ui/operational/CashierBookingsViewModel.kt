package com.vaiinilla.app.ui.operational

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.MutationIdempotency
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.model.Reservation
import com.vaiinilla.app.domain.model.ReservationAccess
import com.vaiinilla.app.domain.model.ReservationPaymentMethod
import com.vaiinilla.app.domain.model.ReservationState
import com.vaiinilla.app.domain.repository.ReservationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

data class CashierBookingsUiState(
    /** Hay canchas en el establecimiento; sin canchas la tarjeta no se muestra. */
    val hasCourts: Boolean = false,
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val bookings: List<Reservation> = emptyList(),
    val loadError: String? = null,
    val notice: String? = null,
    val cancelError: String? = null,
    val canceling: Reservation? = null,
    val cancelBusy: Boolean = false,
    val paying: Reservation? = null,
    val received: String = "",
    val paymentBusy: Boolean = false,
    val paymentError: String? = null,
)

internal data class BookingsBoard(
    val hasCourts: Boolean,
    val bookings: List<Reservation>,
)

internal const val BOOKINGS_LIST_FORBIDDEN = "Tu rol no tiene permiso para ver las reservas."
internal const val BOOKINGS_ACTION_FORBIDDEN =
    "Tu rol no tiene permiso para esta acción todavía. Pide a un cajero que la haga o espera la actualización del backend."

/** Etiqueta de Caja (la web la usa igual); no es la de las pantallas del cliente. */
internal fun bookingStatusLabel(state: ReservationState): String =
    when (state) {
        ReservationState.PENDING_PAYMENT -> "Por pagar en efectivo"
        ReservationState.CONFIRMED,
        ReservationState.IN_PROGRESS,
        ReservationState.FINISHED,
        -> "Pagada"
        ReservationState.CANCELLED -> "Cancelada"
        ReservationState.EXPIRED -> "Expirada"
        ReservationState.CONFLICT -> "Con conflicto"
    }

internal fun isBookingVisible(state: ReservationState): Boolean = state != ReservationState.CANCELLED

/** Importe escrito a mano como "350", "350.5" o "350,50" -> "350.00"; null si no es un importe. */
internal fun normalizeReceived(raw: String): String? {
    val value = raw.trim().replace(',', '.')
    if (!Regex("^\\d+(\\.\\d{1,2})?$").matches(value)) return null
    return value.toBigDecimal().setScale(2, RoundingMode.HALF_UP).toPlainString()
}

/** Cambio que se entrega al cliente; null si el efectivo recibido no alcanza para el importe. */
internal fun cashChange(
    received: String,
    amount: String,
): BigDecimal? {
    val normalized = normalizeReceived(received) ?: return null
    val change = normalized.toBigDecimal() - amount.toBigDecimal()
    return if (change.signum() < 0) null else change.setScale(2, RoundingMode.HALF_UP)
}

/** Mensaje de error: un 403 tiene su propio texto, el resto usa el manejo común. */
internal fun bookingFailure(
    error: Throwable?,
    forbiddenMessage: String,
): String =
    if (error?.causes()?.any { it is ApiClientException && it.httpStatus == 403 } == true) {
        forbiddenMessage
    } else {
        error.toUserFacingMessage()
    }

private fun Throwable.causes(): Sequence<Throwable> = generateSequence(this) { it.cause }

/**
 * Reservas de cancha del día para Caja: cobrar en efectivo o liberar la cancha que no llegó. Carga
 * al abrir la pantalla y con "Actualizar"; no sondea, igual que la web.
 */
@HiltViewModel
class CashierBookingsViewModel
    @Inject
    constructor(
        private val reservationRepository: ReservationRepository,
    ) : ViewModel() {
        private val _uiState = mutableStateOf(CashierBookingsUiState())
        val uiState: State<CashierBookingsUiState> = _uiState

        fun onVisible() = refresh()

        fun refresh() {
            _uiState.value = _uiState.value.copy(loading = true)
            viewModelScope.launch {
                loadWithRetry().fold(
                    onSuccess = { board ->
                        _uiState.value =
                            _uiState.value.copy(
                                hasCourts = board.hasCourts,
                                bookings = board.bookings,
                                loaded = true,
                                loading = false,
                                loadError = null,
                            )
                    },
                    onFailure = { error ->
                        _uiState.value =
                            _uiState.value.copy(
                                loading = false,
                                loadError = bookingFailure(error, BOOKINGS_LIST_FORBIDDEN),
                            )
                    },
                )
            }
        }

        fun dismissNotice() {
            _uiState.value = _uiState.value.copy(notice = null)
        }

        fun startPayment(reservation: Reservation) {
            _uiState.value =
                _uiState.value.copy(paying = reservation, received = "", paymentError = null)
        }

        fun onReceivedChange(value: String) {
            _uiState.value = _uiState.value.copy(received = value, paymentError = null)
        }

        fun dismissPayment() {
            if (_uiState.value.paymentBusy) return
            _uiState.value = _uiState.value.copy(paying = null, paymentError = null)
        }

        fun submitPayment() {
            val state = _uiState.value
            val target = state.paying ?: return
            if (state.paymentBusy) return
            val received = normalizeReceived(state.received) ?: return
            if (cashChange(received, target.amount) == null) return
            _uiState.value = state.copy(paymentBusy = true, paymentError = null)
            viewModelScope.launch {
                val key = MutationIdempotency.reservationPayment(target.id, received)
                val result =
                    withContext(Dispatchers.IO) {
                        reservationRepository.pay(
                            access = ReservationAccess.Account,
                            reservationId = target.id,
                            method = ReservationPaymentMethod.CASH,
                            cashReceived = received,
                            idempotencyKey = key,
                        )
                    }
                result.fold(
                    onSuccess = {
                        _uiState.value =
                            _uiState.value.copy(
                                paying = null,
                                received = "",
                                paymentBusy = false,
                                notice = "Reserva cobrada.",
                            )
                    },
                    onFailure = { error ->
                        _uiState.value =
                            _uiState.value.copy(
                                paymentBusy = false,
                                paymentError = bookingFailure(error, BOOKINGS_ACTION_FORBIDDEN),
                            )
                    },
                )
                refresh()
            }
        }

        fun askCancel(reservation: Reservation) {
            _uiState.value = _uiState.value.copy(canceling = reservation, cancelError = null)
        }

        fun dismissCancel() {
            if (_uiState.value.cancelBusy) return
            _uiState.value = _uiState.value.copy(canceling = null)
        }

        fun confirmCancel() {
            val state = _uiState.value
            val target = state.canceling ?: return
            if (state.cancelBusy) return
            _uiState.value = state.copy(cancelBusy = true, cancelError = null)
            viewModelScope.launch {
                val key = MutationIdempotency.reservationCancellation(target.id)
                val result =
                    withContext(Dispatchers.IO) {
                        reservationRepository.cancel(
                            access = ReservationAccess.Account,
                            reservationId = target.id,
                            idempotencyKey = key,
                        )
                    }
                result.fold(
                    onSuccess = {
                        _uiState.value =
                            _uiState.value.copy(
                                canceling = null,
                                cancelBusy = false,
                                notice = "Cancha liberada. La reserva quedó cancelada.",
                            )
                    },
                    onFailure = { error ->
                        _uiState.value =
                            _uiState.value.copy(
                                canceling = null,
                                cancelBusy = false,
                                cancelError = bookingFailure(error, BOOKINGS_ACTION_FORBIDDEN),
                            )
                    },
                )
                refresh()
            }
        }

        /** Un 404 puntual en la primera carga se reintenta una vez antes de mostrar error. */
        private suspend fun loadWithRetry(): Result<BookingsBoard> {
            val first = loadBoard()
            val notFound =
                first.exceptionOrNull()
                    ?.causes()
                    ?.any { it is ApiClientException && it.httpStatus == 404 } == true
            if (!notFound) return first
            delay(RETRY_DELAY_MS)
            return loadBoard()
        }

        private suspend fun loadBoard(): Result<BookingsBoard> =
            withContext(Dispatchers.IO) {
                runCatching {
                    val day = reservationRepository.day(null, ReservationAccess.Account).getOrThrow()
                    val rows =
                        if (day.courts.isEmpty()) {
                            emptyList()
                        } else {
                            reservationRepository.list(ReservationAccess.Account, null).getOrThrow()
                        }
                    BookingsBoard(
                        hasCourts = day.courts.isNotEmpty(),
                        bookings = rows.filter { isBookingVisible(it.state) },
                    )
                }
            }

        private companion object {
            const val RETRY_DELAY_MS = 600L
        }
    }
