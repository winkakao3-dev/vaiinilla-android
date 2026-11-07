package com.vaiinilla.app.domain.outbox

import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import java.time.Duration
import java.time.Instant

/**
 * Cuánto puede quedarse una comanda sin cambiar de estado antes de avisar a Caja. Cada negocio puede apagarlo o
 * cambiar los minutos; sin ajuste vale [DEFAULT_MINUTES], que cubre una cocina con fila sin avisar de más.
 */
data class StuckOrderPolicy(
    val enabled: Boolean = true,
    val minutes: Int = DEFAULT_MINUTES,
) {
    val threshold: Duration get() = Duration.ofMinutes(minutes.coerceIn(MINIMUM_MINUTES, MAXIMUM_MINUTES).toLong())

    companion object {
        const val DEFAULT_MINUTES = 20
        const val MINIMUM_MINUTES = 5
        const val MAXIMUM_MINUTES = 240
    }
}

data class StuckOrder(
    val id: String,
    val folio: Int,
    val state: OrderState,
    val minutes: Long,
)

/** Donde vive el ajuste de cada negocio en este teléfono. */
interface StuckOrderSettings {
    fun policyFor(establishmentId: String): StuckOrderPolicy

    fun save(
        establishmentId: String,
        policy: StuckOrderPolicy,
    )
}

object StuckOrderRules {
    /** Estados en los que alguien del negocio tiene que mover el pedido: cobrado, preparando o listo. */
    private val WATCHED = setOf(OrderState.PAID, OrderState.PREPARING, OrderState.READY)

    /**
     * Pedidos de hoy parados en un estado más tiempo del permitido, del más viejo al más nuevo. Los apartados para
     * otro día no cuentan: esperan su fecha, no a alguien.
     */
    fun find(
        orders: List<OrderSummary>,
        policy: StuckOrderPolicy,
        now: Instant,
    ): List<StuckOrder> {
        if (!policy.enabled) return emptyList()
        return orders
            .asSequence()
            .filter { it.state in WATCHED && it.deliveryDate == null }
            .mapNotNull { order ->
                val changedAt = runCatching { Instant.parse(order.updatedAt) }.getOrNull() ?: return@mapNotNull null
                val waited = Duration.between(changedAt, now)
                if (waited >=
                    policy.threshold
                ) {
                    StuckOrder(order.id, order.folio, order.state, waited.toMinutes())
                } else {
                    null
                }
            }.sortedByDescending { it.minutes }
            .toList()
    }
}

/** Qué tanto lleva esperando lo guardado sin internet; decide el tono del aviso. */
object OutboxDelayRules {
    /** Pasados estos minutos sin mandarse, una comanda guardada ya no es «un tropiezo»: Cocina no la tiene. */
    const val URGENT_AFTER_MINUTES = 2L

    fun minutesWaiting(
        capturedAt: Instant,
        now: Instant,
    ): Long = Duration.between(capturedAt, now).toMinutes().coerceAtLeast(0)

    fun isUrgent(
        capturedAt: Instant,
        now: Instant,
    ): Boolean = minutesWaiting(capturedAt, now) >= URGENT_AFTER_MINUTES
}
