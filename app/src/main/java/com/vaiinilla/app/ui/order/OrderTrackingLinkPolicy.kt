package com.vaiinilla.app.ui.order

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import kotlin.random.Random

/** Reglas del seguimiento por enlace, portadas de la PWA (`tracking-poll.ts`, `order-labels.ts`). */
object OrderTrackingLinkPolicy {
    const val POLL_MS = 5_000L
    private const val BASE_RETRY_MS = 5_000L
    private const val MAX_RETRY_MS = 60_000L

    /** Tras un error se espera cada vez más (5 s, 10 s, 20 s… hasta 1 min), con algo de azar. */
    fun retryDelayMs(
        failures: Int,
        random: () -> Double = { Random.nextDouble() },
    ): Long {
        val base = (BASE_RETRY_MS * (1L shl (failures - 1).coerceIn(0, 20))).coerceAtMost(MAX_RETRY_MS)
        return Math.round(base * (0.8 + random() * 0.4))
    }

    /** 404 o 410 del seguimiento: el pedido ya no existe; no se reintenta. */
    fun isGone(error: Throwable): Boolean =
        generateSequence(error) { it.cause }
            .filterIsInstance<ApiClientException>()
            .any { it.httpStatus == 404 || it.httpStatus == 410 }

    /** Pedido en curso: se sigue consultando. Un entregado que aún se cobra al final también cuenta. */
    fun isLive(order: OrderDetail): Boolean =
        order.summary.state in LIVE_STATES ||
            (order.summary.state == OrderState.DELIVERED && order.summary.payAtEnd && order.summary.paymentPending)

    private val LIVE_STATES = setOf(OrderState.PENDING_PAYMENT, OrderState.PAID, OrderState.PREPARING, OrderState.READY)
}
