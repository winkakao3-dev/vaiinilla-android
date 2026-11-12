package com.vaiinilla.app.ui.sharedtable

import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.domain.model.SharedTable
import com.vaiinilla.app.domain.model.SharedTableAmounts
import com.vaiinilla.app.domain.repository.SharedTableRepository
import com.vaiinilla.app.ui.operational.StaffHarness
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/** C08: la mesa de un negocio no pasa a otro; con ella el menú de allá llamaba al mesero de acá (404). */
@OptIn(ExperimentalCoroutinesApi::class)
class SharedTableVenueTest {
    private val created = mutableListOf<SharedTableViewModel>()

    private fun viewModel(repository: SharedTableRepository = FakeRepository()) =
        SharedTableViewModel(repository).also { created += it }

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        // Un sondeo o una relectura que sigue en Dispatchers.IO regresa a Main: si regresa mientras se quita
        // Main, resetMain truena con «Dispatchers.Main is used concurrently». Se terminan antes.
        runBlocking {
            created.forEach { vm ->
                vm.viewModelScope.coroutineContext.job.children
                    .forEach { it.cancelAndJoin() }
            }
        }
        StaffHarness.restoreMain()
    }

    @Test
    fun `entering another business forgets the table`() {
        val vm = viewModel()
        vm.bindVenue("usagi")
        runBlocking { vm.joinForWaiterCall("qr-mesa-5", "Kao") }
        assertEquals(5, spaceId(vm))

        vm.bindVenue("usagi")
        assertEquals(5, spaceId(vm))

        vm.bindVenue("sandbox")
        assertNull(vm.uiState.value.table)
    }

    @Test
    fun `si esto lo pago yo falla el error va junto al pedido y la mesa se vuelve a leer`() {
        val repository = FakeRepository(claimFails = true)
        val vm = viewModel(repository)

        vm.claim(folio = 7, payIt = true)
        // El reclamo y la relectura corren en Dispatchers.IO: se espera a que la mesa se vuelva a leer.
        waitUntil { repository.currentCalls == 1 && !vm.uiState.value.busy }

        assertEquals(7, vm.uiState.value.errorFolio)
        assertEquals("Ese pedido ya está pagado", vm.uiState.value.error)
        assertEquals(1, repository.currentCalls)
    }

    @Test
    fun `al pasar de Mis pedidos a la mesa la que se va no apaga la consulta de la que llega`() {
        val vm = viewModel()
        vm.onVisible(SHARED_TABLE_WATCHER_ORDERS)
        // Navegar: la mesa arranca antes de que Mis pedidos se detenga (C05).
        vm.onVisible(SHARED_TABLE_WATCHER_PAGE)
        vm.onHidden(SHARED_TABLE_WATCHER_ORDERS)
        // Al salir de la composición se vuelve a avisar: no cuenta dos veces.
        vm.onHidden(SHARED_TABLE_WATCHER_ORDERS)
        assertTrue(vm.polling)

        vm.onHidden(SHARED_TABLE_WATCHER_PAGE)
        assertFalse(vm.polling)
    }

    @Test
    fun `al llegar por QR antes de tener sesion vuelve a preguntar quien esta en la mesa`() {
        val repository = FakeRepository(othersFailuresBeforeAnswer = 2, others = listOf("Ana"))
        val vm = viewModel(repository)
        vm.othersRetryMs = 20

        vm.watchSpace("qr-mesa-5")

        waitUntil { vm.uiState.value.othersAtTable == listOf("Ana") }
        assertEquals(3, repository.othersCalls)
    }

    @Test
    fun `si nunca contesta quien esta en la mesa deja de preguntar`() {
        val repository = FakeRepository(othersFailuresBeforeAnswer = Int.MAX_VALUE)
        val vm = viewModel(repository)
        vm.othersRetryMs = 5

        vm.watchSpace("qr-mesa-5")

        waitUntil { repository.othersCalls == 4 }
        Thread.sleep(60)
        assertEquals(4, repository.othersCalls)
        assertTrue(vm.uiState.value.othersAtTable.isEmpty())
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "La mesa no se volvió a leer a tiempo." }
            Thread.sleep(10)
        }
    }

    private fun spaceId(vm: SharedTableViewModel): Int? {
        val table = vm.uiState.value.table ?: return null
        return table.spaceId
    }

    private class FakeRepository(
        private val claimFails: Boolean = false,
        private val othersFailuresBeforeAnswer: Int = 0,
        private val others: List<String> = emptyList(),
    ) : SharedTableRepository {
        @Volatile var currentCalls = 0

        @Volatile var othersCalls = 0

        override fun othersAt(qrToken: String): Result<List<String>> {
            othersCalls++
            return if (othersCalls <= othersFailuresBeforeAnswer) {
                Result.failure(IllegalStateException("sin sesión"))
            } else {
                Result.success(others)
            }
        }

        private val zero = SharedTableAmounts(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        private val table =
            SharedTable(
                spaceId = 5,
                spaceName = "Mesa 5",
                spaceType = "mesa",
                myAlias = "Kao",
                accountOpen = true,
                participants = emptyList(),
                groups = emptyList(),
                totals = zero,
                myShare = zero,
            )

        override fun current(): Result<SharedTable?> {
            currentCalls++
            return Result.success(table)
        }

        override fun join(
            qrToken: String,
            alias: String,
        ): Result<SharedTable> = Result.success(table)

        override fun leave(): Result<Unit> = Result.success(Unit)

        override fun claim(
            folio: Int,
            payIt: Boolean,
        ): Result<SharedTable> =
            if (claimFails) {
                Result.failure(
                    IllegalStateException("Ese pedido ya está pagado"),
                )
            } else {
                Result.success(table)
            }
    }
}
