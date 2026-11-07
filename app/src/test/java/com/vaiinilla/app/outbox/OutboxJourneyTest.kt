package com.vaiinilla.app.outbox

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.data.offline.OfflineOrderBody
import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import com.vaiinilla.app.domain.counter.ConnectivityWatcher
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.model.CashCollectionResult
import com.vaiinilla.app.domain.model.CreateOrderItem
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.CreatedOrder
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.StripePaymentSession
import com.vaiinilla.app.domain.offline.OfflineStaffOrderStore
import com.vaiinilla.app.domain.offline.OfflineStaffOrdersSnapshot
import com.vaiinilla.app.domain.offline.OfflineStaffScope
import com.vaiinilla.app.domain.outbox.DrainOutcome
import com.vaiinilla.app.domain.outbox.OutboxBackoff
import com.vaiinilla.app.domain.outbox.OutboxContext
import com.vaiinilla.app.domain.outbox.OutboxDelayRules
import com.vaiinilla.app.domain.outbox.OutboxDispatcher
import com.vaiinilla.app.domain.outbox.OutboxScheduler
import com.vaiinilla.app.domain.outbox.OutboxSource
import com.vaiinilla.app.domain.repository.OrderRepository
import com.vaiinilla.app.domain.repository.OrderRepositoryException
import com.vaiinilla.app.ui.operational.OfflineStaffOrderQueue
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Duration
import java.time.Instant

