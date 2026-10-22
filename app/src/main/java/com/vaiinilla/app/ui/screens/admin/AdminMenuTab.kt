package com.vaiinilla.app.ui.screens.admin

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.admin.AdminCategoryInput
import com.vaiinilla.app.domain.admin.AdminOptionGroupInput
import com.vaiinilla.app.domain.admin.AdminOptionInput
import com.vaiinilla.app.domain.admin.AdminProductInput
import com.vaiinilla.app.domain.admin.AdminProductRules
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.Category
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.ui.admin.AdminImage
import com.vaiinilla.app.ui.admin.AdminUiState
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.SlidingSegments
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.screens.ActionPill
import com.vaiinilla.app.ui.screens.ChipButton
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.OperationalField
import com.vaiinilla.app.ui.screens.prepareProductImage

private sealed interface MenuSheet {
    data class EditCategory(
        val category: Category?,
    ) : MenuSheet

    data class EditProduct(
        val product: Product?,
    ) : MenuSheet
}

/** Menú: categorías y productos completos, como "Menú" en la web. Sin conexión de escritura, solo se ve. */
@Composable
internal fun AdminMenuTab(
    state: AdminUiState,
    colors: OperationalColors,
    readOnly: Boolean,
    actions: AdminActions,
    /** El negocio renta canchas desde Espacios: la renta no se da de alta como producto. */
    rentsSpaces: Boolean = false,
    /** Con tarjeta se suma la comisión al precio (Stripe listo y "el cliente paga la comisión"). */
    cardFeeToCustomer: Boolean = false,
    /** En una escuela, cada producto necesita sus alérgenos revisados para venderse a alumnos con alergias. */
    isSchool: Boolean = false,
) {
    var sheet by remember { mutableStateOf<MenuSheet?>(null) }
    val catalog = state.catalog
    // El aviso flotante queda detrás de la hoja: el error se repite dentro, junto al botón de guardar.
    val sheetError = state.notice?.takeIf { state.noticeIsError }
    when {
        catalog == null && state.catalogError != null ->
            Column(
                Modifier.padding(16.dp),
            ) { AdminLoadError(state.catalogError, colors) { actions.onRefreshCatalog() } }
        catalog == null -> Box(Modifier.fillMaxSize())
        else -> MenuList(catalog, colors, readOnly, actions, onOpen = { sheet = it }, saving = state.saving)
    }
    when (val current = sheet) {
        is MenuSheet.EditCategory ->
            CategorySheet(
                category = current.category,
                nextOrder = (catalog?.categories?.maxOfOrNull { it.order } ?: 0) + 1,
                saving = state.saving,
                error = sheetError,
                colors = colors,
                onDismiss = { sheet = null },
                onSave = { input -> actions.onSaveCategory(current.category?.id, input) { sheet = null } },
            )
        is MenuSheet.EditProduct ->
            ProductSheet(
                product = current.product,
                categories = catalog?.categories.orEmpty().sortedBy { it.order },
                saving = state.saving,
                error = sheetError,
                colors = colors,
                onDismiss = { sheet = null },
                onSave = { input, image -> actions.onSaveProduct(current.product?.id, input, image) { sheet = null } },
                rentsSpaces = rentsSpaces,
                cardFeeToCustomer = cardFeeToCustomer,
                onRemoveImage = { product ->
                    actions.onRemoveImage(product)
                    sheet = null
                },
            )
        null -> Unit
    }
}

