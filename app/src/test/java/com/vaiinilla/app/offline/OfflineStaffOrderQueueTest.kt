package com.vaiinilla.app.offline

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.data.offline.OfflineOrderBody
import com.vaiinilla.app.domain.counter.ConnectivityWatcher
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
import com.vaiinilla.app.domain.offline.OfflineOrderAcceptedException
import com.vaiinilla.app.domain.offline.OfflineStaffOrderRules
import com.vaiinilla.app.domain.offline.OfflineStaffOrderStore
import com.vaiinilla.app.domain.offline.OfflineStaffOrdersSnapshot
import com.vaiinilla.app.domain.offline.OfflineStaffScope
import com.vaiinilla.app.domain.repository.OrderRepository
import com.vaiinilla.app.domain.repository.OrderRepositoryException
import com.vaiinilla.app.ui.operational.OfflineStaffOrderQueue
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Duration
import java.time.Instant

/** Mesero y Caja sin internet: el pedido se guarda, se manda solo con la misma llave y no se duplica. */
@OptIn(ExperimentalCoroutinesApi::class)
class OfflineStaffOrderQueueTest {
    private val mesero = OfflineStaffScope(membershipId = "m-1")

    private val request =
        CreateOrderRequest(
            paymentMethod = PaymentMethod.CASH,
            destination = OrderDestination.TAKE_AWAY,
            spaceId = null,
            kitchenNotes = "",
            items = listOf(CreateOrderItem(productId = 1, quantity = 2, optionIds = emptyList())),
        )

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
    }

    private class FakeConnectivity : ConnectivityWatcher {
        var callback: (() -> Unit)? = null

        override fun onReconnect(callback: () -> Unit): AutoCloseable {
            this.callback = callback
            return AutoCloseable { this.callback = null }
        }
    }

    /** Manda lo que diga [next] y anota cada llave con la que se intentó. */
    private class FakeRepository : OrderRepository {
        val keys = mutableListOf<String>()
        val bodies = mutableListOf<String>()
        var next: () -> Result<CreatedOrder> = { Result.failure(IOException("sin red")) }

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
        ): Result<CreatedOrder> {
            keys += idempotencyKey
            bodies += body
            return next()
        }

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

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)
    private var now = Instant.parse("2026-11-09T03:00:00Z")

    private fun queue(
        repository: FakeRepository,
        store: MemoryStore = MemoryStore(),
        connectivity: FakeConnectivity = FakeConnectivity(),
    ) = OfflineStaffOrderQueue(
        repository,
        store,
        connectivity,
        scope,
        dispatcher,
        now = { now },
        today = { "2026-11-08" },
    )

    @Test
    fun `se guarda en disco con folio local y se manda con la llave del intento fallido`() =
        runTest(dispatcher) {
            val repository = FakeRepository()
            val store = MemoryStore()
            val queue = queue(repository, store)
            queue.start(mesero)
            val saved = queue.enqueue(request, "llave-1", 'M', "Pista 3", "2× Cerveza")!!
            assertEquals("M7-001", saved.localFolio)
            assertEquals(1, store.saved.getValue(mesero).orders.size)

            val body = Json.parseToJsonElement(saved.body).jsonObject
            val capture = body.getValue("sin_conexion").jsonObject
            assertEquals(saved.id, capture.getValue("id").jsonPrimitive.content)
            assertEquals("M7-001", capture.getValue("folio_local").jsonPrimitive.content)
            assertEquals(now.toString(), capture.getValue("capturado_en").jsonPrimitive.content)

            // Sin red: se queda pendiente.
            queue.flush()
            assertEquals(listOf("llave-1"), repository.keys)
            assertEquals(1, queue.state.value.pending.size)

            // A los 30 segundos lo intenta solo; ahora entra.
            var sent: Pair<String, Int>? = null
            queue.onSent = { local, folio -> sent = local to folio }
            repository.next = { Result.success(CreatedOrder(order = ScreenshotFixtures.sampleOrder())) }
            advanceTimeBy(OfflineStaffOrderRules.RETRY_INTERVAL_MS + 1)
            runCurrent()
            assertEquals(listOf("llave-1", "llave-1"), repository.keys)
            assertTrue(queue.state.value.pending.isEmpty())
            assertTrue(store.saved.getValue(mesero).orders.isEmpty())
            assertEquals("M7-001", sent?.first)
            queue.stop()
        }

    @Test
    fun `el folio local sigue el consecutivo del dia`() =
        runTest(dispatcher) {
            val queue = queue(FakeRepository())
            queue.start(mesero)
            assertEquals("M7-001", queue.enqueue(request, "a", 'M', "Mesa 1", "")!!.localFolio)
            assertEquals("M7-002", queue.enqueue(request, "b", 'M', "Mesa 2", "")!!.localFolio)
            queue.stop()
        }

    @Test
    fun `en orden y se detiene en la primera falla de red`() =
        runTest(dispatcher) {
            val repository = FakeRepository()
            val queue = queue(repository)
            queue.start(mesero)
            queue.enqueue(request, "a", 'M', "Mesa 1", "")
            queue.enqueue(request, "b", 'M', "Mesa 2", "")
            queue.flush()
            assertEquals(listOf("a"), repository.keys)
            assertEquals(2, queue.state.value.pending.size)
            queue.stop()
        }

    @Test
    fun `si el intento con red si habia llegado, no se duplica`() =
        runTest(dispatcher) {
            val repository = FakeRepository()
            val queue = queue(repository)
            queue.start(mesero)
            queue.enqueue(request, "a", 'C', "Mostrador", "")
            var already: String? = null
            queue.onAlreadyRegistered = { already = it }
            repository.next = {
                Result.failure(
                    OrderRepositoryException(
                        "IDEMPOTENCY_KEY_REUSED",
                        "La llave de idempotencia se usó con una petición distinta.",
                        ApiClientException("IDEMPOTENCY_KEY_REUSED", "otra petición", 409),
                    ),
                )
            }
            queue.flush()
            assertTrue(queue.state.value.pending.isEmpty())
            assertTrue(queue.state.value.problems.isEmpty())
            assertEquals("C7-001", already)
            queue.stop()
        }

    @Test
    fun `un rechazo del servidor queda como problema y ya no se reintenta`() =
        runTest(dispatcher) {
            val repository = FakeRepository()
            val queue = queue(repository)
            queue.start(mesero)
            queue.enqueue(request, "a", 'M', "Mesa 1", "")
            repository.next = {
                Result.failure(OrderRepositoryException("INVALID_SPACE", "Espacio inválido."))
            }
            queue.flush()
            assertTrue(queue.state.value.pending.isEmpty())
            assertEquals(1, queue.state.value.problems.size)
            queue.flush()
            assertEquals(listOf("a"), repository.keys)
            queue.stop()
        }

    @Test
    fun `mas de 12 horas despues ya no se manda solo`() =
        runTest(dispatcher) {
            val repository = FakeRepository()
            val queue = queue(repository)
            queue.start(mesero)
            queue.enqueue(request, "a", 'M', "Mesa 1", "")
            now = now.plus(Duration.ofHours(13))
            queue.flush()
            assertTrue(repository.keys.isEmpty())
            assertEquals(OfflineStaffOrderRules.TOO_OLD_PROBLEM, queue.state.value.problems.single().problem)
            queue.stop()
        }

    @Test
    fun `sin membresia no guarda nada`() =
        runTest(dispatcher) {
            val queue = queue(FakeRepository())
            assertNull(queue.enqueue(request, "a", 'M', "Mesa 1", ""))
        }

    @Test
    fun `un 500 del servidor no lo aparta y se vuelve a intentar con la misma llave`() =
        runTest(dispatcher) {
            val repository = FakeRepository()
            val queue = queue(repository)
            queue.start(mesero)
            queue.enqueue(request, "a", 'M', "Mesa 1", "")
            repository.next = {
                Result.failure(
                    OrderRepositoryException(
                        "INTERNAL_ERROR",
                        "Error interno.",
                        ApiClientException("INTERNAL_ERROR", "Error interno.", 500),
                    ),
                )
            }
            queue.flush()
            assertEquals(1, queue.state.value.pending.size)
            assertTrue(queue.state.value.problems.isEmpty())
            repository.next = {
                Result.failure(ApiClientException("UNAUTHORIZED", "Sesión vencida.", 401))
            }
            queue.flush()
            assertEquals(1, queue.state.value.pending.size)
            assertEquals(listOf("a", "a"), repository.keys)
            queue.stop()
        }

    @Test
    fun `reintentar regresa a la fila lo apartado por un rechazo`() =
        runTest(dispatcher) {
            val repository = FakeRepository()
            val queue = queue(repository)
            queue.start(mesero)
            queue.enqueue(request, "a", 'M', "Mesa 1", "")
            repository.next = {
                Result.failure(ApiClientException("NO_OPEN_CASH_SESSION", "Abre la caja.", 422))
            }
            queue.flush()
            assertEquals(1, queue.state.value.problems.size)
            repository.next = { Result.success(CreatedOrder(order = ScreenshotFixtures.sampleOrder())) }
            queue.retryAll()
            runCurrent()
            assertTrue(queue.state.value.problems.isEmpty())
            assertTrue(queue.state.value.pending.isEmpty())
            queue.stop()
        }

    @Test
    fun `lo que Caja ya cobro llega con cobrar_ahora`() =
        runTest(dispatcher) {
            val queue = queue(FakeRepository())
            queue.start(mesero)
            val cobrado = queue.enqueue(request, "a", 'C', "Mostrador", "", collectNow = true)!!
            val suelto = queue.enqueue(request, "b", 'M', "Mesa 1", "")!!
            assertEquals("true", Json.parseToJsonElement(cobrado.body).jsonObject.getValue("cobrar_ahora").toString())
            assertNull(Json.parseToJsonElement(suelto.body).jsonObject["cobrar_ahora"])
            queue.stop()
        }

    @Test
    fun `lo guardado de otra membresia se manda con su propio envio sin abrir su modo`() =
        runTest(dispatcher) {
            val store = MemoryStore()
            val caja = OfflineStaffScope(membershipId = "c-9")
            val first = queue(FakeRepository(), store)
            first.start(caja)
            first.enqueue(request, "a", 'C', "Mostrador", "")
            first.stop()

            val queue = queue(FakeRepository(), store)
            queue.start(mesero)
            val sentKeys = mutableListOf<String>()
            queue.rescue(caja) { _, key ->
                sentKeys += key
                Result.success(Unit)
            }
            assertEquals(listOf("a"), sentKeys)
            assertTrue(store.saved.getValue(caja).orders.isEmpty())
            queue.stop()
        }

    @Test
    fun `un pedido con el mismo 500 tres veces se aparta y no atora a los de atras`() =
        runTest(dispatcher) {
            val repository = FakeRepository()
            val queue = queue(repository)
            queue.start(mesero)
            queue.enqueue(request, "malo", 'M', "Mesa 1", "")
            queue.enqueue(request, "bueno", 'M', "Mesa 2", "")
            repository.next = { Result.failure(ApiClientException("INTERNAL_ERROR", "Error interno.", 500)) }
            queue.flush()
            queue.flush()
            assertEquals(listOf("malo", "malo"), repository.keys)
            repository.next = {
                if (repository.keys.last() == "malo") {
                    Result.failure(ApiClientException("INTERNAL_ERROR", "Error interno.", 500))
                } else {
                    Result.success(CreatedOrder(order = ScreenshotFixtures.sampleOrder()))
                }
            }
            queue.flush()
            assertEquals(listOf("malo", "malo", "malo", "bueno"), repository.keys)
            assertEquals(OfflineStaffOrderRules.SERVER_FAILURE_PROBLEM, queue.state.value.problems.single().problem)
            assertTrue(queue.state.value.pending.isEmpty())
            queue.stop()
        }

    @Test
    fun `un 503 sin sobre por servidor caido no cuenta y nunca se aparta`() =
        runTest(dispatcher) {
            val repository = FakeRepository()
            val queue = queue(repository)
            queue.start(mesero)
            queue.enqueue(request, "a", 'M', "Mesa 1", "")
            repository.next = { Result.failure(ApiClientException("HTTP_503", "Servicio no disponible.", 503)) }
            repeat(5) { queue.flush() }
            assertEquals(1, queue.state.value.pending.size)
            assertTrue(queue.state.value.problems.isEmpty())
            queue.stop()
        }

    @Test
    fun `si respondio 2xx con un cuerpo ilegible, ya llego y no se vuelve a mandar`() =
        runTest(dispatcher) {
            val repository = FakeRepository()
            val queue = queue(repository)
            queue.start(mesero)
            queue.enqueue(request, "a", 'M', "Mesa 1", "")
            repository.next = { Result.failure(OfflineOrderAcceptedException(IllegalArgumentException("json"))) }
            queue.flush()
            assertTrue(queue.state.value.pending.isEmpty())
            assertTrue(queue.state.value.problems.isEmpty())
            queue.stop()
        }
}
