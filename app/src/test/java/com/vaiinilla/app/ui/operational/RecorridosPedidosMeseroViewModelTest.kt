package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderOrigin
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
 * Tomar pedidos desde el mesero y el admin (I02, G03, T01, O03): a una mesa, en mostrador, el borrador del admin y
 * sin internet. Corre en la JVM con Robolectric; no cuenta como recorrido en teléfono.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecorridosPedidosMeseroViewModelTest {
    private val repo = FakeWaiterRepository()
    private val orders = FakeOrders()
    private val catalog = FakeCatalog()
    private lateinit var vm: WaiterViewModel

    private val ui get() = vm.uiState.value
    private val line = CartLine(sampleProduct(1, "Hamburguesa"), 2, emptySet())

    private fun start(
        board: WaiterBoard = WaiterBoard(listOf(WaiterHarness.table()), callsEnabled = true),
        internet: Boolean = true,
    ) {
        catalog.products = listOf(sampleProduct(1, "Hamburguesa"))
        repo.board = board
        vm = WaiterHarness.viewModel(repo, orders, catalog, internet)
        vm.bindContext("m-1")
        vm.refresh()
        StaffHarness.awaitUntil("el tablero") { !ui.loading }
    }

    @Before
    fun setUp() {
        StaffHarness.installMain()
    }

    @Test
    fun i02_elPedidoDeUnaMesaVaALaMesaConOrigenMostradorYSeCobraEnCaja() {
        start()
        vm.openTableOrder(WaiterHarness.space())
        StaffHarness.awaitUntil("el menú de la hoja") { ui.counterCatalog != null }
        vm.submitCounterOrder(listOf(line), "")
        StaffHarness.awaitUntil("el aviso") { ui.toastMessage == "Pedido #7 de Mesa 4 enviado a Caja" }
        val request = orders.createdRequests.single().first
        assertEquals(OrderDestination.IN_SPACE, request.destination)
        assertEquals(4, request.spaceId)
        assertEquals(OrderOrigin.MOSTRADOR, request.origin)
        assertEquals(false, request.payAtEnd)
        assertEquals(false, ui.counterOrderOpen)
    }

    @Test
    fun i02_siElNegocioDejaPagarAlFinalElPedidoVaALaCuentaYAvisaALaCocina() {
        start(WaiterBoard(listOf(WaiterHarness.table()), callsEnabled = true, allowsPayAtEnd = true))
        vm.openTableOrder(WaiterHarness.space())
        StaffHarness.awaitUntil("el menú de la hoja") { ui.counterCatalog != null }
        vm.submitCounterOrder(listOf(line), "")
        StaffHarness.awaitUntil("el aviso") { ui.toastMessage == "Pedido #7 de Mesa 4 enviado a cocina" }
        assertEquals(
            true,
            orders.createdRequests
                .single()
                .first.payAtEnd,
        )
    }

    @Test
    fun g03_elPedidoDeMostradorNoLlevaMesaYSiCajaLoCobraYaDevuelveElIdParaAbrirElCobro() {
        start()
        vm.openCounterOrder()
        StaffHarness.awaitUntil("el menú de la hoja") { ui.counterCatalog != null }
        var createdId: String? = null
        vm.submitCounterOrder(listOf(line), "Ana", onCreated = { createdId = it })
        StaffHarness.awaitUntil("el id del pedido creado") { createdId != null }
        val request = orders.createdRequests.single().first
        assertEquals(OrderDestination.TAKE_AWAY, request.destination)
        assertEquals(null, request.spaceId)
        assertEquals("Ana", request.customerName)
        assertEquals("nuevo-1", createdId)
    }

    @Test
    fun g03_siElServidorRechazaLaHojaSeQuedaAbiertaConElErrorYElReintentoUsaLaMismaLlave() {
        orders.createFails = java.lang.IllegalStateException("No hay una caja abierta.")
        start()
        vm.openCounterOrder()
        StaffHarness.awaitUntil("el menú de la hoja") { ui.counterCatalog != null }
        vm.submitCounterOrder(listOf(line), "")
        StaffHarness.awaitUntil("el error") { ui.counterOrderError != null && !ui.counterOrderSending }
        assertTrue(ui.counterOrderOpen)
        vm.submitCounterOrder(listOf(line), "")
        StaffHarness.awaitUntil("el segundo intento") { orders.createdRequests.size == 2 && !ui.counterOrderSending }
        assertEquals(orders.createdRequests[0].second, orders.createdRequests[1].second)
    }

    @Test
    fun t01_elBorradorDelAdminSeManda_ySeLimpiaParaElSiguiente() {
        start()
        vm.updateAdminOrderDraft(mapOf(1 to line), "Pepe")
        assertEquals(1, ui.adminOrderDraft.lines.size)
        vm.submitAdminOrder()
        StaffHarness.awaitUntil("el pedido creado") {
            orders.createdRequests.size == 1 &&
                ui.adminOrderDraft.lines.isEmpty()
        }
        assertEquals(
            "Pepe",
            orders.createdRequests
                .single()
                .first.customerName,
        )
    }

    @Test
    fun t01_unBorradorVacioNoSeManda() {
        start()
        vm.submitAdminOrder()
        Thread.sleep(200)
        assertTrue(orders.createdRequests.isEmpty())
    }

    @Test
    fun o03_sinInternetElPedidoSeGuardaConFolioLocalYNoSeMandaAlServidor() {
        start(internet = false)
        vm.openCounterOrder()
        StaffHarness.awaitUntil("el menú de la hoja") { ui.counterCatalog != null }
        vm.submitCounterOrder(listOf(line), "")
        StaffHarness.awaitUntil("el aviso de sin internet") {
            ui.toastMessage?.startsWith("Sin internet: Pedido ") ==
                true
        }
        assertTrue(ui.toastMessage!!.endsWith("guardado. Llévalo a Caja; se manda solo al volver la red."))
        assertTrue(orders.createdRequests.isEmpty())
        assertEquals(false, ui.counterOrderOpen)
    }
}