@Composable
private fun MenuList(
    catalog: Catalog,
    colors: OperationalColors,
    readOnly: Boolean,
    actions: AdminActions,
    saving: Boolean,
    onOpen: (MenuSheet) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var availability by remember { mutableStateOf(AvailabilityFilter.ALL) }
    var categoryFilter by remember { mutableStateOf<Int?>(null) }
    val allCategories = catalog.categories.sortedBy { it.order }
    val categories = allCategories.filter { categoryFilter == null || it.id == categoryFilter }
    val known = allCategories.map { it.id }.toSet()
    val needle = query.trim().lowercase()
    val visible =
        catalog.products.filter { product ->
            (
                needle.isEmpty() ||
                    product.name.lowercase().contains(needle) ||
                    product.description.lowercase().contains(needle)
            ) &&
                availability.matches(product)
        }
    val orphans = if (categoryFilter == null) visible.filter { it.categoryId !in known } else emptyList()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (!readOnly) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionPill(
                        label = "Nuevo producto",
                        enabled = categories.isNotEmpty() && !saving,
                        loading = false,
                        colors = colors,
                        modifier = Modifier.weight(1f),
                    ) { onOpen(MenuSheet.EditProduct(null)) }
                    ActionPill(
                        label = "Nueva categoría",
                        enabled = !saving,
                        loading = false,
                        colors = colors,
                        primary = false,
                        modifier = Modifier.weight(1f),
                    ) { onOpen(MenuSheet.EditCategory(null)) }
                }
            }
        }
        if (catalog.products.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OperationalField(
                        value = query,
                        onValueChange = { query = it },
                        label = "Buscar en el menú",
                        colors = colors,
                    )
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AvailabilityFilter.entries.forEach { filter ->
                            ChipButton(
                                label = filter.label,
                                enabled = true,
                                colors = colors,
                                selected =
                                    filter == availability,
                            ) {
                                availability = filter
                            }
                        }
                    }
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ChipButton(
                            label = "Todas",
                            enabled = true,
                            colors = colors,
                            selected = categoryFilter == null,
                        ) {
                            categoryFilter =
                                null
                        }
                        allCategories.forEach { category ->
                            ChipButton(
                                label = category.name,
                                enabled = true,
                                colors = colors,
                                selected =
                                    category.id == categoryFilter,
                            ) {
                                categoryFilter = category.id
                            }
                        }
                    }
                }
            }
        }
        if (allCategories.isEmpty()) {
            item {
                AdminCard(colors) {
                    Text(
                        "Todavía no hay categorías",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = colors.textPrimary,
                    )
                    Text(
                        "Crea una categoría (Bebidas, Comida…) para empezar a agregar productos.",
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                    )
                }
            }
        }
        categories.forEach { category ->
            val products = visible.filter { it.categoryId == category.id }
            if (products.isEmpty() && (needle.isNotEmpty() || availability != AvailabilityFilter.ALL)) return@forEach
            item(key = "c${category.id}") {
                CategoryHeader(category, products.size, colors, readOnly) { onOpen(MenuSheet.EditCategory(category)) }
            }
            items(products, key = { "p${it.id}" }) { product ->
                ProductRow(product, colors, readOnly, saving, actions) { onOpen(MenuSheet.EditProduct(product)) }
            }
        }
        if (orphans.isNotEmpty()) {
            item { AdminSectionTitle("Sin categoría", colors, Modifier.padding(top = 8.dp)) }
            items(orphans, key = { "p${it.id}" }) { product ->
                ProductRow(product, colors, readOnly, saving, actions) { onOpen(MenuSheet.EditProduct(product)) }
            }
        }
        item { Box(Modifier.height(8.dp)) }
    }
}

