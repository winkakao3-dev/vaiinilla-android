package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.ui.operational.FakeCatalog
import com.vaiinilla.app.ui.operational.FakeOrders
import com.vaiinilla.app.ui.operational.StaffHarness
import com.vaiinilla.app.ui.operational.sampleProduct
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Confirmar el pedido del cliente (B06 y L02): pagar con saldo manda ese método, y un fallo de red seguido de
 * otro intento reutiliza la misma llave para no cobrar dos veces. Corre en la JVM con Robolectric; no cuenta
 * como recorrido en teléfono ni prueba el cobro real de Stripe.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecorridosPagoViewModelTest {
    private val catalog = FakeCatalog()
    private val orders = FakeOrders()
    private lateinit var vm: OrderFlowViewModel

    private val ui get() = vm.uiState.value

    private fun start() {
        StaffHarness.installMain()
        catalog.products = listOf(sampleProduct(1, "Hamburguesa"))
        catalog.status =
            OperationalStatus(
                acceptingOrders = true,
                cashSessionOpen = true,
                cashierOnline = true,
                kitchenOnline = true,
                estimatedTimeMinutes = 10,
                consultedAt = "2026-11-07T10:00:00Z",
                acceptsCard = false,
                allowsBalance = true,
            )
        vm = OrderFlowHarness.viewModel(RuntimeEnvironment.getApplication(), catalog, orders)
        StaffHarness.awaitUntil("el menú cargado") { !ui.loading && ui.catalog != null && ui.operationalStatus != null }
        vm.quickAddProduct(1)
    }

    @Test
    fun b06_pagarConSaldoMandaEseMetodoYElCarritoQuedaVacio() {
        start()
        vm.updateCheckoutPayment(PaymentMethod.BALANCE)
        vm.submitOrder()
        StaffHarness.awaitUntil("el pedido creado") { ui.createdOrder != null }
        val (request, _) = orders.createdRequests.single()
        assertEquals(PaymentMethod.BALANCE, request.paymentMethod)
        assertTrue(ui.cartLines.isEmpty())
        assertEquals(false, ui.creatingOrder)
    }

    @Test
    fun l02_unFalloDeRedYOtroIntentoReusanLaMismaLlaveYSoloSeCreaUnPedido() {
        start()
        orders.createFails = IllegalStateException("sin red")
        vm.submitOrder()
        StaffHarness.awaitUntil("el error") { !ui.creatingOrder && ui.createOrderError != null }
        assertNotNull(ui.cartLines.singleOrNull())
        orders.createFails = null
        vm.submitOrder()
        StaffHarness.awaitUntil("el pedido creado") { ui.createdOrder != null }
        assertEquals(2, orders.createdRequests.size)
        assertEquals(orders.createdRequests[0].second, orders.createdRequests[1].second)
        assertTrue(ui.cartLines.isEmpty())
    }

    @Test
    fun l02_dosToquesSeguidosEnConfirmarCreanUnSoloPedido() {
        start()
        vm.submitOrder()
        vm.submitOrder()
        StaffHarness.awaitUntil("el pedido creado") { ui.createdOrder != null }
        Thread.sleep(300)
        assertEquals(1, orders.createdRequests.size)
    }
}
