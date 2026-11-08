package com.vaiinilla.app.ui.admin

import com.vaiinilla.app.domain.admin.AdminWalletSummary
import com.vaiinilla.app.domain.admin.CashbackRule
import com.vaiinilla.app.domain.admin.StoreConfig
import com.vaiinilla.app.domain.admin.StoreRules
import com.vaiinilla.app.domain.model.OrderWindow
import com.vaiinilla.app.ui.operational.FakeCatalog
import com.vaiinilla.app.ui.operational.StaffHarness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Flujo de mi tienda, cashback y cartera de clientes del dueño dentro del ViewModel (T10, T08 y J08).
 * Corre en la JVM; no cuenta como recorrido en teléfono.
 */
class RecorridosTiendaAdminViewModelTest {
    private val store = FakeStore()
    private lateinit var vm: AdminStoreViewModel

    private val ui get() = vm.uiState.value

    private val config =
        StoreConfig(
            typeWire = "restaurante",
            deliveryRequiresQr = true,
            allowsPayAtEnd = false,
            allowsBalance = true,
            releaseGraceMinutes = 5,
            orderWindows = emptyList(),
            videos = emptyList(),
            visibleInDirectory = true,
            storeUrl = null,
            imageUrl = null,
            logoUrl = null,
        )

    private val wallet =
        AdminWalletSummary(
            reloads = "1200.00",
            purchases = "800.00",
            cashback = "40.00",
            cancellations = "0.00",
            canceledOrders = 0,
            movements = emptyList(),
            walletsReviewed = 12,
            alerts = 1,
        )

    @Before
    fun setUp() {
        StaffHarness.installMain()
        store.config = Result.success(config)
        store.wallet = Result.success(wallet)
        vm = AdminStoreViewModel(store, FakeCatalog())
        vm.onVisible("negocio-1")
        StaffHarness.awaitUntil("la tienda cargada") { ui.config != null && ui.cashbackLoaded && ui.wallet != null }
    }

    @Test
    fun t10_guardarElFlujoOrdenaLasFranjasAvisaYActualizaLaConfiguracion() {
        var done = false
        val flow =
            config.flow().copy(
                releaseGraceMinutes = 10,
                orderWindows = listOf(OrderWindow("18:00", "20:00"), OrderWindow("12:00", "15:00")),
            )
        vm.saveFlow(flow) { done = true }
        StaffHarness.awaitUntil("flujo guardado") { done && ui.notice == "Flujo guardado" && !ui.saving }
        assertEquals(
            listOf("12:00", "18:00"),
            store.flows
                .single()
                .orderWindows
                .map { it.from },
        )
        assertEquals(10, ui.config!!.releaseGraceMinutes)
    }

    @Test
    fun t10_unFlujoInvalidoNoLlegaAlServidor() {
        val flow = config.flow().copy(releaseGraceMinutes = StoreRules.MAX_GRACE_MINUTES + 1)
        vm.saveFlow(flow)
        StaffHarness.awaitUntil("el aviso") { ui.notice != null && ui.noticeIsError }
        assertTrue(store.flows.isEmpty())
    }

    @Test
    fun t08_elCashbackSeNormalizaAntesDeGuardarseYAvisa() {
        var done = false
        val rule = CashbackRule("Martes", "5", null, null, listOf(2), null, null, true)
        vm.saveCashback(rule) { done = true }
        StaffHarness.awaitUntil("cashback guardado") { done && ui.notice == "Cashback guardado" }
        assertEquals("5.00", store.cashbackSaved.single().percent)
        assertEquals("5.00", ui.cashback?.percent)
    }

    @Test
    fun t08_unPorcentajeFueraDeRangoNoSeGuarda() {
        vm.saveCashback(CashbackRule("Demasiado", "150", null, null, null, null, null, true)) {}
        StaffHarness.awaitUntil("el aviso") { ui.notice != null && ui.noticeIsError }
        assertTrue(store.cashbackSaved.isEmpty())
        assertNull(ui.cashback)
    }

    @Test
    fun j08_laCarteraDeClientesSeCargaYCambiaConElPeriodo() {
        assertEquals("1200.00", ui.wallet!!.reloads)
        val before = store.walletPeriods.size
        vm.selectWalletPeriod(AdminPeriodPreset.WEEK)
        StaffHarness.awaitUntil("la cartera de la semana") {
            store.walletPeriods.size > before &&
                ui.walletPreset == AdminPeriodPreset.WEEK
        }
    }
}