@Composable
private fun CategoryHeader(
    category: Category,
    count: Int,
    colors: OperationalColors,
    readOnly: Boolean,
    onEdit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(category.name, fontWeight = FontWeight.Black, fontSize = 18.sp, color = colors.textPrimary)
            Text(if (count == 1) "1 producto" else "$count productos", fontSize = 12.sp, color = colors.textSecondary)
        }
        if (!readOnly) {
            Row(
                modifier = Modifier.physicalPress(scale = PhysicalPressScale.Small, onClick = onEdit).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(16.dp),
                )
                Text("Editar", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun ProductRow(
    product: Product,
    colors: OperationalColors,
    readOnly: Boolean,
    saving: Boolean,
    actions: AdminActions,
    onOpen: () -> Unit,
) {
    val haptics = rememberVaiinillaHaptics()
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .physicalPress(enabled = !readOnly, onClick = onOpen)
                .clip(shape)
                .background(colors.cardBackground)
                .border(1.dp, colors.cardBorder, shape)
                .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ProductImage(
            imageUrl = product.imageUrl,
            contentDescription = product.name,
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
        )
        Column(Modifier.weight(1f)) {
            Text(
                product.name,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                buildString {
                    append(moneyLabel(product.counterPrice))
                    append(" · ")
                    append(stationLabel(product.preparationStation))
                    when (product.optionGroups.size) {
                        0 -> Unit
                        1 -> append(" · 1 grupo de opciones")
                        else -> append(" · ${product.optionGroups.size} grupos de opciones")
                    }
                },
                fontSize = 12.sp,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!product.available) Text("Agotado", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AdminCoral)
        }
        Switch(
            checked = product.available,
            onCheckedChange = {
                haptics.selection()
                actions.onSetAvailable(product, it)
            },
            enabled = !readOnly && !saving,
            colors = adminSwitchColors(colors),
            modifier = Modifier.semantics { contentDescription = "${product.name} disponible" },
        )
    }
}

@Composable
private fun CategorySheet(
    category: Category?,
    nextOrder: Int,
    saving: Boolean,
    error: String?,
    colors: OperationalColors,
    onDismiss: () -> Unit,
    onSave: (AdminCategoryInput) -> Unit,
) {
    var name by remember { mutableStateOf(category?.name.orEmpty()) }
    var order by remember { mutableStateOf((category?.order ?: nextOrder).toString()) }
    AdminSheet(
        title =
            if (category ==
                null
            ) {
                "Nueva categoría"
            } else {
                "Editar categoría"
            },
        colors = colors,
        busy = saving,
        onDismiss = onDismiss,
    ) {
        OperationalField(
            value = name,
            onValueChange = { name = it.take(80) },
            label = "Nombre",
            colors = colors,
            placeholder = "Ej. Bebidas",
            enabled = !saving,
        )
        OperationalField(
            value = order,
            onValueChange = { order = it.filter(Char::isDigit).take(4) },
            label = "Orden en el menú",
            colors = colors,
            supporting = "Las categorías con número menor salen primero.",
            enabled = !saving,
            keyboardType = KeyboardType.Number,
        )
        error?.let { SheetError(it) }
        ActionPill(label = "Guardar categoría", enabled = name.isNotBlank(), loading = saving, colors = colors) {
            onSave(AdminCategoryInput(name = name.trim(), order = order.toIntOrNull() ?: nextOrder))
        }
    }
}

/** Borrador editable de un grupo de opciones; los números se guardan como texto mientras se escriben. */
private class GroupDraft(
    val id: Int?,
    name: String,
    min: String,
    max: String,
    options: List<OptionDraft>,
) {
    var name by mutableStateOf(name)
    var min by mutableStateOf(min)
    var max by mutableStateOf(max)
    val options: SnapshotStateList<OptionDraft> = mutableStateListOf<OptionDraft>().apply { addAll(options) }

    fun toInput() =
        AdminOptionGroupInput(
            id = id,
            name = name,
            minimumSelections = min.toIntOrNull() ?: -1,
            maximumSelections = max.toIntOrNull() ?: -1,
            options =
                options.map {
                    AdminOptionInput(
                        id = it.id,
                        name = it.name,
                        extraPrice = it.price.ifBlank { "0" },
                    )
                },
        )
}

private class OptionDraft(
    val id: Int?,
    name: String,
    price: String,
) {
    var name by mutableStateOf(name)
    var price by mutableStateOf(price)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductSheet(
    product: Product?,
    categories: List<Category>,
    saving: Boolean,
    error: String?,
    colors: OperationalColors,
    onDismiss: () -> Unit,
    onSave: (AdminProductInput, AdminImage?) -> Unit,
    onRemoveImage: (Product) -> Unit,
    rentsSpaces: Boolean,
    cardFeeToCustomer: Boolean,
) {
    val context = LocalContext.current
    val haptics = rememberVaiinillaHaptics()
    var categoryId by remember { mutableStateOf(product?.categoryId ?: categories.firstOrNull()?.id) }
    var station by remember { mutableStateOf(product?.preparationStation ?: PreparationStation.CASHIER) }
    var name by remember { mutableStateOf(product?.name.orEmpty()) }
    var price by remember { mutableStateOf(product?.counterPrice?.removeSuffix(".00").orEmpty()) }
    var minutes by remember { mutableStateOf((product?.estimatedTimeMinutes ?: 5).toString()) }
    var description by remember { mutableStateOf(product?.description.orEmpty()) }
    var ingredients by remember { mutableStateOf(product?.ingredients.orEmpty()) }
    var allergens by remember { mutableStateOf(product?.allergens.orEmpty()) }
    var available by remember { mutableStateOf(product?.available ?: true) }
    var image by remember { mutableStateOf<AdminImage?>(null) }
    var imageError by remember { mutableStateOf<String?>(null) }
    val groups =
        remember {
            mutableStateListOf<GroupDraft>().apply {
                product?.optionGroups?.forEach { group ->
                    add(
                        GroupDraft(
                            id = group.id,
                            name = group.name,
                            min = group.minimumSelections.toString(),
                            max = group.maximumSelections.toString(),
                            options =
                                group.options.map {
                                    OptionDraft(
                                        it.id,
                                        it.name,
                                        it.extraPrice.removeSuffix(".00"),
                                    )
                                },
                        ),
                    )
                }
            }
        }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            val prepared = runCatching { prepareProductImage(context, uri) }.getOrNull()
            if (prepared == null) {
                imageError = "No se pudo preparar la foto. Prueba otra imagen de hasta 16 MB."
            } else {
                imageError = null
                image = AdminImage(prepared.bytes, prepared.filename, prepared.mimeType)
            }
        }

    AdminSheet(title = product?.name ?: "Nuevo producto", colors = colors, busy = saving, onDismiss = onDismiss) {
        PhotoBox(product, image, colors, saving) { picker.launch("image/*") }
        imageError?.let { Text(it, fontSize = 12.sp, color = AdminCoral) }
        if (product != null && product.imageUrl.isNotBlank() && image == null) {
            Text(
                "Quitar foto",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AdminCoral,
                modifier =
                    Modifier.physicalPress(
                        enabled = !saving,
                        scale = PhysicalPressScale.Small,
                    ) { onRemoveImage(product) },
            )
        }

        AdminFieldLabel("Categoría", colors)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEach { category ->
                ChipButton(
                    label = category.name,
                    enabled = !saving,
                    colors = colors,
                    selected =
                        category.id == categoryId,
                ) {
                    haptics.selection()
                    categoryId = category.id
                }
            }
        }

        AdminFieldLabel("Se prepara en", colors)
        val stations = listOf(PreparationStation.CASHIER, PreparationStation.KITCHEN)
        SlidingSegments(
            labels = stations.map(::stationLabel),
            selectedIndex = stations.indexOf(station).coerceAtLeast(0),
            onSelect = {
                haptics.selection()
                station = stations[it]
            },
            trackColor = colors.cardInner,
            indicatorColor = colors.textPrimary,
            selectedContentColor = colors.background,
            idleContentColor = colors.textSecondary,
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving,
            verticalPadding = 11.dp,
        )

        OperationalField(
            value = name,
            onValueChange = { name = it.take(120) },
            label = "Nombre",
            colors = colors,
            placeholder = "Ej. Matcha frío con avena",
            enabled = !saving,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OperationalField(
                value = price,
                onValueChange = { price = moneyInput(it) },
                label = "Precio",
                colors = colors,
                placeholder = "65",
                enabled = !saving,
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
            )
            OperationalField(
                value = minutes,
                onValueChange = { minutes = it.filter(Char::isDigit).take(3) },
                label = "Minutos",
                colors = colors,
                placeholder = "5",
                enabled = !saving,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            if (cardFeeToCustomer) {
                "Con efectivo o saldo, este precio. Con tarjeta se suma la comisión."
            } else {
                "El mismo precio con efectivo, saldo o tarjeta."
            },
            fontSize = 12.sp,
            color = colors.textSecondary,
        )
        if (rentsSpaces) {
            val looksLikeRental = RentalWords.containsMatchIn(name)
            Text(
                if (looksLikeRental) {
                    "Esto parece una renta de cancha. Las rentas se cobran desde Mesas y espacios, con su precio por hora."
                } else {
                    "Este negocio renta canchas desde Mesas y espacios. No crees la renta como producto."
                },
                fontSize = 12.sp,
                fontWeight = if (looksLikeRental) FontWeight.Bold else FontWeight.Normal,
                color = if (looksLikeRental) AdminCoral else colors.textSecondary,
            )
        }
        OperationalField(
            value = description,
            onValueChange = { description = it },
            label = "Descripción",
            colors = colors,
            enabled = !saving,
            singleLine = false,
            minLines = 2,
        )
        OperationalField(
            value = ingredients,
            onValueChange = { ingredients = it },
            label = "Ingredientes",
            colors = colors,
            enabled = !saving,
            singleLine = false,
        )
        OperationalField(
            value = allergens,
            onValueChange = { allergens = it },
            label = "Alérgenos",
            colors = colors,
            placeholder = "Ej. Lácteos, nuez",
            enabled = !saving,
            singleLine = false,
        )

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Disponible", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Text("Apagado, sale como agotado y no se puede pedir.", fontSize = 12.sp, color = colors.textSecondary)
            }
            Switch(
                checked = available,
                onCheckedChange = { available = it },
                enabled = !saving,
                colors = adminSwitchColors(colors),
            )
        }

        AdminSectionTitle("Opciones y extras", colors, Modifier.padding(top = 6.dp))
        Text(
            "Tamaños, leches, extras… Mínimo 0 si el grupo es opcional.",
            fontSize = 12.sp,
            color = colors.textSecondary,
        )
        groups.forEachIndexed { index, group ->
            GroupEditor(group, colors, saving, onRemove = { groups.removeAt(index) })
        }
        if (groups.size < AdminProductRules.MAX_GROUPS) {
            AddRow("Agregar grupo", colors, saving) {
                groups.add(GroupDraft(null, "", "0", "1", listOf(OptionDraft(null, "", "0"))))
            }
        }

        error?.let { SheetError(it) }
        ActionPill(
            label = if (product == null) "Crear producto" else "Guardar cambios",
            enabled = name.isNotBlank() && price.isNotBlank() && categoryId != null,
            loading = saving,
            colors = colors,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            haptics.impact()
            val category = categoryId ?: return@ActionPill
            onSave(
                AdminProductInput(
                    categoryId = category,
                    preparationStation = station,
                    name = name,
                    description = description,
                    ingredients = ingredients,
                    allergens = allergens,
                    estimatedTimeMinutes = minutes.toIntOrNull() ?: 0,
                    counterPrice = price,
                    available = available,
                    optionGroups = groups.map { it.toInput() },
                ),
                image,
            )
        }
    }
}

