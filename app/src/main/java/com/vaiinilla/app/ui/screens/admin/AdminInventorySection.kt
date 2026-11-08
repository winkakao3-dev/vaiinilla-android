package com.vaiinilla.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.data.inventory.InventoryMovement
import com.vaiinilla.app.data.inventory.InventoryMovementType
import com.vaiinilla.app.data.inventory.InventoryProduct
import com.vaiinilla.app.data.inventory.InventoryReport
import com.vaiinilla.app.data.inventory.InventorySupply
import com.vaiinilla.app.data.inventory.InventorySupplyInput
import com.vaiinilla.app.data.inventory.InventoryUnit
import com.vaiinilla.app.data.inventory.RecipeLine
import com.vaiinilla.app.data.inventory.SupplyProductLink
import com.vaiinilla.app.domain.model.Category
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.ui.admin.AdminInventoryUiState
import com.vaiinilla.app.ui.admin.AdminInventoryViewModel
import com.vaiinilla.app.ui.admin.InventoryPeriod
import com.vaiinilla.app.ui.admin.InventoryTab
import com.vaiinilla.app.ui.admin.formatQuantity
import com.vaiinilla.app.ui.admin.parseQuantity
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.inventory.InventoryAssistantDialog
import com.vaiinilla.app.ui.inventory.InventoryAssistantViewModel
import com.vaiinilla.app.ui.inventory.inventoryOverview
import com.vaiinilla.app.ui.screens.ChipButton
import com.vaiinilla.app.ui.screens.OperationalColors

/** Lo que el inventario puede hacer con el insumo que se tocó. */
private enum class SupplySheet { ACTIONS, EDIT, ENTRY, WASTE, ADJUST }

/**
 * Negocio › Inventario (reemplazo de Soft Restaurant): existencias con su mínimo, recetas por producto, el conteo de
 * fin de turno y el reporte por periodo. Lo vendido se descuenta solo en el servidor al cobrar o mandar a preparar.
 */
@Composable
internal fun AdminInventorySection(
    viewModel: AdminInventoryViewModel,
    catalog: List<Product>?,
    colors: OperationalColors,
    readOnly: Boolean,
    onBack: () -> Unit,
    /** Categorías del menú (con su id) para ligar un insumo a una categoría completa en «Lo usan». */
    categories: List<Category>? = null,
    /** «Cómo vas» y el asistente de inventario; null los esconde. */
    assistant: InventoryAssistantViewModel? = null,
    /** Mermas y conteo: Cocina los hace aunque no pueda editar. */
    canCount: Boolean = !readOnly,
    /** Cocina no ve costos ni el reporte (el servidor tampoco se los manda). */
    showCosts: Boolean = true,
    /** El interruptor «Cocina puede editar el inventario»; solo el admin. */
    ownerControls: Boolean = true,
) {
    val state by viewModel.uiState
    var assistantOpen by rememberSaveable { mutableStateOf(false) }
    // Sin permiso (Cocina sin el interruptor del admin) se ve y se cuenta, pero no se edita.
    val editLocked = readOnly || state.supplies?.canEdit == false
    var sheet by remember { mutableStateOf<Pair<SupplySheet, InventorySupply?>?>(null) }
    var countNote by rememberSaveable { mutableStateOf("") }

    AdminSectionPage(
        title = "Inventario",
        colors = colors,
        notice = state.notice,
        noticeIsError = state.noticeIsError,
        onBack = onBack,
        subtitle = "Lo que se vende se descuenta solo. Se agota en el menú cuando ya no alcanza.",
    ) {
        item(key = "tabs") {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                InventoryTab.entries.filter { showCosts || it != InventoryTab.REPORT }.forEach { tab ->
                    ChipButton(label = tab.label, enabled = true, colors = colors, selected = state.tab == tab) {
                        viewModel.selectTab(tab)
                    }
                }
            }
        }
        if (state.error != null && state.supplies == null) {
            item(key = "error") { AdminLoadError(state.error!!, colors, viewModel::refresh) }
        }
        val openAdd = { sheet = SupplySheet.EDIT to null }
        val openSupply = { supply: InventorySupply -> sheet = SupplySheet.ACTIONS to supply }
        when (state.tab) {
            InventoryTab.OVERVIEW ->
                if (assistant != null) {
                    if (ownerControls && !readOnly) {
                        state.supplies?.let { data ->
                            item(key = "kitchen-permission") {
                                AdminToggleRow(
                                    title = "Cocina puede editar el inventario",
                                    detail = KITCHEN_PERMISSION_DETAIL,
                                    checked = data.kitchenEdits,
                                    colors = colors,
                                    enabled = !state.saving,
                                    onChange = viewModel::setKitchenEdits,
                                )
                            }
                        }
                    }
                    inventoryOverview(assistant.state.value, colors, assistant::refreshAlerts)
                    item(key = "overview-ask") {
                        AdminPrimaryButton(
                            title = "Pregúntale al asistente",
                            colors = colors,
                            modifier = Modifier.padding(top = 16.dp),
                        ) { assistantOpen = true }
                    }
                } else {
                    stockTab(state, colors, editLocked, showCosts, openAdd, openSupply)
                }
            InventoryTab.STOCK -> stockTab(state, colors, editLocked, showCosts, openAdd, openSupply)
            InventoryTab.RECIPES -> recipesTab(state, colors, viewModel::openRecipe)
            InventoryTab.COUNT ->
                countTab(
                    state = state,
                    colors = colors,
                    readOnly = !canCount,
                    note = countNote,
                    onNote = { countNote = it },
                    onCount = viewModel::setCounted,
                    onClear = viewModel::clearCount,
                ) {
                    viewModel.submitCount(countNote)
                    countNote = ""
                }
            InventoryTab.REPORT -> reportTab(state, colors, viewModel::selectPeriod)
        }
    }

    val current = sheet
    if (current != null) {
        val (kind, supply) = current
        val close = {
            sheet = null
            viewModel.clearSheetError()
        }
        when (kind) {
            SupplySheet.ACTIONS ->
                supply?.let {
                    SupplyActionsSheet(
                        it,
                        colors,
                        editLocked,
                        canCount = canCount,
                        showCosts = showCosts,
                        onDismiss = close,
                        onHistory = {
                            sheet = null
                            viewModel.openHistory(it)
                        },
                        onUses = {
                            sheet = null
                            viewModel.openUses(it)
                        },
                    ) { next -> sheet = next to it }
                }
            SupplySheet.EDIT ->
                SupplyEditSheet(supply, state, colors, showCosts = showCosts, onDismiss = close) { input ->
                    viewModel.saveSupply(supply, input) { sheet = null }
                }
            SupplySheet.ENTRY, SupplySheet.WASTE, SupplySheet.ADJUST ->
                supply?.let {
                    val type =
                        when (kind) {
                            SupplySheet.ENTRY -> InventoryMovementType.ENTRY
                            SupplySheet.WASTE -> InventoryMovementType.WASTE
                            else -> InventoryMovementType.COUNT
                        }
                    MovementSheet(it, type, state, colors, onDismiss = close) { qty, cost, note ->
                        viewModel.registerMovement(it, type, qty, cost, note) { sheet = null }
                    }
                }
        }
    }
    InventoryRecipeSheetHost(viewModel, catalog, colors, editLocked)
    if (assistantOpen && assistant != null) {
        InventoryAssistantDialog(assistant, colors) { assistantOpen = false }
    }
    state.usesFor?.let { supply ->
        SupplyUsesSheet(
            supply = supply,
            state = state,
            pickable = usesPicker(state.products, catalog, categories),
            colors = colors,
            readOnly = editLocked,
            onDismiss = viewModel::closeUses,
            onRetry = { viewModel.openUses(supply) },
            onSave = viewModel::saveUses,
        )
    }
    state.historyFor?.let { supply ->
        HistorySheet(supply, state.history, colors, viewModel::closeHistory)
    }
}

