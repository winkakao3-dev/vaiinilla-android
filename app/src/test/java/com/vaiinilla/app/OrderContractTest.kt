package com.vaiinilla.app

import com.vaiinilla.app.data.fixture.ContractFixtureParser
import com.vaiinilla.app.data.order.FixtureOrderRepository
import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.domain.model.CreateOrderItem
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.repository.OrderRepositoryException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class OrderContractTest {
    private val source = TestFixtureSource()
    private val parser = ContractFixtureParser()

    @Test
    fun `created order fixture matches the approved contract`() {
        val order = OrderContractJson().parseOrderDetail(source.read("fixtures/created_order.json"))
        assertEquals(OrderState.PENDING_PAYMENT, order.summary.state)
        assertEquals(PaymentMethod.CASH, order.summary.paymentMethod)
        assertEquals(OrderDestination.TAKE_AWAY, order.summary.destination)
        assertEquals(null, order.summary.space)
        assertEquals("82.00", order.summary.total)
    }

    @Test
    fun `create request serializes only contractual client fields`() {
        val json = OrderContractJson().encodeCreateRequest(validRequest())
        assertTrue(json.contains("\"metodo_pago\":\"efectivo\""))
        assertTrue(json.contains("\"destino\":\"para_llevar\""))
        assertTrue(json.contains("\"espacio_id\":null"))
        assertTrue(json.contains("\"producto_id\":103"))
        assertTrue(json.contains("\"opcion_ids\":[310,314,317]"))
        assertFalse(json.contains("precio"))
        assertFalse(json.contains("total"))
        assertFalse(json.contains("estado"))
        assertFalse(json.contains("folio"))
    }

    @Test
    fun `fixture repository creates pending cash take away order`() {
        val repository = FixtureOrderRepository(source, parser)
        val order = repository.createOrder(validRequest(), UUID.randomUUID().toString()).getOrThrow()
        assertEquals(OrderState.PENDING_PAYMENT, order.summary.state)
        assertEquals(PaymentMethod.CASH, order.summary.paymentMethod)
        assertEquals(OrderDestination.TAKE_AWAY, order.summary.destination)
        assertEquals(null, order.summary.space)
        assertEquals("82.00", order.summary.total)
        assertEquals(1, order.summary.version)
    }

    @Test
    fun `fixture repository replays same idempotency key`() {
        val repository = FixtureOrderRepository(source, parser)
        val key = UUID.randomUUID().toString()
        val first = repository.createOrder(validRequest(), key).getOrThrow()
        val replay = repository.createOrder(validRequest(), key).getOrThrow()
        assertEquals(first.summary.id, replay.summary.id)
        assertEquals(first.summary.folio, replay.summary.folio)
    }

    @Test
    fun `fixture repository rejects reused key with different payload`() {
        val repository = FixtureOrderRepository(source, parser)
        val key = UUID.randomUUID().toString()
        repository.createOrder(validRequest(), key).getOrThrow()
        val changed = validRequest().copy(kitchenNotes = "Sin cebolla")
        val error = repository.createOrder(changed, key).exceptionOrNull()
        assertTrue(error is OrderRepositoryException)
        assertEquals("IDEMPOTENCY_KEY_REUSED", (error as OrderRepositoryException).code)
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
