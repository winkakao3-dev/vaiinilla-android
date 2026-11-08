package com.vaiinilla.app.ui.inventory

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.data.inventory.InventoryAlerts
import com.vaiinilla.app.data.inventory.InventoryChatMessage
import com.vaiinilla.app.data.inventory.InventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Avisos de lo que falta y la plática con el asistente de inventario (admin, Caja y Cocina). */
data class InventoryAssistantState(
    /** null mientras no carga o si el servidor no tiene avisos (rol sin permiso, servidor viejo). */
    val alerts: InventoryAlerts? = null,
    val alertsLoading: Boolean = false,
    val alertsError: String? = null,
    val messages: List<InventoryChatMessage> = emptyList(),
    val options: List<String> = STARTER_QUESTIONS,
    val thinking: Boolean = false,
    val error: String? = null,
    val remainingMessages: Int = Int.MAX_VALUE,
) {
    val canSend: Boolean get() = !thinking && remainingMessages > 1
}

internal val STARTER_QUESTIONS = listOf("¿Me alcanza para mañana?", "¿Qué se acabó hoy?", "¿Quién surte lo que falta?")
internal const val INVENTORY_QUESTION_MAX_LETTERS = 600

@HiltViewModel
class InventoryAssistantViewModel
    @Inject
    constructor(
        private val repository: InventoryRepository,
    ) : ViewModel() {
        private val _state = mutableStateOf(InventoryAssistantState())
        val state: State<InventoryAssistantState> = _state
        private var scope: String? = null

        /** Otra tienda u otro rol: lo de la anterior no se queda. */
        fun onVisible(scope: String?) {
            if (scope != this.scope) {
                this.scope = scope
                _state.value = InventoryAssistantState()
            }
            refreshAlerts()
        }

        fun refreshAlerts() {
            if (_state.value.alertsLoading) return
            val startScope = scope
            _state.value = _state.value.copy(alertsLoading = true)
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { repository.alerts() }
                if (scope != startScope) return@launch
                _state.value =
                    _state.value.copy(
                        alerts = result.getOrNull() ?: _state.value.alerts,
                        alertsLoading = false,
                        alertsError =
                            result.exceptionOrNull()?.toUserFacingMessage(
                                "No se pudo revisar tu inventario.",
                            ),
                    )
            }
        }

        fun ask(text: String): Boolean {
            val clean = text.trim()
            val current = _state.value
            if (clean.isEmpty() || !current.canSend || clean.length > INVENTORY_QUESTION_MAX_LETTERS) return false
            val messages = current.messages + InventoryChatMessage(fromStaff = true, text = clean)
            _state.value = current.copy(messages = messages, options = emptyList(), error = null)
            reply(messages)
            return true
        }

        /** Vuelve a pedir la respuesta que falló. */
        fun retry() {
            val current = _state.value
            if (current.thinking || current.messages.lastOrNull()?.fromStaff != true) return
            reply(current.messages)
        }

        fun restart() {
            if (_state.value.thinking) return
            _state.value = InventoryAssistantState(alerts = _state.value.alerts)
        }

        private fun reply(messages: List<InventoryChatMessage>) {
            val startScope = scope
            _state.value = _state.value.copy(thinking = true, error = null)
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { repository.askAssistant(messages) }
                if (scope != startScope) return@launch
                val current = _state.value
                _state.value =
                    result.fold(
                        onSuccess = { reply ->
                            current.copy(
                                messages = messages + InventoryChatMessage(fromStaff = false, text = reply.message),
                                options = reply.options,
                                thinking = false,
                                remainingMessages = reply.remainingMessages,
                            )
                        },
                        onFailure = {
                            current.copy(
                                thinking = false,
                                error = it.toUserFacingMessage("El asistente no pudo contestar. Vuelve a intentarlo."),
                            )
                        },
                    )
            }
        }
    }
