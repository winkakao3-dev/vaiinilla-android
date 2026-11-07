package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.domain.repository.OrderRepositoryException
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * Caja de punta a punta dentro del ViewModel (N05, Q02, Z05, G09): apertura y cierre contra un servidor en memoria.
 * Corre en la JVM con Robolectric; no cuenta como recorrido en teléfono.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecorridosCajaViewModelTest {
    private val cash = FakeCashSession()
    private val wallet = FakeWallet()
    private lateinit var vm: CashierViewModel

    @Before
    fun setUp() {
        StaffHarness.installMain()
        vm = StaffHarness.cashierViewModel(RuntimeEnvironment.getApplication(), cash = cash, wallet = wallet)
    }

    private val ui get() = vm.uiState.value

    /** El arranque deja cosas en vuelo (caja, catálogo) que escriben el mismo estado: se espera a que acaben. */
    private fun settle() {
        StaffHarness.awaitUntil("el arranque de Caja") { ui.cashSessionOpen != null && ui.catalog != null }
        Thread.sleep(100)
    }

    @Test
    fun n05_abrirCajaMandaElFondoConDosDecimales() {
        vm.start()
        settle()
        StaffHarness.awaitUntil("la caja se lee cerrada") { ui.cashSessionOpen == false }
        vm.openCashRegister("500")
        StaffHarness.awaitUntil("la caja abierta") { ui.cashSessionOpen == true }
        assertEquals(listOf("500.00"), cash.openCalls.map { it.first })
    }

    @Test
    fun n05_unFondoQueNoSeEntiendeNoLlegaAlServidor() {
        vm.start()
        settle()
        StaffHarness.awaitUntil("la caja se lee cerrada") { ui.cashSessionOpen == false }
        vm.openCashRegister("abc")
        StaffHarness.awaitUntil("el error") { ui.errorMessage != null }
        assertTrue(cash.openCalls.isEmpty())
        assertEquals(false, ui.cashSessionOpen)
    }

    @Test
    fun n05_siSePierdeLaRespuestaPeroLaCajaAbrioNoHayError() {
        vm.start()
        settle()
        StaffHarness.awaitUntil("la caja se lee cerrada") { ui.cashSessionOpen == false }
        cash.openFails = java.io.IOException("sin red")
        cash.openLandsDespiteError = true
        vm.openCashRegister("500")
        StaffHarness.awaitUntil("la caja abierta y sin tarea en curso") { ui.cashSessionOpen == true && !ui.acting }
        assertEquals(1, cash.openCalls.size)
        assertEquals(null, ui.errorMessage)
    }

    @Test
    fun n05_elReintentoUsaLaMismaLlave() {
        vm.start()
        settle()
        StaffHarness.awaitUntil("la caja se lee cerrada") { ui.cashSessionOpen == false }
        cash.openFails = java.io.IOException("sin red")
        vm.openCashRegister("500")
        StaffHarness.awaitUntil("primer intento") { cash.openCalls.size == 1 && !ui.acting }
        vm.openCashRegister("500")
        StaffHarness.awaitUntil("segundo intento") { cash.openCalls.size == 2 && !ui.acting }
        assertEquals(cash.openCalls[0].second, cash.openCalls[1].second)
    }

    @Test
    fun q02_cerrarLaCajaMandaElEfectivoContadoDeLaSesionAbierta() {
        cash.sessionOpen = true
        vm.start()
        settle()
        StaffHarness.awaitUntil("la caja abierta") { ui.cashSessionOpen == true }
        vm.requestCloseCash()
        vm.closeCashRegister("1300.00")
        StaffHarness.awaitUntil("la caja cerrada") { ui.cashSessionOpen == false }
        assertEquals(listOf("sesion-1" to "1300.00"), cash.closeCalls.map { it.first to it.second })
        assertEquals(null, ui.closeCash)
    }

    @Test
    fun g09_conCuentasPorCobrarElCierreSeBloqueaYLaCajaSigueAbierta() {
        cash.sessionOpen = true
        vm.start()
        settle()
        StaffHarness.awaitUntil("la caja abierta") { ui.cashSessionOpen == true }
        cash.closeFails = OrderRepositoryException("CASH_SESSION_HAS_OPEN_ACCOUNTS", "Hay cuentas por cobrar.")
        vm.requestCloseCash()
        vm.closeCashRegister("1300.00")
        StaffHarness.awaitUntil("cierre bloqueado") { ui.closeCash?.blockedByAccounts == true }
        assertEquals(true, ui.cashSessionOpen)
    }

    @Test
    fun f04_recargaEnEfectivoVaALaPersonaConElMontoNormalizado() {
        wallet.clients =
            listOf(
                com.vaiinilla.app.domain.model
                    .WalletClient("u2", "María López", null),
            )
        cash.sessionOpen = true
        vm.start()
        settle()
        StaffHarness.awaitUntil("la caja abierta") { ui.cashSessionOpen == true }
        vm.searchWalletClients("mar")
        StaffHarness.awaitUntil("el cliente buscado") { ui.walletClients.map { it.userId } == listOf("u2") }
        // La recarga exige los dos decimales: «100» se frena antes de salir.
        vm.reloadWallet("u2", "100")
        assertTrue(wallet.reloadCalls.isEmpty())
        assertEquals("El monto debe ser positivo y tener dos decimales (ej. 100.00).", ui.errorMessage)
        vm.reloadWallet("u2", "100.00")
        StaffHarness.awaitUntil("el recibo de la recarga") { ui.walletReloadReceipt != null }
        assertEquals(listOf("u2" to "100.00"), wallet.reloadCalls.map { it.first to it.second })
    }

    @Test
    fun f04_sinCajaAbiertaLaRecargaNoSale() {
        vm.start()
        settle()
        StaffHarness.awaitUntil("la caja se lee cerrada") { ui.cashSessionOpen == false }
        vm.reloadWallet("u2", "100")
        Thread.sleep(150)
        assertTrue(wallet.reloadCalls.isEmpty())
    }

    @Test
    fun f04_unaBusquedaDeUnaLetraPideDosYNoBusca() {
        vm.start()
        settle()
        vm.searchWalletClients("m")
        StaffHarness.awaitUntil(
            "el aviso",
        ) { ui.errorMessage == "Escribe al menos 2 caracteres para buscar un cliente." }
        assertTrue(ui.walletClients.isEmpty())
    }

    @Test
    fun f15_conEfectivoSinSincronizarNoSeCierraLaCaja() {
        cash.sessionOpen = true
        vm.start()
        settle()
        StaffHarness.awaitUntil("la caja abierta") { ui.cashSessionOpen == true }
        vm.setUnsyncedOfflineCash(2)
        StaffHarness.awaitUntil("el efectivo sin sincronizar") { ui.unsyncedOfflineCash == 2 }
        vm.requestCloseCash()
        StaffHarness.awaitUntil("el cierre pedido") { ui.closeCash != null }
        Thread.sleep(100)
        vm.closeCashRegister("1300.00")
        StaffHarness.awaitUntil("el aviso de cierre") { ui.closeCash?.errorMessage == unsyncedOfflineCashNotice(2) }
        assertTrue(cash.closeCalls.isEmpty())
        assertEquals(true, ui.cashSessionOpen)
    }
}
