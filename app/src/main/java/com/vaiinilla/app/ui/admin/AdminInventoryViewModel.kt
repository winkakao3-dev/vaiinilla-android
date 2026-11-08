package com.vaiinilla.app.ui.admin

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.data.inventory.AllergenSuggestion
import com.vaiinilla.app.data.inventory.InventoryMovement
import com.vaiinilla.app.data.inventory.InventoryMovementType
import com.vaiinilla.app.data.inventory.InventoryProduct
import com.vaiinilla.app.data.inventory.InventoryReport
import com.vaiinilla.app.data.inventory.InventoryRepository
import com.vaiinilla.app.data.inventory.InventorySupplies
import com.vaiinilla.app.data.inventory.InventorySupply
import com.vaiinilla.app.data.inventory.InventorySupplyInput
import com.vaiinilla.app.data.inventory.RecipeLine
import com.vaiinilla.app.data.inventory.SupplyProductLink
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

/** Las pestañas de Negocio › Inventario. */
enum class InventoryTab(
    val label: String,
) {
    OVERVIEW("Cómo vas"),
    STOCK("Existencias"),
    RECIPES("Recetas"),
    COUNT("Conteo"),
    REPORT("Reporte"),
}

/** Periodos del reporte, como los cortes de Soft Restaurant. */
enum class InventoryPeriod(
    val label: String,
) {
    TODAY("Hoy"),
    YESTERDAY("Ayer"),
    WEEK("7 días"),
    MONTH("30 días"),
}

data class AdminInventoryUiState(
    val tab: InventoryTab = InventoryTab.OVERVIEW,
    val supplies: InventorySupplies? = null,
    val products: List<InventoryProduct>? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val saving: Boolean = false,
    val sheetError: String? = null,
    /** Receta abierta: el producto y sus líneas como están en el servidor. */
    val recipeProduct: InventoryProduct? = null,
    val recipe: List<RecipeLine>? = null,
    /** Conteo de fin de turno: lo que se lleva capturado por insumo (texto tal cual). */
    val counted: Map<Long, String> = emptyMap(),
    val period: InventoryPeriod = InventoryPeriod.TODAY,
    val report: InventoryReport? = null,
    val history: List<InventoryMovement>? = null,
    val historyFor: InventorySupply? = null,
    /** «Lo usan» del insumo [usesFor]: los productos cuya receta lo gasta, como están en el servidor. */
    val usesFor: InventorySupply? = null,
    val uses: List<SupplyProductLink>? = null,
    val notice: String? = null,
    val noticeIsError: Boolean = false,
)

