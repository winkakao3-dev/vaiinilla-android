package com.vaiinilla.app.data.order

import com.vaiinilla.app.data.operational.RemotePendingRefundRepository
import com.vaiinilla.app.data.tape.ServerTape
import com.vaiinilla.app.data.tape.TapeFloor
import com.vaiinilla.app.domain.model.CreateOrderItem
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.repository.RefundMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

/** El repositorio real de pedidos contra lo que el servidor de pruebas contestó de verdad. */
class RemoteOrderRepositoryTapeTest {
    @get:Rule
    val tape = ServerTape()

    private val floor by lazy { TapeFloor(tape) }

    private fun money(amount: String?) = BigDecimal(amount)

    @Test
    fun `un pedido para llevar se cobra, se prepara, el cliente avisa que llego y se entrega con su codigo`() {
        floor.open()
        val created = floor.order(floor.product(), 2)
        val id = created.summary.id
        assertEquals(money(TapeFloor.PRICE) * BigDecimal(2), money(created.summary.total))
        assertEquals(OrderDestination.TAKE_AWAY, created.summary.destination)

        val cashier = floor.orders("cajero")
        assertTrue(cashier.listOrders(OperationalRole.CASHIER, null).getOrThrow().any { it.summary.id == id })
        val paid =
            cashier
                .collectCash(
                    id,
                    created.summary.total,
                    created.summary.version,
                    floor.key(),
                    null,
                ).getOrThrow()
                .order
        assertEquals(OrderState.PAID, paid.summary.state)

        val kitchen = floor.orders("cocina")
        val preparing =
            kitchen
                .transition(
                    id,
                    OrderState.PREPARING,
                    paid.summary.version,
                    floor.key(),
                    null,
                ).getOrThrow()
        val ready = kitchen.transition(id, OrderState.READY, preparing.summary.version, floor.key(), null).getOrThrow()
        assertEquals(OrderState.READY, ready.summary.state)

        val customer = floor.orders("cliente")
        assertNull(customer.getOrder(id).getOrThrow().arrivedAt)
        assertNotNull(customer.announceArrival(id).getOrThrow().arrivedAt)
        val mine = customer.recoverPickupToken(customer.getOrder(id).getOrThrow()).getOrThrow()
        val token = mine.pickupToken
        assertNotNull("el cliente recibe su código de recogida", token)
        val delivered =
            cashier
                .transition(
                    id,
                    OrderState.DELIVERED,
                    mine.summary.version,
                    floor.key(),
                    token,
                ).getOrThrow()
        assertEquals(OrderState.DELIVERED, delivered.summary.state)
    }

    @Test
    fun `cocina quita un articulo, el pedido se cancela y caja entrega la devolucion`() {
        floor.open()
        val product = floor.product()
        val request =
            CreateOrderRequest(
                paymentMethod = PaymentMethod.CASH,
                destination = OrderDestination.TAKE_AWAY,
                spaceId = null,
                kitchenNotes = "Sin cebolla",
                items =
                    listOf(
                        CreateOrderItem(product.id, 1, emptyList()),
                        CreateOrderItem(product.id, 2, emptyList()),
                    ),
            )
        val created =
            floor
                .orders("cliente")
                .createOrder(request, floor.key())
                .getOrThrow()
                .order
        assertEquals("Sin cebolla", created.kitchenNotes)
        assertEquals(2, created.items.size)
        val id = created.summary.id
        val cashier = floor.orders("cajero")
        val paid =
            cashier
                .collectCash(
                    id,
                    created.summary.total,
                    created.summary.version,
                    floor.key(),
                    null,
                ).getOrThrow()
                .order

        val removed = created.items.last()
        val kitchen = floor.orders("cocina")
        val without = kitchen.rejectItem(id, removed.id, paid.summary.version, "Se acabó", floor.key()).getOrThrow()
        val rejection = without.items.single { it.id == removed.id }.rejection
        assertEquals("Se acabó", rejection?.reason)
        assertEquals(money(removed.subtotal), money(rejection?.amount))
        assertEquals(money(created.summary.total) - money(removed.subtotal), money(without.summary.total))

        val canceled =
            cashier
                .cancelOrder(
                    id,
                    without.summary.version,
                    "El cliente ya no lo quiso",
                    floor.key(),
                ).getOrThrow()
        assertEquals(OrderState.CANCELED, canceled.summary.state)
        assertEquals("El cliente ya no lo quiso", canceled.summary.cancelReason)
        assertEquals(
            "Caja devuelve en la mano lo que quedaba pagado",
            money(without.summary.total),
            canceled.refund?.cashHandedBack,
        )

        val other = floor.order(product, 1)
        val otherPaid =
            cashier
                .collectCash(
                    other.summary.id,
                    other.summary.total,
                    other.summary.version,
                    floor.key(),
                    null,
                ).getOrThrow()
                .order
        val byCustomer =
            floor
                .orders("cliente")
                .cancelOrder(other.summary.id, otherPaid.summary.version, "Ya no puedo ir", floor.key())
                .getOrThrow()
        assertEquals(
            "lo que el cliente cancela desde la app lo entrega Caja",
            money(other.summary.total),
            byCustomer.refund?.pendingForCashier,
        )

        val refunds = RemotePendingRefundRepository(floor.client("cajero"))
        val pending = refunds.pending(null).getOrThrow().single { it.orderId == other.summary.id }
        assertEquals(money(other.summary.total), money(pending.amount))
        assertEquals("efectivo", pending.originalMethod)
        refunds.confirm(pending.id, RefundMethod.CASH, null, floor.key()).getOrThrow()
        assertTrue(refunds.pending(null).getOrThrow().none { it.orderId == other.summary.id })
    }

    @Test
    fun `a domicilio el envio se cotiza segun el punto y la agenda trae el pedido en su dia`() {
        val home = TapeFloor(tape, "domicilio")
        val store = tape.store("domicilio")
        val customer = home.orders("cliente")
        val near = customer.quoteHomeDelivery(store.latitude!!, store.longitude!!).getOrThrow()
        assertTrue(near.reaches)
        assertNotNull(near.fee)
        val far = customer.quoteHomeDelivery(store.latitude!! + 3.0, store.longitude!!).getOrThrow()
        assertTrue("a 300 km no llega el reparto", !far.reaches)

        val today = LocalDate.parse(tape.value("hoy") { LocalDate.now().toString() })
        val agenda =
            home
                .orders(
                    "admin",
                ).agenda(today.minusDays(1).toString(), today.plusDays(1).toString())
                .getOrThrow()
        assertTrue(agenda.all { day -> day.orders.all { it.summary.operationalDate.isNotBlank() } })
    }
}
