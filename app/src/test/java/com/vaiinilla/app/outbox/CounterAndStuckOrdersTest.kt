package com.vaiinilla.app.outbox

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.data.outbox.CounterPurchaseOutboxSource
import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import com.vaiinilla.app.domain.counter.CounterScope
import com.vaiinilla.app.domain.counter.CounterShiftRegistry
import com.vaiinilla.app.domain.counter.OfflineCounterRules
import com.vaiinilla.app.domain.counter.OfflineCounterSnapshot
import com.vaiinilla.app.domain.counter.OfflineCounterStore
import com.vaiinilla.app.domain.counter.OfflineStudentPurchase
import com.vaiinilla.app.domain.counter.ReservedDay
import com.vaiinilla.app.domain.counter.SchoolPolicy
import com.vaiinilla.app.domain.counter.StudentCard
import com.vaiinilla.app.domain.counter.StudentCounterRepository
import com.vaiinilla.app.domain.counter.StudentCredential
import com.vaiinilla.app.domain.counter.StudentCredentialKind
import com.vaiinilla.app.domain.counter.StudentPurchaseItem
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.outbox.DrainOutcome
import com.vaiinilla.app.domain.outbox.OutboxContext
import com.vaiinilla.app.domain.outbox.StuckOrderPolicy
import com.vaiinilla.app.domain.outbox.StuckOrderRules
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal
import java.time.Instant

class CounterAndStuckOrdersTest {
    private val shift = CounterScope(membershipId = "m-1", establishmentId = "e-1")
    private val now = Instant.parse("2026-11-09T18:00:00Z")

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

