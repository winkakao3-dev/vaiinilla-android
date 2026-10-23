package com.vaiinilla.app.counter

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.domain.counter.ConnectivityWatcher
import com.vaiinilla.app.domain.counter.CounterScope
import com.vaiinilla.app.domain.counter.OfflineCounterRules
import com.vaiinilla.app.domain.counter.OfflineCounterSnapshot
import com.vaiinilla.app.domain.counter.OfflineCounterStore
import com.vaiinilla.app.domain.counter.ReservedDay
import com.vaiinilla.app.domain.counter.SchoolPolicy
import com.vaiinilla.app.domain.counter.StudentCard
import com.vaiinilla.app.domain.counter.StudentCounterRepository
import com.vaiinilla.app.domain.counter.StudentCredential
import com.vaiinilla.app.domain.counter.StudentCredentialKind
import com.vaiinilla.app.domain.counter.StudentPurchaseItem
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.ui.counter.OfflineCounterQueue
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

/** La cola de Caja sin conexión: guarda en disco, reenvía con la misma llave y una por una, y no pierde nada. */
@OptIn(ExperimentalCoroutinesApi::class)
class OfflineCounterQueueTest {
    private val shift = CounterScope(membershipId = "m-1", establishmentId = "e-1")

    private val tilin =
        StudentCard(
            userId = "u-tilin",
            name = "Tilín Ramírez",
            studentId = "A100",
            group = "3B",
            hasOwnAccount = false,
            balance = BigDecimal("330.00"),
            spentToday = BigDecimal.ZERO,
            dailyLimit = null,
            allergens = emptyList(),
            todayOrders = emptyList(),
        )

    private class MemoryStore : OfflineCounterStore {
        val saved = mutableMapOf<CounterScope, OfflineCounterSnapshot>()

        override fun load(
            scope: CounterScope,
            today: String,
        ) = (saved[scope] ?: OfflineCounterSnapshot(day = today)).startingDay(today)

        override fun save(
            scope: CounterScope,
            snapshot: OfflineCounterSnapshot,
        ) {
            saved[scope] = snapshot
        }
    }

    private class FakeConnectivity : ConnectivityWatcher {
        var callback: (() -> Unit)? = null

        override fun onReconnect(callback: () -> Unit): AutoCloseable {
            this.callback = callback
            return AutoCloseable { this.callback = null }
        }
    }

    /** Responde con lo que diga [next]; anota cada llave con la que se intentó cobrar. */
    private class FakeRepository : StudentCounterRepository {
        val keys = mutableListOf<String>()
        var next: () -> Result<OrderDetail> = { Result.failure(IOException("sin red")) }

        override fun purchase(
            studentId: String,
            items: List<StudentPurchaseItem>,
            pin: String?,
            idempotencyKey: String,
        ): Result<OrderDetail> {
            keys += idempotencyKey
            return next()
        }

        override fun search(text: String) = Result.success(emptyList<StudentCard>())

        override fun find(credential: String) = Result.success<StudentCard?>(null)

        override fun assignCredential(
            studentId: String,
            kind: StudentCredentialKind,
            nfcUid: String?,
            idempotencyKey: String,
        ) = Result.failure<StudentCredential>(UnsupportedOperationException())

        override fun deliver(
            orderId: String,
            expectedVersion: Int,
            credential: String?,
            idempotencyKey: String,
        ) = Result.failure<OrderDetail>(UnsupportedOperationException())

        override fun reservedDay(date: String) = Result.failure<ReservedDay>(UnsupportedOperationException())

