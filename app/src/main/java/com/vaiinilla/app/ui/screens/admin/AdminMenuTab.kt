package com.vaiinilla.app.ui.screens.admin

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.core.text.normalizeForSearch
import com.vaiinilla.app.domain.admin.AdminCategoryInput
import com.vaiinilla.app.domain.admin.AdminOptionGroupInput
import com.vaiinilla.app.domain.admin.AdminOptionInput
import com.vaiinilla.app.domain.admin.AdminProductInput
import com.vaiinilla.app.domain.admin.AdminProductRules
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.Category
import com.vaiinilla.app.domain.model.MoneyInput
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.school.SchoolAllergen
import com.vaiinilla.app.ui.admin.AdminImage
import com.vaiinilla.app.ui.admin.AdminUiState
import com.vaiinilla.app.ui.components.EditorialSearchField
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.SlidingSegments
import com.vaiinilla.app.ui.components.animateSelectionColor
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.productImageIsRemote
import com.vaiinilla.app.ui.components.reducedMotion
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
    header: AdminHeader,
) {
    var sheet by remember { mutableStateOf<MenuSheet?>(null) }
    val catalog = state.catalog
    // El aviso flotante queda detrás de la hoja: el error se repite dentro, junto al botón de guardar.
    val sheetError = state.notice?.takeIf { state.noticeIsError }
    when {
        catalog == null && state.catalogError != null ->
            Column(
                Modifier.statusBarsPadding().padding(16.dp),
            ) { AdminLoadError(state.catalogError, colors) { actions.onRefreshCatalog() } }
        catalog == null -> Box(Modifier.fillMaxSize())
        else ->
            MenuList(
                catalog,
                colors,
                readOnly,
                actions,
                onOpen = { sheet = it },
                saving = state.saving,
                header = header,
                isSchool = isSchool,
                archived = state.archivedProducts,
            )
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
                isSchool = isSchool,
                onArchive = { product ->
                    actions.onSetArchived(product, true)
                    sheet = null
                },
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
    header: AdminHeader,
    isSchool: Boolean = false,
    archived: List<Product> = emptyList(),
    onOpen: (MenuSheet) -> Unit,
) {
    var showArchived by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var availability by remember { mutableStateOf(AvailabilityFilter.ALL) }
    var categoryFilter by remember { mutableStateOf<Int?>(null) }
    val allCategories = catalog.categories.sortedBy { it.order }
    // Una categoría que ya no está (borrada o de otro negocio) no deja el menú vacío.
    LaunchedEffect(allCategories) {
        if (categoryFilter != null && allCategories.none { it.id == categoryFilter }) categoryFilter = null
    }
    val categories = allCategories.filter { categoryFilter == null || it.id == categoryFilter }
    val known = allCategories.map { it.id }.toSet()
    val needle = query.normalizeForSearch()
    val visible =
        catalog.products.filter { product ->
            (
                needle.isEmpty() ||
                    product.name.normalizeForSearch().contains(needle) ||
                    product.description.normalizeForSearch().contains(needle)
            ) &&
                availability.matches(product)
        }
    val orphans = if (categoryFilter == null) visible.filter { it.categoryId !in known } else emptyList()
    val soldOut = catalog.products.count { !it.available }
    val heroPhoto =
        remember(catalog) { catalog.products.firstOrNull { productImageIsRemote(it.imageUrl) }?.imageUrl }
    val gridState = rememberLazyGridState()
    val heroGone by rememberHeroGone(gridState)
    val chips: @Composable () -> Unit = {
        AdminChipRow {
            AdminGlassChip("Todo", categoryFilter == null && availability == AvailabilityFilter.ALL) {
                categoryFilter = null
                availability = AvailabilityFilter.ALL
            }
            allCategories.forEach { category ->
                AdminGlassChip(category.name, category.id == categoryFilter) {
                    categoryFilter = if (categoryFilter == category.id) null else category.id
                }
            }
            if (soldOut > 0 || availability == AvailabilityFilter.HIDDEN) {
                AdminGlassChip("Agotados · $soldOut", availability == AvailabilityFilter.HIDDEN) {
                    availability =
                        if (availability ==
                            AvailabilityFilter.HIDDEN
                        ) {
                            AvailabilityFilter.ALL
                        } else {
                            AvailabilityFilter.HIDDEN
                        }
                }
            }
            if (archived.isNotEmpty()) {
                AdminGlassChip("Archivados · ${archived.size}", showArchived) { showArchived = !showArchived }
            }
            if (!readOnly) AdminGlassChip("+ Categoría", false) { onOpen(MenuSheet.EditCategory(null)) }
        }
    }
    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = MenuSide, end = MenuSide, bottom = if (readOnly) 24.dp else 96.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "hero", span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.bleed(MenuSide)) {
                    AdminHero(
                        title = "Menú",
                        subtitle =
                            buildString {
                                append(
                                    if (catalog.products.size ==
                                        1
                                    ) {
                                        "1 producto"
                                    } else {
                                        "${catalog.products.size} productos"
                                    },
                                )
                                if (soldOut > 0) append(" · ${if (soldOut == 1) "1 agotado" else "$soldOut agotados"}")
                            },
                        colors = colors,
                        height = 290.dp,
                        photoUrl = heroPhoto,
                        logoUrl = header.logoUrl,
                        logoName = header.placeName,
                        trailing = header.avatar,
                    )
                }
            }
            val unreviewed = if (isSchool) catalog.products.count { !it.allergensReviewed } else 0
            if (unreviewed > 0) {
                item(key = "alergenos-sin-revisar", span = { GridItemSpan(maxLineSpan) }) {
                    AdminCard(colors) {
                        Text(
                            if (unreviewed == 1) {
                                "1 producto sin alérgenos revisados"
                            } else {
                                "$unreviewed productos sin alérgenos revisados"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                        )
                        Text(
                            "Un alumno con alergias no puede pedirlos hasta que marques qué tienen.",
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
            if (catalog.products.isNotEmpty()) {
                item(key = "buscar", span = { GridItemSpan(maxLineSpan) }) {
                    EditorialSearchField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = "Buscar en tu menú…",
                        seaGlass = true,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                }
            }
            item(key = "chips", span = { GridItemSpan(maxLineSpan) }) { Box(Modifier.bleed(MenuSide)) { chips() } }
            if (showArchived && archived.isNotEmpty()) {
                item(key = "archivados", span = { GridItemSpan(maxLineSpan) }) {
                    AdminCard(colors) {
                        Text("Archivados", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Text(
                            "No salen en el menú. Desarchiva uno para volver a venderlo.",
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                        )
                        archived.forEach { product ->
                            Row(
                                Modifier.fillMaxWidth().padding(top = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    product.name,
                                    fontSize = 14.sp,
                                    color = colors.textPrimary,
                                    modifier = Modifier.weight(1f),
                                )
                                if (!readOnly) {
                                    Text(
                                        "Desarchivar",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                        modifier =
                                            Modifier
                                                .physicalPress(enabled = !saving, scale = PhysicalPressScale.Small) {
                                                    actions.onSetArchived(product, false)
                                                }.semantics { contentDescription = "Desarchivar ${product.name}" }
                                                .padding(8.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (allCategories.isEmpty()) {
                item(key = "sin-categorias", span = { GridItemSpan(maxLineSpan) }) {
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
                if (products.isEmpty() &&
                    (needle.isNotEmpty() || availability != AvailabilityFilter.ALL)
                ) {
                    return@forEach
                }
                item(key = "c${category.id}", span = { GridItemSpan(maxLineSpan) }) {
                    CategoryHeader(category, products.size, colors, readOnly) {
                        onOpen(MenuSheet.EditCategory(category))
                    }
                }
                items(products, key = { "p${it.id}" }) { product ->
                    Box(Modifier.animateItem()) {
                        AdminProductCard(product, colors, readOnly, saving, actions, isSchool) {
                            onOpen(MenuSheet.EditProduct(product))
                        }
                    }
                }
            }
            if (orphans.isNotEmpty()) {
                item(key = "sin-categoria", span = { GridItemSpan(maxLineSpan) }) {
                    AdminSectionTitle("Sin categoría", colors, Modifier.padding(top = 8.dp))
                }
                items(orphans, key = { "p${it.id}" }) { product ->
                    Box(Modifier.animateItem()) {
                        AdminProductCard(product, colors, readOnly, saving, actions, isSchool) {
                            onOpen(MenuSheet.EditProduct(product))
                        }
                    }
                }
            }
        }
        if (!readOnly) {
            NewProductPill(
                enabled = allCategories.isNotEmpty() && !saving,
                colors = colors,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 16.dp),
            ) { onOpen(MenuSheet.EditProduct(null)) }
        }
        AdminCompactBar(
            visible = heroGone,
            title = header.placeName,
            logoUrl = header.logoUrl,
            colors = colors,
            trailing = { header.avatar() },
            below = chips,
        )
    }
}

private val MenuSide = 20.dp

/** Ocupa también el margen lateral de la cuadrícula, para que la foto y los chips lleguen de borde a borde. */
private fun Modifier.bleed(margin: Dp): Modifier =
    layout { measurable, constraints ->
        val extra = (margin * 2).roundToPx()
        val width = constraints.maxWidth + extra
        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
        layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
    }

@Composable
private fun NewProductPill(
    enabled: Boolean,
    colors: OperationalColors,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val haptics = rememberVaiinillaHaptics()
    Row(
        modifier =
            modifier
                .height(52.dp)
                .physicalPress(enabled = enabled) {
                    haptics.click()
                    onClick()
                }.shadow(14.dp, CircleShape)
                .clip(CircleShape)
                .background(if (enabled) colors.textPrimary else colors.cardBorder)
                .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = colors.background, modifier = Modifier.size(20.dp))
        Text("Nuevo producto", color = colors.background, fontWeight = FontWeight.Black, fontSize = 15.sp)
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
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(category.name, fontWeight = FontWeight.Black, fontSize = 20.sp, color = colors.textPrimary)
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

/**
 * Un producto con su foto completa, como en el menú del alumno. Tocarlo abre el editor; la etiqueta de arriba
 * cambia Disponible/Agotado de un toque, sin abrir nada. Agotado se ve apagado.
 */
@Composable
private fun AdminProductCard(
    product: Product,
    colors: OperationalColors,
    readOnly: Boolean,
    saving: Boolean,
    actions: AdminActions,
    isSchool: Boolean,
    onOpen: () -> Unit,
) {
    val reduced = reducedMotion()
    val soldOut by animateFloatAsState(
        targetValue = if (product.available) 0f else 1f,
        animationSpec = if (reduced) snap() else spring(stiffness = 500f),
        label = "agotado",
    )
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.84f)
                .physicalPress(enabled = !readOnly, scale = PhysicalPressScale.ProductCard, onClick = onOpen)
                .semantics { contentDescription = "Editar ${product.name}" }
                .clip(RoundedCornerShape(26.dp))
                .background(colors.cardInner),
    ) {
        ProductImage(
            imageUrl = product.imageUrl,
            contentDescription = product.name,
            modifier = Modifier.fillMaxSize(),
            placeholder = { m ->
                Box(m.background(colors.cardInner), contentAlignment = Alignment.Center) {
                    Text(
                        product.name
                            .trim()
                            .firstOrNull()
                            ?.uppercase()
                            .orEmpty(),
                        color = colors.textMuted,
                        fontWeight = FontWeight.Black,
                        fontSize = 40.sp,
                    )
                }
            },
            card = true,
        )
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = soldOut }
                .background(Color.Black.copy(alpha = 0.55f)),
        )
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(CardScrim)
                    .padding(start = 14.dp, end = 14.dp, top = 34.dp, bottom = 12.dp),
        ) {
            Text(
                product.name,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                lineHeight = 19.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                moneyLabel(product.counterPrice),
                color = Color.White.copy(alpha = 0.88f),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 3.dp),
            )
            if (isSchool && !product.allergensReviewed) {
                Text("Alérgenos sin revisar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AdminCoral)
            }
        }
        if (!readOnly) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        AvailabilityBadge(
            product = product,
            enabled = !readOnly && !saving,
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
        ) { actions.onSetAvailable(product, !product.available) }
    }
}

@Composable
private fun AvailabilityBadge(
    product: Product,
    enabled: Boolean,
    modifier: Modifier,
    onToggle: () -> Unit,
) {
    val haptics = rememberVaiinillaHaptics()
    val available = product.available
    val background = animateSelectionColor(if (available) Color.Black.copy(alpha = 0.45f) else SoldOutBadge, "badge")
    Row(
        modifier =
            modifier
                .height(32.dp)
                .physicalPress(enabled = enabled, scale = PhysicalPressScale.Small) {
                    haptics.selection()
                    onToggle()
                }.semantics {
                    role = Role.Switch
                    stateDescription = if (available) "Disponible" else "Agotado"
                    contentDescription = "${product.name} disponible"
                }.clip(CircleShape)
                .background(background)
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (available) AvailableDot else AdminCoral))
        Text(
            if (available) "Disponible" else "Agotado",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
        )
    }
}

private val CardScrim =
    Brush.verticalGradient(
        0f to Color.Transparent,
        0.45f to Color.Black.copy(alpha = 0.55f),
        1f to Color.Black.copy(alpha = 0.78f),
    )
private val SoldOutBadge = Color(0xC0782818)
private val AvailableDot = Color(0xFFB9DA6E)

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
    isSchool: Boolean = false,
    onArchive: (Product) -> Unit = {},
) {
    val context = LocalContext.current
    var confirmArchive by remember { mutableStateOf(false) }
    val haptics = rememberVaiinillaHaptics()
    var categoryId by remember { mutableStateOf(product?.categoryId ?: categories.firstOrNull()?.id) }
    var station by remember { mutableStateOf(product?.preparationStation ?: PreparationStation.CASHIER) }
    var name by remember { mutableStateOf(product?.name.orEmpty()) }
    var price by remember { mutableStateOf(product?.counterPrice?.removeSuffix(".00").orEmpty()) }
    var minutes by remember { mutableStateOf((product?.estimatedTimeMinutes ?: 5).toString()) }
    var description by remember { mutableStateOf(product?.description.orEmpty()) }
    var ingredients by remember { mutableStateOf(product?.ingredients.orEmpty()) }
    var allergens by remember { mutableStateOf(legacyAllergensText(product?.allergens)) }
    var allergenCodes by remember { mutableStateOf(product?.allergenCodes.orEmpty().toSet()) }
    var allergensReviewed by remember { mutableStateOf(product?.allergensReviewed ?: false) }
    // Solo lo que el admin revisó se manda: guardar el precio no marca revisado un producto que nadie miró.
    var allergensTouched by remember { mutableStateOf(false) }
    var markUnreviewed by remember { mutableStateOf(false) }
    var available by remember { mutableStateOf(product?.available ?: true) }
    var confirmRental by remember { mutableStateOf(false) }
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
        val looksLikeRental = RentalWords.containsMatchIn(name)
        if (looksLikeRental || rentsSpaces) {
            Text(
                when {
                    looksLikeRental && rentsSpaces ->
                        "Esto parece una renta de cancha. Las rentas se cobran desde Mesas y espacios, con su precio por hora."
                    looksLikeRental ->
                        "Esto parece una renta. Si cobras un espacio por hora, dalo de alta en Mesas y espacios."
                    else -> "Este negocio renta canchas desde Mesas y espacios. No crees la renta como producto."
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
        if (isSchool) {
            AllergenReview(
                codes = allergenCodes,
                reviewed = allergensReviewed,
                colors = colors,
                enabled = !saving,
            ) { codes ->
                haptics.selection()
                allergenCodes = codes
                allergensReviewed = true
                allergensTouched = true
                markUnreviewed = false
            }
            if (allergensReviewed && product != null) {
                Text(
                    "Volver a sin revisar",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier =
                        Modifier
                            .physicalPress(scale = PhysicalPressScale.Small, enabled = !saving) {
                                haptics.selection()
                                allergensReviewed = false
                                allergensTouched = false
                                markUnreviewed = true
                            }.padding(vertical = 8.dp),
                )
            }
        }

        // Toda la fila cambia el interruptor: tocar el texto «Disponible» no hacía nada y el interruptor solo
        // respondía en su borde (EQUIPO-14).
        Row(
            Modifier.fillMaxWidth().toggleable(
                value = available,
                enabled = !saving,
                role = Role.Switch,
                onValueChange = { available = it },
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Disponible", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Text("Apagado, sale como agotado y no se puede pedir.", fontSize = 12.sp, color = colors.textSecondary)
            }
            Switch(
                checked = available,
                onCheckedChange = null,
                enabled = !saving,
                colors = adminSwitchColors(colors),
            )
        }

        if (product != null) {
            Text(
                "Archivar",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AdminCoral,
                modifier =
                    Modifier
                        .physicalPress(enabled = !saving, scale = PhysicalPressScale.Small) { confirmArchive = true }
                        .padding(vertical = 8.dp),
            )
        }
        if (confirmArchive && product != null) {
            AlertDialog(
                onDismissRequest = { confirmArchive = false },
                title = { Text("¿Archivar ${product.name}?") },
                text = { Text("Sale del menú y nadie lo puede pedir. Lo encuentras en Archivados para regresarlo.") },
                confirmButton = {
                    TextButton(onClick = {
                        confirmArchive = false
                        onArchive(product)
                    }) { Text("Archivar") }
                },
                dismissButton = { TextButton(onClick = { confirmArchive = false }) { Text("Cancelar") } },
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
        val save = {
            val category = categoryId
            if (category != null) {
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
                        allergenCodes = allergenCodes.takeIf { isSchool && allergensTouched },
                        markUnreviewed = isSchool && markUnreviewed,
                    ),
                    image,
                )
            }
        }
        if (confirmRental) {
            AlertDialog(
                onDismissRequest = { confirmRental = false },
                title = { Text("¿Guardar como producto?") },
                text = { Text("«${name.trim()}» parece una renta. Como producto se vende por pieza, no por hora.") },
                confirmButton = {
                    TextButton(onClick = {
                        confirmRental = false
                        save()
                    }) { Text("Guardar") }
                },
                dismissButton = { TextButton(onClick = { confirmRental = false }) { Text("Cambiar nombre") } },
            )
        }
        ActionPill(
            label = if (product == null) "Crear producto" else "Guardar cambios",
            enabled = name.isNotBlank() && price.isNotBlank() && categoryId != null,
            loading = saving,
            colors = colors,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            haptics.impact()
            // Solo se pregunta si el nombre de renta es nuevo: editar un producto que ya se llamaba así no vuelve a frenar.
            if (looksLikeRental && name.trim() != product?.name?.trim()) confirmRental = true else save()
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

/** "Alérgenos para alumnos": los del catálogo como chips, más "No tiene ninguno". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AllergenReview(
    codes: Set<String>,
    reviewed: Boolean,
    colors: OperationalColors,
    enabled: Boolean,
    onChange: (Set<String>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
        AdminFieldLabel("Alérgenos para alumnos", colors)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipButton(
                label = "No tiene ninguno",
                enabled = enabled,
                colors = colors,
                selected = reviewed && codes.isEmpty(),
            ) { onChange(emptySet()) }
            SchoolAllergen.entries.forEach { allergen ->
                val on = allergen.code in codes
                ChipButton(label = allergen.label, enabled = enabled, colors = colors, selected = on) {
                    onChange(if (on) codes - allergen.code else codes + allergen.code)
                }
            }
        }
        Text(
            if (reviewed) {
                "Un alumno con alguna de estas alergias no lo podrá pedir."
            } else {
                "Sin revisar: un alumno con alergias no lo puede pedir. Marca lo que tiene o \"No tiene ninguno\"."
            },
            fontSize = 12.sp,
            fontWeight = if (reviewed) FontWeight.Normal else FontWeight.Bold,
            color = if (reviewed) colors.textSecondary else AdminCoral,
        )
    }
}

/** El texto viejo de alérgenos; un "{}" o "[]" guardado por error no se muestra como si fuera algo. */
internal fun legacyAllergensText(raw: String?): String = raw?.takeUnless { it.trim() in setOf("{}", "[]") }.orEmpty()

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

/** Deja dígitos y un separador con hasta dos decimales; la coma del teclado cuenta como punto. */
private fun moneyInput(raw: String): String = MoneyInput.sanitize(raw)
