package com.vaiinilla.app.ui.admin

import com.vaiinilla.app.domain.admin.AdminCategoryInput
import com.vaiinilla.app.domain.admin.AdminLiveCounts
import com.vaiinilla.app.domain.admin.AdminOrdersPage
import com.vaiinilla.app.domain.admin.AdminPeriod
import com.vaiinilla.app.domain.admin.AdminProductInput
import com.vaiinilla.app.domain.admin.AdminSummary
import com.vaiinilla.app.domain.admin.AdminUnpaidAccounts
import com.vaiinilla.app.domain.admin.AdminWalletSummary
import com.vaiinilla.app.domain.admin.CashbackRule
import com.vaiinilla.app.domain.admin.StoreConfig
import com.vaiinilla.app.domain.admin.StoreFlow
import com.vaiinilla.app.domain.admin.StoreVideo
import com.vaiinilla.app.domain.admin.StripeSetup
import com.vaiinilla.app.domain.model.Category
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.repository.AdminRepository
import com.vaiinilla.app.domain.repository.AdminStoreRepository
import com.vaiinilla.app.domain.repository.OrderRepositoryException
import com.vaiinilla.app.ui.operational.FakeCashSession
import com.vaiinilla.app.ui.operational.FakeCatalog
import com.vaiinilla.app.ui.operational.FakeOrders
import com.vaiinilla.app.ui.operational.StaffHarness
import com.vaiinilla.app.ui.operational.sampleProduct
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

internal class FakeAdmin(
    private val catalog: FakeCatalog,
) : AdminRepository {
    val archived = CopyOnWriteArrayList<Pair<Int, Boolean>>()

    private fun <T> no(): Result<T> = Result.failure(IllegalStateException("no"))

    override fun summary(period: AdminPeriod): Result<AdminSummary> = no()

    override fun unpaidAccounts(): Result<AdminUnpaidAccounts> = no()

    override fun orders(
        states: Set<com.vaiinilla.app.domain.model.OrderState>,
        cursor: String?,
    ): Result<AdminOrdersPage> = no()

    override fun createCategory(
        input: AdminCategoryInput,
        idempotencyKey: String,
    ): Result<Category> = no()

    override fun updateCategory(
        id: Int,
        input: AdminCategoryInput,
        idempotencyKey: String,
    ): Result<Category> = no()

    override fun createProduct(
        input: AdminProductInput,
        idempotencyKey: String,
    ): Result<Product> = no()

    override fun updateProduct(
        id: Int,
        input: AdminProductInput,
        idempotencyKey: String,
    ): Result<Product> = no()

    override fun deleteProductImage(
        id: Int,
        idempotencyKey: String,
    ): Result<Product> = no()

    override fun setProductArchived(
        id: Int,
        archived: Boolean,
        idempotencyKey: String,
    ): Result<Unit> {
        this.archived += id to archived
        // Como el repositorio de verdad: recuerda lo archivado porque `GET catalogo` no lo trae.
        remembered.removeAll { it.id == id }
        if (archived) {
            remembered += sampleProduct(id, "Pizza").copy(archived = true)
            // El servidor no devuelve lo archivado en el catálogo.
            catalog.products = catalog.products.filterNot { it.id == id }
        } else {
            catalog.products = catalog.products + sampleProduct(id, "Pizza")
        }
        return Result.success(Unit)
    }

    private val remembered = CopyOnWriteArrayList<Product>()

    override fun archivedProducts(): List<Product> = remembered.toList()

    override fun forgetArchived(id: Int) {
        remembered.removeAll { it.id == id }
    }
}

internal class FakeStore : AdminStoreRepository {
    @Volatile var config: Result<StoreConfig> = Result.failure(IllegalStateException("sin configuración"))

    @Volatile var cashback: CashbackRule? = null

    @Volatile var wallet: Result<AdminWalletSummary> = Result.failure(IllegalStateException("sin cartera"))
    val flows = CopyOnWriteArrayList<StoreFlow>()
    val cashbackSaved = CopyOnWriteArrayList<CashbackRule>()
    val walletPeriods = CopyOnWriteArrayList<AdminPeriod>()

    private fun <T> no(): Result<T> = Result.failure(IllegalStateException("no"))

    override fun config(): Result<StoreConfig> = config

    override fun saveFlow(flow: StoreFlow): Result<StoreConfig> {
        flows += flow
        return config.map { it.copy(releaseGraceMinutes = flow.releaseGraceMinutes, orderWindows = flow.orderWindows) }
    }

    override fun saveVideos(videos: List<StoreVideo>): Result<StoreConfig> = no()

    override fun setVisibleInDirectory(visible: Boolean): Result<StoreConfig> = no()

