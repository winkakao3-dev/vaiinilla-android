package com.vaiinilla.app.ui.admin

import com.vaiinilla.app.data.inventory.AllergenSuggestion
import com.vaiinilla.app.data.inventory.InventoryMovement
import com.vaiinilla.app.data.inventory.InventoryMovementType
import com.vaiinilla.app.data.inventory.InventoryProduct
import com.vaiinilla.app.data.inventory.InventoryReport
import com.vaiinilla.app.data.inventory.InventoryRepository
import com.vaiinilla.app.data.inventory.InventorySupplies
import com.vaiinilla.app.data.inventory.InventorySupply
import com.vaiinilla.app.data.inventory.InventorySupplyInput
import com.vaiinilla.app.data.inventory.InventoryUnit
import com.vaiinilla.app.data.inventory.MovementResult
import com.vaiinilla.app.data.inventory.RecipeLine
import com.vaiinilla.app.data.inventory.SupplyProductLink
import com.vaiinilla.app.ui.operational.StaffHarness
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

private fun supply(
    id: Long,
    name: String,
) = InventorySupply(id, name, null, InventoryUnit.KILO, 10.0, 2.0, 30.0, 300.0, false, true, 0)

private fun product(
    id: Int,
    name: String,
    soldOut: Boolean = false,
) = InventoryProduct(id, name, "Comida", 80.0, true, soldOut, false, null, 0.0, null)

private class FakeInventory : InventoryRepository {
    @Volatile var supplies: List<InventorySupply> = listOf(supply(1, "Harina"), supply(2, "Queso"))

    @Volatile var products: List<InventoryProduct> = listOf(product(10, "Pizza"))

    @Volatile var recipeFails: Throwable? = null

    @Volatile var countResult: Result<Int> = Result.success(0)

    @Volatile var soldOutAfterRecipe = false

    @Volatile var suggestion: Result<AllergenSuggestion> =
        Result.success(AllergenSuggestion(listOf("gluten"), "Lleva harina."))
    val created = CopyOnWriteArrayList<InventorySupplyInput>()
    val recipes = CopyOnWriteArrayList<Pair<Int, List<RecipeLine>>>()
    val links = CopyOnWriteArrayList<Pair<Long, Map<Int, Double>>>()
    val counts = CopyOnWriteArrayList<Pair<List<Triple<String, Long, Double>>, String?>>()
    val reports = CopyOnWriteArrayList<Pair<String?, String?>>()

    override fun supplies(includeInactive: Boolean) =
        Result.success(InventorySupplies(supplies, supplies.count { it.low }, supplies.sumOf { it.value }))

    override fun createSupply(input: InventorySupplyInput): Result<InventorySupply> {
        created += input
        val new = supply(3, input.name)
        supplies = supplies + new
        return Result.success(new)
    }

    override fun updateSupply(
        id: Long,
        input: InventorySupplyInput,
    ) = Result.success(supply(id, input.name))

    override fun registerMovement(
        id: String,
        supplyId: Long,
        type: InventoryMovementType,
        quantity: Double,
        unitCost: Double?,
        note: String?,
    ) = Result.success(MovementResult(quantity, quantity, false))

    override fun registerCount(
        counts: List<Triple<String, Long, Double>>,
        note: String?,
    ): Result<Int> {
        this.counts += counts to note
        return countResult
    }

    override fun products() = Result.success(products)

    override fun suggestAllergens(
        name: String,
        description: String?,
        ingredients: String?,
        productId: Int?,
    ) = suggestion

    override fun recipe(productId: Int): Result<List<RecipeLine>> =
        recipeFails?.let { Result.failure(it) } ?: Result.success(emptyList())

    override fun saveRecipe(
        productId: Int,
        lines: List<RecipeLine>,
    ): Result<List<RecipeLine>> {
        recipes += productId to lines
        if (soldOutAfterRecipe) {
            products =
                products.map { if (it.id == productId) it.copy(soldOutByInventory = true) else it }
        }
        return Result.success(lines)
    }

    override fun supplyProducts(supplyId: Long) = Result.success(emptyList<SupplyProductLink>())

    override fun saveSupplyProducts(
        supplyId: Long,
        products: Map<Int, Double>,
        categories: Map<Int, Double>,
    ): Result<List<SupplyProductLink>> {
        links += supplyId to products
        return Result.success(products.map { (id, qty) -> SupplyProductLink(id, "Pizza", 1, "Comida", qty) })
    }

    override fun movements(
        fromIso: String?,
        toIso: String?,
        supplyId: Long?,
    ) = Result.success(emptyList<InventoryMovement>())

    override fun report(
        fromIso: String?,
        toIso: String?,
    ): Result<InventoryReport> {
        reports += fromIso to toIso
        return Result.success(InventoryReport(fromIso.orEmpty(), toIso.orEmpty(), emptyList(), 120.0, 15.0, 5.0, 300.0))
    }
}

