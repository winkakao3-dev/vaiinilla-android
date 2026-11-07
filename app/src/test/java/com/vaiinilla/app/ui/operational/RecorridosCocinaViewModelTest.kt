package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.domain.model.OrderState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Cocina dentro del ViewModel y «Se acabó» desde Cocina y desde Caja (Z06), con un servidor en memoria.
 * Corre en la JVM con Robolectric; no cuenta como recorrido en teléfono.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecorridosCocinaViewModelTest {
    private val orders = FakeOrders()
    private val catalog = FakeCatalog()
    private lateinit var mainThread: java.util.concurrent.ExecutorService

    @Before
    fun setUp() {
        mainThread = java.util.concurrent.Executors.newSingleThreadExecutor()
        Dispatchers.setMain(mainThread.asCoroutineDispatcher())
        catalog.products = listOf(sampleProduct(1, "Hamburguesa"), sampleProduct(2, "Refresco"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        mainThread.shutdownNow()
    }

    private fun kitchen() = StaffHarness.kitchenViewModel(RuntimeEnvironment.getApplication(), orders, catalog)

    private fun cashier() = StaffHarness.cashierViewModel(RuntimeEnvironment.getApplication(), orders = orders, catalog = catalog)

    @Test
    fun z06_cocinaApagaLoQueSeAcaboYElMenuLoMarcaAgotado() {
        val vm = kitchen()
        vm.start()
        // El menú de Cocina pide el catálogo al abrirse, no al arrancar el rol.
        vm.refreshCatalog()
        StaffHarness.awaitUntil("el catálogo de Cocina") { vm.uiState.value.catalog?.products?.size == 2 }
        vm.setProductAvailable(1, false)
        StaffHarness.awaitUntil("el producto agotado") {
            vm.uiState.value.catalog?.products?.first { it.id == 1 }?.available == false && !vm.uiState.value.catalogBusy
        }
        assertEquals(listOf(1 to false), catalog.availabilityCalls.toList())
        assertTrue(vm.uiState.value.catalog!!.products.first { it.id == 2 }.available)
    }

    @Test
    fun z06_cajaApagaLoQueSeAcaboConElMismoCamino() {
        val vm = cashier()
        vm.start()
        StaffHarness.awaitUntil("el catálogo de Caja") { vm.uiState.value.catalog?.products?.size == 2 }
        vm.setProductAvailable(2, false)
        StaffHarness.awaitUntil("el producto agotado") {
            vm.uiState.value.catalog?.products?.first { it.id == 2 }?.available == false && !vm.uiState.value.catalogBusy
        }
        assertEquals(listOf(2 to false), catalog.availabilityCalls.toList())
    }

    @Test
    fun z06_siElServidorRechazaElProductoSigueDisponibleYSeAvisa() {
        catalog.availabilityFails = java.io.IOException("sin red")
        val vm = kitchen()
        vm.start()
        // El menú de Cocina pide el catálogo al abrirse, no al arrancar el rol.
        vm.refreshCatalog()
        StaffHarness.awaitUntil("el catálogo de Cocina") { vm.uiState.value.catalog?.products?.size == 2 }
        vm.setProductAvailable(1, false)
        StaffHarness.awaitUntil("el aviso de error") { vm.uiState.value.errorMessage != null && !vm.uiState.value.catalogBusy }
        assertTrue(vm.uiState.value.catalog!!.products.first { it.id == 1 }.available)
    }

    @Test
    fun cocinaEmpiezaUnaComandaPagadaConSuVersion() {
        orders.orders = listOf(sampleOrder("o1", OrderState.PAID, version = 3))
        val vm = kitchen()
        vm.start()
        StaffHarness.awaitUntil("la comanda en la fila") { vm.uiState.value.orders.any { it.summary.id == "o1" } }
        vm.startKitchen("o1", 3)
        StaffHarness.awaitUntil("la transición") { orders.transitionCalls.isNotEmpty() }
        assertEquals(Triple("o1", OrderState.PREPARING, 3), orders.transitionCalls.first())
    }

    @Test
    fun cocinaNoEmpiezaUnaComandaQueYaNoEstaPorPreparar() {
        orders.orders = listOf(sampleOrder("o1", OrderState.READY, version = 4))
        val vm = kitchen()
        vm.start()
        StaffHarness.awaitUntil("la comanda en la fila") { vm.uiState.value.orders.any { it.summary.id == "o1" } }
        vm.startKitchen("o1", 4)
        Thread.sleep(200)
        assertFalse(orders.transitionCalls.any { it.second == OrderState.PREPARING })
    }
}
