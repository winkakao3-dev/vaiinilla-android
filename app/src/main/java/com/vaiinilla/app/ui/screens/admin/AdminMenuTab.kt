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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.admin.AdminCategoryInput
import com.vaiinilla.app.domain.admin.AdminOptionGroupInput
import com.vaiinilla.app.domain.admin.AdminOptionInput
import com.vaiinilla.app.domain.admin.AdminProductInput
import com.vaiinilla.app.domain.admin.AdminProductRules
import com.vaiinilla.app.domain.admin.ProductPreparationInput
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.Category
import com.vaiinilla.app.domain.model.MoneyInput
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.school.SchoolAllergen
import com.vaiinilla.app.ui.admin.AdminImage
import com.vaiinilla.app.ui.admin.AdminUiState
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
import com.vaiinilla.app.ui.theme.Yolk

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
    takesPreorders: Boolean = false,
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
                rentsSpaces = rentsSpaces,
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
                takesPreorders = takesPreorders,
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
    rentsSpaces: Boolean = false,
    archived: List<Product> = emptyList(),
    onOpen: (MenuSheet) -> Unit,
) {
    val presentDialog = LocalAdminDialog.current
    val haptics = rememberVaiinillaHaptics()
    var chosenCategory by rememberSaveable { mutableStateOf<Int?>(null) }
    var showingArchived by rememberSaveable { mutableStateOf(false) }
    if (archived.isEmpty()) showingArchived = false
    val categories = catalog.categories.sortedBy { it.order }
    val archivedIds = archived.map { it.id }.toSet()
    val active = catalog.products.filter { it.id !in archivedIds }
    val selected = categories.firstOrNull { it.id == chosenCategory } ?: categories.firstOrNull()
    val products = if (showingArchived) archived else active.filter { it.categoryId == selected?.id }
    val unreviewed = if (isSchool) active.filter { !it.allergensReviewed } else emptyList()
    val cover =
        header.photoUrl ?: remember(catalog) { active.firstOrNull { productImageIsRemote(it.imageUrl) }?.imageUrl }
    AdminHeroPage(
        cover = cover,
        caption = "Administración · ${if (active.size == 1) "1 producto" else "${active.size} productos"}",
        title = "Menú",
        colors = colors,
        compactTitle = "Menú",
        compactLogo = header.logoUrl,
        padsContent = false,
        trailing = { header.avatar() },
        floating = {
            if (!readOnly && categories.isNotEmpty()) {
                NewProductPill(
                    enabled = !saving,
                    colors = colors,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                ) { onOpen(MenuSheet.EditProduct(null)) }
            }
        },
    ) {
        if (categories.isEmpty()) {
            item(key = "vacio") {
                Box(Modifier.padding(horizontal = 16.dp).adminRise(1)) {
                    EmptyMenu(colors, readOnly) { onOpen(MenuSheet.EditCategory(null)) }
                }
            }
            return@AdminHeroPage
        }
        item(key = "chips") {
            Row(
                Modifier
                    .adminRise(1)
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                categories.forEach { category ->
                    val count = active.count { it.categoryId == category.id }
                    AdminFilterChip(
                        "${category.name} · $count",
                        selected = !showingArchived && category.id == selected?.id,
                        colors = colors,
                        onClick = {
                            chosenCategory = category.id
                            showingArchived = false
                        },
                        height = 44.dp,
                    )
                }
                if (archived.isNotEmpty()) {
                    AdminFilterChip("Archivados · ${archived.size}", showingArchived, colors, onClick = {
                        showingArchived =
                            true
                    }, height = 44.dp)
                }
                if (!readOnly) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .physicalPress(enabled = !saving, scale = PhysicalPressScale.Small) {
                                haptics.click()
                                onOpen(MenuSheet.EditCategory(null))
                            }.clip(CircleShape)
                            .border(1.dp, colors.cardBorder, CircleShape)
                            .semantics { contentDescription = "Nueva categoría" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
        if (!readOnly && !showingArchived && selected != null) {
            item(key = "editar-categoria") {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    AdminH2(
                        selected.name,
                        colors,
                        action = "Editar categoría",
                    ) { onOpen(MenuSheet.EditCategory(selected)) }
                }
            }
        }
        if (unreviewed.isNotEmpty() && !showingArchived) {
            item(key = "alergenos") {
                Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp).adminRise(1)) {
                    AllergenWarning(unreviewed.size, colors) {
                        if (!readOnly) onOpen(MenuSheet.EditProduct(unreviewed.first()))
                    }
                }
            }
        }
        if (showingArchived) {
            item(key = "archivados-nota") {
                Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp)) {
                    AdminHint(
                        "Los archivados no se ven en el menú ni se pueden pedir. Tócalos para regresarlos al menú.",
                        colors,
                    )
                }
            }
        }
        products.chunked(2).forEachIndexed { index, pair ->
            item(key = "fila-${if (showingArchived) "a" else selected?.id}-${pair.first().id}") {
                Row(
                    Modifier
                        .animateItem()
                        .adminRise(2)
                        .padding(start = 16.dp, end = 16.dp, top = if (index == 0) 16.dp else 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    pair.forEach { product ->
                        Box(Modifier.weight(1f)) {
                            if (showingArchived) {
                                ArchivedProductCard(product, colors, enabled = !readOnly && !saving) {
                                    presentDialog(
                                        AdminDialogRequest(
                                            title = "¿Regresar ${product.name} al menú?",
                                            message = "Se vuelve a ver en el menú y se puede pedir otra vez.",
                                            confirm = "Regresar al menú",
                                            icon = Icons.AutoMirrored.Rounded.Undo,
                                            destructive = false,
                                        ) {
                                            actions.onSetArchived(product, false)
                                            true
                                        },
                                    )
                                }
                            } else {
                                AdminProductCard(product, colors, readOnly, saving, actions, isSchool) {
                                    onOpen(MenuSheet.EditProduct(product))
                                }
                            }
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        if (products.isEmpty() && !showingArchived) {
            item(key = "categoria-vacia") {
                Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp)) {
                    AdminHint("Esta categoría todavía no tiene productos.", colors)
                }
            }
        }
        if (rentsSpaces) {
            item(key = "rentas") {
                Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp)) {
                    AdminHint("Las rentas de cancha no son productos: se cobran desde Mesas y espacios.", colors)
                }
            }
        }
        if (!readOnly) item(key = "espacio-boton") { Spacer(Modifier.height(70.dp)) }
    }
}

