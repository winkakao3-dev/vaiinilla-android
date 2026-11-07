package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.domain.repository.OrderRepositoryException
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
    private lateinit var mainThread: java.util.concurrent.ExecutorService

    @Before
    fun setUp() {
        mainThread = java.util.concurrent.Executors.newSingleThreadExecutor()
        // Un solo hilo para el estado, como el hilo principal de verdad: con otro, dos escrituras se pisan.
        Dispatchers.setMain(mainThread.asCoroutineDispatcher())
        vm = StaffHarness.cashierViewModel(RuntimeEnvironment.getApplication(), cash = cash, wallet = wallet)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        mainThread.shutdownNow()
    }

    private val ui get() = vm.uiState.value

    @Test
    fun n05_abrirCajaMandaElFondoConDosDecimales() {
        vm.start()
        StaffHarness.awaitUntil("la caja se lee cerrada") { ui.cashSessionOpen == false }
        vm.openCashRegister("500")
        StaffHarness.awaitUntil("la caja abierta") { ui.cashSessionOpen == true }
        assertEquals(listOf("500.00"), cash.openCalls.map { it.first })
    }

    @Test
    fun n05_unFondoQueNoSeEntiendeNoLlegaAlServidor() {
        vm.start()
        StaffHarness.awaitUntil("la caja se lee cerrada") { ui.cashSessionOpen == false }
        vm.openCashRegister("abc")
        StaffHarness.awaitUntil("el error") { ui.errorMessage != null }
        assertTrue(cash.openCalls.isEmpty())
        assertEquals(false, ui.cashSessionOpen)
    }

    @Test
    fun n05_siSePierdeLaRespuestaPeroLaCajaAbrioNoHayError() {
        vm.start()
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
        StaffHarness.awaitUntil("la caja abierta") { ui.cashSessionOpen == true }
        cash.closeFails = OrderRepositoryException("CASH_SESSION_HAS_OPEN_ACCOUNTS", "Hay cuentas por cobrar.")
        vm.requestCloseCash()
        vm.closeCashRegister("1300.00")
        StaffHarness.awaitUntil("cierre bloqueado") { ui.closeCash?.blockedByAccounts == true }
        assertEquals(true, ui.cashSessionOpen)
    }

    @Test
    fun f04_recargaEnEfectivoVaALaPersonaConElMontoNormalizado() {
        wallet.clients = listOf(com.vaiinilla.app.domain.model.WalletClient("u2", "María López", null))
        cash.sessionOpen = true
        vm.start()
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
        StaffHarness.awaitUntil("la caja se lee cerrada") { ui.cashSessionOpen == false }
        vm.reloadWallet("u2", "100")
        Thread.sleep(150)
        assertTrue(wallet.reloadCalls.isEmpty())
    }

    @Test
    fun f04_unaBusquedaDeUnaLetraPideDosYNoBusca() {
        vm.start()
        vm.searchWalletClients("m")
        StaffHarness.awaitUntil("el aviso") { ui.errorMessage == "Escribe al menos 2 caracteres para buscar un cliente." }
        assertTrue(ui.walletClients.isEmpty())
    }

    @Test
    fun f15_conEfectivoSinSincronizarNoSeCierraLaCaja() {
        cash.sessionOpen = true
        vm.start()
        StaffHarness.awaitUntil("la caja abierta") { ui.cashSessionOpen == true }
        vm.setUnsyncedOfflineCash(2)
        vm.requestCloseCash()
        vm.closeCashRegister("1300.00")
        StaffHarness.awaitUntil("el aviso de cierre") { ui.closeCash?.errorMessage == unsyncedOfflineCashNotice(2) }
        assertTrue(cash.closeCalls.isEmpty())
        assertEquals(true, ui.cashSessionOpen)
    }
}