/**
 * Inventario del admin dentro del ViewModel (V01 y V03): alta de un insumo, ligarlo a productos, receta, conteo de
 * fin de turno y reporte. V02 (vender descuenta insumos) lo hace el servidor y no se prueba aquí.
 * Corre en la JVM; no cuenta como recorrido en teléfono.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecorridosInventarioViewModelTest {
    private val repo = FakeInventory()
    private lateinit var vm: AdminInventoryViewModel

    private val ui get() = vm.uiState.value

    @Before
    fun setUp() {
        StaffHarness.installMain()
        vm = AdminInventoryViewModel(repo)
        vm.onVisible("negocio-1")
        wait("el inventario cargado") { ui.supplies != null && ui.products != null && !ui.loading }
    }

    private fun wait(
        what: String,
        cond: () -> Boolean,
    ) {
        val end = System.currentTimeMillis() + 20_000
        while (!cond()) {
            if (System.currentTimeMillis() > end) throw AssertionError("Se esperaba: $what")
            Thread.sleep(10)
        }
    }

    @Test
    fun `la sugerencia de alergenos vive en el estado y un fallo conserva la anterior`() {
        vm.suggestAllergens("Pizza", "", "harina, queso", 10)
        wait("la sugerencia") { !ui.allergenSuggestion.busy }
        assertEquals(listOf("gluten"), ui.allergenSuggestion.suggestion?.codes)
        assertNull(ui.allergenSuggestion.error)

        repo.suggestion = Result.failure(IllegalStateException("sin red"))
        vm.suggestAllergens("Pizza", "", "harina, queso", 10)
        wait("el fallo") { !ui.allergenSuggestion.busy }
        assertTrue(ui.allergenSuggestion.error != null)
        assertEquals(listOf("gluten"), ui.allergenSuggestion.suggestion?.codes)
    }

    @Test
    fun `al abrir otro producto se olvida lo que el asistente sugirio para el anterior`() {
        vm.suggestAllergens("Pizza", "", "harina, queso", 10)
        wait("la sugerencia") { ui.allergenSuggestion.suggestion != null }

        vm.forgetAllergenSuggestion()

        assertEquals(AllergenSuggestionUi(), ui.allergenSuggestion)
    }

    @Test
    fun v01_unInsumoNuevoSeAgregaYSeAbreParaLigarloAProductos() {
        var done = false
        vm.saveSupply(null, InventorySupplyInput("Pan", null, InventoryUnit.PIECE, 5.0, 3.0, initialStock = 20.0)) {
            done =
                true
        }
        wait("el insumo guardado") { done && ui.notice == "Pan agregado · escoge qué productos lo usan" }
        assertEquals("Pan", repo.created.single().name)
        assertEquals(20.0, repo.created.single().initialStock!!, 0.0)
        assertEquals("Pan", ui.usesFor?.name)
        assertEquals(emptyList<SupplyProductLink>(), ui.uses)
    }

    @Test
    fun v01_ligarElInsumoAUnProductoGuardaLaCantidadPorUnidad() {
        vm.openUses(supply(1, "Harina"))
        wait("Lo usan abierto") { ui.uses != null }
        vm.saveUses(mapOf(10 to 0.25), emptyMap())
        wait("el aviso") { ui.notice == "Harina se usa en 1 producto" }
        assertEquals(listOf(1L to mapOf(10 to 0.25)), repo.links.toList())
        assertNull(ui.usesFor)
    }

    @Test
    fun v01_laRecetaSeGuardaYSiNoAlcanzanLosInsumosElProductoQuedaAgotado() {
        repo.soldOutAfterRecipe = true
        vm.openRecipe(product(10, "Pizza"))
        wait("la receta cargada") { ui.recipe != null }
        val line = RecipeLine(1, "Harina", InventoryUnit.KILO, 0.3, null, null, 9.0)
        vm.saveRecipe(listOf(line))
        wait("el aviso de agotado") { ui.notice == "Pizza quedó agotado: no alcanzan los insumos" && ui.noticeIsError }
        assertEquals(listOf(10 to listOf(line)), repo.recipes.toList())
        assertNull(ui.recipeProduct)
    }

    @Test
    fun v01_siLaRecetaNoCargaNoSeOfreceGuardarEncima() {
        repo.recipeFails = java.io.IOException("sin red")
        vm.openRecipe(product(10, "Pizza"))
        wait("el error de la hoja") { ui.sheetError != null }
        assertNull(ui.recipe)
    }

    @Test
    fun v03_elConteoMandaSoloLoCapturadoYAvisaLasDiferencias() {
        repo.countResult = Result.success(1)
        vm.setCounted(1, "8,5")
        vm.setCounted(2, "")
        vm.submitCount("fin de turno")
        wait("el aviso del conteo") { ui.notice == "Conteo guardado, 1 insumo con diferencia" }
        val (items, note) = repo.counts.single()
        assertEquals(listOf(1L to 8.5), items.map { it.second to it.third })
        assertEquals("fin de turno", note)
        assertTrue(ui.counted.isEmpty())
    }

    @Test
    fun v03_unaCantidadAmbiguaComoMilQuinientosSeRechazaYNoSeMandaNada() {
        vm.setCounted(1, "1,500")
        vm.submitCount(null)
        wait("el aviso") { ui.notice == "Captura al menos un insumo" && ui.noticeIsError }
        assertTrue(repo.counts.isEmpty())
    }

    @Test
    fun v03_siElConteoFallaElReintentoUsaLasMismasLlaves() {
        repo.countResult = Result.failure(java.io.IOException("sin red"))
        vm.setCounted(1, "8")
        vm.submitCount(null)
        wait("primer intento") { repo.counts.size == 1 && !ui.saving }
        vm.submitCount(null)
        wait("segundo intento") { repo.counts.size == 2 && !ui.saving }
        assertEquals(repo.counts[0].first.map { it.first }, repo.counts[1].first.map { it.first })
    }

    @Test
    fun v03_elReportePorPeriodoSeCargaYCambiaConElPeriodo() {
        vm.selectTab(InventoryTab.REPORT)
        wait("el reporte de hoy") { ui.report != null }
        vm.selectPeriod(InventoryPeriod.WEEK)
        wait(
            "el reporte de 7 días",
        ) { ui.period == InventoryPeriod.WEEK && ui.report != null && repo.reports.size >= 2 }
        assertEquals(15.0, ui.report!!.wasteCost, 0.0)
    }
}
