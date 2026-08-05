package com.vaiinilla.app

import com.vaiinilla.app.data.contract.ContractResponseParser
import com.vaiinilla.app.data.order.FixtureOrderRepository
import com.vaiinilla.app.domain.model.CreateOrderItem
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.repository.OrderRepositoryException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class OrderOperationalTest {
    private val source = TestFixtureSource()
    private val parser = ContractResponseParser()

    @Test
    fun `cash collection advances kitchen order to cobrado`() {
        val repository = FixtureOrderRepository(source, parser)
        val order = repository.createOrder(validRequest(), UUID.randomUUID().toString()).getOrThrow()

        val paid =
            repository
                .collectCash(
                    orderId = order.summary.id,
                    amountReceived = order.summary.total,
                    expectedVersion = order.summary.version,
                    idempotencyKey = UUID.randomUUID().toString(),
                ).getOrThrow()

        assertEquals(OrderState.PAID, paid.summary.state)
        assertEquals(2, paid.summary.version)
    }

    @Test
    fun `kitchen transitions follow contract order`() {
        val repository = FixtureOrderRepository(source, parser)
        val created = repository.createOrder(validRequest(), UUID.randomUUID().toString()).getOrThrow()
        val paid =
            repository
                .collectCash(
                    created.summary.id,
                    created.summary.total,
                    created.summary.version,
                    UUID.randomUUID().toString(),
                ).getOrThrow()

        val preparing =
            repository
                .transition(
                    orderId = paid.summary.id,
                    targetState = OrderState.PREPARING,
                    expectedVersion = paid.summary.version,
                    idempotencyKey = UUID.randomUUID().toString(),
                ).getOrThrow()
        assertEquals(OrderState.PREPARING, preparing.summary.state)

        val ready =
            repository
                .transition(
                    orderId = preparing.summary.id,
                    targetState = OrderState.READY,
                    expectedVersion = preparing.summary.version,
                    idempotencyKey = UUID.randomUUID().toString(),
                ).getOrThrow()
        assertEquals(OrderState.READY, ready.summary.state)

        val delivered =
            repository
                .transition(
                    orderId = ready.summary.id,
                    targetState = OrderState.DELIVERED,
                    expectedVersion = ready.summary.version,
                    idempotencyKey = UUID.randomUUID().toString(),
                ).getOrThrow()
        assertEquals(OrderState.DELIVERED, delivered.summary.state)
    }

    @Test
    fun `cashier list excludes delivered orders`() {
        val repository = FixtureOrderRepository(source, parser)
        val created = repository.createOrder(validRequest(), UUID.randomUUID().toString()).getOrThrow()
        val paid =
            repository
                .collectCash(
                    created.summary.id,
                    created.summary.total,
                    created.summary.version,
                    UUID.randomUUID().toString(),
                ).getOrThrow()
        val preparing =
            repository
                .transition(
                    paid.summary.id,
                    OrderState.PREPARING,
                    paid.summary.version,
                    UUID.randomUUID().toString(),
                ).getOrThrow()
        val ready =
            repository
                .transition(
                    preparing.summary.id,
                    OrderState.READY,
                    preparing.summary.version,
                    UUID.randomUUID().toString(),
                ).getOrThrow()
        repository
            .transition(
                ready.summary.id,
                OrderState.DELIVERED,
                ready.summary.version,
                UUID.randomUUID().toString(),
            ).getOrThrow()

        val cashierOrders = repository.listOrders(OperationalRole.CASHIER).getOrThrow()
        assertTrue(cashierOrders.isEmpty())
    }

    @Test
    fun `collect cash rejects insufficient amount`() {
        val repository = FixtureOrderRepository(source, parser)
        val order = repository.createOrder(validRequest(), UUID.randomUUID().toString()).getOrThrow()
        val error =
            repository
                .collectCash(
                    order.summary.id,
                    "1.00",
                    order.summary.version,
                    UUID.randomUUID().toString(),
                ).exceptionOrNull()
        assertTrue(error is OrderRepositoryException)
        assertEquals("INSUFFICIENT_CASH", (error as OrderRepositoryException).code)
    }

    private fun validRequest(): CreateOrderRequest =
        CreateOrderRequest(
            paymentMethod = PaymentMethod.CASH,
            destination = OrderDestination.TAKE_AWAY,
            spaceId = null,
            kitchenNotes = "Salsa aparte",
            items =
                listOf(
                    CreateOrderItem(
                        productId = 103,
                        quantity = 1,
                        optionIds = listOf(310, 314, 317),
                    ),
                ),
        )
}