    override fun stripe(): Result<StripeSetup?> = Result.success(null)

    override fun stripeOnboardingLink(idempotencyKey: String): Result<String> = no()

    override fun setCardPayments(
        enabled: Boolean,
        idempotencyKey: String,
    ): Result<StripeSetup> = no()

    override fun setFeePaidByCustomer(
        paidByCustomer: Boolean,
        idempotencyKey: String,
    ): Result<StripeSetup> = no()

    override fun cashbackRule(): Result<CashbackRule?> = Result.success(cashback)

    override fun saveCashbackRule(
        rule: CashbackRule,
        idempotencyKey: String,
    ): Result<CashbackRule> {
        cashbackSaved += rule
        cashback = rule
        return Result.success(rule)
    }

    override fun walletSummary(period: AdminPeriod): Result<AdminWalletSummary> {
        walletPeriods += period
        return wallet
    }

    override fun liveCounts(): Result<AdminLiveCounts> = no()
}

/**
 * La Caja del admin dentro del ViewModel (T04) y archivar o restaurar un producto del menú (T03).
 * Corre en la JVM; no cuenta como recorrido en teléfono.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecorridosAdminViewModelTest {
    private val catalog = FakeCatalog()
    private val admin = FakeAdmin(catalog)
    private val cash = FakeCashSession()
    private lateinit var vm: AdminViewModel

    private val ui get() = vm.uiState.value

    @Before
    fun setUp() {
        StaffHarness.installMain()
        catalog.products = listOf(sampleProduct(1, "Pizza"))
        vm = AdminViewModel(admin, catalog, FakeStore(), FakeSpaces(), cash, FakeOrders())
    }

    private fun refreshCash() {
        vm.refreshCash()
        StaffHarness.awaitUntil("la caja leída") { ui.cashLoaded }
    }

    @Test
    fun t04_abrirLaCajaMandaElFondoYAvisa() {
        refreshCash()
        var done = false
        vm.openCash("500") { done = true }
        StaffHarness.awaitUntil("caja abierta") { done && ui.notice == "Caja abierta" }
        assertEquals(1, cash.openCalls.size)
        assertTrue(cash.sessionOpen)
    }

    @Test
    fun t04_unFondoQueNoSeEntiendeNoLlegaAlServidor() {
        refreshCash()
        vm.openCash("abc") {}
        StaffHarness.awaitUntil("el aviso") { ui.notice == "Escribe el fondo inicial, por ejemplo 500." && ui.noticeIsError }
        assertTrue(cash.openCalls.isEmpty())
    }

    @Test
    fun t04_cerrarLaCajaMandaLoContadoYGuardaElUltimoCierre() {
        cash.sessionOpen = true
        refreshCash()
        StaffHarness.awaitUntil("la sesión abierta") { ui.cashSessionId == "sesion-1" }
        var done = false
        vm.closeCash("1250") { done = true }
        StaffHarness.awaitUntil("caja cerrada") { done && ui.notice == "Caja cerrada" }
        assertEquals("sesion-1", cash.closeCalls.single().first)
        assertEquals(true, ui.cashLastClose?.closedByMe)
        assertEquals(false, cash.sessionOpen)
    }

    @Test
    fun t04_conCuentasDeMesasPorCobrarNoSeCierraYSeExplica() {
        cash.sessionOpen = true
        refreshCash()
        StaffHarness.awaitUntil("la sesión abierta") { ui.cashSessionId == "sesion-1" }
        cash.closeFails = OrderRepositoryException("CASH_SESSION_HAS_OPEN_ACCOUNTS", "x")
        vm.closeCash("1250") {}
        StaffHarness.awaitUntil("el aviso") {
            ui.notice == "Hay cuentas de mesas por cobrar: cóbralas o ciérralas antes de cerrar la caja." && ui.noticeIsError
        }
        assertEquals(true, cash.sessionOpen)
    }

    @Test
    fun t03_archivarUnProductoLoSacaDelMenuYRestaurarloLoRegresa() {
        val pizza = sampleProduct(1, "Pizza")
        vm.setArchived(pizza, true)
        StaffHarness.awaitUntil("archivado") { ui.notice == "Pizza archivado" && ui.archivedProducts.any { it.id == 1 && it.archived } }
        StaffHarness.awaitUntil("sin tarea en curso") { !ui.saving }
        vm.setArchived(pizza, false)
        StaffHarness.awaitUntil("restaurado") { ui.notice == "Pizza volvió al menú" && ui.archivedProducts.none { it.id == 1 } }
        assertEquals(listOf(1 to true, 1 to false), admin.archived.toList())
    }
}