        override fun scopesWithPurchases() = saved.filter { it.value.pending.isNotEmpty() }.keys.toList()
    }

    private class Repository : StudentCounterRepository {
        val keys = mutableListOf<String>()
        val bearers = mutableListOf<String>()
        var next: () -> Result<OrderDetail> = { Result.success(ScreenshotFixtures.sampleOrder()) }

        override fun purchaseAs(
            bearer: String,
            studentId: String,
            items: List<StudentPurchaseItem>,
            idempotencyKey: String,
        ): Result<OrderDetail> {
            bearers += bearer
            keys += idempotencyKey
            return next()
        }

        override fun purchase(
            studentId: String,
            items: List<StudentPurchaseItem>,
            pin: String?,
            idempotencyKey: String,
        ) = Result.failure<OrderDetail>(UnsupportedOperationException())

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

    private val session = StudentAuthSession(uid = "u-1", email = "c@x.com", displayName = "Caja", emailVerified = true)
    private val cashier =
        AuthorizedMode(
            OperationalRole.CASHIER,
            establishmentId = "e-1",
            establishmentName = "Cafetería",
            membershipId = "m-1",
        )

    private fun context() = OutboxContext(session, listOf(cashier)) { "token-caja" }

    private fun purchase(
        id: String,
        createdAt: Instant = now.minusSeconds(60),
    ) = OfflineStudentPurchase(
        id = id,
        studentId = "u-tilin",
        studentName = "Tilín",
        items = listOf(StudentPurchaseItem(4, 1)),
        total = BigDecimal("20.00"),
        createdAt = createdAt,
    )

    private fun source(
        store: MemoryStore,
        repository: Repository,
    ) = CounterPurchaseOutboxSource(store, repository).also { it.now = { now } }

    @Test
    fun `la venta guardada sale con la app cerrada, con el token del cajero y su misma llave`() =
        runBlocking {
            val store =
                MemoryStore().also {
                    it.save(shift, OfflineCounterSnapshot("2026-11-09", purchases = listOf(purchase("v-1"))))
                }
            val repository = Repository()
            val outcome = source(store, repository).drain(context())
            assertEquals(DrainOutcome.DONE, outcome)
            assertEquals(listOf("v-1"), repository.keys)
            assertEquals(listOf("token-caja"), repository.bearers)
            assertTrue(
                store.saved
                    .getValue(shift)
                    .purchases
                    .isEmpty(),
            )
        }

    @Test
    fun `sin red se queda guardada y se reintenta`() =
        runBlocking {
            val store =
                MemoryStore().also {
                    it.save(shift, OfflineCounterSnapshot("2026-11-09", purchases = listOf(purchase("v-2"))))
                }
            val repository = Repository().also { it.next = { Result.failure(IOException("sin red")) } }
            assertEquals(DrainOutcome.RETRY_LATER, source(store, repository).drain(context()))
            assertEquals(
                1,
                store.saved
                    .getValue(shift)
                    .purchases.size,
            )
        }

    @Test
    fun `un rechazo del servidor queda como problema y no se vuelve a mandar`() =
        runBlocking {
            val store =
                MemoryStore().also {
                    it.save(shift, OfflineCounterSnapshot("2026-11-09", purchases = listOf(purchase("v-3"))))
                }
            val repository =
                Repository().also {
                    it.next =
                        { Result.failure(ApiClientException("INSUFFICIENT_BALANCE", "Sin saldo", 422)) }
                }
            assertEquals(DrainOutcome.DONE, source(store, repository).drain(context()))
            assertNotNull(
                store.saved
                    .getValue(shift)
                    .purchases
                    .single()
                    .problem,
            )
            assertEquals(DrainOutcome.DONE, source(store, repository).drain(context()))
            assertEquals(listOf("v-3"), repository.keys)
        }

    @Test
    fun `una venta de mas de 12 horas no se manda sola`() =
        runBlocking {
            val old = purchase("v-4", createdAt = now.minusSeconds(13 * 3600))
            val store =
                MemoryStore().also {
                    it.save(
                        shift,
                        OfflineCounterSnapshot("2026-11-09", purchases = listOf(old)),
                    )
                }
            val repository = Repository()
            source(store, repository).drain(context())
            assertTrue(repository.keys.isEmpty())
            assertEquals(
                OfflineCounterRules.TOO_OLD_PROBLEM,
                store.saved
                    .getValue(shift)
                    .purchases
                    .single()
                    .problem,
            )
        }

    @Test
    fun `mientras Caja tiene su cola abierta la bandeja no la toca pero sigue pendiente`() =
        runBlocking {
            val store =
                MemoryStore().also {
                    it.save(shift, OfflineCounterSnapshot("2026-11-09", purchases = listOf(purchase("v-5"))))
                }
            val repository = Repository()
            CounterShiftRegistry.enter(shift)
            try {
                assertEquals(DrainOutcome.RETRY_LATER, source(store, repository).drain(context()))
                assertTrue(repository.keys.isEmpty())
            } finally {
                CounterShiftRegistry.leave(shift)
            }
            assertEquals(DrainOutcome.DONE, source(store, repository).drain(context()))
            assertEquals(listOf("v-5"), repository.keys)
        }

    private fun summary(
        state: OrderState,
        updatedAt: Instant,
        deliveryDate: String? = null,
    ) = ScreenshotFixtures
        .sampleOrder(state = state)
        .summary
        .copy(updatedAt = updatedAt.toString(), deliveryDate = deliveryDate)

    @Test
    fun `avisa de las comandas que pasan del tiempo del negocio, la mas vieja primero`() {
        val orders =
            listOf(
                summary(OrderState.PREPARING, now.minusSeconds(25 * 60)),
                summary(OrderState.PAID, now.minusSeconds(40 * 60)),
                summary(OrderState.READY, now.minusSeconds(5 * 60)),
            )
        val stuck = StuckOrderRules.find(orders, StuckOrderPolicy(), now)
        assertEquals(listOf(40L, 25L), stuck.map { it.minutes })
    }

    @Test
    fun `no cuentan lo que espera al cliente, lo cerrado ni lo apartado para otro dia`() {
        val old = now.minusSeconds(3 * 3600)
        val orders =
            listOf(
                summary(OrderState.PENDING_PAYMENT, old),
                summary(OrderState.DELIVERED, old),
                summary(OrderState.CANCELED, old),
                summary(OrderState.PREPARING, old, deliveryDate = "2026-11-10"),
            )
        assertTrue(StuckOrderRules.find(orders, StuckOrderPolicy(), now).isEmpty())
    }

    @Test
    fun `entiende la hora con desfase y descarta la que no se lee`() {
        val withOffset = summary(OrderState.PREPARING, now).copy(updatedAt = "2026-11-09T11:30:00-06:00")
        val garbage = summary(OrderState.PREPARING, now).copy(updatedAt = "ayer")
        val stuck = StuckOrderRules.find(listOf(withOffset, garbage), StuckOrderPolicy(), now)
        assertEquals(listOf(30L), stuck.map { it.minutes })
    }

    @Test
    fun `el negocio puede apagarlo o cambiar los minutos`() {
        val orders = listOf(summary(OrderState.PREPARING, now.minusSeconds(10 * 60)))
        assertTrue(StuckOrderRules.find(orders, StuckOrderPolicy(), now).isEmpty())
        assertEquals(1, StuckOrderRules.find(orders, StuckOrderPolicy(minutes = 10), now).size)
        assertTrue(StuckOrderRules.find(orders, StuckOrderPolicy(enabled = false, minutes = 10), now).isEmpty())
        // Un ajuste fuera de rango se acota: 1 minuto avisaría de todo.
        assertEquals(StuckOrderPolicy.MINIMUM_MINUTES.toLong(), StuckOrderPolicy(minutes = 1).threshold.toMinutes())
    }
}
