package com.vaiinilla.app

import com.vaiinilla.app.data.contract.ContractResponseParser
import com.vaiinilla.app.data.order.FixtureOrderRepository
import com.vaiinilla.app.domain.model.CreateOrderItem
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.repository.OrderRepositoryException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * Locks in the operational invariants that keep Cocina/Caja from being able to skip
 * states or deliver without a valid QR, since the repository contract exercised here
 * (FixtureOrderRepository) is exactly what backs [com.vaiinilla.app.ui.operational.OperationalViewModel]'s
 * startKitchen/markReady/deliver actions.
 */
class OperationalStateMachineInvariantsTest {
    private val source = TestFixtureSource()
    private val parser = ContractResponseParser()

    @Test
    fun `cocina cannot jump straight from paid to lista, skipping preparando`() {
        val repository = FixtureOrderRepository(source, parser)
        val paid = createPaidKitchenOrder(repository)

        val error =
            repository
                .transition(
                    orderId = paid.summary.id,
                    targetState = OrderState.READY,
                    expectedVersion = paid.summary.version,
                    idempotencyKey = UUID.randomUUID().toString(),
                ).exceptionOrNull()

        assertTrue(error is OrderRepositoryException)
        assertEquals("INVALID_TRANSITION", (error as OrderRepositoryException).code)
        val unchanged = repository.getOrder(paid.summary.id).getOrThrow()
        assertEquals(OrderState.PAID, unchanged.summary.state)
    }

    @Test
    fun `cocina can only mark lista after pasando por preparando`() {
        val repository = FixtureOrderRepository(source, parser)
        val paid = createPaidKitchenOrder(repository)

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
    }

    @Test
    fun `caja cannot deliver without a valid pickup token, even from lista`() {
        val repository = FixtureOrderRepository(source, parser)
        val ready = createReadyKitchenOrder(repository)

        val error =
            repository
                .transition(
                    orderId = ready.summary.id,
                    targetState = OrderState.DELIVERED,
                    expectedVersion = ready.summary.version,
                    idempotencyKey = UUID.randomUUID().toString(),
                    pickupToken = "",
                ).exceptionOrNull()

        assertTrue(error is OrderRepositoryException)
        assertEquals("INVALID_PICKUP_TOKEN", (error as OrderRepositoryException).code)
        val unchanged = repository.getOrder(ready.summary.id).getOrThrow()
        assertEquals(OrderState.READY, unchanged.summary.state)
    }

    @Test
    fun `caja cannot deliver an order that has not reached lista yet, even with a valid token`() {
        val repository = FixtureOrderRepository(source, parser)
        val paid = createPaidKitchenOrder(repository)
        val validToken = paid.pickupToken
        assertNotEquals(null, validToken)

        val error =
            repository
                .transition(
                    orderId = paid.summary.id,
                    targetState = OrderState.DELIVERED,
                    expectedVersion = paid.summary.version,
                    idempotencyKey = UUID.randomUUID().toString(),
                    pickupToken = validToken,
                ).exceptionOrNull()

        assertTrue(error is OrderRepositoryException)
        assertEquals("INVALID_TRANSITION", (error as OrderRepositoryException).code)
    }

    @Test
    fun `caja delivers successfully once lista and scanning the real qr token`() {
        val repository = FixtureOrderRepository(source, parser)
        val ready = createReadyKitchenOrder(repository)

        val delivered =
            repository
                .transition(
                    orderId = ready.summary.id,
                    targetState = OrderState.DELIVERED,
                    expectedVersion = ready.summary.version,
                    idempotencyKey = UUID.randomUUID().toString(),
                    pickupToken = ready.pickupToken,
                ).getOrThrow()

        assertEquals(OrderState.DELIVERED, delivered.summary.state)
    }

    private fun createPaidKitchenOrder(repository: FixtureOrderRepository) =
        repository.createOrder(kitchenOrderRequest(), UUID.randomUUID().toString()).getOrThrow().let { created ->
            repository
                .collectCash(
                    orderId = created.summary.id,
                    amountReceived = created.summary.total,
                    expectedVersion = created.summary.version,
                    idempotencyKey = UUID.randomUUID().toString(),
                ).getOrThrow()
        }

    private fun createReadyKitchenOrder(repository: FixtureOrderRepository) =
        createPaidKitchenOrder(repository).let { paid ->
            val preparing =
                repository
                    .transition(
                        orderId = paid.summary.id,
                        targetState = OrderState.PREPARING,
                        expectedVersion = paid.summary.version,
                        idempotencyKey = UUID.randomUUID().toString(),
                    ).getOrThrow()
            repository
                .transition(
                    orderId = preparing.summary.id,
                    targetState = OrderState.READY,
                    expectedVersion = preparing.summary.version,
                    idempotencyKey = UUID.randomUUID().toString(),
                ).getOrThrow()
        }

    private fun kitchenOrderRequest(): CreateOrderRequest =
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
