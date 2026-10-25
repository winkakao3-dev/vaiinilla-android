package com.vaiinilla.app.domain.usecase

import com.vaiinilla.app.domain.model.CashCollectionResult
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.repository.CashSessionRepository
import com.vaiinilla.app.domain.repository.OrderRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class ListOrdersUseCase
    @Inject
    constructor(
        private val repository: OrderRepository,
    ) {
        operator fun invoke(
            role: OperationalRole,
            updatedSince: String? = null,
        ): Result<List<OrderDetail>> = repository.listOrders(role, updatedSince)

        fun cachedClientOrders(): List<OrderDetail>? = repository.cachedClientOrders()

        /**
         * Caja solo entrega con el QR del cliente: un pedido listo sin QR en este dispositivo lo
         * recupera del backend. Si no se puede, el pedido queda igual y el detalle sigue sin QR.
         */
        fun withRecoveredPickupTokens(orders: List<OrderDetail>): List<OrderDetail> =
            orders.map { order ->
                if (order.summary.state == OrderState.READY && order.pickupToken.isNullOrBlank()) {
                    recoverOrKeep(order)
                } else {
                    order
                }
            }

        // Cada sondeo (5 s) volvería a pedir el QR de un pedido que ya falló: se espera antes de reintentar.
        private val failedRecoveryAt = ConcurrentHashMap<String, Long>()

        /** El reloj de la espera entre reintentos; las pruebas lo adelantan. */
        internal var now: () -> Long = System::currentTimeMillis

        private fun recoverOrKeep(order: OrderDetail): OrderDetail {
            val id = order.summary.id
            val at = now()
            val failedAt = failedRecoveryAt[id]
            if (failedAt != null && at - failedAt < RECOVERY_RETRY_DELAY_MS) return order
            val recovered = repository.recoverPickupToken(order).getOrNull()
            if (recovered == null) failedRecoveryAt[id] = at else failedRecoveryAt.remove(id)
            return recovered ?: order
        }

        private companion object {
            const val RECOVERY_RETRY_DELAY_MS = 30_000L
        }
    }

class GetOrderUseCase
    @Inject
    constructor(
        private val repository: OrderRepository,
    ) {
        operator fun invoke(orderId: String): Result<OrderDetail> = repository.getOrder(orderId)
    }

class RetryStripePaymentUseCase
    @Inject
    constructor(
        private val repository: OrderRepository,
    ) {
        operator fun invoke(
            orderId: String,
            idempotencyKey: String,
        ) = repository.retryStripePayment(orderId, idempotencyKey)
    }

class CollectCashUseCase
    @Inject
    constructor(
        private val repository: OrderRepository,
    ) {
        operator fun invoke(
            orderId: String,
            amountReceived: String,
            expectedVersion: Int,
            idempotencyKey: String,
            tip: String? = null,
        ): Result<CashCollectionResult> =
            repository.collectCash(
                orderId = orderId,
                amountReceived = amountReceived,
                expectedVersion = expectedVersion,
                idempotencyKey = idempotencyKey,
                tip = tip,
            )
    }

class TransitionOrderUseCase
    @Inject
    constructor(
        private val repository: OrderRepository,
    ) {
        operator fun invoke(
            orderId: String,
            targetState: OrderState,
            expectedVersion: Int,
            idempotencyKey: String,
            pickupToken: String? = null,
        ): Result<OrderDetail> =
            repository.transition(
                orderId = orderId,
                targetState = targetState,
                expectedVersion = expectedVersion,
                idempotencyKey = idempotencyKey,
                pickupToken = pickupToken,
            )
    }

class CancelOrderUseCase
    @Inject
    constructor(
        private val repository: OrderRepository,
    ) {
        operator fun invoke(
            orderId: String,
            expectedVersion: Int,
            reason: String,
            idempotencyKey: String,
        ): Result<OrderDetail> = repository.cancelOrder(orderId, expectedVersion, reason, idempotencyKey)

        /** Quitar un solo artículo; si es el último que queda, el backend pide cancelar el pedido entero. */
        fun rejectItem(
            orderId: String,
            itemId: Int,
            expectedVersion: Int,
            reason: String,
            idempotencyKey: String,
        ): Result<OrderDetail> = repository.rejectItem(orderId, itemId, expectedVersion, reason, idempotencyKey)
    }

class AnnounceArrivalUseCase
    @Inject
    constructor(
        private val repository: OrderRepository,
    ) {
        operator fun invoke(orderId: String): Result<OrderDetail> = repository.announceArrival(orderId)
    }

class CloseCashSessionUseCase
    @Inject
    constructor(
        private val repository: CashSessionRepository,
    ) {
        operator fun invoke(
            sessionId: String,
            finalAmount: String,
            idempotencyKey: String,
        ): Result<Unit> = repository.closeSession(sessionId, finalAmount, idempotencyKey)
    }

class OpenCashSessionUseCase
    @Inject
    constructor(
        private val repository: CashSessionRepository,
    ) {
        operator fun invoke(
            initialAmount: String,
            idempotencyKey: String,
        ): Result<Unit> = repository.openSession(initialAmount, idempotencyKey)
    }