@Composable
private fun PhotoBox(
    product: Product?,
    image: AdminImage?,
    colors: OperationalColors,
    saving: Boolean,
    onPick: () -> Unit,
) {
    val preview =
        remember(image) { image?.let { BitmapFactory.decodeByteArray(it.bytes, 0, it.bytes.size)?.asImageBitmap() } }
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(150.dp)
                .physicalPress(enabled = !saving, onClick = onPick)
                .clip(shape)
                .background(colors.cardBackground)
                .border(1.dp, colors.cardBorder, shape)
                .semantics { contentDescription = "Elegir foto del producto" },
        contentAlignment = Alignment.Center,
    ) {
        when {
            preview != null ->
                Image(
                    preview,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            product != null && product.imageUrl.isNotBlank() ->
                ProductImage(
                    imageUrl = product.imageUrl,
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                )
            else ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.PhotoCamera,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(26.dp),
                    )
                    Text(
                        "Foto del producto",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                    Text("Toca para elegir de la galería", fontSize = 11.sp, color = colors.textMuted)
                }
        }
    }
}

@Composable
private fun GroupEditor(
    group: GroupDraft,
    colors: OperationalColors,
    saving: Boolean,
    onRemove: () -> Unit,
) {
    AdminCard(colors) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OperationalField(
                value = group.name,
                onValueChange = { group.name = it.take(80) },
                label = "Nombre del grupo",
                colors = colors,
                placeholder = "Ej. Tamaño",
                enabled = !saving,
                modifier = Modifier.weight(1f),
            )
            RemoveButton("Quitar grupo", colors, saving, onRemove)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OperationalField(
                value = group.min,
                onValueChange = { group.min = it.filter(Char::isDigit).take(2) },
                label = "Mínimo",
                colors = colors,
                enabled = !saving,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
            OperationalField(
                value = group.max,
                onValueChange = { group.max = it.filter(Char::isDigit).take(2) },
                label = "Máximo",
                colors = colors,
                enabled = !saving,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
        }
        group.options.forEachIndexed { index, option ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OperationalField(
                    value = option.name,
                    onValueChange = { option.name = it.take(80) },
                    label = "Opción",
                    colors = colors,
                    placeholder = "Ej. Grande",
                    enabled = !saving,
                    modifier = Modifier.weight(1.6f),
                )
                OperationalField(
                    value = option.price,
                    onValueChange = { option.price = moneyInput(it) },
                    label = "Extra",
                    colors = colors,
                    placeholder = "0",
                    enabled = !saving,
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
                if (group.options.size >
                    1
                ) {
                    RemoveButton("Quitar opción", colors, saving) { group.options.removeAt(index) }
                }
            }
        }
        if (group.options.size < AdminProductRules.MAX_OPTIONS) {
            AddRow("Agregar opción", colors, saving) { group.options.add(OptionDraft(null, "", "0")) }
        }
    }
}