/** La hoja de la receta abierta, la misma desde Inventario › Recetas que desde el botón «Receta» del menú. */
@Composable
internal fun InventoryRecipeSheetHost(
    viewModel: AdminInventoryViewModel,
    catalog: List<Product>?,
    colors: OperationalColors,
    readOnly: Boolean,
) {
    val state by viewModel.uiState
    state.recipeProduct?.let { product ->
        RecipeSheet(
            product = product,
            catalogProduct = catalog?.firstOrNull { it.id == product.id },
            state = state,
            colors = colors,
            readOnly = readOnly,
            onDismiss = viewModel::closeRecipe,
            onRetry = { viewModel.openRecipe(product) },
            onSave = viewModel::saveRecipe,
        )
    }
}

/** Cómo va la receta de un producto en una línea: si descuenta, para cuántos alcanza, su costo y su margen. */
internal fun recipeSummary(product: InventoryProduct): String =
    if (!product.hasRecipe) {
        "Sin receta · no descuenta"
    } else {
        buildString {
            product.enoughFor?.let { append("Alcanza para $it · ") }
            append("costo ${moneyLabel(product.recipeCost)}")
            product.margin?.let { append(" · margen ${formatQuantity(it)}%") }
        }
    }

private fun qty(
    value: Double,
    unit: InventoryUnit,
) = "${formatQuantity(value)} ${unit.short}"

