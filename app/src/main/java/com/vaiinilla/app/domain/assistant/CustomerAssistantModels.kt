package com.vaiinilla.app.domain.assistant

import com.vaiinilla.app.domain.repository.CallReason

/** Un mensaje de la plática con el asistente de pedidos; lo que trae la respuesta va pegado a ella. */
data class CustomerAssistantTurn(
    val fromCustomer: Boolean,
    val text: String,
    val products: List<AssistantProductCard> = emptyList(),
    val order: AssistantOrderProposal? = null,
    val waiter: AssistantWaiterOffer? = null,
)

/** Un producto que el asistente recomienda o menciona. [price] llega como "95.00". */
data class AssistantProductCard(
    val productId: Int,
    val name: String,
    val price: String,
    val imageUrl: String?,
    val available: Boolean,
    val needsOptions: Boolean,
    val minutes: Int?,
)

data class AssistantOrderLine(
    val product: AssistantProductCard,
    val quantity: Int,
    val note: String?,
)

/** El pedido que propone: solo lo que sí se puede pedir; [unavailable] nombra lo que quitó por agotado. */
data class AssistantOrderProposal(
    val lines: List<AssistantOrderLine>,
    val total: String,
    val unavailable: List<String>,
)

data class AssistantWaiterOffer(
    val reason: CallReason,
    val detail: String?,
)

data class CustomerAssistantReply(
    val message: String,
    val products: List<AssistantProductCard>,
    val order: AssistantOrderProposal?,
    val waiter: AssistantWaiterOffer?,
    val options: List<String>,
    val remainingMessages: Int,
)

interface CustomerAssistantRepository {
    /** `POST /asistente-cliente` con la plática completa; el último mensaje es del cliente. */
    fun reply(turns: List<CustomerAssistantTurn>): Result<CustomerAssistantReply>
}