@Composable
private fun SheetError(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = AdminCoral,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun AddRow(
    label: String,
    colors: OperationalColors,
    saving: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .physicalPress(
                    enabled = !saving,
                    scale = PhysicalPressScale.Small,
                    onClick = onClick,
                ).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Outlined.Add, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
    }
}

@Composable
private fun RemoveButton(
    label: String,
    colors: OperationalColors,
    saving: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .physicalPress(enabled = !saving, scale = PhysicalPressScale.Small, onClick = onClick)
                .size(44.dp)
                .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Outlined.Delete,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun AdminFieldLabel(
    text: String,
    colors: OperationalColors,
) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
}

@Composable
private fun adminSwitchColors(colors: OperationalColors) =
    SwitchDefaults.colors(
        checkedThumbColor = colors.accentInk,
        checkedTrackColor = colors.accentLime,
        uncheckedThumbColor = colors.textSecondary,
        uncheckedTrackColor = colors.cardInner,
        uncheckedBorderColor = colors.cardBorder,
    )

private enum class AvailabilityFilter(
    val label: String,
) {
    ALL("Todos"),
    AVAILABLE("Disponibles"),
    HIDDEN("Agotados"),
    ;

    fun matches(product: Product): Boolean =
        when (this) {
            ALL -> true
            AVAILABLE -> product.available
            HIDDEN -> !product.available
        }
}

private val RentalWords =
    Regex("\\b(renta|rentar|cancha|canchas|hora|horas|reserva|p[aá]del)\\b", RegexOption.IGNORE_CASE)

private fun stationLabel(station: PreparationStation): String =
    when (station) {
        PreparationStation.CASHIER -> "Barra"
        PreparationStation.KITCHEN -> "Cocina"
    }

/** Deja solo dígitos y un punto con hasta dos decimales. */
private fun moneyInput(raw: String): String {
    val clean = raw.filter { it.isDigit() || it == '.' }
    val dot = clean.indexOf('.')
    if (dot < 0) return clean.take(8)
    return clean.substring(0, dot).take(8) + "." + clean.substring(dot + 1).replace(".", "").take(2)
}
