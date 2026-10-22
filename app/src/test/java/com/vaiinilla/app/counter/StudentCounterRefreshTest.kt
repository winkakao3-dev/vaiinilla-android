package com.vaiinilla.app.counter

import com.vaiinilla.app.domain.counter.ConnectivityWatcher
import com.vaiinilla.app.domain.counter.CounterScope
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
import com.vaiinilla.app.ui.counter.ChargePhase
import com.vaiinilla.app.ui.counter.StudentCounterViewModel
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal

/** Tras cobrar, Caja vuelve a pedir la ficha: saldo nuevo en la ficha, en su fila y en lo que recuerda la cola. */
@OptIn(ExperimentalCoroutinesApi::class)
class StudentCounterRefreshTest {
    private val shift = CounterScope(membershipId = "m-1", establishmentId = "e-1")

    private fun card(
        balance: String,
        spent: String,
        studentId: String? = "A100",
    ) = StudentCard(
        userId = "u-tilin",
        name = "Tilín Ramírez",
        studentId = studentId,
        group = "3B",
        hasOwnAccount = false,
        balance = BigDecimal(balance),
        spentToday = BigDecimal(spent),
        dailyLimit = null,
        allergens = emptyList(),
        todayOrders = emptyList(),
    )

    private val before = card("330.00", "0.00")
    private val after = card("310.00", "20.00")
    private val other = before.copy(userId = "u-otra", name = "Tilina Ruiz", studentId = "A200")

    private class MemoryStore : OfflineCounterStore {
        override fun load(
            scope: CounterScope,
            today: String,
        ) = OfflineCounterSnapshot(day = today)

        override fun save(
            scope: CounterScope,
            snapshot: OfflineCounterSnapshot,
        ) = Unit
    }

    private object NoReconnect : ConnectivityWatcher {
        override fun onReconnect(callback: () -> Unit) = AutoCloseable {}
    }

    private class FakeRepository : StudentCounterRepository {
        val searches = mutableListOf<String>()
        var searchResult: (String) -> Result<List<StudentCard>> = { Result.success(emptyList()) }

        override fun search(text: String): Result<List<StudentCard>> {
            searches += text
            return searchResult(text)
        }

        override fun purchase(
            studentId: String,
            items: List<StudentPurchaseItem>,
            pin: String?,
            idempotencyKey: String,
        ): Result<OrderDetail> = Result.success(ScreenshotFixtures.sampleOrder())

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

        override fun schoolPolicy() = Result.success(SchoolPolicy(isSchool = true, deliveryRequiresQr = false))
    }

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    private fun TestScope.counterWithTilinSelected(repository: FakeRepository): StudentCounterViewModel {
        val model = StudentCounterViewModel(repository, MemoryStore(), NoReconnect, dispatcher, scope)
        model.setShift(shift)
        repository.searchResult = { Result.success(listOf(before, other)) }
        model.updateQuery("Til")
        testScheduler.advanceTimeBy(1_000)
        runCurrent()
        model.select(before)
        model.step(productId = 4, delta = 1)
        repository.searches.clear()
        return model
    }

    @Test
    fun `al cobrar vuelve a pedir la ficha y actualiza la ficha, su fila y la cola`() =
        runTest(dispatcher) {
            val repository = FakeRepository()
            val model = counterWithTilinSelected(repository)
            repository.searchResult = { Result.success(listOf(after)) }

            model.charge(BigDecimal("20.00"))
            runCurrent()

            val state = model.uiState.value
            assertTrue(state.chargePhase is ChargePhase.Charged)
            assertEquals(listOf("A100"), repository.searches)
            assertEquals(after, state.selected)
            assertEquals(listOf(after, other), state.results)
            // Lo que la cola usa sin conexión ya trae lo gastado hoy.
            assertEquals(listOf(after), model.offline.cachedCards("A100"))
            model.setShift(null)
        }

    @Test
    fun `si la ficha no llega se queda la que habia`() =
        runTest(dispatcher) {
            val repository = FakeRepository()
            val model = counterWithTilinSelected(repository)
            repository.searchResult = { Result.failure(IOException("sin red")) }

            model.charge(BigDecimal("20.00"))
            runCurrent()

            val state = model.uiState.value
            assertTrue(state.chargePhase is ChargePhase.Charged)
            assertEquals(before, state.selected)
            assertEquals(listOf(before, other), state.results)
            assertEquals(listOf(before), model.offline.cachedCards("A100"))
            model.setShift(null)
        }

    @Test
    fun `con una matricula de una letra la ficha se busca por nombre`() =
        runTest(dispatcher) {
            val repository = FakeRepository()
            val model = StudentCounterViewModel(repository, MemoryStore(), NoReconnect, dispatcher, scope)
            model.setShift(shift)
            val short = card("330.00", "0.00", studentId = "7")
            model.select(short)
            repository.searchResult = { Result.success(listOf(card("300.00", "30.00", studentId = "7"))) }

            model.refreshSelected()

            assertEquals(listOf("Tilín Ramírez"), repository.searches)
            assertEquals(
                BigDecimal("300.00"),
                model.uiState.value.selected
                    ?.balance,
            )
            model.setShift(null)
        }
}