/**
 * El recorrido de una comanda (pedido, cocina, mesero, entrega, cobro) con y sin internet, contra un servidor de
 * mentira que respeta la llave de idempotencia: nada se pierde y nada se duplica.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OutboxJourneyTest {
    private val mesero = OfflineStaffScope(membershipId = "m-1")
    private val request =
        CreateOrderRequest(
            paymentMethod = PaymentMethod.CASH,
            destination = OrderDestination.TAKE_AWAY,
            spaceId = null,
            kitchenNotes = "",
            items = listOf(CreateOrderItem(productId = 1, quantity = 2, optionIds = emptyList())),
        )

    private class ServerOrder(
        var state: OrderState,
        var cashCollections: Int,
    )

    /** Registra una vez por llave; con el internet apagado no responde. */
    private class KitchenServer {
        var online = false
        var lostResponses = 0
        val orders = linkedMapOf<String, ServerOrder>()

        fun send(
            body: String,
            key: String,
        ): Result<CreatedOrder> {
            if (!online) return Result.failure(IOException("sin red"))
            if (orders.containsKey(key)) {
                return Result.failure(
                    OrderRepositoryException(
                        "IDEMPOTENCY_KEY_REUSED",
                        "Ya estaba registrado.",
                        ApiClientException("IDEMPOTENCY_KEY_REUSED", "otra petición", 409),
                    ),
                )
            }
            val collected = body.contains("\"cobrar_ahora\":true")
            orders[key] =
                ServerOrder(if (collected) OrderState.PAID else OrderState.PENDING_PAYMENT, if (collected) 1 else 0)
            if (lostResponses > 0) {
                lostResponses--
                return Result.failure(IOException("se cortó la respuesta"))
            }
            return Result.success(CreatedOrder(order = ScreenshotFixtures.sampleOrder()))
        }

        fun kitchenSees(): List<ServerOrder> =
            orders.values.filter {
                it.state == OrderState.PAID ||
                    it.state == OrderState.PREPARING
            }

        fun collect(key: String) {
            val order = orders.getValue(key)
            if (order.state == OrderState.PENDING_PAYMENT) {
                order.state = OrderState.PAID
                order.cashCollections++
            }
        }

        fun advance(
            key: String,
            to: OrderState,
        ) {
            orders.getValue(key).state = to
        }
    }

    private class MemoryStore : OfflineStaffOrderStore {
        val saved = mutableMapOf<OfflineStaffScope, OfflineStaffOrdersSnapshot>()

        override fun load(
            scope: OfflineStaffScope,
            today: String,
        ) = (saved[scope] ?: OfflineStaffOrdersSnapshot(day = today)).startingDay(today)

        override fun save(
            scope: OfflineStaffScope,
            snapshot: OfflineStaffOrdersSnapshot,
        ) {
            saved[scope] = snapshot
        }

        override fun deviceTag(): Int = 7

        override fun scopesWithOrders() = saved.filter { it.value.orders.any { o -> o.problem == null } }.keys.toList()
    }

    private class Connectivity : ConnectivityWatcher {
        override fun onReconnect(callback: () -> Unit): AutoCloseable = AutoCloseable {}
    }

    private class ServerRepository(
        private val server: KitchenServer,
    ) : OrderRepository {
        override fun offlineOrderBody(
            request: CreateOrderRequest,
            orderId: String,
            capturedAt: Instant,
            localFolio: String,
            collectNow: Boolean,
        ): String =
            OfflineOrderBody.withOfflineCapture(
                """{"metodo_pago":"efectivo"}""",
                orderId,
                capturedAt,
                localFolio,
                collectNow,
            )

        override fun sendOfflineOrder(
            body: String,
            idempotencyKey: String,
        ) = server.send(body, idempotencyKey)

        override fun createOrder(
            request: CreateOrderRequest,
            idempotencyKey: String,
        ) = Result.failure<CreatedOrder>(UnsupportedOperationException())

        override fun getOrder(orderId: String) = Result.failure<OrderDetail>(UnsupportedOperationException())

        override fun listOrders(
            role: OperationalRole,
            updatedSince: String?,
        ) = Result.success(emptyList<OrderDetail>())

        override fun retryStripePayment(
            orderId: String,
            idempotencyKey: String,
        ) = Result.failure<StripePaymentSession>(UnsupportedOperationException())

        override fun collectCash(
            orderId: String,
            amountReceived: String,
            expectedVersion: Int,
            idempotencyKey: String,
            tip: String?,
        ) = Result.failure<CashCollectionResult>(UnsupportedOperationException())

        override fun transition(
            orderId: String,
            targetState: OrderState,
            expectedVersion: Int,
            idempotencyKey: String,
            pickupToken: String?,
        ) = Result.failure<OrderDetail>(UnsupportedOperationException())
    }

    /** Lo que hace StaffOrderOutboxSource en producción, con el servidor de mentira en lugar de HTTP. */
    private class StaffSource(
        private val queue: OfflineStaffOrderQueue,
        private val store: OfflineStaffOrderStore,
        private val server: KitchenServer,
    ) : OutboxSource {
        override val name = "pedidos del personal"

        override fun hasWork() = store.scopesWithOrders().isNotEmpty()

        override suspend fun drain(context: OutboxContext): DrainOutcome {
            var outcome = DrainOutcome.DONE
            for (saved in store.scopesWithOrders()) {
                val mode = context.modeOfMembership(saved.membershipId) ?: continue
                context.tokenFor(mode) ?: return DrainOutcome.RETRY_LATER
                outcome = outcome and queue.rescue(saved) { body, key -> server.send(body, key).map { it } }
            }
            return outcome
        }
    }

    private class CountingScheduler : OutboxScheduler {
        var requests = 0

        override fun requestDrain() {
            requests++
        }

        override fun scheduleRetry(
            attempt: Int,
            delayMillis: Long,
        ) = Unit
    }

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)
    private var now = Instant.parse("2026-11-09T03:00:00Z")
    private val server = KitchenServer()
    private val store = MemoryStore()
    private val scheduler = CountingScheduler()

    private fun queue() =
        OfflineStaffOrderQueue(
            ServerRepository(server),
            store,
            Connectivity(),
            scope,
            dispatcher,
            now = { now },
            today = { "2026-11-08" },
            outbox = scheduler,
        )

    private val session =
        StudentAuthSession(uid = "u-1", email = "m@x.com", displayName = "Mesero", emailVerified = true)
    private val mode =
        AuthorizedMode(
            role = OperationalRole.WAITER,
            establishmentId = "e-1",
            establishmentName = "Boliche",
            membershipId = "m-1",
        )

    private fun context(tokens: Boolean = true) = OutboxContext(session, listOf(mode)) { if (tokens) "token" else null }

    @Test
    fun `sin internet la comanda no llega a cocina, avisa, y al volver llega una sola vez y sigue su camino`() =
        runTest(dispatcher) {
            val queue = queue()
            queue.start(mesero)
            val order = queue.enqueue(request, "k1", 'M', "Mesa 4", "2× Cerveza")!!
            assertEquals("el guardado pide un recorrido de la bandeja", 1, scheduler.requests)

            queue.flush()
            assertTrue("cocina no ve nada sin internet", server.kitchenSees().isEmpty())
            assertFalse(OutboxDelayRules.isUrgent(order.capturedAt, now.plusSeconds(60)))
            assertTrue(
                "a los 3 min ya es un aviso visible",
                OutboxDelayRules.isUrgent(order.capturedAt, now.plusSeconds(180)),
            )

            server.online = true
            queue.flush()
            assertEquals(1, server.orders.size)
            assertTrue(
                queue.state.value.pending
                    .isEmpty(),
            )

            // Caja cobra, cocina prepara, el mesero entrega: todo con internet.
            server.collect("k1")
            assertEquals(1, server.kitchenSees().size)
            server.advance("k1", OrderState.PREPARING)
            server.advance("k1", OrderState.READY)
            server.advance("k1", OrderState.DELIVERED)
            assertEquals(OrderState.DELIVERED, server.orders.getValue("k1").state)
            assertEquals(1, server.orders.getValue("k1").cashCollections)
            queue.stop()
        }

    @Test
    fun `con internet el pedido entra directo y reintentar no lo duplica`() =
        runTest(dispatcher) {
            server.online = true
            val queue = queue()
            queue.start(mesero)
            queue.enqueue(request, "k2", 'M', "Mesa 1", "")
            queue.flush()
            queue.flush()
            queue.launchFlush()
            assertEquals(1, server.orders.size)
            assertTrue(
                store.saved
                    .getValue(mesero)
                    .orders
                    .isEmpty(),
            )
            queue.stop()
        }

    @Test
    fun `si se pierde la respuesta el segundo intento reconoce el pedido y no lo duplica`() =
        runTest(dispatcher) {
            server.online = true
            server.lostResponses = 1
            val queue = queue()
            queue.start(mesero)
            queue.enqueue(request, "k3", 'M', "Mesa 2", "")
            queue.flush()
            assertEquals("el servidor lo registró aunque la respuesta se perdió", 1, server.orders.size)
            assertEquals("para la app sigue pendiente", 1, queue.state.value.pending.size)
            queue.flush()
            assertEquals(1, server.orders.size)
            assertTrue(
                queue.state.value.pending
                    .isEmpty(),
            )
            queue.stop()
        }

    @Test
    fun `el efectivo cobrado sin internet sale con la app cerrada y entra una sola vez al corte`() =
        runTest(dispatcher) {
            val first = queue()
            first.start(mesero)
            first.enqueue(request, "k4", 'C', "Mostrador", "", collectNow = true)
            // Cierran la app: la cola en memoria desaparece, queda lo guardado en disco.
            first.stop()

            val dispatcher = OutboxDispatcher(listOf(StaffSource(queue(), store, server)))
            assertTrue(dispatcher.hasWork())
            assertEquals("sin internet se reintenta", DrainOutcome.RETRY_LATER, dispatcher.drainAll(context()))
            assertTrue(server.orders.isEmpty())

            server.online = true
            assertEquals(DrainOutcome.DONE, dispatcher.drainAll(context()))
            assertEquals(1, server.orders.size)
            assertEquals(1, server.orders.getValue("k4").cashCollections)
            assertEquals(OrderState.PAID, server.orders.getValue("k4").state)
            assertFalse(dispatcher.hasWork())

            // Otro recorrido (el reintento ya agendado) no manda nada más.
            assertEquals(DrainOutcome.DONE, dispatcher.drainAll(context()))
            assertEquals(1, server.orders.size)
        }

    @Test
    fun `sin token del modo se reintenta y no se pierde nada`() =
        runTest(dispatcher) {
            val first = queue()
            first.start(mesero)
            first.enqueue(request, "k5", 'M', "Mesa 3", "")
            first.stop()
            server.online = true
            val outbox = OutboxDispatcher(listOf(StaffSource(queue(), store, server)))
            assertEquals(DrainOutcome.RETRY_LATER, outbox.drainAll(context(tokens = false)))
            assertTrue(server.orders.isEmpty())
            assertEquals(
                1,
                store.saved
                    .getValue(mesero)
                    .orders.size,
            )
        }

    @Test
    fun `la espera entre intentos crece y se detiene en cinco minutos`() {
        assertEquals(30_000L, OutboxBackoff.delayMillis(0))
        assertEquals(60_000L, OutboxBackoff.delayMillis(1))
        assertEquals(120_000L, OutboxBackoff.delayMillis(2))
        assertEquals(240_000L, OutboxBackoff.delayMillis(3))
        assertEquals(300_000L, OutboxBackoff.delayMillis(4))
        assertEquals(300_000L, OutboxBackoff.delayMillis(50))
        assertEquals(30_000L, OutboxBackoff.delayMillis(-1))
        assertTrue(Duration.ofMillis(OutboxBackoff.MAXIMUM_DELAY_MS) <= Duration.ofMinutes(5))
    }

    @Test
    fun `una cola que truena no detiene a las demas y se reintenta`() =
        runTest(dispatcher) {
            val ran = mutableListOf<String>()
            val broken =
                object : OutboxSource {
                    override val name = "rota"

                    override fun hasWork() = true

                    override suspend fun drain(context: OutboxContext): DrainOutcome =
                        throw IllegalStateException("boom")
                }
            val fine =
                object : OutboxSource {
                    override val name = "sana"

                    override fun hasWork() = true

                    override suspend fun drain(context: OutboxContext): DrainOutcome {
                        ran += name
                        return DrainOutcome.DONE
                    }
                }
            val failures = mutableListOf<String>()
            val outbox = OutboxDispatcher(listOf(broken, fine)) { name, _ -> failures += name }
            assertEquals(DrainOutcome.RETRY_LATER, outbox.drainAll(context()))
            assertEquals(listOf("sana"), ran)
            assertEquals(listOf("rota"), failures)
        }
}
