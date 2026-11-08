package com.vaiinilla.app.ui.admin

import com.vaiinilla.app.domain.admin.AdminOrdersPage
import com.vaiinilla.app.domain.admin.AdminPeriod
import com.vaiinilla.app.domain.admin.AdminSummary
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.ui.operational.FakeCashSession
import com.vaiinilla.app.ui.operational.FakeCatalog
import com.vaiinilla.app.ui.operational.FakeOrders
import com.vaiinilla.app.ui.operational.StaffHarness
import com.vaiinilla.app.ui.operational.sampleOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Inicio y pedidos del admin (J01 y J03): el resumen del periodo con su fallo, y el historial con filtro y más
 * páginas. Corre en la JVM; no cuenta como recorrido en teléfono.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecorridosInicioAdminViewModelTest {
    private val catalog = FakeCatalog()
    private val admin = FakeAdmin(catalog)
    private lateinit var vm: AdminViewModel

    private val ui get() = vm.uiState.value

    private fun summary(sales: String) =
        AdminSummary(
            period = AdminPeriod.lastDays(1),
            sales = sales,
            orders = 12,
            averageTicket = "95.00",
            productsSold = 30,
            tips = null,
            walletReloads = "0.00",
            canceledOrders = 1,
            dailySales = emptyList(),
            paymentMethods = emptyList(),
            ordersByStatus = emptyList(),
            topProducts = emptyList(),
        )

    @Before
    fun setUp() {
        StaffHarness.installMain()
        vm = AdminViewModel(admin, catalog, FakeStore(), FakeSpaces(), FakeCashSession(), FakeOrders())
    }

    /** Deja que lo que arranca al entrar (resumen, caja, pedidos) termine antes de actuar, para no pisar sus escrituras. */
    private fun visit() {
        vm.onVisible("negocio-1")
        StaffHarness.awaitUntil("el arranque") { !ui.summaryLoading && !ui.ordersLoading && ui.cashLoaded }
        Thread.sleep(150)
    }

    @Test
    fun j01_alEntrarCargaElResumenDeHoyConSusCifras() {
        admin.summaryResult = Result.success(summary("1140.00"))
        visit()
        StaffHarness.awaitUntil("el resumen") { ui.summary != null }
        assertEquals("1140.00", ui.summary?.sales)
        assertEquals(12, ui.summary?.orders)
        assertEquals(AdminPeriodPreset.TODAY, ui.preset)
        assertNull(ui.summaryError)
    }

    @Test
    fun j01_cambiarAAsieteDiasPideEsePeriodo() {
        admin.summaryResult = Result.success(summary("1140.00"))
        visit()
        StaffHarness.awaitUntil("el resumen de hoy") { ui.summary != null }
        vm.selectPeriod(AdminPeriodPreset.WEEK)
        StaffHarness.awaitUntil("el periodo de 7 días") {
            admin.summaryPeriods.any { it == AdminPeriod.lastDays(7) } && !ui.summaryLoading
        }
        assertEquals(AdminPeriodPreset.WEEK, ui.preset)
    }

    @Test
    fun j01_siElResumenFallaSeDiceYNoSeInventanCifras() {
        visit()
        StaffHarness.awaitUntil("el error") { ui.summaryError != null && !ui.summaryLoading }
        assertNull(ui.summary)
    }

    @Test
    fun j03_elHistorialPideLosEstadosDelFiltroYAgregaLaSiguientePagina() {
        admin.orderPages["primera"] = AdminOrdersPage(listOf(sampleOrder("a", OrderState.DELIVERED, folio = 1)), "c2")
        admin.orderPages["c2"] = AdminOrdersPage(listOf(sampleOrder("b", OrderState.DELIVERED, folio = 2)), null)
        visit()
        vm.selectOrderFilter(AdminOrderFilter.DELIVERED)
        StaffHarness.awaitUntil("la primera página") {
            ui.orders.map { it.summary.id } == listOf("a") &&
                !ui.ordersLoading
        }
        assertTrue(admin.orderQueries.any { it.first == setOf(OrderState.DELIVERED) && it.second == null })
        vm.loadMoreOrders()
        StaffHarness.awaitUntil("la segunda página") {
            ui.orders.map { it.summary.id } == listOf("a", "b") &&
                !ui.ordersLoading
        }
        assertNull(ui.ordersCursor)
    }

    @Test
    fun j03_cambiarDeFiltroVaciaLaListaAntesDeCargarLaNueva() {
        admin.orderPages["primera"] = AdminOrdersPage(listOf(sampleOrder("a", OrderState.DELIVERED)), null)
        visit()
        vm.selectOrderFilter(AdminOrderFilter.DELIVERED)
        StaffHarness.awaitUntil("lo entregado") { ui.orders.isNotEmpty() && !ui.ordersLoading }
        admin.orderPages["primera"] = AdminOrdersPage(listOf(sampleOrder("x", OrderState.CANCELED)), null)
        vm.selectOrderFilter(AdminOrderFilter.INCIDENTS)
        StaffHarness.awaitUntil(
            "las incidencias",
        ) { ui.orders.map { it.summary.id } == listOf("x") && !ui.ordersLoading }
        assertTrue(admin.orderQueries.any { it.first == AdminOrderFilter.INCIDENTS.states })
    }

    @Test
    fun j03_siLosPedidosFallanSeMuestraElErrorYSePuedeReintentar() {
        admin.ordersFail = IllegalStateException("sin red")
        visit()
        vm.selectOrderFilter(AdminOrderFilter.ALL)
        StaffHarness.awaitUntil("el error") { ui.ordersError != null && !ui.ordersLoading }
        assertNotNull(ui.ordersError)
        admin.ordersFail = null
        admin.orderPages["primera"] = AdminOrdersPage(listOf(sampleOrder("a", OrderState.PAID)), null)
        vm.refreshOrders()
        StaffHarness.awaitUntil("la recuperación") { ui.orders.isNotEmpty() && ui.ordersError == null }
    }
}