/** Marco 25: sin categorías, dos tarjetas giradas detrás del ícono y el botón para crear la primera. */
@Composable
private fun EmptyMenu(
    colors: OperationalColors,
    readOnly: Boolean,
    onCreate: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.padding(top = 40.dp).size(width = 150.dp, height = 110.dp), contentAlignment = Alignment.Center) {
            listOf(-8f to (-18).dp, 8f to 18.dp).forEach { (angle, x) ->
                Box(
                    Modifier
                        .offset(x = x, y = 8.dp)
                        .size(width = 112.dp, height = 96.dp)
                        .graphicsLayer { rotationZ = angle }
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.cardBackground),
                )
            }
            Box(
                Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.cardBackground)
                    .background(colors.textPrimary.copy(alpha = if (colors.isDark) 0.08f else 0.06f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Restaurant,
                    contentDescription = null,
                    tint = if (colors.isDark) colors.accentLime else colors.textPrimary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        AdminEmptyState(
            icon = null,
            title = "Todavía no hay categorías",
            message = "Crea una categoría, como «Pasteles» o «Bebidas», y después agrega tus productos con foto y precio.",
            colors = colors,
            action = if (readOnly) null else "Crear categoría",
            onAction = onCreate,
        )
    }
}

/** Abre el primer producto sin revisar; al guardarlo, el aviso baja de número hasta irse. */
@Composable
private fun AllergenWarning(
    count: Int,
    colors: OperationalColors,
    onOpen: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .physicalPress(scale = PhysicalPressScale.Small, onClick = onOpen)
            .clip(shape)
            .background(Yolk.copy(alpha = if (colors.isDark) 0.08f else 0.25f))
            .border(1.dp, Yolk.copy(alpha = 0.45f), shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = null,
            tint = if (colors.isDark) Yolk else colors.textPrimary,
            modifier = Modifier.size(20.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                if (count == 1) "1 producto sin revisar alérgenos" else "$count productos sin revisar alérgenos",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary,
            )
            Text(
                "Revísalos para que los alumnos con alergias no los compren.",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMuted,
            )
        }
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ArchivedProductCard(
    product: Product,
    colors: OperationalColors,
    enabled: Boolean,
    onRestore: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(0.84f)
            .physicalPress(enabled = enabled, scale = PhysicalPressScale.ProductCard, onClick = onRestore)
            .semantics { contentDescription = "${product.name}, archivado. Regresarlo al menú" }
            .clip(RoundedCornerShape(26.dp))
            .background(colors.cardInner),
    ) {
        ProductImage(
            imageUrl = product.imageUrl,
            contentDescription = product.name,
            modifier = Modifier.fillMaxSize(),
            placeholder = { m -> Box(m.background(colors.cardInner)) },
            card = true,
        )
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))
        Column(
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text("Archivado", color = Color.White.copy(alpha = 0.88f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
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
    takesPreorders: Boolean = false,
) {
    val context = LocalContext.current
    // Un servidor que no conoce los días de preparación no los manda: sale solo en repostería o si ya los trae.
    val showsPreparation = takesPreorders || product?.preparationDays != null
    var preparationDays by remember { mutableStateOf(product?.preparationDays ?: 0) }
    var maximumPerDay by remember { mutableStateOf(product?.maximumPerDay?.toString().orEmpty()) }
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
        if (showsPreparation) {
            ProductPreparationEditor(
                days = preparationDays,
                onDaysChange = { preparationDays = it },
                maximumPerDay = maximumPerDay,
                onMaximumChange = { maximumPerDay = it },
                enabled = !saving,
                colors = colors,
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
                Text(
                    if (available) "Prendido: se puede pedir." else "Apagado: sale como agotado y no se puede pedir.",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                )
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
                        preparation =
                            ProductPreparationInput(preparationDays, maximumPerDay.toIntOrNull())
                                .takeIf { showsPreparation },
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

private val RentalWords =
    Regex("\\b(renta|rentar|cancha|canchas|hora|horas|reserva|p[aá]del)\\b", RegexOption.IGNORE_CASE)

private fun stationLabel(station: PreparationStation): String =
    when (station) {
        PreparationStation.CASHIER -> "Barra"
        PreparationStation.KITCHEN -> "Cocina"
    }

/** Deja dígitos y un separador con hasta dos decimales; la coma del teclado cuenta como punto. */
private fun moneyInput(raw: String): String = MoneyInput.sanitize(raw)
