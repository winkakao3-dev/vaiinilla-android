package com.vaiinilla.app.domain.model

import java.math.BigDecimal

/** Montos de la mesa compartida (docs/mesa-compartida.md del backend). */
data class SharedTableAmounts(
    val total: BigDecimal,
    val paid: BigDecimal,
    val pending: BigDecimal,
)

data class SharedTableOrder(
    /** null en los pedidos de otra persona: no se puede actuar sobre ellos. */
    val id: String?,
    val folio: Int,
    val status: OrderState,
    val itemsSummary: String,
    val total: BigDecimal,
    val pendingPayment: Boolean,
    /** "Esto lo pago yo": alias de quien dijo que lo paga. */
    val payer: String?,
    val iPayIt: Boolean,
)

data class SharedTableGroup(
    /** null: pedidos de quien no se unió a la mesa ("Otros en la mesa"). */
    val alias: String?,
    val isMe: Boolean,
    val orders: List<SharedTableOrder>,
    val amounts: SharedTableAmounts,
)

data class SharedTableParticipant(
    val alias: String,
    val isMe: Boolean,
)

data class SharedTable(
    val spaceId: Int,
    val spaceName: String,
    val spaceType: String,
    val myAlias: String,
    val accountOpen: Boolean,
    val participants: List<SharedTableParticipant>,
    val groups: List<SharedTableGroup>,
    val totals: SharedTableAmounts,
    val myShare: SharedTableAmounts,
) {
    /** Tus pedidos primero; luego el resto en el orden en que se unieron. */
    val orderedGroups: List<SharedTableGroup>
        get() = groups.sortedByDescending { it.isMe }
}