private fun androidx.compose.foundation.lazy.LazyListScope.stockTab(
    state: AdminInventoryUiState,
    colors: OperationalColors,
    readOnly: Boolean,
    showCosts: Boolean,
    onAdd: () -> Unit,
    onOpen: (InventorySupply) -> Unit,
) {
    val data = state.supplies
    if (data == null) {
        item(key = "stock-loading") { AdminHint("Cargando insumos…", colors) }
        return
    }
    item(key = "stock-summary") {
        AdminPanel(colors) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryFigure("Insumos", data.supplies.count { it.active }.toString(), colors, Modifier.weight(1f))
                SummaryFigure(
                    "Bajo mínimo",
                    data.lowCount.toString(),
                    colors,
                    Modifier.weight(1f),
                    alert = data.lowCount > 0,
                )
                if (showCosts) SummaryFigure("Valor", moneyLabel(data.totalValue), colors, Modifier.weight(1f))
            }
        }
    }
    if (!readOnly) {
        item(key = "stock-add") {
            AdminPrimaryButton(title = "Agregar insumo", colors = colors, icon = Icons.Outlined.Add, onClick = onAdd)
        }
    } else if (!data.canEdit) {
        item(key = "stock-locked") {
            AdminPanel(colors) {
                Text("Solo ver, mermas y conteo", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                AdminHint(
                    "Para dar de alta insumos, recetas o entradas, pídele a tu administrador que te dé permiso.",
                    colors,
                )
            }
        }
    }
    if (data.supplies.isEmpty()) {
        item(key = "stock-empty") {
            AdminCard(colors) {
                Text("Aún no hay insumos", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                AdminHint(
                    "Da de alta lo que compras (cervezas, botellas, clamato, papas) y luego arma la receta de cada " +
                        "producto en la pestaña Recetas.",
                    colors,
                )
            }
        }
        return
    }
    val sorted =
        data.supplies.sortedWith(
            compareBy({
                !it.active
            }, { !it.low }, { it.category ?: "~" }, { it.name.lowercase() }),
        )
    val groups =
        sorted.groupBy {
            if (!it.active) {
                "Inactivos"
            } else {
                it.category?.takeIf { c -> c.isNotBlank() }
                    ?: "Sin categoría"
            }
        }
    groups.forEach { (group, supplies) ->
        item(key = "stock-group-$group") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AdminCapsLabel(group, colors)
                AdminPanel(colors, padding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)) {
                    supplies.forEachIndexed { index, supply ->
                        if (index > 0) AdminRowDivider(colors)
                        AdminRow(
                            title = supply.name,
                            colors = colors,
                            dimmed = !supply.active,
                            detail =
                                buildString {
                                    append("Mínimo ${qty(supply.minimum, supply.unit)}")
                                    if (supply.unitCost > 0) append(" · ${moneyLabel(supply.unitCost)} c/u")
                                    if (supply.usedBy == 1) append(" · en 1 producto")
                                    if (supply.usedBy > 1) append(" · en ${supply.usedBy} productos")
                                },
                            onClick = { onOpen(supply) },
                        ) {
                            StockBadge(qty(supply.stock, supply.unit), supply.low && supply.active, colors)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryFigure(
    label: String,
    value: String,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    alert: Boolean = false,
) {
    Column(modifier) {
        Text(
            value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = if (alert) AdminCoral else colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
    }
}

@Composable
private fun StockBadge(
    text: String,
    alert: Boolean,
    colors: OperationalColors,
) {
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (alert) AdminCoral.copy(alpha = 0.16f) else colors.cardInner)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (alert) colors.coralText else colors.textPrimary,
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.recipesTab(
    state: AdminInventoryUiState,
    colors: OperationalColors,
    onOpen: (InventoryProduct) -> Unit,
) {
    val products = state.products
    if (products == null) {
        item(key = "recipes-loading") { AdminHint("Cargando productos…", colors) }
        return
    }
    val withRecipe = products.count { it.hasRecipe }
    item(key = "recipes-hint") {
        AdminHint(
            "$withRecipe de ${products.size} ${if (products.size == 1) "producto" else "productos"} con receta. " +
                "Toca uno para decir qué insumos gasta cada vez que se vende.",
            colors,
        )
    }
    products.groupBy { it.category }.forEach { (category, list) ->
        item(key = "recipes-$category") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AdminCapsLabel(category, colors)
                AdminPanel(colors, padding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)) {
                    list.forEachIndexed { index, product ->
                        if (index > 0) AdminRowDivider(colors)
                        AdminRow(
                            title = product.name,
                            colors = colors,
                            detail = recipeSummary(product),
                            onClick = { onOpen(product) },
                        ) {
                            when {
                                product.soldOutByInventory -> StockBadge("Agotado", true, colors)
                                !product.available -> StockBadge("Pausado", false, colors)
                                product.enoughFor != null && product.enoughFor <= 5 ->
                                    StockBadge("Quedan ${product.enoughFor}", true, colors)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.countTab(
    state: AdminInventoryUiState,
    colors: OperationalColors,
    readOnly: Boolean,
    note: String,
    onNote: (String) -> Unit,
    onCount: (Long, String) -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit,
) {
    val supplies = state.supplies?.supplies?.filter { it.active }
    if (supplies == null) {
        item(key = "count-loading") { AdminHint("Cargando insumos…", colors) }
        return
    }
    item(key = "count-hint") {
        AdminHint(
            "Cuenta lo que hay en barra y bodega y escríbelo. Lo que dejes vacío no cambia. La diferencia contra el " +
                "sistema queda en el reporte.",
            colors,
        )
    }
    item(key = "count-list") {
        AdminPanel(colors, padding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
            supplies.sortedBy { it.name.lowercase() }.forEachIndexed { index, supply ->
                if (index > 0) AdminRowDivider(colors)
                val typed = state.counted[supply.id].orEmpty()
                val diff = parseQuantity(typed)?.minus(supply.stock)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(supply.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)
                        Text(
                            buildString {
                                append("Sistema: ${qty(supply.stock, supply.unit)}")
                                if (diff != null && diff != 0.0) {
                                    append(" · ${if (diff > 0) "+" else ""}${formatQuantity(diff)}")
                                }
                            },
                            fontSize = 12.sp,
                            color = if (diff != null && diff < 0) colors.coralText else colors.textSecondary,
                        )
                    }
                    AdminTextField(
                        label = supply.unit.short,
                        value = typed,
                        onValueChange = { onCount(supply.id, it) },
                        colors = colors,
                        modifier = Modifier.width(110.dp),
                        keyboardType = KeyboardType.Decimal,
                        enabled = !readOnly && !state.saving,
                    )
                }
            }
        }
    }
    if (!readOnly) {
        item(key = "count-note") {
            AdminTextField(
                label = "Nota (opcional)",
                value = note,
                onValueChange = onNote,
                colors = colors,
                placeholder = "Cierre turno noche",
            )
        }
        item(key = "count-actions") {
            val filled = state.counted.values.count { parseQuantity(it) != null }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminPrimaryButton(
                    title = if (filled == 0) "Guardar conteo" else "Guardar conteo ($filled)",
                    colors = colors,
                    busy = state.saving,
                    enabled = filled > 0,
                    onClick = onSubmit,
                )
                if (state.counted.isNotEmpty()) {
                    AdminSecondaryButton(title = "Borrar lo capturado", colors = colors, onClick = onClear)
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.reportTab(
    state: AdminInventoryUiState,
    colors: OperationalColors,
    onPeriod: (InventoryPeriod) -> Unit,
) {
    item(key = "report-period") {
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            InventoryPeriod.entries.forEach { p ->
                ChipButton(
                    label = p.label,
                    enabled = true,
                    colors = colors,
                    selected = p == state.period,
                ) { onPeriod(p) }
            }
        }
    }
    val report = state.report
    if (report == null) {
        item(key = "report-loading") { AdminHint("Cargando reporte…", colors) }
        return
    }
    item(key = "report-totals") { ReportTotals(report, colors) }
    val moved = report.rows.filter { it.entries != 0.0 || it.sold != 0.0 || it.waste != 0.0 || it.differences != 0.0 }
    if (moved.isEmpty()) {
        item(key = "report-empty") { AdminHint("Sin movimientos en este periodo.", colors) }
    }
    moved.forEach { row ->
        item(key = "report-${row.id}") {
            AdminPanel(colors) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        row.name,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    StockBadge(qty(row.end, row.unit), row.low, colors)
                }
                AdminValueRow("Inicial", qty(row.start, row.unit), colors)
                if (row.entries != 0.0) AdminValueRow("Entradas", "+${qty(row.entries, row.unit)}", colors)
                if (row.sold != 0.0) {
                    AdminValueRow("Vendido", "−${qty(row.sold, row.unit)}", colors, detail = moneyLabel(row.soldCost))
                }
                if (row.waste != 0.0) {
                    AdminValueRow("Mermas", "−${qty(row.waste, row.unit)}", colors, detail = moneyLabel(row.wasteCost))
                }
                if (row.differences != 0.0) {
                    val sign = if (row.differences > 0) "+" else "−"
                    AdminValueRow(
                        "Diferencia de conteo",
                        "$sign${qty(kotlin.math.abs(row.differences), row.unit)}",
                        colors,
                        detail = moneyLabel(kotlin.math.abs(row.differenceCost)),
                    )
                }
                AdminValueRow("Final", qty(row.end, row.unit), colors, strong = true)
            }
        }
    }
}

@Composable
private fun ReportTotals(
    report: InventoryReport,
    colors: OperationalColors,
) {
    AdminPanel(colors, highlighted = true) {
        AdminCapsLabel("Costo del periodo", colors, inset = false)
        AdminValueRow("Costo de lo vendido", moneyLabel(report.soldCost), colors, strong = true)
        AdminValueRow("Mermas", moneyLabel(report.wasteCost), colors)
        AdminValueRow(
            "Diferencias de conteo",
            (if (report.differenceCost < 0) "−" else "") + moneyLabel(kotlin.math.abs(report.differenceCost)),
            colors,
        )
        AdminValueRow("Compras registradas", moneyLabel(report.purchases), colors)
    }
}

@Composable
private fun SupplyActionsSheet(
    supply: InventorySupply,
    colors: OperationalColors,
    readOnly: Boolean,
    canCount: Boolean = !readOnly,
    showCosts: Boolean = true,
    onDismiss: () -> Unit,
    onHistory: () -> Unit,
    onUses: () -> Unit,
    onPick: (SupplySheet) -> Unit,
) {
    AdminSheet(title = supply.name, colors = colors, busy = false, onDismiss = onDismiss) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryFigure("Hay", qty(supply.stock, supply.unit), colors, Modifier.weight(1f), alert = supply.low)
            SummaryFigure("Mínimo", qty(supply.minimum, supply.unit), colors, Modifier.weight(1f))
            if (showCosts) SummaryFigure("Valor", moneyLabel(supply.value), colors, Modifier.weight(1f))
        }
        if (!readOnly) {
            AdminPrimaryButton(title = "Registrar entrada (compra)", colors = colors) { onPick(SupplySheet.ENTRY) }
        }
        if (canCount) {
            AdminSecondaryButton(
                title = "Registrar merma",
                colors = colors,
                outlined = true,
            ) { onPick(SupplySheet.WASTE) }
            AdminSecondaryButton(title = "Contar este insumo", colors = colors, outlined = true) {
                onPick(SupplySheet.ADJUST)
            }
        }
        if (!readOnly) {
            AdminSecondaryButton(title = "Editar insumo", colors = colors, outlined = true) { onPick(SupplySheet.EDIT) }
        }
        AdminSecondaryButton(
            title =
                when (supply.usedBy) {
                    0 -> if (readOnly) "Lo usan · ningún producto" else "Lo usan · ligar a productos"
                    1 -> "Lo usan · 1 producto"
                    else -> "Lo usan · ${supply.usedBy} productos"
                },
            colors = colors,
            outlined = true,
            onClick = onUses,
        )
        AdminSecondaryButton(title = "Ver movimientos", colors = colors, onClick = onHistory)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SupplyEditSheet(
    supply: InventorySupply?,
    state: AdminInventoryUiState,
    colors: OperationalColors,
    showCosts: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (InventorySupplyInput) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(supply?.name.orEmpty()) }
    var category by rememberSaveable { mutableStateOf(supply?.category.orEmpty()) }
    var unit by rememberSaveable { mutableStateOf(supply?.unit ?: InventoryUnit.PIECE) }
    var minimum by rememberSaveable { mutableStateOf(supply?.minimum?.let(::formatQuantity).orEmpty()) }
    val savedCost =
        supply
            ?.unitCost
            ?.takeIf { it > 0 }
            ?.let { formatQuantity(it, decimals = 4) }
            .orEmpty()
    var cost by rememberSaveable { mutableStateOf(savedCost) }
    // Con existencia o recetas, cambiar la unidad dejaría cantidades sin sentido.
    val unitLocked = supply != null && (supply.stock != 0.0 || supply.usedBy > 0)
    var initial by rememberSaveable { mutableStateOf("") }
    var active by rememberSaveable { mutableStateOf(supply?.active ?: true) }
    var supplierName by rememberSaveable { mutableStateOf(supply?.supplierName.orEmpty()) }
    var supplierPhone by rememberSaveable { mutableStateOf(supply?.supplierPhone.orEmpty()) }
    val phoneOk = supplierPhone.isBlank() || SUPPLIER_PHONE.matches(supplierPhone.trim())
    val categories =
        state.supplies
            ?.supplies
            ?.mapNotNull { it.category?.takeIf(String::isNotBlank) }
            ?.distinct()
            .orEmpty()
    AdminSheet(
        title = if (supply == null) "Nuevo insumo" else "Editar insumo",
        colors = colors,
        busy = state.saving,
        onDismiss = onDismiss,
    ) {
        AdminTextField("Nombre", name, { name = it }, colors, placeholder = "Cerveza Corona 355 ml")
        AdminTextField("Etiqueta", category, { category = it }, colors, placeholder = "Cervezas, Licores, Cocina…")
        AdminHint(
            if (supply == null) {
                "Solo ordena tus insumos en esta lista; no es la categoría del menú. Al guardar escoges qué " +
                    "productos o categorías del menú lo usan."
            } else {
                "Solo ordena tus insumos en esta lista; no es la categoría del menú. Para ligarlo a productos o a " +
                    "una categoría del menú usa «Lo usan»."
            },
            colors,
        )
        if (categories.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categories.forEach { c ->
                    ChipButton(label = c, enabled = true, colors = colors, selected = c == category) { category = c }
                }
            }
        }
        AdminChoice(
            question = "¿En qué se cuenta?",
            options = InventoryUnit.entries.map { it to it.label },
            selected = unit,
            colors = colors,
            enabled = !state.saving && !unitLocked,
        ) { unit = it }
        if (unitLocked) {
            AdminHint("La unidad ya no se cambia porque hay existencia o recetas; da de alta otro insumo.", colors)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AdminTextField(
                "Mínimo (${unit.short})",
                minimum,
                { minimum = it },
                colors,
                Modifier.weight(1f),
                keyboardType = KeyboardType.Decimal,
            )
            if (showCosts) {
                AdminTextField(
                    "Costo por ${unit.short}",
                    cost,
                    { cost = it },
                    colors,
                    Modifier.weight(1f),
                    keyboardType = KeyboardType.Decimal,
                    prefix = "$",
                )
            }
        }
        if (supply == null) {
            AdminTextField(
                "Hay ahorita (${unit.short})",
                initial,
                { initial = it },
                colors,
                keyboardType = KeyboardType.Decimal,
                placeholder = "0",
            )
        } else {
            AdminChoice(
                question = "¿Sigue en uso?",
                options = listOf(true to "Activo", false to "Inactivo"),
                selected = active,
                colors = colors,
                enabled = !state.saving,
            ) { active = it }
        }
        AdminHint("El mínimo marca el insumo en rojo para que se vuelva a comprar.", colors)
        AdminTextField(
            "Proveedor",
            supplierName,
            { supplierName = it.take(80) },
            colors,
            placeholder = "Abarrotes Rivera",
        )
        AdminTextField(
            "Teléfono del proveedor",
            supplierPhone,
            { supplierPhone = it.take(30) },
            colors,
            keyboardType = KeyboardType.Phone,
            placeholder = "614 123 4567",
        )
        AdminHint(
            if (phoneOk) {
                "Quién te lo surte. El asistente y los avisos de Caja y Cocina lo dicen cuando se está acabando."
            } else {
                "Revisa el teléfono: solo números, espacios, + o guiones."
            },
            colors,
        )
        val badField =
            listOf("mínimo" to minimum, "costo" to cost, "existencia" to initial)
                .firstOrNull { (_, t) -> t.isNotBlank() && parseQuantity(t) == null }
                ?.first
        AdminSheetError(
            state.sheetError ?: badField?.let { "Revisa el $it: escribe 1500 o 1.5 (sin comas de miles)." },
        )
        AdminPrimaryButton(
            title = if (supply == null) "Guardar y escoger productos" else "Guardar",
            colors = colors,
            busy = state.saving,
            enabled = name.isNotBlank() && badField == null && phoneOk,
        ) {
            onSave(
                InventorySupplyInput(
                    name = name.trim(),
                    // "" borra la etiqueta: con null el PATCH no la manda y el servidor dejaba la anterior (#29).
                    category = category.trim(),
                    unit = if (supply == null || unit != supply.unit) unit else null,
                    minimum = parseQuantity(minimum) ?: 0.0,
                    unitCost = if (supply == null || cost != savedCost) parseQuantity(cost) ?: 0.0 else null,
                    initialStock = if (supply == null) parseQuantity(initial) else null,
                    active = if (supply == null) null else active,
                    // "" borra el proveedor; sin cambios no se manda.
                    supplierName = supplierName.trim().takeIf { it != supply?.supplierName.orEmpty() },
                    supplierPhone = supplierPhone.trim().takeIf { it != supply?.supplierPhone.orEmpty() },
                ),
            )
        }
    }
}

@Composable
private fun MovementSheet(
    supply: InventorySupply,
    type: InventoryMovementType,
    state: AdminInventoryUiState,
    colors: OperationalColors,
    onDismiss: () -> Unit,
    onSave: (Double, Double?, String?) -> Unit,
) {
    var amount by rememberSaveable { mutableStateOf("") }
    var cost by rememberSaveable {
        mutableStateOf(
            supply.unitCost
                .takeIf { it > 0 }
                ?.let { formatQuantity(it, decimals = 4) }
                .orEmpty(),
        )
    }
    var note by rememberSaveable { mutableStateOf("") }
    val title =
        when (type) {
            InventoryMovementType.ENTRY -> "Entrada de ${supply.name}"
            InventoryMovementType.WASTE -> "Merma de ${supply.name}"
            else -> "Contar ${supply.name}"
        }
    val label =
        when (type) {
            InventoryMovementType.ENTRY -> "¿Cuánto llegó? (${supply.unit.short})"
            InventoryMovementType.WASTE -> "¿Cuánto se perdió? (${supply.unit.short})"
            else -> "¿Cuánto hay? (${supply.unit.short})"
        }
    val value = parseQuantity(amount)
    val costValue = parseQuantity(cost)
    val costInvalid = type == InventoryMovementType.ENTRY && cost.isNotBlank() && costValue == null
    val valid =
        value != null &&
            (value >= MIN_QUANTITY || (type == InventoryMovementType.COUNT && value == 0.0)) &&
            !costInvalid
    AdminSheet(title = title, colors = colors, busy = state.saving, onDismiss = onDismiss) {
        AdminHint("Ahora hay ${qty(supply.stock, supply.unit)} según el sistema.", colors)
        AdminTextField(label, amount, { amount = it }, colors, keyboardType = KeyboardType.Decimal)
        if (type == InventoryMovementType.ENTRY) {
            AdminTextField(
                "Costo por ${supply.unit.short}",
                cost,
                { cost = it },
                colors,
                keyboardType = KeyboardType.Decimal,
                prefix = "$",
            )
        }
        AdminTextField(
            "Nota (opcional)",
            note,
            { note = it },
            colors,
            placeholder =
                when (type) {
                    InventoryMovementType.ENTRY -> "Proveedor o factura"
                    InventoryMovementType.WASTE -> "Se rompió, caducó…"
                    else -> "Conteo de barra"
                },
        )
        if (type == InventoryMovementType.COUNT && value != null) {
            val diff = value - supply.stock
            if (diff != 0.0) {
                AdminHint(
                    "Diferencia: ${if (diff > 0) "+" else ""}${formatQuantity(
                        diff,
                    )} ${supply.unit.short}. Queda en el reporte.",
                    colors,
                )
            }
        }
        AdminSheetError(state.sheetError ?: quantityError(amount, value, costInvalid))
        AdminPrimaryButton(title = "Guardar", colors = colors, busy = state.saving, enabled = valid) {
            onSave(value ?: 0.0, if (type == InventoryMovementType.ENTRY) costValue else null, note)
        }
    }
}

/** Una línea de la receta mientras se edita: la cantidad como texto para no pelear con el teclado. */
private data class DraftLine(
    val supplyId: Long,
    val supplyName: String,
    val unit: InventoryUnit,
    val optionId: Int?,
    val optionName: String?,
    val quantity: String,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeSheet(
    product: InventoryProduct,
    catalogProduct: Product?,
    state: AdminInventoryUiState,
    colors: OperationalColors,
    readOnly: Boolean,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onSave: (List<RecipeLine>) -> Unit,
) {
    val loaded = state.recipe
    val draft = remember(product.id, loaded) { mutableStateListOf<DraftLine>() }
    var initialized by remember(product.id, loaded) { mutableStateOf(false) }
    if (loaded != null && !initialized) {
        draft.clear()
        draft.addAll(
            loaded.map {
                DraftLine(it.supplyId, it.supplyName, it.unit, it.optionId, it.optionName, formatQuantity(it.quantity))
            },
        )
        initialized = true
    }
    var adding by remember { mutableStateOf(false) }
    var addOption by remember { mutableStateOf<Pair<Int, String>?>(null) }
    val options =
        catalogProduct?.optionGroups.orEmpty().flatMap { g ->
            g.options.map { it.id to "${g.name}: ${it.name}" }
        }
    val supplies =
        state.supplies
            ?.supplies
            ?.filter { it.active }
            .orEmpty()

    AdminSheet(title = "Receta de ${product.name}", colors = colors, busy = state.saving, onDismiss = onDismiss) {
        if (loaded == null) {
            // Si no cargó, no se ofrece guardar: un PUT vacío borraría la receta real.
            if (state.sheetError != null) {
                AdminSheetError(state.sheetError)
                AdminSecondaryButton(title = "Reintentar", colors = colors, onClick = onRetry)
            } else {
                AdminHint("Cargando receta…", colors)
            }
            return@AdminSheet
        }
        AdminHint(
            "Lo que gasta UNA unidad. Las líneas «si eligen…» solo se descuentan cuando el cliente escoge esa opción.",
            colors,
        )
        if (draft.isEmpty()) {
            AdminCard(colors) { Text("Sin insumos todavía", color = colors.textSecondary, fontSize = 14.sp) }
        }
        draft.forEachIndexed { index, line ->
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        line.supplyName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        line.optionName?.let { "Si eligen $it" } ?: "Siempre",
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                    )
                }
                AdminTextField(
                    label = line.unit.short,
                    value = line.quantity,
                    onValueChange = { draft[index] = line.copy(quantity = it) },
                    colors = colors,
                    modifier = Modifier.width(96.dp),
                    keyboardType = KeyboardType.Decimal,
                    enabled = !readOnly,
                )
                if (!readOnly) {
                    Box(
                        Modifier
                            .physicalPress(scale = PhysicalPressScale.Small) { draft.removeAt(index) }
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.cardInner),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Close, "Quitar", tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        if (!readOnly) {
            if (!adding) {
                AdminSecondaryButton(
                    title = "Agregar insumo",
                    colors = colors,
                    outlined = true,
                    icon = Icons.Outlined.Add,
                ) {
                    adding = true
                    addOption = null
                }
            } else {
                AdminPanel(colors) {
                    if (options.isNotEmpty()) {
                        Text(
                            "¿Cuándo se gasta?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = colors.textPrimary,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ChipButton("Siempre", true, colors, selected = addOption == null) { addOption = null }
                            options.forEach { opt ->
                                ChipButton(opt.second, true, colors, selected = addOption == opt) { addOption = opt }
                            }
                        }
                    }
                    Text("Elige el insumo", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)
                    if (supplies.isEmpty()) {
                        AdminHint("Primero da de alta insumos en Existencias.", colors)
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        supplies
                            .filter { s -> draft.none { it.supplyId == s.id && it.optionId == addOption?.first } }
                            .sortedBy { it.name.lowercase() }
                            .forEach { s ->
                                ChipButton(s.name, true, colors) {
                                    draft.add(
                                        DraftLine(
                                            s.id,
                                            s.name,
                                            s.unit,
                                            addOption?.first,
                                            addOption?.second?.substringAfter(": "),
                                            if (s.unit == InventoryUnit.PIECE ||
                                                s.unit == InventoryUnit.BOTTLE
                                            ) {
                                                "1"
                                            } else {
                                                ""
                                            },
                                        ),
                                    )
                                    adding = false
                                }
                            }
                    }
                    AdminSecondaryButton(title = "Cancelar", colors = colors) { adding = false }
                }
            }
            val parsed = draft.map { it to parseQuantity(it.quantity) }
            val valid = parsed.all { (_, q) -> q != null && q >= MIN_QUANTITY }
            AdminSheetError(
                state.sheetError ?: if (!valid) "Cada insumo necesita una cantidad de al menos 0.001." else null,
            )
            AdminPrimaryButton(title = "Guardar receta", colors = colors, busy = state.saving, enabled = valid) {
                onSave(
                    parsed.map { (l, q) ->
                        RecipeLine(l.supplyId, l.supplyName, l.unit, q ?: 0.0, l.optionId, l.optionName, 0.0)
                    },
                )
            }
        }
    }
}

@Composable
private fun HistorySheet(
    supply: InventorySupply,
    history: List<InventoryMovement>?,
    colors: OperationalColors,
    onDismiss: () -> Unit,
) {
    AdminSheet(title = "Movimientos de ${supply.name}", colors = colors, busy = false, onDismiss = onDismiss) {
        when {
            history == null -> AdminHint("Cargando…", colors)
            history.isEmpty() -> AdminHint("Sin movimientos en los últimos 30 días.", colors)
            else ->
                history.forEach { m ->
                    val label =
                        when (m.type) {
                            "venta" -> m.folio?.let { "Venta · pedido #$it" } ?: "Venta"
                            "cancelacion" -> m.folio?.let { "Cancelación · pedido #$it" } ?: "Cancelación"
                            "entrada" -> "Entrada"
                            "merma" -> "Merma"
                            "conteo" -> "Conteo"
                            "ajuste" -> "Ajuste"
                            "inicial" -> "Existencia inicial"
                            else -> m.type.replace('_', ' ').replaceFirstChar { it.uppercase() }
                        }
                    val sign = if (m.quantity > 0) "+" else "−"
                    AdminValueRow(
                        label = label,
                        value = "$sign${formatQuantity(kotlin.math.abs(m.quantity))} ${m.unit.short}",
                        colors = colors,
                        detail =
                            listOfNotNull(
                                localTime(m.at),
                                m.by,
                                m.note,
                                "quedó ${formatQuantity(m.stockAfter)}",
                            ).joinToString(" · "),
                    )
                }
        }
    }
}

private fun localTime(iso: String): String =
    runCatching {
        java.time.OffsetDateTime
            .parse(iso)
            .atZoneSameInstant(java.time.ZoneId.systemDefault())
            .format(
                java.time.format.DateTimeFormatter
                    .ofPattern("d MMM HH:mm", java.util.Locale("es", "MX")),
            )
    }.getOrDefault(iso.take(16).replace('T', ' '))

/** Un renglón de «Lo usan»: un producto suelto o una categoría completa del menú (se expande al guardar). */
private data class UseDraft(
    val productId: Int?,
    val categoryId: Int?,
    val name: String,
    val caption: String,
    val quantity: String,
)

/** Un grupo del selector de «Lo usan»: la categoría del menú (sin [id] si no se conoce) y sus productos. */
internal data class UsesPickerCategory(
    val id: Int?,
    val name: String,
    val products: List<Pair<Int, String>>,
)

/**
 * Lo que se puede ligar a un insumo: los productos del inventario (sin rentas ni archivados) agrupados por la categoría
 * del menú. Con el catálogo se conoce el id de la categoría y se puede elegir completa; sin él solo van productos.
 */
internal fun usesPicker(
    products: List<InventoryProduct>?,
    catalog: List<Product>?,
    categories: List<Category>?,
): List<UsesPickerCategory> {
    val inventory = products.orEmpty()
    val categoryOf = catalog.orEmpty().associate { it.id to it.categoryId }
    val known = categories.orEmpty().associateBy { it.id }
    return inventory
        .groupBy { p -> categoryOf[p.id]?.takeIf { it in known } ?: p.category }
        .map { (key, list) ->
            val category = (key as? Int)?.let { known[it] }
            UsesPickerCategory(
                id = category?.id,
                name = category?.name ?: list.first().category.ifBlank { "Sin categoría" },
                products = list.sortedBy { it.name.lowercase() }.map { it.id to it.name },
            )
        }.sortedWith(
            compareBy<UsesPickerCategory>(
                { g -> g.id?.let { known[it]?.order } ?: Int.MAX_VALUE },
                { it.name.lowercase() },
            ),
        )
}

/**
 * «Lo usan»: los productos del menú cuya receta gasta este insumo, y cuánto por unidad. Se agregan productos sueltos o
 * una categoría completa del menú (se aplica a los productos que hoy tiene). Solo toca las líneas «Siempre» de cada
 * receta; las de «si eligen…» se editan en la receta del producto.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SupplyUsesSheet(
    supply: InventorySupply,
    state: AdminInventoryUiState,
    pickable: List<UsesPickerCategory>,
    colors: OperationalColors,
    readOnly: Boolean,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onSave: (products: Map<Int, Double>, categories: Map<Int, Double>) -> Unit,
) {
    val loaded: List<SupplyProductLink>? = state.uses
    val draft = remember(supply.id, loaded) { mutableStateListOf<UseDraft>() }
    var initialized by remember(supply.id, loaded) { mutableStateOf(false) }
    if (loaded != null && !initialized) {
        draft.clear()
        draft.addAll(
            loaded.map {
                UseDraft(it.productId, null, it.name, it.categoryName, formatQuantity(it.quantity))
            },
        )
        initialized = true
    }
    var adding by remember { mutableStateOf(false) }
    val defaultQuantity =
        if (supply.unit == InventoryUnit.PIECE || supply.unit == InventoryUnit.BOTTLE) "1" else ""
    var addQuantity by remember(supply.id) { mutableStateOf(defaultQuantity) }

    AdminSheet(title = "Lo usan: ${supply.name}", colors = colors, busy = state.saving, onDismiss = onDismiss) {
        if (loaded == null) {
            // Sin la lista real no se ofrece guardar: un PUT vacío lo quitaría de todas las recetas.
            if (state.sheetError != null) {
                AdminSheetError(state.sheetError)
                AdminSecondaryButton(title = "Reintentar", colors = colors, onClick = onRetry)
            } else {
                AdminHint("Cargando productos…", colors)
            }
            return@AdminSheet
        }
        AdminHint(
            "Productos del menú que gastan este insumo y cuánto gasta UNA unidad de cada uno " +
                "(${supply.unit.short}). Lo que se vende se descuenta solo.",
            colors,
        )
        if (draft.isEmpty()) {
            AdminCard(colors) { Text("Ningún producto lo usa todavía", color = colors.textSecondary, fontSize = 14.sp) }
        }
        draft.forEachIndexed { index, line ->
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        line.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(line.caption, fontSize = 12.sp, color = colors.textSecondary)
                }
                AdminTextField(
                    label = supply.unit.short,
                    value = line.quantity,
                    onValueChange = { draft[index] = line.copy(quantity = it) },
                    colors = colors,
                    modifier = Modifier.width(96.dp),
                    keyboardType = KeyboardType.Decimal,
                    enabled = !readOnly,
                )
                if (!readOnly) {
                    Box(
                        Modifier
                            .physicalPress(scale = PhysicalPressScale.Small) { draft.removeAt(index) }
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.cardInner),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Close, "Quitar", tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        if (readOnly) return@AdminSheet
        if (!adding) {
            AdminSecondaryButton(
                title = "Agregar productos o categoría",
                colors = colors,
                outlined = true,
                icon = Icons.Outlined.Add,
            ) { adding = true }
        } else {
            AdminPanel(colors) {
                AdminTextField(
                    "Cuánto gasta cada producto (${supply.unit.short})",
                    addQuantity,
                    { addQuantity = it },
                    colors,
                    keyboardType = KeyboardType.Decimal,
                )
                if (pickable.isEmpty()) AdminHint("Cargando el menú…", colors)
                pickable.forEach { group ->
                    val inDraft = { id: Int -> draft.any { it.productId == id } }
                    val groupTaken = group.id != null && draft.any { it.categoryId == group.id }
                    val free = group.products.filterNot { (id, _) -> inDraft(id) }
                    if (free.isEmpty() && (group.id == null || groupTaken)) return@forEach
                    AdminCapsLabel(group.name, colors)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (!groupTaken) {
                            ChipButton("Toda la categoría", true, colors, selected = true) {
                                if (group.id != null) {
                                    draft.add(
                                        UseDraft(
                                            productId = null,
                                            categoryId = group.id,
                                            name = "Toda la categoría ${group.name}",
                                            caption =
                                                if (group.products.size == 1) {
                                                    "Su único producto de hoy"
                                                } else {
                                                    "Sus ${group.products.size} productos de hoy"
                                                },
                                            quantity = addQuantity,
                                        ),
                                    )
                                } else {
                                    free.forEach { (id, name) ->
                                        draft.add(UseDraft(id, null, name, group.name, addQuantity))
                                    }
                                }
                                adding = false
                            }
                        }
                        free.forEach { (id, name) ->
                            ChipButton(name, true, colors) {
                                draft.add(UseDraft(id, null, name, group.name, addQuantity))
                                adding = false
                            }
                        }
                    }
                }
                AdminSecondaryButton(title = "Cancelar", colors = colors) { adding = false }
            }
        }
        if (draft.any { it.categoryId != null }) {
            AdminHint(
                "Una categoría se aplica a los productos que tiene hoy; un producto suelto con otra cantidad gana. " +
                    "Lo que agregues al menú después se liga aquí o en su receta.",
                colors,
            )
        }
        val parsed = draft.map { it to parseQuantity(it.quantity) }
        val valid = parsed.all { (_, q) -> q != null && q >= MIN_QUANTITY }
        AdminSheetError(
            state.sheetError ?: if (!valid) "Cada producto necesita una cantidad de al menos 0.001." else null,
        )
        AdminPrimaryButton(title = "Guardar", colors = colors, busy = state.saving, enabled = valid) {
            val products = parsed.mapNotNull { (l, q) -> l.productId?.let { it to (q ?: 0.0) } }.toMap()
            val byCategory = parsed.mapNotNull { (l, q) -> l.categoryId?.let { it to (q ?: 0.0) } }.toMap()
            onSave(products, byCategory)
        }
    }
}

/** Lo mínimo que guarda el servidor (3 decimales); menos se redondea a cero. */
private const val MIN_QUANTITY = 0.001

private fun quantityError(
    text: String,
    value: Double?,
    costInvalid: Boolean,
): String? =
    when {
        costInvalid -> "Revisa el costo: escribe 1500 o 1.5 (sin comas de miles)."
        text.isBlank() -> null
        value == null -> "Escribe la cantidad como 1500 o 1.5 (sin comas de miles)."
        value > 0 && value < MIN_QUANTITY -> "La cantidad mínima es 0.001."
        else -> null
    }

/** Lo mismo que acepta el servidor (migración 204). */
private val SUPPLIER_PHONE = Regex("^[0-9+() .-]{7,30}$")

private const val KITCHEN_PERMISSION_DETAIL =
    "Dar de alta insumos, cambiar recetas y registrar entradas. Apagado, Cocina solo ve, registra mermas y cuenta."
