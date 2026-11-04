package com.vaiinilla.app.ui.assistant

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.data.assistant.CustomerAssistantJson
import com.vaiinilla.app.domain.assistant.CustomerAssistantRepository
import com.vaiinilla.app.domain.assistant.CustomerAssistantTurn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class CustomerAssistantState(
    val turns: List<CustomerAssistantTurn> = emptyList(),
    val thinking: Boolean = false,
    val error: String? = null,
    val options: List<String> = emptyList(),
    val remaining: Int = CustomerAssistantJson.MAX_MESSAGES,
    /** Índices de los turnos cuyo pedido ya se puso en el carrito o cuyo mesero ya se llamó. */
    val done: Set<Int> = emptySet(),
) {
    val canSend: Boolean get() = !thinking && remaining > 1
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

        /** Otro negocio u otra mesa empiezan plática nueva. */
        fun onVisible(scope: String) {
            if (scope == this.scope) return
            this.scope = scope
            _state.value = CustomerAssistantState()
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
            if (current.thinking || current.turns.lastOrNull()?.fromCustomer != true) return
            ask(prepare)
        }

        fun restart() {
            _state.value = CustomerAssistantState()
        }

        fun markDone(turnIndex: Int) {
            _state.value = _state.value.copy(done = _state.value.done + turnIndex)
        }

        private fun ask(prepare: suspend () -> Unit) {
            _state.value = _state.value.copy(thinking = true, error = null)
            val askedScope = scope
            viewModelScope.launch {
                val turns = _state.value.turns
                val result =
                    withContext(Dispatchers.IO) {
                        runCatching { prepare() }.fold(
                            onSuccess = { repository.reply(turns) },
                            onFailure = { Result.failure(it) },
                        )
                    }
                if (askedScope != scope) return@launch
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
                                        ),
                                thinking = false,
                                options = reply.options,
                                remaining = reply.remainingMessages,
                            )
                        },
                        onFailure = { error ->
                            now.copy(
                                thinking = false,
                                error = error.toUserFacingMessage("No pude contestar. Intenta de nuevo."),
                            )
                        },
                    )
            }
        }
    }
