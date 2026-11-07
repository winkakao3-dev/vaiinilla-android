package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.domain.repository.AccountPaymentMethod
import com.vaiinilla.app.domain.repository.CallStatus
import com.vaiinilla.app.domain.repository.WaiterBoard
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * El tablero del mesero (I01) y cobrar la cuenta de una mesa con la terminal (I05). Corre en la JVM con
 * Robolectric; no cuenta como recorrido en teléfono ni prueba la terminal física.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecorridosCuentaMeseroViewModelTest {
    private val repo = FakeWaiterRepository()
    private lateinit var vm: WaiterViewModel

    private val ui get() = vm.uiState.value

    @Before
    fun setUp() {
        StaffHarness.installMain()
    }

    private fun start(board: WaiterBoard) {
        repo.board = board
        vm = WaiterHarness.viewModel(repo)
        vm.bindContext("m-1")
        vm.refresh()
        StaffHarness.awaitUntil("el tablero") { !ui.loading && ui.boardLoaded }
    }

    @Test
    fun i01_elTableroMuestraLasMesasConSusLlamadasYPedidos() {
        val call = WaiterHarness.call()
        start(WaiterBoard(listOf(WaiterHarness.table(call = call, orders = listOf(WaiterHarness.order()))), callsEnabled = true))
        assertEquals(1, ui.tables.size)
        assertEquals("Mesa 4", ui.tables.single().space.name)
        assertEquals(call.id, ui.tables.single().call?.id)
        assertEquals(listOf("o1"), ui.tables.single().orders.map { it.id })
        assertNull(ui.errorMessage)
    }

    @Test
    fun i01_siElTableroFallaDespuesDeCargarSeConservanLasMesasYSeCuentaElFallo() {
        start(WaiterBoard(listOf(WaiterHarness.table()), callsEnabled = true))
        repo.boardFails = IllegalStateException("sin red")
        vm.refresh()
        StaffHarness.awaitUntil("el fallo") { ui.consecutiveBoardFailures >= 1 && !ui.loading }
        assertEquals(1, ui.tables.size)
        assertNotNull(ui.errorMessage)
        repo.boardFails = null
        vm.refresh()
        StaffHarness.awaitUntil("la recuperación") { ui.consecutiveBoardFailures == 0 && !ui.loading }
        assertNull(ui.errorMessage)
    }

    @Test
    fun i01_filtrarLoQueAtiendoNoPierdeLasMesas() {
        start(WaiterBoard(listOf(WaiterHarness.table(call = WaiterHarness.call(status = CallStatus.PENDIENTE))), callsEnabled = true))
        vm.setFilterAttending(true)
        assertTrue(ui.filterAttending)
        assertEquals(1, ui.tables.size)
    }

    @Test
    fun i05_cobrarConLaTerminalNoMandaEfectivoYMuestraLaCuentaSaldada() {
        start(WaiterBoard(listOf(WaiterHarness.table()), callsEnabled = true))
        vm.collectAccount(4, AccountPaymentMethod.TERMINAL, null, "180.00")
        StaffHarness.awaitUntil("el cobro") { ui.lastCollection != null && !ui.acting }
        val (method, received, _) = repo.collections.single()
        assertEquals(AccountPaymentMethod.TERMINAL, method)
        assertNull(received)
        assertTrue(ui.lastCollection!!.settled)
        assertEquals("Cuenta cobrada", ui.toastMessage)
    }

    @Test
    fun i05_unFalloYElReintentoDelMismoCobroUsanLaMismaLlave() {
        start(WaiterBoard(listOf(WaiterHarness.table()), callsEnabled = true))
        repo.collectFails = IllegalStateException("sin red")
        vm.collectAccount(4, AccountPaymentMethod.TERMINAL, null, "180.00")
        StaffHarness.awaitUntil("el primer intento") { repo.collections.size == 1 && !ui.acting }
        assertNull(ui.lastCollection)
        repo.collectFails = null
        vm.collectAccount(4, AccountPaymentMethod.TERMINAL, null, "180.00")
        StaffHarness.awaitUntil("el segundo intento") { ui.lastCollection != null && !ui.acting }
        assertEquals(repo.collections[0].third, repo.collections[1].third)
    }

    @Test
    fun i05_enEfectivoSeMandaLoRecibidoYSeAvisaElCambio() {
        start(WaiterBoard(listOf(WaiterHarness.table()), callsEnabled = true))
        vm.collectAccount(4, AccountPaymentMethod.CASH, "200.00", "170.00")
        StaffHarness.awaitUntil("el cobro") { ui.lastCollection != null && !ui.acting }
        assertEquals("200.00", repo.collections.single().second)
        assertEquals("30.00", ui.lastCollection!!.change)
    }
}
