package com.vaiinilla.app.ui.assistant

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.data.assistant.CustomerAssistantJson
import com.vaiinilla.app.domain.assistant.AssistantOrderLine
import com.vaiinilla.app.domain.assistant.CustomerAssistantRepository
import com.vaiinilla.app.domain.assistant.CustomerAssistantTurn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class CustomerAssistantState(
    /** Cambia con cada plática nueva: lo que la pantalla recuerde por turno se reinicia con ella. */
    val conversation: Int = 0,
    val turns: List<CustomerAssistantTurn> = emptyList(),
    val thinking: Boolean = false,
    val error: String? = null,
    val options: List<String> = emptyList(),
    val remaining: Int = CustomerAssistantJson.MAX_MESSAGES,
    /** Índices de los turnos cuyo pedido ya se puso en el carrito o cuyo mesero ya se llamó. */
    val done: Set<Int> = emptySet(),
    /** Lo que el último pedido del asistente puso en el carrito: el siguiente lo reemplaza en vez de sumarse. */
    val cartOrder: List<AssistantOrderLine> = emptyList(),
    /** Compra sin cuenta: antes de la primera respuesta se le pide su nombre (lo usa el mesero). */
    val needsName: Boolean = false,
    val enteringName: Boolean = false,
    val nameError: String? = null,
    /** La tienda apagó el asistente para clientes (ASSISTANT_DISABLED): ya no se platica ni se reintenta. */
    val closed: Boolean = false,
) {
    val canSend: Boolean get() = !thinking && !needsName && !closed && remaining > 1

    /** Solo el pedido más reciente se puede poner en el carrito: los de antes ya los reemplazó. */
    val latestOrderTurn: Int get() = turns.indexOfLast { it.order != null }
}

/** El asistente de pedidos del cliente (junto a «Mesero»). La plática vive aquí mientras siga en el mismo lugar. */
@HiltViewModel
class CustomerAssistantViewModel
    @Inject
    constructor(
        private val repository: CustomerAssistantRepository,
    ) : ViewModel() {
        private val _state = mutableStateOf(CustomerAssistantState())
        val state: State<CustomerAssistantState> = _state
        private var scope: String? = null
        private var establishmentId: String? = null

        /** Otro negocio u otra mesa empiezan plática nueva. */
        fun onVisible(
            scope: String,
            establishmentId: String?,
        ) {
            this.establishmentId = establishmentId
            if (scope == this.scope) return
            this.scope = scope
            restart()
        }

        fun send(
            text: String,
            prepare: suspend () -> Unit = {},
        ) {
            val trimmed = text.trim().take(CustomerAssistantJson.MAX_LETTERS)
            val current = _state.value
            if (trimmed.isEmpty() || !current.canSend) return
            _state.value =
                current.copy(
                    turns = current.turns + CustomerAssistantTurn(fromCustomer = true, text = trimmed),
                    error = null,
                    options = emptyList(),
                )
            ask(prepare)
        }

        fun retry(prepare: suspend () -> Unit = {}) {
            val current = _state.value
            val waiting = current.thinking || current.needsName || current.closed
            if (waiting || current.turns.lastOrNull()?.fromCustomer != true) return
            ask(prepare)
        }

        /** Plática nueva. Una respuesta que aún venía en camino ya no entra. */
        fun restart() {
            _state.value = CustomerAssistantState(conversation = _state.value.conversation + 1)
        }

        fun markDone(turnIndex: Int) {
            _state.value = _state.value.copy(done = _state.value.done + turnIndex)
        }

        /** El pedido de [turnIndex] ya está en el carrito con [added]: el próximo pedido lo reemplaza. */
        fun markOrderInCart(
            turnIndex: Int,
            added: List<AssistantOrderLine>,
        ) {
            _state.value = _state.value.copy(done = _state.value.done + turnIndex, cartOrder = added)
        }

        /** El invitado da su nombre (se da de alta como al confirmar un pedido) y sigue la plática. */
        fun submitName(
            name: String,
            prepare: suspend () -> Unit = {},
        ) {
            val clean = name.trim().replace(Regex("\\s+"), " ").take(MAX_NAME)
            val current = _state.value
            if (current.enteringName) return
            if (clean.isEmpty()) {
                _state.value = current.copy(nameError = "Escribe tu nombre.")
                return
            }
            _state.value = current.copy(enteringName = true, nameError = null)
            val conversation = current.conversation
            viewModelScope.launch {
                val entered = withContext(Dispatchers.IO) { repository.enterAsGuest(clean) }
                if (conversation != _state.value.conversation) return@launch
                entered.fold(
                    onSuccess = {
                        _state.value = _state.value.copy(needsName = false, enteringName = false)
                        if (_state.value.turns.lastOrNull()?.fromCustomer == true) ask(prepare)
                    },
                    onFailure = { error ->
                        _state.value =
                            _state.value.copy(
                                enteringName = false,
                                nameError = error.toUserFacingMessage("No pudimos registrarte. Intenta de nuevo."),
                            )
                    },
                )
            }
        }

        private fun ask(prepare: suspend () -> Unit) {
            _state.value = _state.value.copy(thinking = true, error = null)
            val conversation = _state.value.conversation
            val place = establishmentId
            viewModelScope.launch {
                val needsName = withContext(Dispatchers.IO) { repository.needsGuestName() }
                if (conversation != _state.value.conversation) return@launch
                if (needsName) {
                    _state.value = _state.value.copy(thinking = false, needsName = true)
                    return@launch
                }
                val turns = _state.value.turns
                val result =
                    withContext(Dispatchers.IO) {
                        runCatching { prepare() }.fold(
                            onSuccess = { repository.reply(turns, place) },
                            onFailure = { Result.failure(it) },
                        )
                    }
                // Plática nueva u otro lugar mientras contestaba: esta respuesta ya no es de aquí.
                if (conversation != _state.value.conversation) return@launch
                val now = _state.value
                _state.value =
                    result.fold(
                        onSuccess = { reply ->
                            now.copy(
                                turns =
                                    now.turns +
                                        CustomerAssistantTurn(
                                            fromCustomer = false,
                                            text = reply.message.ifBlank { "¿Qué se te antoja?" },
                                            products = reply.products,
                                            order = reply.order,
                                            waiter = reply.waiter,
                                            courts = reply.courts,
                                        ),
                                thinking = false,
                                options = reply.options,
                                remaining = reply.remainingMessages,
                            )
                        },
                        onFailure = { error ->
                            now.copy(thinking = false, error = errorMessage(error), closed = isDisabled(error))
                        },
                    )
            }
        }

        private fun isDisabled(error: Throwable): Boolean =
            generateSequence(error) { it.cause }.filterIsInstance<ApiClientException>().any {
                it.code == "ASSISTANT_DISABLED"
            }

        /** Lo que el servidor explica para el asistente (apagado, otro lugar, muchos mensajes) se dice tal cual. */
        private fun errorMessage(error: Throwable): String {
            val api = generateSequence(error) { it.cause }.filterIsInstance<ApiClientException>().firstOrNull()
            val message = api?.message?.trim()
            return if (api != null && api.code in SERVER_WORDED && !message.isNullOrEmpty()) {
                message
            } else {
                error.toUserFacingMessage("No pude contestar. Intenta de nuevo.")
            }
        }

        private companion object {
            const val MAX_NAME = 60
            val SERVER_WORDED = setOf(
                    "ASSISTANT_UNAVAILABLE",
                    "ASSISTANT_DISABLED",
                    "CONTEXT_MISMATCH",
                    "RATE_LIMITED",
                    "VALIDATION_ERROR",
                )
        }
    }