/** Negocio › Inventario: insumos, recetas, conteos y el reporte por periodo (backend migración 140). */
@HiltViewModel
class AdminInventoryViewModel
    @Inject
    constructor(
        private val repository: InventoryRepository,
    ) : ViewModel() {
        private val _uiState = mutableStateOf(AdminInventoryUiState())
        val uiState: State<AdminInventoryUiState> = _uiState
        private var scope: String? = null
        private var noticeJob: Job? = null

        /** La misma llave mientras el usuario reintenta el mismo movimiento: el servidor no lo cuenta dos veces. */
        private var pendingMovementKey: Pair<String, String>? = null

        private fun update(block: AdminInventoryUiState.() -> AdminInventoryUiState) {
            _uiState.value = _uiState.value.block()
        }

        fun onVisible(scope: String) {
            if (scope != this.scope) {
                this.scope = scope
                _uiState.value = AdminInventoryUiState()
            }
            refresh()
        }

        /** Alérgenos que sugiere el asistente para un producto; solo sugiere, no guarda nada. */
        fun suggestAllergens(
            name: String,
            description: String,
            ingredients: String,
            productId: Int?,
            onResult: (Result<AllergenSuggestion>) -> Unit,
        ) {
            viewModelScope.launch {
                onResult(
                    withContext(
                        Dispatchers.IO,
                    ) { repository.suggestAllergens(name, description, ingredients, productId) },
                )
            }
        }

        /** El admin deja o no a Cocina editar el inventario. */
        fun setKitchenEdits(allowed: Boolean) {
            val supplies = _uiState.value.supplies ?: return
            update { copy(saving = true, supplies = supplies.copy(kitchenEdits = allowed)) }
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { repository.setKitchenEdits(allowed) }
                result.fold(
                    onSuccess = { saved ->
                        update { copy(saving = false, supplies = this.supplies?.copy(kitchenEdits = saved)) }
                        notice(if (saved) KITCHEN_EDITS_ON else KITCHEN_EDITS_OFF)
                    },
                    onFailure = {
                        update { copy(saving = false, supplies = this.supplies?.copy(kitchenEdits = !allowed)) }
                        notice(it.toUserFacingMessage("No se pudo cambiar el permiso de Cocina."), error = true)
                    },
                )
            }
        }

        fun selectTab(tab: InventoryTab) {
            update { copy(tab = tab) }
            if (tab == InventoryTab.REPORT && _uiState.value.report == null) loadReport()
        }

        fun refresh(onLoaded: (() -> Unit)? = null) {
            update { copy(loading = true, error = null) }
            viewModelScope.launch {
                val supplies = withContext(Dispatchers.IO) { repository.supplies(includeInactive = true) }
                val products = withContext(Dispatchers.IO) { repository.products() }
                update {
                    copy(
                        loading = false,
                        supplies = supplies.getOrNull() ?: this.supplies,
                        products = products.getOrNull() ?: this.products,
                        error =
                            (supplies.exceptionOrNull() ?: products.exceptionOrNull())
                                ?.toUserFacingMessage("No se pudo cargar el inventario."),
                    )
                }
                if (_uiState.value.tab == InventoryTab.REPORT) loadReport()
                onLoaded?.invoke()
            }
        }

        private fun notice(
            text: String,
            error: Boolean = false,
        ) {
            noticeJob?.cancel()
            update { copy(notice = text, noticeIsError = error) }
            noticeJob =
                viewModelScope.launch {
                    delay(2_600)
                    update { copy(notice = null) }
                }
        }

        fun clearSheetError() = update { copy(sheetError = null) }

        fun saveSupply(
            existing: InventorySupply?,
            input: InventorySupplyInput,
            onDone: () -> Unit,
        ) {
            if (_uiState.value.saving) return
            update { copy(saving = true, sheetError = null) }
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        if (existing ==
                            null
                        ) {
                            repository.createSupply(input)
                        } else {
                            repository.updateSupply(existing.id, input)
                        }
                    }
                result
                    .onSuccess {
                        update { copy(saving = false) }
                        onDone()
                        if (existing == null) {
                            // Recién creado no lo usa nada: se abre «Lo usan» vacío para ligarlo de una vez.
                            notice("${it.name} agregado · escoge qué productos lo usan")
                            update { copy(usesFor = it, uses = emptyList(), sheetError = null) }
                        } else {
                            notice("${it.name} guardado")
                        }
                        refresh()
                    }.onFailure {
                        update {
                            copy(saving = false, sheetError = it.toUserFacingMessage("No se pudo guardar el insumo."))
                        }
                    }
            }
        }

        /** Entrada, merma o ajuste de un insumo desde su hoja. */
        fun registerMovement(
            supply: InventorySupply,
            type: InventoryMovementType,
            quantity: Double,
            unitCost: Double?,
            note: String?,
            onDone: () -> Unit,
        ) {
            if (_uiState.value.saving) return
            val signature = "${supply.id}|${type.wire}|$quantity|$unitCost|$note"
            val key =
                pendingMovementKey?.takeIf { it.first == signature }?.second
                    ?: UUID.randomUUID().toString().also { pendingMovementKey = signature to it }
            update { copy(saving = true, sheetError = null) }
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    repository.registerMovement(key, supply.id, type, quantity, unitCost, note)
                }.onSuccess {
                    pendingMovementKey = null
                    update { copy(saving = false) }
                    notice("${supply.name}: quedan ${formatQuantity(it.stock)} ${supply.unit.short}")
                    onDone()
                    refresh()
                }.onFailure {
                    update {
                        copy(saving = false, sheetError = it.toUserFacingMessage("No se pudo registrar el movimiento."))
                    }
                }
            }
        }

        fun setCounted(
            supplyId: Long,
            text: String,
        ) = update { copy(counted = counted + (supplyId to text)) }

        fun clearCount() = update { copy(counted = emptyMap()) }

        private var countKeys: Map<Long, String> = emptyMap()

        /** Manda todo lo capturado en el conteo de una vez; lo que quedó vacío no se toca. */
        fun submitCount(note: String?) {
            val state = _uiState.value
            if (state.saving) return
            val entries =
                state.counted.mapNotNull { (id, text) -> parseQuantity(text)?.let { id to it } }
            if (entries.isEmpty()) {
                notice("Captura al menos un insumo", error = true)
                return
            }
            countKeys = entries.associate { (id, _) -> id to (countKeys[id] ?: UUID.randomUUID().toString()) }
            update { copy(saving = true) }
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    repository.registerCount(entries.map { (id, qty) -> Triple(countKeys.getValue(id), id, qty) }, note)
                }.onSuccess { differences ->
                    countKeys = emptyMap()
                    update { copy(saving = false, counted = emptyMap()) }
                    notice(
                        when (differences) {
                            0 -> "Conteo guardado, todo cuadra"
                            1 -> "Conteo guardado, 1 insumo con diferencia"
                            else -> "Conteo guardado, $differences insumos con diferencia"
                        },
                    )
                    refresh()
                }.onFailure {
                    update { copy(saving = false) }
                    notice(it.toUserFacingMessage("No se pudo guardar el conteo."), error = true)
                }
            }
        }

        fun openRecipe(product: InventoryProduct) {
            update { copy(recipeProduct = product, recipe = null, sheetError = null) }
            viewModelScope.launch {
                withContext(Dispatchers.IO) { repository.recipe(product.id) }
                    .onSuccess { lines ->
                        if (_uiState.value.recipeProduct?.id ==
                            product.id
                        ) {
                            update { copy(recipe = lines) }
                        }
                    }.onFailure {
                        // recipe queda en null: sin la receta real no se puede guardar encima.
                        if (_uiState.value.recipeProduct?.id == product.id) {
                            update {
                                copy(
                                    recipe = null,
                                    sheetError = it.toUserFacingMessage("No se pudo cargar la receta."),
                                )
                            }
                        }
                    }
            }
        }

        /**
         * Menú › hoja del producto: carga el inventario la primera vez que se abre una receta desde ahí, sin borrar
         * lo que ya está cargado si es la misma tienda.
         */
        fun ensureLoaded(scope: String) {
            val current = _uiState.value
            if (scope != this.scope || (current.products == null && !current.loading)) onVisible(scope)
        }

        /** El botón «Receta» de la hoja del producto: abre la misma receta que Inventario › Recetas. */
        fun openRecipeFor(
            productId: Int,
            name: String,
        ) {
            val known = _uiState.value.products?.firstOrNull { it.id == productId }
            openRecipe(
                known ?: InventoryProduct(
                    id = productId,
                    name = name,
                    category = "",
                    price = 0.0,
                    available = true,
                    soldOutByInventory = false,
                    hasRecipe = false,
                    enoughFor = null,
                    recipeCost = 0.0,
                    margin = null,
                ),
            )
        }

        fun closeRecipe() = update { copy(recipeProduct = null, recipe = null, sheetError = null) }

        fun saveRecipe(lines: List<RecipeLine>) {
            val product = _uiState.value.recipeProduct ?: return
            if (_uiState.value.saving) return
            update { copy(saving = true, sheetError = null) }
            viewModelScope.launch {
                withContext(Dispatchers.IO) { repository.saveRecipe(product.id, lines) }
                    .onSuccess {
                        update { copy(saving = false, recipeProduct = null, recipe = null) }
                        notice("Receta de ${product.name} guardada")
                        refresh {
                            val now = _uiState.value.products?.find { it.id == product.id }
                            if (now?.soldOutByInventory == true && !product.soldOutByInventory) {
                                notice("${product.name} quedó agotado: no alcanzan los insumos", error = true)
                            }
                        }
                    }.onFailure {
                        update {
                            copy(saving = false, sheetError = it.toUserFacingMessage("No se pudo guardar la receta."))
                        }
                    }
            }
        }

        /** Abre «Lo usan» de un insumo; si no carga, `uses` queda en null y no se ofrece guardar. */
        fun openUses(supply: InventorySupply) {
            update { copy(usesFor = supply, uses = null, sheetError = null) }
            viewModelScope.launch {
                withContext(Dispatchers.IO) { repository.supplyProducts(supply.id) }
                    .onSuccess { links -> if (_uiState.value.usesFor?.id == supply.id) update { copy(uses = links) } }
                    .onFailure {
                        if (_uiState.value.usesFor?.id == supply.id) {
                            update {
                                copy(
                                    uses = null,
                                    sheetError = it.toUserFacingMessage("No se pudieron cargar los productos."),
                                )
                            }
                        }
                    }
            }
        }

        fun closeUses() = update { copy(usesFor = null, uses = null, sheetError = null) }

        /** Guarda en qué productos (y categorías completas del menú) se gasta el insumo abierto en «Lo usan». */
        fun saveUses(
            products: Map<Int, Double>,
            categories: Map<Int, Double>,
        ) {
            val supply = _uiState.value.usesFor ?: return
            if (_uiState.value.saving) return
            update { copy(saving = true, sheetError = null) }
            viewModelScope.launch {
                withContext(Dispatchers.IO) { repository.saveSupplyProducts(supply.id, products, categories) }
                    .onSuccess { links ->
                        update { copy(saving = false, usesFor = null, uses = null) }
                        notice(
                            when (links.size) {
                                0 -> "${supply.name} ya no se usa en ningún producto"
                                1 -> "${supply.name} se usa en 1 producto"
                                else -> "${supply.name} se usa en ${links.size} productos"
                            },
                        )
                        refresh()
                    }.onFailure {
                        update {
                            copy(saving = false, sheetError = it.toUserFacingMessage("No se pudo guardar."))
                        }
                    }
            }
        }

        fun openHistory(supply: InventorySupply) {
            update { copy(historyFor = supply, history = null) }
            viewModelScope.launch {
                val (from, to) = range(InventoryPeriod.MONTH)
                withContext(Dispatchers.IO) { repository.movements(from, to, supply.id) }
                    .onSuccess { list ->
                        if (_uiState.value.historyFor?.id ==
                            supply.id
                        ) {
                            update { copy(history = list) }
                        }
                    }.onFailure { update { copy(history = emptyList()) } }
            }
        }

        fun closeHistory() = update { copy(historyFor = null, history = null) }

        fun selectPeriod(period: InventoryPeriod) {
            update { copy(period = period, report = null) }
            loadReport()
        }

        fun loadReport() {
            val period = _uiState.value.period
            viewModelScope.launch {
                val (from, to) = range(period)
                withContext(Dispatchers.IO) { repository.report(from, to) }
                    .onSuccess { report -> if (_uiState.value.period == period) update { copy(report = report) } }
                    .onFailure { notice(it.toUserFacingMessage("No se pudo cargar el reporte."), error = true) }
            }
        }

        private fun range(period: InventoryPeriod): Pair<String, String> {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val (start, end) =
                when (period) {
                    InventoryPeriod.TODAY -> today to today.plusDays(1)
                    InventoryPeriod.YESTERDAY -> today.minusDays(1) to today
                    InventoryPeriod.WEEK -> today.minusDays(6) to today.plusDays(1)
                    InventoryPeriod.MONTH -> today.minusDays(29) to today.plusDays(1)
                }
            return start.atStartOfDay(zone).toInstant().toString() to end.atStartOfDay(zone).toInstant().toString()
        }
    }