        override fun schoolPolicy() = Result.success(SchoolPolicy(isSchool = true, deliveryRequiresQr = true))
    }

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)
    private var now = Instant.parse("2026-10-21T15:00:00Z")

    private fun queue(
        repository: FakeRepository,
        store: MemoryStore = MemoryStore(),
        connectivity: FakeConnectivity = FakeConnectivity(),
    ) = OfflineCounterQueue(repository, store, connectivity, scope, dispatcher, now = { now }, today = { "2026-10-21" })

    @Test
    fun `se guarda en disco y se reenvia con la misma llave del intento`() =
        runTest(dispatcher) {
            val repository = FakeRepository()
            val store = MemoryStore()
            val queue = queue(repository, store)
            queue.start(shift)
            queue.enqueue(tilin, listOf(StudentPurchaseItem(4, 1)), BigDecimal("20.00"), "llave-1")
            assertEquals(
                1,
                store.saved
                    .getValue(shift)
                    .purchases.size,
            )

            // Sin red: se queda pendiente.
            queue.flush()
            assertEquals(listOf("llave-1"), repository.keys)
            assertEquals(1, queue.state.value.pending.size)

            // A los 30 segundos lo intenta solo; ahora entra.
            repository.next = { Result.success(ScreenshotFixtures.sampleOrder()) }
            advanceTimeBy(OfflineCounterRules.RETRY_INTERVAL_MS + 1)
            runCurrent()
            assertEquals(listOf("llave-1", "llave-1"), repository.keys)
            assertTrue(
                queue.state.value.pending
                    .isEmpty(),
            )
            assertTrue(
                store.saved
                    .getValue(shift)
                    .purchases
                    .isEmpty(),
            )
            queue.stop()
        }

    @Test
    fun `una por una y se detiene en la primera falla de red`() =
        runTest(dispatcher) {
            val repository = FakeRepository()
            val queue = queue(repository)
            queue.start(shift)
            queue.enqueue(tilin, listOf(StudentPurchaseItem(4, 1)), BigDecimal("20.00"), "a")
            queue.enqueue(tilin, listOf(StudentPurchaseItem(5, 1)), BigDecimal("10.00"), "b")
            queue.flush()
            assertEquals(listOf("a"), repository.keys)
            assertEquals(2, queue.state.value.pending.size)
            queue.stop()
        }

    @Test
    fun `lo que el servidor rechaza va a Por resolver con su motivo y se quita a mano`() =
        runTest(dispatcher) {
            val repository = FakeRepository()
            repository.next = {
                Result.failure(ApiClientException("INSUFFICIENT_BALANCE", "Tilín ya no tiene saldo suficiente.", 422))
            }
            val queue = queue(repository)
            queue.start(shift)
            queue.enqueue(tilin, listOf(StudentPurchaseItem(4, 1)), BigDecimal("20.00"), "a")
            queue.flush()
            val problem =
                queue.state.value.problems
                    .single()
            assertEquals("Tilín ya no tiene saldo suficiente.", problem.problem)
            assertTrue(
                queue.state.value.pending
                    .isEmpty(),
            )
            // Lo rechazado ya no cuenta contra lo que se le fía.
            assertEquals(null, queue.refusal(BigDecimal("200.00"), tilin))

            queue.dismiss(problem)
            assertEquals(0, queue.state.value.count)
            queue.stop()
        }

    @Test
    fun `con mas de 12 horas ya no se reenvia`() =
        runTest(dispatcher) {
            val repository = FakeRepository()
            val queue = queue(repository)
            queue.start(shift)
            queue.enqueue(tilin, listOf(StudentPurchaseItem(4, 1)), BigDecimal("20.00"), "a")
            now += Duration.ofHours(13)
            queue.flush()
            assertTrue(repository.keys.isEmpty())
            assertEquals(
                OfflineCounterRules.TOO_OLD_PROBLEM,
                queue.state.value.problems
                    .single()
                    .problem,
            )
            queue.stop()
        }

    @Test
    fun `lo guardado sobrevive a otra instancia y se manda al volver la red`() =
        runTest(dispatcher) {
            val store = MemoryStore()
            val first = queue(FakeRepository(), store)
            first.start(shift)
            first.enqueue(tilin, listOf(StudentPurchaseItem(4, 1)), BigDecimal("20.00"), "a")
            first.remember(listOf(tilin))
            first.stop()

            // La app se cerró: otra instancia lee el mismo archivo.
            val repository = FakeRepository()
            val connectivity = FakeConnectivity()
            val second = queue(repository, store, connectivity)
            second.start(shift)
            runCurrent()
            assertEquals(1, second.state.value.pending.size)
            assertEquals(listOf("Tilín Ramírez"), second.cachedCards("tilin").map { it.name })
            // El tope descuenta lo ya guardado de ese alumno.
            assertEquals(
                OfflineCounterRules.Refusal.OverAllowance(BigDecimal("180.00")),
                second.refusal(BigDecimal("190.00"), tilin),
            )

            repository.next = { Result.success(ScreenshotFixtures.sampleOrder()) }
            connectivity.callback?.invoke()
            runCurrent()
            assertTrue(
                second.state.value.pending
                    .isEmpty(),
            )
            second.stop()
        }
}
