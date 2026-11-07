package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.domain.repository.CallReason
import com.vaiinilla.app.domain.repository.CallStatus
import com.vaiinilla.app.domain.repository.WaiterBoard
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * El Mesero dentro del ViewModel (N09 y Z03): lo llaman y sabe para qué, va y atiende, y entrega con o sin QR.
 * Corre en la JVM con Robolectric; no cuenta como recorrido en teléfono.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecorridosMeseroViewModelTest {
    private val repo = FakeWaiterRepository()
    private lateinit var vm: WaiterViewModel

    private val ui get() = vm.uiState.value

    @Before
    fun setUp() {
        StaffHarness.installMain()
        vm = WaiterHarness.viewModel(repo)
    }

    private fun load(board: WaiterBoard) {
        repo.board = board
        vm.refresh()
        StaffHarness.awaitUntil("el tablero cargado") { !ui.loading }
    }

    @Test
    fun n09_unaLlamadaNuevaAvisaDeQuienEsYParaQue() {
        load(WaiterBoard(listOf(WaiterHarness.table()), callsEnabled = true))
        load(WaiterBoard(listOf(WaiterHarness.table(call = WaiterHarness.call(reason = CallReason.PROBLEMA))), callsEnabled = true))
        StaffHarness.awaitUntil("el aviso de la llamada") {
            ui.toastMessage == "Mesa 4 te llama · ${CallReason.PROBLEMA.staffLabel}"
        }
    }

    @Test
    fun n09_laLlamadaQueYaEstabaAlAbrirNoAvisaOtraVez() {
        load(WaiterBoard(listOf(WaiterHarness.table(call = WaiterHarness.call())), callsEnabled = true))
        Thread.sleep(150)
        assertEquals(null, ui.toastMessage)
    }

    @Test
    fun n09_irYAtenderMandanLosEstadosEnOrden() {
        val call = WaiterHarness.call()
        load(WaiterBoard(listOf(WaiterHarness.table(call = call)), callsEnabled = true))
        vm.markGoing(call)
        StaffHarness.awaitUntil("voy") { repo.transitions.size == 1 && !ui.acting }
        vm.markAttended(call)
        StaffHarness.awaitUntil("atendida") { repo.transitions.size == 2 && !ui.acting }
        assertEquals(listOf("c1" to CallStatus.EN_CAMINO, "c1" to CallStatus.ATENDIDA), repo.transitions.toList())
    }

    @Test
    fun z03_conQrObligatorioSinCodigoNoEntrega() {
        val order = WaiterHarness.order()
        load(WaiterBoard(listOf(WaiterHarness.table(orders = listOf(order))), callsEnabled = true, deliveryRequiresQr = true))
        vm.deliver(order, "  ")
        StaffHarness.awaitUntil("el aviso del código") { ui.toastMessage == "Pega el código del QR para confirmar la entrega." }
        assertTrue(repo.deliveries.isEmpty())
    }

    @Test
    fun z03_conElCodigoDelQrEntregaConEseCodigo() {
        val order = WaiterHarness.order()
        load(WaiterBoard(listOf(WaiterHarness.table(orders = listOf(order))), callsEnabled = true, deliveryRequiresQr = true))
        vm.deliver(order, " VN-7 ")
        StaffHarness.awaitUntil("la entrega") { repo.deliveries.isNotEmpty() && !ui.acting }
        assertEquals(listOf("o1" to "VN-7"), repo.deliveries.toList())
        StaffHarness.awaitUntil("el aviso") { ui.toastMessage == "#7 entregado" }
    }

    @Test
    fun z03_sinQrEntregaConSoloConfirmar() {
        val order = WaiterHarness.order()
        load(WaiterBoard(listOf(WaiterHarness.table(orders = listOf(order))), callsEnabled = true, deliveryRequiresQr = false))
        vm.deliver(order, null)
        StaffHarness.awaitUntil("la entrega") { repo.deliveries.isNotEmpty() && !ui.acting }
        assertEquals(listOf<Pair<String, String?>>("o1" to null), repo.deliveries.toList())
    }

    @Test
    fun z03_unPedidoQueElNegocioDispensaDelQrEntregaSinCodigo() {
        val order = WaiterHarness.order(withoutQr = true)
        load(WaiterBoard(listOf(WaiterHarness.table(orders = listOf(order))), callsEnabled = true, deliveryRequiresQr = true))
        vm.deliver(order, null)
        StaffHarness.awaitUntil("la entrega") { repo.deliveries.isNotEmpty() && !ui.acting }
        assertEquals(listOf<Pair<String, String?>>("o1" to null), repo.deliveries.toList())
    }
}
