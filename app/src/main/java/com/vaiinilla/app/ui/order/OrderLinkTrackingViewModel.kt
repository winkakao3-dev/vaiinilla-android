package com.vaiinilla.app.ui.order

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

data class OrderLinkUiState(
    val loaded: Boolean = false,
    val order: OrderDetail? = null,
    /** El enlace responde 404/410: el pedido ya no está disponible. */
    val gone: Boolean = false,
    /** Error transitorio; el último pedido conocido sigue a la vista. */
    val error: String? = null,
)

/**
 * Seguimiento de un pedido por su enlace (`/seguimiento/<token>`), sin sesión. Consulta cada 5 s
 * mientras el pedido está en curso, con espera creciente tras un error (como la PWA).
 */
@HiltViewModel
class OrderLinkTrackingViewModel
    @Inject
    constructor(
        private val repository: OrderRepository,
    ) : ViewModel() {
        private val _uiState = mutableStateOf(OrderLinkUiState())
        val uiState: State<OrderLinkUiState> = _uiState

        private var pollingJob: Job? = null
        private var openedToken: String? = null

        /** Empieza a seguir [token]; si ya se sigue ese mismo, no hace nada. */
        fun onVisible(token: String) {
            if (pollingJob?.isActive == true && openedToken == token) return
            pollingJob?.cancel()
            openedToken = token
            _uiState.value = OrderLinkUiState()
            pollingJob = viewModelScope.launch { poll(token) }
        }

        fun onHidden() {
            pollingJob?.cancel()
            pollingJob = null
            openedToken = null
        }

        private suspend fun poll(token: String) {
            var failures = 0
            while (coroutineContext.isActive) {
                val result = withContext(Dispatchers.IO) { repository.trackingByLink(token) }
                val nextDelay =
                    result.fold(
                        onSuccess = { order ->
                            failures = 0
                            _uiState.value = OrderLinkUiState(loaded = true, order = order)
                            if (!OrderTrackingLinkPolicy.isLive(order)) return
                            OrderTrackingLinkPolicy.POLL_MS
                        },
                        onFailure = { error ->
                            if (OrderTrackingLinkPolicy.isGone(error)) {
                                _uiState.value = OrderLinkUiState(loaded = true, gone = true)
                                return
                            }
                            failures += 1
                            _uiState.value =
                                _uiState.value.copy(
                                    loaded = true,
                                    error = error.toUserFacingMessage("No pudimos actualizar tu pedido."),
                                )
                            OrderTrackingLinkPolicy.retryDelayMs(failures)
                        },
                    )
                delay(nextDelay)
            }
        }
    }
