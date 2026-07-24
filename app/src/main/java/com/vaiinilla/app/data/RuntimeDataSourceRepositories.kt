package com.vaiinilla.app.data

import com.vaiinilla.app.core.config.DataSourceMode
import com.vaiinilla.app.core.config.EffectiveDataSourceResolver
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.repository.CashSessionRepository
import com.vaiinilla.app.domain.repository.CatalogRepository
import com.vaiinilla.app.domain.repository.DeviceHeartbeatRepository
import com.vaiinilla.app.domain.repository.OrderRepository

class SwitchingCatalogRepository(
    private val resolver: EffectiveDataSourceResolver,
    private val fixture: CatalogRepository,
    private val remote: CatalogRepository,
) : CatalogRepository {
    private fun active(): CatalogRepository = when (resolver.effectiveMode()) {
        DataSourceMode.MOCK -> fixture
        DataSourceMode.REMOTE -> remote
    }

    override fun getCatalog() = active().getCatalog()

    override fun getOperationalStatus() = active().getOperationalStatus()
}

class SwitchingOrderRepository(
    private val resolver: EffectiveDataSourceResolver,
    private val fixture: OrderRepository,
    private val remote: OrderRepository,
) : OrderRepository {
    private fun active(): OrderRepository = when (resolver.effectiveMode()) {
        DataSourceMode.MOCK -> fixture
        DataSourceMode.REMOTE -> remote
    }

    override fun createOrder(request: CreateOrderRequest, idempotencyKey: String) =
        active().createOrder(request, idempotencyKey)

    override fun createStudentCheckout(request: CreateOrderRequest, idempotencyKey: String) =
        active().createStudentCheckout(request, idempotencyKey)

    override fun getOrder(orderId: String) = active().getOrder(orderId)

    override fun listOrders(role: OperationalRole, updatedSince: String?) =
        active().listOrders(role, updatedSince)

    override fun collectCash(
        orderId: String,
        amountReceived: String,
        expectedVersion: Int,
        idempotencyKey: String,
    ) = active().collectCash(orderId, amountReceived, expectedVersion, idempotencyKey)

    override fun transition(
        orderId: String,
        targetState: OrderState,
        expectedVersion: Int,
        idempotencyKey: String,
        pickupToken: String?,
    ) = active().transition(orderId, targetState, expectedVersion, idempotencyKey, pickupToken)
}

class SwitchingDeviceHeartbeatRepository(
    private val resolver: EffectiveDataSourceResolver,
    private val noop: DeviceHeartbeatRepository,
    private val remote: DeviceHeartbeatRepository,
) : DeviceHeartbeatRepository {
    private fun active(): DeviceHeartbeatRepository = when (resolver.effectiveMode()) {
        DataSourceMode.MOCK -> noop
        DataSourceMode.REMOTE -> remote
    }

    override fun sendHeartbeat(deviceId: String, role: OperationalRole, idempotencyKey: String) =
        active().sendHeartbeat(deviceId, role, idempotencyKey)
}

class SwitchingCashSessionRepository(
    private val resolver: EffectiveDataSourceResolver,
    private val noop: CashSessionRepository,
    private val remote: CashSessionRepository,
) : CashSessionRepository {
    private fun active(): CashSessionRepository = when (resolver.effectiveMode()) {
        DataSourceMode.MOCK -> noop
        DataSourceMode.REMOTE -> remote
    }

    override fun openSession(initialAmount: String, idempotencyKey: String) =
        active().openSession(initialAmount, idempotencyKey)

    override fun hasActiveSession(): Result<Boolean> = active().hasActiveSession()
}
