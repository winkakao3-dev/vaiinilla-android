package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.ui.operational.FakeCatalog
import com.vaiinilla.app.ui.operational.FakeOrders
import com.vaiinilla.app.ui.operational.StaffHarness
import com.vaiinilla.app.ui.operational.sampleProduct
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * El cliente arma su pedido (B01, B03, B06 y B08): ve el menú, busca y abre un producto, cambia cantidades y
 * quita, y qué formas de pago se ofrecen. Corre en la JVM con Robolectric; no cuenta como recorrido en teléfono.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecorridosCarritoViewModelTest {
    private val catalog = FakeCatalog()
    private val orders = FakeOrders()
    private lateinit var vm: OrderFlowViewModel

    private val ui get() = vm.uiState.value

    private fun status(
        acceptsCard: Boolean = false,
        allowsBalance: Boolean = true,
    ) = OperationalStatus(
        acceptingOrders = true,
        cashSessionOpen = true,
        cashierOnline = true,
        kitchenOnline = true,
        estimatedTimeMinutes = 10,
        consultedAt = "2026-11-07T10:00:00Z",
        acceptsCard = acceptsCard,
        allowsBalance = allowsBalance,
    )

    private fun start(status: OperationalStatus = status()) {
        StaffHarness.installMain()
        catalog.products = listOf(sampleProduct(1, "Hamburguesa"), sampleProduct(2, "Refresco"))
        catalog.status = status
        vm = OrderFlowHarness.viewModel(RuntimeEnvironment.getApplication(), catalog, orders)
        StaffHarness.awaitUntil("el menú cargado") { !ui.loading && ui.catalog != null && ui.operationalStatus != null }
    }

    @Test
    fun b01_elMenuCargaBuscarYFiltrarSeGuardanYAbrirUnProductoLoSelecciona() {
        start()
        assertEquals(listOf("Hamburguesa", "Refresco"), ui.catalog!!.products.map { it.name })
        vm.updateSearch("hamb")
        vm.selectCategory(1)
        assertEquals("hamb", ui.searchQuery)
        assertEquals(1, ui.selectedCategoryId)
        vm.clearMenuFilters()
        assertEquals("", ui.searchQuery)
        assertNull(ui.selectedCategoryId)
        vm.openProduct(1)
        assertEquals("Hamburguesa", ui.selectedProduct?.name)
        assertEquals(1, ui.selectedQuantity)
        vm.closeProduct()
        assertNull(ui.selectedProduct)
    }

    @Test
    fun b01_unProductoQueNoExisteNoSeAbre() {
        start()
        vm.openProduct(99)
        assertNull(ui.selectedProduct)
    }

    @Test
    fun b03_agregarDosVecesLoMismoSumaYElTotalSigueLasCantidades() {
        start()
        vm.openProduct(1)
        vm.changeSelectedQuantity(1)
        vm.addSelectedProductToCart()
        assertEquals(2, ui.cartItemCount)
        vm.quickAddProduct(1)
        assertEquals(3, ui.cartItemCount)
        assertEquals(1, ui.cartLines.size)
        assertEquals("135.00", ui.cartPreviewTotal)
    }

    @Test
    fun b03_cambiarCantidadesYQuitarLaLineaVaciaElCarrito() {
        start()
        vm.quickAddProduct(1)
        vm.quickAddProduct(2)
        val key = ui.cartLines.first { it.product.id == 1 }.key
        vm.changeCartLineQuantity(key, +2)
        assertEquals(3, ui.cartLines.first { it.product.id == 1 }.quantity)
        vm.changeCartLineQuantity(key, -3)
        assertEquals(listOf(2), ui.cartLines.map { it.product.id })
        assertEquals("45.00", ui.cartPreviewTotal)
    }

    @Test
    fun b03_laCantidadMaximaPorLineaEsVeinteYSeAvisa() {
        start()
        vm.quickAddProduct(1)
        val key = ui.cartLines.single().key
        vm.changeCartLineQuantity(key, +19)
        assertEquals(20, ui.cartLines.single().quantity)
        vm.changeCartLineQuantity(key, +1)
        assertEquals(20, ui.cartLines.single().quantity)
        assertEquals("La cantidad máxima por línea es 20.", ui.createOrderError)
    }

    @Test
    fun b03_conElCarritoVacioNoSePuedeEnviar() {
        start()
        assertFalse(ui.canSubmitCart)
        vm.quickAddProduct(1)
        assertTrue(ui.canSubmitCart)
    }

    @Test
    fun b06_elSaldoSeOfreceSalvoQueElDuenioLoApague() {
        start(status(allowsBalance = true))
        assertTrue(ui.canPayWithBalance)
        start(status(allowsBalance = false))
        assertFalse(ui.canPayWithBalance)
    }

    @Test
    fun b08_laTarjetaNoSeOfreceSiElDuenioNoLaActivo() {
        start(status(acceptsCard = false))
        assertFalse(ui.canPayWithCard)
        start(status(acceptsCard = true))
        assertTrue(ui.canPayWithCard)
    }
}