private val THOUSANDS_COMMA = Regex("^[1-9]\\d{0,2}(,\\d{3})+\\.\\d+$")
private val AMBIGUOUS_COMMA = Regex("^[1-9]\\d{0,2}(,\\d{3})+$")

/**
 * Texto capturado a número; vacío o inválido es null. «1,500.5» lleva coma de miles. «12,5» y
 * «0,500» llevan coma decimal. «1,500» puede ser mil quinientos o uno y medio según el teclado,
 * así que se rechaza (null) para que la app pida escribirlo sin comas, en vez de adivinar.
 */
fun parseQuantity(text: String): Double? {
    val clean = text.trim().replace(" ", "")
    if (clean.isEmpty()) return null
    val normalized =
        when {
            THOUSANDS_COMMA.matches(clean) -> clean.replace(",", "")
            AMBIGUOUS_COMMA.matches(clean) -> return null
            ',' in clean && '.' in clean -> return null
            clean.count { it == ',' } > 1 -> return null
            else -> clean.replace(',', '.')
        }
    return normalized.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
}

/** Sin ceros de sobra: 12, 12.5, 0.25 (costos con hasta 4 decimales). */
fun formatQuantity(
    value: Double,
    decimals: Int = 3,
): String {
    val factor = Math.pow(10.0, decimals.toDouble())
    val rounded = Math.round(value * factor) / factor
    return if (rounded == Math.floor(rounded) && kotlin.math.abs(rounded) < 1e12) {
        rounded.toLong().toString()
    } else {
        rounded.toBigDecimal().stripTrailingZeros().toPlainString()
    }
}

private const val KITCHEN_EDITS_ON = "Cocina ya puede editar el inventario"
private const val KITCHEN_EDITS_OFF = "Cocina solo ve, registra mermas y cuenta"
